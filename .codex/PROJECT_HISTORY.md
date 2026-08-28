# CMHK 项目历史推进记录

本文档记录项目历史过程、日期流水、详细修改内容和历史验证结果。当前有效状态见 `.codex/PROJECT_STATUS.md`。

追加规则：

- 按日期追加，新的记录放在对应日期下方。
- 保留历史事实和测试结果，但不要记录真实数据库密码、Redis 密码、Token 等敏感信息。
- 已失效的信息可以作为历史保留，但当前状态必须以 `PROJECT_STATUS.md` 为准。

## 2026-08-28

### V1 改造阶段 P2：客户、订单与产品标准化

- 为客户主档增加内部负责人，新增客户跟进记录表和管理端录入、查询功能；跟进记录只保存业务沟通摘要，不保存正式身份资料。
- 将订单办理状态统一为 `PENDING`、`FOLLOWING`、`SUBMITTED_UMALL`、`UNDER_REVIEW`、`NEED_SUPPLEMENT`、`WAITING_ACTIVATION`、`ACTIVATED`、`COMPLETED`、`AFTER_SALES`、`CANCELLED`。
- 管理端订单状态由自由文本改为受控选项；H5转人工订单改为 `FOLLOWING`，新建及每次状态变化均写状态历史。
- 新增 `order_status_history`，分别记录JOINCOM、UMALL原始、审核、补件、激活和合约状态；管理端订单页面可查看完整历史。
- `mobile_plan_order` 增加 `review_status`、`supplement_status`、`status_updated_at`；UMALL原始状态继续独立保存在 `umall_status`。
- CMHK导入增加集中状态映射器。无法识别的状态将对账行标记为 `STATUS_EXCEPTION`，不覆盖订单状态；原有SHA-256文件防重保持不变。
- 新增套餐、套餐权益、渠道产品政策管理接口和管理端页面；产品写操作仅ADMIN可用，套餐删除按下架处理，历史订单快照不回写。
- 新增P2预检、V002迁移、完成后核验和回滚兼容说明；同步维护全新安装使用的 `schema.sql`。
- 迁移前完整备份真实库到 `D:\download\cmhk_before_p2_2026-08-28.sql`，随后执行V002；未删除或清空真实数据。
- 142个历史订单旧状态完成标准化：转人工/待寄出映射为 `FOLLOWING`，已寄出映射为 `SUBMITTED_UMALL`，待激活、已激活、已完成映射为对应标准状态。
- 迁移后状态历史142、缺失历史0、非标准状态0、客户跟进悬空0、渠道产品政策悬空0、状态导入异常0。
- 后端最终测试28项：26项通过、0项失败、2项按配置跳过；两个前端类型检查和生产构建均通过。
- 本阶段未启动长期后端或前端进程，没有占用8080端口；未执行Git提交。

## 2026-08-27

### 统一渠道主档、佣金渠道关系与后台角色权限

- 完成P1统一渠道与权限改造，没有提前实施P2订单状态、产品管理或后续阶段功能。
- 新增P1预检查、`V001__unify_channels_and_admin_permissions.sql`、执行后验证和回滚兼容说明；同步维护全新安装用`schema.sql`。
- 迁移前备份真实`cmhk`数据库到`D:\download\cmhk_before_p1_2026-08-27.sql`，随后执行V001。
- `channel`新增渠道类型、父级、联系人、合作状态、结算信息和负责人；新增旧渠道映射表及迁移异常表。
- 旧`secondary_channel`保留只读兼容，写接口停用；管理端渠道档案和客户渠道选项统一读取`channel`。
- 渠道结算改为关联统一`channel.id`，并在生成记录时验证订单客户的渠道归属，规则快照和历史金额不变。
- 真实库旧二级渠道、佣金规则和佣金记录均为0，因此本次没有渠道编码冲突或佣金ID回填；统一渠道迁移前后均为4条。
- 按绑定表为业务事实修正1条`customer.channel_id`空值；迁移后234条客户与234条唯一绑定不存在不一致或悬空关系。
- `admin_user`新增数据范围字段，现有管理员迁移为`ADMIN + ALL`；令牌Principal包含用户ID、账号、角色、范围类型和范围ID。
- 集中权限策略区分401和403：用户管理、渠道写入、佣金规则、金额修正及结算确认仅ADMIN可用，OPERATOR保留日常业务权限。
- CHANNEL范围只允许访问已完成行级过滤的客户、订单、渠道和结算接口，V1不建设渠道或CMHK独立登录门户。
- 管理端新增角色、数据范围和指定渠道维护，菜单及规则、结算按钮按权限显示。
- 后端最终测试22项：20项通过、0项失败、2项按环境跳过；两个前端类型检查与生产构建均通过。
- 使用临时18080端口完成真实登录和鉴权联调：无令牌401，登录成功后首页及统一渠道接口200；联调进程已关闭且端口释放。
- 使用临时15174端口检查管理端登录页实际渲染，标题、品牌文案和登录表单正常，浏览器控制台无错误；未保留临时进程。
- 本阶段按用户要求未执行Git提交。

### 管理端宽屏与笔记本表格适配

- ICCID 卡池、客户管理和订单管理各自增加表格内横向滚动区域，并保留业务列最小可读宽度。
- 移除这三张宽表的固定操作列，避免笔记本窄内容区被固定列进一步挤占；操作列随表格横向滚动。
- 在 1440px 以下收紧内容区、顶部留白和筛选控件宽度；不改变接口、数据或业务规则。
- 根据笔记本实际反馈，客户管理列表移除“联系方式”，订单管理列表移除“手机号”，并将两表合计列宽收紧到 1100px。
- ICCID 卡池“分配时间”和客户管理“创建时间”统一将秒或毫秒时间戳显示为本地日期时间；同时收窄 ICCID 卡池、订单管理的“内部订单号”列。
- 管理端 TypeScript 检查与生产构建通过；未使用管理员凭据登录浏览器，因此没有对需鉴权的页面执行实际数据加载检查。

