# 智能盲杖管理系统 (Intelligent Blind Cane System)

## 项目简介

面向视障人士的智能盲杖管理平台：设备监控、传感上报、电子围栏、报警推送、轨迹回放与 AI 语音助手；覆盖管理端、微信小程序与视觉辅助模块。

**技术亮点**：
- JWT 鉴权 + BCrypt 口令 + 硬件上报 `X-Device-Key` + 接口限流
- 可选 Redis（限流 / 看板缓存 / 设备最新态·告警去重·静止计时），未启用时自动回退本机
- WebSocket 实时推送报警 / 围栏 / 过街辅助
- 小程序接口按域拆分；设备「在线」按传感心跳判定（联调用假硬件模拟器）
- 家庭监护圈：持杖用户 + 主监护人 + 子监护人；主监护人绑设备/邀请成员，子监护人可监视
- 设备绑定：一人一台；专用 bind/unbind / 用户分配接口，普通更新设备不改归属
- 报警闭环：创建同步推管理端+家属；结案记录处理方（guardian/admin）并互相同步

## 项目结构

```
intelligent-blind-cane-system/
├── backend/                         # Spring Boot 3.3
│   ├── src/main/java/com/ruoyi/
│   │   ├── controller/              # 管理端 / 小程序(mini) / Auth / Health
│   │   ├── service/                 # 业务（含限流、看板缓存、设备测试模拟）
│   │   ├── mapper/                  # MyBatis 注解 SQL
│   │   ├── config/                  # 安全、Redis、WebSocket、定时裁剪等
│   │   ├── interceptor/             # JWT / 限流
│   │   ├── websocket/               # 实时推送
│   │   └── utils/                   # JWT、地理距离、口令比对
│   ├── src/main/resources/
│   │   ├── application.yml          # 公共配置
│   │   ├── application-dev.yml      # 开发
│   │   ├── application-prod.yml     # 生产
│   │   └── logback-spring.xml       # 滚动日志
│   ├── application-local.yml.example
│   ├── application-example.env
│   └── sql/                         # 索引等补充脚本
├── frontend/                        # Vue3 + Vite + Element Plus 管理端
├── miniapp/                         # Uni-app 小程序
├── database/mysql/                  # 建库脚本
├── scripts/                         # 视觉 / 模拟等脚本
├── .env.example
└── README.md
```

## 快速开始

### 1. 环境

| 软件 | 说明 |
|------|------|
| JDK 17+ / Maven 3.9+ | 后端 |
| Node.js 18+ | 前端 / 小程序 |
| MySQL 8.0+ | 主库 |
| Redis（可选） | 限流、看板缓存、设备运行时态；本机已装可开启 |

### 2. 数据库

```bash
mysql -u root -p -e "CREATE DATABASE smart_cane CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -u root -p smart_cane < database/mysql/smart_cane.sql
```

### 3. 后端本地配置

```bash
cd backend
cp application-local.yml.example application-local.yml
# 编辑 application-local.yml：数据库密码、第三方 Key、管理员密码等
```

**开启 Redis（可选，需本机 Redis 已启动）：**

```yaml
# application-local.yml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
app:
  redis:
    enabled: true
```

或环境变量：`REDIS_ENABLED=true`、`REDIS_HOST`、`REDIS_PORT`。

```bash
mvn clean test
mvn spring-boot:run
```

- 服务：http://localhost:8081  
- API 文档：http://localhost:8081/doc.html  

默认 profile 为 `dev`；生产：`--spring.profiles.active=prod`。

### 4. 管理端

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 http://localhost:3000 。

### 5. 小程序

```bash
cd miniapp
npm install
# HBuilderX 或微信开发者工具打开；确认 config 中 BASE_URL
```

### 6. 视觉模块（可选）

```bash
cd scripts/vision
pip install -r requirements-vision.txt
python crossing_assist_demo.py --api-base http://127.0.0.1:8081/api --device-id DEVICE001
```

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.3 | 主框架 |
| MyBatis | 3.0.3 | 持久化 |
| MySQL | 8.0 | 主库 |
| Redis | （可选） | 限流、看板/监控缓存、设备运行时态 |
| JWT (jjwt) | 0.12.3 | 登录令牌 |
| spring-security-crypto | — | BCrypt |
| Knife4j | 4.5.0 | OpenAPI 文档 |
| WebSocket | — | 实时推送 |
| Lombok | — | 样板代码 |
| Logback | — | 滚动文件日志 |

### 前端

| 技术 | 用途 |
|------|------|
| Vue 3 + Vite + TypeScript | 管理端 |
| Element Plus | UI |
| ECharts | 图表 |
| 原生 fetch（`http.js` / `request.ts`） | HTTP |

### 小程序

Uni-app + uView Plus + Pinia；地图（微信 / 高德）。

## 核心功能

