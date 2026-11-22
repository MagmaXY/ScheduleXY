import {
  ScheduleResponse,
  normalizeSubject,
  getChangesForLesson,
  isCancelledLesson,
  Subgroup,
} from '../types/api';
import { ScheduleNote, SlotType } from '../types/notes';
import { BELLS } from '../domain/bellSchedule';
import { toLocalDateIso, parseLocalDate } from './dateUtils';

/**
 * Checks if a slot on a given date is in the past.
 */
export function isSlotInPast(
  selectedDateIso: string,
  slot: SlotType,
  lsnNum: number | null
): boolean {
  const todayIso = toLocalDateIso();
  if (selectedDateIso < todayIso) return true;
  if (selectedDateIso > todayIso) return false;

  const now = new Date();
  const nowMinutes = now.getHours() * 60 + now.getMinutes();

  if (slot === 'before_lessons') return nowMinutes >= 8 * 60 + 30;
  if (slot === 'after_lessons') return nowMinutes >= 21 * 60;
  if (slot === 'lesson' && lsnNum !== null) {
    const bell = BELLS.find((b) => b.num === lsnNum);
    if (bell) return nowMinutes >= bell.startMinutes;
  }
  return false;
}

/**
 * Returns ISO date for a day index (0=Mon..5=Sat) in the given week.
 */
export function getDateIsoForDayIndex(
  weekStart: string | null | undefined,
  dayIndex: number
): string {
  if (weekStart && weekStart.trim()) {
    try {
      const cal = parseLocalDate(weekStart);
      cal.setDate(cal.getDate() + dayIndex);
      return toLocalDateIso(cal);
    } catch {}
  }
  const now = new Date();
  const dow = (now.getDay() + 6) % 7;
  now.setDate(now.getDate() - dow + dayIndex);
  return toLocalDateIso(now);
}

/**
 * Finds the matching change for a specific subgroup in a lesson.
 */
export function findChangeForSubgroup(
  sg: Subgroup,
  changes: ReturnType<typeof getChangesForLesson>,
  allSubgroups: Subgroup[]
) {
  if (changes.length === 0) return undefined;

  const sgNum = sg.label?.replace(/\D/g, '');
  const sgTeacherNorm = sg.teacher?.trim().toLowerCase() || '';
  const sgRoomNorm = sg.room?.trim().replace(/^\(|\)$/g, '').toLowerCase() || '';

  // 1. Exact match by group digits
  if (sgNum) {
    const groupMatch = changes.find((c) => {
      const cNum = (c.group || c.subgroup || c.label)?.replace(/\D/g, '');
      return cNum === sgNum;
    });
    if (groupMatch) return groupMatch;
  }

  // 2. Match by exact teacher
  if (sgTeacherNorm) {
    const teacherMatch = changes.find(
      (c) => c.teacher?.trim().toLowerCase() === sgTeacherNorm
    );
    if (teacherMatch) return teacherMatch;
  }

  // 3. Match by exact room
  if (sgRoomNorm) {
    const roomMatch = changes.find((c) => {
      const cRoom = c.room?.trim().replace(/^\(|\)$/g, '').toLowerCase() || '';
      return cRoom === sgRoomNorm;
    });
    if (roomMatch) return roomMatch;
  }

  // 4. Fallback for single change with no group digits
  const singleChange = changes.find((c) => {
    const cNum = (c.group || c.subgroup || c.label)?.replace(/\D/g, '');
    return !cNum;
  });

  if (singleChange && allSubgroups.length > 1) {
    const changeTeacher = singleChange.teacher?.trim().toLowerCase() || '';
    const changeRoom = singleChange.room?.trim().replace(/^\(|\)$/g, '').toLowerCase() || '';

    const belongsToOther = allSubgroups.some((other) => {
      if (other === sg) return false;
      const otherTeacher = other.teacher?.trim().toLowerCase() || '';
      const otherRoom = other.room?.trim().replace(/^\(|\)$/g, '').toLowerCase() || '';
      return (
        (changeTeacher && otherTeacher === changeTeacher) ||
        (changeRoom && otherRoom === changeRoom)
      );
    });

    if (belongsToOther) {
      return undefined;
    }
  }

  return singleChange;
}

