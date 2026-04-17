# 智能盲杖系统 - 企业级项目结构说明

## 当前项目结构问题

当前项目结构存在以下问题：
1. 模块划分不清晰（pro/bac, pro/fro, pro/sc 命名不规范）
2. 配置文件分散
3. 缺少统一的构建和部署配置
4. 缺少CI/CD配置
5. 文档结构不完善
6. 根目录文件散乱

## 企业级项目结构设计

```
intelligent-blind-cane-system/
├── backend/                    # 后端服务（Spring Boot）
│   ├── src/                    # 源代码
│   │   ├── main/               # 主代码
│   │   │   ├── java/com/ruoyi/
│   │   │   │   ├── config/     # 配置类
│   │   │   │   ├── controller/ # 控制器
│   │   │   │   ├── service/    # 服务层
│   │   │   │   ├── mapper/     # 数据访问层
│   │   │   │   ├── entity/     # 实体类
│   │   │   │   ├── dto/        # 数据传输对象
│   │   │   │   ├── vo/         # 视图对象
│   │   │   │   ├── interceptor/# 拦截器
│   │   │   │   ├── websocket/  # WebSocket
│   │   │   │   ├── exception/  # 异常处理
│   │   │   │   ├── annotation/ # 自定义注解
│   │   │   │   ├── enums/      # 枚举
│   │   │   │   ├── util/       # 工具类
│   │   │   │   └── Application.java
│   │   │   └── resources/      # 资源文件
│   │   │       ├── application.yml       # 主配置
│   │   │       ├── application-dev.yml   # 开发环境
│   │   │       ├── application-test.yml  # 测试环境
│   │   │       ├── application-prod.yml  # 生产环境
│   │   │       ├── mapper/                # MyBatis映射文件
│   │   │       └── static/               # 静态资源
│   │   └── test/                # 测试代码
│   ├── pom.xml                  # Maven配置
│   ├── Dockerfile               # Docker镜像构建
│   ├── .dockerignore            # Docker忽略文件
│   └── README.md                # 后端说明文档
│
├── frontend/                   # 前端管理端（Vue 3）
│   ├── src/
│   │   ├── api/                # API接口
│   │   ├── assets/             # 静态资源
│   │   ├── components/         # 公共组件
│   │   ├── composables/        # 组合式函数
│   │   ├── layouts/            # 布局组件
│   │   ├── router/             # 路由配置
│   │   ├── stores/             # 状态管理
│   │   ├── styles/             # 样式文件
│   │   ├── utils/              # 工具函数
│   │   ├── views/              # 页面组件
│   │   ├── App.vue
│   │   └── main.ts
│   ├── public/                 # 公共资源
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── Dockerfile
│   └── README.md
│
├── miniapp/                    # 小程序端（Uni-app）
│   ├── api/                    # API接口
│   ├── components/             # 公共组件
│   ├── pages/                  # 页面
│   ├── static/                 # 静态资源
│   ├── store/                  # 状态管理
│   ├── utils/                  # 工具函数
│   ├── App.vue
│   ├── main.js
│   ├── manifest.json
│   ├── pages.json
│   ├── package.json
│   └── README.md
│
├── database/                   # 数据库相关
│   ├── mysql/                  # MySQL脚本
│   │   ├── schema/             # 表结构
│   │   ├── data/               # 初始数据
│   │   └── migration/          # 迁移脚本
│   ├── redis/                  # Redis配置
│   └── README.md
│
├── scripts/                    # 脚本工具
│   ├── development/            # 开发脚本
│   │   ├── start-all.sh        # 启动所有服务
│   │   └── stop-all.sh         # 停止所有服务
│   └── simulation/             # 模拟器脚本
│       ├── LocationSimulator.java
│       └── simulate_location.py
│
├── docs/                       # 项目文档
│   ├── architecture/           # 架构文档
│   │   ├── system-design.md
│   │   ├── database-design.md
│   │   └── api-design.md
│   ├── development/            # 开发文档
│   │   ├── setup-guide.md      # 环境搭建指南
│   │   ├── coding-standard.md  # 编码规范
│   │   └── git-workflow.md     # Git工作流
│   └── miniapp/                # 小程序文档
│       ├── development.md
│       └── api-reference.md
│
├── .gitignore                  # Git忽略配置
├── .env.example               # 环境变量示例
├── .editorconfig               # 编辑器配置
├── .prettierrc                # Prettier配置
├── .eslintrc.js               # ESLint配置
├── LICENSE                     # 许可证
└── README.md                   # 项目主README
```

## 结构迁移计划

### 第一阶段：创建新目录结构
1. 创建标准目录结构
2. 创建配置文件模板
3. 创建CI/CD配置

### 第二阶段：迁移代码
1. 将 pro/bac 迁移到 backend/
2. 将 pro/fro 迁移到 frontend/
3. 将 mini 保持在 miniapp/（或保持mini/）
4. 将 pro/sc 迁移到 database/
5. 将模拟器脚本迁移到 scripts/simulation/

### 第三阶段：配置优化
1. 统一环境配置
2. 配置Docker支持
3. 配置CI/CD流程
4. 完善文档

### 第四阶段：清理和验证
1. 删除旧目录
2. 更新README
3. 验证项目可运行

## 环境配置管理

### 环境变量
使用 `.env` 文件管理环境变量：
- `.env.example` - 环境变量模板
- `.env.dev` - 开发环境
- `.env.test` - 测试环境
- `.env.prod` - 生产环境

### 配置中心
后端使用Spring Boot的多环境配置：
- application.yml - 基础配置
- application-{profile}.yml - 环境特定配置

## CI/CD流程

### GitHub Actions
- **后端CI**: 编译、测试、构建Docker镜像
- **前端CI**: 编译、测试、构建、部署
- **小程序CI**: 编译、上传预览版本

### 部署流程
1. 代码提交触发CI
2. 自动运行测试
3. 构建Docker镜像
4. 推送到镜像仓库
5. 自动部署到测试环境
6. 手动批准部署到生产环境

## 开发规范

### 分支策略
- `main` - 主分支，生产环境
- `develop` - 开发分支
- `feature/*` - 功能分支
- `hotfix/*` - 紧急修复分支
- `release/*` - 发布分支

### 提交规范
```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 重构
perf: 性能优化
test: 测试
chore: 构建/工具变动
```

### 代码规范
- 后端：遵循阿里巴巴Java开发手册
- 前端：遵循Vue官方风格指南
- 小程序：遵循Uni-app开发规范

## 后续优化建议

1. **引入代码质量工具**
   - SonarQube - 代码质量检测
   - ESLint - 前端代码检查
   - Checkstyle - Java代码检查

2. **引入监控和日志**
   - ELK Stack - 日志收集和分析
   - Prometheus + Grafana - 监控和告警
   - Sentry - 错误追踪

3. **引入自动化测试**
   - 单元测试
   - 集成测试
   - E2E测试

4. **引入API文档自动生成**
   - Swagger/OpenAPI
   - 接口自动化测试
