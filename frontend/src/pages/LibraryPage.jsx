import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import Navbar from '../components/Navbar'
import StaticRating from '../components/StaticRating'
import api from '../services/api'
import useAuthStore from '../store/authStore'
import ConfirmModal from '../components/ConfirmModal'
import useToastStore from '../store/toastStore'

const STATUS_FILTERS = [
    { label: '▶️ In visione', value: 'WATCHING' },
    { label: '📌 Da vedere', value: 'TO_WATCH' },
    { label: '✅ Visti', value: 'WATCHED' },
]

const STATUS_COLORS = {
    TO_WATCH: 'var(--text-dark)',
    WATCHING: '#3b82f6',
    WATCHED: '#22c55e',
}

const STATUS_LABELS = {
    TO_WATCH: 'Da vedere',
    WATCHING: 'In visione',
    WATCHED: 'Visto',
}

// Bottoni contestuali per ogni status
function StatusActions({ entry, onStatusChange, onDelete, onSeasonChange, deleting }) {
    const { status } = entry

    if (status === 'TO_WATCH') {
        return (
            <div style={{ display: 'flex', gap: '6px' }}>
                <button
                    onClick={() => onStatusChange(entry, 'WATCHING')}
                    style={{
                        flex: 1, padding: '7px 0', backgroundColor: '#3b82f6',
                        border: 'none', borderRadius: '6px', color: 'var(--text)',
                        fontSize: '12px', fontWeight: '600', cursor: 'pointer',
                    }}
                >
                    ▶️ Inizia
                </button>
                <button
                    onClick={() => onDelete(entry.id)}
                    disabled={deleting}
                    aria-label="Rimuovi dalla libreria"
                    style={{
                        padding: '7px 10px', backgroundColor: 'transparent',
                        border: '1px solid var(--border-soft)', borderRadius: '6px',
                        color: 'var(--text-dark)', fontSize: '12px', cursor: 'pointer',
                    }}
                >
                    🗑️
                </button>
            </div>
        )
    }

    if (status === 'WATCHING') {
        return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                {/* Fix: prima non c'era modo di aggiornare la stagione corrente
                    senza rimuovere e riaggiungere il titolo da capo (solo serie TV) */}
                {/* Fix (Libreria — altezze): lo stepper stagione esiste solo per le
                    serie TV, ma la sua assenza sui film accorciava la card, spostando
                    in alto i bottoni sotto e lasciando spazio vuoto nelle serie accanto
                    nella stessa riga della griglia. Ora lo spazio è sempre riservato
                    (22px, l'altezza dei pulsanti +/-), invisibile per i film. */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', height: '22px', visibility: (entry.contentType || entry.mediaType) === 'TV' ? 'visible' : 'hidden' }}>
                    <span style={{ color: 'var(--text-dark)', fontSize: '11px' }}>Stagione</span>
                    <button
                        onClick={() => onSeasonChange(entry, Math.max(1, (entry.currentSeason || 1) - 1))}
                        aria-label="Stagione precedente"
                        tabIndex={(entry.contentType || entry.mediaType) === 'TV' ? 0 : -1}
                        style={{ width: '22px', height: '22px', padding: 0, backgroundColor: 'transparent', border: '1px solid var(--border-soft)', borderRadius: '4px', color: 'var(--text-dark)', fontSize: '12px', cursor: 'pointer' }}
                    >−</button>
                    <span style={{ color: 'var(--text)', fontSize: '12px', fontWeight: '600', minWidth: '14px', textAlign: 'center' }}>{entry.currentSeason || 1}</span>
                    <button
                        onClick={() => onSeasonChange(entry, (entry.currentSeason || 1) + 1)}
                        aria-label="Stagione successiva"
                        tabIndex={(entry.contentType || entry.mediaType) === 'TV' ? 0 : -1}
                        style={{ width: '22px', height: '22px', padding: 0, backgroundColor: 'transparent', border: '1px solid var(--border-soft)', borderRadius: '4px', color: 'var(--text-dark)', fontSize: '12px', cursor: 'pointer' }}
                    >+</button>
                </div>
                <div style={{ display: 'flex', gap: '6px' }}>
                    <Link
                        to={`/movie/${entry.tmdbId}?type=${entry.contentType || entry.mediaType}`}
                        style={{ flex: 1, textDecoration: 'none' }}
                    >
                        <button
                            style={{
                                width: '100%', padding: '7px 0', backgroundColor: '#22c55e',
                                border: 'none', borderRadius: '6px', color: 'var(--text)',
                                fontSize: '12px', fontWeight: '600', cursor: 'pointer',
                            }}
                        >
                            ✅ Segna come visto
                        </button>
                    </Link>
                    <button
                        onClick={() => onDelete(entry.id)}
                        disabled={deleting}
                        aria-label="Rimuovi dalla libreria"
                        style={{
                            padding: '7px 10px', backgroundColor: 'transparent',
                            border: '1px solid var(--border-soft)', borderRadius: '6px',
                            color: 'var(--text-dark)', fontSize: '12px', cursor: 'pointer',
                        }}
                    >
                        🗑️
                    </button>
                </div>
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
                    border: '1px solid var(--border-soft)', borderRadius: '6px',
                    color: 'var(--text-dark)', fontSize: '12px', cursor: 'pointer',
                }}
            >
                {deleting ? 'Rimozione...' : '🗑️ Rimuovi'}
            </button>
        )
    }

    return null
}

