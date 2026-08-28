# CMHK 项目跨设备迁移与 Codex 交接文档

## 1. 文档目的

本文用于将 JOINCOM × CMHK 项目从旧电脑完整迁移到新电脑，并让新设备上的 Codex 能够继续开发。

迁移对象不只是 GitHub 中的代码，还包括：

- Git 仓库、分支和全部有效提交；
- MySQL `cmhk` 数据库中的表结构与业务数据；
- 本机未提交到 Git 的 Spring Boot、MySQL、Redis 和 Token 配置；
- 必要的原始导入文件、数据库备份及其他本地业务文件；
- Codex 项目规则、当前状态和历史记录。

Redis 当前主要承担缓存职责，通常可以在新设备重新生成，无须迁移缓存数据。

## 2. 项目基本信息

| 项目项 | 当前约定 |
| --- | --- |
| 项目名称 | JOINCOM × CMHK 智能业务办理网站 |
| 正式开发目录 | `D:\cmhkProject` |
| 正式开发分支 | `main` |
| GitHub 仓库 | `git@github.com:odenmn/cmhkProject.git` |
| 后端 | Spring Boot 3、Java 21、MyBatis-Plus |
| C 端 | `frontend/`，Vue 3 + Vite + TypeScript |
| 管理端 | `admin-frontend/`，Vue 3 + Vite + TypeScript + Element Plus |
| 数据库 | MySQL，数据库名 `cmhk` |
| 缓存 | Redis |
| 后端默认端口 | `8080` |
| C 端开发端口 | 通常为 `5173`，以 Vite 输出为准 |
| 管理端开发端口 | 通常为 `5174`，以 Vite 输出为准 |

新设备上的 Codex 开始工作前，必须完整读取：

1. `.codex/PROJECT_RULES.md`
2. `.codex/PROJECT_STATUS.md`
3. `.codex/PROJECT_HISTORY.md`
4. 当前阶段涉及的设计文档、数据库脚本和源码

## 3. 迁移前的重要安全原则

1. 不要删除、清空或覆盖旧电脑上的项目和数据库，直到新电脑验收完成。
2. 不要使用 `git reset --hard` 或 `git checkout --` 清理未提交修改。
3. 所有未提交修改必须先确认归属，再按功能分别提交并推送。
4. 数据库备份、客户 JSON、证件资料、密码和 Token Secret 不得上传到公开 GitHub。
5. `application-local.yml` 和 `.env` 必须保持在 Git 忽略列表中。
6. 数据库迁移使用“旧库只读导出、新库恢复、核验后切换”的方式。
7. 新设备验收完成前，旧设备作为可回滚副本保留。

## 4. 本文编写时的现场提醒

本文创建时，当前 Codex 临时工作树处于分支：

```text
codex/customer-backup-simulation
```

该工作树存在尚未提交的代码、测试、进度文档和临时文件。它们不一定已经存在于 GitHub 的 `main` 分支。

正式迁移前必须在 `D:\cmhkProject` 主目录重新检查以下内容：

```powershell
cd D:\cmhkProject
git branch --show-current
git status --short --branch
git log --oneline origin/main..main
git branch -vv
```

预期结果：

- 正式目录位于 `main`；
- 工作树没有遗漏的业务修改；
- `origin/main..main` 没有未推送提交；
- 其他仍需保留的开发分支也已经推送到 GitHub。

不要把 Codex 临时 worktree 中的提交默认视为 `main` 已包含。需要先在主目录合并、拣选或重新按功能提交，再推送远端。

## 5. 旧电脑迁出步骤

### 5.1 检查并推送 Git 内容

进入正式项目目录：

```powershell
cd D:\cmhkProject
git status --short --branch
git remote -v
git fetch origin
git log --oneline origin/main..main
```

如果存在未提交修改，先根据实际功能拆分提交。不要把无关功能、数据库临时文件和敏感配置混入同一次提交。

确认提交后推送：

```powershell
git push origin main
```

如果还有必须保留的功能分支，分别推送：

```powershell
git push -u origin <分支名>
```

最后确认：

```powershell
git status --short --branch
git log --oneline origin/main..main
```

`origin/main..main` 没有输出，才表示本地 `main` 没有尚未推送的提交。

### 5.2 检查敏感文件没有进入 Git

执行：

```powershell
git ls-files | Select-String -Pattern "application-local|\.env$|\.sql$|客户数据备份|\.json$"
```

重点确认以下内容没有上传：

