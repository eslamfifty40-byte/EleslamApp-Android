package com.eslam.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

public class AndroidBridge implements SensorEventListener {

    static final String CHANNEL_ID = "eslam_prayer_channel";

    private final Activity activity;
    private final WebView webView;
    private final SensorManager sensorManager;
    private final Sensor rotationSensor;
    private final float[] rotMatrix = new float[9];
    private final float[] orientation = new float[3];
    private boolean compassOn = false;
    private float lastHeading = -1f;
    private float filteredHeading = Float.NaN;

    private float declination = 0f;

    public AndroidBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        this.sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        this.rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        createChannel();
    }

    // ---------- البوصلة (حساس الجهاز مباشرة، بدون أي إذن من المتصفح) ----------

    @JavascriptInterface
    public boolean hasCompass() { return rotationSensor != null; }

    @JavascriptInterface
    public void startCompass() {
        if (rotationSensor == null || compassOn) return;
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        compassOn = true;
    }

    public void stopCompass() {
        if (!compassOn) return;
        sensorManager.unregisterListener(this);
        compassOn = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values);
        SensorManager.getOrientation(rotMatrix, orientation);
        float raw = (float) Math.toDegrees(orientation[0]);
        raw = (raw + declination + 360f) % 360f;
        if (Float.isNaN(filteredHeading)) {
            filteredHeading = raw;
        } else {
            float delta = ((raw - filteredHeading + 540f) % 360f) - 180f;
            filteredHeading = (filteredHeading + delta * 0.08f + 360f) % 360f;
        }
        if (lastHeading >= 0) {
            float change = Math.abs(((filteredHeading - lastHeading + 540f) % 360f) - 180f);
            if (change < 1.0f) return;
        }
        lastHeading = filteredHeading;
        final String js = "window.onNativeCompass && window.onNativeCompass(" + filteredHeading + ")";
        activity.runOnUiThread(() -> webView.evaluateJavascript(js, null));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    // ---------- الموقع (من GPS/Network الجهاز مباشرة) ----------

    @JavascriptInterface
    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // يرجع "lat,lng" أو "" لو مفيش موقع محفوظ
    @JavascriptInterface
    public String getLastLocation() {
        if (!hasLocationPermission()) return "";
        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        for (String p : lm.getProviders(true)) {
            Location l = lm.getLastKnownLocation(p);
            if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
        }
        return best == null ? "" : best.getLatitude() + "," + best.getLongitude();
    }

    // يطلب الإذن أول مرة، وبعدين يجيب موقع جديد ويبعته للصفحة
    @JavascriptInterface
    public void requestLocation() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    MainActivity.REQ_LOCATION);
            return;
        }
        fetchFreshLocation();
    }

    void onPermissionResult(int[] results) {
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            fetchFreshLocation();
        } else {
            sendJs("window.onNativeLocationError && window.onNativeLocationError(1)");
        }
    }

    @SuppressWarnings("MissingPermission")
    private void fetchFreshLocation() {
        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        String provider = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
        LocationListener once = new LocationListener() {
            @Override public void onLocationChanged(Location loc) {
                declination = new android.hardware.GeomagneticField((float) loc.getLatitude(), (float) loc.getLongitude(), 0f, System.currentTimeMillis()).getDeclination();
                sendJs("window.onNativeLocation && window.onNativeLocation(" + loc.getLatitude() + "," + loc.getLongitude() + ")");
                lm.removeUpdates(this);
            }
            @Override public void onProviderDisabled(String p) { }
            @Override public void onProviderEnabled(String p) { }
        };
        try {
            lm.requestSingleUpdate(provider, once, Looper.getMainLooper());
        } catch (Exception e) {
            sendJs("window.onNativeLocationError && window.onNativeLocationError(2)");
        }
    }

    // ---------- الإشعارات وتنبيهات الأذان (AlarmManager، بدون Firebase) ----------

    @JavascriptInterface
    public void notify(String title, String body) {
        showNotification(activity, (int) (System.currentTimeMillis() % 100000), title, body);
    }

    // يجدول تنبيه يومي للصلاة في الساعة والدقيقة المطلوبين
    @JavascriptInterface
    public void schedulePrayer(int id, int hour, int minute, String title, String body) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_MONTH, 1);

        Intent i = new Intent(activity, PrayerReceiver.class)
                .putExtra("id", id).putExtra("title", title).putExtra("body", body)
                .putExtra("hour", hour).putExtra("minute", minute);
        PendingIntent pi = PendingIntent.getBroadcast(activity, id, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        long t = c.getTimeInMillis();
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setWindow(AlarmManager.RTC_WAKEUP, t, 5 * 60 * 1000L, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, t, pi);
        }
    }

    public static void showNotification(Context ctx, int id, String title, String body) {
        createChannel(ctx);
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, b.build());
    }

    private void createChannel() { createChannel(activity); }

    static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "مواقيت الصلاة", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("تنبيهات الأذان والأذكار");
        nm.createNotificationChannel(ch);
    }

    // ---------- زر التثبيت: إضافة أيقونة التطبيق للشاشة الرئيسية ----------

    @JavascriptInterface
    public boolean addHomeShortcut() { if (true) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false;
        ShortcutManager sm = activity.getSystemService(ShortcutManager.class);
        if (sm == null || !sm.isRequestPinShortcutSupported()) return false;
        Intent i = new Intent(activity, MainActivity.class).setAction(Intent.ACTION_MAIN);
        ShortcutInfo info = new ShortcutInfo.Builder(activity, "eslam_home")
                .setShortLabel("الإسلام")
                .setLongLabel("تطبيق الإسلام")
                .setIcon(Icon.createWithResource(activity, R.mipmap.ic_launcher))
                .setIntent(i)
                .build();
        return sm.requestPinShortcut(info, null);
    }

    @JavascriptInterface
    public String appVersion() { return "native-1"; }

    private void sendJs(String js) {
        activity.runOnUiThread(() -> webView.evaluateJavascript(js, null));
    }

    // ---------- إعدادات الموقع: لو الـ GPS مقفول يفتح شاشة تفعيله من الأندرويد ----------

    @JavascriptInterface
    public boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @JavascriptInterface
    public void openLocationSettings() {
        activity.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    @JavascriptInterface
    public boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return true;
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private android.speech.tts.TextToSpeech tts;
    private boolean ttsReady = false;
    private String pendingSpeak = null;

    private void ensureTts() {
        if (tts != null) return;
        tts = new android.speech.tts.TextToSpeech(activity, status -> {
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(new java.util.Locale("ar"));
                if (r == android.speech.tts.TextToSpeech.LANG_MISSING_DATA
                        || r == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(java.util.Locale.getDefault());
                }
                ttsReady = true;
                if (pendingSpeak != null) {
                    tts.speak(pendingSpeak, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "p");
                    pendingSpeak = null;
                }
            }
        });
    }

    @JavascriptInterface
    public void speak(String text) {
        ensureTts();
        if (ttsReady) tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "q");
        else pendingSpeak = text;
    }

    @JavascriptInterface
    public void stopSpeak() { if (tts != null) tts.stop(); }
}
