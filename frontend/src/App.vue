<template>
  <div class="app-container">
    <!-- 检查是否已登录 -->
    <div v-if="isLoggedIn" class="app-layout">
      <!-- 左侧侧边栏 -->
      <aside class="sidebar" :class="{ 'sidebar--collapsed': isCollapse }">
        <!-- Logo 区 -->
        <div class="sidebar-logo" @click="isCollapse = !isCollapse">
          <el-icon :size="26" style="color: #60a5fa;"><Guide /></el-icon>
          <span v-show="!isCollapse" class="sidebar-logo__text">智能盲杖</span>
        </div>
        <!-- 导航菜单 -->
        <div class="sidebar-nav">
          <el-menu
            :default-active="activeMenu"
            class="sidebar-menu"
            :collapse="isCollapse"
            :collapse-transition="false"
            background-color="transparent"
            text-color="#94a3b8"
            active-text-color="#ffffff"
            @select="handleMenuSelect"
          >
            <el-menu-item index="/">
              <el-icon><House /></el-icon>
              <template #title>首页</template>
            </el-menu-item>
            <el-menu-item index="/users">
              <el-icon><User /></el-icon>
              <template #title>用户管理</template>
            </el-menu-item>
            <el-menu-item index="/devices">
              <el-icon><Monitor /></el-icon>
              <template #title>设备管理</template>
            </el-menu-item>
            <el-menu-item index="/monitor-center">
              <el-icon><Monitor /></el-icon>
              <template #title>实时监控</template>
            </el-menu-item>
            <el-menu-item index="/fences">
              <el-icon><Guide /></el-icon>
              <template #title>电子围栏</template>
            </el-menu-item>
            <el-menu-item index="/trajectory-playback">
              <el-icon><Guide /></el-icon>
              <template #title>轨迹回放</template>
            </el-menu-item>
            <el-menu-item index="/sensor-data">
              <el-icon><DataAnalysis /></el-icon>
              <template #title>传感器数据</template>
            </el-menu-item>
            <el-menu-item index="/alarm-records">
              <el-icon><Bell /></el-icon>
              <template #title>告警记录</template>
            </el-menu-item>
          </el-menu>
        </div>
      </aside>

      <!-- 右侧主内容区 -->
      <div class="main-wrapper">
        <!-- 顶部 Header -->
        <header class="app-header">
          <div class="app-header__left">
            <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
              <Fold v-if="!isCollapse" />
              <Expand v-else />
            </el-icon>
            <span class="page-title">{{ getMenuName() }}</span>
          </div>
          <div class="app-header__right">
            <el-popover placement="bottom-end" :width="360" trigger="click" popper-class="notification-popper">
              <template #reference>
                <div class="notification-trigger" @click="fetchHeaderNotifications">
                  <el-badge :value="unreadAlarmCount" :hidden="!unreadAlarmCount" class="header-badge">
                    <el-icon :size="18" class="header-icon"><BellFilled /></el-icon>
                  </el-badge>
                </div>
              </template>
              <div class="notification-panel">
                <div class="notification-panel__header">
                  <div>
                    <strong>报警消息</strong>
                    <p>未处理告警会在这里集中提醒</p>
                  </div>
                  <el-tag type="danger" effect="light">{{ unreadAlarmCount }} 条未处理</el-tag>
                </div>
                <div v-if="latestUnhandledAlarms.length" class="notification-list">
                  <div v-for="alarm in latestUnhandledAlarms" :key="alarm.id" class="notification-item" @click="goToAlarmRecords()">
                    <div class="notification-item__main">
                      <span class="notification-device">{{ alarm.deviceId || '未知设备' }}</span>
                      <el-tag :type="getAlarmTagType(alarm.alarmType)" effect="light" size="small">
                        {{ alarm.alarmType || '未知告警' }}
                      </el-tag>
                    </div>
                    <span class="notification-time">{{ alarm.alarmTime || '-' }}</span>
                  </div>
                </div>
                <el-empty v-else description="暂无未处理告警" :image-size="72" />
                <div class="notification-panel__footer">
                  <el-button type="primary" text @click="goToAlarmRecords">查看全部告警</el-button>
                </div>
              </div>
            </el-popover>
            <el-dropdown trigger="click">
              <div class="user-avatar-wrapper">
                <el-avatar :size="32" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
                <span class="user-name">{{ currentUser.username || 'Admin' }}</span>
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>
                    <el-icon><User /></el-icon> 个人资料
                  </el-dropdown-item>
                  <el-dropdown-item>
                    <el-icon><Setting /></el-icon> 设置
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="logout" style="color: #ef4444;">
                    <el-icon><SwitchButton /></el-icon> 退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </header>

        <!-- 主内容区 -->
        <main class="main-content">
          <router-view v-slot="{ Component }">
            <keep-alive :include="cachedViews" :max="8">
              <component :is="Component" />
            </keep-alive>
          </router-view>
        </main>
      </div>
    </div>
    <!-- 未登录时直接显示路由内容（登录页面） -->
    <div v-else>
      <router-view />
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { House, User, Monitor, DataAnalysis, Bell, Guide, Fold, Expand, BellFilled, Setting, SwitchButton, ArrowDown } from '@element-plus/icons-vue'

