import React, { useState, useEffect } from 'react';
import { usePreferencesStore, ThemePreset } from '../../store/preferencesStore';
import { useNotesStore } from '../../store/notesStore';
import { X, FileDown, FileUp, RefreshCw, Cloud, LogOut, Bell, BellOff } from 'lucide-react';
import { yandexSyncService, YandexUserState } from '../../services/yandexSync';
import { notificationService } from '../../services/notificationService';

interface Props {
  onClose: () => void;
}

export const SettingsDialog: React.FC<Props> = ({ onClose }) => {
  React.useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);
  const themePreset = usePreferencesStore((s) => s.themePreset);
  const setThemePreset = usePreferencesStore((s) => s.setThemePreset);
  const accentColor = usePreferencesStore((s) => s.accentColor);
  const setAccentColor = usePreferencesStore((s) => s.setAccentColor);
  const adminPassword = usePreferencesStore((s) => s.adminPassword);
  const setAdminPassword = usePreferencesStore((s) => s.setAdminPassword);
  const showPairsSchedule = usePreferencesStore((s) => s.showPairsSchedule);
  const setShowPairsSchedule = usePreferencesStore((s) => s.setShowPairsSchedule);
  const statusBarPairsMode = usePreferencesStore((s) => s.statusBarPairsMode);
  const setStatusBarPairsMode = usePreferencesStore((s) => s.setStatusBarPairsMode);
  const overlaySwipeAnimation = usePreferencesStore((s) => s.overlaySwipeAnimation);
  const setOverlaySwipeAnimation = usePreferencesStore((s) => s.setOverlaySwipeAnimation);
  const copyIncludeHomework = usePreferencesStore((s) => s.copyIncludeHomework);
  const setCopyIncludeHomework = usePreferencesStore((s) => s.setCopyIncludeHomework);
  const copyIncludeNotes = usePreferencesStore((s) => s.copyIncludeNotes);
  const setCopyIncludeNotes = usePreferencesStore((s) => s.setCopyIncludeNotes);

  const favoriteClass = usePreferencesStore((s) => s.favoriteClass);
  const targetName = usePreferencesStore((s) => s.targetName);
  const targetType = usePreferencesStore((s) => s.targetType);

  const studentSubgroup = usePreferencesStore((s) => s.studentSubgroup);
  const setStudentSubgroup = usePreferencesStore((s) => s.setStudentSubgroup);
  const studentTechnologySubgroup = usePreferencesStore(
    (s) => s.studentTechnologySubgroup
  );
  const setStudentTechnologySubgroup = usePreferencesStore(
    (s) => s.setStudentTechnologySubgroup
  );

  const useMagmaLogo = usePreferencesStore((s) => s.useMagmaLogo);
  const setUseMagmaLogo = usePreferencesStore((s) => s.setUseMagmaLogo);
  const isMagmaUnlocked = usePreferencesStore((s) => s.isMagmaUnlocked);
  const corsProxy = usePreferencesStore((s) => s.corsProxy);
  const setCorsProxy = usePreferencesStore((s) => s.setCorsProxy);

  const exportNotesJson = useNotesStore((s) => s.exportNotesJson);
  const importNotesJson = useNotesStore((s) => s.importNotesJson);

  const [proxyInput, setProxyInput] = useState(corsProxy);
  const [passwordInput, setPasswordInput] = useState(adminPassword);

  const [yandexState, setYandexState] = useState<YandexUserState>(() =>
    yandexSyncService.getState()
  );
  const [notificationPermission, setNotificationPermission] = useState<NotificationPermission>(() =>
    notificationService.getPermissionStatus()
  );

  const handleToggleNotifications = async () => {
    if (notificationPermission === 'granted') {
      alert('Уведомления уже включены. Чтобы отключить их, измените разрешение в настройках браузера или сайта.');
      return;
    }
    const granted = await notificationService.requestPermission();
    setNotificationPermission(notificationService.getPermissionStatus());
    if (granted) {
      notificationService.updateUserTags(targetType, targetName);
    }
  };

  useEffect(() => {
    return yandexSyncService.subscribe(() => {
      setYandexState(yandexSyncService.getState());
    });
  }, []);

  // Group selector is shown ONLY if a class is selected as favorite (or active target is class)
  const activeClass = favoriteClass || (targetType === 'class' ? targetName : null);

  // Middle school check (classes 5–8): technology subgroup is only applicable for 5-8 grades
  const isMiddleSchool = activeClass
    ? /^[5-8][а-яА-Яa-zA-Z0-9\-_]*/.test(activeClass.trim())
    : false;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-[var(--md-surface)] border border-[var(--md-border)] rounded-3xl p-5 max-w-lg w-full max-h-[90vh] overflow-y-auto shadow-2xl relative">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-[var(--md-border-light)] mb-4">
          <h3 className="font-extrabold text-base text-[var(--md-text-primary)]">
            Настройки
          </h3>
          <button
            onClick={onClose}
            className="p-1.5 text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)] rounded-full hover:bg-[var(--md-surface-variant)]"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-4 text-xs sm:text-sm">
          {/* Section: Themes */}
          <div>
            <h4 className="font-bold text-xs text-[var(--md-text-muted)] mb-2">
              Тема оформления
            </h4>

            {/* Preset Selector */}
            <div className="grid grid-cols-2 gap-2 mb-2">
              {[
                { id: 'classic', label: 'Лицей', color: '#003882' },
                { id: 'emerald', label: 'Шишки', color: '#006C4C' },
                { id: 'dynamic', label: 'Material You', color: '#6750A4' },
                { id: 'oled', label: 'Амолед', color: '#000000' },
              ].map((p) => (
                <button
                  key={p.id}
                  onClick={() => setThemePreset(p.id as ThemePreset)}
                  className={`p-2.5 rounded-xl border text-left font-semibold flex items-center gap-2 transition-all ${
                    themePreset === p.id
                      ? 'bg-[var(--md-surface-variant)] border-[var(--nika-subject)] text-[var(--nika-subject)] ring-1 ring-[var(--nika-subject)]'
                      : 'border-[var(--md-border-light)] text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
                  }`}
                >
                  <span
                    className="w-4 h-4 rounded-full border border-black/20 shrink-0"
                    style={{ backgroundColor: p.color }}
                  />
                  <span>{p.label}</span>
                </button>
              ))}
            </div>

            {/* Accent Color picker for Material You (dynamic theme) */}
            {themePreset === 'dynamic' && (
              <div className="p-3 rounded-2xl bg-[var(--md-surface-variant)]/50 space-y-2 mt-2">
                <span className="font-semibold text-[var(--md-text-primary)] block">
                  Акцентный цвет (Material You)
                </span>
                <div className="flex items-center gap-2">
                  <input
                    type="color"
                    value={accentColor || '#6750A4'}
                    onChange={(e) => setAccentColor(e.target.value)}
                    className="w-9 h-9 rounded-xl border border-[var(--md-border)] cursor-pointer bg-transparent"
                  />
                  <div className="flex flex-wrap gap-1.5">
                    {['#6750A4', '#006A6A', '#00639B', '#8F4C38', '#386A20', '#A03F5C'].map((hex) => (
                      <button
                        key={hex}
                        type="button"
                        onClick={() => setAccentColor(hex)}
                        className={`w-7 h-7 rounded-full border ${
                          accentColor === hex ? 'ring-2 ring-[var(--nika-subject)] scale-110' : 'border-black/20'
                        }`}
                        style={{ backgroundColor: hex }}
                      />
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Section: App Branding / Logo Toggle (when Magma unlocked) */}
          {isMagmaUnlocked && (
            <div className="pt-2 border-t border-[var(--md-border-light)]">
              <h4 className="font-bold text-xs text-[var(--md-text-muted)] mb-2">
                Логотип приложения
              </h4>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => setUseMagmaLogo(false)}
                  className={`p-2.5 rounded-xl border flex items-center gap-2 font-medium transition-all ${
                    !useMagmaLogo
                      ? 'bg-[var(--md-surface-variant)] border-[var(--nika-subject)] text-[var(--nika-subject)] ring-1 ring-[var(--nika-subject)] font-bold'
                      : 'border-[var(--md-border-light)] text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
                  }`}
                >
                  <img src="ic_lyceum_logo.png" alt="Лицей" className="w-6 h-6 object-contain" />
                  <span>Лицей</span>
                </button>
                <button
                  onClick={() => setUseMagmaLogo(true)}
                  className={`p-2.5 rounded-xl border flex items-center gap-2 font-medium transition-all ${
                    useMagmaLogo
                      ? 'bg-[var(--md-surface-variant)] border-[var(--nika-subject)] text-[var(--nika-subject)] ring-1 ring-[var(--nika-subject)] font-bold'
                      : 'border-[var(--md-border-light)] text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/40'
                  }`}
                >
                  <img src="ic_magma_logo.png" alt="MagmaXY" className="w-6 h-6 object-contain" />
                  <span>MagmaXY</span>
                </button>
              </div>
            </div>
          )}

          {/* Section: Pairs Schedule switches (no "Режим пар" header) */}
          <div className="pt-2 border-t border-[var(--md-border-light)] space-y-2">
            <div className="flex items-center justify-between p-3 rounded-2xl bg-[var(--md-surface-variant)]/50">
              <span className="font-semibold text-[var(--md-text-primary)]">
                Отображать расписание парами
              </span>
              <input
                type="checkbox"
                checked={showPairsSchedule}
                onChange={(e) => setShowPairsSchedule(e.target.checked)}
                className="w-5 h-5 rounded text-[var(--nika-subject)] cursor-pointer"
              />
            </div>

            <div className="flex items-center justify-between p-3 rounded-2xl bg-[var(--md-surface-variant)]/50">
              <span className="font-semibold text-[var(--md-text-primary)]">
                Статус-бар парами
              </span>
              <input
                type="checkbox"
                checked={statusBarPairsMode}
                onChange={(e) => setStatusBarPairsMode(e.target.checked)}
                className="w-5 h-5 rounded text-[var(--nika-subject)] cursor-pointer"
              />
            </div>

            <div className="flex items-center justify-between p-3 rounded-2xl bg-[var(--md-surface-variant)]/50">
              <span className="font-semibold text-[var(--md-text-primary)]">
                Анимация свайпа
              </span>
              <input
                type="checkbox"
                checked={overlaySwipeAnimation}
                onChange={(e) => setOverlaySwipeAnimation(e.target.checked)}
                className="w-5 h-5 rounded text-[var(--nika-subject)] cursor-pointer"
              />
            </div>
          </div>

          {/* Section: Copying Options (no "Копирование расписания" header) */}
          <div className="pt-2 border-t border-[var(--md-border-light)] space-y-2">
            <div className="flex items-center justify-between p-3 rounded-2xl bg-[var(--md-surface-variant)]/50">
              <span className="font-semibold text-[var(--md-text-primary)]">
                Включать ДЗ при копировании
              </span>
              <input
                type="checkbox"
                checked={copyIncludeHomework}
                onChange={(e) => setCopyIncludeHomework(e.target.checked)}
                className="w-5 h-5 rounded text-[var(--nika-subject)] cursor-pointer"
              />
            </div>

            <div className="flex items-center justify-between p-3 rounded-2xl bg-[var(--md-surface-variant)]/50">
              <span className="font-semibold text-[var(--md-text-primary)]">
                Включать заметки при копировании
              </span>
              <input
                type="checkbox"
                checked={copyIncludeNotes}
                onChange={(e) => setCopyIncludeNotes(e.target.checked)}
                className="w-5 h-5 rounded text-[var(--nika-subject)] cursor-pointer"
              />
            </div>
          </div>

          {/* Section: Groups (no "Подгруппы (класс)" header, direct row labels) */}
          {activeClass && (
            <div className="pt-2 border-t border-[var(--md-border-light)]">
              <div className="p-3 rounded-2xl bg-[var(--md-surface-variant)]/50 space-y-3">
                {/* Standard subgroup 1 / 2 */}
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-[var(--md-text-primary)]">
                    Моя подгруппа ({activeClass})
                  </span>
                  <div className="flex gap-1.5">
                    {[1, 2].map((g) => (
                      <button
                        key={g}
                        onClick={() => setStudentSubgroup(g)}
                        className={`px-3 py-1 rounded-lg text-xs font-bold transition-all ${
                          studentSubgroup === g
                            ? 'bg-[var(--nika-subject)] text-[var(--md-on-primary)] shadow-sm'
                            : 'bg-[var(--md-surface)] border border-[var(--md-border-light)] text-[var(--md-text-secondary)]'
                        }`}
                      >
                        {g} гр
                      </button>
                    ))}
                  </div>
                </div>

                {/* Technology subgroup (only for 5-8 grades) */}
                {isMiddleSchool && (
                  <div className="flex items-center justify-between pt-2 border-t border-[var(--md-border-light)]/40">
                    <span className="font-semibold text-[var(--md-text-primary)]">
                      Подгруппа по технологии ({activeClass})
                    </span>
                    <div className="flex gap-1.5">
                      {[1, 2, 3].map((g) => (
                        <button
                          key={g}
                          onClick={() => setStudentTechnologySubgroup(g)}
                          className={`px-3 py-1 rounded-lg text-xs font-bold transition-all ${
                            studentTechnologySubgroup === g
                              ? 'bg-[var(--nika-subject)] text-[var(--md-on-primary)] shadow-sm'
                              : 'bg-[var(--md-surface)] border border-[var(--md-border-light)] text-[var(--md-text-secondary)]'
                          }`}
                        >
                          {g}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Section: Admin Password (shown if secret mode / isMagmaUnlocked is active) */}
          {isMagmaUnlocked && (
            <div className="pt-2 border-t border-[var(--md-border-light)]">
              <h4 className="font-bold text-xs text-[var(--md-text-muted)] mb-2">
                Пароль администратора
              </h4>
              <div className="p-3 rounded-2xl bg-[var(--md-surface-variant)]/50">
                <div className="flex gap-2">
                  <input
                    type="password"
                    value={passwordInput}
                    onChange={(e) => setPasswordInput(e.target.value)}
                    placeholder="Введите пароль"
                    className="flex-1 px-3 py-1.5 bg-[var(--md-surface)] border border-[var(--md-border)] rounded-xl text-xs outline-none text-[var(--md-text-primary)]"
                  />
                  <button
                    onClick={() => {
                      setAdminPassword(passwordInput);
                      alert(passwordInput === 'licey2026' ? 'Доступ администратора открыт' : 'Пароль сохранён');
                    }}
                    className="px-3 py-1.5 bg-[var(--nika-subject)] text-[var(--md-on-primary)] text-xs font-bold rounded-xl"
                  >
                    ОК
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Section: Yandex Disk Sync */}
          <div className="pt-2 border-t border-[var(--md-border-light)]">
            <h4 className="font-bold text-xs text-[var(--md-text-muted)] mb-2 flex items-center justify-between">
              <span>Синхронизация заметок и ДЗ</span>
              <span className="text-[10px] text-red-500 font-semibold">Яндекс Диск</span>
            </h4>
            <div className="p-3 rounded-2xl bg-[var(--md-surface-variant)]/50 space-y-2.5">
              {yandexState.token ? (
                <div>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <div className="w-7 h-7 rounded-full bg-red-500/10 flex items-center justify-center text-red-500 font-bold text-xs">
                        Я
                      </div>
                      <div>
                        <div className="text-xs font-bold text-[var(--md-text-primary)]">
                          {yandexState.username || 'Яндекс Аккаунт'}
                        </div>
                        <div className="text-[10.5px] text-[var(--md-text-muted)]">
                          {yandexState.lastSyncTime
                            ? `Синхр.: ${new Date(yandexState.lastSyncTime).toLocaleTimeString([], {
                                hour: '2-digit',
                                minute: '2-digit',
                              })}`
                            : 'Не синхронизировано'}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <button
                        type="button"
                        onClick={async () => {
                          const res = await yandexSyncService.sync();
                          if (!res.success && res.message) {
                            alert(res.message);
                          }
                        }}
                        disabled={yandexState.isSyncing}
                        className="p-1.5 rounded-lg bg-[var(--md-surface)] border border-[var(--md-border)] text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)] disabled:opacity-50"
                        title="Синхронизировать сейчас"
                      >
                        <RefreshCw
                          className={`w-4 h-4 ${yandexState.isSyncing ? 'animate-spin' : ''}`}
                        />
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          yandexSyncService.logout();
                        }}
                        className="p-1.5 rounded-lg bg-[var(--md-surface)] border border-[var(--md-border)] text-red-500 hover:bg-red-500/10 cursor-pointer"
                        title="Выйти"
                      >
                        <LogOut className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                  <div className="mt-1 text-[11px] text-[var(--md-text-muted)] leading-tight">
                    Файл хранится в папке приложения на Яндекс Диске и автоматически синхронизируется между ПК и телефоном.
                  </div>
                </div>
              ) : (
                <div>
                  <div className="text-[12px] text-[var(--md-text-secondary)] mb-2.5">
                    Подключите Яндекс, чтобы ваши заметки и ДЗ автоматически появлялись на всех устройствах (компьютере, телефоне и в приложении).
                  </div>
                  <button
                    type="button"
                    onClick={() => yandexSyncService.startLogin()}
                    className="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs shadow-xs transition-colors"
                  >
                    <span className="font-extrabold text-[13px]">Я</span>
                    <span>Войти через Яндекс ID</span>
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* Section: Notes Backup & Restore */}
          <div className="pt-2 border-t border-[var(--md-border-light)]">
            <h4 className="font-bold text-xs text-[var(--md-text-muted)] mb-2">
              Резервное копирование заметок
            </h4>
            <div className="p-3 rounded-2xl bg-[var(--md-surface-variant)]/50">
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => {
                    const todayIso = new Date().toISOString().split('T')[0];
                    const json = exportNotesJson();
                    const blob = new Blob([json], { type: 'application/json' });
                    const url = URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = `schedulexy-notes-${todayIso}.json`;
                    a.click();
                    URL.revokeObjectURL(url);
                  }}
                  className="flex-1 flex items-center justify-center gap-1.5 px-3 py-2 bg-[var(--md-surface)] border border-[var(--md-border)] rounded-xl text-xs font-bold text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]"
                >
                  <FileDown className="w-3.5 h-3.5" />
                  <span>Экспорт</span>
                </button>
                <label className="flex-1 flex items-center justify-center gap-1.5 px-3 py-2 bg-[var(--md-surface)] border border-[var(--md-border)] rounded-xl text-xs font-bold text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)] cursor-pointer">
                  <FileUp className="w-3.5 h-3.5" />
                  <span>Импорт</span>
                  <input
                    type="file"
                    accept=".json"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (!file) return;
                      const reader = new FileReader();
                      reader.onload = async (event) => {
                        const text = event.target?.result as string;
                        const count = await importNotesJson(text);
                        if (count >= 0) {
                          alert(`Успешно импортировано записей: ${count}`);
                        } else {
                          alert('Ошибка формата файла');
                        }
                      };
                      reader.readAsText(file);
                    }}
                    className="hidden"
                  />
                </label>
              </div>
            </div>
          </div>

          {/* Section: Notifications */}
          <div className="pt-2 border-t border-[var(--md-border-light)]">
            <h4 className="font-bold text-xs text-[var(--md-text-muted)] mb-2 flex items-center gap-1.5">
              <Bell className="w-3.5 h-3.5" />
              <span>Push-уведомления</span>
            </h4>
            <div className="p-3 rounded-2xl bg-[var(--md-surface-variant)]/50 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs text-[var(--md-text-primary)]">
                  Уведомления об изменениях расписания
                </span>
                <button
                  type="button"
                  onClick={handleToggleNotifications}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 ${
                    notificationPermission === 'granted'
                      ? 'bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30'
                      : 'bg-[var(--nika-subject)] text-[var(--md-on-primary)] shadow-sm'
                  }`}
                >
                  {notificationPermission === 'granted' ? (
                    <>
                      <Bell className="w-3 h-3" />
                      <span>Включены</span>
                    </>
                  ) : (
                    <>
                      <BellOff className="w-3 h-3" />
                      <span>Включить</span>
                    </>
                  )}
                </button>
              </div>
              <p className="text-[10px] text-[var(--md-text-muted)] leading-relaxed">
                Приходят при публикации нового расписания или замен для вашего класса / преподавателя ({targetName || 'не выбран'}).
              </p>
            </div>
          </div>

          {/* Section: Network / CORS Proxy */}
          <div className="pt-2 border-t border-[var(--md-border-light)]">
            <h4 className="font-bold text-xs text-[var(--md-text-muted)] mb-2">
              CORS прокси для API
            </h4>
            <div className="p-3 rounded-2xl bg-[var(--md-surface-variant)]/50">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={proxyInput}
                  onChange={(e) => setProxyInput(e.target.value)}
                  placeholder="https://lyceum-proxy.dimafilippov1800.workers.dev"
                  className="flex-1 px-3 py-1.5 bg-[var(--md-surface)] border border-[var(--md-border)] rounded-xl text-xs outline-none text-[var(--md-text-primary)]"
                />
                <button
                  onClick={() => setCorsProxy(proxyInput)}
                  className="px-3 py-1.5 bg-[var(--nika-subject)] text-[var(--md-on-primary)] text-xs font-bold rounded-xl"
                >
                  ОК
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
