// Yandex Cloud Function: Schedule Change Watcher & OneSignal Push Notifier
// Runtime: Node.js 18 / 20 / 22
// Entry point: index.handler (or notifier.handler)
// Trigger: Yandex Cloud Timer (cron: 0/5 * ? * * *)
// Env vars:
//   ONESIGNAL_API_KEY  (required)
//   ONESIGNAL_APP_ID   (optional, defaults to app ID from web/src/services/notificationService.ts)

const fs = require('fs');
const crypto = require('crypto');

const DEFAULT_ONESIGNAL_APP_ID = '23229097-58ee-4682-9aa2-1c11414d2ea3';
const SAMPLE_SCHEDULE_URL = 'https://lyceum.nstu.ru/info/api/schedule/class/10-1?week=0';
const TMP_SNAPSHOT_PATH = '/tmp/schedulexy_changes_snapshot.json';

function sha256(str) {
  return crypto.createHash('sha256').update(str).digest('hex').slice(0, 16);
}

function getAuthHeader(apiKey) {
  if (apiKey.startsWith('Key ') || apiKey.startsWith('Basic ')) {
    return apiKey;
  }
  return apiKey.startsWith('os_v2_') ? `Key ${apiKey}` : `Basic ${apiKey}`;
}

function groupChangesByTarget(changesMap) {
  const grouped = {};
  for (const [key, val] of Object.entries(changesMap || {})) {
    const target = key.split('|')[0]?.trim();
    if (!target) continue;
    if (!grouped[target]) grouped[target] = [];
    grouped[target].push([key, val]);
  }
  const hashed = {};
  for (const [target, entries] of Object.entries(grouped)) {
    entries.sort((a, b) => a[0].localeCompare(b[0]));
    hashed[target] = {
      hash: sha256(JSON.stringify(entries)),
      count: entries.length,
    };
  }
  return hashed;
}

function loadTmpSnapshot() {
  try {
    if (fs.existsSync(TMP_SNAPSHOT_PATH)) {
      return JSON.parse(fs.readFileSync(TMP_SNAPSHOT_PATH, 'utf8'));
    }
  } catch {
    // ignore
  }
  return null;
}

function saveTmpSnapshot(snapshot) {
  try {
    fs.writeFileSync(TMP_SNAPSHOT_PATH, JSON.stringify(snapshot));
  } catch {
    // ignore
  }
}

async function fetchRecentOneSignalHashes(appId, authHeader) {
  const map = { class: {}, teacher: {} };
  try {
    const res = await fetch(
      `https://onesignal.com/api/v1/notifications?app_id=${encodeURIComponent(appId)}&limit=50`,
      {
        headers: { Authorization: authHeader },
        signal: AbortSignal.timeout(5000),
      }
    );
    if (!res.ok) return map;
    const json = await res.json();
    for (const n of json.notifications || []) {
      const d = n.data;
      if (d?.type === 'schedule_change' && d.targetType && d.target && d.hash) {
        if (!map[d.targetType][d.target]) {
          map[d.targetType][d.target] = d.hash;
        }
      }
    }
  } catch {
    // ignore
  }
  return map;
}

async function sendPush(appId, authHeader, targetType, targetName, hash, count) {
  const message =
    count > 1
      ? `Обновлены замены (${count}) для ${targetName}. Откройте расписание для просмотра.`
      : `Появилось изменение в расписании для ${targetName}. Откройте расписание для просмотра.`;

  const payload = {
    app_id: appId,
    filters: [{ field: 'tag', key: targetType, relation: '=', value: targetName }],
    headings: {
      en: 'Лицей НГТУ — Изменение расписания',
      ru: 'Лицей НГТУ — Изменение расписания',
    },
    contents: { en: message, ru: message },
    data: {
      type: 'schedule_change',
      targetType,
      target: targetName,
      hash,
    },
  };

  const res = await fetch('https://onesignal.com/api/v1/notifications', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: authHeader,
    },
    body: JSON.stringify(payload),
    signal: AbortSignal.timeout(5000),
  });

  return res.status;
}

module.exports.handler = async function () {
  const apiKey = process.env.ONESIGNAL_API_KEY;
  if (!apiKey) {
    return { statusCode: 500, body: 'ONESIGNAL_API_KEY env var is not set' };
  }

  const appId = process.env.ONESIGNAL_APP_ID || DEFAULT_ONESIGNAL_APP_ID;
  const authHeader = getAuthHeader(apiKey);

  // 1. Fetch a single schedule endpoint (contains global changes for all classes & teachers)
  const res = await fetch(SAMPLE_SCHEDULE_URL, {
    headers: { 'User-Agent': 'ScheduleXY-Notifier/1.0' },
    signal: AbortSignal.timeout(7000),
  });

  if (!res.ok) {
    return { statusCode: 502, body: `Lyceum API returned HTTP ${res.status}` };
  }

  const schedule = await res.json();
  const changes = schedule?.changes || {};
  const currentClasses = groupChangesByTarget(changes.class);
  const currentTeachers = groupChangesByTarget(changes.teacher);

  // 2. Load previous hashes from /tmp or OneSignal history on cold start
  let prev = loadTmpSnapshot();
  if (!prev) {
    prev = await fetchRecentOneSignalHashes(appId, authHeader);
  }

  let sentCount = 0;

  // 3. Notify changed classes
  for (const [className, info] of Object.entries(currentClasses)) {
    if (prev.class?.[className] !== info.hash) {
      await sendPush(appId, authHeader, 'class', className, info.hash, info.count);
      sentCount++;
    }
  }

  // 4. Notify changed teachers
  for (const [teacherName, info] of Object.entries(currentTeachers)) {
    if (prev.teacher?.[teacherName] !== info.hash) {
      await sendPush(appId, authHeader, 'teacher', teacherName, info.hash, info.count);
      sentCount++;
    }
  }

  // 5. Persist latest hashes in /tmp for warm invocations
  const nextSnapshot = {
    class: Object.fromEntries(Object.entries(currentClasses).map(([k, v]) => [k, v.hash])),
    teacher: Object.fromEntries(Object.entries(currentTeachers).map(([k, v]) => [k, v.hash])),
  };
  saveTmpSnapshot(nextSnapshot);

  return {
    statusCode: 200,
    body: JSON.stringify({
      ok: true,
      exportDate: schedule?.exportDate || null,
      classesWithChanges: Object.keys(currentClasses).length,
      teachersWithChanges: Object.keys(currentTeachers).length,
      notificationsSent: sentCount,
    }),
  };
};
