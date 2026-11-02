package com.eslam.app;

import android.webkit.JavascriptInterface;

public class AndroidAppBridge {
    private final android.content.Context ctx;
    public AndroidAppBridge(android.content.Context c) { this.ctx = c; }
    @android.webkit.JavascriptInterface
    public boolean hasLocationPermission() {
        return ctx.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            || ctx.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
    @JavascriptInterface
    public void startCompass(double lat, double lon, double altitude) {
        // placeholder: the page already handles the heading via device orientation
    }
}
