export default async function handler(req, res) {
  // CORS Preflight
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', '*');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  const { url: targetUrl } = req.query;

  if (!targetUrl || typeof targetUrl !== 'string') {
    return res.status(400).json({ error: 'Missing or invalid "url" query parameter' });
  }

  try {
    const fetchHeaders = {};
    const excludeHeaders = ['host', 'origin', 'referer', 'content-length', 'connection'];
    for (const [key, val] of Object.entries(req.headers)) {
      if (!excludeHeaders.includes(key.toLowerCase()) && typeof val === 'string') {
        fetchHeaders[key] = val;
      }
    }
    // Lyceum / Yandex friendly user agent
    fetchHeaders['User-Agent'] = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36';

    const fetchOptions = {
      method: req.method,
      headers: fetchHeaders,
      redirect: 'follow',
    };

    if (req.method !== 'GET' && req.method !== 'HEAD' && req.body) {
      fetchOptions.body = typeof req.body === 'object' ? JSON.stringify(req.body) : req.body;
    }

    const response = await fetch(targetUrl, fetchOptions);

    // Forward status code
    res.status(response.status);

    // Forward safe headers
    const contentType = response.headers.get('content-type');
    if (contentType) {
      res.setHeader('Content-Type', contentType);
    }

    // Cache responses for schedule/static data for 3 minutes to make it instant
    if (req.method === 'GET' && response.status === 200) {
      res.setHeader('Cache-Control', 's-maxage=180, stale-while-revalidate=60');
    }

    const arrayBuffer = await response.arrayBuffer();
    return res.send(Buffer.from(arrayBuffer));
  } catch (err) {
    return res.status(500).json({ error: 'Proxy request failed', details: err.message });
  }
}
