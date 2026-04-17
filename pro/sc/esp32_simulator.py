#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ESP32 硬件模拟器
模拟智能盲杖设备每隔5秒向后端发送传感器数据
用于调试前端轨迹显示和报警功能

使用方法：
1. 确保后端服务运行在 http://localhost:8080
2. 运行脚本：python esp32_simulator.py
3. 按 Ctrl+C 停止
"""

import requests
import time
import random
import json
from datetime import datetime

# 配置
BACKEND_URL = "http://localhost:8082/api/sensor-data"
DEVICE_ID = "DEVICE001"  # 设备编号（与数据库一致）
INTERVAL = 5  # 发送间隔（秒）

# 模拟位置范围（北京某区域）
BASE_LATITUDE = 39.9042  # 基础纬度
BASE_LONGITUDE = 116.4074  # 基础经度

def generate_sensor_data():
    """生成随机传感器数据"""
    # 随机生成位置（在基础位置附近小幅波动）
    latitude = BASE_LATITUDE + random.uniform(-0.001, 0.001)
    longitude = BASE_LONGITUDE + random.uniform(-0.001, 0.001)
    
    # 随机生成电量（60%-100%）
    battery = random.randint(60, 100)
    
    # 随机生成超声波距离（20-200cm，偶尔有近距离障碍物）
    obstacle_distance = random.choice([
        random.uniform(20, 50),   # 近距离（有障碍物）
        random.uniform(100, 200)    # 远距离（无障碍）
    ])
    
    # 随机生成摔倒检测（90%概率正常，10%概率摔倒，用于测试报警）
    is_fall = random.random() < 0.1  # 10%概率摔倒
    
    # 温湿度
    temperature = random.uniform(20.0, 35.0)
    humidity = random.uniform(30.0, 80.0)
    
    data = {
        "deviceId": DEVICE_ID,
        "latitude": round(latitude, 6),
        "longitude": round(longitude, 6),
        "obstacleDistance": round(obstacle_distance, 2),
        "isFall": is_fall,
        "temperature": round(temperature, 2),
        "humidity": round(humidity, 2),
        "dataTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }
    
    return data

def send_data():
    """发送数据到后端"""
    data = generate_sensor_data()
    
    try:
        response = requests.post(
            BACKEND_URL,
            json=data,
            headers={"Content-Type": "application/json"},
            timeout=5
        )
        
        if response.status_code == 200:
            result = response.json()
            if result.get("code") == 200:
                status = "✅ 摔倒报警！" if data["isFall"] else "✅ 正常"
                print(f"[{datetime.now().strftime('%H:%M:%S')}] {status} "
                      f"位置: ({data['latitude']}, {data['longitude']}) "
                      f"电量: {data['obstacleDistance']:.1f}cm "
                      f"电池: {data.get('battery', '--')}%")
            else:
                print(f"⚠️ 服务器返回错误: {result.get('msg')}")
        else:
            print(f"⚠️ HTTP错误: {response.status_code}")
            
    except requests.exceptions.ConnectionError:
        print(f"❌ 连接失败: 请确保后端服务已启动 {BACKEND_URL}")
    except requests.exceptions.Timeout:
        print(f"⏱️ 请求超时")
    except Exception as e:
        print(f"❌ 错误: {e}")

def main():
    """主函数"""
    print("=" * 60)
    print("🦯 ESP32 智能盲杖硬件模拟器")
    print("=" * 60)
    print(f"设备编号: {DEVICE_ID}")
    print(f"后端地址: {BACKEND_URL}")
    print(f"发送间隔: {INTERVAL}秒")
    print(f"模拟位置: 北京 ({BASE_LATITUDE}, {BASE_LONGITUDE})")
    print("-" * 60)
    print("提示：10%概率模拟摔倒报警，用于测试前端报警功能")
    print("按 Ctrl+C 停止")
    print("=" * 60)
    
    try:
        while True:
            send_data()
            time.sleep(INTERVAL)
    except KeyboardInterrupt:
        print("\n\n👋 模拟器已停止")
        print("=" * 60)

if __name__ == "__main__":
    main()
