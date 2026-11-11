# 数据库初始化说明

本目录用于初始化智能停车场管理系统的 PostgreSQL 数据库。

## 文件

| 文件 | 作用 |
| --- | --- |
| `01_schema.sql` | 创建 8 张核心表、约束和索引 |
| `02_seed_data.sql` | 写入两个停车场、40 个车位及虚构演示账号 |

## 环境要求

- PostgreSQL 17
- 数据库名：`smart_parking`
- 字符编码：UTF-8
- 建议时区：`Asia/Shanghai`

不要把数据库密码写入仓库。每位组员使用自己的本地账号和环境变量。

## Windows PowerShell 执行示例

下面命令使用本机现有的 `D:\pgsql`，端口按当前开发环境填写为 `15432`。其他组员如果使用默认端口，应改为 `5432`。

```powershell
$env:PGPASSWORD = $env:DB_PASSWORD

& 'D:\pgsql\bin\createdb.exe' `
  -h localhost -p 15432 -U Niannya `
  -E UTF8 smart_parking

& 'D:\pgsql\bin\psql.exe' `
  -h localhost -p 15432 -U Niannya -d smart_parking `
  -v ON_ERROR_STOP=1 -f 'database/01_schema.sql'

& 'D:\pgsql\bin\psql.exe' `
  -h localhost -p 15432 -U Niannya -d smart_parking `
  -v ON_ERROR_STOP=1 -f 'database/02_seed_data.sql'

Remove-Item Env:PGPASSWORD
```

如果 `smart_parking` 已存在，跳过 `createdb` 命令。`01_schema.sql` 面向空数据库，防止误删现有业务数据，因此不会自动删除旧表；`02_seed_data.sql` 可以重复执行。

## 验证命令

```powershell
& 'D:\pgsql\bin\psql.exe' `
  -h localhost -p 15432 -U Niannya -d smart_parking `
  -c "SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';" `
  -c "SELECT lot_code, lot_name, status FROM parking_lot ORDER BY lot_code;" `
  -c "SELECT l.lot_code, COUNT(s.id) AS space_count FROM parking_lot l JOIN parking_area a ON a.parking_lot_id = l.id JOIN parking_space s ON s.area_id = a.id GROUP BY l.lot_code ORDER BY l.lot_code;"
```

预期结果：

- 核心表数量为 `8`；
- 存在 `LOT-001`、`LOT-002` 两个停车场；
- 每个停车场有 `20` 个停车位；
- 每个停车场恰好有一条当前收费规则。

## 演示账号

| 角色 | 用户名 | 演示密码 |
| --- | --- | --- |
| 管理员 | `admin` | `Demo123456` |
| 车主 | `owner_demo` | `Demo123456` |

脚本通过 `pgcrypto` 的 `crypt(..., gen_salt('bf'))` 将演示密码保存为 BCrypt 摘要，不在数据库中保存明文密码。上述账号仅用于本地课程演示。

## 需要后端事务保证的规则

以下规则跨越多张表，不能只依靠单表 `CHECK` 约束：

1. `entry_admin_id`、`exit_admin_id`、`created_by`、`handled_by` 对应账号必须是 `ADMIN`。
2. `vehicle.owner_user_id` 对应账号必须是 `OWNER`。
3. 停车记录的停车场、车位和收费规则必须属于同一个停车场。
4. 入场时创建停车记录并把车位改为 `OCCUPIED`，两步必须在同一事务中完成。
5. 离场时更新停车记录、创建收费记录并把车位改为 `FREE`，三步必须在同一事务中完成。
