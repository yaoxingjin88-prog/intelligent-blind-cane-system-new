#!/bin/bash

# 智能盲杖系统 - 启动所有服务

echo "启动智能盲杖系统所有服务..."

# 启动Docker服务
docker-compose up -d

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo "========================================="
echo "服务状态"
echo "========================================="
docker-compose ps

echo "========================================="
echo "服务访问地址"
echo "========================================="
echo "后端API: http://localhost:8081"
echo "前端管理: http://localhost:3000"
echo "Nginx: http://localhost:80"
echo "MySQL: localhost:3306"
echo "Redis: localhost:6379"
echo "========================================="
