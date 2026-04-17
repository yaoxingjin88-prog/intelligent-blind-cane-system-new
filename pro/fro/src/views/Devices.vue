<template>
  <div class="devices-container">
    <el-card class="devices-card">
      <template #header>
        <div class="card-header">
          <span>设备管理</span>
          <el-button type="primary" @click="openAddDeviceDialog">
            <el-icon><Plus /></el-icon>
            添加设备
          </el-button>
        </div>
      </template>
      
      <el-table :data="devices" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="deviceId" label="设备ID" />
        <el-table-column prop="userId" label="用户ID" />
        <el-table-column prop="batteryLevel" label="电池电量" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="操作" width="300" align="center">
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
          <el-input v-model="addDeviceForm.userId" placeholder="请输入用户ID" />
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
          <el-input v-model="editDeviceForm.userId" placeholder="请输入用户ID" />
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
import { ref, onMounted } from 'vue'
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
      userId: [{ required: true, message: '请输入用户ID', trigger: 'blur' }],
      batteryLevel: [{ required: true, message: '请输入电池电量', trigger: 'blur' }],
      status: [{ required: true, message: '请选择状态', trigger: 'blur' }]
    })
    
    const editDeviceRules = ref({
      deviceId: [{ required: true, message: '请输入设备ID', trigger: 'blur' }],
      userId: [{ required: true, message: '请输入用户ID', trigger: 'blur' }],
      batteryLevel: [{ required: true, message: '请输入电池电量', trigger: 'blur' }],
      status: [{ required: true, message: '请选择状态', trigger: 'blur' }]
    })
    
    const addDeviceFormRef = ref(null)
    const editDeviceFormRef = ref(null)
    
    const fetchDevices = async () => {
      try {
        // 调用后端API获取设备列表
        const response = await fetch('/api/devices', {
          cache: 'no-cache' // 禁用缓存，确保获取最新数据
        })
        const data = await response.json()
        devices.value = data.data || []
        total.value = devices.value.length
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
      editDeviceForm.value = { ...row }
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
              userId: parseInt(addDeviceForm.value.userId),
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
              userId: parseInt(editDeviceForm.value.userId),
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
      fetchDevices()
    }
    
    const handleCurrentChange = (current) => {
      currentPage.value = current
      fetchDevices()
    }
    
    onMounted(() => {
      fetchDevices()
      
      // 检查URL参数，如果有action=add则打开添加设备对话框
      if (route.query.action === 'add') {
        setTimeout(() => {
          openAddDeviceDialog()
        }, 100)
      }
    })
    
    return {
      devices,
      total,
      currentPage,
      pageSize,
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
}

.devices-card {
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