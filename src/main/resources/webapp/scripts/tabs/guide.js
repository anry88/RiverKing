function Guide({me}){
  const [section,setSection] = React.useState('locations');
  const [data,setData] = React.useState(null);
  const [error,setError] = React.useState(null);
  const [fishRarityFilter,setFishRarityFilter] = React.useState('all');
  const rodBonusText = React.useCallback((rod)=>{
    if(!rod || !rod.bonusWater) return t('rodNoBonus');
    if(rod.bonusWater==='fresh' && rod.bonusPredator) return t('rodBonusFreshPredator');
    if(rod.bonusWater==='fresh' && !rod.bonusPredator) return t('rodBonusFreshPeaceful');
    if(rod.bonusWater==='salt' && rod.bonusPredator) return t('rodBonusSaltPredator');
    if(rod.bonusWater==='salt' && !rod.bonusPredator) return t('rodBonusSaltPeaceful');
    return t('rodNoBonus');
  }, [me.language]);
  React.useEffect(()=>{
    setData(null); setError(null);
    fetch('/api/guide',{credentials:'include'})
      .then(r=>{ if(!r.ok) throw r.status; return r.json(); })
      .then(setData)
      .catch(e=> setError(e===401 ? t('authRequired') : t('loadFailed')));
  },[me.language]);
  React.useEffect(()=>{
    setFishRarityFilter('all');
  },[data?.fish, me.language]);
  const rarityDisplayName = React.useCallback((rarity)=>{
    const names = (window.rarityNames && window.rarityNames[me.language])
      || (window.rarityNames && window.rarityNames.en)
      || {};
    return names[rarity] || rarity;
  },[me.language]);
  const fishRarityOptions = React.useMemo(()=>{
    if(!data?.fish) return [];
    const order = ['common','uncommon','rare','epic','mythic','legendary'];
    const set = new Set();
    data.fish.forEach(f=>{ if(f?.rarity) set.add(f.rarity); });
    return Array.from(set).sort((a,b)=>{
      const ai = order.indexOf(a);
      const bi = order.indexOf(b);
      if(ai===-1 && bi===-1) return a.localeCompare(b);
      if(ai===-1) return 1;
      if(bi===-1) return -1;
      return ai-bi;
    });
  },[data?.fish]);
  const filteredFish = React.useMemo(()=>{
    if(!data?.fish) return [];
    if(fishRarityFilter==='all') return data.fish;
    return data.fish.filter(f=>f.rarity===fishRarityFilter);
  },[data?.fish, fishRarityFilter]);
  if(error) return <div className="mt-6 text-center opacity-70">{error}</div>;
  if(!data) return <div className="mt-6 text-center opacity-70">{t('loading')}</div>;
  return (
    <div className="mt-4">
      <div className="flex gap-2 mb-4">
        <button onClick={()=>setSection('locations')} className={`flex-1 py-2 rounded-xl ${section==='locations'?'bg-emerald-600':'glass'}`}>{t('locations')}</button>
        <button onClick={()=>setSection('fish')} className={`flex-1 py-2 rounded-xl ${section==='fish'?'bg-emerald-600':'glass'}`}>{t('fish')}</button>
        <button onClick={()=>setSection('lures')} className={`flex-1 py-2 rounded-xl ${section==='lures'?'bg-emerald-600':'glass'}`}>{t('lures')}</button>
        <button onClick={()=>setSection('rods')} className={`flex-1 py-2 rounded-xl ${section==='rods'?'bg-emerald-600':'glass'}`}>{t('rods')}</button>
      </div>
      {section==='locations' && (
        <div className="space-y-4">
          {data.locations.map(loc=>{
            const myLoc = me.locations.find(l=>l.id===loc.id) || {};
            const unlocked = myLoc.unlocked;
            return (
              <div key={loc.id} className="p-4 glass rounded-xl text-center">
                <div className="font-semibold mb-2">{myLoc.name || loc.name}</div>
                {unlocked ? (
                  <>
                    <img src={LOCATION_BG[loc.id]} alt={loc.name} className="w-full h-40 object-cover rounded-lg mb-2"/>
                    <div className="text-sm opacity-80 text-left">{t('fishes')}</div>
                    <div className="text-sm mb-2 text-left">
                      {loc.fish.map((f,i)=>(
                        <span key={f.name} className={rarityColors[f.rarity]||''}>
                          {f.name}{i<loc.fish.length-1?', ':''}
                        </span>
                      ))}
                    </div>
                    <div className="text-sm opacity-80 text-left">{t('baitsLabel')}</div>
                    <div className="text-sm opacity-80 text-left">
                      {loc.lures.map((l,i)=>(
                        <span key={l} className={lureColor(l)}>
                          {l}{i<loc.lures.length-1?', ':''}
                        </span>
                      ))}
                    </div>
                  </>
                ) : (
                  <>
                    <div className="w-full h-40 bg-gray-800 rounded-lg mb-2 flex items-center justify-center text-3xl">?</div>
                    <div className="text-sm opacity-70">{t('reachKg', myLoc.unlockKg)}</div>
                  </>
                )}
              </div>
            );
          })}
        </div>
      )}
      {section==='fish' && (
        <div className="space-y-4">
          {fishRarityOptions.length>0 && (
            <div>
              <div className="text-sm opacity-70 mb-1">{t('rarityFilterLabel')}</div>
              <select
                value={fishRarityFilter}
                onChange={e=>setFishRarityFilter(e.target.value)}
                className="w-full p-2 rounded-lg bg-black/20 border border-white/10"
              >
                <option value="all">{t('allRarities')}</option>
                {fishRarityOptions.map(r=>(
                  <option key={r} value={r}>{rarityDisplayName(r)}</option>
                ))}
              </select>
            </div>
          )}
          {filteredFish.map(f=>{
            const caught = (me.caughtFishIds||[]).includes(f.id);
            return (
              <div key={f.id} className="p-4 glass rounded-xl text-center">
                {caught ? (
                  <img src={FISH_IMG[f.name]} alt={f.name} className="w-24 h-24 object-contain mb-2 mx-auto" onError={e=>e.currentTarget.style.display='none'} />
                ) : (
                  <div className="w-24 h-24 bg-gray-800 rounded mb-2 mx-auto flex items-center justify-center relative">
                    <span className="text-4xl opacity-20">🐟</span>
                    <span className="absolute text-2xl">?</span>
                  </div>
                )}
                <div className={`font-semibold ${caught?(rarityColors[f.rarity]||''):''}`}>{caught ? f.name : '???'}</div>
                {caught ? (
                  <>
                    <div className="text-xs mt-1">{t('locationsLabel')} {f.locations.join(', ')}</div>
                    <div className="text-xs mt-1">{t('baitsLabel')} {f.lures.map((l,i)=>(<span key={l} className={lureColor(l)}>{l}{i<f.lures.length-1?', ':''}</span>))}</div>
                  </>
                ) : (
                  <div className="text-xs mt-1 opacity-70">{t('catchToLearn')}</div>
                )}
              </div>
            );
          })}
        </div>
      )}
      {section==='lures' && (
        <div className="space-y-4">
          {data.lures.map(l=>(
            <div key={l.name} className="p-4 glass rounded-xl">
              <div className={`font-semibold ${lureColor(l.name)}`}>{l.name}</div>
              <div className="text-xs mt-1">{t('fishes')}</div>
              <div className="text-xs mb-1">
                {l.fish.map((f,i)=>(
                  <span key={f.name} className={rarityColors[f.rarity]||''}>
                    {f.name}{i<l.fish.length-1?', ':''}
                  </span>
                ))}
              </div>
              <div className="text-xs">{t('locationsLabel')} {l.locations.join(', ')}</div>
            </div>
          ))}
        </div>
      )}
      {section==='rods' && (
        <div className="space-y-4">
          {data.rods.map(rod=>{
            const myRod = (me.rods||[]).find(r=>r.code===rod.code) || {};
            const unlocked = myRod.unlocked;
            const status = unlocked ? t('unlocked') : t('requiresKg', Number(rod.unlockKg).toFixed(0));
            const rodImage = (window.ROD_IMAGES && (window.ROD_IMAGES[rod.code] || window.ROD_IMAGES.default)) || ROD_IMG;
            return (
              <div key={rod.code} className="p-4 glass rounded-xl">
                <div className="flex items-start gap-3">
                  <img
                    src={rodImage}
                    alt={rod.name}
                    className="w-16 h-16 object-contain shrink-0"
                    loading="lazy"
                    decoding="async"
                  />
                  <div>
                    <div className="font-semibold">{rod.name}</div>
                    <div className="text-xs opacity-70 mt-1">{status}</div>
                    <div className="text-xs mt-2">{rodBonusText(rod)}</div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
