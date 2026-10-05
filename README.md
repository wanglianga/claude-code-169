# 城市智能垃圾房满溢异味投放违规联动服务

基于 **Java 17 + Spring Boot 3.3 + PostgreSQL + Kafka** 的全栈服务。垃圾房设备上报
**厨余桶满溢、其他垃圾容量、异味传感器、门禁开合、摄像头识别、称重、消杀设备、清运车到场、心跳**
等数据；居民刷卡/扫码投放时记录小区、楼栋、垃圾类型、重量、积分和违规提示；物业、环卫、街道、居民
四端围绕**同一条垃圾房状态**协同，把设备告警真正转化为现场治理动作。

## 一、要解决的核心问题

满溢、异味、错投经常同时出现，单独处理某个告警会导致居民体验和分类质量持续恶化。本服务用
"设备/投放/投诉/清运事件 → 统一事件总线 → 联动规则 → 治理任务 + 积分 + 整改 + 留痕"
的方式联动九类场景：

| # | 场景 | 联动动作 |
|---|------|----------|
| 1 | 厨余桶满但清运车未到 | 容量越限即生成清运任务；满溢等待超时自动升级街道/环卫加急；车辆到场解除 |
| 2 | 居民错投被识别 | 暂扣积分 + 违规扣分 + 生成整改单；居民端展示违规提示 |
| 3 | 夜间投放高峰 | 窗口内投放次数达标 → 物业夜间巡查 + 环卫清运待命；高峰过后自动解除 |
| 4 | 设备断网 | 心跳超时 → 垃圾房置离线并派设备维修任务；心跳恢复不重复派单 |
| 5 | 异味投诉 | 投诉即联动物业消杀，投诉与消杀任务绑定，消杀完成投诉自动闭环 |
| 6 | 清运后未清洁 | 清运完成自动生成"清运后清洁"任务，超时未清洁升级物业督办 |
| 7 | 积分未到账 | 断网/积分链路延迟导致未入账，超时巡检自动补发，积分流水可追溯 |
| 8 | 摄像头误判 | 居民申诉 → 物业核查，误判成立返还扣分并补发暂扣积分，不成立则没收 |
| 9 | 满溢+异味并发 | 清运与消杀**同批派单**，避免只治一个告警 |

所有触发、升级、完成动作都写入 `linkage_log`，积分、投诉、整改结果均与实际投放单/清运单号一一对应。

## 二、四端功能

- **居民端** (`/resident.html`)：刷卡/扫码投放（可选模拟 AI 识别结论与断网延迟）、积分余额与流水、
  违规提示、摄像头误判申诉、异味/卫生投诉与处理结果。
- **物业端** (`/property.html`)：保洁/消杀/维修/夜间巡查/清运后清洁待处理任务、派单与完成、
  投诉工单闭环、整改/申诉核查（结论驱动积分返还或没收）、垃圾房实时状态与联动留痕、设备事件模拟器。
- **环卫端** (`/sanitation.html`)：清运**优先级队列**（容量 + 满溢等待 + 异味 + 投诉 + 升级综合评分）、
  派车与路线、车辆到场、清运完成（容量清零并自动生成清运后清洁任务）。
- **街道端** (`/street.html`)：按小区查看分类质量（正确率、错投、重量、积分）、投诉总量/类型趋势图、
  辖区垃圾房状态总览、全街道联动留痕。管理员可手动触发联动巡检。

## 三、本地启动

### 方式 A：一键 Docker Compose（PostgreSQL + Kafka KRaft + 应用）

```bash
docker compose up --build
# 应用：http://localhost:8080  （首次启动会自动建表并写入种子数据）
```

服务与组件：

| 服务 | 地址 | 说明 |
|------|------|------|
| 应用（含四端页面） | http://localhost:8080 | kafka profile：PostgreSQL + Kafka |
| Swagger UI | http://localhost:8080/swagger-ui.html | OpenAPI 3.x 接口文档 |
| PostgreSQL | localhost:5432 | 库/用户/密码 binroom/binroom |
| Kafka | localhost:9092 | KRaft 单节点，三个主题自动创建 |

Kafka 主题：`binroom.device-events`、`binroom.points-events`、`binroom.linkage-events`。

### 方式 B：本地开发（无需 Docker，H2 内存库 + 进程内事件）

要求 JDK 17+、Maven 3.8+。

```bash
mvn spring-boot:run                      # 默认 local profile
# 或
mvn clean package -DskipTests
java -jar target/binroom-linkage.jar    # 默认即 local profile
```

打开 http://localhost:8080 即可。local 模式下事件在事务提交后由进程内分发器直接处理，
联动逻辑与 Kafka 模式完全相同（同一套 `LinkageProcessor`），只是投递介质不同。

连接外部 PostgreSQL/Kafka（kafka profile）：

```bash
mvn clean package -DskipTests
java -jar target/binroom-linkage.jar --spring.profiles.active=kafka \
  --DB_HOST=localhost --DB_PORT=5432 --DB_NAME=binroom \
  --DB_USER=binroom --DB_PASSWORD=binroom \
  --KAFKA_BOOTSTRAP=localhost:9092
```

## 四、测试账号

| 角色 | 入口 | 账号 |
|------|------|------|
| 居民 | 居民端 | 卡号 `R1001`、`R1002`、`R1003`、`R2001`（刷卡/扫码直接输入卡号） |
| 物业 | 物业端 | `property` / `property123`（阳光花园小区） |
| 环卫 | 环卫端 | `sanitation` / `sanitation123` |
| 街道 | 街道端 | `street` / `street123` |
| 管理员 | 街道端（可触发巡检） | `admin` / `admin123` |

种子数据：滨河街道下辖阳光花园小区（BR-001、BR-002）与翠湖家园小区（BR-003）三个垃圾房。

