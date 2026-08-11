<template>
  <div class="monitor-center-page">
    <div class="page-hero">
      <div>
        <h2>实时监控中心</h2>
        <p>集中查看多设备位置、在线状态、未处理告警与测试运行情况</p>
      </div>
      <el-button type="primary" @click="refreshOverview">
        <el-icon><Refresh /></el-icon>
        刷新监控
      </el-button>
    </div>

    <div class="stats-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <span class="stat-label">{{ card.label }}</span>
        <strong class="stat-value">{{ card.value }}</strong>
        <span class="stat-sub">{{ card.sub }}</span>
      </div>
    </div>

    <el-card class="filter-card">
      <div class="filters-row">
        <el-input v-model="keyword" placeholder="搜索设备名 / 设备ID / 用户名" clearable class="filter-item" />
        <el-select v-model="statusFilter" class="filter-item">
          <el-option label="全部状态" value="all" />
          <el-option label="仅在线" value="online" />
          <el-option label="仅离线" value="offline" />
        </el-select>
        <el-select v-model="alertFilter" class="filter-item">
          <el-option label="全部告警状态" value="all" />
          <el-option label="仅未处理告警" value="unhandled" />
          <el-option label="仅正常设备" value="normal" />
        </el-select>
        <el-select v-model="testingFilter" class="filter-item">
          <el-option label="全部测试状态" value="all" />
          <el-option label="仅测试中" value="running" />
          <el-option label="仅未测试" value="stopped" />
        </el-select>
        <el-button @click="resetFilters">重置筛选</el-button>
      </div>
    </el-card>

    <div class="content-grid">
      <el-card class="panel panel-map">
        <template #header>
          <div class="panel-header">
            <div>
              <span>设备分布地图</span>
              <p class="panel-subtitle">地图显示有位置数据的设备，红色优先表示存在未处理告警</p>
            </div>
            <el-tag type="primary" effect="plain">{{ filteredPositionedDevices.length }} 台已定位</el-tag>
          </div>
        </template>
        <div class="map-wrapper">
          <div ref="mapContainerRef" class="map-container"></div>
          <div class="map-tip">点击地图点位可查看详情，点击“轨迹回放”可进入独立回放页</div>
          <div v-if="selectedDevice" class="map-detail-card">
            <div class="map-detail__header">
              <div>
                <strong>{{ selectedDevice.deviceName || selectedDevice.deviceId }}</strong>
                <p>{{ selectedDevice.userName || '未关联用户' }} · {{ selectedDevice.deviceId }}</p>
              </div>
              <el-tag :type="getDeviceStateTag(selectedDevice)" effect="light">
                {{ getDeviceStateLabel(selectedDevice) }}
              </el-tag>
            </div>
            <div class="map-detail__meta">
              <span>电量 {{ selectedDevice.batteryLevel ?? '-' }}%</span>
              <span>{{ formatDistance(selectedDevice.latestData?.obstacleDistance) }}</span>
              <span>更新时间 {{ selectedDevice.latestData?.createTime || '-' }}</span>
              <span>最新告警 {{ getAlarmBrief(selectedDevice) }}</span>
            </div>
            <div class="map-detail__actions">
              <el-button size="small" @click="goToDeviceMonitor(selectedDevice)">设备详情</el-button>
              <el-button size="small" type="primary" @click="goToTrajectoryPlayback(selectedDevice)">轨迹回放</el-button>
            </div>
          </div>
        </div>
      </el-card>

      <div class="side-stack">
        <el-card class="panel side-panel">
          <template #header>
            <div class="panel-header">
              <div>
                <span>设备实时状态</span>
                <p class="panel-subtitle">按最新数据更新时间和风险优先级展示</p>
              </div>
            </div>
          </template>
          <div class="device-list">
            <div
              v-for="device in filteredSortedDevices"
              :key="device.id"
              class="device-item"
              @click="selectDevice(device)"
            >
              <div class="device-item__header">
                <div>
                  <strong>{{ device.deviceName || device.deviceId }}</strong>
                  <p>{{ device.userName || '未关联用户' }} · {{ device.deviceId }}</p>
                </div>
                <el-tag :type="getDeviceStateTag(device)" effect="light">
                  {{ getDeviceStateLabel(device) }}
                </el-tag>
              </div>
              <div class="device-item__meta">
                <span>电量 {{ device.batteryLevel ?? '-' }}%</span>
                <span>测试 {{ device.testing ? '运行中' : '未启动' }}</span>
                <span>更新时间 {{ device.latestData?.createTime || '-' }}</span>
              </div>
              <div class="device-item__footer">
                <el-tag size="small" :type="getAlarmTagType(device.latestAlarm?.alarmType)" effect="plain">
                  {{ getAlarmBrief(device) }}
                </el-tag>
                <div class="device-item__actions">
                  <span class="device-distance">{{ formatDistance(device.latestData?.obstacleDistance) }}</span>
                  <el-button text size="small" @click.stop="goToDeviceMonitor(device)">详情</el-button>
                  <el-button text size="small" type="primary" @click.stop="goToTrajectoryPlayback(device)">轨迹</el-button>
                </div>
              </div>
            </div>
            <el-empty v-if="!filteredSortedDevices.length" description="暂无符合筛选条件的设备" :image-size="72" />
          </div>
        </el-card>

        <el-card class="panel side-panel">
          <template #header>
            <div class="panel-header">
              <div>
                <span>最新未处理告警</span>
                <p class="panel-subtitle">点击告警可跳转到对应设备详情页</p>
              </div>
              <el-tag type="danger" effect="light">{{ unhandledAlarms.length }} 条</el-tag>
            </div>
          </template>
          <div class="alarm-list">
            <div
              v-for="alarm in filteredUnhandledAlarms.slice(0, 6)"
              :key="alarm.id"
              class="alarm-item"
              @click="goToAlarmDevice(alarm)"
            >
              <div class="alarm-item__main">
                <span class="alarm-device">{{ alarm.deviceId }}</span>
                <el-tag :type="getAlarmTagType(alarm.alarmType)" size="small" effect="light">
                  {{ alarm.alarmType || '未知告警' }}
                </el-tag>
              </div>
              <span class="alarm-time">{{ alarm.alarmTime || '-' }}</span>
            </div>
            <el-empty v-if="!filteredUnhandledAlarms.length" description="当前筛选下暂无未处理告警" :image-size="72" />
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onActivated, onDeactivated, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import axios from '../utils/request'
import { ensureAmap } from '../utils/amap'

