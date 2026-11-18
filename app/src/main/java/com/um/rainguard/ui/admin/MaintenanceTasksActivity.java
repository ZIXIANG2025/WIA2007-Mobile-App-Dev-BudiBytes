package com.um.rainguard.ui.admin;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.um.rainguard.R;
import com.um.rainguard.data.model.Report;
import com.um.rainguard.data.model.ReportStatus;
import com.um.rainguard.databinding.ItemTaskCardBinding;
import com.um.rainguard.databinding.ScreenMaintenanceTasksBinding;
import com.um.rainguard.ui.base.BaseActivity;
import com.um.rainguard.utils.NavigationHelper;
import com.um.rainguard.utils.UIUtils;

import java.util.List;

public class MaintenanceTasksActivity extends BaseActivity {
    private ScreenMaintenanceTasksBinding binding;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.screen_maintenance_tasks;
    }

    @Override
    protected String getInitialPage() {
        return "tasks";
    }

    @Override
    protected void initView() {
        View rootView = findViewById(R.id.main_root_container);
        if (rootView == null) return;

        binding = ScreenMaintenanceTasksBinding.bind(rootView);
        setupTasksUI();

        observe(repository.observeReports(), reports -> updateTasksList());
        updateTasksList();
    }

    private void setupTasksUI() {
        if (binding == null) return;

        String[] options = new String[]{
                "All", "Awaiting Assignment", "Assigned", "Inspection in Progress",
                "Repair in Progress", "Completed"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.tasksStatusSpinner.setAdapter(adapter);

        binding.tasksStatusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTasksList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateTasksList() {
        if (binding == null || binding.tasksStatusSpinner.getSelectedItem() == null) return;

        binding.tasksContainer.removeAllViews();
        String selectedFilter = binding.tasksStatusSpinner.getSelectedItem().toString();

        int count = 0;
        List<Report> reports = repository.getReports();
        for (Report item : reports) {
            if (item.status != ReportStatus.VERIFIED && item.assignedTeam == null) continue;
            String taskStatus = UIUtils.taskStatusLabel(item.taskStatus);
            if (!"All".equals(selectedFilter) && !selectedFilter.equals(taskStatus)) continue;

            addTaskCard(item, taskStatus);
            count++;
        }

        if (count == 0) {
            binding.tasksEmptyText.setVisibility(View.VISIBLE);
        } else {
            binding.tasksEmptyText.setVisibility(View.GONE);
        }
    }

    private void addTaskCard(Report item, String taskStatus) {
        ItemTaskCardBinding cardBinding = ItemTaskCardBinding.inflate(getLayoutInflater(), binding.tasksContainer, false);
        String status = UIUtils.statusLabel(item.status);

        cardBinding.taskTitle.setText(UIUtils.reportCode(item.reportId) + " · " + item.locationName);
        cardBinding.taskTeam.setText(item.assignedTeam == null ? "No team allocated" : item.assignedTeam);

        cardBinding.taskStatus.setText(taskStatus + " · " + status);
        cardBinding.taskStatus.setTextColor(UIUtils.statusColor(status));

        cardBinding.taskInstructions.setText(item.instructions == null ? "" : item.instructions);

        cardBinding.getRoot().setOnClickListener(v -> NavigationHelper.navigateTo(
                this, role, userEmail, "report-details", item.reportId, currentPage));

        binding.tasksContainer.addView(cardBinding.getRoot());
    }
}
