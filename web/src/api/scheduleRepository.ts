import { lyceumApi } from './endpoints';
import { cacheManager } from '../cache/cacheManager';
import {
  ArchiveResponse,
  ClassesResponse,
  EventsResponse,
  FreeRoomsResponse,
  FreeTeachersResponse,
  ScheduleResponse,
  TeachersResponse,
} from '../types/api';
import {
  generateFullLocalFreeRooms,
  generateFullLocalFreeTeachers,
} from '../domain/freeAvailabilityEngine';

export const scheduleRepository = {
  async getClasses(): Promise<ClassesResponse> {
    try {
      const res = await lyceumApi.getClasses();
      if (res.items?.length > 0) {
        await cacheManager.saveClasses(res);
      }
      return res;
    } catch {
      const cached = await cacheManager.getClasses();
      return cached || { items: [] };
    }
  },

  async getTeachers(): Promise<TeachersResponse> {
    try {
      const res = await lyceumApi.getTeachers();
      if (res.items?.length > 0) {
        await cacheManager.saveTeachers(res);
      }
      return res;
    } catch {
      const cached = await cacheManager.getTeachers();
      return cached || { items: [] };
    }
  },

  async getSchedule(
    type: string,
    name: string,
    week = 0
  ): Promise<ScheduleResponse> {
    try {
      let res =
        type === 'teacher'
          ? await lyceumApi.getTeacherSchedule(name, week)
          : await lyceumApi.getClassSchedule(name, week);

      if (type === 'teacher') {
        res = await this.enrichTeacherScheduleWithGroupLabels(name, week, res);
      }

      await cacheManager.saveSchedule(type, name, week, res);

      if (week === 0) {
        // Silently update changes in cache for other cached schedules
        cacheManager.updateCachedScheduleChanges(res.exportDate, res.changes);
      }

      return res;
    } catch (err) {
      const cached = await cacheManager.getSchedule(type, name, week);
      if (cached && !JSON.stringify(cached).includes('\uFFFD')) return cached;
      throw err;
    }
  },

  async enrichTeacherScheduleWithGroupLabels(
    teacherName: string,
    week: number,
    teacherSchedule: ScheduleResponse
  ): Promise<ScheduleResponse> {
    try {
      const classNames = new Set<string>();
      for (const day of teacherSchedule.days) {
        for (const lesson of day.lessons) {
          for (const sg of lesson.subgroups) {
            const c = sg.className?.trim();
            if (c) classNames.add(c);
          }
        }
      }

      if (classNames.size === 0) return teacherSchedule;

      const classSchedules = new Map<string, ScheduleResponse>();
      const classesToFetch: string[] = [];

      for (const c of Array.from(classNames)) {
        const cached = await cacheManager.getSchedule('class', c, week);
        if (cached && !JSON.stringify(cached).includes('\uFFFD')) {
          classSchedules.set(c, cached);
        } else {
          classesToFetch.push(c);
        }
      }

      if (classesToFetch.length > 0) {
        await Promise.all(
          classesToFetch.map(async (c) => {
            try {
              const sched = await lyceumApi.getClassSchedule(c, week);
              await cacheManager.saveSchedule('class', c, week, sched);
              classSchedules.set(c, sched);
            } catch {
              // ignore
            }
          })
        );
      }

      const cleanTeacher = teacherName.replace(/\s+/g, '').toLowerCase();

      const newDays = teacherSchedule.days.map((day) => {
        const newLessons = day.lessons.map((lesson) => {
          const newSubgroups = lesson.subgroups.map((sg) => {
            const cName = sg.className?.trim();
            if (cName && !sg.label) {
              const classSched = classSchedules.get(cName);
              const matchingClassDay = classSched?.days.find(
                (d) => d.dayIndex === day.dayIndex
              );
              const matchingClassLesson = matchingClassDay?.lessons.find(
                (l) => l.num === lesson.num
              );
              const matchingSg = matchingClassLesson?.subgroups.find((csg) => {
                const cTeacher = (csg.teacher || '')
                  .replace(/\s+/g, '')
                  .toLowerCase();
                return (
                  cTeacher === cleanTeacher ||
                  (cTeacher.length > 0 && cleanTeacher.includes(cTeacher)) ||
                  (cleanTeacher.length > 0 && cTeacher.includes(cleanTeacher))
                );
              });
              if (matchingSg?.label) {
                return { ...sg, label: matchingSg.label };
              }
            }
            return sg;
          });
          return { ...lesson, subgroups: newSubgroups };
        });
        return { ...day, lessons: newLessons };
      });

      return { ...teacherSchedule, days: newDays };
    } catch {
      return teacherSchedule;
    }
  },

  async prefetchAllClasses(force = false): Promise<ScheduleResponse[]> {
    const cached = await cacheManager.getAllCachedClassSchedules(0);
    if (!force && cached.length >= 35) {
      return cached;
    }

    let classes: { name: string }[] = [];
    try {
      const cls = await lyceumApi.getClasses();
      classes = cls.items;
    } catch {
      const c = await cacheManager.getClasses();
      classes = c?.items || [];
    }

    if (classes.length === 0) return cached;

    const results: ScheduleResponse[] = [];
    // Fetch in batches of 6
    for (let i = 0; i < classes.length; i += 6) {
      const batch = classes.slice(i, i + 6);
      const batchResults = await Promise.all(
        batch.map(async (cls) => {
          try {
            const s = await lyceumApi.getClassSchedule(cls.name, 0);
            await cacheManager.saveSchedule('class', cls.name, 0, s);
            return s;
          } catch {
            return await cacheManager.getSchedule('class', cls.name, 0);
          }
        })
      );
      results.push(...batchResults.filter((s): s is ScheduleResponse => Boolean(s)));
    }

    return results.length > 0 ? results : cached;
  },

  async getFreeRooms(
    adminPassword = '',
    dayIndex = 0,
    lessonNum = 1
  ): Promise<FreeRoomsResponse> {
    try {
      return await lyceumApi.getFreeRooms(adminPassword || 'licey2026');
    } catch {
      const allClassSchedules = await this.prefetchAllClasses();
      const globalChanges = allClassSchedules.find(
        (s) => s.changes && Object.keys(s.changes.class || {}).length > 0
      )?.changes;
      const globalWeekStart = allClassSchedules.find((s) => s.weekStart)?.weekStart;
      return generateFullLocalFreeRooms(allClassSchedules, globalChanges, globalWeekStart);
    }
  },

  async getFreeTeachers(
    adminPassword = '',
    dayIndex = 0,
    lessonNum = 1
  ): Promise<FreeTeachersResponse> {
    try {
      return await lyceumApi.getFreeTeachers(adminPassword || 'licey2026');
    } catch {
      const teachers = (await this.getTeachers()).items;
      const allClassSchedules = await this.prefetchAllClasses();
      const globalChanges = allClassSchedules.find(
        (s) => s.changes && Object.keys(s.changes.class || {}).length > 0
      )?.changes;
      const globalWeekStart = allClassSchedules.find((s) => s.weekStart)?.weekStart;
      return generateFullLocalFreeTeachers(
        allClassSchedules,
        teachers,
        globalChanges,
        globalWeekStart
      );
    }
  },

  async getArchiveWeeks(): Promise<ArchiveResponse> {
    try {
      const res = await lyceumApi.getArchiveWeeks();
      if (res.weeks?.length > 0) {
        await cacheManager.saveArchiveWeeks(res);
      }
      return res;
    } catch {
      const cached = await cacheManager.getArchiveWeeks();
      return cached || { weeks: [] };
    }
  },

  async getEvents(): Promise<EventsResponse> {
    try {
      const res = await lyceumApi.getEvents();
      if (res.items?.length > 0) {
        await cacheManager.saveEvents(res);
      }
      return res;
    } catch {
      const cached = await cacheManager.getEvents();
      return cached || { items: [], categories: [] };
    }
  },
};
