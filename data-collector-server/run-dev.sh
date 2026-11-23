#!/bin/sh

# data-collector:dev with JMX remote on port 9992 for VisualVM: no authentication and no TLS, so only on this machine
docker run -it -p 9990:9990 -p 127.0.0.1:9992:9992 -v $PWD/certs:/opt/dc/certs \
  -e 'JAVA_TOOL_OPTIONS=-Dcom.sun.management.jmxremote.port=9992 -Dcom.sun.management.jmxremote.rmi.port=9992 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.local.only=false -Djava.rmi.server.hostname=localhost' \
  data-collector:dev
