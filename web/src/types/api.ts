// Types from ApiModels.kt

export interface ClassItem {
  name: string;
}

export interface ClassesResponse {
  items: ClassItem[];
}

export interface TeacherItem {
  name: string;
}

export interface TeachersResponse {
  items: TeacherItem[];
}

export interface Subgroup {
  label?: string | null;
  subject?: string | null;
  teacher?: string | null;
  room?: string | null;
  className?: string | null;
}

export function normalizeSubject(name?: string | null): string {
  if (!name || !name.trim()) return '';
  const trimmed = name.trim();
  const lower = trimmed.toLowerCase();
  let subjectStr = trimmed;
  if (lower === 'м' || lower === 'м.' || lower === 'm' || lower === 'm.') {
    subjectStr = 'Методический час';
  }
  return subjectStr.charAt(0).toUpperCase() + subjectStr.slice(1);
}

export interface Lesson {
  num: number;
  time: string;
  subgroups: Subgroup[];
}

export function getLessonDisplaySubject(lesson: Lesson): string {
  const raw = lesson.subgroups.find((s) => s.subject && s.subject.trim())?.subject || '—';
  return raw === '—' ? raw : normalizeSubject(raw);
}

export function getLessonDisplayRoom(lesson: Lesson): string {
  const rooms = Array.from(new Set(lesson.subgroups.map((s) => s.room?.trim()).filter(Boolean)));
  return rooms.join(', ');
}

export function getLessonDisplayTeacherOrClass(lesson: Lesson): string {
  const teachers = Array.from(new Set(lesson.subgroups.map((s) => s.teacher?.trim()).filter(Boolean)));
  if (teachers.length > 0) return teachers.join(', ');
  const classes = Array.from(new Set(lesson.subgroups.map((s) => s.className?.trim()).filter(Boolean)));
  if (classes.length > 0) return classes.join(', ');
  return '';
}

export interface DaySchedule {
  dayIndex: number;
  dayName: string;
  lessons: Lesson[];
}

export interface ScheduleChangeItem {
  dateStr?: string | null;
  lessonNum?: number | null;
  subject?: string | null;
  teacher?: string | null;
  room?: string | null;
  className?: string | null;
  group?: string | null;
  subgroup?: string | null;
  label?: string | null;
  isCancelled?: boolean;
  hasChange?: boolean;
}

export function isCancelledLesson(change: ScheduleChangeItem): boolean {
  return (
    Boolean(change.isCancelled) ||
    change.subject === 'F' ||
    change.subject === '$' ||
    Boolean(change.subject?.startsWith('$')) ||
    change.subject?.toLowerCase() === 'урок отменен'
  );
}

export interface ScheduleChanges {
  class?: Record<string, ScheduleChangeItem>;
  teacher?: Record<string, ScheduleChangeItem>;
}

export interface ScheduleResponse {
  className?: string | null;
  classId?: string | null;
  teacherName?: string | null;
  teacherId?: string | null;
  weekStart?: string | null;
  periodStart?: string | null;
  periodEnd?: string | null;
  exportDate?: string | null;
  changes?: ScheduleChanges | null;
  hasChanges?: boolean;
  changedMonths?: number[];
  days: DaySchedule[];
}

export function getScheduleTitle(schedule?: ScheduleResponse | null): string {
  return schedule?.className || schedule?.teacherName || 'Расписание';
}

export function isScheduleForTeacher(schedule?: ScheduleResponse | null): boolean {
  return Boolean(schedule?.teacherName && schedule.teacherName.trim().length > 0);
}

export function getChangesForLesson(
  schedule: ScheduleResponse | null | undefined,
  dayIndex: number,
  lessonNum: number
): ScheduleChangeItem[] {
  if (!schedule) return [];
  const target = schedule.className || schedule.teacherName;
  if (!target || !schedule.weekStart) return [];

  // Parse weekStart YYYY-MM-DD
  const parts = schedule.weekStart.split('-');
  if (parts.length < 3) return [];
  const year = parseInt(parts[0], 10);
  const month = parseInt(parts[1], 10) - 1;
  const day = parseInt(parts[2], 10);

  const d = new Date(year, month, day);
  d.setDate(d.getDate() + dayIndex);

  const dateStr = `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}.${d.getFullYear()}`;
  const normalizedTarget = target.trim().replace(/\s+/g, ' ');

  const isTeacher = isScheduleForTeacher(schedule);
  const map = isTeacher ? schedule.changes?.teacher : schedule.changes?.class;
  if (!map) return [];

  const results: ScheduleChangeItem[] = [];
  for (const [key, item] of Object.entries(map)) {
    const keyParts = key.split('|');
    const keyTarget = keyParts[0]?.trim().replace(/\s+/g, ' ');
    const keyDate = keyParts[1]?.trim();
    const keyNum = keyParts[2] ? parseInt(keyParts[2].trim(), 10) : null;
    const keyGrp = keyParts[3]?.trim();

    const matchesTarget =
      keyTarget?.toLowerCase() === normalizedTarget.toLowerCase() ||
      item.className?.trim().toLowerCase() === normalizedTarget.toLowerCase() ||
      item.teacher?.trim().toLowerCase() === normalizedTarget.toLowerCase();

    const matchesDate = keyDate === dateStr || item.dateStr === dateStr;
    const matchesNum = keyNum === lessonNum || item.lessonNum === lessonNum;

    if (matchesTarget && matchesDate && matchesNum) {
      const enriched =
        keyGrp && !item.group ? { ...item, group: keyGrp } : { ...item };
      results.push(enriched);
    }
  }
  return results;
}

export function getChangeForLesson(
  schedule: ScheduleResponse | null | undefined,
  dayIndex: number,
  lessonNum: number
): ScheduleChangeItem | undefined {
  return getChangesForLesson(schedule, dayIndex, lessonNum)[0];
}

export interface RoomSlotAvailability {
  busy: string[];
  free: string[];
}

export interface FreeRoomsResponse {
  rooms: Record<string, RoomSlotAvailability>;
  lessonTimes: Record<string, string>;
  updatedAt?: number | null;
}

export interface FreeTeachersResponse {
  teachers: Record<string, string[]>;
  lessonTimes: Record<string, string>;
  totalTeachers?: number | null;
  updatedAt?: number | null;
}

export interface ArchiveResponse {
  weeks: string[];
}

export interface CalendarEvent {
  date: string; // YYYY-MM-DD
  ts?: number | null;
  title: string;
  color?: string | null;
  source?: string | null;
  sourceLabel?: string | null;
}

export interface EventCategory {
  color: string;
  title: string;
  icon: string;
}

export interface EventsResponse {
  items: CalendarEvent[];
  categories: EventCategory[];
  updatedAt?: number | null;
}

export interface AdminCheckResponse {
  ok: boolean;
  required: boolean;
}
