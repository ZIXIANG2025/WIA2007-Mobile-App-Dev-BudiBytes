package com.um.rainguard.ui.hazardmap;

import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.um.rainguard.R;
import com.um.rainguard.data.model.ReportItem;
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
        if (rootView != null) {
            binding = ScreenHazardMapBinding.bind(rootView);
            setupMapUI();
        }
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
        updateMapResults();
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
                boolean isSelected = (pill == v);
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(UIUtils.dp(this, 20));
                if (isSelected) {
                    bg.setColor(Constants.NAVY);
                    pill.setTextColor(Constants.WHITE);
                } else {
                    bg.setColor(Constants.WHITE);
                    bg.setStroke(UIUtils.dp(this, 1), android.graphics.Color.rgb(226, 232, 240));
                    pill.setTextColor(android.graphics.Color.rgb(15, 23, 42));
                }
                pill.setBackground(bg);
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
        if (binding == null) return;

        String zone = binding.mapZoneSpinner.getSelectedItem().toString();
        String category = binding.mapCategorySpinner.getSelectedItem().toString();
        String risk = binding.mapRiskSpinner.getSelectedItem().toString();
        String status = binding.mapStatusSpinner.getSelectedItem().toString();

        List<ReportItem> filtered = new ArrayList<>();
        for (ReportItem item : repository.getReports()) {
            if ("Rejected".equals(item.status)) continue;
            if (!"All".equals(zone) && !zoneFor(item).equals(zone)) continue;
            if (!"All".equals(category) && !item.category.equals(category)) continue;
            if (!"All".equals(risk) && !item.risk.equals(risk)) continue;
            if (!"All".equals(status) && !item.status.equals(status)) continue;
            filtered.add(item);
        }

        binding.mapSketchView.setMapReports(filtered);

        binding.mapResultsContainer.removeAllViews();
        if (filtered.isEmpty()) {
            binding.mapEmptyText.setVisibility(View.VISIBLE);
        } else {
            binding.mapEmptyText.setVisibility(View.GONE);
            for (ReportItem item : filtered) {
                addReportCardToResults(item);
            }
        }
    }

    private String zoneFor(ReportItem item) {
        if (item.place.contains("KK8")) return "KK8";
        if (item.place.contains("Engineering") || item.place.contains("Faculty")) return "Faculty buildings";
        return "Central campus";
    }

    private void addReportCardToResults(ReportItem item) {
        ItemReportCardBinding cardBinding = ItemReportCardBinding.inflate(getLayoutInflater(), binding.mapResultsContainer, false);

        cardBinding.reportDot.setTextColor(UIUtils.riskColor(item.risk));
        cardBinding.reportPlace.setText(item.place);
        cardBinding.reportTime.setText(item.time);
        cardBinding.reportSubtitle.setText(item.type + " · " + item.id);

        cardBinding.reportStatusPill.setText(item.status);
        cardBinding.reportStatusPill.setTextColor(UIUtils.statusColor(item.status));
        cardBinding.reportStatusPill.setBackground(UIUtils.createButtonDrawable(this,
                UIUtils.statusBgColor(item.status), UIUtils.statusBgColor(item.status), 8));

        cardBinding.reportConfirmedText.setText("👍 " + item.votes + " confirmed");
        cardBinding.reportActionRow.setVisibility(View.GONE);

        cardBinding.getRoot().setOnClickListener(v -> NavigationHelper.navigateTo(this, role, userEmail, "report-details", item.id, currentPage));

        binding.mapResultsContainer.addView(cardBinding.getRoot());
    }
}
