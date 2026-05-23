<template>
  <div class="monitor-container">
    <div class="header">
      <h2>设备实时监控 - {{ deviceInfo.deviceName || '加载中...' }}</h2>
      <el-button @click="goBack" type="primary">返回设备列表</el-button>
    </div>
    
    <div class="content">
      <!-- 左侧：地图区域 -->
      <div class="map-section">
        <div id="map-container" class="map-container"></div>
        <div class="map-controls">
          <el-button @click="refreshLocation" type="success" size="small">
            <el-icon><Refresh /></el-icon> 刷新位置
          </el-button>
          <el-button @click="toggleTrajectory" type="warning" size="small">
            {{ showTrajectory ? '隐藏轨迹' : '显示轨迹' }}
          </el-button>
        </div>
      </div>
      
      <!-- 右侧：设备信息面板 -->
      <div class="info-panel">
        <div class="summary-grid">
          <div class="summary-item">
            <span class="summary-label">设备状态</span>
            <el-tag :type="deviceStatus === '在线' ? 'success' : 'danger'">
              {{ deviceStatus }}
            </el-tag>
          </div>
          <div class="summary-item">
            <span class="summary-label">当前电量</span>
            <span :class="getBatteryClass(deviceInfo.batteryLevel)" class="summary-value">
              {{ deviceInfo.batteryLevel ?? '-' }}%
            </span>
          </div>
          <div class="summary-item">
            <span class="summary-label">数据更新时间</span>
            <span class="summary-value">{{ latestData?.createTime || '-' }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">最新报警</span>
            <el-tag :type="latestAlarm && String(latestAlarm.status) === '0' ? 'danger' : 'info'">
              {{ latestAlarm?.alarmType || '暂无报警' }}
            </el-tag>
          </div>
        </div>

        <el-card class="panel-card">
          <el-tabs v-model="activePanel" stretch class="monitor-tabs">
            <el-tab-pane label="设备" name="device">
              <div class="tab-content">
                <div class="section-block">
                  <div class="section-title">
                    <span>设备信息</span>
                    <el-tag :type="deviceStatus === '在线' ? 'success' : 'danger'">
                      {{ deviceStatus }}
                    </el-tag>
                  </div>
                  <div class="info-item">
                    <label>设备编号：</label>
                    <span>{{ deviceInfo.deviceId || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>设备名称：</label>
                    <span>{{ deviceInfo.deviceName || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>所属用户：</label>
                    <span>{{ deviceInfo.userName || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>当前电量：</label>
                    <span :class="getBatteryClass(deviceInfo.batteryLevel)">
                      {{ deviceInfo.batteryLevel ?? '-' }}%
                    </span>
                  </div>
                  <div class="info-item">
                    <label>更新时间：</label>
                    <span>{{ latestData?.createTime || '-' }}</span>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="位置" name="location">
              <div class="tab-content">
                <div class="section-block">
                  <div class="section-title">
                    <span>最新位置</span>
                  </div>
                  <div class="info-item">
                    <label>经度：</label>
                    <span>{{ latestData?.longitude || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>纬度：</label>
                    <span>{{ latestData?.latitude || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>地址：</label>
                    <span class="address-value">{{ address || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>超声波距离：</label>
                    <span>{{ latestData?.obstacleDistance ? latestData.obstacleDistance + ' cm' : '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>摔倒检测：</label>
                    <el-tag :type="latestData?.isFall ? 'danger' : 'success'" size="small">
                      {{ latestData?.isFall ? '⚠️ 摔倒！' : '正常' }}
                    </el-tag>
                  </div>
                  <div class="info-item">
                    <label>摔倒置信度：</label>
                    <span>{{ fallConfidenceText }}</span>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="围栏" name="fence">
              <div class="tab-content">
                <div class="section-block" v-if="fenceList.length > 1">
                  <div class="section-title"><span>围栏列表 ({{ fenceList.length }})</span></div>
                  <div class="fence-selector">
                    <el-button
                      v-for="(f, idx) in fenceList"
                      :key="f.id"
                      :type="idx === currentFenceIndex ? 'primary' : 'default'"
                      size="small"
                      @click="switchFence(idx)"
                    >
                      {{ f.fenceName || '围栏' + (idx + 1) }}
                    </el-button>
                  </div>
                </div>
                <div class="section-block">
                  <div class="section-title">
                    <span>电子围栏</span>
                    <el-tag :type="fenceStatus.outside ? 'danger' : (fenceForm.enabled ? 'success' : 'info')" size="small">
                      {{ fenceStatus.outside ? '已越界' : (fenceForm.enabled ? '安全区内' : '未启用') }}
                    </el-tag>
                  </div>
                  <div class="info-item">
                    <label>围栏名称：</label>
                    <span>{{ fence?.fenceName || fenceForm.fenceName || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>围栏半径：</label>
                    <span>{{ fenceForm.radiusMeters || '-' }} m</span>
                  </div>
                  <div class="info-item">
                    <label>围栏中心：</label>
                    <span>{{ fenceCenterText }}</span>
                  </div>
                  <div class="info-item">
                    <label>当前位置距中心：</label>
                    <span>{{ fenceDistanceText }}</span>
                  </div>
                  <div class="fence-form-row">
                    <el-input v-model="fenceForm.fenceName" placeholder="围栏名称" />
                  </div>
                  <div class="fence-form-row">
                    <el-input-number v-model="fenceForm.radiusMeters" :min="50" :max="5000" :step="50" />
                    <el-switch v-model="fenceForm.enabled" active-text="启用" inactive-text="停用" />
                  </div>
                  <div class="radius-presets">
                    <el-button size="small" @click="setFenceRadius(100)">100米</el-button>
                    <el-button size="small" @click="setFenceRadius(300)">300米</el-button>
                    <el-button size="small" @click="setFenceRadius(500)">500米</el-button>
                  </div>
                  <div class="fence-actions">
                    <el-button size="small" @click="useCurrentLocationAsFenceCenter">当前位置设为中心</el-button>
                    <el-button size="small" @click="focusFence">定位到围栏</el-button>
                    <el-button size="small" type="primary" @click="saveFence">保存围栏</el-button>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="路口辅助" name="crossing">
              <div class="tab-content">
                <div class="section-block">
                  <div class="section-title">
                    <span>路口辅助状态</span>
                    <el-tag :type="crossingTagType" size="small">
                      {{ crossingRecommendationLabel }}
                    </el-tag>
                  </div>
                  <div class="section-actions">
                    <el-button size="small" @click="refreshCrossingAssist">刷新结果</el-button>
                    <el-button size="small" type="primary" @click="mockCrossingAssist">生成演示结果</el-button>
                  </div>
                  <div class="info-item">
                    <label>红绿灯：</label>
                    <span>{{ crossingTrafficLightLabel }}</span>
                  </div>
                  <div class="info-item">
                    <label>斑马线：</label>
                    <span>{{ crossingAssist.zebraCrossingDetected ? '已检测' : '未检测到' }}</span>
                  </div>
                  <div class="info-item">
                    <label>方向提示：</label>
                    <span>{{ crossingDirectionLabel }}</span>
                  </div>
                  <div class="info-item">
                    <label>车辆接近：</label>
                    <span>{{ crossingAssist.vehicleApproaching ? '有车接近' : '未见接近车辆' }}</span>
                  </div>
                  <div class="info-item">
                    <label>识别置信度：</label>
                    <span>{{ crossingConfidenceText }}</span>
                  </div>
                  <div class="info-item">
                    <label>结果来源：</label>
                    <span>{{ crossingAssist.source || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>更新时间：</label>
                    <span>{{ crossingAssist.updateTime || '-' }}</span>
                  </div>
                </div>

                <div class="section-block">
                  <div class="section-title">
                    <span>辅助提示文案</span>
                  </div>
                  <div class="crossing-message">
                    {{ crossingAssist.message || '暂未收到路口辅助识别结果' }}
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="报警" name="alarm">
              <div class="tab-content">
                <div class="section-block" v-if="latestAlarm">
                  <div class="section-title alarm-header">
                    <span>⚠️ 最新报警</span>
                    <el-button @click="handleAlarm" type="primary" size="small">
                      处理报警
                    </el-button>
                  </div>
                  <div class="alarm-content">
                    <p><strong>报警类型：</strong>{{ latestAlarm.alarmType }}</p>
                    <p><strong>报警时间：</strong>{{ latestAlarm.alarmTime }}</p>
                    <p><strong>处理状态：</strong>
                      <el-tag :type="String(latestAlarm.status) === '0' ? 'danger' : 'success'">
                        {{ String(latestAlarm.status) === '0' ? '未处理' : '已处理' }}
                      </el-tag>
                    </p>
                  </div>
                </div>
                <el-empty v-else description="当前暂无报警" />
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const deviceId = ref(route.params.deviceId as string)
const activePanel = ref('device')

// 地图相关
const DEFAULT_LOCATION = {
  longitude: 116.7849,
  latitude: 36.5300
}
let map: any = null
let marker: any = null
let trajectoryLine: any = null
let fenceCircle: any = null

// 数据
const deviceInfo = ref<any>({})
const latestData = ref<any>(null)
const deviceStatus = ref('离线')
const address = ref('')
const showTrajectory = ref(false)
const trajectoryData = ref<any[]>([])
const latestAlarm = ref<any>(null)
const crossingAssist = ref<any>({})
const fence = ref<any>(null)
const fenceList = ref<any[]>([])
const currentFenceIndex = ref(0)
const fenceForm = ref<any>({
  deviceId: '',
  fenceName: '安全活动区',
  centerLatitude: null,
  centerLongitude: null,
  radiusMeters: 300,
  enabled: false
})
const fenceStatus = ref<any>({
  outside: false,
  distanceMeters: null,
  triggered: false
})

const fenceCenterText = computed(() => {
  if (fenceForm.value.centerLongitude == null || fenceForm.value.centerLatitude == null) {
    return '-'
  }
  return `${Number(fenceForm.value.centerLongitude).toFixed(4)}, ${Number(fenceForm.value.centerLatitude).toFixed(4)}`
})

const fenceDistanceText = computed(() => {
  if (fenceStatus.value.distanceMeters == null) {
    return '-'
  }
  return `${Number(fenceStatus.value.distanceMeters).toFixed(1)} m`
})

const fallConfidenceText = computed(() => {
  if (latestData.value?.fallConfidence == null) {
    return latestData.value?.isFall ? '100%' : '0%'
  }
  return `${Math.round(Number(latestData.value.fallConfidence) * 100)}%`
})

const crossingTrafficLightLabel = computed(() => {
  switch (String(crossingAssist.value?.trafficLightStatus || '').toUpperCase()) {
    case 'RED':
      return '红灯'
    case 'GREEN':
      return '绿灯'
    case 'YELLOW':
      return '黄灯'
    default:
      return '未知'
  }
})

const crossingDirectionLabel = computed(() => {
  switch (String(crossingAssist.value?.zebraCrossingDirection || '').toUpperCase()) {
    case 'LEFT':
      return '偏左'
    case 'RIGHT':
      return '偏右'
    case 'CENTER':
      return '居中'
    default:
      return '未知'
  }
})

const crossingRecommendationLabel = computed(() => {
  switch (crossingAssist.value?.recommendation) {
    case 'WAIT':
      return '建议等待'
    case 'PROCEED_CAUTION':
      return '谨慎通行'
    case 'ALIGN_FIRST':
      return '先校准方向'
    case 'SEARCH_ZEBRA':
      return '寻找斑马线'
    default:
      return '等待识别'
  }
})

const crossingTagType = computed(() => {
  switch (crossingAssist.value?.recommendation) {
    case 'WAIT':
      return 'danger'
    case 'PROCEED_CAUTION':
      return 'success'
    case 'ALIGN_FIRST':
      return 'warning'
    default:
      return 'info'
  }
})

const crossingConfidenceText = computed(() => {
  if (crossingAssist.value?.confidence == null) {
    return '-'
  }
  return `${Math.round(Number(crossingAssist.value.confidence) * 100)}%`
})

const applyCrossingAssist = (payload: any) => {
  crossingAssist.value = payload || {}
}

// WebSocket
let ws: WebSocket | null = null
let wsReconnectTimer: any = null
let isPageUnmounted = false

// 定时器
let refreshTimer: any = null
let alarmCheckTimer: any = null
let activeAlarmNotification: any = null
let lastAlarmNotificationAt = 0
let lastAlarmSoundAt = 0
const notifiedAlarmKeys = new Set<string>()
const ALARM_NOTIFICATION_COOLDOWN_MS = 10000

// 返回设备列表
const goBack = () => {
  router.push('/devices')
}

// 获取电池样式
const getBatteryClass = (battery: number) => {
  if (battery <= 20) return 'battery-low'
  if (battery <= 50) return 'battery-medium'
  return 'battery-high'
}

// 初始化高德地图
const initMap = () => {
  if (!window.AMap) {
    ElMessage.error('高德地图API加载失败')
    return
  }
  
  map = new window.AMap.Map('map-container', {
    zoom: 15,
    center: [116.7849, 36.5300],
    viewMode: '2D'
  })
  
  // 添加地图控件
  map.addControl(new window.AMap.Scale())
  map.addControl(new window.AMap.ToolBar())
}

// 更新地图标记
const updateMapMarker = (longitude: number, latitude: number) => {
  if (!map) return
  
  const position = [longitude, latitude]
  
  // 移除旧标记
  if (marker) {
    map.remove(marker)
  }
  
  // 创建新标记
  marker = new window.AMap.Marker({
    position: position,
    title: deviceInfo.value.deviceName,
    label: {
      content: deviceInfo.value.deviceName,
      offset: new window.AMap.Pixel(0, -30)
    }
  })
  
  map.add(marker)
  map.setCenter(position)
  
  // 逆地理编码获取地址
  getAddress(longitude, latitude)
}

const clearFenceCircle = () => {
  if (fenceCircle && map) {
    map.remove(fenceCircle)
    fenceCircle = null
  }
}

const drawFence = () => {
  if (!map) return
  clearFenceCircle()
  if (!fenceForm.value.enabled || fenceForm.value.centerLongitude == null || fenceForm.value.centerLatitude == null || !fenceForm.value.radiusMeters) {
    return
  }
  fenceCircle = new window.AMap.Circle({
    center: [Number(fenceForm.value.centerLongitude), Number(fenceForm.value.centerLatitude)],
    radius: Number(fenceForm.value.radiusMeters),
    strokeColor: fenceStatus.value.outside ? '#F56C6C' : '#67C23A',
    strokeWeight: 2,
    fillColor: fenceStatus.value.outside ? '#FDE2E2' : '#E1F3D8',
    fillOpacity: 0.2
  })
  map.add(fenceCircle)
}

const focusFence = () => {
  if (!map || !fenceForm.value.centerLongitude || !fenceForm.value.centerLatitude) return
  map.setCenter([Number(fenceForm.value.centerLongitude), Number(fenceForm.value.centerLatitude)])
  map.setZoom(16)
}

// 逆地理编码
const getAddress = (longitude: number, latitude: number) => {
  // 这里需要调用后端接口进行逆地理编码
  // 简化处理，实际项目中通过后端调用高德Web服务API
  address.value = `${longitude.toFixed(4)}, ${latitude.toFixed(4)}`
}

// 显示轨迹
const toggleTrajectory = async () => {
  if (showTrajectory.value) {
    if (trajectoryLine) {
      map.remove(trajectoryLine)
      trajectoryLine = null
    }
    showTrajectory.value = false
  } else {
    await loadTrajectory()
    showTrajectory.value = true
    renderTrajectory()
  }
}

const renderTrajectory = () => {
  if (!map || !showTrajectory.value) return
  if (trajectoryLine) {
    map.remove(trajectoryLine)
    trajectoryLine = null
  }
  if (trajectoryData.value.length === 0) return
  const path = trajectoryData.value.map((item: any) => [item.longitude, item.latitude])
  trajectoryLine = new window.AMap.Polyline({
    path,
    strokeColor: '#FF6B6B',
    strokeWeight: 3,
    strokeOpacity: 0.8
  })
  map.add(trajectoryLine)
}

// 加载轨迹数据
const loadTrajectory = async () => {
  try {
    const response = await axios.get(`/api/sensor-data/trajectory`, {
      params: { deviceId: deviceInfo.value?.deviceId, hours: 1 }
    })
    if (response.data.code === 200) {
      trajectoryData.value = response.data.data || []
      renderTrajectory()
    }
  } catch (error) {
    console.error('加载轨迹失败', error)
  }
}

// 获取设备信息
const loadDeviceInfo = async () => {
  try {
    const response = await axios.get(`/api/devices/${deviceId.value}`)
    if (response.data.code === 200) {
      deviceInfo.value = response.data.data
      fenceForm.value.deviceId = response.data.data?.deviceId || ''
      if (!deviceInfo.value.deviceName) {
        deviceInfo.value.deviceName = `智能设备 ${response.data.data?.deviceId || ''}`
      }
    }
  } catch (error) {
    console.error('获取设备信息失败', error)
  }
}

const loadCrossingAssist = async () => {
  if (!deviceInfo.value?.deviceId) return
  try {
    const response = await axios.get(`/api/mini/devices/${deviceInfo.value.deviceId}/crossing-assist`, {
      params: { _t: Date.now() }
    })
    if (response.data.code === 200) {
      applyCrossingAssist(response.data.data)
    }
  } catch (error) {
    console.error('获取路口辅助结果失败', error)
  }
}

const refreshCrossingAssist = async () => {
  if (!deviceInfo.value?.deviceId) {
    ElMessage.warning('设备信息未加载完成')
    return
  }
  try {
    if (crossingAssist.value?.source === 'mock-demo') {
      const response = await axios.post(`/api/mini/devices/${deviceInfo.value.deviceId}/crossing-assist/mock`)
      if (response.data.code === 200) {
        applyCrossingAssist(response.data.data)
        ElMessage.success('已切换演示场景')
      }
      return
    }
    await loadCrossingAssist()
    ElMessage.success('已刷新路口辅助结果')
  } catch (error) {
    console.error('刷新路口辅助结果失败', error)
    ElMessage.error('刷新路口辅助结果失败')
  }
}

const mockCrossingAssist = async () => {
  if (!deviceInfo.value?.deviceId) {
    ElMessage.warning('设备信息未加载完成')
    return
  }
  try {
    const response = await axios.post(`/api/mini/devices/${deviceInfo.value.deviceId}/crossing-assist/mock`)
    if (response.data.code === 200) {
      applyCrossingAssist(response.data.data)
      ElMessage.success('已生成演示结果')
    }
  } catch (error) {
    console.error('生成演示结果失败', error)
    ElMessage.error('生成演示结果失败')
  }
}

const applyFence = (fenceData: any) => {
  if (!fenceData) {
    fence.value = null
    fenceForm.value = {
      ...fenceForm.value,
      deviceId: deviceInfo.value?.deviceId || '',
      fenceName: '安全活动区',
      centerLatitude: null,
      centerLongitude: null,
      radiusMeters: 300,
      enabled: false
    }
    fenceStatus.value = {
      outside: false,
      distanceMeters: null,
      triggered: false
    }
    clearFenceCircle()
    return
  }
  fence.value = fenceData
  fenceForm.value = {
    deviceId: fenceData.deviceId,
    fenceName: fenceData.fenceName || '安全活动区',
    centerLatitude: fenceData.centerLatitude,
    centerLongitude: fenceData.centerLongitude,
    radiusMeters: fenceData.radiusMeters || 300,
    enabled: !!fenceData.enabled
  }
  drawFence()
}

const applyFenceStatus = (payload: any) => {
  if (!payload) return
  if (payload.fence) {
    applyFence(payload.fence)
  }
  fenceStatus.value = {
    outside: !!payload.outside,
    distanceMeters: payload.distanceMeters ?? null,
    triggered: !!payload.triggered
  }
  drawFence()
}

const loadFence = async () => {
  if (!deviceInfo.value?.deviceId) return
  try {
    const response = await axios.get('/api/fences', {
      params: { deviceId: deviceInfo.value.deviceId }
    })
    if (response.data.code === 200) {
      const data = response.data.data
      const list = Array.isArray(data) ? data : (data ? [data] : [])
      fenceList.value = list
      currentFenceIndex.value = 0
      // 应用第一个围栏到表单
      applyFence(list.length > 0 ? list[0] : null)
      // 绘制所有围栏圆圈
      drawAllFences()
    }
  } catch (error) {
    console.error('获取电子围栏失败', error)
  }
}

const drawAllFences = () => {
  if (!map) return
  // 清除旧的额外围栏圆
  if ((window as any).__extraFenceCircles) {
    (window as any).__extraFenceCircles.forEach((c: any) => map.remove(c))
  }
  (window as any).__extraFenceCircles = []
  
  fenceList.value.forEach((f, idx) => {
    if (f && f.enabled && f.centerLongitude && f.centerLatitude && f.radiusMeters) {
      const isSelected = idx === currentFenceIndex.value
      const circle = new window.AMap.Circle({
        center: [Number(f.centerLongitude), Number(f.centerLatitude)],
        radius: Number(f.radiusMeters),
        strokeColor: isSelected ? '#67C23A' : '#409EFF',
        strokeWeight: isSelected ? 3 : 2,
        fillColor: isSelected ? '#E1F3D8' : '#D9ECFF',
        fillOpacity: 0.15
      })
      map.add(circle)
      ;(window as any).__extraFenceCircles.push(circle)
    }
  })
}

const switchFence = (index: number) => {
  if (index < 0 || index >= fenceList.value.length) return
  currentFenceIndex.value = index
  applyFence(fenceList.value[index])
  drawAllFences()
}

const useCurrentLocationAsFenceCenter = () => {
  if (!latestData.value?.latitude || !latestData.value?.longitude) {
    ElMessage.warning('当前还没有位置数据')
    return
  }
  fenceForm.value.centerLatitude = latestData.value.latitude
  fenceForm.value.centerLongitude = latestData.value.longitude
  drawFence()
}

const setFenceRadius = (radius: number) => {
  fenceForm.value.radiusMeters = radius
  drawFence()
}

const saveFence = async () => {
  if (!deviceInfo.value?.deviceId) {
    ElMessage.warning('设备信息未加载完成')
    return
  }
  if (fenceForm.value.centerLatitude == null || fenceForm.value.centerLongitude == null) {
    ElMessage.warning('请先设置围栏中心')
    return
  }
  try {
    const payload: any = {
      deviceId: deviceInfo.value.deviceId,
      fenceName: fenceForm.value.fenceName,
      centerLatitude: fenceForm.value.centerLatitude,
      centerLongitude: fenceForm.value.centerLongitude,
      radiusMeters: fenceForm.value.radiusMeters,
      enabled: fenceForm.value.enabled
    }
    // 如果当前选中了已有围栏，传id确保更新正确的那一个
    if (fence.value?.id) {
      payload.id = fence.value.id
    }
    const response = await axios.put('/api/fences', payload)
    if (response.data.code === 200) {
      applyFence(response.data.data)
      // 刷新围栏列表
      await loadFence()
      ElMessage.success('电子围栏已保存')
    }
  } catch (error) {
    console.error('保存电子围栏失败', error)
    ElMessage.error('保存电子围栏失败')
  }
}

// 刷新最新位置
const refreshLocation = async () => {
  if (!deviceInfo.value?.deviceId) return
  try {
    const sensorResponse = await axios.get(`/api/sensor-data/latest`, {
      params: {
        deviceId: deviceInfo.value?.deviceId,
        _t: Date.now()
      }
    })
    if (sensorResponse.data.code === 200) {
      latestData.value = sensorResponse.data.data || {}
      deviceStatus.value = '在线'
    }

    const locationResponse = await axios.get(`/api/mini/devices/${deviceInfo.value.deviceId}/location`, {
      params: { _t: Date.now() }
    })
    if (locationResponse.data.code === 200 && locationResponse.data.data?.longitude && locationResponse.data.data?.latitude) {
      const location = locationResponse.data.data
      latestData.value = {
        ...latestData.value,
        longitude: location.longitude,
        latitude: location.latitude,
        createTime: location.updateTime || latestData.value?.createTime
      }
      updateMapMarker(Number(location.longitude), Number(location.latitude))
    } else {
      latestData.value = {
        ...latestData.value,
        longitude: DEFAULT_LOCATION.longitude,
        latitude: DEFAULT_LOCATION.latitude
      }
      updateMapMarker(DEFAULT_LOCATION.longitude, DEFAULT_LOCATION.latitude)
    }
  } catch (error) {
    console.error('刷新位置失败', error)
    latestData.value = {
      ...latestData.value,
      longitude: DEFAULT_LOCATION.longitude,
      latitude: DEFAULT_LOCATION.latitude
    }
    updateMapMarker(DEFAULT_LOCATION.longitude, DEFAULT_LOCATION.latitude)
  }
}

// 检查最新报警
const checkLatestAlarm = async () => {
  if (!deviceInfo.value?.deviceId) return
  try {
    const response = await axios.get(`/api/alarm-records/latest`, {
      params: {
        deviceId: deviceInfo.value?.deviceId,
        _t: Date.now()
      }
    })
    if (response.data.code === 200) {
      const alarm = response.data.data
      if (alarm && String(alarm.status) === '0' && alarm.id !== latestAlarm.value?.id) {
        latestAlarm.value = alarm
        showAlarmNotification(alarm)
      }
    }
  } catch (error) {
    console.error('检查报警失败', error)
  }
}

// 显示报警通知
const showAlarmNotification = (alarm: any) => {
  if (!alarm || String(alarm.status) !== '0') return
  const alarmKey = `${alarm.id ?? 'unknown'}-${alarm.alarmType ?? ''}-${alarm.alarmTime ?? ''}`
  if (notifiedAlarmKeys.has(alarmKey)) return

  const now = Date.now()
  if (now - lastAlarmNotificationAt < ALARM_NOTIFICATION_COOLDOWN_MS) {
    notifiedAlarmKeys.add(alarmKey)
    return
  }

  notifiedAlarmKeys.add(alarmKey)
  lastAlarmNotificationAt = now
  activeAlarmNotification?.close?.()

  let notificationInstance: any = null
  notificationInstance = ElNotification({
    title: '⚠️ 设备报警',
    message: `设备 ${deviceInfo.value.deviceName || deviceInfo.value.deviceId || ''} 发生${alarm.alarmType}，请在右侧“最新报警”中处理。`,
    type: 'error',
    duration: 3500,
    position: 'top-right',
    onClose: () => {
      if (activeAlarmNotification === notificationInstance) {
        activeAlarmNotification = null
      }
    }
  })
  activeAlarmNotification = notificationInstance

  playAlarmSound()
}

// 播放报警音效
const playAlarmSound = () => {
  const now = Date.now()
  if (now - lastAlarmSoundAt < ALARM_NOTIFICATION_COOLDOWN_MS) return
  lastAlarmSoundAt = now
  const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext
  if (!AudioContextClass) return
  const audioContext = new AudioContextClass()
  const oscillator = audioContext.createOscillator()
  const gainNode = audioContext.createGain()
  oscillator.type = 'sine'
  oscillator.frequency.value = 880
  gainNode.gain.value = 0.15
  oscillator.connect(gainNode)
  gainNode.connect(audioContext.destination)
  oscillator.start()
  setTimeout(() => {
    oscillator.stop()
    audioContext.close()
  }, 300)
}

// 处理报警
const handleAlarm = async () => {
  if (!latestAlarm.value) return
  
  try {
    const response = await axios.put(`/api/alarm-records/${latestAlarm.value.id}/handle`)
    if (response.data.code === 200) {
      ElMessage.success('报警已处理')
      latestAlarm.value.status = 1
      activeAlarmNotification?.close?.()
    }
  } catch (error) {
    ElMessage.error('处理报警失败')
  }
}

// WebSocket连接
const connectWebSocket = () => {
  const wsDeviceId = deviceInfo.value?.deviceId
  if (!wsDeviceId) return
  if (isPageUnmounted) return
  const wsBaseUrl = import.meta.env.VITE_WS_BASE_URL
    || (import.meta.env.DEV ? 'ws://localhost:8081' : `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}`)
  const wsUrl = `${wsBaseUrl}/ws/alarm?deviceId=${wsDeviceId}`

  if (ws) {
    ws.close()
    ws = null
  }
  
  ws = new WebSocket(wsUrl)
  
  ws.onopen = () => {
    console.log('WebSocket连接成功')
    ElMessage.success('实时监控已连接')
  }
  
  ws.onmessage = (event) => {
    const message = JSON.parse(event.data)
    console.log('收到WebSocket消息:', message)
    
    if (message.type === 'SENSOR_DATA') {
      latestData.value = message.sensorData
      deviceStatus.value = '在线'
      refreshLocation()
      if (showTrajectory.value) {
        loadTrajectory()
      }
      if (message.fence) {
        applyFenceStatus(message.fence)
      }
    } else if (message.type === 'ALARM') {
      latestAlarm.value = message.alarm
      showAlarmNotification(message.alarm)
    } else if (message.type === 'FENCE_STATUS') {
      applyFenceStatus(message.fence)
    } else if (message.type === 'CROSSING_ASSIST') {
      applyCrossingAssist(message.crossingAssist)
    } else if (message.type === 'CONNECTED') {
      console.log('服务器确认连接:', message.message)
    } else if (message.type === 'PONG') {
    }
  }
  
  ws.onerror = (error) => {
    console.error('WebSocket错误:', error)
  }
  
  ws.onclose = () => {
    if (isPageUnmounted) return
    console.log('WebSocket连接关闭，5秒后重连...')
    deviceStatus.value = '离线'
    wsReconnectTimer = setTimeout(connectWebSocket, 5000)
  }
}

// 页面加载
onMounted(async () => {
  isPageUnmounted = false
  initMap()
  await loadDeviceInfo()
  await refreshLocation()
  await loadCrossingAssist()
  await loadFence()
  if (!fence.value && latestData.value?.latitude && latestData.value?.longitude) {
    fenceForm.value.centerLatitude = latestData.value.latitude
    fenceForm.value.centerLongitude = latestData.value.longitude
    fenceForm.value.enabled = true
    drawFence()
  }
  await checkLatestAlarm()
  
  connectWebSocket()
  
  refreshTimer = setInterval(() => {
    refreshLocation()
    checkLatestAlarm()
    loadCrossingAssist()
  }, 30000)
})

// 页面卸载
onUnmounted(() => {
  isPageUnmounted = true
  if (refreshTimer) clearInterval(refreshTimer)
  if (alarmCheckTimer) clearInterval(alarmCheckTimer)
  if (wsReconnectTimer) clearTimeout(wsReconnectTimer)
  activeAlarmNotification?.close?.()
  if (ws) {
    ws.close()
    ws = null
  }
  if (map) {
    map.destroy()
    map = null
  }
})
</script>

<style scoped>
.monitor-container {
  padding: 20px 24px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  background: #f5f7fb;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
}

.header h2 {
  margin: 0;
  font-size: 28px;
  color: #1f2937;
}

.content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 400px;
  flex: 1;
  gap: 20px;
  overflow: hidden;
  min-height: 0;
}

.map-section {
  position: relative;
  min-width: 0;
  background: #ffffff;
  padding: 12px;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.map-container {
  width: 100%;
  height: 100%;
  min-height: 640px;
  border-radius: 12px;
}

.map-controls {
  position: absolute;
  top: 24px;
  right: 24px;
  z-index: 100;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.info-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  background: #ffffff;
  border-radius: 14px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 88px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.summary-label {
  color: #909399;
  font-size: 12px;
  line-height: 1;
}

.summary-value {
  color: #303133;
  font-size: 14px;
  word-break: break-all;
}

.panel-card {
  flex: 1;
  min-height: 0;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.panel-card :deep(.el-card__body) {
  height: 100%;
  padding: 16px;
}

.monitor-tabs {
  height: 100%;
}

.monitor-tabs :deep(.el-tabs__content) {
  height: calc(100% - 55px);
}

.monitor-tabs :deep(.el-tab-pane) {
  height: 100%;
}

.tab-content {
  height: 100%;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-right: 4px;
}

.section-block {
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 700;
  color: #111827;
  margin-bottom: 12px;
}

.section-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.alarm-header {
  color: #F56C6C;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 8px 0;
  border-bottom: 1px solid #EBEEF5;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item label {
  color: #909399;
  font-weight: 500;
  white-space: nowrap;
}

.info-item span {
  flex: 1;
  text-align: right;
  word-break: break-all;
}

.address-value {
  line-height: 1.6;
}

.battery-low {
  color: #F56C6C;
  font-weight: bold;
}

.battery-medium {
  color: #E6A23C;
  font-weight: bold;
}

.battery-high {
  color: #67C23A;
  font-weight: bold;
}

.alarm-content {
  color: #606266;
}

.alarm-content p {
  margin: 8px 0;
}

.crossing-message {
  line-height: 1.8;
  color: #374151;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
  border: 1px solid #dbeafe;
  border-radius: 10px;
  padding: 14px 16px;
}

.fence-form-row {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.fence-actions {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.radius-presets {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.fence-selector {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}

@media (max-width: 1280px) {
  .content {
    grid-template-columns: minmax(0, 1fr) 360px;
  }
}

@media (max-width: 960px) {
  .monitor-container {
    height: auto;
    min-height: 100vh;
    padding: 16px;
  }

  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header h2 {
    font-size: 24px;
  }

  .content {
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .map-container {
    height: 420px;
    min-height: 420px;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .panel-card {
    min-height: 540px;
  }
}
</style>