### V1 改造阶段 P0：改造安全基线

- 完整读取项目规则、状态和 `docs/JOINCOM-CMHK-V1-REFORM-PLAN.md`，按要求只实施 P0，没有提前开发 P1 及后续阶段。
- 建立 `backend/src/main/resources/db/migrations/`，新增迁移执行规则、P0 只读预检、完成后核验及回滚兼容说明；现阶段采用人工审核后执行的版本化 SQL，不引入自动迁移工具。
- 将 `schema.sql` 整理为仅用于全新安装的最终结构，移除历史增量存储过程和 `ALTER/CALL` 逻辑，补齐客户备份、订单来源和 ICCID 生命周期结构。
- 通过只读元数据比较确认：`schema.sql` 与真实库均为 20 张表，逐表字段集合差异为 0。
- H5 移除证件号码输入和 `idNo` 请求字段；后端请求 DTO 与订单创建 Service 不再接收或写入该字段。
- `mobile_plan_order.id_no` 数据库列继续保留；订单实体通过 Jackson 忽略配置禁止接口请求写入和响应输出，同时避免新 Redis 缓存及操作日志序列化该字段。
- 未删除或清空真实数据，未执行真实库 DDL/DML。只读统计显示非空 `id_no` 为 0，操作日志中包含 `idNo` 键的记录为 0。
- 数据兼容基线：客户 234、订单 142、ICCID 155、对账行 94、佣金记录 0；关键唯一键重复组和核心悬空关系均为 0。
- P0 预检 20 条 SQL、核验 4 条 SQL 均已通过真实 MySQL 只读连接逐条执行验证。
- 后端完整测试 19 项：17 项通过、0 项失败、2 项按配置跳过；新增 5 项隐私、旧请求和接口响应兼容测试全部通过。
- `frontend` 和 `admin-frontend` TypeScript 检查、生产构建均通过；管理端仅保留既有的大分块构建警告。
- 用户确认后纳入本次 P0 Git 提交。

## 2026-08-25

### CMHK 客户备份只读分析

- 按交接要求读取真实客户备份 JSON，仅做内存解析和脱敏聚合，未写入实际数据库。
- 文件根结构为数组，共 233 条客户记录、42 个顶层字段和 538 条嵌套跟进日志。
- 用户明确导入口径：只要 `onboardDate` 非空即视为已上台，不使用 `stage`、`progress` 或上台号码推断。
- 按该口径共有 132 条已上台记录：96 条标准 20 位 ICCID、33 条 ICCID 为空、3 条 ICCID 非空但格式异常。
- 33 条已上台且缺 ICCID 的记录进入稳定虚拟 ICCID 候选；3 条格式异常记录进入异常列表，不自动覆盖。
- 全文件 ICCID 为空 129 条；非空 ICCID 存在 1 组重复，涉及 2 条记录。
- 132 条已上台记录均有上台号码，且上台号码在文件内不重复。
- 确认 `number` 表示上台号码、`umall` 表示 UMALL 状态；源文件没有结构化客户手机号或 UMALL 订单号。
- 当前表结构的 `customer.phone` 与 `mobile_plan_order.contact_phone` 必填，且 ICCID 表缺少 REAL/VIRTUAL 和 REPLACED 生命周期字段，因此正式导入前必须先确认建模与迁移方案。

## 2026-08-19

### JOINCOM × CMHK 管理后台 MVP

- 新增独立 `admin-frontend/`，采用 Vue 3、Vite、TypeScript 和 Element Plus。
- 按参考后台提取深棕侧栏、JOINCOM 红、浅灰背景、白色卡片和红色表头设计语言。
- 新增管理员独立登录与 `/api/admin/**` 鉴权。
- 实现 ICCID 卡池、客户、订单、CMHK 文件对账、异常匹配、二级渠道规则与结算、首页指标和操作日志。
- 对账导入采用文件哈希防重及“预览—确认”两阶段流程；匹配顺序为 UMALL订单号、ICCID、手机号。
- 二级渠道佣金使用 `BigDecimal`，保存规则与结果快照，并保留人工修正和确认结算。
- 更新数据库结构、README、长期业务规则和当前状态快照。
- 验证：后端 4 项测试通过，管理端生产构建通过，原 H5 类型检查通过，管理端登录页和主框架完成 1280、1440、1920 宽度视觉检查。
- 已在本机 `cmhk` 执行管理后台增量迁移并核查：8 张管理后台表、客户扩展字段和订单扩展字段已存在；迁移脚本已修复 UTF-8 和保留字段兼容性，支持重复执行。

## 2026-07-27

### 项目基础结构

创建根目录结构：

```text
D:\cmhkProject
  backend/
  frontend/
  docker-compose.yml
  README.md
  .codex/
```

创建 Spring Boot 后端骨架：

- `backend/pom.xml`
- `backend/src/main/java/com/cmhk/business/CmhkBusinessApplication.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/db/schema.sql`

创建通用能力：

- `ApiResponse<T>`：统一接口返回结构
- `CorsConfig`：前端跨域配置
- `/api/health`：健康检查接口

创建业务类型模块：

- `GET /api/business-types`
- 分层包括 `controller`、`service`、`service/impl`、`mapper`、`entity`

创建 Vue 3 + Vite + TypeScript 前端骨架：

- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/index.html`
- `frontend/vite.config.ts`
- `frontend/tsconfig.json`
- `frontend/src/main.ts`
- `frontend/src/App.vue`
- `frontend/src/api/http.ts`
- `frontend/src/styles.css`

最初前端页面包含：

- 左侧导航
- 顶部标题区域
- 业务类型卡片列表
- 调用后端 `/api/business-types`
- 后端不可用提示

用户已确认前端页面可以打开。

### 环境确认

用户 PowerShell 确认 Node.js：

```powershell
node -v
```

曾输出：

```text
v24.18.0
```

用户执行 `npm -v` 曾遇到 PowerShell 执行策略拦截：

```text
无法加载文件 D:\nodejs\npm.ps1，因为在此系统上禁止运行脚本
```

处理策略：优先使用 `npm.cmd`。

用户在前端目录执行成功：

```powershell
npm.cmd install
npm.cmd run dev
```

Vite 曾输出：

```text
Local: http://localhost:5175/
```

说明当时 `5173`、`5174` 被占用，Vite 自动切换到 `5175`。

用户确认 Maven 可用，完整路径执行成功：

```powershell
D:\download\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd -version
```

输出过：

```text
Apache Maven 3.9.16
Java version: 21.0.12
```

### 移动端 H5 和独立路由改造

用户明确页面主要给移动端看。随后完成：

- 新增 `vue-router`
- `App.vue` 改为只承载 `RouterView`
- 新增 `frontend/src/router/index.ts`
- 新增独立页面：
  - `frontend/src/views/HomeView.vue`
  - `frontend/src/views/BusinessApplyView.vue`
  - `frontend/src/views/RecordsView.vue`
  - `frontend/src/views/ProfileView.vue`
- 首页从桌面左侧菜单布局改为移动端单列 H5 布局
- 业务入口点击后跳转到独立办理页面 `/business/:code`
- 办理记录页面路径：`/records`
- 客户中心页面路径：`/profile`

验证：

```powershell
cd D:\cmhkProject\frontend
npm.cmd install
npm.cmd run build
```

结果：通过。

当时 `npm install` 提示 4 个 high severity vulnerabilities，未执行 `npm audit fix --force`，避免强制升级带来破坏性变更。

### 本地中间件和 MySQL 初始数据

创建 `docker-compose.yml`：

- MySQL 8.4
- Redis 7.2
- MySQL 初始化脚本挂载到 `schema.sql`

本地中间件真实连接信息不提交，统一放在本机私有配置：

- 后端真实配置：`backend/src/main/resources/application-local.yml`
- Docker Compose 真实配置：`.env`

公共 `application.yml` 改为环境变量占位，不保留真实配置。

通过本机 MySQL 客户端执行 `schema.sql` 成功。确认初始业务类型曾为：

```text
id  code         name          sort_order  enabled
1   MOBILE_PLAN  移动套餐办理  10          1
2   BROADBAND    宽带业务办理  20          1
3   VALUE_ADDED  增值服务办理  30          1
```

用户要求当前业务只保留“移动套餐办理”，随后删除：

- `BROADBAND`
- `VALUE_ADDED`

确认结果：

```text
id  code         name          sort_order  enabled
1   MOBILE_PLAN  移动套餐办理  10          1
```

同步修改：

- `backend/src/main/resources/db/schema.sql` 后续重跑会删除 `BROADBAND` 和 `VALUE_ADDED`
- `frontend/src/views/HomeView.vue` 删除首页说明文案，兜底数据只保留移动套餐办理
- `BusinessTypeController` 只返回启用业务并按 `sortOrder` 排序

验证：

```powershell
cd D:\cmhkProject\frontend
npm.cmd run build
```

结果：通过。

## 2026-07-28

### 移动套餐办理第一版流程

用户补充需求：

- 先给客户展示套餐选择
- 客户选择套餐后进入确认办理
- 确认办理后转人工
- 页面旁边需要 AI 客服入口
- 点击 AI 客服打开聊天界面
- AI 客服当前只做前端展示，后续网站搭好后再接真实功能

前端新增/修改：

- `frontend/src/views/BusinessApplyView.vue`：移动套餐选择页
- `frontend/src/views/BusinessConfirmView.vue`：确认办理页
- `frontend/src/views/HumanTransferView.vue`：转人工结果页
- `frontend/src/views/AiChatView.vue`：AI 客服聊天展示页
- `frontend/src/components/AiAssistantButton.vue`：右下角 AI 客服悬浮按钮
- `frontend/src/router/index.ts`：新增确认页、转人工页、AI 聊天页路由
- `frontend/src/api/http.ts`：新增移动套餐和订单接口封装
- `frontend/src/styles.css`：新增套餐卡、流程步骤、转人工、聊天页样式

当时移动端路由：

```text
/#/business/MOBILE_PLAN
/#/business/MOBILE_PLAN/confirm
/#/business/MOBILE_PLAN/transfer
/#/ai-chat
```

后端新增/修改：

- `GET /api/mobile-plans`：查询启用的移动套餐
- `POST /api/mobile-plans/orders`：确认办理并生成转人工订单
- 新增 `mobile` 模块：`controller`、`dto`、`entity`、`mapper`、`service`、`service/impl`

数据库新增：

- `mobile_plan`
- `mobile_plan_order`

本机 MySQL 执行 `schema.sql` 后确认表：

```text
business_type
mobile_plan
mobile_plan_order
```

最初演示套餐数据：

```text
CMHK_5G_128  5G 畅享 128 套餐  128.00  30GB 本地数据
CMHK_5G_198  5G 畅享 198 套餐  198.00  80GB 本地数据
CMHK_5G_298  5G 尊享 298 套餐  298.00  150GB 本地数据
```

验证：

```powershell
cd D:\cmhkProject\frontend
npm.cmd run build
```

结果：通过。

### 渠道政策套餐模型和数据

用户提供渠道政策 HTML：

```text
channel-policy-JC-0001-202607 (19).html
```

从套餐对比表提取移动套餐字段：

- 套餐名称
- 套餐类型
- 渠道展示价
- 官方月费
- 数据权益
- 本地通话
- 漫游权益
- 合约期
- 优惠截止日期

当时确认：HTML 中宽频套餐不纳入当前“移动套餐办理”业务。

调整数据库和后端实体：

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
- 新增 `mobile_plan_offer`，记录套餐附加优惠权益，例如积分、渠道补贴、购机补贴、免行政费、额外数据、社交娱乐数据组合等。
- `mobile_plan_order` 增加套餐快照字段，确认办理时保存当时套餐价格和权益，避免后续套餐改价影响历史订单。

写入数据库后确认：

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

前端同步：

- 套餐选择页优先展示折实月费；没有折实月费时展示渠道价
- 套餐卡片展示套餐类型、合约期、数据、通话、漫游/额外权益、优惠截止
- 套餐卡片展示前 4 条优惠权益
- 确认页展示渠道价、折实月费、合约期、优惠截止和折算公式

验证：

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

提交：

```text
472d043 feat: 新增移动套餐优惠模型和展示
```

### 配置脱敏和提交规则

完成后端配置脱敏：

- `backend/src/main/resources/application.yml` 保留为可提交公共配置
- 公共配置使用 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_DATABASE`、`REDIS_PASSWORD` 环境变量占位
- 新增 `backend/src/main/resources/application-local.example.yml` 作为示例配置
- 新增 `backend/src/main/resources/application-local.yml` 保存用户本机真实 MySQL 和 Redis 配置
- `.gitignore` 已忽略 `application-local.yml`

