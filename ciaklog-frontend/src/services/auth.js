// Mantenuto per retrocompatibilità — usa authStore direttamente nei componenti
import useAuthStore from '../store/authStore'

export const getUser = () => JSON.parse(localStorage.getItem('user'))
export const isLoggedIn = () => !!localStorage.getItem('token')
export const logout = () => useAuthStore.getState().logout()
export const login = (u, p) => useAuthStore.getState().login(u, p)
export const register = (u, e, p) => useAuthStore.getState().register(u, e, p)
