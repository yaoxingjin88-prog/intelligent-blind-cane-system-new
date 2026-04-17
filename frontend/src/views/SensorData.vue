<template>
  <div class="sensor-data-container">
    <div class="page-hero">
      <div>
        <h2>传感器数据</h2>
        <p>聚焦设备轨迹、障碍物距离与跌倒风险，便于快速筛查关键记录</p>
      </div>
      <el-button type="primary" @click="openAddSensorDataDialog">
        <el-icon><Plus /></el-icon>
        添加数据
      </el-button>
    </div>

    <div class="stats-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <span class="stat-label">{{ card.label }}</span>
        <strong class="stat-value">{{ card.value }}</strong>
        <span class="stat-sub">{{ card.sub }}</span>
      </div>
    </div>

    <el-card class="sensor-data-card">
      <template #header>
        <div class="card-header">
          <div>
            <span>数据列表</span>
            <p class="header-subtitle">重点展示设备、障碍物距离、跌倒状态、位置坐标和数据时间</p>
          </div>
          <el-tag type="primary" effect="plain">共 {{ total }} 条记录</el-tag>
        </div>
      </template>
      
      <el-table :data="paginatedSensorData" class="sensor-table" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="设备" min-width="140">
          <template #default="{ row }">
            <div class="device-cell">
              <strong>{{ row.deviceId }}</strong>
              <span>记录编号 #{{ row.id }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="障碍物距离" min-width="160">
          <template #default="{ row }">
            <div class="distance-cell">
              <span class="distance-value">{{ formatDistance(row.obstacleDistance) }}</span>
              <el-tag :type="getDistanceTagType(row.obstacleDistance)" effect="light">
                {{ getDistanceLabel(row.obstacleDistance) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="跌倒状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isFall ? 'danger' : 'success'" effect="light">
              {{ row.isFall ? '已跌倒' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="位置坐标" min-width="220">
          <template #default="{ row }">
            <div class="location-cell">
              <span>纬度：{{ row.latitude ?? '-' }}</span>
              <span>经度：{{ row.longitude ?? '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="dataTime" label="数据时间" min-width="180" />
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-button size="small" type="danger" plain @click="deleteSensorData(row.id)">
              <el-icon><Delete /></el-icon>
              删除
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
    
    <!-- 添加传感器数据对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加传感器数据">
      <el-form :model="addSensorDataForm" :rules="addSensorDataRules" ref="addSensorDataFormRef" label-width="90px">
        <el-form-item label="设备ID" prop="deviceId">
          <el-input v-model="addSensorDataForm.deviceId" placeholder="请输入设备ID" />
        </el-form-item>
        <el-form-item label="障碍物距离" prop="obstacleDistance">
          <el-input v-model="addSensorDataForm.obstacleDistance" placeholder="请输入障碍物距离" type="number" />
        </el-form-item>
        <el-form-item label="是否跌倒" prop="isFall">
          <el-switch v-model="addSensorDataForm.isFall" />
        </el-form-item>
        <el-form-item label="纬度" prop="latitude">
          <el-input v-model="addSensorDataForm.latitude" placeholder="请输入纬度" type="number" step="0.000001" />
        </el-form-item>
        <el-form-item label="经度" prop="longitude">
          <el-input v-model="addSensorDataForm.longitude" placeholder="请输入经度" type="number" step="0.000001" />
        </el-form-item>
        <el-form-item label="数据时间" prop="dataTime">
          <el-date-picker
            v-model="addSensorDataForm.dataTime"
            type="datetime"
            placeholder="请选择数据时间"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="addSensorData">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'

export default {
  name: 'SensorData',
  components: {
    Plus,
    Delete
  },
  setup() {
    const sensorData = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    
    const addDialogVisible = ref(false)
    
    const addSensorDataForm = ref({
      deviceId: '',
      obstacleDistance: '',
      isFall: false,
      latitude: '',
      longitude: '',
      dataTime: ''
    })
    
    const addSensorDataRules = ref({
      deviceId: [{ required: true, message: '请输入设备ID', trigger: 'blur' }],
      obstacleDistance: [{ required: true, message: '请输入障碍物距离', trigger: 'blur' }],
      isFall: [{ required: true, message: '请选择是否跌倒', trigger: 'blur' }],
      latitude: [{ required: true, message: '请输入纬度', trigger: 'blur' }],
      longitude: [{ required: true, message: '请输入经度', trigger: 'blur' }],
      dataTime: [{ required: true, message: '请选择数据时间', trigger: 'blur' }]
    })
    
    const addSensorDataFormRef = ref(null)

    const paginatedSensorData = computed(() => {
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      return sensorData.value.slice(start, end)
    })

    const statCards = computed(() => {
      const fallCount = sensorData.value.filter(item => Boolean(item.isFall)).length
      const deviceCount = new Set(sensorData.value.map(item => item.deviceId).filter(Boolean)).size
      const distanceValues = sensorData.value
        .map(item => Number(item.obstacleDistance))
        .filter(value => !Number.isNaN(value))
      const avgDistance = distanceValues.length
        ? Math.round((distanceValues.reduce((sum, value) => sum + value, 0) / distanceValues.length) * 10) / 10
        : 0
      return [
        { label: '记录总数', value: total.value, sub: '当前传感器历史记录总量' },
        { label: '涉及设备', value: deviceCount, sub: '已有上传记录的设备数量' },
        { label: '跌倒记录', value: fallCount, sub: '需重点关注的异常事件' },
        { label: '平均障碍距离', value: `${avgDistance} cm`, sub: '用于判断整体通行风险水平' }
      ]
    })

    const formatDistance = (distance) => {
      const value = Number(distance)
      return Number.isNaN(value) ? '-' : `${value} cm`
    }

    const getDistanceTagType = (distance) => {
      const value = Number(distance)
      if (Number.isNaN(value)) return 'info'
      if (value <= 30) return 'danger'
      if (value <= 80) return 'warning'
      return 'success'
    }

    const getDistanceLabel = (distance) => {
      const value = Number(distance)
      if (Number.isNaN(value)) return '未知'
      if (value <= 30) return '高风险'
      if (value <= 80) return '需注意'
      return '安全'
    }
    
    const fetchSensorData = async () => {
      try {
        // 调用后端API获取传感器数据列表
        const response = await fetch('/api/sensor-data', {
          cache: 'no-cache' // 禁用缓存，确保获取最新数据
        })
        const data = await response.json()
        sensorData.value = data.data || []
        total.value = sensorData.value.length
        const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))
        if (currentPage.value > maxPage) {
          currentPage.value = maxPage
        }
      } catch (error) {
        console.error('获取传感器数据列表失败:', error)
      }
    }
    
    const openAddSensorDataDialog = () => {
      addSensorDataForm.value = {
        deviceId: '',
        obstacleDistance: '',
        isFall: false,
        latitude: '',
        longitude: '',
        dataTime: ''
      }
      addDialogVisible.value = true
    }
    
    const addSensorData = async () => {
      if (addSensorDataFormRef.value) {
        const valid = await addSensorDataFormRef.value.validate()
        if (valid) {
          try {
            // 转换数据类型和日期格式
            const sensorData = {
              ...addSensorDataForm.value,
              deviceId: addSensorDataForm.value.deviceId.toString(),
              obstacleDistance: parseFloat(addSensorDataForm.value.obstacleDistance),
              isFall: addSensorDataForm.value.isFall === 'true' || addSensorDataForm.value.isFall === true,
              latitude: parseFloat(addSensorDataForm.value.latitude),
              longitude: parseFloat(addSensorDataForm.value.longitude),
              dataTime: new Date(addSensorDataForm.value.dataTime).toISOString().slice(0, 19).replace('T', ' ')
            }
            // 调用后端API添加传感器数据
            const response = await fetch('/api/sensor-data', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify(sensorData)
            })
            const data = await response.json()
            if (data.code === 200) {
              ElMessage.success('添加传感器数据成功')
              addDialogVisible.value = false
              fetchSensorData()
            } else {
              ElMessage.error('添加传感器数据失败: ' + data.msg)
            }
          } catch (error) {
            console.error('添加传感器数据失败:', error)
            ElMessage.error('添加传感器数据失败')
          }
        }
      }
    }
    
    const deleteSensorData = async (id) => {
      ElMessageBox.confirm('确定要删除该传感器数据吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          // 调用后端API删除传感器数据
          const response = await fetch(`/api/sensor-data/${id}`, {
            method: 'DELETE'
          })
          const data = await response.json()
          if (data.code === 200) {
            ElMessage.success('删除传感器数据成功')
            fetchSensorData()
          } else {
            ElMessage.error('删除传感器数据失败: ' + data.msg)
          }
        } catch (error) {
          console.error('删除传感器数据失败:', error)
          ElMessage.error('删除传感器数据失败')
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
    
    onMounted(() => {
      fetchSensorData()
    })
    
    return {
      sensorData,
      total,
      currentPage,
      pageSize,
      paginatedSensorData,
      statCards,
      addDialogVisible,
      addSensorDataForm,
      addSensorDataRules,
      addSensorDataFormRef,
      formatDistance,
      getDistanceTagType,
      getDistanceLabel,
      openAddSensorDataDialog,
      addSensorData,
      deleteSensorData,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.sensor-data-container {
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

.sensor-data-card {
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

.sensor-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
}

.sensor-table :deep(.el-table__row td) {
  padding-top: 14px;
  padding-bottom: 14px;
}

.device-cell,
.distance-cell,
.location-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.device-cell strong,
.distance-value {
  color: #0f172a;
  font-weight: 600;
}

.device-cell span,
.location-cell span {
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
  .sensor-data-container {
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