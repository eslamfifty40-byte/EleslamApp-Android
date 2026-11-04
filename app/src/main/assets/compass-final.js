(function () {
  var qibla = null, smooth = null;
  window.islamQiblaBearing = null;
  window.onNativeCompass = function (h) {
    var arrow = document.getElementById('compassArrow');
    if (!arrow || window.islamQiblaBearing == null || isNaN(h)) return;
    qibla = window.islamQiblaBearing;
    if (smooth == null) smooth = h;
    var d = ((h - smooth + 540) % 360) - 180;
    smooth = (smooth + d * 0.25 + 360) % 360;
    var rot = (smooth - qibla + 720) % 360;
    arrow.style.transform = 'rotate(' + ((rot + 90) % 360) + 'deg)';
  };
})();