/**
 * Resolves the effective subject for a given lesson number and date in ISO format (YYYY-MM-DD),
 * taking into account:
 *  - Timetable base lessons
 *  - Subgroups and technology subgroup for middle school
 *  - Cancellations (returns null if cancelled)
 *  - Replacements (returns replacement subject if changed)
 */
export function getEffectiveLessonSubject(
  schedule: ScheduleResponse | null | undefined,
  dateIso: string,
  lessonNum: number,
  userSubgroup: number = 1,
  userTechSubgroup: number = 1,
  isMiddleSchool: Boolean = false
): string | null {
  if (!schedule || !schedule.days || schedule.days.length === 0) return null;

  try {
    const parts = dateIso.split('-');
    if (parts.length !== 3) return null;
    const d = new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
    const dow = (d.getDay() + 6) % 7;
    if (dow > 5) return null; // Sunday

    const day = schedule.days.find((x) => x.dayIndex === dow);
    if (!day) return null;
    const lesson = day.lessons.find((l) => l.num === lessonNum);
    if (!lesson) return null;

    const changes = getChangesForLesson(schedule, dow, lessonNum);

    // Check total cancellation
    const isTotalCancelled = (() => {
      if (changes.length === 0) return false;
      if (lesson.subgroups.length === 0) return changes.some((c) => isCancelledLesson(c));
      if (lesson.subgroups.length <= 1) return changes.some((c) => isCancelledLesson(c));

      const hasGeneralCancel = changes.some(
        (it) =>
          isCancelledLesson(it) &&
          !it.group?.trim() &&
          !it.subgroup?.trim() &&
          !it.label?.trim()
      );
      if (hasGeneralCancel) return true;

      return lesson.subgroups.every((sg) => {
        const c = findChangeForSubgroup(sg, changes, lesson.subgroups);
        return c ? isCancelledLesson(c) : false;
      });
    })();

    if (isTotalCancelled) return null;

    // Subgroup match
    const isTech =
      lesson.subgroups.some((it) =>
        it.subject?.toLowerCase().includes('технолог')
      ) && Boolean(isMiddleSchool);
    const targetGroup = isTech ? userTechSubgroup : userSubgroup;

    const userSg =
      lesson.subgroups.length > 1
        ? lesson.subgroups.find((sg) => {
            const digit = sg.label ? parseInt(sg.label.replace(/\D/g, ''), 10) : null;
            return digit === targetGroup;
          }) || lesson.subgroups[0]
        : lesson.subgroups[0];

    if (userSg) {
      const sgChange = findChangeForSubgroup(userSg, changes, lesson.subgroups);
      if (sgChange) {
        if (isCancelledLesson(sgChange)) return null;
        if (
          sgChange.subject &&
          sgChange.subject.trim() &&
          sgChange.subject !== 'F' &&
          sgChange.subject !== '$' &&
          !sgChange.subject.startsWith('$')
        ) {
          return normalizeSubject(sgChange.subject);
        }
      }

      const subj = userSg.subject?.trim();
      if (subj && subj !== '—' && subj.toLowerCase() !== 'урок отменен') {
        return normalizeSubject(subj);
      }
      return null;
    }

    // Fallback replacement change
    const repl = changes.find((it) => !isCancelledLesson(it) && it.subject?.trim());
    if (repl?.subject) {
      return normalizeSubject(repl.subject);
    }

    const disp = lesson.subgroups[0]?.subject?.trim();
    if (disp && disp !== '—' && disp.toLowerCase() !== 'урок отменен') {
      return normalizeSubject(disp);
    }

    return null;
  } catch {
    return null;
  }
}

/**
 * Returns subject for lesson on date or '' if not found or cancelled.
 */
