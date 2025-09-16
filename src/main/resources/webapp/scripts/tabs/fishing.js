function TapChallengeButton({count, goal, timeLeft, onTap, className=''}){
  const timeLabel = Math.max(0, timeLeft).toFixed(1);
  const ariaLabel = `${t('tapPrompt', goal)} ${t('tapCount', {count, goal})}. ${t('tapCountdown', timeLabel)}`;
  return (
    <div className={`flex flex-col items-center ${className}`}>
      <div className="relative w-full">
        <button
          onClick={onTap}
          className="btn-lg w-full bg-red-600 hover:bg-red-500 font-semibold shadow-lg relative overflow-hidden flex items-center justify-center"
          aria-label={ariaLabel}
        >
          <span className="sr-only">{ariaLabel}</span>
          <span
            className="absolute inset-0 flex items-center justify-center text-2xl font-extrabold tracking-wider uppercase drop-shadow-[0_2px_4px_rgba(0,0,0,0.45)]"
            aria-hidden="true"
          >
            {t('tapButton')}
          </span>
          <div className="tap-visual" aria-hidden="true">
            <span className="tap-visual__ring"></span>
            <span className="tap-visual__ring tap-visual__ring--delay1"></span>
            <span className="tap-visual__ring tap-visual__ring--delay2"></span>
            <span className="tap-visual__dot"></span>
          </div>
        </button>
        <div className="tap-hint"></div>
      </div>
    </div>
  );
}

