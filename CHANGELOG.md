# Changelog

All notable changes to Data Collector are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.0.0] - Unreleased

The first release from this repository. Until 2.x, every module was a repository of its own; they are now one Maven
reactor with one version, and the old repositories redirect here. 3.0.0 runs dc on the Descoped 3.x libraries, current
dependencies and JDK 27. dc's own behaviour stays as in 2.x, except for the changes the libraries force, listed under
**Breaking**.

### Breaking

- **Secrets stay out of logs, exception messages, `/health` and manifests.** Secret headers (`Authorization`,
  `Proxy-Authorization`, `Cookie`, `Set-Cookie`, names containing `key`, `token`, `secret`, `signature` or
  `password`, and those named in the new `data.collector.secret-headers`) keep their names with masked values in
  `manifest.json`, in HTTP error messages and on `/health`. URLs in logs and messages have their query values masked.
  Bodies and documents are given by length and content type: HTTP errors, failed jq, XPath and regular expression
  queries, retries and `Parallel` errors. Failed expressions log variable names only, and the prefetch log masks secret-named variables. `/health?config` masks every
  value. Request paths in error messages are escaped. In 2.x all of these could carry tokens and credentials.
- **`io.descoped.dc` logs at `INFO`** in the image's log configurations (was `DEBUG`) and in the example profiles (was
  `TRACE`); `LOG4J_LEVEL` still sets it.
- **Every endpoint requires authentication**, `/ping`, `/health` and `/metrics` included. The authenticator is a
  provider chosen by `http.authentication.provider`; the built-in `bearer` (the default) accepts
  `Authorization: Bearer <token>` with the token in `http.authentication.bearer.token`. The server does not start
  without the token, or with one shorter than 32 characters. Other schemes are providers of `io.descoped.dc.application.spi.Authenticator`. 2.x had no
  authentication.
- **CORS is off by default.** `http.cors.allow.origin` was `.*` in the defaults files; without it the server now sends
  no CORS headers. Setting it turns CORS on, and preflights are answered before authentication.
- **Topic names in requests are checked:** the integrity check and recovery endpoints answer `400` to a `{topic}` or
  `toTopic` that is not a rawdata topic name (1 to 100 of `A-Z a-z 0-9 _ . -`, starting with a letter or digit). In 2.x
  `PUT /check-integrity/..` deleted the directory above the LMDB indexes.
- **The rawdata dump** writes each position and content key as one file name, percent-encoding anything but letters,
  digits and `. - * _`; in 2.x a position or key could name a path outside the dump directory. It does not start when
  `data.collector.rawdata.dump.topic` is not a topic name.
- **Expressions run in a sandbox** (Apache Commons JEXL 3.7.1, from 3.2.1). They can call only the methods of strings,
  numbers, booleans, `java.util` collections, `cast`, `convert`, `contentStream` and registered expression functions.
  `new(...)`, imports, pragmas, loops and scripts are off. A call of anything else gives `null`, as navigation through
  `null` does; in 2.x an expression could run any code with the rights of the process.
- **`ENV` holds only the configuration keys that start with `spec.`**, without the prefix: configure
  `spec.maskinporten.clientId` for `${ENV.'maskinporten.clientId'}`. In 2.x `ENV` was the whole configuration, secrets
  included.
- **A `Process` names its processor's provider** (`"processor": "<name>"`), registered as an
  `io.descoped.dc.api.ProcessorProvider`; `processorClass` is no longer read, so no class is loaded by a name from a
  specification. The Java DSL keeps `process(Class)` and adds `process(String)`.
- **TLS chains are verified.** A business certificate bundle's trust manager no longer accepts a chain that fails
  verification because its last certificate equals the bundle's own; in 2.x a man in the middle could use that. A
  server whose chain is not in the JDK's `cacerts` is trusted through the bundle's trust anchors: `trust.certificates`
  (a PEM file of CA certificates) in a bundle directory, or `data.collector.sslBundle.trustCertificates` (a secret)
  with Google Secret Manager.
- **Rawdata encryption fails closed.** Once any `rawdata.encryption.*` key is set, an unknown provider, only one of key
  and salt, a secret that is not found or an empty value stops the server; 2.x wrote content in clear. The recovery
  store's bypass flag `recovery.rawdata.encryption.credentials.ignore` is gone: recovery removes every
  `rawdata.encryption.*` key from its own store's configuration.
- **JDK 27:** dc is built for, tested on and runs on JDK 27 (class files of Java 27); 2.x ran on Java 17. The server
  image's runtime is Temurin 27, and its `java` allows native access for the class path (jffi) and lmdbjava's use of
  `sun.misc.Unsafe`, which JDK 27 would otherwise warn about.
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

### Added

- **`io.descoped.dc.api.el.ExpressionFunctions`** and **`io.descoped.dc.api.ProcessorProvider`**: service providers that
  add functions to expressions and processors to `Process` nodes, each under a name a specification uses.

### Changed

- **One reactor, one version.**
  - Every module has one version under `io.descoped.dc`, with a new BOM, `data-collector-bom`.
  - Artifact ids and JPMS module names are unchanged.
- **Descoped libraries on 3.x:**

  | Library | Version |
  |---|---|
  | service-provider-api | 3.0.1 |
  | dynamic-configuration | 3.0.2 |
  | secrets-client | 3.0.0 |
  | encryption-util | 3.0.1 |
  | rawdata-client | 3.1.0 |

  rawdata-client 3.1 makes a message id unique in its topic: the same message again is ignored, a different one
  under a taken id is refused. A recovery run repeated over the same source no longer duplicates the target's
  messages (on 3.0.1 the `filesystem` provider stored them twice and `postgres` failed on its key).

  Every other dependency is at its latest version. classgraph 6 (for JDK 25 and later) scans only the sources it is
  given and no longer loads classes: dc enables the class path and the module path, as 4.x scanned by default, and
  loads the handlers and health and metrics resources the scans find itself.
- **Configuration** keeps the 2.x rules on dynamic-configuration 3:
  - every class path match is read;
  - every `DC_` environment variable and every system property adds a key;
  - values are literal;
  - only `true`, in any case, reads as true.

  The secrets-client 3 key names are mapped internally, so dc's configuration keys are unchanged.
- **Server image**, `descoped/data-collector` on Docker Hub, in three variants from one `Dockerfile`:
  - regular (`X.Y.Z`, `latest`), with a shell (busybox); distroless (`X.Y.Z-distroless`), no shell and no program but
    `java`; debug (`X.Y.Z-debug`), with the JDK's diagnostic tools;
  - Temurin 27 linked with `jlink` to the modules dc uses, on distroless Debian 13 (glibc), for `linux/amd64` and
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
  - a thread interrupted while it waits keeps its interrupt flag when the wait ends in an exception, and an
    interrupted worker (one being stopped) no longer retries a failed HTTP request;
  - errors that were a plain `RuntimeException` are a subclass of it (`IllegalStateException`,
    `IllegalArgumentException`, `UncheckedIOException`), so a caller that catches `RuntimeException` still does;
  - two specification builders are equal only when their URL (`get`, `post`, `put`, `delete`), identity id or string
    body is equal too;
  - a temporary file for a response body is for its owner only: `rw-------` with POSIX permissions, otherwise
    (Windows) an ACL with one entry, for the user running dc;
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
