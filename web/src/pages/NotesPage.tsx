import React, { useState, useEffect } from 'react';
import { useNotesStore } from '../store/notesStore';
import { usePreferencesStore } from '../store/preferencesStore';
import { useScheduleStore } from '../store/scheduleStore';
import { TargetSelector } from '../components/schedule/TargetSelector';
import { NoteEditDialog } from '../components/notes/NoteEditDialog';
import { Plus, CheckCircle2, Circle, Trash2, Calendar, Clock } from 'lucide-react';
import { ScheduleNote } from '../types/notes';
import { useSwipeGesture } from '../hooks/useSwipeGesture';
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
  const transferHomework = useNotesStore((s) => s.transferHomework);
  const markTransferPending = useNotesStore((s) => s.markTransferPending);
  const updateNotes = useNotesStore((s) => s.updateNotes);

  const [filterType, setFilterType] = useState<FilterType>('all');
  const [editingNote, setEditingNote] = useState<ScheduleNote | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  const todayIso = new Date().toISOString().split('T')[0];

  // Target-filtered notes
  const targetNotes = notes.filter((n) => n.targetName === targetName);

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
      {/* Target Selector Header - matched with Schedule and Archive */}
      <div className="px-3 pt-2.5 pb-1.5 bg-[var(--md-surface)]">
        <TargetSelector onTargetChanged={() => loadSchedule()} />
      </div>

      <div className="p-3 space-y-3 flex-1 flex flex-col">
        {/* Filter Chips (Все, ДЗ, Заметки, Долги) */}
        <div data-no-swipe="true" className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none">
          <button
            onClick={() => setFilterType('all')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
              filterType === 'all'
                ? 'bg-[var(--nika-subject)] text-[var(--md-on-primary)] shadow-sm'
                : 'bg-[var(--md-surface)] border border-[var(--md-border)] text-[var(--md-text-secondary)]'
            }`}
          >
            Все
          </button>

          {!isTeacherMode && (
            <button
              onClick={() => setFilterType('homework')}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                filterType === 'homework'
                  ? 'bg-[var(--nika-subject)] text-[var(--md-on-primary)] shadow-sm'
                  : 'bg-[var(--md-surface)] border border-[var(--md-border)] text-[var(--md-text-secondary)]'
              }`}
            >
              ДЗ
            </button>
          )}

        <button
          onClick={() => setFilterType('note')}
          className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
            filterType === 'note'
              ? 'bg-[var(--nika-subject)] text-[var(--md-on-primary)] shadow-sm'
              : 'bg-[var(--md-surface)] border border-[var(--md-border)] text-[var(--md-text-secondary)]'
          }`}
        >
          Заметки
        </button>

        {!isTeacherMode && (
          <button
            onClick={() => setFilterType('debts')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-all ${
              filterType === 'debts'
                ? 'bg-red-500 text-white shadow-sm'
                : 'bg-[var(--md-surface)] border border-[var(--md-border)] text-[var(--md-text-secondary)]'
            }`}
          >
            <span>Долги</span>
            {debts.length > 0 && (
              <span
                className={`px-1.5 py-0.2 rounded-full text-[10px] font-bold ${
                  filterType === 'debts'
                    ? 'bg-white text-red-600'
                    : 'bg-red-500 text-white'
                }`}
              >
                {debts.length}
              </span>
            )}
          </button>
        )}
      </div>

      {/* Notes List */}
      {filteredNotes.length === 0 ? (
        <div className="py-20 text-center bg-[var(--md-surface)] rounded-3xl border border-[var(--md-border)] p-6">
          <p className="font-bold text-sm text-[var(--md-text-primary)]">
            {filterType === 'debts'
              ? 'Задолженностей нет'
              : 'Заметок не найдено'}
          </p>
          <p className="text-xs text-[var(--md-text-muted)] mt-1">
            {filterType === 'debts'
              ? 'Все домашние задания выполнены вовремя'
              : 'Нажмите кнопку +, чтобы создать новое задание или заметку'}
          </p>
        </div>
      ) : (
        <div className="space-y-2.5 pb-24">
          {filteredNotes.map((note) => {
            const isPast = note.date < todayIso;

            return (
              <div
                key={note.id}
                className={`p-3.5 rounded-2xl border transition-all flex items-start justify-between gap-3 ${
                  note.isCompleted
                    ? 'bg-[var(--md-surface)]/60 border-[var(--md-border-light)] opacity-50'
                    : note.type === 'homework' && isPast
                    ? 'bg-red-500/10 border-red-500/30'
                    : 'bg-[var(--md-surface)] border-[var(--md-border)] shadow-sm'
                }`}
              >
                {/* Complete checkbox */}
                <button
                  onClick={() => toggleCompleted(note.id)}
                  className="mt-0.5 text-[var(--nika-subject)] shrink-0"
                >
                  {note.isCompleted ? (
                    <CheckCircle2 className="w-5 h-5 fill-[var(--nika-subject)] text-white" />
                  ) : (
                    <Circle className="w-5 h-5 text-[var(--md-text-muted)] hover:text-[var(--nika-subject)]" />
                  )}
                </button>

                {/* Content */}
                <div
                  className="flex-1 cursor-pointer"
                  onClick={() => {
                    setEditingNote(note);
                    setIsDialogOpen(true);
                  }}
                >
                  <div className="flex items-center gap-2 mb-1 flex-wrap">
                    <span
                      className={`text-[10px] font-extrabold uppercase px-2 py-0.5 rounded-full ${
                        note.type === 'homework'
                          ? isPast && !note.isCompleted
                            ? 'bg-red-500 text-white'
                            : 'bg-[var(--nika-active-bg)] text-[var(--nika-subject)]'
                          : 'bg-[var(--md-surface-variant)] text-[var(--md-text-secondary)]'
                      }`}
                    >
                      {note.type === 'homework' ? 'ДЗ' : 'Заметка'}
                    </span>

                    {note.subject && (
                      <span className="font-bold text-xs text-[var(--md-text-primary)]">
                        {note.subject}
                      </span>
                    )}

                    {note.lessonNum && (
                      <span className="text-[11px] text-[var(--md-text-muted)]">
                        {note.lessonNum} урок
                      </span>
                    )}
                  </div>

                  <p
                    className={`text-xs text-[var(--md-text-primary)] whitespace-pre-wrap leading-relaxed ${
                      note.isCompleted ? 'line-through text-[var(--md-text-muted)]' : ''
                    }`}
                  >
                    {note.text}
                  </p>

                  <div className="flex items-center gap-2 mt-2 text-[10px] text-[var(--md-text-muted)]">
                    <Calendar className="w-3 h-3" />
                    {note.transferPending ? (
                      <span className="px-1.5 py-0.5 rounded font-bold bg-[var(--nika-subject)]/15 text-[var(--nika-subject)]">
                        Перенос на след. неделю
                      </span>
                    ) : (
                      <span>К {note.date}</span>
                    )}
                    {isPast && !note.isCompleted && !note.transferPending && note.type === 'homework' && (
                      <span className="font-bold text-red-500">Просрочено</span>
                    )}
                  </div>
                </div>

                {/* Action buttons */}
                <div className="flex items-center gap-1 shrink-0">
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
                      className="px-2 py-1 rounded-lg bg-[var(--nika-subject)]/10 text-[var(--nika-subject)] hover:bg-[var(--nika-subject)]/20 transition-colors flex items-center gap-1 text-[11px] font-semibold"
                      title="Перенести на следующий урок"
                    >
                      <Clock className="w-3.5 h-3.5" />
                      <span>На след. урок</span>
                    </button>
                  )}

                  <button
                    onClick={() => deleteNote(note.id)}
                    className="text-[var(--md-text-muted)] hover:text-red-500 transition-colors p-1"
                    title="Удалить"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
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
        className="fixed bottom-20 right-5 z-30 w-14 h-14 rounded-full bg-[var(--nika-subject)] text-[var(--md-on-primary)] shadow-xl flex items-center justify-center hover:scale-105 active:scale-95 transition-all"
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
