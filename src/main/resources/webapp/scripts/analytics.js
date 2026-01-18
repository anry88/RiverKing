(() => {
    // Telemetree Configuration
    const config = window.APP_CONFIG?.telemetree || {};
    const PROJECT_ID = config.projectId;
    const API_KEY = config.apiKey;

    window.Analytics = {
        init: function () {
            if (!window.telemetree) {
                console.warn('Telemetree SDK not loaded');
                return;
            }
            if (!PROJECT_ID || !API_KEY) {
                console.warn('Telemetree credentials missing, analytics disabled');
                return;
            }
            try {
                window.telemetree.init({
                    projectId: PROJECT_ID,
                    apiKey: API_KEY,
                    appName: 'RiverKing'
                });
                console.log('Telemetree initialized');
            } catch (e) {
                console.error('Failed to initialize Telemetree', e);
            }
        },
        track: function (eventName, params = {}) {
            if (!window.telemetree) return;
            try {
                window.telemetree.track(eventName, params);
                console.log(`[Analytics] Tracked: ${eventName}`, params);
            } catch (e) {
                console.warn(`[Analytics] Failed to track: ${eventName}`, e);
            }
        }
    };

    // Auto-init if SDK is ready, otherwise wait (though usually script is loaded after SDK)
    if (window.telemetree) {
        window.Analytics.init();
    } else {
        window.addEventListener('telemetree_ready', () => window.Analytics.init());
    }
})();
