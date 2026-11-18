# 成员 3 交付说明：收费规则 / 离场支付 / 停车记录查询

> 交付范围：`02_三人成员任务书.md` 中成员 3 的全部任务（M2-05/06、M4-01/02/03、M5-01/02）。
> 交付环境：本机 JDK 21 + Maven 3.9.16（阿里云镜像）+ PostgreSQL 17.2（zonky 便携二进制，`C:/pgtools`）。
> 所有命令在 `backend/` 目录执行；`mvn` 使用 `tools/apache-maven-3.9.16`（settings.xml 已配阿里云镜像）。

---

## 1. 任务完成情况

| 任务 ID | 内容 | 状态 | 证据 |
|---|---|---|---|
| M2-05 | 收费规则 CRUD（创建即激活、历史保留） | ✅ | `FeeRuleServiceTest` 6 用例通过 |
| M2-06 | 纯计费函数 `FeeCalculator` | ✅ | `FeeCalculatorTest` 18 用例通过 |
| M4-01 | 在场查询 / 费用预览 | ✅ | `ParkingExitServiceTest` 覆盖 |
| M4-02 | 正常离场 + 模拟确认收款 | ✅ | 同上，含三处回滚注入 |
| M4-03 | 异常离场通道 | ✅ | 同上 |
| M5-01 | 管理员记录查询（过滤/排序/详情） | ✅ | `ParkingRecordQueryServiceTest` 9 用例通过 |
| M5-02 | 车主记录查询（隔离/越权） | ✅ | 同上 |
| 前端 | 成员 3 页面与 API 文件 | ✅（语法级） | `node --check` 全部通过；npm registry 不可达，未执行 install/build（见第 6 节） |

## 2. 交付文件清单

### 成员 3 新增（本 PR 主体）

**后端 - 纯计费与规则**
- `backend/src/main/java/org/example/backend/service/fee/FeeCalculator.java` — 纯函数：不访问数据库、不读时钟；三个显式输入（entryTime、exitTime、ruleSnapshot）；秒数向上取整分钟；先扣免费分钟，再按 1440 分钟周期分段、每段封顶；BigDecimal 两位小数（HALF_UP）；`exitTime < entryTime` 拒绝。
- `backend/src/main/java/org/example/backend/service/fee/FeeRuleSnapshot.java` — 规则快照 record（ruleId/ruleName/freeMinutes/billingUnitMinutes/unitPrice/maxFeePer24h）。
- `backend/src/main/java/org/example/backend/service/fee/FeeRuleQuery.java` — 提供给成员 2 的只读契约：`findActiveForLot`（无规则 4303）/ `getSnapshotById`（不存在 4401）/ `findSnapshotByIdOrNull`（记录查询容错）。
- `backend/src/main/java/org/example/backend/service/FeeRuleService.java` + `controller/FeeRuleController.java` + `mapper/FeeRuleMapper.java(.xml)` — `GET /api/fee-rules`、`GET /api/fee-rules/active`（无规则 404/4403）、`POST /api/fee-rules`（创建即激活：锁场→旧规则 `is_active=false, effective_to=now`→插新规则，`createdBy` 取会话）。
- `backend/src/main/java/org/example/backend/dto/feerule/FeeRule.java`、`FeeRuleCreateRequest.java`。

**后端 - 离场/支付/异常通道**
- `backend/src/main/java/org/example/backend/service/ParkingExitService.java` + `controller/ParkingExitController.java` + `mapper/ParkingExitMapper.java(.xml)`：
  - `GET /api/parking-exits/active`：车牌或记录号恰填一个（否则 400）；车牌规范化；无记录 404/4501。
  - `GET /api/parking-exits/{id}/quote`：读**记录绑定的** `feeRuleId`（不取场当前规则）；只接受 PARKING（否则 409/4502）；quote 用服务器当时时刻，不锁定金额。
  - `POST /api/parking-exits/{id}`：一事务——锁在场记录 FOR UPDATE → 读绑定规则/车位 → 计算 → 校验支付（CASH/ELECTRONIC 实收=应收否则 4503；FREE_RELEASE 实收 0 且 remark 非空否则 4504）→ 生成唯一 `paymentNo`（P+31 位十六进制，预检唯一）→ 插 `payment_record` → 条件更新记录 COMPLETED + exitTime/exitAdminId → 条件释放 OCCUPIED→FREE；任一步行数不为 1 抛 4505 回滚；重复 POST 409/4502。
  - `POST /api/parking-exits/{id}/abnormal`：原因必填（否则 400）；锁记录→ABNORMAL + abnormalReason + exitTime/exitAdminId→释放车位；**不插 payment**；重复操作 409/4502。
