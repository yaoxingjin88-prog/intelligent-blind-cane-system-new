/** 首页/用户端实时数据刷新间隔（毫秒） */
export const POLL_INTERVAL_MS = 5000

/**
 * 页面级轮询：可 pause/resume，配合 onHide/onShow，避免后台空刷。
 * @returns {{ start: Function, stop: Function, pause: Function, resume: Function }}
 */
export function createPagePoll(callback, intervalMs = POLL_INTERVAL_MS) {
  let timer = null
  let active = false

  const tick = async () => {
    if (!active) return
    try {
      await callback()
    } catch (e) {
      console.error('轮询任务失败', e)
    }
  }

  const stop = () => {
    if (timer != null) {
      clearInterval(timer)
      timer = null
    }
    active = false
  }

  const start = () => {
    stop()
    active = true
    timer = setInterval(tick, intervalMs)
  }

  const pause = () => {
    active = false
  }

  const resume = () => {
    active = true
    tick()
  }

  return { start, stop, pause, resume }
}
