import { useEffect, useState } from 'react'
import { useParams, useSearchParams, useNavigate, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import StaticRating from '../components/StaticRating'
import ConfirmModal from '../components/ConfirmModal'
import api from '../services/api'
import useAuthStore from '../store/authStore'
import useToastStore from '../store/toastStore'
import useChatStore from '../store/chatStore'

const STATUS_LABELS = {
  TO_WATCH: '📌 Da vedere',
  WATCHING: '▶️ In visione',
  WATCHED: '✅ Visto',
}

// Prima non esisteva nessuna interfaccia per leggere o
// scrivere risposte — il backend (ReviewComment) era pronto ma invisibile.
// Thread collassato di default sotto ogni recensione, caricato on-demand.
function ReplyThread({ reviewId, reviewText, reviewOwnerUsername, token, currentUsername, isAdmin, autoExpand, highlightCommentId }) {
  const toast = useToastStore()
  const [expanded, setExpanded] = useState(!!autoExpand)
  const [loaded, setLoaded] = useState(false)
  const [loading, setLoading] = useState(false)
  const [comments, setComments] = useState([])
  // Fix (Dettaglio Film/Serie — risposte troncate a 50): stesso bug delle
  // recensioni, versione risposte — caricate con size:50 fisso e nessun
  // "carica altre". Stesso pattern di fix: paginazione vera + bottone.
  const [commentsPage, setCommentsPage] = useState(0)
  const [commentsTotalPages, setCommentsTotalPages] = useState(1)
  const [commentsTotalElements, setCommentsTotalElements] = useState(0)
  const [loadingMoreComments, setLoadingMoreComments] = useState(false)
  const [showComposer, setShowComposer] = useState(false)
  const [text, setText] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [editText, setEditText] = useState('')
  const [reportingCommentId, setReportingCommentId] = useState(null)
  const [reportedCommentIds, setReportedCommentIds] = useState(new Set())
  const [hidingCommentId, setHidingCommentId] = useState(null)
  // Toggle reversibile, separato
  // dall'eliminazione — non tocca il punteggio, non genera nessuna segnalazione
  const [togglingHiddenId, setTogglingHiddenId] = useState(null)

  // Stesso pattern di "recensione
  // a botta calda" — scrivi con parole tue, l'AI la struttura meglio
  const [showReplyNotesHelper, setShowReplyNotesHelper] = useState(false)
  const [replyRawNotes, setReplyRawNotes] = useState('')
  const [structuringReply, setStructuringReply] = useState(false)
  const [replyStructureError, setReplyStructureError] = useState('')

  // Riusava /ai/structure-review (pensato per recensioni intere, 4-5
  // frasi) senza nessun contesto su cosa si stava rispondendo — l'AI scriveva
  // come fosse una recensione, non una risposta breve in un thread. Ora usa
  // l'endpoint dedicato /ai/structure-comment, con il testo della recensione
  // a cui si risponde come contesto, per un tono coerente e più breve
  const handleStructureReply = async () => {
    if (!replyRawNotes.trim()) return
    setStructuringReply(true); setReplyStructureError('')
    try {
      const res = await api.post('/ai/structure-comment', {
        rawNotes: replyRawNotes.trim(),
        replyingToText: reviewText || '',
      })
      setText(res.data.text || '')
      setShowReplyNotesHelper(false)
      setReplyRawNotes('')
    } catch (err) {
      setReplyStructureError(err.response?.data?.error || 'Assistente non disponibile, riprova')
    } finally {
      setStructuringReply(false)
    }
  }

  const loadComments = () => {
    setLoading(true)
    api.get(`/reviews/${reviewId}/comments`, { params: { page: 0, size: 20, sort: 'createdAt,asc' } })
      .then(r => {
        const list = r.data.content || r.data
        setComments(list)
        setCommentsPage(0)
        setCommentsTotalPages(r.data.page?.totalPages ?? r.data.totalPages ?? 1)
        setCommentsTotalElements(r.data.page?.totalElements ?? r.data.totalElements ?? list.length)
      })
      .catch(() => {})
      .finally(() => { setLoading(false); setLoaded(true) })
  }

  const loadMoreComments = async () => {
    if (loadingMoreComments || commentsPage + 1 >= commentsTotalPages) return
    setLoadingMoreComments(true)
    try {
      const nextPage = commentsPage + 1
      const res = await api.get(`/reviews/${reviewId}/comments`, { params: { page: nextPage, size: 20, sort: 'createdAt,asc' } })
      const more = res.data.content || res.data
      setComments(prev => [...prev, ...more])
      setCommentsPage(nextPage)
      setCommentsTotalPages(res.data.page?.totalPages ?? res.data.totalPages ?? nextPage + 1)
      setCommentsTotalElements(res.data.page?.totalElements ?? res.data.totalElements ?? commentsTotalElements)
    } catch {
      // silenzioso, il bottone resta cliccabile
    } finally {
      setLoadingMoreComments(false)
    }
  }

  const toggleExpanded = () => {
    setExpanded(v => !v)
    if (!loaded) loadComments()
  }

  // Se questo thread contiene la risposta
  // segnalata (arrivata da "Vedi nel contesto"), si apre e carica da sola
  useEffect(() => {
    if (autoExpand && !loaded) loadComments()
  }, [autoExpand])

  // Scroll automatico + evidenziazione della risposta segnalata, una volta caricata
  useEffect(() => {
    if (!highlightCommentId || comments.length === 0) return
    const el = document.getElementById(`comment-${highlightCommentId}`)
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, [comments, highlightCommentId])

  const handleAddComment = async () => {
    if (!text.trim()) return
    setSubmitting(true)
    try {
      const res = await api.post(`/reviews/${reviewId}/comments`, { text: text.trim() })
      setComments(prev => [...prev, res.data])
      // Fix (Dettaglio Film/Serie): stesso motivo del fix sul conteggio recensioni.
      setCommentsTotalElements(prev => prev + 1)
      setText('')
      setShowComposer(false)
    } catch (err) {
      toast.show(err.response?.data?.error || 'Errore durante l\'invio della risposta')
    } finally {
      setSubmitting(false)
    }
  }

  const handleEditComment = async (id) => {
    if (!editText.trim()) return
    try {
      const res = await api.put(`/comments/${id}`, { text: editText.trim() })
      setComments(prev => prev.map(c => c.id === id ? res.data : c))
      setEditingId(null)
    } catch (err) {
      toast.show(err.response?.data?.error || 'Errore durante la modifica')
    }
  }

  const handleDeleteComment = async (id) => {
    try {
      await api.delete(`/comments/${id}`)
      setComments(prev => prev.filter(c => c.id !== id))
      setCommentsTotalElements(prev => Math.max(0, prev - 1))
    } catch (err) {
      toast.show(err.response?.data?.error || 'Errore durante l\'eliminazione')
    }
  }

  const handleToggleCommentHidden = async (comment) => {
    setTogglingHiddenId(comment.id)
    try {
      const res = await api.patch(`/comments/${comment.id}/visibility`, null, { params: { hidden: !comment.hiddenByAuthor } })
      setComments(prev => prev.map(c => c.id === comment.id ? res.data : c))
    } catch (err) {
      toast.show(err.response?.data?.error || 'Errore durante l\'operazione')
    } finally {
      setTogglingHiddenId(null)
    }
  }

  // Il backend filtra già le risposte nascoste
  // dagli AUTORI DIVERSI dal viewer — l'unico caso in cui hiddenByAuthor=true
  // arriva qui è la propria risposta nascosta (solo tu la vedi). Il badge
  // "Risposte (N)" contava anche quella, dando l'impressione che nascondere
  // non avesse effetto: il conteggio ora riflette solo ciò che è pubblico.
  // Fix (Dettaglio Film/Serie — risposte troncate a 50): questo conteggio
  // usava comments.filter(...).length, cioè solo le risposte caricate finora
  // in pagina — con la paginazione vera (20 alla volta) avrebbe mostrato "20"
  // anche con 60 risposte totali. Ora usa commentsTotalElements (il totale
  // reale dal backend), con lo stesso aggiustamento di 1 già usato per le
  // recensioni per la propria risposta nascosta (unico caso in cui il totale
  // del backend include qualcosa che il conteggio pubblico non deve contare,
  // per un viewer non-Admin).
  const myHiddenComment = comments.find(c => c.authorUsername === currentUsername && c.hiddenByAuthor)
  const visibleCommentsCount = Math.max(0, commentsTotalElements - (myHiddenComment && !isAdmin ? 1 : 0))

  // La risposta di chi ha scritto la recensione
  // era in mezzo alle altre in ordine cronologico, poco visibile. La
  // portiamo sempre in cima (comments arriva già ordinato per data asc dal
  // backend, quindi il sort è stabile e non tocca l'ordine tra le altre)
  const sortedComments = reviewOwnerUsername
      ? [...comments].sort((a, b) => (b.authorUsername === reviewOwnerUsername) - (a.authorUsername === reviewOwnerUsername))
      : comments

  return (
    <div style={{ marginTop: '14px', paddingTop: '12px', borderTop: '1px solid var(--border-soft)' }}>
      <button onClick={toggleExpanded} style={{ background: 'none', border: 'none', color: 'var(--text-dark)', fontSize: '12px', cursor: 'pointer', padding: 0 }}>
        {expanded ? '▲ Nascondi risposte' : `💬 Risposte${loaded ? ` (${visibleCommentsCount})` : ''}`}
      </button>

      {expanded && (
        <div style={{ marginTop: '12px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {loading && <p style={{ color: 'var(--text-dark)', fontSize: '12px' }}>Caricamento...</p>}

          {!loading && sortedComments.map(c => (
            <div key={c.id} id={`comment-${c.id}`} style={{ backgroundColor: 'var(--bg-hover)', borderRadius: '8px', padding: '10px 14px', marginLeft: '16px', border: highlightCommentId === c.id ? '2px solid #3b82f6' : (c.hiddenBySuspension ? '2px solid #ef4444' : (c.status === 'HIDDEN' ? '2px solid #f59e0b' : '2px solid transparent')), boxShadow: highlightCommentId === c.id ? '0 0 0 4px rgba(59,130,246,0.15)' : 'none' }}>
              {/* Fix (Dettaglio — banner moderazione, stessa logica delle recensioni):
                  visibile qui solo se sei Admin, il backend la esclude per chiunque altro */}
              {c.status === 'HIDDEN' && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', backgroundColor: 'rgba(245,158,11,0.1)', border: '1px solid #f59e0b44', borderRadius: '5px', padding: '5px 10px', marginBottom: '8px', color: '#f59e0b', fontSize: '11px', fontWeight: '600' }}>
                    🔶 Nascosta per segnalazioni — in attesa di decisione
                  </div>
              )}
              {/* Fix (dashboard admin — banner distinto per sospensione): richiesto
                  esplicitamente un banner diverso da quello per segnalazioni, per
                  distinguere "nascosta perché l'autore è sospeso" da "nascosta in
                  attesa di decisione su una segnalazione" — colore/testo diversi */}
              {c.hiddenBySuspension && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', backgroundColor: 'rgba(239,68,68,0.1)', border: '1px solid #ef444444', borderRadius: '5px', padding: '5px 10px', marginBottom: '8px', color: '#ef4444', fontSize: '11px', fontWeight: '600' }}>
                    🔒 Nascosta — l'autore è sospeso
                  </div>
              )}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Link to={`/profile/${c.authorUsername}`}>
                    <span style={{ color: 'var(--text)', fontWeight: '600', fontSize: '12px' }}>👤 {c.authorUsername}</span>
                  </Link>
                  {c.authorUsername === reviewOwnerUsername && (
                      <span style={{ padding: '1px 6px', borderRadius: '8px', fontSize: '9px', fontWeight: '700', backgroundColor: 'var(--accent-subtle)', color: 'var(--accent)' }}>
                        AUTORE
                      </span>
                  )}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ color: 'var(--text-dark)', fontSize: '11px' }}>{new Date(c.createdAt).toLocaleDateString('it-IT')}</span>
                  {c.authorUsername === currentUsername ? (
                    <>
                      <button onClick={() => { setEditingId(c.id); setEditText(c.text) }} style={{ fontSize: '11px', color: 'var(--text-dark)', backgroundColor: 'transparent', border: 'none', cursor: 'pointer' }}>Modifica</button>
                      {/* Fix (auto-nascondimento autore): toggle reversibile, distinto dall'eliminazione */}
                      <button onClick={() => handleToggleCommentHidden(c)} disabled={togglingHiddenId === c.id} style={{ fontSize: '11px', color: 'var(--text-dark)', backgroundColor: 'transparent', border: 'none', cursor: 'pointer' }}>
                        {togglingHiddenId === c.id ? '...' : (c.hiddenByAuthor ? '👁️ Mostra' : '🙈 Nascondi')}
                      </button>
                      <button onClick={() => handleDeleteComment(c.id)} style={{ fontSize: '11px', color: '#ff6b6b', backgroundColor: 'transparent', border: 'none', cursor: 'pointer' }}>Elimina</button>
                    </>
                  ) : token && !isAdmin && (
                    reportedCommentIds.has(c.id) ? (
                      <span style={{ fontSize: '11px', color: 'var(--text-dark)' }}>Segnalata ✓</span>
                    ) : (
                      <button onClick={() => setReportingCommentId(c.id)} title="Segnala risposta" aria-label="Segnala risposta"
                        style={{ fontSize: '12px', color: 'var(--text-dark)', backgroundColor: 'transparent', border: 'none', cursor: 'pointer' }}>🚩</button>
                    )
                  )}
                  {isAdmin && (
                    <button onClick={() => setHidingCommentId(c.id)} title="Nascondi direttamente" aria-label="Nascondi direttamente"
                      style={{ fontSize: '12px', color: 'var(--text-dark)', backgroundColor: 'transparent', border: 'none', cursor: 'pointer' }}>🔨</button>
                  )}
                </div>
              </div>

              {editingId === c.id ? (
                <div>
                  <textarea value={editText} onChange={e => setEditText(e.target.value)} rows={2} maxLength={500}
                    style={{ width: '100%', padding: '8px 10px', backgroundColor: 'var(--bg-card)', border: '1px solid var(--border-soft)', borderRadius: '6px', color: 'var(--text)', fontSize: '13px', resize: 'none', boxSizing: 'border-box' }} />
                  <div style={{ display: 'flex', gap: '8px', marginTop: '6px' }}>
                    <button onClick={() => handleEditComment(c.id)} style={{ padding: '4px 12px', backgroundColor: 'var(--accent)', border: 'none', borderRadius: '5px', color: 'var(--text)', fontSize: '12px', cursor: 'pointer' }}>Salva</button>
                    <button onClick={() => setEditingId(null)} style={{ padding: '4px 12px', backgroundColor: 'transparent', border: '1px solid var(--border-soft)', borderRadius: '5px', color: 'var(--text-muted)', fontSize: '12px', cursor: 'pointer' }}>Annulla</button>
                  </div>
                </div>
              ) : (
                <>
                  {c.hiddenByAuthor && c.authorUsername === currentUsername && (
                    <p style={{ color: 'var(--text-dark)', fontSize: '11px', fontStyle: 'italic', marginBottom: '4px' }}>🙈 Nascosta — solo tu la vedi</p>
                  )}
                  {/* Fix (dashboard admin — commenti nascosti): l'Admin ora riceve anche
                      le risposte nascoste dagli altri autori (bypass lato query) — qui
                      lo segnaliamo chiaramente, per non farlo sembrare un contenuto normale */}
                  {c.hiddenByAuthor && c.authorUsername !== currentUsername && isAdmin && (
                    <p style={{ color: '#f59e0b', fontSize: '11px', fontStyle: 'italic', marginBottom: '4px' }}>🙈 Nascosta dall'autore — visibile solo a te come Admin</p>
                  )}
                  <p style={{ color: '#c8c8c8', fontSize: '13px', lineHeight: 1.5, margin: 0 }}>{c.text}</p>
                  <ReactionBar
                    endpoint={`/comments/${c.id}/reaction`}
                    token={token}
                    canReact={!!token && !isAdmin && c.authorUsername !== currentUsername}
                  />
                </>
              )}
            </div>
          ))}

          {!loading && comments.length === 0 && (
            <p style={{ color: 'var(--text-dark)', fontSize: '12px', marginLeft: '16px' }}>Nessuna risposta ancora.</p>
          )}

          {/* Fix (Dettaglio Film/Serie — risposte troncate a 50): bottone per
              caricare le pagine successive, stesso pattern delle recensioni. */}
          {!loading && commentsPage + 1 < commentsTotalPages && (
            <button onClick={loadMoreComments} disabled={loadingMoreComments} style={{
              marginLeft: '16px', alignSelf: 'flex-start', padding: '5px 12px',
              backgroundColor: 'transparent', border: '1px solid var(--border-soft)',
              borderRadius: '5px', color: 'var(--text-dark)', fontSize: '12px',
              cursor: loadingMoreComments ? 'default' : 'pointer', opacity: loadingMoreComments ? 0.6 : 1,
            }}>
              {loadingMoreComments ? 'Caricamento...' : 'Carica altre risposte'}
            </button>
          )}

          {token && !isAdmin && (
            showComposer ? (
              <div style={{ marginLeft: '16px' }}>
                <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '4px' }}>
                  {!showReplyNotesHelper && (
                    <button type="button" onClick={() => setShowReplyNotesHelper(true)}
                      style={{ background: 'none', border: 'none', color: 'var(--accent)', fontSize: '11px', fontWeight: '600', cursor: 'pointer', padding: 0 }}>
                      ✨ Aiutami a scriverla
                    </button>
                  )}
                </div>
                {showReplyNotesHelper && (
                  <div style={{ backgroundColor: 'var(--bg-hover)', border: '1px solid var(--accent)', borderRadius: '6px', padding: '8px', marginBottom: '8px' }}>
                    <textarea value={replyRawNotes} onChange={e => setReplyRawNotes(e.target.value)} placeholder="Butta giù qualche appunto, l'AI lo sistema..." rows={2} maxLength={500}
                      style={{ width: '100%', padding: '6px 8px', backgroundColor: 'var(--bg)', border: '1px solid var(--border-soft)', borderRadius: '5px', color: 'var(--text)', fontSize: '12px', resize: 'none', boxSizing: 'border-box', marginBottom: '6px' }} />
                    {replyStructureError && <p style={{ color: '#ff6b6b', fontSize: '11px', marginBottom: '6px' }}>{replyStructureError}</p>}
                    <div style={{ display: 'flex', gap: '6px' }}>
                      <button type="button" disabled={structuringReply || !replyRawNotes.trim()} onClick={handleStructureReply}
                        style={{ padding: '4px 10px', backgroundColor: structuringReply ? 'var(--border-soft)' : 'var(--accent)', border: 'none', borderRadius: '5px', color: 'var(--text)', fontSize: '11px', fontWeight: '600', cursor: structuringReply ? 'default' : 'pointer' }}>
                        {structuringReply ? 'Genero...' : 'Genera'}
                      </button>
                      <button type="button" onClick={() => { setShowReplyNotesHelper(false); setReplyRawNotes(''); setReplyStructureError('') }}
                        style={{ padding: '4px 10px', backgroundColor: 'transparent', border: '1px solid var(--border-soft)', borderRadius: '5px', color: 'var(--text-muted)', fontSize: '11px', cursor: 'pointer' }}>
                        Annulla
                      </button>
                    </div>
                  </div>
                )}
                <textarea value={text} onChange={e => setText(e.target.value)} placeholder="Scrivi una risposta..." rows={2} maxLength={500}
                  style={{ width: '100%', padding: '8px 10px', backgroundColor: 'var(--bg-hover)', border: '1px solid var(--border-soft)', borderRadius: '6px', color: 'var(--text)', fontSize: '13px', resize: 'none', boxSizing: 'border-box' }} />
                <div style={{ display: 'flex', gap: '8px', marginTop: '6px' }}>
                  <button onClick={handleAddComment} disabled={submitting || !text.trim()} style={{ padding: '5px 14px', backgroundColor: 'var(--accent)', border: 'none', borderRadius: '5px', color: 'var(--text)', fontSize: '12px', fontWeight: '600', cursor: 'pointer' }}>
                    {submitting ? 'Invio...' : 'Rispondi'}
                  </button>
                  <button onClick={() => { setShowComposer(false); setText(''); setShowReplyNotesHelper(false); setReplyRawNotes(''); setReplyStructureError('') }} style={{ padding: '5px 14px', backgroundColor: 'transparent', border: '1px solid var(--border-soft)', borderRadius: '5px', color: 'var(--text-muted)', fontSize: '12px', cursor: 'pointer' }}>Annulla</button>
                </div>
              </div>
            ) : (
              <button onClick={() => setShowComposer(true)} style={{ marginLeft: '16px', alignSelf: 'flex-start', padding: '5px 12px', backgroundColor: 'transparent', border: '1px solid var(--border-soft)', borderRadius: '5px', color: 'var(--text-dark)', fontSize: '12px', cursor: 'pointer' }}>
                💬 Rispondi
              </button>
            )
          )}
        </div>
      )}

      {reportingCommentId && (
        <ReportModal
          reviewCommentId={reportingCommentId}
          onClose={() => setReportingCommentId(null)}
          onSuccess={() => { setReportedCommentIds(prev => new Set([...prev, reportingCommentId])); setReportingCommentId(null) }}
        />
      )}

      {hidingCommentId && (
        <ReportModal
          reviewCommentId={hidingCommentId}
          adminMode
          onClose={() => setHidingCommentId(null)}
          onSuccess={() => {
            setComments(prev => prev.filter(c => c.id !== hidingCommentId))
            setHidingCommentId(null)
            toast.show('Risposta nascosta.', 'success')
          }}
        />
      )}
    </div>
  )
}

