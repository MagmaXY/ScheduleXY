const fs = require('fs');
const path = require('path');
const https = require('https');

const ONESIGNAL_APP_ID = process.env.ONESIGNAL_APP_ID;
const ONESIGNAL_API_KEY = process.env.ONESIGNAL_API_KEY;

const BASE_URL = 'https://lyceum.nstu.ru/info/api';
const DATA_DIR = path.resolve(__dirname, '../web/public/data');

function ensureDir(dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

function fetchJson(url) {
  return new Promise((resolve, reject) => {
    https.get(url, {
      headers: {
        'User-Agent': 'ScheduleXY-Sync/1.0 (GitHub Actions)'
      }
    }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          reject(new Error(`Failed to parse JSON from ${url}: ${e.message}`));
        }
      });
    }).on('error', reject);
  });
}

function sendPush(className, message) {
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

  const req = https.request('https://onesignal.com/api/v1/notifications', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': authHeader
    }
  }, (res) => {
    console.log(`Push sent for ${className}: status ${res.statusCode}`);
  });
  req.on('error', (err) => console.error(`Error sending push for ${className}:`, err.message));
  req.write(payload);
  req.end();
}

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function main() {
  ensureDir(DATA_DIR);
  ensureDir(path.join(DATA_DIR, 'class'));
  ensureDir(path.join(DATA_DIR, 'teacher'));
  ensureDir(path.join(DATA_DIR, 'archive'));

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

  console.log('5. Fetching schedules for classes (week 0)...');
  for (const c of classes) {
    const className = c.name;
    const safeName = encodeURIComponent(className);
    const targetFile = path.join(DATA_DIR, 'class', `${safeName}.json`);

    try {
      const schedule = await fetchJson(`${BASE_URL}/schedule/class/${safeName}?week=0`);
      if (schedule) {
        fs.writeFileSync(targetFile, JSON.stringify(schedule));
        
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
    await sleep(30);
  }

  console.log('6. Fetching schedules for teachers (week 0)...');
  for (const t of teachers) {
    const teacherName = t.name;
    const safeName = encodeURIComponent(teacherName);
    const targetFile = path.join(DATA_DIR, 'teacher', `${safeName}.json`);

    try {
      const schedule = await fetchJson(`${BASE_URL}/schedule/teacher/${safeName}?week=0`);
      if (schedule) {
        fs.writeFileSync(targetFile, JSON.stringify(schedule));
      }
    } catch (err) {
      // Ignore individual teacher fails
    }
    await sleep(30);
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