- `backend/src/main/java/org/example/backend/dto/exit/`：`ActiveRecordDto`、`QuoteDto`、`ExitRequest`、`AbnormalExitRequest`、`ExitResultDto`、`ParkingRecordRow`、`PaymentInsert`。
- `backend/src/main/java/org/example/backend/common/Plates.java` — 车牌规范化公共工具（大小写/空格）。

**后端 - 记录查询**
- `backend/src/main/java/org/example/backend/service/ParkingRecordQueryService.java` + `controller/ParkingRecordQueryController.java` + `mapper/ParkingRecordQueryMapper.java(.xml)`（独立 XML）：
  - `GET /api/parking-records`（ADMIN）：lotId/licensePlate/status/spaceId/from/to 均可省；from/to 须同时提供（否则 400）；空串过滤值 400；非法状态 400；`entryTime DESC, id DESC`。
  - `GET /api/parking-records/{id}`（ADMIN）：无则 404/4601。
  - `GET /api/owner/parking-records`（OWNER）：JOIN `vehicle ON record.vehicle_id=vehicle.id AND vehicle.owner_user_id=会话用户`（不凭车牌快照，不接受客户端传 owner ID）。
  - `GET /api/owner/parking-records/{id}`：非本人 403/4602；不存在 404/4601。
  - 派生字段：`durationMinutes` 同一向上取整规则；PARKING 按查询时刻计算并给 `estimatedAmountDue`、`paymentStatus=UNPAID`、`payment=null`；COMPLETED 正常收款 PAID、FREE_RELEASE 为 WAIVED；ABNORMAL 为 ABNORMAL、`payment=null`；**不返回内部 admin IDs**。
- `backend/src/main/java/org/example/backend/dto/record/`：`RecordDto`、`RecordRow`、`RecordFilter`、`PaymentInfoDto`。

**测试（成员 3）**
- `backend/src/test/java/org/example/backend/service/fee/FeeCalculatorTest.java` — 18 个纯单元用例（0/30/31/60/61/91 分钟、24h/26h/48h 封顶、不同单价与免费时长、秒向上取整、`exit<entry` 拒绝、非法参数）。
- `backend/src/test/java/org/example/backend/service/FeeRuleServiceTest.java` — 6 个数据库用例（T10 规则切换与历史、同场唯一活动规则、无规则 4303、场不存在 2001、停用场 2005、历史规则快照可读）。
- `backend/src/test/java/org/example/backend/service/ParkingExitServiceTest.java` — 12 个数据库用例（T16-T19：跨分钟金额变化、规则切换后按旧规则、重复支付 4502、金额不符 4503、免费放行 4504、异常离场、三处注入回滚、paymentNo 格式与唯一）。
- `backend/src/test/java/org/example/backend/service/ParkingRecordQueryServiceTest.java` — 9 个数据库用例（T20-T22：组合过滤与排序、过滤校验、详情 4601、车主隔离、越权 4602、停用绑定历史可见、临时车历史不开放、PARKING/COMPLETED/ABNORMAL 派生字段）。
- `backend/src/test/java/org/example/backend/support/PostgresIntegrationTest.java` — 数据库集成测试基类（@BeforeAll 重建 schema+种子；@BeforeEach/@AfterEach 清记录并复位规则，保证用例互不依赖、与顺序无关）。

