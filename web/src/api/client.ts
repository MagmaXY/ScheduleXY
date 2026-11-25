// Network client with smart routing:
// 1. In dev/preview environment (Vite): uses local /api-lyceum proxy (zero CORS issues, <300ms)
// 2. In production (GitHub Pages / standalone): races direct API and configured CORS proxy

// Dedicated Cloudflare Worker CORS proxy for Lyceum NSTU
export const DEFAULT_PROXY_BASE = 'https://lyceum-proxy.dimafilippov1800.workers.dev';

export interface ClientConfig {
  corsProxy: string;
  timeoutMs: number;
}

export const defaultClientConfig: ClientConfig = {
  corsProxy: '', // Empty means use default proxy
  timeoutMs: 12000,
};

let currentConfig = { ...defaultClientConfig };

export function setClientConfig(newConfig: Partial<ClientConfig>) {
  currentConfig = { ...currentConfig, ...newConfig };
}

export function getClientConfig(): ClientConfig {
  return currentConfig;
}

function buildProxyUrl(proxyBase: string, targetUrl: string): string {
  const cleanBase = proxyBase.trim().replace(/\/+$/, '');
  if (cleanBase.includes('?url=') || cleanBase.includes('?quest=')) {
    return `${cleanBase}${encodeURIComponent(targetUrl)}`;
  }
  if (cleanBase.endsWith('/proxy-url')) {
    return `${cleanBase}?url=${encodeURIComponent(targetUrl)}`;
  }
  return `${cleanBase}/proxy-url?url=${encodeURIComponent(targetUrl)}`;
}

async function fetchJsonWithTimeout<T>(
  url: string,
  options: RequestInit,
  timeoutMs: number,
  externalSignal?: AbortSignal
): Promise<T> {
  const controller = new AbortController();
  const onExternalAbort = () => controller.abort();
  if (externalSignal) {
    if (externalSignal.aborted) controller.abort();
    else externalSignal.addEventListener('abort', onExternalAbort, { once: true });
  }

  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const res = await fetch(url, {
      ...options,
      cache: 'no-store',
      signal: controller.signal,
      headers: {
        Accept: 'application/json',
        ...(options.headers || {}),
      },
    });
    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }
    return (await res.json()) as T;
  } finally {
    clearTimeout(timer);
    if (externalSignal) {
      externalSignal.removeEventListener('abort', onExternalAbort);
    }
  }
}

export async function fetchWithProxy<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const cleanEndpoint = endpoint.replace(/^\//, '');

  const isLocalhost =
    typeof window !== 'undefined' &&
    (window.location.hostname === 'localhost' ||
      window.location.hostname === '127.0.0.1' ||
      window.location.hostname.startsWith('192.168.') ||
      window.location.hostname.startsWith('26.'));

  // 1. In local dev server, vite /api-lyceum proxy hits lyceum.nstu.ru directly without CORS
  if (isLocalhost) {
    try {
      return await fetchJsonWithTimeout<T>(
        `/api-lyceum/${cleanEndpoint}`,
        options,
        currentConfig.timeoutMs
      );
    } catch {
      // Fallback to remote proxy/direct (e.g. during vite preview where dev proxy is inactive)
    }
  }

  // 2. Race direct Lyceum API against configured CORS proxy:
  // - If CORS is allowed/disabled (WebView/desktop/extension), direct wins in ~70-300ms
  // - Otherwise direct fails CORS in ~100ms and Promise.any keeps waiting on the proxy
  const targetLyceumUrl = `https://lyceum.nstu.ru/info/api/${cleanEndpoint}`;
  const proxyUrl = buildProxyUrl(
    currentConfig.corsProxy || DEFAULT_PROXY_BASE,
    targetLyceumUrl
  );

  const raceController = new AbortController();
  try {
    const result = await Promise.any([
      fetchJsonWithTimeout<T>(
        targetLyceumUrl,
        options,
        currentConfig.timeoutMs,
        raceController.signal
      ),
      fetchJsonWithTimeout<T>(
        proxyUrl,
        options,
        currentConfig.timeoutMs,
        raceController.signal
      ),
    ]);
    raceController.abort();
    return result;
  } catch (err) {
    raceController.abort();
    throw err instanceof Error ? err : new Error('Failed to fetch schedule data');
  }
}
