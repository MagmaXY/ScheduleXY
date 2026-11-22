import React, { useState, useRef, useEffect } from 'react';
import { ScheduleNote, NoteType, SlotType } from '../../types/notes';
import { usePreferencesStore } from '../../store/preferencesStore';
import { useScheduleStore } from '../../store/scheduleStore';
import { normalizeSubject } from '../../types/api';
import { BELLS } from '../../domain/bellSchedule';
import {
  getSubjectForLessonOnDate,
  findNextLessonInCurrentWeek,
} from '../../utils/notesLessonHelper';
import {
  X,
  Calendar,
  Clock,
  Bold,
  Italic,
  Strikethrough,
  List,
  Trash2,
} from 'lucide-react';

interface Props {
  targetName: string;
  initialDate: string; // YYYY-MM-DD
  initialSubject?: string;
  initialLessonNum?: number | null;
  initialSlotType?: SlotType;
  existingNote?: ScheduleNote | null;
  onSave: (note: Omit<ScheduleNote, 'id' | 'createdAt'>) => void;
  onDelete?: (id: string) => void;
  onClose: () => void;
}

function isSlotInPast(selectedDateIso: string, slot: SlotType, lsnNum: number | null): boolean {
  const todayIso = new Date().toISOString().split('T')[0];
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

function formatRussianDateWithDay(iso: string): string {
  try {
    const parts = iso.split('-');
    if (parts.length === 3) {
      const d = new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
      const str = d.toLocaleDateString('ru-RU', { weekday: 'short', day: 'numeric', month: 'short' });
      return str.charAt(0).toUpperCase() + str.slice(1);
    }
  } catch {}
  return iso;
}

export const NoteEditDialog: React.FC<Props> = ({
  targetName,
  initialDate,
  initialSubject = '',
  initialLessonNum = null,
  initialSlotType = 'lesson',
  existingNote,
  onSave,
  onDelete,
  onClose,
}) => {
  const currentSchedule = useScheduleStore((s) => s.currentSchedule);
  const studentSubgroup = usePreferencesStore((s) => s.studentSubgroup);
  const studentTechnologySubgroup = usePreferencesStore(
    (s) => s.studentTechnologySubgroup
  );

  const todayIso = new Date().toISOString().split('T')[0];

  // Ensure initial date is not before today
  const safeInitialDate = existingNote?.date || (initialDate < todayIso ? todayIso : initialDate);

  const [type, setType] = useState<NoteType>(existingNote?.type || 'homework');
  const [date, setDate] = useState(safeInitialDate);
  const [slotType, setSlotType] = useState<SlotType>(
    existingNote?.slotType || (type === 'homework' ? 'lesson' : initialSlotType)
  );
  const [isTransferPending, setIsTransferPending] = useState(
    existingNote?.transferPending || false
  );

  // Helper to determine subject for a given lesson number on a given date (with changes & cancellations)
  const getSubjectForLesson = (num: number, dateIso: string): string => {
    return getSubjectForLessonOnDate(
      currentSchedule,
      num,
      dateIso,
      targetName,
      studentSubgroup,
      studentTechnologySubgroup
    );
  };

  // Find first upcoming lesson for today so we don't default to a past lesson
  const firstUpcomingLesson = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].find(
    (n) => !isSlotInPast(safeInitialDate, 'lesson', n)
  ) || 1;

  const defaultLessonNum = existingNote?.lessonNum !== undefined
    ? existingNote.lessonNum
    : (initialLessonNum || firstUpcomingLesson);

  const [lessonNum, setLessonNum] = useState<number | null>(defaultLessonNum);

  const [subject, setSubject] = useState(() => {
    if (existingNote?.subject) return existingNote.subject;
    if (initialSubject) return initialSubject;
    return defaultLessonNum ? getSubjectForLesson(defaultLessonNum, safeInitialDate) : '';
  });

  const [text, setText] = useState(existingNote?.text || '');
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const dateInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const isMiddleSchool = /^[5-8]/.test(targetName.trim());
  const autoSubgroup = subject.toLowerCase().includes('технолог') && isMiddleSchool
    ? studentTechnologySubgroup
    : studentSubgroup;

  // Handle lesson number selection - adaptive auto-determination of subject!
  const handleSelectLessonNum = (num: number) => {
    setLessonNum(num);
    setIsTransferPending(false);
    const foundSubject = getSubjectForLesson(num, date);
    if (foundSubject) {
      setSubject(foundSubject);
    }
  };

  // Handle date change from DatePicker - updates subject adaptively for selected lesson
  const handleDateChange = (newDate: string) => {
    setDate(newDate);
    setIsTransferPending(false);
    if (lessonNum) {
      const foundSubject = getSubjectForLesson(lessonNum, newDate);
      if (foundSubject) {
        setSubject(foundSubject);
      }
    }
  };

  // "Задать к следующему уроку" calculation: Phase 1
  const handleSetToNextLesson = () => {
    if (!subject.trim()) {
      alert('Сначала укажите название предмета.');
      return;
    }
    if (!currentSchedule) {
      alert('Расписание не загружено.');
      return;
    }

    const targetSg = autoSubgroup;
    const nextInWeek = findNextLessonInCurrentWeek(
      currentSchedule,
      subject,
      targetSg,
      date,
      lessonNum,
      false,
      studentTechnologySubgroup,
      isMiddleSchool
    );

    if (nextInWeek) {
      setDate(nextInWeek[0]);
      setLessonNum(nextInWeek[1]);
      setSlotType('lesson');
      setIsTransferPending(false);
      const nextSubj = getSubjectForLesson(nextInWeek[1], nextInWeek[0]);
      if (nextSubj) setSubject(nextSubj);
      return;
    }

    // No more lessons this week -> deferred transfer to next week!
    setIsTransferPending(true);
    alert(
      'На этой неделе больше нет уроков. Перенос запланирован на след. неделю (к ближайшему уроку).'
    );
  };

  // Markdown formatting helper
  const insertFormatting = (prefix: string, suffix: string) => {
    const el = textareaRef.current;
    if (!el) return;

    const start = el.selectionStart;
    const end = el.selectionEnd;
    const selected = text.substring(start, end);
    const replacement = prefix + selected + suffix;

    const nextText = text.substring(0, start) + replacement + text.substring(end);
    setText(nextText);

    setTimeout(() => {
      el.focus();
      const newPos = start + prefix.length + selected.length;
      el.setSelectionRange(newPos, newPos);
    }, 0);
  };

  // Check if slot is in the past - BOTH notes and homework cannot be set to the past!
  const finalSlot = type === 'homework' ? 'lesson' : slotType;
  const finalLessonNum = (type === 'homework' || finalSlot === 'lesson') ? lessonNum : null;
  const isSlotInPastValue = isSlotInPast(date, finalSlot, finalLessonNum);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!text.trim()) return;

    if (isSlotInPastValue) {
      alert('Выбранное время уже прошло. Нельзя сохранить запись на прошедшее время.');
      return;
    }

    onSave({
      targetName,
      type,
      subject: type === 'homework' ? (subject.trim() || null) : (finalSlot === 'lesson' ? (subject.trim() || null) : null),
      subgroup: autoSubgroup,
      date,
      slotType: finalSlot,
      lessonNum: finalLessonNum,
      text: text.trim(),
      isCompleted: existingNote?.isCompleted || false,
      transferPending: isTransferPending,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-[var(--md-surface)] border border-[var(--md-border)] rounded-3xl p-5 max-w-md w-full max-h-[90vh] overflow-y-auto shadow-2xl relative">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)] p-1 rounded-full hover:bg-[var(--md-surface-variant)]"
        >
          <X className="w-5 h-5" />
        </button>

        <h3 className="font-extrabold text-base text-[var(--md-text-primary)] mb-3">
          {existingNote ? 'Редактировать запись' : 'Новая запись'}
        </h3>

        <form onSubmit={handleSubmit} className="space-y-3.5">
          {/* Note Type Toggle: Записать ДЗ / Своя заметка */}
          <div className="flex bg-[var(--md-surface-variant)] p-1 rounded-2xl">
            <button
              type="button"
              onClick={() => {
                setType('homework');
                setSlotType('lesson');
              }}
              className={`flex-1 py-1.5 rounded-xl text-xs font-bold transition-all ${
                type === 'homework'
                  ? 'bg-[var(--md-surface)] text-[var(--nika-subject)] shadow-sm'
                  : 'text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)]'
              }`}
            >
              Записать ДЗ
            </button>
            <button
              type="button"
              onClick={() => setType('note')}
              className={`flex-1 py-1.5 rounded-xl text-xs font-bold transition-all ${
                type === 'note'
                  ? 'bg-[var(--md-surface)] text-[var(--nika-subject)] shadow-sm'
                  : 'text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)]'
              }`}
            >
              Своя заметка
            </button>
          </div>

          {/* Clean Date Row without duplicate week ribbon */}
          <div className="flex items-center justify-between py-1">
            <span className="text-xs font-semibold text-[var(--md-text-primary)]">
              Дата:
            </span>
            <div className="flex items-center gap-2">
              {isTransferPending && (
                <span className="px-2 py-0.5 rounded-lg text-[10px] font-bold bg-[var(--nika-subject)]/15 text-[var(--nika-subject)] border border-[var(--nika-subject)]/30">
                  Перенос на след. неделю
                </span>
              )}
              <div className="relative">
                <button
                  type="button"
                  onClick={() => {
                    const input = dateInputRef.current;
                    if (input) {
                      if (typeof (input as any).showPicker === 'function') {
                        try {
                          (input as any).showPicker();
                          return;
                        } catch {}
                      }
                      input.focus();
                      input.click();
                    }
                  }}
                  className="flex items-center gap-2 px-3 py-1.5 bg-[var(--md-surface-variant)]/60 border border-[var(--md-border-light)] rounded-xl hover:border-[var(--nika-subject)] transition-all cursor-pointer"
                >
                  <Calendar className="w-4 h-4 text-[var(--nika-subject)] pointer-events-none" />
                  <span className="text-xs font-bold text-[var(--md-text-primary)] pointer-events-none">
                    {formatRussianDateWithDay(date)}
                  </span>
                </button>
                <input
                  ref={dateInputRef}
                  type="date"
                  value={date}
                  min={todayIso}
                  onChange={(e) => handleDateChange(e.target.value)}
                  className="opacity-0 absolute inset-0 w-full h-full pointer-events-none -z-10"
                  tabIndex={-1}
                />
              </div>
            </div>
          </div>

          {/* "Задать к следующему уроку" button - ALWAYS visible for homework */}
          {type === 'homework' && (
            <button
              type="button"
              onClick={handleSetToNextLesson}
              className="w-full py-2 px-3 rounded-xl bg-[var(--nika-active-bg)]/40 hover:bg-[var(--nika-active-bg)]/70 text-[var(--nika-subject)] font-bold text-xs flex items-center justify-center gap-1.5 transition-all border border-[var(--nika-subject)]/30 shadow-xs"
            >
              <Clock className="w-3.5 h-3.5" />
              <span>Задать к следующему уроку</span>
            </button>
          )}

          {/* Time Slot Selection (for Notes: На весь день, Перед уроками, Урок, После уроков) */}
          {type === 'note' && (
            <div>
              <label className="text-[11px] font-semibold text-[var(--md-text-muted)] uppercase tracking-wider block mb-1">
                Привязка по времени
              </label>
              <div className="grid grid-cols-4 gap-1.5">
                {[
                  { id: 'day_note', label: 'На весь день' },
                  { id: 'before_lessons', label: 'Перед уроками' },
                  { id: 'lesson', label: 'Урок' },
                  { id: 'after_lessons', label: 'После уроков' },
                ].map((s) => {
                  const isSel = slotType === s.id;
                  const isPast = isSlotInPast(date, s.id as SlotType, null);
                  return (
                    <button
                      key={s.id}
                      type="button"
                      disabled={isPast && s.id !== 'day_note'}
                      onClick={() => setSlotType(s.id as SlotType)}
                      className={`py-1.5 px-1 rounded-xl text-[11px] font-semibold transition-all text-center leading-tight ${
                        isSel
                          ? 'bg-[var(--nika-subject)] text-[var(--md-on-primary)] font-bold shadow-xs'
                          : isPast && s.id !== 'day_note'
                          ? 'bg-[var(--md-surface-variant)]/30 text-[var(--md-text-muted)]/40 border border-transparent cursor-not-allowed'
                          : 'bg-[var(--md-surface-variant)]/60 text-[var(--md-text-secondary)] hover:bg-[var(--md-surface-variant)] border border-[var(--md-border-light)]'
                      }`}
                    >
                      {s.label}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Lesson Number Selection Ribbon (1..12) */}
          {(type === 'homework' || slotType === 'lesson') && (
            <div>
              <label className="text-[11px] font-semibold text-[var(--md-text-muted)] uppercase tracking-wider block mb-1">
                Номер урока
              </label>
              <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none">
                {Array.from({ length: 12 }, (_, i) => i + 1).map((num) => {
                  const isSel = lessonNum === num;
                  const isPast = isSlotInPast(date, 'lesson', num);
                  return (
                    <button
                      key={num}
                      type="button"
                      disabled={isPast}
                      onClick={() => handleSelectLessonNum(num)}
                      className={`w-8 h-8 rounded-xl text-xs font-bold shrink-0 transition-all flex items-center justify-center ${
                        isSel
                          ? 'bg-[var(--nika-subject)] text-[var(--md-on-primary)] shadow-xs ring-1 ring-[var(--nika-subject)]'
                          : isPast
                          ? 'bg-[var(--md-surface-variant)]/30 text-[var(--md-text-muted)]/40 cursor-not-allowed border border-transparent'
                          : 'bg-[var(--md-surface-variant)]/60 text-[var(--md-text-primary)] border border-[var(--md-border-light)] hover:bg-[var(--md-surface-variant)]'
                      }`}
                    >
                      {num}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Subject Field (for Homework or Lesson Slot) */}
          {(type === 'homework' || slotType === 'lesson') && (
            <div>
              <label className="text-[11px] font-semibold text-[var(--md-text-muted)] uppercase tracking-wider block mb-1">
                Предмет
              </label>
              <input
                type="text"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                placeholder="Название предмета..."
                className="w-full px-3 py-2 bg-[var(--md-surface-variant)]/60 border border-[var(--md-border-light)] rounded-xl text-sm text-[var(--md-text-primary)] outline-none focus:border-[var(--nika-subject)]"
              />
            </div>
          )}

          {/* Content Field with Markdown Toolbar */}
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="text-[11px] font-semibold text-[var(--md-text-muted)] uppercase tracking-wider">
                {type === 'homework' ? 'Задание' : 'Содержание'}
              </label>
              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => insertFormatting('**', '**')}
                  className="p-1 rounded-md text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]"
                  title="Жирный"
                >
                  <Bold className="w-3.5 h-3.5" />
                </button>
                <button
                  type="button"
                  onClick={() => insertFormatting('*', '*')}
                  className="p-1 rounded-md text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]"
                  title="Курсив"
                >
                  <Italic className="w-3.5 h-3.5" />
                </button>
                <button
                  type="button"
                  onClick={() => insertFormatting('~~', '~~')}
                  className="p-1 rounded-md text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]"
                  title="Зачёркнутый"
                >
                  <Strikethrough className="w-3.5 h-3.5" />
                </button>
                <button
                  type="button"
                  onClick={() => insertFormatting('\n- ', '')}
                  className="p-1 rounded-md text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]"
                  title="Список"
                >
                  <List className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            <textarea
              ref={textareaRef}
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder={type === 'homework' ? 'Текст задания, параграф, упражнение...' : 'Текст заметки...'}
              rows={3}
              className="w-full px-3 py-2 bg-[var(--md-surface-variant)]/60 border border-[var(--md-border-light)] rounded-xl text-sm text-[var(--md-text-primary)] outline-none focus:border-[var(--nika-subject)] resize-none"
              autoFocus
            />

            {isSlotInPastValue && (
              <p className="text-xs text-red-500 font-semibold mt-1">
                Выбранное время уже прошло. Нельзя сохранить запись на прошедшее время.
              </p>
            )}
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-between pt-2">
            {existingNote && onDelete ? (
              <button
                type="button"
                onClick={() => {
                  onDelete(existingNote.id);
                  onClose();
                }}
                className="flex items-center gap-1 text-xs text-red-500 font-bold hover:underline"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Удалить</span>
              </button>
            ) : (
              <div />
            )}

            <div className="flex gap-2">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-[var(--md-text-secondary)] hover:bg-[var(--md-surface-variant)]"
              >
                Отмена
              </button>
              <button
                type="submit"
                disabled={!text.trim() || isSlotInPastValue}
                className="px-4 py-2 rounded-xl text-xs font-bold bg-[var(--nika-subject)] text-[var(--md-on-primary)] hover:opacity-90 disabled:opacity-40 transition-all shadow-sm"
              >
                Сохранить
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};
