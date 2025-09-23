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
      enDescription: 'For peaceful freshwater fish.'
    },
    'Пресная хищная': {
      plus: false,
      ruName: 'Ручейный малек',
      enName: 'Brook Minnow',
      ruDescription: 'Для хищной пресноводной рыбы.',
      enDescription: 'For predatory freshwater fish.'
    },
    'Морская мирная': {
      plus: false,
      ruName: 'Морская водоросль',
      enName: 'Seaweed Strand',
      ruDescription: 'Для мирной морской рыбы.',
      enDescription: 'For peaceful saltwater fish.'
    },
    'Морская хищная': {
      plus: false,
      ruName: 'Кольца кальмара',
      enName: 'Squid Rings',
      ruDescription: 'Для хищной морской рыбы.',
      enDescription: 'For predatory saltwater fish.'
    },
    'Пресная мирная+': {
      plus: true,
      ruName: 'Луговой червь',
      enName: 'Meadow Worm',
      ruDescription: 'Для редкой мирной пресноводной рыбы.',
      enDescription: 'For rare peaceful freshwater fish.'
    },
    'Пресная хищная+': {
      plus: true,
      ruName: 'Серебряный живец',
      enName: 'Silver Shiner',
      ruDescription: 'Для редкой хищной пресноводной рыбы.',
      enDescription: 'For rare predatory freshwater fish.'
    },
    'Морская мирная+': {
      plus: true,
      ruName: 'Неоновый планктон',
      enName: 'Neon Plankton',
      ruDescription: 'Для редкой мирной морской рыбы.',
      enDescription: 'For rare peaceful saltwater fish.'
    },
    'Морская хищная+': {
      plus: true,
      ruName: 'Королевская креветка',
      enName: 'Royal Shrimp',
      ruDescription: 'Для редкой хищной морской рыбы.',
      enDescription: 'For rare predatory saltwater fish.'
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
    11: ['Амазонское русло', 'Amazon Riverbed'],
    12: ['Затопленный лес', 'Flooded Forest'],
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

  const FISH_TRANSLATIONS = {
    'Плотва': 'Roach','Окунь': 'Perch','Карась': 'Crucian Carp','Лещ': 'Bream','Щука': 'Pike','Карп': 'Carp','Сом': 'Catfish','Осётр': 'Sturgeon','Уклейка': 'Bleak','Линь': 'Tench','Ротан': 'Rotan','Судак': 'Zander','Чехонь': 'Sabrefish','Хариус': 'Grayling','Форель ручьевая': 'Brook Trout','Таймень': 'Taimen','Налим': 'Burbot','Сиг': 'Whitefish','Голавль': 'Chub','Жерех': 'Asp','Толстолобик': 'Bighead Carp','Белый амур': 'Grass Carp','Угорь европейский': 'European Eel','Стерлядь': 'Sterlet','Кефаль': 'Mullet','Камбала': 'Flounder','Сельдь': 'Herring','Ставрида': 'Horse Mackerel','Треска': 'Cod','Сайда': 'Pollock','Морская форель': 'Sea Trout','Палтус': 'Halibut','Корюшка': 'Smelt','Лосось атлантический': 'Atlantic Salmon','Лаврак': 'Sea Bass','Скумбрия атлантическая': 'Atlantic Mackerel','Белуга': 'Beluga','Ёрш': 'Ruffe','Пескарь': 'Gudgeon','Густера': 'Blue Bream','Краснопёрка': 'Rudd','Елец': 'Dace','Верхоплавка': 'Topmouth Gudgeon','Гольян': 'Minnow','Язь': 'Ide','Бычок': 'Goby','Килька': 'Sprat','Мойва': 'Capelin','Сардина': 'Sardine','Анчоус': 'Anchovy','Дорадо': 'Dorado','Ваху': 'Wahoo','Парусник': 'Sailfish','Рыба-меч': 'Swordfish','Марлин синий': 'Blue Marlin','Тунец синеперый': 'Bluefin Tuna','Акула мако': 'Mako Shark','Альбакор': 'Albacore','Голец арктический': 'Arctic Char','Форель кумжа': 'Brown Trout','Пикша': 'Haddock','Тюрбо': 'Turbot','Сайра': 'Pacific Saury','Летучая рыба': 'Flying Fish','Рыба-луна': 'Ocean Sunfish','Сельдяной король': 'Oarfish'
  };
  const FISH_IMG = {};
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
  Object.entries({
    'Плотва': '/app/assets/fish/plotva.png',
    'Окунь': '/app/assets/fish/okun.png',
    'Карась': '/app/assets/fish/karas.png',
    'Лещ': '/app/assets/fish/lesch.png',
    'Щука': '/app/assets/fish/schuka.png',
    'Карп': '/app/assets/fish/karp.png',
    'Сом': '/app/assets/fish/som.png',
    'Осётр': '/app/assets/fish/osetr.png',
    'Уклейка': '/app/assets/fish/ukleyka.png',
    'Линь': '/app/assets/fish/lin.png',
    'Ротан': '/app/assets/fish/rotan.png',
    'Судак': '/app/assets/fish/sudak.png',
    'Чехонь': '/app/assets/fish/chehon.png',
    'Хариус': '/app/assets/fish/harius.png',
    'Форель ручьевая': '/app/assets/fish/forel_ruchevaya.png',
    'Таймень': '/app/assets/fish/taymen.png',
    'Налим': '/app/assets/fish/nalim.png',
    'Сиг': '/app/assets/fish/sig.png',
    'Голавль': '/app/assets/fish/golavl.png',
    'Жерех': '/app/assets/fish/zhereh.png',
    'Толстолобик': '/app/assets/fish/tolstolobik.png',
    'Белый амур': '/app/assets/fish/beliy_amur.png',
    'Угорь европейский': '/app/assets/fish/ugor_evropeyskiy.png',
    'Стерлядь': '/app/assets/fish/sterlyad.png',
    'Кефаль': '/app/assets/fish/kefal.png',
    'Камбала': '/app/assets/fish/kambala.png',
    'Сельдь': '/app/assets/fish/seld.png',
    'Ставрида': '/app/assets/fish/stavrida.png',
    'Треска': '/app/assets/fish/treska.png',
    'Сайда': '/app/assets/fish/sayda.png',
    'Морская форель': '/app/assets/fish/morskaya_forel.png',
    'Палтус': '/app/assets/fish/paltus.png',
    'Корюшка': '/app/assets/fish/koryushka.png',
    'Лосось атлантический': '/app/assets/fish/losos_atlanticheskiy.png',
    'Лаврак': '/app/assets/fish/lavrak.png',
    'Скумбрия атлантическая': '/app/assets/fish/skumbriya_atlanticheskaya.png',
    'Белуга': '/app/assets/fish/beluga.png',
    'Ёрш': '/app/assets/fish/yorsh.png',
    'Пескарь': '/app/assets/fish/peskar.png',
    'Густера': '/app/assets/fish/gustera.png',
    'Краснопёрка': '/app/assets/fish/krasnopyorka.png',
    'Елец': '/app/assets/fish/elets.png',
    'Верхоплавка': '/app/assets/fish/verhoplavka.png',
    'Гольян': '/app/assets/fish/golyan.png',
    'Язь': '/app/assets/fish/yaz.png',
    'Бычок': '/app/assets/fish/bychyok.png',
    'Килька': '/app/assets/fish/kilka.png',
    'Мойва': '/app/assets/fish/mojva.png',
    'Сардина': '/app/assets/fish/sardina.png',
    'Анчоус': '/app/assets/fish/anchous.png',
    'Дорадо': '/app/assets/fish/dorado.png',
    'Ваху': '/app/assets/fish/vahu.png',
    'Парусник': '/app/assets/fish/parusnik.png',
    'Рыба-меч': '/app/assets/fish/ryba_mech.png',
    'Марлин синий': '/app/assets/fish/marlin_siniy.png',
    'Тунец синеперый': '/app/assets/fish/tunets_sineperiy.png',
    'Акула мако': '/app/assets/fish/akula_mako.png',
    'Альбакор': '/app/assets/fish/albakor.png',
    'Голец арктический': '/app/assets/fish/golets_arkticheskiy.png',
    'Форель кумжа': '/app/assets/fish/forel_kumzha.png',
    'Пикша': '/app/assets/fish/piksha.png',
    'Тюрбо': '/app/assets/fish/tyurbo.png',
    'Сайра': '/app/assets/fish/sayra.png',
    'Летучая рыба': '/app/assets/fish/letuchaya_ryba.png',
    'Рыба-луна': '/app/assets/fish/ryba_luna.png',
    'Сельдяной король': '/app/assets/fish/seldyanoy_korol.png',
  }).forEach(([ru, path]) => {
    FISH_IMG[ru] = path;
    const en = FISH_TRANSLATIONS[ru];
    if (en) FISH_IMG[en] = path;
  });

  const STRINGS = {
    ru: {
      location: 'Локация',
      baits: 'Приманки',
      rod: 'Удочка',
      rods: 'Удочки',
      total: 'Всего',
      today: 'Сегодня',
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
      authRequired: 'Требуется авторизация. Запустите игру через кнопку бота.',
      loadFailed: 'Не удалось загрузить',
      loadProfileFailed: 'Не удалось загрузить профиль',
      purchaseFailed: 'Не удалось купить',
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
      authRequired: 'Authorization required. Launch the game via the bot button.',
      loadFailed: 'Failed to load',
      loadProfileFailed: 'Failed to load profile',
      purchaseFailed: 'Purchase failed',
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
