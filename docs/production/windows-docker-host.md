# Windows Docker Host Deployment

RiverKing can run on the Home Data Center Windows Docker host with the same develop-to-main release flow as the other games.

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

Use the latest external config and SQLite backup. The database remains outside Docker under `D:\riverking\state\<env>`.

```bash
scripts/deploy-hdc.sh \
  --environment prod \
  --seed-config "/path/to/config.properties" \
  --seed-db "/path/to/riverking.db" \
  --force-seed-db
```

The script backs up replaced remote config and database files before copying the seed files.

## Runtime Layout

```text
D:\riverking\env\prod.env
D:\riverking\config\prod\config.properties
D:\riverking\state\prod\riverking.db
D:\riverking\state\prod\logs\
D:\riverking\state\prod\event-assets\
```

`scripts/deploy-hdc.sh` syncs tracked files from `data/event-assets` into the environment data directory before Compose starts.

For a test instance, use `--environment test`; it uses the same structure under `test` and a separate Compose project.

## Cloudflare Tunnel

The Compose service joins the existing external Docker network `hdc-tunnel` and uses these default aliases:

- `riverking-prod-app`
- `riverking-test-app`

Route the public hostname to `http://riverking-prod-app:5005` for production, and to `http://riverking-test-app:5005` for test.