export default {
  name: 'App',
  components: {
    House, User, Monitor, DataAnalysis, Bell,
    Guide, Fold, Expand, BellFilled, Setting, SwitchButton, ArrowDown
  },
  setup() {
    const router = useRouter()
    const activeMenu = ref('/')
    const isLoggedIn = ref(localStorage.getItem('token') !== null)
    const isCollapse = ref(false)
    const currentUser = ref(JSON.parse(localStorage.getItem('user') || '{}'))
    const headerAlarmRecords = ref([])
    const cachedViews = [
      'Dashboard',
      'Users',
      'Devices',
      'MonitoringCenter',
      'Fences',
      'TrajectoryPlayback',
      'SensorData',
      'AlarmRecords'
    ]
    let alarmPollTimer = null

    const menuNameMap = {
      '/': '首页',
      '/users': '用户管理',
      '/devices': '设备管理',
      '/monitor-center': '实时监控',
      '/fences': '电子围栏',
      '/trajectory-playback': '轨迹回放',
      '/sensor-data': '传感器数据',
      '/alarm-records': '告警记录'
    }

    const latestUnhandledAlarms = computed(() => headerAlarmRecords.value.slice(0, 5))
    const unreadAlarmCount = computed(() => headerAlarmRecords.value.length)

    const getMenuName = () => {
      return menuNameMap[activeMenu.value] || '概览'
    }

    const getAlarmTagType = (alarmType) => {
      if (!alarmType) return 'info'
      if (String(alarmType).includes('跌倒')) return 'danger'
      if (String(alarmType).includes('低电')) return 'warning'
      if (String(alarmType).includes('围栏')) return 'warning'
      return 'info'
    }

    const fetchHeaderNotifications = async () => {
      if (!isLoggedIn.value) {
        headerAlarmRecords.value = []
        return
      }
      try {
        const response = await fetch('/api/alarm-records?unhandledOnly=true', {
          cache: 'no-cache'
        })
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`)
        }
        const data = await response.json()
        headerAlarmRecords.value = (data.data || [])
          .sort((a, b) => String(b.alarmTime || '').localeCompare(String(a.alarmTime || '')))
      } catch (error) {
        console.error('获取顶部告警通知失败:', error)
        // 失败时保留上一次数据，避免切页后铃铛被清空成“没数据”
      }
    }

    const goToAlarmRecords = () => {
      router.push('/alarm-records')
    }

    // 监听登录状态变化
    const checkLoginStatus = () => {
      isLoggedIn.value = localStorage.getItem('token') !== null
      currentUser.value = JSON.parse(localStorage.getItem('user') || '{}')
      if (!isLoggedIn.value) {
        headerAlarmRecords.value = []
      }
    }

    const handleMenuSelect = (key) => {
      router.push(key)
    }

    const logout = () => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      isLoggedIn.value = false
      router.push('/login')
    }

    const syncActiveMenu = (path) => {
      activeMenu.value = path.startsWith('/monitor/') ? '/monitor-center' : path
    }

    onMounted(() => {
      syncActiveMenu(router.currentRoute.value.path)
      checkLoginStatus()
      // 延迟拉顶部告警，避免与首页看板首次请求抢后端
      window.setTimeout(() => {
        fetchHeaderNotifications()
      }, 1200)

      alarmPollTimer = window.setInterval(() => {
        fetchHeaderNotifications()
      }, 30000)
      
      // 监听localStorage变化
      window.addEventListener('storage', checkLoginStatus)
      // 监听告警变更事件，立即刷新铃铛
      window.addEventListener('alarm-records-changed', fetchHeaderNotifications)
    })

    // 用 watch 同步菜单高亮，避免重复注册 beforeEach
    watch(() => router.currentRoute.value.path, (path) => {
      checkLoginStatus()
      syncActiveMenu(path)
    })

    onUnmounted(() => {
      window.removeEventListener('storage', checkLoginStatus)
      window.removeEventListener('alarm-records-changed', fetchHeaderNotifications)
      if (alarmPollTimer) {
        window.clearInterval(alarmPollTimer)
        alarmPollTimer = null
      }
    })

    return {
      activeMenu,
      handleMenuSelect,
      isLoggedIn,
      isCollapse,
      currentUser,
      getMenuName,
      latestUnhandledAlarms,
      unreadAlarmCount,
      getAlarmTagType,
      fetchHeaderNotifications,
      goToAlarmRecords,
      logout,
      cachedViews
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  width: 100%;
  overflow: hidden;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Segoe UI', sans-serif;
}

.app-container {
  height: 100%;
  width: 100%;
  background-color: #f0f2f5;
}

/* ===== Layout ===== */
.app-layout {
  display: flex;
  height: 100%;
  width: 100%;
}

/* ===== Sidebar ===== */
.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 20;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.sidebar--collapsed {
  width: 64px;
  overflow: hidden;
}

.sidebar--collapsed .sidebar-menu .el-menu-item {
  margin: 2px 4px;
  padding: 0 !important;
  justify-content: center;
  border-radius: 8px;
}

.sidebar--collapsed .sidebar-menu .el-menu-item .el-icon {
  margin-right: 0;
}

.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.2);
  cursor: pointer;
  gap: 10px;
  transition: all 0.3s;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.sidebar-logo:hover {
  background: rgba(0, 0, 0, 0.3);
}

.sidebar-logo__text {
  color: #ffffff;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 1px;
  white-space: nowrap;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
}

/* Hide scrollbar */
.sidebar-nav::-webkit-scrollbar {
  display: none;
}
.sidebar-nav {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* Menu styling */
.sidebar-menu {
  border-right: none !important;
}

.sidebar-menu .el-menu-item {
  height: 48px;
  line-height: 48px;
  margin: 2px 8px;
  border-radius: 10px;
  transition: all 0.25s ease;
  font-size: 14px;
}

.sidebar-menu .el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.06) !important;
  color: #e2e8f0 !important;
}

.sidebar-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.sidebar-menu .el-menu-item .el-icon {
  font-size: 18px;
}

/* ===== Main Wrapper ===== */
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* ===== Header ===== */
.app-header {
  height: 60px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 10;
  flex-shrink: 0;
}

.app-header__left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #64748b;
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: #3b82f6;
}

.app-header__right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.notification-trigger:hover {
  background: rgba(59, 130, 246, 0.08);
}

.header-icon {
  color: #64748b;
  cursor: pointer;
  transition: color 0.2s;
}

.header-icon:hover {
  color: #3b82f6;
}

.header-badge .el-badge__content {
  box-shadow: 0 4px 10px rgba(239, 68, 68, 0.25);
}

.notification-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-panel__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.notification-panel__header strong {
  color: #0f172a;
}

.notification-panel__header p {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 320px;
  overflow-y: auto;
}

.notification-item {
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.notification-item:hover {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.notification-item__main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.notification-device {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.notification-time {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: #64748b;
}

.notification-panel__footer {
  display: flex;
  justify-content: flex-end;
}

.user-avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-avatar-wrapper:hover {
  background: rgba(59, 130, 246, 0.06);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}

/* ===== Main Content ===== */
.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 0;
  background: linear-gradient(135deg, #f0f2f5 0%, #e8ecf1 100%);
}

/* Smooth scrollbar */
.main-content::-webkit-scrollbar {
  width: 6px;
}
.main-content::-webkit-scrollbar-track {
  background: transparent;
}
.main-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}
.main-content::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* ===== Global Overrides ===== */
.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

/* Collapsed menu tooltip popper */
.el-menu--collapse .el-tooltip__trigger {
  padding: 0 !important;
  display: flex !important;
  align-items: center;
  justify-content: center;
}

.el-popper.is-dark {
  font-size: 13px !important;
}
</style>