package com.autodroid.proxy.auth.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.autodroid.proxy.MainActivity;
import com.autodroid.proxy.R;
import com.autodroid.proxy.auth.viewmodel.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {
    
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button registerButton;
    private TextView errorTextView;
    private TextView loginLink;
    
    private AuthViewModel authViewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        // Initialize UI components
        emailEditText = findViewById(R.id.register_email);
        passwordEditText = findViewById(R.id.register_password);
        confirmPasswordEditText = findViewById(R.id.register_confirm_password);
        registerButton = findViewById(R.id.register_button);
        errorTextView = findViewById(R.id.register_error);
        loginLink = findViewById(R.id.login_link);
        
        // Initialize ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        // Set up observers
        observeViewModel();
        
        // Set up click listeners
        setupClickListeners();
    }
    
    private void observeViewModel() {
        // Observe authentication state
        authViewModel.getIsAuthenticated().observe(this, isAuthenticated -> {
            if (isAuthenticated) {
                // Navigate to main activity on successful registration
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
        
        // Observe loading state
        authViewModel.getIsLoading().observe(this, isLoading -> {
            registerButton.setEnabled(!isLoading);
            if (isLoading) {
                registerButton.setText(getString(R.string.register_button_loading));
            } else {
                registerButton.setText(getString(R.string.register_button));
            }
        });
        
        // Observe error messages
        authViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                errorTextView.setText(errorMessage);
                errorTextView.setVisibility(View.VISIBLE);
            } else {
                errorTextView.setVisibility(View.GONE);
            }
        });
    }
    
    private void setupClickListeners() {
        // Register button click listener
        registerButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();
            
            // Validate input
            if (validateInput(email, password, confirmPassword)) {
                // Call register method in ViewModel
                authViewModel.register(email, password, confirmPassword);
            }
        });
        
        // Login link click listener
        loginLink.setOnClickListener(v -> {
            // Navigate back to login activity
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
    
    private boolean validateInput(String email, String password, String confirmPassword) {
        boolean isValid = true;
        
        // Clear previous error
        errorTextView.setVisibility(View.GONE);
        
        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getString(R.string.email_required));
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError(getString(R.string.email_invalid));
            isValid = false;
        }
        
        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.password_required));
            isValid = false;
        } else if (password.length() < 6) {
            passwordEditText.setError(getString(R.string.password_min_length));
            isValid = false;
        }
        
        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.setError(getString(R.string.confirm_password));
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError(getString(R.string.passwords_mismatch));
            isValid = false;
        }
        
        return isValid;
    }
}
