<template>
  <view class="login-page">
    <!-- 头部装饰与 Logo -->
    <view class="header">
      <!-- 柔和的光晕背景 -->
      <view class="glow glow-1"></view>
      <view class="glow glow-2"></view>
      
      <view class="logo-container">
        <view class="logo">
          <text class="icon">🚶</text>
        </view>
        <text class="title">{{ isRegister ? '欢迎注册' : '欢迎登录' }}</text>
        <text class="subtitle">智能盲杖系统家属端</text>
      </view>
    </view>

    <!-- 登录/注册表单 -->
    <view class="form-container">
      <!-- 登录表单 -->
      <view v-if="!isRegister" class="phone-login">
        <view class="form-item">
          <view class="input-wrapper">
            <text class="icon iconfont icon-shouji"></text>
            <input 
              v-model="formData.phone" 
              type="number" 
              placeholder="请输入手机号" 
              maxlength="11"
              class="input"
            />
          </view>
        </view>

        <view class="form-item">
          <view class="input-wrapper">
            <text class="icon iconfont icon-yanzhengyanzhengma"></text>
            <input 
              v-model="formData.code" 
              type="number" 
              placeholder="请输入验证码（测试可用123456）" 
              maxlength="6"
              class="input"
            />
            <text 
              class="code-btn" 
              :class="{ disabled: countdown > 0 }"
              @click="sendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </text>
          </view>
        </view>

        <view class="login-btn" @click="handlePhoneLogin" :class="{ disabled: loading }">
          {{ loading ? '登录中...' : '登 录' }}
        </view>

        <!-- 第三方登录分隔线 -->
        <view class="divider">
          <view class="line"></view>
          <text class="text">其他登录方式</text>
          <view class="line"></view>
        </view>

        <!-- 微信登录按钮 -->
        <view class="wechat-btn" @click="handleWechatLogin">
          <text class="icon">💬</text>
          <text>微信一键登录</text>
        </view>
      </view>

      <!-- 注册表单 -->
      <view v-else class="register-form">
        <view class="form-item">
          <view class="input-wrapper">
            <text class="icon iconfont icon-shouji"></text>
            <input 
              v-model="registerForm.phone" 
              type="number" 
              placeholder="请输入手机号" 
              maxlength="11"
              class="input"
            />
          </view>
        </view>

        <view class="form-item">
          <view class="input-wrapper">
            <text class="icon iconfont icon-yanzhengyanzhengma"></text>
            <input 
              v-model="registerForm.code" 
              type="number" 
              placeholder="请输入验证码（测试可用123456）" 
              maxlength="6"
              class="input"
            />
            <text 
              class="code-btn" 
              :class="{ disabled: registerCountdown > 0 }"
              @click="sendRegisterCode"
            >
              {{ registerCountdown > 0 ? `${registerCountdown}s` : '获取验证码' }}
            </text>
          </view>
        </view>

        <view class="form-item">
          <view class="input-wrapper">
            <text class="icon iconfont icon-shouji"></text>
            <input 
              v-model="registerForm.password" 
              type="password" 
              placeholder="请设置密码" 
              class="input"
            />
          </view>
        </view>

        <view class="form-item">
          <view class="input-wrapper">
            <text class="icon iconfont icon-shouji"></text>
            <input 
              v-model="registerForm.confirmPassword" 
              type="password" 
              placeholder="请确认密码" 
              class="input"
            />
          </view>
        </view>

        <view class="login-btn" @click="handleRegister" :class="{ disabled: loading }">
          {{ loading ? '注册中...' : '注 册' }}
        </view>
      </view>

      <!-- 底部协议勾选 -->
      <view class="agreement">
        <view 
          class="checkbox" 
          :class="{ checked: agree }"
          @click="agree = !agree"
        >
          <text v-if="agree" class="check">✓</text>
        </view>
        <view class="agreement-text">
          <text>我已阅读并同意</text>
          <text class="link" @click="showAgreement('user')">《用户协议》</text>
          <text>和</text>
          <text class="link" @click="showAgreement('privacy')">《隐私政策》</text>
        </view>
      </view>

      <view class="footer">
        <text v-if="!isRegister" class="link" @click="toggleMode">还没有账号？立即注册</text>
        <text v-else class="link" @click="toggleMode">已有账号？立即登录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { login, wechatLogin, register } from '@/api/auth'
