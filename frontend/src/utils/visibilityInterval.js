/**
 * 页面可见时才轮询；切到后台自动暂停，回到前台立即补一次。
 * @returns stop 函数
 */
export function startVisibilityInterval(callback, intervalMs = 30000) {
  let timer = null

  const clear = () => {
    if (timer != null) {
      window.clearInterval(timer)
      timer = null
    }
  }

  const tick = () => {
    if (!document.hidden) {
      callback()
    }
  }

  const start = () => {
    clear()
    timer = window.setInterval(tick, intervalMs)
  }

  const onVisibility = () => {
    if (!document.hidden) {
      callback()
    }
  }

  document.addEventListener('visibilitychange', onVisibility)
  start()

  return () => {
    clear()
    document.removeEventListener('visibilitychange', onVisibility)
  }
}