// Riepilogo + toggle, riusata sia per recensioni
// che per risposte. Nessuna moderazione qui — un'emoji non porta contenuto
// dannoso (deciso), quindi niente 🚩/🔨, solo il conteggio e il tap per reagire
const REACTION_EMOJI = { LIKE: '👍', LOVE: '❤️', LAUGH: '😂', WOW: '😮' }

function ReactionBar({ endpoint, token, canReact }) {
  const [summary, setSummary] = useState(null)

  useEffect(() => {
    api.get(endpoint).then(r => setSummary(r.data)).catch(() => {})
  }, [endpoint])

  const handleClick = async (type) => {
    if (!canReact) return
    try {
      if (summary?.myReaction === type) {
        const res = await api.delete(endpoint)
        setSummary(res.data)
      } else {
        const res = await api.put(endpoint, { type })
        setSummary(res.data)
      }
    } catch { /* silenzioso: una reazione fallita non merita un errore bloccante */ }
  }

  if (!summary) return null

  return (
    <div style={{ display: 'flex', gap: '6px', marginTop: '10px' }}>
      {Object.entries(REACTION_EMOJI).map(([type, emoji]) => {
        const count = summary.counts?.[type] || 0
        const mine = summary.myReaction === type
        return (
          <button
            key={type}
            onClick={() => handleClick(type)}
            disabled={!canReact}
            title={canReact ? undefined : (token ? 'Non puoi reagire ai tuoi contenuti' : 'Accedi per reagire')}
            style={{
              display: 'flex', alignItems: 'center', gap: '4px',
              padding: '3px 9px', borderRadius: '12px', fontSize: '12px',
              backgroundColor: mine ? 'rgba(229,9,20,0.15)' : 'transparent',
              border: mine ? '1px solid var(--accent)' : '1px solid var(--border-soft)',
              color: mine ? 'var(--accent)' : 'var(--text-dark)',
              cursor: canReact ? 'pointer' : 'default',
            }}
          >
            <span>{emoji}</span>
            {count > 0 && <span>{count}</span>}
          </button>
        )
      })}
    </div>
  )
}

