import { useEffect, useState } from 'react';

const WEEK_VIEW_QUERY = '(min-width: 1024px), (orientation: landscape)';

/**
 * Returns true on desktop viewports (>= 1024px) or mobile devices in landscape orientation.
 */
export function useCanUseWeekView(): boolean {
  const [canUse, setCanUse] = useState<boolean>(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return false;
    }
    return window.matchMedia(WEEK_VIEW_QUERY).matches;
  });

  useEffect(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return;
    }
    const mql = window.matchMedia(WEEK_VIEW_QUERY);
    const update = () => setCanUse(mql.matches);
    update();

    if (typeof mql.addEventListener === 'function') {
      mql.addEventListener('change', update);
      return () => mql.removeEventListener('change', update);
    }
    mql.addListener(update);
    return () => mql.removeListener(update);
  }, []);

  return canUse;
}
