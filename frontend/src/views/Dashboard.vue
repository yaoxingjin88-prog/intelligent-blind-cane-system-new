<template>
  <div class="dashboard-page">
    <div class="hero">
      <div>
        <h2>智能盲杖数据看板</h2>
        <p>面向管理端的风险热力、设备健康与活跃趋势总览</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadDashboard">刷新看板</el-button>
    </div>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="warning"
      show-icon
      :closable="false"
      class="load-error-alert"
    >
      <template #default>
        <el-button type="primary" size="small" :loading="loading" @click="loadDashboard">重新加载</el-button>
      </template>
    </el-alert>

    <div v-loading="loading" class="dashboard-body stats-section">
    <div class="stats-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <p class="stat-label">{{ card.label }}</p>
        <h3 class="stat-value">{{ card.value }}</h3>
        <span class="stat-sub">{{ card.sub }}</span>
      </div>
    </div>
    </div>

    <div v-loading="chartsLoading" class="dashboard-body charts-section">
    <!-- 主栅格：左热力 | 右三图等分；下排双表对齐，避免高低不一留白 -->
    <div class="dashboard-main">
      <el-card class="panel panel--map">
        <template #header>
          <div class="panel-header">
            <div>
              <span>风险区域热力图</span>
              <p class="panel-desc">颜色越红风险越集中；地图标注 Top 热点。</p>
            </div>
            <el-tag type="danger">{{ validHeatmapPoints.length }} 个热点</el-tag>
          </div>
        </template>
        <div class="heatmap-legend">
          <span class="legend-label">风险强度</span>
          <div class="legend-bar"></div>
          <div class="legend-ticks">
            <span>低</span>
            <span>中</span>
            <span>高</span>
          </div>
        </div>
        <div class="heatmap-wrapper">
          <div ref="mapContainer" class="heatmap"></div>
          <div class="map-highlight-tip">Top {{ topHotspots.length }} 热点</div>
        </div>
        <div class="hotspot-list">
          <div v-for="(point, index) in topHotspots" :key="`${point.lng}-${point.lat}`" class="hotspot-item">
            <div>
              <strong>TOP {{ index + 1 }} · {{ point.deviceName || point.deviceId }}</strong>
              <p>{{ point.lng }}, {{ point.lat }}</p>
            </div>
            <div class="hotspot-meta">
              <span>风险 {{ point.count ?? point.value ?? 0 }} 次</span>
              <span>最近障碍 {{ point.minObstacleDistance ?? '-' }} cm</span>
            </div>
          </div>
          <div v-if="!topHotspots.length" class="hotspot-empty">暂无热点数据</div>
        </div>
      </el-card>

      <el-card class="panel panel--activity">
        <template #header>
          <div class="panel-header"><span>活跃时长趋势</span></div>
        </template>
        <div ref="activityChartRef" class="chart"></div>
      </el-card>

      <el-card class="panel panel--battery">
        <template #header>
          <div class="panel-header"><span>设备电量概况</span></div>
        </template>
        <div ref="batteryChartRef" class="chart"></div>
      </el-card>

      <el-card class="panel panel--alarm">
        <template #header>
          <div class="panel-header"><span>报警分布</span></div>
        </template>
        <div ref="alarmChartRef" class="chart"></div>
      </el-card>

      <el-card class="panel panel--ranking">
        <template #header>
          <div class="panel-header"><span>设备报警排行</span></div>
        </template>
        <el-table :data="deviceRanking" size="small" stripe class="panel-table">
          <el-table-column prop="deviceName" label="设备" min-width="120" />
          <el-table-column prop="alarmCount" label="报警总数" width="90" />
          <el-table-column prop="unhandledCount" label="未处理" width="80" />
          <el-table-column prop="latestAlarmTime" label="最近报警时间" min-width="150" />
        </el-table>
      </el-card>

      <el-card class="panel panel--health">
        <template #header>
          <div class="panel-header"><span>设备健康度面板</span></div>
        </template>
        <el-table :data="deviceHealth" size="small" stripe class="panel-table">
          <el-table-column prop="deviceName" label="设备" min-width="120" />
          <el-table-column label="健康度" width="90">
            <template #default="scope">
              <el-tag :type="getHealthTagType(scope.row.healthScore)">{{ scope.row.healthScore }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="batteryLevel" label="电量" width="70" />
          <el-table-column prop="status" label="状态" width="80" />
          <el-table-column prop="latestDataTime" label="最近数据时间" min-width="150" />
          <el-table-column prop="unhandledCount" label="待处理" width="80" />
        </el-table>
      </el-card>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onActivated, onDeactivated, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ensureAmap } from '../utils/amap'
