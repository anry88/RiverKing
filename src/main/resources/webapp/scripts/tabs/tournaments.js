function tournamentFish(name, rarity){
  if(!rarity) return t('anyFish');
  const cls = rarityColors[rarity]||'';
  if(name) return <span className={cls}>{name}</span>;
  const lang = document.documentElement.lang;
  const rn = rarityNames[lang] || rarityNames.ru;
  return <span className={cls}>{(rn[rarity]||rarity)} {t('fishWord')}</span>;
}

const AGGREGATE_METRICS = new Set(['count', 'total_weight']);
const isAggregateMetric = metric => AGGREGATE_METRICS.has(metric);
const AssetImage = window.AssetImage;

function TournamentsTab({
  me,
  tournamentTab,
  setTournamentTab,
  currentTournament,
  upcomingTournaments,
  pastTournaments,
  pastResult,
  openPast,
  setPastResult,
  prizeHint,
  setPrizeHint,
  shop,
  onCatchClick
}){
  React.useEffect(()=>{
    if(currentTournament?.mine){
      setTimeout(()=>{
        document.getElementById('current-tournament-mine')?.scrollIntoView({block:'center'});
      },0);
    }
  },[currentTournament]);

  React.useEffect(()=>{
    if(pastResult?.mine){
      setTimeout(()=>{
        document.getElementById('past-tournament-mine')?.scrollIntoView({block:'center'});
      },0);
    }
  },[pastResult]);

  const shopPacks = React.useMemo(() => shop.reduce((acc,c)=>acc.concat(c.packs),[]), [shop]);
  const locale = (typeof document !== 'undefined' && document.documentElement.lang === 'ru') ? 'ru-RU' : 'en-US';

  const formatPrize = React.useCallback((prize) => {
    if(!prize) return null;
    const isCoins = prize.packageId === 'coins' || typeof prize.coins === 'number';
    if(isCoins){
      const amount = Number(prize.coins ?? prize.qty);
      if(!Number.isFinite(amount) || amount <= 0) return null;
      return { label: t('coins'), amount, isCoins: true };
    }
    const packageId = prize.packageId;
    if(!packageId) return null;
    const pack = shopPacks.find(p=>p.id===packageId);
    const label = pack?.name
      || (packageId==='autofish_week' ? t('autofishWeek') : packageId);
    const amount = prize.qty || 1;
    return { label, amount, isCoins: false };
  }, [shopPacks]);

  const renderPrizeIcon = React.useCallback((prize) => {
    if(!prize) return null;
    const isCoins = prize.packageId === 'coins' || typeof prize.coins === 'number';
    if(isCoins){
      return <span className="text-lg">🪙</span>;
    }
    const packageId = prize.packageId;
    if(!packageId) return null;
    const src = String(packageId).startsWith('autofish')
      ? '/app/assets/shop/autofish.png'
      : `/app/assets/shop/${packageId}.png`;
    return <AssetImage src={src} alt="" className="w-5 h-5 object-contain" onError={ev=>{ if(ev?.currentTarget) ev.currentTarget.style.display='none'; }} />;
  }, []);

  const renderPrizeHint = (rank, prize) => {
    if(prizeHint?.rank !== rank) return null;
    const info = formatPrize(prize);
    if(!info) return null;
    const amountText = info.isCoins
      ? `+${Number(info.amount).toLocaleString(locale)}`
      : (info.amount > 1 ? `x${info.amount}` : '');
    const content = info.isCoins
      ? `🪙 ${amountText}`
      : `${info.label}${amountText ? ` ${amountText}` : ''}`;
    return (
      <div className="absolute right-0 top-full mt-1 text-xs bg-gray-800 border border-white/10 p-2 whitespace-nowrap z-10 rounded-lg text-amber-300">
        {content}
      </div>
    );
  };

  const currentPrizeLeaderboard = React.useMemo(() => {
    if (!currentTournament) return [];
    const list = currentTournament.leaderboard || [];
    const limit = currentTournament.tournament.prizePlaces || 0;
    return limit > 0 ? list.slice(0, limit) : list;
  }, [currentTournament]);

  const pastPrizeLeaderboard = React.useMemo(() => {
    if (!pastResult) return [];
    const list = pastResult.leaderboard || [];
    const limit = pastResult.tournament.prizePlaces || 0;
    return limit > 0 ? list.slice(0, limit) : list;
  }, [pastResult]);

  const currentPrizeCount = currentPrizeLeaderboard.length;
  const pastPrizeCount = pastPrizeLeaderboard.length;

  return (
    <div className="mt-6">
      <div className="flex mb-4">
        <button onClick={()=>setTournamentTab('past')} className={`flex-1 py-2 ${tournamentTab==='past'?'text-emerald-400':''}`}>{t('past')}</button>
        <button onClick={()=>setTournamentTab('current')} className={`flex-1 py-2 ${tournamentTab==='current'?'text-emerald-400':''}`}>{t('current')}</button>
        <button onClick={()=>setTournamentTab('upcoming')} className={`flex-1 py-2 ${tournamentTab==='upcoming'?'text-emerald-400':''}`}>{t('upcoming')}</button>
      </div>
      {tournamentTab==='current' ? (
        currentTournament ? (
          (
            <div>
              <div className="p-4 rounded-xl glass text-left mb-4">
                <div className="text-base font-semibold mb-1">{currentTournament.tournament.name}</div>
                <div className="text-xs opacity-70">{new Date(currentTournament.tournament.startTime*1000).toLocaleString()} — {new Date(currentTournament.tournament.endTime*1000).toLocaleString()}</div>
                <div className="text-sm opacity-80">{t('fishLabel')} {tournamentFish(currentTournament.tournament.fish, currentTournament.tournament.fishRarity)}</div>
                <div className="text-sm opacity-80">{t('locationLabel')} {currentTournament.tournament.location || t('anyLocation')}</div>
                <div className="text-sm opacity-80">{t('conditionsLabel')} {t(currentTournament.tournament.metric)}</div>
                <div className="text-sm opacity-80">{t('prizePlacesLabel')} {currentTournament.tournament.prizePlaces}</div>
              </div>
              <div className="space-y-2">
                {currentPrizeLeaderboard.map(e=> {
                  const isMine = currentTournament.mine && e.rank===currentTournament.mine.rank;
                  const catchData = (!isAggregateMetric(currentTournament.tournament.metric) && e.catchId && e.fish) ? {
                    id: e.catchId,
                    fish: e.fish,
                    weight: e.value,
                    location: e.location,
                    at: e.at ? new Date(e.at*1000).toISOString() : null,
                    user: e.user || '-',
                    userId: e.userId,
                  } : null;
                  return (
                  <div
                    key={e.rank}
                    id={isMine ? 'current-tournament-mine' : undefined}
                    className={`p-3 rounded-xl glass flex items-center justify-between relative ${prizeHint?.rank===e.rank ? 'z-10' : ''} ${isMine ? 'border border-emerald-400' : ''}`}
                    onClick={()=>{ if(catchData && onCatchClick){ onCatchClick(catchData); } }}
                  >
                    <div className="flex items-center gap-2 w-full min-w-0">
                      <div className="w-12 h-8 flex items-center justify-center gap-1 shrink-0">
                        <span>{e.rank}</span>
                        {e.prize && (
                          <button
                            type="button"
                            onClick={ev=>{ev.stopPropagation(); setPrizeHint(p=>p && p.rank===e.rank ? null : {rank:e.rank, prize:e.prize});}}
                            className="w-6 h-6 flex items-center justify-center rounded-md hover:bg-white/10"
                            aria-label={t('prizes')}
                          >
                            <span className="w-5 h-5 flex items-center justify-center">
                              {renderPrizeIcon(e.prize)}
                            </span>
                          </button>
                        )}
                      </div>
                      {isAggregateMetric(currentTournament.tournament.metric) ? (
                        <div className="w-8 h-8"></div>
                      ) : e.fish ? (
                        (me.caughtFishIds||[]).includes(e.fishId) ? (
                          <AssetImage src={FISH_IMG[e.fish]} alt={e.fish} className="w-8 h-8 object-contain shrink-0" onError={ev=>{ if(ev?.currentTarget) ev.currentTarget.style.display='none'; }} />
                        ) : (
                          <div className="w-8 h-8 bg-gray-800 rounded flex items-center justify-center relative shrink-0">
                            <span className="text-lg opacity-20">🐟</span>
                            <span className="absolute">?</span>
                          </div>
                        )
                      ) : (
                        <div className="w-8 h-8 bg-gray-800 rounded flex items-center justify-center shrink-0">
                          <span className="text-lg opacity-20">🐟</span>
                        </div>
                      )}
                      <div className="min-w-0">
                        <div className="flex items-center gap-1 flex-wrap min-w-0 text-sm leading-tight">
                          <bdi className="truncate break-words">{e.user||'-'}</bdi>
                        </div>
                        {!isAggregateMetric(currentTournament.tournament.metric) && (
                          e.fish ? (
                            <div className="text-xs opacity-70 truncate">{e.fish} — {e.location} — {new Date(e.at*1000).toLocaleString()}</div>
                          ) : (
                            <div className="text-xs opacity-70 truncate">{t('anyFish')}</div>
                          )
                        )}
                      </div>
                    </div>
                    <div className="w-12 text-right shrink-0">
                      <div>{currentTournament.tournament.metric==='count'? Number(e.value).toFixed(0) : Number(e.value).toFixed(2)}</div>
                      {currentTournament.tournament.metric!=='count' && <div className="text-xs">{t('kg')}</div>}
                    </div>
                  {renderPrizeHint(e.rank, e.prize)}
                </div>
                );
                })}
                {currentTournament.mine && currentTournament.mine.rank>currentPrizeCount && (()=>{
                  const mineCatch = (!isAggregateMetric(currentTournament.tournament.metric) && currentTournament.mine.catchId && currentTournament.mine.fish) ? {
                    id: currentTournament.mine.catchId,
                    fish: currentTournament.mine.fish,
                    weight: currentTournament.mine.value,
                    location: currentTournament.mine.location,
                    at: currentTournament.mine.at ? new Date(currentTournament.mine.at*1000).toISOString() : null,
                    user: currentTournament.mine.user || t('you'),
                    userId: currentTournament.mine.userId,
                  } : null;
                  return (
                  <div
                    id="current-tournament-mine"
                    className={`p-3 rounded-xl glass flex items-center justify-between relative ${prizeHint?.rank===currentTournament.mine.rank ? 'z-10' : ''} border border-emerald-400`}
                    onClick={()=>{ if(mineCatch && onCatchClick){ onCatchClick(mineCatch); } }}
                  >
                    <div className="flex items-center gap-2 w-full min-w-0">
                      <div className="w-12 h-8 flex items-center justify-center gap-1 shrink-0">
                        <span>{currentTournament.mine.rank}</span>
                        {currentTournament.mine.prize && (
                          <button
                            type="button"
                            onClick={ev=>{ev.stopPropagation(); setPrizeHint(p=>p && p.rank===currentTournament.mine.rank ? null : {rank:currentTournament.mine.rank, prize:currentTournament.mine.prize});}}
                            className="w-6 h-6 flex items-center justify-center rounded-md hover:bg-white/10"
                            aria-label={t('prizes')}
                          >
                            <span className="w-5 h-5 flex items-center justify-center">
                              {renderPrizeIcon(currentTournament.mine.prize)}
                            </span>
                          </button>
                        )}
                      </div>
                      {isAggregateMetric(currentTournament.tournament.metric) ? (
                        <div className="w-8 h-8"></div>
                      ) : currentTournament.mine.fish ? (
                        (me.caughtFishIds||[]).includes(currentTournament.mine.fishId) ? (
                          <AssetImage src={FISH_IMG[currentTournament.mine.fish]} alt={currentTournament.mine.fish} className="w-8 h-8 object-contain shrink-0" onError={ev=>{ if(ev?.currentTarget) ev.currentTarget.style.display='none'; }} />
                        ) : (
                          <div className="w-8 h-8 bg-gray-800 rounded flex items-center justify-center relative shrink-0">
                            <span className="text-lg opacity-20">🐟</span>
                            <span className="absolute">?</span>
                          </div>
                        )
                      ) : (
                        <div className="w-8 h-8 bg-gray-800 rounded flex items-center justify-center shrink-0">
                          <span className="text-lg opacity-20">🐟</span>
                        </div>
                      )}
                      <div className="min-w-0">
                        <div className="flex items-center gap-1 flex-wrap min-w-0 text-sm leading-tight">
                          <bdi className="truncate break-words">{currentTournament.mine.user||t('you')}</bdi>
                        </div>
                        {!isAggregateMetric(currentTournament.tournament.metric) && (
                          currentTournament.mine.fish ? (
                            <div className="text-xs opacity-70 truncate">{currentTournament.mine.fish} — {currentTournament.mine.location} — {new Date(currentTournament.mine.at*1000).toLocaleString()}</div>
                          ) : (
                            <div className="text-xs opacity-70 truncate">{t('anyFish')}</div>
                          )
                        )}
                      </div>
                    </div>
                    <div className="w-12 text-right shrink-0">
                      <div>{currentTournament.tournament.metric==='count'? Number(currentTournament.mine.value).toFixed(0) : Number(currentTournament.mine.value).toFixed(2)}</div>
                      {currentTournament.tournament.metric!=='count' && <div className="text-xs">{t('kg')}</div>}
                    </div>
                    {renderPrizeHint(currentTournament.mine.rank, currentTournament.mine.prize)}
                  </div>
                  );
                })()}
              </div>
            </div>
          )
        ) : (
          <div className="text-center opacity-70">{t('tournamentsSoon')}</div>
        )
      ) : tournamentTab==='upcoming' ? (
        upcomingTournaments.length===0 ? (
          <div className="text-center opacity-70">{t('noData')}</div>
        ) : (
          <div className="space-y-4">
            {upcomingTournaments.map(tn=>(
              <div key={tn.id} className="p-4 rounded-xl glass text-left">
                <div className="text-base font-semibold mb-1">{tn.name}</div>
                <div className="text-xs opacity-70">{new Date(tn.startTime*1000).toLocaleString()} — {new Date(tn.endTime*1000).toLocaleString()}</div>
                <div className="text-sm opacity-80">{t('fishLabel')} {tournamentFish(tn.fish, tn.fishRarity)}</div>
                <div className="text-sm opacity-80">{t('locationLabel')} {tn.location || t('anyLocation')}</div>
                <div className="text-sm opacity-80">{t('conditionsLabel')} {t(tn.metric)}</div>
                <div className="text-sm opacity-80">{t('prizePlacesLabel')} {tn.prizePlaces}</div>
              </div>
            ))}
          </div>
        )
      ) : (
        pastResult ? (
          <div>
            <button onClick={()=>setPastResult(null)} className="mb-4 text-emerald-400">{t('cancel')}</button>
            <div className="p-4 rounded-xl glass text-left mb-4">
              <div className="text-base font-semibold mb-1">{pastResult.tournament.name}</div>
              <div className="text-xs opacity-70">{new Date(pastResult.tournament.startTime*1000).toLocaleString()} — {new Date(pastResult.tournament.endTime*1000).toLocaleString()}</div>
              <div className="text-sm opacity-80">{t('fishLabel')} {tournamentFish(pastResult.tournament.fish, pastResult.tournament.fishRarity)}</div>
              <div className="text-sm opacity-80">{t('locationLabel')} {pastResult.tournament.location || t('anyLocation')}</div>
              <div className="text-sm opacity-80">{t('conditionsLabel')} {t(pastResult.tournament.metric)}</div>
              <div className="text-sm opacity-80">{t('prizePlacesLabel')} {pastResult.tournament.prizePlaces}</div>
            </div>
            <div className="space-y-2">
                {pastPrizeLeaderboard.map(e=> {
                const isMine = pastResult.mine && e.rank===pastResult.mine.rank;
                const catchData = (!isAggregateMetric(pastResult.tournament.metric) && e.catchId && e.fish) ? {
                  id: e.catchId,
                  fish: e.fish,
                  weight: e.value,
                  location: e.location,
                  at: e.at ? new Date(e.at*1000).toISOString() : null,
                  user: e.user || '-',
                  userId: e.userId,
                } : null;
                return (
                <div
                  key={e.rank}
                  id={isMine ? 'past-tournament-mine' : undefined}
                  className={`p-3 rounded-xl glass flex items-center justify-between relative ${prizeHint?.rank===e.rank ? 'z-10' : ''} ${isMine ? 'border border-emerald-400' : ''}`}
                  onClick={()=>{ if(catchData && onCatchClick){ onCatchClick(catchData); } }}
                >
                  <div className="flex items-center gap-2 w-full min-w-0">
                    <div className="w-12 h-8 flex items-center justify-center gap-1 shrink-0">
                      <span>{e.rank}</span>
                      {e.prize && (
                        <button
                          type="button"
                          onClick={ev=>{ev.stopPropagation(); setPrizeHint(p=>p && p.rank===e.rank ? null : {rank:e.rank, prize:e.prize});}}
                          className="w-6 h-6 flex items-center justify-center rounded-md hover:bg-white/10"
                          aria-label={t('prizes')}
                        >
                          <span className="w-5 h-5 flex items-center justify-center">
                            {renderPrizeIcon(e.prize)}
                          </span>
                        </button>
                      )}
                    </div>
                    {isAggregateMetric(pastResult.tournament.metric) ? (
                      <div className="w-8 h-8"></div>
                    ) : e.fish ? (
                      (me.caughtFishIds||[]).includes(e.fishId) ? (
                        <AssetImage src={FISH_IMG[e.fish]} alt={e.fish} className="w-8 h-8 object-contain shrink-0" onError={ev=>{ if(ev?.currentTarget) ev.currentTarget.style.display='none'; }} />
                      ) : (
                        <div className="w-8 h-8 bg-gray-800 rounded flex items-center justify-center relative shrink-0">
                          <span className="text-lg opacity-20">🐟</span>
                          <span className="absolute">?</span>
                        </div>
                      )
                    ) : (
                      <div className="w-8 h-8 bg-gray-800 rounded flex items-center justify-center shrink-0">
                        <span className="text-lg opacity-20">🐟</span>
                      </div>
                    )}
                    <div className="min-w-0">
                      <div className="flex items-center gap-1 flex-wrap min-w-0 text-sm leading-tight">
                        <bdi className="truncate break-words">{e.user||'-'}</bdi>
                      </div>
                      {!isAggregateMetric(pastResult.tournament.metric) && (
                        e.fish ? (
                          <div className="text-xs opacity-70 truncate">{e.fish} — {e.location} — {new Date(e.at*1000).toLocaleString()}</div>
                        ) : (
                          <div className="text-xs opacity-70 truncate">{t('anyFish')}</div>
                        )
                      )}
                    </div>
                  </div>
                  <div className="w-12 text-right shrink-0">
                    <div>{pastResult.tournament.metric==='count'? Number(e.value).toFixed(0) : Number(e.value).toFixed(2)}</div>
                    {pastResult.tournament.metric!=='count' && <div className="text-xs">{t('kg')}</div>}
                  </div>
                {renderPrizeHint(e.rank, e.prize)}
              </div>
              );
              })}
              {pastResult.mine && pastResult.mine.rank>pastPrizeCount && (()=>{
                const mineCatch = (!isAggregateMetric(pastResult.tournament.metric) && pastResult.mine.catchId && pastResult.mine.fish) ? {
                  id: pastResult.mine.catchId,
                  fish: pastResult.mine.fish,
                  weight: pastResult.mine.value,
                  location: pastResult.mine.location,
                  at: pastResult.mine.at ? new Date(pastResult.mine.at*1000).toISOString() : null,
                  user: pastResult.mine.user || t('you'),
                  userId: pastResult.mine.userId,
                } : null;
                return (
                <div
                  id="past-tournament-mine"
                  className={`p-3 rounded-xl glass flex items-center justify-between relative ${prizeHint?.rank===pastResult.mine.rank ? 'z-10' : ''} border border-emerald-400`}
                  onClick={()=>{ if(mineCatch && onCatchClick){ onCatchClick(mineCatch); } }}
                >
                  <div className="flex items-center gap-2 w-full min-w-0">
                    <div className="w-12 h-8 flex items-center justify-center gap-1 shrink-0">
                      <span>{pastResult.mine.rank}</span>
                      {pastResult.mine.prize && (
                        <button
                          type="button"
                          onClick={ev=>{ev.stopPropagation(); setPrizeHint(p=>p && p.rank===pastResult.mine.rank ? null : {rank:pastResult.mine.rank, prize:pastResult.mine.prize});}}
                          className="w-6 h-6 flex items-center justify-center rounded-md hover:bg-white/10"
                          aria-label={t('prizes')}
                        >
                          <span className="w-5 h-5 flex items-center justify-center">
                            {renderPrizeIcon(pastResult.mine.prize)}
                          </span>
                        </button>
                      )}
                    </div>
                    {isAggregateMetric(pastResult.tournament.metric) ? (
                      <div className="w-8 h-8"></div>
                    ) : pastResult.mine.fish ? (
                      (me.caughtFishIds||[]).includes(pastResult.mine.fishId) ? (
                        <AssetImage src={FISH_IMG[pastResult.mine.fish]} alt={pastResult.mine.fish} className="w-8 h-8 object-contain shrink-0" onError={ev=>{ if(ev?.currentTarget) ev.currentTarget.style.display='none'; }} />
                      ) : (
                        <div className="w-8 h-8 bg-gray-800 rounded flex items-center justify-center relative shrink-0">
                          <span className="text-lg opacity-20">🐟</span>
                          <span className="absolute">?</span>
                        </div>
                      )
                    ) : (
                      <div className="w-8 h-8 bg-gray-800 rounded flex items-center justify-center shrink-0">
                        <span className="text-lg opacity-20">🐟</span>
                      </div>
                    )}
                    <div className="min-w-0">
                      <div className="flex items-center gap-1 flex-wrap min-w-0 text-sm leading-tight">
                        <bdi className="truncate break-words">{pastResult.mine.user||t('you')}</bdi>
                      </div>
                      {!isAggregateMetric(pastResult.tournament.metric) && (
                        pastResult.mine.fish ? (
                          <div className="text-xs opacity-70 truncate">{pastResult.mine.fish} — {pastResult.mine.location} — {new Date(pastResult.mine.at*1000).toLocaleString()}</div>
                        ) : (
                          <div className="text-xs opacity-70 truncate">{t('anyFish')}</div>
                        )
                      )}
                    </div>
                  </div>
                  <div className="w-12 text-right shrink-0">
                    <div>{pastResult.tournament.metric==='count'? Number(pastResult.mine.value).toFixed(0) : Number(pastResult.mine.value).toFixed(2)}</div>
                    {pastResult.tournament.metric!=='count' && <div className="text-xs">{t('kg')}</div>}
                  </div>
                  {renderPrizeHint(pastResult.mine.rank, pastResult.mine.prize)}
                </div>
                );
              })()}
            </div>
          </div>
        ) : (
          pastTournaments.length===0 ? (
            <div className="text-center opacity-70">{t('noData')}</div>
          ) : (
            <div className="space-y-4">
              {pastTournaments.map(tn=> (
                <div key={tn.id} className="p-4 rounded-xl glass text-left" onClick={()=>openPast(tn.id)}>
                  <div className="text-base font-semibold mb-1">{tn.name}</div>
                  <div className="text-xs opacity-70">{new Date(tn.startTime*1000).toLocaleString()} — {new Date(tn.endTime*1000).toLocaleString()}</div>
                  <div className="text-sm opacity-80">{t('fishLabel')} {tournamentFish(tn.fish, tn.fishRarity)}</div>
                  <div className="text-sm opacity-80">{t('locationLabel')} {tn.location || t('anyLocation')}</div>
                  <div className="text-sm opacity-80">{t('conditionsLabel')} {t(tn.metric)}</div>
                  <div className="text-sm opacity-80">{t('prizePlacesLabel')} {tn.prizePlaces}</div>
                </div>
              ))}
            </div>
          )
        )
      )}
    </div>
  );
}
