import React from 'react';
import {
  Lesson,
  ScheduleChangeItem,
  getLessonDisplaySubject,
  isCancelledLesson,
  normalizeSubject,
} from '../../types/api';
import { getSplitLessonTimes } from '../../domain/pairSchedule';
import {
  findChangeForSubgroup,
  isLessonTotallyCancelled,
} from '../../utils/notesLessonHelper';

interface Props {
  pairNum: number;
  pairRoman: string;
  pairTimes: string;
  firstLesson: Lesson;
  secondLesson: Lesson;
  dayIndex: number;
  changes1: ScheduleChangeItem[];
  changes2: ScheduleChangeItem[];
  isTeacherMode: boolean;
  isActiveNow: boolean;
  isActiveL1?: boolean;
  isActiveL2?: boolean;
  notes?: any[];
  onToggleNoteCompleted?: (id: string) => void;
  userSubgroup?: number;
  userTechSubgroup?: number;
  onTeacherClassClick?: (name: string) => void;
  onRoomClick?: (room: string, lessonNum: number) => void;
  onClick?: () => void;
}

function areLessonsIdentical(
  l1: Lesson,
  l2: Lesson
): boolean {
  if (l1.subgroups.length !== l2.subgroups.length) return false;
  for (let i = 0; i < l1.subgroups.length; i++) {
    const sg1 = l1.subgroups[i];
    const sg2 = l2.subgroups[i];
    const subj1 = (sg1.subject || '').trim().toLowerCase();
    const subj2 = (sg2.subject || '').trim().toLowerCase();
    if (subj1 !== subj2) return false;

    const teacher1 = (sg1.teacher || '').trim().toLowerCase();
    const teacher2 = (sg2.teacher || '').trim().toLowerCase();
    if (teacher1 !== teacher2) return false;

    const class1 = (sg1.className || '').trim().toLowerCase();
    const class2 = (sg2.className || '').trim().toLowerCase();
    if (class1 !== class2) return false;
  }
  return true;
}

function formatRoom(room?: string | null): string {
  if (!room || !room.trim()) return '';
  const clean = room.trim().replace(/^\(|\)$/g, '');
  return `(${clean})`;
}

function formatGroup(label?: string | null): string {
  if (!label || !label.trim()) return '';
  const trimmed = label.trim();
  const digits = trimmed.replace(/\D/g, '');
  return digits ? `${digits} группа` : trimmed;
}

