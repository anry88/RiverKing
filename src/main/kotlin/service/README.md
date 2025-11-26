# `service` package

Services encapsulate business logic and database access through Exposed. Below are the key classes and the main functions most frequently called by routes and background tasks.

## FishingService
Manages the player lifecycle and fishing process.
- **Initialization and profile:** `ensureUserByTgId` creates or returns a user by Telegram ID; `setNickname`, `setLanguage`, `userLanguage`, `displayName` update profile data.
- **Inventory:** `listLures` and `listRods` return available lures and rods; `currentLureId`/`currentRodId` are read/updated inside `startCast` and `buyPackage`.
- **Fishing:** `startCast`, `hook`, `catch` manage the cast→hook→catch flow; `resetCasting` and `restoreCastingLuresOnStartup` clear stuck states.
- **Locations and progress:** `locations`, `unlockLocation`, `totalCaughtKg`, `todayCaughtKg`, and `recent` build stats and unlock zones; `userLanguage` drives localization.
- **Daily rewards and auto‑fishing:** `dailyRewardSchedule`, `canClaimDaily`, `dailyReward` implement the reward chain; `autoFishUsers` and `applyAutoFishReward` grant scheduled automatic catches.
- **Shop and monetization:** `listPackages`, `findPack`, `buyPackage`, `buyPackageWithCoins` handle purchases; `todayCoins` and `addCoins` track balances.
- **Rods and recommendations:** `listRods`, `setRod`, `recommendRod` suggest rods for the current location and lure.

## TournamentService, PrizeService, RatingPrizeService
- **TournamentService** stores tournament parameters: `createTournament`, `updateTournament`, `listTournaments`, `currentTournament`, `upcomingTournaments`, `pastTournaments`. Leaderboards are built via `leaderboard`, which aggregates catches in the required metric.
- **PrizeService** issues and records rewards: `pendingPrizes`, `grantPrizes`, `userPrizes`, and `applyPrize` (used by the scheduler and API).
- **RatingPrizeService** helps compute rating prizes after tournaments and prepares `PrizeSpec` for delivery.

## Shop and payments
- **StarsPaymentService** creates invoices for Telegram Stars payments (`createInvoice`), validates amounts (`validatePayment`), and records completed payments (`completePayment`).
- **PayService** tracks payment history and coin crediting: `recordPayment`, `listPayments`, plus duplicate checks.

## Additional services
- **I18n** — catalog of localized names for fish, locations, rods, and lures; functions like `fish(code, lang)` and `location(code, lang)` are used by the API for readable responses.
- **ReferralService** — creates and applies referral tokens: `generateLink`, `currentLink`, `invited`, `applyToken`; `onPurchase` awards the referrer after purchases.
- **PrizeService** (supporting types) — `UserPrize` and `PrizeSpec` DTOs are reused by `/api/tournament*` routes.
