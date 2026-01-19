const AssetImage = window.AssetImage;
const useAssetSrc = window.useAssetSrc;

const BOTTOM_NAV_ITEMS = Object.freeze([
  {id:'fish', label:() => t('fishing'), icon:'/app/assets/menu/fishing.png'},
  {id:'tournaments', label:() => t('tournaments'), icon:'/app/assets/menu/tournaments.png'},
  {id:'ratings', label:() => t('ratings'), icon:'/app/assets/menu/ratings.png'},
  {id:'guide', label:() => t('guide'), icon:'/app/assets/menu/guide.png'},
  {id:'shop', label:() => t('shop'), icon:'/app/assets/menu/shop.png'},
]);
window.BOTTOM_NAV_ITEMS = BOTTOM_NAV_ITEMS.map(item => ({...item}));

function StatChip({icon,label,value,active=false,onClick}){
  const iconContent = typeof icon === 'string'
    ? <span className="text-base" aria-hidden="true">{icon}</span>
    : icon
      ? <span className="w-6 h-6 flex items-center justify-center" aria-hidden="true">{icon}</span>
      : null;
  return (
    <button type="button" onClick={onClick}
      className={`h-10 min-w-[92px] px-3 rounded-xl flex items-center justify-center gap-2 shrink-0 glass ${onClick?'hover:bg-white/10':'pointer-events-none'} ${active?'ring-1 ring-emerald-500':''}`}>
      {iconContent}
      <div className="leading-tight">
        <div className="text-[10px] uppercase opacity-70">{label}</div>
        <div className="text-sm font-semibold whitespace-nowrap">{value}</div>
      </div>
    </button>
  );
}

function Header({me,lang,onEditNickname,onOpenLocations,onOpenBaits,onOpenRods,onToggleLanguage}){
  const currentLoc = (me.locations.find(x=>x.id===me.locationId)||{}).name||'—';
  const curLure = me.lures.find(l=>l.id===me.currentLureId);
  const lureName = curLure?.displayName || (curLure ? translateLure(curLure.name) : null);
  const lureVal = curLure? `${lureName} (${curLure.qty})` : '—';
  const lureIconPath = curLure ? getLureIcon(curLure) : null;
  const currentRod = (me.rods||[]).find(r=>r.id===me.currentRodId);
  const rodVal = currentRod ? currentRod.name : '—';
  const languages = [
    {code:'ru', label:'🇷🇺 RU'},
    {code:'en', label:'🇺🇸 EN'},
  ];
  return (
      <div className="-mx-4">
      <div className="app-header backdrop-blur bg-black/30 border-b border-white/10 flex flex-col items-center justify-center gap-1 text-center">
        <div className="flex items-center gap-2 text-[11px] font-medium">
          {languages.map(option=>(
            <button
              key={option.code}
              type="button"
              onClick={()=>{ if(lang!==option.code) onToggleLanguage?.(); }}
              className={`px-2 py-1 rounded-md transition-colors ${lang===option.code ? 'bg-white/15 text-white' : 'text-white/60 hover:text-white/80'}`}
              aria-pressed={lang===option.code}
            >
              {option.label}
            </button>
          ))}
        </div>
        <button onClick={onEditNickname} className="text-sm font-medium hover:underline leading-tight mb-5">
          {me.username || '—'}
        </button>
      </div>
      <div className="px-4 py-2 bg-black/20 border-b border-white/10">
        <div className="flex items-stretch gap-2 overflow-x-auto no-scrollbar">
          <StatChip icon={'📍'} label={t('location')} value={currentLoc} onClick={onOpenLocations} />
          <StatChip icon={'🎣'} label={t('rod')} value={rodVal} onClick={onOpenRods} />
          <StatChip icon={lureIconPath ? <AssetImage src={lureIconPath} alt="" className="w-5 h-5 object-contain" /> : '🪱'} label={t('baits')} value={lureVal} onClick={onOpenBaits} />
          <StatChip icon={'🐟'} label={t('total')} value={`${Number(me.totalWeight||0).toFixed(1)} ${t('kg')}`} />
          <StatChip icon={'📅'} label={t('today')} value={`${Number(me.todayWeight||0).toFixed(1)} ${t('kg')}`} />
          <StatChip icon={'🪙'} label={t('coins')} value={Number(me.coins||0).toLocaleString(lang==='ru'?'ru-RU':'en-US')} />
        </div>
      </div>
    </div>
  );
}

function BottomNav({tab,setTab,dailyAvailable,achievementsAvailable}){
  const items = BOTTOM_NAV_ITEMS.map(item => ({
    ...item,
    label: typeof item.label === 'function' ? item.label() : item.label,
  }));
  const isAndroid = (window.Telegram?.WebApp?.platform || '').toLowerCase() === 'android';
  return (
    <div className="-mx-4 sticky bottom-0 z-20">
      <nav className="app-footer backdrop-blur bg-black/30 border-t border-white/10 flex gap-2" aria-label={t('menu')}>
        {items.map(item=>(
          <button
            key={item.id}
            type="button"
            onClick={()=>setTab(item.id)}
            className={`flex-1 flex flex-col items-center justify-center gap-1 rounded-xl py-2 text-[11px] font-medium transition-colors ${tab===item.id ? 'bg-white/10 text-emerald-400' : 'text-white/70 hover:bg-white/5 hover:text-white'}`}
            aria-current={tab===item.id ? 'page' : undefined}
          >
            <div className="relative">
              <AssetImage src={item.icon} alt="" className={`w-6 h-6 ${tab===item.id ? '' : 'opacity-80'}`} />
              {dailyAvailable && item.id==='shop' && (
                <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center">
                  !
                </span>
              )}
              {achievementsAvailable && item.id==='guide' && (
                <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center">
                  !
                </span>
              )}
            </div>
            <span className="leading-tight text-center">{item.label}</span>
          </button>
        ))}
      </nav>
      {isAndroid && <div className="app-footer-placeholder" aria-hidden="true"></div>}
    </div>
  );
}

