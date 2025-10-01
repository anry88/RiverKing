function Ratings({me, setMe, onCatchClick}){
  const [mode,setMode] = React.useState('global');
  const [period,setPeriod] = React.useState('today');
  const defaultLocationId = String(me.locationId ?? 'all');
  const [locId,setLocId] = React.useState(defaultLocationId);
  const [fishList,setFishList] = React.useState([]);
  const [fishId,setFishId] = React.useState('all');
  const [list,setList] = React.useState([]);
  const [order,setOrder] = React.useState('desc');

  React.useEffect(()=>{
    setLocId(defaultLocationId);
  },[defaultLocationId]);

  React.useEffect(()=>{
    fetch('/api/guide',{credentials:'include'})
      .then(r=>r.json())
      .then(d=> setFishList(d.fish||[]))
      .catch(()=>{});
  },[me.language]);

  React.useEffect(()=>{
    if(me.needsNickname) return;
    const usingSpecies = fishId !== 'all';
    const url = usingSpecies
      ? `/api/ratings/${mode}/species/${fishId}?period=${period}&order=${order}`
      : `/api/ratings/${mode}/location/${locId}?period=${period}&order=${order}`;
    fetch(url,{credentials:'include'})
      .then(r=>r.json())
      .then(data=>{
        if(usingSpecies && locId !== 'all'){
          const locName = (me.locations||[]).find(l=>String(l.id)===String(locId))?.name;
          if(locName){
            setList(data.filter(c=>c.location===locName));
            return;
          }
        }
        setList(data);
      })
      .catch(()=>setList([]));
  },[mode,locId,fishId,period,order,me.needsNickname,me.language,me.locations]);

  const usingSpecies = fishId !== 'all';

  return (
    <div className="mt-4 space-y-4">
      <div className="flex flex-wrap gap-2 text-sm">
        <select value={mode} onChange={e=>setMode(e.target.value)} className="flex-1 p-2 rounded-lg bg-black/20 border border-white/10">
          <option value="personal">{t('personal')}</option>
          <option value="global">{t('global')}</option>
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

      <div className="flex flex-wrap gap-2 text-sm">
        <select value={locId} onChange={e=>setLocId(e.target.value)} className="flex-1 min-w-[150px] p-2 rounded-lg bg-black/20 border border-white/10">
          <option value="all">{t('allLocations')}</option>
          {(me.locations||[]).map(l=> <option key={l.id} value={String(l.id)}>{l.name}</option>)}
        </select>
        <select value={fishId} onChange={e=>setFishId(e.target.value)} className="flex-1 min-w-[150px] p-2 rounded-lg bg-black/20 border border-white/10">
          <option value="all">{t('allFish')}</option>
          {fishList.map(f=> <option key={f.id} value={String(f.id)}>{f.name}</option>)}
        </select>
      </div>

      <div className="space-y-2">
        {list.map((c,i)=>(
          <button
            key={i}
            type="button"
            onClick={()=>{
              if(onCatchClick && c.id){
                onCatchClick({
                  ...c,
                  userId: usingSpecies ? c.userId : (c.userId ?? me.id),
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
                <div className="text-xs opacity-70">
                  <bdi>{c.user}</bdi>
                  {usingSpecies || locId==='all' ? (
                    <> • {c.location}</>
                  ) : null}
                  {' '}• {c.at?new Date(c.at).toLocaleString():''}
                </div>
              </div>
            </div>
            <div className="font-semibold">{Number(c.weight).toFixed(2)} {t('kg')}</div>
          </button>
        ))}
        {list.length===0 && <div className="text-center text-sm opacity-70">{t('noData')}</div>}
      </div>
    </div>
  );
}

