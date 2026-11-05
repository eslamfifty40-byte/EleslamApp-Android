(function () {
  if (!window.islamIsNative) return;

  // مصدر واحد لاتجاه البوصلة في تطبيق Android.
  window.onNativeCompass = function (heading) {
    var qibla = window.islamQiblaBearing;
    var arrow = document.getElementById('compassArrow');

    if (!arrow ||
        typeof heading !== 'number' || !Number.isFinite(heading) ||
        typeof qibla !== 'number' || !Number.isFinite(qibla)) {
      return;
    }

    // نفس معادلة اتجاه القبلة المستخدمة في الصفحة الأصلية.
    var angle = ((qibla - heading) % 360 + 360) % 360;
    arrow.style.transform = 'rotate(' + angle + 'deg)';
  };
})();