用户明确要求：之后所有真实配置都必须抽出来，统一放入 `backend/src/main/resources/application-local.yml` 管理，不允许写入可提交配置文件。

用户要求拆分为多个独立子任务，由专门代理并行处理，主代理只接收干净摘要结果。

当时拆分为：

- Git 提交代理
- 进度读取与更新代理
- 任务讨论代理

代理摘要结论：

- 暂不建议立刻提交，需先清理 `docker-compose.yml`、`README.md`、`PROJECT_STATUS.md` 中的真实配置痕迹
- 进度文档中早期真实密码和带密码命令需要脱敏
- 下一步业务讨论重点应放在套餐字段、订单字段、转人工状态、订单号和 AI 客服展示范围

根据摘要完成脱敏整理：

- `docker-compose.yml` 改为读取 `.env`
- 新增 `.env.example`
- `README.md` 不再记录真实连接密码
- `PROJECT_STATUS.md` 不再记录真实数据库密码或带密码命令
- `application.yml` 不保留真实数据库或 Redis 默认值

用户要求 Git 提交必须规范，commit message 简洁清楚，提交内容语言使用中文。

写入 `.codex/PROJECT_RULES.md`：

- 使用 Conventional Commits 格式
- commit message 使用中文简洁描述
- 不使用 `更新`、`修复 bug` 等含糊描述
- 提交前检查暂存区和敏感配置
- 一次提交对应一个清晰阶段或明确功能

### 固定子代理规范

用户同意将固定子代理角色规范写入项目。

新增：

- `.codex/agents/git-agent.md`
- `.codex/agents/progress-agent.md`
- `.codex/agents/discussion-agent.md`

后续创建临时子代理时，应优先读取这些角色规范。代理实例仍然用完即关闭，项目中只保留角色说明文件。

用户要求在项目中新建一个保留的子代理用于测试。

新增：

- `.codex/agents/test-agent.md`

测试代理职责：

- 前端构建验证
- 后端测试验证
- 必要时接口验证
- 必要时移动端页面检查
- 输出干净测试摘要，不负责 Git 提交，不输出真实配置，不执行破坏性数据库操作

### Controller SLF4J 日志规则

用户要求 Controller 层使用 SLF4J 添加日志。

完成：

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

用户补充长期要求：之后写 Controller 都要添加 SLF4J 日志。已写入 `.codex/PROJECT_RULES.md`。

### 移动套餐 Redis 缓存

用户要求套餐数据使用 Redis 缓存，减轻数据库压力，并实现缓存击穿、缓存穿透的通用复用能力。

新增：

- `backend/src/main/java/com/cmhk/business/common/cache/CacheClient.java`

通用缓存能力：

- 先查 Redis，命中直接返回
- Redis 未命中时回源数据库并写入 Redis
- 使用空值缓存防止缓存穿透
- 使用 Redis 互斥锁防止缓存击穿
- 缓存 TTL 增加随机抖动，降低大量 key 同时过期风险
- Redis 异常时记录 warn 日志并降级查询数据库，避免接口直接失败

接入：

- `MobilePlanServiceImpl.listEnabledPlansWithOffers()`
- Redis key：`cmhk:mobile-plan:list:enabled`
- 套餐列表缓存时间：30 分钟
- 空值缓存时间：2 分钟

验证：

```powershell
cd D:\cmhkProject\backend
D:\download\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd test
```

结果：通过。

注意：当时只完成代码层面接入和单测编译验证，尚未启动 Redis 服务做真实接口缓存命中验证。

提交：

```text
565e492 feat: 新增移动套餐 Redis 缓存
```

### 移动套餐订单确认信息

开始完善移动套餐订单确认信息。

设计结论：

- `customer_identity` 表示客户身份：
  - `0`：自营客户
  - `1`：留学生
- 订单表同时保存套餐关联和套餐快照：
  - `plan_id` / `plan_code` 用于关联套餐和后续统计
  - 套餐名称、价格、权益、折算公式等快照用于历史展示和对账

