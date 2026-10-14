<template>
  <div class="users-container">
    <div class="page-hero">
      <div>
        <h2>用户管理</h2>
        <p>档案、一人一台盲杖绑定，以及主/子监护人家庭圈</p>
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
            <p class="header-subtitle">设备归属用户；主监护人管理绑定，子监护人可监视</p>
          </div>
        </div>
      </template>
      
      <el-table :data="paginatedUsers" class="users-table" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="64" align="center" />
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column prop="username" label="用户名" min-width="110" />
        <el-table-column prop="phone" label="电话" min-width="120" />
        <el-table-column label="绑定设备" min-width="200">
          <template #default="{ row }">
            <div v-if="primaryBoundDevice(row)" class="device-inline">
              <span class="status-dot" :class="deviceStatusClass(primaryBoundDevice(row))" />
              <span class="device-inline-text">
                {{ formatDeviceShort(primaryBoundDevice(row)) }}
                <span class="device-inline-meta">{{ formatBattery(primaryBoundDevice(row).batteryLevel) }}</span>
              </span>
            </div>
            <span v-else class="device-empty">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column label="主监护人" min-width="160">
          <template #default="{ row }">
            <div v-if="row.familyGuardians && row.familyGuardians.length" class="family-cell">
              <span class="family-primary">{{ formatPrimaryGuardian(row.familyGuardians) }}</span>
              <span v-if="memberGuardianCount(row.familyGuardians) > 0" class="family-meta">
                +{{ memberGuardianCount(row.familyGuardians) }} 子监护人
              </span>
            </div>
            <span v-else class="device-empty">未配置</span>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditUserDialog(row)">编辑</el-button>
            <el-button type="success" link size="small" @click="openFamilyDialog(row)">家庭圈</el-button>
            <el-tooltip
              v-if="primaryBoundDevice(row)"
              content="已绑定设备，请先解绑后再删除"
              placement="top"
            >
              <span class="delete-disabled-wrap">
                <el-button type="danger" link size="small" disabled>删除</el-button>
              </span>
            </el-tooltip>
            <el-button v-else type="danger" link size="small" @click="deleteUser(row.id)">删除</el-button>
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
          <el-select
            v-model="addUserForm.selectedDeviceId"
            clearable
            filterable
            :placeholder="bindableDevices().length ? '每人仅可绑定一台未占用设备' : '暂无未绑定设备'"
            style="width: 100%"
          >
            <el-option
              v-for="device in bindableDevices()"
              :key="device.id"
              :label="formatDeviceOptionLabel(device)"
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
          <el-select
            v-model="editUserForm.selectedDeviceId"
            clearable
            filterable
            :placeholder="bindableDevices(editUserForm.id).length ? '每人仅可绑定一台（已占用设备不可选）' : '暂无可用设备'"
            style="width: 100%"
          >
            <el-option
              v-for="device in bindableDevices(editUserForm.id)"
              :key="device.id"
              :label="formatDeviceOptionLabel(device, editUserForm.id)"
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

    <el-dialog v-model="familyDialogVisible" :title="`家庭监护圈 · ${familyTargetUser?.name || ''}`" width="520px">
      <div v-if="familyTargetUser" class="family-dialog">
        <div v-for="g in familyTargetUser.familyGuardians || []" :key="g.id" class="family-row">
          <div>
            <strong>{{ g.name || '未命名' }}</strong>
            <el-tag size="small" :type="g.role === 'PRIMARY' ? 'success' : 'info'" class="family-role-tag">
              {{ g.role === 'PRIMARY' ? '主监护人' : '子监护人' }}
            </el-tag>
            <div class="family-row-meta">{{ g.relation || '家属' }} · {{ g.phone }}</div>
          </div>
          <el-button
            v-if="g.role !== 'PRIMARY'"
            type="danger"
            link
            size="small"
            @click="removeGuardian(g)"
          >移除</el-button>
        </div>
        <el-empty v-if="!(familyTargetUser.familyGuardians || []).length" description="暂无监护人" :image-size="64" />

        <el-divider content-position="left">添加子监护人</el-divider>
        <el-form :model="inviteForm" label-width="72px">
          <el-form-item label="手机号">
            <el-input v-model="inviteForm.phone" placeholder="登录小程序的手机号" />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="inviteForm.name" placeholder="可选" />
          </el-form-item>
          <el-form-item label="关系">
            <el-input v-model="inviteForm.relation" placeholder="如子女、配偶" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="familyDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="inviteGuardian">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted, onActivated } from 'vue'
import { useRoute } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { fetchJson, apiFetch } from '../utils/http'