import { fetchJson } from '../utils/http'
// 看板走 fetchJson（自带 502 重试）；图表/地图异步渲染

defineOptions({ name: 'Dashboard' })

type EchartsModule = typeof import('echarts')

const overview = ref<any>({})
const activityTrend = ref<any[]>([])
const batteryTrend = ref<any[]>([])
const alarmDistribution = ref<any[]>([])
const deviceRanking = ref<any[]>([])
const deviceHealth = ref<any[]>([])
const heatmapPoints = ref<any[]>([])
const loading = ref(false)
const chartsLoading = ref(false)
const loadError = ref('')

const mapContainer = ref<HTMLElement | null>(null)
const activityChartRef = ref<HTMLElement | null>(null)
const batteryChartRef = ref<HTMLElement | null>(null)
const alarmChartRef = ref<HTMLElement | null>(null)

let map: any = null
let heatmap: any = null
let activityChart: any = null
let batteryChart: any = null
let alarmChart: any = null
let echartsLib: EchartsModule | null = null
let hotspotMarkers: any[] = []
let dashboardRequestId = 0
let retryTimer: ReturnType<typeof setTimeout> | null = null
let lastLoadedAt = 0
let stopDashboardPoll: (() => void) | null = null
const DASHBOARD_STALE_MS = 10_000

const ensureEcharts = async () => {
  if (!echartsLib) {
    echartsLib = await import('echarts')
  }
  return echartsLib
}

const statCards = computed(() => [
  { label: '设备总数', value: overview.value.deviceCount ?? 0, sub: `在线 ${overview.value.onlineDevices ?? 0} 台` },
  { label: '待处理报警', value: overview.value.unhandledAlarms ?? 0, sub: `累计报警 ${overview.value.alarmCount ?? 0} 条` },
  { label: '风险事件', value: overview.value.riskEvents ?? 0, sub: '障碍物高风险点' },
  { label: '活跃设备', value: overview.value.activeDevicesToday ?? 0, sub: '今日上传数据的设备' },
  { label: '低电量设备', value: overview.value.lowBatteryDevices ?? 0, sub: '电量 ≤ 20%' },
  { label: '传感器样本', value: overview.value.sensorCount ?? 0, sub: '当前保留数据总量' }
])

// 青岛农业大学
const DEFAULT_MAP_CENTER: [number, number] = [120.397571, 36.321143]

const isValidMapPoint = (point: any) => {
  const lng = Number(point?.lng)
  const lat = Number(point?.lat)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return false
  if (Math.abs(lng) < 0.01 && Math.abs(lat) < 0.01) return false
  return lat >= 3 && lat <= 54 && lng >= 73 && lng <= 136
}

const validHeatmapPoints = computed(() => heatmapPoints.value.filter(isValidMapPoint))

const topHotspots = computed(() => [...validHeatmapPoints.value]
  .sort((a, b) => (b.count ?? b.value ?? 0) - (a.count ?? a.value ?? 0))
  .slice(0, 5))

