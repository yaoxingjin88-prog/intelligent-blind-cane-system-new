// 格式化日期时间
export const formatDateTime = (dateStr, format = 'YYYY-MM-DD HH:mm:ss') => {
  if (!dateStr) return ''
  let date
  if (dateStr instanceof Date) {
    date = dateStr
  } else if (typeof dateStr === 'number') {
    date = new Date(dateStr)
  } else if (typeof dateStr === 'string') {
    // 转换为iOS兼容格式
    date = new Date(dateStr.replace(' ', 'T'))
  } else {
    return ''
  }
  if (isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  
  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

// 格式化相对时间
export const formatRelativeTime = (dateStr) => {
  if (!dateStr) return ''
  let date
  if (dateStr instanceof Date) {
    date = dateStr
  } else if (typeof dateStr === 'number') {
    date = new Date(dateStr)
  } else if (typeof dateStr === 'string') {
    date = new Date(dateStr.replace(' ', 'T'))
  } else {
    return ''
  }
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const diff = now - date
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  
  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return formatDateTime(dateStr, 'YYYY-MM-DD')
}

// 格式化距离
export const formatDistance = (meters) => {
  if (meters < 1000) {
    return `${Math.round(meters)}m`
  }
  return `${(meters / 1000).toFixed(1)}km`
}

// 获取电池电量颜色
export const getBatteryColor = (level) => {
  if (level >= 60) return '#07c160'
  if (level >= 30) return '#ff976a'
  return '#ee0a24'
}

// 获取报警等级颜色
export const getAlarmLevelColor = (level) => {
  const colors = {
    high: '#ee0a24',
    medium: '#ff976a',
    low: '#999999'
  }
  return colors[level] || '#999999'
}

// 获取报警等级文字
export const getAlarmLevelText = (level) => {
  const texts = {
    high: '高危',
    medium: '中危',
    low: '低危'
  }
  return texts[level] || '未知'
}

// 防抖函数
export const debounce = (fn, delay = 300) => {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

// 节流函数
export const throttle = (fn, delay = 300) => {
  let lastTime = 0
  return function (...args) {
    const now = Date.now()
    if (now - lastTime >= delay) {
      fn.apply(this, args)
      lastTime = now
    }
  }
}

// 深拷贝
export const deepClone = (obj) => {
  if (obj === null || typeof obj !== 'object') return obj
  if (obj instanceof Date) return new Date(obj)
  if (obj instanceof Array) return obj.map(item => deepClone(item))
  if (obj instanceof Object) {
    const clonedObj = {}
    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        clonedObj[key] = deepClone(obj[key])
      }
    }
    return clonedObj
  }
}

// 生成唯一ID
export const generateId = () => {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

// 验证手机号
export const validatePhone = (phone) => {
  return /^1[3-9]\d{9}$/.test(phone)
}

// 验证身份证号
export const validateIdCard = (idCard) => {
  return /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/.test(idCard)
}
