<template>
  <div class="app-container">
    <!-- 检查是否已登录 -->
    <div v-if="isLoggedIn" class="app-layout">
      <!-- 左侧侧边栏 -->
      <aside class="sidebar" :class="{ 'sidebar--collapsed': isCollapse }">
        <!-- Logo 区 -->
        <div class="sidebar-logo" @click="isCollapse = !isCollapse">
          <el-icon :size="26" class="sidebar-logo__icon"><Guide /></el-icon>
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
            text-color="var(--app-sidebar-text)"
            active-text-color="var(--app-sidebar-text-active)"
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
                  <div
                    v-for="alarm in latestUnhandledAlarms"
                    :key="alarm.id"
                    class="notification-item"
                    @click="handleHeaderAlarm(alarm)"
                  >
                    <div class="notification-item__main">
                      <span class="notification-device">{{ alarm.deviceId || '未知设备' }}</span>
                      <el-tag :type="getAlarmTagType(alarm.alarmType)" effect="light" size="small">
                        {{ alarm.alarmType || '未知告警' }}
                      </el-tag>
                    </div>
                    <span class="notification-time">{{ alarm.alarmTime || '-' }}</span>
                    <span class="notification-action">点击处理</span>
                  </div>
                </div>
                <el-empty v-else description="暂无未处理告警" :image-size="72" />
                <div class="notification-panel__footer">
                  <el-button
                    v-if="unreadAlarmCount"
                    type="danger"
                    text
                    :loading="handlingAlarms"
                    @click="handleAllHeaderAlarms"
                  >
                    全部标为已处理
                  </el-button>
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
import { ElMessage, ElNotification } from 'element-plus'
import { fetchJson, apiFetch } from './utils/http'
import { startVisibilityInterval } from './utils/visibilityInterval'

