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

RUN mkdir -p /app/src/main/resources/save/youth /app/config \
    && chown -R instagram:instagram /app/src/main/resources/save /app/config /home/instagram

USER instagram

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
