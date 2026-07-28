# CMHK 项目推进记录

本文档用于记录项目已经推进到哪个阶段。Codex 后续继续开发前，应先读取：

1. `.codex/PROJECT_RULES.md`
2. `.codex/PROJECT_STATUS.md`

## 1. 当前阶段

- 阶段：项目基础结构搭建中
- 当前状态：前端已改造为移动端 H5 优先结构，并引入 `vue-router` 实现每次跳转对应独立页面；当前业务只保留“移动套餐办理”；移动套餐办理已具备“套餐选择 -> 确认办理 -> 转人工”的第一版流程；AI 客服已完成前端展示入口和聊天展示页；Maven 已确认可用；MySQL 本机连接已确认，`cmhk` 数据库已创建并写入基础数据；后端接口代码已通过测试；Docker、Redis 暂时先不处理
- 当前补充：已根据渠道政策 HTML 和用户补充的学生优惠图片，重新设计移动套餐数据模型，新增套餐优惠权益表，并把学生 Slash 30GB/50GB 的折实月费、积分、渠道补贴、购机补贴等优惠写入数据库
- 最近确认日期：2026-07-28

## 2. 已完成事项

### 2.1 项目基础结构

已创建根目录结构：

```text
D:\cmhkProject
  backend/
  frontend/
  docker-compose.yml
  README.md
  .codex/
```

### 2.2 后端基础结构

已创建 Spring Boot 后端骨架：

- `backend/pom.xml`
- `backend/src/main/java/com/cmhk/business/CmhkBusinessApplication.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/db/schema.sql`

已创建通用能力：

- `ApiResponse<T>`：统一接口返回结构
- `CorsConfig`：前端跨域配置
- `/api/health`：健康检查接口

已创建第一个业务模块：

- 模块：业务类型 `business_type`
- 接口：`GET /api/business-types`
- 分层：
  - `controller`
  - `service`
  - `service/impl`
  - `mapper`
  - `entity`

### 2.3 前端基础结构

已创建 Vue 3 + Vite + TypeScript 前端骨架：

- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/index.html`
- `frontend/vite.config.ts`
- `frontend/tsconfig.json`
- `frontend/src/main.ts`
- `frontend/src/App.vue`
- `frontend/src/api/http.ts`
- `frontend/src/styles.css`

已完成前端基础页面：

- 左侧导航
- 顶部标题区域
- 业务类型卡片列表
- 调用后端 `/api/business-types`
- 后端不可用时显示提示信息

用户已确认当前前端页面可以打开。此阶段先记录到这里，暂时不继续处理 Docker、Redis 和 MySQL。

2026-07-27 用户明确：页面主要给移动端看。后续前端页面应按移动端 H5 优先设计，当前已有桌面式左侧导航布局需要在后续页面改造中调整为移动端结构。

2026-07-27 已完成移动端 H5 和独立页面路由改造：

- 新增 `vue-router`
- `App.vue` 改为只承载 `RouterView`
- 新增路由表：`frontend/src/router/index.ts`
- 新增独立页面：
  - `frontend/src/views/HomeView.vue`
  - `frontend/src/views/BusinessApplyView.vue`
  - `frontend/src/views/RecordsView.vue`
  - `frontend/src/views/ProfileView.vue`
- 首页从桌面左侧菜单布局改为移动端单列 H5 布局
- 业务入口点击后跳转到独立办理页面 `/business/:code`
- 办理记录页面路径：`/records`
- 客户中心页面路径：`/profile`

已执行验证：

```powershell
cd D:\cmhkProject\frontend
npm.cmd install
npm.cmd run build
```

构建结果：通过。

注意：`npm install` 提示 4 个 high severity vulnerabilities，暂未执行 `npm audit fix --force`，避免强制升级带来破坏性变更。

### 2.4 本地中间件配置

已创建 `docker-compose.yml`：

- MySQL 8.4
- Redis 7.2
- MySQL 初始化脚本挂载到 `schema.sql`

本地中间件真实连接信息不提交，统一放在本机私有配置中：

- 后端真实配置：`backend/src/main/resources/application-local.yml`
- Docker Compose 真实配置：`.env`

当前 `backend/src/main/resources/application.yml` 使用环境变量占位：

```yaml
spring:
  datasource:
    url: ${DB_URL:}
    username: ${DB_USERNAME:}
    password: ${DB_PASSWORD:}
  data:
    redis:
      host: ${REDIS_HOST:}
      port: ${REDIS_PORT:}
      database: ${REDIS_DATABASE:}
      password: ${REDIS_PASSWORD:}