const ADMIN_WS_DEVICE_ID = '__admin_console__'
const ALARM_TOAST_RECENT_MS = 5 * 60 * 1000

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
    const handlingAlarms = ref(false)
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
    let adminWs = null
    let adminWsReconnectTimer = null
    let adminWsUnmounted = false
    const toastedAlarmIds = new Set()
    let headerAlarmBootstrapped = false

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

    const parseAlarmTimeMs = (alarm) => {
      const raw = alarm?.alarmTime || alarm?.createTime
      if (!raw) return null
      const ms = Date.parse(String(raw).trim().replace('T', ' ').replace(/-/g, '/'))
      return Number.isNaN(ms) ? null : ms
    }

    const isRecentAlarm = (alarm) => {
      const at = parseAlarmTimeMs(alarm)
      if (at == null) return false
      return Date.now() - at <= ALARM_TOAST_RECENT_MS
    }

    /** 全局危险告警弹窗（任意管理页可见，不依赖实时监控页） */
    const showGlobalAlarmToast = (alarm) => {
      if (!alarm?.id) return
      if (toastedAlarmIds.has(alarm.id)) return
      // 已处理的不弹；status 缺省时仍弹（WS 刚推过来的新报警）
      if (alarm.status != null && String(alarm.status) !== '0' && alarm.status !== '未处理') return
      toastedAlarmIds.add(alarm.id)
      const deviceLabel = alarm.deviceId || '未知设备'
      ElNotification({
        title: '设备报警',
        message: `设备 ${deviceLabel} 发生${alarm.alarmType || '告警'}，请点击右上角铃铛处理。`,
        type: 'error',
        duration: 4500,
        position: 'top-right'
      })
    }

    const fetchHeaderNotifications = async () => {
      if (!isLoggedIn.value) {
        headerAlarmRecords.value = []
        return
      }
      try {
        const data = await fetchJson('/api/alarm-records?unhandledOnly=true&limit=50', {
          retries: 3,
          retryDelay: 400
        })
        const list = (data.data || [])
          .sort((a, b) => String(b.alarmTime || '').localeCompare(String(a.alarmTime || '')))
        // 轮询兜底：首次只建基线不弹窗，之后对新的近期未处理弹窗
        if (headerAlarmBootstrapped) {
          const prevIds = new Set(headerAlarmRecords.value.map(item => item.id))
          list.forEach(alarm => {
            if (!prevIds.has(alarm.id) && isRecentAlarm(alarm)) {
              showGlobalAlarmToast(alarm)
            }
          })
        } else {
          headerAlarmBootstrapped = true
          list.forEach(alarm => {
            if (alarm?.id) toastedAlarmIds.add(alarm.id)
          })
        }
        headerAlarmRecords.value = list
      } catch (error) {
        console.error('获取顶部告警通知失败:', error)
        // 失败时保留上一次数据，避免切页后铃铛被清空成“没数据”
      }
    }

    const getAdminWsUrl = () => {
      const envBase = import.meta.env.VITE_WS_BASE_URL
      if (envBase) {
        return `${String(envBase).replace(/\/$/, '')}/ws/alarm?deviceId=${ADMIN_WS_DEVICE_ID}`
      }
      if (import.meta.env.DEV) {
        // 经 Vite /ws 代理，避免写死端口不一致
        const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
        return `${proto}://${window.location.host}/ws/alarm?deviceId=${ADMIN_WS_DEVICE_ID}`
      }
      const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
      return `${proto}://${window.location.host}/ws/alarm?deviceId=${ADMIN_WS_DEVICE_ID}`
    }

    const disconnectAdminAlarmWs = () => {
      if (adminWsReconnectTimer) {
        clearTimeout(adminWsReconnectTimer)
        adminWsReconnectTimer = null
      }
      if (adminWs) {
        try { adminWs.close() } catch (_) { /* ignore */ }
        adminWs = null
      }
    }

    const connectAdminAlarmWs = () => {
      if (!isLoggedIn.value || adminWsUnmounted) return
      disconnectAdminAlarmWs()
      try {
        adminWs = new WebSocket(getAdminWsUrl())
      } catch (error) {
        console.error('管理端报警 WebSocket 创建失败:', error)
        adminWsReconnectTimer = setTimeout(connectAdminAlarmWs, 5000)
        return
      }
      adminWs.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data)
          if (message.type === 'ALARM' && message.alarm) {
            showGlobalAlarmToast(message.alarm)
            fetchHeaderNotifications()
            return
          }
          if (message.type === 'ALARM_HANDLED' && message.alarm) {
            // 监护人已处理：清铃铛对应项并弱提示
            const alarmId = message.alarm.id
            if (alarmId != null) {
              headerAlarmRecords.value = headerAlarmRecords.value.filter(item => item.id !== alarmId)
              toastedAlarmIds.add(alarmId)
            }
            ElNotification({
              title: '报警已处理',
              message: message.message || '家属端已处理该报警',
              type: 'success',
              duration: 3500,
              position: 'top-right'
            })
            window.dispatchEvent(new CustomEvent('alarm-records-changed'))
          }
        } catch (error) {
          console.error('解析管理端报警消息失败:', error)
        }
      }
      adminWs.onerror = () => {
        // onclose 会重连
      }
      adminWs.onclose = () => {
        adminWs = null
        if (adminWsUnmounted || !isLoggedIn.value) return
        adminWsReconnectTimer = setTimeout(connectAdminAlarmWs, 5000)
      }
    }

    const goToAlarmRecords = () => {
      router.push('/alarm-records')
    }

    /** 铃铛里点一条 = 标已处理，红点随之减少 */
    const handleHeaderAlarm = async (alarm) => {
      if (!alarm?.id || handlingAlarms.value) return
      handlingAlarms.value = true
      try {
        const response = await apiFetch(`/api/alarm-records/${alarm.id}/handle`, { method: 'PUT' })
        const data = await response.json()
        if (data.code !== 200) {
          ElMessage.error(data.msg || '处理失败')
          return
        }
        headerAlarmRecords.value = headerAlarmRecords.value.filter(item => item.id !== alarm.id)
        window.dispatchEvent(new CustomEvent('alarm-records-changed'))
        ElMessage.success('已处理该告警')
      } catch (error) {
        console.error('处理顶部告警失败:', error)
        ElMessage.error('处理告警失败')
      } finally {
        handlingAlarms.value = false
      }
    }

    /** 一键清空铃铛未处理（标已处理） */
    const handleAllHeaderAlarms = async () => {
      const list = [...headerAlarmRecords.value]
      if (!list.length || handlingAlarms.value) return
      handlingAlarms.value = true
      try {
        let ok = 0
        for (const alarm of list) {
          if (!alarm?.id) continue
          const response = await apiFetch(`/api/alarm-records/${alarm.id}/handle`, { method: 'PUT' })
          const data = await response.json()
          if (data.code === 200) ok += 1
        }
        headerAlarmRecords.value = []
        window.dispatchEvent(new CustomEvent('alarm-records-changed'))
        ElMessage.success(ok ? `已处理 ${ok} 条告警` : '没有可处理的告警')
      } catch (error) {
        console.error('批量处理顶部告警失败:', error)
        ElMessage.error('批量处理失败')
        await fetchHeaderNotifications()
      } finally {
        handlingAlarms.value = false
      }
    }

    // 监听登录状态变化
    const checkLoginStatus = () => {
      const loggedIn = localStorage.getItem('token') !== null
      const wasLoggedIn = isLoggedIn.value
      isLoggedIn.value = loggedIn
      currentUser.value = JSON.parse(localStorage.getItem('user') || '{}')
      if (!loggedIn) {
        headerAlarmRecords.value = []
        headerAlarmBootstrapped = false
        toastedAlarmIds.clear()
        disconnectAdminAlarmWs()
      } else if (!wasLoggedIn) {
        connectAdminAlarmWs()
        fetchHeaderNotifications()
      }
    }

    const handleMenuSelect = (key) => {
      router.push(key)
    }

    const logout = () => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      isLoggedIn.value = false
      headerAlarmRecords.value = []
      headerAlarmBootstrapped = false
      toastedAlarmIds.clear()
      disconnectAdminAlarmWs()
      router.push('/login')
    }

    const syncActiveMenu = (path) => {
      activeMenu.value = path.startsWith('/monitor/') ? '/monitor-center' : path
    }

    onMounted(() => {
      adminWsUnmounted = false
      syncActiveMenu(router.currentRoute.value.path)
      checkLoginStatus()
      // 错开首页看板首屏请求，减少并发 502
      window.setTimeout(() => {
        fetchHeaderNotifications()
        if (isLoggedIn.value) connectAdminAlarmWs()
      }, 2500)

      // 轮询作 WS 兜底（红点 + 漏推时补弹窗）
      alarmPollTimer = startVisibilityInterval(() => {
        fetchHeaderNotifications()
      }, 15000)
      
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
      adminWsUnmounted = true
      disconnectAdminAlarmWs()
      window.removeEventListener('storage', checkLoginStatus)
      window.removeEventListener('alarm-records-changed', fetchHeaderNotifications)
      if (alarmPollTimer) {
        alarmPollTimer()
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
      handlingAlarms,
      handleHeaderAlarm,
      handleAllHeaderAlarms,
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
  background-color: var(--app-page-bg);
}

/* ===== Layout ===== */
.app-layout {
  display: flex;
  height: 100%;
  width: 100%;
}

/* ===== Sidebar：简洁深灰 + 主色高亮 ===== */
.sidebar {
  width: 240px;
  background: var(--app-sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 20;
  box-shadow: 1px 0 0 var(--app-border);
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
  background: rgba(0, 0, 0, 0.15);
  cursor: pointer;
  gap: 10px;
  transition: background 0.2s;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.sidebar-logo:hover {
  background: rgba(0, 0, 0, 0.22);
}

.sidebar-logo__icon {
  color: var(--app-primary);
}

.sidebar-logo__text {
  color: #f8fafc;
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
  border-radius: 8px;
  transition: background 0.2s ease, color 0.2s ease;
  font-size: 14px;
}

.sidebar-menu .el-menu-item:hover {
  background-color: var(--app-sidebar-hover) !important;
  color: #e2e8f0 !important;
}

.sidebar-menu .el-menu-item.is-active {
  background: var(--app-primary) !important;
  color: var(--app-sidebar-text-active) !important;
  font-weight: 600;
  box-shadow: none;
}

.sidebar-menu .el-menu-item.is-active .el-icon {
  color: var(--app-sidebar-text-active) !important;
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
  background: var(--app-header-bg);
  border-bottom: 1px solid var(--app-border);
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
  color: var(--app-primary);
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
  background: var(--app-primary-soft);
}

.header-icon {
  color: #64748b;
  cursor: pointer;
  transition: color 0.2s;
}

.header-icon:hover {
  color: var(--app-primary);
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

.notification-action {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #2563eb;
}

.notification-panel__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
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
  background: var(--app-primary-soft);
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
  background: var(--app-page-bg);
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