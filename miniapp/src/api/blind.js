import { get, put } from './index'

export const getBlindProfile = () => {
  return get('/mini/blind-profile')
}

export const updateBlindProfile = (data) => {
  return put('/mini/blind-profile', data)
}
