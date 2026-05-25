(() => {
  const paramsText = `${window.location.search || ''}${window.location.hash || ''}`;
  const explicitPlayDeck = /(?:^|[?&#])playdeck(?:=1|=true|&|$)/i.test(paramsText);
  const inFrame = (() => {
    try { return window.parent && window.parent !== window; } catch (_) { return false; }
  })();
  const available = explicitPlayDeck || inFrame;
  const pending = new Map();
  const listeners = new Map();
  let requestSeq = 0;
  let loadedSignalled = false;

  function emit(method, value) {
    const callbacks = listeners.get(method);
    if (!callbacks) return;
    callbacks.slice().forEach(callback => {
      try { callback(value); } catch (err) { console.warn('PlayDeck listener failed', method, err); }
    });
  }

  function post(method, value, extra) {
    if (!available) return false;
    const playdeck = { method };
    if (value !== undefined) playdeck.value = value;
    if (extra && typeof extra === 'object') Object.assign(playdeck, extra);
    try {
      window.parent.postMessage({ playdeck }, '*');
      return true;
    } catch (err) {
      console.warn('PlayDeck postMessage failed', method, err);
      return false;
    }
  }

  function waitFor(method, timeoutMs = 60000) {
    return new Promise((resolve, reject) => {
      const id = ++requestSeq;
      const timeout = setTimeout(() => {
        pending.delete(id);
        reject(new Error('playdeck_timeout'));
      }, timeoutMs);
      pending.set(id, { method, resolve, timeout });
    });
  }

  function request(method, value, timeoutMs) {
    const promise = waitFor(method, timeoutMs);
    if (!post(method, value)) {
      return Promise.reject(new Error('playdeck_unavailable'));
    }
    return promise;
  }

  window.addEventListener('message', ({ data }) => {
    const playdeck = data && data.playdeck;
    if (!playdeck || !playdeck.method) return;
    emit(playdeck.method, playdeck.value);
    for (const [id, item] of pending.entries()) {
      if (item.method !== playdeck.method) continue;
      clearTimeout(item.timeout);
      pending.delete(id);
      item.resolve(playdeck.value);
      break;
    }
  });

  function on(method, callback) {
    if (!listeners.has(method)) listeners.set(method, []);
    listeners.get(method).push(callback);
    return () => {
      const callbacks = listeners.get(method) || [];
      listeners.set(method, callbacks.filter(item => item !== callback));
    };
  }

  function once(method, timeoutMs = 90000) {
    return new Promise((resolve, reject) => {
      const unsubscribe = on(method, value => {
        clearTimeout(timeout);
        unsubscribe();
        resolve(value);
      });
      const timeout = setTimeout(() => {
        unsubscribe();
        reject(new Error('playdeck_timeout'));
      }, timeoutMs);
    });
  }

  function loading(value) {
    if (value === 100) loadedSignalled = true;
    return post('loading', value);
  }

  window.PlayDeckBridge = {
    available,
    isAvailable: () => available,
    post,
    on,
    once,
    loading,
    markLoaded: () => loading(100),
    gameEnd: () => post('gameEnd'),
    getState: () => request('getPlaydeckState', undefined, 10000),
    getUserProfile: () => request('getUserProfile', undefined, 10000),
    getPlatform: () => request('getPlatform', undefined, 10000),
    getToken: () => request('getToken', undefined, 10000),
    requestPayment: value => request('requestPayment', value, 30000),
    getPaymentInfo: externalId => request('getPaymentInfo', { externalId }, 15000),
    openTelegramLink: url => post('openTelegramLink', url),
    showAd: () => {
      const result = Promise.race([
        once('rewardedAd', 120000).then(value => ({ status: 'rewarded', value })),
        once('errAd', 120000).then(value => ({ status: 'error', value })),
        once('skipAd', 120000).then(value => ({ status: 'skipped', value })),
        once('notFoundAd', 120000).then(value => ({ status: 'not_found', value })),
      ]);
      post('showAd');
      return result;
    },
    sendGameProgress: (achievements, progress) => post('sendGameProgress', { achievements, progress }),
    sendAnalyticNewSession: event => post('sendAnalyticNewSession', event),
    track: (name, params = {}) => post('sendAnalytics', {
      name,
      type: 'game_event',
      user_properties: {},
      event_properties: { name, ...params },
    }),
    hapticFeedback: value => post('hapticFeedback', value),
  };

  if (available) {
    loading();
    setTimeout(() => {
      if (!loadedSignalled) loading(100);
    }, 9000);
  }
})();
