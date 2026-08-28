# CMHK 项目当前状态快照

本文档只记录当前有效状态，不持续追加历史过程。历史日期记录、详细修改内容和历史测试结果见 `.codex/PROJECT_HISTORY.md`。

Codex 后续继续开发前，应先读取：

1. `.codex/PROJECT_RULES.md`
2. `.codex/PROJECT_STATUS.md`
3. 需要追溯过程时再读取 `.codex/PROJECT_HISTORY.md`

## 1. 当前阶段

- 阶段：JOINCOM × CMHK V1 改造阶段 P2“客户、订单与产品标准化”已完成；等待用户确认后再进入 P3
- 管理端范围：统一渠道、ICCID、客户、订单、甲方对账、渠道结算和操作日志
- 管理端目录：`admin-frontend/`，桌面端 Vue 3 + Vite + TypeScript + Element Plus
- 管理端表格适配：ICCID 卡池、客户管理和订单管理在笔记本宽度下保留列宽；客户管理已移除联系方式列并格式化创建时间，订单管理已移除手机号列并收紧内部订单号等列宽，ICCID 卡池已格式化分配时间并收紧内部订单号列
- 当前业务范围：只保留“移动套餐办理”
- 最近确认日期：2026-08-03
- 当前核心流程：菜单页 -> 进入业务模块时令牌校验 -> 渠道入口识别 -> 手机号验证/注册 -> 客户首次渠道绑定 -> 套餐选择 -> 确认办理 -> 创建归属当前客户的订单 -> 转人工
- 当前页面定位：移动端 H5 优先，每次跳转对应独立页面
- AI 客服：已有前端展示入口和聊天展示页，暂未接真实 AI 能力

## 2. 当前架构

- 项目：CMHK 智能业务办理网站
- 架构：前后端分离
- 前端：Vue 3 + Vite + TypeScript + Axios + Vue Router
- 后端：Spring Boot 3 + Java 21 + MyBatis Plus
- 数据库：MySQL
- 缓存：Redis
- 本机真实配置：统一放在 `backend/src/main/resources/application-local.yml`，该文件不提交

当前目录重点：

```text
D:\cmhkProject
  backend/                   Spring Boot 后端
  frontend/                  Vue3 前端
  docker-compose.yml         本地 MySQL 和 Redis 编排
  README.md                  运行说明
  .codex/PROJECT_RULES.md    项目协作规则
  .codex/PROJECT_STATUS.md   当前状态快照
  .codex/PROJECT_HISTORY.md  历史推进记录
  .codex/agents/             固定子代理角色规范
```

## 3. 已完成

### 3.1 基础工程

- 已创建后端 Spring Boot 项目骨架
- 已创建前端 Vue 3 + Vite 项目骨架
- 已配置前后端分离调用方式
- 已配置公共 `application.yml` 使用环境变量占位
- 已创建 `application-local.example.yml`
- 已忽略本机真实配置 `application-local.yml`、`.env` 等文件
- 已创建 `.codex/PROJECT_RULES.md`
- 已创建固定子代理规范：
  - `.codex/agents/git-agent.md`
  - `.codex/agents/progress-agent.md`
  - `.codex/agents/discussion-agent.md`
  - `.codex/agents/test-agent.md`

### 3.2 后端能力

- 通用返回：`ApiResponse<T>`，`code = 1` 表示成功，`code = 0` 表示业务失败
- 全局异常处理：`GlobalExceptionHandler`，统一处理参数校验、业务参数、请求体格式和未知异常
- 跨域配置：`CorsConfig`
- 健康检查接口：`GET /api/health`
- 业务类型接口：`GET /api/business-types`
- 移动套餐列表接口：`GET /api/mobile-plans`
- 移动套餐详情接口：`GET /api/mobile-plans/{planCode}`
- 移动套餐订单接口：`POST /api/mobile-plans/orders`
- 创建移动套餐订单时，从 Token 获取当前客户 ID 并写入订单 `customer_id`
- Controller 已按规则添加 SLF4J 日志，且不记录敏感信息
- 移动套餐查询已接入 Redis 缓存通用类 `CacheClient`
- `CacheClient` 已实现：
  - 空值缓存，降低缓存穿透风险
  - Redis 互斥锁，降低缓存击穿风险
  - TTL 随机抖动，降低集中失效风险
  - Redis 异常降级查数据库
