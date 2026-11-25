import React from 'react';
import {
  Lesson,
  ScheduleResponse,
  getChangesForLesson,
  isScheduleForTeacher,
} from '../../types/api';
import { ScheduleNote } from '../../types/notes';
import { RealtimeState } from '../../hooks/useRealtimeSync';
import { BELLS } from '../../domain/bellSchedule';
import { getPairRomanNum, getPairTimeRange, getSplitLessonTimes } from '../../domain/pairSchedule';
import { processLessons } from '../../domain/schedulePairMerger';
import { parseLocalDate, toLocalDateIso } from '../../utils/dateUtils';
import { isLessonTotallyCancelled } from '../../utils/notesLessonHelper';
import { LessonBody, LessonNotesList } from './LessonBody';

const FULL_DAYS = [
  'Понедельник',
  'Вторник',
  'Среда',
  'Четверг',
  'Пятница',
  'Суббота',
];

function areLessonsIdentical(l1: Lesson, l2: Lesson): boolean {
  if (l1.subgroups.length !== l2.subgroups.length) return false;
  for (let i = 0; i < l1.subgroups.length; i++) {
    const sg1 = l1.subgroups[i];
    const sg2 = l2.subgroups[i];
    if ((sg1.subject || '').trim().toLowerCase() !== (sg2.subject || '').trim().toLowerCase()) {
      return false;
    }
    if ((sg1.teacher || '').trim().toLowerCase() !== (sg2.teacher || '').trim().toLowerCase()) {
      return false;
    }
    if ((sg1.className || '').trim().toLowerCase() !== (sg2.className || '').trim().toLowerCase()) {
      return false;
    }
  }
  return true;
}

function getBellTimeRange(lessonNum: number, fallbackTime?: string): [string, string] {
  const bell = BELLS.find((b) => b.num === lessonNum);
  const raw = bell?.timeRange || fallbackTime || '';
  const parts = raw.split(/–|-/);
  return [parts[0]?.trim() || raw, parts[1]?.trim() || ''];
}

interface Props {
  schedule: ScheduleResponse | null | undefined;
  targetName: string;
  showPairsSchedule: boolean;
  realtimeState?: RealtimeState;
  activeDayIndex?: number; // 0..5 for today if current week, else -1
  isThisWeek?: boolean;
  notes?: ScheduleNote[];
  weekOffset?: number;
  onToggleNoteCompleted?: (id: string) => void;
  onTeacherClassClick?: (name: string) => void;
  onRoomClick?: (room: string, lessonNum: number, dayIndex: number) => void;
  onCellClick?: (dayIndex: number, lessonNum: number, subject: string, dateIso: string) => void;
  onDayHeaderClick?: (dayIndex: number) => void;
  onDateClick?: (dateIso: string) => void;
}

