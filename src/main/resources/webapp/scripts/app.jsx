const tg = window.Telegram?.WebApp;
console.log('initData length:', tg?.initData?.length || 0, 'platform:', tg?.platform);

const params = new URLSearchParams(window.location.search);
const refToken = tg?.initDataUnsafe?.start_param
  || params.get('tgWebAppStartParam')
  || params.get('ref');
const makeT = window.makeT || ((lang) => (key) => key);
let t = window.t || ((key) => key);
const TAP_CHALLENGE_DURATION_MS = window.TAP_CHALLENGE_DURATION_MS || 5000;
const TAP_CHALLENGE_GOAL = window.TAP_CHALLENGE_GOAL || 10;
const CAST_READY_DELAY_MS = window.CAST_READY_DELAY_MS || 3000;
const initLang = window.initLang || 'en';
const translateLure = (name, lang) => {
  const fn = window.translateLure;
  if (typeof fn === 'function' && fn !== translateLure) return fn(name, lang);
  return name;
};
const useResizeObserver = window.useResizeObserver || (() => ({ w: 0, h: 0 }));
const ROD_IMG = window.ROD_IMG || '/app/assets/rods/yellow_rod.png';
const tgParam = (()=>{
  try{
    const raw = window.location.search + window.location.hash;
    const m = raw.match(/(?:^|[?&#])tgWebAppData=([^&]+)/);
    return m ? decodeURIComponent(m[1]) : null;
  }catch{ return null; }
})();

const FAIL_REACTION_SECONDS = 5.1;
const BOBBER_ICON = window.BOBBER_ICON || '/app/assets/menu/bobber.png';
const AssetImage = window.AssetImage;
const preloadAsset = window.preloadAsset;

function App(){
  const [me,setMe] = React.useState(null);
  const [loading,setLoading] = React.useState(true);
  const [loadingHint] = React.useState(()=>{
    const hints = [t('hintFastBite'), t('hintWaitRare'), t('hintTapPrize'), t('hintAutoCatch'), t('hintNoCloseRod')];
    return hints[Math.floor(Math.random()*hints.length)];
  });
  const [casting,setCasting] = React.useState(false);
  const castingRef = React.useRef(false);
  const [biting,setBiting] = React.useState(false);
  const bitingRef = React.useRef(false);
  const waitRef = React.useRef(0);
  const biteTimeRef = React.useRef(0);
  const hookTimeoutRef = React.useRef(null);
  const [result,setResult] = React.useState(null);
  const [error,setError] = React.useState(null);
  const [drawerOpen, setDrawerOpen] = React.useState(false);
  const [baitsOpen, setBaitsOpen] = React.useState(false);
  const [rodsOpen, setRodsOpen] = React.useState(false);
  const [tab, setTab] = React.useState('fish');
  const [shop,setShop] = React.useState([]);
  const starterPackName = React.useMemo(() => (
    shop.reduce((acc,c)=>acc.concat(c.packs),[]).find(p=>p.id==='bundle_starter')?.name || 'bundle_starter'
  ), [shop]);
  const [currentTournament,setCurrentTournament] = React.useState(null);
  const [upcomingTournaments,setUpcomingTournaments] = React.useState([]);
  const [pastTournaments,setPastTournaments] = React.useState([]);
  const [pastResult,setPastResult] = React.useState(null);
  const [tournamentTab,setTournamentTab] = React.useState('current');
  const [nickOpen,setNickOpen] = React.useState(false);
  const [prize,setPrize] = React.useState(null);
  const [prizeHint,setPrizeHint] = React.useState(null);
  const [catchDetails,setCatchDetails] = React.useState(null);
  const [dailyOpen,setDailyOpen] = React.useState(false);
  const dailyPromptDismissedRef = React.useRef(false);
  const [refOpen,setRefOpen] = React.useState(false);
  const [refInfo,setRefInfo] = React.useState(null);
  const [refRewards,setRefRewards] = React.useState(null);
  const [coinPurchasePack,setCoinPurchasePack] = React.useState(null);
  const [coinPurchaseProcessing,setCoinPurchaseProcessing] = React.useState(false);
  const [coinInsufficientOpen, setCoinInsufficientOpen] = React.useState(false);
  const [achievements, setAchievements] = React.useState([]);
  const [achievementsLoading, setAchievementsLoading] = React.useState(false);
  const [achievementsError, setAchievementsError] = React.useState(null);
  const [achievementsClaimable, setAchievementsClaimable] = React.useState(false);
  const achievementsRef = React.useRef([]);
  const [claimingAchievement, setClaimingAchievement] = React.useState(null);
  const [achievementReward, setAchievementReward] = React.useState(null);
  const [questsOpen, setQuestsOpen] = React.useState(false);
  const [quests, setQuests] = React.useState({ daily: [], weekly: [] });
  const [questsLoading, setQuestsLoading] = React.useState(false);
  const [questsError, setQuestsError] = React.useState(null);
  const coinLocale = (typeof document!=='undefined' && document.documentElement.lang==='en') ? 'en-US' : 'ru-RU';
  const coinPurchasePriceLabel = coinPurchasePack
    ? Number(coinPurchasePack.coinPrice).toLocaleString(coinLocale)
    : '';
  const [autoCast,setAutoCast] = React.useState(()=>{ try{ return localStorage.getItem('autoCast')==='1'; }catch(e){ return false; }});
  const autoCastRef = React.useRef(autoCast);
  const autoCastTimeoutRef = React.useRef(null);
  const [castReady,setCastReady] = React.useState(true);
  const castReadyRef = React.useRef(true);
  const castReadyTimeoutRef = React.useRef(null);
  const [tapActive,setTapActive] = React.useState(false);
  const tapActiveRef = React.useRef(false);
  const [tapCount,setTapCount] = React.useState(0);
  const tapCountRef = React.useRef(0);
  const [tapTimeLeft,setTapTimeLeft] = React.useState(TAP_CHALLENGE_DURATION_MS/1000);
  const tapDeadlineRef = React.useRef(0);
  const tapReactionRef = React.useRef(0);
  const tapTimerRef = React.useRef(null);
  const tapFinishingRef = React.useRef(false);
  const catchAnimationIdRef = React.useRef(0);
  const lastCatchAnimationShownRef = React.useRef(null);
  const essentialAssetsRef = React.useRef(new Set());
  const claimingPrizeRef = React.useRef(false);
  const claimingRefRewardsRef = React.useRef(false);
  const markCatchAnimationShown = React.useCallback(id => {
    if(id == null) return;
    lastCatchAnimationShownRef.current = id;
  }, []);
  const hasCatchAnimationBeenShown = React.useCallback(id => {
    if(id == null) return false;
    return lastCatchAnimationShownRef.current === id;
  }, []);

  const handleCatchClick = React.useCallback(data => {
    if(!data) return;
    setCatchDetails({...data});
  }, []);

  const achievementNameByCode = React.useCallback(code => {
    if(!code) return '';
    return achievementsRef.current.find(a => a.code === code)?.name || code;
  }, []);

  const reloadProfile = React.useCallback(async ()=>{
    try{
      const meResp = await fetch(`/api/me`,{credentials:'include'});
      if(meResp.ok){
        setMe(await meResp.json());
      }
    }catch(e){}
  }, []);

  const loadAchievements = React.useCallback(async ()=>{
    setAchievementsLoading(true);
    setAchievementsError(null);
    try{
      const r = await fetch(`/api/achievements`,{credentials:'include'});
      if(!r.ok){
        if(r.status===401) throw new Error('unauthorized');
        throw new Error('load_failed');
      }
      const list = await r.json();
      achievementsRef.current = list;
      setAchievements(list);
      setAchievementsClaimable(list.some(a=>a.claimable));
    }catch(e){
      setAchievementsError(e.message);
      if(e.message==='unauthorized') setError(t('authRequired'));
    }finally{
      setAchievementsLoading(false);
    }
  }, [t]);

  const loadQuests = React.useCallback(async ()=>{
    setQuestsLoading(true);
    setQuestsError(null);
    try{
      const r = await fetch(`/api/quests`,{credentials:'include'});
      if(!r.ok){
        if(r.status===401) throw new Error('unauthorized');
        throw new Error('load_failed');
      }
      setQuests(await r.json());
    }catch(e){
      setQuestsError(e.message);
      if(e.message==='unauthorized') setError(t('authRequired'));
    }finally{
      setQuestsLoading(false);
    }
  }, [t]);

  React.useEffect(()=>{
    if(tab === 'guide'){
      loadAchievements();
    }
  }, [tab, loadAchievements]);

  const claimAchievement = React.useCallback(async code => {
    if(!code) return;
    setClaimingAchievement(code);
    try{
      const r = await fetch(`/api/achievements/${code}/claim`,{method:'POST',credentials:'include'});
      if(!r.ok){
        if(r.status===401) throw new Error('unauthorized');
        throw new Error('claim_failed');
      }
      const data = await r.json();
      const rewards = Array.isArray(data?.rewards) ? data.rewards : [];
      setAchievementReward({ code, rewards, name: achievementNameByCode(code) });
      await loadAchievements();
      await reloadProfile();
    }catch(e){
      setError(e.message==='unauthorized' ? t('authRequired') : t('achievementsUnavailable'));
    }finally{
      setClaimingAchievement(null);
    }
  }, [achievementNameByCode, loadAchievements, reloadProfile, t]);

  const openQuests = React.useCallback(() => {
    setQuestsOpen(true);
    loadQuests();
  }, [loadQuests]);

  const achievementRewardLabel = React.useCallback((reward)=>{
    if(!reward) return '';
    if(reward.pack === 'coins'){
      const coins = reward.coins ?? reward.qty ?? 0;
      return t('achievementRewardCoins', coins);
    }
    const pack = (shop.reduce((acc,c)=>acc.concat(c.packs||[]),[]).find(p=>p.id===reward.pack));
    const label = pack?.name || (reward.pack === 'autofish' ? t('autoFishExtend') : reward.pack || t('achievementRewardTitle'));
    return t('achievementRewardPack', { name: label, qty: reward.qty || 1 });
  }, [shop, t]);

  React.useEffect(()=>{
    autoCastRef.current = autoCast;
    if(!me?.autoFish) setAutoCast(false);
    if(!autoCast){
      clearTimeout(autoCastTimeoutRef.current);
      autoCastTimeoutRef.current = null;
    }
  }, [autoCast, me?.autoFish]);
  React.useEffect(()=>{ try{ localStorage.setItem('autoCast', autoCast?'1':'0'); }catch(e){} }, [autoCast]);

  React.useEffect(()=>()=>{
    if(castReadyTimeoutRef.current){
      clearTimeout(castReadyTimeoutRef.current);
      castReadyTimeoutRef.current = null;
    }
  }, []);

  function startCastCooldown(){
    castReadyRef.current = false;
    setCastReady(false);
    if(castReadyTimeoutRef.current){
      clearTimeout(castReadyTimeoutRef.current);
    }
    castReadyTimeoutRef.current = setTimeout(()=>{
      castReadyRef.current = true;
      setCastReady(true);
      castReadyTimeoutRef.current = null;
    }, CAST_READY_DELAY_MS);
  }

  React.useEffect(()=>{ tapActiveRef.current = tapActive; }, [tapActive]);
  React.useEffect(()=>{ tapCountRef.current = tapCount; }, [tapCount]);
  React.useEffect(()=>{
    if(!tapActive) return;
    tapActiveRef.current = true;
    const tick = ()=>{
      if(!tapActiveRef.current) return;
      const remaining = tapDeadlineRef.current - Date.now();
      const clamped = Math.max(0, remaining);
      setTapTimeLeft(clamped / 1000);
      if(clamped <= 0){
        finishTap(tapCountRef.current >= TAP_CHALLENGE_GOAL);
        return;
      }
      tapTimerRef.current = requestAnimationFrame(tick);
    };
    tapTimerRef.current = requestAnimationFrame(tick);
    return ()=>{
      if(tapTimerRef.current){
        cancelAnimationFrame(tapTimerRef.current);
        tapTimerRef.current = null;
      }
    };
  }, [tapActive]);

  React.useEffect(()=>{
    if(me?.needsNickname){
      setNickOpen(true);
    }
  },[me?.needsNickname]);

  React.useEffect(()=>{
    if(!me) return;
    loadAchievements();
  }, [me?.language, loadAchievements]);

  React.useEffect(()=>{
    if(loading) return;
    if(!me?.dailyAvailable){
      dailyPromptDismissedRef.current = false;
      if(dailyOpen){
        setDailyOpen(false);
      }
    }
  }, [loading, me?.dailyAvailable, dailyOpen]);

  React.useEffect(()=>{
    if(tournamentTab!=='past') setPastResult(null);
  },[tournamentTab]);

  React.useEffect(()=>{
    try{ tg?.ready(); tg?.expand(); tg?.MainButton?.hide?.(); tg?.enableClosingConfirmation?.(); }catch(e){}
    const essentialAssets = new Set();
    const addEssential = src => {
      if(typeof src === 'string' && src.trim()) essentialAssets.add(src);
    };
    addEssential(BOBBER_ICON);
    if(Array.isArray(window.BOTTOM_NAV_ITEMS)){
      window.BOTTOM_NAV_ITEMS.forEach(item => {
        if(item && typeof item.icon === 'string') addEssential(item.icon);
      });
    }
    const rodImages = window.ROD_IMAGES ? Object.values(window.ROD_IMAGES) : (window.ROD_IMG ? [ROD_IMG] : []);
    rodImages.forEach(addEssential);
    const backgrounds = window.LOCATION_BG ? Object.values(window.LOCATION_BG) : [];
    backgrounds.forEach(addEssential);
    if(window.ACHIEVEMENT_ART){
      Object.values(window.ACHIEVEMENT_ART).forEach(levelMap => {
        if(levelMap && typeof levelMap === 'object'){
          Object.values(levelMap).forEach(addEssential);
        }
      });
    }
    if(window.ACHIEVEMENT_IMAGES){
      Object.values(window.ACHIEVEMENT_IMAGES).forEach(addEssential);
    }
    essentialAssetsRef.current = essentialAssets;

    let preloadStarted = false;
    let preloadPromise = Promise.resolve();
    const startEssentialPreload = () => {
      if(preloadStarted) return preloadPromise;
      preloadStarted = true;
      if(!preloadAsset || essentialAssets.size===0){
        preloadPromise = Promise.resolve();
        return preloadPromise;
      }
      const assets = Array.from(essentialAssets);
      preloadPromise = Promise.all(assets.map(src => {
        try{
          return preloadAsset(src);
        }catch(err){
          console.warn('asset preload invocation failed', src, err);
          return Promise.resolve(null);
        }
      })).catch(err => {
        console.warn('essential asset preload batch failed', err);
        return null;
      });
      return preloadPromise;
    };

    let cancelled = false;
    (async()=>{
      setLoading(true);
      try{
        const initData = tg?.initData || tgParam;
        if(initData){
          await fetch(`/api/auth/telegram${refToken?`?ref=${refToken}`:''}`, {
            method:'POST',
            headers:{'Telegram-Init-Data': initData},
            credentials:'include'
          });
        }
      }catch(e){}
      try{
        const r = await fetch(`/api/me`, {credentials:'include'});
        if(!r.ok){
          if(r.status===401) throw new Error('unauthorized');
          throw new Error('me failed');
        }
        const d = await r.json();
        t = makeT(d.language);
        window.t = t;
        document.documentElement.lang = d.language;
        try{ localStorage.setItem('lang', d.language); }catch(e){}
        if(cancelled) return;
        setMe(d);
        try{ window.assetAuthReady?.(); }catch(e){}
        startEssentialPreload();
        loadAchievements();
        try{
          const s = await fetch(`/api/shop`,{credentials:'include'});
          if(s.ok){
            const shopData = await s.json();
            if(!cancelled) setShop(shopData);
          }
        }catch(e){
          if(e.message==='unauthorized'){
            if(!cancelled) setError(t('authRequired'));
            if(!cancelled) setShop([]);
          }
        }
        try{
          const ct = await fetch(`/api/tournament/current`,{credentials:'include'});
          if(ct.status===200){
            const currentData = await ct.json();
            if(!cancelled) setCurrentTournament(currentData);
          } else if(!cancelled){
            setCurrentTournament(null);
          }
          const ut = await fetch(`/api/tournaments/upcoming`,{credentials:'include'});
          if(ut.status===200){
            const upcoming = await ut.json();
            if(!cancelled) setUpcomingTournaments(upcoming);
          } else if(!cancelled){
            setUpcomingTournaments([]);
          }
          const pt = await fetch(`/api/tournaments/past`,{credentials:'include'});
          if(pt.status===200){
            const past = await pt.json();
            if(!cancelled) setPastTournaments(past);
          } else if(!cancelled){
            setPastTournaments([]);
          }
          const pr = await fetch(`/api/prizes`,{credentials:'include'});
          if(pr.ok){
            const list = await pr.json();
            if(!cancelled && list.length>0) setPrize(list[0]);
          }
          const rr = await fetch(`/api/referrals/rewards`,{credentials:'include'});
          if(rr.ok){
            const rewardsList = await rr.json();
            if(!cancelled && rewardsList.length>0){
              await loadReferrals();
              if(!cancelled) setRefRewards(rewardsList);
            }
          }
        }catch(e){
          if(e.message==='unauthorized'){
            if(!cancelled) setError(t('authRequired'));
            if(!cancelled) setCurrentTournament(null);
            if(!cancelled) setUpcomingTournaments([]);
            if(!cancelled) setPastTournaments([]);
          }
        }
      }catch(e){
        startEssentialPreload();
        if(e.message==='unauthorized'){
          if(!cancelled) setError(t('authRequired'));
          if(!cancelled) setShop([]);
        } else {
          if(!cancelled) setError(t('loadProfileFailed'));
          t = makeT(initLang);
          window.t = t;
          document.documentElement.lang = initLang;
          try{ localStorage.setItem('lang', initLang); }catch(err){}
          if(!cancelled) setMe({
            username:'angler',
            needsNickname:false,
            language:initLang,
            lures:[
              {id:1,name:'Зерновая крошка',qty:5,predator:false,water:'fresh',rarityBonus:0},
              {id:2,name:'Ручейный малек',qty:5,predator:true,water:'fresh',rarityBonus:0},
              {id:3,name:'Морская водоросль',qty:5,predator:false,water:'salt',rarityBonus:0},
              {id:4,name:'Кольца кальмара',qty:5,predator:true,water:'salt',rarityBonus:0},
              {id:5,name:'Луговой червь',qty:2,predator:false,water:'fresh',rarityBonus:0.3},
              {id:6,name:'Серебряный живец',qty:2,predator:true,water:'fresh',rarityBonus:0.3},
              {id:7,name:'Неоновый планктон',qty:2,predator:false,water:'salt',rarityBonus:0.3},
              {id:8,name:'Королевская креветка',qty:2,predator:true,water:'salt',rarityBonus:0.3},
            ],
            currentLureId:1,
            totalWeight:0, todayWeight:0, locationId:1,
            locations:[
              {id:1,name:'Пруд',unlockKg:0,unlocked:true},
              {id:2,name:'Река',unlockKg:10,unlocked:false},
              {id:3,name:'Озеро',unlockKg:50,unlocked:false},
            ],
            caughtFishIds:[],
            recent:[], dailyAvailable:true, dailyStreak:0, dailyRewards:[],
            coins:0,
            todayCoins:0,
          });
          if(!cancelled) setShop([]);
        }
      }finally{
        try{
          const maybePromise = startEssentialPreload();
          if(maybePromise && typeof maybePromise.then === 'function'){
            maybePromise.catch(err => {
              console.warn('essential asset preload failed', err);
            });
          }
        }catch(err){
          console.warn('essential asset preload invocation failed', err);
        }
        if(!cancelled){
          setLoading(false);
        }
      }
    })();
    return ()=>{ cancelled = true; };
  },[]);

  const loadAdditionalAssets = React.useCallback(()=>{
    if(typeof preloadAsset !== 'function') return;
    const extras = new Set();
    const addExtra = src => {
      if(typeof src === 'string' && src.trim()) extras.add(src);
    };
    if(window.FISH_IMG){
      Object.values(window.FISH_IMG).forEach(addExtra);
    }
    if(me?.lures && typeof window.getLureIcon === 'function'){
      me.lures.forEach(lure => {
        try{ addExtra(window.getLureIcon(lure)); }
        catch(err){ console.warn('lure icon resolve failed', err); }
      });
    }
    if(Array.isArray(shop)){
      const shopIconGetter = typeof window.getShopIcon === 'function'
        ? window.getShopIcon
        : (id => {
            if(!id) return '';
            if(String(id).startsWith('autofish')) return '/app/assets/shop/autofish.png';
            return `/app/assets/shop/${id}.png`;
          });
      shop.forEach(category => {
        (category?.packs || []).forEach(item => {
          try{ addExtra(shopIconGetter(item.id)); }
          catch(err){ console.warn('shop icon resolve failed', err); }
        });
      });
    }
    const essential = essentialAssetsRef.current || new Set();
    const run = () => {
      extras.forEach(src => {
        if(!src || essential.has(src)) return;
        try{ preloadAsset(src); }
        catch(err){ console.warn('background asset preload failed', src, err); }
      });
    };
    if(typeof window.requestIdleCallback === 'function'){
      window.requestIdleCallback(()=>run(), {timeout:2000});
    } else {
      setTimeout(run, 50);
    }
  }, [me?.lures, shop]);

  React.useEffect(()=>{
    if(loading) return;
    loadAdditionalAssets();
  }, [loading, loadAdditionalAssets]);


  async function claimPrize(){
    if(!prize || claimingPrizeRef.current) return;
    const currentPrize = prize;
    setPrize(null);
    claimingPrizeRef.current = true;
    try{
      await fetch(`/api/prizes/${currentPrize.id}/claim`,{method:'POST',credentials:'include'});
      const r = await fetch(`/api/me`,{credentials:'include'});
      if(r.ok) setMe(await r.json());
    }catch(e){
      console.warn('prize claim failed', e);
      setPrize(currentPrize);
    }finally{
      claimingPrizeRef.current = false;
    }
  }

  async function claimRefRewards(){
    if(!refRewards || claimingRefRewardsRef.current) return;
    const pendingRewards = refRewards;
    setRefRewards(null);
    claimingRefRewardsRef.current = true;
    try{
      await fetch(`/api/referrals/rewards/claim`,{method:'POST',credentials:'include'});
      const r = await fetch(`/api/me`,{credentials:'include'});
      if(r.ok) setMe(await r.json());
      await loadReferrals();
    }catch(e){
      console.warn('referral reward claim failed', e);
      setRefRewards(pendingRewards);
    }finally{
      claimingRefRewardsRef.current = false;
    }
  }

  async function openPast(id){
    setPastResult(null);
    try{
      const r = await fetch(`/api/tournament/${id}`,{credentials:'include'});
      if(r.status===200){
        setPastResult(await r.json());
      }
    }catch(e){}
  }

  async function claimDaily(){
    setError(null);
    try{
      const r = await fetch(`/api/daily`,{method:'POST',credentials:'include'});
      if(r.status===409){
        setMe(p=> (p ? {
          ...p,
          dailyAvailable:false,
        } : p));
        dailyPromptDismissedRef.current = true;
        setDailyOpen(false);
        setError(t('dailyTaken'));
        return;
      }
      if(!r.ok){
        if(r.status===401) throw new Error('unauthorized');
        throw 0;
      }
      const d = await r.json();
      setMe(p=> (p ? {
        ...p,
        lures: d.lures,
        currentLureId:d.currentLureId,
        dailyAvailable:false,
        dailyStreak:d.dailyStreak
      } : p));
      dailyPromptDismissedRef.current = true;
      setDailyOpen(false);
    }catch(e){
      setError(e.message==='unauthorized'
        ? t('authRequired')
        : t('dailyTaken'));
    }
  }

  function openDaily(){
    dailyPromptDismissedRef.current = false;
    setDailyOpen(true);
  }

  const closeDailyModal = React.useCallback(()=>{
    dailyPromptDismissedRef.current = true;
    setDailyOpen(false);
  }, []);

  async function loadReferrals(){
    try{
      const r = await fetch(`/api/referrals`,{credentials:'include'});
      if(r.ok) setRefInfo(await r.json());
    }catch(e){}
  }

  async function generateRefLink(){
    try{
      const r = await fetch(`/api/referrals`,{method:'POST',credentials:'include'});
      if(r.ok){
        const d = await r.json();
        setRefInfo(p=>({invited:p?.invited || [], token:d.token, link:d.link}));
      }
    }catch(e){}
  }

  async function copyRefLink(){
    if(!refInfo) return;
    try{ await navigator.clipboard.writeText(refInfo.link); }catch(e){}
  }

  function toggleRef(){
    setRefOpen(o=>{
      const n=!o; if(n && !refInfo) loadReferrals(); return n;
    });
  }

  async function buyPack(id){
    setError(null);
    try{
      const initData = tg?.initData;
      const r = await fetch('/api/create-invoice',{
        method:'POST',
        headers:{'Content-Type':'application/json'},
        body: JSON.stringify({productId:id, initData})
      });
      if(!r.ok){
        if(r.status===401) throw new Error('unauthorized');
        let data = null;
        try{ data = await r.json(); }catch(err){}
        if(data?.error==='rod_unlocked'){
          setError(t('rodAlreadyUnlocked'));
          return;
        }
        throw 0;
      }
      const {invoice_url} = await r.json();
      tg?.openInvoice(invoice_url, async (status)=>{
        if(status==='paid'){
          try{
            const meResp = await fetch(`/api/me`,{credentials:'include'});
            if(meResp.ok){
              setMe(await meResp.json());
            }
            const shopResp = await fetch(`/api/shop`,{credentials:'include'});
            if(shopResp.ok){
              setShop(await shopResp.json());
            }
          }catch(err){}
        }
      });
    }catch(e){
      setError(e.message==='unauthorized'
        ? t('authRequired')
        : t('purchaseFailed'));
    }
  }

  async function buyPackWithCoins(id){
    setError(null);
    try{
      const r = await fetch(`/api/shop/${id}/coins`,{method:'POST',credentials:'include'});
      if(!r.ok){
        if(r.status===401) throw new Error('unauthorized');
        let data = null;
        try{ data = await r.json(); }catch(err){}
        if(data?.error==='not_enough_coins') throw new Error('not_enough_coins');
        if(data?.error==='coin_purchase_unavailable') throw new Error('coin_unavailable');
        throw new Error('purchase_failed');
      }
      try{
        const meResp = await fetch(`/api/me`,{credentials:'include'});
        if(meResp.ok){
          setMe(await meResp.json());
        }
      }catch(err){}
      try{
        const shopResp = await fetch(`/api/shop`,{credentials:'include'});
        if(shopResp.ok){
          setShop(await shopResp.json());
        }
      }catch(err){}
      return true;
    }catch(e){
      if(e.message==='unauthorized'){
        setError(t('authRequired'));
      }else if(e.message==='not_enough_coins'){
        setCoinPurchasePack(null);
        setCoinInsufficientOpen(true);
      }else{
        setError(t('purchaseFailed'));
      }
      return false;
    }
  }

  function requestCoinPurchase(id){
    if(!Array.isArray(shop) || shop.length===0) return;
    let pack = null;
    for(const category of shop){
      if(!category?.packs) continue;
      const found = category.packs.find(p=>p.id===id);
      if(found){
        pack = found;
        break;
      }
    }
    if(pack && typeof pack.coinPrice === 'number'){
      const currentCoins = typeof me?.coins === 'number' ? me.coins : null;
      if(currentCoins != null && currentCoins < Number(pack.coinPrice)){
        setCoinPurchaseProcessing(false);
        setCoinPurchasePack(null);
        setCoinInsufficientOpen(true);
        return;
      }
      setCoinPurchaseProcessing(false);
      setCoinPurchasePack(pack);
    }
  }

  function closeCoinPurchaseModal(){
    if(coinPurchaseProcessing) return;
    setCoinPurchasePack(null);
  }

  function closeInsufficientCoinsModal(){
    setCoinInsufficientOpen(false);
  }

  async function confirmCoinPurchase(){
    if(!coinPurchasePack || coinPurchaseProcessing) return;
    setCoinPurchaseProcessing(true);
    try{
      const success = await buyPackWithCoins(coinPurchasePack.id);
      if(success){
        setCoinPurchasePack(null);
      }
    }finally{
      setCoinPurchaseProcessing(false);
    }
  }

  async function selectLocation(id){
    setError(null);
    try{
      const r = await fetch(`/api/location/`+id,{method:'POST',credentials:'include'});
      if(!r.ok){
        if(r.status===401) throw new Error('unauthorized');
        throw r.status;
      }
      setMe(p=>({...p,locationId:id}));
    }catch(e){
      setError(e.message==='unauthorized'
        ? t('authRequired')
        : e===403? t('locationLocked') : t('changeLocationFailed'));
    }
  }

  async function saveNickname(nick){
    nick = (nick||'').replace(/[\u200E\u200F\u202A-\u202E\u2066-\u2069]/g,'').slice(0,30);
    if(!nick || nick===me?.username){
      setNickOpen(false);
      return;
    }
    try{
      const r = await fetch('/api/nickname',{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({nickname:nick})});
      if(r.ok){
        const body = await r.json().catch(()=>null);
        const sanitized = body?.nickname || nick;
        setMe(p=>({...p, username:sanitized, needsNickname:false}));
      }
    }catch(e){}
    finally{
      setNickOpen(false);
    }
  }

  async function toggleLanguage(){
    const newLang = me.language==='ru' ? 'en' : 'ru';
    try{
      await fetch('/api/language',{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({language:newLang})});
      const r = await fetch(`/api/me`,{credentials:'include'});
      if(r.ok){
        const m = await r.json();
        t = makeT(m.language);
        window.t = t;
        document.documentElement.lang = m.language;
        try{ localStorage.setItem('lang', m.language); }catch(e){}
        setMe(m);
      }
      const s = await fetch(`/api/shop`,{credentials:'include'});
      if(s.ok){ setShop(await s.json()); }
      const ct = await fetch(`/api/tournament/current`,{credentials:'include'});
      if(ct.status===200){ setCurrentTournament(await ct.json()); } else { setCurrentTournament(null); }
      const ut = await fetch(`/api/tournaments/upcoming`,{credentials:'include'});
      if(ut.status===200){
        setUpcomingTournaments(await ut.json());
      } else {
        setUpcomingTournaments([]);
      }
      const pt = await fetch(`/api/tournaments/past`,{credentials:'include'});
      if(pt.status===200){
        setPastTournaments(await pt.json());
      } else {
        setPastTournaments([]);
      }
    }catch(e){}
  }

  async function finalizeCatch(reaction, success){
    let caught = false;
    let autoFishActive = me.autoFish;
    try{
      const r = await fetch(`/api/cast`,{
        method:'POST', credentials:'include',
        headers:{'Content-Type':'application/json'},
        body:JSON.stringify({wait:waitRef.current, reaction, success})
      });
      if(!r.ok){
        if(r.status===401) throw new Error('unauthorized');
        throw 0;
      }
      const d = await r.json();
      caught = d.caught;
      autoFishActive = d.autoFish;
      setMe(p=>({...p,autoFish:d.autoFish}));
      if(!d.autoFish) setAutoCast(false);
      if(d.caught){
        const c = d.catch;
        const isNewFish = !(me.caughtFishIds||[]).includes(c.fishId);
        const newTotal = (me.totalWeight||0)+c.weight;
        const newLocs = me.locations.filter(l=>!l.unlocked && newTotal>=l.unlockKg).map(l=>l.name);
        const newRods = Array.isArray(d.unlockedRods) ? d.unlockedRods : [];
        const achievementUnlocks = Array.isArray(d.achievements)
          ? d.achievements.map(a => ({
              ...a,
              name: achievementNameByCode(a.code),
              levelLabel: typeof window.achievementLevelLabel === 'function'
                ? window.achievementLevelLabel(a.newLevelIndex, me.language)
                : a.newLevelIndex,
            }))
          : [];
        const questUpdates = Array.isArray(d.questUpdates) ? d.questUpdates : [];
        const animationId = ++catchAnimationIdRef.current;
        setResult({
          ...c,
          coins: typeof d.coins === 'number' ? d.coins : 0,
          todayCoins: typeof d.todayCoins === 'number' ? d.todayCoins : undefined,
          totalCoins: typeof d.totalCoins === 'number' ? d.totalCoins : undefined,
          newFish:isNewFish,newLocations:newLocs,newRods,animationId,achievements: achievementUnlocks,questUpdates
        });
            setMe(p=>{
              const tot = (p.totalWeight||0)+c.weight;
              const totalCoins = typeof d.totalCoins === 'number' ? d.totalCoins : p.coins;
              const todayCoins = typeof d.todayCoins === 'number' ? d.todayCoins : p.todayCoins;
              return {
                ...p,
                totalWeight:tot,
                todayWeight:(p.todayWeight||0)+c.weight,
                coins: totalCoins,
                todayCoins: todayCoins,
                locations:p.locations.map(l=> l.unlocked || tot>=l.unlockKg ? {...l,unlocked:true} : l),
                rods:(p.rods||[]).map(r=> r.unlocked || tot>=r.unlockKg ? {...r,unlocked:true} : r),
                recent:[{id:c.id,fish:c.fish,weight:c.weight,location:c.location,rarity:c.rarity,at:new Date().toISOString()},...(p.recent||[])].slice(0,5),
                caughtFishIds: isNewFish ? [...(p.caughtFishIds||[]), c.fishId] : p.caughtFishIds
              };
            });
        if(achievementUnlocks.length>0){
          loadAchievements();
        }
        if(questUpdates.length>0){
          loadQuests();
        }
        try{
          const ct = await fetch(`/api/tournament/current`,{credentials:'include'});
          if(ct.status===200){
            setCurrentTournament(await ct.json());
          } else {
            setCurrentTournament(null);
          }
        }catch(e){}
      } else {
        setError(t('fishEscaped'));
      }
    }catch(e){
      setError(e.message==='unauthorized'
        ? t('authRequired')
        : t('castOften'));
    }finally{
      castingRef.current = false;
      setCasting(false);
      startCastCooldown();
      tapFinishingRef.current = false;
      tapReactionRef.current = 0;
      if(caught && autoCastRef.current && autoFishActive){
        autoCastTimeoutRef.current = setTimeout(()=>cast(true), CAST_READY_DELAY_MS);
      }
    }
  }

  function startTapChallenge(reaction){
    tapFinishingRef.current = false;
    tapReactionRef.current = reaction;
    tapDeadlineRef.current = Date.now() + TAP_CHALLENGE_DURATION_MS;
    tapCountRef.current = 0;
    if(tapTimerRef.current){
      cancelAnimationFrame(tapTimerRef.current);
      tapTimerRef.current = null;
    }
    tapActiveRef.current = true;
    setTapCount(0);
    setTapTimeLeft(TAP_CHALLENGE_DURATION_MS/1000);
    setTapActive(true);
  }

  function finishTap(success){
    if(tapFinishingRef.current) return;
    tapFinishingRef.current = true;
    tapActiveRef.current = false;
    if(tapTimerRef.current){
      cancelAnimationFrame(tapTimerRef.current);
      tapTimerRef.current = null;
    }
    setTapActive(false);
    finalizeCatch(tapReactionRef.current || FAIL_REACTION_SECONDS, success);
  }

  function handleTap(){
    if(!tapActiveRef.current) return;
    const next = tapCountRef.current + 1;
    tapCountRef.current = next;
    setTapCount(next);
    if(next >= TAP_CHALLENGE_GOAL){
      finishTap(true);
    }
  }

  async function cast(auto=false){
    if(!auto && !castReadyRef.current) return;
    if(castingRef.current) return; if(!me) return;
    const curId = me.currentLureId;
    const curLure = me.lures.find(l=>l.id===curId);
    if(!curLure || curLure.qty<=0){ setError(t('noBaits')); return; }
    setError(null);
    setDrawerOpen(false);
    setBaitsOpen(false);
    try{
      const r = await fetch(`/api/start-cast`,{method:'POST',credentials:'include'});
      if(!r.ok){
        if(r.status===401) throw new Error('unauthorized');
        const e=await r.json().catch(()=>({}));
        throw new Error(e.error||'failed');
      }
      const {currentLureId} = await r.json();
      setMe(p=>({
        ...p,
        currentLureId,
        lures:p.lures.map(l=> l.id===curId ? {...l,qty:Math.max(0,l.qty-1)} : l)
      }));
    }catch(e){
      setError(e.message==='unauthorized'
        ? t('authRequired')
        : e.message==='failed'? t('castFailed')
        : e.message==='casting'? t('alreadyCasting')
        : e.message==='No suitable fish'? t('noSuitableFish')
        : (e.message||t('wrongLure')));
      return;
    }
    castingRef.current = true;
    setCasting(true);
    setResult(null);
    const wait = 5 + Math.floor(Math.random()*26);
    waitRef.current = wait;
    await new Promise(r=>setTimeout(r, wait*1000));
    biteTimeRef.current = Date.now();
    setBiting(true);
    bitingRef.current = true;
    hookTimeoutRef.current = setTimeout(()=>hook(true),5000);
  }

  async function hook(auto=false){
    if(!bitingRef.current) return;
    const reaction = auto ? FAIL_REACTION_SECONDS : (Date.now()-biteTimeRef.current)/1000;
    clearTimeout(hookTimeoutRef.current);
    hookTimeoutRef.current = null;
    setBiting(false);
    bitingRef.current = false;
    setError(null);
    try{
      const resp = await fetch(`/api/hook`,{
        method:'POST', credentials:'include',
        headers:{'Content-Type':'application/json'},
        body:JSON.stringify({wait:waitRef.current, reaction})
      });
      if(!resp.ok){
        if(resp.status===401) throw new Error('unauthorized');
        throw 0;
      }
      const data = await resp.json();
      setMe(p=>({...p, autoFish:data.autoFish}));
      if(!data.autoFish) setAutoCast(false);
      if(!data.success){
        setError(t('fishEscaped'));
        castingRef.current = false;
        setCasting(false);
        startCastCooldown();
        tapActiveRef.current = false;
        setTapActive(false);
        tapFinishingRef.current = false;
        tapReactionRef.current = 0;
        tapDeadlineRef.current = 0;
        tapCountRef.current = 0;
        setTapCount(0);
        setTapTimeLeft(TAP_CHALLENGE_DURATION_MS/1000);
        if(tapTimerRef.current){
          cancelAnimationFrame(tapTimerRef.current);
          tapTimerRef.current = null;
        }
        return;
      }
      startTapChallenge(reaction);
    }catch(e){
      setError(e.message==='unauthorized'
        ? t('authRequired')
        : t('castOften'));
      castingRef.current = false;
      setCasting(false);
      startCastCooldown();
      tapActiveRef.current = false;
      setTapActive(false);
      tapFinishingRef.current = false;
      tapReactionRef.current = 0;
      tapDeadlineRef.current = 0;
      tapCountRef.current = 0;
      setTapCount(0);
      setTapTimeLeft(TAP_CHALLENGE_DURATION_MS/1000);
      if(tapTimerRef.current){
        cancelAnimationFrame(tapTimerRef.current);
        tapTimerRef.current = null;
      }
    }
  }

  React.useEffect(()=>()=>{
    if(hookTimeoutRef.current){
      clearTimeout(hookTimeoutRef.current);
    }
    if(autoCastTimeoutRef.current){
      clearTimeout(autoCastTimeoutRef.current);
    }
    if(tapTimerRef.current){
      cancelAnimationFrame(tapTimerRef.current);
    }
  },[]);

  React.useEffect(()=>{
    const handler = ()=>{
      tapActiveRef.current = false;
      setTapActive(false);
      if(tapTimerRef.current){
        cancelAnimationFrame(tapTimerRef.current);
        tapTimerRef.current = null;
      }
    };
    window.addEventListener('blur', handler);
    return ()=> window.removeEventListener('blur', handler);
  },[]);

  if(loading){
    return (
      <div className="app-content flex items-center justify-center">
        <div className="animate-pulse text-center">
          <div className="text-2xl font-semibold">RiverKing</div>
          <div className="opacity-70 mt-2">{loadingHint}</div>
        </div>
      </div>
    );
  }

  if(!me){
    return (
      <div className="app-content flex items-center justify-center">
        <div className="text-center">
          <div className="text-2xl font-semibold">RiverKing</div>
          <div className="opacity-70 mt-2">{error || t('loadFailed')}</div>
        </div>
      </div>
    );
  }

  return (
    <div className="app-content w-full px-4 flex justify-center" onClick={()=>setPrizeHint(null)}>
      <div className="w-full max-w-5xl xl:max-w-6xl flex flex-col min-h-full">
        {prize && (
          <div className="fixed inset-0 flex items-center justify-center bg-black/70 z-50" onClick={claimPrize}>
            <div className="glass p-6 rounded-xl text-center animate-pop">
              <AssetImage src={BOBBER_ICON} alt="prize" className="w-20 h-20 mx-auto mb-3 animate-bounce object-contain" />
              <div className="mb-2">{t('prizeCongrats', prize.rank)}</div>
              <div className="text-lg font-semibold mb-1">
                {prize.packageId === 'coins' || typeof prize.coins === 'number'
                  ? <>🪙 +{Number(prize.coins ?? prize.qty).toLocaleString(coinLocale)}</>
                  : (<>
                    {
                      (
                        shop
                          .reduce((acc, c) => acc.concat(c.packs), [])
                          .find(p => p.id === prize.packageId)?.name
                      ) || (prize.packageId === 'autofish_week' ? t('autofishWeek') : prize.packageId)
                    }
                    {prize.qty > 1 ? ` x${prize.qty}` : ''}
                  </>)
                }
              </div>
              <div className="text-sm opacity-80 mt-2">{t('tapToClaim')}</div>
            </div>
          </div>
        )}
        {achievementReward && (
          <div className="fixed inset-0 flex items-center justify-center bg-black/70 z-50" onClick={()=>setAchievementReward(null)}>
            <div className="glass p-6 rounded-xl text-center animate-pop">
              <AssetImage src={BOBBER_ICON} alt="achievement" className="w-20 h-20 mx-auto mb-3 animate-bounce object-contain" />
              <div className="text-lg font-semibold mb-1">{t('achievementRewardTitle')}</div>
              <div className="text-sm opacity-80 mb-2">{achievementReward.name || achievementReward.code}</div>
              <div className="text-left inline-block text-sm font-semibold space-y-1">
                {(achievementReward.rewards||[]).map((reward, idx)=>(
                  <div key={`${achievementReward.code}-${idx}`} className="flex items-center gap-2">
                    <span>•</span>
                    <span>{achievementRewardLabel(reward)}</span>
                  </div>
                ))}
              </div>
              <div className="text-sm opacity-80 mt-2">{t('tapToClaim')}</div>
            </div>
          </div>
        )}
        {refRewards && (
          <div className="fixed inset-0 flex items-center justify-center bg-black/70 z-50" onClick={claimRefRewards}>
            <div className="glass p-6 rounded-xl text-center animate-pop">
              <AssetImage src={BOBBER_ICON} alt="reward" className="w-20 h-20 mx-auto mb-3 animate-bounce object-contain" />
              <div className="mb-2">{t(
                (!refInfo || refInfo.invited.length===0)
                  ? 'welcomeBonus'
                  : refRewards.some(r=>r.packageId==='bundle_starter')
                    ? 'referralBonus'
                    : 'referralPurchaseBonus'
              )}</div>
              {refRewards.map(r => (
                <div key={r.packageId} className="text-lg font-semibold mb-1">
                  {
                    r.name
                    || shop.reduce((acc,c)=>acc.concat(c.packs),[]).find(p=>p.id===r.packageId)?.name
                    || (r.packageId==='autofish_week' ? t('autofishWeek') : r.packageId)
                  } x{r.qty}
                </div>
              ))}
              <div className="text-sm opacity-80 mt-2">{t('tapToClaim')}</div>
            </div>
          </div>
        )}
        <Header
          me={me}
          lang={me.language}
          onEditNickname={()=>setNickOpen(true)}
          onOpenLocations={!casting ? ()=>setDrawerOpen(true) : undefined}
          onOpenBaits={!casting ? ()=>setBaitsOpen(true) : undefined}
          onOpenRods={!casting ? ()=>setRodsOpen(true) : undefined}
          onToggleLanguage={toggleLanguage}
        />
        {nickOpen && <NicknameModal me={me} onClose={()=>setNickOpen(false)} onSave={saveNickname} />}
        {dailyOpen && <DailyModal streak={me.dailyStreak} available={me.dailyAvailable} rewards={me.dailyRewards} onClose={closeDailyModal} onClaim={claimDaily} />}
        {catchDetails && <CatchDetailsModal catchData={catchDetails} me={me} onClose={()=>setCatchDetails(null)} />}
        {coinPurchasePack && (
          <div
            className="fixed inset-0 flex items-center justify-center bg-black/70 z-50"
            onClick={()=>{ if(!coinPurchaseProcessing) closeCoinPurchaseModal(); }}
          >
            <div
              className="glass p-5 rounded-xl max-w-xs w-[90%] text-center animate-pop"
              onClick={e=>e.stopPropagation()}
            >
              <div className="text-lg font-semibold mb-2">{t('confirmCoinPurchaseTitle')}</div>
              <div className="text-sm opacity-80">
                {t('confirmCoinPurchaseText', {
                  name: coinPurchasePack.name || coinPurchasePack.id,
                  price: coinPurchasePriceLabel
                })}
              </div>
              <div className="flex gap-3 mt-5 text-sm">
                <button
                  type="button"
                  className="flex-1 px-3 py-1.5 rounded-xl bg-red-600 hover:bg-red-500 disabled:opacity-60 disabled:hover:bg-red-600"
                  onClick={closeCoinPurchaseModal}
                  disabled={coinPurchaseProcessing}
                >{t('cancel')}</button>
                <button
                  type="button"
                  className="flex-1 px-3 py-1.5 rounded-xl bg-yellow-400 text-black hover:bg-yellow-300 disabled:opacity-60 disabled:hover:bg-yellow-400"
                  onClick={confirmCoinPurchase}
                  disabled={coinPurchaseProcessing}
                >{t('confirmCoinPurchaseButton', coinPurchasePriceLabel)}</button>
              </div>
            </div>
          </div>
        )}

        {coinInsufficientOpen && (
          <div
            className="fixed inset-0 flex items-center justify-center bg-black/70 z-50"
            onClick={closeInsufficientCoinsModal}
          >
            <div
              className="glass p-5 rounded-xl max-w-xs w-[90%] text-center animate-pop"
              onClick={e=>e.stopPropagation()}
            >
              <div className="text-lg font-semibold mb-2">{t('notEnoughCoinsTitle')}</div>
              <div className="text-sm opacity-80 mb-4">{t('notEnoughCoins')}</div>
              <button
                type="button"
                className="w-full px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500"
                onClick={closeInsufficientCoinsModal}
              >{t('close')}</button>
            </div>
          </div>
        )}

        <div className="flex-1 flex flex-col">
          {tab === 'fish' && (
            <FishingTab
              me={me}
              setMe={setMe}
              casting={casting}
              biting={biting}
              tapActive={tapActive}
              tapCount={tapCount}
              tapTimeLeft={tapTimeLeft}
              castReady={castReady}
              onCast={()=>cast(false)}
              onHook={()=>hook(false)}
              onTap={handleTap}
              result={result}
              error={error}
              autoCast={autoCast}
              setAutoCast={setAutoCast}
              autoCastRef={autoCastRef}
              autoCastTimeoutRef={autoCastTimeoutRef}
              hasCatchAnimationBeenShown={hasCatchAnimationBeenShown}
              markCatchAnimationShown={markCatchAnimationShown}
              onCatchClick={handleCatchClick}
              onOpenQuests={openQuests}
            />
          )}

          {tab === 'tournaments' && (
            <TournamentsTab
              me={me}
              tournamentTab={tournamentTab}
              setTournamentTab={setTournamentTab}
              currentTournament={currentTournament}
              upcomingTournaments={upcomingTournaments}
              pastTournaments={pastTournaments}
              pastResult={pastResult}
              openPast={openPast}
              setPastResult={setPastResult}
              prizeHint={prizeHint}
              setPrizeHint={setPrizeHint}
              shop={shop}
              onCatchClick={handleCatchClick}
            />
          )}
          {tab === 'ratings' && (
            <Ratings me={me} setMe={setMe} onCatchClick={handleCatchClick} />
          )}
          {tab === 'shop' && (
            <ShopTab
              shop={shop}
              toggleRef={toggleRef}
              refOpen={refOpen}
              refInfo={refInfo}
              copyRefLink={copyRefLink}
              generateRefLink={generateRefLink}
              starterPackName={starterPackName}
              buyPack={buyPack}
              dailyAvailable={me.dailyAvailable}
              onOpenDaily={openDaily}
              onCoinPurchaseRequest={requestCoinPurchase}
            />
          )}
          {tab === 'guide' && (
            <Guide
              me={me}
              achievements={achievements}
              achievementsLoading={achievementsLoading}
              achievementsError={achievementsError}
              onReloadAchievements={loadAchievements}
              onClaimAchievement={claimAchievement}
              claimInProgress={claimingAchievement}
              achievementsClaimable={achievementsClaimable}
            />
          )}
        </div>

        <BottomNav
          tab={tab}
          setTab={setTab}
          dailyAvailable={me.dailyAvailable}
          achievementsAvailable={achievementsClaimable}
        />

        <QuestsDrawer
          open={questsOpen}
          onClose={()=>setQuestsOpen(false)}
          quests={quests}
          loading={questsLoading}
          error={questsError}
          onReload={loadQuests}
        />
        <LocationsDrawer open={drawerOpen} onClose={()=>setDrawerOpen(false)} me={me} onSelect={selectLocation} />
        <BaitsDrawer open={baitsOpen} onClose={()=>setBaitsOpen(false)} me={me} onSelect={async id=>{
          try{
            const r = await fetch(`/api/lure/`+id,{method:'POST',credentials:'include'});
            if(!r.ok){
              if(r.status===401) throw new Error('unauthorized');
              throw 0;
            }
            setMe(p=>({...p,currentLureId:id}));
          }catch(e){
            setError(e.message==='unauthorized'
              ? t('authRequired')
              : t('selectBaitFailed'));
          }
        }} />
        <RodsDrawer open={rodsOpen} onClose={()=>setRodsOpen(false)} me={me}
          onSelect={async id=>{
          try{
            const r = await fetch(`/api/rod/`+id,{method:'POST',credentials:'include'});
            if(!r.ok){
              if(r.status===401) throw new Error('unauthorized');
              const err = await r.json().catch(()=>({}));
              throw new Error(err.error||'failed');
            }
            setMe(p=>({...p,currentRodId:id}));
          }catch(e){
            const reason = e.message;
            setError(reason==='unauthorized'
              ? t('authRequired')
              : reason==='casting' ? t('rodCasting')
              : reason==='locked' ? t('rodLocked')
              : reason==='no rod' ? t('rodUnavailable')
              : t('selectRodFailed'));
          }
        }}
          onUnlock={rod=>{
            if(!rod?.packId) return;
            buyPack(rod.packId);
          }}
        />
      </div>
    </div>
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
