# 智能盲杖管理系统 (Intelligent Blind Cane System)

## 项目简介

智能盲杖管理系统是一个面向视障人士的智能辅助设备管理平台，通过物联网技术实现对盲杖设备的实时监控、数据分析、安全预警等功能，为视障人士提供更安全、便捷的出行体验，同时为监护人和管理员提供全方位的管理和监控能力。

**核心创新点**：
- 基于计算机视觉的红绿灯识别算法（HSV颜色空间 + 圆形轮廓筛选）
- WebSocket 实时报警推送（延迟 < 100ms）
- 三端覆盖：管理端(Vue3) + 小程序端(Uni-app) + 视觉模块(Python)

## 项目结构

```
intelligent-blind-cane-system/
├── backend/                    # 后端服务 (Spring Boot)
│   ├── src/main/java/com/ruoyi/
│   │   ├── controller/         # REST API 控制器
│   │   ├── service/            # 业务逻辑层
│   │   ├── mapper/             # MyBatis 数据访问层
│   │   ├── entity/             # 实体类
│   │   ├── websocket/          # WebSocket 实时通信
│   │   ├── config/             # 配置类
│   │   └── utils/              # 工具类
│   └── src/main/resources/
│       └── application.yml     # 应用配置
├── frontend/                   # 前端管理端 (Vue 3)
│   ├── src/
│   │   ├── views/              # 页面组件
│   │   ├── components/         # 公共组件
│   │   ├── api/                # API 接口封装
│   │   ├── router/             # 路由配置
│   │   └── stores/             # Pinia 状态管理
│   └── package.json
├── miniapp/                    # 小程序端 (Uni-app)
│   ├── src/
│   │   ├── pages/              # 页面
│   │   ├── api/                # API 接口
│   │   ├── stores/             # 状态管理
│   │   └── utils/              # 工具函数
│   └── manifest.json
├── database/                   # 数据库脚本
│   └── mysql/
│       └── smart_cane.sql      # 完整数据库脚本
├── scripts/                    # 脚本工具
│   ├── vision/                 # 计算机视觉模块
│   │   ├── crossing_assist_demo.py  # 红绿灯识别演示
│   │   └── requirements-vision.txt  # Python依赖
│   ├── simulation/             # 模拟器脚本
│   └── development/            # 开发脚本
├── docs/                       # 项目文档
│   ├── architecture/           # 架构文档
│   ├── development/            # 开发文档
│   └── miniapp/                # 小程序文档
├── .env.example               # 环境变量示例
└── README.md                  # 项目说明
```

## 快速开始

### 1. 环境准备

**必需软件：**
- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8.0+
- Python 3.10+ (可选，用于视觉模块)

