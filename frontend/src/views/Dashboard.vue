<template>
  <div class="dashboard-page">
    <div class="hero">
      <div>
        <h2>智能盲杖数据看板</h2>
        <p>面向管理端的风险热力、设备健康与活跃趋势总览</p>
      </div>
      <el-button type="primary" @click="loadDashboard">刷新看板</el-button>
    </div>

    <div class="stats-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <p class="stat-label">{{ card.label }}</p>
        <h3 class="stat-value">{{ card.value }}</h3>
        <span class="stat-sub">{{ card.sub }}</span>
      </div>
    </div>

    <div class="content-grid">
      <el-card class="panel panel--map">
        <template #header>
          <div class="panel-header">
            <div>
              <span>风险区域热力图</span>
              <p class="panel-desc">颜色越偏红，表示该区域风险事件越集中；热点数表示本次统计纳入分析的位置点数量。</p>
            </div>
            <el-tag type="danger">{{ heatmapPoints.length }} 个热点</el-tag>
          </div>
        </template>
        <div class="heatmap-legend">
          <div class="legend-scale">
            <span class="legend-label">风险强度</span>
            <div class="legend-bar"></div>
            <div class="legend-ticks">
              <span>低</span>
              <span>中</span>
              <span>高</span>
            </div>
          </div>
          <div class="legend-note">
            <span>下方列表说明：</span>
            <span>地图中已重点标注 Top 5 热点；`风险次数` 表示该区域累计触发次数，`最小障碍距离` 表示该热点附近记录到的最近障碍物距离。</span>
          </div>
        </div>
        <div class="heatmap-wrapper">
          <div ref="mapContainer" class="heatmap"></div>
          <div class="map-highlight-tip">地图内已重点标注 Top 5 热点</div>
        </div>
        <div class="hotspot-list">
          <div v-for="(point, index) in topHotspots" :key="`${point.lng}-${point.lat}`" class="hotspot-item">
            <div>
              <strong>TOP {{ index + 1 }} · {{ point.deviceName || point.deviceId }}</strong>
              <p>{{ point.lng }}, {{ point.lat }}</p>
            </div>
            <div class="hotspot-meta">
              <span>风险次数：{{ point.count ?? point.value ?? 0 }} 次</span>
              <span>最小障碍距离：{{ point.minObstacleDistance ?? '-' }} cm</span>
            </div>
          </div>
        </div>
      </el-card>

      <div class="chart-stack">
        <el-card class="panel">
          <template #header>
            <div class="panel-header">
              <span>活跃时长趋势</span>
            </div>
          </template>
          <div ref="activityChartRef" class="chart"></div>
        </el-card>

        <el-card class="panel">
          <template #header>
            <div class="panel-header">
              <span>设备电量概况</span>
            </div>
          </template>
          <div ref="batteryChartRef" class="chart"></div>
        </el-card>

        <el-card class="panel">
          <template #header>
            <div class="panel-header">
              <span>报警分布</span>
            </div>
          </template>
          <div ref="alarmChartRef" class="chart"></div>
        </el-card>
      </div>
    </div>

    <div class="bottom-grid">
      <el-card class="panel">
        <template #header>
          <div class="panel-header">
            <span>设备报警排行</span>
          </div>
        </template>
        <el-table :data="deviceRanking" size="small" stripe>
          <el-table-column prop="deviceName" label="设备" min-width="140" />
          <el-table-column prop="alarmCount" label="报警总数" width="100" />
          <el-table-column prop="unhandledCount" label="未处理" width="90" />
          <el-table-column prop="latestAlarmTime" label="最近报警时间" min-width="160" />
        </el-table>
      </el-card>

      <el-card class="panel">
        <template #header>
          <div class="panel-header">
            <span>设备健康度面板</span>
          </div>
        </template>
        <el-table :data="deviceHealth" size="small" stripe>
          <el-table-column prop="deviceName" label="设备" min-width="140" />
          <el-table-column label="健康度" width="110">
            <template #default="scope">
              <el-tag :type="getHealthTagType(scope.row.healthScore)">{{ scope.row.healthScore }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="batteryLevel" label="电量" width="80" />
          <el-table-column prop="status" label="状态" width="90" />
          <el-table-column prop="latestDataTime" label="最近数据时间" min-width="160" />
          <el-table-column prop="unhandledCount" label="待处理报警" width="100" />
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

const overview = ref<any>({})
const activityTrend = ref<any[]>([])
const batteryTrend = ref<any[]>([])
const alarmDistribution = ref<any[]>([])
const deviceRanking = ref<any[]>([])
const deviceHealth = ref<any[]>([])
const heatmapPoints = ref<any[]>([])

const mapContainer = ref<HTMLElement | null>(null)
const activityChartRef = ref<HTMLElement | null>(null)
const batteryChartRef = ref<HTMLElement | null>(null)
const alarmChartRef = ref<HTMLElement | null>(null)

let map: any = null
let heatmap: any = null
let activityChart: any = null
let batteryChart: any = null
let alarmChart: any = null
let hotspotMarkers: any[] = []

const statCards = computed(() => [
  { label: '设备总数', value: overview.value.deviceCount ?? 0, sub: `在线 ${overview.value.onlineDevices ?? 0} 台` },
  { label: '待处理报警', value: overview.value.unhandledAlarms ?? 0, sub: `累计报警 ${overview.value.alarmCount ?? 0} 条` },
  { label: '风险事件', value: overview.value.riskEvents ?? 0, sub: '障碍物高风险点' },
  { label: '活跃设备', value: overview.value.activeDevicesToday ?? 0, sub: '今日上传数据的设备' },
  { label: '低电量设备', value: overview.value.lowBatteryDevices ?? 0, sub: '电量 ≤ 20%' },
  { label: '传感器样本', value: overview.value.sensorCount ?? 0, sub: '当前保留数据总量' }
])

const topHotspots = computed(() => [...heatmapPoints.value]
  .sort((a, b) => (b.count ?? b.value ?? 0) - (a.count ?? a.value ?? 0))
  .slice(0, 5))

const getHealthTagType = (score: number) => {
  if (score >= 85) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

const loadDashboard = async () => {
  try {
    const response = await axios.get('/api/analytics/dashboard')
    const data = response.data?.data || {}
    overview.value = data.overview || {}
    activityTrend.value = data.activityTrend || []
    batteryTrend.value = data.batteryTrend || []
    alarmDistribution.value = data.alarmDistribution || []
    deviceRanking.value = data.deviceRanking || []
    deviceHealth.value = data.deviceHealth || []
    heatmapPoints.value = data.heatmapPoints || []
    await nextTick()
    initCharts()
    updateCharts()
    initMap()
    updateHeatmap()
  } catch (error) {
    console.error('加载看板失败', error)
    ElMessage.error('加载看板失败')
  }
}

const initCharts = () => {
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

const initMap = () => {
  if (!window.AMap || !mapContainer.value || map) return
  map = new window.AMap.Map(mapContainer.value, {
    zoom: 12,
    center: [116.4074, 39.9042],
    viewMode: '2D'
  })
  map.addControl(new window.AMap.Scale())
  map.addControl(new window.AMap.ToolBar())
  window.AMap.plugin(['AMap.Heatmap'], () => {
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
  hotspotMarkers = topHotspots.value
    .filter(point => point.lng != null && point.lat != null)
    .map((point, index) => new window.AMap.Marker({
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
  const data = heatmapPoints.value.map(item => ({ lng: item.lng, lat: item.lat, count: item.value || item.count || 1 }))
  const max = data.length ? Math.max(...data.map(item => item.count || 1)) : 1
  heatmap.setDataSet({ data, max })
  renderHotspotMarkers()
  if (data.length && map) {
    map.setCenter([topHotspots.value[0]?.lng || data[0].lng, topHotspots.value[0]?.lat || data[0].lat])
  }
}

const handleResize = () => {
  activityChart?.resize()
  batteryChart?.resize()
  alarmChart?.resize()
}

onMounted(async () => {
  await loadDashboard()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
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
  border-radius: 18px;
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
.content-grid {
  display: grid;
  grid-template-columns: 1.45fr 1fr;
  gap: 20px;
  align-items: start;
}
.chart-stack, .bottom-grid {
  display: grid;
  gap: 20px;
}
.chart-stack {
  align-content: start;
}
.bottom-grid {
  grid-template-columns: 1fr 1.2fr;
  align-items: start;
}
.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  font-weight: 600;
  color: #1e293b;
}
.panel-desc {
  margin: 6px 0 0;
  font-size: 12px;
  font-weight: 400;
  color: #64748b;
  line-height: 1.6;
}
.heatmap-legend {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
}
.legend-scale {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 220px;
}
.legend-label {
  font-weight: 600;
  color: #334155;
}
.legend-bar {
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(90deg, #3b82f6 0%, #22c55e 40%, #f59e0b 72%, #ef4444 100%);
}
.legend-ticks {
  display: flex;
  justify-content: space-between;
  color: #64748b;
}
.legend-note {
  display: flex;
  flex-direction: column;
  gap: 6px;
  line-height: 1.6;
}
.heatmap-wrapper {
  position: relative;
}
.map-highlight-tip {
  position: absolute;
  top: 14px;
  right: 14px;
  z-index: 10;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #ef4444;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.12);
}
.heatmap {
  height: 460px;
  border-radius: 14px;
  overflow: hidden;
}
.chart {
  height: 240px;
}
.hotspot-list {
  margin-top: 16px;
  display: grid;
  gap: 10px;
}
.hotspot-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
}
.hotspot-item p {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}
.hotspot-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  color: #ef4444;
  font-size: 12px;
  text-align: right;
}
@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .content-grid, .bottom-grid {
    grid-template-columns: 1fr;
  }
  .heatmap-legend {
    flex-direction: column;
    align-items: flex-start;
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

  .panel-header {
    flex-direction: column;
  }

  .hotspot-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .hotspot-meta {
    align-items: flex-start;
    text-align: left;
  }

  .map-highlight-tip {
    left: 14px;
    right: auto;
  }
}
</style>
