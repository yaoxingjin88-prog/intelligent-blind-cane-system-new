#!/bin/bash

# 智能盲杖系统 - 停止所有服务

echo "停止智能盲杖系统所有服务..."

docker-compose down

echo "所有服务已停止"
