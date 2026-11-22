import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type ThemePreset = 'classic' | 'emerald' | 'dynamic' | 'oled';
export type TargetType = 'class' | 'teacher';

interface PreferencesState {
  targetType: TargetType;
  targetName: string;
  favoriteClass: string | null;
  favoriteTeacher: string | null;
  adminPassword: string;
  isDarkMode: boolean;
  themePreset: ThemePreset;
  accentColor: string; // for dynamic theme (hex)
  showPairsSchedule: boolean;
  statusBarPairsMode: boolean;
  overlaySwipeAnimation: boolean;
  studentSubgroup: number;
  studentTechnologySubgroup: number;
  copyIncludeHomework: boolean;
  copyIncludeNotes: boolean;
  useMagmaLogo: boolean;
  isMagmaUnlocked: boolean;
  corsProxy: string;
  notificationsEnabled: boolean;

  // Actions
  setTargetType: (type: TargetType) => void;
  setTargetName: (name: string) => void;
  setFavoriteClass: (className: string | null) => void;
  setFavoriteTeacher: (teacherName: string | null) => void;
  toggleFavorite: (type: TargetType, name: string) => void;
  isFavorite: (type: TargetType, name: string) => boolean;
  setAdminPassword: (password: string) => void;
  setIsDarkMode: (isDark: boolean) => void;
  toggleDarkMode: () => void;
  setThemePreset: (preset: ThemePreset) => void;
  setAccentColor: (color: string) => void;
  setShowPairsSchedule: (val: boolean) => void;
  setStatusBarPairsMode: (val: boolean) => void;
  setOverlaySwipeAnimation: (val: boolean) => void;
  setStudentSubgroup: (val: number) => void;
  setStudentTechnologySubgroup: (val: number) => void;
  setCopyIncludeHomework: (val: boolean) => void;
  setCopyIncludeNotes: (val: boolean) => void;
  setUseMagmaLogo: (val: boolean) => void;
  setIsMagmaUnlocked: (val: boolean) => void;
  setCorsProxy: (proxy: string) => void;
  setNotificationsEnabled: (val: boolean) => void;
  isMiddleSchoolClass: (name?: string | null) => boolean;
}

export const usePreferencesStore = create<PreferencesState>()(
  persist(
    (set, get) => ({
      targetType: 'class',
      targetName: '10-1',
      favoriteClass: '10-1',
      favoriteTeacher: null,
      adminPassword: '',
      isDarkMode: false,
      themePreset: 'classic',
      accentColor: '#2563EB',
      showPairsSchedule: false,
      statusBarPairsMode: false,
      overlaySwipeAnimation: false,
      studentSubgroup: 1,
      studentTechnologySubgroup: 1,
      copyIncludeHomework: false,
      copyIncludeNotes: false,
      useMagmaLogo: false,
      isMagmaUnlocked: false,
      corsProxy: 'https://corsproxy.io/?',
      notificationsEnabled: true,

      setTargetType: (type) => set({ targetType: type }),
      setTargetName: (name) => set({ targetName: name }),
      setFavoriteClass: (fav) => set({ favoriteClass: fav }),
      setFavoriteTeacher: (fav) => set({ favoriteTeacher: fav }),

      toggleFavorite: (type, name) => {
        if (type === 'teacher') {
          set({ favoriteTeacher: get().favoriteTeacher === name ? null : name });
        } else {
          set({ favoriteClass: get().favoriteClass === name ? null : name });
        }
      },

      isFavorite: (type, name) => {
        return type === 'teacher'
          ? get().favoriteTeacher === name
          : get().favoriteClass === name;
      },

      setAdminPassword: (pw) => set({ adminPassword: pw }),
      setIsDarkMode: (dark) => set({ isDarkMode: dark }),
      toggleDarkMode: () => set((state) => ({ isDarkMode: !state.isDarkMode })),
      setThemePreset: (preset) => set({ themePreset: preset }),
      setAccentColor: (accentColor) => set({ accentColor }),
      setShowPairsSchedule: (val) => set({ showPairsSchedule: val }),
      setStatusBarPairsMode: (val) => set({ statusBarPairsMode: val }),
      setOverlaySwipeAnimation: (val) => set({ overlaySwipeAnimation: val }),
      setStudentSubgroup: (val) => set({ studentSubgroup: val }),
      setStudentTechnologySubgroup: (val) =>
        set({ studentTechnologySubgroup: val }),
      setCopyIncludeHomework: (val) => set({ copyIncludeHomework: val }),
      setCopyIncludeNotes: (val) => set({ copyIncludeNotes: val }),
      setUseMagmaLogo: (val) => set({ useMagmaLogo: val }),
      setIsMagmaUnlocked: (val) => set({ isMagmaUnlocked: val }),
      setCorsProxy: (proxy) => set({ corsProxy: proxy }),
      setNotificationsEnabled: (val) => set({ notificationsEnabled: val }),

      isMiddleSchoolClass: (name) => {
        if (!name || !name.trim()) return false;
        return /^[5-7][а-яА-Яa-zA-Z].*/.test(name.trim());
      },
    }),
    {
      name: 'schedulexy-preferences',
    }
  )
);
