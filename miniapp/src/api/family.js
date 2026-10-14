import { get, post, del } from './index'

/** 当前家庭成员（含本人角色） */
export const getFamilyMembers = () => {
  return get('/mini/family/members')
}

/** 主监护人邀请子监护人 */
export const inviteFamilyMember = (data) => {
  return post('/mini/family/invite', data)
}

/** 主监护人移除子监护人 */
export const removeFamilyMember = (id) => {
  return del(`/mini/family/members/${id}`)
}
