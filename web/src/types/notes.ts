// Types from NotesModels.kt

export type NoteType = 'homework' | 'note';

export type SlotType = 'before_lessons' | 'lesson' | 'after_lessons' | 'day_note';

export interface ScheduleNote {
  id: string;
  targetName: string;
  type: NoteType;
  subject?: string | null;
  subgroup?: number | null; // null = all, 1, 2, 3
  date: string; // "YYYY-MM-DD"
  slotType: SlotType;
  lessonNum?: number | null;
  text: string;
  isCompleted: boolean;
  transferPending?: boolean;
  createdAt: number;
  updatedAt?: number;
  deleted?: boolean;
}