export function getSubjectForLessonOnDate(
  schedule: ScheduleResponse | null | undefined,
  num: number,
  dateIso: string,
  targetName: string,
  studentSubgroup: number = 1,
  studentTechnologySubgroup: number = 1
): string {
  const isMiddleSchool = /^[5-8]/.test(targetName.trim());
  return (
    getEffectiveLessonSubject(
      schedule,
      dateIso,
      num,
      studentSubgroup,
      studentTechnologySubgroup,
      isMiddleSchool
    ) || ''
  );
}

/**
 * Phase 1: Finds the next non-cancelled lesson of the given subject
 * strictly in the remainder of the current week (from fromDateIso through Saturday).
 *
 * @returns [targetDateIso, lessonNum] or null if no lessons remain this week.
 */
export function findNextLessonInCurrentWeek(
  schedule: ScheduleResponse | null | undefined,
  subject: string,
  targetSubgroup: number | null = null,
  fromDateIso: string,
  afterLessonNum: number | null = null,
  includeSameDayBefore: boolean = false,
  userTechSubgroup: number = 1,
  isMiddleSchool: boolean = false
): [string, number] | null {
  if (!schedule || !schedule.days || schedule.days.length === 0) return null;
  const normTarget = normalizeSubject(subject).toLowerCase().trim();
  if (!normTarget) return null;

  try {
    const cal = parseLocalDate(fromDateIso);
    const fromDow = (cal.getDay() + 6) % 7;
    if (fromDow > 5) return null; // Sunday

    const todayIso = toLocalDateIso();

    for (let dow = fromDow; dow <= 5; dow++) {
      const checkCal = new Date(cal);
      checkCal.setDate(cal.getDate() + (dow - fromDow));
      const dateIso = toLocalDateIso(checkCal);

      const daySchedule = schedule.days.find((it) => it.dayIndex === dow);
      if (!daySchedule) continue;

      const sortedLessons = [...daySchedule.lessons].sort((a, b) => a.num - b.num);
      for (const lesson of sortedLessons) {
        if (dow === fromDow) {
          if (!includeSameDayBefore && afterLessonNum !== null && lesson.num <= afterLessonNum) {
            continue;
          }
          if (dateIso === todayIso && isSlotInPast(todayIso, 'lesson', lesson.num)) {
            continue;
          }
          if (afterLessonNum !== null && lesson.num === afterLessonNum) {
            continue;
          }
        }

        const effSubj = getEffectiveLessonSubject(
          schedule,
          dateIso,
          lesson.num,
          targetSubgroup || 1,
          userTechSubgroup,
          isMiddleSchool
        );

        if (effSubj) {
          const normSubj = normalizeSubject(effSubj).toLowerCase().trim();
          const matches =
            normSubj === normTarget ||
            normSubj.includes(normTarget) ||
            normTarget.includes(normSubj);

          if (matches) {
            return [dateIso, lesson.num];
          }
        }
      }
    }
  } catch {
    return null;
  }

  return null;
}

/**
 * Finds the closest non-cancelled occurrence of a subject in a full week's schedule
 * starting from fromDayIndex (0=Mon..5=Sat).
 */
export function findClosestLessonInWeek(
  schedule: ScheduleResponse,
  subject: string,
  targetSubgroup: number | null = null,
  fromDayIndex: number = 0,
  userSubgroup: number = 1,
  userTechSubgroup: number = 1,
  isMiddleSchool: boolean = false
): [string, number] | null {
  if (!schedule.days || schedule.days.length === 0) return null;
  const normTarget = normalizeSubject(subject).toLowerCase().trim();
  if (!normTarget) return null;

  for (let dayIdx = fromDayIndex; dayIdx <= 5; dayIdx++) {
    const dateIso = getDateIsoForDayIndex(schedule.weekStart, dayIdx);
    const daySchedule = schedule.days.find((it) => it.dayIndex === dayIdx);
    if (!daySchedule) continue;

    const sortedLessons = [...daySchedule.lessons].sort((a, b) => a.num - b.num);
    for (const lesson of sortedLessons) {
      const effSubj = getEffectiveLessonSubject(
        schedule,
        dateIso,
        lesson.num,
        targetSubgroup || userSubgroup,
        userTechSubgroup,
        isMiddleSchool
      );

      if (effSubj) {
        const normSubj = normalizeSubject(effSubj).toLowerCase().trim();
        const matches =
          normSubj === normTarget ||
          normSubj.includes(normTarget) ||
          normTarget.includes(normSubj);

        if (matches) {
          return [dateIso, lesson.num];
        }
      }
    }
  }
  return null;
}

