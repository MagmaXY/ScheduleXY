import { useEffect, useState } from 'react';

export function useIOSInstallPrompt() {
  const [showPrompt, setShowPrompt] = useState(false);

  useEffect(() => {
    // Check if on iOS Safari
    const ua = window.navigator.userAgent;
    const isIOS = /iPad|iPhone|iPod/.test(ua) && !(window as any).MSStream;
    const isSafari = /Safari/.test(ua) && !/CriOS|FxiOS|OPiOS/.test(ua);
    const isStandalone =
      window.matchMedia('(display-mode: standalone)').matches ||
      (window.navigator as any).standalone === true;

    const dismissed = localStorage.getItem('ios_install_prompt_dismissed');

    if (isIOS && isSafari && !isStandalone && !dismissed) {
      // Delay showing banner slightly for better UX
      const timer = setTimeout(() => setShowPrompt(true), 2500);
      return () => clearTimeout(timer);
    }
  }, []);

  const dismiss = () => {
    localStorage.setItem('ios_install_prompt_dismissed', 'true');
    setShowPrompt(false);
  };

  return { showPrompt, dismiss };
}
