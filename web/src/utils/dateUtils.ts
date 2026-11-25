/**
 * Converts a Date object or timestamp to local "YYYY-MM-DD" string
 * Avoids any UTC / toISOString timezone shifts (+1 day or -1 day)
 */
export function toLocalDateIso(date: Date = new Date()): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Parses a "YYYY-MM-DD" string into a local Date object set to 12:00:00 (noon)
 * Setting noon prevents any daylight saving or timezone boundary flips
 */
export function parseLocalDate(isoStr: string): Date {
  const parts = isoStr.split('-');
  if (parts.length === 3) {
    const y = parseInt(parts[0], 10);
    const m = parseInt(parts[1], 10) - 1;
    const d = parseInt(parts[2], 10);
    return new Date(y, m, d, 12, 0, 0);
  }
  return new Date();
}

/**
 * Adds days to an ISO date string in local time
 */
export function addDaysToIso(isoStr: string, days: number): string {
  const d = parseLocalDate(isoStr);
  d.setDate(d.getDate() + days);
  return toLocalDateIso(d);
}

const GENITIVE_MONTHS = [
  'января',
  'февраля',
  'марта',
  'апреля',
  'мая',
  'июня',
  'июля',
  'августа',
  'сентября',
  'октября',
  'ноября',
  'декабря',
];

/**
 * Formats a Monday-Saturday week range with genitive Russian month declension,
 * e.g. "23–28 ноября" or "30 ноября–5 декабря".
 */
export function formatWeekRangeGenitive(monday: Date): string {
  const sat = new Date(monday);
  sat.setDate(monday.getDate() + 5);
  const m1 = GENITIVE_MONTHS[monday.getMonth()] || '';
  const m2 = GENITIVE_MONTHS[sat.getMonth()] || '';
  return m1 === m2
    ? `${monday.getDate()}–${sat.getDate()} ${m2}`
    : `${monday.getDate()} ${m1}–${sat.getDate()} ${m2}`;
}

/**
 * Formats a single date with genitive month declension, e.g. "23 ноября".
 */
export function formatFullDateGenitive(date: Date): string {
  return `${date.getDate()} ${GENITIVE_MONTHS[date.getMonth()] || ''}`;
}
