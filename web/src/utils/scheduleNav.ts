/**
 * Schedule Navigation Helper
 * Resolves whether a cell date falls into current week (offset 0) or archive weeks (offset -1, -2, etc.)
 * Strictly matches Android app CalendarScreen.kt:
 * returns null if the week is not current week and not in archiveWeeks.
 */
export function resolveScheduleWeekOffset(
  cellDate: Date,
  currentScheduleWeekStart?: string | null,
  archiveWeeks: string[] = []
): { weekOffset: number; dayIndex: number } | null {
  const dayOfWeek = (cellDate.getDay() + 6) % 7; // 0=Mon..5=Sat, 6=Sun
  const cellDayIdx = dayOfWeek <= 5 ? dayOfWeek : 0;

  // Format Date to YYYY-MM-DD in local time
  const formatIso = (d: Date) => {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  // Monday of cell's week
  const cellMonday = new Date(cellDate);
  cellMonday.setDate(cellMonday.getDate() - dayOfWeek);
  const cellMondayIso = formatIso(cellMonday);

  // Active Monday
  let activeMondayIso = '';
  if (currentScheduleWeekStart && currentScheduleWeekStart.trim()) {
    activeMondayIso = currentScheduleWeekStart.trim();
  }

  if (!activeMondayIso) {
    const now = new Date();
    const nowDayOfWeek = (now.getDay() + 6) % 7;
    const nowHour = now.getHours();
    const activeMon = new Date(now);
    activeMon.setDate(activeMon.getDate() - nowDayOfWeek);
    if (now.getDay() === 0 || (now.getDay() === 6 && nowHour >= 15)) {
      activeMon.setDate(activeMon.getDate() + 7);
    }
    activeMondayIso = formatIso(activeMon);
  }

  if (cellMondayIso === activeMondayIso) {
    return { weekOffset: 0, dayIndex: cellDayIdx };
  }

  // Match strictly against available archive weeks from the API
  const archiveIdx = archiveWeeks.indexOf(cellMondayIso);
  if (archiveIdx !== -1) {
    return { weekOffset: -(archiveIdx + 1), dayIndex: cellDayIdx };
  }

  return null;
}
