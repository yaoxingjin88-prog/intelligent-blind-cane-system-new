<template>
  <view class="feedback-page">
    <scroll-view class="content" scroll-y>
      <!-- 反馈类型 -->
      <view class="card">
        <view class="card-title">反馈类型</view>
        <view class="type-list">
          <view 
            v-for="(type, index) in feedbackTypes" 
            :key="index" 
            class="type-item"
            :class="{ active: formData.type === type.value }"
            @click="selectType(type.value)"
          >
            <text class="type-text">{{ type.label }}</text>
            <text v-if="formData.type === type.value" class="type-check">✓</text>
          </view>
        </view>
      </view>

      <!-- 反馈内容 -->
      <view class="card">
        <view class="card-title">反馈内容</view>
        <textarea 
          v-model="formData.content" 
          class="feedback-input" 
          placeholder="请详细描述您的问题或建议（至少10个字）"
          maxlength="500"
          :show-confirm-bar="false"
        />
        <view class="char-count">{{ formData.content.length }}/500</view>
      </view>

      <!-- 联系方式 -->
      <view class="card">
        <view class="card-title">联系方式（选填）</view>
        <input 
          v-model="formData.contact" 
          class="contact-input" 
          placeholder="手机号或邮箱，方便我们联系您"
        />
      </view>

      <!-- 图片上传 -->
      <view class="card">
        <view class="card-title">图片（选填）</view>
        <view class="image-list">
          <view 
            v-for="(image, index) in formData.images" 
            :key="index" 
            class="image-item"
          >
            <image :src="image" class="image-preview" mode="aspectFill" />
            <view class="image-remove" @click="removeImage(index)">✕</view>
          </view>
          <view v-if="formData.images.length < 3" class="image-add" @click="chooseImage">
            <text class="add-icon">+</text>
            <text class="add-text">添加图片</text>
          </view>
        </view>
      </view>

      <!-- 提交按钮 -->
      <button 
        class="submit-btn" 
        :class="{ disabled: !canSubmit }"
        :disabled="!canSubmit"
        @click="submitFeedbackData"
      >
        {{ loading ? '提交中...' : '提交反馈' }}
      </button>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/store'
import { submitFeedback } from '@/api/feedback'

const userStore = useUserStore()

// 反馈类型
const feedbackTypes = [
  { label: '功能建议', value: 'suggestion' },
  { label: 'Bug反馈', value: 'bug' },
  { label: '使用问题', value: 'usage' },
  { label: '其他', value: 'other' }
]

// 表单数据
const formData = ref({
  type: 'suggestion',
  content: '',
  contact: '',
  images: []
})

// 加载状态
const loading = ref(false)

// 是否可以提交
const canSubmit = computed(() => {
  return formData.value.content.length >= 10 && !loading.value
})

// 选择反馈类型
const selectType = (type) => {
  formData.value.type = type
}

// 选择图片
const chooseImage = () => {
  uni.chooseImage({
    count: 3 - formData.value.images.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      formData.value.images.push(...res.tempFilePaths)
    }
  })
}

// 移除图片
const removeImage = (index) => {
  formData.value.images.splice(index, 1)
}

// 提交反馈
const submitFeedbackData = async () => {
  if (!formData.value.content || formData.value.content.length < 10) {
    uni.showToast({
      title: '请至少输入10个字',
      icon: 'none'
    })
    return
  }

  loading.value = true
  try {
    await submitFeedback({
      token: userStore.token,
      type: formData.value.type,
      content: formData.value.content,
      contact: formData.value.contact,
      images: formData.value.images.join(',')
    })
    
    uni.showToast({
      title: '提交成功，感谢您的反馈',
      icon: 'success'
    })
    
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (error) {
    console.error('提交反馈失败', error)
    uni.showToast({
      title: '提交失败，请重试',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.feedback-page {
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

.card {
  background: #ffffff;
  border-radius: 24rpx;
  margin-bottom: 24rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f3f4f6;
  width: 100%;
  box-sizing: border-box;

  .card-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 24rpx;
  }
}

.type-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;

  .type-item {
    width: calc(50% - 8rpx);
    box-sizing: border-box;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx;
    border: 2rpx solid #e5e7eb;
    border-radius: 12rpx;
    transition: all 0.3s;

    &.active {
      border-color: #07c160;
      background: rgba(7, 193, 96, 0.05);
    }

    .type-text {
      font-size: 28rpx;
      color: #1f2937;
    }

    .type-check {
      font-size: 32rpx;
      color: #07c160;
      font-weight: 600;
    }
  }
}

.feedback-input {
  width: 100%;
  min-height: 200rpx;
  padding: 24rpx;
  font-size: 28rpx;
  color: #1f2937;
  background: #f9fafb;
  border-radius: 12rpx;
  border: none;
  box-sizing: border-box;
  resize: none;
}

.char-count {
  text-align: right;
  font-size: 24rpx;
  color: #9ca3af;
  margin-top: 12rpx;
}

.contact-input {
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #1f2937;
  background: #f9fafb;
  border-radius: 12rpx;
  border: none;
  box-sizing: border-box;
}

.image-list {
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;

  .image-item {
    position: relative;
    width: 160rpx;
    height: 160rpx;

    .image-preview {
      width: 100%;
      height: 100%;
      border-radius: 12rpx;
    }

    .image-remove {
      position: absolute;
      top: -8rpx;
      right: -8rpx;
      width: 40rpx;
      height: 40rpx;
      background: #ee0a24;
      color: #ffffff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24rpx;
      font-weight: 600;
    }
  }

  .image-add {
    width: 160rpx;
    height: 160rpx;
    border: 2rpx dashed #d1d5db;
    border-radius: 12rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #f9fafb;

    .add-icon {
      font-size: 48rpx;
      color: #9ca3af;
    }

    .add-text {
      font-size: 20rpx;
      color: #9ca3af;
      margin-top: 8rpx;
    }
  }
}

.submit-btn {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #07c160 0%, #059669 100%);
  color: #ffffff;
  border-radius: 24rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
  margin-top: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(7, 193, 96, 0.3);
  box-sizing: border-box;

  &.disabled {
    background: #d1d5db;
    box-shadow: none;
  }
}
</style>
