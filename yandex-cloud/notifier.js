// Yandex Cloud Function: Native Web Push (VAPID) Subscriber & 5-Min Change Notifier
// Runtime: Node.js 20
// Entry point: index.handler (put this file as index.js + package.json in the function editor)
// Env vars:
//   VAPID_PUBLIC_KEY       (required)
//   VAPID_PRIVATE_KEY      (required)
//   VAPID_SUBJECT          (optional, e.g. mailto:admin@example.com)
//   YC_BUCKET              (required, Yandex Object Storage bucket name)
//   YC_ACCESS_KEY_ID       (required, static access key ID)
//   YC_SECRET_ACCESS_KEY   (required, static access key secret)

const crypto = require('crypto');
const webpush = require('web-push');

const SAMPLE_SCHEDULE_URL = 'https://lyceum.nstu.ru/info/api/schedule/class/10-1?week=0';
const SUBSCRIPTIONS_KEY = 'subscriptions.json';
const SNAPSHOT_KEY = 'snapshot.json';

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': '*',
  'Cache-Control': 'no-store',
};

function sha256Hex(data) {
  return crypto.createHash('sha256').update(data).digest('hex');
}

function hmacSha256(key, data) {
  return crypto.createHmac('sha256', key).update(data).digest();
}

async function s3Request(method, key, body = null) {
  const bucket = process.env.YC_BUCKET;
  const accessKey = process.env.YC_ACCESS_KEY_ID;
  const secretKey = process.env.YC_SECRET_ACCESS_KEY;
  if (!bucket || !accessKey || !secretKey) {
    throw new Error('Missing YC_BUCKET / YC_ACCESS_KEY_ID / YC_SECRET_ACCESS_KEY env vars');
  }

  const host = `${bucket}.storage.yandexcloud.net`;
  const region = 'ru-central1';
  const service = 's3';
  const now = new Date();
  const amzDate = now.toISOString().replace(/[:-]|\.\d{3}/g, '');
  const dateStamp = amzDate.slice(0, 8);
  const payload = body == null ? '' : typeof body === 'string' ? body : JSON.stringify(body);
  const payloadHash = sha256Hex(payload);
  const canonicalUri = `/${key}`;

  const canonicalHeaders =
    `host:${host}\n` +
    `x-amz-content-sha256:${payloadHash}\n` +
    `x-amz-date:${amzDate}\n`;
  const signedHeaders = 'host;x-amz-content-sha256;x-amz-date';

  const canonicalRequest = [
    method,
    canonicalUri,
    '',
    canonicalHeaders,
    signedHeaders,
    payloadHash,
  ].join('\n');

  const credentialScope = `${dateStamp}/${region}/${service}/aws4_request`;
  const stringToSign = [
    'AWS4-HMAC-SHA256',
    amzDate,
    credentialScope,
    sha256Hex(canonicalRequest),
  ].join('\n');

  const kDate = hmacSha256(`AWS4${secretKey}`, dateStamp);
  const kRegion = hmacSha256(kDate, region);
  const kService = hmacSha256(kRegion, service);
  const kSigning = hmacSha256(kService, 'aws4_request');
  const signature = crypto
    .createHmac('sha256', kSigning)
    .update(stringToSign)
    .digest('hex');

  const authorization =
    `AWS4-HMAC-SHA256 Credential=${accessKey}/${credentialScope}, ` +
    `SignedHeaders=${signedHeaders}, Signature=${signature}`;

  const headers = {
    Host: host,
    'x-amz-content-sha256': payloadHash,
    'x-amz-date': amzDate,
    Authorization: authorization,
  };
  if (body != null) {
    headers['Content-Type'] = 'application/json; charset=utf-8';
  }

  return await fetch(`https://${host}${canonicalUri}`, {
    method,
    headers,
    body: body == null ? undefined : payload,
    signal: AbortSignal.timeout(5000),
  });
}

async function readJsonObject(key, fallback) {
  const res = await s3Request('GET', key);
  if (res.status === 404) return fallback;
  if (!res.ok) throw new Error(`S3 GET ${key} failed: HTTP ${res.status}`);
  return await res.json();
}

async function writeJsonObject(key, data) {
  const res = await s3Request('PUT', key, JSON.stringify(data));
  if (!res.ok) throw new Error(`S3 PUT ${key} failed: HTTP ${res.status}`);
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
      hash: sha256Hex(JSON.stringify(entries)).slice(0, 16),
      count: entries.length,
    };
  }
  return hashed;
}

async function handleSubscribe(body) {
  const { subscription, targetType, targetName } = body || {};
  if (!subscription?.endpoint || !targetType || !targetName) {
    return {
      statusCode: 400,
      headers: { ...CORS_HEADERS, 'Content-Type': 'application/json; charset=utf-8' },
      body: JSON.stringify({ error: 'Invalid subscription payload' }),
    };
  }

  const list = await readJsonObject(SUBSCRIPTIONS_KEY, []);
  const filtered = Array.isArray(list)
    ? list.filter((item) => item?.subscription?.endpoint !== subscription.endpoint)
    : [];

  filtered.push({
    subscription,
    targetType: targetType === 'teacher' ? 'teacher' : 'class',
    targetName: String(targetName).trim(),
    updatedAt: Date.now(),
  });

  await writeJsonObject(SUBSCRIPTIONS_KEY, filtered);
  return {
    statusCode: 200,
    headers: { ...CORS_HEADERS, 'Content-Type': 'application/json; charset=utf-8' },
    body: JSON.stringify({ ok: true, totalSubscribers: filtered.length }),
  };
}

