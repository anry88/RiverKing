(() => {
  const tg = window.Telegram?.WebApp;
  window.BOBBER_ICON = window.BOBBER_ICON || '/app/assets/menu/bobber.png';

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

  const rarityColors = { uncommon: 'text-green-400', rare: 'text-blue-400', epic: 'text-purple-400', legendary: 'text-yellow-400' };
  const rarityNames = {
    ru: { common: 'Простая', uncommon: 'Необычная', rare: 'Редкая', epic: 'Эпическая', legendary: 'Легендарная' },
    en: { common: 'Common', uncommon: 'Uncommon', rare: 'Rare', epic: 'Epic', legendary: 'Legendary' }
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
    if (typeof lure === 'object'){
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
    2: '/app/assets/backgrounds/river.png',
    3: '/app/assets/backgrounds/lake.png',
    4: '/app/assets/backgrounds/swamp.png',
    5: '/app/assets/backgrounds/mountain_river.png',
    6: '/app/assets/backgrounds/reservoir.png',
    7: '/app/assets/backgrounds/river_delta.png',
    8: '/app/assets/backgrounds/sea_coast.png',
    9: '/app/assets/backgrounds/fjord.png',
    10: '/app/assets/backgrounds/open_ocean.png',
    11: '/app/assets/backgrounds/amazon_riverbed.png',
    12: '/app/assets/backgrounds/flooded_forest.png',
    13: '/app/assets/backgrounds/mangroves.png',
    14: '/app/assets/backgrounds/coral_flats.png',
  };

  const normalizeLocationName = value => {
    if (!value && value !== 0) return '';
    return String(value).trim().toLowerCase();
  };

  const LOCATION_NAMES = {
    1: ['Пруд', 'Pond'],
    2: ['Река', 'River'],
    3: ['Озеро', 'Lake'],
    4: ['Болото', 'Swamp'],
    5: ['Горная река', 'Mountain River'],
    6: ['Водохранилище', 'Reservoir'],
    7: ['Дельта реки', 'River Delta'],
    8: ['Прибрежье моря', 'Sea Coast'],
    9: ['Фьорд', 'Fjord'],
    10: ['Открытый океан', 'Open Ocean'],
    11: ['Русло Амазонки', 'Amazon Riverbed'],
    12: ['Игапо, затопленный лес', 'Igapo Flooded Forest'],
    13: ['Мангровые заросли', 'Mangroves'],
    14: ['Коралловые отмели', 'Coral Flats'],
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
  const ROD_TIP_ANCHOR_DEFAULT = { x: 0.07878, y: 0.04785 };
  const ROD_CONFIG = {
    spark: {
      image: '/app/assets/rods/yellow_rod.png',
      tipAnchor: ROD_TIP_ANCHOR_DEFAULT,
    },
    dew: {
      image: '/app/assets/rods/green_rod.png',
      tipAnchor: { x: 0.13607, y: 0.06641 },
    },
    stream: {
      image: '/app/assets/rods/blue_rod.png',
      tipAnchor: { x: 0.16276, y: 0.07422 },
    },
    abyss: {
      image: '/app/assets/rods/black_rod.png',
      tipAnchor: { x: 0.14844, y: 0.04688 },
    },
    storm: {
      image: '/app/assets/rods/silver_rod.png',
      tipAnchor: { x: 0.13411, y: 0.07520 },
    },
    default: {
      image: '/app/assets/rods/yellow_rod.png',
      tipAnchor: ROD_TIP_ANCHOR_DEFAULT,
    },
  };
  const ROD_IMAGES = Object.fromEntries(Object.entries(ROD_CONFIG).map(([code, cfg]) => [code, cfg.image]));
  const ROD_TIP_ANCHORS = Object.fromEntries(Object.entries(ROD_CONFIG).map(([code, cfg]) => [code, cfg.tipAnchor]));
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
    'Сом': { en: 'Catfish', asset: '/app/assets/fish/som.png' },
    'Осётр': { en: 'Sturgeon', asset: '/app/assets/fish/osetr.png' },
    'Уклейка': { en: 'Bleak', asset: '/app/assets/fish/ukleyka.png' },
    'Линь': { en: 'Tench', asset: '/app/assets/fish/lin.png' },
    'Ротан': { en: 'Rotan', asset: '/app/assets/fish/rotan.png' },
    'Судак': { en: 'Zander', asset: '/app/assets/fish/sudak.png' },
    'Чехонь': { en: 'Sabrefish', asset: '/app/assets/fish/chehon.png' },
    'Хариус': { en: 'Grayling', asset: '/app/assets/fish/harius.png' },
    'Форель ручьевая': { en: 'Brook Trout', asset: '/app/assets/fish/forel_ruchevaya.png' },
    'Таймень': { en: 'Taimen', asset: '/app/assets/fish/taymen.png' },
    'Налим': { en: 'Burbot', asset: '/app/assets/fish/nalim.png' },
    'Сиг': { en: 'Whitefish', asset: '/app/assets/fish/sig.png' },
    'Голавль': { en: 'Chub', asset: '/app/assets/fish/golavl.png' },
    'Жерех': { en: 'Asp', asset: '/app/assets/fish/zhereh.png' },
    'Толстолобик': { en: 'Bighead Carp', asset: '/app/assets/fish/tolstolobik.png' },
    'Белый амур': { en: 'Grass Carp', asset: '/app/assets/fish/beliy_amur.png' },
    'Угорь европейский': { en: 'European Eel', asset: '/app/assets/fish/ugor_evropeyskiy.png' },
    'Стерлядь': { en: 'Sterlet', asset: '/app/assets/fish/sterlyad.png' },
    'Кефаль': { en: 'Mullet', asset: '/app/assets/fish/kefal.png' },
    'Камбала': { en: 'Flounder', asset: '/app/assets/fish/kambala.png' },
    'Сельдь': { en: 'Herring', asset: '/app/assets/fish/seld.png' },
    'Ставрида': { en: 'Horse Mackerel', asset: '/app/assets/fish/stavrida.png' },
    'Треска': { en: 'Cod', asset: '/app/assets/fish/treska.png' },
    'Сайда': { en: 'Pollock', asset: '/app/assets/fish/sayda.png' },
    'Морская форель': { en: 'Sea Trout', asset: '/app/assets/fish/morskaya_forel.png' },
    'Палтус': { en: 'Halibut', asset: '/app/assets/fish/paltus.png' },
    'Корюшка': { en: 'Smelt', asset: '/app/assets/fish/koryushka.png' },
    'Лосось атлантический': { en: 'Atlantic Salmon', asset: '/app/assets/fish/losos_atlanticheskiy.png' },
    'Лаврак': { en: 'Sea Bass', asset: '/app/assets/fish/lavrak.png' },
    'Скумбрия атлантическая': { en: 'Atlantic Mackerel', asset: '/app/assets/fish/skumbriya_atlanticheskaya.png' },
    'Белуга': { en: 'Beluga', asset: '/app/assets/fish/beluga.png' },
    'Ёрш': { en: 'Ruffe', asset: '/app/assets/fish/yorsh.png' },
    'Пескарь': { en: 'Gudgeon', asset: '/app/assets/fish/peskar.png' },
    'Густера': { en: 'Blue Bream', asset: '/app/assets/fish/gustera.png' },
    'Краснопёрка': { en: 'Rudd', asset: '/app/assets/fish/krasnopyorka.png' },
    'Елец': { en: 'Dace', asset: '/app/assets/fish/elets.png' },
    'Верхоплавка': { en: 'Topmouth Gudgeon', asset: '/app/assets/fish/verhoplavka.png' },
    'Гольян': { en: 'Minnow', asset: '/app/assets/fish/golyan.png' },
    'Язь': { en: 'Ide', asset: '/app/assets/fish/yaz.png' },
    'Бычок': { en: 'Goby', asset: '/app/assets/fish/bychyok.png' },
    'Килька': { en: 'Sprat', asset: '/app/assets/fish/kilka.png' },
    'Мойва': { en: 'Capelin', asset: '/app/assets/fish/mojva.png' },
    'Сардина': { en: 'Sardine', asset: '/app/assets/fish/sardina.png' },
    'Анчоус': { en: 'Anchovy', asset: '/app/assets/fish/anchous.png' },
    'Дорадо': { en: 'Dorado', asset: '/app/assets/fish/dorado.png' },
    'Ваху': { en: 'Wahoo', asset: '/app/assets/fish/vahu.png' },
    'Парусник': { en: 'Sailfish', asset: '/app/assets/fish/parusnik.png' },
    'Рыба-меч': { en: 'Swordfish', asset: '/app/assets/fish/ryba_mech.png' },
    'Марлин синий': { en: 'Blue Marlin', asset: '/app/assets/fish/marlin_siniy.png' },
    'Тунец синеперый': { en: 'Bluefin Tuna', asset: '/app/assets/fish/tunets_sineperiy.png' },
    'Акула мако': { en: 'Mako Shark', asset: '/app/assets/fish/akula_mako.png' },
    'Альбакор': { en: 'Albacore', asset: '/app/assets/fish/albakor.png' },
    'Голец арктический': { en: 'Arctic Char', asset: '/app/assets/fish/golets_arkticheskiy.png' },
    'Форель кумжа': { en: 'Brown Trout', asset: '/app/assets/fish/forel_kumzha.png' },
    'Пикша': { en: 'Haddock', asset: '/app/assets/fish/piksha.png' },
    'Тюрбо': { en: 'Turbot', asset: '/app/assets/fish/tyurbo.png' },
    'Сайра': { en: 'Pacific Saury', asset: '/app/assets/fish/sayra.png' },
    'Летучая рыба': { en: 'Flying Fish', asset: '/app/assets/fish/letuchaya_ryba.png' },
    'Рыба-луна': { en: 'Ocean Sunfish', asset: '/app/assets/fish/ryba_luna.png' },
    'Сельдяной король': { en: 'Oarfish', asset: '/app/assets/fish/seldyanoy_korol.png' },
    'Тамбаки': { en: 'Tambaqui', asset: '/app/assets/fish/tambaki.png' },
    'Паку чёрный': { en: 'Black Pacu', asset: '/app/assets/fish/paku_cherniy.png' },
    'Прохилодус': { en: 'Prochilodus', asset: '/app/assets/fish/prohilodus.png' },
    'Анциструс': { en: 'Ancistrus', asset: '/app/assets/fish/ancistrus.png' },
    'Отоцинклюс': { en: 'Otocinclus', asset: '/app/assets/fish/otocinklyus.png' },
    'Неоновая тетра': { en: 'Neon Tetra', asset: '/app/assets/fish/tetra_neonovaya.png' },
    'Тернеция': { en: 'Black Tetra', asset: '/app/assets/fish/tetra_chernaya.png' },
    'Арапайма': { en: 'Arapaima', asset: '/app/assets/fish/arapayma.png' },
    'Пиранья краснобрюхая': { en: 'Red-bellied Piranha', asset: '/app/assets/fish/piranya_krasnopuzaya.png' },
    'Трайра': { en: 'Trahira', asset: '/app/assets/fish/zubatka.png' },
    'Ацестринх': { en: 'Bicuda', asset: '/app/assets/fish/bikuda.png' },
    'Электрический угорь': { en: 'Electric Eel', asset: '/app/assets/fish/ugor_elektricheskiy.png' },
    'Краснохвостый сом': { en: 'Redtail Catfish', asset: '/app/assets/fish/krasnohvostiy_som.png' },
    'Пираиба': { en: 'Piraiba', asset: '/app/assets/fish/piraiba.png' },
    'Дискус': { en: 'Discus', asset: '/app/assets/fish/diskus.png' },
    'Скалярия': { en: 'Angelfish', asset: '/app/assets/fish/ryba_angel.png' },
    'Апистограмма Агассиза': { en: "Agassiz's Cichlid", asset: '/app/assets/fish/apistogramma_agassiza.png' },
    'Кардинальная тетра': { en: 'Cardinal Tetra', asset: '/app/assets/fish/tetra_kardinal.png' },
    'Коридорас панда': { en: 'Panda Cory', asset: '/app/assets/fish/koridorus_panda.png' },
    'Нанностомус': { en: 'Pencilfish', asset: '/app/assets/fish/nannostomus.png' },
    'Рамирези': { en: 'Ram Cichlid', asset: '/app/assets/fish/ramirezi.png' },
    'Аравана чёрная': { en: 'Black Arowana', asset: '/app/assets/fish/aravana_chernaya.png' },
    'Оскар': { en: 'Oscar', asset: '/app/assets/fish/oskar.png' },
    'Аймара': { en: 'Aimara', asset: '/app/assets/fish/aymara.png' },
    'Псевдоплатистома тигровая': { en: 'Tiger Shovelnose', asset: '/app/assets/fish/surubin.png' },
    'Пиранья чёрная': { en: 'Black Piranha', asset: '/app/assets/fish/piranya_chernaya.png' },
    'Щучья цихлида': { en: 'Pike Cichlid', asset: '/app/assets/fish/schuchya_cihlida.png' },
    'Павлиний окунь': { en: 'Peacock Bass', asset: '/app/assets/fish/pavliniy_okun.png' },
    'Молочная рыба': { en: 'Milkfish', asset: '/app/assets/fish/molochnaya_ryba.png' },
    'Пятнистая кефаль': { en: 'Spotted Mullet', asset: '/app/assets/fish/kefal_pyatnistaya.png' },
    'Тиляпия мозамбикская': { en: 'Mozambique Tilapia', asset: '/app/assets/fish/tilyapiya_mozambikskaya.png' },
    'Анчоус тропический': { en: 'Tropical Anchovy', asset: '/app/assets/fish/anchous_tropicheskiy.png' },
    'Сардина индийская': { en: 'Indian Sardine', asset: '/app/assets/fish/sardina_indiyskaya.png' },
    'Золотистый сиган': { en: 'Golden Rabbitfish', asset: '/app/assets/fish/zolotistiy_shiponog.png' },
    'Бычок мангровый': { en: 'Mangrove Goby', asset: '/app/assets/fish/bychok_mangroviy.png' },
    'Баррамунди': { en: 'Barramundi', asset: '/app/assets/fish/barramundi.png' },
    'Снук': { en: 'Snook', asset: '/app/assets/fish/snuk.png' },
    'Мангровый луциан': { en: 'Mangrove Snapper', asset: '/app/assets/fish/mangroviy_snapper.png' },
    'Тарпон': { en: 'Tarpon', asset: '/app/assets/fish/tarpon.png' },
    'Морской сом': { en: 'Sea Catfish', asset: '/app/assets/fish/morskoy_som.png' },
    'Морской сарган': { en: 'Needlefish', asset: '/app/assets/fish/morskoy_sargan.png' },
    'Голубой каранкс': { en: 'Blue Trevally', asset: '/app/assets/fish/goluboy_trevalli.png' },
    'Рыба-попугай': { en: 'Parrotfish', asset: '/app/assets/fish/ryba_popugay.png' },
    'Императорский ангел': { en: 'Emperor Angelfish', asset: '/app/assets/fish/angel_imperatorskiy.png' },
    'Голубой хирург': { en: 'Blue Tang', asset: '/app/assets/fish/hirurg_goluboy.png' },
    'Нитеносная бабочка': { en: 'Threadfin Butterflyfish', asset: '/app/assets/fish/babochka_klinopolosaya.png' },
    'Синяя хризиптера': { en: 'Blue Damselfish', asset: '/app/assets/fish/damsel_siniy.png' },
    'Фузилёр жёлтохвостый': { en: 'Yellowtail Fusilier', asset: '/app/assets/fish/fuziler_zheltohvostiy.png' },
    'Барабулька тропическая': { en: 'Tropical Goatfish', asset: '/app/assets/fish/barabulka_tropicheskaya.png' },
    'Большая барракуда': { en: 'Great Barracuda', asset: '/app/assets/fish/barrakuda_bolschaya.png' },
    'Гигантский каранкс': { en: 'Giant Trevally', asset: '/app/assets/fish/gigantskiy_karanks.png' },
    'Пермит': { en: 'Permit', asset: '/app/assets/fish/permit.png' },
    'Альбула': { en: 'Bonefish', asset: '/app/assets/fish/kostlyavaya_ryba.png' },
    'Испанская скумбрия': { en: 'Spanish Mackerel', asset: '/app/assets/fish/ispanskaya_makrel.png' },
    'Коралловая форель': { en: 'Coral Trout', asset: '/app/assets/fish/koralloviy_grupper.png' },
    'Спинорог-титан': { en: 'Titan Triggerfish', asset: '/app/assets/fish/spinorog_titan.png' },
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
      current: 'Текущий',
      rodBonusFreshPeaceful: '−50% шанс побега пресноводных мирных рыб',
      rodBonusFreshPredator: '−50% шанс побега пресноводных хищных рыб',
      rodBonusSaltPeaceful: '−50% шанс побега морских мирных рыб',
      rodBonusSaltPredator: '−50% шанс побега морских хищных рыб',
      rodNoBonus: 'Бонусов нет',
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
      shop: 'Магазин',
      menu: 'Меню',
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
      fishLabel: 'Рыба:',
      anyFish: 'Любая рыба',
      fishWord: 'рыба',
      anyLocation: 'Любая локация',
      conditionsLabel: 'Условия:',
      prizePlacesLabel: 'Призовых мест:',
      day: 'День',
      tapToClaim: 'Нажмите, чтобы получить',
      prizeCongrats: rank => `Поздравляем! Вы заняли ${rank} место в турнире. Ваш приз:`,
      hintFastBite: 'Чем быстрее реагируешь на клев, тем выше шанс поймать рыбу',
      hintWaitRare: 'Чем дольше ждешь клева, тем выше вероятность поймать редкую рыбу',
      hintTapPrize: 'Тапни на карточку в турнирной таблице, чтобы увидеть возможный приз',
      hintAutoCatch: 'С подпиской робот позаботится о вашем улове, если вы не успеете вовремя выловить рыбу',
      hintNoCloseRod: 'Не закрывай игру с заброшенной удочкой чтобы не потерять приманку',
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
      current: 'Current',
      rodBonusFreshPeaceful: '50% less escape chance for freshwater peaceful fish',
      rodBonusFreshPredator: '50% less escape chance for freshwater predator fish',
      rodBonusSaltPeaceful: '50% less escape chance for saltwater peaceful fish',
      rodBonusSaltPredator: '50% less escape chance for saltwater predator fish',
      rodNoBonus: 'No bonus',
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
      coinsEarned: coins => `+${coins} coin${coins===1?'':'s'}`,
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
      shop: 'Shop',
      menu: 'Menu',
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
      fishLabel: 'Fish:',
      anyFish: 'Any fish',
      fishWord: 'fish',
      anyLocation: 'Any location',
      conditionsLabel: 'Conditions:',
      prizePlacesLabel: 'Prize places:',
      day: 'Day',
      tapToClaim: 'Tap to claim',
      prizeCongrats: rank => `Congratulations! You placed ${rank} in the tournament. Your prize:`,
      hintFastBite: 'React to bites quickly for a better catch chance',
      hintWaitRare: 'Waiting longer for a bite increases the odds of rare fish',
      hintTapPrize: 'Tap a leaderboard card to see its potential prize',
      hintAutoCatch: 'With a subscription, the robot will secure your catch if you fail to reel in the fish in time',
      hintNoCloseRod: "Don't close the game with your rod cast or you'll lose the bait",
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
  window.ROD_IMAGES = ROD_IMAGES;
  window.ROD_IMG = ROD_IMG;
  window.ROD_IMG_SIZE = ROD_IMG_SIZE;
  window.ROD_TIP_ANCHOR = ROD_TIP_ANCHOR;
  window.ROD_TIP_ANCHORS = ROD_TIP_ANCHORS;
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
