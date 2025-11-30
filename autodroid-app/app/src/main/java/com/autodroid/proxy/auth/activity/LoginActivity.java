package com.autodroid.proxy.auth.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.concurrent.Executor;

import com.autodroid.proxy.MainActivity;
import com.autodroid.proxy.R;
import com.autodroid.proxy.auth.viewmodel.AuthViewModel;
import com.autodroid.proxy.auth.viewmodel.SharedViewModelFactory;

public class LoginActivity extends AppCompatActivity {
    
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button fingerprintLoginButton;
    private TextView errorTextView;
    private TextView registerLink;
    
    private AuthViewModel authViewModel;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize UI components
        emailEditText = findViewById(R.id.login_email);
        passwordEditText = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        fingerprintLoginButton = findViewById(R.id.fingerprint_login_button);
        errorTextView = findViewById(R.id.login_error);
        registerLink = findViewById(R.id.register_link);
        
        // Initialize ViewModel
        authViewModel = new ViewModelProvider(this, new SharedViewModelFactory()).get(AuthViewModel.class);

        // Set up biometric authentication
        setupBiometricAuthentication();
        
        // Set up observers
        observeViewModel();
        
        // Set up click listeners
        setupClickListeners();
    }
    
    private void observeViewModel() {
        // Observe authentication state
        authViewModel.getIsAuthenticated().observe(this, isAuthenticated -> {
            Log.d("LoginActivity", "Authentication state changed: " + isAuthenticated);

            if (isAuthenticated != null && isAuthenticated) {
                Log.d("LoginActivity", "Login successful, navigating to MainActivity");

                // Navigate to main activity with authentication data
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                // Pass authentication data
                intent.putExtra("isAuthenticated", true);
                intent.putExtra("userId", authViewModel.getUserId().getValue());
                intent.putExtra("email", authViewModel.getEmail().getValue());
                intent.putExtra("token", authViewModel.getToken().getValue());

                Log.d("LoginActivity", "Starting MainActivity with authentication data");
                startActivity(intent);
                finish();
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
    
    private void setupBiometricAuthentication() {
        // Check if biometric authentication is available
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // Biometric authentication is available
                fingerprintLoginButton.setEnabled(true);
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                // Biometric authentication is not available
                fingerprintLoginButton.setEnabled(false);
                break;
        }
        
        // Create executor for biometric prompt
        Executor executor = ContextCompat.getMainExecutor(this);
        
        // Create biometric prompt with callbacks
        biometricPrompt = new BiometricPrompt(LoginActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // Biometric authentication succeeded, login the user
                authViewModel.loginWithBiometrics();
            }
            
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                errorTextView.setText(getString(R.string.biometric_auth_failed));
                errorTextView.setVisibility(View.VISIBLE);
            }
            
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_CANCELED) {
                    errorTextView.setText(getString(R.string.biometric_error) + errString);
                    errorTextView.setVisibility(View.VISIBLE);
                }
            }
        });
        
        // Configure prompt info
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_login_title))
                .setSubtitle(getString(R.string.biometric_login_subtitle))
                .setDescription(getString(R.string.biometric_login_description))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
    }
    
    private void setupClickListeners() {
        // Login button click listener
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            
            // Validate input
            if (validateInput(email, password)) {
                // Call login method in ViewModel
                authViewModel.login(email, password);
            }
        });
        
        // Fingerprint login button click listener
        fingerprintLoginButton.setOnClickListener(v -> {
            // Show biometric prompt
            biometricPrompt.authenticate(promptInfo);
        });
        
        // Register link click listener
        registerLink.setOnClickListener(v -> {
            // Navigate to register activity
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
    
    private boolean validateInput(String email, String password) {
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
        
        return isValid;
    }
}
