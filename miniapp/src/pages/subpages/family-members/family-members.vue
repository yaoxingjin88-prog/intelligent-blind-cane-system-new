<template>
  <view class="family-page">
    <view class="role-banner" :class="isPrimary ? 'primary' : 'member'">
      <text class="role-title">{{ isPrimary ? '主监护人' : '子监护人' }}</text>
      <text class="role-desc">{{ isPrimary ? '可绑定设备、邀请/移除家庭成员' : '可监视盲杖，设备绑定由主监护人管理' }}</text>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">家庭成员</text>
        <text class="section-count">{{ members.length }} 人</text>
      </view>

      <view v-for="item in members" :key="item.id" class="member-card">
        <view class="member-main">
          <text class="member-name">{{ item.name || '未命名' }}</text>
          <text class="member-role" :class="item.role === 'PRIMARY' ? 'primary' : ''">
            {{ item.role === 'PRIMARY' ? '主监护人' : '子监护人' }}
          </text>
        </view>
        <text class="member-meta">{{ item.relation || '家属' }} · {{ item.phone }}</text>
        <view v-if="isPrimary && item.role !== 'PRIMARY'" class="member-actions">
          <text class="remove-btn" @click="onRemove(item)">移除</text>
        </view>
      </view>

      <view v-if="!members.length" class="empty">暂无成员</view>
    </view>

    <view v-if="isPrimary" class="invite-card">
      <text class="invite-title">邀请子监护人</text>
      <input v-model="form.phone" class="input" type="number" maxlength="11" placeholder="手机号" />
      <input v-model="form.name" class="input" placeholder="姓名（可选）" />
      <input v-model="form.relation" class="input" placeholder="关系，如子女/配偶" />
      <button class="invite-btn" :loading="submitting" @click="onInvite">发送邀请</button>
      <text class="invite-hint">对方可用该手机号登录小程序，验证码 123456（开发环境）</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getFamilyMembers, inviteFamilyMember, removeFamilyMember } from '@/api/family'
import { useUserStore } from '@/store'

const userStore = useUserStore()
const members = ref([])
const me = ref(null)
const submitting = ref(false)
const form = ref({ phone: '', name: '', relation: '' })

const isPrimary = computed(() => me.value?.role === 'PRIMARY' || me.value?.primary === true)

const load = async () => {
  const res = await getFamilyMembers()
  const data = res.data || {}
  me.value = data.me || data
  members.value = data.members || []
  if (data.me) {
    userStore.setUserInfo({ ...userStore.userInfo, ...data.me, role: data.me.role })
  }
}

const onInvite = async () => {
  if (!form.value.phone || form.value.phone.length < 11) {
    uni.showToast({ title: '请输入正确手机号', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await inviteFamilyMember({
      phone: form.value.phone,
      name: form.value.name,
      relation: form.value.relation
    })
    uni.showToast({ title: '邀请成功', icon: 'success' })
    form.value = { phone: '', name: '', relation: '' }
    await load()
  } catch (e) {
    // toast 已由 api 处理
  } finally {
    submitting.value = false
  }
}

const onRemove = (item) => {
  uni.showModal({
    title: '移除成员',
    content: `确定移除 ${item.name || item.phone}？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await removeFamilyMember(item.id)
        uni.showToast({ title: '已移除', icon: 'success' })
        await load()
      } catch (e) {
        // ignore
      }
    }
  })
}

onMounted(load)
</script>

<style lang="scss" scoped>
.family-page {
  min-height: 100vh;
  padding: 24rpx;
  background: #f7f8fa;
  box-sizing: border-box;
}

.role-banner {
  padding: 28rpx;
  border-radius: 20rpx;
  margin-bottom: 24rpx;
  &.primary {
    background: linear-gradient(135deg, #0f766e, #115e59);
  }
  &.member {
    background: linear-gradient(135deg, #475569, #334155);
  }
}

.role-title {
  display: block;
  color: #fff;
  font-size: 34rpx;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.role-desc {
  color: rgba(255, 255, 255, 0.82);
  font-size: 24rpx;
  line-height: 1.5;
}

.section {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
}

.section-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #0f172a;
}

.section-count {
  font-size: 24rpx;
  color: #94a3b8;
}

.member-card {
  padding: 20rpx 0;
  border-top: 1rpx solid #f1f5f9;
}

.member-main {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 6rpx;
}

.member-name {
  font-size: 28rpx;
  color: #0f172a;
  font-weight: 500;
}

.member-role {
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 999rpx;
  background: #f1f5f9;
  color: #64748b;
  &.primary {
    background: #ccfbf1;
    color: #0f766e;
  }
}

.member-meta {
  font-size: 24rpx;
  color: #94a3b8;
}

.member-actions {
  margin-top: 12rpx;
}

.remove-btn {
  font-size: 24rpx;
  color: #dc2626;
}

.empty {
  padding: 40rpx 0;
  text-align: center;
  color: #cbd5e1;
  font-size: 26rpx;
}

.invite-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
}

.invite-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 20rpx;
}

.input {
  height: 80rpx;
  background: #f8fafc;
  border-radius: 12rpx;
  padding: 0 24rpx;
  margin-bottom: 16rpx;
  font-size: 28rpx;
}

.invite-btn {
  margin-top: 8rpx;
  background: #0f766e;
  color: #fff;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.invite-hint {
  display: block;
  margin-top: 16rpx;
  font-size: 22rpx;
  color: #94a3b8;
  line-height: 1.5;
}
</style>
