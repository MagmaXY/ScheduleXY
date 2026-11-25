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

let latestScheduleReqId = 0;

export const useScheduleStore = create<ScheduleState>((set) => ({
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
        scheduleRepository.getClasses('cache-first'),
        scheduleRepository.getTeachers('cache-first'),
        scheduleRepository.getArchiveWeeks('cache-first'),
        scheduleRepository.getEvents('cache-first'),
      ]);

      set({
        classesList: cls.items || [],
        teachersList: tch.items || [],
        archiveWeeksList: arch.weeks || [],
        eventsList: ev.items || [],
        eventCategories: ev.categories || [],
      });

      // Background live revalidation so events, archive weeks, classes & teachers stay fresh
      Promise.allSettled([
        scheduleRepository.getClasses('live-only'),
        scheduleRepository.getTeachers('live-only'),
        scheduleRepository.getArchiveWeeks('live-only'),
        scheduleRepository.getEvents('live-only'),
      ]).then(([liveCls, liveTch, liveArch, liveEv]) => {
        set((state) => ({
          classesList:
            liveCls.status === 'fulfilled' && liveCls.value.items?.length > 0
              ? liveCls.value.items
              : state.classesList,
          teachersList:
            liveTch.status === 'fulfilled' && liveTch.value.items?.length > 0
              ? liveTch.value.items
              : state.teachersList,
          archiveWeeksList:
            liveArch.status === 'fulfilled' && liveArch.value.weeks?.length > 0
              ? liveArch.value.weeks
              : state.archiveWeeksList,
          eventsList:
            liveEv.status === 'fulfilled' && liveEv.value.items?.length > 0
              ? liveEv.value.items
              : state.eventsList,
          eventCategories:
            liveEv.status === 'fulfilled' && liveEv.value.categories?.length > 0
              ? liveEv.value.categories
              : state.eventCategories,
        }));
      });
    } catch {
      // ignore
    }
  },

  loadSchedule: async (typeParam?: string, nameParam?: string, week = 0) => {
    const reqId = ++latestScheduleReqId;
    const prefs = usePreferencesStore.getState();
    const type = typeParam || prefs.targetType;
    const name = nameParam || prefs.targetName;
    set({ isLoading: true });

    // 1. Instant cached/static paint so UI renders in <20ms even if proxy is slow/offline
    let hasFastSchedule = false;
    try {
      const fastRes = await scheduleRepository.getCachedOrStaticSchedule(type, name, week);
      if (fastRes && reqId === latestScheduleReqId) {
        hasFastSchedule = true;
        set({ currentSchedule: fastRes });
      }
    } catch {
      // ignore
    }

    // 2. Always fetch live schedule + changes via proxy in background
    try {
      const liveRes = await scheduleRepository.getSchedule(type, name, week, 'live-only');
      if (reqId === latestScheduleReqId) {
        set({ currentSchedule: liveRes, isLoading: false });
      }
    } catch {
      if (reqId === latestScheduleReqId) {
        if (!hasFastSchedule) {
          try {
            const fallback = await scheduleRepository.getSchedule(type, name, week, 'cache-first');
            if (reqId === latestScheduleReqId) {
              set({ currentSchedule: fallback, isLoading: false });
              return;
            }
          } catch {
            // ignore
          }
        }
        set({ isLoading: false });
      }
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