### 2. 数据库配置

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE smart_cane CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入数据库脚本
mysql -u root -p smart_cane < database/mysql/smart_cane.sql
```

### 3. 后端启动

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端服务将在 http://localhost:8081 启动

**API 文档地址**：http://localhost:8081/doc.html (Knife4j)

### 4. 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端服务将在 http://localhost:3000 启动

### 5. 小程序开发

```bash
cd miniapp
npm install
# 使用 HBuilderX 或微信开发者工具打开项目
```

### 6. 视觉模块启动（可选）

```bash
cd scripts/vision
pip install -r requirements-vision.txt
python crossing_assist_demo.py --api-base http://127.0.0.1:8081/api --device-id ESP32_001
```

详细的环境搭建指南请参考 [开发环境搭建指南](./docs/development/setup-guide.md)。

## 技术栈

### 后端
| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.3 | 主框架 |
| MySQL | 8.0 | 数据库 |
| MyBatis | 3.0.3 | ORM框架 |
| JWT | 0.12.3 | 认证授权 |
| Knife4j | 4.5.0 | API文档 |
| WebSocket | - | 实时通信 |
| Maven | 3.9.12 | 构建工具 |
| JDK | 17 | Java版本 |

### 前端
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.3.0 | 框架 |
| Vite | 5.0.0 | 构建工具 |
| Element Plus | 2.13.7 | UI组件库 |
| ECharts | 6.0.0 | 图表库 |
| TypeScript | 5.0.0 | 类型支持 |
| Axios | 1.15.0 | HTTP客户端 |
| Vue Router | 5.0.4 | 路由管理 |
| Pinia | 2.1.0 | 状态管理 |

### 小程序
| 技术 | 用途 |
|------|------|
| Uni-app | 跨端框架 |
| uView Plus | UI组件库 |
| Pinia | 状态管理 |
| 微信地图/高德地图 | 地图服务 |

### 视觉模块
| 技术 | 用途 |
|------|------|
| Python 3.10+ | 编程语言 |
| OpenCV | 计算机视觉 |
| NumPy | 数值计算 |
| Requests | HTTP请求 |

## 核心功能

### 1. 设备管理
- 设备注册与绑定
- 设备状态监控（在线/离线、电池电量、健康度评分）
- 设备信息管理
- 设备测试模拟

### 2. 传感器数据采集
- 障碍物距离检测
- 加速度传感器数据（三轴加速度）
- GPS定位数据（经纬度）
- 温湿度传感器数据
- 跌倒检测（基于加速度阈值算法）

### 3. 报警管理
- 跌倒检测报警
- 越界报警（电子围栏）
- 低电量报警
- 长时间静止报警
- 报警处理与统计
- **WebSocket 实时推送**（延迟 < 100ms）

### 4. 电子围栏
- 圆形围栏设置
- 矩形/多边形围栏设置
- 围栏启用/禁用
- 越界实时报警
- 围栏效果评估

### 5. 轨迹回放
- 历史轨迹查询
- 轨迹回放播放（地图动画）
- 移动距离统计
- 活动时长统计

### 6. 过马路辅助（核心创新功能）
- **红绿灯识别**：基于HSV颜色空间 + 圆形轮廓筛选
- **斑马线检测**：白色条纹提取 + 方向估算
- **实时播报**：语音提示通行建议
- **视觉算法**：7帧滑动窗口稳定策略，防闪烁

### 7. AI智能助手
- AI对话功能（集成百度千帆大模型）
- 语音交互（百度语音识别/合成）
- 智能问答

### 8. 数据统计
- 设备健康度分析
- 活动趋势统计
- 报警统计分析
- 风险热力图

## 数据库设计

### 核心表结构

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `visually_impaired_user` | 视障用户 | id, name, phone, emergency_contact |
| `guardian` | 监护人 | id, user_id, phone, relationship, is_primary |
| `cane_device` | 盲杖设备 | device_id, user_id, battery_level, status |
| `sensor_data` | 传感器数据 | device_id, obstacle_distance, accel_x/y/z, latitude, longitude |
| `alarm_record` | 报警记录 | device_id, alarm_type, status, latitude, longitude |
| `electronic_fence` | 电子围栏 | fence_name, user_id, fence_type, coordinates, radius |
| `feedback` | 用户反馈 | user_id, content, status |

### 优化策略
- **分区**：sensor_data 表按月分区，保留12个月数据
- **索引**：device_id + data_time 联合索引，查询效率高
- **归档**：历史数据自动归档

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                        客户端层                          │
├──────────────────┬──────────────────┬───────────────────┤
│   管理端(Vue3)   │   小程序端       │   视觉模块        │
│                  │   (Uni-app)      │   (Python)        │
└────────┬─────────┴────────┬─────────┴───────────────────┘
         │                  │
         │ HTTP/REST API   │ HTTP/REST API
         │                  │
┌────────┴──────────────────┴────────────────────────────┐
│                    网关层(Nginx)                        │
│         负载均衡、静态资源、反向代理、SSL                │
└────────┬───────────────────────────────────────────────┘
         │
┌────────┴───────────────────────────────────────────────┐
│                    应用层(Spring Boot)                   │
├──────────────┬──────────────┬──────────────┬───────────┤
│  用户认证模块 │  设备管理模块 │  报警管理模块 │ 轨迹模块  │
├──────────────┼──────────────┼──────────────┼───────────┤
│  围栏管理模块 │  统计分析模块 │  WebSocket    │ 定时任务   │
└──────────────┴──────────────┴──────────────┴───────────┘
         │
┌────────┴───────────────────────────────────────────────┐
│                    数据访问层(MyBatis)                    │
└────────┬───────────────────────────────────────────────┘
         │
┌────────┴───────────────────────────────────────────────┐
│                    数据存储层                             │
├──────────────┬──────────────┬──────────────┬───────────┤
│   MySQL      │    Redis      │   文件存储    │  日志存储  │
│   (持久化)   │   (缓存)      │   (OSS/本地)  │  (ELK)    │
└──────────────┴──────────────┴──────────────┴───────────┘
```

