# 南坡之窗

南坡之窗是一个 React 19 + Spring Boot 3.5 单仓应用。开发时前后端可分别运行，交付时构建为一个 Java 21 可执行 JAR。整体功能范围和分阶段安排见 [开发计划](docs/DEVELOPMENT_PLAN.md)。

## 当前实现

- Flyway 初始化的完整 V1 核心表结构，本地默认使用 H2 MySQL 兼容模式，`prod` 配置使用 MySQL。
- 统一 API 包装、请求 ID、错误码、分页、OpenAPI 和 SPA 回退。
- 短信码登录、访问/刷新令牌、RBAC 接口边界和鉴权审计。
- 村庄、路线、景点、旅行计划、民宿、体验、农品、农户与已审核溯源记录公开 API。
- 民宿、游玩项目和景点后台 CRUD、排序、发布/下线及公开页即时生效。
- 农户数据归属校验、地块/农品/SKU/生产记录写入，以及提交、驳回、审核发布状态机。
- 统一媒体票据、受限上传、格式/大小/SHA-256 校验和审核员授权查看；本地使用临时目录对象存储。
- 有来源的文案助手：只读取已审核生产记录，保存来源与生成器版本，并要求农户人工确认。
- 订单价格/商品/收款配置快照、幂等创建、人工核款、农户备货、统一发货、完成/取消/退款和状态日志。
- 前端真实 API 数据层、加载/空/错误状态、公开目录、溯源、下单、农户经营台和村庄运营台。

正式上线仍需按开发计划接入短信、对象存储、语音识别、生成模型、TTS、真实收款码和消息通知供应商，并完成 Docker、MySQL、备份恢复、安全压测与真实设备 UAT。仓库中的本地 AI 模板和演示收款配置只用于可重复开发验收，生产 profile 不会注册这些本地适配器。

## 本地开发

需要 Java 21。仅启动后端即可得到带演示数据的内存数据库：

```bash
./mvnw -pl backend spring-boot:run -Dskip.frontend=true
```

前端独立热更时需要 Node 22：

```bash
npm --prefix frontend ci
npm --prefix frontend run dev
```

- 前端开发地址：`http://localhost:3000`
- 后端与 API：`http://localhost:8080`
- 健康检查：`http://localhost:8080/api/health`
- API 文档：`http://localhost:8080/api/docs`
- OpenAPI JSON：`http://localhost:8080/api/openapi`

本地开发短信码默认为 `123456`。种子账号为农户 `13800000001` 和超级管理员 `13800000002`。这些仅用于本地演示；`prod` profile 不会注册本地短信网关，上线前必须实现真实 `SmsGateway`。

本地媒体写入操作系统临时目录下的 `nanpo-window-media`，也可通过 `app.storage.local-root` 覆盖。上传支持 JPEG、PNG、WebP、MP3、WAV、M4A/MP4 和 WebM，并按类型限制为 10MB、30MB 和 100MB。

## 验证与集成构建

```bash
./scripts/verify.sh
java -jar backend/target/nanpo-window-backend-0.1.0-SNAPSHOT.jar
```

`verify.sh` 会使用 Maven Wrapper 下载项目专用 Node，执行 `npm ci`、前端类型检查与生产构建、后端测试和 JAR 打包。构建后通过 `http://localhost:8080` 访问完整应用。

MySQL/OceanBase MySQL 模式部署通过 `DATABASE_URL`、`DATABASE_USERNAME` 和
`DATABASE_PASSWORD` 提供连接信息。项目已包含 OceanBase 的 Flyway 兼容模块，启动时会自动验证并执行数据库迁移。
复制 `.env.example` 为本地 `.env` 后填写密码；`.env` 已被 Git 和 Docker 构建上下文忽略，不要把真实密码写入源码或镜像。

```bash
docker build -t nanpo-window .
docker run --env-file .env -p 8080:8080 \
  -v nanpo-window-media:/data/uploads nanpo-window
```

Docker 构建默认使用阿里云 Maven 公共仓库、npmmirror Node 镜像和 npm registry。需要切换企业仓库时可覆盖构建参数：

```bash
docker build \
  --build-arg MAVEN_MIRROR_URL=https://your-nexus.example.com/repository/maven-public/ \
  --build-arg NODE_DOWNLOAD_ROOT=https://npmmirror.com/mirrors/node/ \
  --build-arg NPM_REGISTRY=https://registry.npmmirror.com \
  -t nanpo-window .
```

当前 `prod` profile 会禁用本地短信、AI 和文件存储适配器；接入正式供应商前，单机验收部署不要设置
`SPRING_PROFILES_ACTIVE=prod`。数据库密码不应写入仓库。
