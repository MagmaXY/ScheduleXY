import React from 'react';
import {
  Lesson,
  ScheduleChangeItem,
  getLessonDisplaySubject,
  isCancelledLesson,
  normalizeSubject,
} from '../../types/api';
import { ScheduleNote } from '../../types/notes';
import {
  findChangeForSubgroup,
  isLessonTotallyCancelled,
} from '../../utils/notesLessonHelper';

export function formatRoom(room?: string | null): string {
  if (!room || !room.trim()) return '';
  const clean = room.trim().replace(/^\(|\)$/g, '');
  return `(${clean})`;
}

export function formatGroup(label?: string | null): string {
  if (!label || !label.trim()) return '';
  const trimmed = label.trim();
  const digits = trimmed.replace(/\D/g, '');
  return digits ? `${digits} группа` : trimmed;
}

interface LessonBodyProps {
  lesson: Lesson;
  changes: ScheduleChangeItem[];
  isTeacherMode: boolean;
  compact?: boolean;
  onTeacherClassClick?: (name: string) => void;
  onRoomClick?: (room: string, lessonNum: number) => void;
}

export const LessonBody: React.FC<LessonBodyProps> = ({
  lesson,
  changes,
  isTeacherMode,
  compact = false,
  onTeacherClassClick,
  onRoomClick,
}) => {
  const isTotalCancelled = isLessonTotallyCancelled(lesson, changes);
  const displaySubject = getLessonDisplaySubject(lesson);

  const subjSizeClass = compact ? 'text-[13px]' : 'text-[14.5px]';
  const metaSizeClass = compact ? 'text-[11px]' : 'text-[12px]';

  if (isTotalCancelled) {
    return (
      <div>
        <div
          className={`font-bold ${subjSizeClass} leading-tight text-[var(--nika-cancel)] line-through`}
        >
          {displaySubject || 'Урок отменен'}
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
                <div
                  key={idx}
                  className={`flex items-center flex-wrap gap-1 ${metaSizeClass} text-[var(--nika-cancel)]`}
                >
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
                        if (cleanRoom) onRoomClick?.(cleanRoom, lesson.num);
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
    );
  }

  return (
    <div className={compact ? 'space-y-1' : 'space-y-1.5'}>
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

        const rawSubj = sgChange?.subject?.trim()
          ? sgChange.subject
          : sg.subject || '—';
        const displaySubj = normalizeSubject(rawSubj);
        const displayTeacher = sgChange?.teacher?.trim()
          ? sgChange.teacher
          : sg.teacher?.trim()
          ? sg.teacher
          : lesson.subgroups.length <= 1
          ? changes.find((c) => c.teacher?.trim())?.teacher
          : undefined;
        const displayRoom = sgChange?.room?.trim()
          ? sgChange.room
          : sg.room?.trim()
          ? sg.room
          : lesson.subgroups.length <= 1
          ? changes.find((c) => c.room?.trim())?.room
          : undefined;
        const displayClass = sgChange?.className?.trim()
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
              className={`font-bold ${subjSizeClass} leading-tight ${subjectColor} ${
                isSgCancelled ? 'line-through' : ''
              }`}
            >
              {displaySubj}
            </div>

            <div className={`flex items-center flex-wrap gap-1 mt-0.5 ${metaSizeClass}`}>
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
};

interface LessonNotesListProps {
  notes?: ScheduleNote[];
  onToggleNoteCompleted?: (id: string) => void;
}

export const LessonNotesList: React.FC<LessonNotesListProps> = ({
  notes,
  onToggleNoteCompleted,
}) => {
  if (!notes || notes.length === 0) return null;

  return (
    <div className="pt-1.5 space-y-1">
      {notes.map((note) => (
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
            className={`leading-tight break-words min-w-0 ${
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
  );
};
