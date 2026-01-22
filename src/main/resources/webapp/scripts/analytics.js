(() => {
    const config = window.APP_CONFIG?.tgAnalytics || {};
    const TOKEN = config.token?.trim();
    const SCRIPT_URL = config.scriptUrl || 'https://tganalytics.xyz/index.js';
    const APP_NAME = config.appName?.trim();

    const getAnalytics = () =>
        window.telegramAnalytics || window.tgAnalytics || window.tganalytics || window.TGAnalytics;

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
    const TRACK_METHODS = ['track', 'event', 'trackEvent'];
    const callAnalyticsTrack = (eventName, params) => (
        TRACK_METHODS.some((method) => callAnalytics(method, eventName, params))
    );
    const hasTrackMethod = () => {
        const analytics = getAnalytics();
        if (!analytics) return false;
        if (typeof analytics === 'function') return true;
        return TRACK_METHODS.some((method) => typeof analytics[method] === 'function');
    };

    const initAnalytics = () => {
        const analytics = getAnalytics();
        if (!analytics) return false;
        if (typeof analytics === 'function') {
            analytics('init', TOKEN);
            return true;
        }
        if (typeof analytics.init === 'function') {
            if (analytics === window.telegramAnalytics) {
                const payload = APP_NAME ? { token: TOKEN, appName: APP_NAME } : { token: TOKEN };
                analytics.init(payload);
            } else {
                analytics.init(TOKEN);
            }
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
            if (!callAnalyticsTrack(eventName, params)) {
                console.warn('TG Analytics SDK not ready, queued event dropped', eventName);
            }
        });
    };

    const pendingEvents = [];
    let analyticsReady = false;
    let initInProgress = false;

    const waitForAnalytics = (attempt = 0) => {
        if (!hasTrackMethod()) {
            if (attempt < 10) {
                setTimeout(() => waitForAnalytics(attempt + 1), 200);
            } else {
                console.warn('TG Analytics SDK not ready after retries');
            }
            return;
        }
        initAnalytics();
        analyticsReady = true;
        initInProgress = false;
        flushQueue(pendingEvents.splice(0, pendingEvents.length));
        console.log('TG Analytics ready');
    };

    window.Analytics = {
        init: async function () {
            if (!TOKEN) {
                console.warn('TG Analytics token missing, analytics disabled');
                return;
            }
            try {
                if (initInProgress || analyticsReady) return;
                initInProgress = true;
                await loadScript();
                if (!getAnalytics()) {
                    console.error('TG Analytics SDK loaded, but global object missing');
                    initInProgress = false;
                    return;
                }
                waitForAnalytics();
            } catch (e) {
                console.error('Failed to initialize TG Analytics', e);
                initInProgress = false;
            }
        },
        track: function (eventName, params = {}) {
            try {
                if (!analyticsReady) {
                    pendingEvents.push({ eventName, params });
                    if (!initInProgress) {
                        window.Analytics.init();
                    }
                    return;
                }
                if (!callAnalyticsTrack(eventName, params)) {
                    pendingEvents.push({ eventName, params });
                    analyticsReady = false;
                    window.Analytics.init();
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
