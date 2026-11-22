import React from 'react';
import {
  Calendar,
  Clock,
  BookOpen,
  DoorOpen,
  Archive,
} from 'lucide-react';

export type TabKey = 'schedule' | 'calendar' | 'notes' | 'freerooms' | 'archive';

interface Props {
  currentTab: TabKey;
  onSelectTab: (tab: TabKey) => void;
}

const TABS: { key: TabKey; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { key: 'schedule', label: 'Расписание', icon: Clock },
  { key: 'calendar', label: 'Календарь', icon: Calendar },
  { key: 'notes', label: 'Заметки', icon: BookOpen },
  { key: 'freerooms', label: 'Кабинеты', icon: DoorOpen },
  { key: 'archive', label: 'Архив', icon: Archive },
];

export const NavigationBar: React.FC<Props> = ({ currentTab, onSelectTab }) => {
  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-[var(--md-surface)]/95 backdrop-blur-lg border-t border-[var(--md-border)] safe-bottom lg:hidden transition-colors">
      <div className="flex items-center justify-around h-20 max-w-lg mx-auto px-2 py-2">
        {TABS.map((tab) => {
          const Icon = tab.icon;
          const isSelected = currentTab === tab.key;

          return (
            <button
              key={tab.key}
              onClick={() => onSelectTab(tab.key)}
              title={tab.label}
              className={`flex-1 flex flex-col items-center justify-center py-2 transition-all relative ${
                isSelected
                  ? 'text-[var(--nika-subject)] font-semibold'
                  : 'text-[var(--md-text-muted)] hover:text-[var(--md-text-secondary)]'
              }`}
            >
              <div
                className={`p-2.5 rounded-2xl transition-all ${
                  isSelected ? 'bg-[var(--md-surface-variant)] scale-110' : ''
                }`}
              >
                <Icon className="w-5.5 h-5.5" />
              </div>
            </button>
          );
        })}
      </div>
    </nav>
  );
};

export const NavigationRail: React.FC<Props> = ({ currentTab, onSelectTab }) => {
  return (
    <aside className="hidden lg:flex flex-col items-center w-20 bg-[var(--md-surface)] border-r border-[var(--md-border)] py-10 gap-6 shrink-0 safe-top">
      <div className="flex flex-col items-center gap-4 w-full">
        {TABS.map((tab) => {
          const Icon = tab.icon;
          const isSelected = currentTab === tab.key;

          return (
            <button
              key={tab.key}
              onClick={() => onSelectTab(tab.key)}
              title={tab.label}
              className={`w-12 h-12 rounded-2xl flex items-center justify-center transition-all ${
                isSelected
                  ? 'bg-[var(--md-surface-variant)] text-[var(--nika-subject)] font-semibold shadow-sm scale-105'
                  : 'text-[var(--md-text-muted)] hover:text-[var(--md-text-secondary)] hover:bg-[var(--md-surface-variant)]/50'
              }`}
            >
              <Icon className="w-5.5 h-5.5" />
            </button>
          );
        })}
      </div>
    </aside>
  );
};
