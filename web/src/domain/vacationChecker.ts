import { cacheManager } from '../cache/cacheManager';

let cachedVacationDates: Set<string> | null = null;
let lastCheckTime = 0;

function isDefaultVacationDate(dateStr: string): boolean {
  return (
    (dateStr >= '2026-11-01' && dateStr <= '2026-11-08') ||
    (dateStr >= '2026-12-30' && dateStr <= '2027-01-11') ||
    (dateStr >= '2027-03-08' && dateStr <= '2027-03-15') ||
    (dateStr >= '2027-06-01' && dateStr <= '2027-08-31')
  );
}

export async function isVacation(date: Date = new Date()): Promise<boolean> {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const dateStr = `${y}-${m}-${d}`;

  const now = Date.now();
  if (!cachedVacationDates || now - lastCheckTime > 300000) {
    try {
      const events = await cacheManager.getEvents();
      if (events?.items && events.items.length > 0) {
        cachedVacationDates = new Set(
          events.items
            .filter(
              (ev) =>
                ev.color === 'orange' ||
                ev.title.toLowerCase().includes('каникул')
            )
            .map((ev) => ev.date)
        );
        lastCheckTime = now;
      }
    } catch {
      // fallback
    }
  }

  if (cachedVacationDates?.has(dateStr)) {
    return true;
  }

  return isDefaultVacationDate(dateStr);
}
