// Network client with smart routing:
// 1. In dev/preview environment (Vite): uses local /api-lyceum proxy (zero CORS issues!)
// 2. In production (GitHub Pages): tries user configurable proxy or public CORS proxies

export interface ClientConfig {
  corsProxy: string;
  timeoutMs: number;
}

export const defaultClientConfig: ClientConfig = {
  corsProxy: '', // Empty means direct or vite local proxy
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

  // If user configured a custom CORS proxy
  if (currentConfig.corsProxy) {
    urlsToTry.push(
      `${currentConfig.corsProxy}${encodeURIComponent(`https://lyceum.nstu.ru/info/api/${cleanEndpoint}`)}`
    );
  }

  // 1. Direct Lyceum API (works when accessed directly or if server allows CORS)
  urlsToTry.push(`https://lyceum.nstu.ru/info/api/${cleanEndpoint}`);

  // 2. High-speed open CORS proxy: AllOrigins (Cloudflare powered, tested working)
  urlsToTry.push(
    `https://api.allorigins.win/raw?url=${encodeURIComponent(`https://lyceum.nstu.ru/info/api/${cleanEndpoint}`)}`
  );

  // 3. Fallback proxy: CodeTabs
  urlsToTry.push(
    `https://api.codetabs.com/v1/proxy?quest=${encodeURIComponent(`https://lyceum.nstu.ru/info/api/${cleanEndpoint}`)}`
  );

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
