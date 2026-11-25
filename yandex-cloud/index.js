// Yandex Cloud Function CORS proxy for Lyceum NSTU & Yandex Disk
// Runtime: Node.js 18 / 20 / 22
// Entry point: index.handler

const ALLOWED_HOST_SUFFIXES = ['lyceum.nstu.ru', '.yandex.ru', '.yandex.net'];

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': '*',
  'Cache-Control': 'no-store',
};

function isAllowedTarget(rawUrl) {
  try {
    const parsed = new URL(rawUrl);
    if (parsed.protocol !== 'https:' && parsed.protocol !== 'http:') return false;
    const host = parsed.hostname.toLowerCase();
    return ALLOWED_HOST_SUFFIXES.some((suffix) =>
      suffix.startsWith('.') ? host.endsWith(suffix) : host === suffix
    );
  } catch {
    return false;
  }
}

module.exports.handler = async function (event) {
  const method = (event.httpMethod || 'GET').toUpperCase();

  if (method === 'OPTIONS') {
    return { statusCode: 204, headers: CORS_HEADERS, body: '' };
  }

  const targetUrl =
    event.queryStringParameters?.url ||
    event.multiValueQueryStringParameters?.url?.[0];

  if (!targetUrl) {
    return {
      statusCode: 400,
      headers: { ...CORS_HEADERS, 'Content-Type': 'application/json; charset=utf-8' },
      body: JSON.stringify({ error: 'Missing url param' }),
    };
  }

  if (!isAllowedTarget(targetUrl)) {
    return {
      statusCode: 403,
      headers: { ...CORS_HEADERS, 'Content-Type': 'application/json; charset=utf-8' },
      body: JSON.stringify({ error: 'Target host is not allowed' }),
    };
  }

  const forwardHeaders = {
    'User-Agent': 'ScheduleXY-YandexProxy/1.0',
  };

  for (const [k, v] of Object.entries(event.headers || {})) {
    const lower = k.toLowerCase();
    if (
      lower === 'x-admin-password' ||
      lower === 'accept' ||
      lower === 'content-type' ||
      lower === 'authorization'
    ) {
      forwardHeaders[k] = v;
    }
  }

  const fetchOptions = {
    method,
    headers: forwardHeaders,
    redirect: 'follow',
    signal: AbortSignal.timeout(9000),
  };

  if (method !== 'GET' && method !== 'HEAD' && event.body != null) {
    fetchOptions.body = event.isBase64Encoded
      ? Buffer.from(event.body, 'base64')
      : event.body;
  }

  try {
    const upstream = await fetch(targetUrl, fetchOptions);
    const contentType = upstream.headers.get('content-type') || 'application/json; charset=utf-8';
    const isTextLike =
      contentType.includes('json') ||
      contentType.startsWith('text/') ||
      contentType.includes('javascript') ||
      contentType.includes('xml');

    if (isTextLike) {
      const text = await upstream.text();
      return {
        statusCode: upstream.status,
        headers: {
          ...CORS_HEADERS,
          'Content-Type': contentType,
        },
        body: text,
        isBase64Encoded: false,
      };
    }

    const buf = Buffer.from(await upstream.arrayBuffer());
    return {
      statusCode: upstream.status,
      headers: {
        ...CORS_HEADERS,
        'Content-Type': contentType,
      },
      body: buf.toString('base64'),
      isBase64Encoded: true,
    };
  } catch (err) {
    return {
      statusCode: 502,
      headers: { ...CORS_HEADERS, 'Content-Type': 'application/json; charset=utf-8' },
      body: JSON.stringify({ error: 'Upstream request failed', details: err.message }),
    };
  }
};