export default {
  name: 'Users',
  components: {
    Plus
  },
  setup() {
    const route = useRoute()
    const users = ref([])
    const devices = ref([])
    const guardians = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    
    const addDialogVisible = ref(false)
    const editDialogVisible = ref(false)
    const familyDialogVisible = ref(false)
    const familyTargetUser = ref(null)
    const inviteForm = ref({ phone: '', name: '', relation: '家属' })
    
    const addUserForm = ref({
      username: '',
      password: '',
      name: '',
      phone: '',
      idCard: '',
      address: '',
      selectedDeviceId: null
    })
    
    const editUserForm = ref({
      id: '',
      username: '',
      password: '',
      name: '',
      phone: '',
      idCard: '',
      address: '',
      selectedDeviceId: null
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
      const unboundUsers = total.value - boundUsers
      const unboundDevices = devices.value.filter(device => device.userId == null || device.userId === '').length
      const onlineBound = devices.value.filter(device =>
        device.userId != null && device.userId !== '' && device.status === '在线'
      ).length
      return [
        { label: '用户总数', value: total.value, sub: '被监护的盲杖使用人' },
        { label: '已绑定', value: boundUsers, sub: '一人一台，已完成绑定' },
        { label: '待绑定用户', value: unboundUsers, sub: unboundDevices ? `库存未绑设备 ${unboundDevices} 台` : '暂无闲置设备' },
        { label: '在线用户', value: onlineBound, sub: '绑定设备当前在线的用户' }
      ]
    })

    const mergeUsersWithDevices = (userList, deviceList, guardianList = []) => {
      return userList.map(user => {
        // 一人一台：只保留 id 最小的一台，避免历史多绑脏数据撑开展示
        const boundDevices = deviceList
          .filter(device => Number(device.userId) === Number(user.id))
          .sort((a, b) => Number(a.id) - Number(b.id))
          .slice(0, 1)
        const familyGuardians = guardianList
          .filter(g => Number(g.userId) === Number(user.id))
          .sort((a, b) => (a.role === 'PRIMARY' ? 0 : 1) - (b.role === 'PRIMARY' ? 0 : 1))
        return {
          ...user,
          boundDevices,
          familyGuardians
        }
      })
    }

    const formatPrimaryGuardian = (familyGuardians = []) => {
      const primary = familyGuardians.find(g => g.role === 'PRIMARY')
      return primary ? (primary.name || primary.phone) : '未设置'
    }

    const memberGuardianCount = (familyGuardians = []) => {
      return familyGuardians.filter(g => g.role !== 'PRIMARY').length
    }

    /** 一人一台：列表只展示第一台 */
    const primaryBoundDevice = (row) => {
      const list = row && row.boundDevices
      return list && list.length ? list[0] : null
    }

    /** 列表优先展示设备编号，避免「智能盲杖2」类序号干扰 */
    const formatDeviceShort = (device) => {
      if (!device) return ''
      return device.deviceId || device.deviceName || '未命名'
    }

    const formatBattery = (batteryLevel) => {
      const value = Number(batteryLevel)
      return Number.isNaN(value) ? '-' : `${value}%`
    }

    const deviceStatusClass = (device) => {
      if (!device) return 'unknown'
      if (device.status === '在线') return 'online'
      if (device.status === '离线') return 'offline'
      return 'unknown'
    }

    /** 仅未绑定，或已属于当前持杖用户的设备可选（已占用不可抢绑） */
    const bindableDevices = (currentUserId = null) => {
      return devices.value.filter(device => {
        if (device.userId == null || device.userId === '') return true
        return currentUserId != null && Number(device.userId) === Number(currentUserId)
      })
    }

    const formatDeviceOptionLabel = (device, currentUserId = null) => {
      const base = `${device.deviceName || device.deviceId || '未命名'}（${device.deviceId}）`
      if (device.userId == null || device.userId === '') {
        return `${base} · 未绑定`
      }
      if (currentUserId != null && Number(device.userId) === Number(currentUserId)) {
        return `${base} · 当前绑定`
      }
      return base
    }

    /** 一人一台：调用专用分配接口（服务端事务内解绑旧设备再绑定） */
    const syncUserDeviceBindings = async (userId, selectedDeviceId = null) => {
      const targetId = selectedDeviceId != null && selectedDeviceId !== '' ? Number(selectedDeviceId) : null
      const response = await apiFetch(`/api/users/${userId}/device`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deviceId: targetId })
      })
      const data = await response.json()
      if (data.code !== 200) {
        throw new Error(data.msg || '更新设备绑定失败')
      }
    }

    const findCreatedUserId = async (username) => {
      const response = await apiFetch('/api/users', {
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
        const [userData, deviceData, guardianData] = await Promise.all([
          fetchJson('/api/users'),
          fetchJson('/api/devices'),
          fetchJson('/api/guardians')
        ])
        devices.value = deviceData.data || []
        guardians.value = guardianData.data || []
        users.value = mergeUsersWithDevices(userData.data || [], devices.value, guardians.value)
        total.value = users.value.length
        const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))
        if (currentPage.value > maxPage) {
          currentPage.value = maxPage
        }
        if (familyTargetUser.value) {
          familyTargetUser.value = users.value.find(u => Number(u.id) === Number(familyTargetUser.value.id)) || null
        }
      } catch (error) {
        console.error('获取用户列表失败:', error)
        ElMessage.error('获取用户列表失败，请检查后端服务')
      }
    }

    const openFamilyDialog = (row) => {
      familyTargetUser.value = row
      inviteForm.value = { phone: '', name: '', relation: '家属' }
      familyDialogVisible.value = true
    }

    const inviteGuardian = async () => {
      if (!familyTargetUser.value) return
      if (!inviteForm.value.phone) {
        ElMessage.warning('请填写手机号')
        return
      }
      try {
        const response = await apiFetch('/api/guardians', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            userId: Number(familyTargetUser.value.id),
            phone: inviteForm.value.phone.trim(),
            name: inviteForm.value.name || '子监护人',
            relation: inviteForm.value.relation || '家属',
            role: 'MEMBER'
          })
        })
        const data = await response.json()
        if (data.code !== 200) {
          throw new Error(data.msg || '添加失败')
        }
        ElMessage.success('已添加子监护人')
        inviteForm.value = { phone: '', name: '', relation: '家属' }
        await fetchUsers()
      } catch (error) {
        console.error(error)
        ElMessage.error(error.message || '添加子监护人失败')
      }
    }

    const removeGuardian = async (guardian) => {
      try {
        await ElMessageBox.confirm(`确定移除监护人 ${guardian.name || guardian.phone}？`, '提示', { type: 'warning' })
        const response = await apiFetch(`/api/guardians/${guardian.id}`, { method: 'DELETE' })
        const data = await response.json()
        if (data.code !== 200) {
          throw new Error(data.msg || '移除失败')
        }
        ElMessage.success('已移除')
        await fetchUsers()
      } catch (error) {
        if (error === 'cancel' || error === 'close') return
        ElMessage.error(error.message || '移除失败')
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
        selectedDeviceId: null
      }
      addDialogVisible.value = true
    }
    
    const openEditUserDialog = (row) => {
      const firstBound = (row.boundDevices || [])[0]
      editUserForm.value = {
        id: row.id,
        username: row.username,
        password: row.password,
        name: row.name,
        phone: row.phone,
        idCard: row.idCard,
        address: row.address,
        selectedDeviceId: firstBound ? firstBound.id : null
      }
      editDialogVisible.value = true
    }
    
    const addUser = async () => {
      if (addUserFormRef.value) {
        const valid = await addUserFormRef.value.validate()
        if (valid) {
          try {
            const selectedDeviceId = addUserForm.value.selectedDeviceId
            const response = await apiFetch('/api/users', {
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
            if (data.code === 200) {
              if (selectedDeviceId != null && selectedDeviceId !== '') {
                const createdUserId = await findCreatedUserId(addUserForm.value.username)
                if (createdUserId) {
                  await syncUserDeviceBindings(createdUserId, selectedDeviceId)
                }
              }
              ElMessage.success('添加用户成功')
              addDialogVisible.value = false
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
            const selectedDeviceId = editUserForm.value.selectedDeviceId
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
            const response = await apiFetch('/api/users', {
              method: 'PUT',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify(userData)
            })
            const data = await response.json()
            if (data.code === 200) {
              await syncUserDeviceBindings(userData.id, selectedDeviceId)
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
      const row = users.value.find(user => Number(user.id) === Number(id))
      if (row && primaryBoundDevice(row)) {
        ElMessage.warning('该用户已绑定设备，请先解绑后再删除')
        return
      }
      ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await apiFetch(`/api/users/${id}`, {
            method: 'DELETE'
          })
          const data = await response.json()
          if (data.code === 200) {
            ElMessage.success('删除用户成功')
            fetchUsers()
          } else {
            ElMessage.error('删除用户失败: ' + (data.msg || ''))
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
      bindableDevices,
      formatDeviceOptionLabel,
      primaryBoundDevice,
      formatDeviceShort,
      formatBattery,
      deviceStatusClass,
      formatPrimaryGuardian,
      memberGuardianCount,
      familyDialogVisible,
      familyTargetUser,
      inviteForm,
      openFamilyDialog,
      inviteGuardian,
      removeGuardian,
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
  background: var(--app-page-bg, #f5f7fa);
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

.device-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.status-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #cbd5e1;
}

.status-dot.online {
  background: #22c55e;
}

.status-dot.offline {
  background: #94a3b8;
}

.status-dot.unknown {
  background: #f59e0b;
}

.device-inline-text {
  min-width: 0;
  color: #0f172a;
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-inline-meta {
  margin-left: 6px;
  color: #94a3b8;
  font-weight: 400;
}

.device-empty {
  color: #cbd5e1;
  font-size: 13px;
}

.delete-disabled-wrap {
  display: inline-flex;
  margin-left: 12px;
}

.family-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.family-primary {
  color: #0f172a;
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.family-meta {
  color: #94a3b8;
  font-size: 12px;
}

.family-dialog {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.family-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}

.family-role-tag {
  margin-left: 8px;
}

.family-row-meta {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
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