defineOptions({ name: 'MonitoringCenter' })

const router = useRouter()
const mapContainerRef = ref(null)
const devices = ref([])
const alarms = ref([])
const runningTestingIds = ref([])
const keyword = ref('')
const statusFilter = ref('all')
const alertFilter = ref('all')
const testingFilter = ref('all')
const selectedDeviceId = ref(null)

let map = null
let mapMarkers = []
let overviewTimer = null

const isUnhandled = (status) => String(status) === '0' || status === '未处理'

const unhandledAlarms = computed(() => alarms.value
  .filter(alarm => isUnhandled(alarm.status))
  .sort((a, b) => String(b.alarmTime || '').localeCompare(String(a.alarmTime || ''))))

const filteredDevices = computed(() => devices.value.filter((device) => {
  const keywordValue = keyword.value.trim().toLowerCase()
  const matchesKeyword = !keywordValue || [device.deviceName, device.deviceId, device.userName]
    .some(value => String(value || '').toLowerCase().includes(keywordValue))
  const online = Boolean(device.latestData)
  const hasUnhandledAlarm = Boolean(device.latestAlarm && isUnhandled(device.latestAlarm.status))
  const matchesStatus = statusFilter.value === 'all'
    || (statusFilter.value === 'online' && online)
    || (statusFilter.value === 'offline' && !online)
  const matchesAlert = alertFilter.value === 'all'
    || (alertFilter.value === 'unhandled' && hasUnhandledAlarm)
    || (alertFilter.value === 'normal' && !hasUnhandledAlarm)
  const matchesTesting = testingFilter.value === 'all'
    || (testingFilter.value === 'running' && device.testing)
    || (testingFilter.value === 'stopped' && !device.testing)

  return matchesKeyword && matchesStatus && matchesAlert && matchesTesting
}))

