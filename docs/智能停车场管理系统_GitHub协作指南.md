# 智能停车场管理系统 GitHub 协作指南

本指南面向没有 Git 和 GitHub 协作经验的组员，用于指导智能停车场管理系统的共同开发。内容覆盖首次建立仓库、日常开发、前后端与数据库协作、Pull Request、冲突处理和常见故障。

> 核心原则：`main` 只保存可运行代码，每项功能使用独立分支，通过 Pull Request 合并。

## 目录

- [1 当前项目约定](#1-当前项目约定)
- [2 需要理解的基本概念](#2-需要理解的基本概念)
- [3 项目目录与文件管理](#3-项目目录与文件管理)
- [4 首次发布项目](#4-首次发布项目)
- [5 组员第一次配置](#5-组员第一次配置)
- [6 每次开发的标准流程](#6-每次开发的标准流程)
- [7 Pull Request 与合并](#7-pull-request-与合并)
- [8 分支和提交命名](#8-分支和提交命名)
- [9 数据库协作](#9-数据库协作)
- [10 合并冲突处理](#10-合并冲突处理)
- [11 常见情况与处理](#11-常见情况与处理)
- [12 团队任务分配建议](#12-团队任务分配建议)
- [13 每日操作检查清单](#13-每日操作检查清单)
- [14 常用命令速查](#14-常用命令速查)
- [15 推荐工作顺序](#15-推荐工作顺序)

## 1 当前项目约定

本项目使用一个仓库管理前端、后端、数据库脚本和文档。本地数据库和密码不是共享文件，团队共享的是源代码、SQL 脚本和配置模板。

| 项目项 | 统一约定 |
| --- | --- |
| 仓库 | <https://github.com/xiaoze0216/smart-parking-system> |
| 当前仓库根目录 | `D:\Course\大三上\DB\db-app` |
| 后端 | `backend`，Spring Boot 4.1.1，Java 21，Maven，MyBatis |
| 前端 | `frontend`，Vue 3，Vite，Axios，Element Plus |
| 数据库 | PostgreSQL，项目数据库名建议为 `smart_parking` |
| 当前开发机端口 | 前端 `5173`，后端 `8081`，PostgreSQL `15432` |
| 稳定分支 | `main` |

### 1.1 最重要的六条规则

1. 不要直接在 `main` 上开发业务功能。
2. 开始任务前先同步最新 `main`，再创建自己的功能分支。
3. 一个分支只完成一项明确任务，不把无关修改混在一起。
4. 提交前查看 `git status` 和 `git diff`，并完成基本运行测试。
5. 不上传数据库密码、物理数据目录、依赖目录和构建结果。
6. 合并冲突时先理解双方代码，不盲目选择全部保留某一方。

## 2 需要理解的基本概念

| 概念 | 含义 | 在本项目中的用法 |
| --- | --- | --- |
| 仓库 Repository | 保存完整项目及修改历史 | 同时包含 `backend`、`frontend`、`database` 和 `docs` |
| 分支 Branch | 相互隔离的一条开发线 | 每项功能创建一个 `feature` 分支 |
| 提交 Commit | 保存在本机的一次版本快照 | 说明本次完成了什么 |
| 推送 Push | 把本机提交上传至 GitHub | 让组员和 PR 能看到修改 |
| 拉取 Pull | 下载并整合远程最新修改 | 开始工作前更新 `main` |
| PR | 请求把分支合并到 `main` | 组员检查、测试、确认后合并 |
| 冲突 Conflict | Git 无法自动判断两份修改如何组合 | 由修改相关代码的组员共同处理 |

## 3 项目目录与文件管理

仓库根目录统一使用以下结构：

```text
smart-parking-system/
├── backend/                 Spring Boot 后端
├── frontend/                Vue 前端
├── database/
│   ├── 01_schema.sql        建表、主键、外键和约束
│   ├── 02_seed_data.sql     演示数据
│   └── README.md            数据库初始化说明
├── docs/
│   ├── ER 图
│   └── 协作与设计文档
└── .gitignore
```

### 3.1 必须上传的文件

- 后端源代码、`pom.xml`、`mvnw`、`mvnw.cmd` 和 Maven Wrapper 文件。
- 前端 `src`、`public`、`package.json`、`package-lock.json` 和 Vite 配置。
- 数据库结构 SQL、少量演示数据和数据库说明。
- 需求分析、ER 图、接口说明和课程报告。
- 不含真实密码的 `application.properties` 与 `.env.example`。

### 3.2 禁止上传的文件

- 真实数据库密码、令牌、私钥或个人账号信息。
- `frontend/node_modules` 和 `frontend/dist`。
- `backend/target`、IDE 缓存和临时日志。
- PostgreSQL 物理数据目录，例如 `D:\pgsql\data`。
- 个人本地配置文件，例如 `.env.local`。

### 3.3 根目录忽略文件建议

项目根目录应有 `.gitignore`：

```gitignore
.idea/
.vscode/
*.iml
**/target/
**/node_modules/
**/dist/
.env
.env.local
*.log
```

## 4 首次发布项目

这一节只需要由一名仓库负责人执行一次。其他组员不要再次运行 `git init`，也不要分别在 `backend` 和 `frontend` 中建立仓库。

### 4.1 上传前检查密码

`application.properties` 应提交，但密码必须使用环境变量：

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:15432/smart_parking}
spring.datasource.username=${DB_USERNAME:Niannya}
spring.datasource.password=${DB_PASSWORD}
```

每名组员在自己的 IDEA 运行配置中设置：

```text
DB_URL=jdbc:postgresql://localhost:自己的端口/smart_parking
DB_USERNAME=自己的数据库用户名
DB_PASSWORD=自己的数据库密码
```

不同电脑可以使用不同的端口、用户名和密码，不需要修改已经提交的配置文件。

### 4.2 初始化并连接远程仓库

```powershell
cd 'D:\Course\大三上\DB\db-app'
git init
git branch -M main
git remote add origin https://github.com/xiaoze0216/smart-parking-system.git
git remote -v
```

### 4.3 第一次提交

```powershell
git status
git add .
git status
git commit -m "chore: initialize project framework"
git push -u origin main
```

第二次执行 `git status` 是为了检查暂存区。如果看到 `node_modules`、`target`、数据库密码或无关大文件，应停止提交，先修正 `.gitignore` 或配置。

### 4.4 邀请组员

仓库所有者进入：

```text
GitHub 仓库 → Settings → Collaborators → Add people
```

组员接受邀请后才能访问私有仓库。建议暂时不要授予删除仓库或修改关键设置的权限。

## 5 组员第一次配置

每位组员只需要克隆一次仓库，不要在自己电脑上新建另一套同名项目再复制文件。

### 5.1 克隆项目

```powershell
git clone https://github.com/xiaoze0216/smart-parking-system.git
cd smart-parking-system
git status
```

### 5.2 准备开发环境

1. 用 IntelliJ IDEA 打开 `backend`，等待 Maven 完成依赖下载。
2. 进入 `frontend` 目录执行 `npm install`，不要上传生成的 `node_modules`。
3. 安装并启动 PostgreSQL，在本机创建 `smart_parking` 数据库。
4. 按照 `database` 目录说明运行建表和演示数据脚本。
5. 在 IDEA 运行配置中填写本机 `DB_URL`、`DB_USERNAME` 和 `DB_PASSWORD`。
6. 依次启动 PostgreSQL、Spring Boot 和 Vue，确认项目基线能够运行。

### 5.3 每台电脑允许不同的配置

| 配置 | 当前开发机示例 | 组员是否可不同 |
| --- | --- | --- |
| PostgreSQL 端口 | `15432` | 可以，常见默认值为 `5432` |
| 数据库用户名 | `Niannya` | 可以 |
| 数据库密码 | 仅保存在本机 | 必须各自配置 |
| 后端端口 | `8081` | 建议团队统一 |
| 前端端口 | `5173` | 建议团队统一 |

## 6 每次开发的标准流程

以下流程是所有组员日常使用最多的部分。示例任务为开发车位列表。

### 6.1 开始任务

```powershell
git switch main
git pull origin main
git switch -c feature/parking-space-list
```

如果该功能分支已经存在，使用：

```powershell
git switch feature/parking-space-list
```

不要再次添加 `-c`。

### 6.2 开发与自测

- [ ] 只修改本任务需要的文件。
- [ ] 后端能启动，相关接口能返回正确数据。
- [ ] 前端页面能加载，浏览器控制台没有新增错误。
- [ ] 涉及数据库时，SQL 脚本已同步更新。
- [ ] 没有把本机密码和生成目录加入项目。

### 6.3 检查并提交

```powershell
git status
git diff
git add backend/src/... frontend/src/... database/...
git status
git commit -m "feat: add parking space list"
```

初学阶段建议明确写出需要暂存的文件路径。确认所有变化都属于同一任务后，也可以使用 `git add .`。

### 6.4 上传功能分支

```powershell
git push -u origin feature/parking-space-list
```

第一次推送该分支需要 `-u`。之后继续修改并提交时，只需执行：

```powershell
git push
```

## 7 Pull Request 与合并

功能分支推送后，在 GitHub 创建 Pull Request。PR 的目标分支选择 `main`，来源分支选择本次功能分支。不要直接点击合并，至少由另一名组员完成一次检查。

### 7.1 PR 描述应包含

- [ ] 本次完成的功能和对应页面或接口。
- [ ] 修改了哪些数据库表或 SQL 文件。
- [ ] 如何测试，以及实际测试结果。
- [ ] 是否有尚未解决的问题。
- [ ] 必要时附上页面截图或接口响应。

### 7.2 审查者检查

- [ ] 代码是否属于该任务，有没有混入无关修改。
- [ ] 命名、接口路径和数据库字段是否符合团队约定。
- [ ] 密码、`node_modules`、`target` 等是否被误提交。
- [ ] 后端和前端能否运行，关键业务路径是否正确。
- [ ] SQL 修改是否包含约束并能在空数据库执行。

### 7.3 合并后的本地清理

```powershell
git switch main
git pull origin main
git branch -d feature/parking-space-list
```

只有在 GitHub 已经成功合并，并且本地 `main` 已同步后，才删除本地功能分支。

## 8 分支和提交命名

| 用途 | 分支或提交前缀 | 示例 |
| --- | --- | --- |
| 新增功能 | `feature` 或 `feat` | `feature/vehicle-entry`；`feat: add vehicle entry API` |
| 修复错误 | `fix` | `fix/duplicate-entry`；`fix: prevent duplicate active records` |
| 文档 | `docs` | `docs/er-diagram`；`docs: add database ER diagram` |
| 配置与维护 | `chore` | `chore/config`；`chore: update development configuration` |
| 代码整理 | `refactor` | `refactor/fee-service`；`refactor: simplify fee calculation` |
| 测试 | `test` | `test/parking-record`；`test: add parking record tests` |

提交信息应说明本次完成了什么。不要使用 `update`、`修改一下`、`最终版`、`final2` 等无法理解的描述。

## 9 数据库协作

每名组员运行自己的 PostgreSQL，团队通过 SQL 文件保持数据库结构一致。不要把某个人的 `D:\pgsql\data` 上传或复制给其他人。

### 9.1 SQL 文件职责

| 文件 | 内容 | 要求 |
| --- | --- | --- |
| `01_schema.sql` | 建表、主键、外键、唯一约束、检查约束和索引 | 应能在空 `smart_parking` 数据库中完整执行 |
| `02_seed_data.sql` | 少量停车区域、车位、车辆和计费规则演示数据 | 不得包含真实个人信息 |
| `README.md` | 创建数据库、执行顺序、版本变化和注意事项 | 新组员应能按文档独立完成初始化 |

### 9.2 修改表结构时

1. 在自己的功能分支修改 SQL 文件。
2. 在本机测试脚本，确认约束和外键有效。
3. 同步修改 Entity、Mapper、接口以及前端字段。
4. 在 PR 中明确说明数据库变化和重新初始化步骤。
5. 其他组员合并后，根据说明更新自己的本地数据库。

### 9.3 现有 university 数据库

`university` 是前期框架测试数据库，可以继续保留。停车场项目连接 `smart_parking` 后，普通查询不会访问 `university` 中的教师和课程表。

原有 `Instructor` 测试代码可以暂时保留，在停车场第一个业务接口完成后删除。

## 10 合并冲突处理

冲突不代表项目损坏，只表示 Git 无法判断两份修改应该如何组合。发生冲突后不要继续反复 `pull` 或强制推送。

### 10.1 常见冲突标记

```text
<<<<<<< HEAD
本分支中的内容
=======
main 中的内容
>>>>>>> main
```

### 10.2 处理步骤

1. 运行 `git status`，记录所有标记为冲突的文件。
2. 联系修改同一文件的组员，确认最终业务逻辑。
3. 编辑文件，保留正确组合并删除全部冲突标记。
4. 重新运行后端、前端或 SQL 测试。
5. 暂存冲突文件并提交解决结果。

```powershell
git status
git add path/to/resolved-file
git commit -m "fix: resolve merge conflict"
git push
```

不要看到 `Accept Current` 或 `Accept Incoming` 就直接点击。`Current` 和 `Incoming` 的含义取决于合并方向，必须先读懂代码。

## 11 常见情况与处理

| 情况 | 处理方式 |
| --- | --- |
| 误在 `main` 上修改但尚未提交 | 立即执行 `git switch -c feature/任务名`，把当前修改带到新分支后再提交 |
| 文件已暂存但不想提交 | 执行 `git restore --staged 文件路径`，仅取消暂存，不删除工作区修改 |
| `push` 被拒绝 | 不要强推。先确认远程分支是否已有新提交，和组员沟通后同步或合并 |
| `pull` 后出现冲突 | 停止继续拉取，按照冲突处理步骤解决、测试和提交 |
| 后端突然连接失败 | 依次检查 PostgreSQL、端口、环境变量和 Spring Boot 控制台 |
| 前端显示数据加载失败 | 先直接访问后端 API，再检查后端、数据库和浏览器控制台 |
| 不确定能否删除文件 | 先不要删除，使用 `git status` 查看并询问相关组员 |

团队成员不得随意使用以下命令：

```text
git reset --hard
git push --force
```

这些命令可能造成尚未合并的工作丢失。也不要随意删除其他组员的分支。

## 12 团队任务分配建议

可以按模块分工，但不要让某个模块只有一个人了解。每个 PR 至少由另一名组员审查，使前端、后端和数据库之间的字段约定保持一致。

| 任务类别 | 示例内容 | 协作要求 |
| --- | --- | --- |
| 数据库 | ER 图、建表、约束、演示数据 | 变更必须同步 SQL 和 README |
| 后端 | 车位、车辆、入场、出场、计费接口 | 接口字段先与前端确认 |
| 前端 | 车位管理、停车记录、统计页面 | 使用统一 API 地址与字段命名 |
| 测试 | 正常流程、重复入场、车位占用、计费边界 | 每个功能合并前至少完成基本测试 |
| 文档 | 需求、ER 图、接口说明、课程报告 | 随代码更新，不在结课前集中补写 |

### 12.1 推荐任务粒度

- 建立停车区域和车位表，而不是一次完成全部数据库。
- 实现车位列表查询接口，而不是一次完成全部后端。
- 实现车位管理页面，而不是一次完成全部前端。
- 实现车辆入场事务，而不是一次同时实现入场、出场、计费和统计。

## 13 每日操作检查清单

### 13.1 开始开发前

- [ ] 确认自己领取的任务和验收条件。
- [ ] 切换到 `main` 并执行 `git pull origin main`。
- [ ] 从最新 `main` 创建或切换到对应功能分支。
- [ ] 确认数据库、后端和前端基线能够运行。

### 13.2 提交前

- [ ] 运行 `git status` 和 `git diff`。
- [ ] 检查没有密码、依赖目录和无关文件。
- [ ] 完成对应接口、页面或 SQL 的基本测试。
- [ ] 提交信息明确说明本次变化。

### 13.3 创建 PR 前

- [ ] 功能分支已经 `push` 到 GitHub。
- [ ] PR 目标是 `main`，来源是自己的功能分支。
- [ ] 描述功能、测试结果和数据库变化。
- [ ] 邀请至少一名组员审查。

### 13.4 合并后

- [ ] 切回 `main` 并拉取最新代码。
- [ ] 确认合并后的项目仍能运行。
- [ ] 删除已经合并的本地功能分支。
- [ ] 在任务列表中标记完成，并领取下一项任务。

## 14 常用命令速查

| 目的 | 命令 |
| --- | --- |
| 查看状态 | `git status` |
| 查看未暂存修改 | `git diff` |
| 查看提交历史 | `git log --oneline --graph --all` |
| 查看远程仓库 | `git remote -v` |
| 切换分支 | `git switch 分支名` |
| 创建并切换分支 | `git switch -c 分支名` |
| 同步 `main` | `git switch main` 后执行 `git pull origin main` |
| 暂存指定文件 | `git add 文件路径` |
| 取消暂存 | `git restore --staged 文件路径` |
| 提交 | `git commit -m "类型: 说明"` |
| 第一次推送分支 | `git push -u origin 分支名` |
| 后续推送 | `git push` |
| 删除已合并本地分支 | `git branch -d 分支名` |

## 15 推荐工作顺序

1. 将当前可运行框架安全提交到 `main`。
2. 建立 `database` 和 `docs` 目录，提交需求清单与第一版 ER 图。
3. 创建 `smart_parking` 数据库并完成基础表结构。
4. 按车位管理、车辆管理、入场、出场计费和统计查询依次开发。
5. 每个功能使用独立分支和 PR，合并后持续更新 SQL 与文档。
6. 最后进行完整流程测试、演示数据准备和课程报告整理。

当前最重要的共同起点不是继续增加功能，而是完成第一次安全提交，让所有组员能够克隆同一份可运行项目。
