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
    const visible = reviews.filter(r => r.text && r.text.trim().length > 20).slice(0, 2)
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
                        <StarRating rating={item.ciakLogAverageRating} />
                        <div style={{ color: 'var(--text-dark)', fontSize: '12px', marginTop: '6px' }}>
                            💬 {item.weeklyReviewCount} {item.weeklyReviewCount === 1 ? 'recensione' : 'recensioni'} questa settimana
                        </div>
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
                            border: '1px solid #333', color: page === 0 ? '#444' : 'var(--text)',
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
                            border: '1px solid #333', color: page === totalPages - 1 ? '#444' : 'var(--text)',
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
    // Fix (Home Admin): conteggio segnalazioni in attesa, mostrato nell'hero
    // al posto del badge "sei #X in classifica" che per l'Admin non ha senso
    const [pendingReportsCount, setPendingReportsCount] = useState(0)
    // Fix (Home Admin): sezioni community collassate di default per l'Admin —
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
            validTrending.forEach(item => {
                api.get(`/reviews/media/${item.contentType}/${item.tmdbId}`, { params: { size: 5, sort: 'createdAt,desc' }, signal })
                    .then(res => {
                        const reviews = res.data.content || res.data
                        setTrendingReviews(prev => ({ ...prev, [`${item.tmdbId}_${item.contentType}`]: reviews }))
                    })
                    .catch(() => {})
            })
        }).catch(() => {})

        if (token) {
            api.get('/library', { params: { status: 'WATCHING' }, signal })
                .then(r => setWatching(r.data.content || r.data))
                .catch(() => {})
            api.get('/library', { params: { status: 'TO_WATCH' }, signal })
                .then(r => setToWatch(r.data.content || r.data))
                .catch(() => {})
        }

        // Fix (Home Admin): conteggio segnalazioni PENDING per la card operativa
        // nell'hero — solo per Admin, size:1 perché ci serve solo totalElements
        if (token && user?.role === 'ADMIN') {
            api.get('/reports', { params: { status: 'PENDING', size: 1 }, signal })
                .then(r => setPendingReportsCount(r.data?.totalElements ?? 0))
                .catch(() => {})
        }

        return () => controller.abort()
    }, [token])

    // Fix (Home Admin): sezioni community collassate di default per l'Admin
    const isAdmin = user?.role === 'ADMIN'

    // Fix podio: myRank corretto (findIndex restituisce -1 se non trovato, +1 lo rende 0)
    const myRankIndex = topUsers.findIndex(u => u.username === user?.username)
    const myRank = myRankIndex >= 0 ? myRankIndex + 1 : 0

    // Fix podio: ordine visivo corretto — 2° sinistra, 1° centro, 3° destra
    const podiumOrder = [1, 0, 2] // indici in topUsers
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
                            <div style={{ fontSize: '13px', color: 'var(--accent)', fontWeight: '700', marginBottom: '12px', letterSpacing: '1px', textTransform: 'uppercase' }}>{user?.role === 'ADMIN' ? '👮 Pannello Admin' : 'Bentornato'}</div>
                            <h1 style={{ fontSize: '52px', fontWeight: '800', lineHeight: 1.05, marginBottom: '16px' }}>
                                {user?.role === 'ADMIN' ? <>Ciao, <span style={{ color: 'var(--accent)' }}>{user?.username}</span> 🛡️</> : <>Ciao, <span style={{ color: 'var(--accent)' }}>{user?.username}</span> 👋</>}
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
                                <Link to="/admin">
                                    <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', backgroundColor: pendingReportsCount > 0 ? 'rgba(239,68,68,0.12)' : 'var(--accent-subtle)', border: `1px solid ${pendingReportsCount > 0 ? '#ef4444' : 'var(--border-cta)'}`, borderRadius: '8px', padding: '8px 14px', marginBottom: '24px', cursor: 'pointer' }}>
                                        <span>🚩</span>
                                        <span style={{ color: pendingReportsCount > 0 ? '#ef4444' : 'var(--text-muted)', fontSize: '14px', fontWeight: '600' }}>
                                            {pendingReportsCount > 0
                                                ? `${pendingReportsCount} segnalazion${pendingReportsCount === 1 ? 'e' : 'i'} in attesa`
                                                : 'Nessuna segnalazione in attesa'}
                                        </span>
                                    </div>
                                </Link>
                            )}
                            {user?.role === 'ADMIN' ? (
                                <Link to="/admin">
                                    <button style={{ padding: '13px 32px', backgroundColor: 'var(--accent)', border: 'none', borderRadius: '8px', color: 'var(--text)', fontSize: '15px', fontWeight: '700' }}>
                                        🛡️ Vai alla dashboard Admin
                                    </button>
                                </Link>
                            ) : (
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
                                            <div style={{ width: '120px', backgroundColor: 'var(--bg-card)', borderRadius: '8px', overflow: 'hidden', border: '1px solid #222' }}
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
                                                    {item.currentSeason && <div style={{ color: 'var(--text-dark)', fontSize: '10px' }}>S{item.currentSeason}</div>}
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
                                            <div style={{ width: '120px', backgroundColor: 'var(--bg-card)', borderRadius: '8px', overflow: 'hidden', border: '1px solid #222' }}
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
                    {[{ label: '🎬 Top Film', data: topFilms, type: 'MOVIE' }, { label: '📺 Top Serie TV', data: topSeries, type: 'TV' }].map(({ label, data, type }) => (
                        <div key={type} style={{ backgroundColor: 'var(--bg-nav)', border: '1px solid var(--border)', borderRadius: '12px', padding: '20px', boxShadow: 'var(--shadow)' }}>
                            <div style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '1.5px', marginBottom: '16px' }}>{label}</div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                                {data.length === 0
                                    ? <div style={{ color: 'var(--text-dark)', fontSize: '13px', padding: '12px 0' }}>Ancora poche recensioni — torna presto! 🎬</div>
                                    : data.slice(0, 3).map((item, i) => (
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
                                    ))
                                }
                            </div>
                        </div>
                    ))}
                </div>
            </section>

            {/* ── COSA DICE LA COMMUNITY (TRENDING) ── */}
            <section style={{ padding: '0 64px 48px' }}>
                <div style={{ marginBottom: '28px' }}>
                    <h2 style={{ fontSize: '22px', fontWeight: '700', marginBottom: '6px' }}>🔥 Cosa dice la community</h2>
                    <p style={{ color: 'var(--text-dark)', fontSize: '14px' }}>I più discussi questa settimana, con le opinioni dei nostri utenti</p>
                </div>
                <TrendingCarousel trending={trending.length > 0 ? trending : recentFilms} trendingReviews={trendingReviews} />
            </section>

            {/* ── LEADERBOARD UTENTI ── */}
            <section style={{ padding: '0 64px 64px' }}>
                <h2 style={{ fontSize: '22px', fontWeight: '700', marginBottom: '28px' }}>👥 Critici più attivi</h2>

                {/* Fix podio: 2° a sinistra (livello medio), 1° al centro (più alto), 3° a destra (più basso) */}
                {topUsers.length >= 3 && (
                    <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'center', gap: '8px', marginBottom: '24px' }}>
                        {podiumOrder.map(idx => {
                            const u = topUsers[idx]
                            const rank = u?.rank ?? idx + 1
                            const height = rankHeights[rank] ?? 44
                            const avatarSize = rankSizes[rank] ?? 52
                            const isMe = logged && user?.username === u.username

                            return (
                                <Link to={`/profile/${u.username}`} key={u.username}>
                                    <div style={{ textAlign: 'center', width: idx === 0 ? '160px' : '130px' }}>
                                        {/* Corona solo per il 1° posto */}
                                        {rank === 1
                                            ? <div style={{ fontSize: '28px', marginBottom: '6px' }}>👑</div>
                                            : <div style={{ height: '34px', marginBottom: '6px' }} />
                                        }

                                        {/* Avatar + badge rank */}
                                        <div style={{ position: 'relative', display: 'inline-block', marginBottom: '8px' }}>
                                            <UserAvatar username={u.username} size={avatarSize} index={idx} />
                                            {rank !== 1 && (
                                                <div style={{
                                                    position: 'absolute', bottom: -4, right: -4,
                                                    background: rank === 2 ? 'linear-gradient(135deg,#c0c0c0,#9ca3af)' : 'linear-gradient(135deg,#cd7c2c,#a0522d)',
                                                    width: '20px', height: '20px', borderRadius: '50%',
                                                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                                                    fontSize: '10px', fontWeight: '800', color: 'var(--text)', border: '2px solid #0a0a0a',
                                                }}>{rank}</div>
                                            )}
                                        </div>

                                        <div style={{ color: 'var(--text)', fontWeight: rank === 1 ? '800' : '700', fontSize: rank === 1 ? '15px' : '13px', marginBottom: '2px' }}>
                                            {u.username}
                                            {/* Fix: stellina + testo chiaro se è l'utente loggato */}
                                            {isMe && <span style={{ marginLeft: '4px', fontSize: '14px' }}>⭐</span>}
                                        </div>
                                        {isMe && (
                                            <div style={{ color: 'var(--gold)', fontSize: '10px', marginBottom: '2px', fontWeight: '600' }}>Tu sei qui!</div>
                                        )}
                                        {/* Fix: mostra "punti" invece del numero di recensioni */}
                                        <div style={{ color: 'var(--text-muted)', fontSize: '11px', marginBottom: '10px' }}>
                                            🏅 {u.score ?? u.reviewCount} punti
                                        </div>

                                        {/* Colonna podio — Fix: altezze corrette */}
                                        <div style={{
                                            height: height,
                                            background: idx === 0 ? 'linear-gradient(to bottom, #e5091422, #1a1a1a)' : 'var(--bg-hover)',
                                            border: idx === 0 ? '1px solid #e5091433' : '1px solid #2a2a2a',
                                            borderBottom: 'none', borderRadius: '6px 6px 0 0',
                                        }} />
                                    </div>
                                </Link>
                            )
                        })}
                    </div>
                )}

                {/* Lista dal 4° in poi */}
                {topUsers.length > 3 && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        {topUsers.slice(3).map((u, i) => {
                            const isMe = logged && user?.username === u.username
                            return (
                                <Link to={`/profile/${u.username}`} key={u.username}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '12px 16px', backgroundColor: isMe ? '#1a0f0f' : 'var(--bg-nav)', border: `1px solid ${isMe ? 'var(--accent)' : 'var(--bg-hover)'}`, borderRadius: '10px' }}>
                                        <span style={{ color: 'var(--text-dark)', fontSize: '14px', fontWeight: '700', minWidth: '28px' }}>#{i + 4}</span>
                                        <UserAvatar username={u.username} size={34} index={i + 3} />
                                        <span style={{ color: 'var(--text)', fontWeight: '600', flex: 1 }}>{u.username}</span>
                                        {isMe && <span style={{ color: 'var(--gold)', fontSize: '12px', fontWeight: '600' }}>Tu ⭐</span>}
                                        <span style={{ color: 'var(--text-muted)', fontSize: '13px' }}>🏅 {u.score ?? u.reviewCount} punti</span>
                                    </div>
                                </Link>
                            )
                        })}
                    </div>
                )}

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
                        <Link to="/login"><button style={{ padding: '14px 32px', backgroundColor: 'transparent', border: '1px solid #444', borderRadius: '8px', color: 'var(--text)', fontSize: '16px' }}>Ho già un account</button></Link>
                    </div>
                </section>
            )}

            {/* ── FOOTER ── */}
            <footer style={{ borderTop: '1px solid #1a1a1a', padding: '28px 64px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
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
                    style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-dark)', fontSize: '13px', textDecoration: 'none', padding: '8px 16px', border: '1px solid #2a2a2a', borderRadius: '8px', transition: 'border-color 0.15s, color 0.15s' }}
                    onMouseEnter={e => { e.currentTarget.style.borderColor = 'var(--accent)'; e.currentTarget.style.color = 'var(--text)' }}
                    onMouseLeave={e => { e.currentTarget.style.borderColor = '#2a2a2a'; e.currentTarget.style.color = 'var(--text-dark)' }}
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