export const PairCard: React.FC<Props> = ({
  pairNum,
  pairRoman,
  pairTimes,
  firstLesson: l1,
  secondLesson: l2,
  changes1,
  changes2,
  isTeacherMode,
  isActiveNow,
  isActiveL1 = false,
  isActiveL2 = false,
  notes,
  onToggleNoteCompleted,
  onTeacherClassClick,
  onRoomClick,
  onClick,
}) => {
  const areIdentical = areLessonsIdentical(l1, l2);
  const allChanges = changes1.length > 0 ? changes1 : changes2;

  // Split times if lessons are different
  const [splitTimes1, splitTimes2] = getSplitLessonTimes(pairNum, pairTimes);

  if (areIdentical) {
    // -------------------------------------------------------------
    // UNIFIED PAIR CARD (Same subject, teacher, room for both lessons)
    // -------------------------------------------------------------
    const isTotalCancelled = isLessonTotallyCancelled(l1, allChanges);

    const hasReplacement =
      !isTotalCancelled && (changes1.length > 0 || changes2.length > 0);

    const bgClass = isTotalCancelled
      ? 'bg-[var(--nika-cancel-bg)]'
      : isActiveNow
      ? 'bg-[var(--nika-active-bg)]'
      : hasReplacement
      ? 'bg-[var(--nika-change-bg)]'
      : 'bg-transparent';

    const times = pairTimes.split(/–|-/);

    return (
      <div
        onClick={onClick}
        className={`w-full ${bgClass} border-b border-[var(--md-border-light)] transition-colors select-text ${
          onClick ? 'cursor-pointer' : ''
        }`}
      >
        <div className="flex items-start px-3.5 py-2.5 gap-2.5">
          {/* 1. Roman Pair Number (I, II, III, IV) */}
          <div className="w-5 text-right font-bold text-[17px] text-[var(--nika-lsn-num)] shrink-0 pt-0.5">
            {pairRoman}
          </div>

          {/* 2. Pair Times (stacked start and end of the entire pair) */}
          <div className="w-12 shrink-0 flex flex-col items-start leading-none pt-1">
            <span className="font-semibold text-[12px] text-[var(--nika-lsn-time)]">
              {times[0]?.trim() || ''}
            </span>
            <span className="font-semibold text-[12px] text-[var(--nika-lsn-time)] mt-0.5">
              {times[1]?.trim() || ''}
            </span>
          </div>

          {/* 3. Unified Content Column */}
          <div className="flex-1 min-w-0">
            {isTotalCancelled ? (
              <div>
                <div className="font-bold text-[14.5px] leading-tight text-[var(--nika-cancel)] line-through">
                  {getLessonDisplaySubject(l1) || 'Пара отменена'}
                </div>
                {l1.subgroups.length === 0 ? (
                  <div className="text-xs text-[var(--nika-cancel)] mt-0.5">
                    Пара отменена
                  </div>
                ) : (
                  <div className="space-y-1 mt-0.5">
                    {l1.subgroups.map((sg, idx) => {
                      const sgChange = findChangeForSubgroup(sg, allChanges, l1.subgroups);
                      const teacher = sg.teacher || sgChange?.teacher;
                      const className = sg.className || sgChange?.className;
                      const rawRoom = sg.room || sgChange?.room;
                      const roomText = formatRoom(rawRoom);
                      const cleanRoom = rawRoom?.trim().replace(/^\(|\)$/g, '') || '';
                      const groupText = formatGroup(sg.label);

                      return (
                        <div key={idx} className="flex items-center flex-wrap gap-1 text-[12px] text-[var(--nika-cancel)]">
                          {isTeacherMode ? (
                            className && (
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  onTeacherClassClick?.(className);
                                }}
                                className="px-1 py-0.5 rounded font-bold border border-[var(--md-border-light)] text-[var(--nika-cancel)] line-through hover:bg-[var(--md-surface-variant)] transition-colors"
                              >
                                {className}
                              </button>
                            )
                          ) : (
                            teacher && (
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  onTeacherClassClick?.(teacher);
                                }}
                                className="italic px-1 py-0.5 rounded border border-[var(--md-border-light)] text-[var(--nika-cancel)] line-through hover:bg-[var(--md-surface-variant)] transition-colors"
                              >
                                {teacher}
                              </button>
                            )
                          )}
                          {groupText && (
                            <span className="font-bold text-[var(--nika-cancel)] line-through">
                              - {groupText}
                            </span>
                          )}
                          {roomText && (
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                if (cleanRoom) onRoomClick?.(cleanRoom, l1.num);
                              }}
                              className="font-bold px-1 py-0.5 rounded border border-[var(--md-border-light)] text-[var(--nika-cancel)] line-through hover:bg-[var(--md-surface-variant)] transition-colors"
                            >
                              {roomText}
                            </button>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-1.5">
                {l1.subgroups.map((sg, idx) => {
                  const sgChange = findChangeForSubgroup(sg, allChanges, l1.subgroups);
                  const isSgCancelled = Boolean(sgChange && isCancelledLesson(sgChange));

                  const normSgSubj = normalizeSubject(sg.subject).toLowerCase().trim();
                  const normChangeSubj = sgChange?.subject
                    ? normalizeSubject(sgChange.subject).toLowerCase().trim()
                    : '';
                  const isSubjectChanged = Boolean(
                    !isSgCancelled &&
                      normChangeSubj &&
                      normSgSubj &&
                      normChangeSubj !== normSgSubj
                  );

                  const normSgTeacher = sg.teacher?.toLowerCase().trim() || '';
                  const normChangeTeacher = sgChange?.teacher?.toLowerCase().trim() || '';
                  const isTeacherChanged = Boolean(
                    !isSgCancelled &&
                      normChangeTeacher &&
                      normSgTeacher &&
                      normChangeTeacher !== normSgTeacher
                  );

                  const normSgClass = sg.className?.toLowerCase().trim() || '';
                  const normChangeClass = sgChange?.className?.toLowerCase().trim() || '';
                  const isClassChanged = Boolean(
                    !isSgCancelled &&
                      normChangeClass &&
                      normSgClass &&
                      normChangeClass !== normSgClass
                  );

                  const cleanSgRoom =
                    sg.room?.trim().replace(/^\(|\)$/g, '').toLowerCase() || '';
                  const cleanChangeRoom =
                    sgChange?.room?.trim().replace(/^\(|\)$/g, '').toLowerCase() || '';
                  const isRoomChanged = Boolean(
                    !isSgCancelled &&
                      cleanChangeRoom &&
                      cleanSgRoom &&
                      cleanChangeRoom !== cleanSgRoom
                  );

                  const rawSubj =
                    sgChange?.subject?.trim()
                      ? sgChange.subject
                      : sg.subject || '—';
                  const displaySubj = normalizeSubject(rawSubj);
                  const displayTeacher =
                    sgChange?.teacher?.trim()
                      ? sgChange.teacher
                      : sg.teacher?.trim()
                      ? sg.teacher
                      : l1.subgroups.length <= 1
                      ? allChanges.find((c) => c.teacher?.trim())?.teacher
                      : undefined;
                  const displayRoom =
                    sgChange?.room?.trim()
                      ? sgChange.room
                      : sg.room?.trim()
                      ? sg.room
                      : l1.subgroups.length <= 1
                      ? allChanges.find((c) => c.room?.trim())?.room
                      : undefined;
                  const displayClass =
                    sgChange?.className?.trim()
                      ? sgChange.className
                      : sg.className?.trim()
                      ? sg.className
                      : l1.subgroups.length <= 1
                      ? allChanges.find((c) => c.className?.trim())?.className
                      : undefined;

                  const subjectColor = isSgCancelled
                    ? 'text-[var(--nika-cancel)]'
                    : isSubjectChanged
                    ? 'text-[var(--nika-change)]'
                    : 'text-[var(--nika-subject)]';

                  const groupText = formatGroup(sg.label);
                  const roomText = formatRoom(displayRoom);
                  const cleanRoom = displayRoom?.trim().replace(/^\(|\)$/g, '') || '';

                  return (
                    <div key={idx} className="flex flex-col">
                      <div
                        className={`font-bold text-[14.5px] leading-tight ${subjectColor} ${
                          isSgCancelled ? 'line-through' : ''
                        }`}
                      >
                        {displaySubj}
                      </div>

                      <div className="flex items-center flex-wrap gap-1 mt-0.5 text-[12px]">
                        {isTeacherMode ? (
                          displayClass && (
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                onTeacherClassClick?.(displayClass);
                              }}
                              className={`px-1 py-0.5 rounded font-bold border transition-colors hover:bg-[var(--md-surface-variant)] ${
                                isSgCancelled
                                  ? 'text-[var(--nika-cancel)] line-through border-[var(--md-border-light)]'
                                  : isClassChanged
                                  ? 'text-[var(--nika-change)] border-[var(--nika-change)]/60'
                                  : 'text-[var(--nika-teacher)] border-[var(--md-border-light)]'
                              }`}
                            >
                              {displayClass}
                            </button>
                          )
                        ) : (
                          displayTeacher && (
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                onTeacherClassClick?.(displayTeacher);
                              }}
                              className={`italic px-1 py-0.5 rounded border transition-colors hover:bg-[var(--md-surface-variant)] ${
                                isSgCancelled
                                  ? 'text-[var(--nika-cancel)] line-through border-[var(--md-border-light)]'
                                  : isTeacherChanged
                                  ? 'text-[var(--nika-change)] border-[var(--nika-change)]/60'
                                  : 'text-[var(--nika-teacher)] border-[var(--md-border-light)]'
                              }`}
                            >
                              {displayTeacher}
                            </button>
                          )
                        )}

                        {groupText && (
                          <span
                            className={`font-bold ${
                              isSgCancelled
                                ? 'text-[var(--nika-cancel)] line-through'
                                : 'text-[var(--nika-group)]'
                            }`}
                          >
                            - {groupText}
                          </span>
                        )}

                        {roomText && (
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              if (cleanRoom) onRoomClick?.(cleanRoom, l1.num);
                            }}
                            className={`font-bold px-1 py-0.5 rounded border transition-colors hover:bg-[var(--md-surface-variant)] ${
                              isSgCancelled
                                ? 'text-[var(--nika-cancel)] line-through border-[var(--md-border-light)]'
                                : isRoomChanged
                                ? 'text-[var(--nika-change)] border-[var(--nika-change)]/60'
                                : 'text-[var(--nika-room)] border-[var(--md-border-light)]'
                            }`}
                          >
                            {roomText}
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Notes for this pair */}
            {notes && notes.length > 0 && (
              <div className="pt-1.5 space-y-1">
                {notes.map((note: any) => (
                  <div
                    key={note.id}
                    onClick={(e) => {
                      e.stopPropagation();
                      onToggleNoteCompleted?.(note.id);
                    }}
                    className="flex items-start gap-1.5 text-xs cursor-pointer hover:opacity-80 select-none"
                  >
                    <span className="font-bold text-[var(--nika-subject)] shrink-0">
                      {note.type === 'homework' ? 'ДЗ:' : 'Заметка:'}
                    </span>
                    <span
                      className={`leading-tight ${
                        note.isCompleted
                          ? 'line-through text-[var(--md-text-muted)] opacity-60'
                          : 'text-[var(--md-text-primary)]'
                      }`}
                    >
                      {note.text}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------
  // SPLIT PAIR CARD (Different subjects in lesson 1 and lesson 2)
  // -------------------------------------------------------------
  const isL1Cancelled = isLessonTotallyCancelled(l1, changes1);
  const l1HasChange = !isL1Cancelled && changes1.length > 0;
  const l1BgClass = isL1Cancelled
    ? 'bg-[var(--nika-cancel-bg)]'
    : isActiveL1
    ? 'bg-[var(--nika-active-bg)]'
    : l1HasChange
    ? 'bg-[var(--nika-change-bg)]'
    : '';

  const isL2Cancelled = isLessonTotallyCancelled(l2, changes2);
  const l2HasChange = !isL2Cancelled && changes2.length > 0;
  const l2BgClass = isL2Cancelled
    ? 'bg-[var(--nika-cancel-bg)]'
    : isActiveL2
    ? 'bg-[var(--nika-active-bg)]'
    : l2HasChange
    ? 'bg-[var(--nika-change-bg)]'
    : '';

  return (
    <div
      className="w-full border-b border-[var(--md-border-light)] transition-colors select-text"
    >
      <div className="flex items-start px-3.5 py-2 gap-2.5">
        {/* Roman Pair Number in Center Left */}
        <div className="w-5 text-right font-bold text-[17px] text-[var(--nika-lsn-num)] shrink-0 pt-2">
          {pairRoman}
        </div>

        {/* Column of two split lessons */}
        <div className="flex-1 divide-y divide-[var(--md-border-light)]/40">
          {/* Sub-Lesson 1 */}
          <div
            className={`flex items-start gap-2.5 py-1.5 px-2 rounded-lg transition-colors ${l1BgClass}`}
          >
            <div className="w-12 shrink-0 flex flex-col items-start leading-none pt-0.5">
              <span className="font-semibold text-[11.5px] text-[var(--nika-lsn-time)]">
                {splitTimes1.start}
              </span>
              <span className="font-semibold text-[11.5px] text-[var(--nika-lsn-time)] mt-0.5">
                {splitTimes1.end}
              </span>
            </div>
            <div className="flex-1 min-w-0">
              {renderLessonContent(l1, changes1, isTeacherMode, onTeacherClassClick, onRoomClick)}
            </div>
          </div>

          {/* Sub-Lesson 2 */}
          <div
            className={`flex items-start gap-2.5 py-1.5 px-2 rounded-lg transition-colors ${l2BgClass}`}
          >
            <div className="w-12 shrink-0 flex flex-col items-start leading-none pt-0.5">
              <span className="font-semibold text-[11.5px] text-[var(--nika-lsn-time)]">
                {splitTimes2.start}
              </span>
              <span className="font-semibold text-[11.5px] text-[var(--nika-lsn-time)] mt-0.5">
                {splitTimes2.end}
              </span>
            </div>
            <div className="flex-1 min-w-0">
              {renderLessonContent(l2, changes2, isTeacherMode, onTeacherClassClick, onRoomClick)}
            </div>
          </div>

          {/* Notes for this pair */}
          {notes && notes.length > 0 && (
            <div className="pt-1.5 pb-1 space-y-1">
              {notes.map((note: any) => (
                <div
                  key={note.id}
                  onClick={(e) => {
                    e.stopPropagation();
                    onToggleNoteCompleted?.(note.id);
                  }}
                  className="flex items-start gap-1.5 text-xs cursor-pointer hover:opacity-80 select-none"
                >
                  <span className="font-bold text-[var(--nika-subject)] shrink-0">
                    {note.type === 'homework' ? 'ДЗ:' : 'Заметка:'}
                  </span>
                  <span
                    className={`leading-tight ${
                      note.isCompleted
                        ? 'line-through text-[var(--md-text-muted)] opacity-60'
                        : 'text-[var(--md-text-primary)]'
                    }`}
                  >
                    {note.text}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

function renderLessonContent(
  lesson: Lesson,
  changes: ScheduleChangeItem[],
  isTeacherMode: boolean,
  onTeacherClassClick?: (name: string) => void,
  onRoomClick?: (room: string, lessonNum: number) => void
) {
  const isCancelled = isLessonTotallyCancelled(lesson, changes);

  if (isCancelled) {
    const displaySubj = getLessonDisplaySubject(lesson) || 'Урок отменен';
    return (
      <div>
        <div className="font-bold text-[13.5px] text-[var(--nika-cancel)] line-through">
          {displaySubj}
        </div>
        {lesson.subgroups.length === 0 ? (
          <div className="text-[11px] text-[var(--nika-cancel)] mt-0.5">
            Урок отменен
          </div>
        ) : (
          <div className="space-y-0.5 mt-0.5">
            {lesson.subgroups.map((sg, idx) => {
              const sgChange = findChangeForSubgroup(sg, changes, lesson.subgroups);
              const teacher = sg.teacher || sgChange?.teacher;
              const className = sg.className || sgChange?.className;
              const rawRoom = sg.room || sgChange?.room;
              const roomText = formatRoom(rawRoom);
              const cleanRoom = rawRoom?.trim().replace(/^\(|\)$/g, '') || '';
              const groupText = formatGroup(sg.label);

              return (
                <div key={idx} className="flex items-center flex-wrap gap-1 text-[11px] text-[var(--nika-cancel)]">
                  {isTeacherMode ? (
                    className && (
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          onTeacherClassClick?.(className);
                        }}
                        className="px-1 py-0.5 rounded font-bold border border-[var(--md-border-light)] text-[var(--nika-cancel)] line-through"
                      >
                        {className}
                      </button>
                    )
                  ) : (
                    teacher && (
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          onTeacherClassClick?.(teacher);
                        }}
                        className="italic px-1 py-0.5 rounded border border-[var(--md-border-light)] text-[var(--nika-cancel)] line-through"
                      >
                        {teacher}
                      </button>
                    )
                  )}
                  {groupText && (
                    <span className="font-bold text-[var(--nika-cancel)] line-through">
                      - {groupText}
                    </span>
                  )}
                  {roomText && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        if (cleanRoom) onRoomClick?.(cleanRoom, lesson.num);
                      }}
                      className="font-bold px-1 py-0.5 rounded border border-[var(--md-border-light)] text-[var(--nika-cancel)] line-through"
                    >
                      {roomText}
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-1">
      {lesson.subgroups.map((sg, idx) => {
        const sgChange = findChangeForSubgroup(sg, changes, lesson.subgroups);
        const isSgCancelled = Boolean(sgChange && isCancelledLesson(sgChange));

        const normSgSubj = normalizeSubject(sg.subject).toLowerCase().trim();
        const normChangeSubj = sgChange?.subject
          ? normalizeSubject(sgChange.subject).toLowerCase().trim()
          : '';
        const isSubjectChanged = Boolean(
          !isSgCancelled &&
            normChangeSubj &&
            normSgSubj &&
            normChangeSubj !== normSgSubj
        );

        const normSgTeacher = sg.teacher?.toLowerCase().trim() || '';
        const normChangeTeacher = sgChange?.teacher?.toLowerCase().trim() || '';
        const isTeacherChanged = Boolean(
          !isSgCancelled &&
            normChangeTeacher &&
            normSgTeacher &&
            normChangeTeacher !== normSgTeacher
        );

        const normSgClass = sg.className?.toLowerCase().trim() || '';
        const normChangeClass = sgChange?.className?.toLowerCase().trim() || '';
        const isClassChanged = Boolean(
          !isSgCancelled &&
            normChangeClass &&
            normSgClass &&
            normChangeClass !== normSgClass
        );

        const cleanSgRoom =
          sg.room?.trim().replace(/^\(|\)$/g, '').toLowerCase() || '';
        const cleanChangeRoom =
          sgChange?.room?.trim().replace(/^\(|\)$/g, '').toLowerCase() || '';
        const isRoomChanged = Boolean(
          !isSgCancelled &&
            cleanChangeRoom &&
            cleanSgRoom &&
            cleanChangeRoom !== cleanSgRoom
        );

        const rawSubj =
          sgChange?.subject?.trim()
            ? sgChange.subject
            : sg.subject || '—';
        const displaySubj = normalizeSubject(rawSubj);
        const displayTeacher =
          sgChange?.teacher?.trim()
            ? sgChange.teacher
            : sg.teacher?.trim()
            ? sg.teacher
            : lesson.subgroups.length <= 1
            ? changes.find((c) => c.teacher?.trim())?.teacher
            : undefined;
        const displayRoom =
          sgChange?.room?.trim()
            ? sgChange.room
            : sg.room?.trim()
            ? sg.room
            : lesson.subgroups.length <= 1
            ? changes.find((c) => c.room?.trim())?.room
            : undefined;
        const displayClass =
          sgChange?.className?.trim()
            ? sgChange.className
            : sg.className?.trim()
            ? sg.className
            : lesson.subgroups.length <= 1
            ? changes.find((c) => c.className?.trim())?.className
            : undefined;

        const subjectColor = isSgCancelled
          ? 'text-[var(--nika-cancel)]'
          : isSubjectChanged
          ? 'text-[var(--nika-change)]'
          : 'text-[var(--nika-subject)]';

        const groupText = formatGroup(sg.label);
        const roomText = formatRoom(displayRoom);
        const cleanRoom = displayRoom?.trim().replace(/^\(|\)$/g, '') || '';

        return (
          <div key={idx} className="flex flex-col">
            <div
              className={`font-bold text-[13.5px] leading-tight ${subjectColor} ${
                isSgCancelled ? 'line-through' : ''
              }`}
            >
              {displaySubj}
            </div>

            <div className="flex items-center flex-wrap gap-1 mt-0.5 text-[11.5px]">
              {isTeacherMode ? (
                displayClass && (
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      onTeacherClassClick?.(displayClass);
                    }}
                    className={`px-1 py-0.5 rounded font-bold border transition-colors hover:bg-[var(--md-surface-variant)] ${
                      isSgCancelled
                        ? 'text-[var(--nika-cancel)] line-through border-[var(--md-border-light)]'
                        : isClassChanged
                        ? 'text-[var(--nika-change)] border-[var(--nika-change)]/60'
                        : 'text-[var(--nika-teacher)] border-[var(--md-border-light)]'
                    }`}
                  >
                    {displayClass}
                  </button>
                )
              ) : (
                displayTeacher && (
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      onTeacherClassClick?.(displayTeacher);
                    }}
                    className={`italic px-1 py-0.5 rounded border transition-colors hover:bg-[var(--md-surface-variant)] ${
                      isSgCancelled
                        ? 'text-[var(--nika-cancel)] line-through border-[var(--md-border-light)]'
                        : isTeacherChanged
                        ? 'text-[var(--nika-change)] border-[var(--nika-change)]/60'
                        : 'text-[var(--nika-teacher)] border-[var(--md-border-light)]'
                    }`}
                  >
                    {displayTeacher}
                  </button>
                )
              )}

              {groupText && (
                <span className="font-bold text-[var(--nika-group)]">
                  - {groupText}
                </span>
              )}

              {roomText && (
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    if (cleanRoom) onRoomClick?.(cleanRoom, lesson.num);
                  }}
                  className={`font-bold px-1 py-0.5 rounded border transition-colors hover:bg-[var(--md-surface-variant)] ${
                    isSgCancelled
                      ? 'text-[var(--nika-cancel)] line-through border-[var(--md-border-light)]'
                      : isRoomChanged
                      ? 'text-[var(--nika-change)] border-[var(--nika-change)]/60'
                      : 'text-[var(--nika-room)] border-[var(--md-border-light)]'
                  }`}
                >
                  {roomText}
                </button>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
