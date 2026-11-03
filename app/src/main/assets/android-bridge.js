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
