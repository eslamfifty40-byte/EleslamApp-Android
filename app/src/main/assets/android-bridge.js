(function () {
  const N = window.AndroidNative;
  if (!N) { window.islamIsNative = false; return; }
  window.islamIsNative = true;

  const realPush = history.pushState.bind(history);
  const realReplace = history.replaceState.bind(history);
  let navIndex = 0;
  function stamp(st) { return Object.assign({}, st || {}, { __n: navIndex }); }

  history.pushState = function (st, title, url) {
    navIndex++;
    return realPush(stamp(st), title, url);
  };

  let handlingPop = false;
  history.replaceState = function (st, title, url) {
    const cur = history.state || {};
    if (!handlingPop && st && st.view === 'dashboard' && cur.view && cur.view !== 'dashboard' && cur.view !== 'welcome' && navIndex > 0) {
      navIndex++;
      return realPush(stamp(st), title, url);
    }
    return realReplace(stamp(st), title, url);
  };

  window.addEventListener('popstate', function (e) {
    const st = e.state;
    if (st && typeof st.__n === 'number') navIndex = st.__n;
    else navIndex = 0;
    handlingPop = true;
    setTimeout(function () { handlingPop = false; }, 0);
  }, true);

  realReplace(stamp(history.state), '', location.href);

  function fakePos(la, lo) {
    return { coords: { latitude: la, longitude: lo, accuracy: 30, altitude: null, heading: null, speed: null }, timestamp: Date.now() };
  }
  const geo = {
    getCurrentPosition(ok, err) {
      if (!N.isLocationEnabled()) {
        N.openLocationSettings();
        if (err) err({ code: 2, message: 'location services off' });
        return;
      }
      const last = N.getLastLocation();
      if (last) {
        const [la, lo] = last.split(',').map(Number);
        ok(fakePos(la, lo));
        return;
      }
      window.__geoCb = { ok, err };
      N.requestLocation();
    },
    watchPosition(ok, err) { this.getCurrentPosition(ok, err); return 0; },
    clearWatch() {}
  };
  Object.defineProperty(navigator, 'geolocation', { value: geo, configurable: true });
  window.onNativeLocation = function (la, lo) {
    const cb = window.__geoCb; window.__geoCb = null;
    if (cb) cb.ok(fakePos(la, lo));
  };
  window.onNativeLocationError = function (code) {
    const cb = window.__geoCb; window.__geoCb = null;
    if (cb) cb.err({ code: code || 1, message: 'location unavailable' });
  };

  window.islamStartCompass = function () { if (N.hasCompass()) N.startCompass(); };
  window.onNativeCompass = function (heading) {
    if (typeof window.orientationHandler === 'function') {
      window.orientationHandler({ alpha: (360 - heading) % 360, __native: true });
    }
  };

  function FakeNotification(title, opts) { N.notify(String(title), (opts && opts.body) || ''); }
  FakeNotification.permission = 'granted';
  FakeNotification.requestPermission = async function () { return 'granted'; };
  window.Notification = FakeNotification;

  window.islamSchedulePrayer = function (id, hhmm, name) {
    const parts = String(hhmm).split(':').map(Number);
    if (parts.length < 2 || isNaN(parts[0]) || isNaN(parts[1])) return;
    N.schedulePrayer(id, parts[0], parts[1], 'حان وقت ' + name, 'حي على الصلاة');
  };

  window.islamHandleBack = function () {
    const full = document.getElementById('fullMushafReader');
    if (full && !full.classList.contains('hidden')) { full.classList.add('hidden'); return true; }
    const radio = document.getElementById('quranRadioPanel');
    if (radio && !radio.hidden) {
      radio.hidden = true;
      const bd = document.getElementById('quranRadioBackdrop');
      if (bd) bd.hidden = true;
      return true;
    }
    const welcome = document.getElementById('welcomeScreen');
    if (welcome && !welcome.classList.contains('hidden')) return false;
    if (navIndex > 0) { history.back(); return true; }
    const main = document.getElementById('mainAppContent');
    const dash = document.getElementById('islamDashboard');
    const onHome = (!main || main.classList.contains('hidden')) && (!dash || dash.style.display !== 'none');
    if (onHome) return false;
    if (typeof showDashHome === 'function') { showDashHome(); return true; }
    return false;
  };

  window.islamInstallShortcut = function () {
    const ok = N.addHomeShortcut();
    const st = document.getElementById('installAppStatus');
    if (st) st.textContent = ok ? 'تمت إضافة أيقونة التطبيق للشاشة الرئيسية.' : 'التطبيق مثبت بالفعل ويعمل من داخل الأندرويد.';
  };
})();
(function(){
  if (!window.islamIsNative) return;
  const st = document.createElement('style');
  st.textContent = '#installAppBtn,#installAppStatus{display:none!important}';
  document.head.appendChild(st);
})();
(function(){
  if (!window.islamIsNative) return;
  const css = '#islamSettingsBtn{position:fixed;top:10px;right:10px;z-index:99999;width:42px;height:42px;border-radius:50%;background:#064e3b;color:#fde68a;border:2px solid #d4af37;font-size:20px;display:none}' +
    '#islamSettingsPanel{position:fixed;top:60px;right:10px;z-index:99999;background:#031912;color:#fde68a;border:1px solid #d4af37;border-radius:12px;padding:12px;width:240px;display:none}' +
    '#islamSettingsPanel button{background:#064e3b;color:#fde68a;border:1px solid #d4af37;border-radius:8px;padding:6px 10px;margin:4px}';
  const style = document.createElement('style'); style.textContent = css; document.head.appendChild(style);

  const btn = document.createElement('button');
  btn.id = 'islamSettingsBtn'; btn.textContent = '⚙';
  const panel = document.createElement('div');
  panel.id = 'islamSettingsPanel';
  panel.innerHTML = '<div>الإعدادات</div>' +
    '<div><button data-a="font-up">A+</button><button data-a="font-down">A-</button></div>' +
    '<div><button data-a="night">الوضع الليلي</button></div>' +
    '<div>الصوت: <input type="range" min="0" max="1" step="0.05" value="1" id="islamVol" style="width:100%"></div>' +
    '<div><button data-a="pause">إيقاف الصوت</button></div>';
  document.body.appendChild(btn); document.body.appendChild(panel);

  let fontPct = 100;
  btn.addEventListener('click', function(){ panel.style.display = panel.style.display === 'block' ? 'none' : 'block'; });
  panel.addEventListener('click', function(e){
    const a = e.target && e.target.getAttribute && e.target.getAttribute('data-a');
    if (!a) return;
    if (a === 'font-up') fontPct = Math.min(160, fontPct + 10);
    if (a === 'font-down') fontPct = Math.max(80, fontPct - 10);
    if (a === 'font-up' || a === 'font-down') document.documentElement.style.fontSize = fontPct + '%';
    if (a === 'night') document.body.style.filter = document.body.style.filter ? '' : 'brightness(0.75)';
    if (a === 'pause') document.querySelectorAll('audio,video').forEach(function(m){ m.pause(); });
  });
  document.getElementById('islamVol').addEventListener('input', function(e){
    document.querySelectorAll('audio,video').forEach(function(m){ m.volume = Number(e.target.value); });
  });

  // الزر يظهر بس في الصفحة الرئيسية
  setInterval(function(){
    const dash = document.getElementById('islamDashboard');
    const onHome = dash && dash.style.display !== 'none' && !document.getElementById('mainAppContent').classList.contains('hidden');
    btn.style.display = onHome ? 'block' : 'none';
    if (!onHome) panel.style.display = 'none';
    // إخفاء زر التثبيت في صفحة الدخول
    document.querySelectorAll('button,a').forEach(function(el){
      if ((el.textContent || '').indexOf('تثبيت التطبيق على الهاتف') !== -1) el.style.display = 'none';
    });
  }, 500);
})();