```

`backend/src/main/resources/db/schema.sql` 已包含：

- `CREATE DATABASE IF NOT EXISTS cmhk`
- `USE cmhk`
- `business_type` 表
- 初始业务类型数据

2026-07-27 已通过本机 MySQL 客户端执行 `schema.sql` 成功。

注意：进度文档不记录真实数据库账号、密码或带密码命令。

确认查询结果：

```text
Tables_in_cmhk
business_type

id  code         name          sort_order  enabled
1   MOBILE_PLAN  移动套餐办理  10          1
2   BROADBAND    宽带业务办理  20          1
3   VALUE_ADDED  增值服务办理  30          1
```

2026-07-27 用户要求当前业务只保留“移动套餐办理”。已直接删除数据库中的：

- `BROADBAND`
- `VALUE_ADDED`

确认查询结果：

```text
id  code         name          sort_order  enabled
1   MOBILE_PLAN  移动套餐办理  10          1
```

同步修改：

- `backend/src/main/resources/db/schema.sql` 后续重跑会删除 `BROADBAND` 和 `VALUE_ADDED`
- `frontend/src/views/HomeView.vue` 删除首页说明文案，兜底数据只保留移动套餐办理
- `BusinessTypeController` 只返回启用业务并按 `sortOrder` 排序

已执行 `npm.cmd run build`，构建通过。

2026-07-28 用户补充移动套餐办理需求：

- 先给客户展示套餐选择
- 客户选择套餐后进入确认办理
- 确认办理后转人工
- 页面旁边需要 AI 客服入口
- 点击 AI 客服打开聊天界面
- AI 客服当前只做前端展示，后续网站搭好后再接真实功能

已完成第一版实现：

前端新增/修改：

- `frontend/src/views/BusinessApplyView.vue`：移动套餐选择页
- `frontend/src/views/BusinessConfirmView.vue`：确认办理页
- `frontend/src/views/HumanTransferView.vue`：转人工结果页
- `frontend/src/views/AiChatView.vue`：AI 客服聊天展示页
- `frontend/src/components/AiAssistantButton.vue`：右下角 AI 客服悬浮按钮
- `frontend/src/router/index.ts`：新增确认页、转人工页、AI 聊天页路由
- `frontend/src/api/http.ts`：新增移动套餐和订单接口封装
- `frontend/src/styles.css`：新增套餐卡、流程步骤、转人工、聊天页样式

当前移动端路由：

```text
/#/business/MOBILE_PLAN
/#/business/MOBILE_PLAN/confirm
/#/business/MOBILE_PLAN/transfer
/#/ai-chat
```

后端新增/修改：

- `GET /api/mobile-plans`：查询启用的移动套餐
- `POST /api/mobile-plans/orders`：确认办理并生成转人工订单
- 新增 `mobile` 模块：
  - `controller`
  - `dto`
  - `entity`
  - `mapper`
  - `service`
  - `service/impl`

数据库新增：

- `mobile_plan`：移动套餐表
- `mobile_plan_order`：移动套餐办理订单表

已通过本机 MySQL 执行 `schema.sql`，确认当前表：

```text
business_type
mobile_plan
mobile_plan_order
```

当前套餐数据：

```text
CMHK_5G_128  5G 畅享 128 套餐  128.00  30GB 本地数据
CMHK_5G_198  5G 畅享 198 套餐  198.00  80GB 本地数据
CMHK_5G_298  5G 尊享 298 套餐  298.00  150GB 本地数据
```

验证结果：

```powershell
cd D:\cmhkProject\frontend
npm.cmd run build
```

结果：通过。

2026-07-28 用户提供渠道政策 HTML：

- 文件来源：用户本机微信下载目录中的 `channel-policy-JC-0001-202607 (19).html`
- 已从套餐对比表提取移动套餐字段：
  - 套餐名称
  - 套餐类型
  - 渠道展示价
  - 官方月费
  - 数据权益
  - 本地通话
  - 漫游权益
  - 合约期
  - 优惠截止日期
- 当前仍只纳入移动套餐；HTML 中宽频套餐不纳入当前“移动套餐办理”业务。

已调整数据库和后端实体：

- `mobile_plan` 增加：
  - `plan_type`
  - `channel_price_text`
  - `effective_monthly_fee`
  - `effective_price_text`
  - `official_monthly_fee`
  - `official_price_text`
  - `roaming_benefit`
  - `promotion_end_date`
  - `source_version`
  - `discount_formula`
- 新增 `mobile_plan_offer`：记录套餐附加优惠权益，例如积分、渠道补贴、购机补贴、免行政费、额外数据、社交娱乐数据组合等。
- `mobile_plan_order` 增加套餐快照字段，确认办理时保存当时套餐价格和权益，避免后续套餐改价影响历史订单。

已写入数据库：

```text
mobile_plan enabled=1：14 条
mobile_plan_offer enabled=1：35 条
```

重点学生优惠：

```text
STUDENT_SLASH_30GB_24M  学生 Slash 30GB  HK$98/月   约HK$62/月
STUDENT_SLASH_30GB_12M  学生 Slash 30GB  HK$118/月  约HK$71/月
STUDENT_SLASH_50GB_24M  学生 Slash 50GB  HK$138/月  约HK$102/月
STUDENT_SLASH_50GB_12M  学生 Slash 50GB  HK$158/月  约HK$105/月
```

已同步前端：

- 套餐选择页优先展示折实月费；没有折实月费时展示渠道价
- 套餐卡片展示套餐类型、合约期、数据、通话、漫游/额外权益、优惠截止
- 套餐卡片展示前 4 条优惠权益
- 确认页展示渠道价、折实月费、合约期、优惠截止和折算公式

验证结果：

```powershell
cd D:\cmhkProject\frontend
npm.cmd run build
```

结果：通过。

```powershell
cd D:\cmhkProject\backend
D:\download\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd test
```

结果：通过。

```powershell
cd D:\cmhkProject\backend
mvn.cmd test
```

结果：通过。

2026-07-28 已完成后端配置脱敏：

- `backend/src/main/resources/application.yml` 保留为可提交的公共配置
- 公共配置使用 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_DATABASE`、`REDIS_PASSWORD` 环境变量占位
- 新增 `backend/src/main/resources/application-local.example.yml` 作为示例配置
- 新增 `backend/src/main/resources/application-local.yml` 保存用户本机真实 MySQL 和 Redis 配置
- `.gitignore` 已忽略 `application-local.yml`

