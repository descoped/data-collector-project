# The image of the data collector server, in three variants. It is made of what Maven has built, so build the reactor
# first; the context is this directory:
#
#   ./mvnw package -DskipTests
#   docker build -t data-collector data-collector-server                                 # the regular image, with a shell
#   docker build -t data-collector:distroless --target distroless data-collector-server
#   docker build -t data-collector:debug --target debug data-collector-server
#
# The regular image (the last stage, built when no target is named) has a shell and the small tools of busybox, for
# looking around in a running container. The distroless image has no program but java, for production, and above all
# where no shell is ever used. The debug image is the regular one with the tools of the JDK for diagnostics: remote
# JMX, a remote debugger, jcmd, jstack, jmap, jstat, jhsdb, JFR and heap dumps. None of them is on: they are turned on
# with JAVA_TOOL_OPTIONS. All three hold the same server and the same configuration.
#
# The server listens on 9990 (9992 is for JMX, when it is turned on). It reads /conf/application.properties, the
# certificates in /certs, and writes rawdata under /rawdata with the filesystem provider. Options of the JVM, system
# properties (-Dhttp.proxyHost=..., -Dlog4j2.configurationFile=...) included, are given in JAVA_TOOL_OPTIONS.
#
# The JDK is 17, the baseline of dc (see .claude/CLAUDE.md), and is raised by hand. The bases are Google's distroless
# images of Debian 13 (glibc, which the native libraries of jffi and lmdbjava need; CA certificates, time zones). Every
# image the build starts from is named by its digest, so a build of the same commit gives the same image (with
# SOURCE_DATE_EPOCH and rewrite-timestamp, as the release does); Renovate moves the digests.

# No shell, no package manager, no program.
FROM gcr.io/distroless/base-nossl-debian13:latest@sha256:3a2dbf10c2fdafe5865e2b03a4a3b9b23f2e280229a81730415379b76dc70592 AS base-distroless

# The same, with busybox in /busybox: sh, wget, ls, ps, top, vi, ...
FROM gcr.io/distroless/base-nossl-debian13:debug@sha256:2548abf86d3e319cdfd0184dce7417a61087724c94ca29daf9b4bddba677f5f5 AS base-shell

# The server's jars, split into a module path and a class path the way Maven Surefire runs the tests, and the Java
# runtimes. jdeps finds the modules of the JDK that dc and its libraries use, in the classes of every jar taken out
# into one directory (jdeps cannot resolve the jars as modules: some have no valid module name, some require a module
# statically that is not there), and the modules of the module path require more (methanol jdk.httpserver), which
# their descriptors name; jlink links them all. Modules are added that no class names: jdk.crypto.ec (the elliptic
# curves of TLS, a provider in Java 17), java.instrument (the byte-buddy agent, -javaagent, that ContentStoreAgent
# needs), jdk.charsets (character sets a source may name that java.base lacks) and jdk.localedata. /jre, of the regular
# and the distroless image, has no debug information, and of the archives of classes (CDS) that jlink writes only the
# one of the default settings of the JVM. /jre-debug, of the debug image, keeps the debug information and has the
# modules of diagnostics.
FROM eclipse-temurin:17.0.21_9-jdk-noble@sha256:e7984fb69c35910b108a5070b30631c8d350142317b91a5f57313d753fd69987 AS runtime

COPY target/dependency/ /dc/lib/
COPY target/data-collector-*.jar /dc/lib/
COPY docker/SplitModulePath.java docker/java.args /build/

