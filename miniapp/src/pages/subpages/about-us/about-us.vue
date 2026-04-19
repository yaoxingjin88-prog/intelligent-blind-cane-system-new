<template>
  <view class="about-us-page">
    <scroll-view class="content" scroll-y>
      <!-- Logo区域 -->
      <view class="logo-section">
        <view class="logo">
          <text class="logo-icon">🚶</text>
        </view>
        <text class="app-name">智能盲杖系统</text>
        <text class="app-version">Version 1.0.0 (Build 240101)</text>
      </view>

      <!-- 功能列表 -->
      <view class="card">
        <view class="menu-item">
          <text class="menu-title">检查新版本</text>
          <text class="menu-badge">已是最新版</text>
        </view>
        <view class="menu-item">
          <text class="menu-title">功能介绍</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item">
          <text class="menu-title">官方网站</text>
          <text class="menu-desc">www.smart-cane.com</text>
        </view>
      </view>

      <!-- 版权信息 -->
      <view class="copyright">
        <text class="copyright-text">Copyright © 2024 智能盲杖团队</text>
        <text class="copyright-text">保留所有权利</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.about-us-page {
  min-height: 100vh;
  background: #f7f8fa;
  box-sizing: border-box;
}

.content {
  padding: 32rpx;
  padding-bottom: 120rpx;
  width: 100%;
  box-sizing: border-box;
}

.logo-section {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 64rpx 32rpx;
  margin-bottom: 24rpx;
  text-align: center;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f3f4f6;
  width: 100%;
  box-sizing: border-box;

  .logo {
    width: 160rpx;
    height: 160rpx;
    background: linear-gradient(135deg, #07c160 0%, #05a050 100%);
    border-radius: 32rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 32rpx;
    box-shadow: 0 16rpx 40rpx rgba(7, 193, 96, 0.3);

    .logo-icon {
      font-size: 80rpx;
    }
  }

  .app-name {
    font-size: 36rpx;
    font-weight: bold;
    color: #1f2937;
    display: block;
    margin-bottom: 16rpx;
  }

  .app-version {
    font-size: 24rpx;
    color: #9ca3af;
  }
}

.card {
  background: #ffffff;
  border-radius: 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f3f4f6;
  overflow: hidden;
  width: 100%;
  box-sizing: border-box;

  .menu-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 32rpx;
    border-bottom: 1rpx solid #f3f4f6;
    background: #ffffff;
    transition: background 0.3s;

    &:active {
      background: #f9fafb;
    }

    &:last-child {
      border-bottom: none;
    }

    .menu-title {
      font-size: 28rpx;
      color: #1f2937;
    }

    .menu-desc {
      font-size: 24rpx;
      color: #9ca3af;
    }

    .menu-badge {
      font-size: 24rpx;
      color: #07c160;
      background: #d1fae5;
      padding: 8rpx 16rpx;
      border-radius: 12rpx;
    }

    .menu-arrow {
      font-size: 48rpx;
      color: #d1d5db;
    }
  }
}

.copyright {
  text-align: center;
  margin-top: 48rpx;
  width: 100%;
  box-sizing: border-box;

  .copyright-text {
    font-size: 24rpx;
    color: #9ca3af;
    display: block;
    margin-bottom: 8rpx;

    &:last-child {
      margin-bottom: 0;
    }
  }
}
</style>
