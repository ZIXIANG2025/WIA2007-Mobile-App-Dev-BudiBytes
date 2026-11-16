package com.um.rainguard.ui.myreports;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.um.rainguard.R;
import com.um.rainguard.data.model.ReportItem;
import com.um.rainguard.databinding.ItemReportTableRowBinding;
import com.um.rainguard.databinding.ScreenMyReportsBinding;
import com.um.rainguard.ui.base.BaseActivity;
import com.um.rainguard.utils.NavigationHelper;
import com.um.rainguard.utils.UIUtils;

import java.util.List;

public class MyReportsActivity extends BaseActivity {
    protected ScreenMyReportsBinding binding;
    protected boolean isManagement = false;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.screen_my_reports;
    }

    @Override
    protected String getInitialPage() {
        return "reports";
    }

    @Override
    protected void initView() {
        View rootView = findViewById(R.id.main_root_container);
        if (rootView != null) {
            binding = ScreenMyReportsBinding.bind(rootView);
            setupReportsUI();
        }
    }

    protected void setupReportsUI() {
        if (binding == null) return;

        binding.reportsHeaderTitle.setText(isManagement ? "All drainage reports" : "Your drainage reports");
        binding.reportsHeaderSubtitle.setText(isManagement ? "Review, verify and coordinate a response" : "Updates from the maintenance team");

        setupSpinner(binding.reportsStatusSpinner, new String[]{
                "All", "Submitted", "Under Review", "Verified", "Rejected", "In progress", "Resolved"
        });
        setupSpinner(binding.reportsCategorySpinner, new String[]{"All", "Clogged", "Stagnant water", "Overflow", "Sediment"});
        setupSpinner(binding.reportsRiskSpinner, new String[]{"All", "High", "Medium", "Low"});

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTableResults();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        binding.reportsSearchInput.addTextChangedListener(textWatcher);
        binding.reportsDateInput.addTextChangedListener(textWatcher);

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTableResults();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        binding.reportsStatusSpinner.setOnItemSelectedListener(spinnerListener);
        binding.reportsCategorySpinner.setOnItemSelectedListener(spinnerListener);
        binding.reportsRiskSpinner.setOnItemSelectedListener(spinnerListener);

        updateTableResults();
    }

    private void setupSpinner(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    protected void updateTableResults() {
        if (binding == null) return;

        String query = binding.reportsSearchInput.getText().toString();
        String status = binding.reportsStatusSpinner.getSelectedItem().toString();
        String category = binding.reportsCategorySpinner.getSelectedItem().toString();
        String risk = binding.reportsRiskSpinner.getSelectedItem().toString();
        String day = binding.reportsDateInput.getText().toString().trim();

        List<ReportItem> matches = repository.visibleReports(isManagement, userEmail, query, status, category, risk, day);

        binding.reportsHeaderSubtitle.setText(matches.size() + " reports · " +
                (isManagement ? "Review, verify and coordinate a response" : "Updates from the maintenance team"));

        binding.reportsTableBody.removeAllViews();
        if (matches.isEmpty()) {
            binding.reportsEmptyText.setVisibility(View.VISIBLE);
        } else {
            binding.reportsEmptyText.setVisibility(View.GONE);
            for (ReportItem item : matches) {
                addTableRow(item);
            }
        }
    }

    private void addTableRow(ReportItem item) {
        ItemReportTableRowBinding rowBinding = ItemReportTableRowBinding.inflate(getLayoutInflater(), binding.reportsTableBody, false);

        rowBinding.rowReportId.setText(item.id);
        rowBinding.rowReportPlace.setText(item.place);
        rowBinding.rowReportDate.setText(UIUtils.formatDate(item.createdAt) + ", 16:12");

        rowBinding.rowReportCategory.setText(item.category);
        rowBinding.rowReportRisk.setText(item.risk + " risk");
        rowBinding.rowReportRisk.setTextColor(UIUtils.riskColor(item.risk));

        rowBinding.rowReportStatus.setText(item.status);
        rowBinding.rowReportStatus.setTextColor(UIUtils.statusColor(item.status));
        rowBinding.rowReportStatus.setBackground(UIUtils.createButtonDrawable(this,
                UIUtils.statusBgColor(item.status), UIUtils.statusBgColor(item.status), 6));

        rowBinding.rowBtnViewDetails.setOnClickListener(v -> NavigationHelper.navigateTo(this, role, userEmail, "report-details", item.id, currentPage));

        binding.reportsTableBody.addView(rowBinding.getRoot());
    }
}
