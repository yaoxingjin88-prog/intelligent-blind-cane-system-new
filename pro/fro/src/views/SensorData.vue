<template>
  <div class="sensor-data-container">
    <el-card class="sensor-data-card">
      <template #header>
        <div class="card-header">
          <span>传感器数据</span>
          <el-button type="primary" @click="openAddSensorDataDialog">
            <el-icon><Plus /></el-icon>
            添加数据
          </el-button>
        </div>
      </template>
      
      <el-table :data="sensorData" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="deviceId" label="设备ID" />
        <el-table-column prop="obstacleDistance" label="障碍物距离" />
        <el-table-column prop="isFall" label="是否跌倒" />
        <el-table-column prop="latitude" label="纬度" />
        <el-table-column prop="longitude" label="经度" />
        <el-table-column prop="temperature" label="温度" />
        <el-table-column prop="humidity" label="湿度" />
        <el-table-column prop="dataTime" label="数据时间" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="deleteSensorData(row.id)">
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
        <el-form-item label="温度" prop="temperature">
          <el-input v-model="addSensorDataForm.temperature" placeholder="请输入温度" type="number" />
        </el-form-item>
        <el-form-item label="湿度" prop="humidity">
          <el-input v-model="addSensorDataForm.humidity" placeholder="请输入湿度" type="number" />
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
import { ref, onMounted } from 'vue'
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
      temperature: '',
      humidity: '',
      dataTime: ''
    })
    
    const addSensorDataRules = ref({
      deviceId: [{ required: true, message: '请输入设备ID', trigger: 'blur' }],
      obstacleDistance: [{ required: true, message: '请输入障碍物距离', trigger: 'blur' }],
      isFall: [{ required: true, message: '请选择是否跌倒', trigger: 'blur' }],
      latitude: [{ required: true, message: '请输入纬度', trigger: 'blur' }],
      longitude: [{ required: true, message: '请输入经度', trigger: 'blur' }],
      temperature: [{ required: true, message: '请输入温度', trigger: 'blur' }],
      humidity: [{ required: true, message: '请输入湿度', trigger: 'blur' }],
      dataTime: [{ required: true, message: '请选择数据时间', trigger: 'blur' }]
    })
    
    const addSensorDataFormRef = ref(null)
    
    const fetchSensorData = async () => {
      try {
        // 调用后端API获取传感器数据列表
        const response = await fetch('/api/sensor-data', {
          cache: 'no-cache' // 禁用缓存，确保获取最新数据
        })
        const data = await response.json()
        sensorData.value = data.data || []
        total.value = sensorData.value.length
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
        temperature: '',
        humidity: '',
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
              temperature: parseFloat(addSensorDataForm.value.temperature),
              humidity: parseFloat(addSensorDataForm.value.humidity),
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
      fetchSensorData()
    }
    
    const handleCurrentChange = (current) => {
      currentPage.value = current
      fetchSensorData()
    }
    
    onMounted(() => {
      fetchSensorData()
    })
    
    return {
      sensorData,
      total,
      currentPage,
      pageSize,
      addDialogVisible,
      addSensorDataForm,
      addSensorDataRules,
      addSensorDataFormRef,
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
}

.sensor-data-card {
  border-radius: 14px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  margin-bottom: 20px;
  overflow: hidden;
  background-color: #ffffff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 22px;
  border-bottom: 1px solid #f1f5f9;
}

.card-header span {
  font-size: 17px;
  font-weight: 600;
  color: #1e293b;
}

.pagination {
  margin-top: 0;
  display: flex;
  justify-content: flex-end;
  padding: 16px 22px;
  border-top: 1px solid #f1f5f9;
}
</style>