import React, { useState, useEffect, useMemo } from 'react';
import { ChevronDown, Star, Search, Users, User } from 'lucide-react';
import { TargetType, usePreferencesStore } from '../../store/preferencesStore';
import { useScheduleStore } from '../../store/scheduleStore';
import { cacheManager } from '../../cache/cacheManager';
import { ScheduleResponse } from '../../types/api';

interface Props {
  onTargetChanged: (type: TargetType, name: string) => void;
  hideFavoriteStar?: boolean;
  extraRight?: React.ReactNode;
}

export const TargetSelector: React.FC<Props> = ({
  onTargetChanged,
  hideFavoriteStar = false,
  extraRight,
}) => {
  const targetType = usePreferencesStore((s) => s.targetType);
  const targetName = usePreferencesStore((s) => s.targetName);
  const favoriteClass = usePreferencesStore((s) => s.favoriteClass);
  const favoriteTeacher = usePreferencesStore((s) => s.favoriteTeacher);
  const setTargetType = usePreferencesStore((s) => s.setTargetType);
  const setTargetName = usePreferencesStore((s) => s.setTargetName);
  const toggleFavorite = usePreferencesStore((s) => s.toggleFavorite);
  const isFavorite = usePreferencesStore((s) => s.isFavorite(targetType, targetName));

  const classesList = useScheduleStore((s) => s.classesList);
  const teachersList = useScheduleStore((s) => s.teachersList);
  const currentSchedule = useScheduleStore((s) => s.currentSchedule);

  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');

  // Schedules for favorite class & favorite teacher to extract teachers of class & classes of teacher
  const [favClassSched, setFavClassSched] = useState<ScheduleResponse | null>(null);
  const [favTeacherSched, setFavTeacherSched] = useState<ScheduleResponse | null>(null);

  useEffect(() => {
    let isMounted = true;
    const loadFavClassSchedule = async () => {
      if (!favoriteClass) {
        setFavClassSched(null);
        return;
      }
      if (targetType === 'class' && targetName === favoriteClass && currentSchedule) {
        setFavClassSched(currentSchedule);
        return;
      }
      const cached = await cacheManager.getSchedule('class', favoriteClass, 0);
      if (isMounted) {
        setFavClassSched(cached);
      }
    };
    loadFavClassSchedule();
    return () => {
      isMounted = false;
    };
  }, [favoriteClass, targetType, targetName, currentSchedule]);

  useEffect(() => {
    let isMounted = true;
    const loadFavTeacherSchedule = async () => {
      if (!favoriteTeacher) {
        setFavTeacherSched(null);
        return;
      }
      if (targetType === 'teacher' && targetName === favoriteTeacher && currentSchedule) {
        setFavTeacherSched(currentSchedule);
        return;
      }
      const cached = await cacheManager.getSchedule('teacher', favoriteTeacher, 0);
      if (isMounted) {
        setFavTeacherSched(cached);
      }
    };
    loadFavTeacherSchedule();
    return () => {
      isMounted = false;
    };
  }, [favoriteTeacher, targetType, targetName, currentSchedule]);

  // Extract list of teachers who teach in the favorite class
  const classTeachers = useMemo(() => {
    if (!favClassSched?.days) return [];
    const list: string[] = [];
    favClassSched.days.forEach((d) => {
      d.lessons?.forEach((l) => {
        l.subgroups?.forEach((sg) => {
          const t = sg.teacher?.trim();
          if (t && !list.includes(t)) {
            list.push(t);
          }
        });
      });
    });
    return list.sort();
  }, [favClassSched]);

  // Extract list of classes taught by the favorite teacher
  const teacherClasses = useMemo(() => {
    if (!favTeacherSched?.days) return [];
    const list: string[] = [];
    favTeacherSched.days.forEach((d) => {
      d.lessons?.forEach((l) => {
        l.subgroups?.forEach((sg) => {
          const c = sg.className?.trim();
          if (c && !list.includes(c)) {
            list.push(c);
          }
        });
      });
    });
    return list.sort();
  }, [favTeacherSched]);

  // Build categorized groups matching the Android app logic exactly
  const groups = useMemo(() => {
    if (targetType === 'teacher') {
      const allNames = teachersList.map((t) => t.name);
      const favItem = favoriteTeacher
        ? allNames.find((it) => it.toLowerCase() === favoriteTeacher.toLowerCase())
        : undefined;

      const cTeachers = classTeachers
        .map((ct) =>
          allNames.find(
            (it) =>
              it.toLowerCase() === ct.toLowerCase() ||
              it.toLowerCase().includes(ct.toLowerCase())
          )
        )
        .filter((it): it is string => Boolean(it) && it !== favItem)
        .filter((val, idx, arr) => arr.indexOf(val) === idx);

      const other = allNames.filter(
        (it) => it !== favItem && !cTeachers.includes(it)
      );

      const result: { header: string | null; items: string[] }[] = [];
      if (favItem) {
        result.push({ header: 'Избранный учитель', items: [favItem] });
      }
      if (cTeachers.length > 0) {
        const header = favoriteClass ? `Учителя ${favoriteClass}` : 'Учителя класса';
        result.push({ header, items: cTeachers });
      }
      if (other.length > 0) {
        const header = favItem || cTeachers.length > 0 ? 'Все учителя' : null;
        result.push({ header, items: other });
      }
      return result;
    } else {
      // Classes grouping
      const allNames = classesList.map((c) => c.name);
      const favItem = favoriteClass && allNames.includes(favoriteClass) ? favoriteClass : undefined;

      const gradePrefixMatch = favoriteClass ? favoriteClass.match(/^\d+/) : null;
      const gradePrefix = gradePrefixMatch ? gradePrefixMatch[0] : null;

      const parallel = gradePrefix
        ? allNames.filter(
            (it) => it !== favItem && it.startsWith(gradePrefix)
          )
        : [];

      const tClasses = teacherClasses.filter(
        (it) => allNames.includes(it) && it !== favItem && !parallel.includes(it)
      );

      const other = allNames.filter(
        (it) => it !== favItem && !parallel.includes(it) && !tClasses.includes(it)
      );

      const result: { header: string | null; items: string[] }[] = [];
      if (favItem) {
        result.push({ header: 'Избранный класс', items: [favItem] });
      }
      if (parallel.length > 0) {
        result.push({ header: `Параллель ${gradePrefix} классов`, items: parallel });
      }
      if (tClasses.length > 0) {
        const header = favoriteTeacher ? `Уроки: ${favoriteTeacher}` : 'Классы учителя';
        result.push({ header, items: tClasses });
      }
      if (other.length > 0) {
        const header = favItem || parallel.length > 0 || tClasses.length > 0 ? 'Все классы' : null;
        result.push({ header, items: other });
      }
      return result;
    }
  }, [
    targetType,
    classesList,
    teachersList,
    favoriteClass,
    favoriteTeacher,
    classTeachers,
    teacherClasses,
  ]);

  const handleSelect = (name: string) => {
    setTargetName(name);
    onTargetChanged(targetType, name);
    setIsOpen(false);
    setSearch('');
  };

  const handleTypeChange = (type: TargetType) => {
    setTargetType(type);
    const prefs = usePreferencesStore.getState();
    const defaultName =
      type === 'teacher'
        ? prefs.favoriteTeacher || 'Муль П.Ф.'
        : prefs.favoriteClass || '10-1';
    setTargetName(defaultName);
    onTargetChanged(type, defaultName);
    setSearch('');
  };

  // Filtered items when search query is typed
  const filteredItems = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return null;
    const all = groups.flatMap((g) => g.items);
    return Array.from(new Set(all.filter((it) => it.toLowerCase().includes(q))));
  }, [groups, search]);

  return (
    <div className="relative">
      <div data-no-swipe="true" className="flex items-center justify-between gap-2 overflow-x-auto scrollbar-none py-0.5">
        {/* Left: Two flat text buttons: Классы | Учителя */}
        <div className="flex items-center shrink-0">
          <button
            onClick={() => handleTypeChange('class')}
            className={`h-8 px-2.5 rounded-lg text-[13px] transition-colors ${
              targetType === 'class'
                ? 'font-bold text-[var(--md-text-primary)]'
                : 'font-medium text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)]'
            }`}
          >
            Классы
          </button>
          <button
            onClick={() => handleTypeChange('teacher')}
            className={`h-8 px-2.5 rounded-lg text-[13px] transition-colors ${
              targetType === 'teacher'
                ? 'font-bold text-[var(--md-text-primary)]'
                : 'font-medium text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)]'
            }`}
          >
            Учителя
          </button>
        </div>

        {/* Right: Target Selector Box and Star Favorite Button / Extra Right */}
        <div className="flex items-center gap-1.5 shrink-0">
          <button
            onClick={() => setIsOpen(!isOpen)}
            className="h-8 px-2.5 bg-[var(--md-surface)] rounded-lg text-xs font-bold text-[var(--md-text-primary)] flex items-center gap-1.5 hover:bg-[var(--md-surface-variant)]/60 transition-all"
          >
            {targetType === 'class' ? (
              <Users className="w-3.5 h-3.5 text-[var(--md-text-secondary)]" />
            ) : (
              <User className="w-3.5 h-3.5 text-[var(--md-text-secondary)]" />
            )}
            <span className="truncate max-w-[120px]">{targetName}</span>
            <ChevronDown
              className={`w-3.5 h-3.5 text-[var(--md-text-muted)] transition-transform ${
                isOpen ? 'rotate-180' : ''
              }`}
            />
          </button>

          {/* Extra right content (e.g. archive week tabs) */}
          {extraRight}

          {/* Star Favorite Button */}
          {!hideFavoriteStar && (
            <button
              onClick={() => toggleFavorite(targetType, targetName)}
              className="w-8 h-8 flex items-center justify-center rounded-lg text-[var(--md-text-secondary)] hover:text-[var(--md-text-primary)] transition-all shrink-0"
              title={isFavorite ? 'В избранном' : 'Добавить в избранное'}
            >
              <Star
                className={`w-4 h-4 ${
                  isFavorite
                    ? 'fill-[var(--md-text-secondary)] text-[var(--md-text-secondary)]'
                    : 'text-[var(--md-text-secondary)]'
                }`}
              />
            </button>
          )}
        </div>
      </div>

      {/* Dropdown Menu Modal */}
      {isOpen && (
        <div className="absolute top-full left-0 right-0 mt-2 z-50 bg-[var(--md-surface)] border border-[var(--md-border)] rounded-2xl shadow-xl overflow-hidden animate-in fade-in zoom-in-95 duration-150">
          <div className="p-2 border-b border-[var(--md-border-light)] flex items-center gap-2">
            <Search className="w-4 h-4 text-[var(--md-text-muted)] ml-1" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder={`Поиск ${targetType === 'class' ? 'класса' : 'учителя'}...`}
              className="w-full bg-transparent text-sm text-[var(--md-text-primary)] outline-none"
              autoFocus
            />
          </div>

          <div className="max-h-72 overflow-y-auto p-1.5 space-y-1">
            {filteredItems !== null ? (
              filteredItems.length === 0 ? (
                <div className="p-4 text-center text-xs text-[var(--md-text-muted)]">
                  Ничего не найдено
                </div>
              ) : (
                filteredItems.map((name) => {
                  const isCurrent = name === targetName;
                  const isFav =
                    (targetType === 'class' && name === favoriteClass) ||
                    (targetType === 'teacher' &&
                      name.toLowerCase() === (favoriteTeacher || '').toLowerCase());
                  return (
                    <button
                      key={name}
                      onClick={() => handleSelect(name)}
                      className={`w-full text-left px-3 py-2 rounded-xl text-sm transition-all flex items-center justify-between ${
                        isCurrent
                          ? 'bg-[var(--md-surface-variant)] text-[var(--nika-subject)] font-bold'
                          : 'text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/50'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        {isFav ? (
                          <Star className="w-3.5 h-3.5 fill-[var(--md-text-secondary)] text-[var(--md-text-secondary)]" />
                        ) : targetType === 'class' ? (
                          <Users className="w-3.5 h-3.5 text-[var(--md-text-muted)]" />
                        ) : (
                          <User className="w-3.5 h-3.5 text-[var(--md-text-muted)]" />
                        )}
                        <span>{name}</span>
                      </div>
                    </button>
                  );
                })
              )
            ) : groups.length === 0 ? (
              <div className="p-4 text-center text-xs text-[var(--md-text-muted)]">
                Список пуст
              </div>
            ) : (
              groups.map((group, gIdx) => (
                <div key={group.header || `group_${gIdx}`} className="space-y-0.5">
                  {group.header && (
                    <div className="px-3 pt-2 pb-1 text-[11px] font-semibold text-[var(--md-text-secondary)] tracking-wider">
                      {group.header}
                    </div>
                  )}
                  {group.items.map((name) => {
                    const isCurrent = name === targetName;
                    const isFav =
                      (targetType === 'class' && name === favoriteClass) ||
                      (targetType === 'teacher' &&
                        name.toLowerCase() === (favoriteTeacher || '').toLowerCase());
                    return (
                      <button
                        key={name}
                        onClick={() => handleSelect(name)}
                        className={`w-full text-left px-3 py-1.5 rounded-xl text-sm transition-all flex items-center justify-between ${
                          isCurrent
                            ? 'bg-[var(--md-surface-variant)] text-[var(--nika-subject)] font-bold'
                            : 'text-[var(--md-text-primary)] hover:bg-[var(--md-surface-variant)]/50'
                        }`}
                      >
                        <div className="flex items-center gap-2">
                          {isFav ? (
                            <Star className="w-3.5 h-3.5 fill-[var(--md-text-secondary)] text-[var(--md-text-secondary)]" />
                          ) : targetType === 'class' ? (
                            <Users className="w-3.5 h-3.5 text-[var(--md-text-muted)]" />
                          ) : (
                            <User className="w-3.5 h-3.5 text-[var(--md-text-muted)]" />
                          )}
                          <span className={isCurrent ? 'font-bold' : 'font-normal'}>
                            {name}
                          </span>
                        </div>
                      </button>
                    );
                  })}
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};
