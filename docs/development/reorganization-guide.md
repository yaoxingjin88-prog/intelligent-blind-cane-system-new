# 项目重组执行指南

## 重组步骤

### 第一阶段：准备备份

**重要：在执行重组前，请先备份整个项目**

```bash
# 备份整个项目
cd ..
tar -czf intelligent-blind-cane-system-backup-$(date +%Y%m%d).tar.gz intelligent-blind-cane-system/
```

### 第二阶段：创建新目录结构

```bash
cd intelligent-blind-cane-system

# 创建主要目录
mkdir -p backend frontend miniapp database scripts docs
mkdir -p database/mysql
mkdir -p scripts/development scripts/simulation
mkdir -p docs/architecture docs/development docs/miniapp
```

### 第三阶段：迁移后端代码

```bash
# 迁移后端代码
mv pro/bac/* backend/
mv pro/bac/.* backend/ 2>/dev/null || true
rm -rf backend/pro
```

### 第四阶段：迁移前端代码

```bash
# 迁移前端代码
mv pro/fro/* frontend/
mv pro/fro/.* frontend/ 2>/dev/null || true
rm -rf frontend/pro
```

### 第五阶段：迁移数据库脚本

```bash
# 迁移数据库脚本
mv pro/sc/* database/mysql/
# 如果有schema.sql和data.sql，分别放到对应目录
mv database/mysql/smart_cane.sql database/mysql/schema/ 2>/dev/null || true
```

### 第六阶段：迁移小程序代码

```bash
# 迁移小程序代码（保持mini目录或改为miniapp）
# 如果要改名为miniapp：
mv mini/* miniapp/
mv mini/.* miniapp/ 2>/dev/null || true
rm -rf miniapp/mini
```

### 第七阶段：迁移模拟器脚本

```bash
# 迁移模拟器脚本
mv LocationSimulator.java scripts/simulation/
mv simulate_location.py scripts/simulation/
mv smart_cane.sql database/mysql/schema/
```

### 第八阶段：迁移文档

```bash
# 迁移文档
mv 小程序开发文档.md docs/miniapp/development.md
mv 小程序端.html docs/miniapp/prototype.html
mv README.md docs/development/project-overview.md
mv 企业级项目结构说明.md docs/development/enterprise-structure.md
mv 项目重组指南.md docs/development/reorganization-guide.md
```

### 第九阶段：清理旧目录

```bash
# 删除旧目录
rm -rf pro
rm -f LocationSimulator.class
rm -f startup-error.log
rm -f temp_settings.xml
```

### 第十阶段：创建新的README

```bash
# 新的README会在根目录创建
# 旧的README已移动到docs/development/project-overview.md
```

## 重组后需要更新的配置

### 1. 后端配置更新

更新 `backend/src/main/resources/application.yml`：
- 检查数据库路径配置
- 检查文件上传路径配置

### 2. 前端配置更新

更新 `frontend/vite.config.ts`：
- 检查API代理配置
- 检查静态资源路径

### 3. 小程序配置更新

更新 `miniapp/manifest.json`：
- 检查API地址配置

### 4. Docker配置更新

更新 `docker/docker-compose.yml`：
- 确认服务路径正确
- 确认卷挂载路径正确

## 验证步骤

### 1. 验证后端

```bash
cd backend
mvn clean compile
mvn test
```

### 2. 验证前端

```bash
cd frontend
npm install
npm run build
```

### 3. 验证小程序

```bash
cd miniapp
npm install
npm run dev:mp-weixin
```


## 回滚方案

如果重组后出现问题，可以使用备份回滚：

```bash
cd ..
rm -rf intelligent-blind-cane-system
tar -xzf intelligent-blind-cane-system-backup-YYYYMMDD.tar.gz
```

## 注意事项

1. **Git历史**: 重组操作会改变文件路径，建议使用 `git mv` 而不是 `mv` 来保留Git历史

2. **依赖关系**: 检查所有模块间的依赖引用，确保路径正确

3. **配置文件**: 检查所有配置文件中的路径引用

4. **CI/CD**: 更新CI/CD配置中的路径

5. **文档**: 更新所有文档中的路径引用

## 使用Git mv保留历史

为了保留Git提交历史，建议使用Git命令进行迁移：

```bash
# 后端迁移
git mv pro/bac backend/
git mv pro/fro frontend/
git mv pro/sc database/mysql/
git mv mini miniapp/
git mv LocationSimulator.java scripts/simulation/
git mv simulate_location.py scripts/simulation/
git mv 小程序开发文档.md docs/miniapp/development.md
git mv 小程序端.html docs/miniapp/prototype.html
git mv README.md docs/development/project-overview.md

# 删除旧目录
git rm -r pro
git rm LocationSimulator.class startup-error.log temp_settings.xml
```

这样Git会记录文件移动的历史，而不是删除和创建新文件。
