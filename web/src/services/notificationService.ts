import { TargetType } from '../store/preferencesStore';

declare global {
  interface Window {
    OneSignalDeferred?: Array<(OneSignal: any) => void>;
    OneSignal?: any;
  }
}

const ONESIGNAL_APP_ID_KEY = 'lyceum_onesignal_app_id';
// Default App ID can be put here or set via settings
export const DEFAULT_ONESIGNAL_APP_ID = '';

export interface OneSignalState {
  isSupported: boolean;
  isSubscribed: boolean;
  permission: NotificationPermission | 'default';
}

class NotificationService {
  private isInitialized = false;

  public init(appId: string = DEFAULT_ONESIGNAL_APP_ID) {
    if (typeof window === 'undefined' || !('Notification' in window)) return;
    if (this.isInitialized) return;

    const activeAppId = appId || localStorage.getItem(ONESIGNAL_APP_ID_KEY) || DEFAULT_ONESIGNAL_APP_ID;
    if (!activeAppId) return;

    this.isInitialized = true;

    // Load OneSignal SDK
    window.OneSignalDeferred = window.OneSignalDeferred || [];
    window.OneSignalDeferred.push(async (OneSignal: any) => {
      await OneSignal.init({
        appId: activeAppId,
        notifyButton: { enable: false },
        allowLocalhostAsSecureOrigin: true,
      });

      // Update tags on login or schedule preference change
      this.updateUserTags();
    });

    if (!document.getElementById('onesignal-sdk')) {
      const script = document.createElement('script');
      script.id = 'onesignal-sdk';
      script.src = 'https://cdn.onesignal.com/sdks/web/v16/OneSignalSDK.page.js';
      script.defer = true;
      document.head.appendChild(script);
    }
  }

  /**
   * Prompts user for push notification permission
   */
  public async requestPermission(): Promise<boolean> {
    if (typeof window === 'undefined') return false;

    // Direct Notification API for iOS / standard Web
    if ('Notification' in window) {
      const perm = await Notification.requestPermission();
      if (perm === 'granted') {
        window.OneSignalDeferred = window.OneSignalDeferred || [];
        window.OneSignalDeferred.push(async (OneSignal: any) => {
          await OneSignal.Notifications.requestPermission();
          this.updateUserTags();
        });
        return true;
      }
    }
    return false;
  }

  /**
   * Tags the user with their current class or teacher so they only get relevant notifications
   */
  public updateUserTags(targetType?: TargetType, targetName?: string) {
    if (typeof window === 'undefined') return;

    const type = targetType || localStorage.getItem('lyceum_target_type') || 'class';
    const name = targetName || localStorage.getItem('lyceum_target_name') || '';

    window.OneSignalDeferred = window.OneSignalDeferred || [];
    window.OneSignalDeferred.push(async (OneSignal: any) => {
      if (type === 'class' && name) {
        await OneSignal.User.addTags({
          role: 'student',
          class: name,
        });
      } else if (type === 'teacher' && name) {
        await OneSignal.User.addTags({
          role: 'teacher',
          teacher: name,
        });
      }
    });
  }

  public getPermissionStatus(): NotificationPermission {
    if (typeof window === 'undefined' || !('Notification' in window)) return 'denied';
    return Notification.permission;
  }
}

export const notificationService = new NotificationService();
