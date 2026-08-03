# CMHK 项目当前状态快照

本文档只记录当前有效状态，不持续追加历史过程。历史日期记录、详细修改内容和历史测试结果见 `.codex/PROJECT_HISTORY.md`。

Codex 后续继续开发前，应先读取：

1. `.codex/PROJECT_RULES.md`
2. `.codex/PROJECT_STATUS.md`
3. 需要追溯过程时再读取 `.codex/PROJECT_HISTORY.md`

## 1. 当前阶段

- 阶段：渠道入口、手机号注册与令牌鉴权已完成并完成真实接口联调
- 当前业务范围：只保留“移动套餐办理”
- 最近确认日期：2026-08-03
- 当前核心流程：菜单页 -> 进入业务模块时令牌校验 -> 渠道入口识别 -> 手机号验证/注册 -> 客户首次渠道绑定 -> 套餐选择 -> 确认办理 -> 转人工
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
  - `customer_channel_binding`
  - `phone_verification_code`
- `mobile_plan` 已支持渠道政策套餐字段、折实月费、官方月费、合约期、优惠截止、折算公式等信息
- `mobile_plan_offer` 已支持套餐附加优惠权益
- `mobile_plan_order` 已保存套餐关联和套餐快照
- `mobile_plan_order.customer_identity` 当前约定：
  - `0`：自营客户
  - `1`：留学生
- `mobile_plan_order` 当前办理信息字段包括：
  - `has_offer`
  - `has_pass_or_hkid`
  - `expected_start_date`
  - `id_type`
  - `id_no`
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
  - 证件号码
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

### 3.5 协作规则

- Git commit 使用中文 Conventional Commits
- Codex 完成一个功能或阶段后，需要主动询问用户是否提交
- 除非用户明确要求立即提交，否则不要默认直接提交
- 项目进度记录拆分为：
  - `PROJECT_STATUS.md`：当前状态快照
  - `PROJECT_HISTORY.md`：历史推进记录

## 4. 进行中

- 将当前登录客户与后续套餐需求、订单提交关联
- 继续完善移动套餐确认办理字段和交互
- 后续需要启动 Spring Boot Web 服务，验证前端真实调用后端接口
- Redis 代码已接入，但还需要在本机 Redis 服务运行状态下做真实缓存命中验证
- AI 客服目前只是前端展示，后续再接真实能力

## 5. 阻塞问题

- Docker 尚未确认可用
- Redis 服务尚未完成真实运行验证
- Spring Boot 后端尚未作为 Web 服务完整联调验证
- 当前前端开发服务可访问，但如果后端未启动，真实接口会不可用

## 6. 下一步

1. 将当前登录客户与后续套餐需求、订单提交关联
2. 获取领功 API 文档后，设计线索表和异步推送记录
3. 开发老年关怀模式的一键微信人工客服入口
4. 启动或确认 Redis，验证套餐缓存命中和降级行为

## 7. 最近验证结果

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

最近提交：

```text
1f59a04 docs: 新增阶段完成后提交确认规则
34983c3 feat: 完善移动套餐确认办理流程
565e492 feat: 新增移动套餐 Redis 缓存
472d043 feat: 新增移动套餐优惠模型和展示
```
