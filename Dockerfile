FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

ARG NPM_REGISTRY=https://registry.npmmirror.com
ARG NODE_DOWNLOAD_ROOT=https://npmmirror.com/mirrors/node/

COPY . .
RUN mvn -B -pl backend -am clean package -DskipTests \
    -Dnpm.registry="${NPM_REGISTRY}" \
    -Dnode.download.root="${NODE_DOWNLOAD_ROOT}"

FROM eclipse-temurin:21-jre AS runner

WORKDIR /app

ENV PORT=8080 \
    APP_STORAGE_LOCAL_ROOT=/data/uploads

COPY --from=builder /workspace/backend/target/nanpo-window-backend-0.1.0-SNAPSHOT.jar /app/app.jar

RUN mkdir -p /data/uploads

VOLUME ["/data/uploads"]

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
