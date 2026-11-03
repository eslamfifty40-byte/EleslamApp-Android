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
      const last = N.getLastLocation();
      if (last) { const p = last.split(','); ok(fakePos(Number(p[0]), Number(p[1]))); return; }
      window.__geoCb = { ok, err };
      N.requestLocation();
    },
    watchPosition(ok, err) { this.getCurrentPosition(ok, err); return 0; },
    clearWatch() {}
  }});
  window.onNativeLocation = function (la, lo) { const cb = window.__geoCb; window.__geoCb = null; if (cb) cb.ok(fakePos(la, lo)); };
  window.onNativeLocationError = function (c) { const cb = window.__geoCb; window.__geoCb = null; if (cb) cb.err({ code: c || 1, message: 'err' }); };

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
    N.schedulePrayer(id, p[0], p[1], 'حان وقت ' + name, 'حي على الصلاة');
  };

  if (window.speechSynthesis) {
    window.speechSynthesis.speak = function (u) { try { N.speak(String(u && u.text || '')); } catch (e) {} };
    window.speechSynthesis.cancel = function () { try { N.stopSpeak(); } catch (e) {} };
  }

  const journal = [];
  let candidate = null;
  let undoing = false;
  const SHOWN_ATTRS = ['class', 'style', 'hidden', 'aria-hidden'];

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
      if (m.type !== 'attributes') continue;
      candidate.changes.push({ el: m.target, attr: m.attributeName, old: m.oldValue });
      if (!candidate.opens && wasHidden(m.attributeName, m.oldValue) && isVisibleNow(m.target)) candidate.opens = true;
    }
    if (candidate.opens) { journal.push(candidate); if (journal.length > 80) journal.shift(); }
    candidate = null;
  });
  mo.observe(document.documentElement, { subtree: true, attributes: true, attributeOldValue: true, attributeFilter: SHOWN_ATTRS });

  function undoLast() {
    const g = journal.pop();
    if (!g) return false;
    undoing = true;
    try {
      for (let i = g.changes.length - 1; i >= 0; i--) {
        const c = g.changes[i];
        if (c.old === null || c.old === undefined) c.el.removeAttribute(c.attr);
        else c.el.setAttribute(c.attr, c.old);
      }
      window.scrollTo(0, g.scrollY || 0);
    } finally { undoing = false; }
    return true;
  }

  window.islamHandleBack = function () {
    const radio = document.getElementById('quranRadioPanel');
    if (radio && !radio.hidden) {
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
    '#islamSettingsBtn{position:fixed;top:10px;right:10px;z-index:99999;width:42px;height:42px;border-radius:50%;background:#064e3b;color:#fde68a;border:2px solid #d4af37;font-size:20px}',
    '#islamSettingsPanel{position:fixed;top:60px;right:10px;z-index:99999;background:#031912;color:#fde68a;border:1px solid #d4af37;border-radius:12px;padding:12px;width:240px}',
    '#islamSettingsPanel button{background:#064e3b;color:#fde68a;border:1px solid #d4af37;border-radius:8px;padding:6px 10px;margin:4px}'
  ].join('');
  const style = document.createElement('style'); style.textContent = css; document.head.appendChild(style);

  function addSettings() {
    if (document.getElementById('islamSettingsBtn')) return;
    const btn = document.createElement('button');
    btn.id = 'islamSettingsBtn'; btn.textContent = '⚙';
    const panel = document.createElement('div');
    panel.id = 'islamSettingsPanel'; panel.style.display = 'none';
    panel.innerHTML = '<div>الإعدادات</div>' +
      '<div><button data-a="fu">A+</button><button data-a="fd">A-</button></div>' +
      '<div><button data-a="night">الوضع الليلي</button></div>' +
      '<div>الصوت <input type="range" min="0" max="1" step="0.05" value="1" id="islamVol" style="width:100%"></div>' +
      '<div><button data-a="pause">إيقاف الصوت</button></div>';
    document.body.appendChild(btn); document.body.appendChild(panel);
    let pct = 100;
    btn.onclick = function () { panel.style.display = panel.style.display === 'none' ? 'block' : 'none'; };
    panel.onclick = function (e) {
      const a = e.target && e.target.getAttribute && e.target.getAttribute('data-a');
      if (!a) return;
      if (a === 'fu') pct = Math.min(160, pct + 10);
      if (a === 'fd') pct = Math.max(80, pct - 10);
      if (a === 'fu' || a === 'fd') document.documentElement.style.fontSize = pct + '%';
      if (a === 'night') document.body.style.filter = document.body.style.filter ? '' : 'brightness(0.75)';
      if (a === 'pause') document.querySelectorAll('audio,video').forEach(function (m) { m.pause(); });
    };
    document.getElementById('islamVol').oninput = function (e) {
      document.querySelectorAll('audio,video').forEach(function (m) { m.volume = Number(e.target.value); });
    };
    setInterval(function () {
      const dash = document.getElementById('islamDashboard');
      const main = document.getElementById('mainAppContent');
      const onHome = !!dash && dash.style.display !== 'none' && (!main || main.classList.contains('hidden'));
      btn.style.display = onHome ? 'block' : 'none';
      if (!onHome) panel.style.display = 'none';
    }, 400);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', addSettings); else addSettings();
})();
