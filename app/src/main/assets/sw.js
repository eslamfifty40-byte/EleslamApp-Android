/* PWA service worker — repaired cache name and safe offline shell. */
const CACHE_NAME = 'islam-app-navigation-minshawi-fatiha-20261031-v2';
const SHELL = [
  './',
  './index.html',
  './manifest.webmanifest',
  './icon.svg',
  './icon-192.png',
  './icon-512.png',
  './images/splash-screen.png',
  './images/welcome-dedication.png',
  './001.mp3',
  './minshawi-fatiha-51s.mp3',
  './images/dashboard-icons/quran.webp',
  './images/reciters/safe-photo-017.jpeg',
  './quran-simple.txt'
];

self.addEventListener('install', event => {
  event.waitUntil((async () => {
    const cache = await caches.open(CACHE_NAME);
    for (const path of SHELL) {
      try {
        const response = await fetch(path, { cache: 'reload' });
        if (response.ok) await cache.put(path, response.clone());
      } catch (error) {
        // A missing optional shell asset must not prevent service-worker installation.
      }
    }
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(
      keys.filter(key =>
        (key.startsWith('islam-app-') || key.startsWith('islam-cache-stories-audio-20261028-')) &&
        key !== CACHE_NAME
      ).map(key => caches.delete(key))
    );
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;
  const request = event.request;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  event.respondWith((async () => {
    const cache = await caches.open(CACHE_NAME);
    const cached = await cache.match(request);
    if (cached) return cached;
    try {
      const response = await fetch(request);
      const type = (response.headers.get('content-type') || '').toLowerCase();
      const size = Number(response.headers.get('content-length') || 0);
      if (response.ok && !/audio|video|mpeg|mp3|octet-stream/.test(type) && (!size || size < 1800000)) {
        try { await cache.put(request, response.clone()); } catch (error) {}
      }
      return response;
    } catch (error) {
      const offlinePage = await cache.match('./index.html');
      if (offlinePage && request.mode === 'navigate') return offlinePage;
      return new Response('هذا المحتوى يحتاج إلى الإنترنت أو لم يتم حفظه بعد.', {
        status: 503,
        headers: { 'Content-Type': 'text/plain; charset=utf-8' }
      });
    }
  })());
});
