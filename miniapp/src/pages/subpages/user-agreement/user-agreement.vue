<template>
  <view class="user-agreement-page">
    <scroll-view class="content" scroll-y>
      <view class="policy-content">
        <view class="policy-header">
          <text class="policy-title">智能盲杖系统用户服务协议</text>
          <text class="policy-date">本协议更新时间：2024年1月1日</text>
        </view>

        <view class="policy-section">
          <text class="section-text">欢迎您使用智能盲杖系统（以下简称"本系统"）。请您在使用本系统之前，仔细阅读并理解本协议的全部内容。</text>
        </view>

        <view class="policy-section">
          <text class="section-title">1. 服务说明</text>
          <text class="section-text">本系统为视障人士及其家属提供智能盲杖管理服务，包括实时位置监控、跌倒检测报警、电子围栏、轨迹回放等功能。系统通过智能盲杖硬件设备采集环境及运动数据，经服务器处理后在家属端小程序中展示。</text>
        </view>

        <view class="policy-section">
          <text class="section-title">2. 用户注册与账号</text>
          <text class="section-text">您需要使用手机号注册并登录本系统。您应确保注册信息的真实性和准确性，并妥善保管账号密码。因账号密码泄露导致的一切后果由您自行承担。</text>
        </view>

        <view class="policy-section">
          <text class="section-title">3. 用户义务</text>
          <text class="section-text">您应合法、合规地使用本系统，不得将本系统用于任何非法目的。您应确保绑定设备的使用对象知悉并同意被监护，尊重被监护人的人身权利和隐私。</text>
        </view>

        <view class="policy-section">
          <text class="section-title">4. 服务限制</text>
          <text class="section-text">本系统依赖智能盲杖硬件设备和网络通信，在设备故障、网络中断、信号盲区等情况下可能无法正常提供服务。本系统不承担因上述原因导致的服务中断或数据延迟的责任。</text>
        </view>

        <view class="policy-section">
          <text class="section-title">5. 免责声明</text>
          <text class="section-text">本系统提供的报警、定位等功能仅作为辅助安全手段，不能替代专业护理和人工看护。用户不应完全依赖本系统保障被监护人安全。因不可抗力或系统固有技术限制导致的损失，本系统不承担赔偿责任。</text>
        </view>

        <view class="policy-section">
          <text class="section-title">6. 协议修改</text>
          <text class="section-text">我们有权根据业务发展需要修改本协议。修改后的协议将在系统内公布，继续使用本系统即视为接受修改后的协议。</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.user-agreement-page {
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
