(() => {
  const tg = window.Telegram?.WebApp;
  window.BOBBER_ICON = window.BOBBER_ICON || '/app/assets/menu/bobber.png';

  const assetCache = new Map();
  let assetRevision = 0;
  const assetRevisionListeners = new Set();

  function notifyAssetRevision() {
    assetRevision++;
    assetRevisionListeners.forEach(listener => {
      try { listener(assetRevision); }
      catch (e) { console.warn('asset revision listener error', e); }
    });
  }

  function subscribeAssetRevision(listener) {
    assetRevisionListeners.add(listener);
    return () => assetRevisionListeners.delete(listener);
  }

  function parseAssetDescriptor(src) {
    if (!src && src !== '') return null;
    let value = typeof src === 'string' ? src : String(src || '');
    value = value.trim();
    if (!value) return null;
    if (value.startsWith('blob:')) {
      return { key: value, requestPath: null, resolved: value };
    }
    try {
      if (value.startsWith('http://') || value.startsWith('https://')) {
        const url = new URL(value);
        value = url.pathname + url.search;
      }
    } catch (_) { }
    value = value.replace(/\\\\/g, '/');
    if (value.startsWith('/')) {
      value = value.replace(/^\/+/, '');
    }
    if (value.startsWith('app/assets/')) {
      value = value.slice('app/assets/'.length);
    } else if (value.startsWith('webapp/assets/')) {
      value = value.slice('webapp/assets/'.length);
    } else if (value.startsWith('assets/')) {
      value = value.slice('assets/'.length);
    }
    if (value.startsWith('app/')) {
      value = value.slice('app/'.length);
    }
    if (value.startsWith('assets/')) {
      value = value.slice('assets/'.length);
    }
    if (value.includes('..')) return null;
    if (!value) return null;
    return {
      key: value,
      requestPath: value.split('/').map(encodeURIComponent).join('/'),
      resolved: null
    };
  }

  function getCachedAssetSrc(src) {
    const info = parseAssetDescriptor(src);
    if (!info) return null;
    const cached = assetCache.get(info.key);
    return typeof cached === 'string' ? cached : null;
  }

  function ensureAssetSrc(src) {
    const info = parseAssetDescriptor(src);
    if (!info) return Promise.reject(new Error('invalid asset path'));
    if (info.resolved) {
      assetCache.set(info.key, info.resolved);
      return Promise.resolve(info.resolved);
    }
    const cached = assetCache.get(info.key);
    if (typeof cached === 'string') return Promise.resolve(cached);
    if (cached) return cached;
    const requestPath = info.requestPath;
    if (!requestPath) return Promise.reject(new Error('invalid asset path'));
    const inflight = fetch(`/api/assets/${requestPath}`, { credentials: 'include' })
      .then(resp => {
        if (!resp.ok) {
          const err = new Error(`asset ${resp.status}`);
          err.status = resp.status;
          throw err;
        }
        return resp.blob();
      })
      .then(blob => {
        const url = URL.createObjectURL(blob);
        assetCache.set(info.key, url);
        return url;
      })
      .catch(err => {
        assetCache.delete(info.key);
        throw err;
      });
    assetCache.set(info.key, inflight);
    return inflight;
  }

  function decodeImage(url) {
    if (typeof Image === 'undefined') return Promise.resolve(url);
    return new Promise(resolve => {
      let done = false;
      const finish = () => {
        if (done) return;
        done = true;
        resolve(url);
      };
      const img = new Image();
      img.onload = finish;
      img.onerror = finish;
      try {
        img.src = url;
      } catch (e) {
        finish();
        return;
      }
      if (typeof img.decode === 'function') {
        img.decode().then(finish).catch(finish);
      } else if (img.complete) {
        finish();
      }
      setTimeout(finish, 10000);
    });
  }

  function preloadAsset(src) {
    const info = parseAssetDescriptor(src);
    if (!info) return Promise.resolve(null);
    if (info.resolved) return Promise.resolve(info.resolved);
    return ensureAssetSrc(src)
      .then(url => decodeImage(url))
      .catch(err => {
        console.warn('asset preload failed', src, err);
        return null;
      });
  }

  function useAssetSrc(src, options = {}) {
    const { onError } = options;
    const [resolved, setResolved] = React.useState(() => getCachedAssetSrc(src));
    const [revision, setRevision] = React.useState(assetRevision);
    React.useEffect(() => subscribeAssetRevision(setRevision), []);
    React.useEffect(() => {
      let cancelled = false;
      if (!src) {
        setResolved(null);
        return;
      }
      const info = parseAssetDescriptor(src);
      if (!info) {
        setResolved(null);
        return;
      }
      if (info.resolved) {
        setResolved(info.resolved);
        return;
      }
      const cached = getCachedAssetSrc(src);
      if (cached) {
        setResolved(cached);
        return;
      }
      ensureAssetSrc(src)
        .then(url => { if (!cancelled) setResolved(url); })
        .catch(err => {
          if (cancelled) return;
          setResolved(null);
          if (typeof onError === 'function') {
            try { onError(err); }
            catch (handlerErr) { console.warn('asset error handler threw', handlerErr); }
          }
        });
      return () => { cancelled = true; };
    }, [src, onError, revision]);
    return resolved;
  }

  const AssetImage = React.forwardRef(({ src, onError, ...rest }, ref) => {
    const innerRef = React.useRef(null);
    React.useImperativeHandle(ref, () => innerRef.current);
    const resolved = useAssetSrc(src, {
      onError: err => {
        if (typeof onError === 'function') {
          const target = innerRef.current;
          const event = target ? { currentTarget: target, target, error: err } : { error: err };
          try { onError(event); }
          catch (handlerErr) { console.warn('asset onError handler threw', handlerErr); }
        }
      }
    });
    const handleRef = React.useCallback(node => {
      innerRef.current = node;
      if (typeof ref === 'function') ref(node);
      else if (ref) ref.current = node;
    }, [ref]);
    return <img {...rest} ref={handleRef} src={resolved || undefined} onError={onError} />;
  });

  window.useAssetSrc = useAssetSrc;
  window.preloadAsset = preloadAsset;
  window.assetAuthReady = notifyAssetRevision;
  window.ensureAssetSrc = ensureAssetSrc;
  window.AssetImage = AssetImage;

  function applyInsets() {
    const vh = tg?.viewportHeight || window.visualViewport?.height || window.innerHeight;
    document.documentElement.style.setProperty('--vh', vh + 'px');

    const vv = window.visualViewport;
    const layoutH = window.innerHeight || vh;
    const vvH = vv?.height || layoutH;
    const vvTop = vv?.offsetTop || 0;

    const safeTopFB = Math.max(0, Math.round(vvTop));
    const safeBottomFB = Math.max(0, Math.round(layoutH - (vvTop + vvH)));

    document.documentElement.style.setProperty('--safe-top-fb', safeTopFB + 'px');
    document.documentElement.style.setProperty('--safe-bottom-fb', safeBottomFB + 'px');

    const sh = window.screen?.height || vh;
    const bottomGapLegacy = Math.max(0, Math.round(sh - vh));
    const bottomGap = Math.max(safeBottomFB, bottomGapLegacy);
    document.documentElement.style.setProperty('--bottom-gap', bottomGap + 'px');

    const isOverlay = (bottomGap >= 8) || (safeTopFB >= 8);
    document.documentElement.style.setProperty('--overlay', isOverlay ? '1' : '0');

    const plat = (tg?.platform || '').toLowerCase();
    const isMobileTG = plat === 'android' || plat === 'ios';
    const needTGTopbarFallback = isMobileTG && safeTopFB < 4;
    const tgTopbarGuess = needTGTopbarFallback
      ? (plat === 'ios' ? 54 : 48)
      : 0;
    document.documentElement.style.setProperty('--tg-topbar', tgTopbarGuess + 'px');

    if (typeof tg?.requestFullscreen === 'function') tg.requestFullscreen();
    else if (typeof tg?.expand === 'function') tg.expand();
  }

  tg?.ready?.();
  applyInsets();
  tg?.onEvent?.('viewportChanged', applyInsets);
  window.addEventListener('resize', applyInsets);
  window.visualViewport?.addEventListener('resize', applyInsets);
  window.visualViewport?.addEventListener('scroll', applyInsets);
  tg?.setHeaderColor?.('secondary_bg_color');
  tg?.setBottomBarColor?.('bg_color');

  const rarityColors = {
    uncommon: 'text-green-400',
    rare: 'text-blue-400',
    epic: 'text-purple-400',
    mythic: 'text-rose-400',
    legendary: 'text-yellow-400'
  };
  const rarityNames = {
    ru: {
      common: 'Простая',
      uncommon: 'Необычная',
      rare: 'Редкая',
      epic: 'Эпическая',
      mythic: 'Мифическая',
      legendary: 'Легендарная'
    },
    en: {
      common: 'Common',
      uncommon: 'Uncommon',
      rare: 'Rare',
      epic: 'Epic',
      mythic: 'Mythic',
      legendary: 'Legendary'
    }
  };
  const LURE_INFO = {
    'Пресная мирная': {
      plus: false,
      ruName: 'Зерновая крошка',
      enName: 'Grain Crumble',
      ruDescription: 'Для мирной пресноводной рыбы.',
      enDescription: 'For peaceful freshwater fish.',
      icon: '/app/assets/baits/grain_crumble.png'
    },
    'Пресная хищная': {
      plus: false,
      ruName: 'Ручейный малек',
      enName: 'Brook Minnow',
      ruDescription: 'Для хищной пресноводной рыбы.',
      enDescription: 'For predatory freshwater fish.',
      icon: '/app/assets/baits/brook_minnow.png'
    },
    'Морская мирная': {
      plus: false,
      ruName: 'Морская водоросль',
      enName: 'Seaweed Strand',
      ruDescription: 'Для мирной морской рыбы.',
      enDescription: 'For peaceful saltwater fish.',
      icon: '/app/assets/baits/seaweed_strand.png'
    },
    'Морская хищная': {
      plus: false,
      ruName: 'Кольца кальмара',
      enName: 'Squid Rings',
      ruDescription: 'Для хищной морской рыбы.',
      enDescription: 'For predatory saltwater fish.',
      icon: '/app/assets/baits/squid_rings.png'
    },
    'Пресная мирная+': {
      plus: true,
      ruName: 'Луговой червь',
      enName: 'Meadow Worm',
      ruDescription: 'Для редкой мирной пресноводной рыбы.',
      enDescription: 'For rare peaceful freshwater fish.',
      icon: '/app/assets/baits/meadow_worm.png'
    },
    'Пресная хищная+': {
      plus: true,
      ruName: 'Серебряный живец',
      enName: 'Silver Shiner',
      ruDescription: 'Для редкой хищной пресноводной рыбы.',
      enDescription: 'For rare predatory freshwater fish.',
      icon: '/app/assets/baits/silver_shiner.png'
    },
    'Морская мирная+': {
      plus: true,
      ruName: 'Неоновый планктон',
      enName: 'Neon Plankton',
      ruDescription: 'Для редкой мирной морской рыбы.',
      enDescription: 'For rare peaceful saltwater fish.',
      icon: '/app/assets/baits/neon_plankton.png'
    },
    'Морская хищная+': {
      plus: true,
      ruName: 'Королевская креветка',
      enName: 'Royal Shrimp',
      ruDescription: 'Для редкой хищной морской рыбы.',
      enDescription: 'For rare predatory saltwater fish.',
      icon: '/app/assets/baits/royal_shrimp.png'
    },
  };
  const LURE_INFO_BY_DISPLAY = {};
  Object.values(LURE_INFO).forEach(info => {
    LURE_INFO_BY_DISPLAY[info.ruName] = info;
    LURE_INFO_BY_DISPLAY[info.enName] = info;
  });
  const getLureInfo = value => {
    if (!value) return null;
    if (typeof value === 'string') return LURE_INFO[value] || LURE_INFO_BY_DISPLAY[value] || null;
    return LURE_INFO[value.name] || LURE_INFO[value.displayName] || null;
  };
  const getLureIcon = lure => {
    const info = getLureInfo(lure);
    if (info?.icon) return info.icon;
    if (typeof lure === 'object') {
      if (typeof lure?.icon === 'string') return lure.icon;
      if (typeof lure?.image === 'string') return lure.image;
    }
    return null;
  };

  const lureColor = lure => {
    const info = getLureInfo(lure);
    if (info) return info.plus ? rarityColors.legendary : '';
    if (typeof lure === 'object' && lure?.rarityBonus > 0) return rarityColors.legendary;
    const str = typeof lure === 'string' ? lure : (lure?.name || lure?.displayName || '');
    return str.includes('+') ? rarityColors.legendary : '';
  };
  const LOCATION_BG = {
    1: '/app/assets/backgrounds/pond.png',
    2: '/app/assets/backgrounds/swamp.png',
    3: '/app/assets/backgrounds/river.png',
    4: '/app/assets/backgrounds/lake.png',
    5: '/app/assets/backgrounds/reservoir.png',
    6: '/app/assets/backgrounds/mountain_river.png',
    7: '/app/assets/backgrounds/river_delta.png',
    8: '/app/assets/backgrounds/sea_coast.png',
    9: '/app/assets/backgrounds/amazon_riverbed.png',
    10: '/app/assets/backgrounds/flooded_forest.png',
    11: '/app/assets/backgrounds/mangroves.png',
    12: '/app/assets/backgrounds/coral_flats.png',
    13: '/app/assets/backgrounds/fjord.png',
    14: '/app/assets/backgrounds/open_ocean.png',
  };

  const normalizeLocationName = value => {
    if (!value && value !== 0) return '';
    return String(value).trim().toLowerCase();
  };

  const LOCATION_NAMES = {
    1: ['Пруд', 'Pond'],
    2: ['Болото', 'Swamp'],
    3: ['Река', 'River'],
    4: ['Озеро', 'Lake'],
    5: ['Водохранилище', 'Reservoir'],
    6: ['Горная река', 'Mountain River'],
    7: ['Дельта реки', 'River Delta'],
    8: ['Прибрежье моря', 'Sea Coast'],
    9: ['Русло Амазонки', 'Amazon Riverbed'],
    10: ['Игапо, затопленный лес', 'Igapo Flooded Forest'],
    11: ['Мангровые заросли', 'Mangroves'],
    12: ['Коралловые отмели', 'Coral Flats'],
    13: ['Фьорд', 'Fjord'],
    14: ['Открытый океан', 'Open Ocean'],
  };

  const LOCATION_BG_BY_NAME = {};
  Object.entries(LOCATION_NAMES).forEach(([id, names]) => {
    const bg = LOCATION_BG[id];
    if (!bg) return;
    names.forEach(name => {
      if (!name) return;
      LOCATION_BG_BY_NAME[normalizeLocationName(name)] = bg;
    });
  });
  Object.values(LOCATION_BG).forEach(url => {
    const slug = url.split('/').pop()?.replace('.png', '') || '';
    const normalizedSlug = normalizeLocationName(slug.replace(/_/g, ' '));
    if (normalizedSlug && !LOCATION_BG_BY_NAME[normalizedSlug]) {
      LOCATION_BG_BY_NAME[normalizedSlug] = url;
    }
  });

  const getLocationBackground = (id, name) => {
    if (name) {
      const normalized = normalizeLocationName(name);
      if (normalized && LOCATION_BG_BY_NAME[normalized]) {
        return LOCATION_BG_BY_NAME[normalized];
      }
    }
    const numId = Number(id);
    if (Number.isFinite(numId) && LOCATION_BG[numId]) {
      return LOCATION_BG[numId];
    }
    if (name) {
      const slug = normalizeLocationName(String(name).replace(/_/g, ' '));
      if (slug && LOCATION_BG_BY_NAME[slug]) {
        return LOCATION_BG_BY_NAME[slug];
      }
    }
    return null;
  };
  const ROD_TIP_ANCHOR_DEFAULT = { x: 0.07878, y: 0.04785 };
  const ROD_CONFIG = {
    spark: {
      image: '/app/assets/rods/yellow_rod.png',
      tipAnchor: { x: 0.07878, y: 0.04785 },
      linePoints: [
        { x: 0.380, y: 0.99 },
        { x: 0.320, y: 0.80 },
        { x: 0.260, y: 0.61 },
        { x: 0.200, y: 0.42 },
        { x: 0.140, y: 0.23 },
        { x: 0.07878, y: 0.04785 }
      ]
    },
    dew: {
      image: '/app/assets/rods/green_rod.png',
      tipAnchor: { x: 0.13607, y: 0.06641 },
      linePoints: [
        { x: 0.380, y: 0.99 },
        { x: 0.331, y: 0.80 },
        { x: 0.282, y: 0.61 },
        { x: 0.233, y: 0.42 },
        { x: 0.184, y: 0.24 },
        { x: 0.13607, y: 0.06641 }
      ]
    },
    stream: {
      image: '/app/assets/rods/blue_rod.png',
      tipAnchor: { x: 0.16276, y: 0.07422 },
      linePoints: [
        { x: 0.380, y: 0.99 },
        { x: 0.337, y: 0.80 },
        { x: 0.293, y: 0.61 },
        { x: 0.250, y: 0.42 },
        { x: 0.206, y: 0.24 },
        { x: 0.16276, y: 0.07422 }
      ]
    },
    abyss: {
      image: '/app/assets/rods/black_rod.png',
      tipAnchor: { x: 0.14844, y: 0.04688 },
      linePoints: [
        { x: 0.380, y: 0.99 },
        { x: 0.334, y: 0.80 },
        { x: 0.287, y: 0.61 },
        { x: 0.241, y: 0.42 },
        { x: 0.195, y: 0.23 },
        { x: 0.14844, y: 0.04688 }
      ]
    },
    storm: {
      image: '/app/assets/rods/silver_rod.png',
      tipAnchor: { x: 0.13411, y: 0.07520 },
      linePoints: [
        { x: 0.380, y: 0.99 },
        { x: 0.331, y: 0.80 },
        { x: 0.282, y: 0.61 },
        { x: 0.233, y: 0.42 },
        { x: 0.184, y: 0.24 },
        { x: 0.13411, y: 0.07520 }
      ]
    },
    default: {
      image: '/app/assets/rods/yellow_rod.png',
      tipAnchor: ROD_TIP_ANCHOR_DEFAULT,
      linePoints: [
        { x: 0.380, y: 0.99 },
        { x: 0.320, y: 0.80 },
        { x: 0.260, y: 0.61 },
        { x: 0.200, y: 0.42 },
        { x: 0.140, y: 0.23 },
        { x: 0.07878, y: 0.04785 }
      ]
    },
  };
  const ROD_IMAGES = Object.fromEntries(Object.entries(ROD_CONFIG).map(([code, cfg]) => [code, cfg.image]));
  const ROD_TIP_ANCHORS = Object.fromEntries(Object.entries(ROD_CONFIG).map(([code, cfg]) => [code, cfg.tipAnchor]));
  const ROD_LINE_POINTS = Object.fromEntries(Object.entries(ROD_CONFIG).map(([code, cfg]) => [code, cfg.linePoints]));
  const ROD_IMG = ROD_IMAGES.default;
  const ROD_IMG_SIZE = { width: 1536, height: 1024 };
  const ROD_TIP_ANCHOR = ROD_TIP_ANCHORS.default || ROD_TIP_ANCHOR_DEFAULT;
  const ROD_BASE_ANCHOR = { x: 0.383, y: 0.998 };
  const ROD_SIZE_MULT = 1.5;
  const ROD_BASE_X_FRACTION = 2 / 3;

  const TAP_CHALLENGE_GOAL = 10;
  const TAP_CHALLENGE_DURATION_MS = 5000;
  const CAST_READY_DELAY_MS = 3000;

  const FISH_INFO = {
    'Плотва': { en: 'Roach', asset: '/app/assets/fish/plotva.png' },
    'Окунь': { en: 'Perch', asset: '/app/assets/fish/okun.png' },
    'Карась': { en: 'Crucian Carp', asset: '/app/assets/fish/karas.png' },
    'Лещ': { en: 'Bream', asset: '/app/assets/fish/lesch.png' },
    'Щука': { en: 'Pike', asset: '/app/assets/fish/schuka.png' },
    'Карп': { en: 'Carp', asset: '/app/assets/fish/karp.png' },
    'Сом европейский': { en: 'European Catfish', asset: '/app/assets/fish/som.png' },
    'Осётр европейский': { en: 'European Sturgeon', asset: '/app/assets/fish/osetr.png' },
    'Уклейка': { en: 'Bleak', asset: '/app/assets/fish/ukleyka.png' },
    'Линь': { en: 'Tench', asset: '/app/assets/fish/lin.png' },
    'Ротан': { en: 'Rotan', asset: '/app/assets/fish/rotan.png' },
    'Судак': { en: 'Zander', asset: '/app/assets/fish/sudak.png' },
    'Чехонь': { en: 'Sabrefish', asset: '/app/assets/fish/chehon.png' },
    'Хариус': { en: 'Grayling', asset: '/app/assets/fish/harius.png' },
    'Форель ручьевая': { en: 'Brook Trout', asset: '/app/assets/fish/forel_ruchevaya.png' },
    'Таймень': { en: 'Taimen', asset: '/app/assets/fish/taymen.png' },
    'Налим': { en: 'Burbot', asset: '/app/assets/fish/nalim.png' },
    'Сиг обыкновенный': { en: 'Common Whitefish', asset: '/app/assets/fish/sig.png' },
    'Голавль': { en: 'Chub', asset: '/app/assets/fish/golavl.png' },
    'Жерех': { en: 'Asp', asset: '/app/assets/fish/zhereh.png' },
    'Толстолобик': { en: 'Bighead Carp', asset: '/app/assets/fish/tolstolobik.png' },
    'Амур белый': { en: 'Grass Carp', asset: '/app/assets/fish/beliy_amur.png' },
    'Угорь европейский': { en: 'European Eel', asset: '/app/assets/fish/ugor_evropeyskiy.png' },
    'Стерлядь': { en: 'Sterlet', asset: '/app/assets/fish/sterlyad.png' },
    'Кефаль-лобан': { en: 'Flathead Grey Mullet', asset: '/app/assets/fish/kefal.png' },
    'Камбала морская': { en: 'Sea Flounder', asset: '/app/assets/fish/kambala.png' },
    'Сельдь': { en: 'Herring', asset: '/app/assets/fish/seld.png' },
    'Ставрида': { en: 'Horse Mackerel', asset: '/app/assets/fish/stavrida.png' },
    'Тихоокеанский клювач': { en: 'Pacific Snipefish', asset: '/app/assets/fish/klyuvach_pacificocean.png' },
    'Треска': { en: 'Cod', asset: '/app/assets/fish/treska.png' },
    'Сайда': { en: 'Pollock', asset: '/app/assets/fish/sayda.png' },
    'Мерланг': { en: 'Whiting', asset: '/app/assets/fish/merlang.png' },
    'Форель морская': { en: 'Sea Trout', asset: '/app/assets/fish/morskaya_forel.png' },
    'Палтус': { en: 'Halibut', asset: '/app/assets/fish/paltus.png' },
    'Корюшка': { en: 'Smelt', asset: '/app/assets/fish/koryushka.png' },
    'Лосось атлантический': { en: 'Atlantic Salmon', asset: '/app/assets/fish/losos_atlanticheskiy.png' },
    'Лаврак': { en: 'Sea Bass', asset: '/app/assets/fish/lavrak.png' },
    'Скумбрия атлантическая': { en: 'Atlantic Mackerel', asset: '/app/assets/fish/skumbriya_atlanticheskaya.png' },
    'Горбуша': { en: 'Pink Salmon', asset: '/app/assets/fish/gorbusha.png' },
    'Кета': { en: 'Chum Salmon', asset: '/app/assets/fish/keta.png' },
    'Белуга': { en: 'Beluga', asset: '/app/assets/fish/beluga.png' },
    'Ёрш': { en: 'Ruffe', asset: '/app/assets/fish/yorsh.png' },
    'Берш': { en: 'Volga Pikeperch', asset: '/app/assets/fish/bersh.png' },
    'Пескарь': { en: 'Gudgeon', asset: '/app/assets/fish/peskar.png' },
    'Густера': { en: 'Blue Bream', asset: '/app/assets/fish/gustera.png' },
    'Краснопёрка': { en: 'Rudd', asset: '/app/assets/fish/krasnopyorka.png' },
    'Елец': { en: 'Dace', asset: '/app/assets/fish/elets.png' },
    'Верхоплавка': { en: 'Topmouth Gudgeon', asset: '/app/assets/fish/verhoplavka.png' },
    'Вьюн': { en: 'Weather Loach', asset: '/app/assets/fish/vyun.png' },
    'Вобла': { en: 'Caspian Roach', asset: '/app/assets/fish/vobla.png' },
    'Гольян': { en: 'Minnow', asset: '/app/assets/fish/golyan.png' },
    'Язь': { en: 'Ide', asset: '/app/assets/fish/yaz.png' },
    'Бычок-кругляк': { en: 'Round Goby', asset: '/app/assets/fish/bychyok.png' },
    'Килька': { en: 'Sprat', asset: '/app/assets/fish/kilka.png' },
    'Мойва': { en: 'Capelin', asset: '/app/assets/fish/mojva.png' },
    'Тарань': { en: 'Black Sea Roach', asset: '/app/assets/fish/taran.png' },
    'Сардина': { en: 'Sardine', asset: '/app/assets/fish/sardina.png' },
    'Анчоус европейский': { en: 'European Anchovy', asset: '/app/assets/fish/anchous.png' },
    'Дорадо': { en: 'Dorado', asset: '/app/assets/fish/dorado.png' },
    'Ваху': { en: 'Wahoo', asset: '/app/assets/fish/vahu.png' },
    'Парусник': { en: 'Sailfish', asset: '/app/assets/fish/parusnik.png' },
    'Рыба-меч': { en: 'Swordfish', asset: '/app/assets/fish/ryba_mech.png' },
    'Марлин синий': { en: 'Blue Marlin', asset: '/app/assets/fish/marlin_siniy.png' },
    'Тунец синеперый': { en: 'Bluefin Tuna', asset: '/app/assets/fish/tunets_sineperiy.png' },
    'Акула мако': { en: 'Mako Shark', asset: '/app/assets/fish/akula_mako.png' },
    'Катран обыкновенный': { en: 'Spiny Dogfish', asset: '/app/assets/fish/katran_simple.png' },
    'Альбакор': { en: 'Albacore', asset: '/app/assets/fish/albakor.png' },
    'Голец арктический': { en: 'Arctic Char', asset: '/app/assets/fish/golets_arkticheskiy.png' },
    'Форель кумжа': { en: 'Brown Trout', asset: '/app/assets/fish/forel_kumzha.png' },
    'Пикша': { en: 'Haddock', asset: '/app/assets/fish/piksha.png' },
    'Тюрбо': { en: 'Turbot', asset: '/app/assets/fish/tyurbo.png' },
    'Сайра': { en: 'Pacific Saury', asset: '/app/assets/fish/sayra.png' },
    'Летучая рыба': { en: 'Flying Fish', asset: '/app/assets/fish/letuchaya_ryba.png' },
    'Рыба-луна': { en: 'Ocean Sunfish', asset: '/app/assets/fish/ryba_luna.png' },
    'Сельдяной король': { en: 'Oarfish', asset: '/app/assets/fish/seldyanoy_korol.png' },
    'Паку бурый': { en: 'Brown Pacu', asset: '/app/assets/fish/tambaki.png' },
    'Паку краснобрюхий': { en: 'Red-bellied Pacu', asset: '/app/assets/fish/paku_krasnobryuhiy.png' },
    'Паку чёрный': { en: 'Black Pacu', asset: '/app/assets/fish/paku_cherniy.png' },
    'Прохилодус красноштриховый': { en: 'Stripetail Prochilodus', asset: '/app/assets/fish/prohilodus.png' },
    'Лепоринус полосатый': { en: 'Banded Leporinus', asset: '/app/assets/fish/leporinus_polosatiy.png' },
    'Метиннис серебристый': { en: 'Silver Dollar', asset: '/app/assets/fish/metinnis_serebristiy.png' },
    'Пелядь': { en: 'Peled Whitefish', asset: '/app/assets/fish/pelyad.png' },
    'Омуль арктический': { en: 'Arctic Omul', asset: '/app/assets/fish/omul_arcticheskiy.png' },
    'Муксун': { en: 'Muksun Whitefish', asset: '/app/assets/fish/muksun.png' },
    'Анциструс обыкновенный': { en: 'Common Bristlenose', asset: '/app/assets/fish/ancistrus.png' },
    'Птеригоплихт парчовый': { en: 'Sailfin Pleco', asset: '/app/assets/fish/pterigopliht_parchoviy.png' },
    'Отоцинклюс широкополосый': { en: 'Banded Otocinclus', asset: '/app/assets/fish/otocinklyus.png' },
    'Карнегиелла мраморная': { en: 'Marbled Hatchetfish', asset: '/app/assets/fish/karnegiella_mramornaya.png' },
    'Тетра неоновая': { en: 'Neon Tetra', asset: '/app/assets/fish/tetra_neonovaya.png' },
    'Тернеция чёрная': { en: 'Black Skirt Tetra', asset: '/app/assets/fish/tetra_chernaya.png' },
    'Рыба-лист амазонская': { en: 'Amazon Leaf Fish', asset: '/app/assets/fish/ryba_list_amazonskaya.png' },
    'Арапайма': { en: 'Arapaima', asset: '/app/assets/fish/arapayma.png' },
    'Ленок': { en: 'Lenok Trout', asset: '/app/assets/fish/lenok.png' },
    'Пиранья краснобрюхая': { en: 'Red-bellied Piranha', asset: '/app/assets/fish/piranya_krasnopuzaya.png' },
    'Бикуда': { en: 'Bicuda', asset: '/app/assets/fish/bikuda.png' },
    'Угорь электрический': { en: 'Electric Eel', asset: '/app/assets/fish/ugor_elektricheskiy.png' },
    'Сом краснохвостый': { en: 'Redtail Catfish', asset: '/app/assets/fish/krasnohvostiy_som.png' },
    'Пимелодус пятнистый': { en: 'Spotted Pimelodus', asset: '/app/assets/fish/pimelodus_pyatnistiy.png' },
    'Сом веслоносый': { en: 'Paddlefish', asset: '/app/assets/fish/som_veslonosiy.png' },
    'Пираиба': { en: 'Piraiba', asset: '/app/assets/fish/piraiba.png' },
    'Дискус обыкновенный': { en: 'Common Discus', asset: '/app/assets/fish/diskus.png' },
    'Скалярия альтум': { en: 'Altum Angelfish', asset: '/app/assets/fish/skalyaria_altum.png' },
    'Скалярия обыкновенная': { en: 'Freshwater Angelfish', asset: '/app/assets/fish/skalyaria_common.png' },
    'Апистограмма Агассиза': { en: "Agassiz's Cichlid", asset: '/app/assets/fish/apistogramma_agassiza.png' },
    'Тетра кардинальная': { en: 'Cardinal Tetra', asset: '/app/assets/fish/tetra_kardinal.png' },
    'Тетра лимонная': { en: 'Lemon Tetra', asset: '/app/assets/fish/tetra_limonnaya.png' },
    'Тетра огненная': { en: 'Ember Tetra', asset: '/app/assets/fish/tetra_ognennaya.png' },
    'Тетра пингвин': { en: 'Penguin Tetra', asset: '/app/assets/fish/tetra_pingvin.png' },
    'Тетра родостомус': { en: 'Rummy-nose Tetra', asset: '/app/assets/fish/tetra_rodostomus.png' },
    'Тетра чёрный неон': { en: 'Black Neon Tetra', asset: '/app/assets/fish/tetra_black_neon.png' },
    'Коридорас панда': { en: 'Panda Cory', asset: '/app/assets/fish/koridorus_panda.png' },
    'Коридорас Штерба': { en: "Sterba's Corydoras", asset: '/app/assets/fish/koridoras_shterba.png' },
    'Татия леопардовая': { en: 'Leopard Tatia', asset: '/app/assets/fish/tatiya_leopardovaya.png' },
    'Торакатум': { en: 'Hoplo Catfish', asset: '/app/assets/fish/torakatum.png' },
    'Нанностомус трифасциатус': { en: 'Three-Stripe Pencilfish', asset: '/app/assets/fish/nannostomus.png' },
    'Нанностомус маргинатус': { en: 'Dwarf Pencilfish', asset: '/app/assets/fish/nannostomus_marginatus.png' },
    'Рамирези': { en: 'Ram Cichlid', asset: '/app/assets/fish/ramirezi.png' },
    'Аравана чёрная': { en: 'Black Arowana', asset: '/app/assets/fish/aravana_chernaya.png' },
    'Астронотус глазчатый': { en: 'Oscar Cichlid', asset: '/app/assets/fish/oskar.png' },
    'Аймара': { en: 'Aimara', asset: '/app/assets/fish/aymara.png' },
    'Псевдоплатистома тигровая': { en: 'Tiger Shovelnose', asset: '/app/assets/fish/surubin.png' },
    'Брахиплатистома тигровая': { en: 'Tiger Catfish', asset: '/app/assets/fish/brahiplatistoma_tigrovaya.png' },
    'Пиранья чёрная': { en: 'Black Piranha', asset: '/app/assets/fish/piranya_chernaya.png' },
    'Паяра': { en: 'Payara', asset: '/app/assets/fish/payara.png' },
    'Мечерот обыкновенный': { en: 'Common Freshwater Barracuda', asset: '/app/assets/fish/mecherot_common.png' },
    'Мечерот пятнистый': { en: 'Spotted Freshwater Barracuda', asset: '/app/assets/fish/mecherot_pyatnistiy.png' },
    'Гимнотус угревидный': { en: 'Banded Knifefish', asset: '/app/assets/fish/gimnotus_ugrevidniy.png' },
    'Нож-рыба чёрная': { en: 'Black Ghost Knifefish', asset: '/app/assets/fish/ryba_nozh_chernaya.png' },
    'Щучья цихлида': { en: 'Pike Cichlid', asset: '/app/assets/fish/schuchya_cihlida.png' },
    'Павлиний окунь': { en: 'Peacock Bass', asset: '/app/assets/fish/pavliniy_okun.png' },
    'Цихлазома мезонаута': { en: 'Flag Cichlid', asset: '/app/assets/fish/cihlazoma_mezonauta.png' },
    'Цихлазома северум': { en: 'Severum Cichlid', asset: '/app/assets/fish/cihlazoma_severum.png' },
    'Молочная рыба': { en: 'Milkfish', asset: '/app/assets/fish/molochnaya_ryba.png' },
    'Кефаль пятнистая': { en: 'Spotted Mullet', asset: '/app/assets/fish/kefal_pyatnistaya.png' },
    'Тиляпия мозамбикская': { en: 'Mozambique Tilapia', asset: '/app/assets/fish/tilyapiya_mozambikskaya.png' },
    'Анчоус тропический': { en: 'Tropical Anchovy', asset: '/app/assets/fish/anchous_tropicheskiy.png' },
    'Сардина индийская': { en: 'Indian Sardine', asset: '/app/assets/fish/sardina_indiyskaya.png' },
    'Сиган золотистый': { en: 'Golden Rabbitfish', asset: '/app/assets/fish/zolotistiy_shiponog.png' },
    'Бычок-пчёлка': { en: 'Bumblebee Goby', asset: '/app/assets/fish/bychok_mangroviy.png' },
    'Баррамунди': { en: 'Barramundi', asset: '/app/assets/fish/barramundi.png' },
    'Снук': { en: 'Snook', asset: '/app/assets/fish/snuk.png' },
    'Луциан мангровый': { en: 'Mangrove Snapper', asset: '/app/assets/fish/mangroviy_snapper.png' },
    'Тарпон': { en: 'Tarpon', asset: '/app/assets/fish/tarpon.png' },
    'Сом морской': { en: 'Sea Catfish', asset: '/app/assets/fish/morskoy_som.png' },
    'Сарган морской': { en: 'Needlefish', asset: '/app/assets/fish/morskoy_sargan.png' },
    'Каранкс голубой': { en: 'Blue Trevally', asset: '/app/assets/fish/goluboy_trevalli.png' },
    'Рыба-попугай': { en: 'Parrotfish', asset: '/app/assets/fish/ryba_popugay.png' },
    'Ангел императорский': { en: 'Emperor Angelfish', asset: '/app/assets/fish/angel_imperatorskiy.png' },
    'Хирург голубой': { en: 'Blue Tang', asset: '/app/assets/fish/hirurg_goluboy.png' },
    'Бабочка нитеносная': { en: 'Threadfin Butterflyfish', asset: '/app/assets/fish/babochka_klinopolosaya.png' },
    'Хризиптера синяя': { en: 'Blue Damselfish', asset: '/app/assets/fish/damsel_siniy.png' },
    'Фузилёр жёлтохвостый': { en: 'Yellowtail Fusilier', asset: '/app/assets/fish/fuziler_zheltohvostiy.png' },
    'Барабулька тропическая': { en: 'Tropical Goatfish', asset: '/app/assets/fish/barabulka_tropicheskaya.png' },
    'Барракуда большая': { en: 'Great Barracuda', asset: '/app/assets/fish/barrakuda_bolschaya.png' },
    'Конгер': { en: 'Conger Eel', asset: '/app/assets/fish/konger.png' },
    'Каранкс гигантский': { en: 'Giant Trevally', asset: '/app/assets/fish/gigantskiy_karanks.png' },
    'Пермит': { en: 'Permit', asset: '/app/assets/fish/permit.png' },
    'Альбула': { en: 'Bonefish', asset: '/app/assets/fish/kostlyavaya_ryba.png' },
    'Скумбрия испанская': { en: 'Spanish Mackerel', asset: '/app/assets/fish/ispanskaya_makrel.png' },
    'Форель коралловая': { en: 'Coral Trout', asset: '/app/assets/fish/koralloviy_grupper.png' },
    'Спинорог-титан': { en: 'Titan Triggerfish', asset: '/app/assets/fish/spinorog_titan.png' },
    'Карп кои (Кохаку)': { en: 'Koi (Kohaku)', asset: '/app/assets/fish/koi_kohaku.png' },
    'Карп кои (Тайсё Сансёку)': { en: 'Koi (Taisho Sanshoku)', asset: '/app/assets/fish/koi_taisho_sanke.png' },
    'Карп кои (Сёва Сансёку)': { en: 'Koi (Showa Sanshoku)', asset: '/app/assets/fish/koi_showa_sanshoku.png' },
    'Карп кои (Уцуримоно)': { en: 'Koi (Utsurimono)', asset: '/app/assets/fish/koi_utsurimono.png' },
    'Карп кои (Бэкко)': { en: 'Koi (Bekko)', asset: '/app/assets/fish/koi_bekko.png' },
    'Карп кои (Тантё)': { en: 'Koi (Tancho)', asset: '/app/assets/fish/koi_tancho.png' },
    'Карп кои (Асаги)': { en: 'Koi (Asagi)', asset: '/app/assets/fish/koi_asagi.png' },
    'Карп кои (Сюсуй)': { en: 'Koi (Shusui)', asset: '/app/assets/fish/koi_shusui.png' },
    'Карп кои (Коромо)': { en: 'Koi (Koromo)', asset: '/app/assets/fish/koi_koromo.png' },
    'Карп кои (Кингинрин)': { en: 'Koi (Kinginrin)', asset: '/app/assets/fish/koi_kinginrin.png' },
    'Карп кои (Каваримоно)': { en: 'Koi (Kawarimono)', asset: '/app/assets/fish/koi_kawarimono.png' },
    'Карп кои (Огон)': { en: 'Koi (Ogon)', asset: '/app/assets/fish/koi_ogon.png' },
    'Карп кои (Хикари-моёмоно)': { en: 'Koi (Hikari Moyomono)', asset: '/app/assets/fish/koi_hikari_moyomono.png' },
    'Карп кои (Госики)': { en: 'Koi (Goshiki)', asset: '/app/assets/fish/koi_goshiki.png' },
    'Карп кои (Кумонрю)': { en: 'Koi (Kumonryu)', asset: '/app/assets/fish/koi_kumonryu.png' },
    'Карп кои (Дойцу-гои)': { en: 'Koi (Doitsu-goi)', asset: '/app/assets/fish/koi_doitsu.png' },
    'Амур чёрный': { en: 'Black Amur', asset: '/app/assets/fish/cherniy_amur.png' },
    'Змееголов северный': { en: 'Northern Snakehead', asset: '/app/assets/fish/zmeegolov_severniy.png' },
    'Щука амурская': { en: 'Amur Pike', asset: '/app/assets/fish/amurskaya_schuka.png' },
    'Кристивомер': { en: 'Lake Trout', asset: '/app/assets/fish/kristivomer.png' },
    'Лосось дунайский': { en: 'Danube Salmon (Huchen)', asset: '/app/assets/fish/dunaiskiy_losos.png' },
    'Зунгаро': { en: 'Zungaro Catfish', asset: '/app/assets/fish/zungaro.png' },
    'Скат моторо': { en: 'Motoro Stingray', asset: '/app/assets/fish/skat_motoro.png' },
    'Пеленгас': { en: 'Pelingas Mullet', asset: '/app/assets/fish/pelengas.png' },
    'Вырезуб': { en: 'Black Sea Shemaya', asset: '/app/assets/fish/vyrezub.png' },
    'Кубера': { en: 'Cubera Snapper', asset: '/app/assets/fish/kubera.png' },
    'Мурена европейская': { en: 'European Moray', asset: '/app/assets/fish/murena_european.png' },
    'Мурена звёздчатая': { en: 'Starry Moray', asset: '/app/assets/fish/murena_zvezdchataya.png' },
    'Мурена гигантская': { en: 'Giant Moray', asset: '/app/assets/fish/murena_gigantskaya.png' },
    'Зубатка пятнистая': { en: 'Spotted Wolffish', asset: '/app/assets/fish/zubatka_pyatnistaya.png' },
    'Тунец желтоперый': { en: 'Yellowfin Tuna', asset: '/app/assets/fish/tunec_zeltoperiy.png' },
    'Снук чёрный': { en: 'Black Snook', asset: '/app/assets/fish/chyorniy_snuk.png' },
    'Рыба-наполеон': { en: 'Napoleon Wrasse', asset: '/app/assets/fish/ryba_napoleon.png' },
    'Рыба-клоун': { en: 'Clownfish', asset: '/app/assets/fish/ryba_kloun.png' },
    'Кефаль мангровая': { en: 'Mangrove Mullet', asset: '/app/assets/fish/kefal_mangrovaya.png' },
    'Сельдь тихоокеанская': { en: 'Pacific Herring', asset: '/app/assets/fish/seld_tihookeanskaya.png' },
    'Хромис рифовый': { en: 'Reef Chromis', asset: '/app/assets/fish/hromis_rifoviy.png' },
    'Дамсел жёлтохвостый': { en: 'Yellowtail Damselfish', asset: '/app/assets/fish/damsel_zheltohvostiy.png' },
    'Морской конёк': { en: 'Seahorse', asset: '/app/assets/fish/morskoy_konek.png' },
    'Идол мавританский': { en: 'Moorish Idol', asset: '/app/assets/fish/idol_mavritanskiy.png' },
    'Рыба-бабочка полосатая': { en: 'Striped Butterflyfish', asset: '/app/assets/fish/ryba_babochka_polosataya.png' },
    'Гобиодон голубопятнистый': { en: 'Bluespotted Goby', asset: '/app/assets/fish/gobiodon_golubopyatnistiy.png' },
    'Сиган коричневопятнистый': { en: 'Brown-spotted Rabbitfish', asset: '/app/assets/fish/sigan_korichnevopyatnistiy.png' },
    'Хирург полосатый': { en: 'Striped Surgeonfish', asset: '/app/assets/fish/hirurg_polosatiy.png' },
    'Луциан серебристо-пятнистый': { en: 'Silverspot Snapper', asset: '/app/assets/fish/lucian_serebristo-pyatnistiy.png' },
    'Скорпена бородатая': { en: 'Bearded Scorpionfish', asset: '/app/assets/fish/skorpena_borodataya.png' },
    'Барракуда полосатая': { en: 'Striped Barracuda', asset: '/app/assets/fish/barrakuda_polosataya.png' },
    'Каранкс шестиполосый': { en: 'Sixband Trevally', asset: '/app/assets/fish/karanks_polosatiy.png' },
    'Группер леопардовый коралловый': { en: 'Leopard Coral Grouper', asset: '/app/assets/fish/grupper_leopardoviy_koralloviy.png' },
    'Иглорыл-агухон': { en: 'Agujon Needlefish', asset: '/app/assets/fish/igloryl-aguhon.png' },
    'Акула рифовая чёрнопёрая': { en: 'Blacktip Reef Shark', asset: '/app/assets/fish/akula_rifovaya_chernoperaya.png' },
    'Губан-чистильщик': { en: 'Cleaner Wrasse', asset: '/app/assets/fish/guban-chistilschik.png' },
    'Сержант-майор атлантический': { en: 'Atlantic Sergeant Major', asset: '/app/assets/fish/sergant-major_atlanticheskiy.png' },
    'Грамма королевская': { en: 'Royal Gramma', asset: '/app/assets/fish/gramma_korolevskaya.png' },
    'Ангел королевский': { en: 'Queen Angelfish', asset: '/app/assets/fish/angel_korolevskiy.png' },
    'Мандариновая рыба': { en: 'Mandarin Dragonet', asset: '/app/assets/fish/mandarinivaya_ryba.png' },
    'Крылатка зебровая': { en: 'Zebra Lionfish', asset: '/app/assets/fish/krylaka_zebrovaya.png' },
    'Рыба-флейта': { en: 'Trumpetfish', asset: '/app/assets/fish/ryba-fleita.png' },
  };
  const FISH_TRANSLATIONS = {};
  const FISH_IMG = {};
  Object.entries(FISH_INFO).forEach(([ru, { en, asset }]) => {
    if (en) FISH_TRANSLATIONS[ru] = en;
    if (asset) {
      FISH_IMG[ru] = asset;
      if (en) FISH_IMG[en] = asset;
    }
  });
  const translateLure = (n, lang = document.documentElement.lang) => {
    const info = getLureInfo(n);
    if (!info) return n;
    return lang === 'en' ? info.enName : info.ruName;
  };
  const lureDescriptionText = (n, lang = document.documentElement.lang) => {
    const info = getLureInfo(n);
    if (!info) return '';
    return lang === 'en' ? info.enDescription : info.ruDescription;
  };

  const ACHIEVEMENT_LEVEL_LABELS = {
    ru: ['Нет уровня', 'Бронза', 'Серебро', 'Золото', 'Платина'],
    en: ['No tier', 'Bronze', 'Silver', 'Gold', 'Platinum'],
  };
  const achievementLevelLabel = (index, lang = document.documentElement.lang) => {
    const labels = ACHIEVEMENT_LEVEL_LABELS[lang] || ACHIEVEMENT_LEVEL_LABELS.ru;
    const safeIndex = Math.min(index, labels.length - 1);
    return labels[safeIndex] || labels[0];
  };
  const ACHIEVEMENT_IMAGES = {
    0: '/app/assets/achievements/achievement_none.svg',
    1: '/app/assets/achievements/achievement_bronze.svg',
    2: '/app/assets/achievements/achievement_silver.svg',
    3: '/app/assets/achievements/achievement_gold.svg',
    4: '/app/assets/achievements/achievement_platinum.svg',
  };
  const ACHIEVEMENT_ART = {
    simple_fisher: {
      0: '/app/assets/achievements/simple_fisher_grey.png',
      1: '/app/assets/achievements/simple_fisher_bronze.png',
      2: '/app/assets/achievements/simple_fisher_silver.png',
      3: '/app/assets/achievements/simple_fisher_gold.png',
      4: '/app/assets/achievements/simple_fisher_platinum.png',
    },
    uncommon_fisher: {
      0: '/app/assets/achievements/uncommon_fisher_grey.png',
      1: '/app/assets/achievements/uncommon_fisher_bronze.png',
      2: '/app/assets/achievements/uncommon_fisher_silver.png',
      3: '/app/assets/achievements/uncommon_fisher_gold.png',
      4: '/app/assets/achievements/uncommon_fisher_platinum.png',
    },
    rare_fisher: {
      0: '/app/assets/achievements/rare_fisher_grey.png',
      1: '/app/assets/achievements/rare_fisher_bronze.png',
      2: '/app/assets/achievements/rare_fisher_silver.png',
      3: '/app/assets/achievements/rare_fisher_gold.png',
      4: '/app/assets/achievements/rare_fisher_platinum.png',
    },
    epic_fisher: {
      0: '/app/assets/achievements/epic_fisher_grey.png',
      1: '/app/assets/achievements/epic_fisher_bronze.png',
      2: '/app/assets/achievements/epic_fisher_silver.png',
      3: '/app/assets/achievements/epic_fisher_gold.png',
      4: '/app/assets/achievements/epic_fisher_platinum.png',
    },
    mythic_fisher: {
      0: '/app/assets/achievements/mythic_fisher_grey.png',
      1: '/app/assets/achievements/mythic_fisher_bronze.png',
      2: '/app/assets/achievements/mythic_fisher_silver.png',
      3: '/app/assets/achievements/mythic_fisher_gold.png',
      4: '/app/assets/achievements/mythic_fisher_platinum.png',
    },
    legendary_fisher: {
      0: '/app/assets/achievements/legendary_fisher_grey.png',
      1: '/app/assets/achievements/legendary_fisher_bronze.png',
      2: '/app/assets/achievements/legendary_fisher_silver.png',
      3: '/app/assets/achievements/legendary_fisher_gold.png',
      4: '/app/assets/achievements/legendary_fisher_platinum.png',
    },
    traveler: {
      0: '/app/assets/achievements/traveler_grey.png',
      1: '/app/assets/achievements/traveler_bronze.png',
      2: '/app/assets/achievements/traveler_silver.png',
      3: '/app/assets/achievements/traveler_gold.png',
      4: '/app/assets/achievements/traveler_platinum.png',
    },
    trophy_hunter: {
      0: '/app/assets/achievements/trophy_hunter_grey.png',
      1: '/app/assets/achievements/trophy_hunter_bronze.png',
      2: '/app/assets/achievements/trophy_hunter_silver.png',
      3: '/app/assets/achievements/trophy_hunter_gold.png',
      4: '/app/assets/achievements/trophy_hunter_platinum.png',
    },
    koi_collector: {
      0: '/app/assets/achievements/koi_collector_grey.png',
      1: '/app/assets/achievements/koi_collector_bronze.png',
      2: '/app/assets/achievements/koi_collector_silver.png',
      3: '/app/assets/achievements/koi_collector_gold.png',
      4: '/app/assets/achievements/koi_collector_platinum.png',
    },
  };
  const achievementImage = (code, level = 0) => {
    const safeLevel = Math.max(0, Math.min(4, Number(level) || 0));
    const key = typeof code === 'string' ? code.toLowerCase() : '';
    const art = ACHIEVEMENT_ART[key];
    if (art && art[safeLevel]) return art[safeLevel];
    return ACHIEVEMENT_IMAGES[safeLevel] || ACHIEVEMENT_IMAGES[0];
  };
  window.ACHIEVEMENT_LEVEL_LABELS = ACHIEVEMENT_LEVEL_LABELS;
  window.achievementLevelLabel = achievementLevelLabel;
  window.ACHIEVEMENT_IMAGES = ACHIEVEMENT_IMAGES;
  window.ACHIEVEMENT_ART = ACHIEVEMENT_ART;
  window.achievementImage = achievementImage;

  const STRINGS = {
    ru: {
      location: 'Локация',
      baits: 'Приманки',
      rod: 'Удочка',
      rods: 'Удочки',
      total: 'Всего',
      today: 'Сегодня',
      coins: 'Монеты',
      yesterday: 'Вчера',
      lastWeek: 'За неделю',
      lastMonth: 'За месяц',
      lastYear: 'За год',
      allTime: 'За всё время',
      kg: 'кг',
      locations: 'Локации',
      allLocations: 'Все локации',
      unlocked: 'Открыто',
      requiresKg: kg => `Требуется ${kg} кг`,
      rodLockedStars: ({ kg, stars }) => `Требуется ${kg} кг или ${stars}`,
      current: 'Текущий',
      rodBonusFreshPeaceful: '−50% шанс побега пресноводных мирных рыб',
      rodBonusFreshPredator: '−50% шанс побега пресноводных хищных рыб',
      rodBonusSaltPeaceful: '−50% шанс побега морских мирных рыб',
      rodBonusSaltPredator: '−50% шанс побега морских хищных рыб',
      rodNoBonus: 'Бонусов нет',
      rodBonusLine: bonus => `Бонус: ${bonus}`,
      useRod: 'Выбрать',
      unlockRodFor: stars => `🔓 Разблокировать за ${stars}`,
      rodAlreadyUnlocked: 'Эта удочка уже разблокирована.',
      upcoming: 'Предстоящие',
      past: 'Прошедшие',
      lures: 'Приманки',
      qty: qty => `${qty} шт.`,
      nickname: 'Никнейм',
      cancel: 'Отмена',
      save: 'Сохранить',
      close: 'Закрыть',
      authRequired: 'Требуется авторизация. Запустите игру через кнопку бота.',
      loadFailed: 'Не удалось загрузить',
      loadProfileFailed: 'Не удалось загрузить профиль',
      purchaseFailed: 'Не удалось купить',
      buyForCoins: coins => `🪙 Купить за ${Number(coins).toLocaleString('ru-RU')} монет`,
      confirmCoinPurchaseTitle: 'Покупка за монеты',
      confirmCoinPurchaseText: ({ name, price }) => `Купить «${name}» за ${price} монет?`,
      confirmCoinPurchaseButton: price => `🪙 Купить за ${price}`,
      notEnoughCoinsTitle: 'Недостаточно монет',
      notEnoughCoins: 'Недостаточно монет.',
      locationLocked: 'Локация закрыта',
      changeLocationFailed: 'Не удалось сменить локацию',
      castFailed: 'Не удалось забросить',
      selectBaitFailed: 'Не удалось выбрать приманку',
      selectRodFailed: 'Не удалось выбрать удочку',
      rodLocked: 'Удочка ещё недоступна',
      rodCasting: 'Нельзя менять удочку во время заброса',
      rodUnavailable: 'Эта удочка недоступна',
      loading: 'Загрузка...',
      fish: 'Рыбы',
      rarityFilterLabel: 'Редкость',
      allRarities: 'Все',
      reachKg: kg => `Набери ${kg} кг, чтобы открыть`,
      fishes: 'Рыбы:',
      baitsLabel: 'Приманки:',
      locationsLabel: 'Локации:',
      catchToLearn: 'Поймай эту рыбу, чтобы узнать больше',
      noData: 'Нет данных',
      recentCatches: 'Последние уловы',
      emptyCatches: 'Пока пусто — сделай первый заброс!',
      sendToMe: 'Отправить себе',
      sendingCatch: 'Отправка…',
      catchSent: 'Карточка отправлена в личные сообщения.',
      catchSendFailed: 'Не удалось отправить карточку.',
      tournamentsSoon: 'Турниры в разработке',
      shopEmpty: 'Магазин пуст',
      shopDiscountInfo: ({ percent }) => `скидка ${percent}%`,
      selectFish: 'Выбери рыбу',
      castRod: 'Забросить удочку',
      hook: 'Подсечь!',
      autoCast: 'Автозаброс',
      castCooldown: 'Готовим снасти…',
      autoFishUntil: d => `Активно до ${d}`,
      autoFishExtend: 'Продлить ещё на месяц',
      tapButton: 'Тащи!!!',
      tapPrompt: goal => `Тапай ${goal} раз!`,
      tapCountdown: secs => `Осталось ${secs}с`,
      tapCount: ({ count, goal }) => `${count}/${goal}`,
      waitingBite: 'Ожидаем поклёвку…',
      catch: 'Улов!',
      new: 'Новая!',
      locationLabel: 'Локация:',
      coinsEarned: coins => `+${coins} монет`,
      coinsCapReached: 'Монеты не начислены из-за лимита',
      newLocation: 'Открыта новая локация:',
      newRod: 'Открыта новая удочка:',
      newRodPlural: 'Открыты новые удочки:',
      dailyTaken: 'Ежедневные приманки уже забраны.',
      noBaits: 'Нет приманок. Забери ежедневные или купи в магазине.',
      castOften: 'Слишком часто забрасываешь. Подожди пару секунд.',
      alreadyCasting: 'Заброс уже выполняется.',
      fishEscaped: 'Рыба сорвалась!',
      wrongLure: 'Неверная приманка для этой локации',
      noSuitableFish: 'Нет подходящей рыбы на эту приманку.',
      fishing: 'Рыбалка',
      tournaments: 'Турниры',
      you: 'Ты',
      prizes: 'Призы',
      ratings: 'Рейтинги',
      guide: 'Справочник',
      achievements: 'Достижения',
      achievementsTab: 'Достижения',
      shop: 'Магазин',
      menu: 'Меню',
      achievementRewardsAvailable: 'Доступна награда',
      invite: 'Пригласить друзей',
      refBonusInfo: name => `Пригласите друзей — вы оба получите ${name}. За покупки приглашённых друзей вы получаете 25% приманок и неделю авто-рыбалки за их подписку.`,
      referralBonus: 'Ваш друг присоединился по вашей ссылке и вы получили набор:',
      referralPurchaseBonus: 'Ваш друг купил что-то в магазине и вам положен бонус:',
      welcomeBonus: 'Вы присоединились по реферальной ссылке и получили приветственный набор:',
      autofishWeek: 'Автоловля (неделя)',
      myRefLink: 'Моя реферальная ссылка',
      copyLink: 'Скопировать ссылку',
      generateLink: 'Сгеерировать новую',
      invitedFriends: 'Приглашённые',
      noInvited: 'Пока никого',
      gift: 'Подарок',
      personal: 'Личные',
      global: 'Глобальные',
      species: 'Рыбы',
      allFish: 'Все рыбы',
      smallest: 'Самая маленькая рыба',
      largest: 'Самая большая рыба',
      count: 'Количество рыбы',
      total_weight: 'Суммарный вес рыбы',
      fishLabel: 'Рыба:',
      anyFish: 'Любая рыба',
      fishWord: 'рыба',
      anyLocation: 'Любая локация',
      conditionsLabel: 'Условия:',
      prizePlacesLabel: 'Призовых мест:',
      day: 'День',
      tapToClaim: 'Нажмите, чтобы получить',
      prizeCongrats: rank => rank > 0
        ? `Поздравляем! Вы заняли ${rank} место в турнире. Ваш приз:`
        : 'Поздравляем! Ваши призы за рейтинги готовы.',
      hintFastBite: 'Чем быстрее реагируешь на клев, тем выше шанс поймать рыбу',
      hintWaitRare: 'Чем дольше ждешь клева, тем выше вероятность поймать редкую рыбу',
      hintTapPrize: 'Тапни на карточку в турнирной таблице, чтобы увидеть возможный приз',
      hintAutoCatch: 'С подпиской робот позаботится о вашем улове, если вы не успеете вовремя выловить рыбу',
      hintNoCloseRod: 'Не закрывай игру с заброшенной удочкой чтобы не потерять приманку',
      achievementsEmpty: 'Пока нет достижений — продолжайте ловить рыбу!',
      achievementProgress: ({ progress, target }) => `${progress}/${target}`,
      achievementProgressLabel: ({ progress, target }) => `Прогресс ${progress}/${target}`,
      achievementLocked: 'Ловите рыбу, чтобы получить награду.',
      achievementInProgress: 'Продолжайте ловить рыбу.',
      achievementClaim: 'Забрать награду',
      achievementClaimed: 'Награда за достижение получена!',
      achievementsUnavailable: 'Не удалось загрузить достижения',
      achievementsRefresh: 'Обновить',
      achievementUnlockedLine: ({ name, level }) => `Открыто достижение «${name}»: ${level}`,
      achievementRewardTitle: 'Награды за достижение',
      achievementRewardCoins: coins => `🪙 +${coins} монет`,
      achievementRewardPack: ({ name, qty }) => `${name}${qty > 1 ? ` x${qty}` : ''}`,
    },
    en: {
      location: 'Location',
      baits: 'Baits',
      rod: 'Rod',
      rods: 'Rods',
      total: 'Total',
      today: 'Today',
      coins: 'Coins',
      yesterday: 'Yesterday',
      lastWeek: 'Last week',
      lastMonth: 'Last month',
      lastYear: 'Last year',
      allTime: 'All time',
      kg: 'kg',
      locations: 'Locations',
      allLocations: 'All locations',
      unlocked: 'Unlocked',
      requiresKg: kg => `${kg} kg required`,
      rodLockedStars: ({ kg, stars }) => `${kg} kg required or ${stars}`,
      current: 'Current',
      rodBonusFreshPeaceful: '50% less escape chance for freshwater peaceful fish',
      rodBonusFreshPredator: '50% less escape chance for freshwater predator fish',
      rodBonusSaltPeaceful: '50% less escape chance for saltwater peaceful fish',
      rodBonusSaltPredator: '50% less escape chance for saltwater predator fish',
      rodNoBonus: 'No bonus',
      rodBonusLine: bonus => `Bonus: ${bonus}`,
      useRod: 'Use rod',
      unlockRodFor: stars => `🔓 Unlock for ${stars}`,
      rodAlreadyUnlocked: 'You already unlocked this rod.',
      upcoming: 'Upcoming',
      past: 'Past',
      lures: 'Baits',
      qty: qty => `${qty} pcs`,
      nickname: 'Nickname',
      cancel: 'Cancel',
      save: 'Save',
      close: 'Close',
      authRequired: 'Authorization required. Launch the game via the bot button.',
      loadFailed: 'Failed to load',
      loadProfileFailed: 'Failed to load profile',
      purchaseFailed: 'Purchase failed',
      buyForCoins: coins => `🪙 Buy for ${Number(coins).toLocaleString('en-US')} coins`,
      confirmCoinPurchaseTitle: 'Buy with coins',
      confirmCoinPurchaseText: ({ name, price }) => `Buy "${name}" for ${price} coins?`,
      confirmCoinPurchaseButton: price => `🪙 Buy for ${price}`,
      notEnoughCoinsTitle: 'Not enough coins',
      notEnoughCoins: 'Not enough coins.',
      locationLocked: 'Location locked',
      changeLocationFailed: 'Failed to change location',
      castFailed: 'Failed to cast',
      selectBaitFailed: 'Failed to select bait',
      selectRodFailed: 'Failed to select rod',
      rodLocked: 'This rod is locked',
      rodCasting: 'Cannot change rod while casting',
      rodUnavailable: 'This rod is not available',
      loading: 'Loading...',
      fish: 'Fish',
      rarityFilterLabel: 'Rarity',
      allRarities: 'All',
      reachKg: kg => `Reach ${kg} kg to unlock`,
      fishes: 'Fish:',
      baitsLabel: 'Baits:',
      locationsLabel: 'Locations:',
      catchToLearn: 'Catch this fish to learn more',
      noData: 'No data',
      recentCatches: 'Recent catches',
      emptyCatches: 'No catches yet — make your first cast!',
      sendToMe: 'Send to myself',
      sendingCatch: 'Sending…',
      catchSent: 'Card sent to your direct messages.',
      catchSendFailed: 'Failed to send the card.',
      tournamentsSoon: 'Tournaments coming soon',
      shopEmpty: 'Shop is empty',
      shopDiscountInfo: ({ percent }) => `discount ${percent}%`,
      selectFish: 'Select fish',
      castRod: 'Cast the rod',
      hook: 'Hook!',
      autoCast: 'Auto cast',
      castCooldown: 'Preparing tackle…',
      autoFishUntil: d => `Active until ${d}`,
      autoFishExtend: 'Extend for another month',
      tapButton: 'Reel it in!!!',
      tapPrompt: goal => `Tap ${goal} times!`,
      tapCountdown: secs => `${secs}s left`,
      tapCount: ({ count, goal }) => `${count}/${goal}`,
      waitingBite: 'Waiting for a bite…',
      catch: 'Catch!',
      new: 'New!',
      locationLabel: 'Location:',
      coinsEarned: coins => `+${coins} coin${coins === 1 ? '' : 's'}`,
      coinsCapReached: 'No coins due to daily cap',
      newLocation: 'New location unlocked:',
      newRod: 'New rod unlocked:',
      newRodPlural: 'New rods unlocked:',
      dailyTaken: 'Daily baits already claimed.',
      noBaits: 'No baits. Claim daily ones or buy in the shop.',
      castOften: 'Casting too often. Wait a few seconds.',
      alreadyCasting: 'Cast in progress already.',
      fishEscaped: 'Fish escaped!',
      wrongLure: 'Wrong bait for this location',
      noSuitableFish: 'No suitable fish for this bait.',
      fishing: 'Fishing',
      tournaments: 'Tournaments',
      you: 'You',
      prizes: 'Prizes',
      ratings: 'Ratings',
      guide: 'Guide',
      achievements: 'Achievements',
      achievementsTab: 'Achievements',
      shop: 'Shop',
      menu: 'Menu',
      achievementRewardsAvailable: 'Reward available',
      invite: 'Invite friends',
      refBonusInfo: name => `Invite friends — you both get ${name}. When invited friends make purchases, you get 25% of their baits and a week of auto-fish for each auto-fish pack.`,
      referralBonus: 'Your friend joined via your link and you received a pack:',
      referralPurchaseBonus: 'Your friend bought something in the shop and you received a bonus:',
      welcomeBonus: 'You joined via a referral link and received a welcome pack:',
      autofishWeek: 'Auto Catch (week)',
      myRefLink: 'My referral link',
      copyLink: 'Copy link',
      generateLink: 'Generate new',
      invitedFriends: 'Invited friends',
      noInvited: 'No invited friends',
      gift: 'Gift',
      personal: 'Personal',
      global: 'Global',
      species: 'Fish',
      allFish: 'All fish',
      smallest: 'Smallest fish',
      largest: 'Largest fish',
      count: 'Fish count',
      total_weight: 'Total weight',
      fishLabel: 'Fish:',
      anyFish: 'Any fish',
      fishWord: 'fish',
      anyLocation: 'Any location',
      conditionsLabel: 'Conditions:',
      prizePlacesLabel: 'Prize places:',
      day: 'Day',
      tapToClaim: 'Tap to claim',
      prizeCongrats: rank => rank > 0
        ? `Congratulations! You placed ${rank} in the tournament. Your prize:`
        : 'Congratulations! Your rating prizes are ready.',
      hintFastBite: 'React to bites quickly for a better catch chance',
      hintWaitRare: 'Waiting longer for a bite increases the odds of rare fish',
      hintTapPrize: 'Tap a leaderboard card to see its potential prize',
      hintAutoCatch: 'With a subscription, the robot will secure your catch if you fail to reel in the fish in time',
      hintNoCloseRod: "Don't close the game with your rod cast or you'll lose the bait",
      achievementsEmpty: 'No achievements yet — keep fishing!',
      achievementProgress: ({ progress, target }) => `${progress}/${target}`,
      achievementProgressLabel: ({ progress, target }) => `Progress ${progress}/${target}`,
      achievementLocked: 'Keep fishing to unlock rewards.',
      achievementInProgress: 'Keep fishing for the next reward.',
      achievementClaim: 'Claim reward',
      achievementClaimed: 'Achievement reward claimed!',
      achievementsUnavailable: 'Failed to load achievements',
      achievementsRefresh: 'Refresh',
      achievementUnlockedLine: ({ name, level }) => `Achievement unlocked: ${name} — ${level}`,
      achievementRewardTitle: 'Achievement rewards',
      achievementRewardCoins: coins => `🪙 +${coins} coins`,
      achievementRewardPack: ({ name, qty }) => `${name}${qty > 1 ? ` x${qty}` : ''}`,
    }
  };
  function makeT(lang) {
    return (key, p) => {
      const dict = STRINGS[lang] || STRINGS.ru;
      const value = dict[key];
      return typeof value === 'function' ? value(p) : (value || key);
    };
  }
  const tgLang = window.Telegram?.WebApp?.initDataUnsafe?.user?.language_code?.slice(0, 2);
  const savedLang = (() => { try { return localStorage.getItem('lang'); } catch (e) { return null; } })();
  const initLang = (savedLang || tgLang || 'en') === 'ru' ? 'ru' : 'en';
  let t = makeT(initLang);
  window.t = t;
  document.documentElement.lang = initLang;

  const easeOutCubic = t => 1 - Math.pow(1 - t, 3);

  function useResizeObserver(ref) {
    const [size, setSize] = React.useState({ w: 0, h: 0 });
    React.useEffect(() => {
      const node = ref.current;
      if (!node) return;
      const updateFromRect = rect => {
        const w = rect?.width || 0;
        const h = rect?.height || 0;
        setSize(prev => (prev.w === w && prev.h === h) ? prev : { w, h });
      };
      updateFromRect(node.getBoundingClientRect());
      const ro = new ResizeObserver(entries => {
        for (const entry of entries) {
          const rect = entry?.contentRect;
          if (rect) updateFromRect(rect);
        }
      });
      ro.observe(node);
      return () => ro.disconnect();
    }, [ref]);
    return size;
  }

  window.rarityColors = rarityColors;
  window.rarityNames = rarityNames;
  window.lureColor = lureColor;
  window.getLureIcon = getLureIcon;
  window.LOCATION_BG = LOCATION_BG;
  window.LOCATION_BG_BY_NAME = LOCATION_BG_BY_NAME;
  window.getLocationBackground = getLocationBackground;
  window.ROD_IMAGES = ROD_IMAGES;
  window.ROD_IMG = ROD_IMG;
  window.ROD_IMG_SIZE = ROD_IMG_SIZE;
  window.ROD_TIP_ANCHOR = ROD_TIP_ANCHOR;
  window.ROD_TIP_ANCHORS = ROD_TIP_ANCHORS;
  window.ROD_LINE_POINTS = ROD_LINE_POINTS;
  window.ROD_BASE_ANCHOR = ROD_BASE_ANCHOR;
  window.ROD_SIZE_MULT = ROD_SIZE_MULT;
  window.ROD_BASE_X_FRACTION = ROD_BASE_X_FRACTION;
  window.TAP_CHALLENGE_GOAL = TAP_CHALLENGE_GOAL;
  window.TAP_CHALLENGE_DURATION_MS = TAP_CHALLENGE_DURATION_MS;
  window.CAST_READY_DELAY_MS = CAST_READY_DELAY_MS;
  window.FISH_IMG = FISH_IMG;
  window.translateLure = translateLure;
  window.lureDescriptionText = lureDescriptionText;
  window.makeT = makeT;
  window.initLang = initLang;
  window.easeOutCubic = easeOutCubic;
  window.useResizeObserver = useResizeObserver;
})();
