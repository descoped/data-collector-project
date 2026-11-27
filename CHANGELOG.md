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
- **No metrics agent.** The engine reports to engine observers itself (see Added), so the ByteBuddy agents and
  everything that installed them are removed: `MetricsAgent`, `HttpClientAgent`, `ContentStoreAgent`,
  `HttpClientExporter`, `ContentStoreExporter`, `ExecutionWrapper` and the switch that would have wrapped every handler.
  A library user no longer calls `MetricsAgent.premain`; the metrics come with core. Core no longer requires ByteBuddy
  (it was `requires transitive`) or `java.instrument`, the image starts no `-javaagent`, and its regular runtime has no
  `java.instrument` (the debug runtime keeps it for diagnostic agents).
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
- **Prometheus client_java 1.x** (from simpleclient 0.16, whose line has ended). dc's metrics keep their names, labels
  and histogram buckets. Changed on `GET /metrics`:
  - the JVM and process metrics are client_java's `JvmMetrics`: `jvm_memory_bytes_*` is now `jvm_memory_*_bytes`,
    `jvm_memory_pool_bytes_*` is now `jvm_memory_pool_*_bytes`, `jvm_info` is now `jvm_runtime_info`, and
    `jvm_compilation_time_seconds_total` is new;
  - counters no longer have a `_created` series;
  - `name[]` also matches a metric's own name, and a match returns the whole metric, not only the named sample.
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

- **Engine observers** (`io.descoped.dc.api.observer.EngineObserver`): the engine reports workers, node executions,
  requests and their retries, failed GET steps, completed pages, fanned-out entries and content store writes to every
  provider `ServiceLoader` finds. A `RuntimeException` an observer throws is logged once and never fails the engine; the events carry
  no headers, bodies or configuration values. An event that takes time returns an `Observation`, for a timer or,
  later, a span. Core's `PrometheusEngineObserver` records the client and content store metrics of 2.x, with the same
  names, labels and buckets, and new ones: `dc_worker_started`, `dc_worker_finished`, `dc_node_duration`,
  `dc_node_failure`, `dc_client_request_retry`, `dc_pagination_page` and `dc_parallel_entry`.
- **`io.descoped.dc.api.el.ExpressionFunctions`** and **`io.descoped.dc.api.ProcessorProvider`**: service providers that
  add functions to expressions and processors to `Process` nodes, each under a name a specification uses.

### Changed

- **Javadoc for api and core.** Every public element of their exported packages is documented, and Javadoc's doclint
  checks them in full (`missing` is no longer excluded).
