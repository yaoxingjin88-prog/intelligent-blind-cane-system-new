<template>
  <view class="alarm-page">
    <!-- 标签切换 -->
    <view class="tab-bar">
      <view 
        class="tab-item" 
        :class="{ active: activeTab === 'all' }"
        @click="switchTab('all')"
      >
        全部报警
      </view>
      <view class="tab-item" :class="{ active: activeTab === 'unread' }" @click="switchTab('unread')">
        未读 ({{ unreadCount }})
      </view>
      <view class="tab-item" :class="{ active: activeTab === 'handled' }" @click="switchTab('handled')">
        已处理
      </view>
    </view>

    <!-- 报警列表 -->
    <scroll-view class="alarm-list" scroll-y @scrolltolower="loadMore">
      <view v-if="filteredAlarms.length === 0" class="empty-state">
        <text class="empty-icon">📭</text>
        <text class="empty-text">{{ getEmptyText() }}</text>
      </view>

      <view v-for="alarm in filteredAlarms" :key="alarm.id" class="alarm-card" @click="navigateToDetail(alarm)">
        <view class="alarm-indicator" :class="alarm.level"></view>
        <view class="alarm-content">
          <!-- 第一行：标题和时间 -->
          <view class="alarm-header">
            <view class="alarm-title-row">
              <text class="alarm-icon">{{ getAlarmIcon(alarm.alarmType) }}</text>
              <text class="alarm-title">{{ getAlarmTitle(alarm.alarmType) }}</text>
            </view>
            <text class="alarm-time">{{ formatRelativeTime(alarm.alarmTime) }}</text>
          </view>
          
          <!-- 第二行：描述 -->
          <text class="alarm-desc">{{ alarm.description || getAlarmDesc(alarm.alarmType) }}</text>
          
          <!-- 第三行：操作按钮或状态 -->
          <view class="alarm-footer" v-if="alarm.status === 'pending'" @click.stop>
            <view class="alarm-actions">
              <button class="action-btn ignore" @click.stop="handleAlarm(alarm.id, 'ignored')">忽略</button>
              <button class="action-btn handle" @click.stop="handleAlarm(alarm.id, 'handled')">立即处理</button>
            </view>
          </view>
          
          <view class="alarm-footer" v-else>
            <text class="status-text" :class="alarm.status">
              {{ getStatusText(alarm.status) }}
            </text>
            <text v-if="alarm.handleTime" class="handle-time">{{ formatRelativeTime(alarm.handleTime) }}处理</text>
          </view>
        </view>
      </view>

      <view v-if="loading" class="loading-more">
        <text>加载中...</text>
      </view>
      
      <view v-if="!hasMore && filteredAlarms.length > 0" class="no-more">
        <text>没有更多了</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAlarmStore, useDeviceStore } from '@/store'
import { handleAlarm as handleAlarmApi, getAlarmList } from '@/api/alarm'
import { formatRelativeTime } from '@/utils'

const alarmStore = useAlarmStore()
const deviceStore = useDeviceStore()

// 当前标签
const activeTab = ref('all')
const loading = ref(false)
const hasMore = ref(true)
const page = ref(1)
const pageSize = 20

// 未读数量
const unreadCount = computed(() => {
  return alarms.value.filter(item => item.status === 'pending').length
})

// 报警列表
const alarms = ref([])

// 过滤后的报警列表
const filteredAlarms = computed(() => {
  if (activeTab.value === 'all') {
    return alarms.value
  } else if (activeTab.value === 'unread') {
    return alarms.value.filter(item => item.status === 'pending')
  } else if (activeTab.value === 'handled') {
    return alarms.value.filter(item => item.status === 'handled' || item.status === 'ignored')
  }
  return alarms.value
})

// 初始化
onMounted(() => {
  loadAlarms()
})

// 切换标签
const switchTab = (tab) => {
  activeTab.value = tab
  page.value = 1
  hasMore.value = true
  loadAlarms()
}

// 跳转到报警详情页
let isNavigating = false
const navigateToDetail = (alarm) => {
  if (isNavigating) return
  isNavigating = true
  
  // 将报警数据存储，供详情页读取
  try {
    uni.setStorageSync('currentAlarm', JSON.stringify(alarm))
  } catch (e) {
    console.error('存储报警数据失败', e)
  }
  
  uni.navigateTo({
    url: `/pages/subpages/alarm-detail/alarm-detail?id=${alarm.id}`,
    success: () => {
      isNavigating = false
    },
    fail: (err) => {
      console.error('跳转失败', err)
      isNavigating = false
      uni.showToast({
        title: '跳转失败，请重试',
        icon: 'none'
      })
    }
  })
}

