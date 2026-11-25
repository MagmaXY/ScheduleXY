import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { scheduleRepository } from '../api/scheduleRepository';
import { ScheduleResponse } from '../types/api';
import { usePreferencesStore } from '../store/preferencesStore';
import { useScheduleStore } from '../store/scheduleStore';
import {
  SlotOccupancy,
  computeSlotAvailability,
} from '../domain/freeAvailabilityEngine';
import { RealtimeState } from '../hooks/useRealtimeSync';
import { Search, Check } from 'lucide-react';
import { useSwipeGesture } from '../hooks/useSwipeGesture';

interface Props {
  realtimeState: RealtimeState;
}

export const FreeRoomsPage: React.FC<Props> = ({ realtimeState }) => {
  const [searchParams] = useSearchParams();
  const dayParam = searchParams.get('day');
  const lessonParam = searchParams.get('lesson');

  const adminPassword = usePreferencesStore((s) => s.adminPassword);
  const teachersList = useScheduleStore((s) => s.teachersList);

  const [selectedDay, setSelectedDay] = useState(() => {
    if (dayParam !== null) {
      const d = parseInt(dayParam, 10);
      if (!isNaN(d) && d >= 0 && d <= 5) return d;
    }
    return realtimeState.dayIndex < 6 ? realtimeState.dayIndex : 0;
  });

  const [selectedLesson, setSelectedLesson] = useState(() => {
    if (lessonParam !== null) {
      const l = parseInt(lessonParam, 10);
      if (!isNaN(l) && l >= 1 && l <= 12) return l;
    }
    return realtimeState.lessonBellState.type === 'in_lesson' ? realtimeState.lessonBellState.num : 1;
  });

  // If query parameters change dynamically, sync them
  useEffect(() => {
    if (dayParam !== null) {
      const d = parseInt(dayParam, 10);
      if (!isNaN(d) && d >= 0 && d <= 5) setSelectedDay(d);
    }
    if (lessonParam !== null) {
      const l = parseInt(lessonParam, 10);
      if (!isNaN(l) && l >= 1 && l <= 12) setSelectedLesson(l);
    }
  }, [dayParam, lessonParam]);

  const [viewMode, setViewMode] = useState<'rooms' | 'teachers'>('rooms');
  const [occupancy, setOccupancy] = useState<SlotOccupancy | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [search, setSearch] = useState('');

  const isTeachersUnlocked = adminPassword === 'licey2026';

  const currentSchedule = useScheduleStore((s) => s.currentSchedule);
  const [allClassSchedules, setAllClassSchedules] = useState<ScheduleResponse[] | null>(null);

  useEffect(() => {
    let isCancelled = false;
    setIsLoading(true);
    scheduleRepository
      .prefetchAllClasses()
      .then((schedules) => {
        if (!isCancelled) {
          setAllClassSchedules(schedules);
        }
      })
      .catch(() => {})
      .finally(() => {
        if (!isCancelled) setIsLoading(false);
      });

    return () => {
      isCancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!allClassSchedules || allClassSchedules.length === 0) return;

    const globalChanges =
      currentSchedule?.changes ||
      allClassSchedules.find(
        (s) => s.changes && Object.keys(s.changes.class || {}).length > 0
      )?.changes ||
      null;
    const globalWeekStart =
      currentSchedule?.weekStart ||
      allClassSchedules.find((s) => s.weekStart)?.weekStart ||
      null;

    const slot = computeSlotAvailability(
      allClassSchedules,
      teachersList,
      selectedDay,
      selectedLesson,
      globalChanges,
      globalWeekStart
    );
    setOccupancy(slot);
  }, [selectedDay, selectedLesson, allClassSchedules, teachersList, currentSchedule]);

  const freeRooms = occupancy?.freeRooms || [];
  const busyRooms = Object.entries(occupancy?.busyRooms || {}).sort((a, b) =>
    a[0].localeCompare(b[0], undefined, { numeric: true })
  );

  const freeTeachers = (occupancy?.freeTeachers || []).filter((t) =>
    !search.trim() || t.toLowerCase().includes(search.toLowerCase().trim())
  );

  const busyTeachers = Object.entries(occupancy?.busyTeachers || {})
    .filter(([t]) => !search.trim() || t.toLowerCase().includes(search.toLowerCase().trim()))
    .sort((a, b) => a[0].localeCompare(b[0]));

  // Swipe gestures for touch and LMB mouse drag (cyclical 1..12)
  const swipeHandlers = useSwipeGesture({
    onSwipeLeft: () => {
      setSelectedLesson((prev) => (prev < 12 ? prev + 1 : 1));
    },
    onSwipeRight: () => {
      setSelectedLesson((prev) => (prev > 1 ? prev - 1 : 12));
    },
  });

  // Arrow key & Digit hotkeys for lessons
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName)) return;
      if (e.key === 'ArrowRight') {
        setSelectedLesson((prev) => (prev < 12 ? prev + 1 : 1));
      } else if (e.key === 'ArrowLeft') {
        setSelectedLesson((prev) => (prev > 1 ? prev - 1 : 12));
      } else if (e.key >= '1' && e.key <= '9') {
        setSelectedLesson(parseInt(e.key, 10));
      } else if (e.key === '0') {
        setSelectedLesson(10);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  return (
    <div
      {...swipeHandlers}
      className="w-full max-w-4xl mx-auto flex-1 flex flex-col bg-[var(--md-background)] lg:border-x border-[var(--md-border-light)]"
    >
      {/* Mode Switcher: Rooms / Teachers (equal 50% width and stretched across full bar) */}
      {isTeachersUnlocked && (
        <div className="flex w-full px-3 py-1.5 bg-[var(--md-surface)] border-b border-[var(--md-border-light)]">
          <div className="flex w-full gap-2">
            <button
              onClick={() => {
                setViewMode('rooms');
                setSearch('');
              }}
              className={`flex-1 h-8.5 rounded-xl text-xs sm:text-[13px] transition-all flex items-center justify-center ${
                viewMode === 'rooms'
                  ? 'text-[var(--md-text-primary)] font-bold'
                  : 'text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] font-medium'
              }`}
            >
              Кабинеты
            </button>
            <button
              onClick={() => {
                setViewMode('teachers');
                setSearch('');
              }}
              className={`flex-1 h-8.5 rounded-xl text-xs sm:text-[13px] transition-all flex items-center justify-center ${
                viewMode === 'teachers'
                  ? 'text-[var(--md-text-primary)] font-bold'
                  : 'text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] font-medium'
              }`}
            >
              Учителя
            </button>
          </div>
        </div>
      )}

      {/* Days Ribbon (Пн..Сб) */}
      <div className="px-2 py-1.5 bg-[var(--md-surface)] border-b border-[var(--md-border-light)]">
        <div data-no-swipe="true" className="flex items-center justify-between bg-[var(--md-surface-variant)]/60 rounded-xl p-1 gap-1">
          {['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'].map((shortName, idx) => {
            const isSelected = idx === selectedDay;
            const isToday = idx === realtimeState.dayIndex;

            const textColor = isToday
              ? 'text-[var(--nika-subject)] font-bold'
              : isSelected
              ? 'text-[var(--md-text-primary)] font-bold'
              : 'text-[var(--md-text-secondary)] font-normal';

            return (
              <button
                key={shortName}
                onClick={() => setSelectedDay(idx)}
                className={`flex-1 py-1.5 text-center rounded-lg text-xs transition-all ${textColor} ${
                  isSelected
                    ? 'bg-[var(--md-surface)] shadow-sm'
                    : 'hover:bg-[var(--md-surface)]/50'
                }`}
              >
                {shortName}
              </button>
            );
          })}
        </div>
      </div>

      {/* 12 Lessons Ribbon without '#' */}
      <div className="px-2 py-1.5 bg-[var(--md-surface)] border-b border-[var(--md-border-light)]">
        <div data-no-swipe="true" className="flex items-center justify-between bg-[var(--md-surface-variant)]/60 rounded-xl p-1 gap-1">
          {Array.from({ length: 12 }, (_, i) => i + 1).map((num) => {
            const isSelected = num === selectedLesson;
            const isCurrent =
              realtimeState.dayIndex === selectedDay &&
              realtimeState.lessonBellState.type === 'in_lesson' &&
              realtimeState.lessonBellState.num === num;

            const textColor = isCurrent
              ? 'text-[var(--nika-subject)] font-bold'
              : isSelected
              ? 'text-[var(--md-text-primary)] font-bold'
              : 'text-[var(--md-text-secondary)] font-normal';

            return (
              <button
                key={num}
                onClick={() => setSelectedLesson(num)}
                className={`flex-1 py-1.5 rounded-lg text-[12px] text-center transition-all ${textColor} ${
                  isSelected
                    ? 'bg-[var(--md-surface)] shadow-sm'
                    : 'hover:bg-[var(--md-surface)]/50'
                }`}
              >
                {num}
              </button>
            );
          })}
        </div>
      </div>

      {/* Search Input ONLY for Teachers (Room search removed as requested) */}
      {viewMode === 'teachers' && (
        <div className="p-2.5 bg-[var(--md-surface)] border-b border-[var(--md-border-light)]">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-2.5 text-[var(--md-text-muted)]" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Поиск свободного учителя..."
              className="w-full pl-9 pr-3 py-1.5 bg-[var(--md-surface-variant)]/50 border border-[var(--md-border-light)] rounded-xl text-xs outline-none text-[var(--md-text-primary)] placeholder:text-[var(--md-text-muted)]"
            />
          </div>
        </div>
      )}

      {/* Content */}
      <div className="flex-1 p-3 space-y-3">
        {isLoading ? (
          <div className="py-20 text-center">
            <div className="w-8 h-8 border-2 border-[var(--nika-subject)] border-t-transparent rounded-full animate-spin mx-auto mb-2" />
            <p className="text-xs text-[var(--md-text-muted)]">
              Анализ занятости 39 классов...
            </p>
          </div>
        ) : viewMode === 'rooms' ? (
          <div className="space-y-3">
            {/* Separate counters: Free on the Left, Busy on the Right */}
            <div className="flex items-center justify-between px-1 text-sm font-bold">
              <span className="text-[var(--md-secondary)]">
                Свободно: {freeRooms.length}
              </span>
              <span className="text-red-500 dark:text-red-400">
                Занято: {busyRooms.length}
              </span>
            </div>

            {/* Free Rooms Card - matching Android OutlinedCard with secondaryContainer & border */}
            <div className="rounded-xl p-3 bg-[var(--md-secondary-container)]/35 border border-[var(--md-secondary)]/40">
              <div className="text-xs font-bold text-[var(--md-secondary)] mb-1">
                Свободные кабинеты:
              </div>
              <div className="text-[13px] font-semibold text-[var(--md-text-primary)] leading-relaxed">
                {freeRooms.length === 0 ? (
                  <span className="text-[var(--md-text-muted)] font-normal">Все кабинеты заняты</span>
                ) : (
                  freeRooms.join('  •  ')
                )}
              </div>
            </div>

            {/* Busy Rooms Header */}
            <div className="text-xs font-bold text-red-500 dark:text-red-400 pt-1 px-1">
              Занятые кабинеты:
            </div>

            {/* Busy Rooms Cards */}
            <div className="space-y-1.5 pb-4">
              {busyRooms.length === 0 ? (
                <div className="text-xs text-[var(--md-text-muted)] p-2">Все кабинеты свободны</div>
              ) : (
                busyRooms.map(([room, occupancyText]) => (
                  <div
                    key={room}
                    className="flex items-center px-3 py-2 rounded-xl bg-[var(--md-surface)] border border-[var(--md-border-light)] shadow-xs"
                  >
                    <span className="px-2 py-0.5 rounded-md text-xs font-bold bg-red-500/15 text-red-500 dark:text-red-400 mr-2.5 shrink-0">
                      {room}
                    </span>
                    <span className="text-xs text-[var(--md-text-primary)] truncate">
                      {occupancyText}
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>
        ) : (
          <div className="space-y-3">
            {/* Mobile counters row: Free on the Left, Busy on the Right */}
            <div className="flex lg:hidden items-center justify-between px-1 text-sm font-bold">
              <span className="text-[var(--nika-subject)]">
                Свободно учителей: {freeTeachers.length}
              </span>
              <span className="text-red-500 dark:text-red-400">
                Занято: {busyTeachers.length}
              </span>
            </div>

            {/* Responsive split on PC: Free Teachers on the Left, Busy Teachers on the Right */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 items-start">
              {/* Left Column: Free Teachers List */}
              <div className="space-y-2">
                <div className="hidden lg:block px-1 text-sm font-bold text-[var(--nika-subject)]">
                  Свободно учителей: {freeTeachers.length}
                </div>
                <div className="space-y-1.5">
                  {freeTeachers.length === 0 ? (
                    <div className="text-xs text-[var(--md-text-muted)] p-2">
                      {search ? 'Учитель не найден' : 'В этот слот нет свободных учителей'}
                    </div>
                  ) : (
                    freeTeachers.map((teacher) => (
                      <div
                        key={teacher}
                        className="flex items-center px-3 py-2 rounded-xl bg-[var(--md-surface)] border border-[var(--md-border-light)] shadow-xs"
                      >
                        <Check className="w-4 h-4 text-[var(--nika-subject)] mr-2.5 shrink-0" />
                        <span className="text-xs font-semibold text-[var(--md-text-primary)]">
                          {teacher}
                        </span>
                      </div>
                    ))
                  )}
                </div>
              </div>

              {/* Right Column: Busy Teachers List */}
              <div className={`space-y-2 ${busyTeachers.length === 0 ? 'hidden lg:block' : ''}`}>
                <div className="text-xs lg:text-sm font-bold text-red-500 dark:text-red-400 pt-1 lg:pt-0 px-1">
                  Занято учителей: {busyTeachers.length}
                </div>
                <div className="space-y-1.5 pb-4">
                  {busyTeachers.length === 0 ? (
                    <div className="text-xs text-[var(--md-text-muted)] p-2">
                      {search ? 'Учитель не найден' : 'В этот слот нет занятых учителей'}
                    </div>
                  ) : (
                    busyTeachers.map(([teacher, info]) => (
                      <div
                        key={teacher}
                        className="flex flex-col px-3 py-2 rounded-xl bg-[var(--md-surface)] border border-[var(--md-border-light)] shadow-xs"
                      >
                        <span className="text-xs font-bold text-red-500 dark:text-red-400">
                          {teacher}
                        </span>
                        <span className="text-[11px] text-[var(--md-text-secondary)] mt-0.5">
                          {info}
                        </span>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