/**
 * Phase 2: Resolves pending weekly transfers (transferPending === true) against the loaded schedule.
 * If found in an upcoming non-cancelled lesson this week, the note date & lessonNum are updated
 * and transferPending is cleared.
 * If not found (e.g. week parity alternation / subject once every 2 weeks), the note remains transferPending = true.
 */
export function resolvePendingTransfers(
  allNotes: ScheduleNote[],
  schedule: ScheduleResponse | null | undefined,
  userSubgroup: number = 1,
  userTechSubgroup: number = 1,
  isMiddleSchool: boolean = false
): ScheduleNote[] {
  if (!schedule) return allNotes;
  const target = schedule.className || schedule.teacherName;
  if (!target) return allNotes;

  const pendingNotes = allNotes.filter(
    (it) => it.targetName === target && it.transferPending && !it.isCompleted
  );
  if (pendingNotes.length === 0) return allNotes;

  // Determine starting day of week
  let fromDay = 0;
  if (schedule.weekStart) {
    const now = new Date();
    const curMonday = getDateIsoForDayIndex(schedule.weekStart, 0);
    // If schedule matches current week, start from today
    const dow = (now.getDay() + 6) % 7;
    const expectedMonday = getDateIsoForDayIndex(null, 0);
    if (curMonday === expectedMonday) {
      fromDay = Math.min(Math.max(dow, 0), 5);
    }
  }

  const updatedMap = new Map<string, ScheduleNote>();

  for (const note of pendingNotes) {
    if (!note.subject?.trim()) continue;

    const closest = findClosestLessonInWeek(
      schedule,
      note.subject,
      note.subgroup || null,
      fromDay,
      userSubgroup,
      userTechSubgroup,
      isMiddleSchool
    );

    if (closest) {
      updatedMap.set(note.id, {
        ...note,
        date: closest[0],
        lessonNum: closest[1],
        transferPending: false,
      });
    }
  }

  if (updatedMap.size === 0) return allNotes;

  return allNotes.map((n) => updatedMap.get(n.id) || n);
}

/**
 * Phase 3: Checks and relocates cancelled homework notes for the schedule's target class/teacher.
 * - If a lesson on/after today was cancelled or replaced with another subject:
 *   Relocates to the next non-cancelled lesson this week, or sets transferPending = true.
 * - If an earlier lesson of this subject appeared due to changes:
 *   Relocates earlier to that lesson.
 */