**前端（成员 3）**
- `frontend/src/api/feeRule.js`、`exit.js`、`record.js`、`ownerRecord.js`（相对 `/api`，经 `src/http.js` 统一带会话与 CSRF）。
- `frontend/src/views/admin/FeeRulesView.vue`（场地选择、当前生效规则、创建表单、规则历史表）。
- `frontend/src/views/admin/ParkingExitsView.vue`（按车牌/记录号查在场、费用预览、支付方式与实收、免费放行 remark、异常离场二次确认）。
- `frontend/src/views/admin/ParkingRecordsView.vue`（管理员过滤与详情）。
- `frontend/src/views/owner/ParkingRecordsView.vue`（本人记录与详情）。
- 脚手架：`package.json`、`vite.config.js`（`/api`→8081）、`index.html`、`src/main.js`、`src/App.vue`、`src/router`、`src/http.js`、`src/auth/`、`src/views/LoginView.vue`。

### 为全链路可运行而修/补的公共与成员 2 文件（最小脚手架，可被正式实现替换）
- `common/ParkingLotGuard.java` + `mapper/ParkingLotMapper.java(.xml)`：`lockById` 返回值由 primitive `int` 改为 `Integer`（无行时 MyBatis 报 BindingException）；新增 `selectOptions`。
- `common/UserPrincipal.java` + `auth/UserDetailsServiceImpl.java`：`UserPrincipal` 增加 `password` 字段并返回库中 bcrypt 哈希——此前 `getPassword()` 返回 null 导致所有登录报“用户名或密码错误”（冒烟测试发现并修复）。
- `controller/ParkingLotController.java`：`GET /api/parking-lots/options`（联调流程第 2 步，前端场地选择器数据源）。
- 成员 2 scaffold：`ParkingAreaService/Controller`、`ParkingSpaceService/Controller`、`OwnerVehicleService/Controller`、`ParkingEntryService/Controller` + 对应 Mapper/XML/DTO（入场消费成员 3 的 `FeeRuleQuery.findActiveForLot`，演示跨模块协作）。

## 3. 测试证据

### 3.1 后端自动化测试（真实 PostgreSQL 17.2）

```
cd backend
export JAVA_HOME="C:/Program Files/Java/jdk-21"
../tools/apache-maven-3.9.16/bin/mvn -s ../tools/apache-maven-3.9.16/conf/settings.xml test
```

结果：**Tests run: 45, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS**

| 测试类 | 用例数 | 覆盖 |
|---|---|---|
| `FeeCalculatorTest`（纯函数） | 18 | 边界分钟数、24h 周期封顶、秒向上取整、非法输入 |
| `FeeRuleServiceTest` | 6 | 规则创建即激活、历史保留、唯一活动规则、4303/2001/2005 |
| `ParkingExitServiceTest` | 12 | 跨分钟金额、旧规则绑定计费、重复 4502、4503、4504、异常离场、三处回滚、paymentNo |
| `ParkingRecordQueryServiceTest` | 9 | 组合过滤/排序、from/to 校验、4601/4602、车主隔离、停用可见、临时车不开放、派生字段 |

数据库准备（本机一次性）：
```
# zonky 便携 PostgreSQL（GitHub 不可达，走阿里云 Maven 镜像）
curl -sL -o tools/pg-binaries.jar https://maven.aliyun.com/repository/public/io/zonky/test/postgres/embedded-postgres-binaries-windows-amd64/17.2.0/embedded-postgres-binaries-windows-amd64-17.2.0.jar
# 解包 txz 需 xz；本机用 python -c "import tarfile; tarfile.open(...,'r:xz').extractall(...)"
# 注意：initdb 可执行文件所在路径必须为纯 ASCII（PostgreSQL BUG #16926），故放到 C:/pgtools
C:/pgtools/pg17/bin/initdb.exe -D C:/pgdata -U postgres --auth=trust --no-locale --encoding=UTF8
C:/pgtools/pg17/bin/postgres.exe -D C:/pgdata -p 5432        # 后台常驻
# 建测试库（pgtools/CreateTestDb.java 经 JDBC 执行 CREATE DATABASE）
```

