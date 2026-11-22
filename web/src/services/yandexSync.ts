import { ScheduleNote } from '../types/notes';
import { useNotesStore } from '../store/notesStore';

const YANDEX_TOKEN_KEY = 'lyceum_yandex_token';
const YANDEX_USER_KEY = 'lyceum_yandex_user';
const YANDEX_SYNC_TIME_KEY = 'lyceum_yandex_last_sync';
const YANDEX_REMOTE_FILE = 'app:/lyceum_notes.json';
const WORKER_PROXY_URL = 'https://lyceum-proxy.dimafilippov1800.workers.dev/proxy-url?url=';

// OAuth Client ID from oauth.yandex.ru
export const DEFAULT_YANDEX_CLIENT_ID = '2954ef8874cf4e239c98b99a2cbedf36';

export interface YandexUserState {
  token: string | null;
  username: string | null;
  lastSyncTime: number | null;
  isSyncing: boolean;
  error: string | null;
}

class YandexSyncService {
  private syncInProgress = false;
  private listeners: (() => void)[] = [];

  public getState(): YandexUserState {
    return {
      token: localStorage.getItem(YANDEX_TOKEN_KEY),
      username: localStorage.getItem(YANDEX_USER_KEY),
      lastSyncTime: localStorage.getItem(YANDEX_SYNC_TIME_KEY)
        ? parseInt(localStorage.getItem(YANDEX_SYNC_TIME_KEY)!, 10)
        : null,
      isSyncing: this.syncInProgress,
      error: null,
    };
  }

