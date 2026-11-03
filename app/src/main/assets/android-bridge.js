/* جسر الأندرويد: بيحوّل البوصلة والموقع والإشعارات والتثبيت والرجوع لأوامر أندرويد أصلية.
   بيتحمّل أول حاجة في index.html. لو الصفحة فتحت في متصفح عادي، الملف ده مش بيعمل حاجة. */
(function () {
  const N = window.AndroidNative;
  if (!N) { window.islamIsNative = false; return; }
  window.islamIsNative = true;

  // ---------- الموقع ----------
  function fakePos(la, lo) {
    return { coords: { latitude: la, longitude: lo, accuracy: 30, altitude: null, heading: null, speed: null }, timestamp: Date.now() };
  }
  const geo = {
    getCurrentPosition(ok, err) {
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
  window.onNativeLocation = (la, lo) => {
    const cb = window.__geoCb; window.__geoCb = null;
    if (cb) cb.ok(fakePos(la, lo));
  };
  window.onNativeLocationError = (code) => {
    const cb = window.__geoCb; window.__geoCb = null;
    if (cb) cb.err({ code: code || 1, message: 'location unavailable' });
  };

  // ---------- البوصلة ----------
  window.islamStartCompass = function () { if (N.hasCompass()) N.startCompass(); };
  window.onNativeCompass = function (heading) {
    if (typeof window.orientationHandler === 'function') {
      window.orientationHandler({ alpha: (360 - heading) % 360, __native: true });
    }
  };

  // ---------- الإشعارات (بدون إذن متصفح) ----------
  function FakeNotification(title, opts) { N.notify(String(title), (opts && opts.body) || ''); }
  FakeNotification.permission = 'granted';
  FakeNotification.requestPermission = async function () { return 'granted'; };
  window.Notification = FakeNotification;

  // جدولة تنبيه أذان يومي: الساعة بصيغة HH:MM
  window.islamSchedulePrayer = function (id, hhmm, name) {
    const parts = String(hhmm).split(':').map(Number);
    if (parts.length < 2 || isNaN(parts[0]) || isNaN(parts[1])) return;
    N.schedulePrayer(id, parts[0], parts[1], 'حان وقت ' + name, 'حي على الصلاة');
  };

  // ---------- الرجوع ----------
  // بيرجع true لو اتعمل حاجة داخل الصفحة (عشان الأندرويد ما يخرجش)، false لو المفروض يخرج من التطبيق.
  window.islamHandleBack = function () {
    // 1) نوافذ مفتوحة فوق الصفحة: نقفلها الأول
    const full = document.getElementById('fullMushafReader');
    if (full && !full.classList.contains('hidden')) { full.classList.add('hidden'); return true; }
    const radio = document.getElementById('quranRadioPanel');
    if (radio && !radio.hidden) {
      radio.hidden = true;
      const bd = document.getElementById('quranRadioBackdrop');
      if (bd) bd.hidden = true;
      return true;
    }
    // 2) لو في شاشة الترحيب أو الصفحة الرئيسية: نخرج من التطبيق
    const welcome = document.getElementById('welcomeScreen');
    if (welcome && !welcome.classList.contains('hidden')) return false;
    const main = document.getElementById('mainAppContent');
    const dash = document.getElementById('islamDashboard');
    const onHome = (!main || main.classList.contains('hidden')) && (!dash || dash.style.display !== 'none');
    if (onHome) return false;
    // 3) لو في قسم داخلي: نرجع في تاريخ التطبيق بالترتيب
    const st = history.state;
    if (st && st.view && st.view !== 'dashboard' && st.view !== 'welcome') { history.back(); return true; }
    // 4) غير كده: نرجع للصفحة الرئيسية
    if (typeof showDashHome === 'function') { showDashHome(); return true; }
    return false;
  };

  // ---------- زر التثبيت ----------
  window.islamInstallShortcut = function () {
    const ok = N.addHomeShortcut();
    const st = document.getElementById('installAppStatus');
    if (st) st.textContent = ok ? 'تمت إضافة أيقونة التطبيق للشاشة الرئيسية.' : 'التطبيق مثبت بالفعل ويعمل من داخل الأندرويد.';
  };
})();
