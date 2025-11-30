// SharedViewModelFactory.java
package com.autodroid.proxy.auth.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

public class SharedViewModelFactory implements ViewModelProvider.Factory {
    private static AuthViewModel sharedViewModel;

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AuthViewModel.class)) {
            if (sharedViewModel == null) {
                sharedViewModel = new AuthViewModel();
            }
            return (T) sharedViewModel;
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}