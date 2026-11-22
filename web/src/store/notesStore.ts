import { create } from 'zustand';
import { get, set } from 'idb-keyval';
import { NoteType, ScheduleNote, SlotType } from '../types/notes';

const NOTES_IDB_KEY = 'lyceum_notes';

interface NotesState {
  notes: ScheduleNote[];
  isLoaded: boolean;

  // Actions
  loadNotes: () => Promise<void>;
  setNotesDirectly: (notes: ScheduleNote[]) => Promise<void>;
  addNote: (note: Omit<ScheduleNote, 'id' | 'createdAt' | 'updatedAt'>) => Promise<void>;
  updateNote: (updated: ScheduleNote) => Promise<void>;
  deleteNote: (id: string) => Promise<void>;
  toggleCompleted: (id: string) => Promise<void>;
  setCompleted: (id: string, completed: boolean) => Promise<void>;
  transferHomework: (
    id: string,
    targetDate: string,
    targetLessonNum?: number | null
  ) => Promise<void>;
  markTransferPending: (id: string) => Promise<void>;
  updateNotes: (updatedNotes: ScheduleNote[]) => Promise<void>;
  getNotesForDay: (targetName: string, date: string) => ScheduleNote[];
  getNotesForLesson: (
    targetName: string,
    date: string,
    lessonNum: number
  ) => ScheduleNote[];
  getBeforeLessonsNote: (
    targetName: string,
    date: string
  ) => ScheduleNote | undefined;
  getAfterLessonsNote: (
    targetName: string,
    date: string
  ) => ScheduleNote | undefined;
  getDebts: (targetName: string, todayIso: string) => ScheduleNote[];
  cleanupOldCompleted: (olderThanDays?: number) => Promise<void>;
  exportNotesJson: () => string;
  importNotesJson: (json: string) => Promise<number>;
}

function generateId(): string {
  try {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
  } catch {}
  return 'note_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
}

export const useNotesStore = create<NotesState>((setStore, getStore) => ({
  notes: [],
  isLoaded: false,

  loadNotes: async () => {
    try {
      const data = await get<ScheduleNote[]>(NOTES_IDB_KEY);
      setStore({ notes: data || [], isLoaded: true });
    } catch {
      setStore({ notes: [], isLoaded: true });
    }
  },

  setNotesDirectly: async (notes) => {
    setStore({ notes });
    await set(NOTES_IDB_KEY, notes);
  },

  addNote: async (noteData) => {
    const now = Date.now();
    const newNote: ScheduleNote = {
      ...noteData,
      id: generateId(),
      createdAt: now,
      updatedAt: now,
    };
    const updated = [...getStore().notes, newNote];
    setStore({ notes: updated });
    await set(NOTES_IDB_KEY, updated);
  },

  updateNote: async (updatedNote) => {
    const now = Date.now();
    const updated = getStore().notes.map((n) =>
      n.id === updatedNote.id ? { ...updatedNote, updatedAt: now } : n
    );
    setStore({ notes: updated });
    await set(NOTES_IDB_KEY, updated);
  },

  deleteNote: async (id) => {
    const updated = getStore().notes.filter((n) => n.id !== id);
    setStore({ notes: updated });
    await set(NOTES_IDB_KEY, updated);
  },

  toggleCompleted: async (id) => {
    const now = Date.now();
    const updated = getStore().notes.map((n) =>
      n.id === id ? { ...n, isCompleted: !n.isCompleted, updatedAt: now } : n
    );
    setStore({ notes: updated });
    await set(NOTES_IDB_KEY, updated);
  },

  setCompleted: async (id, completed) => {
    const now = Date.now();
    const updated = getStore().notes.map((n) =>
      n.id === id ? { ...n, isCompleted: completed, updatedAt: now } : n
    );
    setStore({ notes: updated });
    await set(NOTES_IDB_KEY, updated);
  },

  transferHomework: async (id, targetDate, targetLessonNum) => {
    const now = Date.now();
    const updated = getStore().notes.map((n) => {
      if (n.id === id) {
        return {
          ...n,
          date: targetDate,
          lessonNum: targetLessonNum !== undefined ? targetLessonNum : n.lessonNum,
          isCompleted: false,
          transferPending: false,
          updatedAt: now,
        };
      }
      return n;
    });
    setStore({ notes: updated });
    await set(NOTES_IDB_KEY, updated);
  },

  markTransferPending: async (id) => {
    const now = Date.now();
    const updated = getStore().notes.map((n) => {
      if (n.id === id) {
        return {
          ...n,
          transferPending: true,
          isCompleted: false,
          updatedAt: now,
        };
      }
      return n;
    });
    setStore({ notes: updated });
    await set(NOTES_IDB_KEY, updated);
  },

  updateNotes: async (updatedNotes) => {
    if (updatedNotes.length === 0) return;
    const now = Date.now();
    const current = [...getStore().notes];
    let changed = false;
    for (const up of updatedNotes) {
      const idx = current.findIndex((n) => n.id === up.id);
      if (idx >= 0) {
        current[idx] = { ...up, updatedAt: now };
        changed = true;
      }
    }
    if (changed) {
      setStore({ notes: current });
      await set(NOTES_IDB_KEY, current);
    }
  },

  getNotesForDay: (targetName, date) => {
    return getStore().notes.filter(
      (n) => n.targetName === targetName && n.date === date
    );
  },

  getNotesForLesson: (targetName, date, lessonNum) => {
    return getStore().notes.filter(
      (n) =>
        n.targetName === targetName &&
        n.date === date &&
        n.slotType === 'lesson' &&
        n.lessonNum === lessonNum
    );
  },

  getBeforeLessonsNote: (targetName, date) => {
    return getStore().notes.find(
      (n) =>
        n.targetName === targetName &&
        n.date === date &&
        n.slotType === 'before_lessons'
    );
  },

  getAfterLessonsNote: (targetName, date) => {
    return getStore().notes.find(
      (n) =>
        n.targetName === targetName &&
        n.date === date &&
        n.slotType === 'after_lessons'
    );
  },

  getDebts: (targetName, todayIso) => {
    return getStore()
      .notes.filter(
        (n) =>
          n.targetName === targetName &&
          n.type === 'homework' &&
          !n.isCompleted &&
          n.date < todayIso
      )
      .sort((a, b) => a.date.localeCompare(b.date));
  },

  cleanupOldCompleted: async (olderThanDays = 30) => {
    const threshold = Date.now() - olderThanDays * 24 * 60 * 60 * 1000;
    const updated = getStore().notes.filter(
      (n) => !(n.isCompleted && n.createdAt < threshold)
    );
    setStore({ notes: updated });
    await set(NOTES_IDB_KEY, updated);
  },

  exportNotesJson: () => {
    return JSON.stringify(getStore().notes, null, 2);
  },

  importNotesJson: async (json: string) => {
    try {
      const parsed = JSON.parse(json);
      if (!Array.isArray(parsed)) return -1;
      setStore({ notes: parsed });
      await set(NOTES_IDB_KEY, parsed);
      return parsed.length;
    } catch {
      return -1;
    }
  },
}));
