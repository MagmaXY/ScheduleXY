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
import { SlotOccupancy } from '../domain/freeAvailabilityEngine';
import { FetchMode, fetchWithProxy } from './client';

export const lyceumApi = {
  getClasses: (mode: FetchMode = 'cache-first'): Promise<ClassesResponse> => {
    return fetchWithProxy<ClassesResponse>('schedule/classes', {}, mode);
  },

  getTeachers: (mode: FetchMode = 'cache-first'): Promise<TeachersResponse> => {
    return fetchWithProxy<TeachersResponse>('schedule/teachers', {}, mode);
  },

  getClassSchedule: (
    name: string,
    week = 0,
    mode: FetchMode = 'cache-first'
  ): Promise<ScheduleResponse> => {
    return fetchWithProxy<ScheduleResponse>(
      `schedule/class/${encodeURIComponent(name)}?week=${week}`,
      {},
      mode
    );
  },

  getTeacherSchedule: (
    name: string,
    week = 0,
    mode: FetchMode = 'cache-first'
  ): Promise<ScheduleResponse> => {
    return fetchWithProxy<ScheduleResponse>(
      `schedule/teacher/${encodeURIComponent(name)}?week=${week}`,
      {},
      mode
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

  getArchiveWeeks: (mode: FetchMode = 'cache-first'): Promise<ArchiveResponse> => {
    return fetchWithProxy<ArchiveResponse>('schedule/archive', {}, mode);
  },

  getAvailabilityGrid: (): Promise<Record<string, SlotOccupancy>> => {
    return fetchWithProxy<Record<string, SlotOccupancy>>('schedule/availability-grid');
  },

  getEvents: (mode: FetchMode = 'cache-first'): Promise<EventsResponse> => {
    return fetchWithProxy<EventsResponse>('events', {}, mode);
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
