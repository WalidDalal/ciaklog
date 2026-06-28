import { useSearchParams, Navigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { ChatCore } from '../components/ChatWidget'
import useAuthStore from '../store/authStore'

function ChatAiPage() {
  const { token } = useAuthStore()
  const [searchParams] = useSearchParams()
  const initialPrompt = searchParams.get('prompt') || null

  if (!token) return <Navigate to="/login" replace />

  return (
    <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Navbar />

      <div style={{ textAlign: 'center', padding: '40px 24px 16px' }}>
        <div style={{ fontSize: '48px', marginBottom: '12px' }}>🤖</div>
        <h1 style={{ fontSize: '32px', fontWeight: '800', color: 'var(--text)', marginBottom: '8px' }}>Chat AI</h1>
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
