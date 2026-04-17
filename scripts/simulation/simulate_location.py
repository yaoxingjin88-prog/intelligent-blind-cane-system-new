import requests
import random
import time
from datetime import datetime

def send_sensor_data(device_id="DEVICE001"):
    """模拟上报传感器数据，位置会有小范围随机变化"""
    # 基础位置：北京天安门附近
    base_lat = 39.9042
    base_lng = 116.4074

    # 随机偏移（模拟移动）
    lat = base_lat + random.uniform(-0.005, 0.005)  # 约±500米范围
    lng = base_lng + random.uniform(-0.005, 0.005)

    data = {
        "deviceId": device_id,
        "obstacleDistance": round(random.uniform(30, 150), 1),  # 障碍物距离 cm
        "isFall": False,
        "latitude": round(lat, 6),
        "longitude": round(lng, 6),
        "temperature": round(random.uniform(20, 30), 1),
        "humidity": round(random.uniform(40, 60), 1),
        "dataTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }

    try:
        response = requests.post("http://localhost:8081/api/sensor-data",
                               json=data,
                               headers={"Content-Type": "application/json"})
        if response.status_code == 200:
            print(f"[{datetime.now().strftime('%H:%M:%S')}] 已上报位置: ({lat:.6f}, {lng:.6f})")
        else:
            print(f"上报失败: {response.status_code}")
    except Exception as e:
        print(f"请求错误: {e}")

if __name__ == "__main__":
    print("开始模拟位置上报...")
    print("按 Ctrl+C 停止")
    try:
        while True:
            send_sensor_data("DEVICE001")
            time.sleep(3)  # 每3秒上报一次
    except KeyboardInterrupt:
        print("\n已停止")
