import { CalendarEvent } from '../types/api';
import { ScheduleNote } from '../types/notes';

const EN_TO_RU: Record<string, string> = {
  q: 'й', w: 'ц', e: 'у', r: 'к', t: 'е', y: 'н', u: 'г', i: 'ш', o: 'щ', p: 'з',
  '[': 'х', ']': 'ъ', a: 'ф', s: 'ы', d: 'в', f: 'а', g: 'п', h: 'р', j: 'о',
  k: 'л', l: 'д', ';': 'ж', "'": 'э', z: 'я', x: 'ч', c: 'с', v: 'м', b: 'и',
  n: 'т', m: 'ь', ',': 'б', '.': 'ю', '`': 'ё',
};

const RU_TO_EN: Record<string, string> = Object.entries(EN_TO_RU).reduce(
  (acc, [en, ru]) => {
    acc[ru] = en;
    return acc;
  },
  {} as Record<string, string>
);

const SUBJECT_SYNONYMS: Record<string, string[]> = {
  матем: ['математика', 'алгебра', 'геометрия'],
  мат: ['математика', 'алгебра', 'геометрия'],
  алг: ['алгебра'],
  геом: ['геометрия'],
  физ: ['физика', 'физическая'],
  физра: ['физическая культура', 'физкультура'],
  инф: ['информатика'],
  инфа: ['информатика'],
  лит: ['литература'],
  литра: ['литература'],
  рус: ['русский'],
  англ: ['английский'],
  'ин яз': ['иностранный', 'английский', 'немецкий', 'французский'],
  ист: ['история'],
  общ: ['обществознание'],
  общага: ['обществознание'],
  био: ['биология'],
  хим: ['химия'],
  гео: ['география'],
  тех: ['технология'],
  обж: ['основы безопасности', 'обж'],
};

