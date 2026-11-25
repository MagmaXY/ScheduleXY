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

export type FetchMode = 'cache-first' | 'live-first' | 'live-only';

export async function fetchWithProxy<T>(
  endpoint: string,
  options: RequestInit = {},
  mode: FetchMode = 'cache-first',
  timeoutMsOverride?: number
): Promise<T> {
  const cleanEndpoint = endpoint.replace(/^\//, '');

  // Detect if we are on localhost or dev server
  const isLocalhost =
    typeof window !== 'undefined' &&
    (window.location.hostname === 'localhost' ||
      window.location.hostname === '127.0.0.1' ||
      window.location.hostname.startsWith('192.168.') ||
      window.location.hostname.startsWith('26.'));

  // Helper to map API endpoint to static pre-built GitHub Pages cache
  const getStaticCacheUrl = (ep: string): string | null => {
    const basePath = import.meta.env.BASE_URL || '/';
    const cleanBase = basePath.endsWith('/') ? basePath : `${basePath}/`;

    if (ep === 'schedule/classes') {
      return `${cleanBase}data/classes.json`;
    }
    if (ep === 'schedule/teachers') {
      return `${cleanBase}data/teachers.json`;
    }
    if (ep === 'events') {
      return `${cleanBase}data/events.json`;
    }
    if (ep === 'schedule/archive') {
      return `${cleanBase}data/archive.json`;
    }
    if (ep === 'schedule/availability-grid') {
      return `${cleanBase}data/availability_grid.json`;
    }
    const classMatch = ep.match(/^schedule\/class\/([^?]+)(?:\?week=([-\d]+))?$/);
    if (classMatch) {
      const rawName = classMatch[1];
      let decodedName = rawName;
      try {
        decodedName = decodeURIComponent(rawName);
      } catch {
        // ignore
      }
      const encodedName = encodeURIComponent(decodedName);
      const week = classMatch[2] || '0';
      if (week === '0') {
        // Return encoded URL path (standard for browsers / web servers)
        return `${cleanBase}data/class/${encodedName}.json`;
      }
      if (['-1', '-2', '-3'].includes(week)) {
        return `${cleanBase}data/archive/week_${week}/${encodedName}.json`;
      }
    }
    const teacherMatch = ep.match(/^schedule\/teacher\/([^?]+)(?:\?week=([-\d]+))?$/);
    if (teacherMatch) {
      const rawName = teacherMatch[1];
      let decodedName = rawName;
      try {
        decodedName = decodeURIComponent(rawName);
      } catch {
        // ignore
      }
      const encodedName = encodeURIComponent(decodedName);
      const week = teacherMatch[2] || '0';
      if (week === '0') {
        return `${cleanBase}data/teacher/${encodedName}.json`;
      }
    }
    return null;
  };

  const staticCacheUrl = getStaticCacheUrl(cleanEndpoint);

  // Priority URLs to try
  const urlsToTry: string[] = [];

  if (mode === 'cache-first' && staticCacheUrl && options.method !== 'POST') {
    // 1. Static pre-built GitHub Pages cache for instant <20ms initial render
    urlsToTry.push(staticCacheUrl);
  }

  if (isLocalhost) {
    // Local dev server vite proxy works directly without CORS
    urlsToTry.push(`/api-lyceum/${cleanEndpoint}`);
  }

  // High-speed custom Cloudflare Worker proxy via /proxy-url (live data)
  const proxyBase = currentConfig.corsProxy || DEFAULT_PROXY_BASE;
  const cleanBase = proxyBase.replace(/\/+$/, '');
  const targetLyceumUrl = `https://lyceum.nstu.ru/info/api/${cleanEndpoint}`;
  urlsToTry.push(`${cleanBase}/proxy-url?url=${encodeURIComponent(targetLyceumUrl)}`);

  // Direct Lyceum API (works where CORS is not enforced or in standalone environments)
  urlsToTry.push(targetLyceumUrl);

  if (mode === 'live-first' && staticCacheUrl && !urlsToTry.includes(staticCacheUrl) && options.method !== 'POST') {
    urlsToTry.push(staticCacheUrl);
  }

  let lastError: unknown = null;
  const effectiveTimeout = timeoutMsOverride ?? (mode === 'live-only' ? 7000 : currentConfig.timeoutMs);

  for (const url of urlsToTry) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), effectiveTimeout);

    try {
      const isLiveRemote = url !== staticCacheUrl;
      const res = await fetch(url, {
        ...options,
        cache: isLiveRemote && mode !== 'cache-first' ? 'no-store' : options.cache,
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
