# Changelog

All notable changes to Data Collector are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.0.0] - Unreleased

The first release from this repository. Until 2.x, every module was a repository of its own; they are now one Maven
reactor with one version, and the old repositories redirect here. 3.0.0 runs dc on the Descoped 3.x libraries, current
dependencies and Java 17. dc's own behaviour stays as in 2.x, except for the changes the libraries force, listed under
**Breaking**.

### Breaking

- **Server image settings:** JVM options and system properties go in `JAVA_TOOL_OPTIONS`, which the JVM reads itself.
  The 2.x variables `JAVA_OPTS`, `PROXY_HTTP_HOST`, `PROXY_HTTP_PORT`, `PROXY_HTTPS_HOST`, `PROXY_HTTPS_PORT` and
  `ENABLE_JMX_REMOTE_DEBUGGING` and the script `/run.sh` are gone: give `-Dhttp.proxyHost=...` and the JMX options in
  `JAVA_TOOL_OPTIONS`. `Dockerfile-dev` is gone; the dev image is the debug variant, and `run-dev.sh` turns JMX on.
- **Message ids are UUIDv7**, as rawdata-client 3 gives them:
  - `ContentStreamBuffer.ulid()` is now `id()` and returns a `UUID`;
  - `io.descoped.dc.api.ulid` is replaced by `io.descoped.dc.api.ids.TimeOrderedIds`, whose order is unsigned;
  - worker ids, correlation ids and integrity report ids are UUIDv7 too. The integrity report keeps its `ulid` field
    name.
- **Storage:** rawdata-client 3 does not read data written by 2.x. The `postgres` provider uses three shared tables,
  and `filesystem` and `gcs` have a new segment layout.
- **Encrypted content** is written in encryption-util 3's ciphertext format 2, which encryption-util 2.0.1 cannot
  read. The key is still derived as in 2.x (PBKDF2-HMAC-SHA256, 65 536 iterations, a salt of any length).
- **Configuration is read once, at startup.** 2.x read environment variables and system properties again on every
  lookup.
- **Google credentials:** without a service-account key, the Google Secret Manager provider uses Application Default
  Credentials. 2.x used Compute Engine credentials only.
- **Java 17** is the minimum.

### Changed

- **One reactor, one version.**
  - Every module has one version under `io.descoped.dc`, with a new BOM, `data-collector-bom`.
  - Artifact ids and JPMS module names are unchanged.
- **Descoped libraries on 3.x:**

  | Library | Version |
  |---|---|
  | service-provider-api | 3.0.1 |
  | dynamic-configuration | 3.0.1 |
  | secrets-client | 3.0.0 |
  | encryption-util | 3.0.1 |
  | rawdata-client | 3.1.0 |

  rawdata-client 3.1 makes a message id unique in its topic: the same message again is ignored, a different one
  under a taken id is refused. A recovery run repeated over the same source no longer duplicates the target's
  messages (on 3.0.1 the `filesystem` provider stored them twice and `postgres` failed on its key).

  Every other dependency is at its latest version except two:
  - classgraph stays on 4.8.200, because later versions need Java 25;
  - commons-jexl3 stays on 3.2.1, because later versions deny the class access that specification expressions use.
- **Configuration** keeps the 2.x rules on dynamic-configuration 3:
  - every class path match is read;
  - every `DC_` environment variable and every system property adds a key;
  - values are literal;
  - only `true`, in any case, reads as true.

  The secrets-client 3 key names are mapped internally, so dc's configuration keys are unchanged.
- **Server image**, `descoped/data-collector` on Docker Hub, in three variants from one `Dockerfile`:
  - regular (`X.Y.Z`, `latest`), with a shell (busybox); distroless (`X.Y.Z-distroless`), no shell and no program but
    `java`; debug (`X.Y.Z-debug`), with the JDK's diagnostic tools;
  - Temurin 17 linked with `jlink` to the modules dc uses, on distroless Debian 13 (glibc), for `linux/amd64` and
    `linux/arm64`, every base pinned by digest;
  - runs as user `dc` (uid 10001); `java` is the container's process, started with `/opt/dc/java.args`;
  - only runtime jars, with no test jars or mock API, split between module path and class path the way the tests run;
  - JMX remote is off in every variant.
- **Releases** are the server images, pushed to Docker Hub with an SBOM, the provenance of the build and a cosign
  signature by `release.yml`, which the owner starts by hand; it also tags the version and makes the GitHub release.
  No jar is released.
- **PostgreSQL 18** in the compose setups. `init-db-and-user.sql` makes `rdc` the owner of its database, which
  PostgreSQL 15 and later require.
- **SNAPSHOTs** are published from CI to `https://maven.descoped.io/` (Descoped's AWS CodeArtifact repository, no
  credentials to read): `X.Y.Z-SNAPSHOT` from `master`, `X.Y.Z-<branch>-SNAPSHOT` from a branch run by hand.
- **No Maven Central publishing.** 3.x is not published to Maven Central, and the POM has no profile for it.
- **brreg updates example** (`enhetsregisteret-updates-spec.json`): a deleted entity's 410 Gone is a success, and its
  body (`organisasjonsnummer`, `slettedato`) is stored as the event. Before, the collection stopped at the first
  deletion in the feed.
- **Documentation** is Markdown in `docs/`, revised for 3.0. The examples (compose setups, profiles, specifications)
  are in `examples/`.
- **SonarCloud findings** (bugs, vulnerabilities and code smells), module by module:
  - a thread interrupted while it waits keeps its interrupt flag when the wait ends in an exception;
  - errors that were a plain `RuntimeException` are a subclass of it (`IllegalStateException`,
    `IllegalArgumentException`, `UncheckedIOException`), so a caller that catches `RuntimeException` still does;
  - two specification builders are equal only when their URL (`get`, `post`, `put`, `delete`), identity id or string
    body is equal too;
  - a temporary file for a response body is readable and writable by its owner only, on a file system with POSIX
    permissions;
  - `HttpClientExporterTest`, which calls www.google.com, is a `live` test.

### Removed

- The OkHttp client delegates. They were never registered, so they never ran.
- The Kafka targets and profile in the examples: rawdata-client 3 has no Kafka provider.
- The production variants of the example specifications, and the scripts tied to one organisation's infrastructure.
- The AsciiDoc documentation and its PlantUML build.
- The unused `docker/entrypoint.sh`, which the image never copied.
- `CryptoUtils` (data-collector-api), an MD5 helper that nothing called.

### Fixed

- `start-collector.sh`: setting an HTTPS proxy no longer drops the HTTP proxy.
- `-XX:+EnableJVMCI` is no longer passed. Temurin has no Graal compiler, and JDK 27 rejects the option.
- jffi's classes and its native libraries are the same version.
- `build-and-run.sh` loads LMDB's native bridge on macOS, including Apple Silicon.
- `worker.config/page-test.json` is in the current specification format.
- The filesystem example setups pass their log4j2 file with `-Dlog4j2.configurationFile` in `JAVA_OPTS`. They used
  to set `LOGBACK_CONFIGURATION_FILE` to a logback file, and that variable has no effect: log4j2 is already configured
  when the server reads it.

## 2.x

2.x was released per module from the separate repositories. Their tags are kept here with the repository name as
prefix, for example `data-collector-api/2.0.4`.
