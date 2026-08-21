import { create } from 'zustand'
import { jwtDecode } from 'jwt-decode'
import api from '../services/api'
import useChatStore from './chatStore'

function userFromToken(token) {
  try {
    const decoded = jwtDecode(token)
    return { username: decoded.sub, role: decoded.role }
  } catch {
    return null
  }
}

const storedToken = localStorage.getItem('token')

const useAuthStore = create((set, get) => ({
  token: storedToken || null,
  user: storedToken ? userFromToken(storedToken) : null,

  login: async (email, password) => {
    const res = await api.post('/auth/login', { email, password })
    const { token } = res.data
    const user = userFromToken(token)
    localStorage.setItem('token', token)
    set({ user, token })
    // Svuota la chat (in memoria, non legata all'utente) — altrimenti
    // cambiando account nella stessa sessione i messaggi restano visibili
    useChatStore.getState().clear()
    return user
  },

  register: async (username, email, password) => {
    const res = await api.post('/auth/register', { username, email, password })
    const { token } = res.data
    if (token) {
      const user = userFromToken(token)
      localStorage.setItem('token', token)
      set({ user, token })
      useChatStore.getState().clear()
    }
    return res.data
  },

  // Chiamato dopo cambio username — riceve il nuovo token dal backend
  updateToken: (token) => {
    const user = userFromToken(token)
    localStorage.setItem('token', token)
    set({ user, token })
  },

  logout: () => {
    localStorage.removeItem('token')
    set({ user: null, token: null })
    useChatStore.getState().clear()
  },

  isLoggedIn: () => !!get().token,
}))

export default useAuthStore
