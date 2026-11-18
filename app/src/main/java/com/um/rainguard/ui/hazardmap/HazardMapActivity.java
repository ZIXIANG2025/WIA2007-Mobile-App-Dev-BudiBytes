package com.um.rainguard.ui.hazardmap;

import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.um.rainguard.R;
import com.um.rainguard.data.model.Report;
import com.um.rainguard.data.model.ReportStatus;
import com.um.rainguard.databinding.ItemReportCardBinding;
import com.um.rainguard.databinding.ScreenHazardMapBinding;
import com.um.rainguard.ui.base.BaseActivity;
import com.um.rainguard.utils.Constants;
import com.um.rainguard.utils.NavigationHelper;
import com.um.rainguard.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class HazardMapActivity extends BaseActivity {
    private ScreenHazardMapBinding binding;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.screen_hazard_map;
    }

    @Override
    protected String getInitialPage() {
        return "map";
    }

    @Override
    protected void initView() {
        View rootView = findViewById(R.id.main_root_container);
        if (rootView == null) return;

        binding = ScreenHazardMapBinding.bind(rootView);
        setupMapUI();

        observe(repository.observeReports(), reports -> updateMapResults());
        updateMapResults();
    }

    private void setupMapUI() {
        if (binding == null) return;

        setupSpinner(binding.mapZoneSpinner, new String[]{"All", "KK8", "Faculty buildings", "Central campus"});
        setupSpinner(binding.mapCategorySpinner, new String[]{"All", "Clogged", "Stagnant water", "Overflow", "Sediment"});
        setupSpinner(binding.mapRiskSpinner, new String[]{"All", "High", "Medium", "Low"});
        setupSpinner(binding.mapStatusSpinner, new String[]{"All", "Submitted", "Under Review", "Verified", "In progress", "Resolved"});

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateMapResults();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        binding.mapZoneSpinner.setOnItemSelectedListener(listener);
        binding.mapCategorySpinner.setOnItemSelectedListener(listener);
        binding.mapRiskSpinner.setOnItemSelectedListener(listener);
        binding.mapStatusSpinner.setOnItemSelectedListener(listener);

        setupFilterPills();
    }

    private void setupSpinner(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setupFilterPills() {
        TextView[] pills = new TextView[]{
                binding.pillAll, binding.pillHighRisk, binding.pillStagnant, binding.pillClogged, binding.pillResolved
        };

        View.OnClickListener pillClickListener = v -> {
            for (TextView pill : pills) {
                if (pill == v) {
                    GradientDrawable selected = new GradientDrawable();
                    selected.setCornerRadius(UIUtils.dp(this, 20));
                    selected.setColor(Constants.NAVY);
                    pill.setTextColor(Constants.WHITE);
                    pill.setBackground(selected);
                } else {
                    GradientDrawable normal = new GradientDrawable();
                    normal.setCornerRadius(UIUtils.dp(this, 20));
                    normal.setColor(Constants.WHITE);
                    normal.setStroke(UIUtils.dp(this, 1), android.graphics.Color.rgb(226, 232, 240));
                    pill.setTextColor(android.graphics.Color.rgb(15, 23, 42));
                    pill.setBackground(normal);
                }
            }

            if (v == binding.pillHighRisk) binding.mapRiskSpinner.setSelection(1);
            else if (v == binding.pillStagnant) binding.mapCategorySpinner.setSelection(2);
            else if (v == binding.pillClogged) binding.mapCategorySpinner.setSelection(1);
            else if (v == binding.pillResolved) binding.mapStatusSpinner.setSelection(5);
            else {
                binding.mapRiskSpinner.setSelection(0);
                binding.mapCategorySpinner.setSelection(0);
                binding.mapStatusSpinner.setSelection(0);
            }
        };

        for (TextView pill : pills) {
            pill.setOnClickListener(pillClickListener);
        }
    }

    private void updateMapResults() {
        if (binding == null || binding.mapZoneSpinner.getSelectedItem() == null) return;

        String zone = binding.mapZoneSpinner.getSelectedItem().toString();
        String category = binding.mapCategorySpinner.getSelectedItem().toString();
        String risk = binding.mapRiskSpinner.getSelectedItem().toString();
        String status = binding.mapStatusSpinner.getSelectedItem().toString();

        List<Report> filtered = new ArrayList<>();
        for (Report item : repository.getReports()) {
            if (item.status == ReportStatus.REJECTED) continue;
            if (!"All".equals(zone) && !zoneOf(item).equals(zone)) continue;
            if (!"All".equals(category) && !UIUtils.categoryLabel(item.category).equals(category)) continue;
            if (!"All".equals(risk) && !UIUtils.riskLabel(item.severity).equals(risk)) continue;
            if (!"All".equals(status) && !UIUtils.statusLabel(item.status).equals(status)) continue;
            filtered.add(item);
        }

        binding.mapSketchView.setMapReports(filtered);

        binding.mapResultsContainer.removeAllViews();
        if (filtered.isEmpty()) {
            binding.mapEmptyText.setVisibility(View.VISIBLE);
        } else {
            binding.mapEmptyText.setVisibility(View.GONE);
            for (Report item : filtered) {
                addReportCardToResults(item);
            }
        }
    }

    /** The zone is a column now, so no guessing from the place name. */
    private String zoneOf(Report item) {
        return item.zone == null || item.zone.isEmpty() ? UIUtils.zoneForPlace(item.locationName) : item.zone;
    }

    private void addReportCardToResults(Report item) {
        ItemReportCardBinding cardBinding = ItemReportCardBinding.inflate(getLayoutInflater(), binding.mapResultsContainer, false);

        String status = UIUtils.statusLabel(item.status);

        cardBinding.reportDot.setTextColor(UIUtils.riskColor(item.severity));
        cardBinding.reportPlace.setText(item.locationName);
        cardBinding.reportTime.setText(UIUtils.relativeTime(item.createdAt));
        cardBinding.reportSubtitle.setText(UIUtils.categoryLabel(item.category) + " · " + UIUtils.reportCode(item.reportId));

        cardBinding.reportStatusPill.setText(status);
        cardBinding.reportStatusPill.setTextColor(UIUtils.statusColor(status));
        cardBinding.reportStatusPill.setBackground(UIUtils.createButtonDrawable(this,
                UIUtils.statusBgColor(status), UIUtils.statusBgColor(status), 8));

        cardBinding.reportConfirmedText.setText("👍 " + item.stillThereCount + " confirmed");
        cardBinding.reportActionRow.setVisibility(View.GONE);

        cardBinding.getRoot().setOnClickListener(v -> NavigationHelper.navigateTo(
                this, role, userEmail, "report-details", item.reportId, currentPage));

        binding.mapResultsContainer.addView(cardBinding.getRoot());
    }
}
