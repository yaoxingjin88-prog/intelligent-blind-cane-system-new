<template>
  <view class="ai-page">
    <!-- 顶部：自动朗读开关 + 连续对话入口 -->
    <view class="top-bar">
      <view
        class="conv-btn"
        :class="{ active: conversationMode }"
        @click="conversationMode ? stopConversation() : startConversation()"
      >
        <text class="conv-icon">{{ conversationMode ? '⏹' : '🎙' }}</text>
        <text class="conv-text">{{ conversationMode ? '结束对话' : '语音对话' }}</text>
      </view>
      <view class="auto-tts" :class="{ on: autoTts }" @click="toggleAutoTts">
        <text class="auto-tts-icon">{{ autoTts ? '🔊' : '🔇' }}</text>
        <text class="auto-tts-text">{{ autoTts ? '朗读：开' : '朗读：关' }}</text>
      </view>
    </view>

    <!-- 消息列表 -->
    <scroll-view
      class="msg-list"
      scroll-y
      :scroll-top="scrollTop"
      :scroll-with-animation="true"
      @scroll="onScroll"
    >
      <!-- 欢迎语 -->
      <view class="welcome" v-if="messages.length === 0">
        <view class="avatar-lg">🤖</view>
        <text class="welcome-title">你好，我是明眼助手</text>
        <text class="welcome-sub">有问题尽管问我，也可以长按底部麦克风说话</text>
        <view class="quick-list">
          <view class="quick-item" v-for="q in quickQuestions" :key="q" @click="sendText(q)">
            {{ q }}
          </view>
        </view>
      </view>

      <!-- 消息气泡 -->
      <view
        v-for="(msg, idx) in messages"
        :key="idx"
        class="msg-row"
        :class="msg.role === 'user' ? 'row-user' : 'row-ai'"
      >
        <view class="avatar" v-if="msg.role === 'assistant'">🤖</view>
        <view class="bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
          <text class="bubble-text">{{ msg.content || (msg.loading ? '思考中...' : '') }}</text>
          <view v-if="msg.role === 'assistant' && msg.content && !msg.loading" class="bubble-actions">
            <text class="action-btn" @click="playTts(msg.content, idx)">
              {{ playingIdx === idx ? '⏸ 停止' : '🔊 朗读' }}
            </text>
            <text class="action-btn" @click="copyText(msg.content)">📋 复制</text>
          </view>
        </view>
        <view class="avatar" v-if="msg.role === 'user'">👤</view>
      </view>

      <view class="bottom-spacer"></view>
    </scroll-view>

    <!-- 录音中浮层 -->
    <view v-if="recording" class="recording-mask" @click="stopRecord">
      <view class="recording-box">
        <view class="recording-wave">
          <view v-for="i in 5" :key="i" class="wave-bar" :style="{ animationDelay: (i * 0.1) + 's' }"></view>
        </view>
        <text class="recording-text">正在聆听...松开发送</text>
        <text class="recording-hint">上滑手指可取消 / 点击屏幕结束</text>
      </view>
    </view>

    <!-- 底部输入栏 -->
    <view class="input-bar" :class="{ 'voice-mode': voiceMode }">
      <view class="mode-toggle" @click="toggleMode">
        <text class="icon">{{ voiceMode ? '⌨️' : '🎙️' }}</text>
      </view>

      <!-- 文字输入 -->
      <view v-if="!voiceMode" class="text-input-wrap">
        <input
          class="text-input"
          v-model="inputText"
          placeholder="请输入想问的问题..."
          confirm-type="send"
          :disabled="loading"
          @confirm="sendText()"
        />
      </view>

      <!-- 语音按钮 -->
      <view
        v-else
        class="voice-btn"
        :class="{ 'voice-active': recording }"
        @touchstart="startRecord"
        @touchend="stopRecord"
        @touchcancel="cancelRecord"
      >
        <text>{{ recording ? '松开发送' : '按住说话' }}</text>
      </view>

      <view
        v-if="!voiceMode"
        class="send-btn"
        :class="{ 'send-active': inputText.trim() && !loading }"
        @click="sendText()"
      >
        <text>发送</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { BASE_URL as AI_BASE_URL } from '@/config/env'

// ===== AI 相关 API 内联（避免 WeChat devtools 模块缓存问题） =====

const apiChat = (messages) => {
  const token = uni.getStorageSync('token')
  return new Promise((resolve, reject) => {
    uni.request({
      url: AI_BASE_URL + '/ai/chat',
      method: 'POST',
      data: { messages },
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          resolve(res.data)
        } else {
          reject(new Error((res.data && res.data.msg) || 'AI 对话失败'))
        }
      },
      fail: reject
    })
  })
}