const getHealthTagType = (score: number) => {
  if (score >= 85) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

const renderCharts = async () => {
  chartsLoading.value = true
  try {
    const echarts = await ensureEcharts()
    await nextTick()
    await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
    initCharts(echarts)
    updateCharts()
    activityChart?.resize()
    batteryChart?.resize()
    alarmChart?.resize()
    // 地图最重，放到图表之后异步加载
    window.setTimeout(() => {
      initMap().then(() => updateHeatmap())
    }, 0)
  } finally {
    chartsLoading.value = false
  }
}

const loadDashboard = async (options: { silent?: boolean } = {}) => {
  const requestId = ++dashboardRequestId
  loading.value = true
  loadError.value = ''
  // 地图与接口并行预热，避免数据到了还要再等高德 SDK
  void ensureAmap().catch(() => {})

  try {
    const result = await fetchJson('/api/analytics/dashboard', {
      retries: 5,
      retryDelay: 300,
      timeout: 20000
    })
    if (requestId !== dashboardRequestId) return

    const data = result?.data || {}
    overview.value = data.overview || {}
    activityTrend.value = data.activityTrend || []
    batteryTrend.value = data.batteryTrend || []
    alarmDistribution.value = data.alarmDistribution || []
    deviceRanking.value = data.deviceRanking || []
    deviceHealth.value = data.deviceHealth || []
    heatmapPoints.value = data.heatmapPoints || []
    lastLoadedAt = Date.now()
    // 数字先出来，立刻结束 stats loading；图表/地图异步补齐
    loading.value = false
    void renderCharts()
  } catch (error) {
    if (requestId !== dashboardRequestId) return
    console.error('加载看板失败', error)
    loadError.value = '看板数据加载失败，网关繁忙时会自动重试'
    if (!options.silent) {
      ElMessage.warning('看板加载较慢，正在自动重试…')
    }
    // 重试期间保持 loading，避免先显示一排 0 再跳变
    scheduleDashboardRetry()
  } finally {
    if (requestId === dashboardRequestId && !retryTimer) {
      loading.value = false
    }
  }
}

const scheduleDashboardRetry = () => {
  if (retryTimer) return
  loading.value = true
  retryTimer = window.setTimeout(async () => {
    retryTimer = null
    await loadDashboard({ silent: true })
  }, 1200)
}

const initCharts = (echarts: EchartsModule) => {
  if (activityChartRef.value && !activityChart) activityChart = echarts.init(activityChartRef.value)
  if (batteryChartRef.value && !batteryChart) batteryChart = echarts.init(batteryChartRef.value)
  if (alarmChartRef.value && !alarmChart) alarmChart = echarts.init(alarmChartRef.value)
}

const updateCharts = () => {
  activityChart?.setOption({
    tooltip: { trigger: 'axis', confine: true },
    legend: {
      data: ['活跃时长', '活跃设备'],
      top: 0,
      right: 0,
      itemWidth: 14,
      itemHeight: 10,
      textStyle: { fontSize: 12 }
    },
    grid: { left: 44, right: 28, top: 56, bottom: 42 },
    xAxis: {
      type: 'category',
      data: activityTrend.value.map(item => item.date?.slice(5) || '-'),
      axisLabel: { margin: 12 }
    },
    yAxis: [
      { type: 'value', name: '分钟', nameGap: 16 },
      { type: 'value', name: '设备数', nameGap: 16 }
    ],
    series: [
      { name: '活跃时长', type: 'bar', data: activityTrend.value.map(item => item.activeMinutes || 0), itemStyle: { color: '#3b82f6' } },
      { name: '活跃设备', type: 'line', yAxisIndex: 1, smooth: true, data: activityTrend.value.map(item => item.activeDevices || 0), itemStyle: { color: '#22c55e' } }
    ]
  })

  batteryChart?.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 40, right: 20, top: 20, bottom: 80 },
    xAxis: { type: 'category', data: batteryTrend.value.map(item => item.deviceName || item.deviceId), axisLabel: { rotate: 25 } },
    yAxis: { type: 'value', max: 100 },
    series: [{ type: 'bar', data: batteryTrend.value.map(item => item.batteryLevel || 0), itemStyle: { color: '#f59e0b' } }]
  })

  alarmChart?.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['35%', '68%'],
      data: alarmDistribution.value.length ? alarmDistribution.value : [{ name: '暂无报警', value: 1 }],
      label: { formatter: '{b}\n{c}' }
    }]
  })
}

const initMap = async () => {
  await ensureAmap()
  if (!window.AMap || !mapContainer.value || map) return
  map = new window.AMap.Map(mapContainer.value, {
    zoom: 12,
    center: DEFAULT_MAP_CENTER,
    viewMode: '2D'
  })
  map.addControl(new window.AMap.Scale())
  map.addControl(new window.AMap.ToolBar())
  window.AMap.plugin(['AMap.Heatmap'], () => {
    if (!map) return
    heatmap = new window.AMap.Heatmap(map, {
      radius: 28,
      opacity: [0, 0.85],
      gradient: {
        0.4: '#3b82f6',
        0.65: '#22c55e',
        0.85: '#f59e0b',
        1: '#ef4444'
      }
    })
    updateHeatmap()
  })
}

const clearHotspotMarkers = () => {
  if (!map || !hotspotMarkers.length) return
  map.remove(hotspotMarkers)
  hotspotMarkers = []
}

