<template>
  <div class="users-container">
    <div class="page-hero">
      <div>
        <h2>用户管理</h2>
        <p>统一查看用户基础信息、设备绑定情况与联系资料</p>
      </div>
      <el-button type="primary" @click="openAddUserDialog">
        <el-icon><Plus /></el-icon>
        添加用户
      </el-button>
    </div>

    <div class="stats-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <span class="stat-label">{{ card.label }}</span>
        <strong class="stat-value">{{ card.value }}</strong>
        <span class="stat-sub">{{ card.sub }}</span>
      </div>
    </div>

    <el-card class="users-card">
      <template #header>
        <div class="card-header">
          <div>
            <span>用户列表</span>
            <p class="header-subtitle">支持查看用户当前绑定的盲杖设备、在线状态和电量概况</p>
          </div>
          <el-tag type="primary" effect="plain">共 {{ total }} 位用户</el-tag>
        </div>
      </template>
      
      <el-table :data="paginatedUsers" class="users-table" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="name" label="姓名" min-width="120" />
        <el-table-column prop="phone" label="电话" min-width="150" />
        <el-table-column label="绑定设备" min-width="260">
          <template #default="{ row }">
            <div v-if="row.boundDevices && row.boundDevices.length" class="device-list">
              <div class="device-tags">
                <el-tag
                  v-for="device in row.boundDevices"
                  :key="device.id || device.deviceId"
                  :type="getDeviceTagType(device.status)"
                  effect="light"
                  class="device-tag"
                >
                  {{ device.deviceName || device.deviceId }}
                </el-tag>
              </div>
              <span class="device-summary">{{ formatDeviceSummary(row.boundDevices) }}</span>
            </div>
            <el-tag v-else type="info" effect="plain">未绑定设备</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditUserDialog(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button type="danger" link size="small" @click="deleteUser(row.id)">
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
    
    <!-- 添加用户对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加用户">
      <el-form :model="addUserForm" :rules="addUserRules" ref="addUserFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addUserForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="addUserForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="addUserForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="addUserForm.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="addUserForm.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="addUserForm.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="绑定设备">
          <el-select v-model="addUserForm.selectedDeviceIds" multiple clearable collapse-tags collapse-tags-tooltip placeholder="请选择绑定设备" style="width: 100%">
            <el-option
              v-for="device in availableDeviceOptions(addUserForm.selectedDeviceIds)"
              :key="device.id"
              :label="`${device.deviceName || device.deviceId}（${device.deviceId}）`"
              :value="device.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="addUser">确定</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 编辑用户对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑用户">
      <el-form :model="editUserForm" :rules="editUserRules" ref="editUserFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editUserForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="editUserForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="editUserForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="editUserForm.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="editUserForm.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="editUserForm.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="绑定设备">
          <el-select v-model="editUserForm.selectedDeviceIds" multiple clearable collapse-tags collapse-tags-tooltip placeholder="请选择绑定设备" style="width: 100%">
            <el-option
              v-for="device in availableDeviceOptions(editUserForm.selectedDeviceIds, editUserForm.id)"
              :key="device.id"
              :label="`${device.deviceName || device.deviceId}（${device.deviceId}）`"
              :value="device.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="updateUser">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted, onActivated } from 'vue'
import { useRoute } from 'vue-router'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'

export default {
  name: 'Users',
  components: {
    Plus,
    Edit,
    Delete
  },
  setup() {
    const route = useRoute()
    const users = ref([])
    const devices = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    
    const addDialogVisible = ref(false)
    const editDialogVisible = ref(false)
    
    const addUserForm = ref({
      username: '',
      password: '',
      name: '',
      phone: '',
      idCard: '',
      address: '',
      selectedDeviceIds: []
    })
    
    const editUserForm = ref({
      id: '',
      username: '',
      password: '',
      name: '',
      phone: '',
      idCard: '',
      address: '',
      selectedDeviceIds: []
    })
    
    const addUserRules = ref({
      username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
      password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
      name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
      phone: [{ required: true, message: '请输入电话', trigger: 'blur' }],
      idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
      address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
    })
    
    const editUserRules = ref({
      username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
      password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
      name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
      phone: [{ required: true, message: '请输入电话', trigger: 'blur' }],
      idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
      address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
    })
    
    const addUserFormRef = ref(null)
    const editUserFormRef = ref(null)

    const paginatedUsers = computed(() => {
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      return users.value.slice(start, end)
    })

    const statCards = computed(() => {
      const boundUsers = users.value.filter(user => user.boundDevices && user.boundDevices.length > 0).length
      const onlineDevices = devices.value.filter(device => device.status === '在线').length
      return [
        { label: '用户总数', value: total.value, sub: '当前系统中的用户数量' },
        { label: '已绑定设备用户', value: boundUsers, sub: '至少绑定 1 台盲杖设备' },
        { label: '未绑定设备用户', value: total.value - boundUsers, sub: '建议尽快完成设备分配' },
        { label: '在线设备', value: onlineDevices, sub: `共关联设备 ${devices.value.length} 台` }
      ]
    })

    const mergeUsersWithDevices = (userList, deviceList) => {
      return userList.map(user => {
        const boundDevices = deviceList.filter(device => Number(device.userId) === Number(user.id))
        return {
          ...user,
          boundDevices
        }
      })
    }

    const getDeviceTagType = (status) => {
      if (status === '在线') return 'success'
      if (status === '离线') return 'info'
      return 'warning'
    }

    const formatDeviceSummary = (boundDevices) => {
      if (!boundDevices || !boundDevices.length) return '未绑定设备'
      const onlineCount = boundDevices.filter(device => device.status === '在线').length
      const batteryValues = boundDevices
        .map(device => Number(device.batteryLevel))
        .filter(value => !Number.isNaN(value))
      const averageBattery = batteryValues.length
        ? Math.round(batteryValues.reduce((sum, value) => sum + value, 0) / batteryValues.length)
        : null
      return `${boundDevices.length} 台设备 · 在线 ${onlineCount} 台${averageBattery !== null ? ` · 平均电量 ${averageBattery}%` : ''}`
    }

    const availableDeviceOptions = (selectedIds = [], currentUserId = null) => {
      return devices.value.filter(device => {
        const selected = selectedIds.includes(device.id)
        const ownedByCurrentUser = currentUserId != null && Number(device.userId) === Number(currentUserId)
        return !device.userId || selected || ownedByCurrentUser
      })
    }

    const updateDeviceBinding = async (device, userId) => {
      const payload = {
        ...device,
        id: parseInt(device.id),
        deviceId: String(device.deviceId),
        userId,
        batteryLevel: parseInt(device.batteryLevel)
      }
      const response = await fetch('/api/devices', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      })
      const data = await response.json()
      if (data.code !== 200) {
        throw new Error(data.msg || '更新设备绑定失败')
      }
    }

    const syncUserDeviceBindings = async (userId, selectedDeviceIds = []) => {
      const selectedSet = new Set(selectedDeviceIds.map(id => Number(id)))
      const devicesToBind = devices.value.filter(device => selectedSet.has(Number(device.id)) && Number(device.userId) !== Number(userId))
      const devicesToUnbind = devices.value.filter(device => Number(device.userId) === Number(userId) && !selectedSet.has(Number(device.id)))

      await Promise.all([
        ...devicesToBind.map(device => updateDeviceBinding(device, Number(userId))),
        ...devicesToUnbind.map(device => updateDeviceBinding(device, null))
      ])
    }

    const findCreatedUserId = async (username) => {
      const response = await fetch('/api/users', {
        cache: 'no-cache'
      })
      const data = await response.json()
      const matchedUser = (data.data || [])
        .filter(user => user.username === username)
        .sort((a, b) => Number(b.id) - Number(a.id))[0]
      return matchedUser?.id ?? null
    }
    
    const fetchUsers = async () => {
      try {
        // 调用后端API获取用户列表
        console.log('开始获取用户列表')
        const [userResponse, deviceResponse] = await Promise.all([
          fetch('/api/users', {
            cache: 'no-cache'
          }),
          fetch('/api/devices', {
            cache: 'no-cache'
          })
        ])
        const userData = await userResponse.json()
        const deviceData = await deviceResponse.json()
        console.log('获取用户列表响应:', userData)
        devices.value = deviceData.data || []
        users.value = mergeUsersWithDevices(userData.data || [], devices.value)
        total.value = users.value.length
        console.log('用户列表数据:', users.value)
        const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))
        if (currentPage.value > maxPage) {
          currentPage.value = maxPage
        }
      } catch (error) {
        console.error('获取用户列表失败:', error)
      }
    }
    
    const openAddUserDialog = () => {
      addUserForm.value = {
        username: '',
        password: '',
        name: '',
        phone: '',
        idCard: '',
        address: '',
        selectedDeviceIds: []
      }
      addDialogVisible.value = true
    }
    
    const openEditUserDialog = (row) => {
      editUserForm.value = {
        id: row.id,
        username: row.username,
        password: row.password,
        name: row.name,
        phone: row.phone,
        idCard: row.idCard,
        address: row.address,
        selectedDeviceIds: (row.boundDevices || []).map(device => device.id)
      }
      editDialogVisible.value = true
    }
    
    const addUser = async () => {
      console.log('开始添加用户')
      if (addUserFormRef.value) {
        const valid = await addUserFormRef.value.validate()
        if (valid) {
          try {
            // 调用后端API添加用户
            console.log('添加用户数据:', addUserForm.value)
            const selectedDeviceIds = [...addUserForm.value.selectedDeviceIds]
            const response = await fetch('/api/users', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify({
                username: addUserForm.value.username,
                password: addUserForm.value.password,
                name: addUserForm.value.name,
                phone: addUserForm.value.phone,
                idCard: addUserForm.value.idCard,
                address: addUserForm.value.address
              })
            })
            const data = await response.json()
            console.log('添加用户响应:', data)
            if (data.code === 200) {
              if (selectedDeviceIds.length) {
                const createdUserId = await findCreatedUserId(addUserForm.value.username)
                if (createdUserId) {
                  await syncUserDeviceBindings(createdUserId, selectedDeviceIds)
                }
              }
              ElMessage.success('添加用户成功')
              addDialogVisible.value = false
              console.log('添加用户成功，开始刷新用户列表')
              fetchUsers()
            } else {
              ElMessage.error('添加用户失败: ' + data.msg)
            }
          } catch (error) {
            console.error('添加用户失败:', error)
            ElMessage.error('添加用户失败')
          }
        }
      }
    }
    
    const updateUser = async () => {
      if (editUserFormRef.value) {
        const valid = await editUserFormRef.value.validate()
        if (valid) {
          try {
            // 转换数据类型
            const selectedDeviceIds = [...editUserForm.value.selectedDeviceIds]
            const userData = {
              id: parseInt(editUserForm.value.id),
              username: editUserForm.value.username,
              password: editUserForm.value.password,
              name: editUserForm.value.name,
              phone: editUserForm.value.phone,
              idCard: editUserForm.value.idCard,
              address: editUserForm.value.address
            }
            // 调用后端API更新用户
            const response = await fetch('/api/users', {
              method: 'PUT',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify(userData)
            })
            const data = await response.json()
            if (data.code === 200) {
              await syncUserDeviceBindings(userData.id, selectedDeviceIds)
              ElMessage.success('更新用户成功')
              editDialogVisible.value = false
              fetchUsers()
            } else {
              ElMessage.error('更新用户失败: ' + data.msg)
            }
          } catch (error) {
            console.error('更新用户失败:', error)
            ElMessage.error('更新用户失败')
          }
        }
      }
    }
    
    const deleteUser = async (id) => {
      ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          // 调用后端API删除用户
          const response = await fetch(`/api/users/${id}`, {
            method: 'DELETE'
          })
          const data = await response.json()
          if (data.code === 200) {
            ElMessage.success('删除用户成功')
            fetchUsers()
          } else {
            ElMessage.error('删除用户失败: ' + data.msg)
          }
        } catch (error) {
          console.error('删除用户失败:', error)
          ElMessage.error('删除用户失败')
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
      fetchUsers()
      
      // 检查URL参数，如果有action=add则打开添加用户对话框
      if (route.query.action === 'add') {
        setTimeout(() => {
          openAddUserDialog()
        }, 100)
      }
    })

    onActivated(() => {
      fetchUsers()
    })
    
    return {
      users,
      devices,
      total,
      currentPage,
      pageSize,
      paginatedUsers,
      statCards,
      addDialogVisible,
      editDialogVisible,
      addUserForm,
      editUserForm,
      addUserRules,
      editUserRules,
      addUserFormRef,
      editUserFormRef,
      openAddUserDialog,
      openEditUserDialog,
      addUser,
      updateUser,
      deleteUser,
      availableDeviceOptions,
      getDeviceTagType,
      formatDeviceSummary,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.users-container {
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

.users-card {
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

.users-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
}

.users-table :deep(.el-table__row td) {
  padding-top: 14px;
  padding-bottom: 14px;
}

.device-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.device-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.device-tag {
  margin: 0;
}

.device-summary {
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
  .users-container {
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