function LibraryPage() {
    const { token, user } = useAuthStore()
    const isAdmin = user?.role === 'ADMIN'
    const navigate = useNavigate()
    const [searchParams] = useSearchParams()

    const [allEntries, setAllEntries] = useState([])
    const [loading, setLoading] = useState(true)
    // Il voto dato non era mai mostrato in libreria — mappa tmdbId_contentType -> rating
    const [ratingMap, setRatingMap] = useState({})
    // Legge il filtro dall'URL se viene da HomePage con ?filter=
    const [filter, setFilter] = useState(searchParams.get('filter') || 'WATCHING')
    const [deletingId, setDeletingId] = useState(null)
    const [confirmDelete, setConfirmDelete] = useState(null) // id da eliminare
    const toast = useToastStore()

    // Frase
    // breve al posto del solito messaggio piatto — fallback silenzioso se
    // l'AI non risponde (il messaggio statico esistente resta comunque)
    const [emptyTip, setEmptyTip] = useState('')

    useEffect(() => {
        setLoading(true)
        api.get('/library', { params: { size: 200 } })
            .then(r => setAllEntries(r.data.content || r.data))
            .catch(() => setAllEntries([]))
            .finally(() => setLoading(false))

        if (user?.username) {
            api.get(`/reviews/user/${user.username}`, { params: { size: 200 } })
                .then(r => {
                    const list = r.data.content || r.data
                    const map = {}
                    list.forEach(rev => { map[`${rev.tmdbId}_${rev.contentType}`] = rev.rating })
                    setRatingMap(map)
                })
                .catch(() => {})
        }
    }, [token, navigate, user?.username])

    const counts = {
        '': allEntries.length,
        TO_WATCH: allEntries.filter(e => e.status === 'TO_WATCH').length,
        WATCHING: allEntries.filter(e => e.status === 'WATCHING').length,
        WATCHED: allEntries.filter(e => e.status === 'WATCHED').length,
    }

    const entries = filter ? allEntries.filter(e => e.status === filter) : allEntries

    useEffect(() => {
        if (loading || entries.length > 0) { setEmptyTip(''); return }
        api.get('/ai/empty-state-tip', { params: { context: 'LIBRARY_EMPTY' } })
            .then(r => setEmptyTip(r.data?.tip || ''))
            .catch(() => setEmptyTip(''))
    }, [loading, entries.length, filter])

    const handleStatusChange = async (entry, newStatus) => {
        // Se si segna come WATCHED, verifica prima se esiste già una recensione.
        // Se non c'è, rimanda alla pagina dettaglio dove l'utente può scriverla.
        try {
            const res = await api.put(`/library/${entry.id}`, null, { params: { status: newStatus } })
            setAllEntries(prev => prev.map(e => e.id === entry.id ? res.data : e))
        } catch (err) {
            toast.show(err.response?.data?.error || 'Errore durante l\'aggiornamento')
        }
    }

    // Aggiorna solo la stagione corrente, senza toccare lo stato
    const handleSeasonChange = async (entry, newSeason) => {
        try {
            const res = await api.patch(`/library/${entry.id}/season`, null, { params: { currentSeason: newSeason } })
            setAllEntries(prev => prev.map(e => e.id === entry.id ? res.data : e))
        } catch (err) {
            toast.show(err.response?.data?.error || 'Errore durante l\'aggiornamento della stagione')
        }
    }

    const handleDelete = (id) => {
        setConfirmDelete(id)
    }

    const confirmDeleteEntry = async () => {
        const id = confirmDelete
        setConfirmDelete(null)
        setDeletingId(id)
        try {
            await api.delete(`/library/${id}`)
            setAllEntries(prev => prev.filter(e => e.id !== id))
            toast.show('Rimosso dalla libreria', 'success')
        } catch (err) {
            // Mostrava sempre "Errore durante la rimozione" generico — ora
            // con la nuova regola (non si rimuove un titolo già recensito) serve
            // mostrare il motivo vero, non un messaggio muto
            toast.show(err.response?.data?.error || 'Errore durante la rimozione')
        } finally {
            setDeletingId(null)
        }
    }

    if (isAdmin) return (
        <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}>
            <Navbar />
            <div style={{ maxWidth: '800px', margin: '120px auto', textAlign: 'center', padding: '0 32px' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>🛡️</div>
                <h2 style={{ color: 'var(--text)', fontSize: '24px', marginBottom: '8px' }}>Sezione non disponibile per gli admin</h2>
                <p style={{ color: 'var(--text-dark)', fontSize: '15px' }}>Gli account admin non hanno una libreria personale.</p>
            </div>
        </div>
    )

    return (
        <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}>
            <Navbar />

            <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '40px 24px' }}>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '32px' }}>
                    <div>
                        <h1 style={{ fontSize: '28px', fontWeight: '800', color: 'var(--text)', marginBottom: '8px' }}>📚 La mia libreria</h1>
                        <p style={{ color: 'var(--text-muted)', fontSize: '14px' }}>{allEntries.length} contenuti salvati</p>
                    </div>
                    <Link to="/search">
                        <button style={{ padding: '10px 20px', backgroundColor: 'var(--accent)', border: 'none', borderRadius: '8px', color: 'var(--text)', fontSize: '14px', fontWeight: '600' }}>
                            + Aggiungi
                        </button>
                    </Link>
                </div>

                {/* Filtri */}
                <div style={{ display: 'flex', gap: '8px', marginBottom: '28px', flexWrap: 'wrap' }}>
                    {STATUS_FILTERS.map(f => (
                        <button key={f.value} onClick={() => setFilter(f.value)} style={{
                            padding: '8px 18px', borderRadius: '20px', fontSize: '14px', fontWeight: '500',
                            border: `1px solid ${filter === f.value ? 'var(--accent)' : 'var(--border-soft)'}`,
                            backgroundColor: filter === f.value ? 'var(--accent)' : 'transparent',
                            color: 'var(--text)', cursor: 'pointer',
                        }}>
                            {f.label} <span style={{ opacity: 0.7 }}>({counts[f.value]})</span>
                        </button>
                    ))}
                </div>

                {loading && <div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '60px' }}>Caricamento...</div>}

                {!loading && entries.length === 0 && (
                    <div style={{ textAlign: 'center', marginTop: '80px' }}>
                        <div style={{ fontSize: '48px', marginBottom: '16px' }}>🎬</div>
                        <p style={{ color: 'var(--text-dark)', fontSize: '16px', marginBottom: emptyTip ? '8px' : '20px' }}>
                            {filter ? 'Nessun contenuto in questa categoria' : 'La tua libreria è vuota'}
                        </p>
                        {emptyTip && (
                            <p style={{ color: 'var(--accent)', fontSize: '13px', marginBottom: '20px' }}>
                                ✨ {emptyTip}
                            </p>
                        )}
                        <Link to="/search">
                            <button style={{ padding: '12px 28px', backgroundColor: 'var(--accent)', border: 'none', borderRadius: '8px', color: 'var(--text)', fontSize: '15px', fontWeight: '600' }}>
                                Cerca qualcosa da aggiungere
                            </button>
                        </Link>
                    </div>
                )}

                {!loading && entries.length > 0 && (
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(175px, 1fr))', gap: '20px' }}>
                        {entries.map(entry => (
                            <div key={entry.id} style={{ backgroundColor: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: '10px', overflow: 'hidden' }}>

                                <Link to={`/movie/${entry.tmdbId}?type=${entry.contentType || entry.mediaType}`}>
                                    <div style={{ position: 'relative' }}>
                                        {entry.posterPath ? (
                                            <img src={`https://image.tmdb.org/t/p/w300${entry.posterPath}`} alt={entry.title}
                                                 style={{ width: '100%', height: '240px', objectFit: 'cover' }} />
                                        ) : (
                                            <div style={{ width: '100%', height: '240px', backgroundColor: 'var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '40px' }}>🎬</div>
                                        )}

                                        {entry.currentSeason && (
                                            <div style={{
                                                position: 'absolute', top: '8px', right: '8px',
                                                backgroundColor: 'var(--bg-overlay)', borderRadius: '8px', padding: '3px 8px',
                                            }}>
                                                <span style={{ color: 'var(--text-muted)', fontSize: '10px' }}>S{entry.currentSeason}</span>
                                            </div>
                                        )}
                                    </div>
                                </Link>

                                <div style={{ padding: '10px' }}>
                                    <Link to={`/movie/${entry.tmdbId}?type=${entry.contentType || entry.mediaType}`}>
                                        <div style={{ color: 'var(--text)', fontWeight: '600', fontSize: '13px', marginBottom: '6px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                            {entry.title}
                                        </div>
                                    </Link>

                                    {/* Fix: voto dato (se presente) — visibile solo per contenuti Visto e recensiti.
                                        Prima appariva solo condizionalmente, spostando i bottoni sotto a
                                        altezze diverse tra le card della stessa riga della griglia — ora lo
                                        spazio è sempre riservato quando lo status è WATCHED */}
                                    {entry.status === 'WATCHED' && (
                                        <div style={{ marginBottom: '10px', minHeight: '17px', transform: 'scale(0.72)', transformOrigin: 'left center' }}>
                                            {ratingMap[`${entry.tmdbId}_${entry.contentType || entry.mediaType}`] && (
                                                <StaticRating rating={ratingMap[`${entry.tmdbId}_${entry.contentType || entry.mediaType}`]} />
                                            )}
                                        </div>
                                    )}

                                    {/* Bottoni contestuali */}
                                    <StatusActions
                                        entry={entry}
                                        onStatusChange={handleStatusChange}
                                        onDelete={handleDelete}
                                        onSeasonChange={handleSeasonChange}
                                        deleting={deletingId === entry.id}
                                    />
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
            {confirmDelete && (
                <ConfirmModal
                    message="Rimuovere questo contenuto dalla libreria?"
                    onConfirm={confirmDeleteEntry}
                    onCancel={() => setConfirmDelete(null)}
                    confirmLabel="Rimuovi"
                />
            )}
        </div>
    )
}

export default LibraryPage