import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import useAuthStore from '../store/authStore'
import api from '../services/api'
import { useLocation } from 'react-router-dom'


function LoginPage() {
  const [posters, setPosters] = useState([])

  useEffect(() => {
    api.get('/charts/films').then(r => {
      const films = r.data || []
      const paths = films.slice(0, 6).map(f => f.posterPath).filter(Boolean)
      setPosters(paths)
    }).catch(() => {})
  }, [])

    const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const from = location.state?.from || '/'
  const { login } = useAuthStore()


  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(email, password)
      navigate(from)
    } catch (err) {
      setError(err.response?.data?.error || 'Credenziali non valide')
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
      <div style={{ position: 'relative', zIndex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 'calc(100vh - 64px)' }}>
        <div style={{ backgroundColor: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: '16px', padding: '40px', width: '100%', maxWidth: '420px' }}>
          <div style={{ textAlign: 'center', marginBottom: '28px' }}>
            <div style={{ fontSize: '40px', marginBottom: '12px' }}>🎬</div>
            <h1 style={{ fontSize: '24px', fontWeight: '700', color: 'var(--text)', marginBottom: '6px' }}>Bentornato su CiakLog</h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '14px' }}>Accedi al tuo diario cinematografico</p>
          </div>

          {error && (
            <div style={{ backgroundColor: 'var(--danger-bg)', border: '1px solid var(--danger-border)', borderRadius: '8px', padding: '12px', color: 'var(--danger-text)', fontSize: '14px', marginBottom: '20px', textAlign: 'center' }}>
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>Email</label>
              <input
                type="email"
                placeholder="la@tua.email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
                style={{ width: '100%', padding: '12px 16px', backgroundColor: 'var(--bg-hover)', border: '1px solid var(--border-soft)', borderRadius: '8px', color: 'var(--text)', fontSize: '15px', boxSizing: 'border-box' }}
              />
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>Password</label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder="••••••••••"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  required
                  style={{ width: '100%', padding: '12px 40px 12px 16px', backgroundColor: 'var(--bg-hover)', border: '1px solid var(--border-soft)', borderRadius: '8px', color: 'var(--text)', fontSize: '15px', boxSizing: 'border-box' }}
                />
                <button type="button" onClick={() => setShowPassword(v => !v)}
                  style={{ position: 'absolute', right: '12px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px', color: 'var(--text-dark)', padding: 0 }}>
                  {showPassword ? '🙈' : '👁️'}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              style={{ width: '100%', padding: '14px', backgroundColor: loading ? 'var(--border-soft)' : 'var(--accent)', border: 'none', borderRadius: '8px', color: 'var(--text)', fontSize: '16px', fontWeight: '600' }}
            >
              {loading ? 'Accesso in corso...' : 'Accedi'}
            </button>
          </form>

          <p style={{ textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px', marginTop: '20px' }}>
            Non hai un account?{' '}
            <Link to="/register" style={{ color: 'var(--accent)', fontWeight: '600' }}>Registrati</Link>
          </p>
        </div>
      </div>
    </div>
  )
}

export default LoginPage