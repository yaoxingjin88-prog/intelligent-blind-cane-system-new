<template>
  <div class="devices-container">
    <div class="page-hero">
      <div>
        <h2>设备管理</h2>
        <p>集中查看设备状态、绑定用户、电量健康与测试模拟情况</p>
      </div>
      <el-button type="primary" @click="openAddDeviceDialog">
        <el-icon><Plus /></el-icon>
        添加设备
      </el-button>
    </div>

    <div class="stats-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <span class="stat-label">{{ card.label }}</span>
        <strong class="stat-value">{{ card.value }}</strong>
        <span class="stat-sub">{{ card.sub }}</span>
      </div>
    </div>

    <el-card class="devices-card">
      <template #header>
        <div class="card-header">
          <div>
            <span>设备列表</span>
            <p class="header-subtitle">支持快速进入实时监控、切换测试状态，并查看设备所属用户与电量概况</p>
          </div>
          <el-tag type="primary" effect="plain">共 {{ total }} 台设备</el-tag>
        </div>
      </template>
      
      <el-table :data="paginatedDevices" class="devices-table" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="设备信息" min-width="220">
          <template #default="{ row }">
            <div class="device-main">
              <strong>{{ row.deviceName || row.deviceId }}</strong>
              <span>{{ row.deviceId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属用户" min-width="180">
          <template #default="{ row }">
            <div class="user-main">
              <strong>{{ row.userName || '未关联用户' }}</strong>
              <span>用户ID：{{ row.userId || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="电池电量" min-width="180">
          <template #default="{ row }">
            <div class="battery-cell">
              <span class="battery-text">{{ Number(row.batteryLevel) || 0 }}%</span>
              <el-progress
                :percentage="Number(row.batteryLevel) || 0"
                :status="getBatteryStatus(row.batteryLevel)"
                :stroke-width="8"
                :show-text="false"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="设备状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" effect="light">
              {{ row.status || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="测试状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="isTesting(row.deviceId) ? 'warning' : 'info'" effect="light">
              {{ isTesting(row.deviceId) ? '测试中' : '未启动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" align="center">
          <template #default="{ row }">
            <el-button type="success" link size="small" @click="goToMonitor(row)">
              <el-icon><View /></el-icon>实时监控
            </el-button>
            <el-button
              :type="isTesting(row.deviceId) ? 'warning' : 'primary'"
              link
              size="small"
              @click="toggleDeviceTest(row)"
            >
              {{ isTesting(row.deviceId) ? '停止测试' : '测试启动' }}
            </el-button>
            <el-button type="primary" link size="small" @click="openEditDeviceDialog(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button type="danger" link size="small" @click="deleteDevice(row.id)">
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
    
    <!-- 添加设备对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加设备">
      <el-form :model="addDeviceForm" :rules="addDeviceRules" ref="addDeviceFormRef" label-width="80px">
        <el-form-item label="设备ID" prop="deviceId">
          <el-input v-model="addDeviceForm.deviceId" placeholder="请输入设备ID" />
        </el-form-item>
        <el-form-item label="用户ID" prop="userId">
          <el-select v-model="addDeviceForm.userId" placeholder="请选择绑定用户" clearable style="width: 100%">
            <el-option
              v-for="user in usersOptions"
              :key="user.id"
              :label="`${user.name}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="电池电量" prop="batteryLevel">
          <el-input v-model="addDeviceForm.batteryLevel" placeholder="请输入电池电量" type="number" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="addDeviceForm.status" placeholder="请选择状态">
            <el-option label="在线" value="在线" />
            <el-option label="离线" value="离线" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="addDevice">确定</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 编辑设备对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑设备">
      <el-form :model="editDeviceForm" :rules="editDeviceRules" ref="editDeviceFormRef" label-width="80px">
        <el-form-item label="设备ID" prop="deviceId">
          <el-input v-model="editDeviceForm.deviceId" placeholder="请输入设备ID" />
        </el-form-item>
        <el-form-item label="用户ID" prop="userId">
          <el-select v-model="editDeviceForm.userId" placeholder="请选择绑定用户" clearable style="width: 100%">
            <el-option
              v-for="user in usersOptions"
              :key="user.id"
              :label="`${user.name}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="电池电量" prop="batteryLevel">
          <el-input v-model="editDeviceForm.batteryLevel" placeholder="请输入电池电量" type="number" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="editDeviceForm.status" placeholder="请选择状态">
            <el-option label="在线" value="在线" />
            <el-option label="离线" value="离线" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="updateDevice">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted, onActivated } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, Edit, Delete, View } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'

export default {
  name: 'Devices',
  components: {
    Plus,
    Edit,
    Delete,
    View
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const devices = ref([])
    const usersOptions = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const testingDeviceIds = ref([])
    
    const addDialogVisible = ref(false)
    const editDialogVisible = ref(false)
    
    const addDeviceForm = ref({
      deviceId: '',
      userId: '',
      batteryLevel: '',
      status: ''
    })
    
    const editDeviceForm = ref({
      id: '',
      deviceId: '',
      userId: '',
      batteryLevel: '',
      status: ''
    })
    
    const addDeviceRules = ref({
      deviceId: [{ required: true, message: '请输入设备ID', trigger: 'blur' }],
      batteryLevel: [{ required: true, message: '请输入电池电量', trigger: 'blur' }],
      status: [{ required: true, message: '请选择状态', trigger: 'blur' }]
    })
    
    const editDeviceRules = ref({
      deviceId: [{ required: true, message: '请输入设备ID', trigger: 'blur' }],
      batteryLevel: [{ required: true, message: '请输入电池电量', trigger: 'blur' }],
      status: [{ required: true, message: '请选择状态', trigger: 'blur' }]
    })
    
    const addDeviceFormRef = ref(null)
    const editDeviceFormRef = ref(null)

    const paginatedDevices = computed(() => {
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      return devices.value.slice(start, end)
    })

    const statCards = computed(() => {
      const onlineCount = devices.value.filter(device => device.status === '在线').length
      const testingCount = devices.value.filter(device => isTesting(device.deviceId)).length
      const lowBatteryCount = devices.value.filter(device => Number(device.batteryLevel) <= 20).length
      const boundUserCount = devices.value.filter(device => device.userId).length
      return [
        { label: '设备总数', value: total.value, sub: '当前已录入系统的设备数量' },
        { label: '在线设备', value: onlineCount, sub: `离线 ${total.value - onlineCount} 台` },
        { label: '测试中设备', value: testingCount, sub: '正在进行模拟上报的设备' },
        { label: '低电量设备', value: lowBatteryCount, sub: `已关联用户 ${boundUserCount} 台` }
      ]
    })

    const getStatusTagType = (status) => {
      if (status === '在线') return 'success'
      if (status === '离线') return 'info'
      return 'warning'
    }

    const getBatteryStatus = (batteryLevel) => {
      const value = Number(batteryLevel) || 0
      if (value <= 20) return 'exception'
      if (value <= 50) return 'warning'
      return 'success'
    }

    const normalizeUserId = (userId) => {
      if (userId === '' || userId == null) return null
      const value = Number(userId)
      return Number.isNaN(value) ? null : value
    }

    const fetchUsersOptions = async () => {
      try {
        const response = await fetch('/api/users', {
          cache: 'no-cache'
        })
        const data = await response.json()
        usersOptions.value = data.data || []
      } catch (error) {
        console.error('获取用户选项失败:', error)
        usersOptions.value = []
      }
    }
    
    const fetchDevices = async () => {
      try {
        // 调用后端API获取设备列表
        const response = await fetch('/api/devices', {
          cache: 'no-cache' // 禁用缓存，确保获取最新数据
        })
        const data = await response.json()
        devices.value = data.data || []
        total.value = devices.value.length
        const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))
        if (currentPage.value > maxPage) {
          currentPage.value = maxPage
        }
        await fetchTestingDeviceIds()
      } catch (error) {
        console.error('获取设备列表失败:', error)
      }
    }

    const fetchTestingDeviceIds = async () => {
      try {
        const response = await fetch('/api/devices/test/running', {
          cache: 'no-cache'
        })
        const data = await response.json()
        testingDeviceIds.value = Array.isArray(data.data) ? data.data : []
      } catch (error) {
        console.error('获取测试设备状态失败:', error)
        testingDeviceIds.value = []
      }
    }

    const isTesting = (deviceId) => {
      return testingDeviceIds.value.includes(deviceId)
    }
    
    const openAddDeviceDialog = () => {
      addDeviceForm.value = {
        deviceId: '',
        userId: '',
        batteryLevel: '',
        status: ''
      }
      addDialogVisible.value = true
    }
    
    const openEditDeviceDialog = (row) => {
      editDeviceForm.value = {
        ...row,
        userId: row.userId ?? null
      }
      editDialogVisible.value = true
    }
    
    const addDevice = async () => {
      if (addDeviceFormRef.value) {
        const valid = await addDeviceFormRef.value.validate()
        if (valid) {
          try {
            // 转换数据类型
            const deviceData = {
              ...addDeviceForm.value,
              deviceId: addDeviceForm.value.deviceId.toString(),
              userId: normalizeUserId(addDeviceForm.value.userId),
              batteryLevel: parseInt(addDeviceForm.value.batteryLevel)
            }
            // 调用后端API添加设备
            const response = await fetch('/api/devices', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify(deviceData)
            })
            const data = await response.json()
            if (data.code === 200) {
              ElMessage.success('添加设备成功')
              addDialogVisible.value = false
              fetchDevices()
            } else {
              ElMessage.error('添加设备失败: ' + data.msg)
            }
          } catch (error) {
            console.error('添加设备失败:', error)
            ElMessage.error('添加设备失败')
          }
        }
      }
    }
    
    const updateDevice = async () => {
      if (editDeviceFormRef.value) {
        const valid = await editDeviceFormRef.value.validate()
        if (valid) {
          try {
            // 转换数据类型
            const deviceData = {
              ...editDeviceForm.value,
              deviceId: editDeviceForm.value.deviceId.toString(),
              userId: normalizeUserId(editDeviceForm.value.userId),
              batteryLevel: parseInt(editDeviceForm.value.batteryLevel),
              id: parseInt(editDeviceForm.value.id)
            }
            // 调用后端API更新设备
            const response = await fetch('/api/devices', {
              method: 'PUT',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify(deviceData)
            })
            const data = await response.json()
            if (data.code === 200) {
              ElMessage.success('更新设备成功')
              editDialogVisible.value = false
              fetchDevices()
            } else {
              ElMessage.error('更新设备失败: ' + data.msg)
            }
          } catch (error) {
            console.error('更新设备失败:', error)
            ElMessage.error('更新设备失败')
          }
        }
      }
    }
    
    const goToMonitor = (row) => {
      router.push(`/monitor/${row.id}`)
    }

    const toggleDeviceTest = async (row) => {
      try {
        const running = isTesting(row.deviceId)
        const action = running ? 'stop' : 'start'
        const response = await fetch(`/api/devices/${row.id}/test/${action}`, {
          method: 'POST'
        })
        const data = await response.json()
        if (data.code === 200) {
          await fetchTestingDeviceIds()
          ElMessage.success(running ? '设备测试已停止' : '设备测试已启动')
        } else {
          ElMessage.error((running ? '停止' : '启动') + '设备测试失败: ' + data.msg)
        }
      } catch (error) {
        console.error('切换设备测试失败:', error)
        ElMessage.error('切换设备测试失败')
      }
    }
    
    const deleteDevice = async (id) => {
      ElMessageBox.confirm('确定要删除该设备吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          // 调用后端API删除设备
          const response = await fetch(`/api/devices/${id}`, {
            method: 'DELETE'
          })
          const data = await response.json()
          if (data.code === 200) {
            ElMessage.success('删除设备成功')
            fetchDevices()
          } else {
            ElMessage.error('删除设备失败: ' + data.msg)
          }
        } catch (error) {
          console.error('删除设备失败:', error)
          ElMessage.error('删除设备失败')
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
      fetchUsersOptions()
      fetchDevices()
      
      // 检查URL参数，如果有action=add则打开添加设备对话框
      if (route.query.action === 'add') {
        setTimeout(() => {
          openAddDeviceDialog()
        }, 100)
      }
    })

    onActivated(() => {
      fetchUsersOptions()
      fetchDevices()
    })
    
    return {
      devices,
      usersOptions,
      total,
      currentPage,
      pageSize,
      paginatedDevices,
      statCards,
      addDialogVisible,
      editDialogVisible,
      addDeviceForm,
      editDeviceForm,
      addDeviceRules,
      editDeviceRules,
      addDeviceFormRef,
      editDeviceFormRef,
      openAddDeviceDialog,
      openEditDeviceDialog,
      addDevice,
      updateDevice,
      deleteDevice,
      toggleDeviceTest,
      isTesting,
      getStatusTagType,
      getBatteryStatus,
      handleSizeChange,
      handleCurrentChange,
      goToMonitor
    }
  }
}
</script>

<style scoped>
.devices-container {
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

.devices-card {
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

.devices-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
}

.devices-table :deep(.el-table__row td) {
  padding-top: 14px;
  padding-bottom: 14px;
}

.device-main,
.user-main,
.battery-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.device-main strong,
.user-main strong {
  color: #0f172a;
}

.device-main span,
.user-main span,
.battery-text {
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
  .devices-container {
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