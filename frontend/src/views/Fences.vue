<template>
  <div class="fences-page">
    <div class="page-hero">
      <div>
        <h2>电子围栏管理</h2>
        <p>集中查看所有设备围栏配置、启用状态与越界情况</p>
      </div>
      <div class="hero-actions">
        <el-button @click="router.push('/trajectory-playback')">查看轨迹回放</el-button>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建围栏
        </el-button>
      </div>
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
        <el-input v-model="keyword" placeholder="搜索设备ID / 围栏名称 / 用户名" clearable class="filter-item" />
        <el-select v-model="statusFilter" class="filter-item">
          <el-option label="全部启用状态" value="all" />
          <el-option label="仅启用" value="enabled" />
          <el-option label="仅停用" value="disabled" />
        </el-select>
        <el-select v-model="boundaryFilter" class="filter-item">
          <el-option label="全部边界状态" value="all" />
          <el-option label="仅已越界" value="outside" />
          <el-option label="仅安全区内" value="inside" />
        </el-select>
        <el-button @click="refreshData">刷新</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div>
            <span>围栏列表</span>
            <p class="header-subtitle">支持统一编辑围栏名称、半径、中心点与启用状态</p>
          </div>
          <el-tag type="primary" effect="plain">{{ filteredFences.length }} 条记录</el-tag>
        </div>
      </template>

      <div class="fence-table-wrap">
      <el-table :data="filteredFences" class="fence-table" stripe>
        <el-table-column prop="deviceId" label="设备ID" min-width="110" show-overflow-tooltip />
        <el-table-column label="关联用户" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.userName || '未关联用户' }}
          </template>
        </el-table-column>
        <el-table-column label="围栏名称" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.fenceName || '未配置围栏' }}
          </template>
        </el-table-column>
        <el-table-column label="围栏半径" width="90" align="center">
          <template #default="{ row }">
            {{ row.hasFence ? `${Number(row.radiusMeters || 0)} m` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="中心坐标" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatCoordinate(row.centerLongitude, row.centerLatitude) }}
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="92" align="center">
          <template #default="{ row }">
            <el-tag :type="row.hasFence ? (row.enabled ? 'success' : 'info') : 'warning'" effect="light">
              {{ row.hasFence ? (row.enabled ? '启用中' : '已停用') : '未配置' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="边界状态" width="92" align="center">
          <template #default="{ row }">
            <el-tag :type="!row.hasFence ? 'info' : (String(row.lastStatus).toUpperCase() === 'OUTSIDE' ? 'danger' : 'success')" effect="light">
              {{ !row.hasFence ? '未配置' : (String(row.lastStatus).toUpperCase() === 'OUTSIDE' ? '已越界' : '安全区内') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="145" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.updatedAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="{ row }">
            <div class="action-cell">
              <div class="action-cell__links">
                <el-button v-if="!row.hasFence" type="primary" link size="small" @click="quickCreateFence(row)">一键启用</el-button>
                <el-button type="primary" link size="small" @click="openEditDialog(row)">{{ row.hasFence ? '编辑' : '高级配置' }}</el-button>
                <el-button type="success" link size="small" @click="goToMonitor(row)">监控详情</el-button>
              </div>
              <el-button type="warning" plain size="small" class="action-cell__playback" @click="goToPlayback(row)">轨迹回放</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新建围栏' : '编辑围栏'" width="860px">
      <el-form :model="fenceForm" label-width="90px">
        <el-form-item label="设备">
          <el-select v-model="fenceForm.deviceId" placeholder="请选择设备" filterable :disabled="dialogMode === 'edit'" style="width: 100%">
            <el-option
              v-for="device in deviceOptions"
              :key="device.id"
              :label="`${device.deviceName || device.deviceId}（${device.deviceId}）`"
              :value="device.deviceId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="围栏名称">
          <el-input v-model="fenceForm.fenceName" placeholder="请输入围栏名称" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number v-model="fenceForm.centerLongitude" :precision="6" :step="0.0001" style="width: 100%" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input-number v-model="fenceForm.centerLatitude" :precision="6" :step="0.0001" style="width: 100%" />
        </el-form-item>
        <el-form-item label="半径">
          <el-input-number v-model="fenceForm.radiusMeters" :min="100" :max="5000" :step="50" style="width: 100%" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="fenceForm.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="地图选点">
          <div class="fence-map-panel">
            <div class="fence-map-toolbar">
              <span>点击地图即可设置围栏中心，圆形范围会随半径实时预览</span>
              <div class="fence-map-actions">
                <el-button text type="primary" @click="fillFenceCenterFromLatestLocation">使用最新位置</el-button>
                <el-button text @click="centerFenceMapOnCurrentValue">定位到当前中心</el-button>
              </div>
            </div>
            <div ref="fenceMapContainerRef" class="fence-map-container"></div>
          </div>
        </el-form-item>
        <el-form-item>
          <div class="location-helper">
            <span>{{ locationHint }}</span>
            <el-button text type="primary" @click="fillFenceCenterFromLatestLocation">使用最新位置</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveFence">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from 'axios'
import { ensureAmap } from '../utils/amap'

defineOptions({ name: 'Fences' })

const router = useRouter()
const devices = ref([])
const fences = ref([])
const keyword = ref('')
const statusFilter = ref('all')
const boundaryFilter = ref('all')
const dialogVisible = ref(false)
const dialogMode = ref('create')
const fenceMapContainerRef = ref(null)
const fenceForm = ref({
  deviceId: '',
  fenceName: '安全活动区',
  centerLatitude: null,
  centerLongitude: null,
  radiusMeters: 300,
  enabled: true,
  lastStatus: 'INSIDE'
})
const locationHint = ref('如不手动修改，系统会优先使用设备最新位置作为围栏中心')

let fenceMap = null
let fenceMapMarker = null
let fenceMapCircle = null

const fenceRows = computed(() => {
  // 按设备分组，一个设备可能有多个围栏
  const deviceFenceMap = new Map()
  fences.value.forEach(item => {
    if (!deviceFenceMap.has(item.deviceId)) {
      deviceFenceMap.set(item.deviceId, [])
    }
    deviceFenceMap.get(item.deviceId).push(item)
  })
  
  const rows = []
  devices.value.forEach(device => {
    const deviceFences = deviceFenceMap.get(device.deviceId)
    if (deviceFences && deviceFences.length > 0) {
      deviceFences.forEach(fence => {
        rows.push({
          ...fence,
          userName: device.userName || '',
          hasFence: true
        })
      })
    } else {
      rows.push({
        id: `device-${device.id}`,
        deviceId: device.deviceId,
        userName: device.userName || '',
        fenceName: '',
        centerLatitude: null,
        centerLongitude: null,
        radiusMeters: null,
        enabled: false,
        lastStatus: '',
        updatedAt: '',
        hasFence: false
      })
    }
  })
  return rows
})

const deviceOptions = computed(() => {
  if (dialogMode.value === 'edit') {
    return devices.value
  }
  const existingIds = new Set(fences.value.map(item => item.deviceId))
  return devices.value.filter(device => !existingIds.has(device.deviceId))
})

const filteredFences = computed(() => fenceRows.value.filter((item) => {
  const keywordValue = keyword.value.trim().toLowerCase()
  const matchesKeyword = !keywordValue || [item.deviceId, item.fenceName, item.userName]
    .some(value => String(value || '').toLowerCase().includes(keywordValue))
  const matchesStatus = statusFilter.value === 'all'
    || (statusFilter.value === 'enabled' && item.hasFence && item.enabled)
    || (statusFilter.value === 'disabled' && item.hasFence && !item.enabled)
  const normalizedStatus = String(item.lastStatus || '').toUpperCase()
  const matchesBoundary = boundaryFilter.value === 'all'
    || (boundaryFilter.value === 'outside' && item.hasFence && normalizedStatus === 'OUTSIDE')
    || (boundaryFilter.value === 'inside' && item.hasFence && normalizedStatus !== 'OUTSIDE')
  return matchesKeyword && matchesStatus && matchesBoundary
}))

const statCards = computed(() => {
  const enabledCount = fences.value.filter(item => item.enabled).length
  const outsideCount = fences.value.filter(item => String(item.lastStatus).toUpperCase() === 'OUTSIDE').length
  const unconfiguredCount = Math.max(devices.value.length - fences.value.length, 0)
  return [
    { label: '围栏总数', value: fences.value.length, sub: '已创建的电子围栏配置' },
    { label: '启用围栏', value: enabledCount, sub: `停用 ${fences.value.length - enabledCount} 条` },
    { label: '越界设备', value: outsideCount, sub: '最近一次判定为越界的设备' },
    { label: '待配置设备', value: unconfiguredCount, sub: `设备总数 ${devices.value.length} 台` }
  ]
})

const formatCoordinate = (lng, lat) => {
  if (lng == null || lat == null) return '-'
  return `${Number(lng).toFixed(4)}, ${Number(lat).toFixed(4)}`
}

const syncFenceMapPreview = (fitView = false) => {
  if (!fenceMap || !window.AMap) return
  if (fenceForm.value.centerLongitude == null || fenceForm.value.centerLatitude == null) {
    if (fenceMapMarker) {
      fenceMap.remove(fenceMapMarker)
      fenceMapMarker = null
    }
    if (fenceMapCircle) {
      fenceMap.remove(fenceMapCircle)
      fenceMapCircle = null
    }
    return
  }

  const center = [Number(fenceForm.value.centerLongitude), Number(fenceForm.value.centerLatitude)]
  if (!fenceMapMarker) {
    fenceMapMarker = new window.AMap.Marker({
      position: center,
      offset: new window.AMap.Pixel(-10, -10)
    })
    fenceMap.add(fenceMapMarker)
  } else {
    fenceMapMarker.setPosition(center)
  }

  if (!fenceMapCircle) {
    fenceMapCircle = new window.AMap.Circle({
      center,
      radius: Number(fenceForm.value.radiusMeters || 300),
      strokeColor: '#2563eb',
      strokeWeight: 2,
      strokeOpacity: 0.85,
      fillColor: '#60a5fa',
      fillOpacity: 0.18
    })
    fenceMap.add(fenceMapCircle)
  } else {
    fenceMapCircle.setCenter(center)
    fenceMapCircle.setRadius(Number(fenceForm.value.radiusMeters || 300))
  }

  if (fitView) {
    fenceMap.setCenter(center)
    fenceMap.setZoom(16)
  }
}

const centerFenceMapOnCurrentValue = () => {
  if (!fenceMap || fenceForm.value.centerLongitude == null || fenceForm.value.centerLatitude == null) return
  fenceMap.setCenter([Number(fenceForm.value.centerLongitude), Number(fenceForm.value.centerLatitude)])
  fenceMap.setZoom(16)
}

const initFenceMap = async () => {
  if (!dialogVisible.value) return
  await nextTick()
  try {
    await ensureAmap()
  } catch (error) {
    console.error(error)
    ElMessage.error('高德地图加载失败')
    return
  }
  if (!window.AMap || !fenceMapContainerRef.value) return
  if (!fenceMap) {
    fenceMap = new window.AMap.Map(fenceMapContainerRef.value, {
      zoom: 13,
      center: [116.4074, 39.9042],
      viewMode: '2D'
    })
    fenceMap.on('click', (event) => {
      fenceForm.value.centerLongitude = Number(event.lnglat.lng.toFixed(6))
      fenceForm.value.centerLatitude = Number(event.lnglat.lat.toFixed(6))
      locationHint.value = `已通过地图选点设置中心：${formatCoordinate(event.lnglat.lng, event.lnglat.lat)}`
      syncFenceMapPreview(true)
    })
  } else {
    fenceMap.resize()
  }
  syncFenceMapPreview(true)
}

const fillFenceCenterFromLatestLocation = async () => {
  if (!fenceForm.value.deviceId) {
    ElMessage.warning('请先选择设备')
    return false
  }
  try {
    const response = await axios.get('/api/sensor-data/latest', {
      params: {
        deviceId: fenceForm.value.deviceId,
        _t: Date.now()
      }
    })
    if (response.data.code === 200 && response.data.data?.latitude != null && response.data.data?.longitude != null) {
      fenceForm.value.centerLatitude = response.data.data.latitude
      fenceForm.value.centerLongitude = response.data.data.longitude
      locationHint.value = `已自动带入最新位置：${formatCoordinate(response.data.data.longitude, response.data.data.latitude)}`
      syncFenceMapPreview(true)
      return true
    }
    locationHint.value = '当前设备暂无最新位置数据，请手动填写经纬度'
    ElMessage.warning('当前设备暂无可用位置数据')
    return false
  } catch (error) {
    console.error('获取设备最新位置失败', error)
    locationHint.value = '读取最新位置失败，请手动填写经纬度'
    ElMessage.error('读取设备最新位置失败')
    return false
  }
}

const openCreateDialog = () => {
  fenceForm.value = {
    deviceId: '',
    fenceName: '安全活动区',
    centerLatitude: null,
    centerLongitude: null,
    radiusMeters: 300,
    enabled: true,
    lastStatus: 'INSIDE'
  }
  locationHint.value = '如不手动修改，系统会优先使用设备最新位置作为围栏中心'
  dialogMode.value = 'create'
  dialogVisible.value = true
  initFenceMap()
}

const openEditDialog = async (row) => {
  fenceForm.value = {
    deviceId: row.deviceId,
    fenceName: row.fenceName || '安全活动区',
    centerLatitude: row.centerLatitude,
    centerLongitude: row.centerLongitude,
    radiusMeters: Number(row.radiusMeters || 300),
    enabled: !!row.enabled,
    lastStatus: row.lastStatus || 'INSIDE'
  }
  dialogMode.value = row.hasFence ? 'edit' : 'create'
  locationHint.value = row.hasFence
    ? '如需重设围栏中心，可一键带入设备最新位置'
    : '未配置围栏时，建议直接使用设备最新位置作为中心'
  dialogVisible.value = true
  await initFenceMap()
  if (!row.hasFence) {
    await fillFenceCenterFromLatestLocation()
  }
}

const fetchDevices = async () => {
  try {
    const response = await axios.get('/api/devices', { params: { _t: Date.now() } })
    if (response.data.code === 200) {
      devices.value = response.data.data || []
    }
  } catch (error) {
    console.error('获取设备失败', error)
    devices.value = []
  }
}

const fetchFences = async () => {
  try {
    const response = await axios.get('/api/fences/all', { params: { _t: Date.now() } })
    if (response.data.code === 200) {
      const deviceMap = new Map(devices.value.map(device => [device.deviceId, device]))
      fences.value = (response.data.data || []).map((item) => ({
        ...item,
        userName: deviceMap.get(item.deviceId)?.userName || ''
      }))
    }
  } catch (error) {
    if (error?.response?.status === 404) {
      try {
        const deviceMap = new Map(devices.value.map(device => [device.deviceId, device]))
        const results = await Promise.allSettled(
          devices.value.map(device => axios.get('/api/fences', {
            params: {
              deviceId: device.deviceId,
              _t: Date.now()
            }
          }))
        )
        fences.value = results
          .filter(result => result.status === 'fulfilled' && result.value.data.code === 200 && result.value.data.data)
          .flatMap(result => {
            const data = result.value.data.data
            const items = Array.isArray(data) ? data : [data]
            return items.map(item => ({
              ...item,
              userName: deviceMap.get(item.deviceId)?.userName || ''
            }))
          })
        return
      } catch (fallbackError) {
        console.error('获取围栏失败，回退查询也失败', fallbackError)
        fences.value = []
        return
      }
    }
    console.error('获取围栏失败', error)
    fences.value = []
  }
}

const refreshData = async () => {
  await fetchDevices()
  await fetchFences()
}

const quickCreateFence = async (row) => {
  try {
    const latestResponse = await axios.get('/api/sensor-data/latest', {
      params: {
        deviceId: row.deviceId,
        _t: Date.now()
      }
    })

    const latestData = latestResponse.data.code === 200 ? latestResponse.data.data : null
    if (!latestData || latestData.latitude == null || latestData.longitude == null) {
      ElMessage.warning('该设备暂无最新位置，建议进入高级配置手动设置围栏中心')
      openEditDialog(row)
      return
    }

    const response = await axios.put('/api/fences', {
      deviceId: row.deviceId,
      fenceName: '安全活动区',
      centerLatitude: latestData.latitude,
      centerLongitude: latestData.longitude,
      radiusMeters: 300,
      enabled: true,
      lastStatus: 'INSIDE'
    })

    if (response.data.code === 200) {
      ElMessage.success(`已为 ${row.deviceId} 一键启用默认围栏`)
      await refreshData()
    }
  } catch (error) {
    console.error('一键启用围栏失败', error)
    ElMessage.error('一键启用围栏失败')
  }
}

const saveFence = async () => {
  if (!fenceForm.value.deviceId) {
    ElMessage.warning('请先选择设备')
    return
  }
  if (fenceForm.value.centerLatitude == null || fenceForm.value.centerLongitude == null) {
    const filled = await fillFenceCenterFromLatestLocation()
    if (!filled) {
      ElMessage.warning('请填写围栏中心经纬度')
      return
    }
  }
  try {
    const response = await axios.put('/api/fences', {
      deviceId: fenceForm.value.deviceId,
      fenceName: fenceForm.value.fenceName,
      centerLatitude: fenceForm.value.centerLatitude,
      centerLongitude: fenceForm.value.centerLongitude,
      radiusMeters: fenceForm.value.radiusMeters,
      enabled: fenceForm.value.enabled,
      lastStatus: fenceForm.value.lastStatus
    })
    if (response.data.code === 200) {
      ElMessage.success(dialogMode.value === 'create' ? '围栏创建成功' : '围栏更新成功')
      dialogVisible.value = false
      refreshData()
    }
  } catch (error) {
    console.error('保存围栏失败', error)
    ElMessage.error('保存围栏失败')
  }
}

const goToMonitor = (row) => {
  const targetDevice = devices.value.find(device => device.deviceId === row.deviceId)
  if (targetDevice?.id) {
    router.push(`/monitor/${targetDevice.id}`)
    return
  }
  ElMessage.warning('未找到对应设备详情')
}

const goToPlayback = (row) => {
  router.push({
    path: '/trajectory-playback',
    query: {
      deviceId: row.deviceId
    }
  })
}

onMounted(() => {
  refreshData()
})

watch(dialogVisible, (visible) => {
  if (visible) {
    initFenceMap()
  }
})

watch(() => [fenceForm.value.centerLongitude, fenceForm.value.centerLatitude, fenceForm.value.radiusMeters], () => {
  syncFenceMapPreview()
})

onUnmounted(() => {
  if (fenceMap) {
    fenceMap.destroy()
    fenceMap = null
  }
})
</script>

<style scoped>
.fences-page {
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

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
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

.stat-card,
.filter-card,
.table-card {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.96);
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
  font-size: 28px;
  color: #0f172a;
}

.stat-sub {
  font-size: 12px;
  color: #94a3b8;
}

.filters-row {
  display: grid;
  grid-template-columns: 1.4fr repeat(2, minmax(0, 0.9fr)) auto;
  gap: 12px;
}

.filter-item {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.header-subtitle {
  margin: 6px 0 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
}

.location-helper {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  color: #64748b;
  font-size: 12px;
}

.fence-map-panel {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.fence-map-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
}

.fence-map-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.fence-map-container {
  width: 100%;
  height: 320px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #dbeafe;
}

.fence-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
}

.fence-table-wrap {
  width: 100%;
  overflow-x: auto;
}

.fence-table {
  min-width: 1120px;
}

.action-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.action-cell__links {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0 6px;
  line-height: 1.2;
}

.action-cell__playback {
  min-width: 76px;
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filters-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .fences-page {
    padding: 16px;
  }

  .page-hero,
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .location-helper {
    flex-direction: column;
    align-items: flex-start;
  }

  .fence-map-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-grid,
  .filters-row {
    grid-template-columns: 1fr;
  }
}
</style>