> 若组员机器已有 PostgreSQL：设 `PARKING_DB_URL/PARKING_DB_USER/PARKING_DB_PASSWORD` 后重跑同一命令即可；没有数据库时集成测试按 JUnit 假设**跳过**（不会误报通过），纯函数 18 用例任何环境可跑。

### 3.2 打包

```
../tools/apache-maven-3.9.16/bin/mvn -s ../tools/apache-maven-3.9.16/conf/settings.xml package -DskipTests
→ target/smart-parking-backend-0.0.1-SNAPSHOT.jar（28.7 MB）BUILD SUCCESS
```

### 3.3 端到端冒烟（jar + 真实数据库，全链路）

启动：`java -jar target/smart-parking-backend-0.0.1-SNAPSHOT.jar --server.port=8081`
（注意：本机环境变量 `SERVER__PORT=55002` 会覆盖配置文件，需命令行显式指定端口。）

按 01 契约第 8 节联调顺序实测：

| 步骤 | 结果 |
|---|---|
| `GET /api/auth/csrf` | 200，返回 token 与 headerName |
| `POST /api/auth/login`（admin/admin123456） | 200，建立会话 |
| `GET /api/parking-lots/options` | 200，返回 LOT-001/LOT-002 |
| `GET /api/fee-rules/active?lotId=1` | 200，日常规则（30/60/5.00/50.00） |
| `POST /api/parking-entries`（京A12345） | 200，分到 A/001，绑定规则 1 |
| `GET /api/parking-exits/3/quote` | 200，1 分钟 0.00 元（免费时段） |
| `POST /api/parking-exits/3`（CASH 0.00） | 200，COMPLETED，车位 FREE，paymentNo=P... |
| 重复 `POST /api/parking-exits/3` | **409/4502** |
| `GET /api/parking-records/3` | 200，paymentStatus=PAID，estimatedAmountDue=null |
| `GET /api/parking-records?status=COMPLETED` | 200，按 entryTime DESC 倒序 |
| 创建新规则（9.00）后 `GET /api/parking-exits/2/quote` | 200，**仍按旧规则 5.00**（记录绑定优先） |
| `POST /api/parking-exits/2/abnormal` | 200，ABNORMAL、paymentNo=null、无支付记录 |
| owner1 登录 `GET /api/owner/parking-records` | 200，只见本人 2 条（JOIN vehicle.owner_user_id） |
| owner1 `GET /api/owner/parking-records/2`（临时车） | **403/4602** |
| owner1 `GET /api/owner/parking-records/999` | **404/4601** |
| owner1 `GET /api/parking-records`（管理端点） | **403/1004** |

## 4. 用例矩阵（契约要求 → 实现位置）

| 场景 | 预期 | 实现/测试 |
|---|---|---|
| 正常离场 | COMPLETED + payment + 车位 FREE | `ParkingExitService.exit` / `normalExitThenDuplicateRejected` |
| 拒绝：重复离场 | 409/4502，不二次成功 | 条件更新行数校验 / 同名用例 |
| 拒绝：实收≠应收 | 4503，记录仍 PARKING、车位仍 OCCUPIED、无支付 | `validatePayment` / `amountMismatchKeepsRecordParking` |
| 拒绝：免费放行缺原因 | 4504 | `freeReleaseRequiresRemarkAndKeepsAmountDue` |
| 拒绝：离场早于入场 | IllegalArgumentException（纯函数层） | `FeeCalculatorTest$InvalidInput` |
| 边界：免费时长临界 | 30 分钟 0 元、31 分钟 1 个单位 | `FeeCalculatorTest$FreeMinutesBoundary` |
| 边界：24h 周期封顶 | 24h 封顶 50；26h=50+5；48h=100 | `FeeCalculatorTest$DailyCap` |
| 边界：秒向上取整 | 30分01秒按 31 分钟计 | `FeeCalculatorTest$SecondsRounding` |
| 规则切换 | 新记录按新规则，旧记录按绑定旧规则 | `quoteUsesRecordBoundRuleAfterRuleSwitch` + 冒烟第 12 行 |
| 异常离场 | ABNORMAL、无支付、释放车位、原因必填 | `abnormalExitSetsAbnormalWithoutPayment` |
| 事务回滚 | 支付插入失败/释放失败时三处全回滚 | `rollbackWhenPaymentInsertFails`、`rollbackWhenSpaceReleaseFails` |
| 车主隔离 | 只看本人绑定车辆记录 | `ownerIsolationBetweenTwoOwners` |
| 越权详情 | 403/4602；不存在 404/4601 | `ownerDetailForbidden` |
| 停用绑定 | 历史仍可见（JOIN 不过滤 vehicle.status） | `disabledVehicleHistoryStillVisible` |
| 临时车 | 后来绑定不开放绑定前历史 | `temporaryHistoryNotOpenedByLaterBinding` |

