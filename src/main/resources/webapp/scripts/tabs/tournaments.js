function tournamentFish(name, rarity){
  if(!rarity) return t('anyFish');
  const cls = rarityColors[rarity]||'';
  if(name) return <span className={cls}>{name}</span>;
  const lang = document.documentElement.lang;
  const rn = rarityNames[lang] || rarityNames.ru;
  return <span className={cls}>{(rn[rarity]||rarity)} {t('fishWord')}</span>;
}

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
  shop
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

  const renderPrizeHint = (rank, prize) => (
    prizeHint?.rank===rank && (
      <div className="absolute right-0 top-full mt-1 text-xs bg-gray-800 border border-white/10 p-2 whitespace-nowrap z-10 rounded-lg text-amber-300">
        {(shop.reduce((acc,c)=>acc.concat(c.packs),[]).find(p=>p.id===prize.packageId)?.name)||prize.packageId} x{prize.qty}
      </div>
    )
  );

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
                {(currentTournament.leaderboard || []).map(e=> {
                  const isMine = currentTournament.mine && e.rank===currentTournament.mine.rank;
                  return (
                  <div
                    key={e.rank}
                    id={isMine ? 'current-tournament-mine' : undefined}
                    className={`p-3 rounded-xl glass flex items-center justify-between relative ${prizeHint?.rank===e.rank ? 'z-10' : ''} ${isMine ? 'border border-emerald-400' : ''}`}
                    onClick={ev=>{ev.stopPropagation(); if(e.prize) setPrizeHint(p=>p && p.rank===e.rank ? null : {rank:e.rank, prize:e.prize});}}
                  >
                    <div className="flex items-center gap-2 w-full min-w-0">
                      <div className="w-12 h-8 flex items-center justify-center gap-1 shrink-0">
                        <span>{e.rank}</span>
                        {e.prize && <img src={`/app/assets/baits/${e.prize.packageId}.png`} alt="" className="w-6 h-6 object-contain" onError={ev=>ev.currentTarget.style.display='none'} />}
                      </div>
                      {currentTournament.tournament.metric==='count' ? (
                        <div className="w-8 h-8"></div>
                      ) : e.fish ? (
                        (me.caughtFishIds||[]).includes(e.fishId) ? (
                          <img src={FISH_IMG[e.fish]} alt={e.fish} className="w-8 h-8 object-contain shrink-0" onError={ev=>ev.currentTarget.style.display='none'} />
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
                        <div className="flex items-center gap-1 flex-wrap min-w-0">
                          <bdi className="truncate">{e.user||'-'}</bdi>
                        </div>
                        {currentTournament.tournament.metric!=='count' && (
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
              {(pastResult.leaderboard || []).map(e=> {
                const isMine = pastResult.mine && e.rank===pastResult.mine.rank;
                return (
                <div
                  key={e.rank}
                  id={isMine ? 'past-tournament-mine' : undefined}
                  className={`p-3 rounded-xl glass flex items-center justify-between relative ${prizeHint?.rank===e.rank ? 'z-10' : ''} ${isMine ? 'border border-emerald-400' : ''}`}
                  onClick={ev=>{ev.stopPropagation(); if(e.prize) setPrizeHint(p=>p && p.rank===e.rank ? null : {rank:e.rank, prize:e.prize});}}
                >
                  <div className="flex items-center gap-2 w-full min-w-0">
                    <div className="w-12 h-8 flex items-center justify-center gap-1 shrink-0">
                      <span>{e.rank}</span>
                      {e.prize && <img src={`/app/assets/baits/${e.prize.packageId}.png`} alt="" className="w-6 h-6 object-contain" onError={ev=>ev.currentTarget.style.display='none'} />}
                    </div>
                    {pastResult.tournament.metric==='count' ? (
                      <div className="w-8 h-8"></div>
                    ) : e.fish ? (
                      (me.caughtFishIds||[]).includes(e.fishId) ? (
                        <img src={FISH_IMG[e.fish]} alt={e.fish} className="w-8 h-8 object-contain shrink-0" onError={ev=>ev.currentTarget.style.display='none'} />
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
                      <div className="flex items-center gap-1 flex-wrap min-w-0">
                        <bdi className="truncate">{e.user||'-'}</bdi>
                      </div>
                      {pastResult.tournament.metric!=='count' && (
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
              {pastResult.mine && pastResult.mine.rank>(pastResult.leaderboard?.length || 0) && (
                <div
                  id="past-tournament-mine"
                  className={`p-3 rounded-xl glass flex items-center justify-between mt-2 relative ${prizeHint?.rank===pastResult.mine.rank ? 'z-10' : ''} border border-emerald-400`}
                  onClick={ev=>{ev.stopPropagation(); if(pastResult.mine.prize) setPrizeHint(p=>p && p.rank===pastResult.mine.rank ? null : {rank:pastResult.mine.rank, prize:pastResult.mine.prize});}}
                >
                  <div className="flex items-center gap-2 w-full min-w-0">
                    <div className="w-12 h-8 flex items-center justify-center gap-1 shrink-0">
                      <span>{pastResult.mine.rank}</span>
                      {pastResult.mine.prize && <img src={`/app/assets/baits/${pastResult.mine.prize.packageId}.png`} alt="" className="w-6 h-6 object-contain" onError={ev=>ev.currentTarget.style.display='none'} />}
                    </div>
                    {pastResult.tournament.metric==='count' ? (
                      <div className="w-8 h-8"></div>
                    ) : pastResult.mine.fish ? (
                      (me.caughtFishIds||[]).includes(pastResult.mine.fishId) ? (
                        <img src={FISH_IMG[pastResult.mine.fish]} alt={pastResult.mine.fish} className="w-8 h-8 object-contain shrink-0" onError={ev=>ev.currentTarget.style.display='none'} />
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
                      <div className="flex items-center gap-1 flex-wrap min-w-0">
                        <bdi className="truncate">{pastResult.mine.user||t('you')}</bdi>
                      </div>
                      {pastResult.tournament.metric!=='count' && (
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
              )}
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
