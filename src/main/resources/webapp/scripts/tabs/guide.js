const AssetImage = window.AssetImage;
const useAssetSrc = window.useAssetSrc;

function Guide({
  me,
  achievements = [],
  onReloadAchievements,
  achievementsLoading,
  achievementsError,
  onClaimAchievement,
  claimInProgress,
  achievementsClaimable,
}){
  const [section,setSection] = React.useState('locations');
  const [data,setData] = React.useState(null);
  const [error,setError] = React.useState(null);
  const [fishRarityFilter,setFishRarityFilter] = React.useState('all');
  const [locationKind,setLocationKind] = React.useState('regular');
  const [eventLocations,setEventLocations] = React.useState([]);
  const [eventLocationOffset,setEventLocationOffset] = React.useState(0);
  const [eventLocationsHasMore,setEventLocationsHasMore] = React.useState(true);
  const [eventLocationsLoading,setEventLocationsLoading] = React.useState(false);
  const [eventLocationsError,setEventLocationsError] = React.useState(null);
  const eventLocationsSentinelRef = React.useRef(null);
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
    setEventLocations([]);
    setEventLocationOffset(0);
    setEventLocationsHasMore(true);
    setEventLocationsLoading(false);
    setEventLocationsError(null);
    fetch('/api/guide',{credentials:'include'})
      .then(r=>{ if(!r.ok) throw r.status; return r.json(); })
      .then(setData)
      .catch(e=> setError(e===401 ? t('authRequired') : t('loadFailed')));
  },[me.language]);
  const loadEventLocations = React.useCallback((reset=false)=>{
    if(eventLocationsLoading) return;
    if(!reset && !eventLocationsHasMore) return;
    const offset = reset ? 0 : eventLocationOffset;
    setEventLocationsLoading(true);
    setEventLocationsError(null);
    fetch(`/api/guide/event-locations?offset=${offset}&limit=10`,{credentials:'include'})
      .then(r=>{ if(!r.ok) throw r.status; return r.json(); })
      .then(page=>{
        const locations = Array.isArray(page.locations) ? page.locations : [];
        setEventLocations(prev=> reset ? locations : [...prev, ...locations]);
        setEventLocationOffset(Number.isFinite(Number(page.nextOffset)) ? Number(page.nextOffset) : offset + locations.length);
        setEventLocationsHasMore(Boolean(page.hasMore));
      })
      .catch(()=> setEventLocationsError(t('loadFailed')))
      .finally(()=> setEventLocationsLoading(false));
  },[eventLocationOffset, eventLocationsHasMore, eventLocationsLoading, me.language]);
  React.useEffect(()=>{
    if(section==='locations' && locationKind==='special' && eventLocations.length===0 && eventLocationsHasMore && !eventLocationsLoading){
      loadEventLocations(true);
    }
  },[section, locationKind, eventLocations.length, eventLocationsHasMore, eventLocationsLoading, loadEventLocations]);
  React.useEffect(()=>{
    if(section!=='locations' || locationKind!=='special') return undefined;
    if(!eventLocationsHasMore || eventLocationsLoading) return undefined;
    const node = eventLocationsSentinelRef.current;
    if(!node) return undefined;
    if(typeof IntersectionObserver === 'undefined') return undefined;
    const observer = new IntersectionObserver(entries=>{
      if(entries.some(entry=>entry.isIntersecting)) loadEventLocations(false);
    },{rootMargin:'180px'});
    observer.observe(node);
    return ()=>observer.disconnect();
  },[section, locationKind, eventLocationsHasMore, eventLocationsLoading, loadEventLocations, eventLocations.length]);
  React.useEffect(()=>{
    setFishRarityFilter('all');
  },[data?.fish, me.language]);
  React.useEffect(()=>{
    if(section!=='achievements') return;
    if(achievementsLoading) return;
    if((achievements||[]).length===0 && typeof onReloadAchievements === 'function'){
      onReloadAchievements();
    }
  }, [section, achievementsLoading, achievements?.length, onReloadAchievements]);
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
    <div className="mt-4 pb-safe">
      <div className="glass rounded-2xl p-3 shadow-lg mb-4">
        <div className="flex flex-wrap justify-center gap-2">
          <button onClick={()=>setSection('locations')} className={`px-3 py-2 rounded-xl min-w-[140px] text-center ${section==='locations'?'bg-emerald-600':'glass'}`}>{t('locations')}</button>
          <button onClick={()=>setSection('fish')} className={`px-3 py-2 rounded-xl min-w-[140px] text-center ${section==='fish'?'bg-emerald-600':'glass'}`}>{t('fish')}</button>
          <button onClick={()=>setSection('lures')} className={`px-3 py-2 rounded-xl min-w-[140px] text-center ${section==='lures'?'bg-emerald-600':'glass'}`}>{t('lures')}</button>
          <button onClick={()=>setSection('rods')} className={`px-3 py-2 rounded-xl min-w-[140px] text-center ${section==='rods'?'bg-emerald-600':'glass'}`}>{t('rods')}</button>
          <button onClick={()=>setSection('achievements')} className={`px-3 py-2 rounded-xl min-w-[140px] text-center relative ${section==='achievements'?'bg-emerald-600':'glass'}`}>
            {t('achievementsTab')}
            {achievementsClaimable && (
              <span className="absolute -top-2 -right-2 w-5 h-5 rounded-full bg-red-500 text-white text-xs font-bold flex items-center justify-center">!</span>
            )}
          </button>
        </div>
      </div>
      {section==='locations' && (
        <div className="space-y-4">
          <div className="glass rounded-xl p-1 flex gap-1">
            <button onClick={()=>setLocationKind('regular')} className={`flex-1 py-2 rounded-lg ${locationKind==='regular'?'bg-emerald-600':'hover:bg-white/5'}`}>{t('regular')}</button>
            <button onClick={()=>setLocationKind('special')} className={`flex-1 py-2 rounded-lg ${locationKind==='special'?'bg-emerald-600':'hover:bg-white/5'}`}>{t('special')}</button>
          </div>
          {(locationKind==='regular' ? data.locations : eventLocations).map(loc=>{
            const myLoc = me.locations.find(l=>l.id===loc.id) || {};
            const isEvent = loc.isEvent === true || locationKind==='special';
            const unlocked = isEvent || myLoc.unlocked;
            const bgResolver = typeof window.getLocationBackground === 'function'
              ? window.getLocationBackground
              : (id, name) => {
                  const normalized = typeof name === 'string' ? name.trim().toLowerCase() : '';
                  if(normalized && window.LOCATION_BG_BY_NAME && window.LOCATION_BG_BY_NAME[normalized]){
                    return window.LOCATION_BG_BY_NAME[normalized];
                  }
                  const numId = Number(id);
                  if(Number.isFinite(numId) && window.LOCATION_BG && window.LOCATION_BG[numId]){
                    return window.LOCATION_BG[numId];
                  }
                  return null;
                };
            const bgUrl = loc.imageUrl || bgResolver(loc.id, myLoc.name || loc.name);
            return (
              <div key={loc.id} className="p-4 glass rounded-xl text-center">
                <div className="font-semibold mb-2">{myLoc.name || loc.name}</div>
                {isEvent && <div className="text-xs mb-2 text-amber-200">{t('specialEvent')}</div>}
                {unlocked ? (
                  <>
                    {bgUrl ? (
                      loc.imageUrl ? (
                        <img src={bgUrl} alt={loc.name} className="w-full h-40 object-cover rounded-lg mb-2"/>
                      ) : (
                        <AssetImage src={bgUrl} alt={loc.name} className="w-full h-40 object-cover rounded-lg mb-2"/>
                      )
                    ) : (
                      <div className="w-full h-40 bg-gray-800 rounded-lg mb-2 flex items-center justify-center text-3xl">?</div>
                    )}
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
          {locationKind==='special' && eventLocationsLoading && (
            <div className="text-center opacity-70 py-3">{t('loading')}</div>
          )}
          {locationKind==='special' && eventLocationsError && !eventLocationsLoading && (
            <div className="text-center text-red-300 py-3">{eventLocationsError}</div>
          )}
          {locationKind==='special' && !eventLocationsLoading && !eventLocationsError && eventLocations.length===0 && (
            <div className="text-center opacity-70 py-3">{t('eventsEmpty')}</div>
          )}
          {locationKind==='special' && eventLocationsHasMore && (
            <div ref={eventLocationsSentinelRef} className="h-8"/>
          )}
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
                  <AssetImage src={FISH_IMG[f.name]} alt={f.name} className="w-24 h-24 object-contain mb-2 mx-auto" onError={e=>{ if(e?.currentTarget) e.currentTarget.style.display='none'; }} />
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
            const price = typeof myRod.priceStars === 'number' ? `${myRod.priceStars}⭐` : null;
            const kgRequired = Number(rod.unlockKg).toFixed(0);
            const status = unlocked
              ? t('unlocked')
              : price ? t('rodLockedStars', {kg: kgRequired, stars: price}) : t('requiresKg', kgRequired);
            const rodImage = (window.ROD_IMAGES && (window.ROD_IMAGES[rod.code] || window.ROD_IMAGES.default)) || ROD_IMG;
            return (
              <div key={rod.code} className="p-4 glass rounded-xl">
                <div className="flex items-start gap-3">
                  <AssetImage
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
      {section==='achievements' && (
        <AchievementsSection
          achievements={achievements}
          loading={achievementsLoading}
          error={achievementsError}
          onReload={onReloadAchievements}
          onClaim={onClaimAchievement}
          claimInProgress={claimInProgress}
        />
      )}
    </div>
  );
}

function AchievementsSection({achievements, loading, error, onReload, onClaim, claimInProgress}){
  const imageForAchievement = (code, level) => {
    if(typeof window.achievementImage === 'function') return window.achievementImage(code, level);
    if(window.ACHIEVEMENT_IMAGES && window.ACHIEVEMENT_IMAGES[level]) return window.ACHIEVEMENT_IMAGES[level];
    return window.ACHIEVEMENT_IMAGES ? window.ACHIEVEMENT_IMAGES[0] : '';
  };
  if(loading) return <div className="mt-6 text-center opacity-70">{t('loading')}</div>;
  if(error) return (
    <div className="mt-6 text-center opacity-80">
      <div className="mb-2">{t('achievementsUnavailable')}</div>
      {onReload && <button className="px-3 py-1 rounded-xl bg-emerald-600 hover:bg-emerald-500" onClick={onReload}>{t('achievementsRefresh')}</button>}
    </div>
  );
  if(!achievements || achievements.length===0) return <div className="mt-6 text-center opacity-80">{t('achievementsEmpty')}</div>;
  return (
    <div className="space-y-4">
      {achievements.map(a=>{
        const statusText = a.levelIndex > 0 ? t('achievementInProgress') : t('achievementLocked');
        return (
          <div key={a.code} className="p-4 glass rounded-xl flex gap-3 items-start">
            <AssetImage src={imageForAchievement(a.code, a.levelIndex)} alt={a.name} className="w-16 h-16 object-contain" />
            <div className="flex-1">
              <div className="font-semibold leading-tight">{a.name}</div>
              <div className="text-xs opacity-70 mt-1 mb-2">{a.description}</div>
              <div className="text-sm font-semibold">
                {t('achievementProgressLabel', {
                  progress: a.progressLabel ?? a.progress,
                  target: a.targetLabel ?? a.target
                })}
              </div>
              {a.claimable ? (
                <button
                  type="button"
                  className="mt-1 px-3 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 disabled:opacity-60"
                  disabled={claimInProgress === a.code}
                  onClick={()=> onClaim && onClaim(a.code)}
                >
                  {claimInProgress === a.code ? t('loading') : t('achievementClaim')}
                </button>
              ) : (
                <div className="text-xs opacity-60">{statusText}</div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
