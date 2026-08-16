import { Link, useNavigate } from 'react-router-dom'
import { useState, useEffect } from 'react'
import useAuthStore from '../store/authStore'
import useThemeStore from '../store/themeStore'
import { useLocation } from 'react-router-dom'
import api from '../services/api'


// Floating Chat AI button — esportato separatamente per essere usato in App.jsx

function Navbar() {
  const [search, setSearch] = useState('')
  const [showDropdown, setShowDropdown] = useState(false)
  const navigate = useNavigate()
  const { user, token, logout } = useAuthStore()
  const { theme, toggle: toggleTheme } = useThemeStore()
  const logged = !!token

  // Fix (Home — "Consiglio del giorno" ridisegnato, v2): niente più popover
  // ("nuvoletta") staccato dal resto — ispirato a "Mi sento fortunato" di
  // Google, ma adattato: qui abbiamo 3 consigli non 1, quindi l'icona ✨
  // dentro la barra di ricerca porta DIRETTAMENTE a uno di essi (un click,
  // una destinazione), e cliccandola di nuovo scorre al successivo dei 3
  // invece di ripetere sempre lo stesso.
  const [dailyPick, setDailyPick] = useState(null)
  const [dailyPickIndex, setDailyPickIndex] = useState(0)

  useEffect(() => {
    if (!token || user?.role === 'ADMIN') { setDailyPick(null); return }
    const controller = new AbortController()
    api.get('/ai/daily', { signal: controller.signal })
        .then(r => setDailyPick(r.data))
        .catch(() => {})
    return () => controller.abort()
  }, [token, user?.role])

  const goToDailyPick = () => {
    if (!dailyPick?.suggestions?.length) return
    const item = dailyPick.suggestions[dailyPickIndex % dailyPick.suggestions.length]
    setDailyPickIndex(i => i + 1)
    navigate(`/movie/${item.tmdbId}?type=${item.contentType}`)
  }

  const handleSearch = (e) => {
    // Fix (🟡 minimo 2 caratteri non coerente): allineato allo stesso
    // controllo di SearchPage.jsx — prima qui bastava "non vuoto".
    if (e.key === 'Enter' && search.trim().length >= 2) {
      navigate(`/search?q=${encodeURIComponent(search.trim())}`)
      setSearch('')
    }
  }

  // Estratta per essere riusata sia dal tasto Invio (sopra) sia dal click
  // sulla lente (ora spostata a destra, cliccabile — vedi sotto)
  const submitSearch = () => {
    if (search.trim().length >= 2) {
      navigate(`/search?q=${encodeURIComponent(search.trim())}`)
      setSearch('')
    }
  }

  const handleLogout = () => {
    logout()
    setShowDropdown(false)
    navigate('/')
  }

  // Chiudi dropdown cliccando fuori
  const handleOverlayClick = () => setShowDropdown(false)

  return (
    <>
      {showDropdown && (
        <div
          onClick={handleOverlayClick}
          style={{ position: 'fixed', inset: 0, zIndex: 99 }}
        />
      )}

      <nav style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 32px',
        height: '64px',
        backgroundColor: 'var(--bg-nav)',
        borderBottom: '1px solid var(--border)',
        position: 'sticky',
        top: 0,
        zIndex: 100,
      }}>
        {/* Logo */}
        <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '8px', flexShrink: 0 }}>
          <span style={{ fontSize: '22px' }}>🎬</span>
          <span style={{ fontSize: '20px', fontWeight: '700', color: 'var(--text)' }}>
            Ciak<span style={{ color: 'var(--accent)' }}>Log</span>
          </span>
        </Link>

        {/* Fix: barra ricerca + pulsante AI raggruppati in un unico
            contenitore flex — così il nav (che usa justify-content:
            space-between tra i suoi figli diretti) li tratta come UN blocco
            solo, e il pulsante resta sempre attaccato alla barra invece di
            allontanarsi quando c'è spazio extra a disposizione */}
        <div style={{ flex: 1, maxWidth: '540px', margin: '0 32px', display: 'flex', alignItems: 'center', gap: '10px' }}>
          {/* Barra ricerca */}
          <div style={{ flex: 1, position: 'relative' }}>
            <input
              type="text"
              placeholder="Cerca un film o una serie..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              onKeyDown={handleSearch}
              style={{
                width: '100%',
                padding: '10px 44px 10px 16px',
                backgroundColor: 'var(--bg-hover)',
                border: '1px solid var(--border-soft)',
                borderRadius: '24px',
                color: 'var(--text)',
                fontSize: '14px',
                outline: 'none',
                boxSizing: 'border-box',
              }}
            />
            {/* Fix: lente spostata a destra e resa cliccabile (equivale a
                Invio) — è la posizione/comportamento che ci si aspetta di
                solito per il tasto di ricerca, prima era solo decorativa a
                sinistra */}
            <button onClick={submitSearch} title="Cerca" aria-label="Cerca"
                    style={{ position: 'absolute', right: '6px', top: '50%', transform: 'translateY(-50%)', width: '30px', height: '30px', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'transparent', border: 'none', borderRadius: '50%', cursor: 'pointer', color: 'var(--text-dark)', fontSize: '14px' }}
                    onMouseEnter={e => e.currentTarget.style.backgroundColor = 'var(--accent-subtle)'}
                    onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}>
              🔍
            </button>
          </div>

          {/* Fix: pulsante "consiglio del giorno" — fuori dalla pillola di
              ricerca (per non confondersi con l'icona di invio, che è la
              lente a destra), ma dentro lo stesso contenitore così resta
              vicino invece di allontanarsi. Un click porta direttamente a
              uno dei 3 consigli del giorno, scorrendo tra i tre a ogni
              click successivo. */}
          {dailyPick?.suggestions?.length > 0 && (
              <button onClick={goToDailyPick} title="Fatti consigliare dall'AI" aria-label="Fatti consigliare dall'AI"
                      style={{ flexShrink: 0, width: '36px', height: '36px', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'var(--bg-hover)', border: '1px solid var(--border-soft)', borderRadius: '50%', cursor: 'pointer', fontSize: '16px' }}
                      onMouseEnter={e => { e.currentTarget.style.backgroundColor = 'var(--accent-subtle)'; e.currentTarget.style.borderColor = 'var(--border-cta)' }}
                      onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'var(--bg-hover)'; e.currentTarget.style.borderColor = 'var(--border-soft)' }}>
                ✨
              </button>
          )}
        </div>

        {/* Destra */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexShrink: 0 }}>
          {/* Toggle dark/light */}
          <button
            onClick={toggleTheme}
            aria-label="Cambia tema"
            title={theme === 'dark' ? 'Passa alla modalità chiara' : 'Passa alla modalità scura'}
            style={{
              backgroundColor: 'transparent', border: '1px solid var(--border)',
              borderRadius: '8px', padding: '6px 10px',
              color: 'var(--text-muted)', fontSize: '16px', cursor: 'pointer',
              display: 'flex', alignItems: 'center',
            }}
          >
            {theme === 'dark' ? '☀️' : '🌙'}
          </button>

          {logged ? (
            <>
              {/* Libreria — nascosta per admin */}
              {user?.role !== 'ADMIN' && (
                <Link to="/library">
                  <button style={{ padding: '8px 14px', backgroundColor: 'var(--bg-hover)', border: '1px solid var(--border-soft)', borderRadius: '6px', color: 'var(--text)', fontSize: '14px', cursor: 'pointer', whiteSpace: 'nowrap' }}>
                    📚 Libreria
                  </button>
                </Link>
              )}

              {/* Admin — visibile direttamente in navbar, non nel dropdown */}
              {user?.role === 'ADMIN' && (
                <Link to="/admin">
                  <button style={{ padding: '8px 14px', backgroundColor: 'var(--accent-subtle)', border: '1px solid #e5091466', borderRadius: '6px', color: 'var(--accent)', fontSize: '14px', fontWeight: '600', cursor: 'pointer', whiteSpace: 'nowrap' }}>
                    🛡️ Dashboard
                  </button>
                </Link>
              )}

              {/* Dropdown profilo */}
              <div style={{ position: 'relative', zIndex: 100 }}>
                <button
                  onClick={() => setShowDropdown(v => !v)}
                  style={{
                    display: 'flex', alignItems: 'center', gap: '6px',
                    padding: '6px 12px', backgroundColor: showDropdown ? 'var(--border)' : 'var(--bg-hover)',
                    border: `1px solid ${showDropdown ? 'var(--accent)' : 'var(--border-soft)'}`,
                    borderRadius: '6px', color: 'var(--text)', fontSize: '14px', fontWeight: '500',
                    cursor: 'pointer', whiteSpace: 'nowrap',
                  }}
                >
                  👤 {user?.username} ▾
                </button>

                {showDropdown && (
                  <div style={{
                    position: 'absolute', right: 0, top: '44px',
                    backgroundColor: 'var(--bg-card)', border: '1px solid var(--border-soft)',
                    borderRadius: '10px', minWidth: '180px', overflow: 'hidden',
                    boxShadow: 'var(--shadow-lg)',
                    zIndex: 200,
                  }}>
                    {/* Fix (Home Admin): il profilo pubblico (recensioni, wrapped, ecc.)
                        non ha senso per l'Admin, escluso da classifica e community — dava
                        anche contrasto/wrap nel dropdown mostrando "recensioni 0". Resta
                        solo Impostazioni, da cui può comunque modificare username e bio */}
                    {user?.role !== 'ADMIN' && (
                      <Link to={`/profile/${user?.username}`} onClick={() => setShowDropdown(false)}>
                        <div style={{ padding: '12px 16px', color: 'var(--text)', fontSize: '14px', borderBottom: '1px solid var(--border)', cursor: 'pointer' }}
                          onMouseEnter={e => e.currentTarget.style.backgroundColor = 'var(--bg-hover)'}
                          onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}
                        >
                          👤 Il mio profilo
                        </div>
                      </Link>
                    )}
                    <Link to="/settings" onClick={() => setShowDropdown(false)}>
                      <div style={{ padding: '12px 16px', color: 'var(--text)', fontSize: '14px', borderBottom: '1px solid var(--border)', cursor: 'pointer' }}
                        onMouseEnter={e => e.currentTarget.style.backgroundColor = 'var(--bg-hover)'}
                        onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}
                      >
                        ⚙️ Impostazioni
                      </div>
                    </Link>
                    <div
                      onClick={handleLogout}
                      style={{ padding: '12px 16px', color: 'var(--text-muted)', fontSize: '14px', cursor: 'pointer' }}
                      onMouseEnter={e => e.currentTarget.style.backgroundColor = 'var(--bg-hover)'}
                      onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}
                    >
                      🚪 Esci
                    </div>
                  </div>
                )}
              </div>
            </>
          ) : (
            <>
              <Link to="/login">
                <button style={{ padding: '8px 20px', backgroundColor: 'transparent', border: '1px solid var(--border-soft)', borderRadius: '6px', color: 'var(--text)', fontSize: '14px', fontWeight: '500', cursor: 'pointer' }}>
                  Accedi
                </button>
              </Link>
              <Link to="/register">
                <button style={{ padding: '8px 20px', backgroundColor: 'var(--accent)', border: 'none', borderRadius: '6px', color: 'var(--text)', fontSize: '14px', fontWeight: '500', cursor: 'pointer' }}>
                  Registrati
                </button>
              </Link>
            </>
          )}
        </div>
      </nav>
    </>
  )
}

export default Navbar