/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

import 'axios'

declare module 'axios' {
  export interface AxiosRequestConfig {
    __retryCount?: number
    __retryCountMax?: number
  }
}

declare global {
  interface Window {
    AMap: any
  }
}

export {}
