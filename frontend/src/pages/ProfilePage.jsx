import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../services/api'

const EMOJI_RATING = ['', '😑', '😐', '🙂', '😊', '🤩']

function ProfilePage() {
  const { username } = useParams()
  const [profile, setProfile] = useState(null)
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [reviewsPage, setReviewsPage] = useState(0)
  const [hasMoreReviews, setHasMoreReviews] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)

  useEffect(() => {
    setReviews([])
    setReviewsPage(0)
    Promise.all([
      api.get(`/users/${username}`),
      api.get(`/reviews/user/${username}`, { params: { page: 0, size: 10 } }),
    ])
      .then(([profileRes, reviewsRes]) => {
        setProfile(profileRes.data)
        const data = reviewsRes.data
        setReviews(data.content || data || [])
        setHasMoreReviews(data.totalPages ? data.page < data.totalPages - 1 : false)
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
      setHasMoreReviews(data.totalPages ? nextPage < data.totalPages - 1 : false)
    } catch {} finally { setLoadingMore(false) }
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

  // "Sta guardando" può essere array o singolo campo
  const watching = profile.watching
    ? (Array.isArray(profile.watching) ? profile.watching : [profile.watching])
    : profile.watchingTitle
      ? [{ title: profile.watchingTitle, season: profile.watchingSeason }]
      : []

  return (
    <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}>
      <Navbar />

      <div style={{ maxWidth: '900px', margin: '0 auto', padding: '48px 24px' }}>

        {/* ── Header profilo ── */}
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '28px', marginBottom: '48px' }}>
          {/* Avatar */}
          <div style={{
            width: '88px', height: '88px', borderRadius: '50%', flexShrink: 0,
            backgroundColor: 'var(--accent)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: '36px', fontWeight: '800', color: 'var(--text)',
          }}>
            {username?.[0]?.toUpperCase()}
          </div>

          <div style={{ flex: 1 }}>
            <h1 style={{ fontSize: '28px', fontWeight: '800', color: 'var(--text)', marginBottom: '6px' }}>
              {username}
            </h1>

            {profile.bio && (
              <p style={{ color: 'var(--text-muted)', fontSize: '14px', lineHeight: 1.6, marginBottom: '14px', maxWidth: '500px' }}>
                {profile.bio}
              </p>
            )}

            {/* Stats */}
            <div style={{ display: 'flex', gap: '28px' }}>
              <div>
                <span style={{ color: 'var(--text)', fontWeight: '700', fontSize: '18px' }}>{reviews.length}</span>
                <span style={{ color: 'var(--text-dark)', fontSize: '13px', marginLeft: '6px' }}>recensioni</span>
              </div>
              {profile.topGenres?.length > 0 && (
                <div style={{ display: 'flex', gap: '6px', alignItems: 'center', flexWrap: 'wrap' }}>
                  {profile.topGenres.slice(0, 3).map(g => (
                    <span key={g} style={{
                      padding: '3px 10px', backgroundColor: 'var(--bg-hover)',
                      border: '1px solid #333', borderRadius: '20px',
                      color: 'var(--text-muted)', fontSize: '12px',
                    }}>{g}</span>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* ── In visione ── */}
        {watching.length > 0 && (
          <div style={{ marginBottom: '40px' }}>
            <h2 style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '12px' }}>
              👀 In visione
            </h2>
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
              {watching.map((w, i) => (
                <div key={i} style={{
                  backgroundColor: 'var(--bg-card)', border: '1px solid #2a2a2a',
                  borderRadius: '8px', padding: '10px 16px',
                  display: 'flex', alignItems: 'center', gap: '10px',
                }}>
                  <span style={{ fontSize: '18px' }}>▶️</span>
                  <div>
                    <div style={{ color: 'var(--text)', fontWeight: '600', fontSize: '14px' }}>
                      {w.title || w}
                    </div>
                    {w.season && (
                      <div style={{ color: 'var(--text-dark)', fontSize: '12px' }}>Stagione {w.season}</div>
                    )}
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
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1px', backgroundColor: 'var(--bg-hover)', borderRadius: '12px', overflow: 'hidden', border: '1px solid #222' }}>
              {reviews.map((r, idx) => (
                <Link
                  to={`/movie/${r.tmdbId}?type=${r.contentType || r.mediaType}`}
                  key={r.id}
                  style={{ textDecoration: 'none' }}
                >
                  <div style={{
                    display: 'flex', alignItems: 'flex-start', gap: '16px',
                    padding: '16px 20px',
                    backgroundColor: 'var(--bg-card)',
                    borderBottom: idx < reviews.length - 1 ? '1px solid #1e1e1e' : 'none',
                    transition: 'background-color 0.15s',
                  }}
                    onMouseEnter={e => e.currentTarget.style.backgroundColor = '#161616'}
                    onMouseLeave={e => e.currentTarget.style.backgroundColor = 'var(--bg-card)'}
                  >
                    {/* Poster placeholder o emoji voto */}
                    <div style={{
                      width: '40px', height: '56px', borderRadius: '4px', flexShrink: 0,
                      backgroundColor: '#1e1e1e', border: '1px solid #2a2a2a',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: '20px',
                    }}>
                      {r.posterPath
                        ? <img src={`https://image.tmdb.org/t/p/w92${r.posterPath}`} alt=""
                            style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '3px' }} />
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

                      {/* Voto con stelle */}
                      <div style={{ display: 'flex', gap: '2px', marginBottom: r.text ? '8px' : '0' }}>
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

                      {r.text && (
                        <p style={{
                          color: 'var(--text-muted)', fontSize: '13px', lineHeight: 1.55,
                          overflow: 'hidden', display: '-webkit-box',
                          WebkitLineClamp: 2, WebkitBoxOrient: 'vertical',
                        }}>
                          {r.text}
                        </p>
                      )}
                    </div>

                    {/* Freccia */}
                    <span style={{ color: 'var(--border-soft)', fontSize: '16px', flexShrink: 0, alignSelf: 'center' }}>›</span>
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
                backgroundColor: 'transparent', border: '1px solid #2a2a2a',
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
  )
}

export default ProfilePage