- 已新增渠道认证接口：
  - `GET /api/channel-auth/entry?entryToken=xxx`
  - `POST /api/channel-auth/verification-codes`
  - `POST /api/channel-auth/phone-login`
- 当前验证码为开发阶段模拟验证码：`123456`，有效期 5 分钟，单验证码最多尝试 5 次，发送间隔 60 秒
- 手机号登录成功后，后端签发带签名的访问令牌，默认有效期 24 小时
- 除健康检查、业务菜单和渠道认证接口外，`/api/**` 均由后端令牌拦截器保护

### 3.3 数据库

- 当前数据库：`cmhk`
- 当前业务类型只保留：
  - `MOBILE_PLAN`：移动套餐办理
- 当前核心业务表：
  - `business_type`
  - `mobile_plan`
  - `mobile_plan_offer`
  - `mobile_plan_order`
- 已在 `schema.sql` 新增渠道入口与客户注册表，并已同步到实际 MySQL：
  - `channel`
  - `channel_entry`
  - `customer`
- 已同步管理后台增量结构到本机 `cmhk`：
  - `iccid_inventory`、`iccid_assignment_history`
  - `cmhk_reconciliation_import`、`cmhk_reconciliation_row`
  - `secondary_channel`、`secondary_commission_rule`、`secondary_commission_record`
  - `operation_log`
  - `customer` 与 `mobile_plan_order` 的管理后台扩展字段
  - `customer_channel_binding`
  - `phone_verification_code`
- `mobile_plan` 已支持渠道政策套餐字段、折实月费、官方月费、合约期、优惠截止、折算公式等信息
- `mobile_plan_offer` 已支持套餐附加优惠权益
- `mobile_plan_order` 已保存套餐关联和套餐快照
- `mobile_plan_order.customer_id` 已关联登录客户，并建立查询索引
- `mobile_plan_order.customer_identity` 当前约定：
  - `0`：自营客户
  - `1`：留学生
- `mobile_plan_order` 当前办理信息字段包括：
  - `has_offer`
  - `has_pass_or_hkid`
  - `expected_start_date`
  - `id_type`
  - `id_no`（历史兼容列，应用不再采集、写入或返回）
  - `referrer_phone`
  - `preferred_contact_time`
- 已删除错误字段：
  - `has_offer_plus_or_hkid`
  - `has_offer_plus`

### 3.4 前端页面

- 已改为移动端 H5 优先
- 已引入 `vue-router`
- 当前独立页面：
  - `/#/`
  - `/#/business/MOBILE_PLAN`
  - `/#/business/MOBILE_PLAN/confirm?planCode=xxx`
  - `/#/business/MOBILE_PLAN/transfer?orderNo=xxx`
  - `/#/ai-chat`
  - `/#/records`
  - `/#/profile`
- 套餐选择页展示套餐价格、权益、优惠、合约期
- 确认页只从 URL 获取 `planCode`，套餐详情从后端接口加载
- 确认页支持填写：
  - 客户身份
  - 客户姓名
  - 联系电话
  - 是否有 offer
  - 是否有通行证 / HKID
  - 预计开始使用日期
  - 证件类型
  - 推荐人号码
  - 方便联系时间
  - 办理备注
- offer 选项只在客户身份为“留学生”时显示
- 通行证 / HKID 选项对所有客户身份显示
- 转人工页 URL 只保留 `orderNo`，不携带联系电话
- 新增渠道手机号验证独立页：`/#/channel-auth?entryToken=xxx`
- 扫码入口可使用：`/#/?entry_token=DEMO-ENTRY-001`
- 长者演示入口：`/#/?entry_token=ELDERLY-ENTRY-001`，会启用大字体和更大操作区
- 菜单页可直接访问；套餐办理、办理记录、客户中心和 AI 客服页面必须先完成手机号登录，登录成功后统一回到首页菜单

### 3.5 管理后台 MVP

- 独立管理员登录与 `/api/admin/**` 鉴权
- ICCID 卡池新增、XLS/XLSX/CSV 导入、查询、分配、解绑、使用、停用和配对历史
- 客户和订单列表、新增、编辑与聚合详情
- CMHK 对账文件 SHA-256 防重、预览、确认、自动匹配和人工异常匹配
- 二级渠道档案、数据库结算规则、BigDecimal 佣金公式、规则快照、T+N 拆分、人工修正和确认结算
- 首页指标、操作日志及桌面管理端全部对应页面