import { useUserStore } from '@/store'
import { validatePhone } from '@/utils'

const userStore = useUserStore()

// 登录类型
const loginType = ref('phone')
const loading = ref(false)
const showRegister = ref(false)
const agree = ref(false)
const isRegister = ref(false)

// 倒计时
const countdown = ref(0)
const registerCountdown = ref(0)

// 表单数据
const formData = reactive({
  phone: '',
  code: ''
})

const registerForm = reactive({
  phone: '',
  code: '',
  password: '',
  confirmPassword: ''
})

// 发送验证码
const sendCode = () => {
  if (!validatePhone(formData.phone)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  
  // 模拟发送验证码
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
  
  uni.showToast({ title: '验证码已发送', icon: 'success' })
}

// 发送注册验证码
const sendRegisterCode = () => {
  if (!validatePhone(registerForm.phone)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  
  registerCountdown.value = 60
  const timer = setInterval(() => {
    registerCountdown.value--
    if (registerCountdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
  
  uni.showToast({ title: '验证码已发送', icon: 'success' })
}

// 手机号登录
const handlePhoneLogin = async () => {
  if (!agree.value) {
    uni.showToast({ title: '请先同意用户协议和隐私政策', icon: 'none' })
    return
  }
  if (!validatePhone(formData.phone)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  
  // 测试模式：验证码为123456时跳过验证
  const isTestMode = formData.code === '123456' || !formData.code
  
  if (!isTestMode && !formData.code) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }
  
  loading.value = true
  try {
    const res = await login({
      phone: formData.phone,
      code: isTestMode ? '123456' : formData.code
    })
    
    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data.userInfo)
    
    uni.showToast({ title: '登录成功', icon: 'success' })
    
    setTimeout(() => {
      uni.switchTab({
        url: '/pages/home/home'
      })
    }, 1500)
  } catch (error) {
    console.error('登录失败', error)
    uni.showToast({ title: '登录失败，请检查手机号和验证码', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 显示协议
const showAgreement = (type) => {
  if (type === 'user') {
    uni.navigateTo({ url: '/pages/subpages/user-agreement/user-agreement' })
  } else {
    uni.navigateTo({ url: '/pages/subpages/privacy-policy/privacy-policy' })
  }
}

// 切换登录/注册模式
const toggleMode = () => {
  isRegister.value = !isRegister.value
  // 清空表单
  if (isRegister.value) {
    formData.phone = ''
    formData.code = ''
  } else {
    registerForm.phone = ''
    registerForm.code = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
  }
}

// 微信登录
const handleWechatLogin = async () => {
  loading.value = true
  try {
    // 获取微信授权信息
    uni.getUserProfile({
      desc: '用于完善用户资料',
      success: async (profileRes) => {
        try {
          const res = await wechatLogin({
            userInfo: profileRes.userInfo
          })
          
          userStore.setToken(res.data.token)
          userStore.setUserInfo(res.data.userInfo)
          
          uni.showToast({ title: '登录成功', icon: 'success' })
          
          setTimeout(() => {
            uni.switchTab({
              url: '/pages/home/home'
            })
          }, 1500)
        } catch (error) {
          console.error('微信登录失败', error)
        }
      },
      fail: () => {
        uni.showToast({ title: '授权失败', icon: 'none' })
      }
    })
  } catch (error) {
    console.error('微信登录失败', error)
  } finally {
    loading.value = false
  }
}

// 注册
const handleRegister = async () => {
  if (!validatePhone(registerForm.phone)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  if (!registerForm.code) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }
  if (!registerForm.password) {
    uni.showToast({ title: '请设置密码', icon: 'none' })
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    uni.showToast({ title: '两次密码不一致', icon: 'none' })
    return
  }
  
  try {
    const res = await register({
      phone: registerForm.phone,
      code: registerForm.code,
      password: registerForm.password
    })
    
    uni.showToast({ title: '注册成功', icon: 'success' })
    
    // 自动切换到登录模式并填充手机号
    isRegister.value = false
    formData.phone = registerForm.phone
    formData.code = registerForm.code
    
    // 自动登录
    setTimeout(() => {
      handlePhoneLogin()
    }, 1000)
  } catch (error) {
    console.error('注册失败', error)
  }
}
</script>

<style lang="scss" scoped>
@import "@/static/font_5163035_73o8ucu6bq/iconfont.css";

.login-page {
  min-height: 100vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
}

.header {
  height: 360rpx;
  background: linear-gradient(180deg, #dcfce7 0%, #f0fdf4 50%, #ffffff 100%);
  position: relative;
  overflow: hidden;
  padding-bottom: 32rpx;
  z-index: 1;
  
  .glow {
    position: absolute;
    border-radius: 50%;
    filter: blur(80rpx);
    pointer-events: none;
    z-index: 0;
  }
  
  .glow-1 {
    width: 320rpx;
    height: 320rpx;
    background: rgba(7, 193, 96, 0.1);
    top: -80rpx;
    right: -80rpx;
  }
  
  .glow-2 {
    width: 256rpx;
    height: 256rpx;
    background: rgba(7, 193, 96, 0.05);
    top: 160rpx;
    left: -80rpx;
  }
  
  .logo-container {
    position: relative;
    z-index: 1;
    padding: 0 64rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
  }
  
  .logo {
    width: 96rpx;
    height: 96rpx;
    background: #07c160;
    border-radius: 24rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 24rpx;
    box-shadow: 0 12rpx 32rpx rgba(7, 193, 96, 0.3);
    
    .icon {
      font-size: 48rpx;
    }
  }
  
  .title {
    font-size: 40rpx;
    font-weight: bold;
    color: #1f2937;
    display: block;
    margin-bottom: 8rpx;
  }
  
  .subtitle {
    font-size: 22rpx;
    color: #6b7280;
  }
}

.form-container {
  flex: 1;
  padding: 0 64rpx;
  padding-top: 48rpx;
  position: relative;
  z-index: 100;
}

.form-item {
  margin-bottom: 32rpx;
  
  .input-wrapper {
    display: flex;
    align-items: center;
    background: rgba(255, 255, 255, 0.8);
    border-radius: 24rpx;
    padding: 0 32rpx;
    height: 112rpx;
    border: none;
    transition: all 0.3s;
    box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
    
    &:focus-within {
      background: #ffffff;
      box-shadow: 0 4rpx 16rpx rgba(7, 193, 96, 0.15);
      transform: translateY(-2rpx);
    }
    
    .icon {
      font-size: 44rpx;
      margin-right: 24rpx;
      opacity: 0.7;
      color: #07c160;
    }
    
    .input {
      flex: 1;
      font-size: 28rpx;
      color: #1f2937;
      border: none;
      background: transparent;
      outline: none;
    }
    
    .code-btn {
      font-size: 24rpx;
      color: #07c160;
      margin-left: 16rpx;
      cursor: pointer;
      
      &.disabled {
        color: #9ca3af;
        cursor: default;
      }
      
      &:not(.disabled):active {
        opacity: 0.7;
      }
    }
  }
}

.login-btn {
  width: 100%;
  height: 112rpx;
  background: linear-gradient(135deg, #07c160 0%, #059669 100%);
  color: #ffffff;
  border-radius: 24rpx;
  font-size: 28rpx;
  font-weight: bold;
  margin-top: 64rpx;
  box-shadow: 0 8rpx 24rpx rgba(7, 193, 96, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  
  &:active {
    opacity: 0.8;
    transform: scale(0.98);
  }
  
  &.disabled {
    opacity: 0.6;
    pointer-events: none;
  }
}

.divider {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 64rpx 0 48rpx;
  
  .line {
    flex: 1;
    height: 2rpx;
    background: #e5e7eb;
  }
  
  .text {
    margin: 0 32rpx;
    font-size: 24rpx;
    color: #9ca3af;
  }
}

.wechat-btn {
  width: 100%;
  height: 112rpx;
  background: rgba(7, 193, 96, 0.1);
  color: #07c160;
  border-radius: 24rpx;
  font-size: 28rpx;
  font-weight: bold;
  border: 2rpx solid rgba(7, 193, 96, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  cursor: pointer;
  
  &:active {
    opacity: 0.8;
  }
  
  .icon {
    font-size: 36rpx;
  }
}

.agreement {
  display: flex;
  align-items: flex-start;
  margin-top: 64rpx;
  
  .checkbox {
    width: 32rpx;
    height: 32rpx;
    border-radius: 8rpx;
    border: 2rpx solid #d1d5db;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 16rpx;
    margin-top: 4rpx;
    transition: all 0.2s;
    
    &.checked {
      background: #07c160;
      border-color: #07c160;
    }
    
    .check {
      color: #ffffff;
      font-size: 20rpx;
      font-weight: bold;
    }
  }
  
  .agreement-text {
    flex: 1;
    font-size: 24rpx;
    color: #6b7280;
    line-height: 1.6;
    
    .link {
      color: #07c160;
      font-weight: 500;
    }
  }
}

.footer {
  text-align: center;
  margin-top: 48rpx;
  
  .link {
    font-size: 28rpx;
    color: #07c160;
  }
}

.form-item {
  margin-bottom: 32rpx;
  
  .label {
    display: block;
    font-size: 28rpx;
    color: #374151;
    margin-bottom: 16rpx;
  }
  
  .input-wrapper {
    display: flex;
    align-items: center;
    background: #f9fafb;
    border-radius: 16rpx;
    padding: 0 24rpx;
    border: 2rpx solid #e5e7eb;
    
    &:focus-within {
      border-color: #07c160;
    }
    
    .icon {
      font-size: 36rpx;
      margin-right: 16rpx;
    }
    
    .input {
      flex: 1;
      height: 88rpx;
      font-size: 28rpx;
      color: #1f2937;
    }
    
    .code-btn {
      padding: 16rpx 24rpx;
      font-size: 24rpx;
      color: #07c160;
      background: transparent;
      border: none;
      
      &:disabled {
        color: #9ca3af;
      }
    }
  }
  
  .input {
    width: 100%;
    height: 88rpx;
    background: #f9fafb;
    border-radius: 16rpx;
    padding: 0 24rpx;
    font-size: 28rpx;
    color: #1f2937;
    border: 2rpx solid #e5e7eb;
    box-sizing: border-box;
  }
}

.login-btn {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #07c160 0%, #05a050 100%);
  color: #ffffff;
  border-radius: 16rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
  margin-top: 48rpx;
}

.wechat-btn {
  width: 100%;
  height: 88rpx;
  background: #07c160;
  color: #ffffff;
  border-radius: 16rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  
  .icon {
    font-size: 36rpx;
  }
}

.tips {
  display: block;
  text-align: center;
  font-size: 24rpx;
  color: #9ca3af;
  margin-top: 24rpx;
}

.footer {
  text-align: center;
  margin-top: 32rpx;
  
  .link {
    font-size: 28rpx;
    color: #07c160;
  }
}

.register-form {
  .form-item {
    margin-bottom: 32rpx;
    
    .input-wrapper {
      display: flex;
      align-items: center;
      background: rgba(255, 255, 255, 0.8);
      border-radius: 24rpx;
      padding: 0 32rpx;
      height: 112rpx;
      border: none;
      transition: all 0.3s;
      box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
      
      &:focus-within {
        background: #ffffff;
        box-shadow: 0 4rpx 16rpx rgba(7, 193, 96, 0.15);
        transform: translateY(-2rpx);
      }
      
      .icon {
        font-size: 44rpx;
        margin-right: 24rpx;
        opacity: 0.7;
        color: #07c160;
      }
      
      .input {
        flex: 1;
        font-size: 28rpx;
        color: #1f2937;
        border: none;
        background: transparent;
        outline: none;
      }
      
      .code-btn {
        font-size: 24rpx;
        color: #07c160;
        margin-left: 16rpx;
        cursor: pointer;
        
        &.disabled {
          color: #9ca3af;
          cursor: default;
        }
        
        &:not(.disabled):active {
          opacity: 0.7;
        }
      }
    }
  }
}
</style>
