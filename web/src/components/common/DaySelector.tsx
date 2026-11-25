import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface Props {
  selectedDayIndex: number;
  currentDayIndex: number; // today's index (0..5)
  dateLabel?: string;
  isWeekView?: boolean;
  weekRangeLabel?: string;
  onSelectWeekView?: () => void;
  onDaySelected: (day: number) => void;
  onDateClick?: () => void;
  weekActions?: React.ReactNode;
}

const SHORT_DAYS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'];
const FULL_DAYS = [
  'Понедельник',
  'Вторник',
  'Среда',
  'Четверг',
  'Пятница',
  'Суббота',
];

export const DaySelector: React.FC<Props> = ({
  selectedDayIndex,
  currentDayIndex,
  dateLabel,
  isWeekView = false,
  weekRangeLabel,
  onSelectWeekView,
  onDaySelected,
  onDateClick,
  weekActions,
}) => {
  const baseDayName = FULL_DAYS[selectedDayIndex] || 'Понедельник';
  const fullTitle = dateLabel ? `${baseDayName}, ${dateLabel}` : baseDayName;
  const weekTitle = weekRangeLabel ? `Вся неделя, ${weekRangeLabel}` : 'Вся неделя';

  return (
    <div className="w-full bg-[var(--md-surface)] border-b border-[var(--md-border-light)] py-1 shadow-sm">
      {/* 1. Header with Arrows, Day Name, Date, and "Сегодня" Badge */}
      <div className="flex items-center justify-between px-2 py-0.5 relative">
        <button
          type="button"
          onClick={() => {
            const prev = selectedDayIndex > 0 ? selectedDayIndex - 1 : 5;
            onDaySelected(prev);
          }}
          className={`w-8 h-8 flex items-center justify-center rounded-full text-[var(--md-text-secondary)] hover:bg-[var(--md-surface-variant)] transition-colors ${
            isWeekView ? 'lg:invisible' : ''
          }`}
          title="Предыдущий день"
        >
          <ChevronLeft className="w-4 h-4" />
        </button>

        {/* Title: single day on mobile (and when not week view), week title on PC week view */}
        <div
          onClick={isWeekView ? undefined : onDateClick}
          className={`flex items-center gap-1.5 select-none ${
            !isWeekView && onDateClick ? 'cursor-pointer hover:opacity-80 active:scale-98 transition-all' : ''
          }`}
          title={!isWeekView && onDateClick ? 'Открыть в календаре' : undefined}
        >
          <span
            className={`font-bold text-[14.5px] ${
              isWeekView ? 'lg:hidden' : ''
            } ${
              selectedDayIndex === 5
                ? 'text-[var(--md-secondary)]'
                : 'text-[var(--md-text-primary)]'
            }`}
          >
            {fullTitle}
          </span>

          {isWeekView && (
            <span className="hidden lg:inline font-bold text-[14.5px] text-[var(--md-text-primary)]">
              {weekTitle}
            </span>
          )}

          {selectedDayIndex === currentDayIndex && (
            <span
              className={`px-2 py-0.5 rounded-full text-[10px] font-bold bg-[var(--md-secondary-container)] text-[var(--md-on-secondary-container)] ${
                isWeekView ? 'lg:hidden' : ''
              }`}
            >
              Сегодня
            </span>
          )}
        </div>

        <div className="flex items-center gap-1">
          {isWeekView && weekActions && (
            <div className="hidden lg:flex items-center">{weekActions}</div>
          )}
          <button
            type="button"
            onClick={() => {
              const next = selectedDayIndex < 5 ? selectedDayIndex + 1 : 0;
              onDaySelected(next);
            }}
            className={`w-8 h-8 flex items-center justify-center rounded-full text-[var(--md-text-secondary)] hover:bg-[var(--md-surface-variant)] transition-colors ${
              isWeekView ? 'lg:hidden' : ''
            }`}
            title="Следующий день"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* 2. Quick Day Buttons Ribbon (PC: Вся, Пн..Сб; Mobile: Пн..Сб) */}
      <div className="px-2 pt-0.5">
        <div
          data-no-swipe="true"
          className="flex items-center justify-between bg-[var(--md-surface-variant)]/60 rounded-xl p-1 gap-1"
        >
          {onSelectWeekView && (
            <button
              type="button"
              onClick={onSelectWeekView}
              title="Вся неделя [1]"
              className={`hidden lg:flex flex-1 items-center justify-center py-1.5 rounded-lg text-xs transition-all text-center ${
                isWeekView
                  ? 'bg-[var(--md-surface)] text-[var(--md-text-primary)] font-bold shadow-sm'
                  : 'text-[var(--md-text-secondary)] font-normal hover:bg-[var(--md-surface)]/50'
              }`}
            >
              <span>Вся</span>
            </button>
          )}

          {SHORT_DAYS.map((name, index) => {
            const isSelected = index === selectedDayIndex;
            const isToday = index === currentDayIndex;
            const shortcut = onSelectWeekView ? index + 2 : index + 1;

            const textColor = isToday
              ? 'text-[var(--nika-subject)] font-bold'
              : isSelected
              ? isWeekView
                ? 'text-[var(--md-text-primary)] font-bold lg:text-[var(--md-text-secondary)] lg:font-normal'
                : 'text-[var(--md-text-primary)] font-bold'
              : 'text-[var(--md-text-secondary)] font-normal';

            const bgClass = isSelected
              ? isWeekView
                ? 'bg-[var(--md-surface)] shadow-sm lg:bg-transparent lg:shadow-none lg:hover:bg-[var(--md-surface)]/50'
                : 'bg-[var(--md-surface)] shadow-sm'
              : 'hover:bg-[var(--md-surface)]/50';

            return (
              <button
                key={name}
                type="button"
                onClick={() => onDaySelected(index)}
                title={`${FULL_DAYS[index]} [${shortcut}]`}
                className={`flex-1 py-1.5 rounded-lg text-xs transition-all text-center ${textColor} ${bgClass}`}
              >
                <span>{name}</span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};
