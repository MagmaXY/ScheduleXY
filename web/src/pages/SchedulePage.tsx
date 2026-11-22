import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TargetSelector } from '../components/schedule/TargetSelector';
import { DaySelector } from '../components/common/DaySelector';
import { LessonCard } from '../components/schedule/LessonCard';
import { PairCard } from '../components/schedule/PairCard';
import { NoteEditDialog } from '../components/notes/NoteEditDialog';
import { useScheduleStore } from '../store/scheduleStore';
import { usePreferencesStore } from '../store/preferencesStore';
import { useNotesStore } from '../store/notesStore';
import { RealtimeState } from '../hooks/useRealtimeSync';
import { processLessons } from '../domain/schedulePairMerger';
import { getChangesForLesson, isScheduleForTeacher } from '../types/api';
import { Copy, Edit, Check } from 'lucide-react';
import { useSwipeGesture } from '../hooks/useSwipeGesture';

interface Props {
  realtimeState: RealtimeState;
}

export const SchedulePage: React.FC<Props> = ({ realtimeState }) => {
  const navigate = useNavigate();
  const currentSchedule = useScheduleStore((s) => s.currentSchedule);
  const selectedDayIndex = useScheduleStore((s) => s.selectedDayIndex);
  const setSelectedDayIndex = useScheduleStore((s) => s.setSelectedDayIndex);
  const loadSchedule = useScheduleStore((s) => s.loadSchedule);
  const isLoading = useScheduleStore((s) => s.isLoading);

  const targetType = usePreferencesStore((s) => s.targetType);
  const targetName = usePreferencesStore((s) => s.targetName);
  const setTargetType = usePreferencesStore((s) => s.setTargetType);
  const setTargetName = usePreferencesStore((s) => s.setTargetName);
  const showPairsSchedule = usePreferencesStore((s) => s.showPairsSchedule);
  const isFavorite = usePreferencesStore((s) => s.isFavorite(targetType, targetName));

  const notes = useNotesStore((s) => s.notes);
  const addNote = useNotesStore((s) => s.addNote);
  const toggleCompleted = useNotesStore((s) => s.toggleCompleted);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [noteInitialSubject, setNoteInitialSubject] = useState('');
  const [noteInitialLessonNum, setNoteInitialLessonNum] = useState<number | null>(null);
  const [copied, setCopied] = useState(false);

  const isTeacher = isScheduleForTeacher(currentSchedule);

  // Current day's schedule
  const daySchedule = currentSchedule?.days?.find(
    (d) => d.dayIndex === selectedDayIndex
  );
  const lessons = daySchedule?.lessons || [];

  // Compute processed lessons (pairs or single)
  const processedItems = processLessons(
    lessons,
    isTeacher,
    targetName,
    showPairsSchedule
  );

  // Formatted date label (e.g., "21 ноября")
  const getDateLabel = (): string => {
    const cal = new Date();
    if (currentSchedule?.weekStart) {
      const parts = currentSchedule.weekStart.split('-');
      if (parts.length === 3) {
        cal.setFullYear(parseInt(parts[0], 10));
        cal.setMonth(parseInt(parts[1], 10) - 1);
        cal.setDate(parseInt(parts[2], 10));
        cal.setDate(cal.getDate() + selectedDayIndex);
        return cal.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' });
      }
    }
    const dayOfWeek = (cal.getDay() + 6) % 7;
    cal.setDate(cal.getDate() - dayOfWeek + selectedDayIndex);
    return cal.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' });
  };

  const getIsoDate = (): string => {
    const cal = new Date();
    if (currentSchedule?.weekStart) {
      const parts = currentSchedule.weekStart.split('-');
      if (parts.length === 3) {
        cal.setFullYear(parseInt(parts[0], 10));
        cal.setMonth(parseInt(parts[1], 10) - 1);
        cal.setDate(parseInt(parts[2], 10));
        cal.setDate(cal.getDate() + selectedDayIndex);
        return cal.toISOString().split('T')[0];
      }
    }
    const dayOfWeek = (cal.getDay() + 6) % 7;
    cal.setDate(cal.getDate() - dayOfWeek + selectedDayIndex);
    return cal.toISOString().split('T')[0];
  };

  const currentDateIso = getIsoDate();
  const dateLabel = getDateLabel();

  const todayIso = new Date().toISOString().split('T')[0];
  const isThisWeek = (() => {
    if (!currentSchedule?.weekStart) return true;
    const now = new Date();
    const dow = (now.getDay() + 6) % 7;
    const mon = new Date(now);
    mon.setDate(mon.getDate() - dow);
    const curMondayIso = `${mon.getFullYear()}-${String(mon.getMonth() + 1).padStart(2, '0')}-${String(mon.getDate()).padStart(2, '0')}`;
    return currentSchedule.weekStart === curMondayIso;
  })();

  const nowDow = (new Date().getDay() + 6) % 7;
  const nowH = new Date().getHours();
  const isWeekendAfterSat3pm = nowDow === 6 || (nowDow === 5 && nowH >= 15);

  const activeDayIndex = (isThisWeek && !isWeekendAfterSat3pm && nowDow < 6) ? nowDow : -1;

  // Notes for selected day
  const dayNotes = notes.filter(
    (n) => n.targetName === targetName && n.date === currentDateIso
  );
  const beforeLessonsNote = dayNotes.find((n) => n.slotType === 'before_lessons');
  const afterLessonsNote = dayNotes.find((n) => n.slotType === 'after_lessons');

  const handleCopySchedule = () => {
    if (!daySchedule) return;
    const lines: string[] = [
      `📅 ${daySchedule.dayName}, ${dateLabel} (${targetName})`,
      '',
    ];

    if (beforeLessonsNote) {
      lines.push(`📌 До уроков: ${beforeLessonsNote.text}\n`);
    }

    lessons.forEach((l) => {
      const subj =
        l.subgroups.map((s) => s.subject).filter(Boolean).join(' / ') || '—';
      const room =
        l.subgroups.map((s) => s.room).filter(Boolean).join(', ') || '';
      const teacher =
        l.subgroups.map((s) => s.teacher).filter(Boolean).join(', ') || '';
      const info = [teacher, room ? `каб. ${room}` : ''].filter(Boolean).join(', ');
      lines.push(`${l.num}. ${subj}${info ? ` (${info})` : ''}`);
    });

    if (afterLessonsNote) {
      lines.push(`\n📌 После уроков: ${afterLessonsNote.text}`);
    }

    navigator.clipboard.writeText(lines.join('\n'));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleTeacherClassClick = (name: string) => {
    if (isTeacher) {
      setTargetType('class');
      setTargetName(name);
      loadSchedule('class', name, 0);
    } else {
      setTargetType('teacher');
      setTargetName(name);
      loadSchedule('teacher', name, 0);
    }
  };

  // Clicking date opens Calendar focused on this date
  const handleDateClick = () => {
    navigate('/calendar', { state: { date: currentDateIso } });
  };

  // Arrow key and Digit hotkey navigation for days (1..6)
  React.useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Don't trigger if user is typing in an input
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName)) return;
      if (e.key === 'ArrowRight') {
        setSelectedDayIndex(selectedDayIndex < 5 ? selectedDayIndex + 1 : 0);
      } else if (e.key === 'ArrowLeft') {
        setSelectedDayIndex(selectedDayIndex > 0 ? selectedDayIndex - 1 : 5);
      } else if (e.key >= '1' && e.key <= '6') {
        setSelectedDayIndex(parseInt(e.key, 10) - 1);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [selectedDayIndex, setSelectedDayIndex]);

  // Touch and LMB mouse drag swipe handling
  const swipeHandlers = useSwipeGesture({
    onSwipeLeft: () => {
      setSelectedDayIndex(selectedDayIndex < 5 ? selectedDayIndex + 1 : 0);
    },
    onSwipeRight: () => {
      setSelectedDayIndex(selectedDayIndex > 0 ? selectedDayIndex - 1 : 5);
    },
  });

  return (
    <div
      {...swipeHandlers}
      className="max-w-4xl mx-auto flex flex-col min-h-screen bg-[var(--md-background)] relative lg:border-x border-[var(--md-border-light)]"
    >
      {/* 1. Target Selector Header */}
      <div className="px-3 pt-2.5 pb-1.5 bg-[var(--md-surface)]">
        <TargetSelector onTargetChanged={() => loadSchedule()} />
      </div>

      {/* 2. Unified Day Selector with Clickable Date */}
      <DaySelector
        selectedDayIndex={selectedDayIndex}
        currentDayIndex={activeDayIndex}
        dateLabel={dateLabel}
        onDaySelected={setSelectedDayIndex}
        onDateClick={handleDateClick}
      />

      {/* 3. Floating Copy & Note Action Icons lowered down well below the day selector */}
      <div className="absolute right-2 top-[138px] z-20 flex flex-col items-center gap-1">
        {/* Copy button */}
        <button
          onClick={handleCopySchedule}
          className="w-9 h-9 flex items-center justify-center rounded-full text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/60 active:scale-95 transition-all"
          title="Скопировать расписание"
        >
          {copied ? <Check className="w-5 h-5 text-green-500" /> : <Copy className="w-5 h-5" />}
        </button>

        {/* Note / Pencil button (visible for classes or favorite) */}
        {!isTeacher && (
          <button
            onClick={() => {
              setNoteInitialLessonNum(null);
              setNoteInitialSubject('');
              setDialogOpen(true);
            }}
            className="w-9 h-9 flex items-center justify-center rounded-full text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/60 active:scale-95 transition-all"
            title="Записать ДЗ или заметку"
          >
            <Edit className="w-5 h-5" />
          </button>
        )}
      </div>

      {/* 4. "До уроков" Note Slot */}
      {beforeLessonsNote ? (
        <div
          onClick={() => toggleCompleted(beforeLessonsNote.id)}
          className={`mx-3 my-2 p-2.5 rounded-xl border flex items-center justify-between cursor-pointer transition-all ${
            beforeLessonsNote.isCompleted
              ? 'bg-[var(--md-surface)]/60 line-through opacity-60 text-[var(--md-text-muted)] border-[var(--md-border-light)]'
              : 'bg-[var(--nika-gold-bg)]/40 border-[var(--nika-gold)]/40 text-[var(--md-text-primary)]'
          }`}
        >
          <span className="text-xs font-semibold">
            📌 До уроков: {beforeLessonsNote.text}
          </span>
        </div>
      ) : null}

      {/* 5. Lessons / Pairs List */}
      <div className="flex-1 bg-[var(--md-surface)]">
        {isLoading ? (
          <div className="py-20 text-center">
            <div className="w-8 h-8 border-2 border-[var(--nika-subject)] border-t-transparent rounded-full animate-spin mx-auto mb-3" />
            <p className="text-xs text-[var(--md-text-muted)]">
              Загрузка расписания...
            </p>
          </div>
        ) : processedItems.length === 0 ? (
          <div className="py-20 text-center px-4">
            <p className="text-sm font-bold text-[var(--md-text-primary)]">
              В этот день занятий нет
            </p>
            <p className="text-xs text-[var(--md-text-muted)] mt-1">
              Отдыхайте или выберите другой день недели
            </p>
          </div>
        ) : (
          <div className="divide-y divide-[var(--md-border-light)]">
            {processedItems.map((item, idx) => {
              if (item.type === 'pair') {
                const ch1 = getChangesForLesson(
                  currentSchedule,
                  selectedDayIndex,
                  item.firstLesson.num
                );
                const ch2 = getChangesForLesson(
                  currentSchedule,
                  selectedDayIndex,
                  item.secondLesson.num
                );

                const isActive =
                  realtimeState.dayIndex === selectedDayIndex &&
                  realtimeState.bellState.type === 'in_lesson' &&
                  (realtimeState.bellState.num === item.firstLesson.num ||
                    realtimeState.bellState.num === item.secondLesson.num);

                const pairNotes = dayNotes.filter(
                  (n) =>
                    n.slotType === 'lesson' &&
                    (n.lessonNum === item.firstLesson.num || n.lessonNum === item.secondLesson.num)
                );

                return (
                  <PairCard
                    key={idx}
                    pairNum={item.pairNum}
                    pairRoman={item.pairRoman}
                    pairTimes={item.pairTimes}
                    firstLesson={item.firstLesson}
                    secondLesson={item.secondLesson}
                    dayIndex={selectedDayIndex}
                    changes1={ch1}
                    changes2={ch2}
                    isTeacherMode={isTeacher}
                    isActiveNow={isActive}
                    notes={pairNotes}
                    onToggleNoteCompleted={toggleCompleted}
                    userSubgroup={1}
                    userTechSubgroup={1}
                    onTeacherClassClick={handleTeacherClassClick}
                    onClick={
                      !isTeacher
                        ? () => {
                            setNoteInitialLessonNum(item.firstLesson.num);
                            setNoteInitialSubject(
                              item.firstLesson.subgroups[0]?.subject || ''
                            );
                            setDialogOpen(true);
                          }
                        : undefined
                    }
                  />
                );
              }

              const ch = getChangesForLesson(
                currentSchedule,
                selectedDayIndex,
                item.lesson.num
              );
              const isActive =
                realtimeState.dayIndex === selectedDayIndex &&
                realtimeState.bellState.type === 'in_lesson' &&
                realtimeState.bellState.num === item.lesson.num;

              const lessonNotes = dayNotes.filter(
                (n) => n.slotType === 'lesson' && n.lessonNum === item.lesson.num
              );

              return (
                <LessonCard
                  key={idx}
                  lesson={item.lesson}
                  dayIndex={selectedDayIndex}
                  changes={ch}
                  isTeacherMode={isTeacher}
                  isActiveNow={isActive}
                  notes={lessonNotes}
                  onToggleNoteCompleted={toggleCompleted}
                  onTeacherClassClick={handleTeacherClassClick}
                  onClick={
                    !isTeacher
                      ? () => {
                          setNoteInitialLessonNum(item.lesson.num);
                          setNoteInitialSubject(
                            item.lesson.subgroups[0]?.subject || ''
                          );
                          setDialogOpen(true);
                        }
                      : undefined
                  }
                />
              );
            })}
          </div>
        )}
      </div>

      {/* 6. "После уроков" Note Slot */}
      {afterLessonsNote ? (
        <div
          onClick={() => toggleCompleted(afterLessonsNote.id)}
          className={`mx-3 my-2 p-2.5 rounded-xl border flex items-center justify-between cursor-pointer transition-all ${
            afterLessonsNote.isCompleted
              ? 'bg-[var(--md-surface)]/60 line-through opacity-60 text-[var(--md-text-muted)] border-[var(--md-border-light)]'
              : 'bg-[var(--nika-gold-bg)]/40 border-[var(--nika-gold)]/40 text-[var(--md-text-primary)]'
          }`}
        >
          <span className="text-xs font-semibold">
            📌 После уроков: {afterLessonsNote.text}
          </span>
        </div>
      ) : null}

      {/* Note Edit Modal */}
      {dialogOpen && (
        <NoteEditDialog
          targetName={targetName}
          initialDate={currentDateIso}
          initialSubject={noteInitialSubject}
          initialLessonNum={noteInitialLessonNum}
          onSave={addNote}
          onClose={() => setDialogOpen(false)}
        />
      )}
    </div>
  );
};
