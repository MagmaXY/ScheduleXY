import { TargetType, usePreferencesStore } from '../store/preferencesStore';

// Public VAPID key (ECDSA P-256) for native Web Push (no third-party SDKs/CDNs)
export const VAPID_PUBLIC_KEY =
  'BMI0E44PnZ_p4Tnh6wlxa-e7F5Wd8Ptb5q-koLwFMaTbkh7hy0vKdM8-2rqzxl2BvrjEdv5MxKWvunRbZqE-hec';

// Yandex Cloud Function URL for saving PushSubscription & checking updates
export const DEFAULT_PUSH_ENDPOINT = '';

function urlBase64ToUint8Array(base64String: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = window.atob(base64);
  const buffer = new ArrayBuffer(rawData.length);
  const outputArray = new Uint8Array(buffer);
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

class NotificationService {
  private isInitialized = false;

  public init() {
    if (typeof window === 'undefined' || !('Notification' in window)) return;
    if (this.isInitialized) return;
    this.isInitialized = true;

    if (Notification.permission === 'granted') {
      this.syncSubscription().catch(() => {});
    }
  }

  public isSupported(): boolean {
    return (
      typeof window !== 'undefined' &&
      'Notification' in window &&
      'serviceWorker' in navigator &&
      'PushManager' in window
    );
  }

  public getPermissionStatus(): NotificationPermission | 'default' {
    if (typeof window === 'undefined' || !('Notification' in window)) return 'default';
    return Notification.permission;
  }

  public async requestPermission(): Promise<boolean> {
    if (!this.isSupported()) return false;

    const perm = await Notification.requestPermission();
    if (perm !== 'granted') return false;

    try {
      await this.syncSubscription();
      return true;
    } catch (err) {
      console.warn('Web Push subscription failed:', err);
      return false;
    }
  }

  private async ensureSubscription(): Promise<PushSubscription | null> {
    if (!this.isSupported() || Notification.permission !== 'granted') return null;

    const registration = await navigator.serviceWorker.ready;
    const existing = await registration.pushManager.getSubscription();
    if (existing) return existing;

    return await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY),
    });
  }

  public async syncSubscription(targetType?: TargetType, targetName?: string): Promise<void> {
    const subscription = await this.ensureSubscription();
    if (!subscription) return;

    const prefs = usePreferencesStore.getState();
    const type = targetType || prefs.targetType || 'class';
    const name = targetName || prefs.targetName || '';
    if (!name) return;

    if (!DEFAULT_PUSH_ENDPOINT) return;

    await fetch(DEFAULT_PUSH_ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        action: 'subscribe',
        subscription: subscription.toJSON(),
        targetType: type,
        targetName: name,
      }),
    });
  }

  public updateUserTags(targetType?: TargetType, targetName?: string) {
    if (!this.isSupported() || Notification.permission !== 'granted') return;
    this.syncSubscription(targetType, targetName).catch((err) => {
      console.warn('Failed to update push subscription target:', err);
    });
  }
}

export const notificationService = new NotificationService();
