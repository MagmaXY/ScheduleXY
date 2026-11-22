import { get, set, del, keys } from 'idb-keyval';
import {
  ArchiveResponse,
  ClassesResponse,
  EventsResponse,
  ScheduleChanges,
  ScheduleResponse,
  TeachersResponse,
} from '../types/api';

function safeKey(name: string): string {
  return name.replace(/\//g, '_').replace(/\\/g, '_').replace(/\s+/g, '_');
}

export const cacheManager = {
  async saveSchedule(
    type: string,
    name: string,
    week: number,
    schedule: ScheduleResponse
  ): Promise<void> {
    const key = `schedule_${type}_${safeKey(name)}_week_${week}`;
    await set(key, schedule);
  },

  async getSchedule(
    type: string,
    name: string,
    week: number
  ): Promise<ScheduleResponse | null> {
    const key = `schedule_${type}_${safeKey(name)}_week_${week}`;
    const data = await get<ScheduleResponse>(key);
    return data || null;
  },

  async saveClasses(classes: ClassesResponse): Promise<void> {
    await set('classes', classes);
  },

  async getClasses(): Promise<ClassesResponse | null> {
    const data = await get<ClassesResponse>('classes');
    return data || null;
  },

  async saveTeachers(teachers: TeachersResponse): Promise<void> {
    await set('teachers', teachers);
  },

  async getTeachers(): Promise<TeachersResponse | null> {
    const data = await get<TeachersResponse>('teachers');
    return data || null;
  },

  async saveEvents(events: EventsResponse): Promise<void> {
    await set('events', events);
  },

  async getEvents(): Promise<EventsResponse | null> {
    const data = await get<EventsResponse>('events');
    return data || null;
  },

  async saveArchiveWeeks(archive: ArchiveResponse): Promise<void> {
    await set('archive_weeks', archive);
  },

  async getArchiveWeeks(): Promise<ArchiveResponse | null> {
    const data = await get<ArchiveResponse>('archive_weeks');
    return data || null;
  },

  async updateCachedScheduleChanges(
    exportDate?: string | null,
    changes?: ScheduleChanges | null
  ): Promise<void> {
    if (!changes && !exportDate) return;
    try {
      const allKeys = await keys();
      for (const k of allKeys) {
        const strKey = String(k);
        if (strKey.startsWith('schedule_') && strKey.endsWith('_week_0')) {
          const item = await get<ScheduleResponse>(strKey);
          if (item) {
            const updated: ScheduleResponse = {
              ...item,
              exportDate: exportDate || item.exportDate,
              changes: changes || item.changes,
              hasChanges: Boolean(
                (changes?.class && Object.keys(changes.class).length > 0) ||
                  (changes?.teacher && Object.keys(changes.teacher).length > 0)
              ),
            };
            await set(strKey, updated);
          }
        }
      }
    } catch {
      // ignore
    }
  },

  async getAllCachedClassSchedules(week = 0): Promise<ScheduleResponse[]> {
    try {
      const allKeys = await keys();
      const list: ScheduleResponse[] = [];
      const prefix = 'schedule_class_';
      const suffix = `_week_${week}`;
      for (const k of allKeys) {
        const strKey = String(k);
        if (strKey.startsWith(prefix) && strKey.endsWith(suffix)) {
          const sched = await get<ScheduleResponse>(strKey);
          if (sched) list.push(sched);
        }
      }
      return list;
    } catch {
      return [];
    }
  },

  async clearScheduleAndEventsCache(): Promise<void> {
    try {
      const allKeys = await keys();
      for (const k of allKeys) {
        const strKey = String(k);
        if (strKey.startsWith('schedule_') || strKey === 'events') {
          await del(strKey);
        }
      }
    } catch {
      // ignore
    }
  },

  async clearAll(): Promise<void> {
    try {
      const allKeys = await keys();
      for (const k of allKeys) {
        await del(k);
      }
    } catch {
      // ignore
    }
  },
};
