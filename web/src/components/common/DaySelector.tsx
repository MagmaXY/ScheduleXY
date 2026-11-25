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
  const showWeekMode = Boolean(onSelectWeekView);
  const activeWeekView = showWeekMode && isWeekView;

  const baseDayName = FULL_DAYS[selectedDayIndex] || 'Понедельник';
  const fullTitle = dateLabel ? `${baseDayName}, ${dateLabel}` : baseDayName;
  const weekTitle = weekRangeLabel ? `Вся неделя, ${weekRangeLabel}` : 'Вся неделя';

  const handlePrev = () => {
    if (activeWeekView) {
      onDaySelected(5); // Сб
    } else if (selectedDayIndex === 0) {
      if (onSelectWeekView) {
        onSelectWeekView();
      } else {
        onDaySelected(5);
      }
    } else {
      onDaySelected(selectedDayIndex - 1);
    }
  };

  const handleNext = () => {
    if (activeWeekView) {
      onDaySelected(0); // Пн
    } else if (selectedDayIndex === 5) {
      if (onSelectWeekView) {
        onSelectWeekView();
      } else {
        onDaySelected(0);
      }
    } else {
      onDaySelected(selectedDayIndex + 1);
    }
  };

  return (
    <div className="w-full bg-[var(--md-surface)] border-b border-[var(--md-border-light)] py-1 shadow-sm">
      {/* 1. Header with Arrows, Day Name/Week Title, Date, and "Сегодня" Badge */}
      <div className="flex items-center justify-between px-2 py-0.5 relative">
        <button
          type="button"
          onClick={handlePrev}
          className="w-8 h-8 flex items-center justify-center rounded-full text-[var(--md-text-secondary)] hover:bg-[var(--md-surface-variant)] transition-colors"
          title="Предыдущий день"
        >
          <ChevronLeft className="w-4 h-4" />
        </button>

        <div
          onClick={activeWeekView ? undefined : onDateClick}
          className={`flex items-center gap-1.5 select-none ${
            !activeWeekView && onDateClick
              ? 'cursor-pointer hover:opacity-80 active:scale-98 transition-all'
              : ''
          }`}
          title={!activeWeekView && onDateClick ? 'Открыть в календаре' : undefined}
        >
          {activeWeekView ? (
            <span className="font-bold text-[14.5px] text-[var(--md-text-primary)]">
              {weekTitle}
            </span>
          ) : (
            <>
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
            </>
          )}
        </div>

        <div className="flex items-center gap-0.5">
          {activeWeekView && weekActions}
          <button
            type="button"
            onClick={handleNext}
            className="w-8 h-8 flex items-center justify-center rounded-full text-[var(--md-text-secondary)] hover:bg-[var(--md-surface-variant)] transition-colors"
            title="Следующий день"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* 2. Quick Day Buttons Ribbon */}
      <div className="px-2 pt-0.5">
        <div
          data-no-swipe="true"
          className="flex items-center justify-between bg-[var(--md-surface-variant)]/60 rounded-xl p-1 gap-1"
        >
          {showWeekMode && (
            <button
              type="button"
              onClick={onSelectWeekView}
              title="Вся неделя [1]"
              className={`flex-1 py-1.5 rounded-lg text-xs transition-all text-center ${
                activeWeekView
                  ? 'bg-[var(--md-surface)] text-[var(--md-text-primary)] font-bold shadow-sm'
                  : 'text-[var(--md-text-secondary)] font-normal hover:bg-[var(--md-surface)]/50'
              }`}
            >
              <span>Вся</span>
            </button>
          )}

          {SHORT_DAYS.map((name, index) => {
            const isSelected = !activeWeekView && index === selectedDayIndex;
            const isToday = index === currentDayIndex;
            const shortcut = showWeekMode ? index + 2 : index + 1;

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
                title={`${FULL_DAYS[index]} [${shortcut}]`}
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
