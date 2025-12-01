package com.autodroid.proxy.auth.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AuthViewModel : ViewModel() {
    // Getters
    // Authentication state
    val isAuthenticated: MutableLiveData<Boolean?> = MutableLiveData<Boolean?>(false)
    val userId: MutableLiveData<String?> = MutableLiveData<String?>()
    val email: MutableLiveData<String?> = MutableLiveData<String?>()
    val token: MutableLiveData<String?> = MutableLiveData<String?>()

    // UI state
    val isLoading: MutableLiveData<Boolean?> = MutableLiveData<Boolean?>(false)
    val errorMessage: MutableLiveData<String?> = MutableLiveData<String?>()

    // Setters
    fun setIsAuthenticated(authenticated: Boolean) {
        isAuthenticated.postValue(authenticated)
    }

    fun setUserId(id: String?) {
        userId.postValue(id)
    }

    fun setEmail(userEmail: String?) {
        email.postValue(userEmail)
    }

    fun setToken(authToken: String?) {
        token.postValue(authToken)
    }

    fun setIsLoading(loading: Boolean) {
        isLoading.postValue(loading)
    }

    fun setErrorMessage(message: String?) {
        errorMessage.postValue(message)
    }

    // Authentication methods
    fun login(email: String?, password: String?) {
        // In a real app, this would call the AuthService to authenticate with the server
        // For now, we'll just simulate a successful login
        setIsLoading(true)
        setErrorMessage(null)


        // Simulate network delay
        Thread(Runnable {
            try {
                Thread.sleep(1000)
                // Simulate successful login
                setIsAuthenticated(true)
                // Small delay to ensure the state is processed
                Thread.sleep(100)

                setUserId("user_123")
                setEmail(email)
                setToken("fake_jwt_token_123")
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                setIsLoading(false)
            }
        }).start()
    }

    fun register(email: String?, password: String?, confirmPassword: String?) {
        // In a real app, this would call the AuthService to register with the server
        // For now, we'll just simulate a successful registration
        setIsLoading(true)
        setErrorMessage(null)


        // Validate input
        if (password == null || password.isEmpty() || (password != confirmPassword)) {
            setErrorMessage("Passwords do not match")
            setIsLoading(false)
            return
        }


        // Simulate network delay
        Thread(Runnable {
            try {
                Thread.sleep(1500)
                // Simulate successful registration
                setIsAuthenticated(true)
                setUserId("user_123")
                setEmail(email)
                setToken("fake_jwt_token_123")
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                setIsLoading(false)
            }
        }).start()
    }

    fun logout() {
        // Clear authentication state
        setIsAuthenticated(false)
        setUserId(null)
        setEmail(null)
        setToken(null)
        setErrorMessage(null)
    }

    val isLoggedIn: Boolean
        get() = isAuthenticated.getValue() != null && isAuthenticated.getValue() == true

    fun isAuthenticated(): Boolean {
        return isAuthenticated.getValue() != null && isAuthenticated.getValue() == true
    }

    fun loginWithBiometrics() {
        // In a real app, this would verify the biometric authentication with the server
        // For now, we'll just simulate a successful biometric login
        setIsLoading(true)
        setErrorMessage(null)


        // Simulate network delay
        Thread(Runnable {
            try {
                Thread.sleep(500)
                // Simulate successful biometric login
                setIsAuthenticated(true)
                setUserId("user_123")
                setEmail("user@example.com")
                setToken("fake_jwt_token_123")
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                setIsLoading(false)
            }
        }).start()
    }
}
