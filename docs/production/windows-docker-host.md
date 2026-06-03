# Windows Docker Host Deployment

RiverKing can run on the Home Data Center Windows Docker host with the same develop-to-main release flow as the other games.

Production public endpoints:

- Public base URL: `https://riverking.tg-games.com`
- Telegram BotFather Mini App URL: `https://riverking.tg-games.com/app`
- Telegram bot webhook URL: `https://riverking.tg-games.com/bot`
- Telegram direct Mini App link: `https://t.me/river_king_bot/app`
- Cloudflare Tunnel target: `http://riverking-prod-app:5005`

## Release Flow

1. Commit changes on `develop`.
2. Push `develop` to GitHub.
3. Open the `develop` -> `main` release PR.
4. Merge the PR.
5. Deploy from a fresh `main` checkout:

   ```bash
   scripts/deploy-hdc.sh --environment prod
   ```

## First Restore From VDS Backup

Use the latest external config and SQLite backup. The restore path starts on SQLite, and the database file remains outside Docker under `D:\Apps\RiverKing\state\<env>`.

```bash
scripts/deploy-hdc.sh \
  --environment prod \
  --seed-config "/path/to/config.properties" \
  --seed-db "/path/to/riverking.db" \
  --force-seed-db
```

The script backs up replaced remote config and database files before copying the seed files.

## SQLite To PostgreSQL Migration

Production can run on a dedicated PostgreSQL container in the same Compose project. The PostgreSQL container does not publish a host port and does not use a fixed `container_name`, so it does not conflict with other PostgreSQL containers on the Windows host. Runtime data is bind-mounted outside Docker:

```text
D:\Apps\RiverKing\state\prod\postgres\
```

Use a two-stage rollout to keep downtime short:

1. Merge the PostgreSQL-capable code to `main`.
2. Deploy the new image and compose files while the app still uses SQLite:

   ```bash
   scripts/deploy-hdc.sh --environment prod --database sqlite
   ```

   This syncs the PostgreSQL-capable Compose files and creates the persistent data directory without switching the app.

3. Run the final migration window:

   ```bash
   scripts/migrate-hdc-sqlite-to-postgres.sh --environment prod
   ```

   The migration script pulls the pgloader image and starts PostgreSQL before stopping the app. During downtime it stops only the app, backs up `riverking.db`, imports the final SQLite snapshot into PostgreSQL, switches `DATABASE_URL` in the Windows env file, and starts the app again.

4. Verify:

   ```bash
   curl -fsS https://riverking.tg-games.com/health
   docker --context hdc ps --filter name=riverking-prod
   ```

For repeated deploys after the migration, use:

```bash
scripts/deploy-hdc.sh --environment prod --database postgres
```

## Runtime Layout

```text
D:\Apps\RiverKing\env\prod.env
D:\Apps\RiverKing\config\prod\config.properties
D:\Apps\RiverKing\state\prod\riverking.db
D:\Apps\RiverKing\state\prod\postgres\
D:\Apps\RiverKing\state\prod\logs\
D:\Apps\RiverKing\state\prod\event-assets\
```

`scripts/deploy-hdc.sh` syncs tracked files from `data/event-assets` into the environment data directory before Compose starts.

For a test instance, use `--environment test`; it uses the same structure under `test` and a separate Compose project.

## Cloudflare Tunnel

The Compose service joins the existing external Docker network `hdc-tunnel` and uses these default aliases:

- `riverking-prod-app`
- `riverking-test-app`

Route the public hostname to `http://riverking-prod-app:5005` for production, and to `http://riverking-test-app:5005` for test.
