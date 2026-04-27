# 路口辅助视觉原型

该目录提供一个最小可运行的视觉原型，用于演示：

- 红绿灯识别
- 斑马线存在检测
- 斑马线方向提示（LEFT / CENTER / RIGHT）
- 将识别结果上报到后端 crossing-assist 接口

## 安装依赖

```bash
pip install -r scripts/vision/requirements-vision.txt
```

## 启动方式

```bash
python scripts/vision/crossing_assist_demo.py --api-base http://127.0.0.1:8081/api --device-id ESP32_001
```

## 常用参数

- `--api-base`
  - 后端 API 根路径，默认 `http://127.0.0.1:8081/api`

- `--device-id`
  - 识别结果上报的设备编号

- `--camera`
  - 摄像头序号，默认 `0`

- `--interval`
  - 最短上报间隔，默认 `1.5` 秒

- `--width`
  - 预处理帧宽度，默认 `960`

- `--no-preview`
  - 不打开本地预览窗口，仅做识别和上报

## 当前原型说明

### 红绿灯识别
当前使用 HSV 颜色阈值 + 圆形轮廓筛选，适合：

- 近距离演示场景
- 灯色对比明显的固定路口
- 室内模拟交通灯环境

### 斑马线方向提示
当前使用亮色区域提取 + 条纹轮廓筛选，估算斑马线主区域中心，输出：

- `LEFT`
- `CENTER`
- `RIGHT`

### 注意事项
该版本是比赛演示原型，不是复杂道路全场景鲁棒方案。建议先在：

- 固定灯箱
- 地贴斑马线
- 光照稳定环境

中进行演示与调参。
