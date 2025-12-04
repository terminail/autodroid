package com.autodroid.manager.auth.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.autodroid.manager.MainActivity
import com.autodroid.manager.R
import com.autodroid.manager.auth.viewmodel.AuthViewModel
import com.autodroid.manager.auth.viewmodel.SharedViewModelFactory

class LoginActivity : AppCompatActivity() {
    private var emailEditText: EditText? = null
    private var passwordEditText: EditText? = null
    private var loginButton: Button? = null
    private var fingerprintLoginButton: Button? = null
    private var errorTextView: TextView? = null
    private var registerLink: TextView? = null

    private var authViewModel: AuthViewModel? = null
    private var biometricPrompt: BiometricPrompt? = null
    private var promptInfo: PromptInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide the action bar
        supportActionBar?.hide()
        
        setContentView(R.layout.activity_login)


        // Initialize UI components
        emailEditText = findViewById<EditText>(R.id.login_email)
        passwordEditText = findViewById<EditText>(R.id.login_password)
        loginButton = findViewById<Button>(R.id.login_button)
        fingerprintLoginButton = findViewById<Button>(R.id.fingerprint_login_button)
        errorTextView = findViewById<TextView>(R.id.login_error)
        registerLink = findViewById<TextView>(R.id.register_link)

        // Set focus on the Login button instead of the email field
        loginButton?.requestFocus()


        // Initialize ViewModel
        authViewModel = ViewModelProvider(
            this,
            SharedViewModelFactory()
        ).get<AuthViewModel>(AuthViewModel::class.java)

        // Set up biometric authentication
        setupBiometricAuthentication()


        // Set up observers
        observeViewModel()


        // Set up click listeners
        setupClickListeners()
    }

    private fun observeViewModel() {
        // Observe authentication state
        authViewModel!!.isAuthenticated.observe(this, Observer { isAuthenticated: Boolean? ->
            Log.d("LoginActivity", "Authentication state changed: " + isAuthenticated)
            if (isAuthenticated != null && isAuthenticated) {
                Log.d("LoginActivity", "Login successful, navigating to MainActivity")

                // Navigate to main activity with authentication data
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

                // Pass authentication data
                intent.putExtra("isAuthenticated", true)
                intent.putExtra("userId", authViewModel!!.userId.getValue())
                intent.putExtra("email", authViewModel!!.email.getValue())
                intent.putExtra("token", authViewModel!!.token.getValue())

                Log.d("LoginActivity", "Starting MainActivity with authentication data")
                startActivity(intent)
                finish()
            }
        })


        // Observe error messages
        authViewModel!!.errorMessage.observe(this, Observer { errorMessage: String? ->
            if (!errorMessage.isNullOrEmpty()) {
                errorTextView!!.text = errorMessage
                errorTextView!!.visibility = View.VISIBLE
            } else {
                errorTextView!!.visibility = View.GONE
            }
        })
    }

    private fun setupBiometricAuthentication() {
        // Check if biometric authentication is available
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Biometric authentication is available
                fingerprintLoginButton!!.setEnabled(true)
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE, BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE, BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Biometric authentication is not available
                fingerprintLoginButton!!.setEnabled(false)
            }

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                TODO()
            }

            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                TODO()
            }

            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                TODO()
            }
        }


        // Create executor for biometric prompt
        val executor = ContextCompat.getMainExecutor(this)


        // Create biometric prompt with callbacks
        biometricPrompt = BiometricPrompt(
            this@LoginActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Biometric authentication succeeded, login the user
                    authViewModel!!.loginWithBiometrics()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    errorTextView!!.setText(getString(R.string.biometric_auth_failed))
                    errorTextView!!.setVisibility(View.VISIBLE)
                }

                @SuppressLint("SetTextI18n")
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_CANCELED) {
                        errorTextView!!.text = getString(R.string.biometric_error) + errString
                        errorTextView!!.visibility = View.VISIBLE
                    }
                }
            })


        // Configure prompt info
        promptInfo = PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_login_title))
            .setSubtitle(getString(R.string.biometric_login_subtitle))
            .setDescription(getString(R.string.biometric_login_description))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
    }

    private fun setupClickListeners() {
        // Login button click listener
        loginButton!!.setOnClickListener { v: View? ->
            val email = emailEditText!!.getText().toString().trim { it <= ' ' }
            val password = passwordEditText!!.getText().toString().trim { it <= ' ' }


            // Validate input
            if (validateInput(email, password)) {
                // Call login method in ViewModel
                authViewModel!!.login(email, password)
            }
        }


        // Fingerprint login button click listener
        fingerprintLoginButton!!.setOnClickListener { v: View? ->
            // Show biometric prompt
            biometricPrompt!!.authenticate(promptInfo!!)
        }


        // Register link click listener
        registerLink!!.setOnClickListener(View.OnClickListener { v: View? ->
            // Navigate to register activity
            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivity(intent)
        })
    }

    private fun validateInput(email: String?, password: String?): Boolean {
        var isValid = true


        // Clear previous error
        errorTextView!!.visibility = View.GONE


        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailEditText!!.error = getString(R.string.email_required)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText!!.error = getString(R.string.email_invalid)
            isValid = false
        }


        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordEditText!!.error = getString(R.string.password_required)
            isValid = false
        } else if (password!!.length < 6) {
            passwordEditText!!.error = getString(R.string.password_min_length)
            isValid = false
        }

        return isValid
    }
}