调整数据库：

- `mobile_plan_order.plan_id`
- `mobile_plan_order.customer_identity`
- `mobile_plan_order.id_type`
- `mobile_plan_order.id_no`
- `mobile_plan_order.referrer_phone`
- `mobile_plan_order.preferred_contact_time`
- 新增 `idx_mobile_plan_order_plan_id` 索引

调整后端：

- `MobilePlanOrderCreateRequest` 增加客户身份、证件、推荐人、方便联系时间字段
- `customer_identity` 增加 0 到 1 的基础校验
- `MobilePlanOrder` 增加对应字段
- `MobilePlanOrderServiceImpl` 创建订单时写入 `plan_id`、客户身份和办理信息

调整前端：

- `BusinessConfirmView.vue` 增加客户身份、证件类型、证件号码、推荐人号码、方便联系时间
- 学生套餐默认客户身份为留学生
- 普通套餐默认客户身份为自营客户
- `frontend/src/api/http.ts` 同步请求和订单类型字段

用户补充办理信息字段：

- 目前是否有 offer？
- 目前是否有通行证 / HKID？
- 预计什么时候开始使用？

新增数据库字段：

- `mobile_plan_order.has_offer`
- `mobile_plan_order.has_pass_or_hkid`
- `mobile_plan_order.expected_start_date`

显示规则：

- 只有客户身份选择“留学生”时，显示 offer 选项
- 通行证 / HKID 选项对所有客户身份显示

用户纠正字段口径：字段名称使用 `offer`，不是 `offer+`。

删除错误字段：

- `mobile_plan_order.has_offer_plus_or_hkid`
- `mobile_plan_order.has_offer_plus`

保留字段：

- `mobile_plan_order.has_offer`
- `mobile_plan_order.has_pass_or_hkid`
- `mobile_plan_order.expected_start_date`

已执行数据库脚本，确认 `mobile_plan_order` 存在正确新增字段，错误字段已删除。

验证：

```powershell
cd D:\cmhkProject\backend
D:\download\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd test
```

结果：通过。

```powershell
cd D:\cmhkProject\frontend
npm.cmd run build
```

结果：通过。

### 确认页 URL 传参修正

修正确认页 URL 传参方式：

- 套餐选择页跳转确认页时，URL 只传 `planCode`
- 确认页进入后调用 `GET /api/mobile-plans/{planCode}` 获取后端真实套餐详情
- 确认页不再从 URL 读取套餐名称、价格、权益、合约期、折算公式等业务数据
- 转人工页 URL 只保留 `orderNo`，不再携带联系电话
- 前端提交订单时，预计开始日期为空则不传该字段，避免后端 `LocalDate` 解析空字符串失败

新增后端接口：

```text
GET /api/mobile-plans/{planCode}
```

设计原因：

- URL 只作为页面定位和最小标识，不承载套餐价格等可被用户篡改的业务数据
- 套餐详情统一从后端读取，后端可继续复用 Redis 套餐缓存
- 确认办理时后端仍按 `planCode` 查询真实套餐并生成订单快照

验证：

```powershell
cd D:\cmhkProject\backend
D:\download\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd test
```

结果：通过，`BUILD SUCCESS`。

```powershell
cd D:\cmhkProject\frontend
npm.cmd run build
```

结果：通过。

提交：

```text
34983c3 feat: 完善移动套餐确认办理流程
```

### 完成阶段后提交确认规则

用户新增协作规则：

- 当 Codex 判断已经新增或完成一个功能、一个阶段时，需要主动询问用户是否进行一次 Git 提交
- 除非用户明确要求立即提交，否则完成阶段后不要默认直接提交

规则写入 `.codex/PROJECT_RULES.md`，并更新当时的进度文档。

提交：

```text
1f59a04 docs: 新增阶段完成后提交确认规则
```

## 2026-07-29

### 调整项目进度记录方式

用户要求调整项目进度记录方式：

- `.codex/PROJECT_STATUS.md` 改为当前状态快照
- 不在 `PROJECT_STATUS.md` 中持续追加历史过程
- 新建 `.codex/PROJECT_HISTORY.md` 保存日期记录、详细修改内容和历史测试结果
- 修改 `.codex/PROJECT_RULES.md`，明确 STATUS 和 HISTORY 的职责
- 每次任务完成后同时判断是否需要更新 STATUS 和 HISTORY
- 同步更新 `.codex/agents/progress-agent.md`，避免进度代理继续把历史细节追加到 STATUS

## 2026-08-03

### 渠道入口与手机号注册基础

根据新的业务流程，先实现“渠道识别 -> 手机号验证 -> 客户创建或复用 -> 首次渠道绑定”。本阶段不接真实短信服务、不接领功 API，也不改动已有移动套餐订单逻辑。

新增数据库模型：

- `channel`：渠道配置，支持 `elderly_mode`、微信客服链接和二维码配置
- `channel_entry`：二维码或链接入口，使用 `entry_token` 映射内部渠道
- `customer`：手机号唯一的客户主体
- `customer_channel_binding`：只保存客户首次成功进入的渠道和入口
- `phone_verification_code`：验证码哈希、有效期、尝试次数和使用状态

新增后端接口：

- `GET /api/channel-auth/entry?entryToken=xxx`：校验渠道入口
- `POST /api/channel-auth/verification-codes`：发送开发阶段模拟验证码
- `POST /api/channel-auth/phone-login`：验证手机号并创建或复用客户

实现规则：

