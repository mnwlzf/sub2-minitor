# sub2-monitor-platform

`sub2-monitor` 是用于监控 `sub2api` / `newapi` 服务账号余额、分组、倍率、连通性和权重变更的管理平台。

## 技术栈

- 后端：Spring Boot 3、JDK 21、MyBatis-Plus、PostgreSQL、Flyway
- 前端：Vue 3、Vite、TypeScript
- 数据库：PostgreSQL 16

## 快速启动

```bash
cp .env.example .env
# 修改 .env 中的必填外部配置后启动
docker compose up
```

首次启动会自动创建 `sub2_monitor` 数据库并执行 Flyway 初始化脚本。

访问：

- 前端：http://localhost:8080
- 后端：http://localhost:8080/actuator/health

Docker 镜像默认使用：

- `ghcr.io/mnwlzf/sub2-minitor:master`

部署时需要通过 `.env` 或宿主机环境变量注入：

- `POSTGRES_PASSWORD`
- `APP_MAIN_DB_PASSWORD`
- `SUB2API_DB_URL`
- `SUB2API_DB_USERNAME`
- `SUB2API_DB_PASSWORD`
- `SPRING_AI_OPENAI_API_KEY`

## 本地开发

项目支持两种启动方式：

- 单体模式：后端直接托管前端静态资源，访问 `http://localhost:8080`
- 前后端分离模式：后端 `8080`，前端 Vite `5173`，前端通过 `/api` 代理到后端

单体模式构建：

```bash
./mvnw -Pwith-web -DskipTests package
java -jar target/sub2-monitor-1.0.0-SNAPSHOT.jar
```

前后端分离模式：

```bash
./mvnw spring-boot:run
```

```bash
cd web
npm install
npm run dev
```

配置文件：

- 仓库中的 `src/main/resources/application.yaml` 为可提交的公开模板，不包含本地私有配置
- 本地私有配置请写入 `src/main/resources/application-local.yml`
- `application-local.yml` 已加入 `.gitignore`，不会被提交到 GitHub
- 如需覆盖数据库或 OpenAI 配置，优先修改本地 `application-local.yml`，或通过环境变量注入

常用环境变量：

- `APP_MAIN_DB_URL`
- `APP_MAIN_DB_USERNAME`
- `APP_MAIN_DB_PASSWORD`
- `SUB2API_DB_URL`
- `SUB2API_DB_USERNAME`
- `SUB2API_DB_PASSWORD`
- `SPRING_AI_OPENAI_BASE_URL`
- `SPRING_AI_OPENAI_API_KEY`

后端单独启动：

```bash
mvn spring-boot:run
```

前端单独启动：

```bash
cd web
npm install
npm run dev
```

## 当前已搭建模块

- 平台管理：`/api/platforms`
- 账号管理：`/api/accounts`
- 配置管理：`/api/configs`
- 历史邮件：`/api/mail-logs`
- 数据保留：默认 15 天，超期物理删除
- 重试配置：默认 3 次，每次间隔 5 秒

## 后续待实现

- 真实采集器：余额、分组、倍率、连通性
- 邮件汇总发送
- 自动权重计算与执行
- 图表 cron 粒度映射
- Shadcn-vue 组件替换基础样式

平台表 需要 id basr_url 名称 充值金额 到账金额

账户表 需要 id username password platform create_time 表名 accounts

余额变化表 需要 id account_id platform 当前总余额 创建时间 表名你帮我起

倍率变化表 需要 id platform 渠道名 当前倍率 创建时间

smtp 配置表 id smtp主机字段 smtp端口字段 smtp 用户字段 smtp 密码字段 发件人邮箱字段 发件人名称字段 是否使用 ssl 字段

配置表 id  
