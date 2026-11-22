import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface Props {
  selectedDayIndex: number;
  currentDayIndex: number; // today's index (0..5)
  dateLabel?: string;
  onDaySelected: (day: number) => void;
  onDateClick?: () => void;
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
  onDaySelected,
  onDateClick,
}) => {
  const baseDayName = FULL_DAYS[selectedDayIndex] || 'Понедельник';
  const fullTitle = dateLabel ? `${baseDayName}, ${dateLabel}` : baseDayName;

  return (
    <div className="w-full bg-[var(--md-surface)] border-b border-[var(--md-border-light)] py-1 shadow-sm">
      {/* 1. Header with Arrows, Day Name, Date, and "Сегодня" Badge */}
      <div className="flex items-center justify-between px-2 py-0.5">
        <button
          type="button"
          onClick={() => {
            const prev = selectedDayIndex > 0 ? selectedDayIndex - 1 : 5;
            onDaySelected(prev);
          }}
          className="w-8 h-8 flex items-center justify-center rounded-full text-[var(--md-text-secondary)] hover:bg-[var(--md-surface-variant)] transition-colors"
          title="Предыдущий день"
        >
          <ChevronLeft className="w-4 h-4" />
        </button>

        <div
          onClick={onDateClick}
          className={`flex items-center gap-1.5 select-none ${
            onDateClick ? 'cursor-pointer hover:opacity-80 active:scale-98 transition-all' : ''
          }`}
          title={onDateClick ? 'Открыть в календаре' : undefined}
        >
          <span
            className={`font-bold text-[14.5px] ${
              selectedDayIndex === 5
                ? 'text-[var(--md-secondary)]'
                : 'text-[var(--md-text-primary)]'
            }`}
          >
            {fullTitle}
          </span>

          {selectedDayIndex === currentDayIndex && (
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-[var(--md-secondary-container)] text-[var(--md-on-secondary-container)]">
              Сегодня
            </span>
          )}
        </div>

        <button
          type="button"
          onClick={() => {
            const next = selectedDayIndex < 5 ? selectedDayIndex + 1 : 0;
            onDaySelected(next);
          }}
          className="w-8 h-8 flex items-center justify-center rounded-full text-[var(--md-text-secondary)] hover:bg-[var(--md-surface-variant)] transition-colors"
          title="Следующий день"
        >
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>

      {/* 2. Quick Day Buttons Ribbon (Пн, Вт, Ср, Чт, Пт, Сб) without dots */}
      <div className="px-2 pt-0.5">
        <div data-no-swipe="true" className="flex items-center justify-between bg-[var(--md-surface-variant)]/60 rounded-xl p-1 gap-1">
          {SHORT_DAYS.map((name, index) => {
            const isSelected = index === selectedDayIndex;
            const isToday = index === currentDayIndex;

            const textColor = isToday
              ? 'text-[var(--nika-subject)] font-bold'
              : isSelected
              ? 'text-[var(--md-text-primary)] font-bold'
              : 'text-[var(--md-text-secondary)] font-normal';

            return (
              <button
                key={name}
                type="button"
                onClick={() => onDaySelected(index)}
                className={`flex-1 py-1.5 rounded-lg text-xs transition-all text-center ${textColor} ${
                  isSelected
                    ? 'bg-[var(--md-surface)] shadow-sm'
                    : 'hover:bg-[var(--md-surface)]/50'
                }`}
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
