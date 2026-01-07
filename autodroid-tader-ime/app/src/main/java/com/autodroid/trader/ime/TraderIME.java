package com.autodroid.trader.ime;

import android.annotation.SuppressLint;
import android.inputmethodservice.InputMethodService;

public class TraderIME extends InputMethodService {
    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public boolean onEvaluateInputViewShown() {
        return false;
    }
}
