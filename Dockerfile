# ── 通用多阶段构建：通过 MODULE 参数构建并打包任一微服务模块 ──
# 在 deploy/ 目录执行（build context 为仓库根）：
#   docker compose build gateway     # MODULE=edu-agent-gateway
#   docker compose build auth        # MODULE=edu-agent-auth
#   docker compose build learning    # MODULE=edu-agent-learning
#   docker compose build resource    # MODULE=edu-agent-resource
#   docker compose build teacher     # MODULE=edu-agent-teacher
#   docker compose build code        # MODULE=edu-agent-code

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
ARG MODULE=edu-agent-auth
# -am 同时构建其依赖（如 edu-agent-common）；-DskipTests 仅打 jar
# --mount=type=cache,target=/root/.m2 让 Maven 本地仓库跨构建复用（6 个服务只全量下载一次公共依赖，断网/慢网下可断点续传）
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp clean package -DskipTests -pl ${MODULE} -am
# 取出可执行 jar（跳过 *.jar.original）
RUN sh -c 'for f in /workspace/${MODULE}/target/*.jar; do case "$f" in *.original) ;; *) mv "$f" /workspace/app.jar ;; esac; done'

FROM eclipse-temurin:17-jre
WORKDIR /app
# SkyWalking agent 由 compose 在启用 skywalking profile 时挂载到 /opt/skywalking/agent
COPY --from=build /workspace/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
