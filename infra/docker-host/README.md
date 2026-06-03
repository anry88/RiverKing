# RiverKing Docker Host

Production-oriented Docker files for running RiverKing on the Windows Home Data Center host.

The stack intentionally keeps runtime state outside Docker:

- `config.properties` is mounted read-only from the Windows host.
- `riverking.db`, WAL files, logs, and event assets live under the host data directory mounted at `/data`.
- The container working directory is `/data`, so legacy configs with `DATABASE_URL=jdbc:sqlite:riverking.db` still keep SQLite outside the container.
- Tracked files from `data/event-assets` are copied to the host data directory by `scripts/deploy-hdc.sh`.

## Files

- `Dockerfile` builds the Gradle fat JAR and runs it on Temurin JRE 17.
- `compose.yml` runs the app on the shared `hdc-tunnel` network without publishing host ports.
- `windows/deploy.ps1` updates the Windows env file image tag, backs up the SQLite DB, and applies Compose.

## Default Windows Layout

```text
D:\Apps\RiverKing\env\prod.env
D:\Apps\RiverKing\config\prod\config.properties
D:\Apps\RiverKing\state\prod\riverking.db
D:\Apps\RiverKing\deploy\prod\repo\infra\docker-host
```

Use the same layout with `test` for a parallel test environment.

## Deploy

From the operator machine after `main` contains the intended code:

```bash
scripts/deploy-hdc.sh --environment prod
```

For first restore from a VDS backup:

```bash
scripts/deploy-hdc.sh \
  --environment prod \
  --seed-config "/path/to/config.properties" \
  --seed-db "/path/to/riverking.db" \
  --force-seed-db
```

The seed flags copy files to the Windows host before Compose starts. Existing remote files are backed up with a timestamp.
