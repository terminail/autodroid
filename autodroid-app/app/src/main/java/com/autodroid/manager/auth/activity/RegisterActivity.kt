package com.autodroid.manager.auth.activity

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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.autodroid.manager.MainActivity
import com.autodroid.manager.R
import com.autodroid.manager.auth.viewmodel.AuthViewModel
import com.autodroid.manager.auth.viewmodel.SharedViewModelFactory

class RegisterActivity : AppCompatActivity() {
    private var emailEditText: EditText? = null
    private var passwordEditText: EditText? = null
    private var confirmPasswordEditText: EditText? = null
    private var registerButton: Button? = null
    private var errorTextView: TextView? = null
    private var loginLink: TextView? = null

    private var authViewModel: AuthViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)


        // Initialize UI components
        emailEditText = findViewById<EditText>(R.id.register_email)
        passwordEditText = findViewById<EditText>(R.id.register_password)
        confirmPasswordEditText = findViewById<EditText>(R.id.register_confirm_password)
        registerButton = findViewById<Button>(R.id.register_button)
        errorTextView = findViewById<TextView>(R.id.register_error)
        loginLink = findViewById<TextView>(R.id.login_link)


        // Initialize ViewModel
        authViewModel = ViewModelProvider(this, SharedViewModelFactory()).get<AuthViewModel>(AuthViewModel::class.java)


        // Set up observers
        observeViewModel()


        // Set up click listeners
        setupClickListeners()
    }

    private fun observeViewModel() {
        // Observe authentication state
        authViewModel!!.isAuthenticated.observe(this, Observer { isAuthenticated: Boolean? ->
            if (isAuthenticated == true) {
                // Navigate to main activity on successful registration
                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        })


        // Observe loading state
        authViewModel!!.isLoading.observe(this, Observer { isLoading: Boolean? ->
            registerButton!!.setEnabled(!isLoading!!)
            if (isLoading) {
                registerButton!!.setText(getString(R.string.register_button_loading))
            } else {
                registerButton!!.setText(getString(R.string.register_button))
            }
        })


        // Observe error messages
        authViewModel!!.errorMessage.observe(this, Observer { errorMessage: String? ->
            if (errorMessage != null && !errorMessage.isEmpty()) {
                errorTextView!!.setText(errorMessage)
                errorTextView!!.setVisibility(View.VISIBLE)
            } else {
                errorTextView!!.setVisibility(View.GONE)
            }
        })
    }

    private fun setupClickListeners() {
        // Register button click listener
        registerButton!!.setOnClickListener(View.OnClickListener { v: View? ->
            val email = emailEditText!!.getText().toString().trim { it <= ' ' }
            val password = passwordEditText!!.getText().toString().trim { it <= ' ' }
            val confirmPassword = confirmPasswordEditText!!.getText().toString().trim { it <= ' ' }


            // Validate input
            if (validateInput(email, password, confirmPassword)) {
                // Call register method in ViewModel
                authViewModel!!.register(email, password, confirmPassword)
            }
        })


        // Login link click listener
        loginLink!!.setOnClickListener(View.OnClickListener { v: View? ->
            // Navigate back to login activity
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        })
    }

    private fun validateInput(email: String?, password: String, confirmPassword: String?): Boolean {
        var isValid = true


        // Clear previous error
        errorTextView!!.setVisibility(View.GONE)


        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailEditText!!.setError(getString(R.string.email_required))
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText!!.setError(getString(R.string.email_invalid))
            isValid = false
        }


        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordEditText!!.setError(getString(R.string.password_required))
            isValid = false
        } else if (password.length < 6) {
            passwordEditText!!.setError(getString(R.string.password_min_length))
            isValid = false
        }


        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText!!.setError(getString(R.string.confirm_password))
            isValid = false
        } else if (password != confirmPassword) {
            confirmPasswordEditText!!.setError(getString(R.string.passwords_mismatch))
            isValid = false
        }

        return isValid
    }
}