export function checkAndRelocateCancelledHomework(
  allNotes: ScheduleNote[],
  schedule: ScheduleResponse | null | undefined,
  userSubgroup: number = 1,
  userTechSubgroup: number = 1,
  isMiddleSchool: boolean = false
): { updatedNotes: ScheduleNote[]; relocatedInfo: { note: ScheduleNote; message: string }[] } {
  if (!schedule || !schedule.days || schedule.days.length === 0) {
    return { updatedNotes: allNotes, relocatedInfo: [] };
  }
  const target = schedule.className || schedule.teacherName;
  if (!target) return { updatedNotes: allNotes, relocatedInfo: [] };

  const todayIso = toLocalDateIso();

  const activeHwNotes = allNotes.filter(
    (n) =>
      n.targetName.toLowerCase() === target.toLowerCase() &&
      n.type === 'homework' &&
      !n.isCompleted &&
      !n.transferPending &&
      n.date >= todayIso &&
      Boolean(n.subject?.trim()) &&
      n.lessonNum != null
  );

  if (activeHwNotes.length === 0) {
    return { updatedNotes: allNotes, relocatedInfo: [] };
  }

  const updatedMap = new Map<string, ScheduleNote>();
  const relocatedInfo: { note: ScheduleNote; message: string }[] = [];

  for (const note of activeHwNotes) {
    const subj = note.subject!;
    const lsnNum = note.lessonNum!;
    const targetSg =
      note.subgroup ??
      (subj.toLowerCase().includes('технолог') && isMiddleSchool
        ? userTechSubgroup
        : userSubgroup);

    // Current effective subject at note's slot
    const currentEffSubj = getEffectiveLessonSubject(
      schedule,
      note.date,
      lsnNum,
      targetSg,
      userTechSubgroup,
      isMiddleSchool
    );

    const normSubj = normalizeSubject(subj).toLowerCase().trim();
    const isStillSameSubject = currentEffSubj
      ? (() => {
          const normCurrent = normalizeSubject(currentEffSubj).toLowerCase().trim();
          return (
            normCurrent === normSubj ||
            normCurrent.includes(normSubj) ||
            normSubj.includes(normCurrent)
          );
        })()
      : false;

    // Case A: Lesson was cancelled or replaced with another subject -> relocate!
    if (!isStillSameSubject) {
      const nextInWeek = findNextLessonInCurrentWeek(
        schedule,
        subj,
        targetSg,
        note.date,
        note.date === todayIso ? lsnNum : null,
        note.date > todayIso,
        userTechSubgroup,
        isMiddleSchool
      );

      if (nextInWeek) {
        const updated: ScheduleNote = {
          ...note,
          date: nextInWeek[0],
          lessonNum: nextInWeek[1],
          transferPending: false,
        };
        updatedMap.set(note.id, updated);
        relocatedInfo.push({
          note: updated,
          message: `ДЗ по «${subj}» перенесено на ${nextInWeek[0]} (${nextInWeek[1]} урок) из-за отмены урока`,
        });
      } else {
        const updated: ScheduleNote = {
          ...note,
          transferPending: true,
        };
        updatedMap.set(note.id, updated);
        relocatedInfo.push({
          note: updated,
          message: `ДЗ по «${subj}» перенесено на следующую неделю (урок отменён)`,
        });
      }
    } else {
      // Case B: Lesson still exists, but check if an earlier lesson appeared due to schedule change
      const earliestInWeek = findNextLessonInCurrentWeek(
        schedule,
        subj,
        targetSg,
        todayIso,
        null,
        true,
        userTechSubgroup,
        isMiddleSchool
      );

      if (earliestInWeek) {
        const isEarlier =
          earliestInWeek[0] < note.date ||
          (earliestInWeek[0] === note.date && earliestInWeek[1] < lsnNum);

        if (isEarlier) {
          // Check if it appeared due to a change at that earlier lesson
          const dow = (new Date(earliestInWeek[0]).getDay() + 6) % 7;
          const changesAtEarlier = getChangesForLesson(schedule, dow, earliestInWeek[1]);
          const appearedDueToChange = changesAtEarlier.some((c) => {
            if (isCancelledLesson(c)) return false;
            const cSubj = c.subject ? normalizeSubject(c.subject).toLowerCase().trim() : '';
            return (
              cSubj === normSubj ||
              cSubj.includes(normSubj) ||
              normSubj.includes(cSubj)
            );
          });

          if (appearedDueToChange) {
            const updated: ScheduleNote = {
              ...note,
              date: earliestInWeek[0],
              lessonNum: earliestInWeek[1],
              transferPending: false,
            };
            updatedMap.set(note.id, updated);
            relocatedInfo.push({
              note: updated,
              message: `ДЗ по «${subj}» перенесено раньше на ${earliestInWeek[0]} (${earliestInWeek[1]} урок) из-за изменения в расписании`,
            });
          }
        }
      }
    }
  }

  if (updatedMap.size === 0) {
    return { updatedNotes: allNotes, relocatedInfo: [] };
  }

  const newNotes = allNotes.map((n) => updatedMap.get(n.id) || n);
  return { updatedNotes: newNotes, relocatedInfo };
}
