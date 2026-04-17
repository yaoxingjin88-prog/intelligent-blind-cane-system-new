# 智能盲杖管理系统 (Intelligent Blind Cane System)

## 项目简介

智能盲杖管理系统是一个面向视障人士的智能辅助设备管理平台，通过物联网技术实现对盲杖设备的实时监控、数据分析、安全预警等功能，为视障人士提供更安全、便捷的出行体验，同时为监护人和管理员提供全方位的管理和监控能力。

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

## 项目结构

```
intelligent-blind-cane-system/
├── backend/                    # 后端服务 (Spring Boot)
├── frontend/                   # 前端管理端 (Vue 3)
├── miniapp/                    # 小程序端 (Uni-app)
├── database/                   # 数据库脚本
├── scripts/                    # 脚本工具
│   ├── development/            # 开发脚本
│   ├── deployment/             # 部署脚本
│   └── simulation/             # 模拟器脚本
├── docker/                     # Docker配置
├── configs/                    # 配置文件
├── docs/                       # 项目文档
├── .github/                    # GitHub配置
├── .env.example               # 环境变量示例
└── README.md
```

> **注意**: 当前项目正在从旧结构迁移到新的企业级结构。详细的重组指南请参考 [项目重组指南.md](./项目重组指南.md) 和 [企业级项目结构说明.md](./企业级项目结构说明.md)。

## 快速开始

### 方式一：Docker（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/your-org/intelligent-blind-cane-system.git
cd intelligent-blind-cane-system

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 文件配置数据库密码等

# 3. 启动服务
docker-compose up -d

# 4. 访问服务
# 后端API: http://localhost:8081
# 前端管理: http://localhost:3000
# Nginx: http://localhost:80
```

### 方式二：手动部署

详细的手动部署步骤请参考 [开发环境搭建指南](./docs/development/setup-guide.md)。

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
- 环境数据（温度、湿度）
- 跌倒检测

### 3. 电子围栏
- 围栏创建与管理
- 围栏范围设置
- 越界报警
- 围栏效果评估

### 4. 报警管理
- 报警记录查询
- 报警状态管理（待处理/已处理）
- 报警类型分类
- 实时报警推送

### 5. 用户管理
- 视障用户信息管理
- 监护人管理
- 设备与用户关联
- 用户权限管理

### 6. 实时监控
- 设备实时位置追踪
- 传感器数据实时展示
- WebSocket实时推送
- 监控中心大屏

### 7. 数据分析
- 数据看板
- 风险区域热力图
- 活跃时长趋势分析
- 设备健康度评估
- 报警分布统计
- 轨迹回放

### 8. 轨迹回放
- 历史轨迹查询
- 轨迹可视化回放
- 时间范围筛选
- 播放速度控制

## 数据库设计

### 主要数据表

- **admin**: 管理员表
- **visually_impaired_user**: 视障用户表
- **guardian**: 监护人表
- **cane_device**: 盲杖设备表
- **sensor_data**: 传感器数据表
- **alarm_record**: 报警记录表
- **electronic_fence**: 电子围栏表

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8.0+
- npm 或 yarn

### 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE smart_cane CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

2. 导入数据库脚本：
```bash
mysql -u root -p smart_cane < pro/sc/smart_cane.sql
```

3. 配置数据库连接（修改 `pro/bac/src/main/resources/application.properties`）：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smart_cane?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_0900_ai_ci&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456
```

### 后端启动

1. 进入后端目录：
```bash
cd pro/bac
```

2. 编译项目：
```bash
mvn clean compile
```

3. 打包项目：
```bash
mvn package
```

4. 运行项目：
```bash
java -jar target/Excellent-1.0-SNAPSHOT.jar
```

后端服务将在 `http://localhost:8081` 启动

### 前端启动

1. 进入前端目录：
```bash
cd pro/fro
```

2. 安装依赖：
```bash
npm install
```

3. 启动开发服务器：
```bash
npm run dev
```

前端服务将在 `http://localhost:3000` 启动

### 模拟器使用

#### Java位置模拟器
```bash
javac LocationSimulator.java
java LocationSimulator
```

#### Python位置模拟器
```bash
pip install requests
python simulate_location.py
```

## API文档

启动后端服务后，访问以下地址查看API文档：

- Swagger UI: `http://localhost:8081/doc.html`

## 主要API端点

- `POST /api/admin/login` - 管理员登录
- `GET /api/sensor-data` - 获取传感器数据
- `POST /api/sensor-data` - 上传传感器数据
- `GET /api/devices` - 获取设备列表
- `POST /api/devices` - 添加设备
- `GET /api/alarms` - 获取报警记录
- `GET /api/analytics/dashboard` - 获取看板数据
- `GET /api/fences` - 获取电子围栏列表
- `POST /api/fences` - 创建电子围栏
- `GET /api/users` - 获取用户列表
- `POST /api/users` - 添加用户

## 默认账号

- 管理员账号: `admin`
- 管理员密码: `123456`

## 项目特色

1. **实时监控**: 基于WebSocket的实时数据推送，实现设备状态的实时监控
2. **智能预警**: 支持跌倒检测、越界报警等多种预警机制
3. **数据可视化**: 丰富的图表展示，包括热力图、趋势图、分布图等
4. **轨迹回放**: 支持历史轨迹的查询和可视化回放
5. **电子围栏**: 灵活的电子围栏设置，支持圆形、矩形等多种形状
6. **设备健康度**: 综合评估设备健康状况，及时发现设备故障
7. **多端支持**: 响应式设计，支持PC端和移动端访问

## 开发说明

### 后端开发

- 遵循RESTful API设计规范
- 使用MyBatis进行数据持久化
- 使用JWT进行身份认证
- 使用WebSocket实现实时通信
- 使用Knife4j生成API文档

### 前端开发

- 使用Vue 3 Composition API
- 使用TypeScript进行类型检查
- 使用Element Plus作为UI组件库
- 使用ECharts进行数据可视化
- 使用Vue Router进行路由管理
- 使用Axios进行HTTP请求

## 注意事项

1. **Java版本**: 项目需要Java 17环境，请确保JAVA_HOME配置正确
2. **Maven配置**: 如遇Maven使用Java 8的问题，请修改 `D:\Maven\apache-maven-3.9.12\bin\mvn.cmd` 文件，强制使用Java 17
3. **数据库编码**: 确保数据库使用utf8mb4编码
4. **跨域配置**: 前端开发时需配置代理或后端允许跨域
5. **地图API**: 前端使用高德地图，需申请高德地图API密钥

## 许可证

本项目仅供学习和研究使用。

## 联系方式

如有问题或建议，请联系项目维护者。
