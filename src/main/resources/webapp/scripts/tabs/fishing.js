const ACTION_HEIGHT_CLASS = "min-h-[72px] md:min-h-[76px]";
const BOBBER_SIZE = 24;
const BOBBER_RADIUS = BOBBER_SIZE / 2;
const BOBBER_VISIBLE_ABOVE_WATER = Math.round(BOBBER_RADIUS * 0.75);
const easeInOutCubic = t => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
const AssetImage = window.AssetImage;
const useAssetSrc = window.useAssetSrc;

function TapChallengeButton({ count, goal, timeLeft, onTap, className = '' }) {
  const timeLabel = Math.max(0, timeLeft).toFixed(1);
  const ariaLabel = `${t('tapPrompt', goal)} ${t('tapCount', { count, goal })}. ${t('tapCountdown', timeLabel)}`;
  return (
    <div className={`flex flex-col items-center ${className}`}>
      <div className="relative w-full">
        <button
          onClick={onTap}
          className={`btn-lg ${ACTION_HEIGHT_CLASS} w-full bg-red-600 hover:bg-red-500 font-semibold shadow-lg relative overflow-hidden flex items-center justify-center`}
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

function FishingStage({ me, setMe, casting, biting, tapping, tapCount, tapGoal, tapTimeLeft, castReady, onCast, onHook, onTap, autoCast, setAutoCast, autoCastRef, autoCastTimeoutRef, result, hasCatchAnimationBeenShown, markCatchAnimationShown }) {
  const stageRef = React.useRef(null);
  const { w, h } = useResizeObserver(stageRef);
  const bobberIcon = window.BOBBER_ICON || '/app/assets/menu/bobber.png';

  const WATER_TOP_REL = 0.48;
  const shorePosRel = React.useMemo(() => ({ x: 0.09, y: WATER_TOP_REL - 0.03 }), []);
  const [floatRel, setFloatRel] = React.useState(shorePosRel);
  const [floatVisual, setFloatVisual] = React.useState({ offset: 0, tilt: 0, submerge: 0 });
  const floatPxRef = React.useRef({ x: 0, y: 0 });
  const floatRelRef = React.useRef(floatRel);
  const [castLanded, setCastLanded] = React.useState(false);
  const tweenCancelRef = React.useRef(null);
  const prevCastingRef = React.useRef(false);

  React.useEffect(() => { floatRelRef.current = floatRel; }, [floatRel.x, floatRel.y]);

  React.useEffect(() => {
    if (!Number.isFinite(floatRel.x) || !Number.isFinite(floatRel.y)) {
      floatRelRef.current = shorePosRel;
      setFloatRel(shorePosRel);
    }
  }, [floatRel.x, floatRel.y, shorePosRel]);

  const toPx = (rel) => ({ x: rel.x * w, y: rel.y * h });
  const floatBasePx = toPx(floatRel);
  const floatPx = React.useMemo(() => ({
    x: floatBasePx.x,
    y: floatBasePx.y + floatVisual.offset
  }), [floatBasePx.x, floatBasePx.y, floatVisual.offset]);
  React.useEffect(() => { floatPxRef.current = floatPx; }, [floatPx.x, floatPx.y]);

  const waterlineY = React.useMemo(() => {
    const value = floatBasePx.y - BOBBER_RADIUS + BOBBER_VISIBLE_ABOVE_WATER;
    return Math.max(0, Math.min(h, value));
  }, [floatBasePx.y, h]);
  const isCastInWater = (casting && castLanded) || biting || tapping;
  const bobberBottom = floatPx.y + BOBBER_RADIUS;
  const bobberHiddenHeight = Math.max(0, Math.min(BOBBER_SIZE, bobberBottom - waterlineY));
  const bobberClipPath = isCastInWater && bobberHiddenHeight > 0.01
    ? `inset(0 0 ${bobberHiddenHeight}px 0 round ${BOBBER_RADIUS}px)`
    : null;
  const lineAttach = React.useMemo(() => ({
    x: floatPx.x,
    y: floatPx.y
  }), [floatPx.x, floatPx.y]);
  const lineClipId = React.useMemo(() => `line-clip-${Math.random().toString(36).slice(2, 9)}`, []);
  const lineClipHeight = Math.max(0, Math.min(h, waterlineY));
  const shouldClipLine = isCastInWater && lineClipHeight > 0 && w > 0;
  const bobberClipStyle = React.useMemo(() => {
    if (!bobberClipPath) return null;
    return {
      clipPath: bobberClipPath,
      WebkitClipPath: bobberClipPath
    };
  }, [bobberClipPath]);

  const shouldAnimateFloat = (casting && castLanded) || biting || tapping;
  React.useEffect(() => {
    let frame;
    if (!shouldAnimateFloat) {
      setFloatVisual(prev => {
        if (prev.offset === 0 && prev.tilt === 0 && prev.submerge === 0) {
          return prev;
        }
        return { offset: 0, tilt: 0, submerge: 0 };
      });
      return;
    }
    let start = null;
    const animate = (now) => {
      if (start === null) { start = now; }
      const t = (now - start) / 1000;
      const state = tapping ? 'tapping' : (biting ? 'biting' : 'idle');
      const basePeriod = state === 'biting' ? 0.8 : (state === 'tapping' ? 0.65 : 3);
      const mainWave = Math.sin((t * Math.PI * 2) / basePeriod);
      let offset;
      let tilt;
      let submerge;
      if (state === 'biting') {
        const extraWave = Math.sin((t * Math.PI * 2) / (basePeriod * 0.75));
        offset = mainWave * 6.5 + extraWave * 1.8;
        tilt = Math.sin((t * Math.PI * 2) / (basePeriod * 0.9)) * 6.5;
        submerge = offset > 0 ? Math.min(1, offset / 11) : 0;
      } else if (state === 'tapping') {
        const quickWave = Math.sin((t * Math.PI * 2) / (basePeriod * 0.85));
        offset = mainWave * 4.2 + quickWave * 1.1;
        tilt = Math.sin((t * Math.PI * 2) / (basePeriod * 0.95)) * 5;
        submerge = offset > 0 ? Math.min(1, offset / 9) : 0;
      } else {
        offset = mainWave * 4;
        tilt = mainWave * 2.5;
        submerge = offset > 0 ? Math.min(1, offset / 6) : 0;
      }
      const nextState = { offset, tilt, submerge };
      setFloatVisual(prev => {
        const lerp = (prevValue, targetValue) => prevValue + (targetValue - prevValue) * 0.18;
        const smoothed = {
          offset: lerp(prev.offset, nextState.offset),
          tilt: lerp(prev.tilt, nextState.tilt),
          submerge: lerp(prev.submerge, nextState.submerge)
        };
        if (
          Math.abs(smoothed.offset - prev.offset) < 0.01 &&
          Math.abs(smoothed.tilt - prev.tilt) < 0.01 &&
          Math.abs(smoothed.submerge - prev.submerge) < 0.01
        ) {
          return prev;
        }
        return smoothed;
      });
      frame = requestAnimationFrame(animate);
    };
    frame = requestAnimationFrame(animate);
    return () => {
      if (frame) { cancelAnimationFrame(frame); }
    };
  }, [shouldAnimateFloat, biting, tapping]);

  const isSmall = w < 420;
  const isTablet = w >= 420 && w < 1024;

  const targetWFrac = isSmall ? 0.70 : (isTablet ? 0.55 : 0.48);
  const targetHFrac = isSmall ? 0.92 : (isTablet ? 0.90 : 0.86);

  const stageHeight = React.useMemo(() => {
    if (isSmall) {
      return 'clamp(220px, calc(var(--vh) * 0.48), 340px)';
    }
    if (isTablet) {
      return 'clamp(320px, calc(var(--vh) * 0.54), 420px)';
    }
    return 'clamp(360px, calc(var(--vh) * 0.56), 540px)';
  }, [isSmall, isTablet]);

  const currentRod = (me.rods || []).find(r => r.id === me.currentRodId);
  const rodImage = (window.ROD_IMAGES && currentRod?.code && window.ROD_IMAGES[currentRod.code])
    || (window.ROD_IMAGES && window.ROD_IMAGES.default)
    || ROD_IMG;
  const rodTipAnchor = (window.ROD_TIP_ANCHORS && currentRod?.code && window.ROD_TIP_ANCHORS[currentRod.code])
    || (window.ROD_TIP_ANCHORS && window.ROD_TIP_ANCHORS.default)
    || ROD_TIP_ANCHOR;
  const rodLinePoints = (window.ROD_LINE_POINTS && currentRod?.code && window.ROD_LINE_POINTS[currentRod.code])
    || (window.ROD_LINE_POINTS && window.ROD_LINE_POINTS.default)
    || [];
  const rodBaseWidth = (ROD_IMG_SIZE && ROD_IMG_SIZE.width) || 1200;
  const rodBaseHeight = (ROD_IMG_SIZE && ROD_IMG_SIZE.height) || 1200;
  const rodScaleBase = Math.min((w * targetWFrac) / rodBaseWidth, (h * targetHFrac) / rodBaseHeight);
  const rodScale = rodScaleBase * ROD_SIZE_MULT;

  const rodW = rodBaseWidth * rodScale;
  const rodH = rodBaseHeight * rodScale;

  const baseDesiredX = w * ROD_BASE_X_FRACTION;
  const rodLeft = baseDesiredX - rodW * ROD_BASE_ANCHOR.x;

  const rodBottomOvershoot = Math.min(Math.max(h * 0.08, 50), 140);
  const rodTop = h - rodH + rodBottomOvershoot;

  const tipX = rodLeft + rodW * rodTipAnchor.x;
  const tipY = rodTop + rodH * rodTipAnchor.y;

  const shouldShowSlack = !casting && !biting && !tapping;
  const { rodLinePath, waterLinePath } = React.useMemo(() => {
    const rodPointsPx = rodLinePoints.map(p => ({
      x: rodLeft + rodW * p.x,
      y: rodTop + rodH * p.y
    }));

    let rPath = '';
    let lastPoint = { x: tipX, y: tipY };

    if (rodPointsPx.length > 0) {
      rPath = `M ${rodPointsPx[0].x},${rodPointsPx[0].y}`;
      for (let i = 1; i < rodPointsPx.length; i++) {
        rPath += ` L ${rodPointsPx[i].x},${rodPointsPx[i].y}`;
      }
      lastPoint = rodPointsPx[rodPointsPx.length - 1];
    } else {
      // If no points, we don't draw a rod line, just start water line from tip
      lastPoint = { x: tipX, y: tipY };
    }

    let wPath = '';
    if (!Number.isFinite(lastPoint.x) || !Number.isFinite(lastPoint.y) || !Number.isFinite(lineAttach.x) || !Number.isFinite(lineAttach.y)) {
      wPath = `M ${lastPoint.x},${lastPoint.y} L ${lineAttach.x},${lineAttach.y}`;
      return { rodLinePath: rPath, waterLinePath: wPath };
    }

    const dx = lineAttach.x - lastPoint.x;
    const dy = lineAttach.y - lastPoint.y;
    const dist = Math.hypot(dx, dy);

    if (shouldShowSlack) {
      const sag = Math.min(h * 0.22, Math.max(16, dist * 0.55));
      const baseMidY = lastPoint.y + dy * 0.5;
      const control1 = {
        x: lastPoint.x + dx * 0.35,
        y: baseMidY + sag * 0.45
      };
      const control2 = {
        x: lastPoint.x + dx * 0.75,
        y: baseMidY + sag
      };
      wPath = `M ${lastPoint.x},${lastPoint.y} C ${control1.x},${control1.y} ${control2.x},${control2.y} ${lineAttach.x},${lineAttach.y}`;
    } else {
      const gentleSag = Math.min(h * 0.08, dist * 0.12);
      const controlX = lastPoint.x + dx * 0.5;
      const controlY = lastPoint.y + dy * 0.5 + gentleSag;
      wPath = `M ${lastPoint.x},${lastPoint.y} Q ${controlX},${controlY} ${lineAttach.x},${lineAttach.y}`;
    }
    return { rodLinePath: rPath, waterLinePath: wPath };
  }, [tipX, tipY, lineAttach.x, lineAttach.y, shouldShowSlack, h, rodLinePoints, rodLeft, rodW, rodTop, rodH]);

  const catchTargetPx = React.useMemo(() => {
    const baseX = rodLeft + rodW * ROD_BASE_ANCHOR.x;
    const baseY = rodTop + rodH * ROD_BASE_ANCHOR.y;
    const targetX = baseX - rodW * (isSmall ? 0.22 : 0.16);
    const targetY = baseY - rodH * (isSmall ? 0.26 : 0.20);
    return {
      x: Math.min(w - 32, Math.max(32, targetX)),
      y: Math.min(h - 32, Math.max(32, targetY))
    };
  }, [rodLeft, rodTop, rodW, rodH, isSmall, w, h]);

  const [catchAnim, setCatchAnim] = React.useState(null);
  const catchAnimRef = React.useRef(null);
  const catchAnimFrameRef = React.useRef(null);
  const catchAnimTimeoutRef = React.useRef(null);

  React.useEffect(() => {
    return () => {
      if (catchAnimFrameRef.current) { cancelAnimationFrame(catchAnimFrameRef.current); }
      if (catchAnimTimeoutRef.current) { clearTimeout(catchAnimTimeoutRef.current); }
      catchAnimRef.current = null;
    };
  }, []);

  React.useEffect(() => {
    if (catchAnimFrameRef.current) { cancelAnimationFrame(catchAnimFrameRef.current); catchAnimFrameRef.current = null; }
    if (catchAnimTimeoutRef.current) { clearTimeout(catchAnimTimeoutRef.current); catchAnimTimeoutRef.current = null; }

    const alreadyShown = result?.animationId != null && typeof hasCatchAnimationBeenShown === 'function'
      ? hasCatchAnimationBeenShown(result.animationId)
      : false;

    if (!result || alreadyShown) {
      catchAnimRef.current = null;
      setCatchAnim(null);
      return;
    }

    const start = { ...floatPxRef.current };
    const end = { ...catchTargetPx };
    const animState = {
      fish: result.fish,
      rarity: result.rarity,
      start,
      end,
      progress: 0
    };
    catchAnimRef.current = animState;
    setCatchAnim(animState);
    if (result.animationId != null && typeof markCatchAnimationShown === 'function') {
      markCatchAnimationShown(result.animationId);
    }

    const duration = 900;
    const startedAt = performance.now();
    const step = (now) => {
      if (catchAnimRef.current !== animState) return;
      const tVal = Math.min(1, (now - startedAt) / duration);
      const eased = easeOutCubic(tVal);
      setCatchAnim(prev => {
        if (!prev || catchAnimRef.current !== animState) return prev;
        return { ...prev, progress: eased };
      });
      if (tVal < 1) {
        catchAnimFrameRef.current = requestAnimationFrame(step);
      } else {
        catchAnimTimeoutRef.current = setTimeout(() => {
          if (catchAnimRef.current === animState) {
            catchAnimRef.current = null;
            setCatchAnim(null);
          }
        }, 350);
      }
    };
    catchAnimFrameRef.current = requestAnimationFrame(step);

    return () => {
      if (catchAnimRef.current === animState) {
        catchAnimRef.current = null;
      }
    };
  }, [result, catchTargetPx, hasCatchAnimationBeenShown, markCatchAnimationShown]);

  const tweenTo = React.useCallback((toRel, ms = 600, options = {}) => {
    const from = {
      x: Number.isFinite(floatRelRef.current.x) ? floatRelRef.current.x : shorePosRel.x,
      y: Number.isFinite(floatRelRef.current.y) ? floatRelRef.current.y : shorePosRel.y
    };
    const { arcHeight = 0, onComplete } = options;
    let frame = null;
    let cancelled = false;
    const start = performance.now();
    const step = (now) => {
      if (cancelled) return;
      const tVal = Math.min(1, (now - start) / ms);
      const eased = easeInOutCubic(tVal);
      const arc = arcHeight > 0 ? Math.sin(tVal * Math.PI) * arcHeight : 0;
      const nextRel = {
        x: from.x + (toRel.x - from.x) * eased,
        y: from.y + (toRel.y - from.y) * eased - arc
      };
      floatRelRef.current = nextRel;
      setFloatRel(nextRel);
      if (tVal < 1) {
        frame = requestAnimationFrame(step);
      } else {
        frame = null;
        if (typeof onComplete === 'function') {
          onComplete();
        }
      }
    };
    frame = requestAnimationFrame(step);
    return () => {
      cancelled = true;
      if (frame) {
        cancelAnimationFrame(frame);
      }
    };
  }, [shorePosRel.x, shorePosRel.y]);

  React.useEffect(() => {
    return () => {
      if (tweenCancelRef.current) {
        tweenCancelRef.current();
        tweenCancelRef.current = null;
      }
    };
  }, []);

  React.useEffect(() => {
    if (casting) {
      if (!prevCastingRef.current) {
        setCastLanded(false);
        if (tweenCancelRef.current) {
          tweenCancelRef.current();
          tweenCancelRef.current = null;
        }
        if (!(w > 0 && Number.isFinite(tipX))) {
          prevCastingRef.current = false;
          return;
        }
        const tipRelX = tipX / w;
        if (!Number.isFinite(tipRelX)) {
          prevCastingRef.current = false;
          return;
        }
        const minX = Math.max(0.05, tipRelX - 0.20);
        const maxX = Math.max(minX, tipRelX - 0.05);
        const target = {
          x: minX + Math.random() * (maxX - minX),
          y: WATER_TOP_REL + 0.12 + Math.random() * 0.18,
        };
        const startRel = floatRelRef.current;
        const relDistanceY = Math.abs((target?.y ?? startRel.y) - startRel.y);
        const arcHeight = Math.max(0.015, Math.min(0.08, relDistanceY * 0.75));
        tweenCancelRef.current = tweenTo(target, 750, {
          arcHeight,
          onComplete: () => {
            tweenCancelRef.current = null;
            setCastLanded(true);
          }
        });
        prevCastingRef.current = true;
      }
    } else {
      if (tweenCancelRef.current) {
        tweenCancelRef.current();
        tweenCancelRef.current = null;
      }
      setCastLanded(false);
      floatRelRef.current = shorePosRel;
      setFloatRel(shorePosRel);
      prevCastingRef.current = false;
    }
  }, [casting, shorePosRel, tipX, tweenTo, w]);

  const resolveLocationBg = React.useCallback((id, name) => {
    if (typeof window.getLocationBackground === 'function') {
      const bg = window.getLocationBackground(id, name);
      if (bg) return bg;
    }
    const normalized = typeof name === 'string' ? name.trim().toLowerCase() : '';
    if (normalized && window.LOCATION_BG_BY_NAME && window.LOCATION_BG_BY_NAME[normalized]) {
      return window.LOCATION_BG_BY_NAME[normalized];
    }
    const numId = Number(id);
    if (Number.isFinite(numId) && window.LOCATION_BG && window.LOCATION_BG[numId]) {
      return window.LOCATION_BG[numId];
    }
    return null;
  }, []);

  const currentLocation = React.useMemo(() => {
    if (!Array.isArray(me?.locations)) return null;
    const targetId = Number(me?.locationId);
    return me.locations.find(loc => Number(loc.id) === targetId) || null;
  }, [me?.locations, me?.locationId]);

  const bgAsset = React.useMemo(() => {
    const byCurrent = resolveLocationBg(currentLocation?.id ?? me.locationId, currentLocation?.name);
    if (byCurrent) return byCurrent;
    const byIdOnly = resolveLocationBg(me.locationId, null);
    if (byIdOnly) return byIdOnly;
    return LOCATION_BG[1];
  }, [resolveLocationBg, currentLocation?.id, currentLocation?.name, me.locationId]);
  const bgUrl = useAssetSrc(bgAsset);
  const [bgLoaded, setBgLoaded] = React.useState(false);
  React.useEffect(() => {
    setBgLoaded(false);
    if (!bgUrl) return;
    const img = new Image();
    img.src = bgUrl;
    img.onload = () => setBgLoaded(true);
    return () => { img.onload = null; };
  }, [bgUrl]);
  const showRipple = castLanded && (biting || tapping);

  const rarityGlow = {
    common: 'rgba(255,255,255,0.5)',
    uncommon: 'rgba(74,222,128,0.65)',
    rare: 'rgba(59,130,246,0.65)',
    epic: 'rgba(168,85,247,0.65)',
    legendary: 'rgba(250,204,21,0.75)'
  };
  let catchStyle = null;
  let catchGlowStyle = null;
  let catchImage = null;
  const catchAnimAsset = catchAnim ? FISH_IMG[catchAnim.fish] : null;
  const catchAnimImage = useAssetSrc(catchAnimAsset);
  if (catchAnim) {
    const img = catchAnimImage;
    if (img) {
      const { start, end, progress } = catchAnim;
      const lift = Math.sin(progress * Math.PI) * (isSmall ? 26 : 38);
      const x = start.x + (end.x - start.x) * progress;
      const y = start.y + (end.y - start.y) * progress - lift;
      const scale = 0.75 + progress * 0.3;
      const rotation = (Math.sin(progress * Math.PI) * (isSmall ? 9 : 13));
      const fadeStart = 0.8;
      const opacity = progress < fadeStart ? 1 : Math.max(0, 1 - (progress - fadeStart) / (1 - fadeStart));
      const size = isSmall ? 72 : 88;
      catchStyle = {
        transform: `translate3d(${x - size / 2}px, ${y - size / 2}px, 0) scale(${scale}) rotate(${rotation}deg)`,
        width: size,
        height: size,
        opacity,
      };
      catchGlowStyle = {
        background: `radial-gradient(circle, ${rarityGlow[catchAnim.rarity] || 'rgba(255,255,255,0.55)'} 0%, transparent 70%)`,
        opacity: 0.9,
      };
      catchImage = img;
    }
  }

  return (
    <div className="relative rounded-2xl overflow-hidden border border-white/10" ref={stageRef}
      style={{ height: stageHeight }}>
      <div
        className="absolute inset-0 transition-opacity duration-200"
        style={{ backgroundImage: bgUrl ? `url(${bgUrl})` : undefined, backgroundSize: 'cover', backgroundPosition: 'center bottom', opacity: bgLoaded ? 1 : 0 }}
      ></div>
      <div className="absolute inset-0 bg-gradient-to-b from-black/25 via-black/25 to-black/55"></div>

      <AssetImage
        src={rodImage}
        alt="rod"
        className="absolute select-none pointer-events-none"
        style={{ left: rodLeft, top: rodTop, width: rodW, height: rodH }}
        loading="eager"
        decoding="async"
      />
      <svg className="absolute inset-0" width={w} height={h} viewBox={`0 0 ${w} ${h}`}>
        {shouldClipLine && (
          <defs>
            <clipPath id={lineClipId} clipPathUnits="userSpaceOnUse">
              <rect x="0" y="0" width={Math.max(0, w)} height={lineClipHeight} />
            </clipPath>
          </defs>
        )}
        <path
          d={rodLinePath}
          stroke="rgba(255,255,255,0.3)"
          strokeWidth="1.2"
          strokeLinecap="round"
          fill="none"
        />
        <path
          d={waterLinePath}
          stroke="rgba(255,255,255,0.3)"
          strokeWidth="1.2"
          strokeLinecap="round"
          fill="none"
          clipPath={shouldClipLine ? `url(#${lineClipId})` : undefined}
        />
      </svg>

      <div
        className="absolute"
        style={{
          left: floatPx.x - BOBBER_RADIUS,
          top: floatPx.y - BOBBER_RADIUS,
          width: BOBBER_SIZE,
          height: BOBBER_SIZE
        }}
      >
        <div className="absolute inset-0">
          <AssetImage
            src={bobberIcon}
            alt="bobber"
            className="relative bobber-cast drop-shadow"
            style={{
              transform: `rotate(${floatVisual.tilt}deg)`,
              ...(bobberClipStyle || {})
            }}
          />
        </div>
        {showRipple && <div className="absolute inset-0 rounded-full ripple"></div>}
      </div>

      {catchImage && catchStyle && (
        <div className="absolute pointer-events-none" style={catchStyle} aria-hidden="true">
          <div className="absolute inset-0 blur-xl" style={catchGlowStyle}></div>
          <AssetImage src={catchImage} alt="" className="absolute inset-0 w-full h-full object-contain drop-shadow-[0_12px_16px_rgba(0,0,0,0.45)]" />
        </div>
      )}

      <div className="absolute bottom-0 left-0 right-0 p-4 pb-safe justify-center hidden md:flex">
        {!casting ? (
          castReady ? (
            <button onClick={onCast} className={`btn-lg ${ACTION_HEIGHT_CLASS} w-full md:w-1/2 bg-emerald-600 hover:bg-emerald-500 font-semibold shadow-lg`}>{t('castRod')}</button>
          ) : (
            <div className={`glass ${ACTION_HEIGHT_CLASS} w-full md:w-1/2 rounded-2xl flex flex-col items-center justify-center gap-2`}>
              <div className="h-6 w-6 rounded-full border-2 border-white/50 border-t-transparent animate-spin"></div>
              <div className="opacity-90 text-center px-2">{t('castCooldown')}</div>
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
          <button onClick={onHook} className={`btn-lg ${ACTION_HEIGHT_CLASS} w-full md:w-1/2 bg-red-600 hover:bg-red-500 font-semibold shadow-lg`}>{t('hook')}</button>
        ) : (
          <div className={`glass ${ACTION_HEIGHT_CLASS} w-full md:w-1/2 rounded-2xl flex flex-col items-center justify-center gap-2`}>
            <div className="h-6 w-6 rounded-full border-2 border-white/50 border-t-transparent animate-spin"></div>
            <div className="opacity-90 text-center px-2">{t('waitingBite')}</div>
          </div>
        )}
      </div>
      {me.autoFish && (
        <button
          type="button"
          className="absolute bottom-3 right-3 z-10 flex items-center gap-1 glass px-2 py-1 rounded-lg text-xs cursor-pointer"
          onClick={() => {
            const checked = !autoCastRef.current;
            autoCastRef.current = checked;
            if (!checked) {
              clearTimeout(autoCastTimeoutRef.current);
              autoCastTimeoutRef.current = null;
            }
            setAutoCast(checked);
          }}
        >
          <span className={"w-3 h-3 rounded-sm border border-white/50 bg-transparent flex items-center justify-center " + (autoCast ? "bg-emerald-500" : "")}>
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
  autoCast,
  setAutoCast,
  autoCastRef,
  autoCastTimeoutRef,
  hasCatchAnimationBeenShown,
  markCatchAnimationShown,
  onCatchClick,
  onOpenQuests
}) {
  const handleCast = () => {
    window.Analytics?.track('cast_attempt');
    if (onCast) onCast();
  };

  const handleHook = () => {
    window.Analytics?.track('hook_attempt');
    if (onHook) onHook();
  };

  React.useEffect(() => {
    if (result) {
      window.Analytics?.track('fish_caught', {
        fish: result.fish,
        weight: result.weight,
        rarity: result.rarity,
        location: result.location
      });
    }
  }, [result]);

  return (
    <>
      <div className="mt-3 md:mt-4">
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
          onCast={handleCast}
          onHook={handleHook}
          onTap={onTap}
          autoCast={autoCast}
          setAutoCast={setAutoCast}
          autoCastRef={autoCastRef}
          autoCastTimeoutRef={autoCastTimeoutRef}
          result={result}
          hasCatchAnimationBeenShown={hasCatchAnimationBeenShown}
          markCatchAnimationShown={markCatchAnimationShown}
        />
      </div>

      <div className="mt-3 flex justify-end">
        <button
          type="button"
          onClick={onOpenQuests}
          className="px-4 py-2 rounded-xl glass border border-white/10 text-sm"
        >
          {t('quests')}
        </button>
      </div>

      <div className="md:hidden mt-3">
        {!casting ? (
          castReady ? (
            <button onClick={handleCast} className={`btn-lg ${ACTION_HEIGHT_CLASS} w-full bg-emerald-600 hover:bg-emerald-500 font-semibold shadow-lg`}>{t('castRod')}</button>
          ) : (
            <div className={`glass ${ACTION_HEIGHT_CLASS} w-full rounded-2xl flex flex-col items-center justify-center gap-2`}>
              <div className="h-6 w-6 rounded-full border-2 border-white/50 border-t-transparent animate-spin"></div>
              <div className="opacity-90 text-center px-2">{t('castCooldown')}</div>
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
          <button onClick={handleHook} className={`btn-lg ${ACTION_HEIGHT_CLASS} w-full bg-red-600 hover:bg-red-500 font-semibold shadow-lg`}>{t('hook')}</button>
        ) : (
          <div className={`glass ${ACTION_HEIGHT_CLASS} w-full rounded-2xl flex flex-col items-center justify-center gap-2`}>
            <div className="h-6 w-6 rounded-full border-2 border-white/50 border-t-transparent animate-spin"></div>
            <div className="opacity-90 text-center px-2">{t('waitingBite')}</div>
          </div>
        )}
      </div>

      {result && (
        <button
          type="button"
          onClick={() => {
            if (onCatchClick && result.id) {
              onCatchClick({
                ...result,
                userId: result.userId ?? me.id,
                user: result.user || me.username || t('you')
              });
            }
          }}
          className="mt-4 w-full text-left p-4 rounded-xl glass flex items-center gap-4"
        >
          <AssetImage src={FISH_IMG[result.fish]} alt={result.fish} className="w-16 h-16 object-contain" onError={e => { if (e?.currentTarget) e.currentTarget.style.display = 'none'; }} />
          <div>
            <div className="text-base">{t('catch')} {result.newFish && <span className="ml-1 text-yellow-300">{t('new')}</span>}</div>
            <div className="text-lg font-bold mt-1"><span className={rarityColors[result.rarity] || ''}>{result.fish}</span> — {Number(result.weight).toFixed(2)} {t('kg')}</div>
            <div className="text-xs opacity-70">{t('locationLabel')} {result.location}</div>
            {typeof result.coins === 'number' && (
              result.coins > 0
                ? <div className="text-xs text-yellow-300">{t('coinsEarned', result.coins)}</div>
                : <div className="text-xs opacity-70">{t('coinsCapReached')}</div>
            )}
            {result.newLocations && result.newLocations.map((n, i) => (
              <div key={i} className="text-xs text-emerald-400">{t('newLocation')} {n}</div>
            ))}
            {result.newRods && result.newRods.length > 0 && (
              <div className="text-xs text-emerald-400">{(result.newRods.length > 1 ? t('newRodPlural') : t('newRod'))} {result.newRods.join(', ')}</div>
            )}
            {Array.isArray(result.achievements) && result.achievements.map((a, i) => (
              <div key={`ach-${i}`} className="text-xs text-amber-300">
                {t('achievementUnlockedLine', { name: a.name || a.code, level: a.levelLabel || a.newLevelIndex })}
              </div>
            ))}
            {Array.isArray(result.questUpdates) && result.questUpdates.map((q, i) => (
              <div key={`quest-${i}`} className="text-xs text-emerald-300">
                {t('questCompletedLine', { name: q.name || q.code, coins: q.rewardCoins || 0 })}
              </div>
            ))}
          </div>
        </button>
      )}
      {error && <div className="mt-3 text-sm text-red-300">{error}</div>}

      <div className="mt-6">
        <div className="text-sm opacity-80 mb-2">{t('recentCatches')}</div>
        {(me.recent || []).length === 0 ? (
          <div className="text-sm opacity-60">{t('emptyCatches')}</div>
        ) : (
          <div className="space-y-2">
            {me.recent.map((c, i) => (
              <button
                key={i}
                type="button"
                onClick={() => {
                  if (onCatchClick && c.id) {
                    onCatchClick({
                      ...c,
                      userId: c.userId ?? me.id,
                      user: c.user || me.username || t('you')
                    });
                  }
                }}
                className="w-full p-3 rounded--xl border border-white/10 flex items-center justify-between text-left"
              >
                <div className="flex items-center gap-3">
                  <AssetImage src={FISH_IMG[c.fish]} alt={c.fish} className="w-12 h-12 object-contain" onError={e => { if (e?.currentTarget) e.currentTarget.style.display = 'none'; }} />
                  <div>
                    <div className={`font-medium ${rarityColors[c.rarity] || ''}`}>{c.fish}</div>
                    <div className="text-xs opacity-70">{c.location} — {new Date(c.at).toLocaleString()}</div>
                  </div>
                </div>
                <div className="text-emerald-300 font-semibold">{Number(c.weight).toFixed(2)} {t('kg')}</div>
              </button>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
