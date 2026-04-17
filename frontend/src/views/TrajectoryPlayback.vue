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
              <p class="header-subtitle">地图会展示起点、终点与完整轨迹折线</p>
            </div>
            <el-tag type="primary" effect="plain">{{ trajectory.length }} 个定位点</el-tag>
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
          <div v-for="point in visiblePoints" :key="point.id || point.createTime" class="point-item" @click="focusPoint(point)">
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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const mapContainerRef = ref(null)
const devices = ref([])
const selectedDeviceId = ref(route.query.deviceId ? String(route.query.deviceId) : '')
const selectedHours = ref(1)
const trajectory = ref([])
const playbackVisible = ref(true)

let map = null
let markers = []
let polyline = null

const statCards = computed(() => {
  const start = trajectory.value[0]
  const end = trajectory.value[trajectory.value.length - 1]
  const fallCount = trajectory.value.filter(item => item.isFall).length
  return [
    { label: '轨迹点数', value: trajectory.value.length, sub: '当前时间范围内的位置采样数' },
    { label: '起点时间', value: start?.createTime || '-', sub: '轨迹第一条记录时间' },
    { label: '终点时间', value: end?.createTime || '-', sub: '轨迹最后一条记录时间' },
    { label: '异常节点', value: fallCount, sub: '跌倒识别节点数量' }
  ]
})

const visiblePoints = computed(() => [...trajectory.value].slice().reverse().slice(0, 40))

const formatCoordinate = (lng, lat) => {
  if (lng == null || lat == null) return '-'
  return `${Number(lng).toFixed(5)}, ${Number(lat).toFixed(5)}`
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
}

const renderTrajectory = () => {
  if (!map || !window.AMap) return
  clearMapOverlays()
  if (!playbackVisible.value || !trajectory.value.length) return

  const path = trajectory.value
    .filter(item => item.longitude != null && item.latitude != null)
    .map(item => [Number(item.longitude), Number(item.latitude)])

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
}

const initMap = () => {
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
    const response = await axios.get('/api/sensor-data/trajectory', {
      params: {
        deviceId: selectedDeviceId.value,
        hours: selectedHours.value,
        _t: Date.now()
      }
    })
    if (response.data.code === 200) {
      trajectory.value = response.data.data || []
      renderTrajectory()
    }
  } catch (error) {
    console.error('获取轨迹失败', error)
    ElMessage.error('加载轨迹失败')
  }
}

const focusPoint = (point) => {
  if (!map || point.longitude == null || point.latitude == null) return
  map.setCenter([Number(point.longitude), Number(point.latitude)])
  map.setZoom(16)
}

watch(playbackVisible, () => {
  renderTrajectory()
})

onMounted(async () => {
  initMap()
  await fetchDevices()
  if (selectedDeviceId.value) {
    loadTrajectory()
  }
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

  .map-container,
  .point-list {
    height: auto;
    max-height: none;
    min-height: 420px;
  }
}
</style>
