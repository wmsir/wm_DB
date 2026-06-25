import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('wmdb_token') || '',
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
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
