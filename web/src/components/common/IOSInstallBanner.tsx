import React from 'react';
import { Share, X, PlusSquare } from 'lucide-react';

interface Props {
  onDismiss: () => void;
}

export const IOSInstallBanner: React.FC<Props> = ({ onDismiss }) => {
  return (
    <div className="fixed bottom-20 left-4 right-4 z-50 bg-[var(--md-surface)] border border-[var(--md-border)] rounded-2xl p-4 shadow-2xl backdrop-blur-md animate-in fade-in slide-in-from-bottom duration-300">
      <div className="flex items-start justify-between">
        <div className="flex gap-3">
          <div className="w-12 h-12 rounded-xl bg-[var(--md-surface-variant)] flex items-center justify-center shrink-0">
            <img src="favicon.svg" alt="App Icon" className="w-8 h-8 rounded-lg" />
          </div>
          <div>
            <h4 className="font-bold text-[var(--md-text-primary)] text-sm">
              Установите ScheduleXY
            </h4>
            <p className="text-xs text-[var(--md-text-secondary)] mt-1">
              Для работы без браузера и быстрого доступа добавьте приложение на главный экран.
            </p>
          </div>
        </div>
        <button
          onClick={onDismiss}
          className="text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)] p-1"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      <div className="mt-3 pt-3 border-t border-[var(--md-border-light)] flex items-center gap-2 text-xs text-[var(--md-text-secondary)]">
        <span>Нажмите</span>
        <Share className="w-4 h-4 text-blue-500" />
        <span>«Поделиться» и выберите</span>
        <PlusSquare className="w-4 h-4 text-[var(--md-text-primary)]" />
        <span className="font-semibold text-[var(--md-text-primary)]">«На экран "Домой"»</span>
      </div>
    </div>
  );
};
