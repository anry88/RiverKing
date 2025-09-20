(() => {
  const tg = window.Telegram?.WebApp;

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
  const lureColor = name => name.includes('+') ? rarityColors.legendary : '';
  const LOCATION_BG = {
    1: '/app/assets/riverking_bg_pond_1600x900.png',
    2: '/app/assets/riverking_bg_river_1600x900.png',
    3: '/app/assets/riverking_bg_lake_1600x900.png',
    4: '/app/assets/riverking_bg_swamp_1600x900.png',
    5: '/app/assets/riverking_bg_mountain_river_1600x900.png',
    6: '/app/assets/riverking_bg_reservoir_1600x900.png',
    7: '/app/assets/riverking_bg_river_delta_1600x900.png',
    8: '/app/assets/riverking_bg_sea_coast_1600x900.png',
    9: '/app/assets/riverking_bg_fjord_1600x900.png',
    10: '/app/assets/riverking_bg_open_ocean_1600x900.png'
  };
  const ROD_IMG = '/app/assets/riverking_rod_right_v2.png';
  const ROD_TIP_ANCHOR = { x: 0.285, y: 0.18 };
  const ROD_BASE_ANCHOR = { x: 0.58, y: 0.98 };
  const ROD_SIZE_MULT = 1.5;
  const ROD_BASE_X_FRACTION = 2 / 3;

  const TAP_CHALLENGE_GOAL = 10;
  const TAP_CHALLENGE_DURATION_MS = 5000;
  const CAST_READY_DELAY_MS = 3000;

  const FISH_TRANSLATIONS = {
    'Плотва': 'Roach','Окунь': 'Perch','Карась': 'Crucian Carp','Лещ': 'Bream','Щука': 'Pike','Карп': 'Carp','Сом': 'Catfish','Осётр': 'Sturgeon','Уклейка': 'Bleak','Линь': 'Tench','Ротан': 'Rotan','Судак': 'Zander','Чехонь': 'Sabrefish','Хариус': 'Grayling','Форель ручьевая': 'Brook Trout','Таймень': 'Taimen','Налим': 'Burbot','Сиг': 'Whitefish','Голавль': 'Chub','Жерех': 'Asp','Толстолобик': 'Bighead Carp','Белый амур': 'Grass Carp','Угорь европейский': 'European Eel','Стерлядь': 'Sterlet','Кефаль': 'Mullet','Камбала': 'Flounder','Сельдь': 'Herring','Ставрида': 'Horse Mackerel','Треска': 'Cod','Сайда': 'Pollock','Морская форель': 'Sea Trout','Палтус': 'Halibut','Корюшка': 'Smelt','Лосось атлантический': 'Atlantic Salmon','Лаврак': 'Sea Bass','Скумбрия атлантическая': 'Atlantic Mackerel','Белуга': 'Beluga','Ёрш': 'Ruffe','Пескарь': 'Gudgeon','Густера': 'Blue Bream','Краснопёрка': 'Rudd','Елец': 'Dace','Верхоплавка': 'Topmouth Gudgeon','Гольян': 'Minnow','Язь': 'Ide','Бычок': 'Goby','Килька': 'Sprat','Мойва': 'Capelin','Сардина': 'Sardine','Анчоус': 'Anchovy','Дорадо': 'Dorado','Ваху': 'Wahoo','Парусник': 'Sailfish','Рыба-меч': 'Swordfish','Марлин синий': 'Blue Marlin','Тунец синеперый': 'Bluefin Tuna','Акула мако': 'Mako Shark','Альбакор': 'Albacore','Голец арктический': 'Arctic Char','Форель кумжа': 'Brown Trout','Пикша': 'Haddock','Тюрбо': 'Turbot','Сайра': 'Pacific Saury','Летучая рыба': 'Flying Fish','Рыба-луна': 'Ocean Sunfish','Сельдяной король': 'Oarfish'
  };
  const LURE_TRANSLATIONS = {
    'Пресная мирная': 'Freshwater Peaceful',
    'Пресная хищная': 'Freshwater Predator',
    'Морская мирная': 'Saltwater Peaceful',
    'Морская хищная': 'Saltwater Predator',
    'Пресная мирная+': 'Freshwater Peaceful+',
    'Пресная хищная+': 'Freshwater Predator+',
    'Морская мирная+': 'Saltwater Peaceful+',
    'Морская хищная+': 'Saltwater Predator+',
  };
  const FISH_IMG = {};
  const translateLure = n => {
    if (document.documentElement.lang === 'en') return LURE_TRANSLATIONS[n] || n;
    return n;
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
      total: 'Всего',
      today: 'Сегодня',
      yesterday: 'Вчера',
      lastWeek: 'За неделю',
      lastMonth: 'За месяц',
      lastYear: 'За год',
      allTime: 'За всё время',
      kg: 'кг',
      locations: 'Локации',
      unlocked: 'Открыто',
      requiresKg: kg => `Требуется ${kg} кг`,
      current: 'Текущий',
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
      tournamentsSoon: 'Турниры в разработке',
      shopEmpty: 'Магазин пуст',
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
      total: 'Total',
      today: 'Today',
      yesterday: 'Yesterday',
      lastWeek: 'Last week',
      lastMonth: 'Last month',
      lastYear: 'Last year',
      allTime: 'All time',
      kg: 'kg',
      locations: 'Locations',
      unlocked: 'Unlocked',
      requiresKg: kg => `${kg} kg required`,
      current: 'Current',
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
      tournamentsSoon: 'Tournaments coming soon',
      shopEmpty: 'Shop is empty',
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
      if (!ref.current) return;
      const ro = new ResizeObserver(([entry]) => {
        const cr = entry.contentRect;
        setSize({ w: cr.width, h: cr.height });
      });
      ro.observe(ref.current);
      return () => ro.disconnect();
    }, [ref]);
    return size;
  }

  window.rarityColors = rarityColors;
  window.rarityNames = rarityNames;
  window.lureColor = lureColor;
  window.LOCATION_BG = LOCATION_BG;
  window.ROD_IMG = ROD_IMG;
  window.ROD_TIP_ANCHOR = ROD_TIP_ANCHOR;
  window.ROD_BASE_ANCHOR = ROD_BASE_ANCHOR;
  window.ROD_SIZE_MULT = ROD_SIZE_MULT;
  window.ROD_BASE_X_FRACTION = ROD_BASE_X_FRACTION;
  window.TAP_CHALLENGE_GOAL = TAP_CHALLENGE_GOAL;
  window.TAP_CHALLENGE_DURATION_MS = TAP_CHALLENGE_DURATION_MS;
  window.CAST_READY_DELAY_MS = CAST_READY_DELAY_MS;
  window.FISH_IMG = FISH_IMG;
  window.translateLure = translateLure;
  window.makeT = makeT;
  window.initLang = initLang;
  window.easeOutCubic = easeOutCubic;
  window.useResizeObserver = useResizeObserver;
})();