const filteredPositionedDevices = computed(() => filteredDevices.value.filter(device => device.latestData?.longitude && device.latestData?.latitude))

const filteredSortedDevices = computed(() => [...filteredDevices.value].sort((a, b) => {
  const aAlert = isUnhandled(a.latestAlarm?.status) ? 1 : 0
  const bAlert = isUnhandled(b.latestAlarm?.status) ? 1 : 0
  if (aAlert !== bAlert) return bAlert - aAlert
  return String(b.latestData?.createTime || '').localeCompare(String(a.latestData?.createTime || ''))
}))

const filteredUnhandledAlarms = computed(() => {
  const visibleIds = new Set(filteredDevices.value.map(device => device.deviceId))
  return unhandledAlarms.value.filter(alarm => visibleIds.has(alarm.deviceId))
})

const selectedDevice = computed(() => devices.value.find(device => device.id === selectedDeviceId.value) || null)

const statCards = computed(() => {
  const onlineCount = devices.value.filter(device => device.latestData).length
  const fallRiskCount = devices.value.filter(device => Boolean(device.latestData?.isFall)).length
  return [
    { label: '设备总数', value: devices.value.length, sub: '当前纳入监控的设备数量' },
    { label: '在线设备', value: onlineCount, sub: '已获取到最新位置数据的设备' },
    { label: '未处理告警', value: unhandledAlarms.value.length, sub: '需要立即关注的告警事件' },
    { label: '测试中设备', value: runningTestingIds.value.length, sub: `跌倒风险设备 ${fallRiskCount} 台` }
  ]
})

const formatDistance = (distance) => {
  const value = Number(distance)
  return Number.isNaN(value) ? '障碍距离 -' : `障碍 ${value} cm`
}

const getAlarmTagType = (alarmType) => {
  if (!alarmType) return 'info'
  if (String(alarmType).includes('跌倒')) return 'danger'
  if (String(alarmType).includes('低电')) return 'warning'
  if (String(alarmType).includes('围栏')) return 'warning'
  return 'info'
}

const getAlarmBrief = (device) => {
  if (device.latestAlarm && isUnhandled(device.latestAlarm.status)) {
    return device.latestAlarm.alarmType || '有未处理告警'
  }
  return '暂无未处理告警'
}

const getDeviceStateLabel = (device) => {
  if (device.latestAlarm && isUnhandled(device.latestAlarm.status)) return '告警中'
  if (device.latestData) return '在线'
  return '离线'
}

const getDeviceStateTag = (device) => {
  if (device.latestAlarm && isUnhandled(device.latestAlarm.status)) return 'danger'
  if (device.latestData) return 'success'
  return 'info'
}

const selectDevice = (device) => {
  selectedDeviceId.value = device.id
  if (map && device.latestData?.longitude && device.latestData?.latitude) {
    map.setCenter([Number(device.latestData.longitude), Number(device.latestData.latitude)])
    map.setZoom(15)
  }
}

const resetFilters = () => {
  keyword.value = ''
  statusFilter.value = 'all'
  alertFilter.value = 'all'
  testingFilter.value = 'all'
}

const goToDeviceMonitor = (device) => {
  router.push(`/monitor/${device.id}`)
}

const goToTrajectoryPlayback = (device) => {
  router.push({
    path: '/trajectory-playback',
    query: {
      deviceId: device.deviceId
    }
  })
}

