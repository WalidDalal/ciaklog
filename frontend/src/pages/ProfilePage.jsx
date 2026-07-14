import { useEffect, useState, useRef } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../services/api'
import useAuthStore from '../store/authStore'
import useToastStore from '../store/toastStore'

const EMOJI_RATING = ['', '😑', '😐', '🙂', '😊', '🤩']

function ProfilePage() {
  const { username } = useParams()
  const navigate = useNavigate()
  const { user: me, updateToken } = useAuthStore()
  const toast = useToastStore()
  const isOwn = me?.username === username

  const [profile, setProfile] = useState(null)
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [reviewsPage, setReviewsPage] = useState(0)
  const [hasMoreReviews, setHasMoreReviews] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)

  // Editing state
  const [editing, setEditing] = useState(false)
  const [editUsername, setEditUsername] = useState('')
  const [editBio, setEditBio] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    setReviews([])
    setReviewsPage(0)
    Promise.all([
      api.get(`/users/${username}`),
      api.get(`/reviews/user/${username}`, { params: { page: 0, size: 10 } }),
    ])
        .then(([profileRes, reviewsRes]) => {
          setProfile(profileRes.data)
          setEditUsername(profileRes.data.username || username)
          setEditBio(profileRes.data.bio || '')
          const data = reviewsRes.data
          setReviews(data.content || data || [])
          // Fix: stesso problema di AdminPage — con PageSerializationMode.VIA_DTO
          // i metadati di paginazione sono annidati sotto `.page.`, non in cima.
          // Prima "data.page" leggeva l'oggetto metadata invece del numero pagina
          // corrente (0, appena richiesta), e "data.totalPages" era sempre undefined
          setHasMoreReviews(data.page?.totalPages ? 0 < data.page.totalPages - 1 : false)
        })
        .catch(() => {})
        .finally(() => setLoading(false))
  }, [username])

  const loadMoreReviews = async () => {
    setLoadingMore(true)
    const nextPage = reviewsPage + 1
    try {
      const res = await api.get(`/reviews/user/${username}`, { params: { page: nextPage, size: 10 } })
      const data = res.data
      setReviews(prev => [...prev, ...(data.content || data || [])])
      setReviewsPage(nextPage)
      setHasMoreReviews(data.page?.totalPages ? nextPage < data.page.totalPages - 1 : false)
    } catch {} finally { setLoadingMore(false) }
  }

  const handleSaveProfile = async () => {
    setSaving(true)
    try {
      const res = await api.put('/users/me', {
        username: editUsername !== username ? editUsername : undefined,
        bio: editBio,
      })
      if (res.data?.token) updateToken(res.data.token)
      setProfile(p => ({ ...p, bio: editBio, username: editUsername }))
      setEditing(false)
      toast.show('Profilo aggiornato!', 'success')
      // Se l'username è cambiato naviga al nuovo URL
      if (editUsername !== username) navigate(`/profile/${editUsername}`, { replace: true })
    } catch (err) {
      toast.show(err.response?.data?.error || 'Errore durante il salvataggio')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return (
      <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}>
        <Navbar />
        <div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '100px' }}>Caricamento...</div>
      </div>
  )

  if (!profile) return (
      <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}>
        <Navbar />
        <div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '100px' }}>Utente non trovato</div>
      </div>
  )

  const watching = profile.watching
      ? (Array.isArray(profile.watching) ? profile.watching : [profile.watching])
      : profile.watchingTitle
          ? [{ title: profile.watchingTitle, season: profile.watchingSeason }]
          : []

  const INPUT_STYLE = {
    width: '100%', padding: '8px 12px', boxSizing: 'border-box',
    backgroundColor: 'var(--bg-input)', border: '1px solid var(--border-soft)',
    borderRadius: '8px', color: 'var(--text)', fontSize: '14px', fontFamily: 'inherit',
  }

  return (
      <div style={{ backgroundColor: 'var(--bg)', height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <Navbar />

        {/* Fix: la parte superiore (avatar, bio, stats) resta fissa; solo la lista
            sotto (in visione + recensioni) scrolla */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', maxWidth: '900px', width: '100%', margin: '0 auto', padding: '0 24px', boxSizing: 'border-box', overflow: 'hidden' }}>

          {/* ── Header profilo (fisso) ── */}
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '28px', paddingTop: '48px', paddingBottom: '32px', flexShrink: 0 }}>
            {/* Avatar */}
            <div style={{
              width: '88px', height: '88px', borderRadius: '50%', flexShrink: 0,
              backgroundColor: 'var(--accent)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '36px', fontWeight: '800', color: 'white',
            }}>
              {(editUsername || username)?.[0]?.toUpperCase()}
            </div>

            <div style={{ flex: 1 }}>
              {editing ? (
                  /* ── Form modifica inline ── */
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', maxWidth: '400px' }}>
                    <div>
                      <label style={{ color: 'var(--text-muted)', fontSize: '12px', display: 'block', marginBottom: '4px' }}>Username</label>
                      <input
                          value={editUsername}
                          onChange={e => setEditUsername(e.target.value)}
                          style={INPUT_STYLE}
                          maxLength={30}
                      />
                    </div>
                    <div>
                      <label style={{ color: 'var(--text-muted)', fontSize: '12px', display: 'block', marginBottom: '4px' }}>Bio</label>
                      <textarea
                          value={editBio}
                          onChange={e => setEditBio(e.target.value)}
                          rows={3}
                          maxLength={200}
                          placeholder="Scrivi qualcosa su di te..."
                          style={{ ...INPUT_STYLE, resize: 'vertical' }}
                      />
                      <div style={{ color: 'var(--text-dark)', fontSize: '11px', textAlign: 'right', marginTop: '2px' }}>
                        {editBio.length}/200
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button
                          onClick={handleSaveProfile}
                          disabled={saving}
                          style={{
                            padding: '8px 20px', backgroundColor: 'var(--accent)', border: 'none',
                            borderRadius: '8px', color: 'white', fontSize: '14px', fontWeight: '600',
                            cursor: saving ? 'default' : 'pointer', opacity: saving ? 0.7 : 1,
                          }}
                      >
                        {saving ? 'Salvataggio...' : 'Salva'}
                      </button>
                      <button
                          onClick={() => { setEditing(false); setEditUsername(profile.username || username); setEditBio(profile.bio || '') }}
                          style={{
                            padding: '8px 16px', backgroundColor: 'transparent',
                            border: '1px solid var(--border)', borderRadius: '8px',
                            color: 'var(--text-muted)', fontSize: '14px', cursor: 'pointer',
                          }}
                      >
                        Annulla
                      </button>
                    </div>
                  </div>
              ) : (
                  /* ── Visualizzazione normale ── */
                  <>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '6px', flexWrap: 'wrap' }}>
                      <h1 style={{ fontSize: '28px', fontWeight: '800', color: 'var(--text)' }}>
                        {profile.username || username}
                      </h1>
                      {/* Fix (styling): badge generi spostati accanto al nome invece che
                          sotto, nella riga stats — più rapido da vedere a colpo d'occhio */}
                      {profile.topGenres?.length > 0 && (
                          <div style={{ display: 'flex', gap: '6px', alignItems: 'center', flexWrap: 'wrap' }}>
                            {profile.topGenres.slice(0, 3).map(g => (
                                <span key={g} style={{
                                  padding: '3px 10px', backgroundColor: 'var(--bg-hover)',
                                  border: '1px solid var(--border)', borderRadius: '20px',
                                  color: 'var(--text-muted)', fontSize: '12px',
                                }}>{g}</span>
                            ))}
                          </div>
                      )}
                      {isOwn && (
                          <button
                              onClick={() => setEditing(true)}
                              title="Modifica profilo"
                              style={{
                                backgroundColor: 'transparent', border: '1px solid var(--border)',
                                borderRadius: '6px', padding: '4px 10px',
                                color: 'var(--text-dark)', fontSize: '12px', cursor: 'pointer',
                              }}
                          >
                            ✏️ Modifica
                          </button>
                      )}
                      {/* Fix (CiakLog Wrapped): link visibile solo sul proprio profilo */}
                      {isOwn && (
                          <Link to="/wrapped" style={{
                            display: 'inline-flex', alignItems: 'center', gap: '4px',
                            backgroundColor: 'var(--accent-subtle)', border: '1px solid var(--border-cta)',
                            borderRadius: '6px', padding: '4px 10px',
                            color: 'var(--gold)', fontSize: '12px', fontWeight: '600', textDecoration: 'none',
                          }}>
                            🎬 Il tuo Wrapped
                          </Link>
                      )}
                    </div>

                    {profile.bio ? (
                        <p style={{ color: 'var(--text-muted)', fontSize: '14px', lineHeight: 1.6, marginBottom: '14px', maxWidth: '500px' }}>
                          {profile.bio}
                        </p>
                    ) : isOwn && (
                        <p
                            onClick={() => setEditing(true)}
                            style={{ color: 'var(--text-dark)', fontSize: '14px', marginBottom: '14px', cursor: 'pointer', fontStyle: 'italic' }}
                        >
                          + Aggiungi una bio
                        </p>
                    )}

                    {/* Stats */}
                    <div style={{ display: 'flex', gap: '28px', alignItems: 'center' }}>
                      <div>
                        <span style={{ color: 'var(--text)', fontWeight: '700', fontSize: '18px' }}>{reviews.length}</span>
                        <span style={{ color: 'var(--text-dark)', fontSize: '13px', marginLeft: '6px' }}>recensioni</span>
                      </div>
                    </div>
                  </>
              )}
            </div>
          </div>

          {/* ── Area scrollabile: in visione + recensioni ── */}
          <div style={{ flex: 1, overflowY: 'auto', paddingBottom: '48px' }}>

          {/* ── In visione ── */}
          {watching.length > 0 && (
              <div style={{ marginBottom: '40px' }}>
                <h2 style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '12px' }}>
                  👀 In visione
                </h2>
                <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                  {watching.map((w, i) => (
                      <div key={i} style={{
                        backgroundColor: 'var(--bg-card)', border: '1px solid var(--border)',
                        borderRadius: '8px', padding: '10px 16px',
                        display: 'flex', alignItems: 'center', gap: '10px',
                      }}>
                        <span style={{ fontSize: '18px' }}>▶️</span>
                        <div>
                          <div style={{ color: 'var(--text)', fontWeight: '600', fontSize: '14px' }}>{w.title || w}</div>
                          {w.season && <div style={{ color: 'var(--text-dark)', fontSize: '12px' }}>Stagione {w.season}</div>}
                        </div>
                      </div>
                  ))}
                </div>
              </div>
          )}

          {/* ── Recensioni ── */}
          <div>
            <h2 style={{ color: 'var(--text)', fontSize: '18px', fontWeight: '700', marginBottom: '20px' }}>
              🎬 Recensioni
              <span style={{ color: 'var(--text-dark)', fontSize: '14px', fontWeight: '400', marginLeft: '8px' }}>
              {reviews.length}
            </span>
            </h2>

            {reviews.length === 0 ? (
                <p style={{ color: 'var(--text-dark)', fontSize: '14px' }}>Nessuna recensione ancora.</p>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--border)' }}>
                  {reviews.map((r, idx) => (
                      <Link
                          to={`/movie/${r.tmdbId}?type=${r.contentType || r.mediaType}`}
                          key={r.id}
                          style={{ textDecoration: 'none' }}
                      >
                        <div
                            style={{
                              display: 'flex', alignItems: 'flex-start', gap: '16px',
                              padding: '16px 20px',
                              backgroundColor: 'var(--bg-card)',
                              borderBottom: idx < reviews.length - 1 ? '1px solid var(--border)' : 'none',
                              transition: 'background-color 0.15s',
                            }}
                            onMouseEnter={e => e.currentTarget.style.backgroundColor = 'var(--bg-hover)'}
                            onMouseLeave={e => e.currentTarget.style.backgroundColor = 'var(--bg-card)'}
                        >
                          {/* Poster */}
                          <div style={{
                            width: '40px', height: '56px', borderRadius: '4px', flexShrink: 0,
                            backgroundColor: 'var(--bg-hover)', border: '1px solid var(--border)',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            fontSize: '20px', overflow: 'hidden',
                          }}>
                            {r.posterPath
                                ? <img src={`https://image.tmdb.org/t/p/w92${r.posterPath}`} alt=""
                                       style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                : '🎬'
                            }
                          </div>

                          {/* Contenuto */}
                          <div style={{ flex: 1, minWidth: 0 }}>
                            <div style={{ display: 'flex', alignItems: 'baseline', gap: '10px', marginBottom: '4px', flexWrap: 'wrap' }}>
                        <span style={{ color: 'var(--text)', fontWeight: '600', fontSize: '15px' }}>
                          {r.title || `#${r.tmdbId}`}
                        </span>
                              <span style={{ color: 'var(--text-dark)', fontSize: '12px' }}>
                          {new Date(r.createdAt).toLocaleDateString('it-IT')}
                        </span>
                            </div>

                            {/* Stelle */}
                            <div style={{ display: 'flex', gap: '2px', marginBottom: r.text ? '6px' : '0', alignItems: 'center' }}>
                              {[1,2,3,4,5].map(n => (
                                  <span key={n} style={{
                                    fontSize: '13px',
                                    filter: n <= r.rating ? 'none' : 'grayscale(1)',
                                    opacity: n <= r.rating ? 1 : 0.2,
                                  }}>🎬</span>
                              ))}
                              <span style={{ color: 'var(--text-dark)', fontSize: '12px', marginLeft: '6px' }}>
                          {EMOJI_RATING[r.rating] || ''}
                        </span>
                            </div>

                            {/* Testo — max 2 righe, gestisce testi lunghi */}
                            {r.text && (
                                <p style={{
                                  color: 'var(--text-muted)', fontSize: '13px', lineHeight: 1.55,
                                  margin: 0,
                                  display: '-webkit-box',
                                  WebkitLineClamp: 2,
                                  WebkitBoxOrient: 'vertical',
                                  overflow: 'hidden',
                                  wordBreak: 'break-word',
                                }}>
                                  {r.text}
                                </p>
                            )}
                          </div>

                          <span style={{ color: 'var(--border-soft)', fontSize: '18px', flexShrink: 0, alignSelf: 'center' }}>›</span>
                        </div>
                      </Link>
                  ))}
                </div>
            )}

            {hasMoreReviews && (
                <button
                    onClick={loadMoreReviews}
                    disabled={loadingMore}
                    style={{
                      marginTop: '12px', width: '100%', padding: '12px',
                      backgroundColor: 'transparent', border: '1px solid var(--border)',
                      borderRadius: '8px', color: 'var(--text-dark)', fontSize: '14px',
                      cursor: loadingMore ? 'default' : 'pointer',
                    }}
                >
                  {loadingMore ? 'Caricamento...' : 'Carica altre recensioni'}
                </button>
            )}
          </div>
          </div>
        </div>
      </div>
  )
}

export default ProfilePage