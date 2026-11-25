import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useScheduleStore } from '../store/scheduleStore';
import { useNotesStore } from '../store/notesStore';
import { usePreferencesStore } from '../store/preferencesStore';
import { ChevronLeft, ChevronRight, Edit2, Calendar as CalendarIcon, Search, X, Info, Trash2, CheckCircle2, Circle } from 'lucide-react';
import { resolveScheduleWeekOffset } from '../utils/scheduleNav';
import { useSwipeGesture } from '../hooks/useSwipeGesture';
import { searchNotes, searchEvents } from '../utils/smartSearch';
import { NoteEditDialog } from '../components/notes/NoteEditDialog';
import { ScheduleNote } from '../types/notes';
import { toLocalDateIso, parseLocalDate, addDaysToIso } from '../utils/dateUtils';
import { CalendarEvent } from '../types/api';

const EVENT_DOT_COLORS: Record<string, string> = {
  blue: '#2563EB',
  green: '#059669',
  red: '#DC2626',
  orange: '#D97706',
  purple: '#9333EA',
  yellow: '#CA8A04',
  cyan: '#0891B2',
};

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
  } catch {}
  return isoMonday;
}

function formatDateWithDay(iso: string): string {
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

export const CalendarPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const eventsList = useScheduleStore((s) => s.eventsList);
  const eventCategories = useScheduleStore((s) => s.eventCategories);
  const currentSchedule = useScheduleStore((s) => s.currentSchedule);
  const archiveWeeksList = useScheduleStore((s) => s.archiveWeeksList);
  const setSelectedDayIndex = useScheduleStore((s) => s.setSelectedDayIndex);
  const setSelectedArchiveWeek = useScheduleStore((s) => s.setSelectedArchiveWeek);
  const setArchiveDayIndex = useScheduleStore((s) => s.setArchiveDayIndex);
  const loadSchedule = useScheduleStore((s) => s.loadSchedule);
  const loadArchiveSchedule = useScheduleStore((s) => s.loadArchiveSchedule);

  const notes = useNotesStore((s) => s.notes);
  const updateNote = useNotesStore((s) => s.updateNote);
  const deleteNote = useNotesStore((s) => s.deleteNote);
  const toggleCompleted = useNotesStore((s) => s.toggleCompleted);

  const targetName = usePreferencesStore((s) => s.targetName);
  const targetType = usePreferencesStore((s) => s.targetType);
  const isTeacherMode = targetType === 'teacher';

  // Selected date from route state if redirected, or null by default
  const initialDateStr = (location.state as any)?.date;
  const [selectedDateString, setSelectedDateString] = useState<string | null>(initialDateStr || null);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [editingNote, setEditingNote] = useState<ScheduleNote | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  const [currentDate, setCurrentDate] = useState(() => {
    if (initialDateStr) {
      const p = initialDateStr.split('-');
      if (p.length === 3) {
        return new Date(parseInt(p[0]), parseInt(p[1]) - 1, parseInt(p[2]));
      }
    }
    return new Date();
  });

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  // Days in month
  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);
  const daysInMonth = lastDay.getDate();
  const startDayOfWeek = (firstDay.getDay() + 6) % 7; // 0=Mon..6=Sun

  const monthNames = [
    'Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
    'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь',
  ];

  const prevMonth = () => setCurrentDate(new Date(year, month - 1, 1));
  const nextMonth = () => setCurrentDate(new Date(year, month + 1, 1));

  const todayIso = toLocalDateIso();

  // Map of events by date
  const eventsByDate = new Map<string, typeof eventsList>();
  eventsList.forEach((ev) => {
    const list = eventsByDate.get(ev.date) || [];
    list.push(ev);
    eventsByDate.set(ev.date, list);
  });

  // Check notes by date
  const hasNotesOnDate = (dateStr: string) => {
    return notes.some((n) => n.date === dateStr);
  };

  // Debts count
  const debtsCount = notes.filter(
    (n) => n.targetName === targetName && n.type === 'homework' && !n.isCompleted && (n.date < todayIso || n.transferPending)
  ).length;

  // Categories list for ribbon & hotkeys 1..9, 0
  const categoriesList = React.useMemo(() => {
    const list: { key: string | null; label: string; isDebt?: boolean }[] = [
      { key: null, label: 'Все' },
    ];
    if (!isTeacherMode) {
      list.push({ key: 'CAT_HW', label: 'ДЗ' });
    }
    list.push({ key: 'CAT_NOTES', label: 'Заметки' });
    if (!isTeacherMode && debtsCount > 0) {
      list.push({ key: 'CAT_DEBTS', label: `Долги (${debtsCount})`, isDebt: true });
    }
    eventCategories.forEach((cat) => {
      list.push({ key: cat.color, label: cat.title });
    });
    return list;
  }, [isTeacherMode, debtsCount, eventCategories]);

  // Archive weeks that belong to the currently viewed month
  const archiveWeeksInMonth = React.useMemo(() => {
    return archiveWeeksList
      .map((isoMonday, idx) => ({ isoMonday, idx }))
      .filter(({ isoMonday }) => {
        const parts = isoMonday.split('-');
        if (parts.length !== 3) return false;
        const mYear = parseInt(parts[0], 10);
        const mMonth = parseInt(parts[1], 10) - 1;
        const mDay = parseInt(parts[2], 10);
        const monDate = new Date(mYear, mMonth, mDay);
        const satDate = new Date(mYear, mMonth, mDay + 5);
        return (
          (monDate.getFullYear() === year && monDate.getMonth() === month) ||
          (satDate.getFullYear() === year && satDate.getMonth() === month)
        );
      });
  }, [archiveWeeksList, year, month]);

  // Resolve schedule navigation for a date
  const getScheduleNavForDate = (dateStr: string) => {
    const parts = dateStr.split('-');
    if (parts.length !== 3) return null;
    const cellCal = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
    return resolveScheduleWeekOffset(cellCal, currentSchedule?.weekStart, archiveWeeksList);
  };

  const selectedScheduleNav = selectedDateString ? getScheduleNavForDate(selectedDateString) : null;

  const handleOpenSchedule = (nav: { weekOffset: number; dayIndex: number }) => {
    if (nav.weekOffset === 0) {
      setSelectedDayIndex(nav.dayIndex);
      loadSchedule();
      navigate('/schedule');
    } else {
      setSelectedArchiveWeek(nav.weekOffset);
      setArchiveDayIndex(nav.dayIndex);
      loadArchiveSchedule(nav.weekOffset);
      navigate('/archive');
    }
  };

  const handleDayClick = (dateStr: string) => {
    if (selectedDateString === dateStr) {
      const nav = getScheduleNavForDate(dateStr);
      if (nav) {
        handleOpenSchedule(nav);
      }
    } else {
      setSelectedDateString(dateStr);
    }
  };

  // Arrow key navigation for calendar months & hotkeys 1,2,3,4,5,6,7,8,9,0 for categories
  React.useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName)) return;
      if (e.metaKey || e.ctrlKey || e.altKey) return;

      if (e.key === 'ArrowRight') {
        nextMonth();
      } else if (e.key === 'ArrowLeft') {
        prevMonth();
      } else if (/^[0-9]$/.test(e.key)) {
        const digit = parseInt(e.key, 10);
        // Digit 1..9 -> index 0..8, digit 0 -> index 9 (10th item)
        const targetIndex = digit === 0 ? 9 : digit - 1;
        if (targetIndex < categoriesList.length) {
          const target = categoriesList[targetIndex];
          setSelectedCategory((prev) => (prev === target.key ? null : target.key));
        } else if (digit === 0) {
          // If fewer than 10 categories, '0' resets to 'Все'
          setSelectedCategory(null);
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [year, month, categoriesList]);

  // Touch and LMB mouse drag swipe support for switching months
  const swipeHandlers = useSwipeGesture({
    onSwipeLeft: nextMonth,
    onSwipeRight: prevMonth,
  });

  // Filtered notes and events:
  // 1) For specific date (selectedDateString || todayIso)
  // 2) Upcoming events/notes for the next 21 days (3 weeks)
  const activeDateIso = selectedDateString || todayIso;
  const upcomingEndIso = React.useMemo(() => addDaysToIso(todayIso, 21), [todayIso]);

  const isNoteCategory = selectedCategory && ['CAT_HW', 'CAT_NOTES', 'CAT_DEBTS'].includes(selectedCategory);
  const isEventColorCategory = selectedCategory && !isNoteCategory;

  const currentNotes = React.useMemo(() => {
    return notes.filter((n) => n.targetName === targetName);
  }, [notes, targetName]);

  const matchesNoteCategory = React.useCallback(
    (note: ScheduleNote) => {
      if (selectedCategory === 'CAT_HW') return note.type === 'homework';
      if (selectedCategory === 'CAT_NOTES') return note.type === 'note';
      if (selectedCategory === 'CAT_DEBTS') {
        return note.type === 'homework' && !note.isCompleted && (note.date < todayIso || note.transferPending);
      }
      return true;
    },
    [selectedCategory, todayIso]
  );

  const displayedNotes = React.useMemo(() => {
    if (isEventColorCategory) return [];

    const base = currentNotes.filter(
      (note) => note.date === activeDateIso && matchesNoteCategory(note)
    );
    return searchNotes(base, searchQuery);
  }, [currentNotes, activeDateIso, matchesNoteCategory, searchQuery, isEventColorCategory]);

  const displayedEvents = React.useMemo(() => {
    if (isNoteCategory) return [];

    const base = eventsList.filter((event) => {
      const matchesCat = !selectedCategory || event.color === selectedCategory;
      return matchesCat && event.date === activeDateIso;
    });

    return searchEvents(base, searchQuery);
  }, [eventsList, selectedCategory, activeDateIso, isNoteCategory, searchQuery]);

  const upcomingItems = React.useMemo(() => {
    const matchedNotes = isEventColorCategory
      ? []
      : searchNotes(
          currentNotes.filter(
            (note) =>
              note.date >= todayIso &&
              note.date <= upcomingEndIso &&
              note.date !== activeDateIso &&
              matchesNoteCategory(note)
          ),
          searchQuery
        );

    const matchedEvents = isNoteCategory
      ? []
      : searchEvents(
          eventsList.filter((event) => {
            const matchesCat = !selectedCategory || event.color === selectedCategory;
            return (
              matchesCat &&
              event.date >= todayIso &&
              event.date <= upcomingEndIso &&
              event.date !== activeDateIso
            );
          }),
          searchQuery
        );

    type UpcomingEntry =
      | { kind: 'note'; date: string; note: ScheduleNote }
      | { kind: 'event'; date: string; event: CalendarEvent };

    const combined: UpcomingEntry[] = [
      ...matchedNotes.map((note) => ({ kind: 'note' as const, date: note.date, note })),
      ...matchedEvents.map((event) => ({ kind: 'event' as const, date: event.date, event })),
    ];

    return combined.sort((a, b) => a.date.localeCompare(b.date));
  }, [
    currentNotes,
    eventsList,
    todayIso,
    upcomingEndIso,
    activeDateIso,
    selectedCategory,
    isNoteCategory,
    isEventColorCategory,
    matchesNoteCategory,
    searchQuery,
  ]);

  const renderNoteCard = (note: ScheduleNote) => {
    const isDebt = note.type === 'homework' && !note.isCompleted && (note.date < todayIso || note.transferPending);
    const title = note.type === 'homework'
      ? `ДЗ: ${note.subject || ''}${note.lessonNum ? ` (${note.lessonNum} урок)` : ''}`
      : note.slotType === 'before_lessons'
      ? 'До уроков'
      : note.slotType === 'after_lessons'
      ? 'После уроков'
      : note.slotType === 'lesson'
      ? `Заметка (${note.lessonNum} урок)`
      : 'Заметка';

    return (
      <div
        key={`note_${note.id}`}
        className={`p-3 rounded-2xl border transition-all flex items-center justify-between gap-3 shadow-sm ${
          isDebt
            ? 'bg-red-500/10 border-red-500/30'
            : 'bg-[var(--md-surface)] border border-[var(--md-border)]'
        }`}
      >
        <button
          onClick={() => toggleCompleted(note.id)}
          className="text-[var(--nika-subject)] shrink-0"
        >
          {note.isCompleted ? (
            <CheckCircle2 className="w-5 h-5 fill-[var(--nika-subject)] text-white" />
          ) : (
            <Circle className={`w-5 h-5 ${isDebt ? 'text-red-500' : 'text-[var(--md-text-muted)]'}`} />
          )}
        </button>

        <div
          className="flex-1 cursor-pointer min-w-0"
          onClick={() => {
            setEditingNote(note);
            setIsDialogOpen(true);
          }}
        >
          <div className="flex items-center justify-between gap-2">
            <h4
              className={`font-semibold text-xs sm:text-sm truncate ${
                isDebt ? 'text-red-500 font-bold' : 'text-[var(--md-text-primary)]'
              }`}
            >
              {title}
            </h4>
            {note.transferPending ? (
              <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-[var(--nika-subject)]/15 text-[var(--nika-subject)] shrink-0">
                Перенос на след. неделю
              </span>
            ) : isDebt ? (
              <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-red-500/20 text-red-600 shrink-0">
                Не сдано
              </span>
            ) : null}
          </div>

          <p className="text-[11px] text-[var(--md-text-muted)] mt-0.5">
            {note.transferPending
              ? 'Ожидает расписание'
              : formatDateWithDay(note.date)}
          </p>

          <p
            className={`text-xs mt-1 whitespace-pre-wrap leading-relaxed ${
              note.isCompleted
                ? 'line-through text-[var(--md-text-muted)]'
                : 'text-[var(--md-text-primary)]'
            }`}
          >
            {note.text}
          </p>
        </div>

        <div className="flex items-center gap-1 shrink-0">
          <button
            onClick={() => {
              setEditingNote(note);
              setIsDialogOpen(true);
            }}
            className="p-1.5 text-[var(--md-text-muted)] hover:text-[var(--nika-subject)] transition-colors"
            title="Редактировать"
          >
            <Edit2 className="w-4 h-4" />
          </button>
          <button
            onClick={() => deleteNote(note.id)}
            className="p-1.5 text-[var(--md-text-muted)] hover:text-red-500 transition-colors"
            title="Удалить"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      </div>
    );
  };

  const renderEventCard = (ev: CalendarEvent, idx: number) => {
    const label =
      ev.sourceLabel &&
      !ev.sourceLabel.toLowerCase().includes('файл') &&
      !ev.sourceLabel.toLowerCase().includes('сайт')
        ? ev.sourceLabel
        : null;
    const formattedDate = formatDateWithDay(ev.date);
    const dateSubtitle = label ? `${formattedDate} • ${label}` : formattedDate;
    const dotColor = (ev.color && EVENT_DOT_COLORS[ev.color]) || '#059669';

    return (
      <div
        key={`event_${ev.date}_${idx}`}
        onClick={() => {
          setSelectedDateString(ev.date);
          const p = ev.date.split('-');
          if (p.length === 3) {
            setCurrentDate(new Date(parseInt(p[0], 10), parseInt(p[1], 10) - 1, 1));
          }
        }}
        className="p-3 bg-[var(--md-surface)] border border-[var(--md-border-light)] rounded-xl flex items-center justify-between gap-3 shadow-xs cursor-pointer hover:bg-[var(--md-surface-variant)]/40 transition-colors"
      >
        <div className="flex items-center gap-2.5 min-w-0">
          <span
            className="w-2.5 h-2.5 rounded-full shrink-0"
            style={{ backgroundColor: dotColor }}
          />
          <div className="min-w-0">
            <h4 className="font-bold text-xs sm:text-sm text-[var(--md-text-primary)]">
              {ev.title}
            </h4>
            <p className="text-[11px] text-[var(--md-text-muted)] mt-0.5">
              {dateSubtitle}
            </p>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div
      {...swipeHandlers}
      className="w-full max-w-4xl lg:max-w-7xl mx-auto flex-1 flex flex-col lg:flex-row lg:items-stretch bg-[var(--md-background)] relative lg:border-x border-[var(--md-border-light)]"
    >
      {/* Left Column (Full width on mobile; Fixed calendar panel on desktop) */}
      <div className="lg:w-[440px] xl:w-[480px] lg:shrink-0 lg:border-r border-[var(--md-border-light)] flex flex-col">
      {/* 1. Unified Surface Header: Month Navigation & Days of Week */}
      <div className="bg-[var(--md-surface)] border-b border-[var(--md-border-light)] transition-colors">
        {/* Month Navigation Row */}
        <div className="flex items-center justify-between px-3 py-1.5">
          <button
            onClick={prevMonth}
            className="p-1.5 rounded-lg text-[var(--md-text-secondary)] hover:bg-[var(--md-surface-variant)] transition-colors"
            title="Предыдущий месяц"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <h2 className="font-bold text-sm sm:text-base text-[var(--md-text-primary)] text-center capitalize">
            {monthNames[month]} {year}
          </h2>
          <button
            onClick={nextMonth}
            className="p-1.5 rounded-lg text-[var(--md-text-secondary)] hover:bg-[var(--md-surface-variant)] transition-colors"
            title="Следующий месяц"
          >
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>

        {/* Days of week header under month row */}
        <div className="grid grid-cols-7 px-2 pb-2 text-center">
          {['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'].map((d, idx) => (
            <div
              key={d}
              className={`text-[11px] font-bold ${
                idx === 6 ? 'text-red-500' : idx === 5 ? 'text-[var(--md-secondary)]' : 'text-[var(--md-text-muted)]'
              }`}
            >
              {d}
            </div>
          ))}
        </div>
      </div>

      {/* 2. Calendar Grid Container: unrounded rectangle across full width, borderless except bottom divider */}
      <div className="w-full bg-[var(--md-surface)] border-b border-[var(--md-border-light)] px-2 py-1.5 transition-colors">
        <div className="grid grid-cols-7 gap-1">
          {Array.from({ length: startDayOfWeek }).map((_, i) => (
            <div key={`empty-${i}`} className="aspect-[1.15] w-full" />
          ))}

          {Array.from({ length: daysInMonth }).map((_, i) => {
            const dayNum = i + 1;
            const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(dayNum).padStart(2, '0')}`;
            const dayEvents = eventsByDate.get(dateStr) || [];
            const hasNotes = hasNotesOnDate(dateStr);
            const isToday = todayIso === dateStr;
            const isSelected = selectedDateString === dateStr;

            const dayOfWeek = (startDayOfWeek + i) % 7;
            const isSunday = dayOfWeek === 6;
            const isSaturday = dayOfWeek === 5;

            const cellBg = isSelected
              ? 'bg-[var(--nika-subject)] text-[var(--md-on-primary)]'
              : isToday
              ? 'bg-[var(--nika-active-bg)]/40 border-[var(--nika-subject)]'
              : isSunday
              ? 'bg-red-500/10 border-red-500/20'
              : isSaturday
              ? 'bg-[var(--md-secondary-container)]/35 border-[var(--md-secondary)]/40'
              : 'bg-[var(--md-surface-variant)]/30 border-[var(--md-border-light)]/60';

            const textColor = isSelected
              ? 'text-[var(--md-on-primary)] font-bold'
              : isToday
              ? 'text-[var(--nika-subject)] font-bold'
              : isSunday
              ? 'text-red-500 font-semibold'
              : isSaturday
              ? 'text-[var(--md-secondary)] font-semibold'
              : 'text-[var(--md-text-primary)] font-semibold';

            return (
              <div
                key={dayNum}
                onClick={() => handleDayClick(dateStr)}
                className={`aspect-[1.15] w-full p-1 rounded-xl border flex flex-col items-center justify-center cursor-pointer transition-all select-none relative ${cellBg}`}
              >
                {/* Large centered day number */}
                <span className={`text-[17px] leading-none ${textColor}`}>
                  {dayNum}
                </span>

                {/* Dot markers strictly under day number */}
                <div className="flex items-center gap-1 mt-1 h-1.5">
                  {dayEvents.length > 0 && (
                    <span
                      className={`w-[4px] h-[4px] rounded-full ${
                        isSelected
                          ? 'bg-[var(--md-on-primary)]'
                          : isSunday
                          ? 'bg-red-500'
                          : isSaturday
                          ? 'bg-[var(--md-secondary)]'
                          : 'bg-black dark:bg-white opacity-80'
                      }`}
                    />
                  )}
                  {hasNotes && (
                    <Edit2
                      className={`w-1.5 h-1.5 ${
                        isSelected
                          ? 'text-[var(--md-on-primary)]'
                          : 'text-black dark:text-white opacity-90'
                      }`}
                    />
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Archive weeks & Schedule nav under calendar */}
      {(archiveWeeksInMonth.length > 0 || selectedDateString) && (
        <div className="px-3 pt-3 space-y-2.5">
          {archiveWeeksInMonth.length > 0 && (
            <div data-no-swipe="true" className="flex items-center gap-1.5 overflow-x-auto pb-0.5 scrollbar-none">
              <span className="text-[11px] font-bold text-[var(--md-text-muted)] shrink-0 mr-1">
                Архив:
              </span>
              {archiveWeeksInMonth.map(({ isoMonday, idx }) => {
                const weekParam = -(idx + 1);
                return (
                  <button
                    key={isoMonday}
                    onClick={() => {
                      setSelectedArchiveWeek(weekParam);
                      loadArchiveSchedule(weekParam);
                      navigate('/archive');
                    }}
                    className="px-2.5 py-1 rounded-xl bg-[var(--md-surface)] border border-[var(--md-border)] text-[11px] font-bold text-[var(--nika-subject)] hover:bg-[var(--md-surface-variant)] shrink-0 transition-all shadow-xs"
                  >
                    {formatArchiveWeekChip(isoMonday)}
                  </button>
                );
              })}
            </div>
          )}

          {selectedDateString && (
            <div className="animate-in fade-in duration-150">
              {selectedScheduleNav ? (
                <button
                  onClick={() => handleOpenSchedule(selectedScheduleNav)}
                  className="w-full py-2.5 px-4 rounded-xl border border-[var(--nika-subject)] text-[var(--nika-subject)] font-bold text-xs sm:text-sm bg-[var(--nika-active-bg)]/20 hover:bg-[var(--nika-active-bg)]/40 flex items-center justify-center gap-2 transition-all shadow-sm"
                >
                  <CalendarIcon className="w-4 h-4" />
                  <span>
                    {selectedScheduleNav.weekOffset === 0
                      ? (new Date(selectedDateString).getDay() === 0
                          ? 'Открыть расписание на понедельник'
                          : 'Открыть расписание на этот день')
                      : (() => {
                          const archiveIdx = -(selectedScheduleNav.weekOffset + 1);
                          const iso = archiveWeeksList[archiveIdx];
                          const label = iso ? formatArchiveWeekChip(iso) : '';
                          return label ? `Открыть архив: ${label}` : 'Открыть архив расписания';
                        })()}
                  </span>
                </button>
              ) : (
                <div className="w-full p-3 rounded-xl bg-[var(--md-surface)] border border-[var(--md-border-light)] flex items-center gap-2.5 text-xs text-[var(--md-text-muted)]">
                  <Info className="w-4 h-4 text-[var(--md-text-muted)] shrink-0" />
                  <span>Расписание на эту неделю недоступно в архиве</span>
                </div>
              )}
            </div>
          )}
        </div>
      )}
      </div>

      {/* Right Column on PC / Stacked Below on Mobile: Search, Categories & 2 Event Lists */}
      <div className="p-3 space-y-4 flex-1 min-w-0 flex flex-col">

      {/* Search Input for Events in Calendar */}
      <div className="relative">
        <Search className="w-4 h-4 text-[var(--md-text-muted)] absolute left-3.5 top-3" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Поиск по событиям..."
          className="w-full pl-10 pr-9 py-2 bg-[var(--md-surface)] border border-[var(--md-border)] rounded-2xl text-xs sm:text-sm outline-none focus:border-[var(--nika-subject)] transition-colors"
        />
        {searchQuery && (
          <button
            onClick={() => setSearchQuery('')}
            className="absolute right-3 top-2.5 text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)]"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>

      {/* Categories Horizontal Ribbon matching Android CalendarScreen.kt */}
      <div data-no-swipe="true" className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none">
        {categoriesList.map((cat, idx) => {
          const isSelected = selectedCategory === cat.key;
          const shortcut = idx < 9 ? String(idx + 1) : idx === 9 ? '0' : undefined;

          return (
            <button
              key={cat.key ?? 'ALL'}
              onClick={() =>
                setSelectedCategory(cat.key === null ? null : selectedCategory === cat.key ? null : cat.key)
              }
              title={shortcut ? `${cat.label} [${shortcut}]` : cat.label}
              className={`h-8 px-3 rounded-lg text-xs font-semibold shrink-0 transition-all ${
                isSelected
                  ? cat.isDebt
                    ? 'bg-red-500/20 text-red-600 dark:text-red-400 font-bold shadow-sm'
                    : 'bg-[var(--md-surface-variant)] text-[var(--md-text-primary)] font-bold shadow-sm'
                  : 'text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
              }`}
            >
              {cat.label}
            </button>
          );
        })}
      </div>

      {/* List 1: Events and Notes for Selected / Today Date */}
      <div className="space-y-2">
        <div className="flex items-center justify-between gap-2">
          <h3 className="font-bold text-xs text-[var(--md-text-muted)]">
            {selectedDateString && selectedDateString !== todayIso
              ? `События и заметки на ${formatDateWithDay(selectedDateString)}`
              : `События и заметки на сегодня (${formatDateWithDay(todayIso)})`}
          </h3>
          {selectedDateString && selectedDateString !== todayIso && (
            <button
              onClick={() => setSelectedDateString(null)}
              className="text-xs text-[var(--nika-subject)] font-semibold hover:underline shrink-0"
            >
              Сегодня
            </button>
          )}
        </div>

        {displayedNotes.length === 0 && displayedEvents.length === 0 ? (
          <div className="py-8 px-4 text-center">
            <p className="text-sm font-medium text-[var(--md-border)]">
              {searchQuery.trim()
                ? `По запросу «${searchQuery}» ничего не найдено`
                : 'На выбранную дату записей нет'}
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {displayedNotes.map((note) => renderNoteCard(note))}
            {displayedEvents.map((ev, idx) => renderEventCard(ev, idx))}
          </div>
        )}
      </div>

      {/* List 2: Upcoming Events for next 21 days (3 weeks) */}
      <div className="space-y-2 pt-2 border-t border-[var(--md-border-light)]">
        <div className="flex items-center justify-between">
          <h3 className="font-bold text-xs text-[var(--md-text-muted)]">
            Ближайшие события (21 день)
          </h3>
        </div>

        {upcomingItems.length === 0 ? (
          <div className="py-8 px-4 text-center">
            <p className="text-sm font-medium text-[var(--md-border)]">
              {searchQuery.trim()
                ? `По запросу «${searchQuery}» ничего не найдено`
                : 'В ближайшие 21 день записей нет'}
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {upcomingItems.map((entry, idx) =>
              entry.kind === 'note'
                ? renderNoteCard(entry.note)
                : renderEventCard(entry.event, idx)
            )}
          </div>
        )}
      </div>
      </div>

      {/* Note Edit Dialog */}
      {isDialogOpen && (
        <NoteEditDialog
          targetName={targetName}
          initialDate={selectedDateString || todayIso}
          existingNote={editingNote}
          onSave={(data) => {
            if (editingNote) {
              updateNote({ ...editingNote, ...data });
            } else {
              useNotesStore.getState().addNote(data);
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
