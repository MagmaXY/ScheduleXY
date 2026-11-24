import { useEffect, useState } from 'react';
import {
  BellState,
  getCurrentBellState,
  getScheduleDayIndex,
} from '../domain/bellSchedule';
import { getCurrentPairState } from '../domain/pairSchedule';
import { isVacation } from '../domain/vacationChecker';
import { usePreferencesStore } from '../store/preferencesStore';

export interface RealtimeState {
  dayIndex: number;
  bellState: BellState;
  lessonBellState: BellState;
  pairBellState: BellState;
}

export function useRealtimeSync(): RealtimeState {
  const statusBarPairsMode = usePreferencesStore((s) => s.statusBarPairsMode);

  const [state, setState] = useState<RealtimeState>(() => {
    const now = new Date();
    const lessonBell = getCurrentBellState(now, false);
    const pairBell = getCurrentPairState(now, false);
    return {
      dayIndex: getScheduleDayIndex(now),
      bellState: statusBarPairsMode ? pairBell : lessonBell,
      lessonBellState: lessonBell,
      pairBellState: pairBell,
    };
  });

  useEffect(() => {
    let isCancelled = false;

    async function update() {
      const now = new Date();
      const vac = await isVacation(now);
      if (isCancelled) return;

      const lessonBell = getCurrentBellState(now, vac);
      const pairBell = getCurrentPairState(now, vac);

      setState({
        dayIndex: getScheduleDayIndex(now),
        bellState: statusBarPairsMode ? pairBell : lessonBell,
        lessonBellState: lessonBell,
        pairBellState: pairBell,
      });
    }

    update();
    const interval = setInterval(update, 15000); // 15 sec update loop

    return () => {
      isCancelled = true;
      clearInterval(interval);
    };
  }, [statusBarPairsMode]);

  return state;
}
