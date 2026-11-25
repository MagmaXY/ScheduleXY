const fs = require('fs');
const path = require('path');

const ONESIGNAL_APP_ID = process.env.ONESIGNAL_APP_ID;
const ONESIGNAL_API_KEY = process.env.ONESIGNAL_API_KEY;

const BASE_URL = 'https://lyceum.nstu.ru/info/api';
const PROXY_BASE = 'https://lyceum-proxy.dimafilippov1800.workers.dev/proxy-url?url=';
const DATA_DIR = path.resolve(__dirname, '../web/public/data');

const ALL_KNOWN_ROOMS = [
  '101', '103', '105', '105(Н)', '108', '109', '110', '113', '116', '128',
  '201', '202', '203', '204', '205', '206', '207', '208', '209', '210', '211', '212', '213', '214', '226', '230', '232',
  '301', '302', '303', '304', '305', '306', '307', '308', '309', '310', '311', '312', '313', '314',
  '1к404', '2к502', 'Акт. зал', 'Спортзал', 'Малый зал',
];

function ensureDir(dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

async function fetchUrl(url, timeoutMs = 6000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const res = await fetch(url, {
      signal: controller.signal,
      headers: {
        'User-Agent': 'ScheduleXY-Sync/1.0 (GitHub Actions)'
      }
    });
    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }
    return await res.json();
  } finally {
    clearTimeout(timer);
  }
}

async function fetchJson(url) {
  try {
    // Try direct connection with 4s timeout
    return await fetchUrl(url, 4000);
  } catch (directErr) {
    // Fallback to Cloudflare Worker proxy with 8s timeout
    try {
      const proxyUrl = `${PROXY_BASE}${encodeURIComponent(url)}`;
      return await fetchUrl(proxyUrl, 8000);
    } catch (proxyErr) {
      throw new Error(`Direct failed (${directErr.message}), Proxy failed (${proxyErr.message}) for ${url}`);
    }
  }
}

async function sendPush(className, message) {
  if (!ONESIGNAL_APP_ID || !ONESIGNAL_API_KEY) {
    return;
  }

  const payload = JSON.stringify({
    app_id: ONESIGNAL_APP_ID,
    filters: [
      { field: 'tag', key: 'class', relation: '=', value: className }
    ],
    headings: { en: 'Лицей НГТУ — Изменение расписания', ru: 'Лицей НГТУ — Изменение расписания' },
    contents: { en: message, ru: message }
  });

  const authHeader = ONESIGNAL_API_KEY.startsWith('os_v2_') || ONESIGNAL_API_KEY.startsWith('Key ') || ONESIGNAL_API_KEY.startsWith('Basic ')
    ? (ONESIGNAL_API_KEY.startsWith('os_v2_') ? `Key ${ONESIGNAL_API_KEY}` : ONESIGNAL_API_KEY)
    : `Basic ${ONESIGNAL_API_KEY}`;

  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 5000);
    const res = await fetch('https://onesignal.com/api/v1/notifications', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': authHeader
      },
      body: payload,
      signal: controller.signal
    });
    clearTimeout(timer);
    console.log(`Push sent for ${className}: status ${res.status}`);
  } catch (err) {
    console.error(`Error sending push for ${className}:`, err.message);
  }
}

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// Compute availability for slots
function computeSlot(allClassSchedules, allTeachers, dayIndex, lessonNum) {
  const busyRooms = {};
  const busyTeachers = {};
  const discoveredRooms = new Set(ALL_KNOWN_ROOMS);

  for (const schedule of allClassSchedules) {
    const className = schedule.className || '';
    const day = schedule.days ? schedule.days.find(d => d.dayIndex === dayIndex) : null;
    if (!day) continue;

    const lesson = day.lessons ? day.lessons.find(l => l.num === lessonNum) : null;
    if (!lesson) continue;

    for (const sg of (lesson.subgroups || [])) {
      const room = (sg.room || '').trim().replace(/^\(|\)$/g, '');
      const teacher = (sg.teacher || '').trim();
      const subject = (sg.subject || '').trim();
      const desc = `${className}: ${subject}`;

      if (room && room !== '-' && room.toLowerCase() !== 'дистант') {
        discoveredRooms.add(room);
        busyRooms[room] = desc;
      }
      if (teacher && teacher !== '-' && !teacher.startsWith('Физкультура')) {
        busyTeachers[teacher] = desc;
      }
    }
  }

  const freeRooms = Array.from(discoveredRooms)
    .filter(r => !busyRooms[r])
    .sort((a, b) => a.localeCompare(b, undefined, { numeric: true, sensitivity: 'base' }));

  const freeTeachers = allTeachers
    .map(t => t.name)
    .filter(t => !busyTeachers[t] && !t.startsWith('Физкультура'))
    .sort((a, b) => a.localeCompare(b, 'ru'));

  return {
    dayIndex,
    lessonNum,
    freeRooms,
    busyRooms,
    freeTeachers,
    busyTeachers
  };
}

