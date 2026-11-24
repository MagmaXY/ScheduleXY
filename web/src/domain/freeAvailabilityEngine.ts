import {
  FreeRoomsResponse,
  FreeTeachersResponse,
  RoomSlotAvailability,
  ScheduleChanges,
  ScheduleResponse,
  TeacherItem,
  getChangesForLesson,
  isCancelledLesson,
  normalizeSubject,
} from '../types/api';

export const ALL_KNOWN_ROOMS = [
  '101', '103', '105', '105(Н)', '108', '109', '110', '113', '116', '128',
  '201', '202', '203', '204', '205', '206', '207', '208', '209', '210', '211', '212', '213', '214', '226', '230', '232',
  '301', '302', '303', '304', '305', '306', '307', '308', '309', '310', '311', '312', '313', '314',
  '1к404', '2к502', 'Акт. зал', 'Спортзал', 'Малый зал',
];

export interface SlotOccupancy {
  dayIndex: number;
  dayName: string;
  lessonNum: number;
  time: string;
  freeRooms: string[];
  busyRooms: Record<string, string>; // room -> "10-1: Физика"
  freeTeachers: string[];
  busyTeachers: Record<string, string>; // teacher -> "10-1: Физика"
}

export function getDayName(dayIndex: number): string {
  switch (dayIndex) {
    case 0: return 'Понедельник';
    case 1: return 'Вторник';
    case 2: return 'Среда';
    case 3: return 'Четверг';
    case 4: return 'Пятница';
    case 5: return 'Суббота';
    default: return 'Воскресенье';
  }
}

export function getLessonTime(lessonNum: number): string {
  switch (lessonNum) {
    case 1: return '8:15–9:00';
    case 2: return '9:10–9:55';
    case 3: return '10:15–11:00';
    case 4: return '11:15–12:00';
    case 5: return '12:10–12:55';
    case 6: return '13:05–13:50';
    case 7: return '14:00–14:40';
    case 8: return '14:55–15:35';
    case 9: return '15:50–16:30';
    case 10: return '16:40–17:20';
    case 11: return '17:30–18:10';
    case 12: return '18:20–19:00';
    default: return '';
  }
}

