import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../services/api'
import useAuthStore from '../store/authStore'

function ChatAiPage() {
  const { token } = useAuthStore()
  const navigate = useNavigate()
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [sessionId, setSessionId] = useState(null)

  if (!token) {
    navigate('/login')
    return null
  }

  const handleSend = async () => {
    if (!input.trim() || loading) return

    const userMsg = { role: 'USER', content: input.trim() }
    setMessages(prev => [...prev, userMsg])
    setInput('')
    setLoading(true)

    try {
      const res = await api.post('/ai/chat', {
        message: userMsg.content,
        sessionId,
        sessionHistory: messages,
      })

      setSessionId(res.data.sessionId)

      const aiMsg = {
        role: 'AI',
        content: res.data.reply,
        suggestions: res.data.suggestions || [],
      }
      setMessages(prev => [...prev, aiMsg])
    } catch (err) {
      setMessages(prev => [...prev, {
        role: 'AI',
        content: err.response?.data?.error || 'Assistente temporaneamente non disponibile.',
        suggestions: [],
      }])
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div style={{ backgroundColor: '#0a0a0a', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Navbar />

      <div style={{ textAlign: 'center', padding: '40px 24px 24px' }}>
        <div style={{ fontSize: '48px', marginBottom: '12px' }}>🤖</div>
        <h1 style={{ fontSize: '32px', fontWeight: '800', color: 'white', marginBottom: '8px' }}>Chat AI</h1>
        <p style={{ color: '#9ca3af', fontSize: '15px' }}>
          Conosce la tua libreria e i tuoi gusti — chiedi un consiglio
        </p>
      </div>

      <div style={{ flex: 1, maxWidth: '760px', width: '100%', margin: '0 auto', padding: '0 24px', display: 'flex', flexDirection: 'column', gap: '20px', paddingBottom: '140px' }}>

        {messages.length === 0 && (
          <div style={{ textAlign: 'center', color: '#6b7280', marginTop: '40px' }}>
            <p style={{ marginBottom: '20px' }}>Inizia a chattare con il tuo assistente cinematografico!</p>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', justifyContent: 'center' }}>
              {[
                'Consigliami un thriller psicologico',
                'Film simile a Inception',
                'Serie TV da guardare questo weekend',
                "Film d'animazione per adulti",
              ].map(suggestion => (
                <button key={suggestion} onClick={() => setInput(suggestion)} style={{
                  padding: '8px 16px', backgroundColor: '#141414', border: '1px solid #333',
                  borderRadius: '20px', color: '#9ca3af', fontSize: '13px', cursor: 'pointer',
                }}>
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((msg, i) => (
          <div key={i} style={{ display: 'flex', justifyContent: msg.role === 'USER' ? 'flex-end' : 'flex-start' }}>
            <div style={{ maxWidth: '80%' }}>
              <div style={{
                padding: '14px 18px',
                borderRadius: msg.role === 'USER' ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
                backgroundColor: msg.role === 'USER' ? '#e50914' : '#141414',
                border: msg.role === 'AI' ? '1px solid #222' : 'none',
                color: 'white', fontSize: '14px', lineHeight: 1.6,
              }}>
                {msg.content}
              </div>

              {msg.suggestions?.length > 0 && (
                <div style={{ display: 'flex', gap: '12px', marginTop: '12px', flexWrap: 'wrap' }}>
                  {msg.suggestions.map((s, j) => (
                    // Fix: usa contentType con fallback su mediaType
                    <Link to={`/movie/${s.tmdbId}?type=${s.contentType || s.mediaType}`} key={j}>
                      <div style={{ backgroundColor: '#141414', border: '1px solid #222', borderRadius: '8px', overflow: 'hidden', width: '120px' }}
                        onMouseEnter={e => e.currentTarget.style.borderColor = '#e50914'}
                        onMouseLeave={e => e.currentTarget.style.borderColor = '#222'}
                      >
                        {s.posterPath ? (
                          <img src={`https://image.tmdb.org/t/p/w200${s.posterPath}`} alt={s.title}
                            style={{ width: '100%', height: '160px', objectFit: 'cover' }} />
                        ) : (
                          <div style={{ width: '100%', height: '160px', backgroundColor: '#222', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#555', fontSize: '28px' }}>🎬</div>
                        )}
                        <div style={{ padding: '8px' }}>
                          <div style={{ color: 'white', fontSize: '11px', fontWeight: '600', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{s.title}</div>
                          <div style={{ color: '#6b7280', fontSize: '11px' }}>{s.releaseYear}</div>
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}

        {loading && (
          <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
            <div style={{ padding: '14px 18px', backgroundColor: '#141414', border: '1px solid #222', borderRadius: '18px 18px 18px 4px', color: '#9ca3af', fontSize: '14px' }}>
              🤖 Sto pensando...
            </div>
          </div>
        )}
      </div>

      <div style={{ position: 'fixed', bottom: 0, left: 0, right: 0, backgroundColor: '#0a0a0a', borderTop: '1px solid #222', padding: '20px 24px' }}>
        <div style={{ maxWidth: '760px', margin: '0 auto', display: 'flex', gap: '12px', alignItems: 'flex-end' }}>
          <textarea
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Scrivi un messaggio... (Invio per inviare)"
            rows={1}
            style={{
              flex: 1, padding: '14px 18px', backgroundColor: '#141414',
              border: '1px solid #333', borderRadius: '12px', color: 'white',
              fontSize: '14px', resize: 'none', lineHeight: 1.5,
            }}
          />
          <button
            onClick={handleSend}
            disabled={loading || !input.trim()}
            style={{
              padding: '14px 24px',
              backgroundColor: loading || !input.trim() ? '#333' : '#e50914',
              border: 'none', borderRadius: '12px', color: 'white',
              fontSize: '15px', fontWeight: '600', cursor: loading || !input.trim() ? 'default' : 'pointer',
            }}
          >
            ➤
          </button>
        </div>
        <p style={{ textAlign: 'center', color: '#6b7280', fontSize: '12px', marginTop: '8px' }}>
          La chat mantiene il contesto della sessione
        </p>
      </div>
    </div>
  )
}

export default ChatAiPage