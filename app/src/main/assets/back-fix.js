(function () {
  if (!window.islamIsNative) return;
  function visible(el) {
    return !!el && !el.hidden && el.getClientRects().length > 0;
  }
  window.islamHandleBack = function () {
    var qiraat = document.getElementById('qiraatTenPanel');
    if (qiraat && qiraat.classList.contains('qiraat-page-open')) {
      if (typeof window.returnFromQiraatToHome === 'function') {
        window.returnFromQiraatToHome();
      } else {
        if (typeof window.closeQiraatPage === 'function')
          window.closeQiraatPage();
        if (typeof window.showDashHome === 'function')
          window.showDashHome();
      }
      return true;
    }
    var radio = document.getElementById('quranRadioPanel');
    if (visible(radio)) {
      var close = document.getElementById('qrClose');
      if (close) { close.click(); return true; }
      radio.hidden = true;
      var bd = document.getElementById('quranRadioBackdrop');
      if (bd) bd.hidden = true;
      return true;
    }
    var dash = document.getElementById('islamDashboard');
    var homeVisible = !!dash && dash.getClientRects().length > 0;
    if (homeVisible) return false;
    if (typeof showDashHome === 'function') { showDashHome(); return true; }
    return false;
  };
})();