const goToAlarmDevice = (alarm) => {
  const targetDevice = devices.value.find(device => device.deviceId === alarm.deviceId)
  if (targetDevice?.id) {
    selectDevice(targetDevice)
    return
  }
  router.push('/alarm-records')
}

const createMarkerContent = (device) => {
  const wrapper = document.createElement('div')
  wrapper.className = 'map-marker'
  if (device.latestAlarm && isUnhandled(device.latestAlarm.status)) {
    wrapper.classList.add('map-marker--alert')
  }

  const badge = document.createElement('div')
  badge.className = 'map-marker__dot'

  const label = document.createElement('div')
  label.className = 'map-marker__label'
  label.textContent = device.deviceName || device.deviceId

  wrapper.appendChild(badge)
  wrapper.appendChild(label)
  return wrapper
}

const clearMarkers = () => {
  if (!map || !mapMarkers.length) return
  map.remove(mapMarkers)
  mapMarkers = []
}

const renderMarkers = () => {
  if (!map || !window.AMap) return
  clearMarkers()
  mapMarkers = filteredPositionedDevices.value.map(device => {
    const marker = new window.AMap.Marker({
      position: [Number(device.latestData.longitude), Number(device.latestData.latitude)],
      anchor: 'bottom-center',
      content: createMarkerContent(device)
    })
    marker.on('click', () => selectDevice(device))
    return marker
  })
  if (mapMarkers.length) {
    map.add(mapMarkers)
    map.setFitView(mapMarkers, false, [80, 80, 80, 80])
  }
}

const initMap = async () => {
  await ensureAmap()
  if (!window.AMap || !mapContainerRef.value || map) return
  map = new window.AMap.Map(mapContainerRef.value, {
    zoom: 12,
    center: [116.4074, 39.9042],
    viewMode: '2D'
  })
  map.addControl(new window.AMap.Scale())
  map.addControl(new window.AMap.ToolBar())
  renderMarkers()
}

let overviewRequestId = 0
let overviewAbortController = null

const refreshOverview = async () => {
  const requestId = ++overviewRequestId
  if (overviewAbortController) {
    overviewAbortController.abort()
  }
  overviewAbortController = new AbortController()

  try {
    const response = await axios.get('/api/monitor/overview', {
      params: { _t: Date.now() },
      signal: overviewAbortController.signal
    })
    if (requestId !== overviewRequestId) return
    if (response.data.code !== 200) {
      ElMessage.error(response.data.msg || '加载实时监控中心失败')
      return
    }

    const payload = response.data.data || {}
    const enhanced = Array.isArray(payload.devices) ? payload.devices : []
    devices.value = enhanced
    alarms.value = Array.isArray(payload.alarms) ? payload.alarms : []
    runningTestingIds.value = Array.isArray(payload.testingDeviceIds) ? payload.testingDeviceIds : []

    if (!selectedDeviceId.value || !enhanced.some(device => device.id === selectedDeviceId.value)) {
      selectedDeviceId.value = enhanced[0]?.id ?? null
    }
    renderMarkers()
  } catch (error) {
    if (axios.isCancel?.(error) || error?.code === 'ERR_CANCELED' || error?.name === 'CanceledError') {
      return
    }
    console.error('获取监控中心设备失败', error)
    ElMessage.error('加载实时监控中心失败')
  }
}

onMounted(async () => {
  await initMap()
  await refreshOverview()
  overviewTimer = window.setInterval(refreshOverview, 30000)
})

onActivated(async () => {
  if (!map) {
    await initMap()
  }
  await refreshOverview()
  if (!overviewTimer) {
    overviewTimer = window.setInterval(refreshOverview, 30000)
  }
})

onDeactivated(() => {
  if (overviewTimer) {
    window.clearInterval(overviewTimer)
    overviewTimer = null
  }
})

watch(filteredSortedDevices, (value) => {
  if (!value.length) {
    selectedDeviceId.value = null
    renderMarkers()
    return
  }
  if (!value.some(device => device.id === selectedDeviceId.value)) {
    selectedDeviceId.value = value[0].id
  }
  renderMarkers()
})

