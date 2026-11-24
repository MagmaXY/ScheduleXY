import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TargetSelector } from '../components/schedule/TargetSelector';
import { DaySelector } from '../components/common/DaySelector';
import { LessonCard } from '../components/schedule/LessonCard';
import { PairCard } from '../components/schedule/PairCard';
import { NoteEditDialog } from '../components/notes/NoteEditDialog';
import { useScheduleStore } from '../store/scheduleStore';
import { toLocalDateIso, parseLocalDate } from '../utils/dateUtils';
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
    if (currentSchedule?.weekStart) {
      const d = parseLocalDate(currentSchedule.weekStart);
      d.setDate(d.getDate() + selectedDayIndex);
      return d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' });
    }
    const now = new Date();
    const dayOfWeek = (now.getDay() + 6) % 7;
    now.setDate(now.getDate() - dayOfWeek + selectedDayIndex);
    return now.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' });
  };

  const getIsoDate = (): string => {
    if (currentSchedule?.weekStart) {
      const d = parseLocalDate(currentSchedule.weekStart);
      d.setDate(d.getDate() + selectedDayIndex);
      return toLocalDateIso(d);
    }
    const cal = new Date();
    const dayOfWeek = (cal.getDay() + 6) % 7;
    cal.setDate(cal.getDate() - dayOfWeek + selectedDayIndex);
    return toLocalDateIso(cal);
  };

  const currentDateIso = getIsoDate();
  const dateLabel = getDateLabel();

  const todayIso = toLocalDateIso();
  const isThisWeek = (() => {
    if (!currentSchedule?.weekStart) return true;
    const now = new Date();
    const dow = (now.getDay() + 6) % 7;
    now.setDate(now.getDate() - dow);
    const curMondayIso = toLocalDateIso(now);
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

  const handleRoomClick = (room: string, lessonNum: number) => {
    navigate(`/freerooms?day=${selectedDayIndex}&lesson=${lessonNum}&room=${encodeURIComponent(room)}`);
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

      {/* 4. Lessons / Pairs List with embedded Before/After notes aligned with lesson content */}
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
            {/* "До уроков" Note Slot */}
            {beforeLessonsNote && (
              <div
                onClick={() => toggleCompleted(beforeLessonsNote.id)}
                className={`w-full flex items-center px-3.5 py-2.5 gap-2.5 cursor-pointer select-text transition-colors border-b border-[var(--md-border-light)] ${
                  beforeLessonsNote.isCompleted
                    ? 'bg-[var(--md-surface)]/60 opacity-60 text-[var(--md-text-muted)]'
                    : 'bg-[var(--md-surface)] text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
                }`}
              >
                {/* 76px width container (w-5 (20px) + gap-2.5 (10px) + w-12 (48px) = 78px approx) */}
                <div className="w-[74px] shrink-0 flex items-center justify-center text-center">
                  <span className="font-semibold text-[11px] leading-tight text-[var(--md-text-secondary)]">
                    До уроков
                  </span>
                </div>
                {/* Note text aligned with subject column */}
                <div className="flex-1 min-w-0 pr-2">
                  <p
                    className={`text-[12px] leading-snug break-words ${
                      beforeLessonsNote.isCompleted
                        ? 'line-through text-[var(--md-text-muted)]'
                        : 'text-[var(--md-text-secondary)] font-medium'
                    }`}
                  >
                    {beforeLessonsNote.text}
                  </p>
                </div>
              </div>
            )}

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

                const isToday = isThisWeek && realtimeState.dayIndex === selectedDayIndex;
                const activeLessonNum =
                  isToday && realtimeState.lessonBellState.type === 'in_lesson'
                    ? realtimeState.lessonBellState.num
                    : null;
                const activePairNum =
                  isToday && realtimeState.pairBellState.type === 'in_lesson'
                    ? realtimeState.pairBellState.num
                    : null;

                const isPairActive =
                  Boolean(
                    isToday &&
                      (activePairNum === item.pairNum ||
                        activeLessonNum === item.firstLesson.num ||
                        activeLessonNum === item.secondLesson.num)
                  );
                const isL1Active = Boolean(isToday && activeLessonNum === item.firstLesson.num);
                const isL2Active = Boolean(isToday && activeLessonNum === item.secondLesson.num);

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
                    isActiveNow={isPairActive}
                    isActiveL1={isL1Active}
                    isActiveL2={isL2Active}
                    notes={pairNotes}
                    onToggleNoteCompleted={toggleCompleted}
                    userSubgroup={1}
                    userTechSubgroup={1}
                    onTeacherClassClick={handleTeacherClassClick}
                    onRoomClick={handleRoomClick}
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
              const isToday = isThisWeek && realtimeState.dayIndex === selectedDayIndex;
              const isActive =
                Boolean(
                  isToday &&
                    realtimeState.lessonBellState.type === 'in_lesson' &&
                    realtimeState.lessonBellState.num === item.lesson.num
                );

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
                  onRoomClick={handleRoomClick}
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

            {/* "После уроков" Note Slot directly under the last lesson */}
            {afterLessonsNote && (
              <div
                onClick={() => toggleCompleted(afterLessonsNote.id)}
                className={`w-full flex items-center px-3.5 py-2.5 gap-2.5 cursor-pointer select-text transition-colors border-b border-[var(--md-border-light)] ${
                  afterLessonsNote.isCompleted
                    ? 'bg-[var(--md-surface)]/60 opacity-60 text-[var(--md-text-muted)]'
                    : 'bg-[var(--md-surface)] text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
                }`}
              >
                {/* 76px width container */}
                <div className="w-[74px] shrink-0 flex items-center justify-center text-center">
                  <span className="font-semibold text-[11px] leading-tight text-[var(--md-text-secondary)]">
                    После уроков
                  </span>
                </div>
                {/* Note text aligned with subject column */}
                <div className="flex-1 min-w-0 pr-2">
                  <p
                    className={`text-[12px] leading-snug break-words ${
                      afterLessonsNote.isCompleted
                        ? 'line-through text-[var(--md-text-muted)]'
                        : 'text-[var(--md-text-secondary)] font-medium'
                    }`}
                  >
                    {afterLessonsNote.text}
                  </p>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

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
