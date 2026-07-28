# CMHK智能业务办理网站

这是一个从 0 搭建的前后端分离项目基础结构。

## 技术栈

- 前端：Vue 3 + Vite + TypeScript + Axios
- 后端：Spring Boot 3 + Java 21 + MyBatis Plus
- 中间件：Redis
- 数据库：MySQL

## 项目结构

```text
cmhkProject/
  backend/                 Spring Boot 后端服务
  frontend/                Vue3 前端应用
  docker-compose.yml       本地 MySQL 和 Redis
  README.md                项目说明
  .codex/PROJECT_RULES.md  后续开发协作规则
  .codex/PROJECT_STATUS.md 项目推进记录
```

## 开发协作规则

后续开发前请先阅读：

- `.codex/PROJECT_RULES.md`
- `.codex/PROJECT_STATUS.md`

## 环境检查结果

当前机器已检测到：

- Java 21：可用

当前机器未检测到 PATH 命令：

- Git
- Maven
- Gradle
- Node.js
- npm

请先安装或配置上述工具，之后即可运行项目。

## 启动 MySQL 和 Redis

复制 `.env.example` 为 `.env`，并填写自己的本机配置：

```bash
copy .env.example .env
```

```bash
docker compose up -d
```

真实数据库、Redis、Token、密钥等配置不要提交到 Git。

## 启动后端

复制 `backend/src/main/resources/application-local.example.yml` 为 `backend/src/main/resources/application-local.yml`，并填写自己的本机配置。

```bash
cd backend
mvn spring-boot:run
```

后端地址：

- `http://localhost:8080`
- 健康检查：`http://localhost:8080/api/health`
- 业务类型：`http://localhost:8080/api/business-types`

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：

- `http://localhost:5173`
