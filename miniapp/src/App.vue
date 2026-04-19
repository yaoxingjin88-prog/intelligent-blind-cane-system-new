<template>
  <view></view>
</template>

<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'

onLaunch(() => {
  console.log('App Launch')
})

onShow(() => {
  console.log('App Show')
})

onHide(() => {
  console.log('App Hide')
})
</script>

<style lang="scss">
@import 'uview-plus/theme.scss';
@import 'uview-plus/index.scss';
@import './static/font_5163035_73o8ucu6bq/iconfont.css';

page {
  background-color: #f7f8fa;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft Yahei', sans-serif;
}

/* 全局样式变量 */
:root {
  --primary-color: #07c160;
  --danger-color: #ee0a24;
  --warning-color: #ff976a;
  --info-color: #1989fa;
  --success-color: #07c160;
  --text-primary: #1f2937;
  --text-secondary: #6b7280;
  --text-tertiary: #9ca3af;
  --bg-primary: #ffffff;
  --bg-secondary: #f3f4f6;
  --border-color: #e5e7eb;
}

/* 通用类 */
.container {
  padding: 0 32rpx;
}

.card {
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f3f4f6;
}

.btn-primary {
  background-color: var(--primary-color);
  color: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx 48rpx;
  font-size: 28rpx;
  font-weight: 600;
}

.text-primary {
  color: var(--primary-color);
}

.text-danger {
  color: var(--danger-color);
}

.text-warning {
  color: var(--warning-color);
}

.text-success {
  color: var(--success-color);
}

.bg-primary {
  background-color: var(--primary-color);
}

.bg-danger {
  background-color: var(--danger-color);
}

.bg-warning {
  background-color: var(--warning-color);
}

.bg-success {
  background-color: var(--success-color);
}

/* 隐藏滚动条 */
.hide-scrollbar::-webkit-scrollbar {
  display: none;
}

.hide-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* 渐变背景 */
.gradient-bg {
  background: linear-gradient(135deg, #e6f8ee 0%, #f7f8fa 100%);
}

/* 淡入动画 */
.fade-in {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
