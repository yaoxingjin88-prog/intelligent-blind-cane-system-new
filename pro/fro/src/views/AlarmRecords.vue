<template>
  <div class="alarm-records-container">
    <el-card class="alarm-records-card">
      <template #header>
        <div class="card-header">
          <span>告警记录</span>
          <el-button type="primary" @click="openAddAlarmRecordDialog">
            <el-icon><Plus /></el-icon>
            添加告警
          </el-button>
        </div>
      </template>
      
      <el-table :data="alarmRecords" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="deviceId" label="设备ID" />
        <el-table-column prop="alarmType" label="告警类型" />
        <el-table-column prop="alarmTime" label="告警时间" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button type="success" link size="small" @click="updateAlarmStatus(row.id, '已处理')">
              <el-icon><Check /></el-icon>已处理
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
            <el-option label="跌倒" value="跌倒" />
            <el-option label="障碍物" value="障碍物" />
            <el-option label="低电量" value="低电量" />
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
            <el-option label="未处理" value="未处理" />
            <el-option label="已处理" value="已处理" />
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
import { ref, onMounted } from 'vue'
import { Plus, Check, Delete } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'

export default {
  name: 'AlarmRecords',
  components: {
    Plus,
    Check,
    Delete
  },
  setup() {
    const alarmRecords = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    
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
    
    const fetchAlarmRecords = async () => {
      try {
        // 调用后端API获取告警记录列表
        const response = await fetch('/api/alarm-records', {
          cache: 'no-cache' // 禁用缓存，确保获取最新数据
        })
        const data = await response.json()
        alarmRecords.value = data.data || []
        total.value = alarmRecords.value.length
      } catch (error) {
        console.error('获取告警记录列表失败:', error)
      }
    }
    
    const openAddAlarmRecordDialog = () => {
      addAlarmRecordForm.value = {
        deviceId: '',
        alarmType: '',
        alarmTime: '',
        status: ''
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
            const response = await fetch('/api/alarm-records', {
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
        const response = await fetch(`/api/alarm-records/status/${id}?status=${status}`, {
          method: 'PUT'
        })
        const data = await response.json()
        if (data.code === 200) {
          ElMessage.success('更新告警状态成功')
          fetchAlarmRecords()
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
          const response = await fetch(`/api/alarm-records/${id}`, {
            method: 'DELETE'
          })
          const data = await response.json()
          if (data.code === 200) {
            ElMessage.success('删除告警记录成功')
            fetchAlarmRecords()
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
      fetchAlarmRecords()
    }
    
    const handleCurrentChange = (current) => {
      currentPage.value = current
      fetchAlarmRecords()
    }
    
    onMounted(() => {
      fetchAlarmRecords()
    })
    
    return {
      alarmRecords,
      total,
      currentPage,
      pageSize,
      addDialogVisible,
      addAlarmRecordForm,
      addAlarmRecordRules,
      addAlarmRecordFormRef,
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
}

.alarm-records-card {
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