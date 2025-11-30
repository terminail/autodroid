package com.autodroid.proxy.auth.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AuthViewModel extends ViewModel {
    // Authentication state
    private final MutableLiveData<Boolean> isAuthenticated = new MutableLiveData<>(false);
    private final MutableLiveData<String> userId = new MutableLiveData<>();
    private final MutableLiveData<String> email = new MutableLiveData<>();
    private final MutableLiveData<String> token = new MutableLiveData<>();
    
    // UI state
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // Getters
    public MutableLiveData<Boolean> getIsAuthenticated() {
        return isAuthenticated;
    }
    
    public MutableLiveData<String> getUserId() {
        return userId;
    }
    
    public MutableLiveData<String> getEmail() {
        return email;
    }
    
    public MutableLiveData<String> getToken() {
        return token;
    }
    
    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    // Setters
    public void setIsAuthenticated(boolean authenticated) {
        isAuthenticated.postValue(authenticated);
    }
    
    public void setUserId(String id) {
        userId.postValue(id);
    }
    
    public void setEmail(String userEmail) {
        email.postValue(userEmail);
    }
    
    public void setToken(String authToken) {
        token.postValue(authToken);
    }
    
    public void setIsLoading(boolean loading) {
        isLoading.postValue(loading);
    }
    
    public void setErrorMessage(String message) {
        errorMessage.postValue(message);
    }
    
    // Authentication methods
    public void login(String email, String password) {
        // In a real app, this would call the AuthService to authenticate with the server
        // For now, we'll just simulate a successful login
        setIsLoading(true);
        setErrorMessage(null);
        
        // Simulate network delay
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                // Simulate successful login
                setIsAuthenticated(true);
                // Small delay to ensure the state is processed
                Thread.sleep(100);

                setUserId("user_123");
                setEmail(email);
                setToken("fake_jwt_token_123");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                setIsLoading(false);
            }
        }).start();
    }
    
    public void register(String email, String password, String confirmPassword) {
        // In a real app, this would call the AuthService to register with the server
        // For now, we'll just simulate a successful registration
        setIsLoading(true);
        setErrorMessage(null);
        
        // Validate input
        if (password == null || password.isEmpty() || !password.equals(confirmPassword)) {
            setErrorMessage("Passwords do not match");
            setIsLoading(false);
            return;
        }
        
        // Simulate network delay
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                // Simulate successful registration
                setIsAuthenticated(true);
                setUserId("user_123");
                setEmail(email);
                setToken("fake_jwt_token_123");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                setIsLoading(false);
            }
        }).start();
    }
    
    public void logout() {
        // Clear authentication state
        setIsAuthenticated(false);
        setUserId(null);
        setEmail(null);
        setToken(null);
        setErrorMessage(null);
    }
    
    public boolean isLoggedIn() {
        return isAuthenticated.getValue() != null && isAuthenticated.getValue();
    }
    
    public boolean isAuthenticated() {
        return isAuthenticated.getValue() != null && isAuthenticated.getValue();
    }
    
    public void loginWithBiometrics() {
        // In a real app, this would verify the biometric authentication with the server
        // For now, we'll just simulate a successful biometric login
        setIsLoading(true);
        setErrorMessage(null);
        
        // Simulate network delay
        new Thread(() -> {
            try {
                Thread.sleep(500);
                // Simulate successful biometric login
                setIsAuthenticated(true);
                setUserId("user_123");
                setEmail("user@example.com");
                setToken("fake_jwt_token_123");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                setIsLoading(false);
            }
        }).start();
    }
}
