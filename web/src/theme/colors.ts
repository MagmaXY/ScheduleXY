export interface ThemeTokens {
  bg: string;
  surface: string;
  surfaceVariant: string;
  border: string;
  borderLight: string;
  headerStart: string;
  headerEnd: string;
  subject: string;
  teacher: string;
  room: string;
  group: string;
  lsnNum: string;
  lsnTime: string;
  cancel: string;
  cancelBg: string;
  cancelBorder: string;
  change: string;
  changeBg: string;
  changeBorder: string;
  activeNowBg: string;
  activeNowBorder: string;
  textPrimary: string;
  textSecondary: string;
  textMuted: string;
  gold: string;
  goldBg: string;
  onPrimary: string;
  secondary: string;
  secondaryContainer: string;
  onSecondaryContainer: string;
}

export const lightClassic: ThemeTokens = {
  bg: '#F8FAFC',
  surface: '#FFFFFF',
  surfaceVariant: '#F1F5F9',
  border: '#CBD5E1',
  borderLight: '#E2E8F0',
  headerStart: '#FFFFFF',
  headerEnd: '#F8FAFC',
  subject: '#1D4ED8',
  teacher: '#475569',
  room: '#0F172A',
  group: '#1D4ED8',
  lsnNum: '#64748B',
  lsnTime: '#059669',
  cancel: '#94A3B8', // Gray from official colors.xml
  cancelBg: '#F1F5F9',
  cancelBorder: '#E2E8F0',
  change: '#DC2626',
  changeBg: '#FEE2E2',
  changeBorder: '#FCA5A5',
  activeNowBg: 'rgba(219, 234, 254, 0.6)',
  activeNowBorder: '#1D4ED8',
  textPrimary: '#0F172A',
  textSecondary: '#334155',
  textMuted: '#64748B',
  gold: '#D97706',
  goldBg: '#FEF3C7',
  onPrimary: '#FFFFFF',
  secondary: '#006C4C',
  secondaryContainer: '#CEE9D8',
  onSecondaryContainer: '#002114',
};

export const darkClassic: ThemeTokens = {
  bg: '#0B0F17',
  surface: '#111827',
  surfaceVariant: '#1E293B',
  border: '#334155',
  borderLight: '#283548',
  headerStart: '#111827',
  headerEnd: '#0B0F17',
  subject: '#9999FF',
  teacher: '#5EEAD4',
  room: '#F8FAFC',
  group: '#8099FF',
  lsnNum: '#94A3B8',
  lsnTime: '#34D399',
  cancel: '#94A3B8', // Gray from official Theme.kt
  cancelBg: '#141C2B',
  cancelBorder: '#1E293B',
  change: '#F87171',
  changeBg: '#450A0A',
  changeBorder: '#7F1D1D',
  activeNowBg: 'rgba(30, 58, 138, 0.4)',
  activeNowBorder: '#60A5FA',
  textPrimary: '#F8FAFC',
  textSecondary: '#CBD5E1',
  textMuted: '#94A3B8',
  gold: '#FBBF24',
  goldBg: '#78350F',
  onPrimary: '#FFFFFF',
  secondary: '#28C76F',
  secondaryContainer: '#005238',
  onSecondaryContainer: '#A7F3D0',
};

export const lightEmerald: ThemeTokens = {
  ...lightClassic,
  subject: '#005A3E',
  teacher: '#1E6B52',
  lsnTime: '#006C4C',
  group: '#006C4C',
  cancel: '#64748B',
  gold: '#059669',
  goldBg: '#CEE9D8',
  activeNowBorder: '#006C4C',
  textMuted: '#4B5563',
  onPrimary: '#FFFFFF',
  secondary: '#006C4C',
  secondaryContainer: '#D1FAE5',
  onSecondaryContainer: '#065F46',
};

export const darkEmerald: ThemeTokens = {
  ...darkClassic,
  bg: '#0E1512',
  surface: '#121B17',
  surfaceVariant: '#1C2722',
  subject: '#6EDBB0',
  teacher: '#7DD3B6',
  lsnTime: '#5EEAD4',
  group: '#6EDBB0',
  cancel: '#94A3B8',
  gold: '#34D399',
  goldBg: '#16211C',
  activeNowBorder: '#6EDBB0',
  textMuted: '#94A3B8',
  onPrimary: '#FFFFFF',
  secondary: '#34D399',
  secondaryContainer: '#064E3B',
  onSecondaryContainer: '#A7F3D0',
};

export const darkOled: ThemeTokens = {
  bg: '#000000',
  surface: '#080808',
  surfaceVariant: '#141414',
  border: '#27272A',
  borderLight: '#18181B',
  headerStart: '#080808',
  headerEnd: '#000000',
  subject: '#FAFAFA',
  teacher: '#A1A1AA',
  room: '#FFFFFF',
  group: '#E4E4E7',
  lsnNum: '#71717A',
  lsnTime: '#E2E8F0',
  cancel: '#71717A',
  cancelBg: '#141414',
  cancelBorder: '#27272A',
  change: '#F87171',
  changeBg: '#27272A',
  changeBorder: '#3F3F46',
  activeNowBg: 'rgba(39, 39, 42, 0.6)',
  activeNowBorder: '#71717A',
  textPrimary: '#FAFAFA',
  textSecondary: '#D4D4D8',
  textMuted: '#A1A1AA',
  gold: '#D4D4D8',
  goldBg: '#18181B',
  onPrimary: '#000000', // Black text on white/silver primary in dark OLED
  secondary: '#A1A1AA',
  secondaryContainer: '#18181B',
  onSecondaryContainer: '#E4E4E7',
};

export const lightOled: ThemeTokens = {
  ...lightClassic,
  bg: '#FAFAFA',
  surface: '#FFFFFF',
  surfaceVariant: '#F4F4F5',
  border: '#E4E4E7',
  borderLight: '#F4F4F5',
  subject: '#18181B',
  teacher: '#3F3F46',
  room: '#09090B',
  group: '#27272A',
  lsnTime: '#18181B',
  cancel: '#71717A',
  gold: '#52525B',
  goldBg: '#E4E4E7',
  textMuted: '#52525B',
  onPrimary: '#FFFFFF',
  secondary: '#52525B',
  secondaryContainer: '#E4E4E7',
  onSecondaryContainer: '#18181B',
};
