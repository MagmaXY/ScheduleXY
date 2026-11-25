import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TargetSelector } from '../components/schedule/TargetSelector';
import { DaySelector } from '../components/common/DaySelector';
import { LessonCard } from '../components/schedule/LessonCard';
import { PairCard } from '../components/schedule/PairCard';
import { WeekScheduleTable } from '../components/schedule/WeekScheduleTable';
import { NoteEditDialog } from '../components/notes/NoteEditDialog';
import { useScheduleStore } from '../store/scheduleStore';
import { toLocalDateIso, parseLocalDate, formatWeekRangeGenitive, formatFullDateGenitive } from '../utils/dateUtils';
import { usePreferencesStore } from '../store/preferencesStore';
import { useNotesStore } from '../store/notesStore';
import { RealtimeState } from '../hooks/useRealtimeSync';
import { processLessons } from '../domain/schedulePairMerger';
import { getChangesForLesson, isScheduleForTeacher } from '../types/api';
import { Copy, Edit, Check } from 'lucide-react';
import { useSwipeGesture } from '../hooks/useSwipeGesture';
import { useCanUseWeekView } from '../hooks/useCanUseWeekView';

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
  const [noteInitialDateIso, setNoteInitialDateIso] = useState<string | null>(null);
  const canUseWeekView = useCanUseWeekView();
  const [isWeekView, setIsWeekView] = useState(false);
  const effectiveWeekView = canUseWeekView && isWeekView;
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
      return formatFullDateGenitive(d);
    }
    const now = new Date();
    const dayOfWeek = (now.getDay() + 6) % 7;
    now.setDate(now.getDate() - dayOfWeek + selectedDayIndex);
    return formatFullDateGenitive(now);
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

  const getWeekMonday = (): Date => {
    if (currentSchedule?.weekStart) {
      return parseLocalDate(currentSchedule.weekStart);
    }
    const mon = new Date();
    const dow = (mon.getDay() + 6) % 7;
    mon.setDate(mon.getDate() - dow);
    return mon;
  };
  const weekRangeLabel = formatWeekRangeGenitive(getWeekMonday());

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

  const handleCopyWeekSchedule = () => {
    if (!currentSchedule?.days) return;
    const monday = getWeekMonday();
    const blocks: string[] = [`📅 Вся неделя, ${weekRangeLabel} (${targetName})`];
    for (let dIdx = 0; dIdx < 6; dIdx++) {
      const ds = currentSchedule.days.find((d) => d.dayIndex === dIdx);
      if (!ds || ds.lessons.length === 0) continue;
      const dDate = new Date(monday);
      dDate.setDate(monday.getDate() + dIdx);
      blocks.push(`\n— ${ds.dayName}, ${formatFullDateGenitive(dDate)} —`);
      ds.lessons.forEach((l) => {
        const subj = l.subgroups.map((s) => s.subject).filter(Boolean).join(' / ') || '—';
        const room = l.subgroups.map((s) => s.room).filter(Boolean).join(', ') || '';
        const teacher = l.subgroups.map((s) => s.teacher).filter(Boolean).join(', ') || '';
        const info = [teacher, room ? `каб. ${room}` : ''].filter(Boolean).join(', ');
        blocks.push(`${l.num}. ${subj}${info ? ` (${info})` : ''}`);
      });
    }
    navigator.clipboard.writeText(blocks.join('\n'));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleRoomClick = (room: string, lessonNum: number, dayIdx: number = selectedDayIndex) => {
    navigate(`/freerooms?day=${dayIdx}&lesson=${lessonNum}&room=${encodeURIComponent(room)}`);
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

  const goNextInCycle = () => {
    if (effectiveWeekView) {
      setIsWeekView(false);
      setSelectedDayIndex(0);
    } else if (selectedDayIndex < 5) {
      setSelectedDayIndex(selectedDayIndex + 1);
    } else if (canUseWeekView) {
      setIsWeekView(true);
    } else {
      setSelectedDayIndex(0);
    }
  };

  const goPrevInCycle = () => {
    if (effectiveWeekView) {
      setIsWeekView(false);
      setSelectedDayIndex(5);
    } else if (selectedDayIndex > 0) {
      setSelectedDayIndex(selectedDayIndex - 1);
    } else if (canUseWeekView) {
      setIsWeekView(true);
    } else {
      setSelectedDayIndex(5);
    }
  };

  // Arrow key and Digit hotkey navigation: PC/Landscape (1 = Вся, 2-7 = Пн-Сб), Mobile portrait (1-6 = Пн-Сб)
  React.useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (dialogOpen) return;
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName)) return;
      if (e.metaKey || e.ctrlKey || e.altKey) return;

      if (e.key === 'ArrowRight') {
        goNextInCycle();
      } else if (e.key === 'ArrowLeft') {
        goPrevInCycle();
      } else if (canUseWeekView) {
        if (e.key === '1') {
          setIsWeekView(true);
        } else if (e.key >= '2' && e.key <= '7') {
          setIsWeekView(false);
          setSelectedDayIndex(parseInt(e.key, 10) - 2);
        }
      } else if (e.key >= '1' && e.key <= '6') {
        setSelectedDayIndex(parseInt(e.key, 10) - 1);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [selectedDayIndex, setSelectedDayIndex, effectiveWeekView, canUseWeekView, dialogOpen]);

  // Touch and LMB mouse drag swipe handling (includes "Вся" in cycle when available)
  const swipeHandlers = useSwipeGesture({
    onSwipeLeft: goNextInCycle,
    onSwipeRight: goPrevInCycle,
  });

  return (
    <div
      {...swipeHandlers}
      className={`w-full mx-auto flex-1 flex flex-col bg-[var(--md-background)] relative border-[var(--md-border-light)] ${
        effectiveWeekView ? 'max-w-none border-x-0' : 'max-w-4xl lg:border-x'
      }`}
    >
      {/* 1. Target Selector Header */}
      <div className="px-3 pt-2.5 pb-1.5 bg-[var(--md-surface)]">
        <TargetSelector onTargetChanged={() => loadSchedule()} />
      </div>

      {/* 2. Unified Day Selector with Clickable Date & "Вся" button on PC / landscape */}
      <DaySelector
        selectedDayIndex={selectedDayIndex}
        currentDayIndex={activeDayIndex}
        dateLabel={dateLabel}
        isWeekView={effectiveWeekView}
        weekRangeLabel={weekRangeLabel}
        onSelectWeekView={canUseWeekView ? () => setIsWeekView(true) : undefined}
        onDaySelected={(day) => {
          setIsWeekView(false);
          setSelectedDayIndex(day);
        }}
        onDateClick={handleDateClick}
        weekActions={
          <div className="flex items-center gap-0.5">
            <button
              type="button"
              onClick={handleCopyWeekSchedule}
              className="w-8 h-8 flex items-center justify-center rounded-full text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/60 transition-all"
              title="Скопировать расписание на неделю"
            >
              {copied ? <Check className="w-4 h-4 text-green-500" /> : <Copy className="w-4 h-4" />}
            </button>
            {!isTeacher && (
              <button
                type="button"
                onClick={() => {
                  setNoteInitialDateIso(currentDateIso);
                  setNoteInitialLessonNum(null);
                  setNoteInitialSubject('');
                  setDialogOpen(true);
                }}
                className="w-8 h-8 flex items-center justify-center rounded-full text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/60 transition-all"
                title="Записать ДЗ или заметку"
              >
                <Edit className="w-4 h-4" />
              </button>
            )}
          </div>
        }
      />

      {/* 3. Floating Copy & Note Action Icons lowered down well below the day selector (hidden in week view) */}
      {!effectiveWeekView && (
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
              setNoteInitialDateIso(currentDateIso);
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
      )}

      {/* 4. Lessons / Pairs List or Weekly Table */}
      <div className="flex-1 bg-[var(--md-surface)] flex flex-col">
        {isLoading ? (
          <div className="py-20 text-center">
            <div className="w-8 h-8 border-2 border-[var(--nika-subject)] border-t-transparent rounded-full animate-spin mx-auto mb-3" />
            <p className="text-xs text-[var(--md-text-muted)]">
              Загрузка расписания...
            </p>
          </div>
        ) : effectiveWeekView ? (
          <WeekScheduleTable
            schedule={currentSchedule}
            targetName={targetName}
            showPairsSchedule={showPairsSchedule}
            realtimeState={realtimeState}
            activeDayIndex={activeDayIndex}
            isThisWeek={isThisWeek}
            notes={notes}
            onToggleNoteCompleted={toggleCompleted}
            onTeacherClassClick={handleTeacherClassClick}
            onRoomClick={handleRoomClick}
            onCellClick={
              !isTeacher
                ? (_dayIdx, lessonNum, subject, dateIso) => {
                    setNoteInitialDateIso(dateIso);
                    setNoteInitialLessonNum(lessonNum);
                    setNoteInitialSubject(subject);
                    setDialogOpen(true);
                  }
                : undefined
            }
            onDayHeaderClick={(dayIdx) => {
              setIsWeekView(false);
              setSelectedDayIndex(dayIdx);
            }}
            onDateClick={(dateIso) => navigate('/calendar', { state: { date: dateIso } })}
          />
        ) : (
          <div className="flex-1 flex flex-col">
              {processedItems.length === 0 ? (
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
                          setNoteInitialDateIso(currentDateIso);
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
        )}
      </div>

      {/* Note Edit Modal */}
      {dialogOpen && (
        <NoteEditDialog
          targetName={targetName}
          initialDate={noteInitialDateIso || currentDateIso}
          initialSubject={noteInitialSubject}
          initialLessonNum={noteInitialLessonNum}
          onSave={addNote}
          onClose={() => {
            setDialogOpen(false);
            setNoteInitialDateIso(null);
          }}
        />
      )}
    </div>
  );
};
