# 硬件模拟（假硬件）

用本地面板扮演真机：对**已在管理端建档**的 `deviceId` 调用 `POST /api/sensor-data`，后台设备页会显示在线，围栏越界走正式报警链路。

## 说明

设备管理页已去掉「测试启动」。无真硬件时的联调、控位置、测围栏，统一用本模拟器（假硬件 `POST /api/sensor-data`）。

## 正确对接方式

1. 管理端「设备管理」添加设备（如 `SIM_001`），可绑定用户  
2. 实时监控里为该设备设置电子围栏（半径等）  
3. 打开本模拟器 → 登录 → 选中同一设备 → **启动上报**  
4. 设备页刷新后应变为「在线」——**没有「后台确认接收新设备」步骤**  
5. 点「一键移出围栏」→ 监控页出现越界报警  

未知 `deviceId` 硬上报虽可能被后端自动建档，但本面板要求先选已有设备，避免脏数据。

## 打开模拟器

**不要双击用 `file://` 打开**（origin 为 `null`，会被 CORS 拦住）。

推荐：先启动管理端前端（Vite `:3000`），浏览器打开：

http://localhost:3000/hardware-simulator.html  

该页走同源 `/api` 代理到后端，无需改 CORS。文件在 `frontend/public/hardware-simulator.html`（与本目录脚本同步维护）。

备选：本目录 `python -m http.server 8765`，再访问 http://localhost:8765/hardware-simulator.html，并把 API 填成 `http://localhost:8081`。

若配置了 `DEVICE_API_KEY`，在面板填入并随上报带 `X-Device-Key`。

## 其它脚本

- `simulate_location.py`：命令行随机抖点（写死 DEVICE001），适合简单探活  
- `LocationSimulator.java`：历史 Java 示例  

可控联调请优先用 `hardware-simulator.html`。
