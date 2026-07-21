import { useEffect, useState, useRef } from 'react'
import { Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../services/api'
import useAuthStore from '../store/authStore'
import HeroSection from '../components/HeroSection'

function StarRating({ rating }) {
    if (!rating) return null
    return (
        <span style={{ color: 'var(--gold)', fontSize: '13px' }}>
      {'★'.repeat(Math.round(rating))}{'☆'.repeat(5 - Math.round(rating))}
            <span style={{ color: 'var(--text-muted)', marginLeft: '6px', fontSize: '12px' }}>{rating?.toFixed(1)}</span>
    </span>
    )
}

const AVATAR_COLORS = ['var(--accent)', '#3b82f6', '#22c55e', '#f59e0b', '#8b5cf6', '#ec4899']

function UserAvatar({ username, size = 48, index = 0 }) {
    const color = AVATAR_COLORS[index % AVATAR_COLORS.length]
    return (
        <div style={{
            width: size, height: size, borderRadius: '50%', backgroundColor: color,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: 'var(--text)', fontWeight: '800', fontSize: size * 0.4, flexShrink: 0,
        }}>
            {username?.[0]?.toUpperCase()}
        </div>
    )
}

function RankBadge({ rank }) {
    const configs = {
        1: { bg: 'linear-gradient(135deg, #f5c518, #f59e0b)', color: '#000' },
        2: { bg: 'linear-gradient(135deg, #9ca3af, #6b7280)', color: '#fff' },
        3: { bg: 'linear-gradient(135deg, #cd7c2c, #a0522d)', color: '#fff' },
    }
    const c = configs[rank] || { bg: 'var(--bg-hover)', color: 'var(--text-muted)' }
    return (
        <div style={{
            background: c.bg, color: c.color, width: '28px', height: '28px', borderRadius: '50%',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontWeight: '800', fontSize: '12px', flexShrink: 0,
        }}>
            {rank}
        </div>
    )
}

function TrendingQuoteCard({ item, reviews }) {
    //
    // weeklyReviewCount conta le recensioni degli ultimi 7 giorni (vedi
    // ChartServiceImpl), ma qui si mostravano le ultime 2 recensioni in
    // assoluto — con "1 recensione questa settimana" sopra e sotto 2 card
    // (una recente + una vecchia). Le card ora rispettano la stessa finestra
    // di 7 giorni SOLO per il trending vero — il fallback ai titoli popolari
    // (item.weeklyReviewCount == null) esiste apposta per i casi con poca
    // attività settimanale, quindi lì mostriamo comunque i commenti disponibili
    const weekAgo = Date.now() - 7 * 24 * 60 * 60 * 1000
    const visible = reviews
        .filter(r => r.text && r.text.trim().length > 0)
        .filter(r => item.weeklyReviewCount == null || !r.createdAt || new Date(r.createdAt).getTime() >= weekAgo)
        .slice(0, 2)
    return (
        <Link to={`/movie/${item.tmdbId}?type=${item.contentType}`}>
            <div style={{
                backgroundColor: 'var(--bg-nav)', border: '1px solid var(--border)', borderRadius: '14px',
                overflow: 'hidden', display: 'flex', flexDirection: 'column', height: '100%',
            }}
                 onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--accent)'}
                 onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}
            >
                <div style={{ display: 'flex', gap: '14px', padding: '16px 16px 12px' }}>
                    {item.posterPath
                        ? <img src={`https://image.tmdb.org/t/p/w92${item.posterPath}`} alt={item.title}
                               style={{ width: '52px', height: '78px', objectFit: 'cover', borderRadius: '6px', flexShrink: 0 }} />
                        : <div style={{ width: '52px', height: '78px', backgroundColor: 'var(--border)', borderRadius: '6px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '20px', flexShrink: 0 }}>🎬</div>
                    }
                    <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ color: 'var(--text)', fontWeight: '700', fontSize: '15px', marginBottom: '6px', lineHeight: 1.3 }}>{item.title}</div>
                        <StarRating rating={item.ciakLogAverageRating ?? item.averageRating} />
                        {/* Fix: quando il fallback (titoli popolari) sostituisce il trending vero,
                            weeklyReviewCount non esiste su quei dati — mostrarlo comunque dava
                            "💬  recensioni questa settimana" con un buco vuoto al posto del numero,
                            che sembrava un errore invece di semplicemente non applicarsi */}
                        {item.weeklyReviewCount != null && (
                            <div style={{ color: 'var(--text-dark)', fontSize: '12px', marginTop: '6px' }}>
                                💬 {item.weeklyReviewCount} {item.weeklyReviewCount === 1 ? 'recensione' : 'recensioni'} questa settimana
                            </div>
                        )}
                    </div>
                </div>
                <div style={{ padding: '0 16px 16px', display: 'flex', flexDirection: 'column', gap: '10px', flex: 1 }}>
                    {visible.length === 0
                        ? <div style={{ color: 'var(--text-dark)', fontSize: '13px', fontStyle: 'italic', padding: '8px 0' }}>
                            Ancora nessuna recensione con testo.
                        </div>
                        : visible.map((r, i) => (
                            <div key={r.id} style={{
                                backgroundColor: 'var(--bg-card)', borderRadius: '10px', padding: '12px',
                                borderLeft: `3px solid ${i === 0 ? 'var(--accent)' : '#3b82f6'}`,
                            }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                                    <UserAvatar username={r.username} size={22} index={i} />
                                    <span style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: '600' }}>{r.username}</span>
                                    <span style={{ color: 'var(--gold)', fontSize: '11px', marginLeft: 'auto' }}>{'★'.repeat(r.rating)}</span>
                                </div>
                                <p style={{
                                    color: 'var(--text)', fontSize: '13px', lineHeight: 1.5, margin: 0,
                                    display: '-webkit-box', WebkitLineClamp: 3, WebkitBoxOrient: 'vertical', overflow: 'hidden',
                                }}>
                                    "{r.text}"
                                </p>
                            </div>
                        ))
                    }
                </div>
            </div>
        </Link>
    )
}

