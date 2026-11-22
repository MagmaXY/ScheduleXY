import { useEffect } from 'react';
import { usePreferencesStore } from '../store/preferencesStore';

export function useFavicon() {
  const useMagmaLogo = usePreferencesStore((s) => s.useMagmaLogo);

  useEffect(() => {
    let link = document.querySelector<HTMLLinkElement>('#app-favicon');
    if (!link) {
      link = document.createElement('link');
      link.id = 'app-favicon';
      link.rel = 'icon';
      document.head.appendChild(link);
    }
    link.type = 'image/png';
    const logoUrl = useMagmaLogo ? 'ic_magma_logo.png' : 'ic_lyceum_logo.png';
    link.href = logoUrl;

    const appleLink = document.querySelector<HTMLLinkElement>('link[rel="apple-touch-icon"]');
    if (appleLink) {
      appleLink.href = logoUrl;
    }
  }, [useMagmaLogo]);
}
