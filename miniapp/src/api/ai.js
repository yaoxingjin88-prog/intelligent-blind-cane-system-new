import { post, BASE_URL } from './index.js'

/**
 * 发送消息给 AI 助手（非流式）
 * @param {Array} messages [{role:'user'|'assistant', content:'...'}]
 */
export const chat = (messages) => {
  return post('/ai/chat', { messages })
}

/**
 * 语音识别：上传音频文件，返回识别文字
 * @param {string} filePath 本地音频文件路径
 * @param {string} format 音频格式 mp3 / pcm / wav
 * @returns {Promise<{text: string}>}
 */
export const stt = (filePath, format = 'mp3') => {
  const token = uni.getStorageSync('token')
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/ai/stt',
      filePath,
      name: 'file',
      formData: { format, rate: 16000 },
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        try {
          const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          if (data.code === 200) {
            resolve(data.data)
          } else {
            reject(new Error(data.msg || '语音识别失败'))
          }
        } catch (e) {
          reject(e)
        }
      },
      fail: reject
    })
  })
}

/**
 * 语音合成：传文字返回 MP3 ArrayBuffer
 * @param {string} text
 * @returns {Promise<ArrayBuffer>}
 */
export const tts = (text) => {
  const token = uni.getStorageSync('token')
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + '/ai/tts',
      method: 'POST',
      data: { text },
      responseType: 'arraybuffer',
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else {
          reject(new Error('TTS 请求失败: ' + res.statusCode))
        }
      },
      fail: reject
    })
  })
}

export default {
  chat,
  stt,
  tts
}