- 开发阶段模拟验证码固定为 `123456`
- 验证码有效期 5 分钟，同一验证码最多尝试 5 次
- 同一手机号发送验证码间隔 60 秒
- 同一手机号只创建一个客户记录
- 客户首次渠道绑定后，后续从其他渠道进入不会覆盖归属
- Controller 使用 SLF4J 日志，不记录手机号或验证码

前端新增：

- `frontend/src/views/ChannelAuthView.vue`：独立手机号验证页
- 首页检测 `entry_token` 后自动跳转注册页
- 演示入口：`DEMO-ENTRY-001`
- 长者关怀演示入口：`ELDERLY-ENTRY-001`，启用大字体和更大操作区

验证：

```powershell
cd D:\cmhkProject\backend
D:\download\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd test

cd D:\cmhkProject\frontend
npm.cmd run build
```

结果：后端 Maven 测试通过，前端 Vite 生产构建通过。

当时待处理：当前环境未发现可用的 `mysql` 或 Docker 命令行，`schema.sql` 的新增表尚未直接同步到实际 MySQL；该问题已在下方“实际 MySQL 同步与接口联调”记录中解决。

## 2026-08-26：CMHK 客户备份模拟导入实际数据库

### 管理后台接入 Redis 缓存

- 复用 C 端通用 `CacheClient`，缓存管理端首页指标、客户、订单、ICCID 等高频只读数据；MySQL 继续作为唯一真实数据源。
- 新增管理端缓存命名空间和 SHA-256 查询条件摘要，避免手机号等敏感查询值直接写入 Redis 键。
- 新增命名空间版本失效机制；写事务提交成功后提升缓存版本，规避事务提交前清理造成的旧数据回填。
- 客户备份确认导入会统一失效客户、渠道、订单、ICCID 和首页缓存；对账与结算操作同步失效其影响的首页、订单和客户缓存。
- 修复 Redis 首次读取异常时不能自动回源的问题，Redis 故障时管理后台继续查询 MySQL。
- 测试：缓存单元测试 3 项通过；真实 Redis 联调 1 项通过；完整后端测试 14 项中 12 项通过、0 项失败、2 项条件测试默认跳过。

- 读取并遵循“有上台日期就是上台”的唯一判断口径。
- 新增客户备份只读模拟接口，真实文件模拟结果为：233 个客户候选、233 个订单候选、131 张 ICCID 候选、6 条异常。
- 新增文件 SHA-256 预览确认机制；确认时必须使用同一文件摘要，并禁止同一文件重复确认。
- 为 `customer`、`mobile_plan_order`、`iccid_inventory` 补充来源唯一键、渠道来源、上台日期、UMALL 状态、REAL/VIRTUAL 类型及替换链字段。
- 新增导入批次和明细留痕表，异常只保存来源行、来源 ID、异常码和原因，不保存整行敏感原文。
- 通过管理端业务 API 执行实际导入，批次 1 状态为 `CONFIRMED`：
  - 新建客户 233、复用客户 0
  - 新建订单 233、复用订单 0
  - 新建 ICCID 119、复用已有 ICCID 12
  - 异常 6，均未覆盖冲突数据
- 只读核验结果：233 个订单分别对应 233 个客户；132 个订单有上台日期；98 张 REAL、33 张 VIRTUAL；127 张 USED 卡绑定正确，4 张未上台 REAL 卡保持 AVAILABLE；关系冲突和上台号码不一致均为 0。
- 将 `com.cmhk.business` 日志级别从 debug 调整为 info，避免 MyBatis 调试参数记录敏感业务字段。
- Redis 默认连接参数使用 `localhost:6379`、数据库 `0`，避免空字符串无法绑定整数导致启动失败。
- 测试：`CustomerBackupSimulationServiceTests` 3 项通过，`BUILD SUCCESS`。
- 完整测试：9 项中 8 项通过、0 项失败、真实文件测试默认跳过 1 项，`BUILD SUCCESS`。
- 临时 Spring Boot 进程已关闭。

### 客户类别与数字状态模型调整

- 新增 `customer.customer_category`，用于保存留学生、地产客户、研究生等业务类别，不再占用 `requirement_summary`。
- 将 233 条备份客户中原暂存于需求摘要的类别迁出：226 条写入独立类别字段，7 条源文件类别为空，残留类别摘要 0 条。
- `customer.current_status` 改为 `TINYINT`，数据库 COMMENT 和 Java 常量均声明：0待处理、1跟进中、2待资料、3办理中、4待激活、5已激活、6已完成、9无效。
- 用户确认：`onboardDate` 代表上台日期；上台后默认待激活，不能仅凭上台日期推断已激活。只有来源阶段明确“已激活”时才记 5。
- 实际状态分布：0=12、1=70、2=6、3=2、4=48、5=89、6=1、9=5，共 233 条。
- 132 条有上台日期记录中：48 条待激活、83 条明确已激活、1 条已完成，其他状态为 0。
- 订单激活状态同步取消“已上台”：49 条待激活、89 条明确已激活、95 条尚无激活状态，旧“已上台”残留 0 条。
- 管理端客户列表、筛选、编辑和详情已改为数字码选项及中文标签，同时展示独立客户类别和需求摘要。
- 补充 `GET /api/admin/customers/channels` 主渠道选项接口；列表、详情和编辑页面均将 `channel_id` 映射为渠道名称。
- 实际库只读核验：233 条备份客户缺失渠道 ID 0、无法匹配渠道 0、非法状态码 0，详情渠道对象关联正确。
- 验证：真实备份映射测试 4 项通过；管理端生产构建通过。
- 完整后端测试 10 项：9 项通过、0 项失败、真实文件测试默认跳过 1 项，`BUILD SUCCESS`。

### 客户备份订单上台口径修正

用户明确统一口径：`待激活`、`已激活`、`已完成`三个客户状态才算已上台；只有已上台客户才能进入订单表。

