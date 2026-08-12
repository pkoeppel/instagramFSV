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

RUN useradd --system --uid 10001 --create-home --home-dir /home/instagram instagram

COPY --from=build /workspace/target/*.jar /app/app.jar
COPY --from=build /workspace/src/main/resources /app/src/main/resources
COPY docker/defaults/ /app/src/main/resources/

RUN mkdir -p /app/src/main/resources/save/youth /app/fonts /app/config \
    && chown -R instagram:instagram /app /home/instagram

USER instagram

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