const apiStt = (filePath, format = 'mp3') => {
  const token = uni.getStorageSync('token')
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: AI_BASE_URL + '/ai/stt',
      filePath,
      name: 'file',
      formData: { format, rate: 16000 },
      header: { 'Authorization': token ? `Bearer ${token}` : '' },
      success: (res) => {
        try {
          const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          if (data.code === 200) resolve(data.data)
          else reject(new Error(data.msg || '语音识别失败'))
        } catch (e) { reject(e) }
      },
      fail: reject
    })
  })
}

const apiTts = (text) => {
  const token = uni.getStorageSync('token')
  return new Promise((resolve, reject) => {
    uni.request({
      url: AI_BASE_URL + '/ai/tts',
      method: 'POST',
      data: { text },
      responseType: 'arraybuffer',
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        if (res.statusCode === 200) resolve(res.data)
        else reject(new Error('TTS 失败: ' + res.statusCode))
      },
      fail: reject
    })
  })
}

// 消息历史
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const scrollTop = ref(0)

// 语音模式
const voiceMode = ref(false)
const recording = ref(false)
const canceled = ref(false)

// 自动朗读 AI 回复（默认开启，并记忆到本地）
const autoTts = ref(uni.getStorageSync('ai_auto_tts') !== false)

// 连续对话模式（盲杖唤醒或手动开启后进入）
const conversationMode = ref(false)

// TTS 播放
const playingIdx = ref(-1)
let innerAudioContext = null

// 录音管理器（uni-app 原生）
let recorderManager = null

// 推荐快捷问题
const quickQuestions = ref([
  '盲杖怎么使用？',
  '遇到马路该怎么办？',
  '突然迷路了怎么办？',
  '我的身体不舒服'
])

onMounted(() => {
  initRecorder()
  // 检查是否由盲杖唤醒进入（App.vue 设置的全局 flag）
  // #ifdef MP-WEIXIN
  if (getApp().globalData && getApp().globalData.aiWakeTrigger) {
    getApp().globalData.aiWakeTrigger = false
    startConversation()
  }
  // #endif
})

onUnmounted(() => {
  stopAudio()
  if (recorderManager && recording.value) {
    try { recorderManager.stop() } catch (e) {}
  }
})

// 初始化录音管理器
const initRecorder = () => {
  recorderManager = uni.getRecorderManager()

  recorderManager.onStart(() => {
    console.log('开始录音')
  })

  recorderManager.onStop((res) => {
    recording.value = false
    if (canceled.value) {
      canceled.value = false
      return
    }
    if (!res || !res.tempFilePath) {
      uni.showToast({ title: '录音失败', icon: 'none' })
      return
    }
    // 时长过短过滤
    if (res.duration && res.duration < 800) {
      uni.showToast({ title: '说话时间太短', icon: 'none' })
      return
    }
    // 上传到后端识别
    recognizeAndSend(res.tempFilePath)
  })

  recorderManager.onError((err) => {
    recording.value = false
    console.error('录音错误', err)
    uni.showToast({ title: '录音失败: ' + (err.errMsg || ''), icon: 'none' })
  })
}

// 切换输入模式
const toggleMode = () => {
  voiceMode.value = !voiceMode.value
}

// ========= 连续对话模式（盲杖唤醒 / 长按全屏 触发）=========
// 语音播报一段提示并等待播放完成
const speak = (text) => {
  return new Promise(async (resolve) => {
    try {
      const arrayBuffer = await apiTts(text)
      // #ifdef MP-WEIXIN
      const fs = wx.getFileSystemManager()
      const filePath = `${wx.env.USER_DATA_PATH}/tts_prompt_${Date.now()}.mp3`
      fs.writeFileSync(filePath, arrayBuffer, 'binary')
      const ctx = uni.createInnerAudioContext()
      ctx.src = filePath
      ctx.onEnded(() => { try { ctx.destroy() } catch (e) {}; resolve() })
      ctx.onError(() => { try { ctx.destroy() } catch (e) {}; resolve() })
      ctx.play()
      // #endif
      // #ifndef MP-WEIXIN
      resolve()
      // #endif
    } catch (e) {
      console.warn('语音播报失败', e)
      resolve()
    }
  })
}

