FROM clojure:alpine

RUN apk update && \
  apk add --no-cache openssl && \
  apk add --no-cache nodejs nodejs-npm && \
  rm -rf /var/cache/apk/*

COPY . /usr/src/checklist

WORKDIR /usr/src/checklist

RUN LEIN_SNAPSHOTS_IN_RELEASE=1 lein do deps, resource, ring uberjar
RUN ln -s $(find /usr/src/checklist/target/uberjar/checklist-*-standalone.jar) checklist.jar

ENV CHECKLIST_ADMIN_USER="admin" \
    CHECKLIST_ADMIN_PASSWORD=""

EXPOSE 5000

CMD ["/usr/bin/java", \
     "-Djavax.net.ssl.trustStore=/usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts", \
     "-jar", "/usr/src/checklist/checklist.jar"]