export function computeSlotAvailability(
  allClassSchedules: ScheduleResponse[],
  allTeachers: TeacherItem[],
  dayIndex: number,
  lessonNum: number,
  globalChanges?: ScheduleChanges | null,
  globalWeekStart?: string | null
): SlotOccupancy {
  const busyRooms: Record<string, string> = {};
  const busyTeachers: Record<string, string> = {};
  const discoveredRooms = new Set<string>(ALL_KNOWN_ROOMS);

  for (const schedule of allClassSchedules) {
    const className = schedule.className;
    if (!className) continue;

    const schedWithChanges: ScheduleResponse = {
      ...schedule,
      weekStart: schedule.weekStart || globalWeekStart || undefined,
      changes: schedule.changes || globalChanges || undefined,
    };

    const changes = getChangesForLesson(schedWithChanges, dayIndex, lessonNum);
    const day = schedule.days?.find((d) => d.dayIndex === dayIndex);
    const lesson = day?.lessons?.find((l) => l.num === lessonNum);

    // If there are changes for this class at this lesson
    if (changes.length > 0) {
      const allCancelled = changes.every((c) => isCancelledLesson(c));
      const hasGeneralCancel = changes.some(
        (c) => isCancelledLesson(c) && (!c.group || c.group === '0')
      );

      if (allCancelled || hasGeneralCancel) {
        // Lesson cancelled - class does not occupy any room or teacher
        continue;
      }

      let appliedChange = false;
      for (const change of changes) {
        if (isCancelledLesson(change)) continue;

        const repSubj = normalizeSubject(change.subject) || 'Занятие';
        const repRoom = change.room?.trim();
        const repTeacher = change.teacher?.trim();

        if (repRoom) {
          discoveredRooms.add(repRoom);
          const info = `${className}: ${repSubj} (замена)`;
          busyRooms[repRoom] = busyRooms[repRoom]
            ? `${busyRooms[repRoom]}, ${info}`
            : info;
          appliedChange = true;
        }
        if (repTeacher) {
          const info = `${className}: ${repSubj} ${
            repRoom ? `(${repRoom})` : ''
          } (замена)`.trim();
          busyTeachers[repTeacher] = busyTeachers[repTeacher]
            ? `${busyTeachers[repTeacher]}, ${info}`
            : info;
          appliedChange = true;
        }
      }

      const hasGeneralChange = changes.some((c) => !c.group || c.group === '0');
      if (hasGeneralChange || appliedChange) {
        continue;
      }
    }

    // Normal timetable lesson
    if (lesson) {
      for (const subgroup of lesson.subgroups) {
        const subject = normalizeSubject(subgroup.subject) || 'Занятие';
        const room = subgroup.room?.trim();
        if (room) {
          discoveredRooms.add(room);
          const info = `${className}: ${subject}`;
          busyRooms[room] = busyRooms[room] ? `${busyRooms[room]}, ${info}` : info;
        }

        const teacher = subgroup.teacher?.trim();
        if (teacher) {
          const info = `${className}: ${subject} ${
            room ? `(${room})` : ''
          }`.trim();
          busyTeachers[teacher] = busyTeachers[teacher]
            ? `${busyTeachers[teacher]}, ${info}`
            : info;
        }
      }
    }
  }

  // Also apply direct teacher changes if any (e.g. consultations or substitute assignments)
  const teacherChanges = globalChanges?.teacher;
  if (teacherChanges && globalWeekStart) {
    const parts = globalWeekStart.split('-');
    if (parts.length >= 3) {
      const year = parseInt(parts[0], 10);
      const month = parseInt(parts[1], 10) - 1;
      const day = parseInt(parts[2], 10);
      const d = new Date(year, month, day);
      d.setDate(d.getDate() + dayIndex);
      const dateStr = `${String(d.getDate()).padStart(2, '0')}.${String(
        d.getMonth() + 1
      ).padStart(2, '0')}.${d.getFullYear()}`;

      for (const [key, item] of Object.entries(teacherChanges)) {
        const keyParts = key.split('|');
        const keyTeacher = keyParts[0]?.trim();
        const keyDate = keyParts[1]?.trim();
        const keyNum = keyParts[2] ? parseInt(keyParts[2].trim(), 10) : null;

        const matchesDate = keyDate === dateStr || item.dateStr === dateStr;
        const matchesNum = keyNum === lessonNum || item.lessonNum === lessonNum;

        if (matchesDate && matchesNum && !isCancelledLesson(item)) {
          const teacherName = item.teacher?.trim() || keyTeacher;
          const room = item.room?.trim();
          const subj = normalizeSubject(item.subject) || 'Занятие';
          const cls = item.className?.trim() || '';

          if (room && !busyRooms[room]?.includes('(замена)')) {
            discoveredRooms.add(room);
            const info = `${cls ? `${cls}: ` : ''}${subj} (замена)`;
            busyRooms[room] = busyRooms[room]
              ? `${busyRooms[room]}, ${info}`
              : info;
          }
          if (teacherName && !busyTeachers[teacherName]?.includes('(замена)')) {
            const info = `${cls ? `${cls}: ` : ''}${subj} ${
              room ? `(${room})` : ''
            } (замена)`.trim();
            busyTeachers[teacherName] = busyTeachers[teacherName]
              ? `${busyTeachers[teacherName]}, ${info}`
              : info;
          }
        }
      }
    }
  }

  const freeRooms = Array.from(discoveredRooms)
    .filter((r) => !busyRooms[r])
    .sort();

  const allTeacherNames = Array.from(new Set(allTeachers.map((t) => t.name)));
  const freeTeachers = allTeacherNames
    .filter((t) => !busyTeachers[t])
    .sort();

  return {
    dayIndex,
    dayName: getDayName(dayIndex),
    lessonNum,
    time: getLessonTime(lessonNum),
    freeRooms,
    busyRooms,
    freeTeachers,
    busyTeachers,
  };
}

export function generateFullLocalFreeRooms(
  allClassSchedules: ScheduleResponse[],
  globalChanges?: ScheduleChanges | null,
  globalWeekStart?: string | null
): FreeRoomsResponse {
  const roomsMap: Record<string, RoomSlotAvailability> = {};
  const days = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота'];
  const lessonTimes: Record<string, string> = {};

  for (let num = 1; num <= 12; num++) {
    lessonTimes[String(num)] = getLessonTime(num);
  }

  for (let dIdx = 0; dIdx < 6; dIdx++) {
    const dName = days[dIdx];
    for (let num = 1; num <= 12; num++) {
      const slot = computeSlotAvailability(
        allClassSchedules,
        [],
        dIdx,
        num,
        globalChanges,
        globalWeekStart
      );
      const key = `${dName}|${num}`;
      roomsMap[key] = {
        busy: Object.keys(slot.busyRooms).sort(),
        free: slot.freeRooms,
      };
    }
  }

  return {
    rooms: roomsMap,
    lessonTimes,
    updatedAt: Date.now(),
  };
}

export function generateFullLocalFreeTeachers(
  allClassSchedules: ScheduleResponse[],
  allTeachers: TeacherItem[],
  globalChanges?: ScheduleChanges | null,
  globalWeekStart?: string | null
): FreeTeachersResponse {
  const teachersMap: Record<string, string[]> = {};
  const days = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота'];
  const lessonTimes: Record<string, string> = {};

  for (let num = 1; num <= 12; num++) {
    lessonTimes[String(num)] = getLessonTime(num);
  }

  for (let dIdx = 0; dIdx < 6; dIdx++) {
    const dName = days[dIdx];
    for (let num = 1; num <= 12; num++) {
      const slot = computeSlotAvailability(
        allClassSchedules,
        allTeachers,
        dIdx,
        num,
        globalChanges,
        globalWeekStart
      );
      const key = `${dName}|${num}`;
      teachersMap[key] = slot.freeTeachers;
    }
  }

  return {
    teachers: teachersMap,
    lessonTimes,
    totalTeachers: allTeachers.length,
    updatedAt: Date.now(),
  };
}
