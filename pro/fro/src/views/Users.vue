<template>
  <div class="users-container">
    <el-card class="users-card">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="openAddUserDialog">
            <el-icon><Plus /></el-icon>
            添加用户
          </el-button>
        </div>
      </template>
      
      <el-table :data="users" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="address" label="地址" />
        <el-table-column label="操作" width="160" align="center">
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
import { ref, onMounted, onMounted as onMountedVue } from 'vue'
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
      address: ''
    })
    
    const editUserForm = ref({
      id: '',
      username: '',
      password: '',
      name: '',
      phone: '',
      idCard: '',
      address: ''
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
    
    const fetchUsers = async () => {
      try {
        // 调用后端API获取用户列表
        console.log('开始获取用户列表')
        const response = await fetch('/api/users', {
          cache: 'no-cache' // 禁用缓存，确保获取最新数据
        })
        const data = await response.json()
        console.log('获取用户列表响应:', data)
        users.value = data.data || []
        total.value = users.value.length
        console.log('用户列表数据:', users.value)
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
        address: ''
      }
      addDialogVisible.value = true
    }
    
    const openEditUserDialog = (row) => {
      editUserForm.value = { ...row }
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
            const response = await fetch('/api/users', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify(addUserForm.value)
            })
            const data = await response.json()
            console.log('添加用户响应:', data)
            if (data.code === 200) {
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
            const userData = {
              ...editUserForm.value,
              id: parseInt(editUserForm.value.id)
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
      fetchUsers()
    }
    
    const handleCurrentChange = (current) => {
      currentPage.value = current
      fetchUsers()
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
    
    return {
      users,
      total,
      currentPage,
      pageSize,
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
}

.users-card {
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