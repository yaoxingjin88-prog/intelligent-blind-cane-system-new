const AMAP_KEY = 'b3611a5f92bf32771df5676f9436094c'
const AMAP_SRC = `https://webapi.amap.com/maps?v=1.4.15&key=${AMAP_KEY}&plugin=AMap.Scale,AMap.ToolBar,AMap.MapType,AMap.Heatmap`

let amapLoadingPromise = null

export function ensureAmap() {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('AMap is only available in browser'))
  }
  if (window.AMap) {
    return Promise.resolve(window.AMap)
  }
  if (amapLoadingPromise) {
    return amapLoadingPromise
  }

  amapLoadingPromise = new Promise((resolve, reject) => {
    const existing = document.querySelector(`script[data-amap-loader="true"]`)
    if (existing) {
      existing.addEventListener('load', () => resolve(window.AMap))
      existing.addEventListener('error', () => reject(new Error('高德地图脚本加载失败')))
      return
    }

    const script = document.createElement('script')
    script.src = AMAP_SRC
    script.async = true
    script.dataset.amapLoader = 'true'
    script.onload = () => {
      if (window.AMap) {
        resolve(window.AMap)
      } else {
        reject(new Error('高德地图未正确初始化'))
      }
    }
    script.onerror = () => {
      amapLoadingPromise = null
      reject(new Error('高德地图脚本加载失败'))
    }
    document.head.appendChild(script)
  })

  return amapLoadingPromise
}