function FishingStage({me, setMe, casting, biting, tapping, tapCount, tapGoal, tapTimeLeft, castReady, onCast, onHook, onTap, onClaimDaily, autoCast, setAutoCast, autoCastRef, autoCastTimeoutRef}){
  const stageRef = React.useRef(null);
  const {w,h} = useResizeObserver(stageRef);

  const WATER_TOP_REL = 0.48;
  const shorePosRel = React.useMemo(()=>({x: 0.12, y: WATER_TOP_REL - 0.02}),[]);
  const [floatRel, setFloatRel] = React.useState(shorePosRel);
  const [tension, setTension] = React.useState(0.2);

  const toPx = (rel)=>({ x: rel.x * w, y: rel.y * h });
  const floatPx = toPx(floatRel);

  const isSmall = w < 420;
  const isTablet = w >= 420 && w < 1024;

  const targetWFrac = isSmall ? 0.70 : (isTablet ? 0.55 : 0.48);
  const targetHFrac = isSmall ? 0.92 : (isTablet ? 0.90 : 0.86);

  const rodScaleBase = Math.min((w * targetWFrac) / 1200, (h * targetHFrac) / 1200);
  const rodScale = rodScaleBase * ROD_SIZE_MULT;

  const rodW = 1200 * rodScale;
  const rodH = 1200 * rodScale;

  const baseDesiredX = w * ROD_BASE_X_FRACTION;
  const rodLeft = baseDesiredX - rodW * ROD_BASE_ANCHOR.x;

  const rodBottomOvershoot = Math.min( Math.max(h * 0.08, 50), 140 );
  const rodTop = h - rodH + rodBottomOvershoot;

  const tipX = rodLeft + rodW * ROD_TIP_ANCHOR.x;
  const tipY = rodTop  + rodH * ROD_TIP_ANCHOR.y;

  const ctrl = {
    x: (tipX + floatPx.x) / 2 - (isSmall ? 24 : 40),
    y: (tipY + floatPx.y) / 2 + (isSmall ? 50 : (60 + 120 * tension))
  };
  const linePath = `M ${tipX},${tipY} Q ${ctrl.x},${ctrl.y} ${floatPx.x},${floatPx.y}`;

  const tweenTo = (toRel, ms=600)=>{
    const from = {...floatRel};
    const start = performance.now();
    const step = (now)=>{
      const tVal = Math.min(1, (now - start)/ms);
      const k = easeOutCubic(tVal);
      setFloatRel({ x: from.x + (toRel.x - from.x)*k, y: from.y + (toRel.y - from.y)*k });
      if(tVal<1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
  };

  React.useEffect(()=>{
    if(casting){
      const tipRelX = tipX / w;
      const minX = Math.max(0.05, tipRelX - 0.20);
      const maxX = Math.max(minX, tipRelX - 0.05);
      const target = {
        x: minX + Math.random() * (maxX - minX),
        y: WATER_TOP_REL + 0.12 + Math.random() * 0.18,
      };
      setTension(0.15 + Math.random() * 0.25);
      tweenTo(target, 650);
    } else {
      setFloatRel(shorePosRel);
      setTension(0.18);
    }
  }, [casting, shorePosRel, tipX, w]);

  const bgUrl = LOCATION_BG[me.locationId] || LOCATION_BG[1];
  const [bgLoaded, setBgLoaded] = React.useState(false);
  React.useEffect(() => {
    setBgLoaded(false);
    const img = new Image();
    img.src = bgUrl;
    img.onload = () => setBgLoaded(true);
  }, [bgUrl]);
  const bobberAnim = biting ? 'animate-[bite_0.6s_ease-in-out_infinite]' : (casting ? 'animate-[bob_3s_ease-in-out_infinite]' : '');
  const showRipple = casting && !biting;

  return (
    <div className="relative rounded-2xl overflow-hidden border border-white/10" ref={stageRef}
         style={{height:'calc(var(--vh) * 0.56)'}}>
      <div
        className="absolute inset-0 transition-opacity duration-200"
        style={{backgroundImage:`url(${bgUrl})`, backgroundSize:'cover', backgroundPosition:'center', opacity:bgLoaded?1:0}}
      ></div>
      <div className="absolute inset-0 bg-gradient-to-b from-black/25 via-black/25 to-black/55"></div>

      {me.dailyAvailable && (
        <button
          onClick={onClaimDaily}
          className="absolute top-3 right-3 z-10 px-3 py-1 rounded-lg glass flex items-center gap-1 text-sm transform origin-top-right scale-[1.33]"
        >
          <span>🎁</span>
          <span>{t('gift')}</span>
        </button>
      )}

      <img
        src={ROD_IMG}
        alt="rod"
        className="absolute select-none pointer-events-none"
        style={{ left: rodLeft, top: rodTop, width: rodW, height: rodH }}
        loading="eager"
        decoding="async"
      />
      <svg className="absolute inset-0" width={w} height={h} viewBox={`0 0 ${w} ${h}`}>
        <path d={linePath} stroke="#e6e6e6" strokeWidth="2" fill="none" opacity="0.95"/>
      </svg>

      <div className="absolute" style={{left: floatPx.x-12, top: floatPx.y-12, width: 24, height: 24}}>
        <img src="/app/assets/riverking_bobber.svg" alt="bobber" className={`relative w-6 h-6 drop-shadow ${bobberAnim}`}/>
        {showRipple && <div className="absolute inset-0 rounded-full ripple"></div>}
      </div>

      <div className="absolute bottom-0 left-0 right-0 p-4 pb-safe justify-center hidden md:flex">
        {!casting ? (
          castReady ? (
            <button onClick={onCast} className="btn-lg w-full md:w-1/2 bg-emerald-600 hover:bg-emerald-500 font-semibold shadow-lg">{t('castRod')}</button>
          ) : (
            <div className="glass w-full md:w-1/2 py-4 rounded-2xl text-center">
              <div className="mx-auto h-6 w-6 rounded-full border-2 border-white/50 border-t-transparent animate-spin mb-2"></div>
              <div className="opacity-90">{t('castCooldown')}</div>
            </div>
          )
        ) : tapping ? (
          <TapChallengeButton
            count={tapCount}
            goal={tapGoal}
            timeLeft={tapTimeLeft}
            onTap={onTap}
            className="w-full md:w-1/2"
          />
        ) : biting ? (
          <button onClick={onHook} className="btn-lg w-full md:w-1/2 bg-red-600 hover:bg-red-500 font-semibold shadow-lg">{t('hook')}</button>
        ) : (
          <div className="glass w-full md:w-1/2 py-4 rounded-2xl text-center">
            <div className="mx-auto h-6 w-6 rounded-full border-2 border-white/50 border-t-transparent animate-spin mb-2"></div>
            <div className="opacity-90">{t('waitingBite')}</div>
          </div>
        )}
      </div>
      {me.autoFish && (
        <button
          type="button"
          className="absolute bottom-3 right-3 z-10 flex items-center gap-1 glass px-2 py-1 rounded-lg text-xs cursor-pointer"
          onClick={()=>{
            const checked = !autoCastRef.current;
            autoCastRef.current = checked;
            if(!checked){
              clearTimeout(autoCastTimeoutRef.current);
              autoCastTimeoutRef.current = null;
            }
            setAutoCast(checked);
          }}
        >
          <span className={"w-3 h-3 rounded-sm border border-white/50 bg-transparent flex items-center justify-center " + (autoCast ? "bg-emerald-500" : "") }>
            <svg viewBox="0 0 14 14" className={"w-2 h-2 text-black " + (autoCast ? "opacity-100" : "opacity-0")} fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="3 7 6 10 11 4" />
            </svg>
          </span>
          <span>{t('autoCast')}</span>
        </button>
      )}
    </div>
  );
}

function FishingTab({
  me,
  setMe,
  casting,
  biting,
  tapActive,
  tapCount,
  tapTimeLeft,
  castReady,
  onCast,
  onHook,
  onTap,
  result,
  error,
  onClaimDaily,
  autoCast,
  setAutoCast,
  autoCastRef,
  autoCastTimeoutRef
}){
  return (
    <>
      <div className="mt-4">
        <FishingStage
          me={me}
          setMe={setMe}
          casting={casting}
          biting={biting}
          tapping={tapActive}
          tapCount={tapCount}
          tapGoal={TAP_CHALLENGE_GOAL}
          tapTimeLeft={tapTimeLeft}
          castReady={castReady}
          onCast={onCast}
          onHook={onHook}
          onTap={onTap}
          onClaimDaily={onClaimDaily}
          autoCast={autoCast}
          setAutoCast={setAutoCast}
          autoCastRef={autoCastRef}
          autoCastTimeoutRef={autoCastTimeoutRef}
        />
      </div>

      <div className="md:hidden mt-3">
        {!casting ? (
          castReady ? (
            <button onClick={onCast} className="btn-lg w-full bg-emerald-600 hover:bg-emerald-500 font-semibold shadow-lg">{t('castRod')}</button>
          ) : (
            <div className="glass w-full py-4 rounded-2xl text-center">
              <div className="mx-auto h-6 w-6 rounded-full border-2 border-white/50 border-t-transparent animate-spin mb-2"></div>
              <div className="opacity-90">{t('castCooldown')}</div>
            </div>
          )
        ) : tapActive ? (
          <TapChallengeButton
            count={tapCount}
            goal={TAP_CHALLENGE_GOAL}
            timeLeft={tapTimeLeft}
            onTap={onTap}
            className="w-full"
          />
        ) : biting ? (
          <button onClick={onHook} className="btn-lg w-full bg-red-600 hover:bg-red-500 font-semibold shadow-lg">{t('hook')}</button>
        ) : (
          <div className="glass w-full py-4 rounded-2xl text-center">
            <div className="mx-auto h-6 w-6 rounded-full border-2 border-white/50 border-t-transparent animate-spin mb-2"></div>
            <div className="opacity-90">{t('waitingBite')}</div>
          </div>
        )}
      </div>

      {result && (
        <div className="mt-4 p-4 rounded-xl glass flex items-center gap-4">
          <img src={FISH_IMG[result.fish]} alt={result.fish} className="w-16 h-16 object-contain" onError={e=>e.currentTarget.style.display='none'} />
          <div>
            <div className="text-base">{t('catch')} {result.newFish && <span className="ml-1 text-yellow-300">{t('new')}</span>}</div>
            <div className="text-lg font-bold mt-1"><span className={rarityColors[result.rarity]||''}>{result.fish}</span> — {Number(result.weight).toFixed(2)} {t('kg')}</div>
            <div className="text-xs opacity-70">{t('locationLabel')} {result.location}</div>
            {result.newLocations && result.newLocations.map((n,i)=>(
              <div key={i} className="text-xs text-emerald-400">{t('newLocation')} {n}</div>
            ))}
          </div>
        </div>
      )}
      {error && <div className="mt-3 text-sm text-red-300">{error}</div>}

      <div className="mt-6">
        <div className="text-sm opacity-80 mb-2">{t('recentCatches')}</div>
        {(me.recent||[]).length===0 ? (
          <div className="text-sm opacity-60">{t('emptyCatches')}</div>
        ) : (
          <div className="space-y-2">
            {me.recent.map((c,i)=> (
              <div key={i} className="p-3 rounded--xl border border-white/10 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <img src={FISH_IMG[c.fish]} alt={c.fish} className="w-12 h-12 object-contain" onError={e=>e.currentTarget.style.display='none'} />
                  <div>
                    <div className={`font-medium ${rarityColors[c.rarity]||''}`}>{c.fish}</div>
                    <div className="text-xs opacity-70">{c.location} — {new Date(c.at).toLocaleString()}</div>
                  </div>
                </div>
                <div className="text-emerald-300 font-semibold">{Number(c.weight).toFixed(2)} {t('kg')}</div>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