// Carosello con frecce
function TrendingCarousel({ trending, trendingReviews }) {
    const [page, setPage] = useState(0)
    const itemsPerPage = 3
    const totalPages = Math.ceil(trending.length / itemsPerPage)
    const visible = trending.slice(page * itemsPerPage, page * itemsPerPage + itemsPerPage)

    if (trending.length === 0) return (
        <div style={{ color: 'var(--text-dark)', textAlign: 'center', padding: '40px' }}>Nessun dato disponibile.</div>
    )

    return (
        <div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '20px' }}>
                {visible.map(item => (
                    <TrendingQuoteCard
                        key={item.tmdbId}
                        item={item}
                        reviews={trendingReviews[`${item.tmdbId}_${item.contentType}`] || []}
                    />
                ))}
            </div>
            {totalPages > 1 && (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '16px' }}>
                    <button
                        onClick={() => setPage(p => Math.max(0, p - 1))}
                        disabled={page === 0}
                        style={{
                            width: '40px', height: '40px', borderRadius: '50%',
                            backgroundColor: page === 0 ? 'var(--bg-hover)' : 'var(--border)',
                            border: '1px solid var(--border-soft)', color: page === 0 ? 'var(--text-dark)' : 'var(--text)',
                            fontSize: '18px', cursor: page === 0 ? 'default' : 'pointer',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                        }}
                    >←</button>
                    <div style={{ display: 'flex', gap: '6px' }}>
                        {Array.from({ length: totalPages }).map((_, i) => (
                            <div key={i} onClick={() => setPage(i)} style={{
                                width: i === page ? '20px' : '8px', height: '8px',
                                borderRadius: '4px',
                                backgroundColor: i === page ? 'var(--accent)' : 'var(--border-soft)',
                                cursor: 'pointer', transition: 'all 0.2s',
                            }} />
                        ))}
                    </div>
                    <button
                        onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                        disabled={page === totalPages - 1}
                        style={{
                            width: '40px', height: '40px', borderRadius: '50%',
                            backgroundColor: page === totalPages - 1 ? 'var(--bg-hover)' : 'var(--border)',
                            border: '1px solid var(--border-soft)', color: page === totalPages - 1 ? 'var(--text-dark)' : 'var(--text)',
                            fontSize: '18px', cursor: page === totalPages - 1 ? 'default' : 'pointer',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                        }}
                    >→</button>
                </div>
            )}
        </div>
    )
}