// 震动反馈（兼容不支持 type 参数的设备）
const vibrate = (long = false) => {
  try {
    if (long) {
      uni.vibrateLong({ fail: () => {} })
    } else {
      // 先尝试带 type，失败回落到无参数
      uni.vibrateShort({
        type: 'heavy',
        fail: () => {
          try { uni.vibrateShort({ fail: () => {} }) } catch (e) {}
        }
      })
    }
  } catch (e) {}
}

// 启动一次"连续对话"会话：播报欢迎语 → 自动开始录音
const startConversation = async () => {
  if (conversationMode.value) return
  conversationMode.value = true
  voiceMode.value = true
  autoTts.value = true
  uni.setStorageSync('ai_auto_tts', true)

  vibrate()
  await speak('我在，请说话')
  // 播报结束后自动开始录音
  startRecord({ autoStopMs: 8000 })
}

// 手动停止连续对话
const stopConversation = async () => {
  conversationMode.value = false
  if (recording.value) {
    canceled.value = true
    try { recorderManager.stop() } catch (e) {}
    recording.value = false
  }
  stopAudio()
  vibrate()
}

// 切换自动朗读
const toggleAutoTts = () => {
  autoTts.value = !autoTts.value
  uni.setStorageSync('ai_auto_tts', autoTts.value)
  if (!autoTts.value) {
    stopAudio()
  }
  uni.showToast({
    title: autoTts.value ? '已开启自动朗读' : '已关闭自动朗读',
    icon: 'none',
    duration: 1200
  })
}

// 录音自动停止计时器（连续对话模式用）
let autoStopTimer = null

// 开始录音
// options.autoStopMs: 若指定，录音 N 毫秒后自动停止（用于连续对话）
const startRecord = (options = {}) => {
  if (!recorderManager) return
  canceled.value = false
  uni.authorize({
    scope: 'scope.record',
    success: () => {
      recording.value = true
      recorderManager.start({
        format: 'mp3',
        sampleRate: 16000,
        numberOfChannels: 1,
        encodeBitRate: 48000,
        duration: 30000
      })
      // 连续对话模式：定时自动停止
      if (options.autoStopMs && options.autoStopMs > 0) {
        clearTimeout(autoStopTimer)
        autoStopTimer = setTimeout(() => {
          if (recording.value) {
            try { recorderManager.stop() } catch (e) {}
          }
        }, options.autoStopMs)
      }
    },
    fail: () => {
      uni.showModal({
        title: '权限提示',
        content: '请在设置中开启麦克风权限',
        confirmText: '去设置',
        success: (res) => {
          if (res.confirm) uni.openSetting()
        }
      })
    }
  })
}

// 停止录音
const stopRecord = () => {
  if (!recorderManager || !recording.value) return
  recorderManager.stop()
}

// 取消录音（上滑取消）
const cancelRecord = () => {
  if (!recorderManager || !recording.value) return
  canceled.value = true
  try { recorderManager.stop() } catch (e) {}
  recording.value = false
}

// 上传音频给后端识别，再把识别到的文字作为消息发送
const recognizeAndSend = async (filePath) => {
  uni.showLoading({ title: '识别中...', mask: true })
  try {
    const res = await apiStt(filePath, 'mp3')
    uni.hideLoading()
    const text = (res && res.text) ? res.text.trim() : ''
    if (!text) {
      uni.showToast({ title: '没听清，再试一次', icon: 'none' })
      // 连续对话模式下：没听清，再给一次机会
      if (conversationMode.value) {
        await speak('没听清，请再说一次')
        startRecord({ autoStopMs: 8000 })
      }
      return
    }
    sendText(text)
  } catch (e) {
    uni.hideLoading()
    console.error('识别失败', e)
    uni.showToast({ title: '语音识别失败', icon: 'none' })
    if (conversationMode.value) {
      await speak('语音识别失败')
      conversationMode.value = false
    }
  }
}

