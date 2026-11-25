import React, { useRef, useState } from 'react';
import {
  Sun,
  Moon,
  Settings,
  Info,
} from 'lucide-react';
import { RealtimeState } from '../../hooks/useRealtimeSync';
import { getBellDisplayText } from '../../domain/bellSchedule';
import { usePreferencesStore } from '../../store/preferencesStore';

interface Props {
  realtimeState: RealtimeState;
  isLoading: boolean;
  onRefresh?: () => void;
  onOpenSettings: () => void;
  onOpenInfo: () => void;
}

export const Header: React.FC<Props> = ({
  realtimeState,
  isLoading,
  onOpenSettings,
  onOpenInfo,
}) => {
  const isDarkMode = usePreferencesStore((s) => s.isDarkMode);
  const toggleDarkMode = usePreferencesStore((s) => s.toggleDarkMode);
  const useMagmaLogo = usePreferencesStore((s) => s.useMagmaLogo);
  const setIsMagmaUnlocked = usePreferencesStore((s) => s.setIsMagmaUnlocked);
  const statusBarPairsMode = usePreferencesStore((s) => s.statusBarPairsMode);

  // Easter egg: 7 taps on the logo within 2 seconds
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const logoTapCountRef = useRef(0);
  const lastLogoTapTimeRef = useRef(0);

  const handleLogoClick = () => {
    const now = Date.now();
    if (now - lastLogoTapTimeRef.current > 2000) {
      logoTapCountRef.current = 1;
    } else {
      logoTapCountRef.current += 1;
    }
    lastLogoTapTimeRef.current = now;

    if (logoTapCountRef.current >= 4 && logoTapCountRef.current <= 6) {
      const remaining = 7 - logoTapCountRef.current;
      showToast(`Осталось тапов: ${remaining}`);
    } else if (logoTapCountRef.current >= 7) {
      logoTapCountRef.current = 0;
      setIsMagmaUnlocked(true);
      showToast('Режим MagmaXY разблокирован!');
      onOpenSettings();
    }
  };

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage((prev) => (prev === msg ? null : prev));
    }, 2000);
  };

  // Bell status logic strictly between 8:00 AM and 19:00 (15:30 in pairs mode)
  const now = new Date();
  const curMinutes = now.getHours() * 60 + now.getMinutes();
  const maxMinutes = statusBarPairsMode ? 15 * 60 + 30 : 19 * 60;
  const isTimeInRange = curMinutes >= 8 * 60 && curMinutes < maxMinutes;

  const bellState = realtimeState.bellState;
  const isLesson = bellState.type === 'in_lesson';
  const isBreak = bellState.type === 'break';
  const isBeforeSchool = bellState.type === 'before_school';

  const isSchoolActive = isTimeInRange && (isLesson || isBreak || isBeforeSchool);

  const bellText = getBellDisplayText(bellState, statusBarPairsMode);
  const progress =
    isLesson || isBreak ? ('progress' in bellState ? bellState.progress : null) : null;
  const showPercent = progress !== null && progress >= 0.01 && progress <= 0.99;
  const percent = progress !== null ? Math.round(progress * 100) : 0;

  const statusColorClass = isLesson
    ? 'text-[var(--md-secondary)]'
    : isBreak
    ? 'text-[var(--nika-gold)]'
    : 'text-[var(--nika-subject)]';

  const progressBgClass = isLesson
    ? 'bg-[var(--md-secondary)]'
    : isBreak
    ? 'bg-[var(--nika-gold)]'
    : 'bg-[var(--nika-subject)]';

  return (
    <header className="sticky top-0 z-30 bg-[var(--md-surface)] border-b border-[var(--md-border-light)] safe-top transition-colors shadow-[0_1px_2px_rgba(0,0,0,0.03)]">
      <div className="max-w-4xl lg:max-w-none mx-auto px-3.5 lg:px-5 py-1.5 flex flex-col lg:border-x-0 border-[var(--md-border-light)]">
        {/* Main Top Bar */}
        <div className="flex items-center justify-between">
          {/* Left: Logo & Permanent School Title */}
          <div
            onClick={handleLogoClick}
            className="flex items-center gap-2 cursor-pointer select-none py-1 flex-1 min-w-0"
          >
            <div className="w-[34px] h-[34px] rounded-full overflow-hidden shrink-0 flex items-center justify-center transition-transform active:scale-95">
              <img
                src={useMagmaLogo ? 'ic_magma_logo.png' : 'ic_lyceum_logo.png'}
                alt="Логотип"
                className="w-8 h-8 object-contain"
              />
            </div>
            {/* Title is PERMANENTLY "Инженерный лицей НГТУ" */}
            <h1 className="font-bold text-[15.5px] leading-tight text-[var(--md-text-primary)] truncate">
              Инженерный лицей НГТУ
            </h1>
          </div>

          {/* Right: Action Buttons (Info, Theme, Settings) - Refresh button removed per request */}
          <div className="flex items-center">
            <button
              onClick={onOpenInfo}
              className="w-9 h-9 flex items-center justify-center rounded-full text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] active:bg-[var(--md-surface-variant)] transition-all"
              title="О приложении и сервисы"
            >
              <Info className="w-5 h-5" />
            </button>

            <button
              onClick={toggleDarkMode}
              className="w-9 h-9 flex items-center justify-center rounded-full text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] active:bg-[var(--md-surface-variant)] transition-all"
              title={isDarkMode ? 'Светлая тема' : 'Тёмная тема'}
            >
              {isDarkMode ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
            </button>

            <button
              onClick={onOpenSettings}
              className="w-9 h-9 flex items-center justify-center rounded-full text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] active:bg-[var(--md-surface-variant)] transition-all"
              title="Настройки"
            >
              <Settings className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Realtime Live Bell Ticker Card */}
        {isSchoolActive && (
          <div className="mt-1.5 mb-1 px-3 py-1.5 rounded-xl bg-[var(--md-surface-variant)]/60 border border-[var(--md-border-light)] transition-all">
            <div className="flex items-center justify-between text-xs">
              <span className={`font-semibold text-[12px] truncate ${statusColorClass}`}>
                {bellText}
              </span>
              {showPercent && (
                <span className={`font-bold text-[11px] ml-2 shrink-0 ${statusColorClass}`}>
                  {percent}%
                </span>
              )}
            </div>

            {/* Linear Progress Indicator */}
            {progress !== null && (isLesson || isBreak) && (
              <div className="mt-1.5 w-full h-1 bg-[var(--md-border-light)] rounded-full overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-300 ${progressBgClass}`}
                  style={{ width: `${Math.min(100, Math.max(0, percent))}%` }}
                />
              </div>
            )}
          </div>
        )}

        {/* Network sync progress bar under header */}
        {isLoading && (
          <div className="h-0.5 w-full bg-[var(--md-surface-variant)] overflow-hidden rounded-full mt-1">
            <div className="h-full bg-[var(--nika-subject)] animate-pulse w-full" />
          </div>
        )}
      </div>

      {/* Floating feedback toast */}
      {toastMessage && (
        <div className="fixed top-14 left-1/2 -translate-x-1/2 z-50 px-3.5 py-1.5 rounded-full bg-black/80 text-white text-xs font-medium shadow-lg animate-in fade-in duration-150">
          {toastMessage}
        </div>
      )}
    </header>
  );
};
