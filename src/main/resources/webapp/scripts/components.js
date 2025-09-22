function StatChip({icon,label,value,active=false,onClick}){
  return (
    <button type="button" onClick={onClick}
      className={`h-10 min-w-[92px] px-3 rounded-xl flex items-center justify-center gap-2 shrink-0 glass ${onClick?'hover:bg-white/10':'pointer-events-none'} ${active?'ring-1 ring-emerald-500':''}`}>
      {icon && <span className="text-base">{icon}</span>}
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
          <StatChip icon={'🪱'} label={t('baits')} value={lureVal} onClick={onOpenBaits} />
          <StatChip icon={'🐟'} label={t('total')} value={`${Number(me.totalWeight||0).toFixed(1)} ${t('kg')}`} />
          <StatChip icon={'📅'} label={t('today')} value={`${Number(me.todayWeight||0).toFixed(1)} ${t('kg')}`} />
        </div>
      </div>
    </div>
  );
}

function BottomNav({tab,setTab,dailyAvailable}){
  const items = [
    {id:'fish', label:t('fishing'), icon:'/app/assets/menu/fishing.png'},
    {id:'tournaments', label:t('tournaments'), icon:'/app/assets/menu/tournaments.png'},
    {id:'achievements', label:t('ratings'), icon:'/app/assets/menu/ratings.png'},
    {id:'guide', label:t('guide'), icon:'/app/assets/menu/guide.png'},
    {id:'shop', label:t('shop'), icon:'/app/assets/menu/shop.png'},
  ];
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
              <img src={item.icon} alt="" className={`w-6 h-6 ${tab===item.id ? '' : 'opacity-80'}`} />
              {dailyAvailable && item.id==='shop' && (
                <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-amber-400 text-black text-[10px] font-bold flex items-center justify-center">
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

function RodsDrawer({open,onClose,me,onSelect}){
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
            const info = locked ? t('requiresKg', Number(rod.unlockKg).toFixed(0)) : rodBonusText(rod);
            return (
              <button key={rod.id} disabled={locked}
                      onClick={()=>{ if(!locked){ onSelect(rod.id); onClose(); } }}
                      className={`w-full text-left p-3 rounded-xl border ${isCurrent? 'border-emerald-500 bg-emerald-500/10':'border-white/10 hover:bg-white/5'} ${locked?'opacity-50 cursor-not-allowed':''}`}>
                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-semibold">{rod.name}</div>
                    <div className="text-xs opacity-70">{info}</div>
                  </div>
                  {isCurrent && <div className="text-emerald-400 text-sm">{t('current')}</div>}
                </div>
              </button>
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
  const fishImg = catchData.fish && FISH_IMG[catchData.fish];
  const rarityClass = catchData.rarity && rarityColors[catchData.rarity] ? rarityColors[catchData.rarity] : '';
  const isOwnCatch = catchData.userId != null && me?.id != null && Number(catchData.userId) === Number(me.id);
  const canSend = Boolean(isOwnCatch && catchData.id && catchData.fish);

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
        <div className="glass w-full max-w-sm rounded-2xl p-4 pointer-events-auto">
          <div className="flex items-start justify-between mb-3">
            <div className={`text-lg font-semibold ${rarityClass}`}>{catchData.fish || '-'}</div>
            <button onClick={onClose} className="px-3 py-1 rounded-xl hover:bg-white/10 leading-none">✕</button>
          </div>
          {fishImg ? (
            <img src={fishImg} alt={catchData.fish} className="w-32 h-32 object-contain mx-auto" onError={e=>{e.currentTarget.style.display='none';}} />
          ) : (
            <div className="w-32 h-32 bg-gray-800 rounded-xl flex items-center justify-center mx-auto text-4xl">🐟</div>
          )}
          {catchData.location && <div className={`text-sm opacity-70 mt-3 ${rarityClass}`}>{t('locationLabel')} {catchData.location}</div>}
          <div className={`text-2xl font-semibold mt-2 ${rarityClass}`}>{weight} {t('kg')}</div>
          {dateLabel && <div className={`text-xs opacity-60 mt-1 ${rarityClass}`}>{dateLabel}</div>}
          {catchData.user && <div className={`text-xs opacity-70 mt-1 ${rarityClass}`}><bdi>{catchData.user}</bdi></div>}
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
  );
}

function DailyModal({streak,available,rewards,onClose,onClaim}){
  const defaultRewards = [
    [ {name:'Пресная мирная',qty:8}, {name:'Пресная хищная',qty:4} ],
    [ {name:'Пресная мирная',qty:10}, {name:'Пресная хищная',qty:6} ],
    [ {name:'Пресная мирная',qty:12}, {name:'Пресная хищная',qty:6} ],
    [ {name:'Пресная мирная',qty:12}, {name:'Пресная хищная',qty:8} ],
    [ {name:'Пресная мирная',qty:12}, {name:'Пресная хищная',qty:8}, {name:'Пресная мирная+',qty:1} ],
    [ {name:'Пресная мирная',qty:12}, {name:'Пресная хищная',qty:10} ],
    [ {name:'Пресная мирная',qty:12}, {name:'Пресная хищная',qty:12}, {name:'Пресная хищная+',qty:1} ],
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
        <img src="/app/assets/backgrounds/pond.png" alt="" className="absolute inset-0 w-full h-full object-cover"/>
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
