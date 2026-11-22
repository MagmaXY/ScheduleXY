import { BellState } from './bellSchedule';

export interface LessonPair {
  pairNum: number;
  timeRange: string;
  startHour: number;
  startMinute: number;
  endHour: number;
  endMinute: number;
  romanNum: string;
  startMinutes: number;
  endMinutes: number;
}

export function createPair(
  pairNum: number,
  timeRange: string,
  startHour: number,
  startMinute: number,
  endHour: number,
  endMinute: number,
  romanNum: string
): LessonPair {
  return {
    pairNum,
    timeRange,
    startHour,
    startMinute,
    endHour,
    endMinute,
    romanNum,
    startMinutes: startHour * 60 + startMinute,
    endMinutes: endHour * 60 + endMinute,
  };
}

export const PAIRS: LessonPair[] = [
  createPair(1, '8:30 – 10:00', 8, 30, 10, 0, 'I'),
  createPair(2, '10:15 – 11:45', 10, 15, 11, 45, 'II'),
  createPair(3, '12:10 – 13:40', 12, 10, 13, 40, 'III'),
  createPair(4, '14:00 – 15:30', 14, 0, 15, 30, 'IV'),
];

export function getCurrentPairState(
  date: Date = new Date(),
  isVacation = false
): BellState {
  if (isVacation) return { type: 'vacation' };
  const dayOfWeek = date.getDay();
  if (dayOfWeek === 0) return { type: 'weekend' };

  const currentMinutes = date.getHours() * 60 + date.getMinutes();
  const activePairs = PAIRS;

  const firstPair = activePairs[0];
  if (currentMinutes < firstPair.startMinutes) {
    return {
      type: 'before_school',
      firstLessonStart: `${String(firstPair.startHour).padStart(2, '0')}:${String(firstPair.startMinute).padStart(2, '0')}`,
      minutesUntil: firstPair.startMinutes - currentMinutes,
    };
  }

  const lastPair = activePairs[activePairs.length - 1];
  if (currentMinutes >= lastPair.endMinutes) {
    return { type: 'after_school' };
  }

  for (let i = 0; i < activePairs.length; i++) {
    const pair = activePairs[i];
    if (currentMinutes >= pair.startMinutes && currentMinutes < pair.endMinutes) {
      const totalDuration = pair.endMinutes - pair.startMinutes;
      const elapsed = currentMinutes - pair.startMinutes;
      const progress = Math.min(1, Math.max(0, elapsed / totalDuration));
      return {
        type: 'in_lesson',
        num: pair.pairNum,
        timeRange: pair.timeRange,
        minutesLeft: pair.endMinutes - currentMinutes,
        progress,
      };
    }

    if (i < activePairs.length - 1) {
      const nextPair = activePairs[i + 1];
      if (currentMinutes >= pair.endMinutes && currentMinutes < nextPair.startMinutes) {
        const breakDuration = nextPair.startMinutes - pair.endMinutes;
        const breakElapsed = currentMinutes - pair.endMinutes;
        const breakProgress =
          breakDuration > 0
            ? Math.min(1, Math.max(0, breakElapsed / breakDuration))
            : 0;
        return {
          type: 'break',
          nextNum: nextPair.pairNum,
          minutesLeft: nextPair.startMinutes - currentMinutes,
          nextStart: `${String(nextPair.startHour).padStart(2, '0')}:${String(nextPair.startMinute).padStart(2, '0')}`,
          progress: breakProgress,
        };
      }
    }
  }

  return { type: 'after_school' };
}

export function isSeniorTarget(targetType: string, targetName: string): boolean {
  if (targetType !== 'class') return false;
  const clean = targetName.trim();
  return clean.startsWith('9') || clean.startsWith('10') || clean.startsWith('11');
}

export function getPairTimeRange(pairIndex: number): string {
  return PAIRS[pairIndex - 1]?.timeRange || '';
}

export function getPairRomanNum(pairIndex: number): string {
  return PAIRS[pairIndex - 1]?.romanNum || String(pairIndex);
}

export function getSplitLessonTimes(
  pairIndex: number,
  pairTimes: string
): [{ start: string; end: string }, { start: string; end: string }] {
  const pair = PAIRS[pairIndex - 1];
  let startMinutes = pair ? pair.startMinutes : 8 * 60 + 30;
  if (!pair) {
    const parts = pairTimes.split(/–|-/);
    const s = parts[0]?.trim() || '8:30';
    const [sh, sm] = s.split(':').map((x) => parseInt(x, 10));
    if (!isNaN(sh) && !isNaN(sm)) {
      startMinutes = sh * 60 + sm;
    }
  }

  const l1Start = startMinutes;
  const l1End = startMinutes + 45;
  const l2Start = l1End;
  const l2End = startMinutes + 90;

  const formatMin = (min: number) => {
    const h = Math.floor(min / 60);
    const m = min % 60;
    return `${h}:${String(m).padStart(2, '0')}`;
  };

  return [
    { start: formatMin(l1Start), end: formatMin(l1End) },
    { start: formatMin(l2Start), end: formatMin(l2End) },
  ];
}
