<template>
  <div class="login-container">
    <div class="login-bg"></div>
    <div class="login-wrapper">
      <el-card class="login-card">
        <template #header>
          <div class="login-header">
            <h2>智能盲杖系统</h2>
            <p>欢迎回来，请登录</p>
          </div>
        </template>
        <div class="login-form">
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input 
                v-model="loginForm.username" 
                placeholder="请输入用户名" 
                size="large"
                :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input 
                v-model="loginForm.password" 
                type="password" 
                placeholder="请输入密码" 
                show-password 
                size="large"
                :prefix-icon="Lock"
              />
            </el-form-item>
            <el-form-item>
              <el-button 
                type="primary" 
                @click="handleLogin" 
                style="width: 100%"
                size="large"
                :loading="loading"
              >
                登录
              </el-button>
            </el-form-item>
            <div class="login-footer">
              <el-checkbox v-model="rememberMe">记住密码</el-checkbox>
              <el-button type="text" @click="handleForgotPassword">忘记密码？</el-button>
            </div>
            <div class="demo-account-tip">
              <el-alert
                title="演示账号"
                type="info"
                :closable="false"
                show-icon
              >
                <template #default>
                  <div>用户名：admin</div>
                  <div>密码：123456</div>
                </template>
              </el-alert>
            </div>
          </el-form>
        </div>
      </el-card>
      <div class="login-info">
        <p>© 2026 智能盲杖系统 - 为视力障碍用户提供安全保障</p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

export default {
  name: 'Login',
  components: {
    User,
    Lock
  },
  setup() {
    const router = useRouter()
    const loginFormRef = ref(null)
    const loginForm = ref({
      username: '',
      password: ''
    })
    const loginRules = ref({
      username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
      password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
    })
    const loading = ref(false)
    const rememberMe = ref(false)

    // 检查本地存储中的记住密码
    onMounted(() => {
      const savedUser = localStorage.getItem('savedUser')
      if (savedUser) {
        const user = JSON.parse(savedUser)
        loginForm.value.username = user.username
        loginForm.value.password = user.password
        rememberMe.value = true
      }
    })

    const handleLogin = async () => {
      if (loginFormRef.value) {
        const valid = await loginFormRef.value.validate()
        if (valid) {
          loading.value = true
          try {
            // 这里可以调用后端API进行登录验证
            // 暂时使用模拟登录，匹配数据库中的用户信息
            if (loginForm.value.username === 'admin' && loginForm.value.password === '123456') {
              // 存储登录状态
              localStorage.setItem('token', 'mock-token')
              localStorage.setItem('user', JSON.stringify({ username: loginForm.value.username }))
              
              // 记住密码
              if (rememberMe.value) {
                localStorage.setItem('savedUser', JSON.stringify(loginForm.value))
              } else {
                localStorage.removeItem('savedUser')
              }
              
              ElMessage.success('登录成功')
              router.push('/')
            } else {
              ElMessage.error('用户名或密码错误')
            }
          } catch (error) {
            console.error('登录失败:', error)
            ElMessage.error('登录失败')
          } finally {
            loading.value = false
          }
        }
      }
    }

    const handleForgotPassword = () => {
      ElMessageBox.alert('请联系系统管理员重置密码', '忘记密码', {
        confirmButtonText: '确定',
        type: 'info'
      })
    }

    return {
      loginForm,
      loginRules,
      loginFormRef,
      handleLogin,
      loading,
      rememberMe,
      handleForgotPassword
    }
  }
}
</script>

<style scoped>
@keyframes gradientShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

@keyframes floatUp {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(rgba(15, 23, 42, 0.6), rgba(30, 41, 59, 0.8)), url('../../static/login-bg.jpg') no-repeat center center;
  background-size: cover;
  z-index: 1;
}

.login-bg::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 30% 40%, rgba(59, 130, 246, 0.15) 0%, transparent 50%),
              radial-gradient(circle at 70% 60%, rgba(139, 92, 246, 0.12) 0%, transparent 50%);
  z-index: 2;
}

.login-wrapper {
  position: relative;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 420px;
  padding: 0 20px;
}

.login-card {
  width: 100%;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.06);
  overflow: hidden;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.login-card:hover {
  box-shadow: 0 25px 70px rgba(0, 0, 0, 0.35);
  transform: translateY(-4px);
}

.login-header {
  text-align: center;
  padding: 36px 20px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.04), rgba(139, 92, 246, 0.04));
}

.login-header h2 {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px;
  letter-spacing: -0.5px;
}

.login-header p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.login-form {
  padding: 28px 32px 32px;
}

.login-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}
  
.demo-account-tip {
  margin-top: 16px;
}

.demo-account-tip :deep(.el-alert__content) {
  padding-left: 8px;
}

.demo-account-tip :deep(.el-alert__description) {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.6;
}

.demo-account-tip {
  margin-top: 16px;
}

.demo-account-tip :deep(.el-alert__content) {
  padding-left: 8px;
}

.demo-account-tip :deep(.el-alert__description) {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.6;
}

.login-info {
  margin-top: 28px;
  text-align: center;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
}

/* 按钮样式 */
.el-button--primary {
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  border: none;
  border-radius: 10px;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  transition: all 0.3s ease;
}

.el-button--primary:hover {
  background: linear-gradient(135deg, #2563eb 0%, #4f46e5 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.35);
}

/* 输入框样式 */
.el-input__wrapper {
  border-radius: 10px;
  transition: all 0.3s ease;
}

.el-input__wrapper:hover {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.3);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-card {
    margin: 0 16px;
    border-radius: 16px;
  }
  
  .login-header {
    padding: 28px 20px 16px;
  }
  
  .login-header h2 {
    font-size: 22px;
  }
  
  .login-form {
    padding: 24px 20px;
  }
}
</style>
