import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue')
  },
  {
    path: '/users',
    name: 'Users',
    component: () => import('../views/Users.vue')
  },
  {
    path: '/devices',
    name: 'Devices',
    component: () => import('../views/Devices.vue')
  },
  {
    path: '/sensor-data',
    name: 'SensorData',
    component: () => import('../views/SensorData.vue')
  },
  {
    path: '/alarm-records',
    name: 'AlarmRecords',
    component: () => import('../views/AlarmRecords.vue')
  },
  {
    path: '/monitor-center',
    name: 'MonitoringCenter',
    component: () => import('../views/MonitoringCenter.vue')
  },
  {
    path: '/fences',
    name: 'Fences',
    component: () => import('../views/Fences.vue')
  },
  {
    path: '/trajectory-playback',
    name: 'TrajectoryPlayback',
    component: () => import('../views/TrajectoryPlayback.vue')
  },
  {
    path: '/monitor/:deviceId',
    name: 'RealTimeMonitor',
    component: () => import('../views/RealTimeMonitor.vue'),
    props: true
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from) => {
  // 检查是否有登录令牌
  const token = localStorage.getItem('token')
  
  // 不需要登录的页面
  if (to.path === '/login') {
    return true
  }
  
  // 需要登录的页面
  if (!token) {
    return '/login'
  }
  
  return true
})

export default router