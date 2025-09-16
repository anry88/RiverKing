const tg = window.Telegram?.WebApp;
console.log('initData length:', tg?.initData?.length || 0, 'platform:', tg?.platform);

const params = new URLSearchParams(window.location.search);
const refToken = tg?.initDataUnsafe?.start_param
  || params.get('tgWebAppStartParam')
  || params.get('ref');
const tgParam = (()=>{
  try{
    const raw = window.location.search + window.location.hash;
    const m = raw.match(/(?:^|[?&#])tgWebAppData=([^&]+)/);
    return m ? decodeURIComponent(m[1]) : null;
  }catch{ return null; }
})();

const WAIT_MIN_MS = 5000;
const WAIT_MAX_MS = 25000;
const TAP_CHANCE = 0.22;
const TAP_REACTION_MIN = 0.6;
const TAP_REACTION_MAX = 2.6;
const DEFAULT_REACTION_WINDOW_MS = 5000;
const FAIL_REACTION_SECONDS = 5.1;

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
  const [dailyOpen,setDailyOpen] = React.useState(false);
  const [refOpen,setRefOpen] = React.useState(false);
  const [refInfo,setRefInfo] = React.useState(null);
  const [refRewards,setRefRewards] = React.useState(null);
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
    if(tournamentTab!=='past') setPastResult(null);
  },[tournamentTab]);

  React.useEffect(()=>{
    try{ tg?.ready(); tg?.expand(); tg?.MainButton?.hide?.(); tg?.enableClosingConfirmation?.(); }catch(e){}
    try{
      Object.values(LOCATION_BG)
        .concat([ROD_IMG, '/app/assets/riverking_bobber.svg'])
        .forEach(src=>{ new Image().src = src; });
    }catch(e){}
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
        setMe(d);
        try{
          const s = await fetch(`/api/shop`,{credentials:'include'});
          if(s.ok){
            setShop(await s.json());
          }
        }catch(e){
          if(e.message==='unauthorized'){
            setError(t('authRequired'));
            setShop([]);
          }
        }
        try{
          const ct = await fetch(`/api/tournament/current`,{credentials:'include'});
          if(ct.status===200){
            setCurrentTournament(await ct.json());
          } else {
            setCurrentTournament(null);
          }
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
          const pr = await fetch(`/api/prizes`,{credentials:'include'});
          if(pr.ok){
            const list = await pr.json();
            if(list.length>0) setPrize(list[0]);
          }
          const rr = await fetch(`/api/referrals/rewards`,{credentials:'include'});
          if(rr.ok){
            const rewardsList = await rr.json();
            if(rewardsList.length>0){
              await loadReferrals();
              setRefRewards(rewardsList);
            }
          }
        }catch(e){
          if(e.message==='unauthorized'){
            setError(t('authRequired'));
            setCurrentTournament(null);
            setUpcomingTournaments([]);
            setPastTournaments([]);
          }
        }
      }catch(e){
        if(e.message==='unauthorized'){
          setError(t('authRequired'));
          setShop([]);
        } else {
          setError(t('loadProfileFailed'));
          t = makeT(initLang);
          window.t = t;
          document.documentElement.lang = initLang;
          try{ localStorage.setItem('lang', initLang); }catch(err){}
          setMe({
            username:'angler',
            needsNickname:false,
            language:initLang,
            lures:[
              {id:1,name:'Пресная мирная',qty:5,predator:false,water:'fresh',rarityBonus:0},
              {id:2,name:'Пресная хищная',qty:5,predator:true,water:'fresh',rarityBonus:0},
              {id:3,name:'Морская мирная',qty:5,predator:false,water:'salt',rarityBonus:0},
              {id:4,name:'Морская хищная',qty:5,predator:true,water:'salt',rarityBonus:0},
              {id:5,name:'Пресная мирная+',qty:2,predator:false,water:'fresh',rarityBonus:0.3},
              {id:6,name:'Пресная хищная+',qty:2,predator:true,water:'fresh',rarityBonus:0.3},
              {id:7,name:'Морская мирная+',qty:2,predator:false,water:'salt',rarityBonus:0.3},
              {id:8,name:'Морская хищная+',qty:2,predator:true,water:'salt',rarityBonus:0.3},
            ],
            currentLureId:1,
            totalWeight:0, todayWeight:0, locationId:1,
            locations:[
              {id:1,name:'Пруд',unlockKg:0,unlocked:true},
              {id:2,name:'Река',unlockKg:10,unlocked:false},
              {id:3,name:'Озеро',unlockKg:50,unlocked:false},
            ],
            caughtFishIds:[],
            recent:[], dailyAvailable:true, dailyStreak:0,
          });
          setShop([]);
        }
      }finally{ setLoading(false); }
    })();
  },[]);

  async function claimPrize(){
    if(!prize) return;
    try{
      await fetch(`/api/prizes/${prize.id}/claim`,{method:'POST',credentials:'include'});
      const r = await fetch(`/api/me`,{credentials:'include'});
      if(r.ok) setMe(await r.json());
    }catch(e){}
    setPrize(null);
  }

  async function claimRefRewards(){
    if(!refRewards) return;
    try{
      await fetch(`/api/referrals/rewards/claim`,{method:'POST',credentials:'include'});
      const r = await fetch(`/api/me`,{credentials:'include'});
      if(r.ok) setMe(await r.json());
    }catch(e){}
    setRefRewards(null);
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
      if(!r.ok){
        if(r.status===401) throw new Error('unauthorized');
        throw 0;
      }
      const d = await r.json();
      setMe(p=>({
        ...p,
        lures: d.lures,
        currentLureId:d.currentLureId,
        dailyAvailable:false,
        dailyStreak:d.dailyStreak
      }));
      setDailyOpen(false);
    }catch(e){
      setError(e.message==='unauthorized'
        ? t('authRequired')
        : t('dailyTaken'));
    }
  }

  function openDaily(){
    setDailyOpen(true);
  }

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

  function saveNickname(nick){
    nick = (nick||'').replace(/[\u200E\u200F\u202A-\u202E\u2066-\u2069]/g,'').slice(0,30);
    if(!nick || nick===me?.username){
      setNickOpen(false);
      return;
    }
    fetch('/api/nickname',{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({nickname:nick})})
      .then(()=> setMe(p=>({...p, username:nick, needsNickname:false})))
      .catch(()=>{})
      .finally(()=> setNickOpen(false));
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

  function finishCast(caught, autoFishActive){
    if(hookTimeoutRef.current){
      clearTimeout(hookTimeoutRef.current);
      hookTimeoutRef.current = null;
    }
    if(tapTimerRef.current){
      cancelAnimationFrame(tapTimerRef.current);
      tapTimerRef.current = null;
    }
    tapFinishingRef.current = false;
    tapReactionRef.current = 0;
    tapActiveRef.current = false;
    setTapActive(false);
    setTapCount(0);
    setTapTimeLeft(TAP_CHALLENGE_DURATION_MS/1000);
    bitingRef.current = false;
    setBiting(false);
    castingRef.current = false;
    setCasting(false);
    if(autoCastTimeoutRef.current){
      clearTimeout(autoCastTimeoutRef.current);
      autoCastTimeoutRef.current = null;
    }
    waitRef.current = 0;
    startCastCooldown();
    if(caught && autoCastRef.current && autoFishActive){
      autoCastTimeoutRef.current = setTimeout(()=>cast(true), CAST_READY_DELAY_MS);
    }
  }

  async function finalizeCatch(reactionSeconds){
    const waitSeconds = Math.max(5, Math.round(waitRef.current || 0));
    let hookSuccess = false;
    let autoFishActive = me.autoFish;
    try{
      const hookRes = await fetch(`/api/hook`,{
        method:'POST',
        credentials:'include',
        headers:{'Content-Type':'application/json'},
        body:JSON.stringify({wait:waitSeconds, reaction:reactionSeconds})
      });
      if(!hookRes.ok){
        if(hookRes.status===401) throw new Error('unauthorized');
        if(hookRes.status===429){
          setError(t('castOften'));
          finishCast(false, autoFishActive);
          return;
        }
        let msg;
        try{ msg = (await hookRes.json())?.error; }catch(e){}
        if(msg === 'No suitable fish') setError(t('noSuitableFish'));
        else if(msg === 'No baits') setError(t('noBaits'));
        else if(msg === 'No lure selected') setError(t('selectBaitFailed'));
        else if(msg === 'locked') setError(t('locationLocked'));
        else setError(t('castFailed'));
        finishCast(false, autoFishActive);
        return;
      }
      const hookData = await hookRes.json();
      hookSuccess = hookData.success;
      autoFishActive = hookData.autoFish;
      if(!hookSuccess){
        setError(t('fishEscaped'));
        finishCast(false, autoFishActive);
        return;
      }
    }catch(e){
      setError(e.message==='unauthorized' ? t('authRequired') : t('castFailed'));
      finishCast(false, autoFishActive);
      return;
    }

    let caught = false;
    try{
      const r = await fetch(`/api/cast`,{
        method:'POST', credentials:'include',
        headers:{'Content-Type':'application/json'},
        body:JSON.stringify({wait:waitSeconds, reaction:reactionSeconds, success:hookSuccess})
      });
      if(!r.ok){
        if(r.status===401) throw new Error('unauthorized');
        if(r.status===429){
          setError(t('castOften'));
          finishCast(false, autoFishActive);
          return;
        }
        let msg;
        try{ msg = (await r.json())?.error; }catch(e){}
        if(msg === 'No suitable fish') setError(t('noSuitableFish'));
        else if(msg === 'No baits') setError(t('noBaits'));
        else if(msg === 'No lure selected') setError(t('selectBaitFailed'));
        else setError(t('castFailed'));
        finishCast(false, autoFishActive);
        return;
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
        setResult({...c,newFish:isNewFish,newLocations:newLocs});
        setMe(p=>{
          const tot = (p.totalWeight||0)+c.weight;
          return {
            ...p,
            totalWeight:tot,
            todayWeight:(p.todayWeight||0)+c.weight,
            locations:p.locations.map(l=> l.unlocked || tot>=l.unlockKg ? {...l,unlocked:true} : l),
            recent:[{fish:c.fish,weight:c.weight,location:c.location,rarity:c.rarity,at:new Date().toISOString()},...(p.recent||[])].slice(0,5),
            caughtFishIds: isNewFish ? [...(p.caughtFishIds||[]), c.fishId] : p.caughtFishIds
          };
        });
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
      finishCast(caught, autoFishActive);
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
    const reaction = success
      ? (tapReactionRef.current || 1.5)
      : FAIL_REACTION_SECONDS;
    finalizeCatch(reaction);
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
        if(r.status===409){ setError(t('alreadyCasting')); return; }
        throw 0;
      }
      const d = await r.json();
      const nextCurrentId = d.currentLureId;
      const hasNextCurrent = nextCurrentId !== undefined && nextCurrentId !== null;
      setMe(p=>{
        if(!p) return p;
        const updatedLures = (p.lures||[]).map(l=> l.id===curId ? {...l, qty: Math.max(0, l.qty-1)} : l);
        return {
          ...p,
          lures: updatedLures,
          currentLureId: hasNextCurrent ? nextCurrentId : p.currentLureId
        };
      });
      const waitMs = Math.round(WAIT_MIN_MS + Math.random() * (WAIT_MAX_MS - WAIT_MIN_MS));
      const waitSeconds = Math.max(5, Math.round(waitMs / 1000));
      waitRef.current = waitSeconds;
      const hasTapChallenge = Math.random() < TAP_CHANCE;
      const tapReaction = hasTapChallenge
        ? TAP_REACTION_MIN + Math.random() * (TAP_REACTION_MAX - TAP_REACTION_MIN)
        : null;
      bitingRef.current = false;
      setBiting(false);
      castingRef.current = true;
      setCasting(true);
      setResult(null);
      hookTimeoutRef.current = setTimeout(()=>{
        bitingRef.current = true;
        setBiting(true);
        biteTimeRef.current = Date.now();
        hookTimeoutRef.current = null;
        if(hasTapChallenge && tapReaction!=null){
          startTapChallenge(tapReaction);
        } else {
          hookTimeoutRef.current = setTimeout(()=>{
            finalizeCatch(FAIL_REACTION_SECONDS);
          }, DEFAULT_REACTION_WINDOW_MS);
        }
      }, waitMs);
    }catch(e){
      setError(e.message==='unauthorized'
        ? t('authRequired')
        : t('castFailed'));
    }
  }

  function hook(auto=false){
    if(hookTimeoutRef.current){
      clearTimeout(hookTimeoutRef.current);
      hookTimeoutRef.current = null;
    }
    if(tapActiveRef.current){
      finishTap(tapCountRef.current >= TAP_CHALLENGE_GOAL);
      return;
    }
    if(!bitingRef.current){
      if(auto) return;
      setError(t('wrongLure'));
      return;
    }
    const reaction = Math.max(0, (Date.now() - biteTimeRef.current) / 1000);
    finalizeCatch(reaction);
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
    if(!casting) return;
    return ()=>{
      if(hookTimeoutRef.current){
        clearTimeout(hookTimeoutRef.current);
        hookTimeoutRef.current = null;
      }
    };
  },[casting]);

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
    <div className="app-content w-full px-4" onClick={()=>setPrizeHint(null)}>
      <div className="pb-safe">
        {prize && (
          <div className="fixed inset-0 flex items-center justify-center bg-black/70 z-50" onClick={claimPrize}>
            <div className="glass p-6 rounded-xl text-center animate-pop">
              <img src="/app/assets/riverking_bobber.svg" alt="prize" className="w-20 h-20 mx-auto mb-3 animate-bounce" />
              <div className="mb-2">{t('prizeCongrats', prize.rank)}</div>
              <div className="text-lg font-semibold mb-1">
                {
                  (
                    shop
                      .reduce((acc, c) => acc.concat(c.packs), [])
                      .find(p => p.id === prize.packageId)?.name
                  ) || prize.packageId
                } x{prize.qty}
              </div>
              <div className="text-sm opacity-80 mt-2">{t('tapToClaim')}</div>
            </div>
          </div>
        )}
        {refRewards && (
          <div className="fixed inset-0 flex items-center justify-center bg-black/70 z-50" onClick={claimRefRewards}>
            <div className="glass p-6 rounded-xl text-center animate-pop">
              <img src="/app/assets/riverking_bobber.svg" alt="reward" className="w-20 h-20 mx-auto mb-3 animate-bounce" />
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
          tab={tab}
          setTab={setTab}
          onEditNickname={()=>setNickOpen(true)}
          onOpenLocations={!casting ? ()=>setDrawerOpen(true) : undefined}
          onOpenBaits={!casting ? ()=>setBaitsOpen(true) : undefined}
          onToggleLanguage={toggleLanguage}
        />
        {nickOpen && <NicknameModal me={me} onClose={()=>setNickOpen(false)} onSave={saveNickname} />}
        {dailyOpen && <DailyModal streak={me.dailyStreak} available={me.dailyAvailable} onClose={()=>setDailyOpen(false)} onClaim={claimDaily} />}

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
            onClaimDaily={openDaily}
            autoCast={autoCast}
            setAutoCast={setAutoCast}
            autoCastRef={autoCastRef}
            autoCastTimeoutRef={autoCastTimeoutRef}
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
          />
        )}
        {tab === 'achievements' && (
          <Achievements me={me} setMe={setMe} />
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
          />
        )}
        {tab === 'guide' && (
          <Guide me={me} />
        )}

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
      </div>
    </div>
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