export function convertLayout(text: string): string {
  const lower = text.toLowerCase();
  const hasEn = /[a-z[\];',.`]/.test(lower);
  const hasRu = /[а-яё]/.test(lower);

  if (hasEn && !hasRu) {
    return lower.split('').map((c) => EN_TO_RU[c] || c).join('');
  } else if (hasRu && !hasEn) {
    return lower.split('').map((c) => RU_TO_EN[c] || c).join('');
  }
  return text;
}

export function normalize(text?: string | null): string {
  if (!text || !text.trim()) return '';
  return text
    .toLowerCase()
    .replace(/ё/g, 'е')
    .replace(/[^a-zа-я0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

export function levenshteinDistance(s1: string, s2: string): number {
  if (s1 === s2) return 0;
  if (!s1.length) return s2.length;
  if (!s2.length) return s1.length;

  const d: number[][] = Array.from({ length: s1.length + 1 }, () =>
    Array(s2.length + 1).fill(0)
  );

  for (let i = 0; i <= s1.length; i++) d[i][0] = i;
  for (let j = 0; j <= s2.length; j++) d[0][j] = j;

  for (let i = 1; i <= s1.length; i++) {
    for (let j = 1; j <= s2.length; j++) {
      const cost = s1[i - 1] === s2[j - 1] ? 0 : 1;
      d[i][j] = Math.min(
        d[i - 1][j] + 1,
        d[i][j - 1] + 1,
        d[i - 1][j - 1] + cost
      );
    }
  }

  return d[s1.length][s2.length];
}

export function evaluateScore(
  rawQuery: string,
  title?: string | null,
  content?: string | null,
  extra?: string | null
): number {
  const q = normalize(rawQuery);
  if (!q) return 100;

  const qLayout = normalize(convertLayout(rawQuery));
  const normTitle = normalize(title);
  const normContent = normalize(content);
  const normExtra = normalize(extra);

  const fullText = `${normTitle} ${normContent} ${normExtra}`.trim();
  if (!fullText) return 0;

  let bestScore = 0;

  // 1. Exact match in title/subject
  if (normTitle) {
    if (normTitle === q) return 100;
    if (normTitle.includes(q)) bestScore = Math.max(bestScore, 90);
    if (qLayout && normTitle.includes(qLayout)) bestScore = Math.max(bestScore, 85);
  }

  // 2. Exact match in content
  if (normContent) {
    if (normContent.includes(q)) bestScore = Math.max(bestScore, 80);
    if (qLayout && normContent.includes(qLayout)) bestScore = Math.max(bestScore, 75);
  }

  // 3. Extra text match
  if (normExtra) {
    if (normExtra.includes(q)) bestScore = Math.max(bestScore, 70);
    if (qLayout && normExtra.includes(qLayout)) bestScore = Math.max(bestScore, 65);
  }

  // 4. Token matching with Levenshtein & synonyms
  const queryTokens = q.split(' ').filter((t) => t.length >= 2);
  const layoutTokens = qLayout !== q ? qLayout.split(' ').filter((t) => t.length >= 2) : [];
  const allQueryTokens = Array.from(new Set([...queryTokens, ...layoutTokens]));
  const targetTokens = fullText.split(' ').filter(Boolean);

  if (allQueryTokens.length > 0) {
    let matchedTokens = 0;
    let tokenScoreSum = 0;

    for (const token of allQueryTokens) {
      let tokenMatched = false;
      let tokenBest = 0;

      for (const tWord of targetTokens) {
        if (tWord === token) {
          tokenMatched = true;
          tokenBest = Math.max(tokenBest, 80);
          break;
        }
        if (tWord.startsWith(token) && token.length >= 3) {
          tokenMatched = true;
          tokenBest = Math.max(tokenBest, 70);
        }
      }

      if (!tokenMatched) {
        const synonyms = SUBJECT_SYNONYMS[token];
        if (synonyms) {
          for (const syn of synonyms) {
            if (targetTokens.some((t) => t.includes(syn) || syn.includes(t))) {
              tokenMatched = true;
              tokenBest = Math.max(tokenBest, 68);
              break;
            }
          }
        }
      }

      if (!tokenMatched && token.length >= 3) {
        const maxDist = token.length >= 6 ? 2 : 1;
        for (const tWord of targetTokens) {
          if (Math.abs(tWord.length - token.length) <= maxDist) {
            const dist = levenshteinDistance(token, tWord);
            if (dist <= maxDist) {
              tokenMatched = true;
              tokenBest = Math.max(tokenBest, dist === 1 ? 60 : 45);
              break;
            }
          }
        }
      }

      if (tokenMatched) {
        matchedTokens++;
        tokenScoreSum += tokenBest;
      }
    }

    if (matchedTokens > 0) {
      const average = Math.round(tokenScoreSum / matchedTokens);
      bestScore = Math.max(bestScore, average);
    }
  }

  return bestScore;
}

export function searchNotes(notes: ScheduleNote[], query: string): ScheduleNote[] {
  const q = query.trim();
  if (!q) return notes;

  return notes
    .map((note) => ({
      note,
      score: evaluateScore(
        q,
        note.subject,
        note.text,
        note.type === 'homework' ? 'домашнее задание дз' : 'заметка'
      ),
    }))
    .filter((item) => item.score > 0)
    .sort((a, b) => {
      if (b.score !== a.score) return b.score - a.score;
      if (a.note.date !== b.note.date) return a.note.date.localeCompare(b.note.date);
      return (a.note.lessonNum || 99) - (b.note.lessonNum || 99);
    })
    .map((item) => item.note);
}

export function searchEvents(events: CalendarEvent[], query: string): CalendarEvent[] {
  const q = query.trim();
  if (!q) return events;

  return events
    .map((event) => ({
      event,
      score: evaluateScore(q, event.title, event.source, event.sourceLabel),
    }))
    .filter((item) => item.score > 0)
    .sort((a, b) => {
      if (b.score !== a.score) return b.score - a.score;
      return a.event.date.localeCompare(b.event.date);
    })
    .map((item) => item.event);
}
