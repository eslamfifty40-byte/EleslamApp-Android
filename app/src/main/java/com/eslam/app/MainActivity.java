package com.eslam.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {

    static final String APP_HOST = "appassets.androidplatform.net";
    static final String START_URL = "https://" + APP_HOST + "/assets/index.html";
    static final int REQ_LOCATION = 1001;
    static final int REQ_NOTIFY = 1002;

    private WebView webView;
    private ImageView splash;
    private AndroidBridge bridge;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // شاشة البداية: صورة شاشة الإهداء تظهر لحد ما الصفحة تتحمل بالكامل
        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        splash = new ImageView(this);
        splash.setImageResource(R.drawable.welcome_dedication);
        splash.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(splash, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        spinner = new android.widget.ProgressBar(this);
        FrameLayout.LayoutParams spLp = new FrameLayout.LayoutParams(120, 120, android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.BOTTOM);
        spLp.bottomMargin = 220;
        root.addView(spinner, spLp);
        setContentView(root);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);     // localStorage: الحفظ يفضل بعد قفل التطبيق
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setGeolocationEnabled(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);   // الموقع من الكود الأصلي مش من المتصفح
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (APP_HOST.equals(uri.getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                hideSplash();
            }
        });

        bridge = new AndroidBridge(this, webView);
        webView.addJavascriptInterface(bridge, "AndroidNative");

        // لو الصفحة اتأخرت، نخفي شاشة البداية بعد 6 ثواني على أي حال
        handler.postDelayed(this::hideSplash, 6000);

        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, android.webkit.ValueCallback<android.net.Uri[]> callback,
                                             android.webkit.WebChromeClient.FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.loadUrl(START_URL);

        // أول مرة بس بعد التثبيت: نطلب إذن الموقع والإشعارات من أندرويد نفسه
        android.content.SharedPreferences prefs = getSharedPreferences("eslam_setup", MODE_PRIVATE);
        if (!prefs.getBoolean("asked_perms", false)) {
            prefs.edit().putBoolean("asked_perms", true).apply();
            askFirstLaunchPermissions();
        }
    }

    // يطلب الإذن من نافذة أندرويد الرسمية (مش من Google ولا Firebase ولا المتصفح)
    private void askFirstLaunchPermissions() {
        java.util.ArrayList<String> needed = new java.util.ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            needed.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(android.Manifest.permission.POST_NOTIFICATIONS);
        }
        String[] media = Build.VERSION.SDK_INT >= 33
                ? new String[]{android.Manifest.permission.READ_MEDIA_AUDIO, android.Manifest.permission.READ_MEDIA_IMAGES}
                : new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE};
        for (String mp : media) {
            if (ContextCompat.checkSelfPermission(this, mp) != PackageManager.PERMISSION_GRANTED) needed.add(mp);
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), REQ_LOCATION);
        }
    }

    private android.webkit.ValueCallback<android.net.Uri[]> fileCallback;
    static final int REQ_FILE = 2001;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && fileCallback != null) {
            android.net.Uri[] out = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                out = new android.net.Uri[]{data.getData()};
            }
            fileCallback.onReceiveValue(out);
            fileCallback = null;
        }
    }

    private android.widget.ProgressBar spinner;
    private final long splashStart = System.currentTimeMillis();

    private void hideSplash() {
        if (splash == null || splash.getVisibility() != View.VISIBLE) return;
        long wait = 3000 - (System.currentTimeMillis() - splashStart);
        if (wait > 0) { handler.postDelayed(this::hideSplash, wait); return; }
        if (spinner != null) spinner.setVisibility(View.GONE);
        splash.animate().alpha(0f).setDuration(300).withEndAction(() -> splash.setVisibility(View.GONE)).start();
    }

    // زر الرجوع: الصفحة تقفل اللي فوق الأول، لو مش هتعمل حاجة الأندرويد يرجع في التاريخ، وفي الآخر يخرج
    @Override
    public void onBackPressed() {
        webView.evaluateJavascript(
                "(function(){try{return window.islamHandleBack ? window.islamHandleBack() : false;}catch(e){return false;}})()",
                value -> {
                    if ("true".equals(value)) return;
                    if (webView.canGoBack()) webView.goBack();
                    else finish();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_LOCATION && bridge != null && results.length > 0) {
            bridge.onPermissionResult(results);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        bridge.stopCompass();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
