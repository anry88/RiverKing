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

function Header({me,lang,tab,setTab,onEditNickname,onOpenLocations,onOpenBaits,onToggleLanguage}){
  const [menuOpen,setMenuOpen] = React.useState(false);
  const currentLoc = (me.locations.find(x=>x.id===me.locationId)||{}).name||'—';
  const curLure = me.lures.find(l=>l.id===me.currentLureId);
  const lureVal = curLure? `${curLure.name} (${curLure.qty})` : '—';
  return (
      <div className="-mx-4">
      <div className="app-header backdrop-blur bg-black/30 border-b border-white/10 flex justify-between gap-2">
        <button onClick={onEditNickname} className="text-sm hover:underline pb-0.5 leading-none">
          {me.username || '—'}
        </button>
        <div className="flex items-end gap-2">
          <button onClick={onToggleLanguage} className="text-sm hover:underline pb-0.5 leading-none">
            {lang==='ru'? '🇷🇺 RU' : '🇺🇸 EN'}
          </button>
          <button onClick={()=>setMenuOpen(true)} className="text-sm hover:underline pb-0.5 leading-none">
            {t('menu')}
          </button>
        </div>
      </div>
      <div className="px-4 py-2 bg-black/20 border-b border-white/10">
        <div className="flex items-stretch gap-2 overflow-x-auto no-scrollbar">
          <StatChip icon={'📍'} label={t('location')} value={currentLoc} onClick={onOpenLocations} />
          <StatChip icon={'🪱'} label={t('baits')} value={lureVal} onClick={onOpenBaits} />
          <StatChip icon={'🐟'} label={t('total')} value={`${Number(me.totalWeight||0).toFixed(1)} ${t('kg')}`} />
          <StatChip icon={'📅'} label={t('today')} value={`${Number(me.todayWeight||0).toFixed(1)} ${t('kg')}`} />
        </div>
      </div>
      {menuOpen && (
        <MenuDrawer open={menuOpen} onClose={()=>setMenuOpen(false)} tab={tab} setTab={setTab} />
      )}
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
          {me.lures.map(l=> (
            <button key={l.id} disabled={l.qty<=0}
                    onClick={()=>{ if(l.qty>0){ onSelect(l.id); onClose(); } }}
                    className={`w-full text-left p-3 rounded-xl border ${me.currentLureId===l.id? 'border-emerald-500 bg-emerald-500/10':'border-white/10 hover:bg-white/5'} ${l.qty<=0?'opacity-50 cursor-not-allowed':''}`}>
              <div className="flex items-center justify-between">
                <div>
                  <div className={`font-semibold ${lureColor(l.name)}`}>{l.name}</div>
                  <div className="text-xs opacity-70">{t('qty', l.qty)}</div>
                </div>
                {me.currentLureId===l.id && <div className="text-emerald-400 text-sm">{t('current')}</div>}
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

function MenuDrawer({open,onClose,tab,setTab}){
  return (
    <div className={`fixed inset-0 z-50 ${open?'' :'pointer-events-none'}`}>
      <div onClick={onClose} className={`absolute inset-0 transition-opacity ${open? 'opacity-100':'opacity-0'} bg-black/60`}></div>
      <div
          className={`absolute right-0 inset-y-0 w-[88%] sm:w-[260px] glass transition-transform ${open? 'translate-x-0':'translate-x-full'} px-4 pb-safe flex flex-col`}
          style={{paddingTop:'calc(1rem + var(--safe-top-ui) + (var(--overlay) * 10px) + 8px)'}}>
        <div className="flex items-center justify-between mb-3">
          <div className="text-lg font-semibold leading-none">{t('menu')}</div>
          <button onClick={onClose} className="px-3 py-1 rounded-xl hover:bg-white/10 leading-none">✕</button>
        </div>
        <div className="space-y-2 overflow-y-auto pr-1 pt-1">
          <button onClick={()=>{setTab('fish'); onClose();}} className={`w-full text-left px-3 pt-1 pb-0.5 rounded hover:bg-white/10 leading-none ${tab==='fish'?'text-emerald-400':''}`}>{t('fishing')}</button>
          <button onClick={()=>{setTab('tournaments'); onClose();}} className={`w-full text-left px-3 pt-1 pb-0.5 rounded hover:bg-white/10 leading-none ${tab==='tournaments'?'text-emerald-400':''}`}>{t('tournaments')}</button>
          <button onClick={()=>{setTab('achievements'); onClose();}} className={`w-full text-left px-3 pt-1 pb-0.5 rounded hover:bg-white/10 leading-none ${tab==='achievements'?'text-emerald-400':''}`}>{t('ratings')}</button>
          <button onClick={()=>{setTab('guide'); onClose();}} className={`w-full text-left px-3 pt-1 pb-0.5 rounded hover:bg-white/10 leading-none ${tab==='guide'?'text-emerald-400':''}`}>{t('guide')}</button>
          <button onClick={()=>{setTab('shop'); onClose();}} className={`w-full text-left px-3 pt-1 pb-0.5 rounded hover:bg-white/10 leading-none ${tab==='shop'?'text-emerald-400':''}`}>{t('shop')}</button>
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

function DailyModal({streak,available,onClose,onClaim}){
  const rewards = [
    [ {name:'Пресная мирная',qty:10}, {name:'Пресная хищная',qty:5} ],
    [ {name:'Пресная мирная',qty:10}, {name:'Пресная хищная',qty:10} ],
    [ {name:'Пресная мирная',qty:15}, {name:'Пресная хищная',qty:10} ],
    [ {name:'Пресная мирная',qty:15}, {name:'Пресная хищная',qty:15} ],
    [ {name:'Пресная мирная',qty:15}, {name:'Пресная хищная',qty:15}, {name:'Морская мирная',qty:5} ],
    [ {name:'Пресная мирная',qty:15}, {name:'Пресная хищная',qty:15}, {name:'Морская мирная',qty:5}, {name:'Морская хищная',qty:5} ],
    [ {name:'Пресная мирная',qty:15}, {name:'Пресная хищная',qty:15}, {name:'Морская мирная',qty:5}, {name:'Морская хищная',qty:5}, {name:'Пресная мирная+',qty:1}, {name:'Пресная хищная+',qty:1} ],
  ];
  const day = Math.min(streak,7);
  const current = available ? day : day - 1;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div onClick={onClose} className="absolute inset-0 bg-black/60"></div>
      <div className="relative w-[90%] max-w-sm rounded-xl overflow-hidden">
        <img src="/app/assets/riverking_bg_pond_1600x900.png" alt="" className="absolute inset-0 w-full h-full object-cover"/>
        <div className="absolute inset-0 bg-black/50"></div>
        <div className="relative p-4">
          <div className="text-lg font-semibold mb-3 text-center">{t('gift')}</div>
          <div className="grid grid-cols-2 gap-2 mb-3 text-sm">
            {rewards.map((items,i)=>{
              const done = i < day;
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