当前 `application-local.yml` 不应提交。

2026-07-28 用户明确要求：之后所有真实配置都必须抽出来，统一放入 `backend/src/main/resources/application-local.yml` 管理，不允许写入可提交配置文件。

2026-07-28 用户要求将当前任务拆分为多个独立子任务，由专门代理并行处理，主代理只接收干净摘要结果，保持上下文清晰。

当前拆分为：

- Git 提交代理：检查提交范围、忽略文件、提交风险和 commit message
- 进度读取与更新代理：检查 `.codex/PROJECT_RULES.md` 和 `.codex/PROJECT_STATUS.md` 是否完整
- 任务讨论代理：整理后续业务需求讨论、优先级、接口/表和页面任务

代理摘要结论：

- 暂不建议立刻提交，需先清理 `docker-compose.yml`、`README.md`、`PROJECT_STATUS.md` 中的真实配置痕迹
- 进度文档中早期真实密码和带密码命令需要脱敏
- 下一步业务讨论重点应放在套餐字段、订单字段、转人工状态、订单号和 AI 客服展示范围

已根据摘要完成脱敏整理：

- `docker-compose.yml` 改为读取 `.env`
- 新增 `.env.example`
- `README.md` 不再记录真实连接密码
- `PROJECT_STATUS.md` 不再记录真实数据库密码或带密码命令
- `application.yml` 不保留真实数据库或 Redis 默认值

2026-07-28 用户要求 Git 提交必须规范，commit message 要简洁清楚，并且提交内容语言使用中文。

已写入 `.codex/PROJECT_RULES.md`：

- 使用 Conventional Commits 格式
- commit message 使用中文简洁描述
- 不使用 `更新`、`修复 bug` 等含糊描述
- 提交前检查暂存区和敏感配置
- 一次提交对应一个清晰阶段或明确功能

2026-07-28 用户同意将固定子代理角色规范写入项目。

已新增：

- `.codex/agents/git-agent.md`
- `.codex/agents/progress-agent.md`
- `.codex/agents/discussion-agent.md`

