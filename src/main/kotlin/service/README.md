# `service` package

Services encapsulate business logic and database access through Exposed. Below are the key classes and the main functions most frequently called by routes and background tasks.

## FishingService
Manages the player lifecycle and fishing process.
- **Initialization and profile:** `createUser`, `ensureUserById`, and `ensureUserByTgId` manage shared player records; `setNickname`, `setLanguage`, `userLanguage`, and `displayName` update profile data.
- **Inventory:** `listLures` and `listRods` return available lures and rods; `currentLureId`/`currentRodId` are read/updated inside `startCast` and `buyPackage`.
- **Fishing:** `startCast`, `hook`, `catch` manage the cast→hook→catch flow; `hook` now stores the pending catch and returns the hooked fish plus tap goal, duration, and struggle intensity for both clients; `resetCasting` and `restoreCastingLuresOnStartup` clear stuck states.
- **Locations and progress:** `locations`, `unlockLocation`, `totalCaughtKg`, `todayCaughtKg`, and `recent` build stats and unlock zones; `locations` can prepend the active special-event location with lock metadata for users outside clubs; `userLanguage` drives localization.
- **Daily rewards and auto‑fishing:** `dailyRewardSchedule`, `canClaimDaily`, `dailyReward` implement the reward chain; `autoFishUsers` and `applyAutoFishReward` grant scheduled automatic catches.
- **Shop and monetization:** `listPackages`, `findPack`, `buyPackage`, `buyPackageWithCoins` handle purchases; `todayCoins` and `addCoins` track balances.
- **Rods and recommendations:** `listRods`, `setRod`, `recommendRod` suggest rods for the current location and lure.

## TournamentService, SpecialEventService, PrizeService, RatingPrizeService
- **TournamentService** stores tournament parameters: `createTournament`, `updateTournament`, paged/sorted `listTournaments`, `currentTournament`, `upcomingTournaments`, `pastTournaments`. Leaderboards are built via `leaderboard`, which aggregates catches in the required metric.
- **SpecialEventService** manages temporary club events: non-overlapping event CRUD, event fish pools/manual weights, active event locations, club total-weight and total-count leaderboards, personal rarest/largest fish leaderboards, progress subtraction on club exit, and event prize distribution.
- **PrizeService** issues and records rewards: `pendingPrizes`, `grantPrizes`, `userPrizes`, and `applyPrize` (used by the scheduler and API), including `PrizeSource.EVENT` prizes when an event service is wired in.
- **RatingPrizeService** helps compute rating prizes after tournaments and prepares `PrizeSpec` for delivery.

## Shop and payments
- **StarsPaymentService** creates invoices for Telegram Stars payments (`createInvoice`), validates amounts (`validatePayment`), and records completed payments (`completePayment`).
- **PayService** tracks payment history and coin crediting: `recordPayment`, `listPayments`, plus duplicate checks.
- **PlayPurchaseVerifier / PlayPurchaseService** verify Android `play` purchase tokens against Google Play, reject pending/cancelled/mismatched purchases, and only then grant entitlement plus record the payment.

## Additional services
- **I18n** — catalog of localized names for fish, locations, rods, and lures; functions like `fish(code, lang)` and `location(code, lang)` are used by the API for readable responses.
- **AndroidUpdateService** — loads `src/main/resources/android-update-policy.json`, compares Android client version headers against `latestVersionCode` / `minSupportedVersionCode`, honors the explicit `requireVersionHeaders` fatal rollout switch, and returns store/APK install targets for optional or mandatory upgrades.
- **ReferralService** — creates and applies referral tokens: `generateLink`, `currentLink`, `invited`, `applyToken`; `onPurchase` awards the referrer after purchases.
- **PrizeService** (supporting types) — `UserPrize` and `PrizeSpec` DTOs are reused by `/api/tournament*` routes.
- **ClubService** — manages fishing clubs: `createClub`, `joinClub`, `searchClubs`, `clubDetails`, `clubChat`, `sendChatMessage`, weekly contribution tracking (from daily rating rewards), chat system events, and weekly club reward distribution. `clubDetails` now returns both current/previous weekly rating views and current/previous weekly club-quest views for the club page.
- **ClubQuestService** — manages shared weekly club quests: assigns `2` quests from the club pool each week, tracks pooled catch progress for the whole club, persists per-member quest contribution snapshots for each week, and splits completion rewards across the active roster at completion time.
- **AchievementService** — tracks the full achievement catalog and player progress: rarity ladders, koi collection, location completion, traveler/trophy milestones, tournament wins, and daily-rating stars. Rewards and claim status are persisted in `AchievementProgress` and surfaced through the API and bot.
- **QuestService** — assigns daily/weekly personal quests, filters fish-specific quests by actually unlocked fish via `LocationFishWeights`, tracks progress based on catches, awards coin rewards on completion, and powers the `daily` / `weekly` parts of `/api/quests` plus the `/quests` bot command.
