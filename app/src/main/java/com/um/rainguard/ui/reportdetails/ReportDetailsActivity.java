package com.um.rainguard.ui.reportdetails;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.um.rainguard.R;
import com.um.rainguard.data.model.HistoryEntry;
import com.um.rainguard.data.model.ReportItem;
import com.um.rainguard.databinding.DialogAdminActionBinding;
import com.um.rainguard.databinding.ItemHistoryEntryBinding;
import com.um.rainguard.databinding.ScreenReportDetailsBinding;
import com.um.rainguard.ui.base.BaseActivity;
import com.um.rainguard.utils.Constants;
import com.um.rainguard.utils.NavigationHelper;
import com.um.rainguard.utils.UIUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ReportDetailsActivity extends BaseActivity {
    private ScreenReportDetailsBinding binding;
    private ReportItem selectedReport;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.screen_report_details;
    }

    @Override
    protected String getInitialPage() {
        return "report-details";
    }

    @Override
    protected void initView() {
        selectedReport = repository.findReport(selectedReportId);
        if (selectedReport == null) {
            NavigationHelper.navigateTo(this, role, userEmail, returnPage, null, returnPage);
            finish();
            return;
        }

        View rootView = findViewById(R.id.main_root_container);
        if (rootView != null) {
            binding = ScreenReportDetailsBinding.bind(rootView);
            setupReportDetailsUI();
        }
    }

    private void setupReportDetailsUI() {
        if (binding == null || selectedReport == null) return;

        binding.detailHeaderInfo.setText(selectedReport.id + " · " + UIUtils.formatDate(selectedReport.createdAt) + ", 16:12 MYT");
        binding.detailPlaceTitle.setText(selectedReport.place);

        binding.detailRiskTag.setText(selectedReport.risk + " risk");
        binding.detailRiskTag.setTextColor(UIUtils.riskColor(selectedReport.risk));

        binding.detailStatusPill.setText(selectedReport.status);
        binding.detailStatusPill.setTextColor(UIUtils.statusColor(selectedReport.status));
        binding.detailStatusPill.setBackground(UIUtils.createButtonDrawable(this,
                UIUtils.statusBgColor(selectedReport.status), UIUtils.statusBgColor(selectedReport.status), 6));

        binding.detailOriginalCategory.setText("ORIGINAL REPORT · " + selectedReport.category.toUpperCase(Locale.ROOT));
        binding.detailDescription.setText(selectedReport.description);

        setupPhotos();
        setupMaintenanceSection();
        setupHistorySection();

        if (isAdmin()) {
            binding.detailCommunitySection.setVisibility(View.GONE);
            binding.detailAdminSection.setVisibility(View.VISIBLE);
            setupAdminReviewSection();
        } else {
            binding.detailAdminSection.setVisibility(View.GONE);
            if (!"Resolved".equals(selectedReport.status) && !"Rejected".equals(selectedReport.status)) {
                binding.detailCommunitySection.setVisibility(View.VISIBLE);
                setupCommunitySection();
            } else {
                binding.detailCommunitySection.setVisibility(View.GONE);
            }
        }
    }

    private void setupPhotos() {
        binding.detailPhotoContainer.removeAllViews();
        if (selectedReport.photoUri != null && !selectedReport.photoUri.isEmpty()) {
            binding.detailNoPhotoText.setVisibility(View.GONE);
            addPhotoToContainer(selectedReport.photoUri, "Report photo");
        } else {
            binding.detailNoPhotoText.setVisibility(View.VISIBLE);
        }

        if (selectedReport.completionPhotoUri != null && !selectedReport.completionPhotoUri.isEmpty()) {
            addPhotoToContainer(selectedReport.completionPhotoUri, "Completion photo");
        }
    }

    private void addPhotoToContainer(String uriText, String description) {
        try {
            ImageView image = new ImageView(this);
            image.setContentDescription(description);
            image.setAdjustViewBounds(true);
            image.setMaxHeight(UIUtils.dp(this, 280));
            image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            image.setImageURI(Uri.parse(uriText));

            binding.detailPhotoContainer.addView(image);
        } catch (Exception exception) {
            TextView errorText = new TextView(this);
            errorText.setText("The saved photo is unavailable on this device.");
            errorText.setTextColor(Constants.MUTED);
            errorText.setTextSize(12);
            binding.detailPhotoContainer.addView(errorText);
        }
    }

    private void setupMaintenanceSection() {
        if (selectedReport.team != null || selectedReport.taskStatus != null) {
            binding.detailMaintenanceContainer.setVisibility(View.VISIBLE);
            binding.detailTeamText.setText("Responsible team: " + (selectedReport.team == null ? "JPPHB Drainage Team" : selectedReport.team));
            binding.detailTaskStatusPill.setText(selectedReport.taskStatus == null ? "Inspection in Progress" : selectedReport.taskStatus);
        } else {
            binding.detailMaintenanceContainer.setVisibility(View.GONE);
        }
    }

    private void setupHistorySection() {
        binding.detailHistoryContainer.removeAllViews();
        for (HistoryEntry entry : selectedReport.history) {
            ItemHistoryEntryBinding historyBinding = ItemHistoryEntryBinding.inflate(getLayoutInflater(), binding.detailHistoryContainer, false);
            historyBinding.historyText.setText(entry.text);
            historyBinding.historyDate.setText(entry.date + " MYT");
            binding.detailHistoryContainer.addView(historyBinding.getRoot());
        }
    }

    private void setupCommunitySection() {
        binding.btnDetailStillThere.setText("Still There (" + selectedReport.votes + ")");
        binding.btnDetailCleared.setText("Cleared (" + selectedReport.cleared + ")");

        binding.btnDetailStillThere.setOnClickListener(v -> {
            selectedReport.votes++;
            repository.persistReports();
            showToast("Still There observation recorded.");
            setupReportDetailsUI();
        });

        binding.btnDetailCleared.setOnClickListener(v -> {
            selectedReport.cleared++;
            repository.persistReports();
            showToast("Cleared observation recorded.");
            setupReportDetailsUI();
        });
    }

    private void setupAdminReviewSection() {
        binding.detailAiSummaryText.setText(selectedReport.summary == null ? "Generate a draft summary to support your review." : selectedReport.summary);
        binding.btnGenerateSummary.setText(selectedReport.summary == null ? "Generate Summary" : "Regenerate Summary");

        binding.btnGenerateSummary.setOnClickListener(v -> {
            selectedReport.summary = selectedReport.category + " was reported at " + selectedReport.place + ". "
                    + selectedReport.description + " The affected area may create a slipping hazard and disrupt "
                    + "pedestrian movement. An on-site inspection is recommended. This is a demo "
                    + "rule-based summary; administrator review is required.";
            repository.persistReports();
            showToast("Demo report summary generated. No status change.");
            setupReportDetailsUI();
        });

        binding.detailSuggestedRiskText.setText("Suggested risk: " + selectedReport.risk);

        setupDuplicateSuggestions();
        setupAdminActionButtons();
    }

    private void setupDuplicateSuggestions() {
        binding.detailDuplicatesContainer.removeAllViews();
        int duplicates = 0;
        List<ReportItem> reports = repository.getReports();
        for (ReportItem other : reports) {
            if (other != selectedReport && other.place.equals(selectedReport.place)
                    && other.category.equals(selectedReport.category)
                    && !"Rejected".equals(other.status)) {
                duplicates++;
                TextView dupTv = new TextView(this);
                dupTv.setText(other.id + " · " + other.category + " · " + other.status);
                dupTv.setTextColor(Constants.MUTED);
                dupTv.setTextSize(12);
                binding.detailDuplicatesContainer.addView(dupTv);
            }
        }
        binding.detailDuplicateBox.setVisibility(duplicates > 0 ? View.VISIBLE : View.GONE);
    }

    private void setupAdminActionButtons() {
        binding.btnAdminUnderReview.setVisibility(View.GONE);
        binding.btnAdminVerify.setVisibility(View.GONE);
        binding.btnAdminReject.setVisibility(View.GONE);
        binding.btnAdminAssign.setVisibility(View.GONE);
        binding.btnAdminUpdateProgress.setVisibility(View.GONE);
        binding.btnAdminComplete.setVisibility(View.GONE);
        binding.btnAdminAddPhoto.setVisibility(View.GONE);
        binding.detailClosedText.setVisibility(View.GONE);

        if ("Submitted".equals(selectedReport.status) || "Under Review".equals(selectedReport.status)) {
            if ("Submitted".equals(selectedReport.status)) {
                binding.btnAdminUnderReview.setVisibility(View.VISIBLE);
                binding.btnAdminUnderReview.setOnClickListener(v -> applyReportAction("review", null, null, null));
            }
            binding.btnAdminVerify.setVisibility(View.VISIBLE);
            binding.btnAdminVerify.setOnClickListener(v -> applyReportAction("verify", null, null, null));

            binding.btnAdminReject.setVisibility(View.VISIBLE);
            binding.btnAdminReject.setOnClickListener(v -> showActionDialog("reject"));
        } else if ("Verified".equals(selectedReport.status)) {
            binding.btnAdminAssign.setVisibility(View.VISIBLE);
            binding.btnAdminAssign.setOnClickListener(v -> showActionDialog("assign"));
        } else if (selectedReport.team != null && !"Resolved".equals(selectedReport.status)) {
            binding.btnAdminUpdateProgress.setVisibility(View.VISIBLE);
            binding.btnAdminUpdateProgress.setOnClickListener(v -> showActionDialog("progress"));

            binding.btnAdminComplete.setVisibility(View.VISIBLE);
            binding.btnAdminComplete.setOnClickListener(v -> showActionDialog("resolve"));

            binding.btnAdminAddPhoto.setVisibility(View.VISIBLE);
            binding.btnAdminAddPhoto.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, Constants.REQUEST_COMPLETION_PHOTO);
            });
        } else {
            binding.detailClosedText.setVisibility(View.VISIBLE);
        }
    }

    private void showActionDialog(String action) {
        DialogAdminActionBinding dialogBinding = DialogAdminActionBinding.inflate(getLayoutInflater());

        if ("assign".equals(action)) {
            dialogBinding.dialogTeamLabel.setVisibility(View.VISIBLE);
            dialogBinding.dialogTeamSpinner.setVisibility(View.VISIBLE);
            setupSpinner(dialogBinding.dialogTeamSpinner, new String[]{
                    "JPPHB Drainage Team", "Campus Facilities Team",
                    "Landscape & Grounds Team", "Maintenance Officer Ahmad"
            });
        } else if ("progress".equals(action)) {
            dialogBinding.dialogStatusLabel.setVisibility(View.VISIBLE);
            dialogBinding.dialogStatusSpinner.setVisibility(View.VISIBLE);
            String[] progressOptions = new String[]{"Assigned", "Inspection in Progress", "Repair in Progress"};
            setupSpinner(dialogBinding.dialogStatusSpinner, progressOptions);
            if (selectedReport.taskStatus != null) {
                int index = Arrays.asList(progressOptions).indexOf(selectedReport.taskStatus);
                if (index >= 0) dialogBinding.dialogStatusSpinner.setSelection(index);
            }
        }

        String notesLabel = "reject".equals(action) ? "Rejection reason"
                : "resolve".equals(action) ? "Resolution notes"
                : "assign".equals(action) ? "Maintenance instructions"
                : "Inspection / repair notes";

        dialogBinding.dialogNotesLabel.setText(notesLabel);
        dialogBinding.dialogNotesInput.setHint(notesLabel);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("reject".equals(action) ? "Reject report"
                        : "assign".equals(action) ? "Assign maintenance"
                        : "resolve".equals(action) ? "Complete task & resolve report"
                        : "Update maintenance progress")
                .setMessage(selectedReport.id + " · " + selectedReport.place)
                .setView(dialogBinding.getRoot())
                .setNegativeButton("Cancel", null)
                .setPositiveButton("reject".equals(action) ? "Confirm rejection"
                        : "assign".equals(action) ? "Confirm assignment"
                        : "resolve".equals(action) ? "Confirm completion" : "Save progress", null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String notes = dialogBinding.dialogNotesInput.getText().toString().trim();
            if (notes.isEmpty()) {
                dialogBinding.dialogNotesInput.setError("resolve".equals(action)
                        ? "Resolution notes are required."
                        : "Please include a reason or work notes.");
                return;
            }
            String selectedTeam = dialogBinding.dialogTeamSpinner.getVisibility() == View.VISIBLE ? dialogBinding.dialogTeamSpinner.getSelectedItem().toString() : null;
            String selectedTaskStatus = dialogBinding.dialogStatusSpinner.getVisibility() == View.VISIBLE ? dialogBinding.dialogStatusSpinner.getSelectedItem().toString() : null;

            boolean saved = applyReportAction(action, notes, selectedTeam, selectedTaskStatus);
            if (saved) dialog.dismiss();
        }));

        dialog.show();
    }

    private void setupSpinner(android.widget.Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private boolean applyReportAction(String action, String notes, String team, String taskStatus) {
        if (!isAdmin()) {
            showToast("Administrators only.");
            return false;
        }

        if ("review".equals(action) || "verify".equals(action)) {
            selectedReport.status = "review".equals(action) ? "Under Review" : "Verified";
            selectedReport.addHistory(UIUtils.timestamp(), "review".equals(action)
                    ? "Report is under administrator review."
                    : "Report verified by maintenance administrator.");
        } else if ("reject".equals(action)) {
            selectedReport.status = "Rejected";
            selectedReport.addHistory(UIUtils.timestamp(), "Rejected: " + notes);
        } else if ("assign".equals(action)) {
            selectedReport.status = "In progress";
            selectedReport.team = team;
            selectedReport.instructions = notes;
            selectedReport.taskStatus = "Assigned";
            selectedReport.addHistory(UIUtils.timestamp(), "Maintenance assigned to " + team + ". Instructions: " + notes);
        } else if ("progress".equals(action)) {
            selectedReport.taskStatus = taskStatus;
            selectedReport.addHistory(UIUtils.timestamp(), taskStatus + ": " + notes);
        } else if ("resolve".equals(action)) {
            selectedReport.status = "Resolved";
            selectedReport.taskStatus = "Completed";
            selectedReport.resolution = notes;
            selectedReport.resolvedAt = UIUtils.timestamp();
            selectedReport.addHistory(UIUtils.timestamp(), "Task Completed; report Resolved. " + notes);
        }

        repository.persistReports();
        showToast("verify".equals(action) ? "Report verified."
                : "review".equals(action) ? "Report moved to Under Review."
                : "Report update saved.");

        setupReportDetailsUI();
        return true;
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

        if (requestCode == Constants.REQUEST_COMPLETION_PHOTO) {
            if (selectedReport != null) {
                selectedReport.completionPhotoUri = uri.toString();
                selectedReport.addHistory(UIUtils.timestamp(), "Inspection / completion photo attached.");
                repository.persistReports();
                showToast("Maintenance photo attached.");
                setupReportDetailsUI();
            }
        }
    }
}
