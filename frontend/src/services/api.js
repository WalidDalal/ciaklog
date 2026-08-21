import axios from 'axios'
import { jwtDecode } from 'jwt-decode'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
})

// Throttle a 5s: una pagina può avere decine di chiamate in parallelo,
// senza questo si spammerebbero altrettanti toast identici tutti insieme.
let lastNetworkErrorToastAt = 0
function notifyNetworkError() {
  const now = Date.now()
  if (now - lastNetworkErrorToastAt < 5000) return
  lastNetworkErrorToastAt = now
  import('../store/toastStore').then(({ default: useToastStore }) => {
    useToastStore.getState().show('Impossibile connettersi al server — controlla la connessione o riprova più tardi')
  })
}

function isTokenExpired(token) {
  try {
    return jwtDecode(token).exp < Date.now() / 1000
  } catch {
    return true
  }
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (!token) return config

  if (isTokenExpired(token) && !config.url.includes('/auth/')) {
    // Token scaduto — pulizia e redirect con messaggio
    localStorage.removeItem('token')
    // Importazione dinamica per evitare circular dependency con authStore
    import('../store/toastStore').then(({ default: useToastStore }) => {
      useToastStore.getState().show('Sessione scaduta, effettua di nuovo il login', 'info')
    })
    // Piccolo delay per far apparire il toast prima del redirect
    setTimeout(() => { window.location.href = '/login' }, 800)
    return Promise.reject(new axios.Cancel('Token scaduto'))
  }

  config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  res => res,
  err => {
    if (axios.isCancel(err)) return Promise.reject(err)

    // err.response è undefined solo per errori di rete veri (backend giù,
    // DNS, CORS, timeout) — un errore HTTP normale ha sempre err.response
    if (!err.response) {
      notifyNetworkError()
      return Promise.reject(err)
    }

    // Il backend manda il messaggio specifico in `details` (per campo), ma
    // le pagine leggono solo `error` — riscritto qui una volta sola invece
    // che in ogni pagina
    if (err.response?.data?.details) {
      const messages = Object.values(err.response.data.details)
      if (messages.length > 0) {
        err.response.data.error = messages.join(' ')
      }
    }

    // 401 dal backend (token manomesso o invalidato server-side)
    if (err.response?.status === 401 && !err.config.url.includes('/auth/')) {
      localStorage.removeItem('token')
      import('../store/toastStore').then(({ default: useToastStore }) => {
        useToastStore.getState().show('Sessione scaduta, effettua di nuovo il login', 'info')
      })
      setTimeout(() => { window.location.href = '/login' }, 800)
    }
    return Promise.reject(err)
  }
)

export default api
