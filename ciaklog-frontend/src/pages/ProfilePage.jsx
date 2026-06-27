import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../services/api'

// Fix: stesso componente grayscale di MovieDetailPage
function StaticRating({ rating }) {
  return (
    <span>
      {[1, 2, 3, 4, 5].map(n => (
        <span key={n} style={{
          fontSize: '18px',
          filter: n <= rating ? 'none' : 'grayscale(1)',
          opacity: n <= rating ? 1 : 0.25,
        }}>🎬</span>
      ))}
    </span>
  )
}

function ProfilePage() {
  const { username } = useParams()
  const [profile, setProfile] = useState(null)
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      api.get(`/users/${username}`),
      api.get(`/reviews/user/${username}`),
    ])
      .then(([profileRes, reviewsRes]) => {
        setProfile(profileRes.data)
        setReviews(reviewsRes.data.content || reviewsRes.data || [])
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [username])

  if (loading) return (
    <div style={{ backgroundColor: '#0a0a0a', minHeight: '100vh' }}>
      <Navbar />
      <div style={{ textAlign: 'center', color: '#9ca3af', marginTop: '100px' }}>Caricamento...</div>
    </div>
  )

  if (!profile) return (
    <div style={{ backgroundColor: '#0a0a0a', minHeight: '100vh' }}>
      <Navbar />
      <div style={{ textAlign: 'center', color: '#9ca3af', marginTop: '100px' }}>Utente non trovato</div>
    </div>
  )

  return (
    <div style={{ backgroundColor: '#0a0a0a', minHeight: '100vh' }}>
      <Navbar />

      <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '40px 24px', display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '40px' }}>

        {/* Colonna sinistra */}
        <div>
          <div style={{ width: '120px', height: '120px', borderRadius: '50%', backgroundColor: '#e50914', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '48px', fontWeight: '700', color: 'white', marginBottom: '20px' }}>
            {username?.[0]?.toUpperCase()}
          </div>

          <h1 style={{ fontSize: '32px', fontWeight: '800', color: 'white', marginBottom: '8px' }}>{username}</h1>

          {profile.bio && (
            <p style={{ color: '#9ca3af', fontSize: '14px', marginBottom: '20px', lineHeight: 1.6 }}>{profile.bio}</p>
          )}

          <div style={{ display: 'flex', gap: '20px', marginBottom: '20px' }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ color: 'white', fontWeight: '700', fontSize: '20px' }}>{reviews.length}</div>
              <div style={{ color: '#9ca3af', fontSize: '12px' }}>Recensioni</div>
            </div>
          </div>

          {/* Fix: campi corretti dal UserProfileResponse */}
          {profile.watchingTitle && (
            <div style={{ backgroundColor: '#141414', border: '1px solid #333', borderRadius: '8px', padding: '12px 16px', marginBottom: '20px' }}>
              <div style={{ color: '#9ca3af', fontSize: '12px', marginBottom: '4px' }}>👀 Sta guardando</div>
              <div style={{ color: 'white', fontWeight: '600', fontSize: '14px' }}>{profile.watchingTitle}</div>
              {profile.watchingSeason && (
                <div style={{ color: '#6b7280', fontSize: '12px' }}>Stagione {profile.watchingSeason}</div>
              )}
            </div>
          )}

          {profile.topGenres?.length > 0 && (
            <div>
              <h3 style={{ color: '#9ca3af', fontSize: '13px', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '12px' }}>🏆 Top generi</h3>
              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                {profile.topGenres.map(g => (
                  <span key={g} style={{ padding: '4px 12px', backgroundColor: '#1a1a1a', border: '1px solid #333', borderRadius: '20px', color: '#9ca3af', fontSize: '13px' }}>{g}</span>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Colonna destra — recensioni */}
        <div>
          <h2 style={{ color: 'white', fontSize: '20px', fontWeight: '700', marginBottom: '20px' }}>
            🎬 Recensioni <span style={{ color: '#6b7280', fontSize: '16px' }}>({reviews.length})</span>
          </h2>

          {reviews.length === 0 ? (
            <p style={{ color: '#6b7280' }}>Nessuna recensione ancora.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {reviews.map(r => (
                <Link to={`/movie/${r.tmdbId}?type=${r.contentType || r.mediaType}`} key={r.id}>
                  <div style={{ backgroundColor: '#141414', border: '1px solid #222', borderRadius: '10px', padding: '16px' }}
                    onMouseEnter={e => e.currentTarget.style.borderColor = '#333'}
                    onMouseLeave={e => e.currentTarget.style.borderColor = '#222'}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                      <span style={{ color: 'white', fontWeight: '600', fontSize: '14px' }}>
                        {r.title || `#${r.tmdbId}`}
                      </span>
                      {/* Fix: grayscale invece di ⬜ */}
                      <StaticRating rating={r.rating} />
                    </div>
                    {r.text && <p style={{ color: '#d1d5db', fontSize: '13px', lineHeight: 1.5 }}>{r.text}</p>}
                    <p style={{ color: '#6b7280', fontSize: '12px', marginTop: '6px' }}>
                      {new Date(r.createdAt).toLocaleDateString('it-IT')}
                    </p>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default ProfilePage