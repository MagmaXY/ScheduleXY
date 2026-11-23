import React from 'react';
import ReactDOM from 'react-dom/client';
import { HashRouter } from 'react-router-dom';
import { App } from './App';
import { ThemeProvider } from './theme/ThemeProvider';
import { yandexSyncService } from './services/yandexSync';
import { notificationService } from './services/notificationService';
import './index.css';

// Initialize OneSignal Push Notifications
notificationService.init();

// Intercept Yandex OAuth token from hash before React HashRouter attempts to parse it as a route
yandexSyncService.handleAuthCallback();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <HashRouter>
      <ThemeProvider>
        <App />
      </ThemeProvider>
    </HashRouter>
  </React.StrictMode>
);
