import React, { useMemo, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useScheduleStore } from '../store/scheduleStore';
import { usePreferencesStore } from '../store/preferencesStore';
import { TargetSelector } from '../components/schedule/TargetSelector';
import { DaySelector } from '../components/common/DaySelector';
import { LessonCard } from '../components/schedule/LessonCard';
import { PairCard } from '../components/schedule/PairCard';
import { processLessons } from '../domain/schedulePairMerger';
import { getChangesForLesson, isScheduleForTeacher } from '../types/api';
import { Calendar } from 'lucide-react';
import { useSwipeGesture } from '../hooks/useSwipeGesture';

function formatArchiveWeekChip(isoMonday: string): string {
  try {
    const parts = isoMonday.split('-');
    if (parts.length === 3) {
      const cal = new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
      const startDay = cal.getDate();
      const startMonth = cal.getMonth();

      cal.setDate(cal.getDate() + 5); // Saturday
      const endDay = cal.getDate();
      const endMonth = cal.getMonth();

      const months = ['янв', 'фев', 'мар', 'апр', 'мая', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
      return startMonth === endMonth
        ? `${startDay}–${endDay} ${months[startMonth] || ''}`
        : `${startDay} ${months[startMonth] || ''}–${endDay} ${months[endMonth] || ''}`;
    }
  } catch {
    // fallback
  }
  return isoMonday;
}

export const ArchivePage: React.FC = () => {
  const navigate = useNavigate();
  const archiveSchedule = useScheduleStore((s) => s.archiveSchedule);
  const archiveWeeksList = useScheduleStore((s) => s.archiveWeeksList);
  const selectedArchiveWeek = useScheduleStore((s) => s.selectedArchiveWeek);
  const setSelectedArchiveWeek = useScheduleStore((s) => s.setSelectedArchiveWeek);
  const loadArchiveSchedule = useScheduleStore((s) => s.loadArchiveSchedule);
  const archiveDayIndex = useScheduleStore((s) => s.archiveDayIndex);
  const setArchiveDayIndex = useScheduleStore((s) => s.setArchiveDayIndex);
  const isLoading = useScheduleStore((s) => s.isLoading);

  const targetName = usePreferencesStore((s) => s.targetName);
  const showPairsSchedule = usePreferencesStore((s) => s.showPairsSchedule);

  const isTeacher = isScheduleForTeacher(archiveSchedule);
  const daySchedule = archiveSchedule?.days?.find(
    (d) => d.dayIndex === archiveDayIndex
  );
  const lessons = daySchedule?.lessons || [];
  const processedItems = processLessons(
    lessons,
    isTeacher,
    targetName,
    showPairsSchedule
  );

  // Archive week options: weekParam is -1, -2, -3...
  const weekOptions = useMemo(() => {
    return archiveWeeksList.slice(0, 3).map((isoMonday, index) => ({
      weekParam: -(index + 1),
      isoMonday,
      label: formatArchiveWeekChip(isoMonday),
    }));
  }, [archiveWeeksList]);

  // Initial load: ensure selectedArchiveWeek is negative and schedule is loaded
  useEffect(() => {
    if (selectedArchiveWeek >= 0) {
      setSelectedArchiveWeek(-1);
      loadArchiveSchedule(-1);
    } else if (!archiveSchedule) {
      loadArchiveSchedule(selectedArchiveWeek || -1);
    }
  }, [selectedArchiveWeek, archiveSchedule, loadArchiveSchedule, setSelectedArchiveWeek]);

  const handleSelectWeek = (weekParam: number) => {
    setSelectedArchiveWeek(weekParam);
    loadArchiveSchedule(weekParam);
  };

  const activeWeekOption = weekOptions.find((opt) => opt.weekParam === selectedArchiveWeek);
  const activeWeekIso = activeWeekOption?.isoMonday || null;

  // Date label format: "21 ноября" (DaySelector itself will prepend the day name, preventing duplication)
  const getArchiveDateLabel = (): string => {
    const cal = new Date();
    const mondayIso = archiveSchedule?.weekStart || activeWeekIso;

    if (mondayIso) {
      const parts = mondayIso.split('-');
      if (parts.length === 3) {
        cal.setFullYear(parseInt(parts[0], 10));
        cal.setMonth(parseInt(parts[1], 10) - 1);
        cal.setDate(parseInt(parts[2], 10));
        cal.setDate(cal.getDate() + archiveDayIndex);
        return cal.toLocaleDateString('ru-RU', {
          day: 'numeric',
          month: 'long',
        });
      }
    }

    const dayOfWeek = (cal.getDay() + 6) % 7;
    cal.setDate(cal.getDate() - dayOfWeek + selectedArchiveWeek * 7 + archiveDayIndex);
    return cal.toLocaleDateString('ru-RU', {
      day: 'numeric',
      month: 'long',
    });
  };

  const getArchiveIso = (): string => {
    const cal = new Date();
    const mondayIso = archiveSchedule?.weekStart || activeWeekIso;

    if (mondayIso) {
      const parts = mondayIso.split('-');
      if (parts.length === 3) {
        cal.setFullYear(parseInt(parts[0], 10));
        cal.setMonth(parseInt(parts[1], 10) - 1);
        cal.setDate(parseInt(parts[2], 10));
        cal.setDate(cal.getDate() + archiveDayIndex);
        const y = cal.getFullYear();
        const m = String(cal.getMonth() + 1).padStart(2, '0');
        const d = String(cal.getDate()).padStart(2, '0');
        return `${y}-${m}-${d}`;
      }
    }

    const dayOfWeek = (cal.getDay() + 6) % 7;
    cal.setDate(cal.getDate() - dayOfWeek + selectedArchiveWeek * 7 + archiveDayIndex);
    const y = cal.getFullYear();
    const m = String(cal.getMonth() + 1).padStart(2, '0');
    const d = String(cal.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  };

  // Arrow key and Digit hotkey navigation for archive days (1..6)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName)) return;
      if (e.key === 'ArrowRight') {
        setArchiveDayIndex(archiveDayIndex < 5 ? archiveDayIndex + 1 : 0);
      } else if (e.key === 'ArrowLeft') {
        setArchiveDayIndex(archiveDayIndex > 0 ? archiveDayIndex - 1 : 5);
      } else if (e.key >= '1' && e.key <= '6') {
        setArchiveDayIndex(parseInt(e.key, 10) - 1);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [archiveDayIndex, setArchiveDayIndex]);

  // Touch and LMB mouse drag swipe handling
  const swipeHandlers = useSwipeGesture({
    onSwipeLeft: () => {
      setArchiveDayIndex(archiveDayIndex < 5 ? archiveDayIndex + 1 : 0);
    },
    onSwipeRight: () => {
      setArchiveDayIndex(archiveDayIndex > 0 ? archiveDayIndex - 1 : 5);
    },
  });

  return (
    <div
      {...swipeHandlers}
      className="w-full max-w-4xl mx-auto flex-1 flex flex-col bg-[var(--md-background)] relative lg:border-x border-[var(--md-border-light)]"
    >
      {/* Top Unified Header Bar for Archive: Classes/Teachers, Target Box, Archive Week Switcher */}
      <div className="px-3 pt-3 pb-2 bg-[var(--md-surface)]">
        <TargetSelector
          hideFavoriteStar
          extraRight={
            weekOptions.length > 0 ? (
              <div data-no-swipe="true" className="flex items-center h-8 gap-1 ml-1 overflow-x-auto scrollbar-none">
                {weekOptions.map((opt) => {
                  const isSelected = opt.weekParam === selectedArchiveWeek;
                  return (
                    <button
                      key={opt.weekParam}
                      onClick={() => handleSelectWeek(opt.weekParam)}
                      className={`h-8 px-2 rounded-lg text-xs whitespace-nowrap transition-colors ${
                        isSelected
                          ? 'font-bold text-[var(--md-text-primary)]'
                          : 'font-medium text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)]'
                      }`}
                    >
                      {opt.label}
                    </button>
                  );
                })}
              </div>
            ) : null
          }
          onTargetChanged={() => {
            loadArchiveSchedule(selectedArchiveWeek);
          }}
        />
      </div>

      {archiveWeeksList.length === 0 && !isLoading ? (
        <div className="py-12 text-center bg-[var(--md-surface)] m-3 rounded-2xl border border-[var(--md-border)] p-6">
          <Calendar className="w-8 h-8 text-[var(--md-text-muted)] mx-auto mb-2" />
          <p className="font-bold text-sm text-[var(--md-text-primary)]">
            Архив расписаний прошлых недель
          </p>
          <p className="text-xs text-[var(--md-text-muted)] mt-1">
            Список архива пока пуст
          </p>
        </div>
      ) : (
        <>
          <DaySelector
            selectedDayIndex={archiveDayIndex}
            currentDayIndex={-1}
            dateLabel={getArchiveDateLabel()}
            onDaySelected={setArchiveDayIndex}
            onDateClick={() => {
              navigate('/calendar', { state: { date: getArchiveIso() } });
            }}
          />

          <div className="flex-1 bg-[var(--md-surface)] mt-1">
            {isLoading ? (
              <div className="py-20 text-center">
                <div className="w-8 h-8 border-2 border-[var(--nika-subject)] border-t-transparent rounded-full animate-spin mx-auto mb-3" />
                <p className="text-xs text-[var(--md-text-muted)]">
                  Загрузка архивного расписания...
                </p>
              </div>
            ) : processedItems.length === 0 ? (
              <div className="py-16 text-center">
                <p className="text-sm font-bold text-[var(--md-text-primary)]">
                  Нет занятий в выбранный день
                </p>
              </div>
            ) : (
              <div className="divide-y divide-[var(--md-border-light)]">
                {processedItems.map((item, idx) => {
                  if (item.type === 'pair') {
                    const ch1 = getChangesForLesson(
                      archiveSchedule,
                      archiveDayIndex,
                      item.firstLesson.num
                    );
                    const ch2 = getChangesForLesson(
                      archiveSchedule,
                      archiveDayIndex,
                      item.secondLesson.num
                    );

                    return (
                      <PairCard
                        key={idx}
                        pairNum={item.pairNum}
                        pairRoman={item.pairRoman}
                        pairTimes={item.pairTimes}
                        firstLesson={item.firstLesson}
                        secondLesson={item.secondLesson}
                        dayIndex={archiveDayIndex}
                        changes1={ch1}
                        changes2={ch2}
                        isTeacherMode={isTeacher}
                        isActiveNow={false}
                      />
                    );
                  }

                  const ch = getChangesForLesson(
                    archiveSchedule,
                    archiveDayIndex,
                    item.lesson.num
                  );

                  return (
                    <LessonCard
                      key={idx}
                      lesson={item.lesson}
                      dayIndex={archiveDayIndex}
                      changes={ch}
                      isTeacherMode={isTeacher}
                      isActiveNow={false}
                    />
                  );
                })}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};
