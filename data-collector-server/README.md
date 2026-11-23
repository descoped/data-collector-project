# Data Collector Server

The server runs specifications as tasks over HTTP (`PUT /tasks`). It also checks topics with an integrity check (LMDB),
and copies a checked topic to a new one (recovery). This module also builds the Docker image.

JPMS module: `io.descoped.dc.server`. Part of [Data Collector](../README.md); built and released with the whole
repository.

## Files

| File | Purpose |
|---|---|
| `Dockerfile` | The image in three variants (regular, distroless, debug): Temurin 17 linked with `jlink` on distroless Debian 13, `linux/amd64` and `linux/arm64`, JMX off |
| `docker/java.args` | The arguments of `java`, the image's entrypoint |
| `docker/SplitModulePath.java` | Splits the jars into module path and class path at image build |
| `build-dev.sh` | Builds the server and the debug image as `data-collector:dev` |
| `build-and-run.sh` | Builds the server and runs it locally, without Docker |
| `run-dev.sh` | Runs `data-collector:dev` with JMX remote on port 9992, on this machine only, for VisualVM |
| `docker-compose.yml`, `init-db-and-user.sql`, `docker-postgres-init-db.bash` | A PostgreSQL 18 database for running the server locally |

## Documentation

- [Getting started](../docs/getting-started.md): build the image, start it, run a specification
- [REST API](../docs/rest-api.md): every endpoint
- [Administration](../docs/administration.md): configuration, the image, logging, metrics, Visual VM, client certificates,
  encryption, integrity check and recovery
