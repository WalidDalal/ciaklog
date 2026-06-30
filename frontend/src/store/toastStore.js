import { create } from 'zustand'

const useToastStore = create((set) => ({
  toasts: [],
  show: (msg, type = 'error', duration = null) => {
    // duration null = auto (3000 per info/success, 5000 per error)
    const autoMs = type === 'error' ? 5000 : 3000
    set(s => ({
      toasts: [...s.toasts, {
        id: Date.now() + Math.random(),
        msg,
        type,
        duration: duration ?? autoMs,
      }]
    }))
  },
  remove: (id) =>
      set(s => ({ toasts: s.toasts.filter(t => t.id !== id) })),
}))

export default useToastStore