- `backend/src/main/resources/application-local.yml`；
- `.env`；
- MySQL 数据库备份；
- 客户数据 JSON 或其他含个人资料的文件；
- 数据库密码、Redis 密码、管理员密码、Token Secret；
- `node_modules/`、`dist/`、`target/`。

如果敏感信息曾经进入 Git 历史，仅删除当前文件不够，需要停止迁移并处理 Git 历史和密钥轮换。

### 5.3 导出 MySQL 数据库

先创建仅用于迁移的安全目录，例如：

```powershell
New-Item -ItemType Directory -Force D:\cmhk-transfer
```

使用本机实际 MySQL 安装版本对应的 `mysqldump.exe`。下面路径仅作示例：

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqldump.exe" `
  --host=127.0.0.1 `
  --port=3306 `
  --user=root `
  --password `
  --single-transaction `
  --routines `
  --triggers `
  --events `
  --default-character-set=utf8mb4 `
  --result-file="D:\cmhk-transfer\cmhk-full.sql" `
  cmhk
```

命令会交互式询问密码，不要把密码直接写进命令或文档。

导出后记录文件摘要，便于确认传输没有损坏：

```powershell
Get-FileHash D:\cmhk-transfer\cmhk-full.sql -Algorithm SHA256
```

数据库备份包含真实业务数据，必须加密保存并通过私人渠道传输，不得放入项目仓库。

### 5.4 保存本机私有配置

需要安全迁移或在新电脑重新生成：

```text
backend/src/main/resources/application-local.yml
.env
```

其中可能包含：

- MySQL 地址、账号和密码；
- Redis 地址和密码；
- C 端访问令牌签名密钥；
- 管理员访问令牌签名密钥；
- 本地管理员启动配置。

建议将私有配置与数据库备份一起放入加密压缩包。不要通过 GitHub 传输。

### 5.5 保存 Git 之外的业务文件

根据是否仍需复现或审计，单独检查并安全迁移：

- CMHK 客户数据备份 JSON；
- ICCID、对账或结算导入文件；
- `D:\download` 下仍需保留的数据库备份；
- 尚未纳入仓库的业务说明和验收材料。

含客户数据的文件必须按敏感数据处理，不得上传公开仓库。

### 5.6 建议的迁移包结构

```text
cmhk-transfer/
  cmhk-full.sql
  cmhk-full.sql.sha256.txt
  private-config/
    application-local.yml
    .env
  business-source-files/
    需要保留的原始导入文件
  MIGRATION-NOTES.txt
```

整个目录应加密后再传输。Git 仓库代码直接通过 GitHub 获取，无需把 `.git` 目录复制进迁移包。

## 6. 新电脑恢复步骤

### 6.1 安装基础环境

新电脑至少需要：

- Git；
- Java 21；
- Maven；
- Node.js 和 npm；
- MySQL；
- Redis，或可运行 Docker Compose 的 Docker Desktop；
- Codex/ChatGPT 桌面应用。

安装后检查：

```powershell
git --version
java -version
mvn -version
node --version
npm.cmd --version
```

### 6.2 克隆正式仓库

```powershell
git clone git@github.com:odenmn/cmhkProject.git D:\cmhkProject
cd D:\cmhkProject
git checkout main
git pull origin main
git status --short --branch
```

如果新电脑尚未配置 GitHub SSH Key，也可以临时使用仓库的 HTTPS 地址克隆。

### 6.3 恢复本机配置

如果不直接复制旧配置，先从示例创建：

```powershell
Copy-Item `
  D:\cmhkProject\backend\src\main\resources\application-local.example.yml `
  D:\cmhkProject\backend\src\main\resources\application-local.yml

Copy-Item D:\cmhkProject\.env.example D:\cmhkProject\.env
```

然后填写新电脑实际的 MySQL、Redis和Token配置。

恢复后确认私有文件仍被 Git 忽略：

```powershell
git status --short
git check-ignore backend/src/main/resources/application-local.yml
git check-ignore .env
```

### 6.4 恢复 MySQL 数据库

先创建数据库：

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" `
  --host=127.0.0.1 `
  --port=3306 `
  --user=root `
  --password `
  --execute="CREATE DATABASE IF NOT EXISTS cmhk CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

再恢复备份：

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" `
  --host=127.0.0.1 `
  --port=3306 `
  --user=root `
  --password `
  cmhk `
  --execute="SOURCE D:/cmhk-transfer/cmhk-full.sql"
```

恢复前后核对 SQL 文件 SHA-256。不要对旧电脑数据库执行删除或覆盖操作。

### 6.5 启动 Redis

如果使用 Docker Compose：

```powershell
cd D:\cmhkProject
docker compose up -d redis
```