function StarPicker({ value, onChange }) {
  const [hover, setHover] = useState(0)
  const active = hover > 0 ? hover : value
  return (
    <div style={{ display: 'flex', gap: '4px' }}>
      {[1, 2, 3, 4, 5].map(n => (
        <span key={n} onClick={() => onChange(n)}
          onMouseEnter={() => setHover(n)} onMouseLeave={() => setHover(0)}
          style={{ fontSize: '28px', cursor: 'pointer', filter: n <= active ? 'none' : 'grayscale(1)', opacity: n <= active ? 1 : 0.25, transition: 'opacity 0.1s, filter 0.1s' }}
        >🎬</span>
      ))}
    </div>
  )
}

const REPORT_CATEGORIES = [
  { value: 'SPAM', label: '🚫 Spam', desc: 'Contenuto ripetuto o fuori luogo' },
  { value: 'INAPPROPRIATE_CONTENT', label: '⚠️ Contenuto inappropriato', desc: 'Linguaggio offensivo o volgare' },
  { value: 'OFF_TOPIC', label: '💬 Fuori tema', desc: 'Non riguarda il film o la serie' },
  { value: 'OTHER', label: '📝 Altro', desc: 'Specifica il motivo nel campo testo' },
]

// Esteso per segnalare/nascondere
// sia recensioni che risposte, e per la modalità admin ("Nascondi direttamente")
function ReportModal({ reviewId, reviewCommentId, adminMode, onClose, onSuccess }) {
  const [category, setCategory] = useState('')
  const [reasonText, setReasonText] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async () => {
    if (!category) { setError('Seleziona una categoria'); return }
    if (category === 'OTHER' && !reasonText.trim()) { setError('Descrivi il motivo'); return }
    setLoading(true); setError('')
    try {
      const endpoint = adminMode ? '/reports/admin-hide' : '/reports'
      await api.post(endpoint, { reviewId, reviewCommentId, reasonCategory: category, reasonText: reasonText.trim() || null })
      onSuccess()
    } catch (err) {
      setError(err.response?.data?.error || 'Errore durante l\'operazione')
    } finally { setLoading(false) }
  }

  const targetLabel = reviewCommentId ? 'risposta' : 'recensione'

  return (
    <>
      <div onClick={onClose} style={{ position: 'fixed', inset: 0, backgroundColor: 'var(--bg-modal)', zIndex: 500 }} />
      <div style={{ position: 'fixed', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', backgroundColor: 'var(--bg-card)', border: '1px solid var(--border-soft)', borderRadius: '16px', padding: '32px', width: '100%', maxWidth: '460px', zIndex: 600 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <h2 style={{ color: 'var(--text)', fontSize: '20px', fontWeight: '700' }}>
            {adminMode ? `🔨 Nascondi ${targetLabel}` : `🚩 Segnala ${targetLabel}`}
          </h2>
          <button onClick={onClose} style={{ backgroundColor: 'transparent', border: 'none', color: 'var(--text-dark)', fontSize: '20px', cursor: 'pointer' }}>✕</button>
        </div>
        <p style={{ color: 'var(--text-muted)', fontSize: '14px', marginBottom: '20px' }}>
          {adminMode
            ? `Il contenuto verrà rimosso immediatamente e l'autore riceverà una violazione, come per una segnalazione approvata. Il motivo è obbligatorio.`
            : 'Seleziona il motivo della segnalazione. La esamineremo e prenderemo i provvedimenti necessari.'}
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '20px' }}>
          {REPORT_CATEGORIES.map(c => (
            <button key={c.value} onClick={() => setCategory(c.value)} style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', padding: '12px 16px', borderRadius: '8px', textAlign: 'left', border: `1px solid ${category === c.value ? 'var(--accent)' : 'var(--border-soft)'}`, backgroundColor: category === c.value ? 'var(--accent-subtle)' : 'var(--bg-card)', cursor: 'pointer', width: '100%' }}>
              <div style={{ marginTop: '2px', width: '16px', height: '16px', borderRadius: '50%', border: `2px solid ${category === c.value ? 'var(--accent)' : 'var(--border-soft)'}`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                {category === c.value && <div style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: 'var(--accent)' }} />}
              </div>
              <div>
                <div style={{ color: 'var(--text)', fontWeight: '600', fontSize: '14px', marginBottom: '2px' }}>{c.label}</div>
                <div style={{ color: 'var(--text-dark)', fontSize: '12px' }}>{c.desc}</div>
              </div>
            </button>
          ))}
        </div>
        <div style={{ marginBottom: '20px' }}>
          <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>
            {category === 'OTHER' ? 'Descrivi il motivo (obbligatorio)' : 'Dettagli aggiuntivi (opzionale)'}
          </label>
          <textarea value={reasonText} onChange={e => setReasonText(e.target.value)} placeholder="Spiega il problema..." rows={3} maxLength={500}
            style={{ width: '100%', padding: '10px 14px', backgroundColor: 'var(--bg-hover)', border: '1px solid var(--border-soft)', borderRadius: '8px', color: 'var(--text)', fontSize: '14px', resize: 'none', boxSizing: 'border-box' }} />
          <div style={{ color: 'var(--text-dark)', fontSize: '11px', textAlign: 'right', marginTop: '4px' }}>{reasonText.length}/500</div>
        </div>
        {error && <p style={{ color: '#ff6b6b', fontSize: '13px', marginBottom: '16px' }}>{error}</p>}
        <div style={{ display: 'flex', gap: '10px' }}>
          <button onClick={handleSubmit} disabled={loading} style={{ flex: 1, padding: '12px', backgroundColor: loading ? 'var(--border-soft)' : 'var(--accent)', border: 'none', borderRadius: '8px', color: 'var(--text)', fontSize: '15px', fontWeight: '600', cursor: loading ? 'default' : 'pointer' }}>
            {loading ? 'Invio...' : (adminMode ? 'Nascondi contenuto' : 'Invia segnalazione')}
          </button>
          <button onClick={onClose} style={{ padding: '12px 20px', backgroundColor: 'transparent', border: '1px solid var(--border-soft)', borderRadius: '8px', color: 'var(--text-muted)', fontSize: '14px', cursor: 'pointer' }}>
            Annulla
          </button>
        </div>
      </div>
    </>
  )
}

