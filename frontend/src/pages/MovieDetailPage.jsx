import { useEffect, useState } from 'react'
import { useParams, useSearchParams, useNavigate, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import StaticRating from '../components/StaticRating'
import api from '../services/api'
import useAuthStore from '../store/authStore'
import useToastStore from '../store/toastStore'

const STATUS_LABELS = {
  TO_WATCH: '📌 Da vedere',
  WATCHING: '▶️ In visione',
  WATCHED: '✅ Visto',
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

function ReportModal({ reviewId, onClose, onSuccess }) {
  const [category, setCategory] = useState('')
  const [reasonText, setReasonText] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async () => {
    if (!category) { setError('Seleziona una categoria'); return }
    if (category === 'OTHER' && !reasonText.trim()) { setError('Descrivi il motivo'); return }
    setLoading(true); setError('')
    try {
      await api.post('/reports', { reviewId, reasonCategory: category, reasonText: reasonText.trim() || null })
      onSuccess()
    } catch (err) {
      setError(err.response?.data?.error || 'Errore durante la segnalazione')
    } finally { setLoading(false) }
  }

  return (
    <>
      <div onClick={onClose} style={{ position: 'fixed', inset: 0, backgroundColor: 'var(--bg-modal)', zIndex: 500 }} />
      <div style={{ position: 'fixed', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', backgroundColor: 'var(--bg-card)', border: '1px solid #333', borderRadius: '16px', padding: '32px', width: '100%', maxWidth: '460px', zIndex: 600 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <h2 style={{ color: 'var(--text)', fontSize: '20px', fontWeight: '700' }}>🚩 Segnala recensione</h2>
          <button onClick={onClose} style={{ backgroundColor: 'transparent', border: 'none', color: 'var(--text-dark)', fontSize: '20px', cursor: 'pointer' }}>✕</button>
        </div>
        <p style={{ color: 'var(--text-muted)', fontSize: '14px', marginBottom: '20px' }}>
          Seleziona il motivo della segnalazione. La esamineremo e prenderemo i provvedimenti necessari.
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '20px' }}>
          {REPORT_CATEGORIES.map(c => (
            <button key={c.value} onClick={() => setCategory(c.value)} style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', padding: '12px 16px', borderRadius: '8px', textAlign: 'left', border: `1px solid ${category === c.value ? 'var(--accent)' : 'var(--border-soft)'}`, backgroundColor: category === c.value ? 'var(--accent-subtle)' : 'var(--bg-card)', cursor: 'pointer', width: '100%' }}>
              <div style={{ marginTop: '2px', width: '16px', height: '16px', borderRadius: '50%', border: `2px solid ${category === c.value ? 'var(--accent)' : '#444'}`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
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
            style={{ width: '100%', padding: '10px 14px', backgroundColor: 'var(--bg-hover)', border: '1px solid #333', borderRadius: '8px', color: 'var(--text)', fontSize: '14px', resize: 'none', boxSizing: 'border-box' }} />
          <div style={{ color: '#555', fontSize: '11px', textAlign: 'right', marginTop: '4px' }}>{reasonText.length}/500</div>
        </div>
        {error && <p style={{ color: '#ff6b6b', fontSize: '13px', marginBottom: '16px' }}>{error}</p>}
        <div style={{ display: 'flex', gap: '10px' }}>
          <button onClick={handleSubmit} disabled={loading} style={{ flex: 1, padding: '12px', backgroundColor: loading ? '#666' : 'var(--accent)', border: 'none', borderRadius: '8px', color: 'var(--text)', fontSize: '15px', fontWeight: '600', cursor: loading ? 'default' : 'pointer' }}>
            {loading ? 'Invio...' : 'Invia segnalazione'}
          </button>
          <button onClick={onClose} style={{ padding: '12px 20px', backgroundColor: 'transparent', border: '1px solid #333', borderRadius: '8px', color: 'var(--text-muted)', fontSize: '14px', cursor: 'pointer' }}>
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

  const isAdmin = user?.role === 'ADMIN'

  const [detail, setDetail] = useState(null)
  const [reviews, setReviews] = useState([])
  const [watchEntry, setWatchEntry] = useState(null)
  const [myReview, setMyReview] = useState(null)
  const [loading, setLoading] = useState(true)

  const [rating, setRating] = useState(0)
  const [text, setText] = useState('')
  const [reviewLoading, setReviewLoading] = useState(false)
  const [reviewError, setReviewError] = useState('')
  const [reviewSuccess, setReviewSuccess] = useState('')
  const [editMode, setEditMode] = useState(false)

  const [libraryLoading, setLibraryLoading] = useState(false)
  const [libraryError, setLibraryError] = useState('')

  const [reportingReviewId, setReportingReviewId] = useState(null)
  const [reportedIds, setReportedIds] = useState(new Set())

  useEffect(() => {
    Promise.all([
      api.get(`/tmdb/${mediaType}/${id}`),
      api.get(`/reviews/media/${mediaType}/${id}`),
    ]).then(([detailRes, reviewsRes]) => {
      setDetail(detailRes.data)
      const allReviews = reviewsRes.data.content || reviewsRes.data
      setReviews(allReviews)
      if (user) {
        const mine = allReviews.find(r => r.username === user.username)
        if (mine) { setMyReview(mine); setRating(mine.rating); setText(mine.text || '') }
      }
    }).catch(() => {}).finally(() => setLoading(false))

    if (token) {
      api.get('/library', { params: { size: 200 } }).then(r => {
        const entries = r.data.content || r.data
        const found = entries.find(e => String(e.tmdbId) === String(id) && (e.contentType || e.mediaType) === mediaType)
        if (found) setWatchEntry(found)
      }).catch(() => {})
    }
  }, [id, mediaType, token, user])

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
    setReviewLoading(true); setReviewError(''); setReviewSuccess('')
    try {
      if (myReview && editMode) {
        const res = await api.put(`/reviews/${myReview.id}`, { rating, text: text.trim() || null })
        setMyReview(res.data); setReviews(prev => prev.map(r => r.id === myReview.id ? res.data : r))
        setReviewSuccess('Recensione aggiornata!'); setEditMode(false)
      } else {
        const res = await api.post('/reviews', { tmdbId: Number(id), contentType: mediaType, rating, text: text.trim() || null })
        setMyReview(res.data); setReviews(prev => [res.data, ...prev])
        setReviewSuccess('Recensione pubblicata!')
        setWatchEntry(prev => prev ? { ...prev, status: 'WATCHED' } : null)
      }
    } catch (err) { setReviewError(err.response?.data?.error || 'Errore durante la pubblicazione') }
    finally { setReviewLoading(false) }
  }

  if (loading) return (<div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}><Navbar /><div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '100px' }}>Caricamento...</div></div>)
  if (!detail) return (<div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}><Navbar /><div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '100px' }}>Contenuto non trovato</div></div>)

  const otherReviews = reviews.filter(r => r.username !== user?.username)
  const tmdbRating = detail.tmdbRating ?? detail.votoTmdb
  const ciakLogRating = detail.ciakLogAverageRating ?? detail.votoCiakLog
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

      <div style={{ padding: '48px 64px', display: 'flex', gap: '48px', alignItems: 'flex-start', maxWidth: '1100px', margin: '0 auto' }}>
        <div style={{ flexShrink: 0 }}>
          {detail.posterPath
            ? <img src={`https://image.tmdb.org/t/p/w300${detail.posterPath}`} alt={detail.title} style={{ width: '200px', borderRadius: '12px', border: '1px solid #222' }} />
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

          {detail.genres?.length > 0 && (
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '20px' }}>
              {detail.genres.map(g => <span key={g} style={{ padding: '4px 12px', backgroundColor: 'var(--bg-hover)', border: '1px solid #333', borderRadius: '20px', color: 'var(--text-muted)', fontSize: '13px' }}>{g}</span>)}
            </div>
          )}

          <div style={{ display: 'flex', gap: '24px', marginBottom: '28px' }}>
            {tmdbRating && (
              <div>
                <div style={{ color: 'var(--text-dark)', fontSize: '12px', marginBottom: '2px' }}>TMDB</div>
                <div style={{ color: 'var(--gold)', fontWeight: '700', fontSize: '20px' }}>⭐ {Number(tmdbRating).toFixed(1)}</div>
              </div>
            )}
            {ciakLogRating && (
              <div>
                <div style={{ color: 'var(--text-dark)', fontSize: '12px', marginBottom: '2px' }}>CiakLog</div>
                <div style={{ color: 'var(--accent)', fontWeight: '700', fontSize: '20px' }}>🎬 {Number(ciakLogRating).toFixed(1)}</div>
              </div>
            )}
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
              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                {detail.cast.slice(0, 8).map((actor, i) => (
                  <span key={i} style={{ padding: '4px 12px', backgroundColor: 'var(--bg-hover)', border: '1px solid #333', borderRadius: '20px', color: 'var(--text-muted)', fontSize: '13px' }}>
                    {typeof actor === 'string' ? actor : actor.name}
                  </span>
                ))}
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
          <div style={{ backgroundColor: 'var(--bg-card)', border: '1px solid #222', borderRadius: '12px', padding: '28px', marginBottom: '40px' }}>

            {myReview && !editMode ? (
              /* Ho già recensito */
              <>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                  <h2 style={{ color: 'var(--text)', fontSize: '18px', fontWeight: '700' }}>✏️ La tua recensione</h2>
                  <button onClick={() => setEditMode(true)} style={{ padding: '6px 16px', backgroundColor: 'transparent', border: '1px solid #333', borderRadius: '6px', color: 'var(--text-muted)', fontSize: '13px', cursor: 'pointer' }}>Modifica</button>
                </div>
                <div style={{ marginBottom: '10px' }}><StaticRating rating={myReview.rating} /></div>
                {myReview.text && <p style={{ color: '#d1d5db', fontSize: '14px', lineHeight: 1.6 }}>{myReview.text}</p>}
                {reviewSuccess && <p style={{ color: '#4ade80', fontSize: '13px', marginTop: '10px' }}>{reviewSuccess}</p>}
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
                    <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '8px' }}>Commento (opzionale)</label>
                    <textarea value={text} onChange={e => setText(e.target.value)} placeholder="Cosa ne pensi?" rows={4}
                      style={{ width: '100%', padding: '12px', backgroundColor: 'var(--bg-hover)', border: '1px solid #333', borderRadius: '8px', color: 'var(--text)', fontSize: '14px', resize: 'vertical', boxSizing: 'border-box' }} />
                  </div>
                  {reviewError && <p style={{ color: '#ff6b6b', fontSize: '13px', marginBottom: '12px' }}>{reviewError}</p>}
                  {reviewSuccess && <p style={{ color: '#4ade80', fontSize: '13px', marginBottom: '12px' }}>{reviewSuccess}</p>}
                  <div style={{ display: 'flex', gap: '10px' }}>
                    <button type="submit" disabled={reviewLoading} style={{ padding: '12px 28px', backgroundColor: reviewLoading ? '#666' : 'var(--accent)', border: 'none', borderRadius: '8px', color: 'var(--text)', fontSize: '15px', fontWeight: '600', cursor: 'pointer' }}>
                      {reviewLoading ? 'Salvataggio...' : editMode ? 'Aggiorna' : 'Pubblica recensione'}
                    </button>
                    {editMode && (
                      <button type="button" onClick={() => { setEditMode(false); setRating(myReview.rating); setText(myReview.text || '') }}
                        style={{ padding: '12px 20px', backgroundColor: 'transparent', border: '1px solid #333', borderRadius: '8px', color: 'var(--text-muted)', fontSize: '14px', cursor: 'pointer' }}>
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
        <h2 style={{ color: 'var(--text)', fontSize: '20px', fontWeight: '700', marginBottom: '20px' }}>
          💬 Recensioni della community{' '}
          {otherReviews.length > 0 && <span style={{ color: 'var(--text-dark)', fontSize: '16px' }}>({otherReviews.length})</span>}
        </h2>

        {otherReviews.length === 0 ? (
          <p style={{ color: 'var(--text-dark)' }}>Ancora nessuna recensione dalla community. {!myReview && !isAdmin && 'Sii il primo!'}</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {otherReviews.map(r => (
              <div key={r.id} style={{ backgroundColor: 'var(--bg-card)', border: '1px solid #222', borderRadius: '10px', padding: '20px' }}>
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
                  </div>
                </div>
                {r.text && <p style={{ color: '#d1d5db', fontSize: '14px', lineHeight: 1.6 }}>{r.text}</p>}
                <p style={{ color: 'var(--text-dark)', fontSize: '12px', marginTop: '8px' }}>
                  {new Date(r.createdAt).toLocaleDateString('it-IT')}
                </p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default MovieDetailPage