## 核心算法

### 红绿灯识别算法
```python
# 1. ROI提取 - 画面上半部分
# 2. HSV颜色检测 - 红/黄/绿三种颜色掩码
# 3. 形态学处理 - 开运算去噪，闭运算连接
# 4. 圆形筛选 - circularity = 4π×面积/周长² ≥ 0.42
# 5. 综合评分 - 面积×34% + 圆度×28% + 饱和度×18% + 亮度×10% + 位置×10%
# 6. 稳定策略 - 7帧滑动窗口，多数投票
```

### 斑马线检测算法
```python
# 1. ROI提取 - 画面下半部分
# 2. 白色区域提取 - HSV + 自适应阈值
# 3. 形态学处理 - 闭运算连接条纹
# 4. 条形筛选 - 长宽比 ≥ 2.5
# 5. 方向估算 - 加权水平中心位置
#    → <44% = LEFT, >56% = RIGHT, 中间 = CENTER
```

## API 接口

### 主要接口列表

| 接口 | 说明 |
|------|------|
| `POST /api/auth/login` | 用户登录 |
| `GET /api/devices` | 获取设备列表 |
| `POST /api/sensor-data` | 上传传感器数据 |
| `GET /api/alarms` | 获取报警记录 |
| `POST /api/fences` | 创建电子围栏 |
| `GET /api/trajectory/{deviceId}` | 获取轨迹数据 |
| `POST /api/mini/devices/{id}/crossing-assist` | 过马路辅助数据上报 |
| `POST /api/ai/chat` | AI对话 |

完整 API 文档请参考：http://localhost:8081/doc.html

## 性能指标

| 指标 | 数值 |
|------|------|
| 后端接口响应时间 | < 50ms |
| WebSocket推送延迟 | < 100ms |
| 首屏加载时间 | 1.2s |
| 代码复用率 | 60% |
| 数据库查询时间 | < 20ms |

## 开发指南

### 后端开发
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 前端开发
```bash
cd frontend
npm install
npm run dev
```

### 小程序开发
```bash
cd miniapp
npm install
# 使用 HBuilderX 或微信开发者工具打开
```

### 代码规范
- 后端：遵循阿里巴巴Java开发手册
- 前端：ESLint + Prettier
- 提交规范：feat/fix/docs/style/refactor/test/chore

## 文档

- [系统架构设计](./docs/architecture/system-design.md)
- [数据库设计](./docs/architecture/database-design.md)
- [开发环境搭建指南](./docs/development/setup-guide.md)
- [编码规范](./docs/development/coding-standard.md)
- [小程序开发文档](./docs/miniapp/development.md)

## 配置说明

### 环境变量
复制 `.env.example` 为 `.env` 并配置相关参数：
```bash
cp .env.example .env
```

主要配置项：
- 数据库连接信息
- Redis配置
- JWT密钥
- 小程序AppID
- 高德地图API密钥
- 百度语音API密钥
- AI大模型API密钥

## 演示账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | admin | 123456 |

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 LICENSE 文件

## 联系方式

- 项目地址: https://github.com/your-org/intelligent-blind-cane-system
- 问题反馈: [Issues](https://github.com/your-org/intelligent-blind-cane-system/issues)

---

**更新日志**:
- 2024-XX-XX: 初始版本，完成基础功能
- 2024-XX-XX: 新增过马路辅助功能（红绿灯识别）
- 2024-XX-XX: 新增AI智能对话功能
- 2024-XX-XX: 优化WebSocket实时推送性能