// 发送文字
const sendText = async (text) => {
  const content = (text || inputText.value || '').trim()
  if (!content || loading.value) return
  inputText.value = ''

  messages.value.push({ role: 'user', content })
  messages.value.push({ role: 'assistant', content: '', loading: true })
  scrollToBottom()

  loading.value = true
  try {
    const history = messages.value
      .filter(m => !m.loading && m.content)
      .map(m => ({ role: m.role, content: m.content }))

    const res = await apiChat(history)
    const answer = (res.data && res.data.content) ? res.data.content : '抱歉，我没听清。'

    const last = messages.value[messages.value.length - 1]
    last.content = answer
    last.loading = false
    scrollToBottom()

    // 自动朗读 AI 回复
    const answerIdx = messages.value.length - 1
    if (autoTts.value) {
      // 连续对话模式下，朗读完自动进入下一轮录音
      if (conversationMode.value) {
        await playTtsAndWait(answer)
        if (conversationMode.value) {
          await speak('请继续说话')
          startRecord({ autoStopMs: 8000 })
        }
      } else {
        playTts(answer, answerIdx)
      }
    }
  } catch (e) {
    console.error('AI 对话失败', e)
    const last = messages.value[messages.value.length - 1]
    last.content = '抱歉，我暂时无法回答，请稍后再试。'
    last.loading = false
    scrollToBottom()
    if (conversationMode.value) {
      await speak('网络不好，请稍后再试')
      conversationMode.value = false
    }
  } finally {
    loading.value = false
  }
}

// TTS 播放并等待结束（连续对话用）
const playTtsAndWait = (text) => {
  return new Promise(async (resolve) => {
    try {
      const arrayBuffer = await apiTts(text)
      // #ifdef MP-WEIXIN
      const fs = wx.getFileSystemManager()
      const filePath = `${wx.env.USER_DATA_PATH}/tts_ans_${Date.now()}.mp3`
      fs.writeFileSync(filePath, arrayBuffer, 'binary')
      stopAudio()
      innerAudioContext = uni.createInnerAudioContext()
      innerAudioContext.src = filePath
      innerAudioContext.onEnded(() => { playingIdx.value = -1; resolve() })
      innerAudioContext.onError(() => { playingIdx.value = -1; resolve() })
      innerAudioContext.play()
      // #endif
      // #ifndef MP-WEIXIN
      resolve()
      // #endif
    } catch (e) {
      console.error('TTS 失败', e)
      resolve()
    }
  })
}

// TTS 播放（调后端，获取 MP3 ArrayBuffer，写入临时文件后播放）
const playTts = async (text, idx) => {
  if (playingIdx.value === idx) {
    stopAudio()
    return
  }
  stopAudio()
  playingIdx.value = idx

  try {
    const arrayBuffer = await apiTts(text)
    // 写入本地临时文件
    // #ifdef MP-WEIXIN
    const fs = wx.getFileSystemManager()
    const filePath = `${wx.env.USER_DATA_PATH}/tts_${Date.now()}.mp3`
    fs.writeFileSync(filePath, arrayBuffer, 'binary')
    innerAudioContext = uni.createInnerAudioContext()
    innerAudioContext.src = filePath
    innerAudioContext.onEnded(() => { playingIdx.value = -1 })
    innerAudioContext.onError((err) => {
      console.error('播放错误', err)
      playingIdx.value = -1
    })
    innerAudioContext.play()
    // #endif
    // #ifndef MP-WEIXIN
    uni.showToast({ title: '仅微信小程序支持朗读', icon: 'none' })
    playingIdx.value = -1
    // #endif
  } catch (e) {
    console.error('TTS 失败', e)
    playingIdx.value = -1
    uni.showToast({ title: '朗读失败', icon: 'none' })
  }
}

const stopAudio = () => {
  if (innerAudioContext) {
    try {
      innerAudioContext.stop()
      innerAudioContext.destroy()
    } catch (e) {}
    innerAudioContext = null
  }
  playingIdx.value = -1
}

// 复制文字
const copyText = (text) => {
  uni.setClipboardData({
    data: text,
    success: () => uni.showToast({ title: '已复制', icon: 'success' })
  })
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    scrollTop.value = 9999999
    setTimeout(() => { scrollTop.value = 9999998 + Math.random() }, 50)
  })
}

const onScroll = () => {}
</script>

<style lang="scss" scoped>
.ai-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f7f8fa;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 24rpx 8rpx;
  background: #f7f8fa;
  gap: 16rpx;
}

.conv-btn {
  display: flex;
  align-items: center;
  padding: 14rpx 28rpx;
  border-radius: 40rpx;
  background: #07c160;
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  box-shadow: 0 4rpx 12rpx rgba(7, 193, 96, 0.3);
  transition: all 0.2s;

  &.active {
    background: #ff4d4f;
    box-shadow: 0 4rpx 12rpx rgba(255, 77, 79, 0.4);
    animation: pulse 1.2s ease-in-out infinite;
  }

  .conv-icon {
    margin-right: 10rpx;
    font-size: 32rpx;
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.04); }
}

