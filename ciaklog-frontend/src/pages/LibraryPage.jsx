import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../services/api'
import useAuthStore from '../store/authStore'

const STATUS_FILTERS = [
  { label: '▶️ In visione', value: 'WATCHING' },
  { label: '📌 Da vedere', value: 'TO_WATCH' },
  { label: '✅ Visti', value: 'WATCHED' },
  { label: 'Tutti', value: '' },
]

const STATUS_COLORS = {
  TO_WATCH: '#6b7280',
  WATCHING: '#3b82f6',
  WATCHED: '#22c55e',
}

const STATUS_LABELS = {
  TO_WATCH: 'Da vedere',
  WATCHING: 'In visione',
  WATCHED: 'Visto',
}

// Bottoni contestuali per ogni status
function StatusActions({ entry, onStatusChange, onDelete, deleting }) {
  const { status } = entry

  if (status === 'TO_WATCH') {
    return (
      <div style={{ display: 'flex', gap: '6px' }}>
        <button
          onClick={() => onStatusChange(entry, 'WATCHING')}
          style={{
            flex: 1, padding: '7px 0', backgroundColor: '#3b82f6',
            border: 'none', borderRadius: '6px', color: 'white',
            fontSize: '12px', fontWeight: '600', cursor: 'pointer',
          }}
        >
          ▶️ Inizia
        </button>
        <button
          onClick={() => onDelete(entry.id)}
          disabled={deleting}
          style={{
            padding: '7px 10px', backgroundColor: 'transparent',
            border: '1px solid #2a2a2a', borderRadius: '6px',
            color: '#6b7280', fontSize: '12px', cursor: 'pointer',
          }}
        >
          🗑️
        </button>
      </div>
    )
  }

  if (status === 'WATCHING') {
    return (
      <div style={{ display: 'flex', gap: '6px' }}>
        <button
          onClick={() => onStatusChange(entry, 'WATCHED')}
          style={{
            flex: 1, padding: '7px 0', backgroundColor: '#22c55e',
            border: 'none', borderRadius: '6px', color: 'white',
            fontSize: '12px', fontWeight: '600', cursor: 'pointer',
          }}
        >
          ✅ Completato
        </button>
        <button
          onClick={() => onDelete(entry.id)}
          disabled={deleting}
          style={{
            padding: '7px 10px', backgroundColor: 'transparent',
            border: '1px solid #2a2a2a', borderRadius: '6px',
            color: '#6b7280', fontSize: '12px', cursor: 'pointer',
          }}
        >
          🗑️
        </button>
      </div>
    )
  }

  // WATCHED — solo rimuovi
  if (status === 'WATCHED') {
    return (
      <button
        onClick={() => onDelete(entry.id)}
        disabled={deleting}
        style={{
          width: '100%', padding: '7px 0', backgroundColor: 'transparent',
          border: '1px solid #2a2a2a', borderRadius: '6px',
          color: '#6b7280', fontSize: '12px', cursor: 'pointer',
        }}
      >
        {deleting ? 'Rimozione...' : '🗑️ Rimuovi'}
      </button>
    )
  }

  return null
}