后续创建临时子代理时，应优先读取这些角色规范。代理实例仍然用完即关闭，项目中只保留角色说明文件。

2026-07-28 用户要求在项目中新建一个保留的子代理用于测试。

已新增：

- `.codex/agents/test-agent.md`

测试代理职责：

- 前端构建验证
- 后端测试验证
- 必要时接口验证
- 必要时移动端页面检查
- 输出干净测试摘要，不负责 Git 提交，不输出真实配置，不执行破坏性数据库操作

已同步写入 `.codex/PROJECT_RULES.md` 的多代理协作规定。

2026-07-28 用户要求 Controller 层使用 SLF4J 添加日志。

已完成：

- `BusinessTypeController`：记录业务类型查询开始和查询数量
- `HealthController`：记录健康检查开始、Redis 状态和 Redis 检查失败原因
- `MobilePlanController`：记录套餐查询数量、优惠权益数量、订单创建的订单号、套餐编码和状态

日志注意：

- 不记录客户姓名
- 不记录联系电话
- 不记录数据库、Redis 等真实配置

验证：

```powershell
cd D:\cmhkProject\backend
D:\download\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd test
```

结果：通过。

2026-07-28 用户补充长期要求：之后写 Controller 都要添加 SLF4J 日志。

已写入 `.codex/PROJECT_RULES.md`：

- 新增或修改 Controller 时必须添加关键日志
- 记录接口进入、关键查询数量、关键业务结果
- 不记录客户姓名、联系电话、身份证件、真实配置、Token 等敏感信息
- 使用 SLF4J 占位符写法

### 2.5 项目协作规定

已创建：

- `.codex/PROJECT_RULES.md`

该文件记录用户背景、对话策略、项目结构和开发约定。

## 3. 环境状态

### 3.1 用户本机已确认

用户 PowerShell 已确认：

```powershell
node -v
```

输出过：

```text
v24.18.0
```

用户已在前端目录执行成功：

```powershell
npm.cmd install
npm.cmd run dev
```

Vite 已成功启动，曾输出：

```text
Local: http://localhost:5175/
```

说明：`5173` 和 `5174` 当时被占用，Vite 自动切换到 `5175`。

用户已确认 Maven 可用。直接执行 Maven 完整路径曾成功：

```powershell
D:\download\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd -version
```

输出过：

```text
Apache Maven 3.9.16
Java version: 21.0.12
```

用户之后反馈环境变量已配置完成，Maven 已 OK。

### 3.2 用户遇到过的问题

PowerShell 执行：

```powershell
npm -v
```

曾报错：

```text
无法加载文件 D:\nodejs\npm.ps1，因为在此系统上禁止运行脚本
```

处理策略：

- 优先让用户使用 `npm.cmd`
- 不强制修改 PowerShell 执行策略

推荐命令：

```powershell
cd D:\cmhkProject\frontend
npm.cmd run dev
```

### 3.3 Codex 当前进程限制

Codex 当前工具进程可能暂时读不到用户新安装后的 PATH，曾检测不到：

- `node`
- `npm.cmd`
- `mvn`
- `docker`
- `git`

但这不一定代表用户本机未安装。后续判断环境时，要结合用户自己 PowerShell 的输出。

## 4. 已知未完成事项

- Docker 尚未确认可用
- Git 尚未确认可用
- Spring Boot 后端测试已通过，但尚未作为 Web 服务启动验证
- MySQL 建库脚本已在用户本机执行成功
- Redis 尚未完成本机启动验证
- 前端页面已启动，但后端未启动时会显示“后端服务暂不可用”
- 当前没有登录、用户、订单、业务办理流程等真实业务功能
- 用户已明确表示暂时不管 Docker 和 Redis 能否运行

## 5. 下一步建议

建议下一阶段按以下顺序推进：

1. 保持前端页面可访问
2. 继续细化移动套餐办理字段和页面交互
3. 启动 Spring Boot Web 服务，验证前端真实调用 `/api/mobile-plans`
4. 后续再接入真实 AI 客服能力

## 6. 后续更新规则

每完成一个阶段或重要功能后，应更新本文档：

- 当前阶段
- 已完成事项
- 新增接口
- 新增页面
- 环境变化
- 已知问题
- 下一步建议

不要只在聊天里说明进度，重要进度必须写回本文件。