function HomePage() {
    const { token, user } = useAuthStore()
    const logged = !!token

    const [topFilms, setTopFilms] = useState([])
    const [topSeries, setTopSeries] = useState([])
    const [topUsers, setTopUsers] = useState([])
    const [trending, setTrending] = useState([])
    const [trendingReviews, setTrendingReviews] = useState({})
    const [recentFilms, setRecentFilms] = useState([])
    const [watching, setWatching] = useState([])
    const [toWatch, setToWatch] = useState([])
    // Conteggio segnalazioni in attesa, mostrato nell'hero
    // al posto del badge "sei #X in classifica" che per l'Admin non ha senso
    const [pendingReportsCount, setPendingReportsCount] = useState(0)
    // Card operativa leggera — segnalazioni di oggi
    // + utenti da controllare (2ª violazione, sospensione temporanea)
    const [opStats, setOpStats] = useState(null)
    // Sezioni community collassate di default per l'Admin —
    // restano disponibili con un click, ma non sono la priorità visiva
    const [communityOpen, setCommunityOpen] = useState(false)

    useEffect(() => {
        const controller = new AbortController()
        const signal = controller.signal
        const cfg = { signal }

        api.get('/charts/films', cfg).then(r => setTopFilms(r.data?.content || r.data || [])).catch(() => {})
        api.get('/charts/series', cfg).then(r => setTopSeries(r.data?.content || r.data || [])).catch(() => {})
        api.get('/charts/users', cfg).then(r => setTopUsers(r.data?.content || r.data || [])).catch(() => {})
        api.get('/charts/films', cfg).then(r => setRecentFilms(r.data?.slice(0, 10) || [])).catch(() => {})
        api.get('/charts/trending', cfg).then(r => {
            const validTrending = (r.data || []).filter(item => item.weeklyReviewCount > 0)
            setTrending(validTrending)
        }).catch(() => {})

        if (token) {
            api.get('/library', { params: { status: 'WATCHING' }, signal })
                .then(r => setWatching(r.data.content || r.data))
                .catch(() => {})
            api.get('/library', { params: { status: 'TO_WATCH' }, signal })
                .then(r => setToWatch(r.data.content || r.data))
                .catch(() => {})
        }

        // Conteggio segnalazioni PENDING per la card operativa
        // nell'hero — solo per Admin, size:1 perché ci serve solo totalElements.
        // PageSerializationMode.VIA_DTO annida totalElements sotto `.page.`,
        // non in cima — stesso bug già trovato in AdminPage.jsx/ProfilePage.jsx
        if (token && user?.role === 'ADMIN') {
            api.get('/reports', { params: { status: 'PENDING', size: 1 }, signal })
                .then(r => setPendingReportsCount(r.data?.page?.totalElements ?? 0))
                .catch(() => {})

            api.get('/admin/operational-stats', { signal })
                .then(r => setOpStats(r.data))
                .catch(() => {})
        }

        return () => controller.abort()
    }, [token])

    // Prima le recensioni/commenti venivano caricati
    // SOLO per il trending vero — quando era vuoto (poca attività questa
    // settimana) si passava ai titoli popolari (recentFilms) come fallback, ma
    // nessuno andava mai a recuperare le loro recensioni: 3 titoli, zero
    // commenti sotto, sempre. Ora carica le recensioni per qualunque lista sia
    // effettivamente mostrata (evitando di richiederle due volte se già in cache).
    useEffect(() => {
        const itemsShown = trending.length > 0 ? trending : recentFilms
        if (itemsShown.length === 0) return
        const controller = new AbortController()
        itemsShown.forEach(item => {
            const key = `${item.tmdbId}_${item.contentType}`
            if (trendingReviews[key]) return
            api.get(`/reviews/media/${item.contentType}/${item.tmdbId}`, { params: { size: 5, sort: 'createdAt,desc' }, signal: controller.signal })
                .then(res => {
                    const reviews = res.data.content || res.data
                    setTrendingReviews(prev => ({ ...prev, [key]: reviews }))
                })
                .catch(() => {})
        })
        return () => controller.abort()
    }, [trending, recentFilms]) // eslint-disable-line

    // Sezioni community collassate di default per l'Admin
    const isAdmin = user?.role === 'ADMIN'

    // FindIndex+1 ignorava del tutto
    // il campo "rank" calcolato dal backend (che gestisce i pareggi), usando
    // la posizione grezza nell'array — con 3 utenti a pari punteggio, il 2°
    // e il 3° dell'array finivano "3ª" e "4ª posizione" anche se avevano lo
    // stesso punteggio del 1° tra i tre. Ora usa il rank corretto dal backend.
    const myRankIndex = topUsers.findIndex(u => u.username === user?.username)
    const myRank = myRankIndex >= 0 ? (topUsers[myRankIndex].rank ?? myRankIndex + 1) : 0

    // Fix podio: ordine visivo corretto — 2° sinistra, 1° centro, 3° destra
    // L'ordine visivo [2°,1°,3°] ora si
    // calcola dinamicamente in base ai gruppi di rank (vedi sezione podio),
    // non più su indici fissi nell'array — un pareggio poteva lasciare fuori
    // dal podio un utente con lo stesso punteggio di chi ci stava dentro
    const rankHeights = { 1: 110, 2: 70, 3: 44 } // altezza colonna per rank
    const rankSizes   = { 1: 76,  2: 60, 3: 52 }

    return (
        <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh', color: 'var(--text)' }}>
            <Navbar />

            {/* ── HERO ── */}
            <section style={{ position: 'relative', minHeight: '480px', display: 'flex', alignItems: 'center', overflow: 'hidden' }}>
                <div style={{
                    position: 'absolute', right: 0, top: 0, bottom: 0, width: '65%',
                    display: 'flex', gap: '6px', overflow: 'hidden',
                }}>
                    {/* Velo uniforme su tutta la fascia: senza, i poster verso destra restano
                        completamente non sfumati e risultano troppo in risalto rispetto agli
                        altri — specie in light mode, dove il contrasto con il bg chiaro li fa
                        notare di più */}
                    <div style={{
                        position: 'absolute', inset: 0, zIndex: 1,
                        backgroundColor: 'var(--bg)', opacity: 0.18,
                        pointerEvents: 'none',
                    }} />
                    {/* Overlay che sfuma verso il bg — funziona in dark e light */}
                    <div style={{
                        position: 'absolute', inset: 0, zIndex: 1,
                        background: 'linear-gradient(to right, var(--bg) 0%, transparent 35%)',
                        pointerEvents: 'none',
                    }} />
                    {['/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg','/qNBAXBIQlnOThrVvA6mA2B5ggV6.jpg','/tmU7GeKVybMWFButWEGl2M4GeiP.jpg','/3bhkrj58Vtu7enYsRolD1fZdja1.jpg','/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg'].map((p,i) => (
                        <img key={i} src={`https://image.tmdb.org/t/p/w300${p}`} alt="" style={{ height: '100%', width: '140px', objectFit: 'cover', flexShrink: 0, opacity: 0.55 }} />
                    ))}
                </div>
                <div style={{ position: 'absolute', inset: 0, background: 'var(--gradient-hero)', zIndex: 1 }} />

                <div style={{ position: 'relative', zIndex: 2, padding: '0 64px', maxWidth: '620px' }}>
                    {logged ? (
                        <>
                            {/* Fix (Homepage — saluto duplicato): "Bentornato" sopra e "Ciao, username"
                                sotto dicevano la stessa cosa due volte. L'eyebrow dell'Admin resta
                                ("Pannello Admin" è un'etichetta di sezione, non un saluto — non è ridondante) */}
                            {user?.role === 'ADMIN' && (
                                <div style={{ fontSize: '13px', color: 'var(--accent)', fontWeight: '700', marginBottom: '12px', letterSpacing: '1px', textTransform: 'uppercase' }}>👮 Pannello Admin</div>
                            )}
                            <h1 style={{ fontSize: '52px', fontWeight: '800', lineHeight: 1.05, marginBottom: '16px' }}>
                                {user?.role === 'ADMIN' ? <>Ciao, <span style={{ color: 'var(--accent)' }}>{user?.username}</span> 🛡️</> : <>Bentornato, <span style={{ color: 'var(--accent)' }}>{user?.username}</span> 👋</>}
                            </h1>
                            {myRank > 0 && myRank <= 10 && user?.role !== 'ADMIN' && (
                                <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', backgroundColor: 'var(--accent-subtle)', border: '1px solid var(--border-cta)', borderRadius: '8px', padding: '8px 14px', marginBottom: '24px' }}>
                                    <span>🏆</span>
                                    <span style={{ color: 'var(--gold)', fontSize: '14px', fontWeight: '600' }}>
                    Sei #{myRank} nella classifica — continua così!
                  </span>
                                </div>
                            )}
                            {/* Fix (Home Admin): card operativa al posto del badge classifica,
                                che per l'Admin non ha senso (è escluso dalla classifica) */}
                            {user?.role === 'ADMIN' && (
                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', marginBottom: '24px' }}>
                                    <Link to="/admin?tab=reports&filter=PENDING">
                                        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', backgroundColor: pendingReportsCount > 0 ? 'rgba(239,68,68,0.12)' : 'var(--accent-subtle)', border: `1px solid ${pendingReportsCount > 0 ? '#ef4444' : 'var(--border-cta)'}`, borderRadius: '8px', padding: '8px 14px', cursor: 'pointer' }}>
                                            <span>🚩</span>
                                            <span style={{ color: pendingReportsCount > 0 ? '#ef4444' : 'var(--text-muted)', fontSize: '14px', fontWeight: '600' }}>
                                                {pendingReportsCount > 0
                                                    ? `${pendingReportsCount} segnalazion${pendingReportsCount === 1 ? 'e' : 'i'} in attesa`
                                                    : 'Nessuna segnalazione in attesa'}
                                            </span>
                                        </div>
                                    </Link>
                                    {/* Fix (Home Admin, deciso): card operativa leggera, non una
                                        dashboard ricopiata — solo i numeri utili per decidere cosa
                                        fare oggi. "Utenti da controllare" = sospesi temporaneamente
                                        (2ª violazione) in attesa di una decisione manuale */}
                                    {opStats && (
                                        <>
                                            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', backgroundColor: 'var(--bg-nav)', border: '1px solid var(--border)', borderRadius: '8px', padding: '8px 14px' }}>
                                                <span>📅</span>
                                                <span style={{ color: 'var(--text-muted)', fontSize: '14px', fontWeight: '600' }}>
                                                    {opStats.reportsToday} segnalazion{opStats.reportsToday === 1 ? 'e' : 'i'} oggi
                                                </span>
                                            </div>
                                            {opStats.usersToReview > 0 && (
                                                <Link to="/admin?tab=users">
                                                    <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', backgroundColor: 'rgba(234,179,8,0.12)', border: '1px solid #eab308', borderRadius: '8px', padding: '8px 14px', cursor: 'pointer' }}>
                                                        <span>⚠️</span>
                                                        <span style={{ color: '#eab308', fontSize: '14px', fontWeight: '600' }}>
                                                            {opStats.usersToReview} utent{opStats.usersToReview === 1 ? 'e' : 'i'} da controllare
                                                        </span>
                                                    </div>
                                                </Link>
                                            )}
                                        </>
                                    )}
                                </div>
                            )}
                            {user?.role !== 'ADMIN' && (
                                <Link to="/library">
                                    <button style={{ padding: '13px 32px', backgroundColor: 'var(--accent)', border: 'none', borderRadius: '8px', color: 'var(--text)', fontSize: '15px', fontWeight: '700' }}>
                                        📚 Vai alla tua libreria
                                    </button>
                                </Link>
                            )}
                        </>
                    ) : (
                        <HeroSection />
                    )}
                </div>
            </section>

            {/* ── CONTINUA A GUARDARE + DA VEDERE (loggato) ── */}
            {logged && (watching.length > 0 || toWatch.length > 0) && (
                <section style={{ padding: '0 64px 48px' }}>
                    <div style={{ display: 'grid', gridTemplateColumns: watching.length > 0 && toWatch.length > 0 ? '1fr 1fr' : '1fr', gap: '32px' }}>

                        {/* Continua a guardare */}
                        {watching.length > 0 && (
                            <div>
                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                                    <h2 style={{ fontSize: '18px', fontWeight: '700' }}>▶️ Continua a guardare</h2>
                                    <Link to="/library?filter=WATCHING" style={{ color: 'var(--accent)', fontSize: '13px', fontWeight: '600' }}>Vedi tutti →</Link>
                                </div>
                                <div style={{ display: 'flex', gap: '12px', overflowX: 'auto', paddingBottom: '4px' }}>
                                    {watching.slice(0, 6).map(item => (
                                        <Link to={`/movie/${item.tmdbId}?type=${item.contentType || item.mediaType}`} key={item.id} style={{ flexShrink: 0 }}>
                                            <div style={{ width: '120px', backgroundColor: 'var(--bg-card)', borderRadius: '8px', overflow: 'hidden', border: '1px solid var(--border)' }}
                                                 onMouseEnter={e => e.currentTarget.style.borderColor = '#3b82f6'}
                                                 onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}
                                            >
                                                <div style={{ position: 'relative' }}>
                                                    {item.posterPath
                                                        ? <img src={`https://image.tmdb.org/t/p/w200${item.posterPath}`} alt={item.title} style={{ width: '100%', height: '170px', objectFit: 'cover' }} />
                                                        : <div style={{ width: '100%', height: '170px', backgroundColor: 'var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '32px' }}>🎬</div>
                                                    }
                                                    <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: '16px 6px 4px', background: 'var(--gradient-card)' }}>
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '3px' }}>
                                                            <div style={{ width: '5px', height: '5px', borderRadius: '50%', backgroundColor: '#3b82f6' }} />
                                                            <span style={{ color: '#93c5fd', fontSize: '10px', fontWeight: '600' }}>In visione</span>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div style={{ padding: '8px' }}>
                                                    <div style={{ color: 'var(--text)', fontSize: '11px', fontWeight: '600', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item.title}</div>
                                                    {/* Fix (Homepage — In visione, stesso problema della Libreria): senza
                                                        questa riga sempre presente, i film (senza stagione) restavano più
                                                        bassi delle serie nella stessa fila della scroll orizzontale */}
                                                    <div style={{ color: 'var(--text-dark)', fontSize: '10px', visibility: item.currentSeason ? 'visible' : 'hidden' }}>S{item.currentSeason || 1}</div>
                                                </div>
                                            </div>
                                        </Link>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Da vedere */}
                        {toWatch.length > 0 && (
                            <div>
                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                                    <h2 style={{ fontSize: '18px', fontWeight: '700' }}>📌 Da vedere</h2>
                                    <Link to="/library?filter=TO_WATCH" style={{ color: 'var(--accent)', fontSize: '13px', fontWeight: '600' }}>Vedi tutti →</Link>
                                </div>
                                <div style={{ display: 'flex', gap: '12px', overflowX: 'auto', paddingBottom: '4px' }}>
                                    {toWatch.slice(0, 6).map(item => (
                                        <Link to={`/movie/${item.tmdbId}?type=${item.contentType || item.mediaType}`} key={item.id} style={{ flexShrink: 0 }}>
                                            <div style={{ width: '120px', backgroundColor: 'var(--bg-card)', borderRadius: '8px', overflow: 'hidden', border: '1px solid var(--border)' }}
                                                 onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--text-dark)'}
                                                 onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}
                                            >
                                                {item.posterPath
                                                    ? <img src={`https://image.tmdb.org/t/p/w200${item.posterPath}`} alt={item.title} style={{ width: '100%', height: '170px', objectFit: 'cover' }} />
                                                    : <div style={{ width: '100%', height: '170px', backgroundColor: 'var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '32px' }}>🎬</div>
                                                }
                                                <div style={{ padding: '8px' }}>
                                                    <div style={{ color: 'var(--text)', fontSize: '11px', fontWeight: '600', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item.title}</div>
                                                </div>
                                            </div>
                                        </Link>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                </section>
            )}

            {/* Fix (Home Admin): toggle visibile solo per l'Admin — le sezioni
                community restano disponibili ma collassate di default */}
            {isAdmin && (
                <div style={{ padding: '0 64px' }}>
                    <button
                        onClick={() => setCommunityOpen(o => !o)}
                        style={{
                            display: 'flex', alignItems: 'center', gap: '8px', width: '100%',
                            padding: '14px 18px', backgroundColor: 'var(--bg-nav)', border: '1px solid var(--border)',
                            borderRadius: '10px', color: 'var(--text-muted)', fontSize: '14px', fontWeight: '600',
                            cursor: 'pointer', marginBottom: communityOpen ? '8px' : '48px',
                        }}
                    >
                        <span>{communityOpen ? '▼' : '▶'}</span>
                        <span>📊 Vista Community (Top Film/Serie, Trending, Classifica)</span>
                    </button>
                </div>
            )}

            {(!isAdmin || communityOpen) && (
                <>
                    {/* ── TOP FILM & SERIE ── */}
                    <section style={{ padding: '0 64px 48px' }}>
                        <h2 style={{ fontSize: '22px', fontWeight: '700', marginBottom: '28px' }}>🏆 I più amati dalla community</h2>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
                            {/* Fix (Homepage — podio "leggero"): niente rettangoli/gradino sotto
                        (troppo ingombranti in questo riquadro stretto), ma le 3 posizioni
                        sono comunque a altezze diverse — 1° in alto, 2° un po' più in
                        basso, 3° ancora di più — usando solo un offset verticale */}
                            {[{ label: '🎬 Top Film', data: topFilms, type: 'MOVIE' }, { label: '📺 Top Serie TV', data: topSeries, type: 'TV' }].map(({ label, data, type }) => {
                                const podium = data.length >= 3 ? [data[1], data[0], data[2]] : []
                                const offsetForRank = { 1: 0, 2: 22, 3: 40 }
                                return (
                                    <div key={type} style={{ backgroundColor: 'var(--bg-nav)', border: '1px solid var(--border)', borderRadius: '12px', padding: '20px', boxShadow: 'var(--shadow)' }}>
                                        <div style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '1.5px', marginBottom: '16px' }}>{label}</div>
                                        {data.length === 0 ? (
                                            <div style={{ color: 'var(--text-dark)', fontSize: '13px', padding: '12px 0' }}>Ancora poche recensioni — torna presto! 🎬</div>
                                        ) : podium.length === 3 ? (
                                            <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'center', gap: '14px', padding: '4px 0 8px' }}>
                                                {podium.map((item, i) => {
                                                    const rank = i === 1 ? 1 : (i === 0 ? 2 : 3)
                                                    const posterSize = rank === 1 ? 76 : 58
                                                    return (
                                                        <Link to={`/movie/${item.tmdbId}?type=${type}`} key={item.tmdbId} style={{ marginTop: `${offsetForRank[rank]}px` }}>
                                                            <div style={{ textAlign: 'center', width: rank === 1 ? '100px' : '82px' }}>
                                                                {rank === 1 && <div style={{ fontSize: '18px', marginBottom: '4px' }}>👑</div>}
                                                                <div style={{ position: 'relative', display: 'inline-block', marginBottom: '6px' }}>
                                                                    {item.posterPath
                                                                        ? <img src={`https://image.tmdb.org/t/p/w154${item.posterPath}`} alt={item.title} style={{ width: posterSize, height: posterSize * 1.44, objectFit: 'cover', borderRadius: '6px' }} />
                                                                        : <div style={{ width: posterSize, height: posterSize * 1.44, backgroundColor: 'var(--bg-hover)', borderRadius: '6px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '16px' }}>🎬</div>
                                                                    }
                                                                    {rank !== 1 && (
                                                                        <div style={{
                                                                            position: 'absolute', bottom: -4, right: -4,
                                                                            background: rank === 2 ? 'linear-gradient(135deg,#c0c0c0,#9ca3af)' : 'linear-gradient(135deg,#cd7c2c,#a0522d)',
                                                                            width: '16px', height: '16px', borderRadius: '50%',
                                                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                                                            fontSize: '8px', fontWeight: '800', color: 'var(--text)', border: '2px solid var(--bg-nav)',
                                                                        }}>{rank}</div>
                                                                    )}
                                                                </div>
                                                                <div style={{ color: 'var(--text)', fontWeight: rank === 1 ? '800' : '700', fontSize: rank === 1 ? '12px' : '11px', marginBottom: '3px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                                                    {item.title}
                                                                </div>
                                                                <StarRating rating={item.ciakLogAverageRating ?? item.averageRating} />
                                                            </div>
                                                        </Link>
                                                    )
                                                })}
                                            </div>
                                        ) : (
                                            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                                                {data.slice(0, 3).map((item, i) => (
                                                    <Link to={`/movie/${item.tmdbId}?type=${type}`} key={item.tmdbId ?? item.id ?? i}>
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '10px', borderRadius: '8px', backgroundColor: 'var(--bg-card)', border: '1px solid var(--border-soft)' }}
                                                             onMouseEnter={e => e.currentTarget.style.backgroundColor = 'var(--bg-hover)'}
                                                             onMouseLeave={e => e.currentTarget.style.backgroundColor = 'var(--bg-card)'}
                                                        >
                                                            <RankBadge rank={i + 1} />
                                                            {item.posterPath && <img src={`https://image.tmdb.org/t/p/w92${item.posterPath}`} alt={item.title} style={{ width: '36px', height: '52px', objectFit: 'cover', borderRadius: '4px', flexShrink: 0 }} />}
                                                            <div style={{ flex: 1, minWidth: 0 }}>
                                                                <div style={{ color: 'var(--text)', fontWeight: '600', fontSize: '14px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item.title}</div>
                                                                <StarRating rating={item.ciakLogAverageRating ?? item.averageRating} />
                                                            </div>
                                                        </div>
                                                    </Link>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                )})}
                        </div>
                    </section>

                    {/* ── COSA DICE LA COMMUNITY (TRENDING) ── */}
                    <section style={{ padding: '0 64px 48px' }}>
                        <div style={{ marginBottom: '28px' }}>
                            <h2 style={{ fontSize: '22px', fontWeight: '700', marginBottom: '6px' }}>🔥 Cosa dice la community</h2>
                            {/* Fix: quando il trending vero è vuoto (poche recensioni questa
                        settimana) il fallback ai titoli popolari appariva silenzioso,
                        senza spiegazione — sembrava un errore invece di una scelta */}
                            <p style={{ color: 'var(--text-dark)', fontSize: '14px' }}>
                                {trending.length > 0
                                    ? 'I più discussi questa settimana, con le opinioni dei nostri utenti'
                                    : 'Ancora poche recensioni questa settimana — nel frattempo, ecco i titoli più popolari'}
                            </p>
                        </div>
                        <TrendingCarousel trending={trending.length > 0 ? trending : recentFilms} trendingReviews={trendingReviews} />
                    </section>

                    {/* ── LEADERBOARD UTENTI ── */}
                    <section style={{ padding: '0 64px 64px' }}>
                        <h2 style={{ fontSize: '22px', fontWeight: '700', marginBottom: '28px' }}>👥 Critici più attivi</h2>

                        {/* Fix (Homepage — pareggi nel podio): prima il podio prendeva sempre
                    esattamente 3 SLOT fissi per indice (topUsers[0],[1],[2]) — con un
                    pareggio tra 3° e 4° posto (es. punteggi 300-200-120-120), il quarto
                    utente aveva il rank corretto ("3") ma finiva comunque nella lista
                    sotto, come se non facesse parte del podio. Ora si raggruppa per
                    rank: ogni colonna del podio può contenere più utenti a pari merito. */}
                        {(() => {
                            const byRank = {}
                            topUsers.forEach(u => {
                                const r = u.rank ?? 0
                                if (!byRank[r]) byRank[r] = []
                                byRank[r].push(u)
                            })
                            const groups = Object.keys(byRank).map(Number).sort((a, b) => a - b).slice(0, 3)
                                .map(r => ({ rank: r, users: byRank[r] }))
                            if (groups.length === 0) return null
                            const visualOrder = groups.length === 3 ? [1, 0, 2] : groups.map((_, i) => i)

                            return (
                                <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'center', gap: '8px', marginBottom: '24px' }}>
                                    {visualOrder.map(gi => {
                                        const group = groups[gi]
                                        const { rank, users } = group
                                        const height = rankHeights[rank] ?? 44
                                        const avatarSize = (rankSizes[rank] ?? 52) - (users.length > 1 ? Math.min(16, (users.length - 1) * 6) : 0)
                                        const isChampion = rank === 1

                                        return (
                                            <div key={rank} style={{ textAlign: 'center', width: isChampion ? '160px' : '130px' }}>
                                                {isChampion
                                                    ? <div style={{ fontSize: '28px', marginBottom: '6px' }}>👑</div>
                                                    : <div style={{ height: '34px', marginBottom: '6px' }} />
                                                }

                                                {/* Più utenti a pari merito: un piccolo cluster di avatar affiancati,
                                        stesso rank badge condiviso, stessa colonna del podio sotto */}
                                                <div style={{ display: 'flex', justifyContent: 'center', gap: '4px', flexWrap: 'wrap', marginBottom: '8px' }}>
                                                    {users.map((u, ui) => {
                                                        const isMe = logged && user?.username === u.username
                                                        return (
                                                            <Link to={`/profile/${u.username}`} key={u.username} title={u.username}>
                                                                <div style={{ position: 'relative', display: 'inline-block' }}>
                                                                    <UserAvatar username={u.username} size={avatarSize} index={ui} />
                                                                    {!isChampion && (
                                                                        <div style={{
                                                                            position: 'absolute', bottom: -4, right: -4,
                                                                            background: rank === 2 ? 'linear-gradient(135deg,#c0c0c0,#9ca3af)' : 'linear-gradient(135deg,#cd7c2c,#a0522d)',
                                                                            width: '18px', height: '18px', borderRadius: '50%',
                                                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                                                            fontSize: '9px', fontWeight: '800', color: 'var(--text)', border: '2px solid var(--bg)',
                                                                        }}>{rank}</div>
                                                                    )}
                                                                    {isMe && <div style={{ position: 'absolute', top: -6, right: -2, fontSize: '13px' }}>⭐</div>}
                                                                </div>
                                                            </Link>
                                                        )
                                                    })}
                                                </div>

                                                <div style={{ color: 'var(--text)', fontWeight: isChampion ? '800' : '700', fontSize: isChampion ? '14px' : '12px', marginBottom: '2px', lineHeight: 1.4 }}>
                                                    {users.map(u => u.username).join(' · ')}
                                                </div>
                                                <div style={{ color: 'var(--text-muted)', fontSize: '11px', marginBottom: '10px' }}>
                                                    🏅 {users[0].score ?? users[0].reviewCount} punti
                                                </div>

                                                <div style={{
                                                    height: height,
                                                    background: isChampion ? 'linear-gradient(to bottom, #e5091422, var(--bg-hover))' : 'var(--bg-hover)',
                                                    border: isChampion ? '1px solid #e5091433' : '1px solid var(--border-soft)',
                                                    borderBottom: 'none', borderRadius: '6px 6px 0 0',
                                                }} />
                                            </div>
                                        )
                                    })}
                                </div>
                            )
                        })()}

                        {/* Lista dal 4° in poi.
                    Fix: usa u.rank invece di un indice sequenziale, per rispettare i
                    pareggi. Fix: filtra per rank > 3 invece di tagliare l'array
                    all'indice 3, per non duplicare chi è già nel podio a pari merito.
                    Fix (segnalato dopo il podio): a parità di rank qui sotto restavano
                    comunque righe separate — stesso raggruppamento già fatto nel podio,
                    applicato anche qui: una riga per rank, più utenti dentro se in pareggio. */}
                        {(() => {
                            const restByRank = {}
                            topUsers.filter(u => (u.rank ?? 0) > 3).forEach(u => {
                                const r = u.rank
                                if (!restByRank[r]) restByRank[r] = []
                                restByRank[r].push(u)
                            })
                            const restGroups = Object.keys(restByRank).map(Number).sort((a, b) => a - b)
                                .map(r => ({ rank: r, users: restByRank[r] }))
                            if (restGroups.length === 0) return null
                            return (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                    {restGroups.map(({ rank, users }) => {
                                        const anyIsMe = logged && users.some(u => u.username === user?.username)
                                        return (
                                            <div key={rank} style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '12px 16px', backgroundColor: anyIsMe ? '#1a0f0f' : 'var(--bg-nav)', border: `1px solid ${anyIsMe ? 'var(--accent)' : 'var(--bg-hover)'}`, borderRadius: '10px' }}>
                                                <span style={{ color: 'var(--text-dark)', fontSize: '14px', fontWeight: '700', minWidth: '28px' }}>#{rank}</span>
                                                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap', flex: 1 }}>
                                                    {users.map((u, ui) => (
                                                        <Link to={`/profile/${u.username}`} key={u.username} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                                            <UserAvatar username={u.username} size={30} index={ui} />
                                                            <span style={{ color: 'var(--text)', fontWeight: '600' }}>{u.username}</span>
                                                            {logged && user?.username === u.username && <span style={{ color: 'var(--gold)', fontSize: '12px', fontWeight: '600' }}>Tu ⭐</span>}
                                                        </Link>
                                                    ))}
                                                </div>
                                                <span style={{ color: 'var(--text-muted)', fontSize: '13px' }}>🏅 {users[0].score ?? users[0].reviewCount} punti</span>
                                            </div>
                                        )
                                    })}
                                </div>
                            )
                        })()}

                        {topUsers.length === 0 && (
                            <div style={{ color: 'var(--text-dark)', textAlign: 'center', padding: '40px' }}>Scrivi recensioni per entrare in classifica! 🏅</div>
                        )}
                    </section>
                </>
            )}

            {/* ── CTA GUEST ── */}
            {!logged && (
                <section style={{ margin: '0 64px 64px', padding: '48px', background: 'var(--gradient-cta)', borderRadius: '16px', border: '1px solid #2a1a1a', textAlign: 'center' }}>
                    <div style={{ fontSize: '40px', marginBottom: '16px' }}>🎬</div>
                    <h2 style={{ fontSize: '28px', fontWeight: '800', marginBottom: '12px' }}>Unisciti alla community</h2>
                    <p style={{ color: 'var(--text-muted)', fontSize: '16px', marginBottom: '28px' }}>Traccia i tuoi film, scrivi recensioni, chiedi consigli alla tua AI personale.</p>
                    <div style={{ display: 'flex', gap: '16px', justifyContent: 'center' }}>
                        <Link to="/register"><button style={{ padding: '14px 40px', backgroundColor: 'var(--accent)', border: 'none', borderRadius: '8px', color: 'var(--text)', fontSize: '16px', fontWeight: '700' }}>Registrati gratis</button></Link>
                        <Link to="/login"><button style={{ padding: '14px 32px', backgroundColor: 'transparent', border: '1px solid var(--border-soft)', borderRadius: '8px', color: 'var(--text)', fontSize: '16px' }}>Ho già un account</button></Link>
                    </div>
                </section>
            )}

            {/* ── FOOTER ── */}
            <footer style={{ borderTop: '1px solid var(--border-soft)', padding: '28px 64px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
                <div>
                    <div style={{ fontSize: '20px', fontWeight: '700', marginBottom: '4px' }}>Ciak<span style={{ color: 'var(--accent)' }}>Log</span></div>
                    <div style={{ color: 'var(--text-dark)', fontSize: '13px' }}>Traccia • Recensisci • Scopri</div>
                </div>

                <div style={{ color: 'var(--text-dark)', fontSize: '13px', textAlign: 'center' }}>
                    <div>Progetto di <span style={{ color: 'var(--text)', fontWeight: '600' }}>Walid Dalal</span></div>
                    <div style={{ marginTop: '2px' }}>© 2026 CiakLog</div>
                </div>


                <a
                    href="https://github.com/WalidDalal/ciaklog"
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-dark)', fontSize: '13px', textDecoration: 'none', padding: '8px 16px', border: '1px solid var(--border-soft)', borderRadius: '8px', transition: 'border-color 0.15s, color 0.15s' }}
                    onMouseEnter={e => { e.currentTarget.style.borderColor = 'var(--accent)'; e.currentTarget.style.color = 'var(--text)' }}
                    onMouseLeave={e => { e.currentTarget.style.borderColor = 'var(--border-soft)'; e.currentTarget.style.color = 'var(--text-dark)' }}
                >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12 0C5.37 0 0 5.37 0 12c0 5.3 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61-.546-1.385-1.335-1.755-1.335-1.755-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.605-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 21.795 24 17.295 24 12c0-6.63-5.37-12-12-12"/>
                    </svg>
                    GitHub
                </a>
            </footer>


        </div>
    )
}

export default HomePage