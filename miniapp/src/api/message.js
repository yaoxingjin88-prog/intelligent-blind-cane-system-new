import { get, put } from './index'

// 获取消息列表
export const getMessageList = (params) => {
  return get('/mini/messages', params)
}

// 标记消息已读
export const markMessageRead = (id) => {
  return put(`/mini/messages/${id}/read`)
}

// 标记消息已读（别名）
export const markAsRead = markMessageRead

// 获取消息设置
export const getMessageSettings = () => {
  return get('/mini/messages/settings')
}

// 更新消息设置
export const updateMessageSettings = (data) => {
  return put('/mini/messages/settings', data)
}
