package com.autodroid.trader.auth.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.Observer
import com.autodroid.trader.MainActivity
import com.autodroid.trader.R
import com.autodroid.trader.ui.BaseActivity

class RegisterActivity : BaseActivity() {
    private var emailEditText: EditText? = null
    private var passwordEditText: EditText? = null
    private var confirmPasswordEditText: EditText? = null
    private var registerButton: Button? = null
    private var loginLink: TextView? = null

    // Remove AuthViewModel as it's now part of AppViewModel
    // private var authViewModel: AuthViewModel? = null

    override fun getLayoutId(): Int = R.layout.activity_register

    override fun initViews() {
        // Initialize UI components
        emailEditText = findViewById<EditText>(R.id.register_email)
        passwordEditText = findViewById<EditText>(R.id.register_password)
        confirmPasswordEditText = findViewById<EditText>(R.id.register_confirm_password)
        registerButton = findViewById<Button>(R.id.register_button)
        errorTextView = findViewById<TextView>(R.id.register_error)
        loginLink = findViewById<TextView>(R.id.login_link)

        // Set error text view for base class
        errorTextView = findViewById<TextView>(R.id.register_error)
    }

    override fun setupObservers() {
        observeViewModel()
        observeErrorMessages()
    }

    override fun setupClickListeners() {
        setupRegisterClickListeners()
    }
    
    protected fun setupCommonObservers() {
        observeErrorMessages()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Observe authentication state
        observeViewModel()
    }
    
    override fun initializeViewModel() {
        // Let BaseActivity handle the global appViewModel initialization
        // This ensures all activities have access to the global AppViewModel
        super.initializeViewModel()
    }

    private fun observeViewModel() {
        // Observe authentication state from AppViewModel
        appViewModel.user.observe(this, Observer { user ->
            if (user?.isAuthenticated == true) {
                // Navigate to main activity on successful registration
                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        })
    }

    private fun setupRegisterClickListeners() {
        // Register button click listener
        registerButton!!.setOnClickListener(View.OnClickListener { v: View? ->
            val email = emailEditText!!.getText().toString().trim { it <= ' ' }
            val password = passwordEditText!!.getText().toString().trim { it <= ' ' }
            val confirmPassword = confirmPasswordEditText!!.getText().toString().trim { it <= ' ' }


            // Validate input
            if (validateInput(email, password, confirmPassword)) {
                // Call register method in AppViewModel
                appViewModel!!.register(email, password, confirmPassword)
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
        errorTextView.setVisibility(View.GONE)


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

