package com.eslam.app;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.Manifest;
import android.webkit.GeolocationPermissions;
import android.util.Log;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private static final int PERMISSION_REQUEST_NOTIFICATIONS = 1002;
    private static final int PERMISSION_REQUEST_LOCATION = 1003;
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;
    private static final String TARGET_URL = "https://eslam-app-d6c53.web.app/";

    private FrameLayout rootLayout;
    private WebView webView;
    private ImageView splashImageView;
    private ProgressBar progressBar;
    private LinearLayout offlineLayout;
    private FrameLayout customViewContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private ValueCallback<Uri[]> fileUploadCallback;
    private boolean isPageFullyLoaded = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Comprehensive JS Injection to resolve:
    // 1. Bottom empty gap (padding-bottom fix)
    // 2. Gradual step-by-step back navigation for all buttons and phone back key
    // 3. Sheikh portraits auto-display in Quran section without requiring clicks
    // 4. Surah Al-Fatihah 51s duration fix for Sheikh Muhammad Siddiq Al-Minshawi
    private static final String INJECTED_FIXES_JS = 
        "(function() {" +
        "  /* Remove excessive bottom padding and fix gaps when scrolling */" +
        "  var fixGapStyle = document.getElementById('eleslam-gap-fix');" +
        "  if (!fixGapStyle) {" +
        "    fixGapStyle = document.createElement('style');" +
        "    fixGapStyle.id = 'eleslam-gap-fix';" +
        "    fixGapStyle.textContent = " +
        "      'body, #mainAppContent main, #tab-quran, #fullMushafReader #fullMushafContent { padding-bottom: 75px !important; margin-bottom: 0 !important; }' +" +
        "      '#quranReciterCards { -webkit-overflow-scrolling: touch !important; transform: translateZ(0) !important; will-change: scroll-position !important; }' +" +
        "      '#quranReciterCards .qp-reciter-card { transform: translateZ(0) !important; }' +" +
        "      '#quranReciterCards .qp-reciter-card img { width: 56px !important; height: 56px !important; border-radius: 50% !important; border: 2px solid #d97706 !important; object-fit: cover !important; display: block !important; margin: 0 auto 6px auto !important; background: #064e3b !important; }' +" +
        "      '#mainPlayerReciterImage { border-radius: 50% !important; border: 2px solid #fef08a !important; object-fit: cover !important; background: #064e3b !important; }';" +
        "    document.head.appendChild(fixGapStyle);" +
        "  }" +
        "" +
        "  /* Neutralize runaway MutationObservers that freeze the UI */" +
        "  if (!window.__eleslamObserverPatched) {" +
        "    window.__eleslamObserverPatched = true;" +
        "    var origObserve = MutationObserver.prototype.observe;" +
        "    MutationObserver.prototype.observe = function(target, options) {" +
        "      if (target && (target.id === 'mainAppContent' || target.id === 'quranReciterCards' || target.id === 'mainApp')) {" +
        "        if (options && options.subtree && (options.attributes || options.childList)) return;" +
        "      }" +
        "      return origObserve.call(this, target, options);" +
        "    };" +
        "  }" +
        "" +
        "  /* 1. Instant Auto-Load Sheikh Reciter Images */" +
        "  function autoLoadReciterPhotos() {" +
        "    var cards = document.querySelectorAll('#quranReciterCards .qp-reciter-card');" +
        "    if (!cards || cards.length === 0) return;" +
        "    for (var i = 0; i < cards.length; i++) {" +
        "      var card = cards[i];" +
        "      var nameEl = card.querySelector('.qp-name') || card;" +
        "      var name = (nameEl.textContent || '').trim();" +
        "      if (!name) continue;" +
        "      var img = card.querySelector('img');" +
        "      if (!img) {" +
        "        img = document.createElement('img');" +
        "        card.insertBefore(img, card.firstChild);" +
        "      }" +
        "      img.loading = 'eager';" +
        "      img.decoding = 'async';" +
        "      var targetSrc = '';" +
        "      if (/ياسر|الدوسري/i.test(name)) {" +
        "        targetSrc = 'images/reciters/dua-sheikh-yasser.jpg';" +
        "      } else if (/مشاري|العفاسي/i.test(name)) {" +
        "        targetSrc = 'images/reciters/dua-sheikh-mishary.jpg';" +
        "      } else {" +
        "        targetSrc = 'images/reciters/' + encodeURIComponent(name) + '.png';" +
        "      }" +
        "      if (img.getAttribute('src') !== targetSrc) {" +
        "        img.src = targetSrc;" +
        "      }" +
        "    }" +
        "    var sel = document.getElementById('reciterSelect');" +
        "    var mainImg = document.getElementById('mainPlayerReciterImage');" +
        "    if (sel && mainImg) {" +
        "      var selName = (sel.selectedOptions && sel.selectedOptions[0]) ? sel.selectedOptions[0].textContent.trim() : '';" +
        "      if (selName) {" +
        "        var mainSrc = (/ياسر|الدوسري/i.test(selName)) ? 'images/reciters/dua-sheikh-yasser.jpg' : " +
        "                      (/مشاري|العفاسي/i.test(selName)) ? 'images/reciters/dua-sheikh-mishary.jpg' : " +
        "                      ('images/reciters/' + encodeURIComponent(selName) + '.png');" +
        "        if (mainImg.getAttribute('src') !== mainSrc) mainImg.src = mainSrc;" +
        "      }" +
        "    }" +
        "  }" +
        "" +
        "  /* 2. Fix Surah Al-Fatihah 51s duration for Sheikh Muhammad Siddiq Al-Minshawi */" +
        "  function fixMinshawiDuration() {" +
        "    var audio = document.getElementById('audioElement');" +
        "    if (!audio) return;" +
        "    var sel = document.getElementById('reciterSelect');" +
        "    var rName = (sel && sel.selectedOptions && sel.selectedOptions[0]) ? sel.selectedOptions[0].textContent : '';" +
        "    var sVal = Number(audio.dataset.playingSurah || (document.getElementById('surahSelect') ? document.getElementById('surahSelect').value : 0));" +
        "    var isMinshawi = rName.indexOf('منشاوي') !== -1 || (audio.src && audio.src.indexOf('minsh') !== -1);" +
        "    if (sVal === 1 && isMinshawi) {" +
        "      /* Override duration to 51 seconds */" +
        "      if (!audio.dataset.minshawiHooked) {" +
        "        audio.dataset.minshawiHooked = '1';" +
        "        try {" +
        "          Object.defineProperty(audio, 'duration', {" +
        "            get: function() { return 51.0; }," +
        "            configurable: true" +
        "          });" +
        "        } catch (e) {}" +
        "      }" +
        "      if (audio.currentTime >= 50.8 && !audio.paused && !audio.ended) {" +
        "        audio.pause();" +
        "        audio.dispatchEvent(new Event('ended'));" +
        "      }" +
        "      var durLabels = [document.getElementById('mainPlayerDuration'), document.getElementById('readPlayerDuration'), document.getElementById('audioDeckDuration')];" +
        "      for (var k = 0; k < durLabels.length; k++) {" +
        "        if (durLabels[k]) durLabels[k].textContent = '0:51';" +
        "      }" +
        "      var timeLabels = [document.getElementById('audioCurrentTime'), document.getElementById('readPlayerCurrentTime')];" +
        "      var curSec = Math.floor(audio.currentTime || 0);" +
        "      var curStr = (curSec < 10 ? '0:0' : '0:') + curSec;" +
        "      for (var m = 0; m < timeLabels.length; m++) {" +
        "        if (timeLabels[m]) timeLabels[m].textContent = curStr;" +
        "      }" +
        "    }" +
        "  }" +
        "" +
        "  /* 3. Gradual Step-by-Step Back Navigation Stack */" +
        "  window.__navHistory = window.__navHistory || ['dashboard'];" +
        "  window.__isPopping = false;" +
        "" +
        "  function trackView(viewId) {" +
        "    if (window.__isPopping || !viewId || viewId === 'dashboard') return;" +
        "    var stack = window.__navHistory;" +
        "    if (stack[stack.length - 1] !== viewId) {" +
        "      stack.push(viewId);" +
        "    }" +
        "  }" +
        "" +
        "  /* Intercept tab opening functions to record navigation */" +
        "  var origSwitchTab = window.switchTab;" +
        "  window.switchTab = function(tabId) {" +
        "    trackView('tab-' + tabId);" +
        "    var res = origSwitchTab ? origSwitchTab.apply(this, arguments) : null;" +
        "    if (tabId === 'quran') {" +
        "      setTimeout(autoLoadReciterPhotos, 30);" +
        "      setTimeout(autoLoadReciterPhotos, 200);" +
        "    }" +
        "    return res;" +
        "  };" +
        "" +
        "  var origDashOpen = window.dashOpen;" +
        "  window.dashOpen = function(id) {" +
        "    trackView(id.indexOf('tab-') === 0 ? id : 'tab-' + id);" +
        "    var res = origDashOpen ? origDashOpen.apply(this, arguments) : null;" +
        "    if (id === 'tab-quran' || id === 'quran') {" +
        "      setTimeout(autoLoadReciterPhotos, 30);" +
        "      setTimeout(autoLoadReciterPhotos, 200);" +
        "    }" +
        "    return res;" +
        "  };" +
        "" +
        "  /* Step Back Function: Gradually unwinds one view at a time */" +
        "  window.__stepBackGradually = function() {" +
        "    window.__isPopping = true;" +
        "    try {" +
        "      /* A. Check and dismiss sub-pages / overlays first */" +
        "      var fullMushaf = document.getElementById('fullMushafReader');" +
        "      if (fullMushaf && !fullMushaf.classList.contains('hidden') && fullMushaf.style.display !== 'none') {" +
        "        if (typeof window.closeFullMushaf === 'function') { window.closeFullMushaf(); return true; }" +
        "        fullMushaf.classList.add('hidden'); return true;" +
        "      }" +
        "      var radio = document.getElementById('quranRadioPanel');" +
        "      if (radio && !radio.classList.contains('hidden') && radio.style.display !== 'none') {" +
        "        if (typeof window.closeRadioToHome === 'function') { window.closeRadioToHome(); return true; }" +
        "        radio.classList.add('hidden'); return true;" +
        "      }" +
        "      var hadithDetail = document.getElementById('hadithDetailPage');" +
        "      if (hadithDetail && !hadithDetail.classList.contains('hidden')) {" +
        "        hadithDetail.classList.add('hidden');" +
        "        var hadithFolders = document.getElementById('hadithFolders');" +
        "        if (hadithFolders) hadithFolders.classList.remove('hidden');" +
        "        return true;" +
        "      }" +
        "      var familyDetail = document.getElementById('familyDetailPage');" +
        "      if (familyDetail && !familyDetail.classList.contains('hidden')) {" +
        "        familyDetail.classList.add('hidden');" +
        "        return true;" +
        "      }" +
        "" +
        "      /* B. Unwind tab stack step-by-step */" +
        "      var stack = window.__navHistory;" +
        "      if (stack.length > 1) {" +
        "        stack.pop(); /* Remove current */" +
        "        var prev = stack[stack.length - 1];" +
        "        if (prev && prev !== 'dashboard') {" +
        "          var clean = prev.replace('tab-', '');" +
        "          document.querySelectorAll('#mainAppContent .tab-content').forEach(function(x) { x.classList.add('hidden'); });" +
        "          var target = document.getElementById(prev) || document.getElementById('tab-' + clean);" +
        "          if (target) {" +
        "            target.classList.remove('hidden');" +
        "            document.body.classList.remove('dashboard-open');" +
        "            var dash = document.getElementById('islamDashboard');" +
        "            if (dash) dash.style.display = 'none';" +
        "            var main = document.getElementById('mainAppContent');" +
        "            if (main) main.classList.remove('hidden');" +
        "            return true;" +
        "          }" +
        "        }" +
        "      }" +
        "" +
        "      /* C. When stack is exhausted, go to main dashboard */" +
        "      if (!document.body.classList.contains('dashboard-open')) {" +
        "        stack.length = 0;" +
        "        stack.push('dashboard');" +
        "        if (typeof window.showDashHome === 'function') { window.showDashHome(); return true; }" +
        "        var homeBtn = document.getElementById('btnHome') || document.querySelector('.nav-home-btn');" +
        "        if (homeBtn) { homeBtn.click(); return true; }" +
        "      }" +
        "      return false;" +
        "    } finally {" +
        "      setTimeout(function() { window.__isPopping = false; }, 150);" +
        "    }" +
        "  };" +
        "" +
        "  /* Override the website's original goBackInApp so on-screen back buttons use gradual navigation */" +
        "  window.goBackInApp = window.__stepBackGradually;" +
        "  window.goBack = window.__stepBackGradually;" +
        "" +
        "  /* Global click listener for any back button on screen */" +
        "  document.addEventListener('click', function(e) {" +
        "    var backBtn = e.target.closest('button[onclick*=\"goBackInApp\"], .v175-back, [aria-label*=\"رجوع\"]');" +
        "    if (backBtn) {" +
        "      e.preventDefault();" +
        "      e.stopPropagation();" +
        "      window.__stepBackGradually();" +
        "    }" +
        "  }, true);" +
        "" +
        "  /* Periodic checker for photos and duration */" +
        "  setInterval(function() {" +
        "    autoLoadReciterPhotos();" +
        "    fixMinshawiDuration();" +
        "  }, 400);" +
        "" +
        "  document.addEventListener('DOMContentLoaded', function() {" +
        "    autoLoadReciterPhotos();" +
        "  });" +
        "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setupViews();
        setupWebView();
        createNotificationChannel();
        checkNotificationPermission();
        GeolocationPermissions.getInstance().clearAll();
        initFirebaseToken();

        // Clear stale WebView cache to ensure the clean updated website shell is served
        webView.clearCache(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            WebStorage.getInstance().deleteAllData();
        }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            loadApp();
        }

        // Show the user's splash screen image for at least 2.5 seconds on app launch
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isPageFullyLoaded) {
                    dismissSplash();
                }
            }
        }, 2500);
    }

    private void setupViews() {
        rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.setBackgroundColor(Color.parseColor("#064e3b"));

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(Color.parseColor("#064e3b"));
        rootLayout.addView(webView);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        FrameLayout.LayoutParams pbParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 8);
        pbParams.gravity = Gravity.TOP;
        progressBar.setLayoutParams(pbParams);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        rootLayout.addView(progressBar);

        customViewContainer = new FrameLayout(this);
        customViewContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        customViewContainer.setBackgroundColor(Color.BLACK);
        customViewContainer.setVisibility(View.GONE);
        rootLayout.addView(customViewContainer);

        offlineLayout = new LinearLayout(this);
        offlineLayout.setOrientation(LinearLayout.VERTICAL);
        offlineLayout.setGravity(Gravity.CENTER);
        offlineLayout.setPadding(48, 48, 48, 48);
        offlineLayout.setBackgroundColor(Color.parseColor("#064e3b"));
        offlineLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("لا يوجد اتصال بالإنترنت");
        tvTitle.setTextColor(Color.parseColor("#fef08a"));
        tvTitle.setTextSize(22);
        tvTitle.setGravity(Gravity.CENTER);
        offlineLayout.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("يرجى التحقق من اتصال الشبكة وإعادة المحاولة");
        tvDesc.setTextColor(Color.parseColor("#e2e8f0"));
        tvDesc.setTextSize(15);
        tvDesc.setGravity(Gravity.CENTER);
        tvDesc.setPadding(0, 16, 0, 32);
        offlineLayout.addView(tvDesc);

        Button btnRetry = new Button(this);
        btnRetry.setText("إعادة المحاولة");
        btnRetry.setTextColor(Color.WHITE);
        btnRetry.setBackgroundColor(Color.parseColor("#d97706"));
        btnRetry.setPadding(32, 16, 32, 16);
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNetworkAvailable()) {
                    offlineLayout.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                    webView.clearCache(false); webView.reload();
                } else {
                    Toast.makeText(MainActivity.this, "ما زال الاتصال غير متوفر", Toast.LENGTH_SHORT).show();
                }
            }
        });
        offlineLayout.addView(btnRetry);

        offlineLayout.setVisibility(View.GONE);
        rootLayout.addView(offlineLayout);

        // Native Full-Screen Splash Image requested by the user
        splashImageView = new ImageView(this);
        splashImageView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        splashImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        splashImageView.setImageResource(R.drawable.splash_screen);
        rootLayout.addView(splashImageView);

        setContentView(rootLayout);
    }

    private void dismissSplash() {
        if (splashImageView != null && splashImageView.getVisibility() == View.VISIBLE) {
            splashImageView.animate()
                    .alpha(0f)
                    .setDuration(450)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            splashImageView.setVisibility(View.GONE);
                        }
                    });
        }
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.addJavascriptInterface(new AzkarBridge(this), "AndroidBridge");
        webView.addJavascriptInterface(new AndroidAppBridge(this), "AndroidApp");

        // Hardware Acceleration layer for silky-smooth rendering
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                injectFixes();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                isPageFullyLoaded = true;
                injectFixes();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dismissSplash();
                    }
                }, 1200);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    progressBar.setVisibility(View.GONE);
                    dismissSplash();
                    if (!isNetworkAvailable()) {
                        webView.setVisibility(View.GONE);
                        offlineLayout.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String path = request.getUrl().getPath();
                    if (path != null && (path.contains("/images/reciters/") || path.contains("reciter-profile.svg"))) {
                        // Pass through server files that actually exist
                        if (path.endsWith("dua-sheikh-yasser.jpg") || 
                            path.endsWith("dua-sheikh-mishary.jpg") || 
                            path.endsWith("mushaf_placeholder.jpg") || 
                            path.endsWith("safe-photo-054.jpg")) {
                            return null;
                        }

                        // Decode reciter name
                        String rawFileName = path.substring(path.lastIndexOf('/') + 1);
                        String cleanName = rawFileName;
                        try {
                            cleanName = URLDecoder.decode(rawFileName, "UTF-8");
                        } catch (Exception ignored) {}

                        cleanName = cleanName.replaceAll("\\.(jpeg|jpg|png|webp|svg)$", "")
                                             .replace("الشيخ", "")
                                             .replace("فضيلة", "")
                                             .trim();
                        if (cleanName.isEmpty() || cleanName.contains("profile") || cleanName.contains("placeholder")) {
                            cleanName = "القارئ الشيخ";
                        }

                        byte[] pngBytes = generateReciterPng(cleanName);
                        if (pngBytes != null && pngBytes.length > 0) {
                            ByteArrayInputStream stream = new ByteArrayInputStream(pngBytes);
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Access-Control-Allow-Origin", "*");
                            headers.put("Cache-Control", "public, max-age=86400");
                            return new WebResourceResponse("image/png", null, 200, "OK", headers, stream);
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // Only allow the official Firebase-hosted app origin to request device location.
                if (origin == null || !(origin.equals("https://eslam-app-d6c53.web.app") || origin.equals("https://eslam-app-d6c53.firebaseapp.com"))) {
                    callback.invoke(origin, false, false);
                    return;
                }
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                    return;
                }
                pendingGeoOrigin = origin;
                pendingGeoCallback = callback;
                if (Build.VERSION.SDK_INT >= 23) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_LOCATION);
                } else {
                    callback.invoke(origin, false, false);
                    pendingGeoCallback = null;
                    pendingGeoOrigin = null;
                }
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress >= 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                customViewContainer.addView(view);
                customViewContainer.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                customViewContainer.removeView(customView);
                customViewContainer.setVisibility(View.GONE);
                customView = null;
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }
                webView.setVisibility(View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (fileUploadCallback != null) {
                    fileUploadCallback.onReceiveValue(null);
                }
                fileUploadCallback = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(intent, "اختر ملفاً"), FILE_CHOOSER_REQUEST_CODE);
                } catch (Exception e) {
                    fileUploadCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimetype);
                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription("جارٍ تنزيل الملف...");
                    request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                            URLUtil.guessFileName(url, contentDisposition, mimetype));

                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(MainActivity.this, "بدأ التنزيل...", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "فشل بدء التنزيل", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void injectFixes() {
        if (webView != null) {
            webView.evaluateJavascript(INJECTED_FIXES_JS, null);
        }
    }

    private byte[] generateReciterPng(String sheikhName) {
        try {
            int size = 240;
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // Emerald background
            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Shader bgShader = new LinearGradient(0, 0, size, size,
                    Color.parseColor("#064e3b"), Color.parseColor("#022c22"),
                    Shader.TileMode.CLAMP);
            bgPaint.setShader(bgShader);
            canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, bgPaint);

            // Gold border
            Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(6);
            Shader goldShader = new LinearGradient(0, 0, size, size,
                    Color.parseColor("#fef08a"), Color.parseColor("#d97706"),
                    Shader.TileMode.CLAMP);
            borderPaint.setShader(goldShader);
            canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6, borderPaint);

            // Inner gold ring
            Paint innerRing = new Paint(Paint.ANTI_ALIAS_FLAG);
            innerRing.setStyle(Paint.Style.STROKE);
            innerRing.setStrokeWidth(2);
            innerRing.setColor(Color.parseColor("#fef08a"));
            innerRing.setAlpha(140);
            canvas.drawCircle(size / 2f, size / 2f, size / 2f - 16, innerRing);

            // Subtitle
            Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            subPaint.setColor(Color.parseColor("#d97706"));
            subPaint.setTextSize(18);
            subPaint.setTextAlign(Paint.Align.CENTER);
            subPaint.setFakeBoldText(true);
            canvas.drawText("القارئ الشيخ", size / 2f, size * 0.40f, subPaint);

            // Sheikh Name
            Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            namePaint.setColor(Color.WHITE);
            namePaint.setTextSize(sheikhName.length() > 18 ? 16 : 19);
            namePaint.setTextAlign(Paint.Align.CENTER);
            namePaint.setFakeBoldText(true);

            if (sheikhName.length() > 14 && sheikhName.contains(" ")) {
                int split = sheikhName.indexOf(' ', sheikhName.length() / 2 - 3);
                if (split < 0) split = sheikhName.indexOf(' ');
                if (split > 0) {
                    String p1 = sheikhName.substring(0, split).trim();
                    String p2 = sheikhName.substring(split).trim();
                    canvas.drawText(p1, size / 2f, size * 0.58f, namePaint);
                    canvas.drawText(p2, size / 2f, size * 0.72f, namePaint);
                } else {
                    canvas.drawText(sheikhName, size / 2f, size * 0.62f, namePaint);
                }
            } else {
                canvas.drawText(sheikhName, size / 2f, size * 0.62f, namePaint);
            }

            // Accent bar
            Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            barPaint.setStyle(Paint.Style.STROKE);
            barPaint.setStrokeWidth(3);
            barPaint.setStrokeCap(Paint.Cap.ROUND);
            barPaint.setShader(goldShader);
            canvas.drawLine(size * 0.28f, size * 0.82f, size * 0.72f, size * 0.82f, barPaint);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return stream.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private void loadApp() {
        if (isNetworkAvailable()) {
            offlineLayout.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(TARGET_URL);
        } else {
            webView.setVisibility(View.GONE);
            offlineLayout.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                     capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                     capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            android.net.NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }
    }

    @Override
    public void onBackPressed() {
        if (splashImageView != null && splashImageView.getVisibility() == View.VISIBLE) {
            dismissSplash();
            return;
        }

        if (customView != null) {
            if (webView.getWebChromeClient() != null) {
                customViewContainer.removeView(customView);
                customViewContainer.setVisibility(View.GONE);
                customView = null;
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }
                webView.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (webView != null) {
            webView.evaluateJavascript("window.__stepBackGradually ? window.__stepBackGradually() : false", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    if ("true".equalsIgnoreCase(value) || "\"true\"".equalsIgnoreCase(value)) {
                        // Successfully unwound to previous view in the app
                        return;
                    }
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        MainActivity.super.onBackPressed();
                    }
                }
            });
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (fileUploadCallback == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                } else if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                }
            }
            fileUploadCallback.onReceiveValue(results);
            fileUploadCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            injectFixes();
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingPushIntent(intent);
    }

    private void handleIncomingPushIntent(Intent intent) {
        if (intent == null) return;
        String url = null;
        if (intent.hasExtra("url")) {
            url = intent.getStringExtra("url");
        } else if (intent.hasExtra("link")) {
            url = intent.getStringExtra("link");
        } else if (intent.hasExtra("targetUrl")) {
            url = intent.getStringExtra("targetUrl");
        } else if (intent.getDataString() != null) {
            url = intent.getDataString();
        }
        if (url != null && !url.trim().isEmpty()) {
            final String finalUrl = url.trim();
            if (finalUrl.startsWith("http://") || finalUrl.startsWith("https://")) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (webView != null) {
                            webView.loadUrl(finalUrl);
                        }
                    }
                });
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel(
                    "eslam_fcm_notifications_channel",
                    "تطبيق إسلام - الإشعارات والتنبيهات",
                    NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("قناة إشعارات تطبيق إسلام للأذكار، التنبيهات، والآيات القرآنية");
                channel.enableVibration(true);
                channel.enableLights(true);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, PERMISSION_REQUEST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            boolean granted = false;
            for (int i = 0; i < permissions.length; i++) {
                if ((Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i]) || Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i])) &&
                    i < grantResults.length && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                }
            }
            if (pendingGeoCallback != null) {
                pendingGeoCallback.invoke(pendingGeoOrigin, granted, false);
                pendingGeoCallback = null;
                pendingGeoOrigin = null;
            }
        } else if (requestCode == PERMISSION_REQUEST_NOTIFICATIONS) {
            if (Build.VERSION.SDK_INT >= 33 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("EslamFCM", "Notification permission granted");
            }
        }
    }

    private void initFirebaseToken() {
        try {
            Class<?> fcmClass = Class.forName("com.google.firebase.messaging.FirebaseMessaging");
            Object fcmInstance = fcmClass.getMethod("getInstance").invoke(null);
            Object task = fcmClass.getMethod("getToken").invoke(fcmInstance);
            Class<?> taskClass = Class.forName("com.google.android.gms.tasks.Task");
            Class<?> listenerClass = Class.forName("com.google.android.gms.tasks.OnCompleteListener");
            java.lang.reflect.Proxy proxy = (java.lang.reflect.Proxy) java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[]{listenerClass},
                new java.lang.reflect.InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                        if ("onComplete".equals(method.getName()) && args != null && args.length > 0) {
                            Object t = args[0];
                            Boolean isSuccessful = (Boolean) t.getClass().getMethod("isSuccessful").invoke(t);
                            if (Boolean.TRUE.equals(isSuccessful)) {
                                String token = (String) t.getClass().getMethod("getResult").invoke(t);
                                Log.d("EslamFCM", "FCM Device Token: " + token);
                            }
                        }
                        return null;
                    }
                }
            );
            taskClass.getMethod("addOnCompleteListener", listenerClass).invoke(task, proxy);
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
