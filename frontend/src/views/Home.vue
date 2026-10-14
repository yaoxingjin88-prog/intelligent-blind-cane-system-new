<template>
  <div class="home-container">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-banner__content">
        <h2 class="welcome-banner__title">欢迎回来，{{ currentUser.username || '管理员' }}</h2>
        <p class="welcome-banner__desc">智能盲杖系统管理平台 — 实时监测设备状态、传感器数据和告警信息</p>
      </div>
      <div class="welcome-banner__deco"></div>
    </div>

    <!-- 核心数据卡片 -->
    <div class="stats-container">
      <div class="stat-card stat-card--blue">
        <div class="stat-card__body">
          <p class="stat-card__label">用户数量</p>
          <h3 class="stat-card__value">{{ userCount }}</h3>
          <p class="stat-card__sub">注册用户总数</p>
        </div>
        <div class="stat-card__icon stat-card__icon--blue">
          <el-icon :size="26"><User /></el-icon>
        </div>
        <div class="stat-card__circle stat-card__circle--blue"></div>
      </div>
      <div class="stat-card stat-card--green">
        <div class="stat-card__body">
          <p class="stat-card__label">设备数量</p>
          <h3 class="stat-card__value">{{ deviceCount }}</h3>
          <p class="stat-card__sub">{{ onlineDevices }} 台在线</p>
        </div>
        <div class="stat-card__icon stat-card__icon--green">
          <el-icon :size="26"><Monitor /></el-icon>
        </div>
        <div class="stat-card__circle stat-card__circle--green"></div>
      </div>
      <div class="stat-card stat-card--orange">
        <div class="stat-card__body">
          <p class="stat-card__label">告警数量</p>
          <h3 class="stat-card__value">{{ alarmCount }}</h3>
          <p class="stat-card__sub">待处理告警</p>
        </div>
        <div class="stat-card__icon stat-card__icon--orange">
          <el-icon :size="26"><Bell /></el-icon>
        </div>
        <div class="stat-card__circle stat-card__circle--orange"></div>
      </div>
      <div class="stat-card stat-card--purple">
        <div class="stat-card__body">
          <p class="stat-card__label">传感器数据</p>
          <h3 class="stat-card__value">{{ sensorDataCount }}</h3>
          <p class="stat-card__sub">数据采集条数</p>
        </div>
        <div class="stat-card__icon stat-card__icon--purple">
          <el-icon :size="26"><DataAnalysis /></el-icon>
        </div>
        <div class="stat-card__circle stat-card__circle--purple"></div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-container">
      <div class="chart-card">
        <div class="chart-card__header">
          <span class="chart-card__title">设备状态分布</span>
        </div>
        <div ref="deviceStatusChart" class="chart-item"></div>
      </div>
      <div class="chart-card">
        <div class="chart-card__header">
          <span class="chart-card__title">告警类型分布</span>
        </div>
        <div ref="alarmTypeChart" class="chart-item"></div>
      </div>
      <div class="chart-card chart-card--full">
        <div class="chart-card__header">
          <span class="chart-card__title">传感器数据趋势</span>
        </div>
        <div ref="sensorDataChart" class="chart-item chart-item--tall"></div>
      </div>
    </div>

    <!-- 最近告警记录 -->
    <div class="table-card">
      <div class="table-card__header">
        <span class="table-card__title">最近告警记录</span>
        <el-button type="primary" link @click="goToAlarms">查看全部 →</el-button>
      </div>
      <el-table :data="recentAlarms" style="width: 100%" size="default" :header-cell-style="{ background: '#f8fafc', color: '#334155', fontWeight: 600 }">
        <el-table-column prop="deviceId" label="设备ID" min-width="120" />
        <el-table-column prop="alarmType" label="告警类型" min-width="120">
          <template #default="scope">
            <el-tag size="small" effect="light" :type="scope.row.alarmType === '摔倒' || scope.row.alarmType === 'SOS呼救' ? 'danger' : scope.row.alarmType === '障碍物' ? 'warning' : 'info'">{{ scope.row.alarmType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alarmTime" label="告警时间" min-width="180" />
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.status === '已处理' ? 'success' : 'danger'">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="80" align="right">
          <template #default="scope">
            <el-button type="primary" link size="small" @click="handleAlarm(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 系统状态监控 -->
    <div class="system-card">
      <div class="table-card__header">
        <span class="table-card__title">系统状态监控</span>
      </div>
      <div class="system-status-content">
        <div class="system-status-item">
          <div class="status-dot status-dot--green"></div>
          <el-icon class="status-icon"><Monitor /></el-icon>
          <div class="status-info">
            <div class="status-value">正常</div>
            <div class="status-label">系统运行状态</div>
          </div>
          <el-tag type="success" effect="light" size="small">运行中</el-tag>
        </div>
        <div class="system-status-item">
          <div class="status-dot status-dot--blue"></div>
          <el-icon class="status-icon"><VideoCamera /></el-icon>
          <div class="status-info">
            <div class="status-value">{{ onlineDevices }}</div>
            <div class="status-label">在线设备数</div>
          </div>
          <el-tag type="primary" effect="light" size="small">良好</el-tag>
        </div>
        <div class="system-status-item">
          <div class="status-dot status-dot--gray"></div>
          <el-icon class="status-icon"><Cpu /></el-icon>
          <div class="status-info">
            <div class="status-value">25%</div>
            <div class="status-label">系统负载</div>
          </div>
          <el-tag type="info" effect="light" size="small">正常</el-tag>
        </div>
        <div class="system-status-item">
          <div class="status-dot status-dot--orange"></div>
          <el-icon class="status-icon"><Document /></el-icon>
          <div class="status-info">
            <div class="status-value">12.5 GB</div>
            <div class="status-label">可用空间</div>
          </div>
          <el-tag type="warning" effect="light" size="small">充足</el-tag>
        </div>
      </div>
    </div>

    <!-- 最新设备数据 -->
    <div class="table-card">
      <div class="table-card__header">
        <span class="table-card__title">最新设备数据</span>
        <el-button type="primary" link @click="goToDataAnalysis">查看全部 →</el-button>
      </div>
      <el-table :data="latestDeviceData" style="width: 100%" size="default" :header-cell-style="{ background: '#f8fafc', color: '#334155', fontWeight: 600 }">
        <el-table-column prop="deviceId" label="设备ID" min-width="120" />
        <el-table-column prop="obstacleDistance" label="障碍物距离(cm)" min-width="140" />
        <el-table-column prop="isFall" label="跌倒状态" width="100">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.isFall ? 'danger' : 'success'">{{ scope.row.isFall ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="temperature" label="温度(°C)" min-width="100" />
        <el-table-column prop="humidity" label="湿度(%)" min-width="100" />
        <el-table-column prop="dataTime" label="采集时间" min-width="180" />
      </el-table>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions-container">
      <div class="quick-action-card" @click="goToAddUser">
        <div class="quick-action-card__inner quick-action-card__inner--blue">
          <el-icon class="action-icon"><Plus /></el-icon>
          <span>添加用户</span>
        </div>
      </div>
      <div class="quick-action-card" @click="goToAddDevice">
        <div class="quick-action-card__inner quick-action-card__inner--green">
          <el-icon class="action-icon"><Monitor /></el-icon>
          <span>添加设备</span>
        </div>
      </div>
      <div class="quick-action-card" @click="goToViewAlarms">
        <div class="quick-action-card__inner quick-action-card__inner--orange">
          <el-icon class="action-icon"><Bell /></el-icon>
          <span>查看告警</span>
        </div>
      </div>
      <div class="quick-action-card" @click="goToDataAnalysis">
        <div class="quick-action-card__inner quick-action-card__inner--purple">
          <el-icon class="action-icon"><DataAnalysis /></el-icon>
          <span>数据分析</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { User, Monitor, Bell, DataAnalysis, Plus, VideoCamera, Cpu, Document } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { apiFetch } from '../utils/http'

export default {
  name: 'Home',
  components: {
    User,
    Monitor,
    Bell,
    DataAnalysis,
    Plus,
    VideoCamera,
    Cpu,
    Document
  },
  setup() {
    const router = useRouter()
    const userCount = ref(0)
    const deviceCount = ref(0)
    const alarmCount = ref(0)
    const sensorDataCount = ref(0)
    const recentAlarms = ref([])
    const onlineDevices = ref(0)
    const latestDeviceData = ref([])
    const currentUser = ref(JSON.parse(localStorage.getItem('user')) || {})
    
    // 图表引用
    const deviceStatusChart = ref(null)
    const alarmTypeChart = ref(null)
    const sensorDataChart = ref(null)
    
    // 图表实例
    let deviceStatusChartInstance = null
    let alarmTypeChartInstance = null
    let sensorDataChartInstance = null
    
    // 图表数据
    const chartData = ref({
      deviceStatus: {
        active: 0,
        offline: 0
      },
      alarmTypes: {
        obstacle: 0,
        fall: 0,
        battery: 0
      },
      sensorData: []
    })

    const fetchStats = async () => {
      try {
        let usersData = { data: [] }
        let devicesData = { data: [] }
        let alarmsData = { data: [] }
        let sensorDataData = { data: [] }

        try {
          const usersResponse = await apiFetch('/api/users')
          usersData = await usersResponse.json()
        } catch (error) {
          console.error('获取用户数据失败:', error)
        }

        try {
          const devicesResponse = await apiFetch('/api/devices')
          devicesData = await devicesResponse.json()
        } catch (error) {
          console.error('获取设备数据失败:', error)
        }

        try {
          const alarmsResponse = await apiFetch('/api/alarm-records')
          alarmsData = await alarmsResponse.json()
        } catch (error) {
          console.error('获取告警数据失败:', error)
        }

        try {
          const sensorDataResponse = await apiFetch('/api/sensor-data')
          sensorDataData = await sensorDataResponse.json()
        } catch (error) {
          console.error('获取传感器数据失败:', error)
        }

        userCount.value = usersData.data ? usersData.data.length : 0
        deviceCount.value = devicesData.data ? devicesData.data.length : 0
        alarmCount.value = alarmsData.data ? alarmsData.data.length : 0
        sensorDataCount.value = sensorDataData.data ? sensorDataData.data.length : 0

        if (alarmsData.data) {
          recentAlarms.value = alarmsData.data
            .sort((a, b) => new Date(b.alarmTime) - new Date(a.alarmTime))
            .slice(0, 5)
        } else {
          recentAlarms.value = []
        }

        if (sensorDataData.data) {
          latestDeviceData.value = sensorDataData.data
            .sort((a, b) => new Date(b.dataTime || b.createdAt) - new Date(a.dataTime || a.createdAt))
            .slice(0, 5)
        } else {
          latestDeviceData.value = []
        }

        if (devicesData.data) {
          onlineDevices.value = devicesData.data.filter(d => d.status === '在线' || d.status === 'active' || d.status === 'ACTIVE').length
        } else {
          onlineDevices.value = 0
        }

        processChartData(devicesData.data, alarmsData.data, sensorDataData.data)
        updateCharts()
      } catch (error) {
        console.error('获取统计数据失败:', error)
        userCount.value = 0
        deviceCount.value = 0
        alarmCount.value = 0
        sensorDataCount.value = 0
        recentAlarms.value = []
        latestDeviceData.value = []
        onlineDevices.value = 0
        chartData.value = {
          deviceStatus: { active: 0, offline: 0 },
          alarmTypes: { obstacle: 0, fall: 0, battery: 0 },
          sensorData: []
        }
        updateCharts()
      }
    }

    const processChartData = (devices, alarms, sensorData) => {
      if (devices) {
        chartData.value.deviceStatus.active = devices.filter(d => d.status === '在线' || d.status === 'active' || d.status === 'ACTIVE').length
        chartData.value.deviceStatus.offline = devices.filter(d => d.status === '离线' || d.status === 'offline' || d.status === 'OFFLINE').length
      }

      if (alarms) {
        chartData.value.alarmTypes.obstacle = alarms.filter(a => a.alarmType === '障碍物' || a.alarmType === 'obstacle' || a.alarmType === 'OBSTACLE').length
        chartData.value.alarmTypes.fall = alarms.filter(a => a.alarmType === '摔倒' || a.alarmType === 'fall' || a.alarmType === 'FALL' || (a.alarmType && a.alarmType.includes('摔'))).length
        chartData.value.alarmTypes.battery = alarms.filter(a => a.alarmType === '电池' || a.alarmType === 'battery' || a.alarmType === 'BATTERY' || (a.alarmType && a.alarmType.includes('电'))).length
      }

      if (sensorData && sensorData.length > 0) {
        chartData.value.sensorData = sensorData.slice(-20).map(item => ({
          time: item.dataTime || item.createdAt || '',
          distance: item.obstacleDistance != null ? item.obstacleDistance : 0
        }))
      } else {
        chartData.value.sensorData = []
      }
    }

    const goToAlarms = () => {
      router.push('/alarm-records')
    }

    const handleAlarm = () => {
      router.push('/alarm-records')
    }
    
    // 跳转到添加用户页面
    const goToAddUser = () => {
      router.push({ path: '/users', query: { action: 'add' } })
    }
    
    // 跳转到添加设备页面
    const goToAddDevice = () => {
      router.push({ path: '/devices', query: { action: 'add' } })
    }
    
    // 跳转到查看告警页面
    const goToViewAlarms = () => {
      router.push('/alarm-records')
    }
    
    // 跳转到数据分析页面
    const goToDataAnalysis = () => {
      router.push('/sensor-data')
    }
    
    // 登出功能
    const logout = () => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      router.push('/login')
    }
    
    const initCharts = () => {
      if (deviceStatusChart.value) {
        deviceStatusChartInstance = echarts.init(deviceStatusChart.value)
      }
      if (alarmTypeChart.value) {
        alarmTypeChartInstance = echarts.init(alarmTypeChart.value)
      }
      if (sensorDataChart.value) {
        sensorDataChartInstance = echarts.init(sensorDataChart.value)
      }
      updateCharts()
    }
    
    // 更新图表
    const updateCharts = () => {
      // 设备状态图表
      if (deviceStatusChartInstance) {
        deviceStatusChartInstance.setOption({
          tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b}: {c} ({d}%)',
            backgroundColor: 'rgba(0, 0, 0, 0.7)',
            borderColor: '#409eff',
            textStyle: {
              color: '#fff'
            }
          },
          legend: {
            bottom: '2%',
            left: 'center',
            textStyle: {
              color: '#666'
            }
          },
          series: [
            {
              name: '设备状态',
              type: 'pie',
              radius: ['35%', '65%'],
              center: ['50%', '45%'],
              avoidLabelOverlap: false,
              itemStyle: {
                borderRadius: 10,
                borderColor: '#fff',
                borderWidth: 2
              },
              label: {
                show: false,
                position: 'center'
              },
              emphasis: {
                label: {
                  show: true,
                  fontSize: 20,
                  fontWeight: 'bold'
                },
                itemStyle: {
                  shadowBlur: 15,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0, 0, 0, 0.8)'
                }
              },
              labelLine: {
                show: false
              },
              animationType: 'scale',
              animationEasing: 'elasticOut',
              animationDelay: function (idx) {
                return idx * 80;
              },
              data: [
                { value: chartData.value.deviceStatus.active, name: '在线', itemStyle: { color: '#3b82f6' } },
                { value: chartData.value.deviceStatus.offline || 1, name: '离线', itemStyle: { color: '#e2e8f0' } }
              ]
            }
          ]
        })
      }
      
      // 告警类型图表
      if (alarmTypeChartInstance) {
        alarmTypeChartInstance.setOption({
          tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b}: {c} ({d}%)',
            backgroundColor: 'rgba(0, 0, 0, 0.7)',
            borderColor: '#e6a23c',
            textStyle: {
              color: '#fff'
            }
          },
          legend: {
            bottom: '2%',
            left: 'center',
            textStyle: {
              color: '#666'
            }
          },
          series: [
            {
              name: '告警类型',
              type: 'pie',
              radius: ['35%', '65%'],
              center: ['50%', '45%'],
              avoidLabelOverlap: false,
              itemStyle: {
                borderRadius: 10,
                borderColor: '#fff',
                borderWidth: 2
              },
              label: {
                show: false,
                position: 'center'
              },
              emphasis: {
                label: {
                  show: true,
                  fontSize: 20,
                  fontWeight: 'bold'
                },
                itemStyle: {
                  shadowBlur: 15,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0, 0, 0, 0.8)'
                }
              },
              labelLine: {
                show: false
              },
              animationType: 'scale',
              animationEasing: 'elasticOut',
              animationDelay: function (idx) {
                return idx * 80;
              },
              data: [
                { value: chartData.value.alarmTypes.obstacle, name: '障碍物', itemStyle: { color: '#409eff' } },
                { value: chartData.value.alarmTypes.fall, name: '跌倒', itemStyle: { color: '#e6a23c' } },
                { value: chartData.value.alarmTypes.battery, name: '电池', itemStyle: { color: '#f56c6c' } }
              ]
            }
          ]
        })
      }
      
      // 传感器数据趋势图表
      if (sensorDataChartInstance) {
        sensorDataChartInstance.setOption({
          title: {
            text: '传感器数据趋势',
            left: 'center',
            textStyle: {
              fontSize: 16,
              fontWeight: 'bold',
              color: '#333'
            }
          },
          tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(0, 0, 0, 0.7)',
            borderColor: '#409eff',
            textStyle: {
              color: '#fff'
            }
          },
          legend: {
            data: ['障碍物距离'],
            bottom: '5%',
            left: 'center',
            textStyle: {
              color: '#666'
            }
          },
          grid: {
            left: '3%',
            right: '4%',
            bottom: '15%',
            containLabel: true
          },
          xAxis: {
            type: 'category',
            boundaryGap: false,
            data: chartData.value.sensorData.map(item => item.time),
            axisLabel: {
              rotate: 45,
              fontSize: 10,
              color: '#666'
            },
            axisLine: {
              lineStyle: {
                color: '#ccc'
              }
            }
          },
          yAxis: {
            type: 'value',
            name: '距离 (cm)',
            nameTextStyle: {
              color: '#666'
            },
            axisLabel: {
              color: '#666'
            },
            axisLine: {
              lineStyle: {
                color: '#ccc'
              }
            },
            splitLine: {
              lineStyle: {
                color: '#f0f0f0'
              }
            }
          },
          series: [
            {
              name: '障碍物距离',
              type: 'line',
              smooth: true,
              symbol: 'circle',
              symbolSize: 6,
              lineStyle: {
                color: '#409eff',
                width: 3
              },
              itemStyle: {
                color: '#409eff',
                borderColor: '#fff',
                borderWidth: 2
              },
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  {
                    offset: 0,
                    color: 'rgba(64, 158, 255, 0.5)'
                  },
                  {
                    offset: 1,
                    color: 'rgba(64, 158, 255, 0.1)'
                  }
                ])
              },
              animationType: 'monotone',
              animationEasing: 'elasticOut',
              animationDelay: function (idx) {
                return idx * 100;
              },
              data: chartData.value.sensorData.map(item => item.distance)
            }
          ]
        })
      }
    }
    
    // 响应式调整
    const handleResize = () => {
      deviceStatusChartInstance?.resize()
      alarmTypeChartInstance?.resize()
      sensorDataChartInstance?.resize()
    }

    onMounted(() => {
      fetchStats()
      // 延迟初始化图表，确保DOM已渲染
      setTimeout(() => {
        initCharts()
        window.addEventListener('resize', handleResize)
      }, 100)
    })
    
    onUnmounted(() => {
      window.removeEventListener('resize', handleResize)
      deviceStatusChartInstance?.dispose()
      alarmTypeChartInstance?.dispose()
      sensorDataChartInstance?.dispose()
    })

    return {
      userCount,
      deviceCount,
      alarmCount,
      sensorDataCount,
      deviceStatusChart,
      alarmTypeChart,
      sensorDataChart,
      recentAlarms,
      onlineDevices,
      latestDeviceData,
      currentUser,
      goToAlarms,
      handleAlarm,
      goToAddUser,
      goToAddDevice,
      goToViewAlarms,
      goToDataAnalysis,
      logout
    }
  }
}
</script>