  public subscribe(listener: () => void): () => void {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter((l) => l !== listener);
    };
  }

  private notify() {
    this.listeners.forEach((l) => l());
  }

  /**
   * Starts OAuth authorization redirect
   */
  public startLogin(clientId: string = DEFAULT_YANDEX_CLIENT_ID) {
    const redirectUri = window.location.origin + window.location.pathname;
    const authUrl = `https://oauth.yandex.ru/authorize?response_type=token&client_id=${encodeURIComponent(
      clientId
    )}&redirect_uri=${encodeURIComponent(redirectUri)}`;
    window.location.href = authUrl;
  }

  /**
   * Checks URL hash for access_token on app init / redirect return
   */
  public handleAuthCallback(): boolean {
    if (typeof window === 'undefined') return false;
    const hash = window.location.hash;
    if (!hash || !hash.includes('access_token=')) return false;

    try {
      const cleanHash = hash.replace(/^#\/?/, '');
      const params = new URLSearchParams(cleanHash);
      const token = params.get('access_token');
      if (token) {
        localStorage.setItem(YANDEX_TOKEN_KEY, token);
        // Clean hash without reload and reset to home route
        const cleanUrl = window.location.origin + window.location.pathname + '#/';
        window.history.replaceState(null, '', cleanUrl);

        // Fetch user info in background
        this.fetchUserInfo(token);
        // Trigger initial sync
        setTimeout(() => this.sync(), 500);
        this.notify();
        return true;
      }
    } catch (e) {
      console.error('Failed to parse Yandex OAuth callback', e);
    }
    return false;
  }

  public async fetchUserInfo(token?: string) {
    const authToken = token || localStorage.getItem(YANDEX_TOKEN_KEY);
    if (!authToken) return;

    try {
      const res = await fetch('https://login.yandex.ru/info?format=json', {
        headers: { Authorization: `OAuth ${authToken}` },
      });
      if (res.ok) {
        const info = await res.json();
        const displayName =
          info.display_name || info.first_name || info.default_email || 'Пользователь';
        localStorage.setItem(YANDEX_USER_KEY, displayName);
        this.notify();
      }
    } catch {
      // ignore
    }
  }

  public logout() {
    localStorage.removeItem(YANDEX_TOKEN_KEY);
    localStorage.removeItem(YANDEX_USER_KEY);
    localStorage.removeItem(YANDEX_SYNC_TIME_KEY);
    this.notify();
  }

  /**
   * Performs bidirectional sync between local notes and Yandex Disk (app:/lyceum_notes.json)
   */
  public async sync(): Promise<{ success: boolean; message?: string }> {
    const token = localStorage.getItem(YANDEX_TOKEN_KEY);
    if (!token) return { success: false, message: 'Не авторизован' };
    if (this.syncInProgress) return { success: false, message: 'Синхронизация уже идёт' };

    this.syncInProgress = true;
    this.notify();

    try {
      // 1. Fetch remote notes if file exists
      let remoteNotes: ScheduleNote[] = [];
      let fileExists = false;

      let metaRes: Response;
      try {
        metaRes = await fetch(
          `https://cloud-api.yandex.net/v1/disk/resources/download?path=${encodeURIComponent(
            YANDEX_REMOTE_FILE
          )}`,
          {
            headers: { Authorization: `OAuth ${token}` },
          }
        );
      } catch (networkErr: any) {
        // If file does not exist yet on disk or network issues
        console.warn('Yandex download check error', networkErr);
        metaRes = new Response(null, { status: 404 });
      }

      if (metaRes.ok) {
        const meta = await metaRes.json();
        if (meta.href) {
          try {
            // Proxied through Cloudflare Worker to avoid Yandex downloader CORS 403
            const proxyDownloadUrl = `${WORKER_PROXY_URL}${encodeURIComponent(meta.href)}`;
            const downloadRes = await fetch(proxyDownloadUrl);
            if (downloadRes.ok) {
              const data = await downloadRes.json();
              if (Array.isArray(data)) {
                remoteNotes = data;
                fileExists = true;
              }
            }
          } catch (dlErr) {
            console.warn('Failed to download notes via proxy:', dlErr);
          }
        }
      } else if (metaRes.status === 401) {
        this.logout();
        throw new Error('Сессия Яндекс истекла. Войдите снова.');
      }

      // 2. Merge local and remote notes
      const notesStore = useNotesStore.getState();
      const localNotes = notesStore.notes;

      const mergedMap = new Map<string, ScheduleNote>();

      // Put all remote notes first
      for (const rn of remoteNotes) {
        mergedMap.set(rn.id, rn);
      }

      // Merge local notes (latest updatedAt wins)
      for (const ln of localNotes) {
        const existing = mergedMap.get(ln.id);
        if (!existing) {
          mergedMap.set(ln.id, ln);
        } else {
          const localTime = ln.updatedAt || ln.createdAt || 0;
          const remoteTime = existing.updatedAt || existing.createdAt || 0;
          if (localTime >= remoteTime) {
            mergedMap.set(ln.id, ln);
          }
        }
      }

      // Filter out deleted notes (both deleted boolean or deletedAt timestamp)
      const finalNotes = Array.from(mergedMap.values()).filter((n) => !n.deleted && !n.deletedAt);

      // 3. Save merged list locally
      await notesStore.setNotesDirectly(finalNotes);

      // 4. Upload merged list to Yandex Disk
      const uploadMetaRes = await fetch(
        `https://cloud-api.yandex.net/v1/disk/resources/upload?path=${encodeURIComponent(
          YANDEX_REMOTE_FILE
        )}&overwrite=true`,
        {
          headers: { Authorization: `OAuth ${token}` },
        }
      );

      if (uploadMetaRes.ok) {
        const uploadMeta = await uploadMetaRes.json();
        if (uploadMeta.href) {
          const proxyUploadUrl = `${WORKER_PROXY_URL}${encodeURIComponent(uploadMeta.href)}`;
          const uploadRes = await fetch(proxyUploadUrl, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(finalNotes, null, 2),
          });

          if (!uploadRes.ok) {
            throw new Error(`Ошибка загрузки на Яндекс Диск: ${uploadRes.status}`);
          }
        }
      }

      const now = Date.now();
      localStorage.setItem(YANDEX_SYNC_TIME_KEY, now.toString());
      this.syncInProgress = false;
      this.notify();
      return { success: true };
    } catch (err: any) {
      console.error('Yandex Disk sync error:', err);
      this.syncInProgress = false;
      this.notify();
      return { success: false, message: err?.message || 'Ошибка синхронизации' };
    }
  }
}

export const yandexSyncService = new YandexSyncService();
