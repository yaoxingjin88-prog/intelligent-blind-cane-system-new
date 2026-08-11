<template>
  <div class="trajectory-page">
    <div class="page-hero">
      <div>
        <h2>轨迹回放</h2>
        <p>选择设备和时间范围，查看历史移动轨迹与关键定位点</p>
      </div>
      <div class="hero-actions">
        <el-button @click="loadTrajectory">加载轨迹</el-button>
        <el-button type="primary" @click="playbackVisible = !playbackVisible">
          {{ playbackVisible ? '隐藏轨迹' : '显示轨迹' }}
        </el-button>
      </div>
    </div>

    <el-card class="control-card">
      <div class="controls-grid">
        <el-select v-model="selectedDeviceId" filterable placeholder="请选择设备" class="control-item">
          <el-option
            v-for="device in devices"
            :key="device.id"
            :label="`${device.deviceName || device.deviceId}（${device.deviceId}）`"
            :value="device.deviceId"
          />
        </el-select>
        <el-select v-model="selectedHours" class="control-item">
          <el-option label="最近 1 小时" :value="1" />
          <el-option label="最近 3 小时" :value="3" />
          <el-option label="最近 6 小时" :value="6" />
          <el-option label="最近 12 小时" :value="12" />
          <el-option label="最近 24 小时" :value="24" />
        </el-select>
        <el-button type="primary" @click="loadTrajectory">查询轨迹</el-button>
      </div>
      <div v-if="trajectoryPath.length" class="playback-toolbar">
        <div class="playback-actions">
          <el-button type="primary" @click="togglePlayback">{{ isPlaying ? '暂停回放' : '开始回放' }}</el-button>
          <el-button @click="replayPlayback">重新播放</el-button>
          <el-select v-model="playbackSpeed" class="speed-select">
            <el-option label="慢速" :value="1800" />
            <el-option label="标准" :value="1200" />
            <el-option label="快速" :value="700" />
            <el-option label="极速" :value="350" />
          </el-select>
        </div>
        <div class="playback-slider">
          <span class="playback-label">回放进度</span>
          <el-slider v-model="currentPlaybackIndex" :min="0" :max="Math.max(trajectoryPath.length - 1, 0)" :show-tooltip="false" />
          <span class="playback-meta">{{ currentPlaybackIndex + 1 }} / {{ trajectoryPath.length }}</span>
        </div>
      </div>
    </el-card>

    <div class="stats-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <span class="stat-label">{{ card.label }}</span>
        <strong class="stat-value">{{ card.value }}</strong>
        <span class="stat-sub">{{ card.sub }}</span>
      </div>
    </div>

    <div class="content-grid">
      <el-card class="panel map-panel">
        <template #header>
          <div class="card-header">
            <div>
              <span>轨迹地图</span>
              <p class="header-subtitle">地图会展示起点、终点、回放点与完整轨迹折线</p>
            </div>
            <div class="map-header-tags">
              <el-tag type="primary" effect="plain">{{ trajectoryPath.length }} 个定位点</el-tag>
              <el-tag v-if="activePoint" type="success" effect="light">当前：{{ activePoint.createTime || '-' }}</el-tag>
            </div>
          </div>
        </template>
        <div ref="mapContainerRef" class="map-container"></div>
      </el-card>

      <el-card class="panel point-panel">
        <template #header>
          <div class="card-header">
            <div>
              <span>轨迹节点</span>
              <p class="header-subtitle">按时间倒序展示关键定位数据</p>
            </div>
          </div>
        </template>
        <div class="point-list">
          <div
            v-for="point in visiblePoints"
            :key="point.id || point.createTime"
            class="point-item"
            :class="{ 'point-item--active': isActivePoint(point) }"
            @click="focusPoint(point)"
          >
            <strong>{{ point.createTime || '-' }}</strong>
            <span>{{ formatCoordinate(point.longitude, point.latitude) }}</span>
            <span>障碍距离 {{ point.obstacleDistance ?? '-' }} cm</span>
            <el-tag size="small" :type="point.isFall ? 'danger' : 'success'" effect="light">
              {{ point.isFall ? '疑似跌倒' : '正常' }}
            </el-tag>
          </div>
          <el-empty v-if="!visiblePoints.length" description="暂无轨迹数据" :image-size="72" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { ensureAmap } from '../utils/amap'

defineOptions({ name: 'TrajectoryPlayback' })

const route = useRoute()
const mapContainerRef = ref(null)
const devices = ref([])
const selectedDeviceId = ref(route.query.deviceId ? String(route.query.deviceId) : '')
const selectedHours = ref(1)
const trajectory = ref([])
const playbackVisible = ref(true)
const playbackSpeed = ref(1200)
const currentPlaybackIndex = ref(0)
const isPlaying = ref(false)

let map = null
let markers = []
let polyline = null
let playedPolyline = null
let playbackMarker = null
let playbackTimer = null

