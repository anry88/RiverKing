# RiverKing Product Overview

`RiverKing` is a Telegram Mini App fishing game backed by a Kotlin/Ktor service layer, Telegram bot flows, persistent player progression, and scheduled game operations.

The project is not a thin wrapper around a single mechanic. It combines a session-authenticated Mini App, real gameplay systems, retention loops, bot-side automation, monetization, and operational tooling in one product codebase.

## Product Scope

The shipped product includes:

- Telegram Mini App authentication and session handling through Telegram `initData`
- interactive fishing flow with cast, hook, catch, reaction timing, and catch presentation
- progression through locations, rods, lure inventory, and fish discovery
- tournaments with multiple ranking metrics and prize distribution
- daily rewards, daily and weekly quests, achievements, and daily ratings
- fishing clubs with recruiting, roles, weekly contribution tracking, and chat feed
- referral links and referral rewards
- shop flows with Telegram Stars purchases and in-game coin purchases
- bot-side commands, auto-casting, admin tooling, and payment-support flows
- profanity filtering, metrics, and TG Analytics support

## Core Game Loop

RiverKing is built around a short-session loop:

1. Enter the Mini App from Telegram.
2. Pick a location, rod, and lure.
3. Start a cast, react to the bite, and land a fish.
4. Convert catches into progression:
   - new fish discovered
   - location unlock progress
   - coins
   - quest progress
   - achievement progress
   - daily rating and tournament placement

That loop is intentionally short, but it feeds larger systems that create reasons to return: streak rewards, quests, achievements, rotating rankings, club competition, and shop upgrades.

## Engagement And Retention Decisions

Several systems in the repository are clearly aimed at repeat engagement rather than one-off play:

- `daily rewards` create a predictable return path with streak behavior
- `daily and weekly quests` create short and medium-term goals
- `achievements` convert collection and milestone play into visible progression
- `tournaments` add time-boxed competition and prize tension
- `daily ratings` provide lightweight leaderboard pressure even outside tournaments
- `clubs` create social stickiness through weekly contribution and member roles
- `auto-casting` extends utility for subscribed or advanced users and connects the bot to the core gameplay loop

The key product decision is that the fishing mechanic is only the front door. The surrounding systems turn it into an engagement product.

## Economy And Monetization

The codebase supports a layered economy:

- rods and lures gate access to better outcomes and broader coverage
- coins support in-game purchases and progression-oriented spending
- Telegram Stars power paid shop flows
- referral rewards turn acquisition into a rewarded loop
- tournament and rating prizes feed resources back into the economy

This matters from a product-engineering perspective because monetization is not isolated. It is connected to progression, retention, and social competition.

## Architecture

- `Telegram client`: launches the Mini App and bot commands
- `Mini App frontend`: shipped from `src/main/resources/webapp`
- `Ktor backend`: handles auth, profile, fishing actions, guide, ratings, shop, tournaments, clubs, quests, and referrals
- `Telegram bot layer`: handles commands, inline flows, admin actions, payment-support actions, and auto-casting
- `Exposed + SQLite`: store users, catches, inventories, tournaments, prizes, quests, achievements, clubs, referrals, and payments
- `scheduler`: distributes rewards, restores state, runs auto-fishing, and keeps periodic systems moving
- `metrics / analytics`: Prometheus-style `/metrics` plus optional TG Analytics events in the Mini App

## Operating Model

RiverKing runs as a product backend, not only as an on-demand API:

- the app serves both HTTP API routes and bot webhook handling
- the same persistence layer powers both Mini App and bot experiences
- scheduled jobs handle background economy and reward flows
- moderation helpers sanitize nicknames and text before display
- metrics expose operational and gameplay signals

That makes the repository useful as an example of a Telegram-native product backend with gameplay, monetization, admin tooling, and live-service concerns in one place.