<style scoped>
/* ===== Base ===== */
.home-container {
  width: 100%;
  padding: 24px;
  min-height: 100vh;
}

/* ===== Welcome Banner ===== */
.welcome-banner {
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 50%, #8b5cf6 100%);
  border-radius: 16px;
  padding: 28px 32px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.25);
}

.welcome-banner__content {
  position: relative;
  z-index: 2;
}

.welcome-banner__title {
  font-size: 22px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 6px;
}

.welcome-banner__desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.welcome-banner__deco {
  position: absolute;
  right: -30px;
  top: -30px;
  width: 180px;
  height: 180px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.welcome-banner__deco::after {
  content: '';
  position: absolute;
  right: 40px;
  bottom: -50px;
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

/* ===== Stat Cards ===== */
.stats-container {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  min-width: 0;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px -4px rgba(0, 0, 0, 0.1);
}

.stat-card--blue { border-left: 4px solid #3b82f6; }
.stat-card--green { border-left: 4px solid #10b981; }
.stat-card--orange { border-left: 4px solid #f59e0b; }
.stat-card--purple { border-left: 4px solid #8b5cf6; }

.stat-card__body {
  position: relative;
  z-index: 2;
  min-width: 0;
  flex: 1;
}

.stat-card__label {
  font-size: 13px;
  color: #94a3b8;
  margin: 0 0 4px;
  font-weight: 500;
}

.stat-card__value {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  line-height: 1.2;
}

.stat-card__sub {
  font-size: 12px;
  color: #94a3b8;
  margin: 6px 0 0;
}

.stat-card__icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
  flex-shrink: 0;
}

.stat-card__icon--blue { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.stat-card__icon--green { background: rgba(16, 185, 129, 0.1); color: #10b981; }
.stat-card__icon--orange { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
.stat-card__icon--purple { background: rgba(139, 92, 246, 0.1); color: #8b5cf6; }

.stat-card__circle {
  position: absolute;
  right: -20px;
  bottom: -20px;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  opacity: 0.06;
}

.stat-card__circle--blue { background: #3b82f6; }
.stat-card__circle--green { background: #10b981; }
.stat-card__circle--orange { background: #f59e0b; }
.stat-card__circle--purple { background: #8b5cf6; }

/* ===== Chart Cards ===== */
.charts-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card {
  background: #ffffff;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.chart-card:hover {
  box-shadow: 0 8px 20px -4px rgba(0, 0, 0, 0.08);
}

.chart-card--full {
  grid-column: 1 / -1;
}

.chart-card__header {
  padding: 18px 22px 0;
}

.chart-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.chart-item {
  width: 100%;
  height: 300px;
  padding: 12px 16px 16px;
}

.chart-item--tall {
  height: 380px;
}

/* ===== Table Cards ===== */
.table-card, .system-card {
  background: #ffffff;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
  transition: all 0.3s ease;
}

.table-card:hover, .system-card:hover {
  box-shadow: 0 8px 20px -4px rgba(0, 0, 0, 0.08);
}

.table-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 22px;
  border-bottom: 1px solid #f1f5f9;
}

.table-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

/* ===== System Status ===== */
.system-status-content {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 20px;
}

.system-status-item {
  display: flex;
  align-items: center;
  padding: 14px;
  background: #f8fafc;
  border-radius: 12px;
  transition: all 0.25s ease;
  gap: 10px;
}

.system-status-item:hover {
  background: #f1f5f9;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.04);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot--green { background: #10b981; box-shadow: 0 0 6px rgba(16, 185, 129, 0.4); }
.status-dot--blue { background: #3b82f6; box-shadow: 0 0 6px rgba(59, 130, 246, 0.4); }
.status-dot--gray { background: #94a3b8; }
.status-dot--orange { background: #f59e0b; box-shadow: 0 0 6px rgba(245, 158, 11, 0.4); }

.status-icon {
  font-size: 20px;
  color: #64748b;
  flex-shrink: 0;
}

.status-info {
  flex: 1;
  min-width: 0;
}

.status-value {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.status-label {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

/* ===== Quick Actions ===== */
.quick-actions-container {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.quick-action-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.quick-action-card:hover {
  transform: translateY(-4px);
}

.quick-action-card__inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 28px 16px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.quick-action-card:hover .quick-action-card__inner--blue {
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  border-color: #93c5fd;
}
.quick-action-card:hover .quick-action-card__inner--green {
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  border-color: #6ee7b7;
}
.quick-action-card:hover .quick-action-card__inner--orange {
  background: linear-gradient(135deg, #fffbeb, #fef3c7);
  border-color: #fcd34d;
}
.quick-action-card:hover .quick-action-card__inner--purple {
  background: linear-gradient(135deg, #f5f3ff, #ede9fe);
  border-color: #c4b5fd;
}

.action-icon {
  font-size: 28px;
  margin-bottom: 10px;
  color: #64748b;
  transition: color 0.2s;
}

.quick-action-card:hover .quick-action-card__inner--blue .action-icon { color: #3b82f6; }
.quick-action-card:hover .quick-action-card__inner--green .action-icon { color: #10b981; }
.quick-action-card:hover .quick-action-card__inner--orange .action-icon { color: #f59e0b; }
.quick-action-card:hover .quick-action-card__inner--purple .action-icon { color: #8b5cf6; }

.quick-action-card__inner span {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

/* ===== Responsive ===== */
@media (max-width: 1200px) {
  .stats-container { grid-template-columns: repeat(2, 1fr); }
  .system-status-content { grid-template-columns: repeat(2, 1fr); }
  .quick-actions-container { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .home-container { padding: 16px; }
  .stats-container { grid-template-columns: 1fr; }
  .charts-container { grid-template-columns: 1fr; }
  .system-status-content { grid-template-columns: 1fr; }
  .quick-actions-container { grid-template-columns: repeat(2, 1fr); }
  .chart-item { height: 250px; }
  .chart-item--tall { height: 300px; }
  .welcome-banner { padding: 20px; }
  .welcome-banner__title { font-size: 18px; }
}
</style>