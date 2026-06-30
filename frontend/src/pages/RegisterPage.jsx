import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import useAuthStore from '../store/authStore'
import api from '../services/api'

function RegisterPage() {
  const [posters, setPosters] = useState([])

  useEffect(() => {
    api.get('/charts/films').then(r => {
      const films = r.data || []
      const paths = films.slice(0, 6).map(f => f.posterPath).filter(Boolean)
      setPosters(paths)
    }).catch(() => {})
  }, [])

    const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { register } = useAuthStore()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (password !== confirm) {
      setError('Le password non coincidono')
      return
    }
    if (username.length < 3 || username.length > 30) {
      setError('Lo username deve essere tra 3 e 30 caratteri')
      return
    }
    if (password.length < 8) {
      setError('La password deve essere di almeno 8 caratteri')
      return
    }

    setLoading(true)
    try {
      await register(username, email, password)
      navigate('/')
    } catch (err) {
      setError(err.response?.data?.error || 'Errore durante la registrazione')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}>
      <Navbar />

      {/* Sfondo poster */}
      <div style={{ position: 'fixed', inset: 0, zIndex: 0, display: 'flex', flexWrap: 'wrap', opacity: 0.15, pointerEvents: 'none', marginTop: '64px' }}>
{posters.map((path, i) => (
          <img key={i} src={`https://image.tmdb.org/t/p/w300${path}`} alt="" style={{ width: '16.66%', objectFit: 'cover', height: '50vh' }} />
        ))}
      </div>

      {/* Form */}
      <div style={{ position: 'relative', zIndex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 'calc(100vh - 64px)', padding: '32px 16px' }}>
        <div style={{ backgroundColor: 'var(--bg-card)', border: '1px solid #222', borderRadius: '16px', padding: '40px', width: '100%', maxWidth: '420px' }}>
          <div style={{ textAlign: 'center', marginBottom: '28px' }}>
            <div style={{ fontSize: '40px', marginBottom: '12px' }}>🎬</div>
            <h1 style={{ fontSize: '24px', fontWeight: '700', color: 'var(--text)', marginBottom: '6px' }}>Crea il tuo account</h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '14px' }}>Inizia a tracciare i tuoi film preferiti</p>
          </div>

          {error && (
            <div style={{ backgroundColor: '#2d1a1a', border: '1px solid #e50914', borderRadius: '8px', padding: '12px', color: '#ff6b6b', fontSize: '14px', marginBottom: '20px', textAlign: 'center' }}>
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>Username</label>
              <input
                type="text"
                placeholder="Scegli un username"
                value={username}
                onChange={e => setUsername(e.target.value)}
                required
                style={{ width: '100%', padding: '12px 16px', backgroundColor: 'var(--bg-hover)', border: '1px solid #333', borderRadius: '8px', color: 'var(--text)', fontSize: '15px', boxSizing: 'border-box' }}
              />
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>Email</label>
              <input
                type="email"
                placeholder="la@tua.email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
                style={{ width: '100%', padding: '12px 16px', backgroundColor: 'var(--bg-hover)', border: '1px solid #333', borderRadius: '8px', color: 'var(--text)', fontSize: '15px', boxSizing: 'border-box' }}
              />
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>Password</label>
              <input
                type="password"
                placeholder="Min. 8 caratteri"
                value={password}
                onChange={e => setPassword(e.target.value)}
                required
                style={{ width: '100%', padding: '12px 16px', backgroundColor: 'var(--bg-hover)', border: '1px solid #333', borderRadius: '8px', color: 'var(--text)', fontSize: '15px', boxSizing: 'border-box' }}
              />
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>Conferma password</label>
              <input
                type="password"
                placeholder="Ripeti la password"
                value={confirm}
                onChange={e => setConfirm(e.target.value)}
                required
                style={{
                  width: '100%', padding: '12px 16px', backgroundColor: 'var(--bg-hover)',
                  border: `1px solid ${confirm && confirm !== password ? 'var(--accent)' : 'var(--border-soft)'}`,
                  borderRadius: '8px', color: 'var(--text)', fontSize: '15px', boxSizing: 'border-box'
                }}
              />
              {confirm && confirm !== password && (
                <p style={{ color: 'var(--accent)', fontSize: '12px', marginTop: '4px' }}>Le password non coincidono</p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              style={{ width: '100%', padding: '14px', backgroundColor: loading ? '#666' : 'var(--accent)', border: 'none', borderRadius: '8px', color: 'var(--text)', fontSize: '16px', fontWeight: '600' }}
            >
              {loading ? 'Registrazione in corso...' : 'Registrati'}
            </button>
          </form>

          <p style={{ textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px', marginTop: '20px' }}>
            Hai già un account?{' '}
            <Link to="/login" style={{ color: 'var(--accent)', fontWeight: '600' }}>Accedi</Link>
          </p>
        </div>
      </div>
    </div>
  )
}

export default RegisterPage
