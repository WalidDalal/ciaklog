import { Link, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import useAuthStore from '../store/authStore'
import useThemeStore from '../store/themeStore'
import { useLocation } from 'react-router-dom'


// Floating Chat AI button — esportato separatamente per essere usato in App.jsx

function Navbar() {
  const [search, setSearch] = useState('')
  const [showDropdown, setShowDropdown] = useState(false)
  const navigate = useNavigate()
  const { user, token, logout } = useAuthStore()
  const { theme, toggle: toggleTheme } = useThemeStore()
  const logged = !!token

  const handleSearch = (e) => {
    if (e.key === 'Enter' && search.trim()) {
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
        borderBottom: '1px solid #222',
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

        {/* Barra ricerca */}
        <div style={{ flex: 1, maxWidth: '480px', margin: '0 32px', position: 'relative' }}>
          <span style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-dark)', pointerEvents: 'none' }}>🔍</span>
          <input
            type="text"
            placeholder="Cerca un film o una serie..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            onKeyDown={handleSearch}
            style={{
              width: '100%',
              padding: '10px 16px 10px 40px',
              backgroundColor: 'var(--bg-hover)',
              border: '1px solid #333',
              borderRadius: '24px',
              color: 'var(--text)',
              fontSize: '14px',
              outline: 'none',
              boxSizing: 'border-box',
            }}
          />
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
              {/* Libreria */}
              <Link to="/library">
                <button style={{ padding: '8px 14px', backgroundColor: 'var(--bg-hover)', border: '1px solid #333', borderRadius: '6px', color: 'var(--text)', fontSize: '14px', cursor: 'pointer', whiteSpace: 'nowrap' }}>
                  📚 Libreria
                </button>
              </Link>

              {/* Admin — visibile direttamente in navbar, non nel dropdown */}
              {user?.role === 'ADMIN' && (
                <Link to="/admin">
                  <button style={{ padding: '8px 14px', backgroundColor: 'var(--accent-subtle)', border: '1px solid #e5091466', borderRadius: '6px', color: 'var(--accent)', fontSize: '14px', fontWeight: '600', cursor: 'pointer', whiteSpace: 'nowrap' }}>
                    🛡️ Admin
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
                    backgroundColor: 'var(--bg-card)', border: '1px solid #333',
                    borderRadius: '10px', minWidth: '180px', overflow: 'hidden',
                    boxShadow: 'var(--shadow-lg)',
                    zIndex: 200,
                  }}>
                    <Link to={`/profile/${user?.username}`} onClick={() => setShowDropdown(false)}>
                      <div style={{ padding: '12px 16px', color: 'var(--text)', fontSize: '14px', borderBottom: '1px solid #222', cursor: 'pointer' }}
                        onMouseEnter={e => e.currentTarget.style.backgroundColor = 'var(--bg-hover)'}
                        onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}
                      >
                        👤 Il mio profilo
                      </div>
                    </Link>
                    <Link to="/settings" onClick={() => setShowDropdown(false)}>
                      <div style={{ padding: '12px 16px', color: 'var(--text)', fontSize: '14px', borderBottom: '1px solid #222', cursor: 'pointer' }}
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
                <button style={{ padding: '8px 20px', backgroundColor: 'transparent', border: '1px solid #555', borderRadius: '6px', color: 'var(--text)', fontSize: '14px', fontWeight: '500', cursor: 'pointer' }}>
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