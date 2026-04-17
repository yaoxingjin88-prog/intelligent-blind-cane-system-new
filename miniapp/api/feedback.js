import { post } from './index'

// 提交意见反馈
export const submitFeedback = (data) => {
  return post('/mini/feedback', data)
}