# jdeps writes its warnings before the list of modules, its last line. pipefail and the test make a failure of jdeps
# stop the build with its own message. The agent gets a name without its version, so java.args can name it.
SHELL ["/bin/bash", "-c"]
RUN set -o pipefail \
    && mkdir /classes \
    && (cd /classes && for jar in /dc/lib/*.jar; do jar xf "${jar}"; done) \
    && find /classes -name module-info.class -delete \
    && rm -rf /classes/META-INF \
    && modules="$(jdeps --ignore-missing-deps --print-module-deps /classes | tail -n 1)" \
    && rm -rf /classes \
    && echo "jdeps: ${modules}" \
    && [ -n "${modules}" ] \
    && java /build/SplitModulePath.java /dc/lib /dc/classpath io.descoped.dc.server \
    && agent="$(find /dc -name 'byte-buddy-agent-*.jar')" \
    && [ -n "${agent}" ] \
    && dir="$(dirname "${agent}")" \
    && mv "${agent}" "${dir}/byte-buddy-agent.jar" \
    && sed "s|@AGENT@|/opt/dc${dir#/dc}/byte-buddy-agent.jar|" /build/java.args > /dc/java.args \
    && jdk="$(java --list-modules | sed 's/@.*//')" \
    && required="$(for jar in /dc/lib/*.jar; do jar --describe-module --file "${jar}"; done \
            | awk '$1 == "requires" {print $2}' | sort -u | command grep -x -F "${jdk}" | paste -s -d, -)" \
    && echo "required by the module path: ${required}" \
    && modules="${modules},${required}" \
    && jlink --add-modules "${modules},jdk.crypto.ec,java.instrument,jdk.charsets,jdk.localedata" \
            --strip-debug --no-man-pages --no-header-files --generate-cds-archive --output /jre \
    && rm -f /jre/lib/server/classes_no*.jsa \
    && /jre/bin/java -Xshare:on --list-modules > /dev/null \
    && jlink --add-modules "${modules},jdk.crypto.ec,java.instrument,jdk.charsets,jdk.localedata,java.management.rmi,jdk.attach,jdk.hotspot.agent,jdk.jcmd,jdk.jdi,jdk.jdwp.agent,jdk.jfr,jdk.jstatd,jdk.management,jdk.management.agent,jdk.management.jfr" \
            --no-man-pages --no-header-files --generate-cds-archive --output /jre-debug \
    && rm -f /jre-debug/lib/server/classes_no*.jsa \
    && /jre-debug/bin/jcmd -h > /dev/null

# The users of the bases, and dc (10001), without a password and with no shell to log in to: the bases have no
# adduser. The two bases have the same users. The server's home and /rawdata belong to dc and are writable by the group
# root too, so any --user with group 0 works.
COPY --from=base-distroless /etc/passwd /etc/group /users/
RUN echo 'dc:x:10001:10001:dc:/opt/dc:/sbin/nologin' >> /users/passwd \
    && echo 'dc:x:10001:' >> /users/group \
    && mkdir -p /dirs/rawdata /dirs/conf /dirs/certs \
    && chmod 775 /dirs/rawdata

# dc: the jars, the arguments of java, the logstash configuration of log4j2, and the users.
FROM scratch AS app

COPY --from=runtime /users/passwd /users/group /etc/
COPY --from=runtime --chown=10001:0 --chmod=775 /dc/ /opt/dc/
COPY --from=runtime --chown=10001:0 /dirs/ /
COPY target/classes/log4j2-logstash.xml /opt/dc/

# The three images. They differ in their base and their runtime; the rest is the same in each, and is kept the same.
#
# java is the process of the container, so that the signal that stops the container reaches it.

FROM base-distroless AS distroless

ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
ENV DC_HOME=/opt/dc
COPY --from=runtime /jre ${JAVA_HOME}
COPY --from=app / /
WORKDIR /opt/dc
USER 10001
VOLUME ["/conf", "/certs"]
EXPOSE 9990 9992
ENTRYPOINT ["java", "@/opt/dc/java.args"]

FROM base-shell AS debug

ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/busybox"
ENV DC_HOME=/opt/dc
COPY --from=runtime /jre-debug ${JAVA_HOME}
COPY --from=app / /
WORKDIR /opt/dc
USER 10001
VOLUME ["/conf", "/certs"]
EXPOSE 9990 9992
ENTRYPOINT ["java", "@/opt/dc/java.args"]

FROM base-shell AS regular

ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/busybox"
ENV DC_HOME=/opt/dc
COPY --from=runtime /jre ${JAVA_HOME}
COPY --from=app / /
WORKDIR /opt/dc
USER 10001
VOLUME ["/conf", "/certs"]
EXPOSE 9990 9992
ENTRYPOINT ["java", "@/opt/dc/java.args"]
