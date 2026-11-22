import React, { useEffect, useMemo } from 'react';
import { usePreferencesStore } from '../store/preferencesStore';
import {
  ThemeTokens,
  darkClassic,
  darkEmerald,
  darkOled,
  lightClassic,
  lightEmerald,
  lightOled,
} from './colors';
import {
  argbFromHex,
  themeFromSourceColor,
  hexFromArgb,
} from '@material/material-color-utilities';

export const ThemeContext = React.createContext<{ tokens: ThemeTokens }>({
  tokens: lightClassic,
});

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const isDarkMode = usePreferencesStore((s) => s.isDarkMode);
  const themePreset = usePreferencesStore((s) => s.themePreset);
  const accentColor = usePreferencesStore((s) => s.accentColor);

  const tokens = useMemo<ThemeTokens>(() => {
    if (themePreset === 'emerald') {
      return isDarkMode ? darkEmerald : lightEmerald;
    }
    if (themePreset === 'oled') {
      return isDarkMode ? darkOled : lightOled;
    }
    if (themePreset === 'dynamic') {
      try {
        const sourceArgb = argbFromHex(accentColor || '#2563EB');
        const theme = themeFromSourceColor(sourceArgb);
        const scheme = isDarkMode ? theme.schemes.dark : theme.schemes.light;

        const base = isDarkMode ? darkClassic : lightClassic;
        return {
          ...base,
          bg: hexFromArgb(scheme.background),
          surface: hexFromArgb(scheme.surface),
          surfaceVariant: hexFromArgb(scheme.surfaceVariant),
          border: hexFromArgb(scheme.outline),
          borderLight: hexFromArgb(scheme.outlineVariant),
          subject: hexFromArgb(scheme.primary),
          teacher: hexFromArgb(scheme.secondary),
          group: hexFromArgb(scheme.tertiary),
          gold: hexFromArgb(scheme.primary),
          textPrimary: hexFromArgb(scheme.onSurface),
          textSecondary: hexFromArgb(scheme.onSurfaceVariant),
          textMuted: isDarkMode ? '#94A3B8' : '#64748B',
          cancel: isDarkMode ? '#94A3B8' : '#64748B',
          onPrimary: hexFromArgb(scheme.onPrimary),
          secondary: hexFromArgb(scheme.secondary),
          secondaryContainer: hexFromArgb(scheme.secondaryContainer),
          onSecondaryContainer: hexFromArgb(scheme.onSecondaryContainer),
        };
      } catch {
        return isDarkMode ? darkClassic : lightClassic;
      }
    }
    return isDarkMode ? darkClassic : lightClassic;
  }, [isDarkMode, themePreset, accentColor]);

  useEffect(() => {
    const root = document.documentElement;
    root.classList.toggle('dark', isDarkMode);
    root.setAttribute(
      'data-theme',
      `${themePreset}-${isDarkMode ? 'dark' : 'light'}`
    );

    // Inject CSS variables
    root.style.setProperty('--md-background', tokens.bg);
    root.style.setProperty('--md-surface', tokens.surface);
    root.style.setProperty('--md-surface-variant', tokens.surfaceVariant);
    root.style.setProperty('--md-border', tokens.border);
    root.style.setProperty('--md-border-light', tokens.borderLight);
    root.style.setProperty('--md-text-primary', tokens.textPrimary);
    root.style.setProperty('--md-text-secondary', tokens.textSecondary);
    root.style.setProperty('--md-text-muted', tokens.textMuted);

    root.style.setProperty('--md-primary', tokens.subject);
    root.style.setProperty('--md-on-primary', tokens.onPrimary);
    root.style.setProperty('--md-secondary', tokens.secondary);
    root.style.setProperty('--md-secondary-container', tokens.secondaryContainer);
    root.style.setProperty('--md-on-secondary-container', tokens.onSecondaryContainer);

    root.style.setProperty('--nika-subject', tokens.subject);
    root.style.setProperty('--nika-teacher', tokens.teacher);
    root.style.setProperty('--nika-room', tokens.room);
    root.style.setProperty('--nika-group', tokens.group);
    root.style.setProperty('--nika-lsn-num', tokens.lsnNum);
    root.style.setProperty('--nika-lsn-time', tokens.lsnTime);
    root.style.setProperty('--nika-cancel', tokens.cancel);
    root.style.setProperty('--nika-cancel-bg', tokens.cancelBg);
    root.style.setProperty('--nika-cancel-border', tokens.cancelBorder);
    root.style.setProperty('--nika-change', tokens.change);
    root.style.setProperty('--nika-change-bg', tokens.changeBg);
    root.style.setProperty('--nika-change-border', tokens.changeBorder);
    root.style.setProperty('--nika-active-bg', tokens.activeNowBg);
    root.style.setProperty('--nika-active-border', tokens.activeNowBorder);
    root.style.setProperty('--nika-gold', tokens.gold);
    root.style.setProperty('--nika-gold-bg', tokens.goldBg);

    // Update theme-color meta tag
    const metaThemeColor = document.querySelector('meta[name="theme-color"]');
    if (metaThemeColor) {
      metaThemeColor.setAttribute('content', tokens.surface);
    }
  }, [tokens, isDarkMode, themePreset]);

  return (
    <ThemeContext.Provider value={{ tokens }}>{children}</ThemeContext.Provider>
  );
};