const trajectoryPath = computed(() => trajectory.value.filter(item => item.longitude != null && item.latitude != null))
const activePoint = computed(() => trajectoryPath.value[currentPlaybackIndex.value] || null)

const statCards = computed(() => {
  const start = trajectoryPath.value[0]
  const end = trajectoryPath.value[trajectoryPath.value.length - 1]
  const fallCount = trajectory.value.filter(item => item.isFall).length
  return [
    { label: '轨迹点数', value: trajectoryPath.value.length, sub: '当前时间范围内的位置采样数' },
    { label: '起点时间', value: start?.createTime || '-', sub: '轨迹第一条记录时间' },
    { label: '终点时间', value: end?.createTime || '-', sub: '轨迹最后一条记录时间' },
    { label: '异常节点', value: fallCount, sub: '跌倒识别节点数量' }
  ]
})

const visiblePoints = computed(() => [...trajectoryPath.value].slice().reverse().slice(0, 40))

const formatCoordinate = (lng, lat) => {
  if (lng == null || lat == null) return '-'
  return `${Number(lng).toFixed(5)}, ${Number(lat).toFixed(5)}`
}

const clearPlaybackTimer = () => {
  if (playbackTimer) {
    window.clearInterval(playbackTimer)
    playbackTimer = null
  }
}

const pausePlayback = () => {
  isPlaying.value = false
  clearPlaybackTimer()
}

const clearMapOverlays = () => {
  if (!map) return
  if (markers.length) {
    map.remove(markers)
    markers = []
  }
  if (polyline) {
    map.remove(polyline)
    polyline = null
  }
  if (playedPolyline) {
    map.remove(playedPolyline)
    playedPolyline = null
  }
  if (playbackMarker) {
    map.remove(playbackMarker)
    playbackMarker = null
  }
}

const updatePlaybackOverlay = (centerMap = false) => {
  if (!map || !window.AMap || !playbackVisible.value || !trajectoryPath.value.length) return
  const point = activePoint.value || trajectoryPath.value[0]
  if (!point) return
  const position = [Number(point.longitude), Number(point.latitude)]

  if (!playedPolyline) {
    playedPolyline = new window.AMap.Polyline({
      path: [],
      strokeColor: '#22c55e',
      strokeWeight: 6,
      strokeOpacity: 0.95,
      zIndex: 30
    })
    map.add(playedPolyline)
  }

  playedPolyline.setPath(
    trajectoryPath.value
      .slice(0, currentPlaybackIndex.value + 1)
      .map(item => [Number(item.longitude), Number(item.latitude)])
  )

  if (!playbackMarker) {
    playbackMarker = new window.AMap.Marker({
      position,
      offset: new window.AMap.Pixel(-12, -12),
      content: '<div class="trajectory-playback-marker"></div>'
    })
    map.add(playbackMarker)
  } else {
    playbackMarker.setPosition(position)
  }

  playbackMarker.setLabel({
    content: point.createTime || '当前点',
    offset: new window.AMap.Pixel(0, -26)
  })

  if (centerMap || isPlaying.value) {
    map.setCenter(position)
  }
}

const restartPlaybackTimer = () => {
  clearPlaybackTimer()
  if (!isPlaying.value || trajectoryPath.value.length <= 1) return
  playbackTimer = window.setInterval(() => {
    if (currentPlaybackIndex.value >= trajectoryPath.value.length - 1) {
      pausePlayback()
      return
    }
    currentPlaybackIndex.value += 1
  }, playbackSpeed.value)
}

const renderTrajectory = () => {
  if (!map || !window.AMap) return
  clearMapOverlays()
  if (!playbackVisible.value || !trajectoryPath.value.length) return

  const path = trajectoryPath.value.map(item => [Number(item.longitude), Number(item.latitude)])

  if (!path.length) return

  polyline = new window.AMap.Polyline({
    path,
    strokeColor: '#2563eb',
    strokeWeight: 4,
    strokeOpacity: 0.85,
    showDir: true
  })
  map.add(polyline)

  const startMarker = new window.AMap.Marker({
    position: path[0],
    label: { content: '起点', offset: new window.AMap.Pixel(0, -24) }
  })
  const endMarker = new window.AMap.Marker({
    position: path[path.length - 1],
    label: { content: '终点', offset: new window.AMap.Pixel(0, -24) }
  })
  markers = [startMarker, endMarker]
  map.add(markers)
  map.setFitView([polyline, ...markers], false, [60, 60, 60, 60])
  updatePlaybackOverlay(true)
}

const initMap = async () => {
  try {
    await ensureAmap()
  } catch (error) {
    console.error(error)
    ElMessage.error('高德地图加载失败')
    return
  }
  if (!window.AMap || !mapContainerRef.value || map) return
  map = new window.AMap.Map(mapContainerRef.value, {
    zoom: 13,
    center: [116.4074, 39.9042],
    viewMode: '2D'
  })
  map.addControl(new window.AMap.Scale())
  map.addControl(new window.AMap.ToolBar())
}

