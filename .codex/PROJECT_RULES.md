# CMHK 项目开发规定

本文档是本项目后续开发的长期协作规则。Codex 每次继续开发前，应优先阅读本文件，再读取 `.codex/PROJECT_STATUS.md`，最后阅读相关源码。

## 1. 用户背景与对话策略

- 用户主要熟悉 Spring Boot / Java 后端开发，不假设用户熟悉前端工程化。
- 解释前端时，优先使用后端类比：
  - `package.json` 类似 Maven 的 `pom.xml`
  - `npm install` 类似下载 Maven 依赖
  - `npm run dev` 类似启动本地开发服务
  - `src/main.ts` 类似应用启动入口
  - `App.vue` 类似一个页面 Controller + View 的组合
  - `axios` 类似后端调用 HTTP 接口的客户端
- 回答时不要只讲方案。涉及开发任务时按顺序说明：
  1. 需求分析
  2. 需要修改的文件
  3. 实际修改代码
  4. 解释代码
  5. 如何运行和测试
- 对前端概念要讲清楚“为什么需要它”，避免只给命令。
- 用户遇到报错时，优先解释报错含义，再给可复制执行的命令。

## 2. 项目定位

- 项目名称：CMHK 智能业务办理网站
- 当前阶段：从 0 搭建项目
- 架构：前后端分离
- 前端：Vue 3 + Vite + TypeScript + Axios
- 后端：Spring Boot 3 + Java 21 + MyBatis Plus
- 数据库：MySQL
- 缓存：Redis

## 3. 目录规定

```text
cmhkProject/
  backend/                 Spring Boot 后端
  frontend/                Vue3 前端
  docker-compose.yml       本地 MySQL 和 Redis
  README.md                面向运行使用的说明
  .codex/PROJECT_RULES.md  面向后续开发协作的规则
  .codex/PROJECT_STATUS.md 面向项目推进阶段的记录
```

后端目录：

```text
backend/src/main/java/com/cmhk/business/
  common/                  通用响应、异常、工具类
  config/                  Spring 配置
  module/                  业务模块
```

前端目录：

```text
frontend/src/
  api/                     后端接口封装
  App.vue                  当前首页
  main.ts                  Vue 启动入口
  styles.css               全局样式
```

## 4. 后端开发规定

- 后端以 Spring Boot 分层结构为主：
  - `controller`：接收 HTTP 请求
  - `service`：业务逻辑
  - `mapper`：数据库访问
  - `entity`：数据库实体
- 接口统一返回 `ApiResponse<T>`。
- 接口路径统一放在 `/api/**` 下。
- 数据库字段使用下划线命名，Java 属性使用驼峰命名。
- 新增业务表时，同时维护 `backend/src/main/resources/db/schema.sql`。
- 不在 Controller 中堆复杂业务逻辑。
- `backend/src/main/resources/application.yml` 可以提交，但不能写任何真实本机配置，包括数据库地址、账号、密码、Redis 地址、Redis 密码、第三方服务地址、密钥、Token 等。
- 所有真实本机配置统一写入 `backend/src/main/resources/application-local.yml`，该文件必须被 `.gitignore` 忽略，不提交。
- 如需说明本机配置格式，维护 `backend/src/main/resources/application-local.example.yml`。

## 5. 前端开发规定

- 本项目页面主要给移动端用户使用，后续前端采用移动端 H5 优先设计。
- 默认设计宽度按手机屏幕考虑，重点适配 `375px`、`390px`、`414px` 等常见移动端宽度。
- 页面结构优先采用：
  - 顶部导航栏
  - 内容卡片或表单区域
  - 底部操作按钮或底部 Tab
- 避免使用桌面后台常见的左侧固定菜单、大面积表格、复杂多列布局。
- 表单输入、按钮、业务卡片要适合手指点击，主要按钮高度建议不低于 `44px`。
- 移动端页面要优先考虑纵向滚动，不依赖横向滚动。
- 后续如需要桌面端，只作为兼容适配，不作为第一设计目标。
- 前端优先保持简单，不引入复杂状态管理，除非业务需要。
- 后端接口统一写在 `frontend/src/api/` 下。
- 页面组件优先写清楚，不为了“高级写法”牺牲可读性。
- 新增页面或复杂组件时，需要解释：
  - 页面入口在哪里
  - 数据从哪个接口来
  - 用户操作会调用哪个函数
- 用户熟悉后端，所以解释前端请求链路时优先写成：
  - 页面加载
  - 调用 API 方法
  - Axios 请求后端
  - 后端 Controller 返回数据
  - 页面刷新数据

## 6. 本地运行规定

前端启动：

```powershell
cd D:\cmhkProject\frontend
npm.cmd run dev
```

如果 PowerShell 已允许脚本执行，也可以使用：

```powershell
npm run dev
```

后端启动：

```powershell
cd D:\cmhkProject\backend
mvn spring-boot:run
```

MySQL 和 Redis：

```powershell
cd D:\cmhkProject
docker compose up -d
```

前端端口以 Vite 终端输出为准。若 `5173` 被占用，可能自动切换到 `5174` 或 `5175`。

## 7. 当前环境备注

- 用户本机 PowerShell 已能运行 Node.js。
- 用户曾遇到 `npm.ps1` 被 PowerShell 执行策略拦截，推荐优先使用 `npm.cmd`。
- Codex 当前进程可能暂时读不到用户新安装后的 PATH；遇到这种情况时，不要误判为用户没安装，先结合用户终端输出判断。

## 8. 协作输出规定

每次完成代码修改后，回复需要包含：

- 修改了哪些文件
- 改动解决了什么问题
- 用户下一步如何运行
- 如果未能执行测试，说明原因

不要在用户还没理解前端基础时一次性引入太多前端库。

## 9. 项目进度记忆规定

- 项目推进状态记录在 `.codex/PROJECT_STATUS.md`。
- 后续继续开发前，必须先读取 `.codex/PROJECT_RULES.md` 和 `.codex/PROJECT_STATUS.md`。
- 每完成一个阶段或重要功能后，需要更新 `.codex/PROJECT_STATUS.md`。
- 不要只依赖聊天上下文记忆项目进度，重要进度必须写回文件。

## 10. 多代理协作规定

- 用户要求拆分子任务时，主代理负责协调和最终决策，只接收子代理的干净摘要。
- Git 提交代理负责检查提交范围、忽略规则、提交风险和建议提交信息；除非用户明确要求，不直接提交。
- 进度读取与更新代理负责读取 `.codex/PROJECT_RULES.md` 和 `.codex/PROJECT_STATUS.md`，检查进度缺口并输出更新建议。
- 任务讨论代理负责整理需求问题、优先级、接口/表设计和页面任务，不直接修改代码。
- 子代理摘要回来后，主代理负责整合结论、执行必要修改，并保持主上下文简洁。
