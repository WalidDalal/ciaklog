import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../services/api'
import useAuthStore from '../store/authStore'

const FILTERS = [
  { label: 'Tutti', value: 'all' },
  { label: 'Film', value: 'movie' },
  { label: 'Serie TV', value: 'tv' },
]

function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [query, setQuery] = useState(searchParams.get('q') || '')
  const [type, setType] = useState(searchParams.get('type') || 'all')

  // Fix: prima il backend ignorava il parametro page — sempre e solo la prima
  // pagina di TMDB (20 risultati fissi, 40 con "Tutti" = movie+tv separati).
  // Ora teniamo pagina + hasMore + totale REALE, separati per movie/tv perché
  // "Tutti" combina due endpoint TMDB indipendenti con paginazioni proprie
  const [moviePage, setMoviePage] = useState(1)
  const [tvPage, setTvPage] = useState(1)
  const [movieHasMore, setMovieHasMore] = useState(false)
  const [tvHasMore, setTvHasMore] = useState(false)
  const [totalResults, setTotalResults] = useState(0)
  const { token } = useAuthStore()

  // Fix (suggerimento contestuale AI negli stati vuoti, approvato): frase
  // breve al posto del solito messaggio piatto — solo per utenti loggati
  // (l'endpoint richiede autenticazione), fallback silenzioso altrimenti
  const [emptyTip, setEmptyTip] = useState('')

  const q = searchParams.get('q') || ''
  const hasMore = movieHasMore || tvHasMore

  useEffect(() => {
    if (!token || loading || !q || results.length > 0) { setEmptyTip(''); return }
    api.get('/ai/empty-state-tip', { params: { context: 'SEARCH_NO_RESULTS', query: q } })
      .then(r => setEmptyTip(r.data?.tip || ''))
      .catch(() => setEmptyTip(''))
  }, [token, loading, q, results.length])

  useEffect(() => {
    if (!q.trim()) return
    setLoading(true)
    setResults([])

    if (type === 'all') {
      Promise.all([
        api.get('/tmdb/search', { params: { q, type: 'movie', page: 1 } }),
        api.get('/tmdb/search', { params: { q, type: 'tv', page: 1 } }),
      ])
        .then(([filmsRes, seriesRes]) => {
          const films = filmsRes.data?.results || []
          const series = seriesRes.data?.results || []
          const merged = []
          const max = Math.max(films.length, series.length)
          for (let i = 0; i < max; i++) {
            if (i < films.length) merged.push(films[i])
            if (i < series.length) merged.push(series[i])
          }
          setResults(merged)
          setMoviePage(1); setTvPage(1)
          setMovieHasMore(1 < (filmsRes.data?.totalPages || 1))
          setTvHasMore(1 < (seriesRes.data?.totalPages || 1))
          setTotalResults((filmsRes.data?.totalResults || 0) + (seriesRes.data?.totalResults || 0))
        })
        .catch(() => { setResults([]); setMovieHasMore(false); setTvHasMore(false); setTotalResults(0) })
        .finally(() => setLoading(false))
    } else {
      api.get('/tmdb/search', { params: { q, type, page: 1 } })
        .then(r => {
          setResults(r.data?.results || [])
          if (type === 'movie') { setMoviePage(1); setMovieHasMore(1 < (r.data?.totalPages || 1)); setTvHasMore(false) }
          else { setTvPage(1); setTvHasMore(1 < (r.data?.totalPages || 1)); setMovieHasMore(false) }
          setTotalResults(r.data?.totalResults || 0)
        })
        .catch(() => { setResults([]); setMovieHasMore(false); setTvHasMore(false); setTotalResults(0) })
        .finally(() => setLoading(false))
    }
  }, [q, type])

  const handleLoadMore = async () => {
    setLoadingMore(true)
    try {
      if (type === 'all') {
        const nextMoviePage = movieHasMore ? moviePage + 1 : moviePage
        const nextTvPage = tvHasMore ? tvPage + 1 : tvPage
        const [filmsRes, seriesRes] = await Promise.all([
          movieHasMore ? api.get('/tmdb/search', { params: { q, type: 'movie', page: nextMoviePage } }) : Promise.resolve(null),
          tvHasMore ? api.get('/tmdb/search', { params: { q, type: 'tv', page: nextTvPage } }) : Promise.resolve(null),
        ])
        const films = filmsRes?.data?.results || []
        const series = seriesRes?.data?.results || []
        const merged = []
        const max = Math.max(films.length, series.length)
        for (let i = 0; i < max; i++) {
          if (i < films.length) merged.push(films[i])
          if (i < series.length) merged.push(series[i])
        }
        setResults(prev => [...prev, ...merged])
        if (filmsRes) { setMoviePage(nextMoviePage); setMovieHasMore(nextMoviePage < (filmsRes.data?.totalPages || 1)) }
        if (seriesRes) { setTvPage(nextTvPage); setTvHasMore(nextTvPage < (seriesRes.data?.totalPages || 1)) }
      } else {
        const currentPage = type === 'movie' ? moviePage : tvPage
        const nextPage = currentPage + 1
        const res = await api.get('/tmdb/search', { params: { q, type, page: nextPage } })
        setResults(prev => [...prev, ...(res.data?.results || [])])
        const stillHasMore = nextPage < (res.data?.totalPages || 1)
        if (type === 'movie') { setMoviePage(nextPage); setMovieHasMore(stillHasMore) }
        else { setTvPage(nextPage); setTvHasMore(stillHasMore) }
      }
    } catch { /* silenzioso: chi carica altri risultati non ha bisogno di un errore bloccante */ }
    finally { setLoadingMore(false) }
  }

  const handleSearch = (e) => {
    e.preventDefault()
    if (query.trim()) setSearchParams({ q: query.trim(), type })
  }

  return (
    <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}>
      <Navbar />

      <div style={{ maxWidth: '960px', margin: '0 auto', padding: '40px 24px' }}>

        <form onSubmit={handleSearch} style={{ display: 'flex', gap: '12px', marginBottom: '32px' }}>
          <input
            type="text"
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder="Cerca un film o una serie..."
            style={{
              flex: 1, padding: '14px 20px', backgroundColor: 'var(--bg-hover)',
              border: '1px solid var(--border-soft)', borderRadius: '8px', color: 'var(--text)', fontSize: '16px',
            }}
          />
          <button type="submit" style={{
            padding: '14px 28px', backgroundColor: 'var(--accent)', border: 'none',
            borderRadius: '8px', color: 'var(--text)', fontSize: '15px', fontWeight: '600',
          }}>
            Cerca
          </button>
        </form>

        <div style={{ display: 'flex', gap: '8px', marginBottom: '28px' }}>
          {FILTERS.map(f => (
            <button key={f.value} onClick={() => { setType(f.value); if (q) setSearchParams({ q, type: f.value }) }} style={{
              padding: '8px 20px', borderRadius: '20px', fontSize: '14px', fontWeight: '500',
              border: `1px solid ${type === f.value ? 'var(--accent)' : 'var(--border-soft)'}`,
              backgroundColor: type === f.value ? 'var(--accent)' : 'transparent',
              color: 'var(--text)',
            }}>
              {f.label}
            </button>
          ))}
        </div>

        {!q && (
          <div style={{ textAlign: 'center', color: 'var(--text-dark)', marginTop: '80px', fontSize: '16px' }}>
            🔍 Digita qualcosa per iniziare a cercare
          </div>
        )}
        {loading && <div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '60px' }}>Ricerca in corso...</div>}
        {!loading && q && results.length === 0 && (
          <div style={{ textAlign: 'center', color: 'var(--text-dark)', marginTop: '60px' }}>
            <p>Nessun risultato per "<span style={{ color: 'var(--text)' }}>{q}</span>"</p>
            {emptyTip && (
              <p style={{ color: 'var(--accent)', fontSize: '13px', marginTop: '10px' }}>
                ✨ {emptyTip}
              </p>
            )}
          </div>
        )}

        {!loading && results.length > 0 && (
          <>
            <p style={{ color: 'var(--text-muted)', fontSize: '14px', marginBottom: '20px' }}>
              {/* Fix: prima mostrava solo results.length (max 40/20, il tetto fisso
                  di TMDB) — ora mostra il totale reale disponibile su TMDB */}
              {results.length} di {totalResults} risultati per "<span style={{ color: 'var(--text)' }}>{q}</span>"
            </p>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: '20px' }}>
              {results.map(item => (
                <Link to={`/movie/${item.tmdbId}?type=${item.contentType || item.mediaType}`} key={`${item.tmdbId}-${item.contentType || item.mediaType}`}>
                  <div
                    style={{ backgroundColor: 'var(--bg-card)', borderRadius: '8px', overflow: 'hidden', border: '1px solid var(--border)' }}
                    onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--accent)'}
                    onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}
                  >
                    {item.posterPath ? (
                      <img src={`https://image.tmdb.org/t/p/w300${item.posterPath}`} alt={item.title}
                        style={{ width: '100%', height: '220px', objectFit: 'cover' }} />
                    ) : (
                      <div style={{ width: '100%', height: '220px', backgroundColor: 'var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-dark)', fontSize: '32px' }}>🎬</div>
                    )}
                    <div style={{ padding: '12px' }}>
                      <div style={{ color: 'var(--text)', fontWeight: '600', fontSize: '13px', marginBottom: '4px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {item.title}
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ color: 'var(--text-dark)', fontSize: '12px' }}>{item.releaseYear || '—'}</span>
                        <span style={{ fontSize: '11px', padding: '2px 6px', borderRadius: '4px', backgroundColor: 'var(--border)', color: 'var(--text-muted)' }}>
                          {(item.contentType || item.mediaType) === 'MOVIE' ? '🎬' : '📺'}
                        </span>
                      </div>
                      {/* Fix: prima il voto TMDB spariva del tutto se mancante — un
                          "–" esplicito è più chiaro di una riga che appare/scompare */}
                      <div style={{ color: item.tmdbRating ? 'var(--gold)' : 'var(--text-dark)', fontSize: '12px', marginTop: '4px' }}>
                        {item.tmdbRating ? `⭐ ${item.tmdbRating.toFixed(1)}` : '⭐ –'}
                      </div>
                    </div>
                  </div>
                </Link>
              ))}
            </div>

            {/* Fix: prima non c'era nessun modo di andare oltre il tetto fisso — ora
                "carica altri" richiede la pagina successiva a TMDB finché ce ne sono */}
            {hasMore && (
              <div style={{ textAlign: 'center', marginTop: '32px' }}>
                <button
                  onClick={handleLoadMore}
                  disabled={loadingMore}
                  style={{
                    padding: '12px 32px', backgroundColor: 'transparent',
                    border: '1px solid var(--border-soft)', borderRadius: '8px',
                    color: 'var(--text)', fontSize: '14px', fontWeight: '600',
                    cursor: loadingMore ? 'default' : 'pointer', opacity: loadingMore ? 0.6 : 1,
                  }}
                >
                  {loadingMore ? 'Caricamento...' : 'Carica altri risultati'}
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}

export default SearchPage