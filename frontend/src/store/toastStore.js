import { create } from 'zustand'

const useToastStore = create((set) => ({
  toasts: [],
  show: (msg, type = 'error') =>
    set(s => ({ toasts: [...s.toasts, { id: Date.now() + Math.random(), msg, type }] })),
  remove: (id) =>
    set(s => ({ toasts: s.toasts.filter(t => t.id !== id) })),
}))

export default useToastStore
