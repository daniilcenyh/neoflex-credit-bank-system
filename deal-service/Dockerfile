FROM gradle:8.14.2-jdk24 AS build

ARG NEXUS_URL=http://host.docker.internal:8081/repository/maven-snapshots/
ARG NEXUS_USERNAME=admin
ARG NEXUS_PASSWORD=admin

# Добавьте эти переменные для компиляции
ARG SHOW_SQL=true
ARG FORMAT_SQL=true
ARG HIGHLIGHT_SQL=true
ARG SLOW_SQL_QUERY_INDICATOR_MS=1500
ARG POSTGRES_HOST=deal-postgres
ARG POSTGRES_PORT=5432
ARG POSTGRES_USERNAME=postgres
ARG POSTGRES_PASSWORD=postgres
ARG DB_NAME=deal

ENV NEXUS_URL=${NEXUS_URL}
ENV NEXUS_USERNAME=${NEXUS_USERNAME}
ENV NEXUS_PASSWORD=${NEXUS_PASSWORD}
ENV SHOW_SQL=${SHOW_SQL}
ENV FORMAT_SQL=${FORMAT_SQL}
ENV HIGHLIGHT_SQL=${HIGHLIGHT_SQL}
ENV SLOW_SQL_QUERY_INDICATOR_MS=${SLOW_SQL_QUERY_INDICATOR_MS}
ENV POSTGRES_HOST=${POSTGRES_HOST}
ENV POSTGRES_PORT=${POSTGRES_PORT}
ENV POSTGRES_USERNAME=${POSTGRES_USERNAME}
ENV POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
ENV DB_NAME=${DB_NAME}

WORKDIR /home/gradle/project

COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src ./src
COPY openapi ./openapi

RUN gradle --no-daemon clean build -x test && \
    gradle --no-daemon publish && \
    echo "=== Published deal-api to Nexus successfully ==="

FROM eclipse-temurin:24-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

EXPOSE 8093
CMD ["java", "-jar", "app.jar"]