- `CustomerStatusCode.isOnboarded` 统一判断 `4/5/6` 三个状态。
- 客户备份模拟仍为每条有效来源记录生成客户候选，但仅为上述状态生成订单候选；ICCID 的绑定和虚拟卡生成同步依据状态判断，不再直接用上台日期决定是否进入订单表。
- 上台日期继续用于把来源记录映射为默认的“待激活”状态，以及虚拟 ICCID 的稳定摘要，不等同于订单表准入条件。
- 新增历史订单范围修正接口：先由 `GET /api/admin/customer-backups/order-scope/preview` 返回待清理数量和 ICCID、对账、结算关联冲突；仅在管理员请求 `POST /api/admin/customer-backups/order-scope/confirm` 后，在事务中清理未上台客户的 `CMHK_BACKUP` 订单。
- 修正不会删除客户或卡池中的可用 ICCID；发现关联冲突会拒绝自动执行。
- 验证：`mvn.cmd test` 通过，14 项测试中 0 项失败、2 项按配置跳过。

- 实际数据修正：通过临时 `8081` 管理员 API 先预览后确认，233 条历史订单中清理 95 条未上台订单，修正后保留 138 条；233 名客户保留，ICCID、对账、结算关联冲突均为 0，临时进程已关闭。

### 实际 MySQL 同步与接口联调

后续确认可通过 JDBC 使用本机私有配置连接 MySQL，因此按用户要求直接执行了本次新增表结构和演示数据：

- 创建 `channel`、`channel_entry`、`customer`、`customer_channel_binding`、`phone_verification_code`
- 写入 `DEMO-ENTRY-001` 自营渠道演示入口
- 写入 `ELDERLY-ENTRY-001` 长者关怀演示入口
- 查询确认两个入口及其长者模式配置均正确存在

使用临时 `8081` 后端进程完成真实接口联调：

- 渠道入口校验成功
- 模拟验证码发送成功
- 手机号登录成功
- 客户首次渠道绑定成功

联调生成的测试客户、渠道绑定和验证码记录已直接从 MySQL 清理。临时 `8081` 后端进程已关闭，未影响原有 `8080` 服务。

### 手机号登录令牌鉴权

实现“菜单页公开、进入业务模块必须登录”的令牌鉴权机制：

- 手机号验证码登录成功后，后端使用本机私有密钥签发带签名、带有效期的访问令牌
- 令牌默认有效期为 24 小时，令牌密钥仅保存在被忽略的 `application-local.yml`
- 新增 Spring MVC 令牌拦截器，保护套餐查询、订单创建等业务接口
- 保持健康检查、业务菜单和渠道认证接口可公开访问
- 前端将登录会话保存在 `sessionStorage`，Axios 自动添加 `Authorization: Bearer Token`
- 前端路由守卫保护套餐办理、转人工、办理记录、客户中心和 AI 客服页；初始实现为登录后返回原目标页，后续已调整为统一回到首页菜单

验证：

- `mvn.cmd test` 通过，`BUILD SUCCESS`
- `npm.cmd run build` 通过
- 临时 `8081` 后端接口联调通过：无令牌访问套餐接口返回 `401`；登录后取得令牌；携带令牌访问返回 `200`
- 联调产生的测试客户、绑定和验证码记录已从 MySQL 清理，临时 `8081` 服务已关闭

### 调整登录成功跳转

- 手机号验证成功后统一跳转首页菜单
- 移除路由守卫和接口未授权处理中的原目标页面回跳参数

### 统一响应码调整

- `ApiResponse<T>` 统一约定：`code = 1` 表示成功，`code = 0` 表示业务失败
- 前端所有接口成功判断同步改为 `code === 1`

### 全局异常处理

- 新增 `GlobalExceptionHandler`，统一返回 `ApiResponse<T>` 失败结构
- 统一处理参数校验、请求体格式、业务参数和未知异常
- 渠道认证 Controller 移除重复的 `try/catch`，由全局异常处理器统一接管

### 修复确认办理伪成功跳转

- 删除确认办理接口异常时生成本地订单号并跳转转人工页的演示兜底逻辑
- 只有后端成功创建订单并返回 `code = 1` 后，前端才进入转人工页面

### 登录客户与订单关联

- `mobile_plan_order` 新增 `customer_id` 字段和 `idx_mobile_plan_order_customer_id` 索引，并已直接同步到实际 MySQL
- Token 拦截器将当前客户 ID 写入请求属性；订单 Controller 从该属性取得客户 ID，不接受前端传入
- 订单创建服务校验登录客户存在后写入 `customer_id`
- 完成真实接口联调：登录取得的客户 ID 与新订单 `customer_id` 一致
- 联调生成的订单、客户、渠道绑定和验证码记录已从 MySQL 清理，临时 `8081` 服务已关闭
## 2026-08-28：V1 改造 P3 资源运营补齐

- 为 ICCID 解绑、使用、停用补充条件更新，防止并发操作覆盖新状态。
- 新增虚拟 ICCID 替换真实卡事务：真实卡继承客户、订单和上台号码；虚拟卡转 `REPLACED`，双方写入替换历史及操作日志。
- 新增 `referral_chain`、`referral_number_pool`、`referral_number_assignment_history`，支持多条独立接龙、创建接龙、指定龙头、暂停/关闭、学生订单占用、释放、上台换头、停用和全历史追溯。
- 推荐号码导入实现“预览摘要 -> 人工确认”；导入号码先作为候选，不按历史上台日期自动串龙。
- 学生订单资格兼容 `customer_identity`、学生套餐类型/编码及客户类别；待激活、已激活、已完成作为完成接龙允许状态。
- 新增资源诊断、订单资源聚合和客户详情推荐号码展示；管理端新增资源管理菜单、推荐号码接龙页和虚拟卡替换页。
- 执行真实库迁移前备份到 `D:\download\cmhk_before_p3_2026-08-28.sql`；迁移新增三张推荐号码表，并为127张现有绑定卡补充 `MIGRATION_BASELINE` 历史，未改变卡状态和绑定。
- 验证：后端30项测试中28项通过、2项条件跳过；管理端和H5类型检查、生产构建均通过；P3数据库核验无缺失 ICCID 历史、无重复占用结果。
- 本阶段未自动创建任何历史接龙，初始龙头必须由运营人员确认后指定。

