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

  if (isLocalhost) {
    // In local dev server, vite proxy works directly without CORS
    urlsToTry.push(`/api-lyceum/${cleanEndpoint}`);
  }

  // 1. High-speed custom Cloudflare Worker proxy (no CORS issues, instant response)
  const proxyBase = currentConfig.corsProxy || DEFAULT_PROXY_BASE;
  const cleanBase = proxyBase.replace(/\/+$/, '');
  urlsToTry.push(`${cleanBase}/${cleanEndpoint}`);

  // 2. Direct Lyceum API
  urlsToTry.push(`https://lyceum.nstu.ru/info/api/${cleanEndpoint}`);

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
