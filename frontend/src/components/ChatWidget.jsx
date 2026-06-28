import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../services/api'
import useAuthStore from '../store/authStore'
import useChatStore from '../store/chatStore'

const SUGGESTIONS = [
  'Consigliami un thriller psicologico',
  'Film simile a Inception',
  'Serie TV da guardare questo weekend',
  "Film d'animazione per adulti",
]

// Componente puro della chat — usato sia nella floating che nella pagina intera
export function ChatCore({ compact = false, initialPrompt = null }) {
  const { token } = useAuthStore()
  const { messages, sessionId, addMessage, setSessionId, clear } = useChatStore()
  const [input, setInput] = useState(initialPrompt || '')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef(null)
  const autoSentRef = useRef(false)

  // Scroll automatico all'ultimo messaggio
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  // Auto-invio se arriva un prompt precompilato (es. da /movie/:id)
  useEffect(() => {
    if (initialPrompt && !autoSentRef.current && token) {
      autoSentRef.current = true
      setTimeout(() => handleSend(initialPrompt), 300)
    }
  }, [token]) // eslint-disable-line

  const handleSend = async (overrideText) => {
    const text = typeof overrideText === 'string' ? overrideText : input
    if (!text.trim() || loading) return

    const userMsg = { role: 'USER', content: text.trim() }
    addMessage(userMsg)
    setInput('')
    setLoading(true)

    try {
      const res = await api.post('/ai/chat', {
        message: text.trim(),
        sessionId,
        sessionHistory: messages,
      })
      setSessionId(res.data.sessionId)
      addMessage({
        role: 'AI',
        content: res.data.reply,
        suggestions: res.data.suggestions || [],
      })
    } catch (err) {
      addMessage({
        role: 'AI',
        content: err.response?.data?.error || 'Assistente temporaneamente non disponibile.',
        suggestions: [],
      })
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend() }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Header */}
      {!compact && (
        <div style={{ padding: '16px 20px', borderBottom: '1px solid #222', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexShrink: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontSize: '22px' }}>🤖</span>
            <div>
              <div style={{ color: 'var(--text)', fontWeight: '700', fontSize: '15px' }}>Assistente AI</div>
              <div style={{ color: 'var(--text-dark)', fontSize: '12px' }}>Conosce la tua libreria e i tuoi gusti</div>
            </div>
          </div>
          {messages.length > 0 && (
            <button onClick={clear} title="Nuova conversazione"
              style={{ backgroundColor: 'transparent', border: '1px solid #333', borderRadius: '6px', color: 'var(--text-dark)', fontSize: '12px', padding: '4px 10px', cursor: 'pointer' }}>
              Nuova chat
            </button>
          )}
        </div>
      )}

      {/* Messaggi */}
      <div style={{
        flex: 1, overflowY: 'auto', padding: compact ? '12px' : '16px 20px',
        display: 'flex', flexDirection: 'column', gap: '12px',
      }}>
        {messages.length === 0 && (
          <div style={{ textAlign: 'center', color: 'var(--text-dark)', marginTop: '24px' }}>
            <div style={{ fontSize: '32px', marginBottom: '12px' }}>🎬</div>
            <p style={{ fontSize: '13px', marginBottom: '16px' }}>Chiedimi un consiglio cinematografico!</p>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', justifyContent: 'center' }}>
              {SUGGESTIONS.map(s => (
                <button key={s} onClick={() => setInput(s)} style={{
                  padding: '6px 12px', backgroundColor: 'var(--bg-card)', border: '1px solid #333',
                  borderRadius: '16px', color: 'var(--text-muted)', fontSize: '12px', cursor: 'pointer',
                }}>{s}</button>
              ))}
            </div>
          </div>
        )}

        {messages.map((msg, i) => (
          <div key={i} style={{ display: 'flex', justifyContent: msg.role === 'USER' ? 'flex-end' : 'flex-start' }}>
            <div style={{ maxWidth: compact ? '90%' : '80%' }}>
              <div style={{
                padding: compact ? '10px 14px' : '12px 16px',
                borderRadius: msg.role === 'USER' ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
                backgroundColor: msg.role === 'USER' ? 'var(--accent)' : 'var(--bg-hover)',
                border: msg.role === 'AI' ? '1px solid #2a2a2a' : 'none',
                color: 'var(--text)', fontSize: compact ? '13px' : '14px', lineHeight: 1.6,
                whiteSpace: 'pre-wrap',
              }}>
                {msg.content}
              </div>

              {msg.suggestions?.length > 0 && (
                <div style={{ display: 'flex', gap: '8px', marginTop: '10px', flexWrap: 'wrap' }}>
                  {msg.suggestions.map((s, j) => (
                    <Link to={`/movie/${s.tmdbId}?type=${s.contentType || s.mediaType}`} key={j}
                      style={{ textDecoration: 'none' }}>
                      <div style={{
                        backgroundColor: 'var(--bg-card)', border: '1px solid #222',
                        borderRadius: '8px', overflow: 'hidden', width: compact ? '90px' : '110px',
                        transition: 'border-color 0.15s',
                      }}
                        onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--accent)'}
                        onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}
                      >
                        {s.posterPath
                          ? <img src={`https://image.tmdb.org/t/p/w200${s.posterPath}`} alt={s.title}
                              style={{ width: '100%', height: compact ? '120px' : '150px', objectFit: 'cover' }} />
                          : <div style={{ width: '100%', height: compact ? '120px' : '150px', backgroundColor: 'var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '24px' }}>🎬</div>
                        }
                        <div style={{ padding: '6px 8px' }}>
                          <div style={{ color: 'var(--text)', fontSize: '11px', fontWeight: '600', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{s.title}</div>
                          <div style={{ color: 'var(--text-dark)', fontSize: '11px' }}>{s.releaseYear}</div>
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
            <div style={{
              padding: '10px 14px', backgroundColor: 'var(--bg-hover)', border: '1px solid #2a2a2a',
              borderRadius: '16px 16px 16px 4px', color: 'var(--text-muted)', fontSize: '13px',
            }}>
              🤖 Sto pensando...
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div style={{
        padding: compact ? '10px 12px' : '14px 20px',
        borderTop: '1px solid #222', flexShrink: 0,
        display: 'flex', gap: '8px', alignItems: 'flex-end',
      }}>
        <textarea
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Scrivi un messaggio... (Invio per inviare)"
          rows={1}
          style={{
            flex: 1, padding: compact ? '10px 14px' : '12px 16px',
            backgroundColor: 'var(--bg-card)', border: '1px solid #333',
            borderRadius: '10px', color: 'var(--text)', fontSize: '13px',
            resize: 'none', lineHeight: 1.5, fontFamily: 'inherit',
          }}
        />
        <button
          onClick={() => handleSend()}
          disabled={loading || !input.trim()}
          aria-label="Invia messaggio"
          style={{
            padding: compact ? '10px 14px' : '12px 18px',
            backgroundColor: loading || !input.trim() ? 'var(--border-soft)' : 'var(--accent)',
            border: 'none', borderRadius: '10px', color: 'var(--text)',
            fontSize: '16px', cursor: loading || !input.trim() ? 'default' : 'pointer',
            flexShrink: 0,
          }}
        >➤</button>
      </div>
    </div>
  )
}

// Finestra floating — rimpiazza ChatAiFloatingButton in Navbar
export function ChatFloating() {
  const { token } = useAuthStore()
  const { isOpen, toggle, close, messages } = useChatStore()
  const navigate = useNavigate()

  // Non mostrare se non loggato
  if (!token) return null

  const unread = 0 // placeholder per future notifiche

  return (
    <>
      {/* Bottone di apertura */}
      <button
        onClick={toggle}
        aria-label={isOpen ? 'Chiudi chat AI' : 'Apri chat AI'}
        style={{
          position: 'fixed', bottom: '28px', right: '28px', zIndex: 600,
          width: '56px', height: '56px', borderRadius: '50%',
          backgroundColor: isOpen ? 'var(--border-soft)' : 'var(--accent)',
          border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: isOpen ? '20px' : '24px',
          boxShadow: isOpen ? '0 4px 16px rgba(0,0,0,0.4)' : '0 4px 20px rgba(229,9,20,0.4)',
          transition: 'all 0.2s',
        }}
        onMouseEnter={e => { e.currentTarget.style.transform = 'scale(1.1)' }}
        onMouseLeave={e => { e.currentTarget.style.transform = 'scale(1)' }}
      >
        {isOpen ? '✕' : '🤖'}
      </button>

      {/* Finestra chat */}
      {isOpen && (
        <div style={{
          position: 'fixed', bottom: '96px', right: '28px',
          width: '380px', height: '520px',
          backgroundColor: 'var(--bg-card)', border: '1px solid #2a2a2a',
          borderRadius: '16px', zIndex: 599,
          display: 'flex', flexDirection: 'column',
          boxShadow: '0 8px 40px rgba(0,0,0,0.6)',
          animation: 'chatSlideUp 0.2s ease',
          overflow: 'hidden',
        }}>
          <style>{`
            @keyframes chatSlideUp {
              from { opacity: 0; transform: translateY(16px) scale(0.97); }
              to   { opacity: 1; transform: translateY(0)   scale(1);    }
            }
          `}</style>

          {/* Header floating */}
          <div style={{
            padding: '14px 16px', borderBottom: '1px solid #222',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            flexShrink: 0, backgroundColor: 'var(--bg-card)',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '18px' }}>🤖</span>
              <div>
                <div style={{ color: 'var(--text)', fontWeight: '700', fontSize: '14px' }}>Assistente AI</div>
                <div style={{ color: 'var(--text-dark)', fontSize: '11px' }}>
                  {messages.length > 0 ? `${messages.length} messaggi in sessione` : 'Chiedi un consiglio'}
                </div>
              </div>
            </div>
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <button
                onClick={() => { close(); navigate('/chat') }}
                title="Apri a schermo intero"
                style={{ backgroundColor: 'transparent', border: '1px solid #333', borderRadius: '6px', color: 'var(--text-dark)', fontSize: '11px', padding: '4px 8px', cursor: 'pointer' }}
              >⤢ Espandi</button>
            </div>
          </div>

          {/* Chat content */}
          <div style={{ flex: 1, overflow: 'hidden' }}>
            <ChatCore compact />
          </div>
        </div>
      )}
    </>
  )
}

export default ChatCore
