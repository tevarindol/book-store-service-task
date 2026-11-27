FROM docker.io/oven/bun:1-alpine AS frontend

WORKDIR /workspace

COPY package.json bun.lock ./
RUN bun install --frozen-lockfile

COPY src/main/tailwind.css ./src/main/tailwind.css
COPY src/main/resources/templates ./src/main/resources/templates
RUN mkdir -p src/main/resources/static/css && bun run build:css

FROM docker.io/library/gradle:9.9.2-jdk17-alpine AS backend

WORKDIR /workspace
USER root
ENV GRADLE_USER_HOME=/root/.gradle

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

COPY . .
COPY --from=frontend /workspace/src/main/resources/static/css/app.css ./src/main/resources/static/css/app.css

RUN --mount=type=cache,id=gradle-wrapper,target=/root/.gradle/wrapper \
    --mount=type=cache,id=gradle-caches,target=/root/.gradle/caches \
    ./gradlew --no-daemon bootJar

FROM docker.io/library/amazoncorretto:17.0.21-alpine3.24 AS runtime

WORKDIR /app
RUN addgroup -S app && adduser -S -G app app
COPY --from=backend /workspace/build/libs/bookstore-0.0.1.jar ./app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
