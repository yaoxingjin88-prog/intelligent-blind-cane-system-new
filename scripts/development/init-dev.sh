#!/bin/bash

# 智能盲杖系统 - 开发环境初始化脚本

echo "========================================="
echo "智能盲杖系统 - 开发环境初始化"
echo "========================================="

# 检查Docker是否安装
if ! command -v docker &> /dev/null; then
    echo "错误: Docker未安装，请先安装Docker"
    exit 1
fi

# 检查Docker Compose是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "错误: Docker Compose未安装，请先安装Docker Compose"
    exit 1
fi

# 创建必要的目录
echo "创建必要的目录..."
mkdir -p docker/mysql/data
mkdir -p docker/mysql/conf
mkdir -p docker/redis/data
mkdir -p backend/logs
mkdir -p configs/logback
mkdir -p configs/nginx
mkdir -p configs/nginx/conf.d
mkdir -p database/mysql/schema
mkdir -p database/mysql/data
mkdir -p database/mysql/migration

# 复制环境配置文件
if [ ! -f .env ]; then
    echo "复制环境配置文件..."
    cp .env.example .env
    echo "请编辑 .env 文件配置环境变量"
fi

# 初始化数据库
echo "初始化数据库..."
docker-compose up -d mysql redis

echo "等待MySQL启动..."
sleep 30

echo "导入数据库脚本..."
docker-compose exec -T mysql mysql -uroot -p123456 smart_cane < database/mysql/schema.sql

# 启动所有服务
echo "启动所有服务..."
docker-compose up -d

echo "========================================="
echo "开发环境初始化完成！"
echo "========================================="
echo "后端服务: http://localhost:8081"
echo "前端服务: http://localhost:3000"
echo "Nginx: http://localhost:80"
echo "========================================="
