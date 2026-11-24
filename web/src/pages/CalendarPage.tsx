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
import { toLocalDateIso, parseLocalDate } from '../utils/dateUtils';

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

  // Arrow key navigation for calendar months
  React.useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName)) return;
      if (e.key === 'ArrowRight') {
        nextMonth();
      } else if (e.key === 'ArrowLeft') {
        prevMonth();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [year, month]);

  // Touch and LMB mouse drag swipe support for switching months
  const swipeHandlers = useSwipeGesture({
    onSwipeLeft: nextMonth,
    onSwipeRight: prevMonth,
  });

  // Filtered notes and events matching Android CalendarScreen.kt
  const isNoteCategory = selectedCategory && ['CAT_HW', 'CAT_NOTES', 'CAT_DEBTS'].includes(selectedCategory);
  const isEventColorCategory = selectedCategory && !isNoteCategory;

  const currentNotes = React.useMemo(() => {
    return notes.filter((n) => n.targetName === targetName);
  }, [notes, targetName]);

  const displayedNotes = React.useMemo(() => {
    if (isEventColorCategory) return [];

    const base = currentNotes.filter((note) => {
      const matchesDate = !selectedDateString || note.date === selectedDateString;
      const matchesCat = (() => {
        if (selectedCategory === 'CAT_HW') return note.type === 'homework';
        if (selectedCategory === 'CAT_NOTES') return note.type === 'note';
        if (selectedCategory === 'CAT_DEBTS') {
          return note.type === 'homework' && !note.isCompleted && (note.date < todayIso || note.transferPending);
        }
        return true;
      })();
      return matchesDate && matchesCat;
    });

    return searchNotes(base, searchQuery);
  }, [currentNotes, selectedCategory, selectedDateString, todayIso, searchQuery, isEventColorCategory]);

  const displayedEvents = React.useMemo(() => {
    if (isNoteCategory) return [];

    const base = eventsList.filter((event) => {
      const matchesCat = !selectedCategory || event.color === selectedCategory;
      const matchesDate = !selectedDateString || event.date === selectedDateString;
      return matchesCat && matchesDate;
    });

    return searchEvents(base, searchQuery);
  }, [eventsList, selectedCategory, selectedDateString, isNoteCategory, searchQuery]);

  return (
    <div
      {...swipeHandlers}
      className="max-w-4xl mx-auto flex flex-col min-h-screen bg-[var(--md-background)] relative lg:border-x border-[var(--md-border-light)]"
    >
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

      <div className="p-3 space-y-3 flex-1 flex flex-col pb-24">

      {/* Archive weeks available in this month */}
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

      {/* Button to open schedule or archive info banner for selected day */}
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
        <button
          onClick={() => setSelectedCategory(null)}
          className={`h-8 px-3 rounded-lg text-xs font-semibold shrink-0 transition-all border ${
            selectedCategory === null
              ? 'bg-[var(--md-surface-variant)] border-[var(--md-border)] text-[var(--md-text-primary)] font-bold shadow-sm'
              : 'border-[var(--md-border)] text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
          }`}
        >
          Все
        </button>

        {!isTeacherMode && (
          <button
            onClick={() =>
              setSelectedCategory(selectedCategory === 'CAT_HW' ? null : 'CAT_HW')
            }
            className={`h-8 px-3 rounded-lg text-xs font-semibold shrink-0 transition-all border ${
              selectedCategory === 'CAT_HW'
                ? 'bg-[var(--md-surface-variant)] border-[var(--md-border)] text-[var(--md-text-primary)] font-bold shadow-sm'
                : 'border-[var(--md-border)] text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
            }`}
          >
            ДЗ
          </button>
        )}

        <button
          onClick={() =>
            setSelectedCategory(selectedCategory === 'CAT_NOTES' ? null : 'CAT_NOTES')
          }
          className={`h-8 px-3 rounded-lg text-xs font-semibold shrink-0 transition-all border ${
            selectedCategory === 'CAT_NOTES'
              ? 'bg-[var(--md-surface-variant)] border-[var(--md-border)] text-[var(--md-text-primary)] font-bold shadow-sm'
              : 'border-[var(--md-border)] text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
          }`}
        >
          Заметки
        </button>

        {!isTeacherMode && debtsCount > 0 && (
          <button
            onClick={() =>
              setSelectedCategory(selectedCategory === 'CAT_DEBTS' ? null : 'CAT_DEBTS')
            }
            className={`h-8 px-3 rounded-lg text-xs font-semibold shrink-0 transition-all border ${
              selectedCategory === 'CAT_DEBTS'
                ? 'bg-red-500/20 border-red-500/50 text-red-600 dark:text-red-400 font-bold shadow-sm'
                : 'border-[var(--md-border)] text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
            }`}
          >
            Долги ({debtsCount})
          </button>
        )}

        {/* Lyceum event categories */}
        {eventCategories.map((cat) => (
          <button
            key={cat.color}
            onClick={() =>
              setSelectedCategory(selectedCategory === cat.color ? null : cat.color)
            }
            className={`h-8 px-3 rounded-lg text-xs font-semibold shrink-0 transition-all border ${
              selectedCategory === cat.color
                ? 'bg-[var(--md-surface-variant)] border-[var(--md-border)] text-[var(--md-text-primary)] font-bold shadow-sm'
                : 'border-[var(--md-border)] text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
            }`}
          >
            {cat.title}
          </button>
        ))}
      </div>

      {/* Events and Notes List */}
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <h3 className="font-bold text-xs text-[var(--md-text-muted)]">
            {selectedDateString
              ? `События и заметки на ${selectedDateString}`
              : 'События лицея и заметки'}
          </h3>
          {selectedDateString && (
            <button
              onClick={() => setSelectedDateString(null)}
              className="text-xs text-[var(--nika-subject)] font-semibold hover:underline"
            >
              Показать все
            </button>
          )}
        </div>

        {displayedNotes.length === 0 && displayedEvents.length === 0 ? (
          <div className="p-4 text-center text-xs text-[var(--md-text-muted)] bg-[var(--md-surface)] border border-[var(--md-border)] rounded-2xl">
            {searchQuery.trim()
              ? `По запросу «${searchQuery}» ничего не найдено`
              : 'В выбранной категории записей не найдено'}
          </div>
        ) : (
          <>
            {/* Notes Section first */}
            {displayedNotes.map((note) => {
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
            })}

            {/* Lyceum Events */}
            {displayedEvents.map((ev, idx) => {
              const label =
                ev.sourceLabel &&
                !ev.sourceLabel.toLowerCase().includes('файл') &&
                !ev.sourceLabel.toLowerCase().includes('сайт')
                  ? ev.sourceLabel
                  : null;
              const dateSubtitle = label ? `${ev.date} • ${label}` : ev.date;

              return (
                <div
                  key={`event_${idx}`}
                  className="p-3 bg-[var(--md-surface)] border border-[var(--md-border-light)] rounded-xl flex items-center justify-between gap-3 shadow-xs"
                >
                  <div className="flex items-center gap-2.5">
                    <span
                      className="w-2.5 h-2.5 rounded-full shrink-0"
                      style={{
                        backgroundColor:
                          ev.color === 'blue'
                            ? '#2563EB'
                            : ev.color === 'orange'
                            ? '#D97706'
                            : ev.color === 'red'
                            ? '#DC2626'
                            : '#059669',
                      }}
                    />
                    <div>
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
            })}
          </>
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
