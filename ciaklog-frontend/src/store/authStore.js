import { create } from 'zustand'
import api from '../services/api'

const useAuthStore = create((set) => ({
  user: JSON.parse(localStorage.getItem('user')) || null,
  token: localStorage.getItem('token') || null,

  login: async (email, password) => {
    const res = await api.post('/auth/login', { email, password })
    const { token, ...user } = res.data
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
    set({ user, token })
    return user
  },

  register: async (username, email, password) => {
    const res = await api.post('/auth/register', { username, email, password })
    return res.data
  },

  logout: () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    set({ user: null, token: null })
  },

  isLoggedIn: () => !!localStorage.getItem('token'),
}))

export default useAuthStore