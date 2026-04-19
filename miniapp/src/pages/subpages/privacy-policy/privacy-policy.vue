<template>
  <view class="privacy-policy-page">
    <scroll-view class="content" scroll-y>
      <view class="policy-content">
        <view class="policy-header">
          <text class="policy-title">智能盲杖系统隐私保护指引</text>
          <text class="policy-date">本指引更新时间：2024年1月1日</text>
        </view>

        <view class="policy-section">
          <text class="section-text">欢迎使用智能盲杖系统！我们非常重视您的个人信息和隐私保护。为了提供更优质的守护服务，我们需要收集一些必要信息：</text>
        </view>

        <view class="policy-section">
          <text class="section-title">1. 位置信息收集</text>
          <text class="section-text">为了实现实时位置查看、电子围栏和轨迹回放功能，当设备处于开机状态时，系统会持续收集智能盲杖的 GPS/基站定位数据。这些数据仅用于家属端查看，系统不会将其用于任何商业营销。</text>
        </view>

        <view class="policy-section">
          <text class="section-title">2. 传感器数据处理</text>
          <text class="section-text">盲杖前端的超声波及惯性传感器数据（用于跌倒检测、避障等）主要在设备端进行本地计算，仅在触发报警时将状态回传至服务器，以确保响应及时并最大程度保护用户隐私。</text>
        </view>

        <view class="policy-section">
          <text class="section-title">3. 信息存储与安全</text>
          <text class="section-text">我们采用业界标准的数据加密技术保护您的账户及老人健康档案（如病史、血型等）。除您授权绑定的家庭成员外，任何第三方均无法访问这些信息。</text>
        </view>

        <view class="policy-section">
          <text class="section-title">4. 信息使用范围</text>
          <text class="section-text">收集的信息仅用于提供智能盲杖系统相关服务，包括但不限于：实时监控、报警通知、轨迹查询、健康统计等。我们不会将您的个人信息用于其他商业目的。</text>
        </view>

        <view class="policy-section">
          <text class="section-title">5. 您的权利</text>
          <text class="section-text">您有权访问、更正、删除您的个人信息。如需行使相关权利，请通过应用内的反馈渠道联系我们。</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.privacy-policy-page {
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

.policy-content {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 48rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f3f4f6;
  width: 100%;
  box-sizing: border-box;

  .policy-header {
    text-align: center;
    margin-bottom: 48rpx;

    .policy-title {
      font-size: 36rpx;
      font-weight: bold;
      color: #1f2937;
      display: block;
      margin-bottom: 16rpx;
    }

    .policy-date {
      font-size: 24rpx;
      color: #9ca3af;
    }
  }

  .policy-section {
    margin-bottom: 48rpx;

    &:last-child {
      margin-bottom: 0;
    }

    .section-title {
      font-size: 28rpx;
      font-weight: 600;
      color: #1f2937;
      display: block;
      margin-bottom: 16rpx;
    }

    .section-text {
      font-size: 28rpx;
      color: #4b5563;
      line-height: 1.8;
      display: block;
    }
  }
}
</style>
