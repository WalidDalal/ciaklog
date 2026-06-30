import { create } from 'zustand'

// Memoria della chat persistente durante la sessione (non si perde chiudendo la floating)
const useChatStore = create((set, get) => ({
  messages: [],
  sessionId: null,
  isOpen: false,

  open: () => set({ isOpen: true }),
  close: () => set({ isOpen: false }),
  toggle: () => set(s => ({ isOpen: !s.isOpen })),

  addMessage: (msg) => set(s => ({ messages: [...s.messages, msg] })),
  setSessionId: (id) => set({ sessionId: id }),
  clear: () => set({ messages: [], sessionId: null }),
}))

export default useChatStore
