# 智能盲杖小程序端

基于 Uni-app 开发的智能盲杖监护小程序，用于监护人实时监控视障老人的安全状况。

## 技术栈

- **框架**: Uni-app (支持多端发布)
- **UI组件库**: uView Plus
- **状态管理**: Pinia
- **地图服务**: 微信小程序地图组件 / 高德地图
- **图表库**: ECharts for WeChat Mini Program

## 项目结构

```
mini/
├── api/                      # API接口
│   ├── index.js             # 请求封装
│   ├── auth.js              # 认证接口
│   ├── device.js            # 设备接口
│   ├── alarm.js             # 报警接口
│   ├── fence.js             # 围栏接口
│   ├── trajectory.js        # 轨迹接口
│   ├── statistics.js        # 统计接口
│   └── message.js           # 消息接口
├── pages/                   # 页面
│   ├── login/               # 登录页
│   ├── home/                # 首页（实时监控）
│   ├── alarm/               # 报警管理
│   ├── fence/               # 电子围栏 & 轨迹
│   ├── profile/             # 个人中心
│   ├── device/              # 设备管理
│   └── subpages/            # 子页面
│       ├── elder-info/      # 老人信息管理
│       ├── dnd-settings/    # 免打扰设置
│       ├── system-settings/ # 系统设置
│       ├── privacy-policy/  # 隐私政策
│       └── about-us/        # 关于我们
├── store/                   # 状态管理
│   ├── index.js             # 用户状态
│   ├── device.js            # 设备状态
│   ├── alarm.js             # 报警状态
│   ├── elder.js             # 老人信息状态
│   └── settings.js          # 设置状态
├── utils/                   # 工具函数
│   ├── index.js             # 通用工具
│   └── map.js               # 地图工具
├── static/                  # 静态资源
│   ├── images/              # 图片资源
│   └── tabbar/              # 底部导航图标
├── App.vue                  # 应用入口
├── main.js                  # 主入口文件
├── manifest.json            # 应用配置
├── pages.json               # 页面配置
└── package.json             # 依赖配置
```

## 功能模块

### 1. 用户认证
- 手机号登录
- 微信授权登录
- 用户注册
- 实名认证

### 2. 设备管理
- 设备绑定（扫码/手动输入）
- 设备列表
- 设备解绑
- 设备切换

### 3. 实时监控
- 实时位置地图
- 设备状态（在线/离线、电量）
- 传感器数据（障碍物距离、步数、静止时长）
- 安全守护（跌倒检测、SOS）

### 4. 报警管理
- 报警列表（全部/未读/已处理）
- 报警详情
- 报警处理（忽略/处理）
- 报警统计

### 5. 电子围栏
- 围栏列表
- 创建围栏（圆形/矩形）
- 围栏开关
- 围栏地图预览

### 6. 轨迹回放
- 时间范围选择
- 轨迹播放控制
- 播放速度调节
- 轨迹统计（距离、时长、速度）

### 7. 个人中心
- 用户信息
- 设备管理入口
- 健康活动统计
- 老人信息管理
- 免打扰设置
- 系统设置

### 8. 子页面
- 老人信息管理（基本信息、联系信息、健康信息）
- 免打扰设置（定时免打扰、消息通知）
- 系统设置（隐私政策、关于我们）
- 隐私政策
- 关于我们

## 快速开始

### 环境要求

- Node.js 18+
- HBuilderX 或 VS Code + uni-app CLI
- 微信开发者工具（用于微信小程序）

### 安装依赖

```bash
npm install
```

### 开发运行

#### 微信小程序

```bash
npm run dev:mp-weixin
```

运行后，使用微信开发者工具打开 `dist/dev/mp-weixin` 目录。

#### H5

```bash
npm run dev:h5
```

### 生产构建

```bash
npm run build:mp-weixin  # 构建微信小程序
npm run build:h5        # 构建H5
```

## 配置说明

### 后端API地址

修改 `api/index.js` 中的 `BASE_URL`：

```javascript
const BASE_URL = 'http://localhost:8081/api'
```

### 微信小程序配置

修改 `manifest.json` 中的 `mp-weixin` 配置：

```json
{
  "mp-weixin": {
    "appid": "你的小程序AppID",
    "permission": {
      "scope.userLocation": {
        "desc": "您的位置信息将用于小程序位置功能"
      }
    }
  }
}
```

## API接口对接

小程序端通过 RESTful API 与后端对接，主要接口包括：

- 认证接口: `/api/mini/login`, `/api/mini/wechat-login`
- 设备接口: `/api/mini/devices`, `/api/mini/devices/{id}/location`
- 报警接口: `/api/mini/alarms`, `/api/mini/alarms/{id}/handle`
- 围栏接口: `/api/mini/fences`
- 轨迹接口: `/api/mini/devices/{id}/trajectory`
- 统计接口: `/api/mini/devices/{id}/activity`

详细接口文档请参考后端 API 文档。

## 状态管理

使用 Pinia 进行状态管理，主要 Store：

- `useUserStore`: 用户信息、登录状态
- `useDeviceStore`: 设备信息、设备列表、实时数据
- `useAlarmStore`: 报警列表、未读数量
- `useElderStore`: 老人信息
- `useSettingsStore`: 应用设置

## 注意事项

1. **权限申请**: 微信小程序需要申请位置权限，在 `manifest.json` 中配置
2. **地图服务**: 使用微信地图组件，需要配置相关权限
3. **Token管理**: Token 存储在本地，过期后自动跳转登录
4. **数据持久化**: 使用 Pinia 持久化插件，重要数据自动保存
5. **WebSocket**: 实时报警推送需要后端支持 WebSocket

## 开发规范

### 命名规范

- 页面文件: kebab-case (如: `home-page.vue`)
- 组件文件: PascalCase (如: `UserCard.vue`)
- 变量命名: camelCase
- 常量命名: UPPER_SNAKE_CASE

### 代码规范

- 使用 Vue 3 Composition API
- 使用 `<script setup>` 语法
- 组件使用 scoped 样式
- 使用 rpx 单位适配不同屏幕

### Git提交规范

```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 重构
perf: 性能优化
test: 测试
chore: 构建/工具变动
```

## 常见问题

### Q: 如何切换到其他小程序平台？

A: 修改 `manifest.json` 中对应的平台配置，然后运行对应的构建命令。

### Q: 地图不显示？

A: 检查是否配置了正确的地图权限，以及是否在 `manifest.json` 中启用了地图组件。

### Q: Token过期怎么办？

A: 系统会自动检测Token过期并跳转到登录页，无需手动处理。

### Q: 如何调试？

A: 使用微信开发者工具的调试功能，或在浏览器中调试H5版本。

## 后续优化

- [ ] 添加WebSocket实时通信
- [ ] 优化地图性能
- [ ] 添加更多图表类型
- [ ] 支持语音播报
- [ ] 添加视频通话功能
- [ ] 支持多语言
- [ ] 优化加载性能

## 联系方式

如有问题或建议，请联系项目维护者。
