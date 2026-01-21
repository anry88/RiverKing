(() => {
    const config = window.APP_CONFIG?.tgAnalytics || {};
    const TOKEN = config.token;
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
        script.onload = () => resolve();
        script.onerror = () => reject(new Error('Failed to load TG Analytics SDK'));
        document.head.appendChild(script);
    });

    window.Analytics = {
        init: async function () {
            if (!TOKEN) {
                console.warn('TG Analytics token missing, analytics disabled');
                return;
            }
            try {
                await loadScript();
                if (!callAnalytics('init', TOKEN)) {
                    console.warn('TG Analytics SDK loaded, but init handler missing');
                    return;
                }
                console.log('TG Analytics initialized');
            } catch (e) {
                console.error('Failed to initialize TG Analytics', e);
            }
        },
        track: function (eventName, params = {}) {
            try {
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
