# 智能停车场管理系统

`dev` 是团队的开发和集成分支。按下面的顺序启动 PostgreSQL、后端和前端，就可以在本机打开管理系统。各业务页面以 `dev` 实际合入的内容为准。

## 本机启动

需要 JDK 21、PostgreSQL 17 和 Node.js 22.12 或更高版本（也可用 20.19 及以上的 Node.js 20）。下列终端命令使用 PowerShell 7.2 或更高版本。

### 1. 启动 PostgreSQL

使用自己的 PostgreSQL 实例，数据库名为 `smart_parking`。首次使用空库时，按[数据库初始化说明](database/README.md)依次导入建表和演示数据；已有库不需要重建或重复导入。记下实际端口和数据库用户名。

### 2. 启动后端

在仓库根目录打开终端。示例使用 `15432`；如果本机数据库使用 `5432` 或其他端口，修改环境变量里的连接地址，不改共享配置。

```powershell
cd backend
$env:DB_URL = 'jdbc:postgresql://localhost:15432/smart_parking'
$env:DB_USERNAME = Read-Host 'PostgreSQL 用户名'
$env:DB_PASSWORD = Read-Host 'PostgreSQL 密码' -MaskInput
.\mvnw.cmd spring-boot:run
```

也可在 IDEA 的 `BackendApplication` 运行配置里填写同样的环境变量。这里输入的是 **PostgreSQL 密码**，不是系统网页的演示密码。后端默认使用 `8081`，与前端开发代理保持一致。

### 3. 启动前端

另开终端，从同一个仓库根目录执行：

```powershell
cd frontend
npm ci
npm run dev
```

打开终端显示的前端地址，默认是 [http://localhost:5173/login](http://localhost:5173/login)。不要用后端 `8081` 端口打开网页。开发环境由 Vite 把 `/api` 请求转发到后端。

## 当前可演示的入口

用[演示账号](database/README.md#演示账号)登录后，管理员可以进入管理首页、账号管理、停车场管理和停车区域。管理首页可按停车场查看车位状态、入离场摘要和区间实收。尚未产生入离场或支付流水时，相应统计为零；这与接口报错不同。车主可以用车主账号登录并查看该角色的导航，尚未接入的菜单会提示当前状态。

如果登录显示「系统繁忙」，先看后端控制台里的 `Caused by:`。连接被拒绝时确认 PostgreSQL 是否启动、端口是否填对；密码认证失败时检查后端环境变量；出现表不存在错误时确认连接的是已初始化的数据库。

## 两机复现与验收

组员使用同一个 `dev` 提交开始，各自记录提交号、数据库端口、实际启动流程以及可打开的页面，把实测结果填入[里程碑验收记录](docs/milestones/停车场项目_里程碑验收记录.md)。完整演示要等车辆入场、离场支付、停车记录和统计接通后，再走一次完整流程并核对实收。

业务分工和接口请见[三人成员任务书](docs/implementation/02_三人成员任务书.md)和[接口与数据契约](docs/implementation/01_接口与数据契约.md)。
