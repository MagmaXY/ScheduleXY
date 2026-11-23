// Network client with smart routing:
// 1. In dev/preview environment (Vite): uses local /api-lyceum proxy (zero CORS issues!)
// 2. In production (GitHub Pages): tries user configurable proxy or public CORS proxies

// Dedicated Cloudflare Worker CORS proxy for Lyceum NSTU
export const DEFAULT_PROXY_BASE = 'https://lyceum-proxy.dimafilippov1800.workers.dev';

export interface ClientConfig {
  corsProxy: string;
  timeoutMs: number;
}

export const defaultClientConfig: ClientConfig = {
  corsProxy: '', // Empty means use default proxy
  timeoutMs: 15000,
};

let currentConfig = { ...defaultClientConfig };

export function setClientConfig(newConfig: Partial<ClientConfig>) {
  currentConfig = { ...currentConfig, ...newConfig };
}

export function getClientConfig(): ClientConfig {
  return currentConfig;
}

export async function fetchWithProxy<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const cleanEndpoint = endpoint.replace(/^\//, '');

  // Detect if we are on localhost or dev server
  const isLocalhost =
    typeof window !== 'undefined' &&
    (window.location.hostname === 'localhost' ||
      window.location.hostname === '127.0.0.1' ||
      window.location.hostname.startsWith('192.168.') ||
      window.location.hostname.startsWith('26.'));

  // Priority URLs to try
  const urlsToTry: string[] = [];

  // Helper to map API endpoint to static pre-built GitHub Pages cache
  // e.g. schedule/classes -> data/classes.json
  // schedule/class/10-1?week=0 -> data/class/10-1.json
  const getStaticCacheUrl = (ep: string): string | null => {
    // Determine base public path
    const basePath = import.meta.env.BASE_URL || '/';
    const cleanBase = basePath.endsWith('/') ? basePath : `${basePath}/`;

    if (ep === 'schedule/classes') {
      return `${cleanBase}data/classes.json`;
    }
    if (ep === 'schedule/teachers') {
      return `${cleanBase}data/teachers.json`;
    }
    const classMatch = ep.match(/^schedule\/class\/([^?]+)(\?week=0)?$/);
    if (classMatch) {
      return `${cleanBase}data/class/${classMatch[1]}.json`;
    }
    const teacherMatch = ep.match(/^schedule\/teacher\/([^?]+)(\?week=0)?$/);
    if (teacherMatch) {
      return `${cleanBase}data/teacher/${teacherMatch[1]}.json`;
    }
    return null;
  };

  const staticCacheUrl = getStaticCacheUrl(cleanEndpoint);

  // 1. If static cache is available and we are on production / not localhost,
  // try the instant GitHub Pages static cache first!
  // It has zero CORS, works in RU without VPN, and loads in < 50ms.
  if (staticCacheUrl && !isLocalhost && options.method !== 'POST') {
    urlsToTry.push(staticCacheUrl);
  }

  if (isLocalhost) {
    // In local dev server, vite proxy works directly without CORS
    urlsToTry.push(`/api-lyceum/${cleanEndpoint}`);
  }

  // 2. High-speed custom Cloudflare Worker proxy (live data / fallback / week!=0)
  const proxyBase = currentConfig.corsProxy || DEFAULT_PROXY_BASE;
  const cleanBase = proxyBase.replace(/\/+$/, '');
  urlsToTry.push(`${cleanBase}/${cleanEndpoint}`);

  // 3. Direct Lyceum API
  urlsToTry.push(`https://lyceum.nstu.ru/info/api/${cleanEndpoint}`);

  // 4. If static cache wasn't prioritized, add as final fallback
  if (staticCacheUrl && !urlsToTry.includes(staticCacheUrl) && options.method !== 'POST') {
    urlsToTry.push(staticCacheUrl);
  }

  let lastError: unknown = null;

  for (const url of urlsToTry) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), currentConfig.timeoutMs);

    try {
      const res = await fetch(url, {
        ...options,
        signal: controller.signal,
        headers: {
          Accept: 'application/json',
          ...(options.headers || {}),
        },
      });

      clearTimeout(timer);

      if (res.ok) {
        return (await res.json()) as T;
      }
    } catch (err) {
      clearTimeout(timer);
      lastError = err;
    }
  }

  throw lastError || new Error('Failed to fetch schedule data');
}
