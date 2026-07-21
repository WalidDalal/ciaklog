import { create } from 'zustand'

const useToastStore = create((set) => ({
  toasts: [],
  show: (msg, type = 'error', duration = null) => {
    // duration null = auto (3000 per info/success, 5000 per error)
    const autoMs = type === 'error' ? 5000 : 3000
    set(s => {
      // Chiamate API in parallelo che falliscono nello stesso istante
      // (es. Home che carica classifiche + libreria + consigli AI insieme)
      // mostravano lo stesso toast 2-3 volte. Se esiste già un toast identico
      // (stesso msg+type) non lo duplica.
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