(() => {
    const config = window.APP_CONFIG?.tgAnalytics || {};
    const TOKEN = config.token?.trim();
    const SCRIPT_URL = config.scriptUrl || 'https://tganalytics.xyz/index.js';

    const getAnalytics = () =>
        window.tgAnalytics || window.tganalytics || window.TGAnalytics;

    const callAnalytics = (method, ...args) => {
        const analytics = getAnalytics();
        if (!analytics) return false;
        if (typeof analytics === 'function') {
            analytics(method, ...args);
            return true;
        }
        if (typeof analytics[method] === 'function') {
            analytics[method](...args);
            return true;
        }
        return false;
    };

    const initAnalytics = () => {
        const analytics = getAnalytics();
        if (!analytics) return false;
        if (typeof analytics === 'function') {
            analytics('init', TOKEN);
            return true;
        }
        if (typeof analytics.init === 'function') {
            analytics.init(TOKEN);
            return true;
        }
        return true;
    };

    const loadScript = () => new Promise((resolve, reject) => {
        if (!SCRIPT_URL) {
            reject(new Error('TG Analytics script URL missing'));
            return;
        }
        if (document.querySelector('script[data-tg-analytics]')) {
            resolve();
            return;
        }
        const script = document.createElement('script');
        script.async = true;
        script.src = SCRIPT_URL;
        script.dataset.tgAnalytics = 'true';
        if (TOKEN) {
            script.dataset.token = TOKEN;
            script.dataset.tgAnalyticsToken = TOKEN;
        }
        script.onload = () => resolve();
        script.onerror = () => reject(new Error('Failed to load TG Analytics SDK'));
        document.head.appendChild(script);
    });

    const flushQueue = (queue) => {
        queue.forEach(({ eventName, params }) => {
            if (!callAnalytics('track', eventName, params)) {
                console.warn('TG Analytics SDK not ready, queued event dropped', eventName);
            }
        });
    };

    const pendingEvents = [];
    let analyticsReady = false;

    window.Analytics = {
        init: async function () {
            if (!TOKEN) {
                console.warn('TG Analytics token missing, analytics disabled');
                return;
            }
            try {
                await loadScript();
                if (!getAnalytics()) {
                    console.error('TG Analytics SDK loaded, but global object missing');
                    return;
                }
                initAnalytics();
                analyticsReady = true;
                flushQueue(pendingEvents.splice(0, pendingEvents.length));
                console.log('TG Analytics ready');
            } catch (e) {
                console.error('Failed to initialize TG Analytics', e);
            }
        },
        track: function (eventName, params = {}) {
            try {
                if (!analyticsReady) {
                    pendingEvents.push({ eventName, params });
                    return;
                }
                if (!callAnalytics('track', eventName, params)) {
                    console.warn('TG Analytics SDK not ready, event skipped', eventName);
                    return;
                }
                console.log(`[Analytics] Tracked: ${eventName}`, params);
            } catch (e) {
                console.warn(`[Analytics] Failed to track: ${eventName}`, e);
            }
        }
    };

    window.Analytics.init();
})();