function LibraryPage() {
  const { token } = useAuthStore()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [allEntries, setAllEntries] = useState([])
  const [loading, setLoading] = useState(true)
  // Fix: legge il filtro dall'URL se viene da HomePage con ?filter=
  const [filter, setFilter] = useState(searchParams.get('filter') || 'WATCHING')
  const [deletingId, setDeletingId] = useState(null)

  useEffect(() => {
    if (!token) { navigate('/login'); return }
    setLoading(true)
    api.get('/library')
      .then(r => setAllEntries(r.data.content || r.data))
      .catch(() => setAllEntries([]))
      .finally(() => setLoading(false))
  }, [token, navigate])

  const counts = {
    '': allEntries.length,
    TO_WATCH: allEntries.filter(e => e.status === 'TO_WATCH').length,
    WATCHING: allEntries.filter(e => e.status === 'WATCHING').length,
    WATCHED: allEntries.filter(e => e.status === 'WATCHED').length,
  }

  const entries = filter ? allEntries.filter(e => e.status === filter) : allEntries

  const handleStatusChange = async (entry, newStatus) => {
    try {
      const res = await api.put(`/library/${entry.id}`, null, { params: { status: newStatus } })
      setAllEntries(prev => prev.map(e => e.id === entry.id ? res.data : e))
    } catch (err) {
      alert(err.response?.data?.error || 'Errore durante l\'aggiornamento')
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Rimuovere dalla libreria?')) return
    setDeletingId(id)
    try {
      await api.delete(`/library/${id}`)
      setAllEntries(prev => prev.filter(e => e.id !== id))
    } catch {
      alert('Errore durante la rimozione')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div style={{ backgroundColor: '#0a0a0a', minHeight: '100vh' }}>
      <Navbar />

      <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '40px 24px' }}>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '32px' }}>
          <div>
            <h1 style={{ fontSize: '28px', fontWeight: '800', color: 'white', marginBottom: '8px' }}>📚 La mia libreria</h1>
            <p style={{ color: '#9ca3af', fontSize: '14px' }}>{allEntries.length} contenuti salvati</p>
          </div>
          <Link to="/search">
            <button style={{ padding: '10px 20px', backgroundColor: '#e50914', border: 'none', borderRadius: '8px', color: 'white', fontSize: '14px', fontWeight: '600' }}>
              + Aggiungi
            </button>
          </Link>
        </div>

        {/* Filtri */}
        <div style={{ display: 'flex', gap: '8px', marginBottom: '28px', flexWrap: 'wrap' }}>
          {STATUS_FILTERS.map(f => (
            <button key={f.value} onClick={() => setFilter(f.value)} style={{
              padding: '8px 18px', borderRadius: '20px', fontSize: '14px', fontWeight: '500',
              border: `1px solid ${filter === f.value ? '#e50914' : '#333'}`,
              backgroundColor: filter === f.value ? '#e50914' : 'transparent',
              color: 'white', cursor: 'pointer',
            }}>
              {f.label} <span style={{ opacity: 0.7 }}>({counts[f.value]})</span>
            </button>
          ))}
        </div>

        {loading && <div style={{ textAlign: 'center', color: '#9ca3af', marginTop: '60px' }}>Caricamento...</div>}

        {!loading && entries.length === 0 && (
          <div style={{ textAlign: 'center', marginTop: '80px' }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>🎬</div>
            <p style={{ color: '#6b7280', fontSize: '16px', marginBottom: '20px' }}>
              {filter ? 'Nessun contenuto in questa categoria' : 'La tua libreria è vuota'}
            </p>
            <Link to="/search">
              <button style={{ padding: '12px 28px', backgroundColor: '#e50914', border: 'none', borderRadius: '8px', color: 'white', fontSize: '15px', fontWeight: '600' }}>
                Cerca qualcosa da aggiungere
              </button>
            </Link>
          </div>
        )}

        {!loading && entries.length > 0 && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(175px, 1fr))', gap: '20px' }}>
            {entries.map(entry => (
              <div key={entry.id} style={{ backgroundColor: '#141414', border: '1px solid #222', borderRadius: '10px', overflow: 'hidden' }}>

                <Link to={`/movie/${entry.tmdbId}?type=${entry.contentType || entry.mediaType}`}>
                  <div style={{ position: 'relative' }}>
                    {entry.posterPath ? (
                      <img src={`https://image.tmdb.org/t/p/w300${entry.posterPath}`} alt={entry.title}
                        style={{ width: '100%', height: '240px', objectFit: 'cover' }} />
                    ) : (
                      <div style={{ width: '100%', height: '240px', backgroundColor: '#222', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '40px' }}>🎬</div>
                    )}

                    {/* Badge status sovrapposto */}
                    <div style={{
                      position: 'absolute', top: '8px', left: '8px',
                      display: 'flex', alignItems: 'center', gap: '4px',
                      backgroundColor: 'rgba(0,0,0,0.75)', borderRadius: '10px', padding: '3px 8px',
                    }}>
                      <div style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: STATUS_COLORS[entry.status] }} />
                      <span style={{ color: 'white', fontSize: '10px', fontWeight: '600' }}>{STATUS_LABELS[entry.status]}</span>
                    </div>

                    {entry.currentSeason && (
                      <div style={{
                        position: 'absolute', top: '8px', right: '8px',
                        backgroundColor: 'rgba(0,0,0,0.75)', borderRadius: '8px', padding: '3px 8px',
                      }}>
                        <span style={{ color: '#9ca3af', fontSize: '10px' }}>S{entry.currentSeason}</span>
                      </div>
                    )}
                  </div>
                </Link>

                <div style={{ padding: '10px' }}>
                  <Link to={`/movie/${entry.tmdbId}?type=${entry.contentType || entry.mediaType}`}>
                    <div style={{ color: 'white', fontWeight: '600', fontSize: '13px', marginBottom: '10px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {entry.title}
                    </div>
                  </Link>

                  {/* Bottoni contestuali */}
                  <StatusActions
                    entry={entry}
                    onStatusChange={handleStatusChange}
                    onDelete={handleDelete}
                    deleting={deletingId === entry.id}
                  />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default LibraryPage