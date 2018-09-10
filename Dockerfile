FROM clojure:alpine

RUN apk update && \
  apk add --no-cache openssl && \
  apk add --no-cache nodejs nodejs-npm && \
  rm -rf /var/cache/apk/*

COPY . /usr/src/checklist

WORKDIR /usr/src/checklist

RUN LEIN_SNAPSHOTS_IN_RELEASE=1 lein with-profile uberjar do clean, deps, resource, run, ring uberjar


FROM java:8-alpine

RUN apk --no-cache add ca-certificates

WORKDIR /app/checklist

COPY --from=0 /usr/src/checklist/target/uberjar/checklist-*-standalone.jar checklist.jar

RUN mkdir /var/lib/checklist; chmod a+rwx /var/lib/checklist

ENV CHECKLIST_ADMIN_USER="admin" \
    CHECKLIST_ADMIN_PASSWORD="" \
    CHECKLIST_GITHUB_CLIENT_ID="" \
    CHECKLIST_GITHUB_SECRET="" \
    CHECKLIST_DOMAIN="" \
    CHECKLIST_DATABASE_URI="" \
    CHECKLIST_SESSION_KEY=""

EXPOSE 8080

CMD ["/usr/bin/java", \
     "-Djavax.net.ssl.trustStore=/usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts", \
     "-jar", "/app/checklist/checklist.jar"]
