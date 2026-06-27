import { defineStore } from 'pinia'
import { jwtDecode } from 'jwt-decode'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('wmdb_token') || '',
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    realName: (state) => {
      if (!state.token) return ''
      try {
        const decoded: any = jwtDecode(state.token)
        return decoded.realName || ''
      } catch (e) {
        return ''
      }
    }
  },
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('wmdb_token', token)
    },
    logout() {
      this.token = ''
      localStorage.removeItem('wmdb_token')
    }
  }
})