// 加载报警列表
const loadAlarms = async () => {
  loading.value = true
  try {
    const res = await getAlarmList()
    const alarmData = res.data || []
    
    console.log('后端返回的报警数据:', alarmData)
    
    // 映射后端数据到前端格式
    const mappedAlarms = alarmData.map(item => {
      const type = item.alarmType || 'unknown'
      console.log('报警类型:', type)
      return {
        id: item.id,
        alarmType: type,
        description: getAlarmDesc(type),
        alarmTime: item.alarmTime,
        status: item.status === '0' ? 'pending' : 'handled',
        level: getAlarmLevel(type),
        deviceId: item.deviceId
      }
    })
    
    if (page.value === 1) {
      alarms.value = mappedAlarms
    } else {
      alarms.value.push(...mappedAlarms)
    }
    hasMore.value = mappedAlarms.length >= pageSize
  } catch (error) {
    console.error('加载报警列表失败', error)
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = () => {
  if (!loading.value && hasMore.value) {
    page.value++
    loadAlarms()
  }
}

// 处理报警
const handleAlarm = async (alarmId, status) => {
  try {
    // 后端使用 '1' 表示已处理，'0' 表示待处理
    const backendStatus = status === 'handled' || status === 'ignored' ? '1' : '0'
    await handleAlarmApi(alarmId, { status: backendStatus })
    
    // 更新本地状态
    const alarm = alarms.value.find(item => item.id === alarmId)
    if (alarm) {
      alarm.status = status
      alarm.handleTime = new Date().toISOString()
      alarm.handler = (deviceStore.currentDevice && deviceStore.currentDevice.userName) ? deviceStore.currentDevice.userName : '当前用户'
    }
    
    // 更新store
    alarmStore.updateAlarmStatus(alarmId, status)
    
    uni.showToast({
      title: status === 'handled' ? '已标记为已处理' : '已忽略',
      icon: 'success'
    })
  } catch (error) {
    console.error('处理报警失败', error)
    uni.showToast({
      title: '处理失败',
      icon: 'none'
    })
  }
}

// 报警类型映射（支持英文和中文）
const alarmTypeMap = {
  // 英文key
  fall: { title: '跌倒报警', icon: '🚨', desc: '设备检测到老人可能发生跌倒，请立即确认！' },
  out_of_bounds: { title: '越界提醒', icon: '📍', desc: '设备已离开电子围栏区域。' },
  low_battery: { title: '低电量报警', icon: '🔋', desc: '当前设备电量过低，请提醒充电。' },
  stationary: { title: '长时间静止', icon: '⏰', desc: '设备已连续静止超过设定时间。' },
  obstacle: { title: '障碍物报警', icon: '⚠️', desc: '检测到前方障碍物距离过近。' },
  sos: { title: 'SOS求助', icon: '🔔', desc: '老人触发SOS紧急求助。' },
  // 中文key（后端返回）
  '摔倒': { title: '跌倒报警', icon: '🚨', desc: '设备检测到老人可能发生跌倒，请立即确认！' },
  '跌倒': { title: '跌倒报警', icon: '🚨', desc: '设备检测到老人可能发生跌倒，请立即确认！' },
  '障碍物': { title: '障碍物报警', icon: '⚠️', desc: '检测到前方障碍物距离过近。' },
  '电池': { title: '低电量报警', icon: '🔋', desc: '当前设备电量过低，请提醒充电。' },
  '低电量': { title: '低电量报警', icon: '🔋', desc: '当前设备电量过低，请提醒充电。' },
  '电子围栏越界报警': { title: '越界提醒', icon: '📍', desc: '设备已离开电子围栏区域。' },
  '越界': { title: '越界提醒', icon: '📍', desc: '设备已离开电子围栏区域。' },
  '静止': { title: '长时间静止', icon: '⏰', desc: '设备已连续静止超过设定时间。' },
  'sos': { title: 'SOS求助', icon: '🔔', desc: '老人触发SOS紧急求助。' }
}

// 获取报警图标
const getAlarmIcon = (type) => {
  const info = alarmTypeMap[type]
  return info ? info.icon : '📢'
}

// 获取报警标题
const getAlarmTitle = (type) => {
  const info = alarmTypeMap[type]
  return info ? info.title : (type || '未知报警')
}

// 获取报警描述
const getAlarmDesc = (type) => {
  const info = alarmTypeMap[type]
  return info ? info.desc : ('报警类型: ' + (type || '未知'))
}

// 获取状态文字
const getStatusText = (status) => {
  const texts = {
    pending: '待处理',
    handled: '已处理',
    ignored: '已忽略'
  }
  return texts[status] || '未知'
}

// 获取报警等级（支持中英文类型）
const getAlarmLevel = (type) => {
  const highLevelTypes = ['fall', 'sos', '摔倒', '跌倒', 'sos']
  const mediumLevelTypes = ['out_of_bounds', 'obstacle', '障碍物', '越界', '电子围栏越界报警']
  
  if (highLevelTypes.includes(type)) return 'high'
  if (mediumLevelTypes.includes(type)) return 'medium'
  return 'low'
}

// 获取空状态文字
const getEmptyText = () => {
  if (activeTab.value === 'unread') return '暂无未读报警'
  if (activeTab.value === 'handled') return '暂无已处理报警'
  return '暂无报警记录'
}
</script>

<style lang="scss" scoped>
.alarm-page {
  min-height: 100vh;
  background: #f7f8fa;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.tab-bar {
  background: #ffffff;
  display: flex;
  border-bottom: 1rpx solid #f3f4f6;
  position: sticky;
  top: 0;
  z-index: 99;

  .tab-item {
    flex: 1;
    text-align: center;
    padding: 24rpx 0;
    font-size: 28rpx;
    color: #6b7280;
    position: relative;

    &.active {
      color: #07c160;
      font-weight: 600;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 80rpx;
        height: 4rpx;
        background: #07c160;
        border-radius: 2rpx;
      }
    }
  }
}

.alarm-list {
  flex: 1;
  padding: 32rpx;
  width: 100%;
  box-sizing: border-box;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;

  .empty-icon {
    font-size: 120rpx;
    margin-bottom: 24rpx;
    opacity: 0.5;
  }

  .empty-text {
    font-size: 28rpx;
    color: #9ca3af;
  }
}

.alarm-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f3f4f6;
  position: relative;
  overflow: hidden;
  width: 100%;
  box-sizing: border-box;

  .alarm-indicator {
    position: absolute;
    top: 0;
    left: 0;
    width: 6rpx;
    height: 100%;
    border-radius: 3rpx;

    &.high {
      background: linear-gradient(180deg, #ff4d4f 0%, #ff7875 100%);
      box-shadow: 2rpx 0 8rpx rgba(255, 77, 79, 0.3);
    }

    &.medium {
      background: linear-gradient(180deg, #ff7a45 0%, #ffa940 100%);
      box-shadow: 2rpx 0 8rpx rgba(255, 122, 69, 0.3);
    }

    &.low {
      background: linear-gradient(180deg, #8c8c8c 0%, #bfbfbf 100%);
    }
  }

  .alarm-content {
    padding-left: 16rpx;
  }

  .alarm-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12rpx;

    .alarm-title-row {
      display: flex;
      align-items: center;
      gap: 12rpx;

      .alarm-icon {
        font-size: 32rpx;
      }

      .alarm-title {
        font-size: 30rpx;
        font-weight: 600;
        color: #1f2937;
      }
    }

    .alarm-time {
      font-size: 24rpx;
      color: #9ca3af;
    }
  }

  .alarm-desc {
    font-size: 26rpx;
    color: #6b7280;
    line-height: 1.5;
    margin-bottom: 20rpx;
    display: block;
  }

  .alarm-footer {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    min-height: 64rpx;

    .alarm-actions {
      display: flex;
      gap: 16rpx;

      .action-btn {
        height: 56rpx;
        padding: 0 32rpx;
        border-radius: 28rpx;
        font-size: 26rpx;
        font-weight: 500;
        border: none;
        line-height: 56rpx;

        &.ignore {
          background: #ffffff;
          color: #6b7280;
          border: 1rpx solid #e5e7eb;
        }

        &.handle {
          background: #07c160;
          color: #ffffff;
        }
      }
    }

    .status-text {
      font-size: 24rpx;
      font-weight: 500;

      &.pending {
        color: #ee0a24;
      }

      &.handled {
        color: #07c160;
      }

      &.ignored {
        color: #9ca3af;
      }
    }

    .handle-time {
      font-size: 24rpx;
      color: #9ca3af;
    }
  }
}

.loading-more,
.no-more {
  text-align: center;
  padding: 32rpx 0;
  font-size: 24rpx;
  color: #9ca3af;
}
</style>
