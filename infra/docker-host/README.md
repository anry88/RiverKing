# RiverKing Docker Host

Production-oriented Docker files for running RiverKing on the Windows Home Data Center host.

The stack intentionally keeps runtime state outside Docker:

- `config.properties` is mounted read-only from the Windows host.
- `riverking.db`, WAL files, logs, and event assets live under the host data directory mounted at `/data`.
- PostgreSQL data lives under the host data directory at `postgres\` and is mounted into the PostgreSQL container.
- The container working directory is `/data`, so legacy configs with `DATABASE_URL=jdbc:sqlite:riverking.db` still keep SQLite outside the container.
- Tracked files from `data/event-assets` are copied to the host data directory by `scripts/deploy-hdc.sh`.

## Files

- `Dockerfile` builds the Gradle fat JAR and runs it on Temurin JRE 17.
- `compose.yml` runs the app on the shared `hdc-tunnel` network and runs a project-scoped PostgreSQL service without publishing host ports.
- `windows/deploy.ps1` updates the Windows env file image tag, backs up the SQLite DB when SQLite is the runtime database, and applies Compose.
- `windows/migrate-sqlite-to-postgres.ps1` performs the final one-time SQLite to PostgreSQL import.

## Default Windows Layout

```text
D:\Apps\RiverKing\env\prod.env
D:\Apps\RiverKing\config\prod\config.properties
D:\Apps\RiverKing\state\prod\riverking.db
D:\Apps\RiverKing\state\prod\postgres\
D:\Apps\RiverKing\deploy\prod\repo\infra\docker-host
```

Use the same layout with `test` for a parallel test environment.

## Deploy

From the operator machine after `main` contains the intended code:

```bash
scripts/deploy-hdc.sh --environment prod
```

After the production database has been migrated to PostgreSQL, keep passing the database mode explicitly:

```bash
scripts/deploy-hdc.sh --environment prod --database postgres
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

## SQLite To PostgreSQL

To prepare the host without switching the app yet:

```bash
scripts/deploy-hdc.sh --environment prod --database sqlite
```

To run the final migration window:

```bash
scripts/migrate-hdc-sqlite-to-postgres.sh --environment prod
```

The PostgreSQL service is scoped to the RiverKing Compose project, has no fixed container name, publishes no host port, and stores its data in `D:\Apps\RiverKing\state\prod\postgres`.
