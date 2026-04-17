# 智能盲杖管理系统 (Intelligent Blind Cane System)

## 项目简介

智能盲杖管理系统是一个面向视障人士的智能辅助设备管理平台，通过物联网技术实现对盲杖设备的实时监控、数据分析、安全预警等功能，为视障人士提供更安全、便捷的出行体验，同时为监护人和管理员提供全方位的管理和监控能力。

## 项目结构

```
intelligent-blind-cane-system/
├── backend/                    # 后端服务 (Spring Boot)
├── frontend/                   # 前端管理端 (Vue 3)
├── miniapp/                    # 小程序端 (Uni-app)
├── database/                   # 数据库脚本
│   └── mysql/                  # MySQL脚本
├── scripts/                    # 脚本工具
│   ├── development/            # 开发脚本
│   └── simulation/             # 模拟器脚本
├── docs/                       # 项目文档
│   ├── architecture/           # 架构文档
│   ├── development/            # 开发文档
│   └── miniapp/               # 小程序文档
├── .env.example               # 环境变量示例
├── .gitignore                 # Git忽略配置
└── README.md                  # 项目说明
```

## 快速开始

### 1. 环境准备

**必需软件：**
- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8.0+

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

详细的环境搭建指南请参考 [开发环境搭建指南](./docs/development/setup-guide.md)。

## 技术栈

### 后端
- **框架**: Spring Boot 3.3.3
- **数据库**: MySQL 8.0
- **ORM**: MyBatis 3.0.3
- **认证**: JWT (jjwt 0.12.3)
- **API文档**: Knife4j 4.5.0 (OpenAPI 3)
- **WebSocket**: Spring WebSocket
- **构建工具**: Maven 3.9.12
- **JDK版本**: Java 17

### 前端
- **框架**: Vue 3 + TypeScript
- **构建工具**: Vite 8.0.8
- **UI组件库**: Element Plus 2.13.7
- **图表库**: ECharts 6.0.0
- **路由**: Vue Router 5.0.4
- **HTTP客户端**: Axios 1.15.0
- **地图**: 高德地图 (AMap)

### 小程序
- **框架**: Uni-app
- **UI**: uView Plus
- **状态**: Pinia
- **地图**: 微信地图/高德地图

## 核心功能

### 1. 设备管理
- 设备注册与绑定
- 设备状态监控（在线/离线、电池电量）
- 设备信息管理
- 设备测试模拟

### 2. 传感器数据采集
- 障碍物距离检测
- 加速度传感器数据（三轴加速度）
- GPS定位数据（经纬度）
- 温湿度传感器数据

### 3. 报警管理
- 跌倒检测报警
- 越界报警（电子围栏）
- 低电量报警
- 长时间静止报警
- 报警处理与统计

### 4. 电子围栏
- 圆形围栏设置
- 矩形围栏设置
- 围栏启用/禁用
- 越界实时报警

### 5. 轨迹回放
- 历史轨迹查询
- 轨迹回放播放
- 移动距离统计
- 活动时长统计

### 6. 数据统计
- 设备健康度分析
- 活动趋势统计
- 报警统计分析
- 风险热力图

## 开发指南

### 环境要求
- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8.0+
- Redis 7+ (可选)

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

## 文档

- [系统架构设计](./docs/architecture/system-design.md)
- [数据库设计](./docs/architecture/database-design.md)
- [开发环境搭建指南](./docs/development/setup-guide.md)
- [编码规范](./docs/development/coding-standard.md)
- [小程序开发文档](./docs/miniapp/development.md)
- [企业级项目结构说明](./docs/development/enterprise-structure.md)
- [项目重组指南](./docs/development/reorganization-guide.md)

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

**注意**: 项目已从旧结构（pro/bac, pro/fro等）迁移到新的企业级结构。旧目录 `pro/` 可在服务停止后手动删除。根目录的 `小程序开发文档.md` 和 `小程序端.html` 可手动移动到 `docs/miniapp/`。
