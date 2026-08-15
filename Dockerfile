FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

ENV TZ=Europe/Berlin \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.awt.headless=true"

WORKDIR /app

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        fontconfig \
        fonts-dejavu-core \
        fonts-liberation2 \
        fonts-noto-core && \
    rm -rf /var/lib/apt/lists/*

# Optional: Microsoft Core Fonts (Arial, Times New Roman, Comic Sans etc.).
# Kann fehlschlagen, wenn der Download von SourceForge blockiert ist.
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        cabextract wget && \
    echo ttf-mscorefonts-installer msttcorefonts/accepted-mscorefonts-eula select true | debconf-set-selections && \
    (apt-get install -y ttf-mscorefonts-installer || true) && \
    rm -rf /var/lib/apt/lists/*

RUN useradd --system --uid 10001 --create-home --home-dir /home/instagram instagram

COPY --from=build --chown=instagram:instagram /workspace/target/*.jar /app/app.jar
COPY --from=build --chown=instagram:instagram /workspace/src/main/resources /app/src/main/resources
COPY --chown=instagram:instagram docker/defaults/ /app/src/main/resources/
COPY --chown=instagram:instagram fonts/ /app/fonts/

# Reference copy of static resources. /app/src/main/resources is often a
# Docker volume, so image updates would otherwise be hidden by stale volume
# contents (e.g. old template formats). The entrypoint re-syncs these on start.
RUN mkdir -p /app/.image/resources
COPY --from=build --chown=instagram:instagram /workspace/src/main/resources/pictures/template/ /app/.image/resources/pictures/template/
COPY --from=build --chown=instagram:instagram /workspace/src/main/resources/pictures/sponsor/ /app/.image/resources/pictures/sponsor/
COPY --from=build --chown=instagram:instagram /workspace/src/main/resources/fonts/ /app/.image/resources/fonts/
COPY --from=build --chown=instagram:instagram /workspace/src/main/resources/static/ /app/.image/resources/static/
COPY --from=build --chown=instagram:instagram /workspace/src/main/resources/application.properties /app/.image/resources/application.properties

RUN cat > /app/entrypoint.sh <<'EOF'
#!/bin/sh
set -e

# Sync static resources from the image reference into the runtime directory.
# /app/src/main/resources may be a Docker volume; image updates would otherwise
# be hidden by stale volume contents (e.g. old JPG templates vs new PNG ones).
IMAGE_RES="/app/.image/resources"
RUNTIME_RES="/app/src/main/resources"

if [ -d "$IMAGE_RES" ]; then
    # Fully replace picture templates so format/name changes always win.
    for dir in pictures/template; do
        if [ -d "$IMAGE_RES/$dir" ]; then
            rm -rf "$RUNTIME_RES/$dir"
            mkdir -p "$RUNTIME_RES/$dir"
            cp -rT "$IMAGE_RES/$dir" "$RUNTIME_RES/$dir"
        fi
    done

    # Additively sync other static assets; existing files are overwritten by name
    # but files that are no longer in the image are not removed.
    for dir in pictures/sponsor fonts static; do
        if [ -d "$IMAGE_RES/$dir" ]; then
            mkdir -p "$RUNTIME_RES/$dir"
            cp -rT "$IMAGE_RES/$dir" "$RUNTIME_RES/$dir"
        fi
    done

    cp -f "$IMAGE_RES/application.properties" "$RUNTIME_RES/application.properties" 2>/dev/null || true
fi

# Ensure directories the application writes to exist.
mkdir -p "$RUNTIME_RES/save/youth" "$RUNTIME_RES/templates"

exec java $JAVA_OPTS -jar /app/app.jar
EOF
RUN sed -i 's/\r$//' /app/entrypoint.sh && chmod +x /app/entrypoint.sh && chown instagram:instagram /app/entrypoint.sh

RUN mkdir -p /app/src/main/resources/save/youth /app/config \
    && chown -R instagram:instagram /app/src/main/resources/save /app/config /home/instagram

USER instagram

EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]
