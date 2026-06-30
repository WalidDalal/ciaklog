import { useState } from 'react'
import { useNavigate, Navigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../services/api'
import useAuthStore from '../store/authStore'
import useToastStore from '../store/toastStore'

const INPUT_STYLE = {
  width: '100%', padding: '12px 16px', boxSizing: 'border-box',
  backgroundColor: 'var(--bg-input)', border: '1px solid var(--border-soft)',
  borderRadius: '8px', color: 'var(--text)', fontSize: '15px', fontFamily: 'inherit',
}

const CARD_STYLE = {
  backgroundColor: 'var(--bg-card)', border: '1px solid var(--border)',
  borderRadius: '12px', padding: '28px', marginBottom: '20px',
}

function SettingsPage() {
  const { user, token, logout, updateToken } = useAuthStore()
  const navigate = useNavigate()
  const toast = useToastStore()

  const [username, setUsername] = useState(user?.username || '')
  const [bio, setBio] = useState(user?.bio || '')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const [loadingProfile, setLoadingProfile] = useState(false)
  const [loadingPassword, setLoadingPassword] = useState(false)

  if (!token) return <Navigate to="/login" replace />

  const handleProfileSubmit = async (e) => {
    e.preventDefault()
    setLoadingProfile(true)
    try {
      const res = await api.put('/users/me', { username, bio })
      if (res.data?.token) {
        updateToken(res.data.token)
        toast.show('Profilo aggiornato!', 'success')
      } else {
        toast.show('Profilo aggiornato! Effettua di nuovo il login.', 'success')
        setTimeout(() => { logout(); navigate('/login') }, 2000)
      }
    } catch (err) {
      toast.show(err.response?.data?.error || 'Errore durante il salvataggio')
    } finally {
      setLoadingProfile(false)
    }
  }

  const handlePasswordSubmit = async (e) => {
    e.preventDefault()
    if (newPassword !== confirmPassword) { toast.show('Le password non coincidono'); return }
    if (newPassword.length < 8) { toast.show('La password deve essere di almeno 8 caratteri'); return }
    setLoadingPassword(true)
    try {
      await api.put('/users/me', { currentPassword, newPassword })
      toast.show('Password aggiornata!', 'success')
      setCurrentPassword(''); setNewPassword(''); setConfirmPassword('')
    } catch (err) {
      toast.show(err.response?.data?.error || 'Errore durante il salvataggio')
    } finally {
      setLoadingPassword(false)
    }
  }

  const avatarLetter = (username || user?.username || '?')[0].toUpperCase()

  return (
    <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}>
      <Navbar />

      <div style={{ maxWidth: '640px', margin: '0 auto', padding: '40px 24px' }}>
        <h1 style={{ fontSize: '26px', fontWeight: '800', color: 'var(--text)', marginBottom: '32px' }}>
          ⚙️ Impostazioni
        </h1>

        {/* ── Avatar + anteprima ── */}
        <div style={{ ...CARD_STYLE, display: 'flex', alignItems: 'center', gap: '20px' }}>
          <div style={{
            width: '72px', height: '72px', borderRadius: '50%', flexShrink: 0,
            backgroundColor: 'var(--accent)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: '30px', fontWeight: '800', color: 'white',
          }}>
            {avatarLetter}
          </div>
          <div>
            <div style={{ color: 'var(--text)', fontWeight: '700', fontSize: '18px' }}>{username || user?.username}</div>
            <div style={{ color: 'var(--text-dark)', fontSize: '13px', marginTop: '2px' }}>
              {bio || <span style={{ fontStyle: 'italic' }}>Nessuna bio</span>}
            </div>
          </div>
        </div>

        {/* ── Modifica profilo ── */}
        <div style={CARD_STYLE}>
          <h2 style={{ color: 'var(--text)', fontSize: '17px', fontWeight: '700', marginBottom: '20px' }}>
            👤 Modifica profilo
          </h2>
          <form onSubmit={handleProfileSubmit}>
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>
                Username
              </label>
              <input
                type="text"
                value={username}
                onChange={e => setUsername(e.target.value)}
                required
                minLength={3}
                maxLength={30}
                style={INPUT_STYLE}
              />
            </div>
            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>
                Bio <span style={{ color: 'var(--text-dark)' }}>({bio.length}/200)</span>
              </label>
              <textarea
                value={bio}
                onChange={e => setBio(e.target.value)}
                maxLength={200}
                rows={3}
                placeholder="Scrivi qualcosa su di te..."
                style={{ ...INPUT_STYLE, resize: 'vertical' }}
              />
            </div>
            <button
              type="submit"
              disabled={loadingProfile}
              style={{
                padding: '12px 28px', backgroundColor: loadingProfile ? 'var(--border-soft)' : 'var(--accent)',
                border: 'none', borderRadius: '8px', color: 'white',
                fontSize: '15px', fontWeight: '600', cursor: loadingProfile ? 'default' : 'pointer',
              }}
            >
              {loadingProfile ? 'Salvataggio...' : 'Salva modifiche'}
            </button>
          </form>
        </div>

        {/* ── Cambia password ── */}
        <div style={CARD_STYLE}>
          <h2 style={{ color: 'var(--text)', fontSize: '17px', fontWeight: '700', marginBottom: '20px' }}>
            🔒 Cambia password
          </h2>
          <form onSubmit={handlePasswordSubmit}>
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>Password attuale</label>
              <input type="password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} required style={INPUT_STYLE} />
            </div>
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>Nuova password</label>
              <input type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)} required style={INPUT_STYLE} />
            </div>
            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>Conferma nuova password</label>
              <input
                type="password"
                value={confirmPassword}
                onChange={e => setConfirmPassword(e.target.value)}
                required
                style={{
                  ...INPUT_STYLE,
                  border: `1px solid ${confirmPassword && confirmPassword !== newPassword ? 'var(--accent)' : 'var(--border-soft)'}`,
                }}
              />
              {confirmPassword && confirmPassword !== newPassword && (
                <p style={{ color: 'var(--accent)', fontSize: '12px', marginTop: '4px' }}>Le password non coincidono</p>
              )}
            </div>
            <button
              type="submit"
              disabled={loadingPassword}
              style={{
                padding: '12px 28px', backgroundColor: loadingPassword ? 'var(--border-soft)' : 'var(--accent)',
                border: 'none', borderRadius: '8px', color: 'white',
                fontSize: '15px', fontWeight: '600', cursor: loadingPassword ? 'default' : 'pointer',
              }}
            >
              {loadingPassword ? 'Aggiornamento...' : 'Aggiorna password'}
            </button>
          </form>
        </div>

        {/* ── Zona pericolosa ── */}
        <div style={{ ...CARD_STYLE, borderColor: '#3a1a1a', marginBottom: 0 }}>
          <h2 style={{ color: 'var(--accent)', fontSize: '17px', fontWeight: '700', marginBottom: '8px' }}>
            🚨 Zona pericolosa
          </h2>
          <p style={{ color: 'var(--text-dark)', fontSize: '13px', marginBottom: '16px' }}>
            Il logout termina la sessione corrente.
          </p>
          <button
            onClick={() => { logout(); navigate('/login') }}
            style={{
              padding: '10px 24px', backgroundColor: 'transparent',
              border: '1px solid var(--accent)', borderRadius: '8px',
              color: 'var(--accent)', fontSize: '14px', fontWeight: '600', cursor: 'pointer',
            }}
          >
            Esci dall'account
          </button>
        </div>
      </div>
    </div>
  )
}

export default SettingsPage