onUnmounted(() => {
  overviewRequestId += 1
  if (overviewAbortController) {
    overviewAbortController.abort()
    overviewAbortController = null
  }
  if (overviewTimer) {
    window.clearInterval(overviewTimer)
    overviewTimer = null
  }
  clearMarkers()
  if (map) {
    map.destroy()
    map = null
  }
})
</script>

<style scoped>
.monitor-center-page {
  min-height: 100%;
  padding: 24px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef4ff 100%);
}

.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.page-hero h2 {
  margin: 0 0 6px;
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}

.page-hero p {
  margin: 0;
  color: #64748b;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
}

.stat-label {
  color: #64748b;
  font-size: 13px;
}

.stat-value {
  font-size: 28px;
  color: #0f172a;
}

.stat-sub {
  font-size: 12px;
  color: #94a3b8;
}

.filter-card {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
}

.filters-row {
  display: grid;
  grid-template-columns: 1.4fr repeat(3, minmax(0, 0.85fr)) auto;
  gap: 12px;
  align-items: center;
}

.filter-item {
  width: 100%;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.45fr 1fr;
  gap: 20px;
  align-items: start;
}

.side-stack {
  display: grid;
  gap: 20px;
}

.panel {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
  background-color: rgba(255, 255, 255, 0.96);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.panel-subtitle {
  margin: 6px 0 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
}

.map-wrapper {
  position: relative;
}

.map-container {
  height: 620px;
  border-radius: 16px;
  overflow: hidden;
}

.map-tip {
  position: absolute;
  right: 16px;
  top: 16px;
  z-index: 10;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #2563eb;
  font-size: 12px;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.12);
}

.map-detail-card {
  position: absolute;
  left: 16px;
  bottom: 16px;
  z-index: 10;
  width: min(340px, calc(100% - 32px));
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(12px);
}

.map-detail__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.map-detail__header strong {
  color: #0f172a;
  font-size: 15px;
}

.map-detail__header p,
.map-detail__meta span {
  font-size: 12px;
  color: #64748b;
}

.map-detail__meta {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.map-detail__actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.device-list,
.alarm-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 420px;
  overflow-y: auto;
}

.device-item,
.alarm-item {
  padding: 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.device-item:hover,
.alarm-item:hover {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.device-item__header,
.alarm-item__main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.device-item__header strong,
.alarm-device {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}

.device-item__header p,
.device-item__meta,
.alarm-time,
.device-distance {
  font-size: 12px;
  color: #64748b;
}

.device-item__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.device-item__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
}

.device-item__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

:global(.map-marker) {
  display: flex;
  align-items: center;
  gap: 8px;
  transform: translate(-10px, -36px);
}

:global(.map-marker__dot) {
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: #2563eb;
  border: 3px solid #ffffff;
  box-shadow: 0 8px 16px rgba(37, 99, 235, 0.25);
}

:global(.map-marker__label) {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #dbeafe;
  color: #0f172a;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
  white-space: nowrap;
}

:global(.map-marker--alert .map-marker__dot) {
  background: #ef4444;
  box-shadow: 0 8px 16px rgba(239, 68, 68, 0.28);
}

:global(.map-marker--alert .map-marker__label) {
  border-color: rgba(239, 68, 68, 0.2);
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filters-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .map-container {
    height: 520px;
  }
}

@media (max-width: 768px) {
  .monitor-center-page {
    padding: 16px;
  }

  .page-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .filters-row,
  .map-detail__actions {
    grid-template-columns: 1fr;
    display: grid;
  }

  .device-item__header,
  .alarm-item__main,
  .device-item__footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .map-tip {
    left: 16px;
    right: 16px;
    top: 16px;
  }

  .map-detail-card {
    left: 16px;
    right: 16px;
    width: auto;
  }
}
</style>
