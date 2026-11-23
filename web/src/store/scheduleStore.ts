import { create } from 'zustand';
import {
  CalendarEvent,
  ClassItem,
  EventCategory,
  ScheduleResponse,
  TeacherItem,
} from '../types/api';
import { scheduleRepository } from '../api/scheduleRepository';
import { getDefaultScheduleDayIndex } from '../domain/bellSchedule';
import { usePreferencesStore } from './preferencesStore';

interface ScheduleState {
  currentSchedule: ScheduleResponse | null;
  archiveSchedule: ScheduleResponse | null;
  classesList: ClassItem[];
  teachersList: TeacherItem[];
  archiveWeeksList: string[];
  eventsList: CalendarEvent[];
  eventCategories: EventCategory[];
  isLoading: boolean;
  selectedDayIndex: number;
  selectedArchiveWeek: number;
  archiveDayIndex: number;

  // Actions
  setSelectedDayIndex: (day: number) => void;
  setSelectedArchiveWeek: (week: number) => void;
  setArchiveDayIndex: (day: number) => void;
  initBaseData: () => Promise<void>;
  loadSchedule: (type?: string, name?: string, week?: number) => Promise<void>;
  loadArchiveSchedule: (week: number) => Promise<void>;
}

export const useScheduleStore = create<ScheduleState>((set, get) => ({
  currentSchedule: null,
  archiveSchedule: null,
  classesList: [],
  teachersList: [],
  archiveWeeksList: [],
  eventsList: [],
  eventCategories: [],
  isLoading: false,
  selectedDayIndex: getDefaultScheduleDayIndex(),
  selectedArchiveWeek: -1,
  archiveDayIndex: 0,

  setSelectedDayIndex: (day) => set({ selectedDayIndex: day }),
  setSelectedArchiveWeek: (week) => set({ selectedArchiveWeek: week }),
  setArchiveDayIndex: (day) => set({ archiveDayIndex: day }),

  initBaseData: async () => {
    try {
      const [cls, tch, arch, ev] = await Promise.all([
        scheduleRepository.getClasses(),
        scheduleRepository.getTeachers(),
        scheduleRepository.getArchiveWeeks(),
        scheduleRepository.getEvents(),
      ]);

      set({
        classesList: cls.items || [],
        teachersList: tch.items || [],
        archiveWeeksList: arch.weeks || [],
        eventsList: ev.items || [],
        eventCategories: ev.categories || [],
      });
    } catch {
      // ignore
    }
  },

  loadSchedule: async (typeParam?: string, nameParam?: string, week = 0) => {
    const prefs = usePreferencesStore.getState();
    const type = typeParam || prefs.targetType;
    const name = nameParam || prefs.targetName;
    set({ isLoading: true });
    try {
      const res = await scheduleRepository.getSchedule(type, name, week);
      set({ currentSchedule: res, isLoading: false });
    } catch {
      set({ isLoading: false });
    }
  },

  loadArchiveSchedule: async (week) => {
    const prefs = usePreferencesStore.getState();
    set({ selectedArchiveWeek: week, isLoading: true });
    try {
      const res = await scheduleRepository.getSchedule(
        prefs.targetType,
        prefs.targetName,
        week
      );
      set({ archiveSchedule: res, isLoading: false });
    } catch {
      set({ isLoading: false });
    }
  },
}));
