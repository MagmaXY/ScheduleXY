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
