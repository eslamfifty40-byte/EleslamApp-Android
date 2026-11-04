(function () {
  const N = window.AndroidNative;
  if (!N) { window.islamIsNative = false; return; }
  window.islamIsNative = true;

  function fakePos(la, lo) {
    return { coords: { latitude: la, longitude: lo, accuracy: 30, altitude: null, heading: null, speed: null }, timestamp: Date.now() };
  }
  Object.defineProperty(navigator, 'geolocation', { configurable: true, value: {
    getCurrentPosition(ok, err) {
      if (!N.isLocationEnabled()) { N.openLocationSettings(); if (err) err({ code: 2, message: 'off' }); return; }
      const cb = { ok: ok, err: err, done: false };
      window.__geoCb = cb;
      const last = N.getLastLocation();
      N.requestLocation();
      setTimeout(function () {
        if (cb.done) return;
        cb.done = true; window.__geoCb = null;
        if (last) { const p = last.split(','); ok(fakePos(Number(p[0]), Number(p[1]))); }
        else if (err) err({ code: 3, message: 'timeout' });
      }, 12000);
    },
    watchPosition(ok, err) { this.getCurrentPosition(ok, err); return 0; },
    clearWatch() {}
  }});
  window.onNativeLocation = function (la, lo) {
    const cb = window.__geoCb; if (!cb || cb.done) return;
    cb.done = true; window.__geoCb = null; cb.ok(fakePos(la, lo));
  };
  window.onNativeLocationError = function (c) {
    const cb = window.__geoCb; if (!cb || cb.done) return;
    cb.done = true; window.__geoCb = null; if (cb.err) cb.err({ code: c || 1, message: 'err' });
  };

  window.islamStartCompass = function () { if (N.hasCompass()) N.startCompass(); };
  window.onNativeCompass = function (h) {
    if (typeof window.orientationHandler === 'function') window.orientationHandler({ alpha: (360 - h) % 360, __native: true });
  };

  function FakeNotification(title, opts) { N.notify(String(title), (opts && opts.body) || ''); }
  FakeNotification.permission = 'granted';
  FakeNotification.requestPermission = async function () { return 'granted'; };
  window.Notification = FakeNotification;
  window.islamSchedulePrayer = function (id, hhmm, name) {
    const p = String(hhmm).split(':').map(Number);
    if (p.length < 2 || isNaN(p[0]) || isNaN(p[1])) return;
    N.schedulePrayer(id, p[0], p[1], 'موعد الصلاة', 'حان الآن موعد الصلاة');
  };

  if (window.speechSynthesis) {
    window.speechSynthesis.speak = function (u) { try { N.speak(String(u && u.text || '')); } catch (e) {} };
    window.speechSynthesis.cancel = function () { try { N.stopSpeak(); } catch (e) {} };
  }

  const journal = [];
  let candidate = null;
  let undoing = false;
  const SHOWN_ATTRS = ['class', 'style', 'hidden', 'aria-hidden'];
  function isElem(n) { return n && n.nodeType === 1; }
  function isVisibleNow(el) {
    if (!el || !el.getAttribute) return false;
    if (el.hidden || el.getAttribute('aria-hidden') === 'true') return false;
    if (el.style && el.style.display === 'none') return false;
    if (el.classList && el.classList.contains('hidden')) return false;
    return true;
  }
  function wasHidden(attr, old) {
    if (attr === 'hidden') return old !== null && old !== undefined;
    if (attr === 'aria-hidden') return old === 'true';
    if (old === null || old === undefined) return false;
    return /(^|\s)hidden(\s|$)/.test(old) || /display\s*:\s*none/.test(old);
  }

  document.addEventListener('click', function () {
    if (undoing) return;
    candidate = { scrollY: window.scrollY, changes: [], opens: false };
  }, true);

  const mo = new MutationObserver(function (muts) {
    if (undoing || !candidate) return;
    for (const m of muts) {
      if (m.type === 'attributes') {
        candidate.changes.push({ kind: 'attr', el: m.target, attr: m.attributeName, old: m.oldValue });
        if (!candidate.opens && wasHidden(m.attributeName, m.oldValue) && isVisibleNow(m.target)) candidate.opens = true;
      } else if (m.type === 'childList') {
        m.addedNodes.forEach(function (n) {
          candidate.changes.push({ kind: 'add', node: n });
          if (isElem(n) && n.getClientRects && n.getClientRects().length > 0) candidate.opens = true;
        });
        m.removedNodes.forEach(function (n) {
          candidate.changes.push({ kind: 'rem', node: n, parent: m.target, next: m.nextSibling });
        });
      }
    }
    if (candidate.opens) { journal.push(candidate); if (journal.length > 80) journal.shift(); }
    candidate = null;
  });
  mo.observe(document.documentElement, { subtree: true, childList: true, attributes: true, attributeOldValue: true, attributeFilter: SHOWN_ATTRS });

  function undoLast() {
    const g = journal.pop();
    if (!g) return false;
    undoing = true;
    try {
      for (let i = g.changes.length - 1; i >= 0; i--) {
        const c = g.changes[i];
        if (c.kind === 'attr') {
          if (c.old === null || c.old === undefined) c.el.removeAttribute(c.attr);
          else c.el.setAttribute(c.attr, c.old);
        } else if (c.kind === 'add') {
          if (c.node.parentNode) c.node.parentNode.removeChild(c.node);
        } else if (c.kind === 'rem') {
          if (c.next && c.next.parentNode === c.parent) c.parent.insertBefore(c.node, c.next);
          else c.parent.appendChild(c.node);
        }
      }
      window.scrollTo(0, g.scrollY || 0);
    } finally { undoing = false; }
    return true;
  }

  window.islamHandleBack = function () {
    const radio = document.getElementById('quranRadioPanel');
    if (radio && !radio.hidden && getComputedStyle(radio).display !== 'none') {
      radio.querySelectorAll('audio,video').forEach(function (m) {
        try { m.pause(); } catch (e) {}
      });
      radio.hidden = true;
      const bd = document.getElementById('quranRadioBackdrop');
      if (bd) bd.hidden = true;
      return true;
    }
    if (undoLast()) return true;
    return false;
  };

  const css = [
    '[onclick*="installIslamApp"],#installAppBtn,#installAppStatus{display:none!important}',
    'body.islam-night-mode{filter:brightness(.75)!important}',
    '#globalSizeControls,#globalZoomReopen{position:fixed!important;left:10px!important;right:auto!important;top:auto!important;bottom:calc(10px + env(safe-area-inset-bottom))!important;z-index:2147483008!important}',
    '#islamSettingsBtn{position:fixed!important;left:auto!important;top:10px!important;right:10px!important;z-index:99999;width:44px!important;height:44px!important;min-width:44px;border-radius:50%;background:#064e3b;color:#fde68a;border:2px solid #d4af37;font-size:20px!important;line-height:1;padding:0;zoom:1}',
    '#islamSettingsPanel{position:fixed;top:64px;right:10px;z-index:99999;background:#031912;color:#fde68a;border:1px solid #d4af37;border-radius:14px;padding:14px;width:250px;box-shadow:0 8px 24px rgba(0,0,0,.5)}',
    '#islamSettingsPanel .ttl{font-size:18px;text-align:center;margin-bottom:10px}',
    '#islamSettingsPanel .row{display:flex;gap:8px;justify-content:center;margin:8px 0;flex-wrap:wrap}',
    '#islamSettingsPanel button{flex:1;background:#064e3b;color:#fde68a;border:1px solid #d4af37;border-radius:10px;padding:8px 6px;font-size:14px;min-width:90px}',
    '#islamSettingsPanel input[type=range]{width:100%;margin-top:6px}',
    '#islamSpin{position:fixed;left:50%;bottom:14vh;transform:translateX(-50%);width:54px;height:54px;border:5px solid rgba(212,175,55,.25);border-top-color:#f5d76e;border-radius:50%;animation:islamspin 1s linear infinite;z-index:99998}',
    '@keyframes islamspin{to{transform:translateX(-50%) rotate(360deg)}}'
  ].join('');
  const style = document.createElement('style'); style.textContent = css; document.head.appendChild(style);

  function mediaList() { return Array.prototype.slice.call(document.querySelectorAll('audio,video')); }

  function addSettings() {
    if (document.getElementById('islamSettingsBtn')) return;
    const btn = document.createElement('button');
    btn.id = 'islamSettingsBtn'; btn.textContent = '⚙';
    const panel = document.createElement('div');
    panel.id = 'islamSettingsPanel'; panel.style.display = 'none';
    panel.innerHTML =
      '<div class="ttl">الإعدادات</div>' +
      '<div class="row"><button data-a="fu">تكبير الخط A+</button><button data-a="fd">تصغير الخط A-</button></div>' +
      '<div class="row"><button data-a="night">الوضع الليلي</button></div>' +
      '<div>الصوت</div><input type="range" min="0" max="1" step="0.05" value="1" id="islamVol">' +
      '<div class="row"><button data-a="pause">إيقاف الصوت</button><button data-a="play">تشغيل الصوت</button></div>';
    document.body.appendChild(btn); document.body.appendChild(panel);
    let pct = 100;
    try {
      pct = Math.max(80, Math.min(160,
        Number(localStorage.getItem('islamFontPercent')) || 100));
      document.documentElement.style.fontSize = pct + '%';
      if (localStorage.getItem('islamNightMode') === 'true')
        document.body.classList.add('islam-night-mode');
    } catch(e) {}
    let pausedByUs = [];
    btn.onclick = function () { panel.style.display = panel.style.display === 'none' ? 'block' : 'none'; };
    panel.onclick = function (e) {
      const a = e.target && e.target.getAttribute && e.target.getAttribute('data-a');
      if (!a) return;
      if (a === 'fu') pct = Math.min(160, pct + 10);
      if (a === 'fd') pct = Math.max(80, pct - 10);
      if (a === 'fu' || a === 'fd') {
        document.documentElement.style.fontSize = pct + '%';
        try { localStorage.setItem('islamFontPercent', String(pct)); } catch(e) {}
      }
      if (a === 'night') {
        document.body.classList.toggle('islam-night-mode');
        try {
          localStorage.setItem('islamNightMode',
            String(document.body.classList.contains('islam-night-mode')));
        } catch(e) {}
      }
      if (a === 'pause') { pausedByUs = []; mediaList().forEach(function (m) { if (!m.paused) { pausedByUs.push(m); m.pause(); } }); }
      if (a === 'play') { pausedByUs.forEach(function (m) { m.play().catch(function () {}); }); pausedByUs = []; }
    };
    document.getElementById('islamVol').oninput = function (e) {
      mediaList().forEach(function (m) { m.volume = Number(e.target.value); });
    };
    setInterval(function () {
      document.querySelectorAll('button,a').forEach(function (el) {
        const t = (el.textContent || '').trim();
        if (t.indexOf('تثبيت التطبيق') !== -1 || t.indexOf('تحميل التطبيق') !== -1) el.style.display = 'none';
      });
      const dash = document.getElementById('islamDashboard');
      const onHome = !!dash && dash.getClientRects().length > 0;
      btn.style.display = onHome ? 'block' : 'none';
      if (!onHome) panel.style.display = 'none';
    }, 300);

  }

  function introFix() {
    // Startup order is controlled by index.html:
    // splash screen, then dedication, then dashboard.
  }

  function boot() { addSettings(); introFix(); }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot); else boot();
})();
(function () {
  if (!window.islamIsNative) return;
  setInterval(function () {
    ['islamSettingsBtn', 'islamSettingsPanel'].forEach(function (id) {
      var e = document.getElementById(id);
      if (e) e.remove();
    });
  }, 300);
})();

(function () {
  if (!window.islamIsNative) return;
  var smooth = null, lastT = 0;
  window.onNativeCompass = function (h) {
    var arrow = document.getElementById('compassArrow');
    var q = window.islamQiblaBearing;
    if (!arrow || q == null || isNaN(h)) return;
    var now = Date.now();
    if (now - lastT < 60) return;
    lastT = now;
    if (smooth == null) smooth = h;
    var d = ((h - smooth + 540) % 360) - 180;
    smooth = (smooth + d * 0.25 + 360) % 360;
    var rot = (q - smooth + 360) % 360;
    arrow.style.transform = 'rotate(' + rot + 'deg)';
  };
})();