### 3.6 协作规则

- Git commit 使用中文 Conventional Commits
- Codex 完成一个功能或阶段后，需要主动询问用户是否提交
- 除非用户明确要求立即提交，否则不要默认直接提交
- 项目进度记录拆分为：
  - `PROJECT_STATUS.md`：当前状态快照
  - `PROJECT_HISTORY.md`：历史推进记录

### 3.7 真实客户备份只读分析

- 已只读分析 `CMHK客户数据备份_2026-08-21 (7).json`，未写入实际数据库。
- 根结构为数组，共 233 条记录、42 个顶层字段，另有 538 条嵌套跟进日志。
- 用户确认导入口径：`onboardDate` 非空即“已上台”，不得使用阶段或进展文本推断。
- 按该口径共 132 条已上台记录：
  - 96 条包含标准 20 位 ICCID
  - 33 条 ICCID 为空，应进入稳定虚拟 ICCID 候选
  - 3 条 ICCID 非空但格式异常，必须进入异常列表，不得自动覆盖
- 全文件 ICCID 为空 129 条；非空 ICCID 中有 1 组重复，涉及 2 条记录。
- 132 条已上台记录均有上台号码，且上台号码在文件内唯一。
- 源文件没有结构化客户手机号和 UMALL 订单号；`number` 是上台号码，`umall` 是 UMALL 状态，二者不得误映射。

### 3.8 真实客户备份模拟导入

- 已新增 `POST /api/admin/customer-backups/simulate`，只在内存中解析并返回文件摘要、候选数据和脱敏异常。
- 已新增 `POST /api/admin/customer-backups/confirm`，必须提交同一文件及模拟返回的 SHA-256 摘要，文件变化或重复确认会拒绝。
- 已为客户和订单补充稳定来源键；客户按 `CMHK_BACKUP + source_customer_id` 复用，订单按 `CMHK_BACKUP + source_record_id` 复用。
- 已保留原始渠道来源，导入客户建立渠道、渠道入口和首次渠道绑定，不覆盖已有冲突绑定。
- ICCID 已支持 `REAL`、`VIRTUAL`、`REPLACED` 生命周期所需字段及替换关系字段。
- 用户确认规则已固化：只有 `onboardDate` 非空才视为已上台。
- 已通过业务 API 向实际 `cmhk` 数据库确认导入批次 1：233 个客户、233 个订单、131 张卡、6 条异常。
- 131 张卡中 98 张 REAL、33 张 VIRTUAL；127 张已上台卡为 USED 并绑定客户和订单，4 张未上台 REAL 卡为 AVAILABLE。
- 119 张卡为新建，12 张复用原卡池记录并补充关系；冲突不覆盖，进入异常明细。
- 已新增 `customer_backup_import` 与 `customer_backup_import_row`，保存批次汇总、关联 ID 和异常原因，不保存源文件原文。
- 公共日志级别已从 debug 调整为 info，避免 MyBatis 参数日志记录客户敏感字段。
- `customer.customer_category` 已独立保存留学生、地产客户、研究生等业务类别；`requirement_summary` 只保留 C 端承接的客户需求。
- `customer.current_status` 已改为带数据库 COMMENT 的数字状态码：0待处理、1跟进中、2待资料、3办理中、4待激活、5已激活、6已完成、9无效。
- 状态映射规则：来源阶段明确“已激活”才记 5；有 `onboardDate` 且尚未明确激活时记 4=待激活；已完成保留为终态 6。
- 订单 `activation_status` 不再使用“已上台”：来源阶段明确已激活为“已激活”，有上台日期或阶段待激活为“待激活”。
- 实际库迁移结果：233 条备份客户中 226 条有客户类别，7 条源类别为空；原“来源客户类型”需求摘要残留 0 条。
- 状态分布：待处理 12、跟进中 70、待资料 6、办理中 2、待激活 48、已激活 89、已完成 1、无效 5。

### 3.9 管理后台 Redis 缓存

