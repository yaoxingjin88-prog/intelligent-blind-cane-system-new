// 地图工具类

// 计算两点之间的距离（米）
export const calculateDistance = (lat1, lng1, lat2, lng2) => {
  const R = 6371000 // 地球半径（米）
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLng = (lng2 - lng1) * Math.PI / 180
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

// 判断点是否在圆内
export const isPointInCircle = (point, center, radius) => {
  const distance = calculateDistance(
    point.latitude,
    point.longitude,
    center.latitude,
    center.longitude
  )
  return distance <= radius
}

// 判断点是否在矩形内
export const isPointInRectangle = (point, rectangle) => {
  return point.latitude >= rectangle.south &&
         point.latitude <= rectangle.north &&
         point.longitude >= rectangle.west &&
         point.longitude <= rectangle.east
}

// 判断点是否在多边形内
export const isPointInPolygon = (point, polygon) => {
  let inside = false
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const xi = polygon[i].longitude, yi = polygon[i].latitude
    const xj = polygon[j].longitude, yj = polygon[j].latitude
    
    if (((yi > point.latitude) !== (yj > point.latitude)) &&
        (point.longitude < (xj - xi) * (point.latitude - yi) / (yj - yi) + xi)) {
      inside = !inside
    }
  }
  return inside
}

// 计算多边形中心点
export const calculatePolygonCenter = (polygon) => {
  let x = 0, y = 0
  polygon.forEach(point => {
    x += point.longitude
    y += point.latitude
  })
  return {
    longitude: x / polygon.length,
    latitude: y / polygon.length
  }
}

// 格式化坐标
export const formatCoordinate = (coord, precision = 6) => {
  return Number(coord).toFixed(precision)
}

// 地址解析（需要调用地图API）
export const geocode = async (latitude, longitude) => {
  // 这里需要调用高德地图或腾讯地图的逆地理编码API
  // 暂时返回模拟数据
  return '北京市朝阳区'
}

// 地址搜索（需要调用地图API）
export const searchAddress = async (keyword) => {
  // 这里需要调用高德地图或腾讯地图的地址搜索API
  // 暂时返回模拟数据
  return []
}
