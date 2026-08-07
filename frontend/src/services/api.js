import axios from 'axios'
import { jwtDecode } from 'jwt-decode'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
})

// Fix (🟡 nessuna gestione errori di rete — trovato nei test funzionali):
// l'interceptor gestiva solo `err.response?.data?.details` e 401, ma un
// errore di rete VERO (backend spento/irraggiungibile, `err.response`
// undefined, es. ERR_NETWORK) non veniva mai controllato. Ogni pagina ha
// `.catch(() => {})` sulle sue chiamate — niente crash (bene), ma zero
// feedback: le sezioni restavano semplicemente vuote, dando l'impressione
// di un'app rotta invece di comunicare chiaramente "il server non risponde".
// Throttle a 5s: una pagina può avere decine di chiamate in parallelo, senza
// questo si spammerebbero altrettanti toast identici tutti insieme.
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

    // Fix (🟡 nessuna gestione errori di rete): err.response è undefined solo
    // per errori di rete veri (backend giù, DNS, CORS, timeout) — un errore
    // HTTP normale (400/404/500...) ha SEMPRE err.response valorizzato,
    // quindi questo controllo non interferisce con nessuna delle gestioni
    // sotto (401, details, ecc.), che restano invariate.
    if (!err.response) {
      notifyNetworkError()
      return Promise.reject(err)
    }

    // Il backend manda il messaggio specifico in `details` (per campo),
    // ma tutte le pagine leggono solo `err.response?.data?.error`, che è sempre
    // il generico "Dati non validi". Centralizzato qui una volta sola: se
    // `details` esiste, riscrive `error` col messaggio specifico prima di
    // rilanciare, così ogni pagina lo riceve corretto senza essere toccata.
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