### 接龙初始化与模拟验收

- 经用户确认，使用两条既有、已上台且未占用推荐号码的学生订单，建立一条名称明确的 `P3模拟验收接龙`。
- 通过管理后台 API 依次完成候选初始龙头导入、人工指定龙头、分配给学生订单、完成上台换头；未直接写数据库，未修改客户、订单办理状态、ICCID 或 UMALL 状态。
- 核验结果：旧龙头为 `USED`，其 `next_number_id` 指向新龙头；新龙头为 `AVAILABLE`，其 `previous_number_id` 指向旧龙头，来源订单正确；目标订单的 `referrer_phone` 与旧龙头号码一致。
- 旧龙头历史包含 `IMPORT`、`DESIGNATE_HEAD`、`RESERVE`、`USE` 四类事件各一条；接龙没有残留 `RESERVED` 号码。
- 验收使用临时18081后端，操作完成后已确认端口释放。

### 接龙总览与完整链路展示

- 管理端推荐号码页改为以接龙为一级对象的卡片总览，卡片按创建顺序展示“接龙1/2/3”、接龙名称、状态、号码数和当前最新号码。
- 点击接龙卡片后，后端按 `previous_number_id` / `next_number_id` 输出首号到当前龙头的完整链路；管理端以横向链路呈现每个号码、顺序、关联订单/客户和当前龙头标记。
- 原有候选导入、人工指定龙头、分配、释放、上台换头和停用操作保持不变。
- 验证：后端测试30项中28项通过、2项按配置跳过；管理端类型检查和生产构建通过。

### 简化接龙创建

- 新建接龙弹窗移除人工填写的接龙编号，改为接龙名称、初始推荐号码和备注。
- 服务端在同一事务中创建接龙、自动生成 `REF-` 内部编号、创建初始号码并设为第一任龙头；推荐号码冲突会使整笔创建回滚。
- 验证：后端测试30项中28项通过、2项按配置跳过；管理端生产构建通过。

## 2026-08-28：V1 改造 P4 任务与异常闭环

- 新增 `operation_task`、`operation_task_history` 以及任务模块的实体、Mapper、Service接口与实现、管理端接口。
- 任务状态为 `PENDING`、`PROCESSING`、`DONE`、`CLOSED`；支持领取、管理员转派、处理中记录、完成和关闭，并保留每次操作的专属历史及通用操作日志。
- 任务通过来源类型、来源记录和任务类型组成活动去重键，MySQL唯一索引确保同一来源没有重复未完成任务；完成或关闭后可在新异常出现时重新建任务。
- CMHK对账确认后会创建补件、审核明确失败/拒绝、激活明确失败/异常和对账匹配异常任务；审核中、待激活不被错误标为异常。任务不会回写 UMALL 或订单外部事实状态。
- 资源巡检按真实可用ICCID少于10张、活跃接龙缺少龙头创建资源不足任务；提供任务中心人工巡检入口。
- 管理端新增任务中心、首页待处理任务指标、客户详情关联任务、订单管理关联任务入口。
- 已执行真实库 `V004__operation_tasks.sql`，迁移前备份为 `D:\download\cmhk_before_p4_2026-08-28.sql`；迁移仅创建两张空表和索引，未变更既有业务数据。
- 验证：后端30项测试中28项通过、2项跳过；管理端和H5类型检查、生产构建通过；临时18082管理员接口联调通过，当前资源巡检创建0条任务且临时服务已关闭。

### 任务中心模拟验收

- 发现并修复任务创建临时编号超过 `task_no` 32位字段限制的问题；失败请求已事务回滚，没有留下任务记录。
- 通过管理后台 API 创建 `TASK-000001` 模拟客户跟进任务，明确标记为P4验收用途且不关联任何真实业务对象。
- 已依次完成领取和处理中记录，任务保持 `PROCESSING`，任务历史为 `CREATE`、`CLAIM`、`PROCESS` 三条；未改写客户、订单、ICCID、接龙或 UMALL 状态。
- 修复后后端完整测试30项中28项通过、2项跳过；临时18082服务已关闭。

### 订单状态受控录入与任务触发补充

- 订单编辑表单移除联系电话；已有订单的历史联系电话保留，不再要求后台操作人员填写。
- 套餐编码和套餐名称改为选择现有启用套餐。服务端只接受套餐 ID，并从套餐库写入编码、名称、类型、月费和合约期快照，避免手工输入不一致；历史订单原有的已停用套餐只可保留，不能作为新套餐选择。
- 审核状态、补件状态、激活状态均调整为受控下拉选项；审核拒绝/失败、待补件/补件中、激活失败/异常首次出现时，会根据订单状态自动建立相应的补件、审核异常或激活异常任务。
- 保存无关字段不会反复创建任务；任务以订单和任务类型去重。审核中、待激活不属于异常，不会创建异常任务。
- 验证：后端 `mvn test` 共30项测试，28项通过、2项跳过；管理端 `npm run build`（含 `vue-tsc`）通过。未写入真实业务数据，未进行 Git 提交。
