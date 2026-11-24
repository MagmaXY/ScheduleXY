import React, { useState, useEffect } from 'react';
import { useNotesStore } from '../store/notesStore';
import { usePreferencesStore } from '../store/preferencesStore';
import { useScheduleStore } from '../store/scheduleStore';
import { TargetSelector } from '../components/schedule/TargetSelector';
import { NoteEditDialog } from '../components/notes/NoteEditDialog';
import { Plus, Check, Edit3, Trash2, Clock } from 'lucide-react';
import { ScheduleNote } from '../types/notes';
import { useSwipeGesture } from '../hooks/useSwipeGesture';
import { toLocalDateIso } from '../utils/dateUtils';
import {
  findNextLessonInCurrentWeek,
  resolvePendingTransfers,
  checkAndRelocateCancelledHomework,
} from '../utils/notesLessonHelper';

type FilterType = 'all' | 'homework' | 'note' | 'debts';

export const NotesPage: React.FC = () => {
  const targetName = usePreferencesStore((s) => s.targetName);
  const targetType = usePreferencesStore((s) => s.targetType);
  const loadSchedule = useScheduleStore((s) => s.loadSchedule);
  const isTeacherMode = targetType === 'teacher';
  const currentSchedule = useScheduleStore((s) => s.currentSchedule);
  const studentSubgroup = usePreferencesStore((s) => s.studentSubgroup);
  const studentTechnologySubgroup = usePreferencesStore((s) => s.studentTechnologySubgroup);

  const notes = useNotesStore((s) => s.notes);
  const addNote = useNotesStore((s) => s.addNote);
  const updateNote = useNotesStore((s) => s.updateNote);
  const deleteNote = useNotesStore((s) => s.deleteNote);
  const toggleCompleted = useNotesStore((s) => s.toggleCompleted);
  const cleanupOldCompleted = useNotesStore((s) => s.cleanupOldCompleted);
  const transferHomework = useNotesStore((s) => s.transferHomework);
  const markTransferPending = useNotesStore((s) => s.markTransferPending);
  const updateNotes = useNotesStore((s) => s.updateNotes);

  const [filterType, setFilterType] = useState<FilterType>('all');
  const [editingNote, setEditingNote] = useState<ScheduleNote | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  const todayIso = toLocalDateIso();

  // Target-filtered notes
  const targetNotes = notes.filter((n) => n.targetName === targetName);
  const completedCount = targetNotes.filter((n) => n.isCompleted).length;

  const formatDateWithDay = (dateStr: string): string => {
    try {
      const [y, m, d] = dateStr.split('-').map(Number);
      const date = new Date(y, m - 1, d);
      const dayNames = ['вс', 'пн', 'вт', 'ср', 'чт', 'пт', 'сб'];
      const monthNames = ['янв.', 'февр.', 'мар.', 'апр.', 'мая', 'июн.', 'июл.', 'авг.', 'сент.', 'окт.', 'нояб.', 'дек.'];
      return `${d} ${monthNames[date.getMonth()]}, ${dayNames[date.getDay()]}`;
    } catch {
      return dateStr;
    }
  };

  // Debts: Overdue uncompleted homework or transferPending homework
  const debts = targetNotes.filter(
    (n) => n.type === 'homework' && !n.isCompleted && (n.date < todayIso || n.transferPending)
  );

  const filteredNotes = targetNotes.filter((n) => {
    if (filterType === 'all') {
      return n.date >= todayIso || n.isCompleted || n.transferPending;
    }
    if (filterType === 'homework') {
      return n.type === 'homework' && (n.date >= todayIso || n.transferPending);
    }
    if (filterType === 'note') {
      return n.type === 'note' && n.date >= todayIso;
    }
    if (filterType === 'debts') {
      return n.type === 'homework' && !n.isCompleted && (n.date < todayIso || n.transferPending);
    }
    return true;
  });

  // Automatically trigger Phase 2 & Phase 3 whenever schedule updates
  useEffect(() => {
    if (!currentSchedule) return;

    // Phase 3: Check and relocate cancelled homework
    const { updatedNotes: afterRelocate } = checkAndRelocateCancelledHomework(
      notes,
      currentSchedule,
      studentSubgroup,
      studentTechnologySubgroup,
      isTeacherMode ? false : /^[5-8]/.test(targetName.trim())
    );

    // Phase 2: Resolve pending transfers
    const afterResolve = resolvePendingTransfers(
      afterRelocate,
      currentSchedule,
      studentSubgroup,
      studentTechnologySubgroup,
      isTeacherMode ? false : /^[5-8]/.test(targetName.trim())
    );

    // If changes occurred, save to store
    if (JSON.stringify(afterResolve) !== JSON.stringify(notes)) {
      updateNotes(afterResolve);
    }
  }, [currentSchedule, notes, targetName, studentSubgroup, studentTechnologySubgroup, isTeacherMode, updateNotes]);

  const categories: FilterType[] = isTeacherMode
    ? ['all', 'note']
    : ['all', 'homework', 'note', 'debts'];

  // Swipe gestures for touch and LMB mouse drag
  const swipeHandlers = useSwipeGesture({
    onSwipeLeft: () => {
      const curIdx = categories.indexOf(filterType);
      const nextIdx = (curIdx + 1) % categories.length;
      setFilterType(categories[nextIdx]);
    },
    onSwipeRight: () => {
      const curIdx = categories.indexOf(filterType);
      const prevIdx = (curIdx - 1 + categories.length) % categories.length;
      setFilterType(categories[prevIdx]);
    },
  });

  // Arrow key and Digit hotkey navigation for categories
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName)) return;
      const curIdx = categories.indexOf(filterType);
      if (e.key === 'ArrowRight') {
        const nextIdx = (curIdx + 1) % categories.length;
        setFilterType(categories[nextIdx]);
      } else if (e.key === 'ArrowLeft') {
        const prevIdx = (curIdx - 1 + categories.length) % categories.length;
        setFilterType(categories[prevIdx]);
      } else if (e.key >= '1' && e.key <= '4') {
        const num = parseInt(e.key, 10);
        if (num <= categories.length) {
          setFilterType(categories[num - 1]);
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [filterType, categories]);

  return (
    <div
      {...swipeHandlers}
      className="max-w-4xl mx-auto flex flex-col min-h-screen bg-[var(--md-background)] relative lg:border-x border-[var(--md-border-light)]"
    >
      {/* Top Unified Header Bar */}
      <div className="bg-[var(--md-surface)] border-b border-[var(--md-border-light)] transition-colors">
        <div className="px-3 pt-2.5 pb-1">
          <TargetSelector onTargetChanged={() => loadSchedule()} />
        </div>
        <div data-no-swipe="true" className="flex items-center justify-between px-3 pb-2 pt-0.5">
          <div className="flex items-center gap-1.5 overflow-x-auto scrollbar-none">
            <button
              onClick={() => setFilterType('all')}
              className={`h-8 px-2.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all border ${
                filterType === 'all'
                  ? 'bg-[var(--md-surface-variant)] border-[var(--md-border)] text-[var(--md-text-primary)] font-bold shadow-sm'
                  : 'border-[var(--md-border)] text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
              }`}
            >
              Все
            </button>

            {!isTeacherMode && (
              <button
                onClick={() => setFilterType('homework')}
                className={`h-8 px-2.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all border ${
                  filterType === 'homework'
                    ? 'bg-[var(--md-surface-variant)] border-[var(--md-border)] text-[var(--md-text-primary)] font-bold shadow-sm'
                    : 'border-[var(--md-border)] text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
                }`}
              >
                ДЗ
              </button>
            )}

            <button
              onClick={() => setFilterType('note')}
              className={`h-8 px-2.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all border ${
                filterType === 'note'
                  ? 'bg-[var(--md-surface-variant)] border-[var(--md-border)] text-[var(--md-text-primary)] font-bold shadow-sm'
                  : 'border-[var(--md-border)] text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
              }`}
            >
              Заметки
            </button>

            {!isTeacherMode && (
              <button
                onClick={() => setFilterType('debts')}
                className={`h-8 px-2.5 rounded-lg text-xs font-semibold whitespace-nowrap flex items-center gap-1.5 transition-all border ${
                  filterType === 'debts'
                    ? 'bg-red-500/20 border-red-500/50 text-red-600 dark:text-red-400 font-bold shadow-sm'
                    : 'border-[var(--md-border)] text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
                }`}
              >
                <span>Долги</span>
                {debts.length > 0 && (
                  <span
                    className={`px-1.5 py-0.2 rounded-full text-[10px] font-bold ${
                      filterType === 'debts'
                        ? 'bg-red-600 text-white'
                        : 'bg-red-500 text-white'
                    }`}
                  >
                    {debts.length}
                  </span>
                )}
              </button>
            )}
          </div>

          {completedCount > 0 && (
            <button
              onClick={() => cleanupOldCompleted(0)}
              className="text-xs text-[var(--nika-subject)] hover:underline whitespace-nowrap ml-2 font-medium"
            >
              Очистить сделанные
            </button>
          )}
        </div>
      </div>

      <div className="p-3 space-y-3 flex-1 flex flex-col">
        {/* Notes List */}
        {filteredNotes.length === 0 ? (
          <div className="flex-1 flex items-center justify-center py-24 px-4">
            <p className="text-sm font-medium text-[var(--md-border)] text-center">
              {filterType === 'debts'
                ? 'Долгов нет! Все задания выполнены.'
                : filterType === 'homework'
                ? 'Нет актуальных домашних заданий.'
                : filterType === 'note'
                ? 'Нет заметок.'
                : 'Записей пока нет. Нажмите +, чтобы добавить!'}
            </p>
          </div>
        ) : (
          <div className="space-y-2.5 pb-24">
            {filteredNotes.map((note) => {
              const isPast = note.date < todayIso;
              const isDebt = note.type === 'homework' && !note.isCompleted && (isPast || note.transferPending);
              const typeLabel =
                note.type === 'homework'
                  ? note.subject || 'ДЗ'
                  : note.slotType === 'before_lessons'
                  ? 'До уроков'
                  : note.slotType === 'after_lessons'
                  ? 'После уроков'
                  : note.slotType === 'lesson'
                  ? `Урок ${note.lessonNum || ''}`
                  : 'Заметка';

              return (
                <div
                  key={note.id}
                  className={`p-3.5 rounded-xl border transition-all flex flex-col justify-between gap-2.5 ${
                    isDebt
                      ? 'bg-red-500/10 border-red-500/30'
                      : note.isCompleted
                      ? 'bg-[var(--md-surface)]/60 border-[var(--md-border-light)] opacity-60'
                      : 'bg-[var(--md-surface)] border-[var(--md-border-light)] shadow-xs'
                  }`}
                >
                  {/* Header Row: Type / Subject on Left, Date on Right */}
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-1.5 min-w-0">
                      <span
                        className={`font-bold text-sm truncate ${
                          isDebt
                            ? 'text-red-500'
                            : note.isCompleted
                            ? 'text-[var(--md-text-muted)]'
                            : 'text-[var(--md-text-primary)]'
                        }`}
                      >
                        {typeLabel}
                      </span>
                      {note.type === 'homework' && note.lessonNum && (
                        <span className="text-xs text-[var(--md-text-muted)] shrink-0">
                          ({note.lessonNum} урок)
                        </span>
                      )}
                    </div>

                    {/* Date badge on the right */}
                    {note.transferPending ? (
                      <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-[var(--nika-subject)]/15 text-[var(--nika-subject)] shrink-0">
                        Перенос на след. неделю
                      </span>
                    ) : (
                      <span
                        className={`text-xs font-semibold shrink-0 ${
                          isDebt ? 'text-red-500' : 'text-[var(--md-text-muted)]'
                        }`}
                      >
                        {formatDateWithDay(note.date)}
                      </span>
                    )}
                  </div>

                  {/* Note Body Text */}
                  <div
                    className="cursor-pointer"
                    onClick={() => {
                      setEditingNote(note);
                      setIsDialogOpen(true);
                    }}
                  >
                    <p
                      className={`text-[13.5px] leading-relaxed whitespace-pre-wrap ${
                        note.isCompleted
                          ? 'line-through text-[var(--md-text-muted)]'
                          : 'text-[var(--md-text-primary)]'
                      }`}
                    >
                      {note.text}
                    </p>
                  </div>

                  {/* Bottom Action Row */}
                  <div className="flex items-center justify-between gap-2 pt-1 border-t border-[var(--md-border-light)]/40">
                    {/* Status Chip (Сдано / Не сдано) */}
                    <button
                      onClick={() => toggleCompleted(note.id)}
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${
                        note.isCompleted
                          ? 'bg-[var(--md-primary-container)] text-[var(--md-on-primary-container)]'
                          : 'bg-[var(--md-surface-variant)] text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)]'
                      }`}
                    >
                      <Check className="w-3.5 h-3.5" />
                      <span>{note.isCompleted ? 'Сдано' : 'Не сдано'}</span>
                    </button>

                    {/* Transfer Button (if debt) + Edit Button next to Delete Button */}
                    <div className="flex items-center gap-1">
                      {note.type === 'homework' && !note.isCompleted && note.subject && !note.transferPending && (isPast || note.date === todayIso) && (
                        <button
                          onClick={() => {
                            const targetSg =
                              note.subgroup ??
                              (note.subject!.toLowerCase().includes('технолог') && /^[5-8]/.test(targetName.trim())
                                ? studentTechnologySubgroup
                                : studentSubgroup);

                            const nextInWeek = findNextLessonInCurrentWeek(
                              currentSchedule,
                              note.subject!,
                              targetSg,
                              todayIso,
                              note.date === todayIso ? note.lessonNum : null,
                              false,
                              studentTechnologySubgroup,
                              isTeacherMode ? false : /^[5-8]/.test(targetName.trim())
                            );

                            if (nextInWeek) {
                              transferHomework(note.id, nextInWeek[0], nextInWeek[1]);
                              alert(`Перенесено на ${nextInWeek[0]} (${nextInWeek[1]} урок)`);
                            } else {
                              markTransferPending(note.id);
                              alert('Перенесено на след. неделю (будет привязано к ближайшему уроку)');
                            }
                          }}
                          className="px-2 py-1 rounded-lg bg-[var(--nika-subject)]/10 text-[var(--nika-subject)] hover:bg-[var(--nika-subject)]/20 transition-colors flex items-center gap-1 text-[11px] font-semibold mr-1"
                          title="Перенести на следующий урок"
                        >
                          <Clock className="w-3.5 h-3.5" />
                          <span>На след. урок</span>
                        </button>
                      )}

                      <button
                        onClick={() => {
                          setEditingNote(note);
                          setIsDialogOpen(true);
                        }}
                        className="p-1.5 rounded-lg text-[var(--md-text-secondary)] hover:bg-[var(--md-surface-variant)] transition-colors"
                        title="Редактировать"
                      >
                        <Edit3 className="w-4 h-4" />
                      </button>

                      <button
                        onClick={() => deleteNote(note.id)}
                        className="p-1.5 rounded-lg text-[var(--md-text-secondary)] hover:text-red-500 hover:bg-red-500/10 transition-colors"
                        title="Удалить"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Floating Action Button (FAB) in the bottom-right corner */}
      <button
        onClick={() => {
          setEditingNote(null);
          setIsDialogOpen(true);
        }}
        className="fixed bottom-24 right-5 z-30 w-14 h-14 rounded-2xl bg-[var(--nika-subject)] text-[var(--md-on-primary)] shadow-xl flex items-center justify-center hover:scale-105 active:scale-95 transition-all"
        title="Создать заметку или ДЗ"
      >
        <Plus className="w-7 h-7" />
      </button>

      {/* Dialog */}
      {isDialogOpen && (
        <NoteEditDialog
          targetName={targetName}
          initialDate={todayIso}
          existingNote={editingNote}
          onSave={(data) => {
            if (editingNote) {
              updateNote({ ...editingNote, ...data });
            } else {
              addNote(data);
            }
          }}
          onDelete={(id) => deleteNote(id)}
          onClose={() => {
            setIsDialogOpen(false);
            setEditingNote(null);
          }}
        />
      )}
    </div>
  );
};
