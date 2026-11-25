import {
  AdminCheckResponse,
  ArchiveResponse,
  ClassesResponse,
  EventsResponse,
  FreeRoomsResponse,
  FreeTeachersResponse,
  ScheduleResponse,
  TeachersResponse,
} from '../types/api';
import { fetchWithProxy } from './client';

export const lyceumApi = {
  getClasses: (): Promise<ClassesResponse> => {
    return fetchWithProxy<ClassesResponse>('schedule/classes');
  },

  getTeachers: (): Promise<TeachersResponse> => {
    return fetchWithProxy<TeachersResponse>('schedule/teachers');
  },

  getClassSchedule: (name: string, week = 0): Promise<ScheduleResponse> => {
    return fetchWithProxy<ScheduleResponse>(
      `schedule/class/${encodeURIComponent(name)}?week=${week}`
    );
  },

  getTeacherSchedule: (name: string, week = 0): Promise<ScheduleResponse> => {
    return fetchWithProxy<ScheduleResponse>(
      `schedule/teacher/${encodeURIComponent(name)}?week=${week}`
    );
  },

  getFreeRooms: (adminPassword = 'licey2026'): Promise<FreeRoomsResponse> => {
    return fetchWithProxy<FreeRoomsResponse>('schedule/free-rooms', {
      headers: { 'X-Admin-Password': adminPassword },
    });
  },

  getFreeTeachers: (adminPassword = 'licey2026'): Promise<FreeTeachersResponse> => {
    return fetchWithProxy<FreeTeachersResponse>('schedule/free-teachers', {
      headers: { 'X-Admin-Password': adminPassword },
    });
  },

  getArchiveWeeks: (): Promise<ArchiveResponse> => {
    return fetchWithProxy<ArchiveResponse>('schedule/archive');
  },

  getEvents: (): Promise<EventsResponse> => {
    return fetchWithProxy<EventsResponse>('events');
  },

  checkAdminPassword: (adminPassword: string): Promise<AdminCheckResponse> => {
    return fetchWithProxy<AdminCheckResponse>('admin/check', {
      headers: { 'X-Admin-Password': adminPassword },
    });
  },

  refreshSchedule: (): Promise<{ ok: boolean }> => {
    return fetchWithProxy<{ ok: boolean }>('schedule/refresh', {
      method: 'POST',
    });
  },
};
