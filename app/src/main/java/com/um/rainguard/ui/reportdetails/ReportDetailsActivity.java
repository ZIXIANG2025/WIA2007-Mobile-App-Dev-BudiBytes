package com.um.rainguard.ui.reportdetails;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.um.rainguard.R;
import com.um.rainguard.data.model.AiTriage;
import com.um.rainguard.data.model.MaintenanceTeams;
import com.um.rainguard.data.model.Report;
import com.um.rainguard.data.model.ReportStatus;
import com.um.rainguard.data.model.TaskStatus;
import com.um.rainguard.data.model.VerificationLog;
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
    private Report selectedReport;
    private AiTriage triage;
    private List<VerificationLog> timeline;

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
        View rootView = findViewById(R.id.main_root_container);
        if (rootView == null) return;

        binding = ScreenReportDetailsBinding.bind(rootView);

        selectedReport = repository.findReport(selectedReportId);
        if (selectedReport != null) {
            setupReportDetailsUI();
        }

        observe(repository.observeReport(selectedReportId), report -> {
            if (report == null) {
                // The report was deleted or the id is stale — go back rather than show an empty page.
                NavigationHelper.navigateTo(this, role, userEmail, returnPage, null, returnPage);
                finish();
                return;
            }
            selectedReport = report;
            setupReportDetailsUI();
        });

        observe(repository.observeTimeline(selectedReportId), logs -> {
            timeline = logs;
            setupHistorySection();
        });

        observe(repository.observeTriage(selectedReportId), suggestion -> {
            triage = suggestion;
            setupAiSection();
        });
    }

    private void setupReportDetailsUI() {
        if (binding == null || selectedReport == null) return;

        String status = UIUtils.statusLabel(selectedReport.status);
        String risk = UIUtils.riskLabel(selectedReport.severity);

        binding.detailHeaderInfo.setText(UIUtils.reportCode(selectedReport.reportId) + " · "
                + UIUtils.formatStamp(selectedReport.createdAt) + " MYT");
        binding.detailPlaceTitle.setText(selectedReport.locationName);

        binding.detailRiskTag.setText(risk + " risk");
        binding.detailRiskTag.setTextColor(UIUtils.riskColor(risk));

        binding.detailStatusPill.setText(status);
        binding.detailStatusPill.setTextColor(UIUtils.statusColor(status));
        binding.detailStatusPill.setBackground(UIUtils.createButtonDrawable(this,
                UIUtils.statusBgColor(status), UIUtils.statusBgColor(status), 6));

        binding.detailOriginalCategory.setText("ORIGINAL REPORT · "
                + UIUtils.categoryLabel(selectedReport.category).toUpperCase(Locale.ROOT));
        binding.detailDescription.setText(selectedReport.description);

        setupPhotos();
        setupMaintenanceSection();

        if (isAdmin()) {
            binding.detailCommunitySection.setVisibility(View.GONE);
            binding.detailAdminSection.setVisibility(View.VISIBLE);
            setupAiSection();
            setupDuplicateSuggestions();
            setupAdminActionButtons();
        } else {
            binding.detailAdminSection.setVisibility(View.GONE);
            if (selectedReport.status != ReportStatus.RESOLVED && selectedReport.status != ReportStatus.REJECTED) {
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
        if (selectedReport.assignedTeam != null || selectedReport.taskStatus != null) {
            binding.detailMaintenanceContainer.setVisibility(View.VISIBLE);
            binding.detailTeamText.setText("Responsible team: "
                    + (selectedReport.assignedTeam == null ? "JPPHB Drainage Team" : selectedReport.assignedTeam));
            binding.detailTaskStatusPill.setText(UIUtils.taskStatusLabel(selectedReport.taskStatus));
        } else {
            binding.detailMaintenanceContainer.setVisibility(View.GONE);
        }
    }

    /** The timeline is the report's own creation, followed by every verification_logs row. */
    private void setupHistorySection() {
        if (binding == null || selectedReport == null) return;

        binding.detailHistoryContainer.removeAllViews();

        ItemHistoryEntryBinding created = ItemHistoryEntryBinding.inflate(getLayoutInflater(), binding.detailHistoryContainer, false);
        created.historyText.setText("Report " + UIUtils.reportCode(selectedReport.reportId) + " received.");
        created.historyDate.setText(UIUtils.formatStamp(selectedReport.createdAt) + " MYT");
        binding.detailHistoryContainer.addView(created.getRoot());

        if (timeline == null) return;
        for (VerificationLog entry : timeline) {
            ItemHistoryEntryBinding historyBinding = ItemHistoryEntryBinding.inflate(getLayoutInflater(), binding.detailHistoryContainer, false);
            historyBinding.historyText.setText(entry.remarks);
            historyBinding.historyDate.setText(UIUtils.formatStamp(entry.createdAt) + " MYT");
            binding.detailHistoryContainer.addView(historyBinding.getRoot());
        }
    }

    private void setupCommunitySection() {
        binding.btnDetailStillThere.setText("Still There (" + selectedReport.stillThereCount + ")");
        binding.btnDetailCleared.setText("Cleared (" + selectedReport.clearedCount + ")");

        binding.btnDetailStillThere.setOnClickListener(v -> {
            repository.addStillThere(selectedReport.reportId);
            showToast("Still There observation recorded.");
        });

        binding.btnDetailCleared.setOnClickListener(v -> {
            repository.addCleared(selectedReport.reportId);
            showToast("Cleared observation recorded.");
        });
    }

    /** Module 3 lives in ai_triage; the admin page only reads it. */
    private void setupAiSection() {
        if (binding == null || selectedReport == null || !isAdmin()) return;

        boolean hasSummary = triage != null && triage.summary != null && !triage.summary.isEmpty();
        binding.detailAiSummaryText.setText(hasSummary
                ? triage.summary
                : "Generate a draft summary to support your review.");
        binding.btnGenerateSummary.setText(hasSummary ? "Regenerate Summary" : "Generate Summary");

        binding.btnGenerateSummary.setOnClickListener(v -> {
            repository.generateSummary(selectedReport.reportId);
            showToast("Demo report summary generated. No status change.");
        });

        String suggested = triage != null && triage.suggestedSeverity != null
                ? UIUtils.riskLabel(triage.suggestedSeverity)
                : UIUtils.riskLabel(selectedReport.severity);
        binding.detailSuggestedRiskText.setText("Suggested risk: " + suggested);
    }

    private void setupDuplicateSuggestions() {
        if (binding == null || selectedReport == null) return;

        binding.detailDuplicatesContainer.removeAllViews();
        List<Report> duplicates = repository.findDuplicates(selectedReport);

        for (Report other : duplicates) {
            TextView dupTv = new TextView(this);
            dupTv.setText(UIUtils.reportCode(other.reportId) + " · "
                    + UIUtils.categoryLabel(other.category) + " · " + UIUtils.statusLabel(other.status));
            dupTv.setTextColor(Constants.MUTED);
            dupTv.setTextSize(12);
            binding.detailDuplicatesContainer.addView(dupTv);
        }
        binding.detailDuplicateBox.setVisibility(duplicates.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupAdminActionButtons() {
        if (binding == null || selectedReport == null) return;

        binding.btnAdminUnderReview.setVisibility(View.GONE);
        binding.btnAdminVerify.setVisibility(View.GONE);
        binding.btnAdminReject.setVisibility(View.GONE);
        binding.btnAdminAssign.setVisibility(View.GONE);
        binding.btnAdminUpdateProgress.setVisibility(View.GONE);
        binding.btnAdminComplete.setVisibility(View.GONE);
        binding.btnAdminAddPhoto.setVisibility(View.GONE);
        binding.detailClosedText.setVisibility(View.GONE);

        if (selectedReport.status == ReportStatus.PENDING_MODERATOR) {
            binding.btnAdminVerify.setVisibility(View.VISIBLE);
            binding.btnAdminVerify.setOnClickListener(v -> {
                repository.verify(selectedReport.reportId, userId);
                showToast("Report verified.");
            });

            binding.btnAdminReject.setVisibility(View.VISIBLE);
            binding.btnAdminReject.setOnClickListener(v -> showActionDialog("reject"));
        } else if (selectedReport.status == ReportStatus.VERIFIED) {
            binding.btnAdminAssign.setVisibility(View.VISIBLE);
            binding.btnAdminAssign.setOnClickListener(v -> showActionDialog("assign"));
        } else if (selectedReport.assignedTeam != null && selectedReport.status != ReportStatus.RESOLVED) {
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
            setupSpinner(dialogBinding.dialogTeamSpinner,
                    MaintenanceTeams.ALL.toArray(new String[0]));
        } else if ("progress".equals(action)) {
            dialogBinding.dialogStatusLabel.setVisibility(View.VISIBLE);
            dialogBinding.dialogStatusSpinner.setVisibility(View.VISIBLE);
            String[] progressOptions = new String[]{"Assigned", "Inspection in Progress", "Repair in Progress"};
            setupSpinner(dialogBinding.dialogStatusSpinner, progressOptions);
            if (selectedReport.taskStatus != null) {
                int index = Arrays.asList(progressOptions).indexOf(UIUtils.taskStatusLabel(selectedReport.taskStatus));
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
                .setMessage(UIUtils.reportCode(selectedReport.reportId) + " · " + selectedReport.locationName)
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
            String selectedTeam = dialogBinding.dialogTeamSpinner.getVisibility() == View.VISIBLE
                    ? dialogBinding.dialogTeamSpinner.getSelectedItem().toString() : null;
            String selectedTaskStatus = dialogBinding.dialogStatusSpinner.getVisibility() == View.VISIBLE
                    ? dialogBinding.dialogStatusSpinner.getSelectedItem().toString() : null;

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

    /**
     * Module 4: every branch is one transaction in the repository (status + verification_logs row
     * together), and the screen simply re-renders when Room reports the change.
     */
    private boolean applyReportAction(String action, String notes, String team, String taskStatusLabel) {
        if (!isAdmin()) {
            showToast("Administrators only.");
            return false;
        }

        String reportId = selectedReport.reportId;

        if ("reject".equals(action)) {
            repository.reject(reportId, userId, notes);
        } else if ("assign".equals(action)) {
            repository.assign(reportId, userId, team, notes);
        } else if ("progress".equals(action)) {
            TaskStatus taskStatus = UIUtils.taskStatusFromLabel(taskStatusLabel);
            repository.updateProgress(reportId, userId, taskStatus, notes);
        } else if ("resolve".equals(action)) {
            repository.resolve(reportId, userId, notes);
        }

        showToast("reject".equals(action) ? "Report rejected."
                : "Report update saved.");
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

        if (requestCode == Constants.REQUEST_COMPLETION_PHOTO && selectedReport != null) {
            repository.attachCompletionPhoto(selectedReport.reportId, userId, uri.toString());
            showToast("Maintenance photo attached.");
        }
    }
}