const createHotspotMarkerContent = (point: any, index: number) => {
  const wrapper = document.createElement('div')
  wrapper.style.display = 'flex'
  wrapper.style.alignItems = 'center'
  wrapper.style.gap = '8px'
  wrapper.style.transform = 'translate(-10px, -44px)'

  const badge = document.createElement('div')
  badge.textContent = `TOP ${index + 1}`
  badge.style.background = '#ef4444'
  badge.style.color = '#ffffff'
  badge.style.fontSize = '10px'
  badge.style.fontWeight = '700'
  badge.style.padding = '4px 6px'
  badge.style.borderRadius = '999px'
  badge.style.boxShadow = '0 6px 16px rgba(239, 68, 68, 0.28)'

  const label = document.createElement('div')
  label.textContent = `${point.deviceName || point.deviceId} · ${point.count ?? point.value ?? 0}次`
  label.style.background = 'rgba(255, 255, 255, 0.96)'
  label.style.color = '#1e293b'
  label.style.fontSize = '12px'
  label.style.fontWeight = '600'
  label.style.padding = '6px 10px'
  label.style.borderRadius = '999px'
  label.style.border = '1px solid rgba(239, 68, 68, 0.2)'
  label.style.boxShadow = '0 8px 20px rgba(15, 23, 42, 0.12)'
  label.style.whiteSpace = 'nowrap'

  wrapper.appendChild(badge)
  wrapper.appendChild(label)
  return wrapper
}

const renderHotspotMarkers = () => {
  if (!map || !window.AMap) return
  clearHotspotMarkers()
  hotspotMarkers = topHotspots.value.map((point, index) => new window.AMap.Marker({
    position: [point.lng, point.lat],
    anchor: 'bottom-center',
    offset: new window.AMap.Pixel(0, 0),
    content: createHotspotMarkerContent(point, index),
    zIndex: 120 + index
  }))
  if (hotspotMarkers.length) {
    map.add(hotspotMarkers)
  }
}

const updateHeatmap = () => {
  if (!heatmap || !map) return
  const data = validHeatmapPoints.value.map(item => ({
    lng: item.lng,
    lat: item.lat,
    count: item.value || item.count || 1
  }))
  const max = data.length ? Math.max(...data.map(item => item.count || 1)) : 1
  heatmap.setDataSet({ data, max })
  renderHotspotMarkers()
  if (data.length) {
    const focus = topHotspots.value[0] || data[0]
    map.setCenter([focus.lng, focus.lat])
  } else {
    map.setCenter(DEFAULT_MAP_CENTER)
  }
}

const handleResize = () => {
  activityChart?.resize()
  batteryChart?.resize()
  alarmChart?.resize()
}

const loadDashboardIfStale = (force = false) => {
  const stale = Date.now() - lastLoadedAt > DASHBOARD_STALE_MS
  if (!force && !stale && overview.value?.deviceCount != null) {
    return
  }
  void loadDashboard({ silent: true })
}

const startDashboardPoll = () => {
  if (stopDashboardPoll) return
  const tick = () => {
    if (document.visibilityState === 'visible') {
      loadDashboardIfStale(true)
    }
  }
  const timer = window.setInterval(tick, 15_000)
  const onVis = () => {
    if (document.visibilityState === 'visible') {
      loadDashboardIfStale()
    }
  }
  document.addEventListener('visibilitychange', onVis)
  stopDashboardPoll = () => {
    window.clearInterval(timer)
    document.removeEventListener('visibilitychange', onVis)
    stopDashboardPoll = null
  }
}

onMounted(async () => {
  // 登录后首次进入时，稍等布局稳定再拉数，减少偶发空数据
  await nextTick()
  requestAnimationFrame(() => {
    loadDashboard()
  })
  startDashboardPoll()
  window.addEventListener('resize', handleResize)
})

onActivated(() => {
  handleResize()
  // keep-alive 切回首页时重新拉数（设备页在线状态会变，看板不能沿用旧缓存）
  loadDashboardIfStale()
  startDashboardPoll()
  if (activityChart || batteryChart || alarmChart) {
    handleResize()
  }
})

onDeactivated(() => {
  if (stopDashboardPoll) {
    stopDashboardPoll()
  }
})

onUnmounted(() => {
  dashboardRequestId += 1
  if (retryTimer) {
    window.clearTimeout(retryTimer)
    retryTimer = null
  }
  if (stopDashboardPoll) {
    stopDashboardPoll()
  }
  window.removeEventListener('resize', handleResize)
  activityChart?.dispose()
  batteryChart?.dispose()
  alarmChart?.dispose()
  clearHotspotMarkers()
  map?.destroy()
  activityChart = null
  batteryChart = null
  alarmChart = null
  map = null
  heatmap = null
  hotspotMarkers = []
})
</script>

