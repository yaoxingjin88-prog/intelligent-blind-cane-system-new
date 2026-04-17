import { get, post } from './index'

// 登录
export const login = (data) => {
  return post('/mini/login', data)
}

// 微信授权登录
export const wechatLogin = (data) => {
  return post('/mini/wechat-login', data)
}

// 注册
export const register = (data) => {
  return post('/mini/register', data)
}

// 退出登录
export const logout = () => {
  return post('/mini/logout')
}

// 获取用户信息
export const getUserInfo = () => {
  return get('/mini/user/info')
}

// 更新用户信息
export const updateUserInfo = (data) => {
  return put('/mini/user/info', data)
}
