package com.um.rainguard.ui.admin;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.um.rainguard.R;
import com.um.rainguard.data.model.ReportItem;
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
        if (rootView != null) {
            binding = ScreenMaintenanceTasksBinding.bind(rootView);
            setupTasksUI();
        }
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

        updateTasksList();
    }

    private void updateTasksList() {
        if (binding == null) return;

        binding.tasksContainer.removeAllViews();
        String selectedFilter = binding.tasksStatusSpinner.getSelectedItem().toString();

        int count = 0;
        List<ReportItem> reports = repository.getReports();
        for (ReportItem item : reports) {
            if (!"Verified".equals(item.status) && item.team == null) continue;
            String taskStatus = item.taskStatus == null ? "Awaiting Assignment" : item.taskStatus;
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

    private void addTaskCard(ReportItem item, String taskStatus) {
        ItemTaskCardBinding cardBinding = ItemTaskCardBinding.inflate(getLayoutInflater(), binding.tasksContainer, false);

        cardBinding.taskTitle.setText(item.id + " · " + item.place);
        cardBinding.taskTeam.setText(item.team == null ? "No team allocated" : item.team);

        cardBinding.taskStatus.setText(taskStatus + " · " + item.status);
        cardBinding.taskStatus.setTextColor(UIUtils.statusColor(item.status));

        cardBinding.taskInstructions.setText(item.instructions == null ? "" : item.instructions);

        cardBinding.getRoot().setOnClickListener(v -> NavigationHelper.navigateTo(this, role, userEmail, "report-details", item.id, currentPage));

        binding.tasksContainer.addView(cardBinding.getRoot());
    }
}