- **`close()` of `ContentStore`, `ContentStream`, `ContentStreamConsumer` and `ContentStreamProducer`** throws no
  checked exception; every implementation already closed without one.
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
  | rawdata-client | 3.2.0-SNAPSHOT (on the owner's request, until 3.2.0 is released) |

  rawdata-client 3.1 makes a message id unique in its topic: the same message again is ignored, a different one under
  a taken id is refused. rawdata-client 3.2 makes two guarantees in every provider: a publish that returned survives a
  crash of the process, and once a producer of a topic exists its last message holds everything published before, a
  crashed process's publishes included. The worker opens its topics' producers before it, or the specification's
  `contentStream` expressions, read what is committed, so a run resumed after a crash writes every position once. This
  assumes that on `filesystem` and `gcs`, `local-temp-folder` is on durable, persistent storage (a persistent block
  device such as AWS EBS or a GCE persistent disk, or a `PersistentVolume`). On ephemeral storage (EC2 instance store,
  the container's writable layer, `emptyDir`, `tmpfs`) the guarantees fall away, and positions that were not uploaded
  are silently lost. A crash of the operating system or the host, rather than the process, can lose up to
  `avro-file.sync.interval` bytes per topic that were not yet forced to disk. A recovery run repeated over the same
  source no longer duplicates the target's messages (on 3.0.1 the `filesystem` provider stored them twice and
  `postgres` failed on its key).

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

- **A failing `Parallel` entry outside a `Paginate` fails the worker** (#24). Nothing waited for those entries, so the
  worker completed while they ran and their failures were lost (and an entry still running 5 seconds after the start
  node returned was interrupted when the thread pool shut down). The worker now waits for every entry it started and
  fails with the first failure, as inside a `Paginate`.
- **Stopping the server cancels the running tasks** (#45) and waits for them, at most
  `data.collector.worker.stop.timeoutInSeconds` (30), before the content store is closed; it stopped none and closed
  the store under them. A task's future no longer fails with a `NullPointerException` when the task succeeds.
- **Data races** (#46): the contexts of a worker no longer copy its shared global state onto itself from every thread,
  the global state is synchronized, and `/health` reads the workers' health resources under the lock that guards them.
- **Each worker counts its own `Parallel` iterations** (#66). The counters behind `stopAtNumberOfIterations` and the
  queue buffer's page log were static, shared by every worker in the JVM: a worker's limit counted the entries of
  every other worker, and a second worker after a limited one published nothing. `ParallelHandler.countNumberOfIterations`
  is gone; `ParallelHandler.Counters` is registered in each worker's services.
- **One integrity check or recovery per topic** (#67): two `PUT /check-integrity/{topic}` (or `PUT /recovery/{topic}`)
  requests that raced could both start, the integrity check deleting and opening the same LMDB directory twice. The
  topic is now claimed before anything starts, and the request that loses gets 409 Conflict.
- **An integrity check's buffer pool fails at once when it is empty** (#68), instead of waiting about 26 seconds for a
  buffer that no other thread can release.
- **A stopped worker closes its topic on an interrupted thread.** The rawdata content store took the topic's lock
  interruptibly in `closeTopic`, so a worker whose thread was interrupted while it waited for its pool failed in its
  `finally` before its producer was closed, and what the producer held stayed in `local-temp-folder` until the next
  start. The lock is now taken uninterruptibly, and the interrupt flag is kept.
- **A page is no longer fetched twice** (#75). When a page completed, the next page was scheduled and only then
  recorded as the last page; if it ran first and started the next prefetch, that prefetch chained on the completed
  page and fetched the same page again, so its entries were published twice. A page is now recorded and queued before
  it can run.
- **The stream is durable, and a committed position is never written again** (#71, #72).
  - **Lost entries:** a page was committed to `<topic>-pages` when it was fetched, before its entries, and prefetch ran
    several pages ahead. A specification that resumes from the page topic skipped the entries of those pages: 28 of
    3,000 were lost in a test that stopped a worker three times. A page is now committed when it is complete. A page
    in flight, a failed page and an empty page are not committed, so the empty page at the head is no longer written.
  - **Repeated pages:** a resumed run wrote the page in flight at the stop again, and a run resumed on its last page
    wrote that page's entries again. At start a worker now reads the last committed page and entry, and writes neither
    again; a last page that has grown since still gets its new entries written.
  - **Stale content:** a stopped run's buffered content stayed in the content store, and a later run of the topic
    could publish it. A worker now discards it when it ends. `ContentStore` has `discard(topic, positions...)` and
    `discard(topic)`.
  - **Discarding store:** it keeps the last position per topic, pages included. Before, `lastPosition` returned the
    last entry for every topic.
- **Positions are published in order whatever the content store locks** (#69). The reordering of completed positions
  handed a released run on after leaving its lock, so two threads could publish their runs in reverse order. The rawdata
  store's topic lock hid it; with the `discarding` store, the default, the last position could end below the real one.
- **`http.port=0` reports the port the server got** (#62): `UndertowApplication.getPort()`, the startup log and the
  health output give the ephemeral port the listener is bound to, not 0. The test servers of data-collector-testutils
  use this: they bind port 0 instead of probing for a free port between 9000 and 9499 and binding it later, which
  could fail with `BindException` when another process took the port in between.
- **The integrity check runs to the topic's last message** (#60). It stopped at the first message with the last
  message's position, so a topic whose last position also occurs earlier, as a duplicate, was checked only up to
  that occurrence. Recovery then could find no duplicate to recover to and failed with a `NullPointerException`; it
  now fails with a message that says so.
- **The worker history on `/health` keeps the latest 25 workers** (#56). Once full, it dropped the newest entry
  instead of the oldest, so it showed the 25 oldest workers and the latest one, and it held 26.
- **A `Process` fails when its output lacks any required variable** (#57). It failed only when the output had none of
  them, so a `Process` without required outputs always failed and one with several passed when one was present.
- **The `Execute specification` log of a node is YAML**, as for a specification; `NodeBuilder.serializeAsYaml()` gave
  JSON.
- **The client and content store metrics are recorded** (#25). 2.x's agent used ByteBuddy `MethodDelegation`, which
  could not define its helper classes on JDK 27, so no `dc_client_*` or `dc_content_stream_*` metric was recorded.
  They now come from the engine observer, which wraps the client and the content store a worker uses.
- **Recovery closes its own producer and consumer** (#31): the producer of the target topic and the consumer of the
  source topic; it closed them under each other's topic, so neither was closed.
- **The rawdata dump stops on the first `stop()`** and waits for its exporter (#32); it waited only on a second call.
- **`GET /check-integrity` lists each job once** (#33); a completed job still held in memory was listed again for
  each job on disk.
- **Response predicates decide the outcome** (#28): a status listed with predicates is a success when one of them
  holds, and a failure otherwise. In 2.x the status was accepted whatever the body held.
- **`QueryXmlToken` gives entries** (#26): each child element of the root, streamed with StAX, with the root's
  namespace declarations. Its token reader was empty, so a `Sequence` or `Parallel` split with it gave nothing. A
  document with a DOCTYPE fails, as with `QueryXPath`.
- **`data.collector.http.request.napOnRetryInMillis`** is the pause between attempts (#23); it was read from the value
  of `data.collector.http.request.retryCount`.
- **A retried request with a body streamed to a file** receives its body (#47): each attempt gets a new handler of the
  file, and the abandoned attempt is cancelled and writes no more. A request in the steps of a request that streams to a
  file reads its own response; it used to take over the outer request's file handler.
- **`convert.utcDateToEpoc(value)`** converts its argument (#30); it returned the epoch second of
  `2020-10-05T00:00:00Z` whatever it was given.
- **`HttpStatus.okCodes()`, `redirectionCodes()`, `clientErrorList()`, `clientErrorCodes()` and
  `serverErrorCodes()`** return their codes (#29); every call threw `ClassCastException`.
- **`ExecutionContext.globalStateIfAbsent`** stores the supplier's value and returns the value held for the key (#44);
  it stored the supplier itself.
- **An `OPTIONS` request is sent as `OPTIONS`** (#27), not as `GET`.
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
