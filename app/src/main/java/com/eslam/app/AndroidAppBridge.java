package com.eslam.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.webkit.JavascriptInterface;

public class AndroidAppBridge {
    private final Context ctx;

    public AndroidAppBridge(Context c) {
        this.ctx = c;
    }

    @JavascriptInterface
    public boolean hasLocationPermission() {
        return ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @JavascriptInterface
    public void startCompass(double lat, double lon, double altitude) {
        // placeholder: the page handles the heading via device orientation
    }

    @JavascriptInterface
    public String getLastLocation() {
        if (!hasLocationPermission()) return null;
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        Location loc = null;
        try { loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); } catch (SecurityException e) {}
        if (loc == null) {
            try { loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); } catch (SecurityException e) {}
        }
        if (loc == null) return null;
        return loc.getLatitude() + "," + loc.getLongitude();
    }
}