function LocationsDrawer({open, onClose, me, onSelect}){
  return (
    <div className={`fixed inset-0 z-50 ${open?'' :'pointer-events-none'}`}>
      <div onClick={onClose} className={`absolute inset-0 transition-opacity ${open? 'opacity-100':'opacity-0'} bg-black/60`}></div>
      <div
          className={`absolute right-0 inset-y-0 w-[88%] sm:w-[380px] glass transition-transform ${open? 'translate-x-0':'translate-x-full'} px-4 pb-safe flex flex-col`}
          style={{paddingTop:'calc(1rem + var(--safe-top-ui) + (var(--overlay) * 10px) + 8px)'}}>
        <div className="flex items-center justify-between mb-3">
          <div className="text-lg font-semibold">{t('locations')}</div>
          <button onClick={onClose} className="px-3 py-1 rounded-xl hover:bg-white/10 leading-none">✕</button>
        </div>
        <div className="space-y-2 overflow-y-auto pr-1">
          {me.locations.map(l=> (
            <button key={l.id} disabled={!l.unlocked}
                    onClick={()=>{ if(l.unlocked){ onSelect(l.id); onClose(); } }}
                    className={`w-full text-left p-3 rounded-xl border ${me.locationId===l.id? 'border-emerald-500 bg-emerald-500/10':'border-white/10 hover:bg-white/5'} ${!l.unlocked?'opacity-50 cursor-not-allowed':''}`}>
              <div className="flex items-center justify-between">
                <div>
                  <div className="font-semibold">{l.name}</div>
                  {l.unlocked? <div className="text-xs opacity-70">{t('unlocked')}</div> : <div className="text-xs opacity-70">{t('requiresKg', l.unlockKg)}</div>}
                </div>
                {me.locationId===l.id && <div className="text-emerald-400 text-sm">{t('current')}</div>}
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

function QuestCard({quest}){
  if(!quest) return null;
  const progress = Math.max(0, Number(quest.progress) || 0);
  const target = Math.max(1, Number(quest.target) || 1);
  const ratio = Math.min(1, progress / target);
  const completed = !!quest.completed;
  return (
    <div className={`p-3 rounded-xl border ${completed ? 'border-emerald-500/60 bg-emerald-500/10' : 'border-white/10'}`}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="font-semibold">{quest.name}</div>
          <div className="text-xs opacity-70 mt-1">{quest.description}</div>
        </div>
        {completed && <div className="text-emerald-300 text-xs font-semibold uppercase">{t('questCompleted')}</div>}
      </div>
      <div className="mt-3 h-2 rounded-full bg-white/10 overflow-hidden">
        <div
          className={`h-2 rounded-full ${completed ? 'bg-emerald-400' : 'bg-emerald-600'}`}
          style={{width:`${Math.round(ratio*100)}%`}}
        ></div>
      </div>
      <div className="text-xs mt-2 text-yellow-300">{t('questRewardCoins', quest.rewardCoins)}</div>
    </div>
  );
}

function QuestsDrawer({open, onClose, quests, loading, error, onReload}){
  const daily = quests?.daily || [];
  const weekly = quests?.weekly || [];
  return (
    <div className={`fixed inset-0 z-50 ${open?'' :'pointer-events-none'}`}>
      <div onClick={onClose} className={`absolute inset-0 transition-opacity ${open? 'opacity-100':'opacity-0'} bg-black/60`}></div>
      <div
          className={`absolute right-0 inset-y-0 w-[88%] sm:w-[420px] glass transition-transform ${open? 'translate-x-0':'translate-x-full'} px-4 pb-safe flex flex-col`}
          style={{paddingTop:'calc(1rem + var(--safe-top-ui) + (var(--overlay) * 10px) + 8px)'}}>
        <div className="flex items-center justify-between mb-3">
          <div className="text-lg font-semibold">{t('quests')}</div>
          <button onClick={onClose} className="px-3 py-1 rounded-xl hover:bg-white/10 leading-none">✕</button>
        </div>
        {loading ? (
          <div className="text-sm opacity-70">{t('loading')}</div>
        ) : error ? (
          <div className="text-sm opacity-70">
            <div>{t('questsUnavailable')}</div>
            {onReload && (
              <button className="mt-3 px-3 py-1 rounded-xl bg-emerald-600 hover:bg-emerald-500" onClick={onReload}>
                {t('questsRefresh')}
              </button>
            )}
          </div>
        ) : (
          <div className="space-y-4 overflow-y-auto pr-1">
            <div>
              <div className="text-sm font-semibold mb-2">{t('dailyQuests')}</div>
              {daily.length === 0 ? (
                <div className="text-sm opacity-60">{t('questsEmpty')}</div>
              ) : (
                <div className="space-y-2">
                  {daily.map(q => <QuestCard key={q.code} quest={q} />)}
                </div>
              )}
            </div>
            <div>
              <div className="text-sm font-semibold mb-2">{t('weeklyQuests')}</div>
              {weekly.length === 0 ? (
                <div className="text-sm opacity-60">{t('questsEmpty')}</div>
              ) : (
                <div className="space-y-2">
                  {weekly.map(q => <QuestCard key={q.code} quest={q} />)}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function BaitsDrawer({open,onClose,me,onSelect}){
  return (
    <div className={`fixed inset-0 z-50 ${open?'' :'pointer-events-none'}`}>
      <div onClick={onClose} className={`absolute inset-0 transition-opacity ${open? 'opacity-100':'opacity-0'} bg-black/60`}></div>
      <div
          className={`absolute right-0 inset-y-0 w-[88%] sm:w-[380px] glass transition-transform ${open? 'translate-x-0':'translate-x-full'} px-4 pb-safe flex flex-col`}
          style={{paddingTop:'calc(1rem + var(--safe-top-ui) + (var(--overlay) * 10px) + 8px)'}}>
        <div className="flex items-center justify-between mb-3">
          <div className="text-lg font-semibold">{t('baits')}</div>
          <button onClick={onClose} className="px-3 py-1 rounded-xl hover:bg-white/10 leading-none">✕</button>
        </div>
        <div className="space-y-2 overflow-y-auto pr-1">
          {me.lures.map(l=> {
            const displayName = l.displayName || translateLure(l.name);
            const desc = l.description || lureDescriptionText(l);
            return (
            <button key={l.id} disabled={l.qty<=0}
                    onClick={()=>{ if(l.qty>0){ onSelect(l.id); onClose(); } }}
                    className={`w-full text-left p-3 rounded-xl border ${me.currentLureId===l.id? 'border-emerald-500 bg-emerald-500/10':'border-white/10 hover:bg-white/5'} ${l.qty<=0?'opacity-50 cursor-not-allowed':''}`}>
              <div className="flex items-center justify-between">
                <div>
                  <div className={`font-semibold ${lureColor(l)}`}>{displayName}</div>
                  {desc && <div className="text-xs opacity-70 mt-1">{desc}</div>}
                  <div className="text-xs opacity-60 mt-1">{t('qty', l.qty)}</div>
                </div>
                {me.currentLureId===l.id && <div className="text-emerald-400 text-sm">{t('current')}</div>}
              </div>
            </button>
          )})}
        </div>
      </div>
    </div>
  );
}

function rodBonusText(rod){
  if(!rod) return '';
  if(!rod.bonusWater) return t('rodNoBonus');
  if(rod.bonusWater==='fresh' && rod.bonusPredator) return t('rodBonusFreshPredator');
  if(rod.bonusWater==='fresh' && !rod.bonusPredator) return t('rodBonusFreshPeaceful');
  if(rod.bonusWater==='salt' && rod.bonusPredator) return t('rodBonusSaltPredator');
  if(rod.bonusWater==='salt' && !rod.bonusPredator) return t('rodBonusSaltPeaceful');
  return t('rodNoBonus');
}

function RodsDrawer({open,onClose,me,onSelect,onUnlock}){
  const rods = me.rods || [];
  return (
    <div className={`fixed inset-0 z-50 ${open?'' :'pointer-events-none'}`}>
      <div onClick={onClose} className={`absolute inset-0 transition-opacity ${open? 'opacity-100':'opacity-0'} bg-black/60`}></div>
      <div
          className={`absolute right-0 inset-y-0 w-[88%] sm:w-[380px] glass transition-transform ${open? 'translate-x-0':'translate-x-full'} px-4 pb-safe flex flex-col`}
          style={{paddingTop:'calc(1rem + var(--safe-top-ui) + (var(--overlay) * 10px) + 8px)'}}>
        <div className="flex items-center justify-between mb-3">
          <div className="text-lg font-semibold">{t('rods')}</div>
          <button onClick={onClose} className="px-3 py-1 rounded-xl hover:bg-white/10 leading-none">✕</button>
        </div>
        <div className="space-y-2 overflow-y-auto pr-1">
          {rods.map(rod=>{
            const locked = !rod.unlocked;
            const isCurrent = me.currentRodId===rod.id;
            const unlockKg = Number(rod.unlockKg).toFixed(0);
            const price = typeof rod.priceStars === 'number' ? rod.priceStars : null;
            const priceText = price != null ? `${price}⭐` : null;
            const bonusText = rodBonusText(rod);
            const bonusLine = bonusText ? t('rodBonusLine', bonusText) : '';
            const info = locked
              ? [priceText ? t('rodLockedStars', {kg: unlockKg, stars: priceText}) : t('requiresKg', unlockKg), bonusLine]
                  .filter(Boolean)
                  .join('\n')
              : bonusText;
            const canUnlock = locked && rod.packId && priceText;
            return (
              <div key={rod.id}
                   className={`p-3 rounded-xl border ${isCurrent? 'border-emerald-500 bg-emerald-500/10':'border-white/10'} ${locked?'opacity-80':''}`}>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="font-semibold">{rod.name}</div>
                    <div className="text-xs opacity-70 mt-1 whitespace-pre-line">{info}</div>
                  </div>
                  {isCurrent && rod.unlocked && (
                    <div className="text-emerald-400 text-sm">{t('current')}</div>
                  )}
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                  {rod.unlocked ? (
                    <button
                      onClick={()=>{ onSelect(rod.id); onClose(); }}
                      className={`flex-1 min-w-[120px] py-2 rounded-xl ${isCurrent? 'glass border border-emerald-400 text-emerald-100':'bg-emerald-600 hover:bg-emerald-500'}`}
                      disabled={isCurrent}
                    >
                      {isCurrent ? t('current') : t('useRod')}
                    </button>
                  ) : (
                    <>
                      <button
                        disabled
                        className="flex-1 min-w-[140px] py-2 rounded-xl border border-white/10 bg-white/5 text-sm"
                      >
                        {t('rodLocked')}
                      </button>
                      {canUnlock && (
                        <button
                          onClick={()=>{ onUnlock && onUnlock(rod); }}
                          className="flex-1 min-w-[160px] py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-black font-semibold"
                        >
                          {t('unlockRodFor', priceText)}
                        </button>
                      )}
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function NicknameModal({me,onClose,onSave}){
  const [value,setValue] = React.useState(me.username||'');
  return (
    <div className="fixed inset-0 z-50">
      <div onClick={onClose} className="absolute inset-0 bg-black/60"></div>
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 glass p-4 rounded-xl w-[90%] max-w-sm">
        <div className="text-lg font-semibold mb-3">{t('nickname')}</div>
        <input value={value} maxLength={30} onChange={e=>setValue(e.target.value.replace(/[\u200E\u200F\u202A-\u202E\u2066-\u2069]/g,''))} className="w-full p-2 mb-4 rounded-lg bg-black/20 border border-white/10" />
        <div className="flex gap-2">
          <button onClick={onClose} className="flex-1 py-2 rounded-xl glass">{t('cancel')}</button>
          <button onClick={()=>onSave(value)} className="flex-1 py-2 rounded-xl bg-emerald-600">{t('save')}</button>
        </div>
      </div>
    </div>
  );
}

function CatchDetailsModal({catchData, me, onClose}){
  const [status,setStatus] = React.useState(null);
  const [error,setError] = React.useState(null);
  const [sending,setSending] = React.useState(false);
  if(!catchData) return null;
  const weight = Number(catchData.weight||0).toFixed(2);
  const dateLabel = catchData.at ? new Date(catchData.at).toLocaleString() : null;
  const fishId = catchData.fishId != null ? Number(catchData.fishId) : null;
  const unlockedFishIds = Array.isArray(me?.caughtFishIds) ? me.caughtFishIds : [];
  const fishUnlocked = fishId == null || unlockedFishIds.some(id => Number(id) === fishId);
  const fishImg = fishUnlocked && catchData.fish ? FISH_IMG[catchData.fish] : null;
  const rarityClass = catchData.rarity && rarityColors[catchData.rarity] ? rarityColors[catchData.rarity] : '';
  const isOwnCatch = catchData.userId != null && me?.id != null && Number(catchData.userId) === Number(me.id);
  const canSend = Boolean(isOwnCatch && catchData.id && catchData.fish);
  const locationBg = React.useMemo(()=>{
    if(catchData.locationBg) return catchData.locationBg;
    const resolver = typeof window.getLocationBackground === 'function'
      ? window.getLocationBackground
      : (id, name) => {
          const normalized = typeof name === 'string' ? name.trim().toLowerCase() : '';
          if (normalized && window.LOCATION_BG_BY_NAME && window.LOCATION_BG_BY_NAME[normalized]) {
            return window.LOCATION_BG_BY_NAME[normalized];
          }
          const numId = Number(id);
          if (Number.isFinite(numId) && window.LOCATION_BG && window.LOCATION_BG[numId]) {
            return window.LOCATION_BG[numId];
          }
          return null;
        };
    const direct = resolver(catchData.locationId, catchData.location);
    if(direct) return direct;
    if(Array.isArray(me?.locations)){
      const normalized = typeof catchData.location === 'string' ? catchData.location.trim().toLowerCase() : '';
      const match = normalized
        ? me.locations.find(loc=> typeof loc.name === 'string' && loc.name.trim().toLowerCase() === normalized)
        : me.locations.find(loc=> Number(loc.id) === Number(catchData.locationId));
      if(match){
        const resolved = resolver(match.id, match.name);
        if(resolved) return resolved;
      }
    }
    return null;
  }, [catchData.locationBg, catchData.locationId, catchData.location, me?.locations]);

  async function handleSend(){
    if(!canSend || sending) return;
    setSending(true);
    setStatus(null);
    setError(null);
    try{
      const res = await fetch(`/api/catches/${catchData.id}/send`,{method:'POST',credentials:'include'});
      if(!res.ok) throw new Error('send_failed');
      setStatus(t('catchSent'));
    }catch(e){
      setError(t('catchSendFailed'));
    }finally{
      setSending(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50">
      <div onClick={onClose} className="absolute inset-0 bg-black/60"></div>
      <div className="relative mx-4 mt-safe flex items-center justify-center min-h-full pointer-events-none">
        <div className="glass w-full max-w-sm rounded-2xl p-4 pointer-events-auto relative overflow-hidden">
          {locationBg && (
            <>
              <AssetImage
                src={locationBg}
                alt={catchData.location || ''}
                className="absolute inset-0 w-full h-full object-cover"
                onError={e=>{ if(e?.currentTarget) e.currentTarget.style.display='none'; }}
              />
              <div className="absolute inset-0 bg-black/60 pointer-events-none"></div>
            </>
          )}
          <div className="relative">
            <div className="flex items-start justify-between mb-3">
              <div className={`text-lg font-semibold ${rarityClass}`}>{catchData.fish || '-'}</div>
              <button onClick={onClose} className="px-3 py-1 rounded-xl hover:bg-white/10 leading-none">✕</button>
            </div>
            <div className="mt-3 relative w-full pb-24">
              <div className="flex justify-center">
                {fishUnlocked ? (
                  fishImg ? (
                    <AssetImage src={fishImg} alt={catchData.fish} className="w-32 h-32 object-contain" onError={e=>{ if(e?.currentTarget) e.currentTarget.style.display='none'; }} />
                  ) : (
                    <div className="w-32 h-32 bg-gray-800 rounded-xl flex items-center justify-center text-4xl">🐟</div>
                  )
                ) : (
                  <div className="w-32 h-32 bg-gray-800/70 rounded-xl flex items-center justify-center relative">
                    <span className="text-5xl opacity-20">🐟</span>
                    <span className="absolute text-3xl">?</span>
                  </div>
                )}
              </div>
              <div className="absolute bottom-0 right-0 p-2 flex flex-col items-end text-right gap-1">
                {catchData.location && <div className={`text-sm opacity-70 ${rarityClass}`}>{t('locationLabel')} {catchData.location}</div>}
                {catchData.user && <div className={`text-xs opacity-70 ${rarityClass}`}><bdi>{catchData.user}</bdi></div>}
                {dateLabel && <div className={`text-xs opacity-60 ${rarityClass}`}>{dateLabel}</div>}
                <div className={`text-2xl font-semibold ${rarityClass}`}>{weight} {t('kg')}</div>
              </div>
            </div>
            {canSend && (
              <button
                type="button"
                onClick={handleSend}
                disabled={sending}
                className={`w-full mt-4 px-4 py-3 rounded-xl font-semibold ${sending?'bg-emerald-500/60':'bg-emerald-600 hover:bg-emerald-500'}`}
              >
                {sending ? t('sendingCatch') : t('sendToMe')}
              </button>
            )}
            {status && <div className="text-xs text-emerald-300 mt-2">{status}</div>}
            {error && <div className="text-xs text-red-300 mt-2">{error}</div>}
          </div>
        </div>
      </div>
    </div>
  );
}

function DailyModal({streak,available,rewards,onClose,onClaim}){
  const defaultRewards = [
    [ {name:'Зерновая крошка',qty:8}, {name:'Ручейный малек',qty:4} ],
    [ {name:'Зерновая крошка',qty:10}, {name:'Ручейный малек',qty:6} ],
    [ {name:'Зерновая крошка',qty:12}, {name:'Ручейный малек',qty:6} ],
    [ {name:'Зерновая крошка',qty:12}, {name:'Ручейный малек',qty:8} ],
    [ {name:'Зерновая крошка',qty:12}, {name:'Ручейный малек',qty:8}, {name:'Луговой червь',qty:1} ],
    [ {name:'Зерновая крошка',qty:12}, {name:'Ручейный малек',qty:10} ],
    [
      {name:'Зерновая крошка',qty:12},
      {name:'Ручейный малек',qty:12},
      {name:'Луговой червь',qty:1},
      {name:'Серебряный живец',qty:1},
    ],
  ];
  const displayRewards = (rewards && rewards.length ? rewards : defaultRewards);
  const totalDays = displayRewards.length;
  const day = Math.min(Math.max(streak,0), totalDays);
  const current = totalDays === 0
    ? -1
    : (available
      ? Math.min(day, totalDays - 1)
      : Math.min(Math.max(day - 1, 0), totalDays - 1));
  const doneThreshold = available
    ? Math.min(day, Math.max(totalDays - 1, 0))
    : Math.min(day, totalDays);
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div onClick={onClose} className="absolute inset-0 bg-black/60"></div>
      <div className="relative w-[90%] max-w-sm rounded-xl overflow-hidden">
        <AssetImage src="/app/assets/backgrounds/pond.png" alt="" className="absolute inset-0 w-full h-full object-cover"/>
        <div className="absolute inset-0 bg-black/50"></div>
        <div className="relative p-4">
          <div className="text-lg font-semibold mb-3 text-center">{t('gift')}</div>
          <div className="grid grid-cols-2 gap-2 mb-3 text-sm">
            {displayRewards.map((items,i)=>{
              const done = i < doneThreshold;
              const isCurrent = i === current;
              return (
                <div key={i} className={`p-2 rounded-lg text-center ${done ? 'opacity-40' : 'bg-white/10'} ${isCurrent ? 'ring-2 ring-yellow-300' : ''}`}>
                  <div className="font-semibold mb-1">{t('day')} {i+1}</div>
                  {items.map(it=> <div key={it.name} className="text-xs">{it.qty} {translateLure(it.name)}</div>)}
                </div>
              );
            })}
          </div>
          {available && <button onClick={onClaim} className="w-full py-2 rounded-xl bg-emerald-600 mb-2">{t('tapToClaim')}</button>}
          <button onClick={onClose} className="w-full py-2 rounded-xl glass">{t('cancel')}</button>
        </div>
      </div>
    </div>
  );
}

function ClubScreen({active,onClose,me,onReloadProfile}){
  const CLUB_CREATE_COST = 1000;
  const CLUB_MIN_WEIGHT = 1000;
  const [mode, setMode] = React.useState('hub');
  const [club, setClub] = React.useState(null);
  const [clubTab, setClubTab] = React.useState('current');
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState(null);
  const [search, setSearch] = React.useState([]);
  const [searchLoading, setSearchLoading] = React.useState(false);
  const [searchError, setSearchError] = React.useState(null);
  const [selectedClubId, setSelectedClubId] = React.useState(null);
  const [name, setName] = React.useState('');
  const [confirming, setConfirming] = React.useState(false);
  const [createLoading, setCreateLoading] = React.useState(false);
  const [createError, setCreateError] = React.useState(null);
  const [joinLoading, setJoinLoading] = React.useState(false);
  const [joinError, setJoinError] = React.useState(null);
  const [chatOpen, setChatOpen] = React.useState(false);
  const [chatMessages, setChatMessages] = React.useState([]);
  const [chatLoading, setChatLoading] = React.useState(false);
  const [chatError, setChatError] = React.useState(null);
  const coinLocale = (typeof document!=='undefined' && document.documentElement.lang==='en') ? 'en-US' : 'ru-RU';

  const roleLabel = React.useCallback(role => {
    if(role === 'president') return t('clubRolePresident');
    if(role === 'heir') return t('clubRoleHeir');
    if(role === 'veteran') return t('clubRoleVeteran');
    if(role === 'novice') return t('clubRoleNovice');
    return role;
  }, []);

  const canCreate = (me?.totalWeight || 0) >= CLUB_MIN_WEIGHT;

  const resetState = React.useCallback(() => {
    setMode('hub');
    setClub(null);
    setClubTab('current');
    setLoading(false);
    setError(null);
    setSearch([]);
    setSearchLoading(false);
    setSearchError(null);
    setSelectedClubId(null);
    setName('');
    setConfirming(false);
    setCreateLoading(false);
    setCreateError(null);
    setJoinLoading(false);
    setJoinError(null);
    setChatOpen(false);
    setChatMessages([]);
    setChatLoading(false);
    setChatError(null);
  }, []);

  const loadClub = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try{
      const resp = await fetch(`/api/club`, {credentials:'include'});
      if(resp.status === 204){
        setClub(null);
        setMode('hub');
        return;
      }
      if(!resp.ok){
        if(resp.status===401) throw new Error('unauthorized');
        throw new Error('load_failed');
      }
      const data = await resp.json();
      setClub(data);
      setClubTab('current');
      setMode('club');
    }catch(e){
      setError(e.message==='unauthorized' ? t('authRequired') : t('clubLoadFailed'));
    }finally{
      setLoading(false);
    }
  }, []);

  const loadSearch = React.useCallback(async () => {
    setSearchLoading(true);
    setSearchError(null);
    try{
      const resp = await fetch(`/api/club/search`, {credentials:'include'});
      if(!resp.ok){
        if(resp.status===401) throw new Error('unauthorized');
        throw new Error('search_failed');
      }
      const data = await resp.json();
      setSearch(Array.isArray(data) ? data : []);
      if(!selectedClubId && data.length > 0){
        setSelectedClubId(data[0]?.id || null);
      }
    }catch(e){
      setSearchError(e.message==='unauthorized' ? t('authRequired') : t('clubSearchFailed'));
    }finally{
      setSearchLoading(false);
    }
  }, [selectedClubId]);

  const loadChat = React.useCallback(async () => {
    setChatLoading(true);
    setChatError(null);
    try{
      const resp = await fetch(`/api/club/chat`, {credentials:'include'});
      if(!resp.ok){
        if(resp.status===401) throw new Error('unauthorized');
        throw new Error('chat_failed');
      }
      const data = await resp.json();
      setChatMessages(Array.isArray(data) ? data : []);
    }catch(e){
      setChatError(e.message==='unauthorized' ? t('authRequired') : t('clubChatFailed'));
    }finally{
      setChatLoading(false);
    }
  }, []);

  const parseClubError = React.useCallback((code, fallbackKey) => {
    if(code === 'already_in_club') return t('clubAlreadyIn');
    if(code === 'club_full') return t('clubFull');
    if(code === 'weight_required') return t('clubNeedWeightError', CLUB_MIN_WEIGHT);
    if(code === 'not_enough_coins') return t('clubNotEnoughCoins');
    if(code === 'name_empty') return t('clubNameEmpty');
    if(code === 'name_too_long') return t('clubNameTooLong');
    if(code === 'name_profanity') return t('clubNameProfanity');
    if(code === 'not_found') return t('clubNotFound');
    return t(fallbackKey);
  }, []);

  const handleCreate = React.useCallback(async () => {
    const trimmed = name.trim();
    setCreateError(null);
    if(!trimmed){
      setCreateError(t('clubNameEmpty'));
      return;
    }
    if(trimmed.length > 20){
      setCreateError(t('clubNameTooLong'));
      return;
    }
    setCreateLoading(true);
    try{
      const resp = await fetch(`/api/club/create`, {
        method:'POST',
        credentials:'include',
        headers:{'Content-Type':'application/json'},
        body: JSON.stringify({name: trimmed})
      });
      if(!resp.ok){
        const data = await resp.json().catch(()=> ({}));
        throw new Error(data.error || 'create_failed');
      }
      const data = await resp.json();
      setClub(data);
      setMode('club');
      setClubTab('current');
      setName('');
      setConfirming(false);
      onReloadProfile?.();
    }catch(e){
      setCreateError(parseClubError(e.message, 'clubCreateFailed'));
    }finally{
      setCreateLoading(false);
    }
  }, [name, onReloadProfile, parseClubError]);

  const handleJoin = React.useCallback(async () => {
    if(!selectedClubId) return;
    setJoinError(null);
    setJoinLoading(true);
    try{
      const resp = await fetch(`/api/club/${selectedClubId}/join`, {method:'POST', credentials:'include'});
      if(!resp.ok){
        const data = await resp.json().catch(()=> ({}));
        throw new Error(data.error || 'join_failed');
      }
      const data = await resp.json();
      setClub(data);
      setMode('club');
      setClubTab('current');
      setSelectedClubId(null);
      onReloadProfile?.();
    }catch(e){
      setJoinError(parseClubError(e.message, 'clubJoinFailed'));
    }finally{
      setJoinLoading(false);
    }
  }, [selectedClubId, onReloadProfile, parseClubError]);

  const canManageMembers = React.useMemo(() => (
    club?.role === 'president' || club?.role === 'heir'
  ), [club?.role]);

  const canActOnMember = React.useCallback((member) => {
    if(!club || !member) return false;
    if(member.userId === me?.id) return false;
    if(club.role === 'president') return member.role !== 'president';
    if(club.role === 'heir') return member.role === 'veteran' || member.role === 'novice';
    return false;
  }, [club, me?.id]);

  React.useEffect(() => {
    if(!active) return;
    resetState();
    loadClub();
  }, [active, resetState, loadClub]);

  if(!active) return null;

  const weekData = clubTab === 'previous' ? club?.previousWeek : club?.currentWeek;

  const handleLeave = async () => {
    if(!window.confirm(t('clubConfirmLeave'))) return;
    try{
      const resp = await fetch(`/api/club/leave`, {method:'POST', credentials:'include'});
      if(!resp.ok){
        const data = await resp.json().catch(()=> ({}));
        throw new Error(data.error || 'leave_failed');
      }
      resetState();
      onReloadProfile?.();
    }catch(e){
      setError(t('clubLeaveFailed'));
    }
  };

  const handleMemberAction = async (memberId, action) => {
    try{
      const resp = await fetch(`/api/club/members/${memberId}/${action}`, {method:'POST', credentials:'include'});
      if(!resp.ok){
        const data = await resp.json().catch(()=> ({}));
        throw new Error(data.error || 'action_failed');
      }
      const data = await resp.json();
      setClub(data);
    }catch(e){
      setError(t('clubActionFailed'));
    }
  };

  return (
    <div className="flex-1 flex flex-col">
      <div className="flex items-center gap-2 mb-3">
        <button onClick={onClose} className="px-3 py-1 rounded-xl glass">←</button>
        <div className="flex-1 text-lg font-semibold">{t('club')}</div>
        {mode === 'club' && club && (
          <button
            type="button"
            className="px-3 py-1 rounded-xl glass text-sm"
            onClick={()=>{
              setChatOpen(true);
              loadChat();
            }}
          >{t('clubChat')}</button>
        )}
      </div>

      {chatOpen && (
        <div className="fixed inset-0 z-50">
          <div onClick={()=>setChatOpen(false)} className="absolute inset-0 bg-black/60"></div>
          <div className="absolute inset-x-4 top-16 bottom-16 glass rounded-2xl p-4 flex flex-col">
            <div className="flex items-center justify-between mb-3">
              <div className="text-lg font-semibold">{t('clubChatTitle')}</div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  className="px-3 py-1 rounded-xl glass text-sm"
                  onClick={loadChat}
                  disabled={chatLoading}
                >{t('clubChatRefresh')}</button>
                <button onClick={()=>setChatOpen(false)} className="px-3 py-1 rounded-xl glass">✕</button>
              </div>
            </div>
            <div className="flex-1 space-y-2 overflow-y-auto pr-1 text-sm">
              {chatLoading ? (
                <div className="text-sm opacity-70">{t('loading')}</div>
              ) : chatError ? (
                <div className="text-sm opacity-70">{chatError}</div>
              ) : chatMessages.length === 0 ? (
                <div className="text-sm opacity-70">{t('clubChatEmpty')}</div>
              ) : (
                chatMessages.map(item => {
                  const ts = item.createdAt ? new Date(item.createdAt).toLocaleString(coinLocale) : '';
                  return (
                    <div key={item.id || item.createdAt} className="p-2 rounded-xl border border-white/10">
                      <div className="text-xs opacity-70">{ts}</div>
                      <div className="mt-1">{item.message}</div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>
      )}

      {loading ? (
        <div className="text-sm opacity-70">{t('loading')}</div>
      ) : error ? (
        <div className="text-sm opacity-70">{error}</div>
      ) : mode === 'club' && club ? (
        <div className="space-y-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <div className="text-base font-semibold">{club.name}</div>
              <div className="text-xs opacity-70">{roleLabel(club.role)}</div>
            </div>
            <div className="text-xs opacity-70">{club.memberCount}/{club.capacity}</div>
          </div>

          <div className="p-3 rounded-xl border border-white/10">
            <div className="text-sm font-semibold mb-1">{t('clubInfoTitle')}</div>
            <div className="text-xs opacity-70">{t('clubInfoPlaceholder')}</div>
          </div>

          <div className="flex gap-2 text-sm">
            <button
              type="button"
              onClick={()=>setClubTab('current')}
              className={`flex-1 py-2 rounded-xl ${clubTab==='current' ? 'bg-emerald-600' : 'glass'}`}
            >{t('clubCurrentWeek')}</button>
            <button
              type="button"
              onClick={()=>setClubTab('previous')}
              className={`flex-1 py-2 rounded-xl ${clubTab==='previous' ? 'bg-emerald-600' : 'glass'}`}
            >{t('clubPreviousWeek')}</button>
          </div>
          <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
            {(weekData?.members || []).length === 0 ? (
              <div className="text-sm opacity-70">{t('clubNoContributions')}</div>
            ) : (
              weekData.members.map(member => (
                <div key={member.userId} className="p-2 rounded-xl border border-white/10 space-y-2">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <div className="font-semibold text-sm">
                        <bdi>{member.name || '—'}</bdi>
                      </div>
                      <div className="text-xs opacity-70">{roleLabel(member.role)}</div>
                    </div>
                    <div className="text-sm text-yellow-300">🪙 {Number(member.coins || 0).toLocaleString(coinLocale)}</div>
                  </div>
                  {canManageMembers && canActOnMember(member) && (
                    <div className="flex flex-wrap gap-2 text-xs">
                      {member.role !== 'heir' && member.role !== 'president' && (
                        <button
                          type="button"
                          className="px-2 py-1 rounded-lg glass"
                          onClick={()=>handleMemberAction(member.userId, 'promote')}
                        >{t('clubPromote')}</button>
                      )}
                      {member.role !== 'novice' && (
                        <button
                          type="button"
                          className="px-2 py-1 rounded-lg glass"
                          onClick={()=>handleMemberAction(member.userId, 'demote')}
                        >{t('clubDemote')}</button>
                      )}
                      <button
                        type="button"
                        className="px-2 py-1 rounded-lg glass text-red-300"
                        onClick={()=>{
                          if(window.confirm(t('clubConfirmKick', member.name || '—'))){
                            handleMemberAction(member.userId, 'kick');
                          }
                        }}
                      >{t('clubKick')}</button>
                      {club.role === 'president' && member.role === 'heir' && (
                        <button
                          type="button"
                          className="px-2 py-1 rounded-lg bg-yellow-400 text-black"
                          onClick={()=>handleMemberAction(member.userId, 'appoint-president')}
                        >{t('clubAppointPresident')}</button>
                      )}
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
          <div className="text-sm opacity-80 text-right">
            {t('clubWeeklyTotal', Number(weekData?.totalCoins || 0).toLocaleString(coinLocale))}
          </div>
          <button
            type="button"
            className="w-full px-3 py-2 rounded-xl glass text-red-300"
            onClick={handleLeave}
          >{t('clubLeave')}</button>
        </div>
      ) : mode === 'create' ? (
        <div className="space-y-3">
          {!canCreate && (
            <div className="text-sm opacity-70">{t('clubNeedWeightError', CLUB_MIN_WEIGHT)}</div>
          )}
          {canCreate && (
            <>
              <div className="text-sm opacity-70">{t('clubNameHint')}</div>
              <input
                type="text"
                maxLength={20}
                value={name}
                onChange={e=>{ setName(e.target.value); setCreateError(null); }}
                className="w-full px-3 py-2 rounded-xl bg-black/40 border border-white/10"
                placeholder={t('clubNamePlaceholder')}
              />
              {createError && <div className="text-sm text-red-400">{createError}</div>}
              {confirming ? (
                <div className="space-y-3">
                  <div className="text-sm opacity-80">{t('clubCreateConfirm', { name: name.trim(), coins: CLUB_CREATE_COST })}</div>
                  <div className="flex gap-2 text-sm">
                    <button
                      type="button"
                      className="flex-1 px-3 py-2 rounded-xl glass"
                      onClick={()=>setConfirming(false)}
                      disabled={createLoading}
                    >{t('cancel')}</button>
                    <button
                      type="button"
                      className="flex-1 px-3 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 disabled:opacity-60"
                      onClick={handleCreate}
                      disabled={createLoading}
                    >{t('clubConfirmCreate')}</button>
                  </div>
                </div>
              ) : (
                <button
                  type="button"
                  className="w-full px-3 py-2 rounded-xl bg-yellow-400 text-black hover:bg-yellow-300 disabled:opacity-60"
                  onClick={()=>{
                    const trimmed = name.trim();
                    if(!trimmed){
                      setCreateError(t('clubNameEmpty'));
                      return;
                    }
                    if(trimmed.length > 20){
                      setCreateError(t('clubNameTooLong'));
                      return;
                    }
                    setConfirming(true);
                    setCreateError(null);
                  }}
                  disabled={createLoading}
                >{t('clubCreateCost', CLUB_CREATE_COST)}</button>
              )}
            </>
          )}
          <button
            type="button"
            className="w-full px-3 py-2 rounded-xl glass"
            onClick={()=>{ setMode('hub'); setConfirming(false); setCreateError(null); }}
          >{t('back')}</button>
        </div>
      ) : mode === 'search' ? (
        <div className="space-y-3">
          {searchLoading ? (
            <div className="text-sm opacity-70">{t('loading')}</div>
          ) : searchError ? (
            <div className="text-sm opacity-70">{searchError}</div>
          ) : search.length === 0 ? (
            <div className="text-sm opacity-70">{t('clubSearchEmpty')}</div>
          ) : (
            <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
              {search.map(item => (
                <button
                  key={item.id}
                  type="button"
                  onClick={()=>setSelectedClubId(item.id)}
                  className={`w-full text-left p-3 rounded-xl border ${selectedClubId===item.id ? 'border-emerald-500 bg-emerald-500/10' : 'border-white/10 hover:bg-white/5'}`}
                >
                  <div className="flex items-center justify-between gap-3">
                    <div className="font-semibold text-sm">
                      <bdi>{item.name}</bdi>
                    </div>
                    <div className="text-xs opacity-70">{item.memberCount}/{item.capacity}</div>
                  </div>
                </button>
              ))}
            </div>
          )}
          {joinError && <div className="text-sm text-red-400">{joinError}</div>}
          <div className="flex gap-2 text-sm">
            <button
              type="button"
              className="flex-1 px-3 py-2 rounded-xl glass"
              onClick={()=>{ loadSearch(); }}
              disabled={searchLoading}
            >{t('clubSearchRefresh')}</button>
            <button
              type="button"
              className="flex-1 px-3 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 disabled:opacity-60"
              onClick={handleJoin}
              disabled={joinLoading || !selectedClubId || searchLoading || searchError}
            >{t('clubJoin')}</button>
          </div>
          <button
            type="button"
            className="w-full px-3 py-2 rounded-xl glass"
            onClick={()=>{ setMode('hub'); setJoinError(null); }}
          >{t('back')}</button>
        </div>
      ) : (
        <div className="space-y-3">
          <button
            type="button"
            className="w-full px-3 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500"
            onClick={()=>{ setMode('create'); setCreateError(null); setConfirming(false); }}
          >{t('clubCreate')}</button>
          <button
            type="button"
            className="w-full px-3 py-2 rounded-xl glass"
            onClick={()=>{ setMode('search'); loadSearch(); }}
          >{t('clubSearch')}</button>
          {createError && <div className="text-sm text-red-400">{createError}</div>}
        </div>
      )}
    </div>
  );
}
