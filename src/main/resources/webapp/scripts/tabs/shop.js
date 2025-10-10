const AssetImage = window.AssetImage;

const getShopIcon = id => {
  if(!id) return '';
  if(String(id).startsWith('autofish')) return '/app/assets/shop/autofish.png';
  return `/app/assets/shop/${id}.png`;
};
window.getShopIcon = getShopIcon;

function ShopTab({
  shop,
  toggleRef,
  refOpen,
  refInfo,
  copyRefLink,
  generateRefLink,
  starterPackName,
  buyPack,
  dailyAvailable,
  onOpenDaily,
  onCoinPurchaseRequest
}){
  const coinLocale = (typeof document!=='undefined' && document.documentElement.lang==='en') ? 'en-US' : 'ru-RU';
  return (
    <div className="mt-6">
      {dailyAvailable && (
        <button
          type="button"
          onClick={onOpenDaily}
          className="w-full px-3 py-2 mb-3 rounded-xl glass flex items-center justify-center gap-2 text-sm"
        >
          <span>🎁</span>
          <span>{t('gift')}</span>
        </button>
      )}
      <div className="mb-4">
        <button onClick={toggleRef} className="w-full p-3 rounded-xl border border-white/10">{t('invite')}</button>
        {refOpen && (
          <div className="mt-2 p-3 rounded-xl border border-white/10 space-y-2">
            <div className="text-sm opacity-70">{t('refBonusInfo', starterPackName)}</div>
            {refInfo && (
              <>
                <div className="text-sm opacity-70">{t('myRefLink')}</div>
                <div className="break-all text-xs px-3 py-1 rounded-xl bg-emerald-900/50">{refInfo.link}</div>
                <button onClick={copyRefLink} className="w-full px-3 py-1 rounded-xl bg-emerald-600 hover:bg-emerald-500">{t('copyLink')}</button>
              </>
            )}
            <button onClick={generateRefLink} className="w-full px-3 py-1 rounded-xl bg-emerald-600 hover:bg-emerald-500">{t('generateLink')}</button>
            <div className="text-sm opacity-70">{t('invitedFriends')}</div>
            {refInfo && refInfo.invited.length>0 ? (
              <ul className="text-sm space-y-1">{refInfo.invited.map(n=><li key={n}>{n}</li>)}</ul>
            ) : (
              <div className="text-sm opacity-50">{t('noInvited')}</div>
            )}
          </div>
        )}
      </div>
      {shop.length===0 ? (
        <div className="text-center opacity-70">{t('shopEmpty')}</div>
      ) : (
        <div className="space-y-4">
          {shop.map(cat=> (
            <div key={cat.id}>
              <div className="font-semibold mb-1">{cat.name}</div>
              <div className="space-y-2">
                {cat.packs.map(item=> {
                  const hasDiscount = item.originalPrice != null && item.originalPrice > item.price;
                  const discountPercent = hasDiscount ? Math.floor(((item.originalPrice - item.price) * 100) / item.originalPrice) : null;
                  return (
                    <div key={item.id} className="p-3 rounded-xl border border-white/10 flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <AssetImage src={getShopIcon(item.id)} alt={item.name} className="w-10 h-10 object-contain" onError={e=>{ if(e?.currentTarget) e.currentTarget.style.display='none'; }} />
                        <div>
                          <div className="font-semibold">{item.name}</div>
                          <div className="text-xs opacity-70">{item.desc}</div>
                          {item.until && (
                            <div className="text-xs opacity-70">{t('autoFishUntil', new Date(item.until).toLocaleDateString())}. {t('autoFishExtend')}</div>
                          )}
                        </div>
                      </div>
                      <div className="flex flex-col items-end gap-1">
                        <button
                          type="button"
                          onClick={()=>buyPack(item.id)}
                          className="px-3 py-1 rounded-xl bg-emerald-600 hover:bg-emerald-500"
                        >{`${item.price}★`}</button>
                        {typeof item.coinPrice === 'number' && (
                          <button
                            type="button"
                            onClick={()=>onCoinPurchaseRequest?.(item.id)}
                            className="px-3 py-1 rounded-xl bg-yellow-500 text-black hover:bg-yellow-400 flex items-center justify-center gap-1 whitespace-nowrap"
                          >
                            <span className="whitespace-nowrap">{Number(item.coinPrice).toLocaleString(coinLocale)}</span>
                            <span aria-hidden="true" className="leading-none">🪙</span>
                          </button>
                        )}
                        {hasDiscount && (
                          <div className="text-xs text-right">
                            <div className="line-through opacity-60">{`${item.originalPrice}★`}</div>
                            <div className="opacity-70">{t('shopDiscountInfo', { percent: discountPercent })}</div>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
