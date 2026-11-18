# 智能停车场管理系统（课设）— 成员 3 交付 + 最小全链路脚手架

四份契约文档见 `docs/implementation/`；成员 3 的交付细节见 `docs/implementation/PR_成员3_交付说明.md`。

## 目录

```
database/            建表与种子数据（PostgreSQL）
backend/             Spring Boot 3.2.5 + Java 21 + MyBatis 后端
frontend/            Vue 3 + Vite 前端
tools/               Maven 3.9.16（含阿里云镜像 settings.xml）等本地工具
docs/implementation/ 契约文档 + 成员 3 交付说明
```

## 快速开始

```bash
# 0) 数据库（本机已用 zonky 便携 PostgreSQL 17.2 建好；重启用：
#    C:/pgtools/pg17/bin/postgres.exe -D C:/pgdata -p 5432   （后台运行）

# 1) 建运行库并导入结构+种子（幂等）
cd backend
export JAVA_HOME="C:/Program Files/Java/jdk-21"
java -cp "C:/Users/ymm19/.m2/repository/org/postgresql/postgresql/42.6.2/postgresql-42.6.2.jar;C:/Users/ymm19/.m2/repository/org/mybatis/mybatis/3.5.14/mybatis-3.5.14.jar" \
  "C:/pgtools/SetupRuntimeDb.java" ../database/01_schema.sql ../database/02_seed_data.sql

# 2) 测试（45 个用例；无数据库时集成测试自动跳过，纯函数 18 个用例仍可跑）
../tools/apache-maven-3.9.16/bin/mvn -s ../tools/apache-maven-3.9.16/conf/settings.xml test

# 3) 启动后端（注意显式指定端口：本机 SERVER__PORT 环境变量会覆盖配置文件）
java -jar target/smart-parking-backend-0.0.1-SNAPSHOT.jar --server.port=8081

# 4) 前端（需 npm 可用；registry 不可达时见交付说明第 6 节）
cd ../frontend && npm install && npm run dev    # Vite 5173，/api 代理到 8081
```

演示账号：`admin/admin123456`（管理员）、`owner1/owner123456`、`owner2/owner123456`（车主）。

## 成员 3 负责的接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/fee-rules?lotId=` | 规则历史（createdAt DESC, id DESC） |
| GET | `/api/fee-rules/active?lotId=` | 生效规则；无规则 404/4403 |
| POST | `/api/fee-rules` | 创建即激活，旧规则转历史 |
| GET | `/api/parking-exits/active?licensePlate=\|?recordNo=` | 在场查询（恰填一个） |
| GET | `/api/parking-exits/{id}/quote` | 费用预览（按记录绑定规则） |
| POST | `/api/parking-exits/{id}` | 离场+模拟收款（CASH/ELECTRONIC/FREE_RELEASE） |
| POST | `/api/parking-exits/{id}/abnormal` | 异常离场（不生成支付） |
| GET | `/api/parking-records` | 管理员记录查询（多过滤+排序） |
| GET | `/api/parking-records/{id}` | 管理员详情 |
| GET | `/api/owner/parking-records` | 车主本人记录（JOIN vehicle.owner_user_id） |
| GET | `/api/owner/parking-records/{id}` | 车主详情（非本人 403/4602） |
