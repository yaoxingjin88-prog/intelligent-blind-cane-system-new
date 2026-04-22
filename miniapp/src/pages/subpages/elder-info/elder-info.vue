<template>
  <view class="elder-info-page">
    <scroll-view class="content" scroll-y>
      <!-- 基本信息 -->
      <view class="card">
        <text class="card-title">盲人基本信息</text>
        <view class="form-item">
          <text class="label">姓名</text>
          <input v-model="elderInfo.name" class="input" placeholder="请输入姓名" />
        </view>
        <view class="form-item">
          <text class="label">年龄</text>
          <input v-model.number="elderInfo.age" type="number" class="input" placeholder="请输入年龄" />
        </view>
        <view class="form-item">
          <text class="label">性别</text>
          <picker mode="selector" :range="genders" @change="onGenderChange">
            <view class="picker-input">{{ elderInfo.gender }}</view>
          </picker>
        </view>
        <view class="form-item">
          <text class="label">血型</text>
          <picker mode="selector" :range="bloodTypes" @change="onBloodTypeChange">
            <view class="picker-input">{{ elderInfo.bloodType }}</view>
          </picker>
        </view>
      </view>

      <!-- 联系信息 -->
      <view class="card">
        <text class="card-title">联系与监护信息</text>
        <view class="form-item">
          <text class="label">联系电话</text>
          <input v-model="elderInfo.phone" type="number" class="input" placeholder="请输入联系电话" maxlength="11" />
        </view>
        <view class="form-item">
          <text class="label">紧急联系人</text>
          <input v-model="elderInfo.emergencyContact" class="input" placeholder="请输入紧急联系人" />
        </view>
        <view class="form-item">
          <text class="label">紧急联系电话</text>
          <input v-model="elderInfo.emergencyPhone" type="number" class="input" placeholder="请输入紧急联系电话" maxlength="11" />
        </view>
        <view class="form-item">
          <text class="label">家庭住址</text>
          <input v-model="elderInfo.address" class="input" placeholder="请输入家庭住址" />
        </view>
      </view>

      <!-- 健康信息 -->
      <view class="card">
        <text class="card-title">健康与注意事项</text>
        <view class="form-item">
          <text class="label">过往病史及注意事项</text>
          <textarea 
            v-model="elderInfo.medicalHistory" 
            class="textarea" 
            placeholder="例如：高血压、糖尿病、听力障碍等"
            maxlength="500"
          />
          <text class="char-count">{{ (elderInfo.medicalHistory || '').length }}/500</text>
        </view>
      </view>

      <!-- 保存按钮 -->
      <button class="save-btn" :disabled="saving" @click="handleSave">{{ saving ? '保存中...' : '保存信息' }}</button>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBlindProfile, updateBlindProfile } from '@/api/blind'

// 老人信息
const elderInfo = ref({
  name: '',
  age: '',
  gender: '男',
  bloodType: '未知',
  phone: '',
  emergencyContact: '',
  emergencyPhone: '',
  address: '',
  medicalHistory: ''
})

// 选项
const genders = ['男', '女']
const bloodTypes = ['A型', 'B型', 'AB型', 'O型', '未知']
const saving = ref(false)

const normalizeBlindProfile = (data = {}) => ({
  ...elderInfo.value,
  ...data,
  name: data.name ?? '',
  age: data.age ?? '',
  gender: data.gender ?? '男',
  bloodType: data.bloodType ?? '未知',
  phone: data.phone ?? '',
  emergencyContact: data.emergencyContact ?? '',
  emergencyPhone: data.emergencyPhone ?? '',
  address: data.address ?? '',
  medicalHistory: data.medicalHistory ?? ''
})

// 初始化
onMounted(async () => {
  try {
    const res = await getBlindProfile()
    if (res && res.data) {
      elderInfo.value = normalizeBlindProfile(res.data)
    }
  } catch (error) {
    console.error('加载盲人档案失败', error)
  }
})

// 性别选择
const onGenderChange = (e) => {
  elderInfo.value.gender = genders[e.detail.value]
}

// 血型选择
const onBloodTypeChange = (e) => {
  elderInfo.value.bloodType = bloodTypes[e.detail.value]
}

// 保存
const handleSave = async () => {
  if (!elderInfo.value.name) {
    uni.showToast({
      title: '请输入姓名',
      icon: 'none'
    })
    return
  }

  try {
    saving.value = true
    const payload = {
      ...elderInfo.value,
      age: elderInfo.value.age === '' ? null : Number(elderInfo.value.age)
    }
    const res = await updateBlindProfile(payload)
    if (res && res.data) {
      elderInfo.value = normalizeBlindProfile(res.data)
    }

    uni.showToast({
      title: '保存成功',
      icon: 'success'
    })

    setTimeout(() => {
      uni.navigateBack()
    }, 1200)
  } catch (error) {
    console.error('保存盲人档案失败', error)
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.elder-info-page {
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
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f3f4f6;
  width: 100%;
  box-sizing: border-box;

  .card-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #1f2937;
    display: block;
    margin-bottom: 32rpx;
  }

  .form-item {
    margin-bottom: 32rpx;
    position: relative;

    &:last-child {
      margin-bottom: 0;
    }

    .label {
      display: block;
      font-size: 28rpx;
      color: #374151;
      margin-bottom: 16rpx;
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

      &:focus {
        border-color: #07c160;
      }
    }

    .picker-input {
      width: 100%;
      height: 88rpx;
      background: #f9fafb;
      border-radius: 16rpx;
      padding: 0 24rpx;
      font-size: 28rpx;
      color: #1f2937;
      border: 2rpx solid #e5e7eb;
      display: flex;
      align-items: center;
      box-sizing: border-box;
    }

    .textarea {
      width: 100%;
      min-height: 200rpx;
      background: #f9fafb;
      border-radius: 16rpx;
      padding: 24rpx;
      font-size: 28rpx;
      color: #1f2937;
      border: 2rpx solid #e5e7eb;
      box-sizing: border-box;
      resize: none;
    }

    .char-count {
      position: absolute;
      bottom: 16rpx;
      right: 24rpx;
      font-size: 24rpx;
      color: #9ca3af;
    }
  }
}

.save-btn {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #07c160 0%, #05a050 100%);
  color: #ffffff;
  border-radius: 24rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
  margin-top: 24rpx;
}
</style>
