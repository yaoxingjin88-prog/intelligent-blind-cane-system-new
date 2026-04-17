# 智能盲杖硬件实现方案

> 本文档为非硬件背景开发者提供的智能盲杖传感器选型与实现方案。方案以**低成本、易上手、模块化**为原则，所有传感器均可通过电商平台直接购买。

---

## 目录

- [一、项目功能与传感器需求分析](#一项目功能与传感器需求分析)
- [二、推荐硬件清单（带购买建议）](#二推荐硬件清单带购买建议)
- [三、传感器数据字段规范](#三传感器数据字段规范)
- [四、硬件架构图](#四硬件架构图)
- [五、实现方案（三阶段）](#五实现方案三阶段)
- [六、通信协议设计](#六通信协议设计)
- [七、接线方案](#七接线方案)
- [八、固件开发建议](#八固件开发建议)
- [九、成本预算](#九成本预算)
- [十、采购清单快照](#十采购清单快照)
- [十一、常见问题](#十一常见问题)

---

## 一、项目功能与传感器需求分析

根据项目现有后端功能（参考 `backend/MiniController.java`、`AlarmRecord` 等），系统需要以下数据：

| 功能模块 | 数据需求 | 传感器类型 |
|----------|----------|-----------|
| 障碍物检测 | 前方障碍物距离 (cm) | 超声波传感器 |
| 跌倒检测 | 三轴加速度、姿态角 | MPU6050（加速度+陀螺仪） |
| GPS 定位 | 经纬度、速度 | GPS 模块 |
| 电子围栏越界 | 实时 GPS | GPS 模块 |
| 轨迹回放 | 历史 GPS 点 | GPS 模块 |
| 环境感知 | 温度、湿度 | DHT11/DHT22 |
| 设备电量 | 电池电压/百分比 | 分压电路 + ADC |
| SOS 求助 | 按键触发 | 按键模块 |
| 震动提醒 | 震动反馈给用户 | 震动马达 |
| 数据上传 | 联网 | 4G / WiFi |

---

## 二、推荐硬件清单（带购买建议）

### 方案 A：入门版（推荐）—— ESP32 + 4G 扩展

**主控：ESP32-WROOM-32 开发板** ⭐ 推荐
- 价格：约 ¥30~50
- 优点：内置 WiFi + 蓝牙，GPIO 丰富，Arduino IDE 可开发，教程海量
- 购买关键词：`ESP32开发板 乐鑫`

### 核心传感器模块

| 模块 | 型号 | 价格 | 说明 | 购买关键词 |
|------|------|------|------|-----------|
| **超声波** | HC-SR04 | ¥5~10 | 测距 2~400cm，精度 3mm | `HC-SR04 超声波模块` |
| **加速度/陀螺仪** | MPU6050 | ¥10~15 | 6 轴运动检测，用于跌倒判断 | `MPU6050 六轴模块` |
| **GPS 定位** | ATGM336H（国产）或 NEO-6M/NEO-M8N | ¥25~60 | 输出 NMEA 协议经纬度 | `GPS模块 NEO-6M`、`ATGM336H` |
| **温湿度** | DHT22（精度高）/ DHT11（便宜） | ¥10~20 | 数字输出 | `DHT22 温湿度传感器` |
| **SOS 按键** | 自锁/轻触按键 | ¥1~3 | 物理紧急按钮 | `SOS按钮 防水` |
| **震动马达** | 币式/圆柱震动电机 | ¥2~5 | 障碍物警告反馈 | `震动马达 3V` |
| **蜂鸣器** | 有源蜂鸣器 | ¥2~5 | 报警音提示 | `有源蜂鸣器 5V` |
| **4G 模块** | Air780E / SIM7600 | ¥50~120 | 无 WiFi 时上传数据 | `合宙 Air780E` |
| **电池** | 18650 锂电 + 充电保护板 | ¥20~40 | 3.7V，≥2000mAh | `18650电池 保护板` |
| **杜邦线+面包板** | 套装 | ¥15~30 | 原型调试 | `面包板杜邦线套装` |

### 方案 B：简化版（仅 WiFi 室内测试）

如果仅做**演示或毕设**，可去掉 4G 模块，改用 WiFi 连接手机热点上传数据，成本降到 **¥150 以内**。

---

## 三、传感器数据字段规范

按照与现有后端 API (`GET /api/mini/devices/{id}/sensor-data`) 对齐的 JSON 格式：

```json
{
  "deviceId": "DEVICE001",
  "obstacleDistance": 120.5,
  "isFall": false,
  "acceleration": { "x": 0.1, "y": 0.2, "z": 9.8 },
  "gyroscope": { "x": 0.01, "y": 0.02, "z": 0.00 },
  "temperature": 25.5,
  "humidity": 60.0,
  "battery": 85,
  "signal": 4,
  "latitude": 39.9042,
  "longitude": 116.4074,
  "speed": 1.2,
  "sosPressed": false,
  "online": true,
  "updateTime": "2026-04-17 23:00:00"
}
```

### 数据采集频率建议

| 数据 | 上报间隔 | 说明 |
|------|---------|------|
| GPS 定位 | 10 秒 | 节省流量 |
| 加速度（跌倒检测） | 本地 50Hz 实时判断，**事件触发**时上报 | 跌倒瞬间立即推送 |
| 障碍物距离 | 本地 5Hz 实时反馈震动，**≤30cm** 时上报 | 不上传常规值 |
| 温湿度 | 1 分钟 | 变化慢 |
| 电池电量 | 1 分钟 | 低于 20% 立即上报 |
| SOS 按键 | 事件触发 | 立即上报 |

---

## 四、硬件架构图

```
                  ┌─────────────────┐
                  │   后端服务       │
                  │ Spring Boot     │
                  │ (HTTP/MQTT/WS)  │
                  └────────▲────────┘
                           │ JSON over HTTP/MQTT
                  ┌────────┴────────┐
                  │  4G 模块 / WiFi │
                  └────────▲────────┘
                           │ UART/AT
              ┌────────────┴────────────┐
              │     ESP32 主控          │
              │  (Arduino / MicroPython)│
              └─┬─┬─┬─┬─┬─┬─┬─┬───────┘
                │ │ │ │ │ │ │ │
        ┌───────┘ │ │ │ │ │ │ └───────┐
        │         │ │ │ │ │ │         │
     HC-SR04  MPU6050 GPS DHT22 SOS  震动马达 蜂鸣器  电池
     (超声波) (跌倒) (定位)(温湿度)(按键)(反馈) (警报)
```

---

## 五、实现方案（三阶段）

### 🟢 阶段 1：PC + 模拟器（1~2 天）**【当前阶段】**

**目标**：完全不买硬件，用脚本模拟设备上报数据。

- 已实现：`backend/MiniController.java` 中的接口返回模拟数据（带 `Random`）
- 建议补充：创建 `scripts/simulation/device-simulator.js` 定时 POST 数据到后端

**脚本示例**：
```javascript
// scripts/simulation/device-simulator.js
const axios = require('axios')
setInterval(() => {
  axios.post('http://localhost:8081/api/device/report', {
    deviceId: 'DEVICE001',
    obstacleDistance: 50 + Math.random() * 100,
    isFall: Math.random() < 0.01,
    latitude: 39.9042 + (Math.random() - 0.5) * 0.001,
    longitude: 116.4074 + (Math.random() - 0.5) * 0.001,
    battery: 60 + Math.floor(Math.random() * 40),
    temperature: 20 + Math.random() * 10,
    humidity: 40 + Math.random() * 30
  })
}, 5000)
```

### 🟡 阶段 2：面包板原型（1~2 周）

**目标**：最小硬件验证传感器功能。

**推荐套装**：ESP32 + HC-SR04 + MPU6050 + DHT22 + 面包板 ≈ ¥80~120

**开发工具**：
- Arduino IDE（新手友好）
- 现成库：`Adafruit_MPU6050`、`TinyGPSPlus`、`HTTPClient`

**最小代码框架**（Arduino，伪代码）：
```cpp
#include <WiFi.h>
#include <HTTPClient.h>
#include <Adafruit_MPU6050.h>
#include <TinyGPSPlus.h>

void setup() {
  WiFi.begin("ssid", "password");
  initSensors();
}

void loop() {
  float distance = readUltrasonic();
  bool isFall = detectFall();         // MPU6050 加速度突变判断
  double lat, lng; readGPS(&lat, &lng);
  
  if (distance < 30) triggerVibration();
  if (isFall) httpPost("/api/alarm", {...});
  
  if (millis() - lastReport > 10000) {
    httpPost("/api/device/report", buildJson());
    lastReport = millis();
  }
}
```

### 🔴 阶段 3：集成到盲杖（2~4 周）

**目标**：把模块装进盲杖外壳，加 4G 联网。

- 3D 打印外壳或改装拐杖
- PCB 打样（嘉立创免费打样）
- 用 18650 电池 + 充电保护板供电
- 加 4G 模块脱离 WiFi 环境

---

## 六、通信协议设计

### HTTP（简单）—— 推荐新手

ESP32 直接 POST JSON 到后端：

```
POST /api/device/report
Content-Type: application/json
{ "deviceId": "DEVICE001", "obstacleDistance": 50, ... }
```

### MQTT（进阶）—— 低功耗、高并发

适合多设备场景，后端已有 WebSocket 可接入 MQTT Broker（如 EMQX）。

### Topic 设计建议
- `cane/{deviceId}/sensor` — 传感器数据上行
- `cane/{deviceId}/alarm` — 报警事件上行
- `cane/{deviceId}/command` — 指令下发（如响铃寻回）

---

## 七、接线方案（ESP32）

| 传感器 | ESP32 引脚 |
|--------|-----------|
| HC-SR04 Trig | GPIO 5 |
| HC-SR04 Echo | GPIO 18 |
| MPU6050 SDA | GPIO 21（I2C 默认） |
| MPU6050 SCL | GPIO 22（I2C 默认） |
| DHT22 DATA | GPIO 4 |
| GPS TX | GPIO 16（RX2） |
| GPS RX | GPIO 17（TX2） |
| SOS 按键 | GPIO 13（上拉） |
| 震动马达 | GPIO 25（PWM） |
| 蜂鸣器 | GPIO 26 |
| 电池 ADC | GPIO 34 |

---

## 八、固件开发建议

### 跌倒检测算法（MPU6050）
```
1. 持续读取加速度模 |a| = √(ax² + ay² + az²)
2. 自由落体检测：|a| < 0.5g，持续 100ms
3. 冲击检测：|a| > 2.5g
4. 静止检测：冲击后 |a| ≈ 1g 且持续 3 秒
5. 三条件全满足 → 触发跌倒报警
```

### 电池电量换算
```
电池电压范围：3.0V (0%) ~ 4.2V (100%)
ESP32 ADC 最大 3.3V，需要 2:1 分压电阻
battery_percent = (voltage - 3.0) / (4.2 - 3.0) * 100
```

### 低功耗策略
- 正常模式：50Hz 加速度 + 10s GPS 上报
- 静止 5 分钟：进入 Light Sleep，仅保留按键中断
- 电量 < 15%：降低上报频率到 30s

---

## 九、成本预算

| 档次 | 清单 | 预算 | 适用 |
|------|------|------|------|
| **最低配** | ESP32 + HC-SR04 + MPU6050 + 电池 | **¥80** | 原型验证 |
| **标准配** | 标准配 + GPS + DHT22 + 震动 + SOS | **¥180** | 毕设/演示 |
| **完整配** | 标准配 + 4G 模块 + 外壳 + PCB | **¥400** | 实际使用 |

---

## 十、采购清单快照

### 淘宝/京东/拼多多搜索（复制即可）

```
ESP32-WROOM-32 开发板 type-c
HC-SR04 超声波测距模块
MPU6050 六轴加速度陀螺仪
ATGM336H GPS 模块 带天线
DHT22 AM2302 温湿度传感器
合宙 Air780E 4G 模块
18650 锂电池 3.7V 2600mAh 带保护板
3V 震动马达 扁平
5V 有源蜂鸣器
SOS 紧急按钮 防水
面包板杜邦线套装 MB-102
```

### 一站式购买建议
- **嘉立创商城**：元器件齐全，质量可靠，PCB 打样便宜
- **淘宝"中景园"、"爱板网"**：模块化传感器套装
- **深圳华强北**：线下市场，适合广东地区

---

## 十一、常见问题

### Q1：不会 C/C++ 怎么办？
**A**：用 MicroPython 开发 ESP32，语法与 Python 一致。但实时性比 Arduino 稍差，跌倒检测建议仍用 Arduino。

### Q2：GPS 室内搜不到星？
**A**：GPS 只在室外有效，室内测试建议用固定经纬度模拟。

### Q3：4G 卡去哪买？
**A**：合宙官方物联卡，约 ¥5/月 100MB，足够传感器数据用。

### Q4：电池续航？
**A**：2600mAh 电池正常模式 ≈ 12 小时，低功耗模式 ≈ 3 天。

### Q5：如何接入到现有后端？
**A**：后端已有 `/api/device/*` 和 `/api/alarm/*` 接口，ESP32 只需 HTTP POST 即可。跨域已在 Spring Boot 配置。

### Q6：跌倒误报率高？
**A**：建议采集真实数据后做阈值调优，或接入机器学习模型（可后期在后端做推理）。

---

## 下一步行动建议

1. ✅ **本周**：继续完善模拟器脚本，前后端功能跑通
2. ⬜ **下周**：先买 **ESP32 + HC-SR04 + MPU6050**（共约 ¥60），跑通最简 demo
3. ⬜ **第 3~4 周**：加入 GPS 和 4G，完成整机联调
4. ⬜ **第 5 周**：外壳 + 电池集成，真机测试

---

**编写日期**：2026-04-17  
**适用版本**：智能盲杖管理系统 v1.0  
**维护者**：项目开发团队
