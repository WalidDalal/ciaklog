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
    <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Navbar />

      <div style={{ textAlign: 'center', padding: '40px 24px 16px' }}>
        <div style={{ fontSize: '48px', marginBottom: '12px' }}>🤖</div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '12px', marginBottom: '8px' }}>
          <h1 style={{ fontSize: '32px', fontWeight: '800', color: 'var(--text)' }}>Chat AI</h1>
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
        <p style={{ color: 'var(--text-muted)', fontSize: '15px' }}>
          Conosce la tua libreria e i tuoi gusti — chiedi un consiglio
        </p>
      </div>

      {/* ChatCore riusa i messaggi dallo store — la sessione sopravvive alla navigazione */}
      <div style={{
        flex: 1, maxWidth: '760px', width: '100%', margin: '0 auto',
        padding: '0 24px 100px', display: 'flex', flexDirection: 'column',
      }}>
        <ChatCore initialPrompt={initialPrompt} />
      </div>
    </div>
  )
}

export default ChatAiPage