## 五、验证方式

### 1. 自动化测试

```bash
mvn test
```

- `LinkageScenariosTest`：九类联动场景（local 模式 + H2），10 个用例中的 9 个场景断言。
- `KafkaLinkageFlowTest`：EmbeddedKafka 端到端，设备事件经 Kafka 主题消费后生成消杀/清运任务。

### 2. 页面手工验证（推荐路径）

1. 物业端登录，用"设备事件模拟器"对 BR-001 依次点击 **厨余桶满溢 95%**、**异味 78（越限）**，
   可看到物业端出现消杀任务、环卫端出现清运任务（同一垃圾房状态四端同步）。
2. 居民端登录（R1001），做一次投放，摄像头结论选"识别为错投" → 看到违规提示、积分暂扣与扣分；
   点击"申诉误判" → 物业端核查通过（误判撤销）→ 积分返还、违规解除。
3. 居民端勾选"模拟设备断网/积分链路延迟"投放 → 积分暂不到账；等待 60 秒后管理员在街道端
   点击"立即执行联动巡检"（或调用 `POST /api/admin/sweep`）→ 积分自动补发。
4. 居民端提交"异味投诉" → 物业端投诉与消杀任务绑定；消杀"完成"后投诉自动闭环。
5. 环卫端按优先级队列派车 → 车辆到场（满溢告警解除）→ 完成清运（容量清零）→ 物业端自动出现
   "清运后清洁"任务。
6. 街道端查看按小区的分类质量与投诉趋势图表。

### 3. 接口快速验证（curl）

```bash
# 设备上报（免鉴权，模拟设备网关）
curl -s -XPOST http://localhost:8080/api/devices/events -H 'Content-Type: application/json' \
  -d '{"roomCode":"BR-001","eventType":"KITCHEN_FULL","numericValue":95}'
curl -s -XPOST http://localhost:8080/api/devices/events -H 'Content-Type: application/json' \
  -d '{"roomCode":"BR-001","eventType":"ODOR","numericValue":78}'

# 各端登录取 token
curl -s -XPOST http://localhost:8080/api/auth/staff/login -H 'Content-Type: application/json' \
  -d '{"username":"sanitation","password":"sanitation123"}'
# 后续请求带头：Authorization: Bearer <token>
```

完整接口见 Swagger UI（`/swagger-ui.html`），主要分组：鉴权、设备接入、居民端、物业端、环卫端、
街道端、管理运维。

## 六、实际验证记录（2026-10-05 执行）

- `mvn clean test`：**10/10 通过**。其中 9 个联动场景（local 模式 + H2）覆盖满溢升级、
  满溢+异味同批派单、错投暂扣/申诉返还、夜间高峰、断网派单（含恢复不重复派单）、
  积分超时补发、申诉不成立没收、清运优先级、街道统计；另 1 个为 EmbeddedKafka 端到端用例，
  验证设备事件经 `binroom.device-events` 主题消费后真实落库生成消杀/清运任务。
- **H2（local profile）HTTP 全链路**：登录四端 → 设备上报满溢+异味 → 物业消杀/环卫清运派单 →
  错投扣分(-20) → 申诉误判撤销后返还并补发(+40) → 断网投放积分超时巡检补发(+30) →
  异味投诉绑定消杀且完成后自动闭环（异味回落 10）→ 清运完成容量清零(95→10)并生成清运后清洁任务 →
  街道统计正确 → Swagger UI、OpenAPI JSON、五个前端页面均返回 200。
- **PostgreSQL 15（真实数据库）HTTP 全链路**：用独立 PG 15 实例（5433 端口）+ PostgreSQLDialect
  启动应用，Hibernate 自动建 14 张表，上述同一链路全部通过；积分未到账在 60 秒超时后经巡检自动补发，
  居民余额 20 → 50；`psql` 直查 `linkage_log`/任务/投放/积分/投诉/整改各表数据齐全互相对应。
- Docker Compose 配置（PostgreSQL 16 + Kafka 3.8 KRaft + 应用）已提供并补齐健康检查；
  当前沙箱无 Docker 守护进程，未在本机实际执行 `docker compose up`。

## 七、工程结构
```
src/main/java/com/city/binroom
├── domain            # 实体、枚举、Repository（垃圾房状态聚合 BinRoom 是四端唯一状态源）
├── event             # 事件总线：LinkageMessage、事务后桥、Kafka 收发、统一分发器
├── service           # 设备接入、投放、投诉、整改、保洁/清运生命周期、积分、统计、定时巡检
│   └── processor     # 联动处理器（设备/投放/投诉/整改），local 与 Kafka 共用
├── web               # 四端 REST 接口、Token 鉴权拦截器、视图映射
├── config            # 联动规则参数、MVC 配置、Kafka 主题
└── bootstrap         # 种子数据
src/main/resources
├── application.yml   # local（H2+进程内事件）与 kafka（PostgreSQL+Kafka）两套 profile
└── static            # 四端前端页面（原生 HTML/CSS/JS，无外部依赖）
```

关键设计：

- 业务方法只发布领域事件（`EventPublisher`），事务提交后 `TransactionalEventBridge` 按
  `app.eventbus.mode` 决定进程内直投或发 Kafka；两种入口都汇聚到 `EventDispatcher`，
  每个联动处理器在独立新事务中执行，保证联动结果可靠落库且幂等（同垃圾房同类未结任务复用）。
- 清运优先级评分 `RoomStateService.pickupPriorityScore`：桶容量 + 满溢等待时长 + 异味 +
  夜间高峰 + 近 12 小时投诉数 + 已升级加权。
- 定时巡检（30 秒）：断网检测、积分超时补发、满溢等待升级、清运后清洁超时升级、任务超时、夜间高峰解除。
