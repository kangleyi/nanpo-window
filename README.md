# 乡见西村

乡见西村是一个 React 19 + Spring Boot 3.5 单仓应用。开发时前后端可分别运行，交付时构建为一个 Java 21 可执行 JAR。整体功能范围和分阶段安排见 [开发计划](docs/DEVELOPMENT_PLAN.md)。

## 当前实现

- Flyway 初始化的完整 V1 核心表结构，本地默认使用 H2 MySQL 兼容模式，`prod` 配置使用 MySQL。
- 统一 API 包装、请求 ID、错误码、分页、OpenAPI 和 SPA 回退。
- 短信码登录、访问/刷新令牌、RBAC 接口边界和鉴权审计。
- 村庄、路线、景点、旅行计划、民宿、体验、农品、农户与已审核溯源记录公开 API。
- 民宿、游玩项目和景点后台 CRUD、排序、发布/下线及公开页即时生效。
- 农户数据归属校验、地块/农品/SKU/生产记录写入，以及提交、驳回、审核发布状态机。
- 统一媒体票据、受限上传、格式/大小/SHA-256 校验和审核员授权查看；支持本地文件和腾讯云 COS 两种存储。
- 有来源的文案助手：只读取已审核生产记录，保存来源与生成器版本，并要求农户人工确认。
- 农产品管理表单内嵌 AI 辅助：可就地优化农产品介绍，也可在上传封面后识别图中的可验证信息并回填文案，所有结果由运营人员编辑确认后再保存。
- 订单价格/商品/收款配置快照、幂等创建、人工核款、农户备货、统一发货、完成/取消/退款和状态日志。
- 前端真实 API 数据层、加载/空/错误状态、公开目录、溯源、下单、农户经营台和村庄运营台。

正式上线仍需按开发计划接入短信、语音识别、生成模型、TTS、真实收款码和消息通知供应商，并完成 Docker、MySQL、备份恢复、安全压测与真实设备 UAT。仓库中的本地 AI 模板和演示收款配置只用于可重复开发验收，生产 profile 不会注册这些本地适配器。

## 本地开发

需要 Java 21。先复制 `.env.example` 为 `.env` 并填写 COS 参数；Spring Boot 会自动读取该文件。仅启动后端即可得到带演示数据的内存数据库：

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

本地种子账号为农户 `13800000001` 和超级管理员 `13800000002`，默认密码均为 `12345678`。这些账号仅用于本地演示，正式环境请通过 `LOCAL_ACCOUNT_PASSWORD` 配置安全密码。

开发、Docker 和生产环境默认都使用腾讯云 COS。项目启动时会自动读取根目录下未纳入版本控制的 `.env`；上传支持 JPEG、PNG、WebP、MP3、WAV、M4A/MP4 和 WebM，并按类型限制为 10MB、30MB 和 100MB。只有自动化测试会显式使用本地临时目录，避免测试访问云端。

## 腾讯云 COS

COS 采用浏览器直传：后端生成 5 分钟有效的 PUT 预签名地址，`SecretKey` 不会返回前端；上传完成后，后端通过对象元数据校验文件大小和 SHA-256，再将媒体状态变为 `READY`。媒体读取仍经过本系统鉴权，不会直接公开存储桶对象。

复制 `.env.example` 为 `.env`，填写以下运行时变量：

```dotenv
STORAGE_TYPE=cos
COS_SECRET_ID=AKIDxxxxxxxx
COS_SECRET_KEY=xxxxxxxx
COS_SECRET_TOKEN=
COS_REGION=ap-guangzhou
COS_BUCKET=your-bucket-1250000000
COS_UPLOAD_PREFIX=xiangjian-xicun/uploads
COS_UPLOAD_URL_TTL=PT5M
```

`COS_BUCKET` 必须是带 AppId 的完整名称。使用临时密钥时同时填写 `COS_SECRET_TOKEN`。建议给后端密钥仅授予指定目录的 `PutObject`、`HeadObject` 和 `GetObject` 权限。

由于浏览器会直接请求 COS，需要在存储桶的跨域访问 CORS 中配置实际网站来源（开发环境可增加 `http://localhost:3000`），允许 `PUT` 方法，并允许请求头 `Content-Type`、`x-cos-meta-sha256`。生产环境不要使用 `*` 来源。

## AI 营销文案

超级管理员或内容运营员登录 `/admin` 后，从“农产品管理”进入新增或编辑表单即可使用。“AI 优化当前介绍”会将结果直接回填到介绍输入框；“识图生成并回填”会在上传封面后识别商品名称、分类和卖点并生成介绍。纯文本优化在没有外部 AI 密钥时会自动使用本地合规规则降级；图片识别需要在 Spring 后端运行环境配置：

```dotenv
ZHIPU_API_KEY=your-api-key
ZHIPU_TEXT_MODEL=glm-4-flash-250414
ZHIPU_VISION_MODEL=glm-4v-flash
```

密钥不会返回或下发到浏览器。前端会把 JPG、PNG 或 WebP 图片缩放到最长边 1600 像素再交给后端，压缩后上限为 6MB。后端会校验图片格式和大小、过滤常见绝对化用语，并为每次生成写入操作审计日志。

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
docker run --env-file .env -p 8080:8080 nanpo-window
```

当前默认使用 COS，无需挂载 `/data/uploads` 数据卷。COS 密钥属于运行时参数，不会被 Docker 构建进镜像；仅在离线故障排查时才临时设置 `STORAGE_TYPE=local`。

Docker 构建默认使用阿里云 Maven 公共仓库、npmmirror Node 镜像和 npm registry。需要切换企业仓库时可覆盖构建参数：

```bash
docker build \
  --build-arg MAVEN_MIRROR_URL=https://your-nexus.example.com/repository/maven-public/ \
  --build-arg NODE_DOWNLOAD_ROOT=https://npmmirror.com/mirrors/node/ \
  --build-arg NPM_REGISTRY=https://registry.npmmirror.com \
  -t nanpo-window .
```

当前 `prod` profile 默认启用 COS，并会禁用本地短信和 AI 适配器；接入这些正式供应商前，单机验收部署不要设置
`SPRING_PROFILES_ACTIVE=prod`。数据库密码和 COS 密钥都不应写入仓库。
