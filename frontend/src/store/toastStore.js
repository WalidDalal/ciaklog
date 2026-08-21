import { create } from 'zustand'

const useToastStore = create((set) => ({
  toasts: [],
  show: (msg, type = 'error', duration = null) => {
    // duration null = auto (3000 per info/success, 5000 per error)
    const autoMs = type === 'error' ? 5000 : 3000
    set(s => {
      // Evita duplicati quando più chiamate falliscono nello stesso istante
      // (es. Home che carica classifiche + libreria + consigli AI insieme)
      if (s.toasts.some(t => t.msg === msg && t.type === type)) {
        return s
      }
      return {
        toasts: [...s.toasts, {
          id: Date.now() + Math.random(),
          msg,
          type,
          duration: duration ?? autoMs,
        }]
      }
    })
  },
  remove: (id) =>
      set(s => ({ toasts: s.toasts.filter(t => t.id !== id) })),
}))

export default useToastStore