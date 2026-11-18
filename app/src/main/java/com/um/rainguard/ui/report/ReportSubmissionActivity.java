package com.um.rainguard.ui.report;

import android.content.Intent;
import android.net.Uri;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.um.rainguard.R;
import com.um.rainguard.data.model.Report;
import com.um.rainguard.data.model.ReportCategory;
import com.um.rainguard.data.model.Severity;
import com.um.rainguard.databinding.ScreenReportSubmissionBinding;
import com.um.rainguard.ui.base.BaseActivity;
import com.um.rainguard.utils.Constants;
import com.um.rainguard.utils.NavigationHelper;
import com.um.rainguard.utils.UIUtils;

public class ReportSubmissionActivity extends BaseActivity {
    private ScreenReportSubmissionBinding binding;
    private Uri selectedPhoto;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.screen_report_submission;
    }

    @Override
    protected String getInitialPage() {
        return "report";
    }

    @Override
    protected void initView() {
        View rootView = findViewById(R.id.main_root_container);
        if (rootView == null) return;

        binding = ScreenReportSubmissionBinding.bind(rootView);
        setupReportFormUI();
    }

    private void setupReportFormUI() {
        if (binding == null) return;

        setupSpinner(binding.placeSpinner, new String[]{
                "Faculty of Engineering, Block A", "KK8 walkway to bus stop",
                "Main Library back entrance", "Rimba Ilmu car park",
                "Dewan Tunku Canselor side drain"
        });

        setupSpinner(binding.categorySpinner, new String[]{"Clogged", "Stagnant water", "Overflow", "Sediment"});
        setupSpinner(binding.riskSpinner, new String[]{"Medium", "Low", "High"});

        binding.btnConfirmExisting.setOnClickListener(v -> {
            String place = binding.placeSpinner.getSelectedItem().toString();
            ReportCategory category = UIUtils.categoryFromLabel(binding.categorySpinner.getSelectedItem().toString());
            Report existing = repository.findSimilar(place, category);
            if (existing != null) {
                repository.addStillThere(existing.reportId);
                showToast("Existing report confirmed.");
            } else {
                showToast("No similar open report. Submit a new one.");
            }
            NavigationHelper.navigateTo(this, role, userEmail, "reports", null, "report");
            finish();
        });

        binding.btnChoosePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, Constants.REQUEST_REPORT_PHOTO);
        });

        binding.btnSubmitReport.setOnClickListener(v -> {
            String details = binding.descriptionInput.getText().toString().trim();
            if (details.length() < 10) {
                binding.descriptionInput.setError("Describe the issue using at least 10 characters.");
                return;
            }
            createReport(
                    binding.placeSpinner.getSelectedItem().toString(),
                    binding.categorySpinner.getSelectedItem().toString(),
                    binding.riskSpinner.getSelectedItem().toString(),
                    details
            );
        });
    }

    private void setupSpinner(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void createReport(String place, String categoryLabel, String riskLabel, String details) {
        if (isAdmin()) {
            showToast("Campus users only.");
            return;
        }

        ReportCategory category = UIUtils.categoryFromLabel(categoryLabel);
        Severity severity = UIUtils.severityFromLabel(riskLabel);
        String zone = UIUtils.zoneForPlace(place);
        String photoUri = selectedPhoto == null ? null : selectedPhoto.toString();

        // The repository stores the report, writes the demo AI row and moves it to the review queue.
        repository.createReport(userId, place, zone, category, severity, details, photoUri);

        selectedPhoto = null;
        showToast("Report submitted. Awaiting administrator review.");
        NavigationHelper.navigateTo(this, role, userEmail, "reports", null, "report");
        finish();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }

        if (requestCode == Constants.REQUEST_REPORT_PHOTO) {
            selectedPhoto = uri;
            if (binding != null) {
                binding.btnChoosePhoto.setText("Photo selected · " + uri.getLastPathSegment());
            }
        }
    }
}