const fetchDevices = async () => {
  try {
    const response = await axios.get('/api/devices', { params: { _t: Date.now() } })
    if (response.data.code === 200) {
      devices.value = response.data.data || []
      if (!selectedDeviceId.value && devices.value.length) {
        selectedDeviceId.value = devices.value[0].deviceId
      }
    }
  } catch (error) {
    console.error('获取设备失败', error)
    devices.value = []
  }
}

const loadTrajectory = async () => {
  if (!selectedDeviceId.value) {
    ElMessage.warning('请先选择设备')
    return
  }
  try {
    pausePlayback()
    const response = await axios.get('/api/sensor-data/trajectory', {
      params: {
        deviceId: selectedDeviceId.value,
        hours: selectedHours.value,
        _t: Date.now()
      }
    })
    if (response.data.code === 200) {
      trajectory.value = response.data.data || []
      currentPlaybackIndex.value = 0
      renderTrajectory()
    }
  } catch (error) {
    console.error('获取轨迹失败', error)
    ElMessage.error('加载轨迹失败')
  }
}

const togglePlayback = () => {
  if (!trajectoryPath.value.length) {
    ElMessage.warning('请先加载轨迹数据')
    return
  }
  if (currentPlaybackIndex.value >= trajectoryPath.value.length - 1) {
    currentPlaybackIndex.value = 0
  }
  isPlaying.value = !isPlaying.value
  restartPlaybackTimer()
}

const replayPlayback = () => {
  if (!trajectoryPath.value.length) {
    ElMessage.warning('请先加载轨迹数据')
    return
  }
  currentPlaybackIndex.value = 0
  isPlaying.value = true
  restartPlaybackTimer()
}

const isActivePoint = (point) => {
  if (!activePoint.value) return false
  return point.id
    ? point.id === activePoint.value.id
    : point.createTime === activePoint.value.createTime
}

const focusPoint = (point) => {
  if (!map || point.longitude == null || point.latitude == null) return
  const index = trajectoryPath.value.findIndex(item => item.id ? item.id === point.id : item.createTime === point.createTime)
  if (index >= 0) {
    currentPlaybackIndex.value = index
  }
  pausePlayback()
  map.setCenter([Number(point.longitude), Number(point.latitude)])
  map.setZoom(16)
}

watch(playbackVisible, () => {
  pausePlayback()
  renderTrajectory()
})

watch(currentPlaybackIndex, () => {
  updatePlaybackOverlay(true)
})

watch(playbackSpeed, () => {
  if (isPlaying.value) {
    restartPlaybackTimer()
  }
})

onMounted(async () => {
  await initMap()
  await fetchDevices()
  if (selectedDeviceId.value) {
    loadTrajectory()
  }
})

onUnmounted(() => {
  clearPlaybackTimer()
})
</script>

<style scoped>
.trajectory-page {
  min-height: 100%;
  padding: 24px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef4ff 100%);
}

.page-hero,
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-hero h2 {
  margin: 0 0 6px;
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}

.page-hero p,
.header-subtitle {
  margin: 0;
  color: #64748b;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.map-header-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.control-card,
.panel,
.stat-card {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.96);
}

.controls-grid,
.stats-grid {
  display: grid;
  gap: 16px;
}

.controls-grid {
  grid-template-columns: 1.4fr 0.9fr auto;
}

.control-item {
  width: 100%;
}

.playback-toolbar {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 20px;
  align-items: center;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #e2e8f0;
}

.playback-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.speed-select {
  width: 120px;
}

.playback-slider {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: center;
}

.playback-label,
.playback-meta {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
}

.stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px 20px;
}

.stat-label {
  color: #64748b;
  font-size: 13px;
}

.stat-value {
  font-size: 26px;
  color: #0f172a;
}

.stat-sub {
  font-size: 12px;
  color: #94a3b8;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.45fr 0.95fr;
  gap: 20px;
}

.map-container {
  height: 620px;
  border-radius: 16px;
  overflow: hidden;
}

.point-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 620px;
  overflow-y: auto;
}

.point-item {
  padding: 14px;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.point-item:hover {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.point-item--active {
  border-color: #60a5fa;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.12);
}

.point-item strong {
  color: #0f172a;
}

.point-item span {
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .playback-toolbar {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .trajectory-page {
    padding: 16px;
  }

  .page-hero,
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions,
  .controls-grid,
  .stats-grid {
    grid-template-columns: 1fr;
    width: 100%;
  }

  .playback-actions,
  .playback-slider {
    grid-template-columns: 1fr;
    width: 100%;
  }

  .map-container,
  .point-list {
    height: auto;
    max-height: none;
    min-height: 420px;
  }
}

:deep(.trajectory-playback-marker) {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  background: linear-gradient(135deg, #2563eb 0%, #22c55e 100%);
  border: 3px solid #ffffff;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.28);
}
</style>