- **设备**：档案、绑定、心跳在线（近 3 分钟有传感上报则在线）、电量与健康度  
- **传感上报**：障碍距离、加速度、跌倒判定、GPS、温湿度；硬件头 `X-Device-Key`  
- **报警**：跌倒 / 围栏越界 / 低电 / 静止等；WebSocket 推送。监护人主责处理，管理端兜底；任一侧结案推送 `ALARM_HANDLED` 通知对端。二期（订阅消息/短信/超时升级等）见 [`docs/alarm-roadmap-phase2.md`](docs/alarm-roadmap-phase2.md)  
- **电子围栏**：圆形围栏评估与状态推送  
- **轨迹**：按时间查询与回放  
- **过街辅助**：视觉模块 + 小程序演示 mock  
- **AI**：DeepSeek 对话、百度 ASR/TTS  
- **看板**：风险热力、活跃/电量/报警分布、设备排行与健康面板  

### 假硬件模拟器（可控位置）

打开（需先起管理端前端）：http://localhost:3000/hardware-simulator.html  

- **先在设备页建档**，再用同一 `deviceId` 启动上报（等同真机心跳，无需后台「确认接收」）  
- 可调经纬度、按米步进、一键移出围栏；越界报警由后台围栏判定  
- 上报可带 `batteryLevel`，会回写设备电量  
- 设备管理页已去掉「测试启动」，联调统一用模拟器；**勿用双击 file:// 打开**（会 CORS）；说明见 [`scripts/simulation/README.md`](scripts/simulation/README.md)

## 配置要点

| 项 | 说明 |
|----|------|
| `application-local.yml` | 本地密钥（已 gitignore），勿提交 |
| `backend/application-example.env` | 环境变量示例 |
| `app.security.*` | JWT、管理员、CORS、设备密钥、测试短信码开关 |
| `app.redis.enabled` | 是否启用 Redis（默认 `false`；启用后含设备运行时态） |
| `DEVICE_API_KEY` / `X-Device-Key` | 硬件上报鉴权（生产建议必填） |
| `demo.location` | 小程序演示定位（开发可强制） |

生产请使用 `prod` profile，并由 `ProdSecurityValidator` 校验弱口令 / JWT / 设备密钥。

## 主要 API（节选）

| 接口 | 说明 |
|------|------|
| `POST /api/login` | 管理端登录 |
| `GET /api/devices` | 设备列表（状态含心跳在线） |
| `PUT /api/devices` | 更新设备档案（不改归属） |
| `POST /api/devices/{id}/bind` | 绑定设备到用户（一人一台） |
| `POST /api/devices/{id}/unbind` | 解绑设备 |
| `PUT /api/users/{id}/device` | 用户侧分配/更换/清空绑定 |
| `POST /api/devices/{id}/test/start\|stop` | 服务端随机联调（保留 API；管理端 UI 已改用假硬件模拟器） |
| `POST /api/sensor-data` | 硬件传感上报（可选 `batteryLevel` 回写电量） |
| `GET /api/analytics/dashboard` | 数据看板（可 Redis 缓存） |
| `GET /api/monitor/overview` | 实时监控总览（可 Redis 缓存） |
| `/api/mini/**` | 小程序：登录、设备、围栏、报警、轨迹、关怀等 |
| `WS /ws/alarm?deviceId=` | 实时推送 |

完整接口见 Knife4j：http://localhost:8081/doc.html

## 系统架构（简图）

```
管理端 Vue3 ──┐
小程序 Uni-app ┼── HTTP / WS ──► Spring Boot
视觉 Python ──┘                    │
                    ┌──────────────┼──────────────┐
                    │  JWT/限流/设备密钥            │
                    │  业务 Service（传感/围栏/报警）│
                    │  WebSocket 推送               │
                    └──────────────┼──────────────┘
                                   │
              MySQL ◄──────────────┤
              Redis（可选）◄────────┘  限流 + 看板缓存 + 设备运行时态
```

## 演示账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | admin | 开发默认 `123456`（生产务必修改或改为 BCrypt） |

## 仓库

- Gitee：`git@gitee.com:yaoxingjin/intelligent-blind-cane-system.git`  
- GitHub：`git@github.com:yaoxingjin88-prog/intelligent-blind-cane-system-new.git`  

## 更新日志（摘录）

- 可选 Redis：限流、看板/监控缓存、设备最新态/告警去重/静止计时；本地默认关闭，可 `app.redis.enabled=true` 开启  

- 安全加固：JWT、BCrypt 兼容、设备密钥、限流、生产配置校验、logback 滚动  
- 小程序 Controller 拆分；设备在线按心跳；设备测试模拟竞态修复  
- 配置迁至 `application.yml` + `dev`/`prod`；前端去掉未用 axios  

---

许可证：MIT（若仓库内含 LICENSE 文件）
