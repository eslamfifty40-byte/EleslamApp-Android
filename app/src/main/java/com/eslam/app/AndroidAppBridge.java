package com.eslam.app;

import android.webkit.JavascriptInterface;

public class AndroidAppBridge {
    @JavascriptInterface
    public void startCompass(double lat, double lon, double altitude) {
        // placeholder: the page already handles the heading via device orientation
    }
}
