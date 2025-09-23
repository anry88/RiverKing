function Ratings({me, setMe, onCatchClick}){
  const [mode,setMode] = React.useState('personal');
  const [section,setSection] = React.useState('location');
  const [period,setPeriod] = React.useState('all');
  const [locId,setLocId] = React.useState(String(me.locationId ?? 'all'));
  const [fishList,setFishList] = React.useState([]);
  const [fishId,setFishId] = React.useState('all');
  const [list,setList] = React.useState([]);
  const [order,setOrder] = React.useState('desc');

  React.useEffect(()=>{
    fetch('/api/guide',{credentials:'include'})
      .then(r=>r.json())
      .then(d=> setFishList(d.fish||[]))
      .catch(()=>{});
  },[me.language]);

  React.useEffect(()=>{
    if(me.needsNickname) return;
    if(section==='location'){
      fetch(`/api/ratings/${mode}/location/${locId}?period=${period}&order=${order}`,{credentials:'include'})
        .then(r=>r.json()).then(setList).catch(()=>setList([]));
    } else if(section==='species' && fishId){
      fetch(`/api/ratings/${mode}/species/${fishId}?period=${period}&order=${order}`,{credentials:'include'})
        .then(r=>r.json()).then(setList).catch(()=>setList([]));
    }
  },[mode,section,locId,fishId,period,order,me.needsNickname,me.language]);

  return (
    <div className="mt-4 space-y-4">
      <div className="flex flex-wrap gap-2 text-sm">
        <select value={mode} onChange={e=>setMode(e.target.value)} className="flex-1 p-2 rounded-lg bg-black/20 border border-white/10">
          <option value="personal">{t('personal')}</option>
          <option value="global">{t('global')}</option>
        </select>
        <select value={section} onChange={e=>setSection(e.target.value)} className="flex-1 p-2 rounded-lg bg-black/20 border border-white/10">
          <option value="location">{t('locations')}</option>
          <option value="species">{t('species')}</option>
        </select>
        <select value={period} onChange={e=>setPeriod(e.target.value)} className="flex-1 p-2 rounded-lg bg-black/20 border border-white/10">
          <option value="today">{t('today')}</option>
          <option value="yesterday">{t('yesterday')}</option>
          <option value="week">{t('lastWeek')}</option>
          <option value="month">{t('lastMonth')}</option>
          <option value="year">{t('lastYear')}</option>
          <option value="all">{t('allTime')}</option>
        </select>
        <select value={order} onChange={e=>setOrder(e.target.value)} className="flex-1 p-2 rounded-lg bg-black/20 border border-white/10">
          <option value="desc">{t('largest')}</option>
          <option value="asc">{t('smallest')}</option>
        </select>
      </div>

      {section==='location' && (
        <div className="space-y-3">
          <select value={locId} onChange={e=>setLocId(e.target.value)} className="w-full p-2 rounded-lg bg-black/20 border border-white/10">
            <option value="all">{t('allLocations')}</option>
            {me.locations.map(l=> <option key={l.id} value={String(l.id)}>{l.name}</option>)}
          </select>
          <div className="space-y-2">
            {list.map((c,i)=>(
              <button
                key={i}
                type="button"
                onClick={()=>{
                  if(onCatchClick && c.id){
                    onCatchClick({
                      ...c,
                      userId: c.userId ?? me.id,
                      user: c.user,
                    });
                  }
                }}
                className="w-full p-3 glass rounded-xl flex justify-between text-left"
              >
                <div className="flex items-center gap-3">
                  {(me.caughtFishIds||[]).includes(c.fishId) ? (
                    <img src={FISH_IMG[c.fish]} alt={c.fish} className="w-10 h-10 object-contain" onError={e=>e.currentTarget.style.display='none'} />
                  ) : (
                    <div className="w-10 h-10 bg-gray-800 rounded flex items-center justify-center relative">
                      <span className="text-xl opacity-20">🐟</span>
                      <span className="absolute">?</span>
                    </div>
                  )}
                  <div>
                    <div className={`font-medium ${rarityColors[c.rarity]||''}`}>{c.fish}</div>
                    <div className="text-xs opacity-70"><bdi>{c.user}</bdi> • {c.at?new Date(c.at).toLocaleString():''}</div>
                  </div>
                </div>
                <div className="font-semibold">{Number(c.weight).toFixed(2)} {t('kg')}</div>
              </button>
            ))}
            {list.length===0 && <div className="text-center text-sm opacity-70">{t('noData')}</div>}
          </div>
        </div>
      )}

      {section==='species' && (
        <div className="space-y-3">
          <select value={fishId} onChange={e=>setFishId(e.target.value)} className="w-full p-2 rounded-lg bg-black/20 border border-white/10">
            <option value="all">{t('allFish')}</option>
            {fishList.map(f=> <option key={f.id} value={String(f.id)}>{f.name}</option>)}
          </select>
          <div className="space-y-2">
            {list.map((c,i)=>(
              <button
                key={i}
                type="button"
                onClick={()=>{
                  if(onCatchClick && c.id){
                    onCatchClick({
                      ...c,
                      userId: c.userId,
                      user: c.user,
                    });
                  }
                }}
                className="w-full p-3 glass rounded-xl flex justify-between text-left"
              >
                <div className="flex items-center gap-3">
                  {(me.caughtFishIds||[]).includes(c.fishId) ? (
                    <img src={FISH_IMG[c.fish]} alt={c.fish} className="w-10 h-10 object-contain" onError={e=>e.currentTarget.style.display='none'} />
                  ) : (
                    <div className="w-10 h-10 bg-gray-800 rounded flex items-center justify-center relative">
                      <span className="text-xl opacity-20">🐟</span>
                      <span className="absolute">?</span>
                    </div>
                  )}
                  <div>
                    <div className={`font-medium ${rarityColors[c.rarity]||''}`}>{c.fish}</div>
                    <div className="text-xs opacity-70"><bdi>{c.user}</bdi> • {c.location} • {c.at?new Date(c.at).toLocaleString():''}</div>
                  </div>
                </div>
                <div className="font-semibold">{Number(c.weight).toFixed(2)} {t('kg')}</div>
              </button>
            ))}
            {list.length===0 && <div className="text-center text-sm opacity-70">{t('noData')}</div>}
          </div>
        </div>
      )}
    </div>
  );
}

