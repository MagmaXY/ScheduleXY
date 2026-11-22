export interface LessonBell {
  num: number;
  startHour: number;
  startMinute: number;
  endHour: number;
  endMinute: number;
  startMinutes: number;
  endMinutes: number;
  timeRange: string;
}

export function createBell(
  num: number,
  startHour: number,
  startMinute: number,
  endHour: number,
  endMinute: number
): LessonBell {
  const startMinutes = startHour * 60 + startMinute;
  const endMinutes = endHour * 60 + endMinute;
  const timeRange = `${String(startHour).padStart(2, '0')}:${String(startMinute).padStart(2, '0')}–${String(endHour).padStart(2, '0')}:${String(endMinute).padStart(2, '0')}`;
  return {
    num,
    startHour,
    startMinute,
    endHour,
    endMinute,
    startMinutes,
    endMinutes,
    timeRange,
  };
}

export const BELLS: LessonBell[] = [
  createBell(1, 8, 15, 9, 0),
  createBell(2, 9, 10, 9, 55),
  createBell(3, 10, 15, 11, 0),
  createBell(4, 11, 15, 12, 0),
  createBell(5, 12, 10, 12, 55),
  createBell(6, 13, 5, 13, 50),
  createBell(7, 14, 0, 14, 40),
  createBell(8, 14, 55, 15, 35),
  createBell(9, 15, 50, 16, 30),
  createBell(10, 16, 40, 17, 20),
  createBell(11, 17, 30, 18, 10),
  createBell(12, 18, 20, 19, 0),
];

export type BellState =
  | { type: 'before_school'; firstLessonStart: string; minutesUntil: number }
  | { type: 'in_lesson'; num: number; timeRange: string; minutesLeft: number; progress: number }
  | { type: 'break'; nextNum: number; minutesLeft: number; nextStart: string; progress: number }
  | { type: 'after_school' }
  | { type: 'weekend' }
  | { type: 'vacation' };

export function getBellDisplayText(state: BellState, isPair = false): string {
  switch (state.type) {
    case 'before_school':
      return isPair
        ? `До 1 пары: ${state.minutesUntil} мин`
        : `До 1 урока: ${state.minutesUntil} мин`;
    case 'in_lesson':
      return isPair
        ? `${state.num} пара (до звонка: ${state.minutesLeft} мин)`
        : `${state.num} урок (до звонка: ${state.minutesLeft} мин)`;
    case 'break':
      return isPair
        ? `Перемена до ${state.nextNum} пары (осталось: ${state.minutesLeft} мин)`
        : `Перемена до ${state.nextNum} урока (осталось: ${state.minutesLeft} мин)`;
    case 'after_school':
      return isPair ? 'Пары на сегодня завершены' : 'Уроки на сегодня завершены';
    case 'weekend':
      return 'Занятий нет';
    case 'vacation':
      return 'Каникулы — занятий нет';
  }
}

export function getCurrentBellState(
  date: Date = new Date(),
  isVacation = false
): BellState {
  if (isVacation) {
    return { type: 'vacation' };
  }

  const dayOfWeek = date.getDay(); // 0 is Sunday, 6 is Saturday
  if (dayOfWeek === 0) {
    return { type: 'weekend' };
  }

  const currentMinutes = date.getHours() * 60 + date.getMinutes();
  const isSaturday = dayOfWeek === 6;
  const activeBells = isSaturday ? BELLS.slice(0, 8) : BELLS;

  const firstBell = activeBells[0];
  if (currentMinutes < firstBell.startMinutes) {
    return {
      type: 'before_school',
      firstLessonStart: `${String(firstBell.startHour).padStart(2, '0')}:${String(firstBell.startMinute).padStart(2, '0')}`,
      minutesUntil: firstBell.startMinutes - currentMinutes,
    };
  }

  const lastBell = activeBells[activeBells.length - 1];
  if (currentMinutes >= lastBell.endMinutes) {
    return { type: 'after_school' };
  }

  for (let i = 0; i < activeBells.length; i++) {
    const bell = activeBells[i];
    if (currentMinutes >= bell.startMinutes && currentMinutes < bell.endMinutes) {
      const totalDuration = bell.endMinutes - bell.startMinutes;
      const elapsed = currentMinutes - bell.startMinutes;
      const progress = Math.min(1, Math.max(0, elapsed / totalDuration));
      return {
        type: 'in_lesson',
        num: bell.num,
        timeRange: bell.timeRange,
        minutesLeft: bell.endMinutes - currentMinutes,
        progress,
      };
    }

    if (i < activeBells.length - 1) {
      const nextBell = activeBells[i + 1];
      if (currentMinutes >= bell.endMinutes && currentMinutes < nextBell.startMinutes) {
        const breakDuration = nextBell.startMinutes - bell.endMinutes;
        const breakElapsed = currentMinutes - bell.endMinutes;
        const breakProgress =
          breakDuration > 0
            ? Math.min(1, Math.max(0, breakElapsed / breakDuration))
            : 0;
        return {
          type: 'break',
          nextNum: nextBell.num,
          minutesLeft: nextBell.startMinutes - currentMinutes,
          nextStart: `${String(nextBell.startHour).padStart(2, '0')}:${String(nextBell.startMinute).padStart(2, '0')}`,
          progress: breakProgress,
        };
      }
    }
  }

  return { type: 'after_school' };
}

/**
 * Monday = 0, Tuesday = 1, Wednesday = 2, Thursday = 3, Friday = 4, Saturday = 5, Sunday = 6
 */
export function getScheduleDayIndex(date: Date = new Date()): number {
  const d = date.getDay();
  return d === 0 ? 6 : d - 1;
}

export function getDefaultScheduleDayIndex(date: Date = new Date()): number {
  const dayOfWeek = date.getDay(); // 0 = Sun, 6 = Sat
  const hour = date.getHours();

  if (dayOfWeek === 0) {
    return 0; // Monday
  }
  if (dayOfWeek === 6 && hour >= 15) {
    return 0; // Monday for upcoming week
  }

  const currentMinutes = date.getHours() * 60 + date.getMinutes();
  const isMorningOrNight = currentMinutes < 8 * 60 + 15;

  const bellState = getCurrentBellState(date);
  const isAfterSchool = !isMorningOrNight && bellState.type === 'after_school';
  const dayIdx = getScheduleDayIndex(date);

  if (isAfterSchool) {
    switch (dayIdx) {
      case 0: return 1;
      case 1: return 2;
      case 2: return 3;
      case 3: return 4;
      case 4: return 5;
      case 5: return 0;
      default: return 0;
    }
  }

  return Math.min(5, Math.max(0, dayIdx));
}