- 已复用 C 端 `CacheClient`，为管理端首页指标、客户列表/详情/渠道、订单列表/详情、ICCID 列表/历史增加 Redis 缓存。
- 管理后台缓存 TTL：核心列表和详情 5 分钟、首页指标 2 分钟、渠道选项 15 分钟、空值 1 分钟，并保留随机抖动。
- 查询条件统一转换为 SHA-256 摘要，手机号等筛选值不会直接出现在 Redis 键中。
- 缓存按业务命名空间维护版本；数据库事务提交成功后提升版本，旧缓存短期自然过期，无需使用 `KEYS` 或 `SCAN` 批量删除。
- 客户、订单、ICCID、客户备份确认导入、对账和二级渠道结算写操作均已补充对应缓存失效。
- 修复 `CacheClient` 首次读取 Redis 异常时未进入降级逻辑的问题；Redis 不可用时会回退 MySQL。
- 操作日志、对账预览和结算过程列表不缓存，继续保证人工处理页面读取实时数据。

### 3.10 客户备份订单上台口径修正

- 已统一上台口径：只有客户状态为 `4=待激活`、`5=已激活`、`6=已完成` 才视为已上台。
- 后续客户备份模拟与确认导入会保留全部客户，但只为上述三个状态生成 `mobile_plan_order`；未上台客户的真实 ICCID 保留在卡池 `AVAILABLE`，不绑定订单。
- 已新增历史模拟订单的 `GET /api/admin/customer-backups/order-scope/preview` 只读预览，以及 `POST /api/admin/customer-backups/order-scope/confirm` 确认修正接口。
- 确认修正会先检查 ICCID、对账、结算关联；存在关联冲突时拒绝自动删除。无冲突时仅清理不符合上台口径的 `CMHK_BACKUP` 订单，保留客户、卡池记录和导入留痕。
- 已于 2026-08-26 通过管理员业务 API 完成预览和确认：清理 95 条未上台 `CMHK_BACKUP` 订单，保留 233 名客户；订单余 138 条，ICCID、对账、结算关联冲突均为 0。
- 最近验证：2026-08-26，`mvn.cmd test` 通过，14 项测试中 0 失败、2 项按配置跳过。

### 3.11 V1改造P1统一渠道与权限

- `channel` 已成为唯一渠道主档，新增渠道类型、上级渠道、联系人、合作状态、结算信息和内部负责人字段。
- 已新增 `channel_legacy_mapping` 与 `channel_migration_exception`；旧 `secondary_channel` 保留只读兼容，写接口明确停用。
- 渠道佣金记录统一关联 `channel.id`；生成结算时校验订单客户归属渠道一致。
- 客户首次渠道绑定和后台客户维护均以 `customer_channel_binding` 为业务事实，并同步 `customer.channel_id`。
- `admin_user` 已增加 `scope_type`、`scope_id`；当前角色启用 `ADMIN`、`OPERATOR`。
- 管理员令牌验证结果已改为包含用户ID、账号、角色和数据范围的 `AdminPrincipal`，停用或改权后旧令牌立即失效。
- 系统用户、统一渠道写入、佣金规则、金额修正和结算确认仅允许 `ADMIN`；`OPERATOR` 可处理日常业务。
- `CHANNEL`范围在V1只开放已实现行级过滤的客户、订单、渠道及结算接口，其他接口返回403，不建设渠道独立门户。
- 管理端渠道档案、后台用户页面、菜单和关键操作按钮已按统一渠道与角色权限调整。
- 已执行真实库 `V001__unify_channels_and_admin_permissions.sql`；迁移前完整备份位于 `D:\download\cmhk_before_p1_2026-08-27.sql`。
- 迁移核验：统一渠道4、旧二级渠道0、佣金记录0、迁移异常0；客户渠道不一致由1修正为0，非法管理员角色或范围0。

### 3.12 V1改造P2客户、订单与产品标准化

