package com.um.rainguard.ui.auth;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.um.rainguard.databinding.ScreenSignInBinding;
import com.um.rainguard.utils.Constants;
import com.um.rainguard.utils.NavigationHelper;

import java.util.Locale;

public class MainActivity extends Activity {
    private ScreenSignInBinding binding;
    private SharedPreferences preferences;
    private String role;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Constants.BG);
        getWindow().setNavigationBarColor(Constants.BG);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        binding = ScreenSignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View rootContent = findViewById(android.R.id.content);
        if (rootContent != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootContent, (v, insets) -> {
                Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }

        preferences = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);

        // Always show the Sign In / Role Selection screen on start
        setupSignInListeners();
    }

    private void setupSignInListeners() {
        View.OnClickListener chooseRoleListener = view -> {
            role = view.getId() == binding.chooseAdmin.getId() ? "admin" : "student";
            binding.roleSelection.setVisibility(View.GONE);
            binding.loginForm.setVisibility(View.VISIBLE);
            binding.loginRoleTitle.setText("admin".equals(role)
                    ? "Admin / Maintenance Team"
                    : "UM Campus User");
            binding.emailInput.setText("admin".equals(role) ? "admin@um.edu.my" : "student@um.edu.my");
            binding.passwordInput.setText("");
            binding.loginError.setVisibility(View.GONE);
        };

        binding.chooseStudent.setOnClickListener(chooseRoleListener);
        binding.chooseAdmin.setOnClickListener(chooseRoleListener);

        binding.backToRoles.setOnClickListener(view -> {
            role = null;
            binding.roleSelection.setVisibility(View.VISIBLE);
            binding.loginForm.setVisibility(View.GONE);
            binding.loginError.setVisibility(View.GONE);
            binding.passwordInput.setText("");
        });

        binding.signIn.setOnClickListener(view -> {
            String enteredEmail = binding.emailInput.getText().toString().trim().toLowerCase(Locale.ROOT);
            String enteredPassword = binding.passwordInput.getText().toString();

            if (role == null) {
                showLoginError(binding.loginError, "Choose a demo role first.");
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(enteredEmail).matches()) {
                showLoginError(binding.loginError, "Enter a valid email address.");
            } else if (!enteredPassword.equals("RainGuard123!")) {
                showLoginError(binding.loginError, "Incorrect demo password. Use RainGuard123!");
            } else {
                userEmail = enteredEmail;
                boolean remember = binding.rememberMe.isChecked();
                preferences.edit()
                        .putBoolean("remember", remember)
                        .putString("role", remember ? role : null)
                        .putString("email", remember ? userEmail : null)
                        .apply();

                NavigationHelper.navigateTo(this, role, userEmail, "dashboard", null, "dashboard");
                finish();
            }
        });
    }

    private void showLoginError(TextView errorView, String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }
}
