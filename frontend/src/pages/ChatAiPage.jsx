import { useSearchParams, Navigate, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { ChatCore } from '../components/ChatWidget'
import useChatStore from '../store/chatStore'
import useAuthStore from '../store/authStore'

function ChatAiPage() {
  const { token } = useAuthStore()
  const { open } = useChatStore()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const initialPrompt = searchParams.get('prompt') || null

  if (!token) return <Navigate to="/login" replace />

  return (
    // Il contenitore usava minHeight:100vh
    // senza altezza fissata e senza flexShrink/minHeight:0 sulla parte sotto —
    // ChatCore chiede height:100% al suo genitore per far scrollare solo i
    // messaggi e tenere l'input sempre ancorato in fondo (come nel popup a
    // metà schermo, che ha un'altezza fissa di 520px), ma senza un contesto
    // di altezza vincolata quella regola non aveva niente a cui ancorarsi: la
    // pagina cresceva con i messaggi invece di scrollare internamente, e
    // l'input finiva ovunque capitasse invece che in fondo allo schermo.
    <div style={{ backgroundColor: 'var(--bg)', height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <Navbar />

      <div style={{ textAlign: 'center', padding: '24px 24px 12px', flexShrink: 0 }}>
        <div style={{ fontSize: '36px', marginBottom: '8px' }}>🤖</div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '12px', marginBottom: '4px' }}>
          <h1 style={{ fontSize: '26px', fontWeight: '800', color: 'var(--text)' }}>Chat AI</h1>
          <button
            onClick={() => { open(); navigate(-1) }}
            title="Rimpicciolisci"
            style={{
              backgroundColor: 'transparent', border: '1px solid var(--border)',
              borderRadius: '8px', padding: '6px 12px',
              color: 'var(--text-dark)', fontSize: '13px', cursor: 'pointer',
            }}
          >⤡ Rimpicciolisci</button>
        </div>
        <p style={{ color: 'var(--text-muted)', fontSize: '14px' }}>
          Conosce la tua libreria e i tuoi gusti — chiedi un consiglio
        </p>
      </div>

      {/* ChatCore riusa i messaggi dallo store — la sessione sopravvive alla navigazione.
          minHeight: 0 è ciò che manca di solito ai figli flex per poter scrollare invece
          di spingere in basso il contenuto oltre lo schermo. */}
      <div style={{
        flex: 1, minHeight: 0, maxWidth: '760px', width: '100%', margin: '0 auto',
        padding: '0 24px', display: 'flex', flexDirection: 'column', overflow: 'hidden',
      }}>
        <ChatCore initialPrompt={initialPrompt} />
      </div>
    </div>
  )
}

export default ChatAiPage