function MovieDetailPage() {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const mediaType = searchParams.get('type') || 'MOVIE'
  const navigate = useNavigate()
  const { token, user } = useAuthStore()
  const toast = useToastStore()

  const isAdmin = user?.role === 'ADMIN'
  const closeChatWidget = useChatStore(s => s.close)

  // "Chiedi su questo film" — stateless, ogni domanda
  // è indipendente, nessuna cronologia salvata
  const [showMovieQA, setShowMovieQA] = useState(false)
  const [movieQuestion, setMovieQuestion] = useState('')
  const [movieAnswer, setMovieAnswer] = useState(null) // { answer, containsSpoiler }
  const [spoilerRevealed, setSpoilerRevealed] = useState(false)
  const [askingMovie, setAskingMovie] = useState(false)
  const [movieQAError, setMovieQAError] = useState('')

  const openMovieQA = () => {
    // Minimizza (non chiude/resetta) la chat generale se era aperta —
    // le due non devono mai stare aperte sovrapposte sullo schermo insieme
    closeChatWidget()
    setShowMovieQA(true)
  }

  const handleAskMovie = async () => {
    if (!movieQuestion.trim()) return
    setAskingMovie(true); setMovieQAError(''); setMovieAnswer(null); setSpoilerRevealed(false)
    try {
      const res = await api.post('/ai/movie-question', {
        tmdbId: Number(id),
        contentType: mediaType,
        question: movieQuestion.trim(),
      })
      setMovieAnswer(res.data)
    } catch (err) {
      setMovieQAError(err.response?.data?.error || 'Assistente non disponibile, riprova')
    } finally {
      setAskingMovie(false)
    }
  }

  // SOLO su richiesta esplicita
  const [aiOpinion, setAiOpinion] = useState('')
  const [aiOpinionError, setAiOpinionError] = useState('')
  const [askingOpinion, setAskingOpinion] = useState(false)

  const handleAskAiOpinion = async () => {
    setAskingOpinion(true); setAiOpinionError(''); setAiOpinion('')
    try {
      const res = await api.post(`/ai/reviews/${myReview.id}/opinion`)
      setAiOpinion(res.data.opinion || '')
    } catch (err) {
      setAiOpinionError(err.response?.data?.error || 'Assistente non disponibile, riprova')
    } finally {
      setAskingOpinion(false)
    }
  }

  // Target da evidenziare quando si arriva
  // qui dal link "Vedi nel contesto" della dashboard, e marcatore per
  // mostrare il bottone di ritorno (che usa la history, non un link fisso,
  // così il "torna alla dashboard" riporta l'admin esattamente dov'era)
  const highlightReviewId = searchParams.get('highlightReview')
  const highlightCommentId = searchParams.get('highlightComment')
  const parentReviewForHighlight = searchParams.get('parentReview')
  const cameFromAdmin = searchParams.get('adminRef') === '1'

  const [detail, setDetail] = useState(null)
  const [reviews, setReviews] = useState([])
  // Fix (Dettaglio Film/Serie — recensioni troncate a 20): prima si
  // caricavano tutte le recensioni in un colpo solo senza page/size, quindi
  // il backend applicava il default di Spring (20) e oltre sparivano senza
  // nessun "carica altre". Ora paginazione vera, 10 alla volta come in
  // Profilo, con bottone per caricarne altre.
  const [reviewsPage, setReviewsPage] = useState(0)
  const [reviewsTotalPages, setReviewsTotalPages] = useState(1)
  // Il conteggio in testata ("Recensioni della community (N)") deve riflettere
  // il totale reale, non solo quante ne sono state caricate finora in pagina —
  // vedi uso più sotto, vicino a "visibleReviewsCount".
  const [reviewsTotalElements, setReviewsTotalElements] = useState(0)
  const [loadingMoreReviews, setLoadingMoreReviews] = useState(false)
  const [watchEntry, setWatchEntry] = useState(null)
  const [myReview, setMyReview] = useState(null)
  const [loading, setLoading] = useState(true)

  const [rating, setRating] = useState(0)
  const [text, setText] = useState('')
  const [reviewLoading, setReviewLoading] = useState(false)
  const [reviewError, setReviewError] = useState('')
  const [reviewSuccess, setReviewSuccess] = useState('')
  const [editMode, setEditMode] = useState(false)

  // "recensione a botta calda" — appunti sparsi -> AI li struttura
  const [showNotesHelper, setShowNotesHelper] = useState(false)
  const [rawNotes, setRawNotes] = useState('')
  const [structuring, setStructuring] = useState(false)
  const [structureError, setStructureError] = useState('')

  const [libraryLoading, setLibraryLoading] = useState(false)
  const [libraryError, setLibraryError] = useState('')

  const [reportingReviewId, setReportingReviewId] = useState(null)
  // "Nascondi direttamente" — stato separato per il modal in modalità admin
  const [hidingReviewId, setHidingReviewId] = useState(null)
  const [reportedIds, setReportedIds] = useState(new Set())

  useEffect(() => {
    // Questa pagina resta montata
    // quando si passa da un film all'altro tramite i link "consigliami qualcosa
    // di simile" (stesso componente, cambia solo :id nell'URL) — senza reset,
    // watchEntry/myReview/rating restavano quelli del film di partenza finché
    // non si ricaricava manualmente la pagina, mostrando uno stato "vecchio"
    // (es. "Salvato come: Visto" di un film che non era mai stato aggiunto).
    setDetail(null)
    setWatchEntry(null)
    setMyReview(null)
    setRating(0)
    setText('')
    setEditMode(false)
    setLoading(true)
    setReviewsPage(0)
    setReviewsTotalPages(1)

    Promise.all([
      api.get(`/tmdb/${mediaType}/${id}`),
      api.get(`/reviews/media/${mediaType}/${id}`, { params: { page: 0, size: 10, sort: 'createdAt,desc' } }),
      // Fix (Dettaglio Film/Serie): "myReview" non si cerca più dentro le
      // recensioni caricate in pagina (potrebbe non esserci, essendo ora
      // paginate) — endpoint dedicato, indipendente dalla paginazione.
      token ? api.get(`/reviews/media/${mediaType}/${id}/mine`).catch(() => ({ data: null })) : Promise.resolve({ data: null }),
    ]).then(([detailRes, reviewsRes, mineRes]) => {
      setDetail(detailRes.data)
      const allReviews = reviewsRes.data.content || reviewsRes.data
      setReviews(allReviews)
      setReviewsTotalPages(reviewsRes.data.page?.totalPages ?? reviewsRes.data.totalPages ?? 1)
      setReviewsTotalElements(reviewsRes.data.page?.totalElements ?? reviewsRes.data.totalElements ?? allReviews.length)
      const mine = mineRes.data
      if (mine) { setMyReview(mine); setRating(mine.rating); setText(mine.text || '') }
    }).catch(() => {}).finally(() => setLoading(false))

    if (token) {
      api.get('/library', { params: { size: 200 } }).then(r => {
        const entries = r.data.content || r.data
        const found = entries.find(e => String(e.tmdbId) === String(id) && (e.contentType || e.mediaType) === mediaType)
        if (found) setWatchEntry(found)
      }).catch(() => {})
    }
  }, [id, mediaType, token, user])

  // Scroll automatico + evidenziazione della
  // recensione segnalata, quando si arriva qui da "Vedi nel contesto"
  useEffect(() => {
    if (!highlightReviewId || reviews.length === 0) return
    const el = document.getElementById(`review-${highlightReviewId}`)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }, [reviews, highlightReviewId])

  const handleAddToLibrary = async (status) => {
    if (!token) { navigate('/login', { state: { from: `/movie/${id}?type=${mediaType}` } }); return }
    // Se clicco lo status già attivo → rimuovo dalla libreria
    if (watchEntry?.status === status) {
      setLibraryLoading(true); setLibraryError('')
      try {
        await api.delete(`/library/${watchEntry.id}`)
        setWatchEntry(null)
      } catch (err) { setLibraryError(err.response?.data?.error || 'Errore durante la rimozione') }
      finally { setLibraryLoading(false) }
      return
    }
    setLibraryLoading(true); setLibraryError('')
    try {
      if (watchEntry) {
        const res = await api.put(`/library/${watchEntry.id}`, null, { params: { status } })
        setWatchEntry(res.data)
      } else {
        const res = await api.post('/library', {
          tmdbId: Number(id), contentType: mediaType, title: detail.title,
          posterPath: detail.posterPath, releaseYear: detail.releaseYear,
          genres: (detail.genres || []).join(','), status,
        })
        setWatchEntry(res.data)
      }
    } catch (err) { setLibraryError(err.response?.data?.error || 'Errore durante il salvataggio') }
    finally { setLibraryLoading(false) }
  }

  const handleSubmitReview = async (e) => {
    e.preventDefault()
    if (!token) { navigate('/login', { state: { from: `/movie/${id}?type=${mediaType}` } }); return }
    if (!rating) { setReviewError('Seleziona un voto'); return }
    if (!text.trim()) { setReviewError('Il commento è obbligatorio'); return }
    setReviewLoading(true); setReviewError(''); setReviewSuccess('')
    try {
      if (myReview && editMode) {
        const res = await api.put(`/reviews/${myReview.id}`, { rating, text: text.trim() || null })
        setMyReview(res.data); setReviews(prev => prev.map(r => r.id === myReview.id ? res.data : r))
        setReviewSuccess('Recensione aggiornata!'); setEditMode(false)
      } else {
        const res = await api.post('/reviews', { tmdbId: Number(id), contentType: mediaType, rating, text: text.trim() || null })
        setMyReview(res.data); setReviews(prev => [res.data, ...prev])
        // Fix (Dettaglio Film/Serie): il conteggio in testata ora si basa sul
        // totale dal backend (reviewsTotalElements), non più sulla sola
        // lista caricata — va incrementato a mano qui, altrimenti resterebbe
        // indietro di 1 finché non si ricarica la pagina.
        setReviewsTotalElements(prev => prev + 1)
        setReviewSuccess('Recensione pubblicata!')
        setWatchEntry(prev => prev ? { ...prev, status: 'WATCHED' } : null)
      }
    } catch (err) { setReviewError(err.response?.data?.error || 'Errore durante la pubblicazione') }
    finally { setReviewLoading(false) }
  }

  // "recensione a botta calda" — non salva nulla,
  // pre-compila solo il campo testo, l'utente rivede/modifica prima di pubblicare
  const handleStructureNotes = async () => {
    if (!rawNotes.trim()) return
    setStructuring(true); setStructureError('')
    try {
      const res = await api.post('/ai/structure-review', {
        rawNotes: rawNotes.trim(),
        movieTitle: detail?.title || '',
      })
      setText(res.data.text || '')
      setShowNotesHelper(false)
      setRawNotes('')
    } catch (err) {
      setStructureError(err.response?.data?.error || 'Assistente non disponibile, riprova')
    } finally {
      setStructuring(false)
    }
  }

  // L'endpoint DELETE /api/reviews/{id} esisteva già ma nessun bottone
  // lo richiamava nel frontend
  const [deletingReview, setDeletingReview] = useState(false)
  const [confirmDeleteReview, setConfirmDeleteReview] = useState(false)
  const handleDeleteReview = async () => {
    if (!myReview) return
    setConfirmDeleteReview(false)
    setDeletingReview(true)
    try {
      await api.delete(`/reviews/${myReview.id}`)
      setReviews(prev => prev.filter(r => r.id !== myReview.id))
      // Fix (Dettaglio Film/Serie): stesso motivo dell'incremento in
      // handleSubmitReview, ma al contrario.
      setReviewsTotalElements(prev => Math.max(0, prev - 1))
      setMyReview(null)
      setRating(0); setText(''); setEditMode(false)
      setReviewSuccess('Recensione eliminata.')
    } catch (err) {
      setReviewError(err.response?.data?.error || 'Errore durante l\'eliminazione')
    } finally {
      setDeletingReview(false)
    }
  }

  // Toggle reversibile, separato
  // dall'eliminazione — non tocca il punteggio, non genera nessuna segnalazione
  const [togglingHidden, setTogglingHidden] = useState(false)
  const handleToggleHidden = async () => {
    if (!myReview) return
    setTogglingHidden(true)
    try {
      const res = await api.patch(`/reviews/${myReview.id}/visibility`, null, { params: { hidden: !myReview.hiddenByAuthor } })
      setMyReview(res.data)
      setReviews(prev => prev.map(r => r.id === myReview.id ? res.data : r))
    } catch (err) {
      toast.show(err.response?.data?.error || 'Errore durante l\'operazione')
    } finally {
      setTogglingHidden(false)
    }
  }

  // Fix (Dettaglio Film/Serie — recensioni troncate a 20): carica la
  // pagina successiva e la accoda a quelle già visibili, stesso pattern di
  // "Carica altre recensioni" già usato in Profilo.
  const loadMoreReviews = async () => {
    if (loadingMoreReviews || reviewsPage + 1 >= reviewsTotalPages) return
    setLoadingMoreReviews(true)
    try {
      const nextPage = reviewsPage + 1
      const res = await api.get(`/reviews/media/${mediaType}/${id}`, { params: { page: nextPage, size: 10, sort: 'createdAt,desc' } })
      const more = res.data.content || res.data
      setReviews(prev => [...prev, ...more])
      setReviewsPage(nextPage)
      setReviewsTotalPages(res.data.page?.totalPages ?? res.data.totalPages ?? nextPage + 1)
      setReviewsTotalElements(res.data.page?.totalElements ?? res.data.totalElements ?? reviewsTotalElements)
    } catch {
      // silenzioso: il bottone resta cliccabile, l'utente può riprovare
    } finally {
      setLoadingMoreReviews(false)
    }
  }

  if (loading) return (<div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}><Navbar /><div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '100px' }}>Caricamento...</div></div>)
  if (!detail) return (<div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}><Navbar /><div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '100px' }}>Contenuto non trovato</div></div>)

  const otherReviews = reviews.filter(r => r.username !== user?.username)
  const tmdbRating = detail.tmdbRating ?? detail.votoTmdb
  const ciakLogRating = detail.ciakLogAverageRating ?? detail.votoCiakLog

  // CiakLog è su scala 1-5, TMDB su
  // scala 0-10 — normalizzo CiakLog x2 prima di fare la media, altrimenti il
  // combinato sarebbe falsato (una media diretta tra 1-5 e 0-10 non ha senso)
  const normalizedCiak = ciakLogRating != null ? Number(ciakLogRating) * 2 : null
  const normalizedTmdb = tmdbRating != null ? Number(tmdbRating) : null
  const combinedRating = normalizedTmdb != null && normalizedCiak != null
      ? (normalizedTmdb + normalizedCiak) / 2
      : (normalizedTmdb ?? normalizedCiak)
  const ciakLogVotes = detail.ciakLogVoteCount ?? detail.numeroVotiCiakLog
  const isMovie = (detail.contentType ?? mediaType) === 'MOVIE'
  const canReview = token && !isAdmin && watchEntry?.status === 'WATCHED'

  return (
    <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}>
      <Navbar />

      {reportingReviewId && (
        <ReportModal
          reviewId={reportingReviewId}
          onClose={() => setReportingReviewId(null)}
          onSuccess={() => { setReportedIds(prev => new Set([...prev, reportingReviewId])); setReportingReviewId(null) }}
        />
      )}

      {hidingReviewId && (
        <ReportModal
          reviewId={hidingReviewId}
          adminMode
          onClose={() => setHidingReviewId(null)}
          onSuccess={() => {
            setReviews(prev => prev.filter(r => r.id !== hidingReviewId))
            setHidingReviewId(null)
            toast.show('Recensione nascosta.', 'success')
          }}
        />
      )}

      {confirmDeleteReview && (
        <ConfirmModal
          message="Eliminare la tua recensione? L'azione non è reversibile."
          confirmLabel="Elimina"
          onConfirm={handleDeleteReview}
          onCancel={() => setConfirmDeleteReview(false)}
        />
      )}

      {/* Fix (AI più centrale): modal "Chiedi su questo film" — separato dal
          widget flottante della chat generale, stateless (nessuna cronologia) */}
      {showMovieQA && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 200 }}
          onClick={() => setShowMovieQA(false)}>
          <div style={{ backgroundColor: 'var(--bg-card)', border: '1px solid var(--border-soft)', borderRadius: '12px', padding: '24px', width: '440px', maxWidth: '90vw', maxHeight: '80vh', overflowY: 'auto' }}
            onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
              <h3 style={{ color: 'var(--text)', fontSize: '16px', fontWeight: '700', margin: 0 }}>💬 Chiedi su "{detail.title}"</h3>
              <button onClick={() => setShowMovieQA(false)} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', fontSize: '20px', cursor: 'pointer', lineHeight: 1 }}>✕</button>
            </div>
            <p style={{ color: 'var(--text-dark)', fontSize: '12px', marginBottom: '16px' }}>
              Domande solo su questo titolo — trama, cast, temi. Ogni domanda è indipendente, non c'è memoria della conversazione.
            </p>

            <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
              <input
                value={movieQuestion}
                onChange={e => setMovieQuestion(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && !askingMovie && handleAskMovie()}
                placeholder="es. chi è il regista? com'è il ritmo?"
                maxLength={300}
                style={{ flex: 1, padding: '10px 12px', backgroundColor: 'var(--bg-hover)', border: '1px solid var(--border-soft)', borderRadius: '8px', color: 'var(--text)', fontSize: '13px' }}
              />
              <button onClick={handleAskMovie} disabled={askingMovie || !movieQuestion.trim()} style={{
                padding: '10px 18px', backgroundColor: askingMovie ? 'var(--border-soft)' : 'var(--accent)', border: 'none',
                borderRadius: '8px', color: 'var(--text)', fontSize: '13px', fontWeight: '600',
                cursor: askingMovie ? 'default' : 'pointer', whiteSpace: 'nowrap',
              }}>
                {askingMovie ? '...' : 'Chiedi'}
              </button>
            </div>

            {movieQAError && <p style={{ color: '#ff6b6b', fontSize: '13px' }}>{movieQAError}</p>}

            {movieAnswer && (
              <div style={{ backgroundColor: 'var(--bg-hover)', borderRadius: '8px', padding: '14px' }}>
                {movieAnswer.containsSpoiler && !spoilerRevealed ? (
                  <button onClick={() => setSpoilerRevealed(true)} style={{
                    width: '100%', padding: '10px', backgroundColor: 'rgba(239,68,68,0.12)', border: '1px solid #ef4444',
                    borderRadius: '6px', color: '#ef4444', fontSize: '13px', fontWeight: '600', cursor: 'pointer',
                  }}>
                    ⚠️ Attenzione, spoiler — mostra comunque la risposta
                  </button>
                ) : (
                  <p style={{ color: 'var(--text)', fontSize: '14px', lineHeight: 1.5, margin: 0 }}>{movieAnswer.answer}</p>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Fix (dashboard admin, Step 6): usa la history (back), non un link fisso
          a /admin — così tab/filtro/gruppo aperto/scroll della dashboard restano
          esattamente come li aveva lasciati l'admin, non ripartono da capo */}
      {cameFromAdmin && (
        <div style={{ maxWidth: '1100px', margin: '16px auto 0', padding: '0 64px' }}>
          <button
            onClick={() => navigate(-1)}
            style={{ padding: '8px 18px', backgroundColor: 'transparent', border: '1px solid var(--border-soft)', borderRadius: '8px', color: 'var(--text-muted)', fontSize: '13px', cursor: 'pointer' }}
          >
            ← Torna alla dashboard admin
          </button>
        </div>
      )}

      <div style={{ padding: '48px 64px', display: 'flex', gap: '48px', alignItems: 'flex-start', maxWidth: '1100px', margin: '0 auto' }}>
        <div style={{ flexShrink: 0 }}>
          {detail.posterPath
            ? <img src={`https://image.tmdb.org/t/p/w300${detail.posterPath}`} alt={detail.title} style={{ width: '200px', borderRadius: '12px', border: '1px solid var(--border)' }} />
            : <div style={{ width: '200px', height: '300px', backgroundColor: 'var(--border)', borderRadius: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '48px' }}>🎬</div>
          }
        </div>

        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', padding: '3px 10px', borderRadius: '4px', backgroundColor: 'var(--border)', color: 'var(--text-muted)' }}>
              {isMovie ? '🎬 Film' : '📺 Serie TV'}
            </span>
            <span style={{ color: 'var(--text-dark)', fontSize: '14px' }}>{detail.releaseYear}</span>
          </div>

          <h1 style={{ fontSize: '36px', fontWeight: '800', color: 'var(--text)', marginBottom: '16px', lineHeight: 1.2 }}>{detail.title}</h1>

          {detail.overview && <p style={{ color: 'var(--text-muted)', fontSize: '14px', lineHeight: 1.6, marginBottom: '20px' }}>{detail.overview}</p>}

          {!isAdmin && (
            <button onClick={openMovieQA} style={{
              display: 'inline-flex', alignItems: 'center', gap: '6px', marginBottom: '20px',
              padding: '8px 16px', backgroundColor: 'var(--bg-hover)', border: '1px solid var(--accent)',
              borderRadius: '20px', color: 'var(--accent)', fontSize: '13px', fontWeight: '600', cursor: 'pointer',
            }}>
              {/* Fix (🟡 "Chiedi su questo film" fisso anche per le serie TV):
                  usa mediaType, già disponibile nel componente (da ?type= in
                  URL), invece del testo fisso "film". */}
              💬 Chiedi su {mediaType === 'TV' ? 'questa serie' : 'questo film'}
            </button>
          )}

          {detail.genres?.length > 0 && (
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '20px' }}>
              {detail.genres.map(g => <span key={g} style={{ padding: '4px 12px', backgroundColor: 'var(--bg-hover)', border: '1px solid var(--border-soft)', borderRadius: '20px', color: 'var(--text-muted)', fontSize: '13px' }}>{g}</span>)}
            </div>
          )}

          <div style={{ marginBottom: '28px' }}>
            {/* Fix (Dettaglio — voto combinato, deciso): media grande in evidenza,
                con le due fonti separate mostrate più piccole sotto */}
            {combinedRating != null && (
              <div style={{ marginBottom: '10px' }}>
                <div style={{ color: 'var(--gold)', fontWeight: '800', fontSize: '38px', lineHeight: 1 }}>
                  ⭐ {combinedRating.toFixed(1)}<span style={{ fontSize: '18px', color: 'var(--text-dark)', fontWeight: '600' }}>/10</span>
                </div>
                <div style={{ color: 'var(--text-dark)', fontSize: '12px', marginTop: '2px' }}>Voto complessivo</div>
              </div>
            )}

            {/* Fix: prima la riga spariva del tutto quando mancava il voto — un "–"
                esplicito comunica meglio "non ancora votato" di una sezione che
                sparisce silenziosamente (o, peggio, di uno "0" che sembra un voto reale) */}
            <div style={{ display: 'flex', gap: '24px' }}>
              <div>
                <div style={{ color: 'var(--text-dark)', fontSize: '11px', marginBottom: '2px' }}>TMDB</div>
                <div style={{ color: tmdbRating ? 'var(--text-muted)' : 'var(--text-dark)', fontWeight: '600', fontSize: '14px' }}>
                  {tmdbRating ? `⭐ ${Number(tmdbRating).toFixed(1)}` : '–'}
                </div>
              </div>
              <div>
                <div style={{ color: 'var(--text-dark)', fontSize: '11px', marginBottom: '2px' }}>CiakLog</div>
                <div style={{ color: ciakLogRating ? 'var(--text-muted)' : 'var(--text-dark)', fontWeight: '600', fontSize: '14px' }}>
                  {ciakLogRating ? `🎬 ${Number(ciakLogRating).toFixed(1)}` : '–'}
                </div>
              </div>
            </div>

            {/* Fix (🔴 attribuzione TMDB — versione completa): il testo qui da
                solo non basta ai loro requisiti (serve anche il logo, in una
                sezione "Crediti" dedicata) — link alla sezione vera, CreditsPage.jsx. */}
            <Link
              to="/credits"
              style={{ display: 'block', color: 'var(--text-dark)', fontSize: '11px', marginTop: '10px', textDecoration: 'none' }}
              onMouseEnter={e => e.currentTarget.style.color = 'var(--text-muted)'}
              onMouseLeave={e => e.currentTarget.style.color = 'var(--text-dark)'}
            >
              Dati forniti da TMDB — Crediti e attribuzioni →
            </Link>
          </div>

          {/* Bottoni libreria — nascosti per ADMIN */}
          {!isAdmin && (
            <>
              <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginBottom: '8px' }}>
                {['TO_WATCH', 'WATCHING', 'WATCHED'].map(s => (
                  <button key={s} onClick={() => handleAddToLibrary(s)} disabled={libraryLoading}
                    title={watchEntry?.status === s ? 'Clicca di nuovo per rimuovere dalla libreria' : ''}
                    style={{
                      padding: '10px 18px', borderRadius: '8px', fontSize: '13px', fontWeight: '600',
                      border: `1px solid ${watchEntry?.status === s ? 'var(--accent)' : 'var(--border-soft)'}`,
                      backgroundColor: watchEntry?.status === s ? 'var(--accent)' : 'var(--bg-hover)',
                      color: 'var(--text)', cursor: 'pointer',
                      opacity: libraryLoading ? 0.6 : 1,
                  }}>{watchEntry?.status === s ? `${STATUS_LABELS[s]} ✕` : STATUS_LABELS[s]}</button>
                ))}
              </div>
              {libraryError && <p style={{ color: '#ff6b6b', fontSize: '13px', marginTop: '6px' }}>{libraryError}</p>}
              {watchEntry && <p style={{ color: 'var(--text-muted)', fontSize: '13px', marginTop: '6px' }}>Salvato come: <strong style={{ color: 'var(--text)' }}>{STATUS_LABELS[watchEntry.status]}</strong></p>}
            </>
          )}

          {detail.cast?.length > 0 && (
            <div style={{ marginTop: '28px' }}>
              <h3 style={{ color: 'var(--text-muted)', fontSize: '13px', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '12px' }}>Cast</h3>
              {/* Fix: il backend fornisce già photoPath (da TMDB profile_path) ma
                  veniva ignorato — il cast appariva solo come pillole di testo,
                  mai con le foto. Ripristinate, con fallback per gli attori senza foto */}
              <div style={{ display: 'flex', gap: '14px', flexWrap: 'wrap' }}>
                {detail.cast.slice(0, 8).map((actor, i) => {
                  const name = typeof actor === 'string' ? actor : actor.name
                  const photoPath = typeof actor === 'string' ? null : actor.photoPath
                  return (
                    <div key={i} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', width: '72px', textAlign: 'center' }}>
                      {photoPath ? (
                        <img
                          src={`https://image.tmdb.org/t/p/w185${photoPath}`}
                          alt={name}
                          style={{ width: '56px', height: '56px', borderRadius: '50%', objectFit: 'cover', border: '1px solid var(--border-soft)', marginBottom: '6px' }}
                        />
                      ) : (
                        <div style={{ width: '56px', height: '56px', borderRadius: '50%', backgroundColor: 'var(--bg-hover)', border: '1px solid var(--border-soft)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '20px', marginBottom: '6px' }}>
                          🎭
                        </div>
                      )}
                      <span style={{ color: 'var(--text-muted)', fontSize: '11px', lineHeight: 1.3 }}>{name}</span>
                    </div>
                  )
                })}
              </div>
            </div>
          )}

          {/* Pulsante AI simili */}
          {token && !isAdmin && (
            <div style={{ marginTop: '28px' }}>
              <Link to={`/chat?prompt=${encodeURIComponent(`Consigliami qualcosa di simile a "${detail.title}"`)}`}>
                <button style={{
                  padding: '10px 20px', backgroundColor: 'transparent',
                  border: '1px solid #e50914', borderRadius: '8px',
                  color: 'var(--accent)', fontSize: '14px', fontWeight: '600', cursor: 'pointer',
                  display: 'flex', alignItems: 'center', gap: '8px',
                }}>
                  🤖 Consigliami qualcosa di simile
                </button>
              </Link>
            </div>
          )}
        </div>
      </div>

      <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '0 64px 64px' }}>

        {/* Sezione recensione — logica completa */}
        {token && !isAdmin && (
          <div id={myReview ? `review-${myReview.id}` : undefined} style={{ backgroundColor: 'var(--bg-card)', border: myReview && highlightReviewId === myReview.id ? '2px solid #3b82f6' : '1px solid var(--border)', borderRadius: '12px', padding: '28px', marginBottom: '40px', boxShadow: myReview && highlightReviewId === myReview.id ? '0 0 0 4px rgba(59,130,246,0.15)' : 'none' }}>

            {myReview && !editMode ? (
              /* Ho già recensito */
              <>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                  <h2 style={{ color: 'var(--text)', fontSize: '18px', fontWeight: '700' }}>✏️ La tua recensione</h2>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button onClick={() => setEditMode(true)} style={{ padding: '6px 16px', backgroundColor: 'transparent', border: '1px solid var(--border-soft)', borderRadius: '6px', color: 'var(--text-muted)', fontSize: '13px', cursor: 'pointer' }}>Modifica</button>
                    {/* Fix (auto-nascondimento autore): toggle reversibile, distinto
                        dall'eliminazione — nasconde/rimostra senza penalità */}
                    <button onClick={handleToggleHidden} disabled={togglingHidden} style={{ padding: '6px 16px', backgroundColor: 'transparent', border: '1px solid var(--border-soft)', borderRadius: '6px', color: 'var(--text-muted)', fontSize: '13px', cursor: 'pointer' }}>
                      {togglingHidden ? '...' : (myReview.hiddenByAuthor ? '👁️ Mostra di nuovo' : '🙈 Nascondi')}
                    </button>
                    <button onClick={() => setConfirmDeleteReview(true)} disabled={deletingReview} style={{ padding: '6px 16px', backgroundColor: 'transparent', border: '1px solid #4a2222', borderRadius: '6px', color: '#ff6b6b', fontSize: '13px', cursor: 'pointer' }}>
                      {deletingReview ? 'Eliminazione...' : 'Elimina'}
                    </button>
                  </div>
                </div>
                {myReview.hiddenByAuthor && (
                  <p style={{ color: 'var(--text-dark)', fontSize: '12px', marginBottom: '12px', fontStyle: 'italic' }}>
                    🙈 Nascosta — solo tu la vedi. Gli altri utenti non la vedono più sotto questo titolo.
                  </p>
                )}
                <div style={{ marginBottom: '10px' }}><StaticRating rating={myReview.rating} /></div>
                {myReview.text && <p style={{ color: 'var(--text-muted)', fontSize: '14px', lineHeight: 1.6 }}>{myReview.text}</p>}
                {reviewSuccess && <p style={{ color: '#4ade80', fontSize: '13px', marginTop: '10px' }}>{reviewSuccess}</p>}

                {/* Fix (AI che replica a una recensione negativa): SOLO su richiesta
                    esplicita, mai automatica — solo per voti bassi */}
                {myReview.rating <= 2 && (
                  <div style={{ marginTop: '14px' }}>
                    {!aiOpinion && (
                      <button onClick={handleAskAiOpinion} disabled={askingOpinion} style={{
                        padding: '6px 14px', backgroundColor: 'transparent', border: '1px solid var(--accent)',
                        borderRadius: '20px', color: 'var(--accent)', fontSize: '12px', fontWeight: '600',
                        cursor: askingOpinion ? 'default' : 'pointer',
                      }}>
                        {askingOpinion ? '...' : '🤖 Chiedi il parere dell\'AI'}
                      </button>
                    )}
                    {aiOpinionError && <p style={{ color: '#ff6b6b', fontSize: '12px', marginTop: '8px' }}>{aiOpinionError}</p>}
                    {aiOpinion && (
                      <div style={{ backgroundColor: 'var(--bg-hover)', border: '1px solid var(--accent)', borderRadius: '8px', padding: '12px', marginTop: '8px' }}>
                        <p style={{ color: 'var(--text-dark)', fontSize: '11px', fontWeight: '600', marginBottom: '4px' }}>🤖 L'AI dice:</p>
                        <p style={{ color: 'var(--text)', fontSize: '13px', lineHeight: 1.5, margin: 0 }}>{aiOpinion}</p>
                      </div>
                    )}
                  </div>
                )}

                <ReactionBar endpoint={`/reviews/${myReview.id}/reaction`} token={token} canReact={false} />
                <ReplyThread
                  reviewId={myReview.id}
                  reviewText={myReview.text}
                  reviewOwnerUsername={myReview.username}
                  token={token}
                  currentUsername={user?.username}
                  isAdmin={isAdmin}
                  autoExpand={parentReviewForHighlight === myReview.id}
                  highlightCommentId={highlightCommentId}
                />
              </>

            ) : canReview ? (
              /* Posso recensire — sono WATCHED */
              <>
                <h2 style={{ color: 'var(--text)', fontSize: '18px', fontWeight: '700', marginBottom: '20px' }}>
                  {editMode ? '✏️ Modifica la tua recensione' : '✏️ Scrivi una recensione'}
                </h2>
                <form onSubmit={handleSubmitReview}>
                  <div style={{ marginBottom: '16px' }}>
                    <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '8px' }}>Voto (obbligatorio)</label>
                    <StarPicker value={rating} onChange={setRating} />
                  </div>
                  <div style={{ marginBottom: '20px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px' }}>Commento (obbligatorio)</label>
                      {!showNotesHelper && (
                        <button type="button" onClick={() => setShowNotesHelper(true)}
                          style={{ background: 'none', border: 'none', color: 'var(--accent)', fontSize: '12px', fontWeight: '600', cursor: 'pointer', padding: 0 }}>
                          ✨ Aiutami a scriverla
                        </button>
                      )}
                    </div>
                    {showNotesHelper && (
                      <div style={{ backgroundColor: 'var(--bg-hover)', border: '1px solid var(--accent)', borderRadius: '8px', padding: '12px', marginBottom: '10px' }}>
                        <p style={{ color: 'var(--text-dark)', fontSize: '12px', marginBottom: '8px' }}>
                          Butta giù qualche appunto sparso — l'AI lo trasforma in una recensione, mantenendo il tuo tono e le tue opinioni.
                        </p>
                        <textarea value={rawNotes} onChange={e => setRawNotes(e.target.value)} placeholder="es. ritmo lento primi 20 min, finale wow, colonna sonora top..." rows={3} maxLength={1000}
                          style={{ width: '100%', padding: '10px', backgroundColor: 'var(--bg)', border: '1px solid var(--border-soft)', borderRadius: '6px', color: 'var(--text)', fontSize: '13px', resize: 'vertical', boxSizing: 'border-box', marginBottom: '8px' }} />
                        {structureError && <p style={{ color: '#ff6b6b', fontSize: '12px', marginBottom: '8px' }}>{structureError}</p>}
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button type="button" disabled={structuring || !rawNotes.trim()} onClick={handleStructureNotes}
                            style={{ padding: '8px 16px', backgroundColor: structuring ? 'var(--border-soft)' : 'var(--accent)', border: 'none', borderRadius: '6px', color: 'var(--text)', fontSize: '13px', fontWeight: '600', cursor: structuring ? 'default' : 'pointer' }}>
                            {structuring ? 'Genero...' : '✨ Genera recensione'}
                          </button>
                          <button type="button" onClick={() => { setShowNotesHelper(false); setRawNotes(''); setStructureError('') }}
                            style={{ padding: '8px 16px', backgroundColor: 'transparent', border: '1px solid var(--border-soft)', borderRadius: '6px', color: 'var(--text-muted)', fontSize: '13px', cursor: 'pointer' }}>
                            Annulla
                          </button>
                        </div>
                      </div>
                    )}
                    <textarea value={text} onChange={e => setText(e.target.value)} placeholder="Cosa ne pensi?" rows={4}
                      style={{ width: '100%', padding: '12px', backgroundColor: 'var(--bg-hover)', border: '1px solid var(--border-soft)', borderRadius: '8px', color: 'var(--text)', fontSize: '14px', resize: 'vertical', boxSizing: 'border-box' }} />
                  </div>
                  {reviewError && <p style={{ color: '#ff6b6b', fontSize: '13px', marginBottom: '12px' }}>{reviewError}</p>}
                  {reviewSuccess && <p style={{ color: '#4ade80', fontSize: '13px', marginBottom: '12px' }}>{reviewSuccess}</p>}
                  <div style={{ display: 'flex', gap: '10px' }}>
                    <button type="submit" disabled={reviewLoading} style={{ padding: '12px 28px', backgroundColor: reviewLoading ? 'var(--border-soft)' : 'var(--accent)', border: 'none', borderRadius: '8px', color: 'var(--text)', fontSize: '15px', fontWeight: '600', cursor: 'pointer' }}>
                      {reviewLoading ? 'Salvataggio...' : editMode ? 'Aggiorna' : 'Pubblica recensione'}
                    </button>
                    {editMode && (
                      <button type="button" onClick={() => { setEditMode(false); setRating(myReview.rating); setText(myReview.text || '') }}
                        style={{ padding: '12px 20px', backgroundColor: 'transparent', border: '1px solid var(--border-soft)', borderRadius: '8px', color: 'var(--text-muted)', fontSize: '14px', cursor: 'pointer' }}>
                        Annulla
                      </button>
                    )}
                  </div>
                </form>
              </>

            ) : (
              /* Non ancora WATCHED — messaggio informativo */
              <div style={{ textAlign: 'center', padding: '12px 0' }}>
                <div style={{ fontSize: '32px', marginBottom: '10px' }}>🎬</div>
                <p style={{ color: 'var(--text-muted)', fontSize: '14px', marginBottom: '12px' }}>
                  Puoi lasciare una recensione solo dopo aver contrassegnato questo contenuto come <strong style={{ color: 'var(--text)' }}>Visto</strong>.
                </p>
                {!watchEntry && (
                  <p style={{ color: 'var(--text-dark)', fontSize: '13px' }}>Aggiungilo alla libreria e impostalo come "✅ Visto".</p>
                )}
                {watchEntry && watchEntry.status !== 'WATCHED' && (
                  <button onClick={() => handleAddToLibrary('WATCHED')} style={{ padding: '10px 24px', backgroundColor: 'var(--accent)', border: 'none', borderRadius: '8px', color: 'var(--text)', fontSize: '14px', fontWeight: '600', cursor: 'pointer' }}>
                    Segna come Visto e recensisci
                  </button>
                )}
              </div>
            )}
          </div>
        )}

        {/* Recensioni community */}
        {/* Fix: il conteggio qui escludeva sempre la propria recensione (mostrando
            "1" invece di "2" con 2 recensioni di cui 1 tua), ma la media voti altrove
            nella pagina la include correttamente — disallineamento tra conteggio e
            media. Ora il conteggio è sul totale reale, coerente con la media; la
            lista sotto resta senza la tua per evitare il duplicato visivo (è già
            mostrata nel blocco "La tua recensione" sopra).
            Fix (Dettaglio — stesso bug delle risposte): reviews include la TUA
            recensione anche se l'hai nascosta (per poterla ripristinare), ma il
            conteggio pubblico non deve contarla — altrimenti nasconderla non
            sembra avere alcun effetto sul numero mostrato. */}
        {/* Fix (Dettaglio Film/Serie — recensioni troncate a 20): questo
            conteggio usava reviews.filter(...).length, cioè solo le recensioni
            caricate finora in pagina — con la paginazione vera (10 alla volta)
            avrebbe mostrato "10" anche con 50 recensioni totali, finché non le
            carichi tutte cliccando "Carica altre". Ora usa reviewsTotalElements
            (il totale reale dal backend), sottraendo al massimo 1 per la
            propria recensione nascosta (l'unico caso — per un viewer normale
            non-Admin — in cui il totale del backend include una recensione che
            il conteggio pubblico non deve contare, come già gestito nel fix
            precedente). Per un Admin il totale può includere anche recensioni
            nascoste di ALTRI autori (bypass admin) — approssimazione accettata,
            servirebbe un conteggio dedicato lato backend per essere esatto anche lì. */}
        {(() => {
          const ownHiddenAdjustment = (myReview?.hiddenByAuthor && !isAdmin) ? 1 : 0
          const visibleReviewsCount = Math.max(0, reviewsTotalElements - ownHiddenAdjustment)
          return (
            <h2 style={{ color: 'var(--text)', fontSize: '20px', fontWeight: '700', marginBottom: '20px' }}>
              💬 Recensioni della community{' '}
              {visibleReviewsCount > 0 && <span style={{ color: 'var(--text-dark)', fontSize: '16px' }}>({visibleReviewsCount})</span>}
            </h2>
          )
        })()}

        {reviews.length === 0 ? (
          <p style={{ color: 'var(--text-dark)' }}>Ancora nessuna recensione. {!isAdmin && 'Sii il primo!'}</p>
        ) : otherReviews.length === 0 ? (
          <p style={{ color: 'var(--text-dark)' }}>Per ora c'è solo la tua recensione — nessun altro ha ancora scritto la sua.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {otherReviews.map(r => (
              <div key={r.id} id={`review-${r.id}`} style={{ backgroundColor: 'var(--bg-card)', border: highlightReviewId === r.id ? '2px solid #3b82f6' : (r.hiddenBySuspension ? '1px solid #ef4444' : (r.status === 'HIDDEN' ? '1px solid #f59e0b' : '1px solid var(--border)')), borderRadius: '10px', padding: '20px', boxShadow: highlightReviewId === r.id ? '0 0 0 4px rgba(59,130,246,0.15)' : 'none' }}>
                {/* Fix (Dettaglio — banner moderazione): questa recensione arriva qui
                    solo se sei Admin (il backend la esclude per chiunque altro) — un
                    bordo ambra e un banner esplicito evitano che sembri una recensione
                    normale, distinguendola chiaramente da quelle pubbliche */}
                {r.status === 'HIDDEN' && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', backgroundColor: 'rgba(245,158,11,0.1)', border: '1px solid #f59e0b44', borderRadius: '6px', padding: '8px 12px', marginBottom: '12px', color: '#f59e0b', fontSize: '12px', fontWeight: '600' }}>
                      🔶 Nascosta per segnalazioni — in attesa di decisione. Visibile solo a te come Admin.
                    </div>
                )}
                {/* Fix (dashboard admin — banner distinto per sospensione): stesso
                    principio dei commenti, colore/testo diverso da quello per
                    segnalazioni così l'admin riconosce subito il motivo */}
                {r.hiddenBySuspension && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', backgroundColor: 'rgba(239,68,68,0.1)', border: '1px solid #ef444444', borderRadius: '6px', padding: '8px 12px', marginBottom: '12px', color: '#ef4444', fontSize: '12px', fontWeight: '600' }}>
                      🔒 Nascosta — l'autore è sospeso. Visibile solo a te come Admin.
                    </div>
                )}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                  <Link to={`/profile/${r.username}`}>
                    <span style={{ color: 'var(--text)', fontWeight: '600' }}>👤 {r.username}</span>
                  </Link>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <StaticRating rating={r.rating} />
                    {token && !isAdmin && (
                      reportedIds.has(r.id) ? (
                        <span style={{ fontSize: '11px', color: 'var(--text-dark)' }}>Segnalata ✓</span>
                      ) : (
                        <button onClick={() => setReportingReviewId(r.id)}
                          style={{ fontSize: '13px', color: 'var(--text-dark)', backgroundColor: 'transparent', border: 'none', cursor: 'pointer', padding: '4px 8px', borderRadius: '4px' }}
                          onMouseEnter={e => e.currentTarget.style.color = 'var(--accent)'}
                          onMouseLeave={e => e.currentTarget.style.color = 'var(--text-dark)'}
                          title="Segnala recensione"
                          aria-label="Segnala recensione"
                        >🚩</button>
                      )
                    )}
                    {isAdmin && (
                      <button onClick={() => setHidingReviewId(r.id)}
                        style={{ fontSize: '13px', color: 'var(--text-dark)', backgroundColor: 'transparent', border: 'none', cursor: 'pointer', padding: '4px 8px', borderRadius: '4px' }}
                        onMouseEnter={e => e.currentTarget.style.color = 'var(--accent)'}
                        onMouseLeave={e => e.currentTarget.style.color = 'var(--text-dark)'}
                        title="Nascondi direttamente"
                        aria-label="Nascondi direttamente"
                      >🔨</button>
                    )}
                  </div>
                </div>
                {r.text && <p style={{ color: 'var(--text-muted)', fontSize: '14px', lineHeight: 1.6 }}>{r.text}</p>}
                <p style={{ color: 'var(--text-dark)', fontSize: '12px', marginTop: '8px' }}>
                  {new Date(r.createdAt).toLocaleDateString('it-IT')}
                </p>
                <ReactionBar endpoint={`/reviews/${r.id}/reaction`} token={token} canReact={!!token && !isAdmin} />
                <ReplyThread
                  reviewId={r.id}
                  reviewText={r.text}
                  reviewOwnerUsername={r.username}
                  token={token}
                  currentUsername={user?.username}
                  isAdmin={isAdmin}
                  autoExpand={parentReviewForHighlight === r.id}
                  highlightCommentId={highlightCommentId}
                />
              </div>
            ))}
          </div>
        )}

        {/* Fix (Dettaglio Film/Serie — recensioni troncate a 20): bottone per
            caricare le pagine successive, stesso pattern già usato in Profilo. */}
        {reviewsPage + 1 < reviewsTotalPages && (
          <div style={{ textAlign: 'center', marginTop: '20px' }}>
            <button onClick={loadMoreReviews} disabled={loadingMoreReviews} style={{
              padding: '10px 24px', borderRadius: '8px', border: '1px solid var(--border-soft)',
              backgroundColor: 'transparent', color: 'var(--text)', fontSize: '14px',
              cursor: loadingMoreReviews ? 'default' : 'pointer', opacity: loadingMoreReviews ? 0.6 : 1,
            }}>
              {loadingMoreReviews ? 'Caricamento...' : 'Carica altre recensioni'}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

export default MovieDetailPage