.auto-tts {
  display: flex;
  align-items: center;
  padding: 8rpx 20rpx;
  border-radius: 32rpx;
  background: #e8e8e8;
  color: #666;
  font-size: 24rpx;
  transition: all 0.2s;

  &.on {
    background: #e8f7ee;
    color: #07c160;
  }

  .auto-tts-icon {
    margin-right: 8rpx;
    font-size: 28rpx;
  }
}

.msg-list {
  flex: 1;
  padding: 24rpx;
  box-sizing: border-box;
}

.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 48rpx;

  .avatar-lg {
    width: 140rpx;
    height: 140rpx;
    border-radius: 50%;
    background: linear-gradient(135deg, #07c160, #0ea678);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 80rpx;
    margin-bottom: 32rpx;
    box-shadow: 0 8rpx 24rpx rgba(7, 193, 96, 0.3);
  }
  .welcome-title {
    font-size: 36rpx;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 12rpx;
  }
  .welcome-sub {
    font-size: 26rpx;
    color: #6b7280;
    text-align: center;
    margin-bottom: 48rpx;
  }
  .quick-list {
    display: flex;
    flex-direction: column;
    gap: 16rpx;
    width: 100%;
  }
  .quick-item {
    padding: 24rpx 32rpx;
    background: #ffffff;
    border-radius: 16rpx;
    border: 1rpx solid #e5e7eb;
    font-size: 28rpx;
    color: #374151;
    text-align: center;
    box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);

    &:active {
      background: #f3f4f6;
    }
  }
}

.msg-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 32rpx;
  align-items: flex-start;

  &.row-user {
    flex-direction: row-reverse;
  }

  .avatar {
    width: 72rpx;
    height: 72rpx;
    border-radius: 50%;
    background: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 40rpx;
    flex-shrink: 0;
    box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
  }

  .bubble {
    max-width: 70%;
    padding: 20rpx 24rpx;
    border-radius: 20rpx;
    word-break: break-word;

    .bubble-text {
      font-size: 30rpx;
      line-height: 1.5;
      white-space: pre-wrap;
    }
  }

  .bubble-user {
    background: #07c160;
    color: #ffffff;
    border-top-right-radius: 4rpx;
  }

  .bubble-ai {
    background: #ffffff;
    color: #1f2937;
    border-top-left-radius: 4rpx;
    border: 1rpx solid #e5e7eb;
  }

  .bubble-actions {
    display: flex;
    gap: 16rpx;
    margin-top: 12rpx;
    padding-top: 12rpx;
    border-top: 1rpx solid #f3f4f6;

    .action-btn {
      font-size: 22rpx;
      color: #6b7280;
      padding: 4rpx 8rpx;

      &:active {
        color: #07c160;
      }
    }
  }
}

.bottom-spacer {
  height: 40rpx;
}

.input-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  background: #ffffff;
  border-top: 1rpx solid #e5e7eb;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));

  .mode-toggle {
    width: 72rpx;
    height: 72rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f3f4f6;
    border-radius: 50%;

    .icon {
      font-size: 36rpx;
    }
  }

  .text-input-wrap {
    flex: 1;
    background: #f3f4f6;
    border-radius: 36rpx;
    padding: 0 24rpx;

    .text-input {
      height: 72rpx;
      font-size: 28rpx;
      color: #1f2937;
    }
  }

  .voice-btn {
    flex: 1;
    height: 72rpx;
    background: #f3f4f6;
    border-radius: 36rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    color: #374151;

    &.voice-active {
      background: #07c160;
      color: #ffffff;
      transform: scale(0.98);
    }
  }

  .send-btn {
    padding: 0 32rpx;
    height: 72rpx;
    background: #e5e7eb;
    color: #9ca3af;
    border-radius: 36rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    font-weight: 600;

    &.send-active {
      background: #07c160;
      color: #ffffff;
    }
  }
}

.recording-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.recording-box {
  width: 400rpx;
  padding: 48rpx;
  background: rgba(40, 40, 40, 0.9);
  border-radius: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;

  .recording-wave {
    display: flex;
    align-items: flex-end;
    gap: 8rpx;
    height: 80rpx;

    .wave-bar {
      width: 8rpx;
      background: #07c160;
      border-radius: 4rpx;
      animation: wave 1s infinite ease-in-out;
      height: 40rpx;
    }
  }

  .recording-text {
    font-size: 28rpx;
    color: #ffffff;
  }
  .recording-hint {
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.6);
  }
}

@keyframes wave {
  0%, 100% { height: 16rpx; }
  50% { height: 60rpx; }
}
</style>
