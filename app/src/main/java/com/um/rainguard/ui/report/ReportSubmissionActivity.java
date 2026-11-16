package com.um.rainguard.ui.report;

import android.content.Intent;
import android.net.Uri;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.um.rainguard.R;
import com.um.rainguard.data.model.ReportItem;
import com.um.rainguard.databinding.ScreenReportSubmissionBinding;
import com.um.rainguard.ui.base.BaseActivity;
import com.um.rainguard.utils.Constants;
import com.um.rainguard.utils.NavigationHelper;
import com.um.rainguard.utils.UIUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
        if (rootView != null) {
            binding = ScreenReportSubmissionBinding.bind(rootView);
            setupReportFormUI();
        }
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
            ReportItem existing = repository.findReport("RG-1042");
            if (existing != null) {
                existing.votes++;
                repository.persistReports();
            }
            showToast("Existing report confirmed.");
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

    private void createReport(String place, String category, String risk, String details) {
        if (isAdmin()) {
            showToast("Campus users only.");
            return;
        }

        int nextId = 1042;
        for (ReportItem item : repository.getReports()) {
            try {
                nextId = Math.max(nextId, Integer.parseInt(item.id.replace("RG-", "")));
            } catch (NumberFormatException ignored) {
            }
        }

        String categoryLabel = "Overflow".equals(category) ? "Overflowing drain" : category;
        ReportItem created = new ReportItem("RG-" + (nextId + 1), place, category, categoryLabel,
                "Submitted", risk, "Just now", 0, 0, details, userEmail,
                new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date()),
                null, null, null);

        if (selectedPhoto != null) {
            created.photoUri = selectedPhoto.toString();
        }

        created.addHistory(UIUtils.timestamp(), "Report submitted. Awaiting administrator review.");
        repository.addReport(created);

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
