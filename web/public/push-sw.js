// Native Web Push listener imported into VitePWA's Workbox sw.js
self.addEventListener('push', (event) => {
  if (!event.data) return;

  let payload = {};
  try {
    payload = event.data.json();
  } catch {
    payload = { title: 'Лицей НГТУ — Расписание', body: event.data.text() };
  }

  const title = payload.title || 'Лицей НГТУ — Изменение расписания';
  const options = {
    body: payload.body || 'Появились изменения в расписании. Откройте для просмотра.',
    icon: './icons/icon-192.png',
    badge: './icons/icon-192.png',
    tag: payload.tag || 'schedulexy-update',
    renotify: true,
    data: {
      url: payload.url || './#/schedule',
    },
  };

  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const targetUrl = event.notification.data?.url || './#/schedule';

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientsList) => {
      for (const client of clientsList) {
        if ('focus' in client) {
          return client.focus();
        }
      }
      if (self.clients.openWindow) {
        return self.clients.openWindow(targetUrl);
      }
      return undefined;
    })
  );
});
