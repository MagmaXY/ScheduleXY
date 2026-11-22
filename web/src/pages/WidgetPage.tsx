import React, { useState } from 'react';
import { TargetSelector } from '../components/schedule/TargetSelector';
import { LessonCard } from '../components/schedule/LessonCard';
import { PairCard } from '../components/schedule/PairCard';
import { useScheduleStore } from '../store/scheduleStore';
import { usePreferencesStore } from '../store/preferencesStore';
import { RealtimeState } from '../hooks/useRealtimeSync';
import { getBellDisplayText } from '../domain/bellSchedule';
import { processLessons } from '../domain/schedulePairMerger';
import { getChangesForLesson, isScheduleForTeacher } from '../types/api';
import { ExternalLink, RefreshCw } from 'lucide-react';

interface Props {
  realtimeState: RealtimeState;
}

export const WidgetPage: React.FC<Props> = ({ realtimeState }) => {
  const currentSchedule = useScheduleStore((s) => s.currentSchedule);
  const loadSchedule = useScheduleStore((s) => s.loadSchedule);
  const isLoading = useScheduleStore((s) => s.isLoading);

  const targetName = usePreferencesStore((s) => s.targetName);
  const showPairsSchedule = usePreferencesStore((s) => s.showPairsSchedule);
  const statusBarPairsMode = usePreferencesStore((s) => s.statusBarPairsMode);

  const [activeDayIndex, setActiveDayIndex] = useState(
    realtimeState.dayIndex < 6 ? realtimeState.dayIndex : 0
  );

  const isTeacher = isScheduleForTeacher(currentSchedule);
  const daySchedule = currentSchedule?.days?.find(
    (d) => d.dayIndex === activeDayIndex
  );
  const lessons = daySchedule?.lessons || [];
  const processedItems = processLessons(
    lessons,
    isTeacher,
    targetName,
    showPairsSchedule
  );

  const bellText = getBellDisplayText(realtimeState.bellState, statusBarPairsMode);

  return (
    <div className="min-h-screen bg-[var(--md-background)] p-3 max-w-sm mx-auto flex flex-col justify-between">
      <div className="space-y-3">
        {/* Compact Kiosk Bar */}
        <div className="flex items-center justify-between p-2.5 bg-[var(--md-surface)] border border-[var(--md-border)] rounded-2xl shadow-sm">
          <div>
            <h1 className="font-extrabold text-xs text-[var(--md-text-primary)]">
              ScheduleXY • Виджет
            </h1>
            <p className="text-[11px] font-semibold text-[var(--nika-lsn-time)] truncate max-w-[220px]">
              {bellText}
            </p>
          </div>
          <div className="flex items-center gap-1">
            <button
              onClick={() => {
                const prefs = usePreferencesStore.getState();
                loadSchedule(prefs.targetType, prefs.targetName, 0);
              }}
              className="p-1.5 rounded-lg text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)]"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            </button>
            <a
              href="./#/"
              className="p-1.5 rounded-lg text-[var(--md-text-muted)] hover:text-[var(--md-text-primary)]"
              title="Открыть полное приложение"
            >
              <ExternalLink className="w-3.5 h-3.5" />
            </a>
          </div>
        </div>

        {/* Target Selector */}
        <TargetSelector
          onTargetChanged={(type, name) => loadSchedule(type, name, 0)}
        />

        {/* Mini Days Ribbon */}
        <div className="flex gap-1">
          {['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'].map((d, i) => (
            <button
              key={d}
              onClick={() => setActiveDayIndex(i)}
              className={`flex-1 py-1 rounded-xl text-xs font-bold transition-all ${
                activeDayIndex === i
                  ? 'bg-[var(--nika-subject)] text-[var(--md-on-primary)] shadow-sm'
                  : 'bg-[var(--md-surface)] text-[var(--md-text-muted)] border border-[var(--md-border)]'
              }`}
            >
              {d}
            </button>
          ))}
        </div>

        {/* Lessons List */}
        {processedItems.length === 0 ? (
          <div className="py-12 text-center text-xs text-[var(--md-text-muted)] bg-[var(--md-surface)] border border-[var(--md-border)] rounded-2xl">
            Занятий нет
          </div>
        ) : (
          <div className="bg-[var(--md-surface)] rounded-2xl border border-[var(--md-border)] divide-y divide-[var(--md-border-light)] overflow-hidden">
            {processedItems.map((item, idx) => {
              if (item.type === 'pair') {
                const ch1 = getChangesForLesson(
                  currentSchedule,
                  activeDayIndex,
                  item.firstLesson.num
                );
                const ch2 = getChangesForLesson(
                  currentSchedule,
                  activeDayIndex,
                  item.secondLesson.num
                );
                return (
                  <PairCard
                    key={idx}
                    pairNum={item.pairNum}
                    pairRoman={item.pairRoman}
                    pairTimes={item.pairTimes}
                    firstLesson={item.firstLesson}
                    secondLesson={item.secondLesson}
                    dayIndex={activeDayIndex}
                    changes1={ch1}
                    changes2={ch2}
                    isTeacherMode={isTeacher}
                    isActiveNow={false}
                  />
                );
              }
              const ch = getChangesForLesson(
                currentSchedule,
                activeDayIndex,
                item.lesson.num
              );
              return (
                <LessonCard
                  key={idx}
                  lesson={item.lesson}
                  dayIndex={activeDayIndex}
                  changes={ch}
                  isTeacherMode={isTeacher}
                  isActiveNow={false}
                />
              );
            })}
          </div>
        )}
      </div>

      <div className="text-center text-[10px] text-[var(--md-text-muted)] pt-4 pb-2">
        Окно можно закрепить как отдельный виджет на рабочем столе
      </div>
    </div>
  );
};
