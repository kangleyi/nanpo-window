# 南坡之窗

同一仓库中的前后端分离项目：前端使用 React + Vite，后端使用 Java 21 + Spring Boot。开发时可分别运行，发布时打包为一个由 Spring Boot 直接提供页面与 API 的可执行 JAR。

整体系统设计、功能范围和分阶段研发安排见 [开发计划文档](docs/DEVELOPMENT_PLAN.md)。

## 本地开发

```bash
npm --prefix frontend install --registry=https://registry.npmmirror.com
npm --prefix frontend run dev
```

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw -pl backend spring-boot:run -Dskip.frontend=true
```

- 前端开发地址：`http://localhost:3000`
- 后端与 API：`http://localhost:8080`
- 健康检查：`http://localhost:8080/api/health`

## 集成构建

```bash
./scripts/verify.sh
java -jar backend/target/nanpo-window-backend-0.1.0-SNAPSHOT.jar
```

集成后通过 `http://localhost:8080` 访问完整页面。Maven 会自动下载项目专用 Node、安装前端依赖、构建静态文件并将其打入后端 JAR。
