<template>
  <div class="alarm-records-container">
    <div class="page-hero">
      <div>
        <h2>告警记录</h2>
        <p>统一查看跌倒、低电量与围栏相关告警，并快速处理未完成事件</p>
      </div>
      <div class="hero-actions">
        <el-tag v-if="filterDeviceId" type="warning" effect="plain" closable @close="clearDeviceFilter">
          设备 {{ filterDeviceId }}
        </el-tag>
        <el-button type="primary" @click="openAddAlarmRecordDialog">
          <el-icon><Plus /></el-icon>
          添加告警
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

    <el-card class="app-filter-card" shadow="never">
      <div class="app-filters-row">
        <el-date-picker
          v-model="timeRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD HH:mm:ss"
          :default-time="defaultTimeRange"
          class="filter-time"
          clearable
        />
        <el-select
          v-model="selectedDeviceId"
          placeholder="全部设备"
          clearable
          filterable
          class="filter-device"
        >
          <el-option
            v-for="device in deviceOptions"
            :key="device.deviceId"
            :label="deviceLabel(device)"
            :value="device.deviceId"
          />
        </el-select>
        <el-select
          v-model="selectedAlarmType"
          placeholder="全部类型"
          clearable
          filterable
          class="filter-select"
        >
          <el-option
            v-for="type in alarmTypeOptions"
            :key="type"
            :label="type"
            :value="type"
          />
        </el-select>
        <el-select v-model="timeSort" class="filter-sort">
          <el-option label="时间倒序" value="desc" />
          <el-option label="时间正序" value="asc" />
        </el-select>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </el-card>

    <el-card class="alarm-records-card">
      <template #header>
        <div class="card-header">
          <div>
            <span>{{ listTitle }}</span>
            <p v-if="selectedDeviceId" class="header-subtitle">当前按设备筛选，可点重置或关闭标签清除</p>
          </div>
          <el-tag type="primary" effect="plain">共 {{ total }} 条告警</el-tag>
        </div>
      </template>
      
      <el-table :data="paginatedAlarmRecords" class="alarm-table" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="设备" min-width="150">
          <template #default="{ row }">
            <div class="device-cell">
              <strong>{{ row.deviceId }}</strong>
              <span>记录编号 #{{ row.id }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="告警类型" min-width="220">
          <template #default="{ row }">
            <div class="alarm-type-cell">
              <el-tag :type="getAlarmTypeTagType(row.alarmType)" effect="light">
                {{ row.alarmType || '未知告警' }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="alarmTime" label="告警时间" min-width="180" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" effect="light">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理方" width="120" align="center">
          <template #default="{ row }">
            <span>{{ formatHandledBy(row.handledBy) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button v-if="isUnhandled(row.status)" type="success" link size="small" @click="updateAlarmStatus(row.id, '1')">
              <el-icon><Check /></el-icon>兜底处理
            </el-button>
            <el-button type="danger" link size="small" @click="deleteAlarmRecord(row.id)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 添加告警记录对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加告警记录">
      <el-form :model="addAlarmRecordForm" :rules="addAlarmRecordRules" ref="addAlarmRecordFormRef" label-width="80px">
        <el-form-item label="设备ID" prop="deviceId">
          <el-input v-model="addAlarmRecordForm.deviceId" placeholder="请输入设备ID" />
        </el-form-item>
        <el-form-item label="告警类型" prop="alarmType">
          <el-select v-model="addAlarmRecordForm.alarmType" placeholder="请选择告警类型">
            <el-option label="摔倒报警" value="摔倒报警" />
            <el-option label="电子围栏越界报警" value="电子围栏越界报警" />
            <el-option label="低电量报警" value="低电量报警" />
            <el-option label="SOS求助" value="SOS求助" />
          </el-select>
        </el-form-item>
        <el-form-item label="告警时间" prop="alarmTime">
          <el-date-picker
            v-model="addAlarmRecordForm.alarmTime"
            type="datetime"
            placeholder="请选择告警时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="addAlarmRecordForm.status" placeholder="请选择状态">
            <el-option label="未处理" value="0" />
            <el-option label="已处理" value="1" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="addAlarmRecord">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, watch, onMounted, onUnmounted, onActivated, onDeactivated } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, Check, Delete } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { fetchJson, apiFetch } from '../utils/http'
import { startVisibilityInterval } from '../utils/visibilityInterval'

export default {
  name: 'AlarmRecords',
  components: {
    Plus,
    Check,
    Delete
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    /** 接口原始列表（最近 200 条），筛选在前端完成 */
    const allAlarmRecords = ref([])
    const deviceOptions = ref([])
    const currentPage = ref(1)
    const pageSize = ref(10)
    const timeRange = ref(null)
    /** 日期范围：开始 00:00:00，结束 23:59:59，便于按天筛选 */
    const defaultTimeRange = [
      new Date(2000, 1, 1, 0, 0, 0),
      new Date(2000, 1, 1, 23, 59, 59)
    ]
    const selectedDeviceId = ref('')
    const selectedAlarmType = ref('')
    const timeSort = ref('desc')
    let syncingDeviceFromRoute = false

    const routeDeviceId = computed(() => {
      const raw = route.query.deviceId
      const value = Array.isArray(raw) ? raw[0] : raw
      return value ? String(value).trim() : ''
    })
    const filterDeviceId = computed(() => selectedDeviceId.value || '')

    const addDialogVisible = ref(false)
    
    const addAlarmRecordForm = ref({
      deviceId: '',
      alarmType: '',
      alarmTime: '',
      status: ''
    })
    
    const addAlarmRecordRules = ref({
      deviceId: [{ required: true, message: '请输入设备ID', trigger: 'blur' }],
      alarmType: [{ required: true, message: '请选择告警类型', trigger: 'blur' }],
      alarmTime: [{ required: true, message: '请选择告警时间', trigger: 'blur' }],
      status: [{ required: true, message: '请选择状态', trigger: 'blur' }]
    })
    
    const addAlarmRecordFormRef = ref(null)

    const PRESET_ALARM_TYPES = ['摔倒报警', '电子围栏越界报警', '低电量报警', 'SOS求助']

    const deviceLabel = (device) => {
      if (!device) return ''
      const name = device.deviceName || device.deviceId
      return name === device.deviceId ? device.deviceId : `${name}（${device.deviceId}）`
    }

    const alarmTypeOptions = computed(() => {
      const fromData = allAlarmRecords.value
        .map(item => item.alarmType)
        .filter(Boolean)
      return [...new Set([...PRESET_ALARM_TYPES, ...fromData])]
    })

    const filteredAlarmRecords = computed(() => {
      const [startTime, endTime] = Array.isArray(timeRange.value) ? timeRange.value : [null, null]
      let list = allAlarmRecords.value.filter((record) => {
        if (selectedDeviceId.value && record.deviceId !== selectedDeviceId.value) return false
        if (selectedAlarmType.value && record.alarmType !== selectedAlarmType.value) return false
        const alarmTime = String(record.alarmTime || '')
        if (startTime && alarmTime && alarmTime < startTime) return false
        if (endTime && alarmTime && alarmTime > endTime) return false
        return true
      })
      list = list.slice().sort((a, b) => {
        const left = String(a.alarmTime || '')
        const right = String(b.alarmTime || '')
        const cmp = left.localeCompare(right)
        return timeSort.value === 'asc' ? cmp : -cmp
      })
      return list
    })

    const total = computed(() => filteredAlarmRecords.value.length)

    const listTitle = computed(() => {
      if (selectedDeviceId.value) return `设备 ${selectedDeviceId.value} 的告警`
      return '告警列表'
    })

    const paginatedAlarmRecords = computed(() => {
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      return filteredAlarmRecords.value.slice(start, end)
    })

    const isUnhandled = (status) => String(status) === '0' || status === '未处理'

    const formatHandledBy = (handledBy) => {
      if (handledBy === 'guardian') return '监护人'
      if (handledBy === 'admin') return '管理端'
      return handledBy || '-'
    }

    const getStatusLabel = (status) => {
      if (String(status) === '1' || status === '已处理') return '已处理'
      return '未处理'
    }

    const getStatusTagType = (status) => {
      return isUnhandled(status) ? 'danger' : 'success'
    }

    const getAlarmTypeTagType = (alarmType) => {
      if (!alarmType) return 'info'
      if (String(alarmType).includes('摔') || String(alarmType).includes('跌倒')) return 'danger'
      if (String(alarmType).includes('低电')) return 'warning'
      if (String(alarmType).includes('围栏')) return 'warning'
      if (String(alarmType).includes('SOS')) return 'danger'
      return 'info'
    }

    const statCards = computed(() => {
      const list = filteredAlarmRecords.value
      const unhandledCount = list.filter(record => isUnhandled(record.status)).length
      const handledCount = list.length - unhandledCount
      const deviceCount = new Set(list.map(record => record.deviceId).filter(Boolean)).size
      const latestAlarmTime = list[0]?.alarmTime || '暂无记录'
      return [
        { label: '告警总数', value: list.length, sub: '当前筛选条件下的告警数量' },
        { label: '未处理告警', value: unhandledCount, sub: '需要尽快处理的事件数量' },
        { label: '已处理告警', value: handledCount, sub: '已完成处置的历史告警' },
        { label: '涉及设备', value: deviceCount, sub: `最近告警时间 ${latestAlarmTime}` }
      ]
    })

    const fetchDevices = async () => {
      try {
        const data = await fetchJson('/api/devices', { retries: 1, retryDelay: 500 })
        deviceOptions.value = data.data || []
      } catch (error) {
        console.error('获取设备列表失败:', error)
        deviceOptions.value = []
      }
    }
    
    const fetchAlarmRecords = async () => {
      try {
        // 管理页取最近 200 条，设备/类型/时间在前端筛选
        const data = await fetchJson('/api/alarm-records?limit=200', {
          retries: 2,
          retryDelay: 700
        })
        allAlarmRecords.value = data.data || []
        const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))
        if (currentPage.value > maxPage) {
          currentPage.value = maxPage
        }
      } catch (error) {
        console.error('获取告警记录列表失败:', error)
        ElMessage.error('获取告警记录失败，请稍后重试')
      }
    }

    const syncDeviceQuery = (deviceId) => {
      const nextQuery = { ...route.query }
      if (deviceId) {
        nextQuery.deviceId = deviceId
      } else {
        delete nextQuery.deviceId
      }
      router.replace({ path: '/alarm-records', query: nextQuery })
    }

    const clearDeviceFilter = () => {
      selectedDeviceId.value = ''
      syncDeviceQuery('')
    }

    const resetFilters = () => {
      timeRange.value = null
      selectedAlarmType.value = ''
      timeSort.value = 'desc'
      currentPage.value = 1
      clearDeviceFilter()
    }
    
    const openAddAlarmRecordDialog = () => {
      addAlarmRecordForm.value = {
        deviceId: '',
        alarmType: '',
        alarmTime: '',
        status: '0'
      }
      addDialogVisible.value = true
    }
    
    const addAlarmRecord = async () => {
      if (addAlarmRecordFormRef.value) {
        const valid = await addAlarmRecordFormRef.value.validate()
        if (valid) {
          try {
            // 转换数据类型和日期格式
            const alarmData = {
              ...addAlarmRecordForm.value,
              deviceId: addAlarmRecordForm.value.deviceId.toString(),
              alarmTime: new Date(addAlarmRecordForm.value.alarmTime).toISOString().slice(0, 19).replace('T', ' ')
            }
            // 调用后端API添加告警记录
            const response = await apiFetch('/api/alarm-records', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify(alarmData)
            })
            const data = await response.json()
            if (data.code === 200) {
              ElMessage.success('添加告警记录成功')
              addDialogVisible.value = false
              fetchAlarmRecords()
            } else {
              ElMessage.error('添加告警记录失败: ' + data.msg)
            }
          } catch (error) {
            console.error('添加告警记录失败:', error)
            ElMessage.error('添加告警记录失败')
          }
        }
      }
    }
    
    const updateAlarmStatus = async (id, status) => {
      try {
        // 调用后端API更新告警状态
        const response = await apiFetch(`/api/alarm-records/status/${id}?status=${status}`, {
          method: 'PUT'
        })
        const data = await response.json()
        if (data.code === 200) {
          ElMessage.success('更新告警状态成功')
          fetchAlarmRecords()
          window.dispatchEvent(new CustomEvent('alarm-records-changed'))
        } else {
          ElMessage.error('更新告警状态失败: ' + data.msg)
        }
      } catch (error) {
        console.error('更新告警状态失败:', error)
        ElMessage.error('更新告警状态失败')
      }
    }
    
    const deleteAlarmRecord = async (id) => {
      ElMessageBox.confirm('确定要删除该告警记录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          // 调用后端API删除告警记录
          const response = await apiFetch(`/api/alarm-records/${id}`, {
            method: 'DELETE'
          })
          const data = await response.json()
          if (data.code === 200) {
            ElMessage.success('删除告警记录成功')
            fetchAlarmRecords()
            window.dispatchEvent(new CustomEvent('alarm-records-changed'))
          } else {
            ElMessage.error('删除告警记录失败: ' + data.msg)
          }
        } catch (error) {
          console.error('删除告警记录失败:', error)
          ElMessage.error('删除告警记录失败')
        }
      }).catch(() => {
        // 取消删除
      })
    }
    
    const handleSizeChange = (size) => {
      pageSize.value = size
      currentPage.value = 1
    }
    
    const handleCurrentChange = (current) => {
      currentPage.value = current
    }
    
    let stopAlarmPollingFn = null
    const onAlarmsChanged = () => fetchAlarmRecords()
    const startAlarmPolling = () => {
      if (stopAlarmPollingFn) return
      stopAlarmPollingFn = startVisibilityInterval(fetchAlarmRecords, 30000)
    }
    const stopAlarmPolling = () => {
      if (stopAlarmPollingFn) {
        stopAlarmPollingFn()
        stopAlarmPollingFn = null
      }
    }
    watch(routeDeviceId, (value) => {
      if (selectedDeviceId.value === value) return
      syncingDeviceFromRoute = true
      selectedDeviceId.value = value
      currentPage.value = 1
      Promise.resolve().then(() => {
        syncingDeviceFromRoute = false
      })
    }, { immediate: true })

    watch(selectedDeviceId, (value) => {
      currentPage.value = 1
      if (syncingDeviceFromRoute) return
      if ((value || '') !== routeDeviceId.value) {
        syncDeviceQuery(value || '')
      }
    })

    watch([timeRange, selectedAlarmType, timeSort], () => {
      currentPage.value = 1
    })

    onMounted(async () => {
      await Promise.all([fetchDevices(), fetchAlarmRecords()])
      startAlarmPolling()
      window.addEventListener('alarm-records-changed', onAlarmsChanged)
    })
    onActivated(() => {
      fetchAlarmRecords()
      fetchDevices()
      startAlarmPolling()
    })
    onDeactivated(() => {
      stopAlarmPolling()
    })
    onUnmounted(() => {
      stopAlarmPolling()
      window.removeEventListener('alarm-records-changed', onAlarmsChanged)
    })
    
    return {
      filterDeviceId,
      clearDeviceFilter,
      resetFilters,
      timeRange,
      defaultTimeRange,
      selectedDeviceId,
      selectedAlarmType,
      timeSort,
      deviceOptions,
      alarmTypeOptions,
      deviceLabel,
      listTitle,
      total,
      currentPage,
      pageSize,
      paginatedAlarmRecords,
      statCards,
      addDialogVisible,
      addAlarmRecordForm,
      addAlarmRecordRules,
      addAlarmRecordFormRef,
      isUnhandled,
      formatHandledBy,
      getStatusLabel,
      getStatusTagType,
      getAlarmTypeTagType,
      openAddAlarmRecordDialog,
      addAlarmRecord,
      updateAlarmStatus,
      deleteAlarmRecord,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
  .alarm-records-container {
    width: 100%;
    padding: 24px;
    min-height: 100%;
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
    align-items: center;
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

  .alarm-records-card {
    border-radius: 18px;
    border: 1px solid rgba(148, 163, 184, 0.16);
    box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
    overflow: hidden;
    background-color: rgba(255, 255, 255, 0.96);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    padding: 18px 22px 16px;
    border-bottom: 1px solid #f1f5f9;
  }

  .card-header span {
    font-size: 17px;
    font-weight: 600;
    color: #1e293b;
  }

  .header-subtitle {
    margin: 6px 0 0;
    font-size: 12px;
    color: #64748b;
    line-height: 1.6;
  }

  .alarm-table :deep(.el-table__header th) {
    background: #f8fafc;
    color: #475569;
    font-weight: 600;
  }

  .alarm-table :deep(.el-table__row td) {
    padding-top: 14px;
    padding-bottom: 14px;
  }

  .device-cell,
  .alarm-type-cell {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .device-cell strong {
    color: #0f172a;
  }

  .device-cell span {
    font-size: 12px;
    color: #64748b;
  }

  .pagination {
    margin-top: 0;
    display: flex;
    justify-content: flex-end;
    padding: 16px 22px;
    border-top: 1px solid #f1f5f9;
  }

  @media (max-width: 1200px) {
    .stats-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 768px) {
    .alarm-records-container {
      padding: 16px;
    }

    .page-hero {
      flex-direction: column;
      align-items: flex-start;
    }

    .stats-grid {
      grid-template-columns: 1fr;
    }

    .card-header {
      flex-direction: column;
    }

  }
</style>