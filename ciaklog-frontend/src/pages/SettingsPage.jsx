import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../services/api'
import useAuthStore from '../store/authStore'

function SettingsPage() {
  const { user, token, logout } = useAuthStore()
  const navigate = useNavigate()

  const [username, setUsername] = useState(user?.username || '')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const [usernameSuccess, setUsernameSuccess] = useState('')
  const [usernameError, setUsernameError] = useState('')
  const [passwordSuccess, setPasswordSuccess] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [loadingUsername, setLoadingUsername] = useState(false)
  const [loadingPassword, setLoadingPassword] = useState(false)

  if (!token) { navigate('/login'); return null }

  const handleUsernameSubmit = async (e) => {
    e.preventDefault()
    setUsernameError('')
    setUsernameSuccess('')
    setLoadingUsername(true)
    try {
      await api.put('/users/me', { username })
      setUsernameSuccess('Username aggiornato! Effettua di nuovo il login.')
      setTimeout(() => { logout(); navigate('/login') }, 2000)
    } catch (err) {
      setUsernameError(err.response?.data?.error || 'Errore durante il salvataggio')
    } finally {
      setLoadingUsername(false)
    }
  }

  const handlePasswordSubmit = async (e) => {
    e.preventDefault()
    setPasswordError('')
    setPasswordSuccess('')
    if (newPassword !== confirmPassword) {
      setPasswordError('Le password non coincidono')
      return
    }
    if (newPassword.length < 8) {
      setPasswordError('La password deve essere di almeno 8 caratteri')
      return
    }
    setLoadingPassword(true)
    try {
      await api.put('/users/me', { currentPassword, newPassword })
      setPasswordSuccess('Password aggiornata con successo!')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (err) {
      setPasswordError(err.response?.data?.error || 'Errore durante il salvataggio')
    } finally {
      setLoadingPassword(false)
    }
  }

  return (
    <div style={{ backgroundColor: '#0a0a0a', minHeight: '100vh' }}>
      <Navbar />

      <div style={{ maxWidth: '600px', margin: '0 auto', padding: '40px 24px' }}>
        <h1 style={{ fontSize: '28px', fontWeight: '800', color: 'white', marginBottom: '40px' }}>
          ⚙️ Impostazioni
        </h1>

        {/* Modifica username */}
        <div style={{ backgroundColor: '#141414', border: '1px solid #222', borderRadius: '12px', padding: '28px', marginBottom: '24px' }}>
          <h2 style={{ color: 'white', fontSize: '18px', fontWeight: '700', marginBottom: '20px' }}>
            👤 Modifica profilo
          </h2>
          <form onSubmit={handleUsernameSubmit}>
            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', color: '#9ca3af', fontSize: '13px', marginBottom: '6px' }}>Username</label>
              <input
                type="text"
                value={username}
                onChange={e => setUsername(e.target.value)}
                required
                style={{ width: '100%', padding: '12px 16px', backgroundColor: '#1a1a1a', border: '1px solid #333', borderRadius: '8px', color: 'white', fontSize: '15px', boxSizing: 'border-box' }}
              />
            </div>
            {usernameError && <p style={{ color: '#ff6b6b', fontSize: '13px', marginBottom: '12px' }}>{usernameError}</p>}
            {usernameSuccess && <p style={{ color: '#4ade80', fontSize: '13px', marginBottom: '12px' }}>{usernameSuccess}</p>}
            <button type="submit" disabled={loadingUsername} style={{ padding: '12px 28px', backgroundColor: loadingUsername ? '#666' : '#e50914', border: 'none', borderRadius: '8px', color: 'white', fontSize: '15px', fontWeight: '600' }}>
              {loadingUsername ? 'Salvataggio...' : 'Salva modifiche'}
            </button>
          </form>
        </div>

        {/* Cambia password */}
        <div style={{ backgroundColor: '#141414', border: '1px solid #222', borderRadius: '12px', padding: '28px' }}>
          <h2 style={{ color: 'white', fontSize: '18px', fontWeight: '700', marginBottom: '20px' }}>
            🔒 Cambia password
          </h2>
          <form onSubmit={handlePasswordSubmit}>
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', color: '#9ca3af', fontSize: '13px', marginBottom: '6px' }}>Password attuale</label>
              <input type="password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} required
                style={{ width: '100%', padding: '12px 16px', backgroundColor: '#1a1a1a', border: '1px solid #333', borderRadius: '8px', color: 'white', fontSize: '15px', boxSizing: 'border-box' }} />
            </div>
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', color: '#9ca3af', fontSize: '13px', marginBottom: '6px' }}>Nuova password</label>
              <input type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)} required
                style={{ width: '100%', padding: '12px 16px', backgroundColor: '#1a1a1a', border: '1px solid #333', borderRadius: '8px', color: 'white', fontSize: '15px', boxSizing: 'border-box' }} />
            </div>
            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', color: '#9ca3af', fontSize: '13px', marginBottom: '6px' }}>Conferma nuova password</label>
              <input type="password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} required
                style={{
                  width: '100%', padding: '12px 16px', backgroundColor: '#1a1a1a',
                  border: `1px solid ${confirmPassword && confirmPassword !== newPassword ? '#e50914' : '#333'}`,
                  borderRadius: '8px', color: 'white', fontSize: '15px', boxSizing: 'border-box'
                }} />
              {confirmPassword && confirmPassword !== newPassword && (
                <p style={{ color: '#e50914', fontSize: '12px', marginTop: '4px' }}>Le password non coincidono</p>
              )}
            </div>
            {passwordError && <p style={{ color: '#ff6b6b', fontSize: '13px', marginBottom: '12px' }}>{passwordError}</p>}
            {passwordSuccess && <p style={{ color: '#4ade80', fontSize: '13px', marginBottom: '12px' }}>{passwordSuccess}</p>}
            <button type="submit" disabled={loadingPassword} style={{ padding: '12px 28px', backgroundColor: loadingPassword ? '#666' : '#e50914', border: 'none', borderRadius: '8px', color: 'white', fontSize: '15px', fontWeight: '600' }}>
              {loadingPassword ? 'Aggiornamento...' : 'Aggiorna password'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}

export default SettingsPage