<style scoped>
.dashboard-page {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 100%;
  height: auto;
  box-sizing: border-box;
  overflow-x: hidden;
  overflow-y: visible;
  background: linear-gradient(180deg, #f8fafc 0%, #eef4ff 100%);
}

.load-error-alert {
  margin-bottom: 4px;
}

.stats-section {
  min-height: 120px;
}

.charts-section {
  min-height: 0;
}

.dashboard-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.hero h2 {
  margin-bottom: 6px;
  color: #0f172a;
}
.hero p {
  color: #64748b;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
}
.stat-card, .panel {
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}
.stat-card {
  padding: 18px;
  background: rgba(255, 255, 255, 0.92);
}
.stat-label {
  color: #64748b;
  font-size: 13px;
}
.stat-value {
  margin: 10px 0 6px;
  font-size: 30px;
  color: #0f172a;
}
.stat-sub {
  color: #94a3b8;
  font-size: 12px;
}

/* 12 列栅格：左热力跨两行，右上活跃/电量，右中报警，下排排行+健康 */
.dashboard-main {
  display: grid;
  grid-template-columns: 1.35fr 1fr 1fr;
  grid-template-rows: minmax(220px, 1fr) minmax(220px, 1fr) auto;
  gap: 16px;
  align-items: stretch;
  grid-template-areas:
    "map activity battery"
    "map alarm ranking"
    "health health health";
}

.panel--map { grid-area: map; }
.panel--activity { grid-area: activity; }
.panel--battery { grid-area: battery; }
.panel--alarm { grid-area: alarm; }
.panel--ranking { grid-area: ranking; }
.panel--health { grid-area: health; }

.panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.panel :deep(.el-card__header) {
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.panel :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 12px 16px 16px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  font-weight: 600;
  color: #1e293b;
}
.panel-desc {
  margin: 4px 0 0;
  font-size: 12px;
  font-weight: 400;
  color: #64748b;
  line-height: 1.5;
}
.heatmap-legend {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 8px 12px;
  border-radius: 10px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
}
.legend-label {
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
}
.legend-bar {
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, #3b82f6 0%, #22c55e 40%, #f59e0b 72%, #ef4444 100%);
}
.legend-ticks {
  display: flex;
  gap: 8px;
  color: #64748b;
  white-space: nowrap;
}
.heatmap-wrapper {
  position: relative;
  flex: 1;
  min-height: 280px;
}
.map-highlight-tip {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 10;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  color: #ef4444;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.1);
}
.heatmap {
  height: 100%;
  min-height: 280px;
  border-radius: 12px;
  overflow: hidden;
}
.chart {
  flex: 1;
  width: 100%;
  min-height: 180px;
  height: 180px;
}
.panel-table {
  width: 100%;
}
.hotspot-list {
  margin-top: 12px;
  display: grid;
  gap: 8px;
  max-height: 168px;
  overflow-y: auto;
  flex-shrink: 0;
}
.hotspot-empty {
  padding: 16px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
  background: #f8fafc;
  border-radius: 10px;
}
.hotspot-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
}
.hotspot-item p {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}
.hotspot-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  color: #ef4444;
  font-size: 12px;
  text-align: right;
  flex-shrink: 0;
}

@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .dashboard-main {
    grid-template-columns: 1fr 1fr;
    grid-template-rows: auto;
    grid-template-areas:
      "map map"
      "activity battery"
      "alarm ranking"
      "health health";
  }
  .heatmap-wrapper,
  .heatmap {
    min-height: 320px;
    height: 320px;
  }
  .chart {
    height: 200px;
  }
}

@media (max-width: 768px) {
  .dashboard-page {
    padding: 16px;
  }

  .hero {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-main {
    grid-template-columns: 1fr;
    grid-template-areas:
      "map"
      "activity"
      "battery"
      "alarm"
      "ranking"
      "health";
  }

  .heatmap-legend {
    grid-template-columns: 1fr;
  }

  .panel-header {
    flex-direction: column;
  }

  .hotspot-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .hotspot-meta {
    align-items: flex-start;
    text-align: left;
  }

  .map-highlight-tip {
    left: 12px;
    right: auto;
  }
}
</style>
