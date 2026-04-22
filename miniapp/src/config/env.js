// ======================================================
// 后端服务器地址统一配置
// 换 WiFi / 换电脑后 IP 变了？运行：
//   cd "d:\28149\Documents\pro (2)"
//   .\update-ip.ps1
// 脚本会自动检测本机 IP 并更新此文件
// ======================================================
export const BACKEND_HOST = '192.168.115.214:8081'

export const BASE_URL = `http://${BACKEND_HOST}/api`
export const WS_URL = `ws://${BACKEND_HOST}/ws/alarm`
