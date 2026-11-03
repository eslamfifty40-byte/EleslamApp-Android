(function () {
  if (!window.islamIsNative) return;
  window.islamHandleBack = function () {
    var radio = document.getElementById('quranRadioPanel');
    if (radio && !radio.hidden) { radio.hidden = true; var bd = document.getElementById('quranRadioBackdrop'); if (bd) bd.hidden = true; return true; }
    var dash = document.getElementById('islamDashboard');
    if (dash && dash.getClientRects().length > 0) return false;
    if (typeof showDashHome === 'function') { showDashHome(); return true; }
    return false;
  };
})();
