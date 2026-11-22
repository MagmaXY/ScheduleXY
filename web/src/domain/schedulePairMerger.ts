import { Lesson } from '../types/api';
import { getPairRomanNum, getPairTimeRange } from './pairSchedule';

export type ScheduleItemUi =
  | { type: 'single'; lesson: Lesson }
  | {
      type: 'pair';
      pairNum: number;
      pairRoman: string;
      pairTimes: string;
      firstLesson: Lesson;
      secondLesson: Lesson;
    };

export function processLessons(
  lessons: Lesson[],
  isTeacherMode: boolean,
  targetName: string,
  showPairsSetting: boolean
): ScheduleItemUi[] {
  if (!showPairsSetting || lessons.length === 0) {
    return lessons.map((l) => ({ type: 'single', lesson: l }));
  }

  const isSeniorClass =
    !isTeacherMode &&
    (targetName.startsWith('9') ||
      targetName.startsWith('10') ||
      targetName.startsWith('11'));

  const result: ScheduleItemUi[] = [];
  const byNum = new Map<number, Lesson>();
  lessons.forEach((l) => byNum.set(l.num, l));

  const pairSlots = [
    [1, 2],
    [3, 4],
    [5, 6],
    [7, 8],
  ];

  const processedNums = new Set<number>();

  for (let pIndex = 0; pIndex < pairSlots.length; pIndex++) {
    const [num1, num2] = pairSlots[pIndex];
    const l1 = byNum.get(num1);
    const l2 = byNum.get(num2);
    const pairNum = pIndex + 1;

    if (l1 && l2) {
      let canPair = false;
      if (isSeniorClass) {
        canPair = true;
      } else if (isTeacherMode) {
        const c1 = Array.from(
          new Set(
            l1.subgroups.map((s) => s.className?.trim()).filter(Boolean)
          )
        );
        const c2 = Array.from(
          new Set(
            l2.subgroups.map((s) => s.className?.trim()).filter(Boolean)
          )
        );
        const common = c1.filter((c) => c2.includes(c));
        canPair =
          common.length > 0 &&
          common.some(
            (c) =>
              c?.startsWith('9') || c?.startsWith('10') || c?.startsWith('11')
          );
      }

      if (canPair) {
        result.push({
          type: 'pair',
          pairNum,
          pairRoman: getPairRomanNum(pairNum),
          pairTimes: getPairTimeRange(pairNum),
          firstLesson: l1,
          secondLesson: l2,
        });
        processedNums.add(num1);
        processedNums.add(num2);
      }
    }
  }

  // Add leftovers
  for (const lesson of lessons) {
    if (!processedNums.has(lesson.num)) {
      result.push({ type: 'single', lesson });
    }
  }

  return result.sort((a, b) => {
    const numA = a.type === 'single' ? a.lesson.num : a.firstLesson.num;
    const numB = b.type === 'single' ? b.lesson.num : b.firstLesson.num;
    return numA - numB;
  });
}