## 5. 联调测试矩阵对照（03_联调测试矩阵.md）

T10（规则切换与历史）、T16（预览与确认跨分钟变化/旧规则绑定）、T17（正常离场一次成功+重复 409）、T18（金额校验/免费放行/回滚）、T19（异常离场）、T20（管理员过滤排序）、T21（记录详情与派生字段）、T22（车主隔离与越权）均已在自动化测试或冒烟中实测通过。

## 6. 未完成项与阻塞

1. **前端未执行 `npm install` / 构建**：本机 npm registry（registry.npmjs.org 与 registry.npmmirror.com）均不可达。已用 `node --check` 对全部 9 个 JS 与 6 个 Vue 的 script 块做语法级校验（全部通过）。组员在有网络环境执行：
   ```
   cd frontend && npm install && npm run dev     # Vite 5173，/api 代理到 8081
   ```
2. **Git 仓库**：本地未初始化 git（原仓库 `Niannya/smart-parking-system` 不可达）。交付物在工作区 `C:/Users/ymm19/WorkBuddy/数据库/`，组员可 `git init` 后按第 2 节清单提交。
3. **PostgreSQL 为便携实例**：`C:/pgtools/pg17`（二进制）与 `C:/pgdata`（数据）。重启数据库：`C:/pgtools/pg17/bin/postgres.exe -D C:/pgdata -p 5432`（后台运行）。运行库 `smart_parking` 已导入 schema+种子；测试库 `smart_parking_test` 由测试自行重建。
4. **成员 1/2 的正式实现**：`ParkingLotGuard`、`UserPrincipal`、`AuthController`、区域/车位/车辆/入场均为最小脚手架，语义以契约文档为准，欢迎替换；替换时保持 `FeeRuleQuery`、`ApiResult`、`BusinessException`、`CurrentUser`、`ParkingLotGuard` 签名不变即可，成员 3 代码无需改动。

## 7. 复验命令速查

```bash
# 1) 确保 PostgreSQL 在跑（127.0.0.1:5432，库 smart_parking_test 可自动建则更好）
# 2) 全量测试
cd backend
export JAVA_HOME="C:/Program Files/Java/jdk-21"
../tools/apache-maven-3.9.16/bin/mvn -s ../tools/apache-maven-3.9.16/conf/settings.xml test
# 3) 打包
../tools/apache-maven-3.9.16/bin/mvn -s ../tools/apache-maven-3.9.16/conf/settings.xml package -DskipTests
# 4) 运行库导入（幂等，可重复）
cd ../.. && java -cp <postgresql.jar>;<mybatis.jar> tools/../pgtools/SetupRuntimeDb.java database/01_schema.sql database/02_seed_data.sql
# 5) 启动
cd backend && java -jar target/smart-parking-backend-0.0.1-SNAPSHOT.jar --server.port=8081
```

演示账号：`admin/admin123456`、`owner1/owner123456`、`owner2/owner123456`。