- `customer` 已增加内部负责人；新增客户跟进记录，客户详情可录入和查看跟进内容及下次跟进时间。
- 订单办理状态统一为10个英文状态码；管理端改为受控下拉选项，H5新订单使用 `FOLLOWING`。
- 新增 `order_status_history`，记录JOINCOM、UMALL、审核、补件、激活和合约状态来源与操作人；142个历史订单均已生成迁移历史。
- `review_status`、`supplement_status`、`status_updated_at` 已同步到订单主表；UMALL原始状态与JOINCOM标准状态分开保存。
- CMHK对账导入使用集中状态映射；未知状态写入 `STATUS_EXCEPTION`，不覆盖订单状态；文件哈希防重机制保持不变。
- 新增管理端产品管理：套餐、套餐权益、渠道产品政策的查询和维护；套餐删除采用下架，历史订单套餐价格和规则快照不回写。
- 产品写操作仅ADMIN可用；OPERATOR可以只读查看产品。
- 已执行真实库 `V002__standardize_customers_orders_and_products.sql`；迁移前完整备份位于 `D:\download\cmhk_before_p2_2026-08-28.sql`。
- 迁移核验：订单142、状态历史142、非标准订单状态0、缺失历史0、跟进/政策悬空关系0。

## 4. 进行中

- 为虚拟 ICCID 后续替换真实卡补充管理端操作接口和页面
- 使用真实 CMHK 对账文件核对列名别名和状态映射
- 配置独立管理员密码与 Token 密钥
- 开发当前登录客户的办理记录查询接口与页面
- 继续完善移动套餐确认办理字段和交互
- Redis 已完成本机真实缓存写入、命中和反序列化验证
- AI 客服目前只是前端展示，后续再接真实能力

## 5. 阻塞问题

- Docker 尚未确认可用
- 暂无 Redis 运行阻塞；缓存异常时仍会自动回退 MySQL

## 6. 下一步

1. 等待用户确认是否进入 V1 改造阶段 P3“资源运营补齐”
2. 在管理端增加客户备份模拟、确认、批次明细和异常列表页面
3. 实现虚拟 ICCID 替换真实 ICCID 的事务接口，并写入 REPLACED 状态及分配历史
4. 人工处理 4 条格式异常和 1 组重复 ICCID 的 6 条异常明细
5. 为 Spring 上下文测试补充隔离的数据源配置，避免依赖开发机私有数据库配置

## 7. 最近验证结果

2026-08-28 V1 改造阶段 P2：

- 后端完整测试28项：26项通过、0项失败、2项按配置跳过，`BUILD SUCCESS`；新增状态映射未知值和误判保护测试。
- `frontend` 与 `admin-frontend` 的类型检查和生产构建均通过；管理端只有既有的大分块警告。
- 真实库迁移前客户234、订单142、套餐17、权益35；P2表和字段均为未创建状态。
- 迁移后订单状态分布：`ACTIVATED` 89、`COMPLETED` 1、`FOLLOWING` 7、`SUBMITTED_UMALL` 6、`WAITING_ACTIVATION` 39。
- 状态历史142、缺失历史0、非标准状态0、跟进悬空0、政策悬空0、状态异常0。

2026-08-27 V1 改造阶段 P1：

- 后端完整测试22项：20项通过、0项失败、2项按配置跳过，`BUILD SUCCESS`；权限矩阵测试覆盖ADMIN、OPERATOR和CHANNEL范围。
- `frontend` 与 `admin-frontend` 的类型检查和生产构建均通过；管理端仅有既有的大分块警告。
- 临时端口18080真实接口联调：健康检查200、无令牌访问管理端401、管理员登录成功、带令牌访问首页和统一渠道接口200；进程已关闭，端口已释放。
- 临时15174管理端页面检查：登录页标题、品牌文案和登录表单正常渲染，浏览器控制台无错误；临时端口已释放。
- P1真实库迁移后：统一渠道4、旧二级渠道0、佣金记录0、渠道映射0、迁移异常0、客户234、渠道绑定234、管理员1。
- 客户主表与绑定表渠道不一致0、佣金统一渠道悬空0、非法管理员角色或数据范围0。

2026-08-27 V1 改造阶段 P0：

- 后端完整测试 19 项：17 项通过、0 项失败、2 项按配置跳过，`BUILD SUCCESS`；其中 P0 隐私与旧请求兼容测试 5 项全部通过。
- `frontend` 与 `admin-frontend` 的 `npm.cmd run type-check` 均通过。
- `frontend` 与 `admin-frontend` 的 `npm.cmd run build` 均通过；管理端仅有既有的大分块警告，不影响构建成功。
- P0 预检 20 条 SQL、核验 4 条 SQL 已通过真实 MySQL 只读连接逐条验证。
- 实际库只读基线：客户 234、订单 142、ICCID 155、对账行 94、佣金记录 0、非空 `id_no` 0；核心重复组和悬空关系均为 0。
- `schema.sql` 声明 20 张表，真实库 20 张表；逐表字段集合差异为 0。