export const WeekScheduleTable: React.FC<Props> = ({
  schedule,
  targetName,
  showPairsSchedule,
  realtimeState,
  activeDayIndex = -1,
  isThisWeek = false,
  notes = [],
  weekOffset = 0,
  onToggleNoteCompleted,
  onTeacherClassClick,
  onRoomClick,
  onCellClick,
  onDayHeaderClick,
  onDateClick,
}) => {
  const isTeacher = isScheduleForTeacher(schedule);

  const dayColumns = React.useMemo(() => {
    return Array.from({ length: 6 }, (_, dayIndex) => {
      let dateObj: Date;
      if (schedule?.weekStart) {
        dateObj = parseLocalDate(schedule.weekStart);
        dateObj.setDate(dateObj.getDate() + dayIndex);
      } else {
        dateObj = new Date();
        const dow = (dateObj.getDay() + 6) % 7;
        dateObj.setDate(dateObj.getDate() - dow + weekOffset * 7 + dayIndex);
      }
      const dateIso = toLocalDateIso(dateObj);
      const dateLabel = dateObj.toLocaleDateString('ru-RU', {
        day: 'numeric',
        month: 'short',
      });
      const daySched = schedule?.days?.find((d) => d.dayIndex === dayIndex);
      const lessons = daySched?.lessons || [];
      const processed = processLessons(lessons, isTeacher, targetName, showPairsSchedule);
      const dayNotes = notes.filter(
        (n) => n.targetName === targetName && n.date === dateIso
      );

      return {
        dayIndex,
        dayName: FULL_DAYS[dayIndex],
        dateIso,
        dateLabel,
        lessons,
        processed,
        dayNotes,
        beforeNote: dayNotes.find((n) => n.slotType === 'before_lessons'),
        afterNote: dayNotes.find((n) => n.slotType === 'after_lessons'),
      };
    });
  }, [schedule, isTeacher, targetName, showPairsSchedule, notes, weekOffset]);

  const hasBeforeNotes = dayColumns.some((col) => Boolean(col.beforeNote));
  const hasAfterNotes = dayColumns.some((col) => Boolean(col.afterNote));

  const usePairRows = React.useMemo(() => {
    if (!showPairsSchedule) return false;
    return dayColumns.some((col) => col.processed.some((it) => it.type === 'pair'));
  }, [showPairsSchedule, dayColumns]);

  type RowDef =
    | { kind: 'single'; lessonNum: number; start: string; end: string }
    | { kind: 'pair'; pairNum: number; pairRoman: string; start: string; end: string; nums: [number, number] };

  const rows: RowDef[] = React.useMemo(() => {
    const allNums = new Set<number>();
    dayColumns.forEach((col) => {
      col.lessons.forEach((l) => allNums.add(l.num));
    });
    if (allNums.size === 0) return [];

    const sortedNums = Array.from(allNums).sort((a, b) => a - b);
    const minNum = sortedNums[0];
    const maxNum = sortedNums[sortedNums.length - 1];

    if (!usePairRows) {
      const result: RowDef[] = [];
      for (let num = minNum; num <= maxNum; num++) {
        let fallbackTime: string | undefined;
        for (const col of dayColumns) {
          const found = col.lessons.find((l) => l.num === num);
          if (found?.time) {
            fallbackTime = found.time;
            break;
          }
        }
        const [start, end] = getBellTimeRange(num, fallbackTime);
        result.push({ kind: 'single', lessonNum: num, start, end });
      }
      return result;
    }

    const result: RowDef[] = [];
    const maxPairSlots = 4; // Pairs I..IV cover lessons 1..8
    for (let p = 1; p <= maxPairSlots; p++) {
      const n1 = (p - 1) * 2 + 1;
      const n2 = n1 + 1;
      if (n2 < minNum) continue;
      if (n1 > maxNum) break;
      const timeRange = getPairTimeRange(p);
      const parts = timeRange.split(/–|-/);
      result.push({
        kind: 'pair',
        pairNum: p,
        pairRoman: getPairRomanNum(p),
        start: parts[0]?.trim() || '',
        end: parts[1]?.trim() || '',
        nums: [n1, n2],
      });
    }
    for (let num = Math.max(9, minNum); num <= maxNum; num++) {
      const [start, end] = getBellTimeRange(num);
      result.push({ kind: 'single', lessonNum: num, start, end });
    }
    return result;
  }, [dayColumns, usePairRows]);

  if (rows.length === 0) {
    return (
      <div className="py-20 text-center px-4 bg-[var(--md-surface)] flex-1">
        <p className="text-sm font-bold text-[var(--md-text-primary)]">
          На эту неделю занятий нет
        </p>
        <p className="text-xs text-[var(--md-text-muted)] mt-1">
          Отдыхайте или выберите другую неделю
        </p>
      </div>
    );
  }

  const activeLessonNum =
    realtimeState && realtimeState.lessonBellState.type === 'in_lesson'
      ? realtimeState.lessonBellState.num
      : null;
  const activePairNum =
    realtimeState && realtimeState.pairBellState.type === 'in_lesson'
      ? realtimeState.pairBellState.num
      : null;

  return (
    <div className="w-full overflow-x-auto bg-[var(--md-surface)] flex-1">
      <table className="w-full min-w-[920px] table-fixed border-collapse">
        <thead>
          <tr className="bg-[var(--md-surface)] border-b border-[var(--md-border-light)]">
            <th className="w-[74px] py-2.5 px-2 text-center font-bold text-[11px] uppercase tracking-wider text-[var(--md-text-muted)] align-middle">
              {usePairRows ? 'Пара' : 'Урок'}
            </th>
            {dayColumns.map((col) => {
              const isToday = isThisWeek && col.dayIndex === activeDayIndex;
              const isSaturday = col.dayIndex === 5;
              return (
                <th
                  key={col.dayIndex}
                  className={`py-2 px-2.5 text-left border-l border-[var(--md-border-light)] align-middle transition-colors ${
                    isToday ? 'bg-[var(--nika-active-bg)]/35' : ''
                  }`}
                >
                  <div className="flex items-center justify-between gap-1.5 min-w-0">
                    <button
                      type="button"
                      onClick={() => onDayHeaderClick?.(col.dayIndex)}
                      className={`font-bold text-[13px] truncate transition-opacity ${
                        onDayHeaderClick ? 'hover:opacity-75 cursor-pointer' : 'cursor-default'
                      } ${
                        isToday
                          ? 'text-[var(--nika-subject)]'
                          : isSaturday
                          ? 'text-[var(--md-secondary)]'
                          : 'text-[var(--md-text-primary)]'
                      }`}
                      title={onDayHeaderClick ? `Открыть ${col.dayName}` : undefined}
                    >
                      {col.dayName}
                    </button>
                    {isToday && (
                      <span className="px-1.5 py-0.5 rounded-full text-[9px] font-bold bg-[var(--md-secondary-container)] text-[var(--md-on-secondary-container)] shrink-0">
                        Сегодня
                      </span>
                    )}
                  </div>
                  <button
                    type="button"
                    onClick={() => onDateClick?.(col.dateIso)}
                    className={`block text-[11px] font-medium text-[var(--md-text-muted)] mt-0.5 ${
                      onDateClick ? 'hover:text-[var(--nika-subject)] hover:underline cursor-pointer' : 'cursor-default'
                    }`}
                  >
                    {col.dateLabel}
                  </button>
                </th>
              );
            })}
          </tr>
        </thead>

        <tbody>
          {/* "До уроков" row if any day has a before_lessons note */}
          {hasBeforeNotes && (
            <tr className="border-b border-[var(--md-border-light)]">
              <td className="py-2 px-2 text-center align-middle">
                <span className="font-semibold text-[11px] leading-tight text-[var(--md-text-secondary)]">
                  До уроков
                </span>
              </td>
              {dayColumns.map((col) => {
                const note = col.beforeNote;
                return (
                  <td
                    key={col.dayIndex}
                    onClick={() => note && onToggleNoteCompleted?.(note.id)}
                    className={`py-2 px-2.5 border-l border-[var(--md-border-light)] align-middle transition-colors ${
                      note
                        ? note.isCompleted
                          ? 'bg-[var(--md-surface)]/60 opacity-60 cursor-pointer'
                          : 'cursor-pointer hover:bg-[var(--md-surface-variant)]/40'
                        : ''
                    }`}
                  >
                    {note && (
                      <p
                        className={`text-[12px] leading-snug break-words ${
                          note.isCompleted
                            ? 'line-through text-[var(--md-text-muted)]'
                            : 'text-[var(--md-text-secondary)] font-medium'
                        }`}
                      >
                        {note.text}
                      </p>
                    )}
                  </td>
                );
              })}
            </tr>
          )}

          {/* Main schedule rows */}
          {rows.map((row) => {
            if (row.kind === 'single') {
              const lessonNum = row.lessonNum;
              return (
                <tr
                  key={`row_${lessonNum}`}
                  className="border-b border-[var(--md-border-light)]"
                >
                  {/* Row Header: Lesson Num & Bell Times */}
                  <td className="py-2.5 px-2 align-top">
                    <div className="flex items-start justify-center gap-1.5">
                      <span className="font-bold text-[16px] leading-none text-[var(--nika-lsn-num)] pt-0.5">
                        {lessonNum}
                      </span>
                      <div className="flex flex-col items-start leading-none pt-0.5">
                        <span className="font-semibold text-[11px] text-[var(--nika-lsn-time)]">
                          {row.start}
                        </span>
                        {row.end && (
                          <span className="font-semibold text-[11px] text-[var(--nika-lsn-time)] mt-0.5">
                            {row.end}
                          </span>
                        )}
                      </div>
                    </div>
                  </td>

                  {dayColumns.map((col) => {
                    const lesson = col.lessons.find((l) => l.num === lessonNum);
                    const changes = getChangesForLesson(schedule, col.dayIndex, lessonNum);
                    const isToday = isThisWeek && col.dayIndex === activeDayIndex;
                    const isActiveNow = Boolean(isToday && activeLessonNum === lessonNum);
                    const lessonNotes = col.dayNotes.filter(
                      (n) => n.slotType === 'lesson' && n.lessonNum === lessonNum
                    );

                    if (!lesson) {
                      return (
                        <td
                          key={col.dayIndex}
                          onClick={
                            onCellClick
                              ? () => onCellClick(col.dayIndex, lessonNum, '', col.dateIso)
                              : undefined
                          }
                          className={`py-2.5 px-2.5 border-l border-[var(--md-border-light)] align-top transition-colors ${
                            isActiveNow ? 'bg-[var(--nika-active-bg)]/40' : ''
                          } ${onCellClick ? 'cursor-pointer hover:bg-[var(--md-surface-variant)]/30' : ''}`}
                        >
                          <LessonNotesList
                            notes={lessonNotes}
                            onToggleNoteCompleted={onToggleNoteCompleted}
                          />
                        </td>
                      );
                    }

                    const isTotalCancelled = isLessonTotallyCancelled(lesson, changes);
                    const hasReplacement = !isTotalCancelled && changes.length > 0;
                    const cellBg = isTotalCancelled
                      ? 'bg-[var(--nika-cancel-bg)]'
                      : isActiveNow
                      ? 'bg-[var(--nika-active-bg)]'
                      : hasReplacement
                      ? 'bg-[var(--nika-change-bg)]'
                      : '';

                    return (
                      <td
                        key={col.dayIndex}
                        onClick={
                          onCellClick
                            ? () =>
                                onCellClick(
                                  col.dayIndex,
                                  lesson.num,
                                  lesson.subgroups[0]?.subject || '',
                                  col.dateIso
                                )
                            : undefined
                        }
                        className={`py-2.5 px-2.5 border-l border-[var(--md-border-light)] align-top transition-colors select-text ${cellBg} ${
                          onCellClick ? 'cursor-pointer' : ''
                        }`}
                      >
                        <LessonBody
                          lesson={lesson}
                          changes={changes}
                          isTeacherMode={isTeacher}
                          compact
                          onTeacherClassClick={onTeacherClassClick}
                          onRoomClick={(room, num) => onRoomClick?.(room, num, col.dayIndex)}
                        />
                        <LessonNotesList
                          notes={lessonNotes}
                          onToggleNoteCompleted={onToggleNoteCompleted}
                        />
                      </td>
                    );
                  })}
                </tr>
              );
            }

            // Pair row (Roman I..IV)
            const [n1, n2] = row.nums;
            const [split1, split2] = getSplitLessonTimes(row.pairNum, `${row.start}–${row.end}`);

            return (
              <tr
                key={`pair_${row.pairNum}`}
                className="border-b border-[var(--md-border-light)]"
              >
                {/* Row Header: Pair Roman Numeral & Times */}
                <td className="py-2.5 px-2 align-top">
                  <div className="flex items-start justify-center gap-1.5">
                    <span className="font-bold text-[16px] leading-none text-[var(--nika-lsn-num)] pt-0.5">
                      {row.pairRoman}
                    </span>
                    <div className="flex flex-col items-start leading-none pt-0.5">
                      <span className="font-semibold text-[11px] text-[var(--nika-lsn-time)]">
                        {row.start}
                      </span>
                      {row.end && (
                        <span className="font-semibold text-[11px] text-[var(--nika-lsn-time)] mt-0.5">
                          {row.end}
                        </span>
                      )}
                    </div>
                  </div>
                </td>

                {dayColumns.map((col) => {
                  const l1 = col.lessons.find((l) => l.num === n1);
                  const l2 = col.lessons.find((l) => l.num === n2);
                  const ch1 = getChangesForLesson(schedule, col.dayIndex, n1);
                  const ch2 = getChangesForLesson(schedule, col.dayIndex, n2);
                  const isToday = isThisWeek && col.dayIndex === activeDayIndex;
                  const isL1Active = Boolean(isToday && activeLessonNum === n1);
                  const isL2Active = Boolean(isToday && activeLessonNum === n2);
                  const isPairActive = Boolean(
                    isToday &&
                      (activePairNum === row.pairNum ||
                        activeLessonNum === n1 ||
                        activeLessonNum === n2)
                  );

                  const pairNotes = col.dayNotes.filter(
                    (n) =>
                      n.slotType === 'lesson' && (n.lessonNum === n1 || n.lessonNum === n2)
                  );

                  if (!l1 && !l2) {
                    return (
                      <td
                        key={col.dayIndex}
                        onClick={
                          onCellClick
                            ? () => onCellClick(col.dayIndex, n1, '', col.dateIso)
                            : undefined
                        }
                        className={`py-2.5 px-2.5 border-l border-[var(--md-border-light)] align-top transition-colors ${
                          isPairActive ? 'bg-[var(--nika-active-bg)]/40' : ''
                        } ${onCellClick ? 'cursor-pointer hover:bg-[var(--md-surface-variant)]/30' : ''}`}
                      >
                        <LessonNotesList
                          notes={pairNotes}
                          onToggleNoteCompleted={onToggleNoteCompleted}
                        />
                      </td>
                    );
                  }

                  const pairEntry = col.processed.find(
                    (it) => it.type === 'pair' && it.pairNum === row.pairNum
                  );
                  const unifiedIdentical = Boolean(
                    pairEntry && l1 && l2 && areLessonsIdentical(l1, l2)
                  );

                  if (unifiedIdentical && l1) {
                    const allChanges = ch1.length > 0 ? ch1 : ch2;
                    const isTotalCancelled = isLessonTotallyCancelled(l1, allChanges);
                    const hasReplacement =
                      !isTotalCancelled && (ch1.length > 0 || ch2.length > 0);
                    const cellBg = isTotalCancelled
                      ? 'bg-[var(--nika-cancel-bg)]'
                      : isPairActive
                      ? 'bg-[var(--nika-active-bg)]'
                      : hasReplacement
                      ? 'bg-[var(--nika-change-bg)]'
                      : '';

                    return (
                      <td
                        key={col.dayIndex}
                        onClick={
                          onCellClick
                            ? () =>
                                onCellClick(
                                  col.dayIndex,
                                  l1.num,
                                  l1.subgroups[0]?.subject || '',
                                  col.dateIso
                                )
                            : undefined
                        }
                        className={`py-2.5 px-2.5 border-l border-[var(--md-border-light)] align-top transition-colors select-text ${cellBg} ${
                          onCellClick ? 'cursor-pointer' : ''
                        }`}
                      >
                        <LessonBody
                          lesson={l1}
                          changes={allChanges}
                          isTeacherMode={isTeacher}
                          compact
                          onTeacherClassClick={onTeacherClassClick}
                          onRoomClick={(room, num) => onRoomClick?.(room, num, col.dayIndex)}
                        />
                        <LessonNotesList
                          notes={pairNotes}
                          onToggleNoteCompleted={onToggleNoteCompleted}
                        />
                      </td>
                    );
                  }

                  // Split or single-half inside pair row
                  const renderSubSlot = (
                    subLesson: Lesson | undefined,
                    subChanges: typeof ch1,
                    subTimes: { start: string; end: string },
                    subActive: boolean,
                    subNum: number
                  ) => {
                    if (!subLesson) {
                      return null;
                    }
                    const isSubCancelled = isLessonTotallyCancelled(subLesson, subChanges);
                    const isSubChanged = !isSubCancelled && subChanges.length > 0;
                    const subBg = isSubCancelled
                      ? 'bg-[var(--nika-cancel-bg)]'
                      : subActive
                      ? 'bg-[var(--nika-active-bg)]'
                      : isSubChanged
                      ? 'bg-[var(--nika-change-bg)]'
                      : '';
                    const subNotes = col.dayNotes.filter(
                      (n) => n.slotType === 'lesson' && n.lessonNum === subNum
                    );

                    return (
                      <div
                        onClick={
                          onCellClick
                            ? (e) => {
                                e.stopPropagation();
                                onCellClick(
                                  col.dayIndex,
                                  subLesson.num,
                                  subLesson.subgroups[0]?.subject || '',
                                  col.dateIso
                                );
                              }
                            : undefined
                        }
                        className={`p-1.5 rounded-lg transition-colors ${subBg} ${
                          onCellClick ? 'cursor-pointer' : ''
                        }`}
                      >
                        <div className="text-[10px] font-semibold text-[var(--nika-lsn-time)] mb-0.5">
                          {subTimes.start}–{subTimes.end}
                        </div>
                        <LessonBody
                          lesson={subLesson}
                          changes={subChanges}
                          isTeacherMode={isTeacher}
                          compact
                          onTeacherClassClick={onTeacherClassClick}
                          onRoomClick={(room, num) => onRoomClick?.(room, num, col.dayIndex)}
                        />
                        <LessonNotesList
                          notes={subNotes}
                          onToggleNoteCompleted={onToggleNoteCompleted}
                        />
                      </div>
                    );
                  };

                  return (
                    <td
                      key={col.dayIndex}
                      className="py-1.5 px-1.5 border-l border-[var(--md-border-light)] align-top select-text"
                    >
                      <div className="space-y-1 divide-y divide-[var(--md-border-light)]/40">
                        {renderSubSlot(l1, ch1, split1, isL1Active, n1)}
                        {renderSubSlot(l2, ch2, split2, isL2Active, n2)}
                      </div>
                    </td>
                  );
                })}
              </tr>
            );
          })}

          {/* "После уроков" row if any day has an after_lessons note */}
          {hasAfterNotes && (
            <tr className="border-b border-[var(--md-border-light)]">
              <td className="py-2 px-2 text-center align-middle">
                <span className="font-semibold text-[11px] leading-tight text-[var(--md-text-secondary)]">
                  После уроков
                </span>
              </td>
              {dayColumns.map((col) => {
                const note = col.afterNote;
                return (
                  <td
                    key={col.dayIndex}
                    onClick={() => note && onToggleNoteCompleted?.(note.id)}
                    className={`py-2 px-2.5 border-l border-[var(--md-border-light)] align-middle transition-colors ${
                      note
                        ? note.isCompleted
                          ? 'bg-[var(--md-surface)]/60 opacity-60 cursor-pointer'
                          : 'cursor-pointer hover:bg-[var(--md-surface-variant)]/40'
                        : ''
                    }`}
                  >
                    {note && (
                      <p
                        className={`text-[12px] leading-snug break-words ${
                          note.isCompleted
                            ? 'line-through text-[var(--md-text-muted)]'
                            : 'text-[var(--md-text-secondary)] font-medium'
                        }`}
                      >
                        {note.text}
                      </p>
                    )}
                  </td>
                );
              })}
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
};
