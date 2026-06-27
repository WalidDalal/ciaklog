import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../services/api'

const FILTERS = [
  { label: 'Tutti', value: 'all' },
  { label: 'Film', value: 'movie' },
  { label: 'Serie TV', value: 'tv' },
]

function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [query, setQuery] = useState(searchParams.get('q') || '')
  const [type, setType] = useState('all')

  const q = searchParams.get('q') || ''

  useEffect(() => {
    if (!q.trim()) return
    setLoading(true)
    // Fix: omette il param type quando è 'all' — il backend non lo accetta
    const params = { q }
    if (type !== 'all') params.type = type
    api.get('/tmdb/search', { params })
      .then(r => setResults(r.data))
      .catch(() => setResults([]))
      .finally(() => setLoading(false))
  }, [q, type])

  const handleSearch = (e) => {
    e.preventDefault()
    if (query.trim()) setSearchParams({ q: query.trim() })
  }

  return (
    <div style={{ backgroundColor: '#0a0a0a', minHeight: '100vh' }}>
      <Navbar />

      <div style={{ maxWidth: '960px', margin: '0 auto', padding: '40px 24px' }}>

        <form onSubmit={handleSearch} style={{ display: 'flex', gap: '12px', marginBottom: '32px' }}>
          <input
            type="text"
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder="Cerca un film o una serie..."
            style={{
              flex: 1, padding: '14px 20px', backgroundColor: '#1a1a1a',
              border: '1px solid #333', borderRadius: '8px', color: 'white', fontSize: '16px',
            }}
          />
          <button type="submit" style={{
            padding: '14px 28px', backgroundColor: '#e50914', border: 'none',
            borderRadius: '8px', color: 'white', fontSize: '15px', fontWeight: '600',
          }}>
            Cerca
          </button>
        </form>

        <div style={{ display: 'flex', gap: '8px', marginBottom: '28px' }}>
          {FILTERS.map(f => (
            <button key={f.value} onClick={() => setType(f.value)} style={{
              padding: '8px 20px', borderRadius: '20px', fontSize: '14px', fontWeight: '500',
              border: `1px solid ${type === f.value ? '#e50914' : '#333'}`,
              backgroundColor: type === f.value ? '#e50914' : 'transparent',
              color: 'white',
            }}>
              {f.label}
            </button>
          ))}
        </div>

        {!q && (
          <div style={{ textAlign: 'center', color: '#6b7280', marginTop: '80px', fontSize: '16px' }}>
            🔍 Digita qualcosa per iniziare a cercare
          </div>
        )}
        {loading && <div style={{ textAlign: 'center', color: '#9ca3af', marginTop: '60px' }}>Ricerca in corso...</div>}
        {!loading && q && results.length === 0 && (
          <div style={{ textAlign: 'center', color: '#6b7280', marginTop: '60px' }}>
            Nessun risultato per "<span style={{ color: 'white' }}>{q}</span>"
          </div>
        )}

        {!loading && results.length > 0 && (
          <>
            <p style={{ color: '#9ca3af', fontSize: '14px', marginBottom: '20px' }}>
              {results.length} risultati per "<span style={{ color: 'white' }}>{q}</span>"
            </p>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: '20px' }}>
              {results.map(item => (
                <Link to={`/movie/${item.tmdbId}?type=${item.contentType || item.mediaType}`} key={`${item.tmdbId}-${item.contentType || item.mediaType}`}>
                  <div
                    style={{ backgroundColor: '#141414', borderRadius: '8px', overflow: 'hidden', border: '1px solid #222' }}
                    onMouseEnter={e => e.currentTarget.style.borderColor = '#e50914'}
                    onMouseLeave={e => e.currentTarget.style.borderColor = '#222'}
                  >
                    {item.posterPath ? (
                      <img src={`https://image.tmdb.org/t/p/w300${item.posterPath}`} alt={item.title}
                        style={{ width: '100%', height: '220px', objectFit: 'cover' }} />
                    ) : (
                      <div style={{ width: '100%', height: '220px', backgroundColor: '#222', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#555', fontSize: '32px' }}>🎬</div>
                    )}
                    <div style={{ padding: '12px' }}>
                      <div style={{ color: 'white', fontWeight: '600', fontSize: '13px', marginBottom: '4px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {item.title}
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ color: '#6b7280', fontSize: '12px' }}>{item.releaseYear || '—'}</span>
                        <span style={{ fontSize: '11px', padding: '2px 6px', borderRadius: '4px', backgroundColor: '#222', color: '#9ca3af' }}>
                          {(item.contentType || item.mediaType) === 'MOVIE' ? '🎬' : '📺'}
                        </span>
                      </div>
                      {item.tmdbRating && (
                        <div style={{ color: '#f5c518', fontSize: '12px', marginTop: '4px' }}>⭐ {item.tmdbRating.toFixed(1)}</div>
                      )}
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  )
}

export default SearchPage