async function main() {
  ensureDir(DATA_DIR);
  ensureDir(path.join(DATA_DIR, 'class'));
  ensureDir(path.join(DATA_DIR, 'teacher'));
  ensureDir(path.join(DATA_DIR, 'archive'));
  ensureDir(path.join(DATA_DIR, 'freerooms'));
  ensureDir(path.join(DATA_DIR, 'freeteachers'));

  console.log('1. Fetching classes list...');
  let classes = [];
  try {
    const classesRes = await fetchJson(`${BASE_URL}/schedule/classes`);
    classes = (classesRes && classesRes.items) ? classesRes.items : [];
    fs.writeFileSync(path.join(DATA_DIR, 'classes.json'), JSON.stringify(classesRes, null, 2));
    console.log(`Saved ${classes.length} classes.`);
  } catch (err) {
    console.error('Failed to fetch classes:', err.message);
  }

  console.log('2. Fetching teachers list...');
  let teachers = [];
  try {
    const teachersRes = await fetchJson(`${BASE_URL}/schedule/teachers`);
    teachers = (teachersRes && teachersRes.items) ? teachersRes.items : [];
    fs.writeFileSync(path.join(DATA_DIR, 'teachers.json'), JSON.stringify(teachersRes, null, 2));
    console.log(`Saved ${teachers.length} teachers.`);
  } catch (err) {
    console.error('Failed to fetch teachers:', err.message);
  }

  console.log('3. Fetching events (calendar)...');
  try {
    const eventsRes = await fetchJson(`${BASE_URL}/events`);
    fs.writeFileSync(path.join(DATA_DIR, 'events.json'), JSON.stringify(eventsRes, null, 2));
    console.log('Saved events.');
  } catch (err) {
    console.error('Failed to fetch events:', err.message);
  }

  console.log('4. Fetching archive weeks...');
  let archiveWeeks = [];
  try {
    const archiveRes = await fetchJson(`${BASE_URL}/schedule/archive`);
    fs.writeFileSync(path.join(DATA_DIR, 'archive.json'), JSON.stringify(archiveRes, null, 2));
    archiveWeeks = (archiveRes && archiveRes.weeks) ? archiveRes.weeks : [];
    console.log(`Saved archive weeks: ${archiveWeeks.length}.`);
  } catch (err) {
    console.error('Failed to fetch archive weeks:', err.message);
  }

  // Load previous state snapshot for diff checking
  const snapshotFile = path.join(DATA_DIR, 'schedule_snapshot.json');
  let oldSnapshots = {};
  if (fs.existsSync(snapshotFile)) {
    try {
      oldSnapshots = JSON.parse(fs.readFileSync(snapshotFile, 'utf8'));
    } catch (e) {
      oldSnapshots = {};
    }
  }
  const newSnapshots = {};
  const changedClasses = [];
  const allClassSchedules = [];

  console.log('5. Fetching schedules for classes (week 0)...');
  for (const c of classes) {
    const className = c.name;
    const safeName = encodeURIComponent(className);
    const targetFile = path.join(DATA_DIR, 'class', `${safeName}.json`);
    const rawTargetFile = path.join(DATA_DIR, 'class', `${className}.json`);

    try {
      const schedule = await fetchJson(`${BASE_URL}/schedule/class/${safeName}?week=0`);
      if (schedule) {
        fs.writeFileSync(targetFile, JSON.stringify(schedule));
        if (targetFile !== rawTargetFile) {
          fs.writeFileSync(rawTargetFile, JSON.stringify(schedule));
        }
        allClassSchedules.push(schedule);
        
        // Compute diff hash / representation
        const changesStr = JSON.stringify(schedule.changes || {});
        const daysStr = JSON.stringify(schedule.days || []);
        const scheduleHash = `${changesStr}_${daysStr}`;
        newSnapshots[className] = scheduleHash;

        if (oldSnapshots[className] && oldSnapshots[className] !== scheduleHash) {
          console.log(`>>> Schedule change detected for class: ${className}`);
          changedClasses.push(className);
        }
      }
    } catch (err) {
      console.warn(`Failed to fetch class ${className}:`, err.message);
    }
    await sleep(25);
  }

  console.log('6. Fetching schedules for teachers (week 0)...');
  for (const t of teachers) {
    const teacherName = t.name;
    const safeName = encodeURIComponent(teacherName);
    const targetFile = path.join(DATA_DIR, 'teacher', `${safeName}.json`);
    const rawTargetFile = path.join(DATA_DIR, 'teacher', `${teacherName}.json`);

    try {
      const schedule = await fetchJson(`${BASE_URL}/schedule/teacher/${safeName}?week=0`);
      if (schedule) {
        fs.writeFileSync(targetFile, JSON.stringify(schedule));
        if (targetFile !== rawTargetFile) {
          fs.writeFileSync(rawTargetFile, JSON.stringify(schedule));
        }
      }
    } catch (err) {
      // Ignore individual teacher fails
    }
    await sleep(25);
  }

  console.log('7. Pre-calculating free rooms & teachers availability table...');
  const availabilityGrid = {};
  for (let d = 0; d < 6; d++) {
    for (let l = 1; l <= 12; l++) {
      const slot = computeSlot(allClassSchedules, teachers, d, l);
      availabilityGrid[`${d}_${l}`] = slot;
    }
  }
  fs.writeFileSync(path.join(DATA_DIR, 'availability_grid.json'), JSON.stringify(availabilityGrid));
  console.log('Precalculated availability grid saved.');

  // Also prefetch archive schedules for weeks -1, -2, -3 for classes
  console.log('8. Prefetching archive weeks (-1, -2, -3) for classes...');
  for (let w = 1; w <= 3; w++) {
    const weekParam = -w;
    ensureDir(path.join(DATA_DIR, 'archive', `week_${weekParam}`));
    for (const c of classes) {
      const safeName = encodeURIComponent(c.name);
      const targetFile = path.join(DATA_DIR, 'archive', `week_${weekParam}`, `${safeName}.json`);
      const rawTargetFile = path.join(DATA_DIR, 'archive', `week_${weekParam}`, `${c.name}.json`);
      const isCorrupted = (p) => fs.existsSync(p) && fs.readFileSync(p, 'utf8').includes('\uFFFD');
      const needsFetch = !fs.existsSync(targetFile) || isCorrupted(targetFile) || isCorrupted(rawTargetFile);
      if (needsFetch) {
        try {
          const archSchedule = await fetchJson(`${BASE_URL}/schedule/class/${safeName}?week=${weekParam}`);
          if (archSchedule) {
            fs.writeFileSync(targetFile, JSON.stringify(archSchedule));
            if (targetFile !== rawTargetFile) {
              fs.writeFileSync(rawTargetFile, JSON.stringify(archSchedule));
            }
          }
        } catch (e) {
          // ignore
        }
        await sleep(20);
      }
    }
  }

  // Save updated snapshots
  fs.writeFileSync(snapshotFile, JSON.stringify(newSnapshots, null, 2));

  // Save metadata timestamp
  fs.writeFileSync(path.join(DATA_DIR, 'meta.json'), JSON.stringify({
    lastSync: new Date().toISOString(),
    changedClassesCount: changedClasses.length,
    classesCount: classes.length,
    teachersCount: teachers.length
  }, null, 2));

  // Send push notifications for any changed classes
  if (changedClasses.length > 0) {
    console.log(`Sending OneSignal notifications for ${changedClasses.length} classes...`);
    for (const cls of changedClasses) {
      sendPush(cls, `В расписании или заменах класса ${cls} появились обновления!`);
      await sleep(100);
    }
  } else {
    console.log('No schedule changes detected in this run.');
  }

  console.log('Sync finished successfully.');
}

main().catch(err => {
  console.error('Fatal sync error:', err);
  process.exit(1);
});
