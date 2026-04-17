# 开发环境搭建指南

## 环境要求

### 必需软件
- **JDK 17+**: 后端开发
- **Maven 3.9+**: 后端构建
- **Node.js 18+**: 前端和小程序开发
- **MySQL 8.0+**: 数据库
- **Redis 7+**: 缓存（可选）
- **Docker & Docker Compose**: 容器化部署（推荐）

### 推荐工具
- **IDE**: IntelliJ IDEA / VS Code
- **Git**: 版本控制
- **Postman**: API测试
- **微信开发者工具**: 小程序开发

## 快速开始（Docker方式）

### 1. 克隆项目

```bash
git clone https://github.com/your-org/intelligent-blind-cane-system.git
cd intelligent-blind-cane-system
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 文件，配置数据库密码等敏感信息
```

### 3. 启动服务

```bash
# 使用初始化脚本
chmod +x scripts/development/init-dev.sh
./scripts/development/init-dev.sh

# 或手动启动
docker-compose up -d
```

### 4. 访问服务

- **后端API**: http://localhost:8081
- **前端管理**: http://localhost:3000
- **Nginx**: http://localhost:80

## 手动搭建方式

### 1. 数据库配置

#### 安装MySQL

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install mysql-server

# macOS
brew install mysql

# Windows
# 下载并安装 MySQL Installer
```

#### 创建数据库

```sql
CREATE DATABASE smart_cane CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 导入数据库脚本

```bash
mysql -u root -p smart_cane < database/mysql/schema/smart_cane.sql
```

### 2. Redis配置

```bash
# Ubuntu/Debian
sudo apt install redis-server

# macOS
brew install redis

# Windows
# 下载并安装 Redis for Windows
```

### 3. 后端配置

#### 安装JDK 17

```bash
# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# macOS
brew install openjdk@17

# Windows
# 下载并安装 JDK 17
```

#### 配置Maven

```bash
# 下载Maven
wget https://dlcdn.apache.org/maven/maven-3/3.9.12/binaries/apache-maven-3.9.12-bin.tar.gz

# 解压
tar -xzf apache-maven-3.9.12-bin.tar.gz

# 配置环境变量
export MAVEN_HOME=/path/to/maven
export PATH=$MAVEN_HOME/bin:$PATH
```

#### 启动后端

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 4. 前端配置

#### 安装Node.js

```bash
# Ubuntu/Debian
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# macOS
brew install node

# Windows
# 下载并安装 Node.js
```

#### 安装依赖

```bash
cd frontend
npm install
```

#### 启动前端

```bash
npm run dev
```

### 5. 小程序配置

#### 安装HBuilderX

下载并安装 [HBuilderX](https://www.dcloud.io/hbuilderx.html)

#### 导入项目

1. 打开HBuilderX
2. 文件 -> 导入 -> 从本地目录导入
3. 选择 `miniapp` 目录

#### 配置小程序

1. 打开 `manifest.json`
2. 配置小程序AppID
3. 配置服务器域名

#### 运行小程序

1. 运行 -> 运行到小程序模拟器
2. 或 运行 -> 运行到微信开发者工具

## 开发工具配置

### VS Code配置

安装推荐扩展：
- ESLint
- Prettier
- GitLens
- Vue - Official
- Java Extension Pack
- Maven for Java

创建 `.vscode/settings.json`:

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "eslint.validate": ["javascript", "javascriptreact", "typescript", "vue"],
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  }
}
```

### IntelliJ IDEA配置

1. 安装Lombok插件
2. 配置Maven
3. 配置代码风格
4. 配置检查规范

## 常见问题

### Q: Maven编译失败

A: 检查JDK版本是否为17，Maven是否配置正确

### Q: 前端npm install失败

A: 尝试使用淘宝镜像：`npm config set registry https://registry.npmmirror.com`

### Q: 数据库连接失败

A: 检查数据库服务是否启动，用户名密码是否正确

### Q: Docker启动失败

A: 检查Docker服务是否运行，端口是否被占用

## 开发规范

### 分支管理

- `main`: 生产分支
- `develop`: 开发分支
- `feature/*`: 功能分支
- `hotfix/*`: 紧急修复分支

### 提交规范

```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式
refactor: 重构
perf: 性能优化
test: 测试
chore: 构建/工具
```

### 代码规范

- 后端：遵循阿里巴巴Java开发手册
- 前端：遵循Vue官方风格指南
- 小程序：遵循Uni-app开发规范
