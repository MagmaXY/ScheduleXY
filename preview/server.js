const http = require('http');
const fs = require('fs');
const path = require('path');
const https = require('https');

const PORT = 3000;
const API_BASE = 'https://lyceum.nstu.ru/info/api';

const server = http.createServer((req, res) => {
  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, X-Admin-Password');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  // Reverse proxy for /api/ -> https://lyceum.nstu.ru/info/api/
  if (req.url.startsWith('/api/')) {
    const apiPath = req.url.replace('/api', '');
    const targetUrl = API_BASE + apiPath;

    const proxyReq = https.request(targetUrl, {
      method: req.method,
      headers: {
        'User-Agent': 'ScheduleXY-App/1.0',
        'X-Admin-Password': req.headers['x-admin-password'] || 'licey2026'
      },
      rejectUnauthorized: false
    }, (proxyRes) => {
      res.writeHead(proxyRes.statusCode, proxyRes.headers);
      proxyRes.pipe(res);
    });

    proxyReq.on('error', (err) => {
      res.writeHead(502, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: err.message }));
    });

    req.pipe(proxyReq);
    return;
  }

  // Serve static preview files
  let filePath = path.join(__dirname, req.url === '/' ? 'index.html' : req.url);
  if (!fs.existsSync(filePath)) {
    filePath = path.join(__dirname, 'index.html');
  }

  const ext = path.extname(filePath);
  const mimeTypes = {
    '.html': 'text/html; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.json': 'application/json; charset=utf-8'
  };

  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404);
      res.end('Not found');
      return;
    }
    res.writeHead(200, { 'Content-Type': mimeTypes[ext] || 'text/plain' });
    res.end(data);
  });
});

server.listen(PORT, () => {
  console.log(`====================================================`);
  console.log(`  Инженерный лицей НГТУ — Расписание & Виджет 2х2  `);
  console.log(`  Предпросмотр запущен: http://localhost:${PORT}     `);
  console.log(`====================================================`);
});