Redis 当前主要保存缓存，新设备可以从空 Redis 启动。后端会在访问业务数据时重新建立缓存。

### 6.6 安装依赖并验证构建

后端：

```powershell
cd D:\cmhkProject\backend
mvn test
```

C 端：

```powershell
cd D:\cmhkProject\frontend
npm.cmd ci
npm.cmd run build
```

管理端：

```powershell
cd D:\cmhkProject\admin-frontend
npm.cmd ci
npm.cmd run build
```

两个前端目录均有 `package-lock.json`，优先使用 `npm.cmd ci` 获得与锁文件一致的依赖。

### 6.7 启动项目

后端：

```powershell
cd D:\cmhkProject\backend
mvn spring-boot:run
```

C 端：

```powershell
cd D:\cmhkProject\frontend
npm.cmd run dev
```

管理端：

```powershell
cd D:\cmhkProject\admin-frontend
npm.cmd run dev
```

不要重复启动后端占用 `8080`。端口被占用时，应先确认现有进程归属，再决定是否关闭。

## 7. 新设备验收清单

### 7.1 Git 验收

- [ ] 当前目录为 `D:\cmhkProject`。
- [ ] 当前分支为 `main`。
- [ ] `git status` 没有未知修改。
- [ ] 旧电脑上需要保留的提交均能在新电脑查询到。
- [ ] 必要的功能分支已推送并可拉取。
- [ ] 私有配置和敏感数据没有被 Git 跟踪。

### 7.2 数据库验收

- [ ] `cmhk` 数据库恢复成功。
- [ ] 核心表、管理端表和阶段改造新增表均存在。
- [ ] 客户、订单、ICCID 数量与旧电脑一致。
- [ ] 客户、订单、渠道、ICCID 关联关系抽样正确。
- [ ] 管理员账户存在，密码仍只保存为 BCrypt 哈希。
- [ ] 不通过 `schema.sql` 覆盖或清空已恢复的真实数据。

### 7.3 功能验收

- [ ] 后端测试通过。
- [ ] C 端类型检查和生产构建通过。
- [ ] 管理端类型检查和生产构建通过。
- [ ] `GET /api/health` 正常。
- [ ] 管理端无 Token 访问受保护接口返回 `401`。
- [ ] 管理员登录后携带 Bearer Token 可正常访问接口。
- [ ] ICCID、客户、订单、接龙、任务、返现等当前功能能够读取恢复后的数据。
- [ ] Redis 不可用时的降级行为和 Redis 恢复后的缓存行为正常。

## 8. 新设备上的 Codex 交接提示词

在 Codex 中打开 `D:\cmhkProject` 后，新建任务并发送：

```text
接手 D:\cmhkProject 的 JOINCOM × CMHK 项目。

开始前必须完整读取：
1. .codex/PROJECT_RULES.md
2. .codex/PROJECT_STATUS.md
3. .codex/PROJECT_HISTORY.md
4. 当前阶段相关设计文档、schema.sql 和源码

然后检查 git branch、git status、最近提交和远端同步状态。
以 D:\cmhkProject 的 main 为正式开发目录和分支，保留全部未提交修改，
禁止 reset/checkout。先汇报当前进度、数据库兼容情况和下一步，不要直接改代码，
未经明确同意不要进行 Git 提交，也不要输出 application-local.yml 中的密码或密钥。
```

不要只依赖旧设备上的聊天记录。项目规则、当前状态、历史决策和重要验收结果应以仓库内 `.codex/` 文档为交接依据。

## 9. 回滚方式

如果新电脑恢复失败：

1. 停止新电脑上的后端和前端进程；
2. 不修改旧电脑的数据库和项目目录；
3. 删除新电脑上失败的临时数据库前，先确认目标确实是新设备测试库；
4. 重新校验 Git 提交、SQL 备份摘要和私有配置；
5. 在新设备创建空的 `cmhk` 数据库后重新导入；
6. 新设备全部验收通过后，再决定是否停用旧设备环境。

旧电脑在验收完成前就是完整回滚点，不要提前格式化、清理或转让。

## 10. 迁移完成记录

迁移时填写：

```text
旧设备迁出日期：
新设备恢复日期：
迁移执行人：
GitHub main 最新提交：
额外迁移分支：
数据库备份文件名：
数据库备份 SHA-256：
旧库客户数量：
新库客户数量：
旧库订单数量：
新库订单数量：
旧库 ICCID 数量：
新库 ICCID 数量：
后端测试结果：
C 端构建结果：
管理端构建结果：
遗留问题：
```