async function handleTimerCheck() {
  const publicKey = process.env.VAPID_PUBLIC_KEY;
  const privateKey = process.env.VAPID_PRIVATE_KEY;
  if (!publicKey || !privateKey) {
    return { statusCode: 500, body: 'VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY env vars are missing' };
  }

  webpush.setVapidDetails(
    process.env.VAPID_SUBJECT || 'mailto:schedulexy@example.com',
    publicKey,
    privateKey
  );

  const res = await fetch(SAMPLE_SCHEDULE_URL, {
    headers: { 'User-Agent': 'ScheduleXY-Notifier/2.0' },
    signal: AbortSignal.timeout(7000),
  });
  if (!res.ok) {
    return { statusCode: 502, body: `Lyceum API returned HTTP ${res.status}` };
  }

  const schedule = await res.json();
  const changes = schedule?.changes || {};
  const currentClasses = groupChangesByTarget(changes.class);
  const currentTeachers = groupChangesByTarget(changes.teacher);

  const nextSnapshot = {
    class: Object.fromEntries(Object.entries(currentClasses).map(([k, v]) => [k, v.hash])),
    teacher: Object.fromEntries(Object.entries(currentTeachers).map(([k, v]) => [k, v.hash])),
  };

  const prev = await readJsonObject(SNAPSHOT_KEY, null);
  if (!prev) {
    await writeJsonObject(SNAPSHOT_KEY, nextSnapshot);
    return {
      statusCode: 200,
      headers: { ...CORS_HEADERS, 'Content-Type': 'application/json; charset=utf-8' },
      body: JSON.stringify({ ok: true, initializedBaseline: true, notificationsSent: 0 }),
    };
  }

  const changedTargets = [];
  for (const [name, info] of Object.entries(currentClasses)) {
    if (prev.class?.[name] !== info.hash) {
      changedTargets.push({ targetType: 'class', targetName: name, count: info.count });
    }
  }
  for (const [name, info] of Object.entries(currentTeachers)) {
    if (prev.teacher?.[name] !== info.hash) {
      changedTargets.push({ targetType: 'teacher', targetName: name, count: info.count });
    }
  }

  let sentCount = 0;
  if (changedTargets.length > 0) {
    const subscribers = await readJsonObject(SUBSCRIPTIONS_KEY, []);
    const deadEndpoints = new Set();

    for (const target of changedTargets) {
      const matching = subscribers.filter(
        (s) => s.targetType === target.targetType && s.targetName === target.targetName
      );
      if (matching.length === 0) continue;

      const payload = JSON.stringify({
        title: 'Лицей НГТУ — Изменение расписания',
        body:
          target.count > 1
            ? `Обновлены замены (${target.count}) для ${target.targetName}.`
            : `Появилось изменение в расписании для ${target.targetName}.`,
        tag: `change_${target.targetType}_${target.targetName}`,
        url: './#/schedule',
      });

      await Promise.all(
        matching.map(async (entry) => {
          try {
            await webpush.sendNotification(entry.subscription, payload);
            sentCount++;
          } catch (err) {
            if (err.statusCode === 404 || err.statusCode === 410) {
              deadEndpoints.add(entry.subscription.endpoint);
            }
          }
        })
      );
    }

    if (deadEndpoints.size > 0) {
      const alive = subscribers.filter((s) => !deadEndpoints.has(s.subscription.endpoint));
      await writeJsonObject(SUBSCRIPTIONS_KEY, alive);
    }
  }

  await writeJsonObject(SNAPSHOT_KEY, nextSnapshot);

  return {
    statusCode: 200,
    headers: { ...CORS_HEADERS, 'Content-Type': 'application/json; charset=utf-8' },
    body: JSON.stringify({
      ok: true,
      changedTargets: changedTargets.length,
      notificationsSent: sentCount,
    }),
  };
}

module.exports.handler = async function (event) {
  const method = (event?.httpMethod || '').toUpperCase();

  if (method === 'OPTIONS') {
    return { statusCode: 204, headers: CORS_HEADERS, body: '' };
  }

  if (method === 'POST') {
    let body = {};
    try {
      const raw = event.isBase64Encoded
        ? Buffer.from(event.body || '', 'base64').toString('utf8')
        : event.body || '{}';
      body = JSON.parse(raw);
    } catch {
      return {
        statusCode: 400,
        headers: { ...CORS_HEADERS, 'Content-Type': 'application/json; charset=utf-8' },
        body: JSON.stringify({ error: 'Invalid JSON body' }),
      };
    }

    if (body.action === 'subscribe') {
      return await handleSubscribe(body);
    }
  }

  return await handleTimerCheck();
};
