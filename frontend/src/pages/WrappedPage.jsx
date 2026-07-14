import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../services/api'

// Fix (Priorità 1 — CiakLog Wrapped): recap personale stile "Spotify Wrapped".
// Statistiche calcolate lato backend da dati già esistenti, più un piccolo
// commento narrativo generato dall'AI (opzionale — la pagina funziona anche
// se l'AI non risponde, il backend torna aiNarrative vuota in quel caso).
function StatCard({ label, value, sub, delay }) {
  return (
    <div style={{
      backgroundColor: 'var(--bg-card)', border: '1px solid #222', borderRadius: '14px',
      padding: '28px 24px', textAlign: 'center',
      animation: `wrappedFadeUp .5s ease both`, animationDelay: `${delay}ms`,
    }}>
      <div style={{ fontSize: '13px', color: 'var(--text-dark)', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '10px' }}>{label}</div>
      <div style={{ fontSize: '32px', fontWeight: '800', color: 'var(--gold)', lineHeight: 1.1, marginBottom: sub ? '6px' : 0 }}>{value}</div>
      {sub && <div style={{ fontSize: '13px', color: 'var(--text-muted)' }}>{sub}</div>}
    </div>
  )
}

function WrappedPage() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get('/wrapped')
      .then(r => setData(r.data))
      .catch(err => setError(err.response?.data?.error || 'Impossibile caricare il tuo Wrapped'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div style={{ minHeight: '100vh', backgroundColor: 'var(--bg)' }}>
      <Navbar />
      <style>{`
        @keyframes wrappedFadeUp { from { opacity: 0; transform: translateY(14px); } to { opacity: 1; transform: translateY(0); } }
      `}</style>

      <div style={{ maxWidth: '780px', margin: '0 auto', padding: '56px 24px 80px' }}>
        {loading && <p style={{ color: 'var(--text-muted)', textAlign: 'center' }}>Preparo il tuo Wrapped...</p>}

        {!loading && error && (
          <div style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
            <p>{error}</p>
            <Link to="/" style={{ color: 'var(--accent)' }}>Torna alla Home</Link>
          </div>
        )}

        {!loading && data && data.totalWatched === 0 && (
          <div style={{ textAlign: 'center', padding: '60px 20px' }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>🎬</div>
            <h1 style={{ color: 'var(--text)', fontSize: '22px', marginBottom: '10px' }}>Il tuo Wrapped è ancora vuoto</h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '14px', marginBottom: '24px' }}>
              Segna qualche titolo come visto e torna qui — ti racconteremo il tuo anno cinematografico.
            </p>
            <Link to="/search" style={{
              display: 'inline-block', padding: '10px 24px', backgroundColor: 'var(--accent)',
              borderRadius: '8px', color: 'var(--text)', fontSize: '14px', fontWeight: '600', textDecoration: 'none',
            }}>
              Cerca qualcosa da guardare
            </Link>
          </div>
        )}

        {!loading && data && data.totalWatched > 0 && (
          <>
            {/* Hero — il numero più caratteristico prima di tutto */}
            <div style={{ textAlign: 'center', marginBottom: '48px', animation: 'wrappedFadeUp .5s ease both' }}>
              <div style={{ fontSize: '13px', color: 'var(--accent)', fontWeight: '700', letterSpacing: '2px', textTransform: 'uppercase', marginBottom: '12px' }}>
                🎬 Il tuo CiakLog Wrapped
              </div>
              <div style={{ fontSize: '72px', fontWeight: '800', color: 'var(--text)', lineHeight: 1 }}>{data.totalWatched}</div>
              <div style={{ fontSize: '15px', color: 'var(--text-muted)', marginTop: '8px' }}>
                titol{data.totalWatched === 1 ? 'o visto' : 'i visti'} finora
              </div>
            </div>

            {/* Commento narrativo AI, se disponibile */}
            {data.aiNarrative && (
              <div style={{
                backgroundColor: 'var(--accent-subtle)', border: '1px solid var(--border-cta)', borderRadius: '12px',
                padding: '20px 24px', marginBottom: '40px', animation: 'wrappedFadeUp .5s ease both', animationDelay: '100ms',
              }}>
                <p style={{ color: 'var(--text)', fontSize: '15px', lineHeight: 1.6, fontStyle: 'italic', margin: 0 }}>
                  "{data.aiNarrative}"
                </p>
              </div>
            )}

            {/* Griglia statistiche */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '16px', marginBottom: '16px' }}>
              <StatCard label="Recensioni scritte" value={data.totalReviews} delay={150} />
              {data.topGenre && (
                <StatCard label="Genere del cuore" value={data.topGenre} sub={`${data.topGenreCount} titoli`} delay={200} />
              )}
              {data.mostActiveMonth && (
                <StatCard label="Mese più cinefilo" value={data.mostActiveMonth} sub={`${data.mostActiveMonthCount} titoli visti`} delay={250} />
              )}
              {data.averageRating != null && (
                <StatCard label="Voto medio dato" value={`${data.averageRating.toFixed(1)}/5`} delay={300} />
              )}
            </div>

            {(data.favoriteTitle || data.leastFavoriteTitle) && (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginTop: '16px' }}>
                {data.favoriteTitle && (
                  <StatCard label="Il tuo preferito" value={data.favoriteTitle} sub={`${'★'.repeat(data.favoriteRating)}`} delay={350} />
                )}
                {data.leastFavoriteTitle && (
                  <StatCard label="Il meno amato" value={data.leastFavoriteTitle} sub={`${'★'.repeat(data.leastFavoriteRating)}`} delay={400} />
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}

export default WrappedPage
