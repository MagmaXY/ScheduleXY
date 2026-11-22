import React, { useState, useEffect } from 'react';
import {
  X,
  Copy,
  Check,
  Code,
} from 'lucide-react';
import developerAvatarImg from '../../assets/developer_avatar.jpg';
import sysadminAvatarImg from '../../assets/sysadmin_avatar.jpg';
import {
  LyceumLogoIcon,
  SdoLogoIcon,
  NikaSoftLogoIcon,
  DashboardLogoIcon,
  NsoCoatOfArmsIcon,
  VkTwoToneIcon,
  TelegramTwoToneIcon,
  MaxTwoToneIcon,
  YoutubeTwoToneIcon,
} from './ServiceIcons';

interface Props {
  onClose: () => void;
}

export const InfoSheet: React.FC<Props> = ({ onClose }) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const [copiedLink, setCopiedLink] = useState<string | null>(null);

  const handleCopy = (label: string, text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedLink(label);
    setTimeout(() => setCopiedLink(null), 2000);
  };

  const lyceumServices = [
    {
      title: 'Основной сайт лицея',
      subtitle: 'lyceum.nstu.ru',
      url: 'https://lyceum.nstu.ru/',
      icon: <LyceumLogoIcon className="w-5 h-5" />,
    },
    {
      title: 'СДО Лицея',
      subtitle: 'lyceum.nstu.ru/sdo',
      url: 'https://lyceum.nstu.ru/sdo',
      icon: <SdoLogoIcon className="w-5 h-5" />,
    },
    {
      title: 'Веб-расписание',
      subtitle: 'lyceum.nstu.ru/rasp/schedule.html',
      url: 'https://lyceum.nstu.ru/rasp/schedule.html',
      icon: <NikaSoftLogoIcon className="w-5 h-5" />,
    },
    {
      title: 'Информационная панель',
      subtitle: 'lyceum.nstu.ru/info',
      url: 'https://lyceum.nstu.ru/info',
      icon: <DashboardLogoIcon className="w-5 h-5" />,
    },
    {
      title: 'Дневник НСО',
      subtitle: 'school.nso.ru/journal-app',
      url: 'https://school.nso.ru/journal-app',
      icon: <NsoCoatOfArmsIcon className="w-5 h-5" />,
    },
  ];

  const socialLinks = [
    {
      title: 'ВКонтакте',
      subtitle: 'vk.com/il_nstu',
      url: 'https://vk.com/il_nstu',
      icon: <VkTwoToneIcon className="w-5 h-5 text-[var(--nika-subject)] shrink-0" />,
    },
    {
      title: 'Telegram',
      subtitle: 't.me/il_ngtu',
      url: 'https://t.me/il_ngtu',
      icon: <TelegramTwoToneIcon className="w-5 h-5 text-[var(--nika-subject)] shrink-0" />,
    },
    {
      title: 'МАХ',
      subtitle: 'max.ru/id5404140563_gos',
      url: 'https://max.ru/id5404140563_gos',
      icon: <MaxTwoToneIcon className="w-5 h-5 text-[var(--nika-subject)] shrink-0" />,
    },
    {
      title: 'YouTube (архивный)',
      subtitle: 'youtube.com/c/ИнженерныйлицейНГТУ',
      url: 'https://www.youtube.com/c/ИнженерныйлицейНГТУ',
      icon: <YoutubeTwoToneIcon className="w-5 h-5 text-[var(--nika-subject)] shrink-0" />,
    },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-[var(--md-surface)] border border-[var(--md-border)] rounded-t-3xl sm:rounded-3xl p-5 max-w-lg w-full shadow-2xl relative max-h-[88vh] overflow-y-auto">
        <div className="flex items-center justify-between pb-3 border-b border-[var(--md-border-light)] mb-4">
          <h3 className="font-extrabold text-base text-[var(--md-text-primary)]">
            О приложении и сервисы
          </h3>
          <button
            onClick={onClose}
            className="text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)] p-1 rounded-full hover:bg-[var(--md-surface-variant)]"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-4 text-xs sm:text-sm">
          {/* 1. Developer Section */}
          <div>
            <h4 className="font-semibold text-xs text-[var(--nika-subject)] mb-2">
              Разработчик
            </h4>
            <div className="p-3 bg-[var(--md-surface-variant)]/50 rounded-2xl border border-[var(--md-border-light)] flex flex-col gap-3">
              <div className="flex items-center gap-3">
                <img
                  src={developerAvatarImg}
                  alt="Дима Филиппов"
                  className="w-11 h-11 rounded-full object-cover border border-[var(--md-border-light)]"
                  onError={(e) => {
                    (e.target as HTMLImageElement).src = 'ic_magma_logo.png';
                  }}
                />
                <div>
                  <h5 className="font-bold text-sm text-[var(--md-text-primary)]">
                    Дима Филиппов
                  </h5>
                  <p className="text-[11.5px] font-semibold text-[var(--nika-subject)]">
                    Л10-3 (2026–2027 уч. год)
                  </p>
                </div>
              </div>

              {/* GitHub Link (supports Left Click, СКМ Middle Click, Ctrl+Click) */}
              <div className="flex items-center justify-between px-3 py-2 bg-[var(--md-surface)] rounded-xl border border-[var(--md-border-light)]">
                <a
                  href="https://github.com/MagmaXY/ScheduleXY"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2 text-[var(--md-text-primary)] hover:text-[var(--nika-subject)] transition-colors"
                >
                  <Code className="w-4 h-4 text-[var(--nika-subject)] shrink-0" />
                  <span className="font-semibold text-xs">GitHub: MagmaXY/ScheduleXY</span>
                </a>
                <button
                  type="button"
                  onClick={() => handleCopy('github', 'https://github.com/MagmaXY/ScheduleXY')}
                  className="p-1 text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)]"
                  title="Скопировать ссылку"
                >
                  {copiedLink === 'github' ? (
                    <Check className="w-3.5 h-3.5 text-green-500" />
                  ) : (
                    <Copy className="w-3.5 h-3.5" />
                  )}
                </button>
              </div>
            </div>
          </div>

          {/* 2. System Administrator Section */}
          <div>
            <h4 className="font-semibold text-xs text-[var(--nika-subject)] mb-2">
              Системный администратор
            </h4>
            <div className="p-3 bg-[var(--md-surface-variant)]/50 rounded-2xl border border-[var(--md-border-light)] flex flex-col gap-3">
              <div className="flex items-center gap-3">
                <img
                  src={sysadminAvatarImg}
                  alt="Муль Павел Фридрихович"
                  className="w-11 h-11 rounded-full object-cover border border-[var(--md-border-light)]"
                  onError={(e) => {
                    (e.target as HTMLImageElement).src = 'ic_lyceum_logo.png';
                  }}
                />
                <div>
                  <h5 className="font-bold text-sm text-[var(--md-text-primary)]">
                    Муль Павел Фридрихович
                  </h5>
                  <p className="text-[11.5px] font-semibold text-[var(--nika-subject)]">
                    Учитель информатики
                  </p>
                </div>
              </div>

              {/* Sysadmin GitHub Link (supports Left Click, СКМ Middle Click, Ctrl+Click) */}
              <div className="flex items-center justify-between px-3 py-2 bg-[var(--md-surface)] rounded-xl border border-[var(--md-border-light)]">
                <a
                  href="https://github.com/paulmy"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2 text-[var(--md-text-primary)] hover:text-[var(--nika-subject)] transition-colors"
                >
                  <Code className="w-4 h-4 text-[var(--nika-subject)] shrink-0" />
                  <span className="font-semibold text-xs">GitHub: paulmy</span>
                </a>
                <button
                  type="button"
                  onClick={() => handleCopy('paulmy', 'https://github.com/paulmy')}
                  className="p-1 text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)]"
                  title="Скопировать ссылку"
                >
                  {copiedLink === 'paulmy' ? (
                    <Check className="w-3.5 h-3.5 text-green-500" />
                  ) : (
                    <Copy className="w-3.5 h-3.5" />
                  )}
                </button>
              </div>
            </div>
          </div>

          {/* 3. Lyceum Services Section (Native 2-tone vector icons from ServiceIcons.kt) */}
          <div>
            <h4 className="font-semibold text-xs text-[var(--nika-subject)] mb-2">
              Сервисы лицея
            </h4>
            <div className="space-y-1.5">
              {lyceumServices.map((item, idx) => (
                <div
                  key={idx}
                  className="flex items-center justify-between p-2.5 bg-[var(--md-surface-variant)]/40 rounded-xl border border-[var(--md-border-light)] hover:bg-[var(--md-surface-variant)] transition-all group"
                >
                  <a
                    href={item.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-2.5 flex-1 min-w-0"
                  >
                    <div className="w-8 h-8 rounded-xl bg-[var(--md-surface-variant)] flex items-center justify-center border border-[var(--md-border-light)] shrink-0">
                      {item.icon}
                    </div>
                    <div className="min-w-0">
                      <div className="font-bold text-xs text-[var(--md-text-primary)] group-hover:text-[var(--nika-subject)] transition-colors truncate">
                        {item.title}
                      </div>
                      <div className="text-[10px] text-[var(--md-text-muted)] truncate">
                        {item.subtitle}
                      </div>
                    </div>
                  </a>
                  <button
                    type="button"
                    onClick={() => handleCopy(item.title, item.url)}
                    className="p-1 text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)] shrink-0"
                    title="Скопировать ссылку"
                  >
                    {copiedLink === item.title ? (
                      <Check className="w-3.5 h-3.5 text-green-500" />
                    ) : (
                      <Copy className="w-3.5 h-3.5" />
                    )}
                  </button>
                </div>
              ))}
            </div>
          </div>

          {/* 4. Social Media Section with inverted 2-tone themed vector icons */}
          <div className="pb-3">
            <h4 className="font-semibold text-xs text-[var(--nika-subject)] mb-2">
              Социальные сети
            </h4>
            <div className="space-y-1.5">
              {socialLinks.map((item, idx) => (
                <div
                  key={idx}
                  className="flex items-center justify-between p-2.5 bg-[var(--md-surface-variant)]/40 rounded-xl border border-[var(--md-border-light)] hover:bg-[var(--md-surface-variant)] transition-all group"
                >
                  <a
                    href={item.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-2.5 flex-1 min-w-0"
                  >
                    <div className="w-8 h-8 rounded-xl bg-[var(--md-surface-variant)] flex items-center justify-center border border-[var(--md-border-light)] shrink-0">
                      {item.icon}
                    </div>
                    <div className="min-w-0">
                      <div className="font-bold text-xs text-[var(--md-text-primary)] group-hover:text-[var(--nika-subject)] transition-colors truncate">
                        {item.title}
                      </div>
                      <div className="text-[10px] text-[var(--md-text-muted)] truncate">
                        {item.subtitle}
                      </div>
                    </div>
                  </a>
                  <button
                    type="button"
                    onClick={() => handleCopy(item.title, item.url)}
                    className="p-1 text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)] shrink-0"
                    title="Скопировать ссылку"
                  >
                    {copiedLink === item.title ? (
                      <Check className="w-3.5 h-3.5 text-green-500" />
                    ) : (
                      <Copy className="w-3.5 h-3.5" />
                    )}
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