2026-08-26 客户备份模拟导入：

- 管理后台 Redis 缓存单元测试 3 项通过。
- 管理后台真实 Redis 联调测试 1 项通过，覆盖首页、客户、订单、ICCID 的首次写入和二次读取。
- 后端完整测试 14 项：12 项通过、0 项失败、真实 Redis 与真实备份文件测试默认跳过 2 项，`BUILD SUCCESS`。

- 真实备份模拟测试 3 项通过，`BUILD SUCCESS`
- 后端完整测试 10 项：9 项通过、0 项失败、真实文件测试默认跳过 1 项，`BUILD SUCCESS`
- 实际导入批次：1，状态 `CONFIRMED`
- 客户 233、订单 233，订单对应 233 个不同客户，悬空关系 0
- 有上台日期订单 132
- ICCID 131：REAL 98、VIRTUAL 33、USED 127、AVAILABLE 4
- ICCID 绑定关系冲突 0，上台号码与订单不一致 0
- 渠道绑定客户 233
- 客户类别字段迁移 226，源类别为空 7，错误占用需求摘要 0
- 132 条有上台日期记录：待激活 48、已激活 83、已完成 1，其他状态 0
- 233 条备份订单激活状态：待激活 49、已激活 89、空 95，旧“已上台”及其他状态 0
- 管理端已新增主渠道选项接口，列表和详情按 `channel_id` 显示渠道名称，编辑客户使用渠道下拉框。
- 233 条备份客户渠道映射：缺失渠道 ID 0、无法匹配渠道 0、非法状态码 0；详情渠道对象与客户渠道 ID 一致。
- 客户状态模型调整后完整测试 10 项：9 项通过、0 项失败、1 项默认跳过，`BUILD SUCCESS`
- 临时后端进程已关闭

2026-08-25 客户备份只读分析：

- JSON 可正常解析，共 233 条记录
- `id` 233 条全部非空且唯一
- 上台号码 138 条非空且全部唯一；按 `onboardDate` 口径的 132 条已上台记录均有上台号码
- ICCID 129 条为空、100 条为标准 20 位、4 条为非标准非空值；非空值中有 1 组重复
- 未执行任何实际数据库写入

2026-08-19 管理后台验证：

- 后端 `mvn.cmd test` 通过，共 4 项测试
- 管理端 `npm.cmd run build` 通过
- 原移动端 `npx.cmd vue-tsc --noEmit` 通过
- 管理端登录页及主框架已完成 1280、1440、1920 宽度 Chrome 渲染检查

最近一次已通过：

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

```powershell
cd D:\cmhkProject\frontend
npm.cmd run build
```

结果：通过，已包含渠道手机号验证页面。

```powershell
cd D:\cmhkProject\backend
D:\download\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd test
```

结果：通过，`BUILD SUCCESS`，已包含渠道认证模块。

令牌鉴权真实接口联调已通过：

- 未携带令牌访问 `GET /api/mobile-plans` 返回 `401`
- 手机号登录成功后返回访问令牌
- 携带 `Authorization: Bearer Token` 访问套餐接口返回 `200`

真实接口联调已通过：

- 自营入口 `DEMO-ENTRY-001` 可正确识别为普通渠道
- 长者入口 `ELDERLY-ENTRY-001` 可正确识别为关怀模式
- 模拟验证码发送和手机号登录成功
- 客户首次渠道绑定成功
- 联调产生的测试客户、绑定和验证码记录已清理

订单客户关联真实接口联调已通过：

- 手机号登录成功后取得客户 ID 与 Token
- 创建移动套餐订单成功
- 新订单 `customer_id` 与当前登录客户 ID 一致
- 联调产生的订单、客户、绑定和验证码记录已清理

最近提交：

```text
1f59a04 docs: 新增阶段完成后提交确认规则
c5c6c56 fix: 修复确认办理伪成功跳转
4de6f0a feat: 新增全局异常处理
a0f6a8e feat: 新增渠道登录令牌鉴权
34983c3 feat: 完善移动套餐确认办理流程
565e492 feat: 新增移动套餐 Redis 缓存
472d043 feat: 新增移动套餐优惠模型和展示
```
