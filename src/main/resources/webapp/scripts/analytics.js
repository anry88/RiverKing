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
    const TRACK_METHODS = ['track', 'event', 'trackEvent', 'recordEvent', 'collectEvent'];
    const callAnalyticsTrack = (eventName, params) => (
        TRACK_METHODS.some((method) => callAnalytics(method, eventName, params))
    );
    const hasTrackMethod = () => {
        const analytics = getAnalytics();
        if (!analytics) return false;
        if (typeof analytics === 'function') return true;
        return TRACK_METHODS.some((method) => typeof analytics[method] === 'function');
    };
    const hasInitMethod = () => {
        const analytics = getAnalytics();
        if (!analytics) return false;
        if (typeof analytics === 'function') return true;
        return typeof analytics.init === 'function';
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

    const SCRIPT_STATE_ATTR = 'tgAnalyticsState';
    const loadScript = () => new Promise((resolve, reject) => {
        if (!SCRIPT_URL) {
            reject(new Error('TG Analytics script URL missing'));
            return;
        }
        const existingScript = document.querySelector('script[data-tg-analytics]');
        if (existingScript) {
            console.debug('TG Analytics SDK script already present');
            const state = existingScript.dataset[SCRIPT_STATE_ATTR];
            if (state === 'loaded') {
                resolve();
                return;
            }
            if (state === 'error') {
                reject(new Error('Failed to load TG Analytics SDK'));
                return;
            }
            if (state) {
                console.debug('TG Analytics SDK script state', { state });
            }
            existingScript.addEventListener('load', () => {
                existingScript.dataset[SCRIPT_STATE_ATTR] = 'loaded';
                resolve();
            }, { once: true });
            existingScript.addEventListener('error', () => {
                existingScript.dataset[SCRIPT_STATE_ATTR] = 'error';
                reject(new Error('Failed to load TG Analytics SDK'));
            }, { once: true });
            return;
        }
        const script = document.createElement('script');
        script.async = true;
        script.src = SCRIPT_URL;
        script.dataset.tgAnalytics = 'true';
        script.dataset[SCRIPT_STATE_ATTR] = 'loading';
        if (TOKEN) {
            script.dataset.token = TOKEN;
            script.dataset.tgAnalyticsToken = TOKEN;
        }
        console.debug('TG Analytics SDK loading', { scriptUrl: SCRIPT_URL });
        script.onload = () => {
            console.debug('TG Analytics SDK script loaded');
            script.dataset[SCRIPT_STATE_ATTR] = 'loaded';
            resolve();
        };
        script.onerror = () => {
            console.error('TG Analytics SDK script failed to load', { scriptUrl: SCRIPT_URL });
            script.dataset[SCRIPT_STATE_ATTR] = 'error';
            reject(new Error('Failed to load TG Analytics SDK'));
        };
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
    let analyticsSupportsTracking = false;
    let trackingUnsupportedWarned = false;
    let initInProgress = false;
    let initRequested = false;

    const MAX_RETRIES = Number.isFinite(config.maxRetries) ? config.maxRetries : 10;
    const RETRY_DELAY_MS = Number.isFinite(config.retryDelayMs) ? config.retryDelayMs : 200;

    const waitForAnalytics = (attempt = 0) => {
        if (!hasInitMethod()) {
            if (getAnalytics() && !initRequested) {
                initRequested = initAnalytics();
            }
            if (attempt < MAX_RETRIES) {
                if (attempt === 0) {
                    console.debug('TG Analytics SDK ready check started');
                }
                setTimeout(() => waitForAnalytics(attempt + 1), RETRY_DELAY_MS);
            } else {
                const analytics = getAnalytics();
                const analyticsInfo = analytics
                    ? { type: typeof analytics, keys: Object.keys(analytics) }
                    : { type: 'missing' };
                console.warn('TG Analytics SDK not ready after retries', {
                    attempts: MAX_RETRIES,
                    analytics: analyticsInfo
                });
                initInProgress = false;
            }
            return;
        }
        analyticsSupportsTracking = hasTrackMethod();
        initAnalytics();
        analyticsReady = true;
        initInProgress = false;
        if (!analyticsSupportsTracking) {
            const droppedCount = pendingEvents.length;
            pendingEvents.splice(0, pendingEvents.length);
            if (droppedCount > 0) {
                console.warn('TG Analytics SDK ready without tracking methods; queued events dropped', {
                    count: droppedCount
                });
            }
        } else {
            flushQueue(pendingEvents.splice(0, pendingEvents.length));
        }
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
                console.error('Failed to initialize TG Analytics', {
                    error: e,
                    scriptUrl: SCRIPT_URL,
                    tokenPresent: Boolean(TOKEN)
                });
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
                if (!analyticsSupportsTracking) {
                    if (!trackingUnsupportedWarned) {
                        console.warn('TG Analytics SDK does not expose tracking methods; event dropped');
                        trackingUnsupportedWarned = true;
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
