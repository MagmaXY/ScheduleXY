import React, { useState, useEffect } from 'react';
import { Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { Header } from './components/layout/Header';
import { NavigationBar, NavigationRail, TabKey } from './components/layout/NavigationBar';
import { SchedulePage } from './pages/SchedulePage';
import { NotesPage } from './pages/NotesPage';
import { CalendarPage } from './pages/CalendarPage';
import { FreeRoomsPage } from './pages/FreeRoomsPage';
import { ArchivePage } from './pages/ArchivePage';
import { WidgetPage } from './pages/WidgetPage';
import { SettingsDialog } from './components/settings/SettingsDialog';
import { InfoSheet } from './components/layout/InfoSheet';
import { IOSInstallBanner } from './components/common/IOSInstallBanner';
import { useRealtimeSync } from './hooks/useRealtimeSync';
import { useFavicon } from './hooks/useFavicon';
import { useIOSInstallPrompt } from './hooks/useIOSInstallPrompt';
import { useScheduleStore } from './store/scheduleStore';
import { usePreferencesStore } from './store/preferencesStore';
import { useNotesStore } from './store/notesStore';
import { yandexSyncService } from './services/yandexSync';
import { setClientConfig } from './api/client';
import { notificationService } from './services/notificationService';

export const App: React.FC = () => {
  useFavicon();
  const { showPrompt, dismiss } = useIOSInstallPrompt();
  const realtimeState = useRealtimeSync();

  const initBaseData = useScheduleStore((s) => s.initBaseData);
  const loadSchedule = useScheduleStore((s) => s.loadSchedule);
  const isLoading = useScheduleStore((s) => s.isLoading);
  const loadNotes = useNotesStore((s) => s.loadNotes);
  const targetType = usePreferencesStore((s) => s.targetType);
  const targetName = usePreferencesStore((s) => s.targetName);

  const [settingsOpen, setSettingsOpen] = useState(false);
  const [infoOpen, setInfoOpen] = useState(false);

  const navigate = useNavigate();
  const location = useLocation();

  // Tab mapping
  const currentTab: TabKey = location.pathname.includes('notes')
    ? 'notes'
    : location.pathname.includes('calendar')
    ? 'calendar'
    : location.pathname.includes('freerooms')
    ? 'freerooms'
    : location.pathname.includes('archive')
    ? 'archive'
    : 'schedule';

  // Update document HTML title based on active tab
  useEffect(() => {
    const tabTitles: Record<TabKey, string> = {
      schedule: 'Расписание',
      calendar: 'Календарь',
      notes: 'Заметки',
      freerooms: 'Кабинеты',
      archive: 'Архив',
    };

    const titlePrefix = location.pathname === '/widget'
      ? 'Виджет'
      : tabTitles[currentTab] || 'Расписание';

    document.title = titlePrefix;
  }, [currentTab, location.pathname]);

  const handleSelectTab = (tab: TabKey) => {
    switch (tab) {
      case 'schedule':
        navigate('/schedule');
        break;
      case 'notes':
        navigate('/notes');
        break;
      case 'calendar':
        navigate('/calendar');
        break;
      case 'freerooms':
        navigate('/freerooms');
        break;
      case 'archive':
        navigate('/archive');
        break;
    }
  };

  useEffect(() => {
    const initialPrefs = usePreferencesStore.getState();
    setClientConfig({ corsProxy: initialPrefs.corsProxy });

    // Check if returning from Yandex OAuth redirect
    yandexSyncService.handleAuthCallback();

    initBaseData();
    loadNotes();
    loadSchedule(initialPrefs.targetType, initialPrefs.targetName, 0);

    // Sync notes on startup if user is logged into Yandex
    const yState = yandexSyncService.getState();
    if (yState.token) {
      yandexSyncService.sync().catch(() => {});
    }

    // Refresh on visibility change
    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        const p = usePreferencesStore.getState();
        loadSchedule(p.targetType, p.targetName, 0);
        if (yandexSyncService.getState().token) {
          yandexSyncService.sync().catch(() => {});
        }
      }
    };
    document.addEventListener('visibilitychange', onVisibilityChange);
    return () => document.removeEventListener('visibilitychange', onVisibilityChange);
  }, []);

  useEffect(() => {
    notificationService.updateUserTags(targetType, targetName);
  }, [targetType, targetName]);

  // Widget mode has its own stripped down layout
  if (location.pathname === '/widget') {
    return <WidgetPage realtimeState={realtimeState} />;
  }

  return (
    <div className="min-h-screen min-h-dvh flex bg-[var(--md-background)] text-[var(--md-text-primary)] transition-colors">
      {/* Side Navigation Rail for Desktop */}
      <NavigationRail currentTab={currentTab} onSelectTab={handleSelectTab} />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 pb-[calc(5rem+var(--safe-area-bottom))] lg:pb-0">
        <Header
          realtimeState={realtimeState}
          isLoading={isLoading}
          onRefresh={() => {
            const prefs = usePreferencesStore.getState();
            loadSchedule(prefs.targetType, prefs.targetName, 0);
          }}
          onOpenSettings={() => setSettingsOpen(true)}
          onOpenInfo={() => setInfoOpen(true)}
        />

        <main className="flex-1 flex flex-col">
          <Routes>
            <Route path="/" element={<SchedulePage realtimeState={realtimeState} />} />
            <Route path="/schedule" element={<SchedulePage realtimeState={realtimeState} />} />
            <Route path="/notes" element={<NotesPage />} />
            <Route path="/calendar" element={<CalendarPage />} />
            <Route path="/freerooms" element={<FreeRoomsPage realtimeState={realtimeState} />} />
            <Route path="/archive" element={<ArchivePage />} />
          </Routes>
        </main>
      </div>

      {/* Bottom Navigation for Mobile */}
      <NavigationBar currentTab={currentTab} onSelectTab={handleSelectTab} />

      {/* iOS Install Prompt Banner */}
      {showPrompt && <IOSInstallBanner onDismiss={dismiss} />}

      {/* Settings Dialog */}
      {settingsOpen && <SettingsDialog onClose={() => setSettingsOpen(false)} />}

      {/* Info Sheet */}
      {infoOpen && <InfoSheet onClose={() => setInfoOpen(false)} />}
    </div>
  );
};
