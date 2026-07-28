import { useState, useEffect } from 'react'
import { useNavigate, Navigate, Link } from 'react-router-dom'
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

// Fix (Impostazioni — colore profilo): stessa palette/logica di ProfilePage.jsx
// (colorForUsername), qui duplicata perché non condivisa in un modulo comune.
// Il colore va modificato da qui O da "Modifica profilo" in ProfilePage — stesso
// campo profileColor sul backend, cambia solo da dove lo apri.
const AVATAR_COLORS = ['var(--accent)', '#3b82f6', '#22c55e', '#f59e0b', '#8b5cf6', '#ec4899', '#14b8a6', '#eab308']
function colorForUsername(name) {
  if (!name) return AVATAR_COLORS[0]
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = (hash * 31 + name.charCodeAt(i)) >>> 0
  }
  return AVATAR_COLORS[hash % AVATAR_COLORS.length]
}

function SettingsPage() {
  const { user, token, logout, updateToken } = useAuthStore()
  const navigate = useNavigate()
  const toast = useToastStore()

  const [username, setUsername] = useState(user?.username || '')
  const [bio, setBio] = useState('')
  // Fix (Impostazioni — colore profilo): mancava del tutto qui, si poteva
  // cambiare solo dalla pagina Profilo pubblico ("Modifica profilo"). Stesso
  // campo (profileColor), stesso salvataggio via PUT /users/me.
  const [profileColor, setProfileColor] = useState('')

  // Tengo i valori originali per poterli
  // ripristinare se l'utente annulla senza salvare, invece di avere sempre
  // tutto editabile
  const [originalBio, setOriginalBio] = useState('')
  const [originalProfileColor, setOriginalProfileColor] = useState('')
  const [editingProfile, setEditingProfile] = useState(false)

  // Card riepilogo account, dati già
  // disponibili dalla stessa chiamata usata per la bio, nessun endpoint nuovo
  const [accountSummary, setAccountSummary] = useState(null)

  // Carica bio + riepilogo dal backend al mount (non sono nel JWT)
  useEffect(() => {
    if (user?.username) {
      api.get(`/users/${user.username}`).then(r => {
        const b = r.data?.bio || ''
        const c = r.data?.profileColor || ''
        setBio(b)
        setOriginalBio(b)
        setProfileColor(c)
        setOriginalProfileColor(c)
        setAccountSummary({
          memberSince: r.data?.memberSince,
          score: r.data?.score,
          totalReviews: r.data?.totalReviews,
        })
      }).catch(() => {})
    }
  }, [user?.username])
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  // La sezione password era l'unica sempre aperta,
  // diversa dalle altre card che seguono il pattern Modifica/Annulla —
  // ora è chiusa di default e si apre solo cliccando "Modifica"
  const [editingPassword, setEditingPassword] = useState(false)

  const [loadingProfile, setLoadingProfile] = useState(false)
  const [loadingPassword, setLoadingPassword] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [deletePassword, setDeletePassword] = useState('')
  const [deleteError, setDeleteError] = useState('')
  const [showCurrent, setShowCurrent] = useState(false)
  const [showNew, setShowNew] = useState(false)
  const [showConfirmPwd, setShowConfirmPwd] = useState(false)

  if (!token) return <Navigate to="/login" replace />

  const handleProfileSubmit = async (e) => {
    e.preventDefault()
    setLoadingProfile(true)
    try {
      // profileColor: '' significa esplicitamente "torna al colore
      // automatico" per il backend (vedi UpdateCredentialsRequest), quindi va
      // mandato così com'è — trasformarlo in null significherebbe "non
      // modificare", cioè ignorare la scelta "Automatico" dell'utente.
      const payload = { bio, profileColor }
      if (username !== user?.username) payload.username = username
      const res = await api.put('/users/me', payload)
      if (res.data?.token) {
        updateToken(res.data.token)
      }
      // Questo form non tocca mai la password, quindi un 204 (nessun
      // token) qui significa solo "è cambiata la bio" — non serve rilogin.
      // Prima si assumeva sempre "password cambiata" e si forzava il logout
      // anche per un semplice edit della bio.
      toast.show('Profilo aggiornato!', 'success')
      setOriginalBio(bio)
      setOriginalProfileColor(profileColor)
      setEditingProfile(false)
    } catch (err) {
      toast.show(err.response?.data?.error || 'Errore durante il salvataggio')
    } finally {
      setLoadingProfile(false)
    }
  }

  // Ripristina i valori originali senza
  // salvare nulla, e richiude il form
  const handleCancelProfile = () => {
    setUsername(user?.username || '')
    setBio(originalBio)
    setProfileColor(originalProfileColor)
    setEditingProfile(false)
  }

  const handlePasswordSubmit = async (e) => {
    e.preventDefault()
    if (newPassword !== confirmPassword) { toast.show('Le password non coincidono'); return }
    // Fix (disallineamento con la registrazione): qui si controllava solo la
    // lunghezza (8+ caratteri) — RegisterPage controlla anche maiuscola,
    // minuscola e numero (stesso pattern richiesto dal backend, vedi
    // UpdateCredentialsRequest.newPassword). Una password come "aaaaaaaa"
    // passava questo controllo lato client per poi essere rifiutata dal
    // backend con un errore generico, invece di essere segnalata subito qui.
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/.test(newPassword)) {
      toast.show('La password deve contenere almeno 8 caratteri, una maiuscola, una minuscola e un numero')
      return
    }
    setLoadingPassword(true)
    try {
      await api.put('/users/me', { currentPassword, newPassword })
      toast.show('Password aggiornata!', 'success')
      setCurrentPassword(''); setNewPassword(''); setConfirmPassword('')
      setEditingPassword(false)
    } catch (err) {
      toast.show(err.response?.data?.error || 'Errore durante il salvataggio')
    } finally {
      setLoadingPassword(false)
    }
  }

  const handleCancelPassword = () => {
    setCurrentPassword(''); setNewPassword(''); setConfirmPassword('')
    setEditingPassword(false)
  }

  const handleDeleteAccount = async () => {
    // Richiede la password come conferma finale, non solo il modal "sei sicuro?"
    if (!deletePassword) { setDeleteError('Inserisci la password per confermare'); return }
    setDeleteError('')
    try {
      await api.delete('/users/me', { data: { password: deletePassword } })
      logout()
      navigate('/')
    } catch (err) {
      setDeleteError(err.response?.data?.error || 'Errore durante l\'eliminazione')
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
            backgroundColor: profileColor || colorForUsername(username || user?.username),
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

        {/* Fix (Impostazioni — pagina povera): card riepilogo account,
            dati già disponibili (membro dal, punteggio, recensioni scritte) */}
        {accountSummary && (
          <div style={CARD_STYLE}>
            <div style={{ display: 'flex', justifyContent: 'space-around', textAlign: 'center', padding: '20px 0' }}>
              <div>
                <div style={{ color: 'var(--gold)', fontSize: '20px', fontWeight: '800' }}>{accountSummary.score ?? 0}</div>
                <div style={{ color: 'var(--text-dark)', fontSize: '12px', marginTop: '2px' }}>Punteggio</div>
              </div>
              <div>
                <div style={{ color: 'var(--gold)', fontSize: '20px', fontWeight: '800' }}>{accountSummary.totalReviews ?? 0}</div>
                <div style={{ color: 'var(--text-dark)', fontSize: '12px', marginTop: '2px' }}>Recensioni</div>
              </div>
              <div>
                <div style={{ color: 'var(--text)', fontSize: '14px', fontWeight: '700', marginTop: '3px' }}>{accountSummary.memberSince || '—'}</div>
                <div style={{ color: 'var(--text-dark)', fontSize: '12px', marginTop: '2px' }}>Membro dal</div>
              </div>
            </div>
            {/* Fix (Impostazioni, idea facoltativa a costo zero): riusa la pagina
                profilo già esistente, dà un motivo in più per passare da qui */}
            <div style={{ textAlign: 'center', borderTop: '1px solid var(--border-soft)', paddingTop: '14px' }}>
              <Link to={`/profile/${user?.username}`} style={{ color: 'var(--accent)', fontSize: '13px', fontWeight: '600' }}>
                🔗 Vedi il tuo profilo pubblico
              </Link>
            </div>
          </div>
        )}

        {/* ── Modifica profilo ── */}
        <div style={CARD_STYLE}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: editingProfile ? '20px' : 0 }}>
            <h2 style={{ color: 'var(--text)', fontSize: '17px', fontWeight: '700', margin: 0 }}>
              👤 Modifica profilo
            </h2>
            {/* Fix: pattern Modifica/Annulla — i campi restano di sola lettura
                (già visibili nella card avatar sopra) finché non clicchi Modifica,
                invece di avere sempre tutto editabile */}
            {!editingProfile && (
              <button
                type="button"
                onClick={() => setEditingProfile(true)}
                style={{
                  padding: '6px 16px', backgroundColor: 'transparent',
                  border: '1px solid var(--border-soft)', borderRadius: '8px',
                  color: 'var(--text-muted)', fontSize: '13px', fontWeight: '600', cursor: 'pointer',
                }}
              >
                ✏️ Modifica
              </button>
            )}
          </div>

          {editingProfile && (
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
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>
                  Colore profilo
                </label>
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
                  {AVATAR_COLORS.map(c => (
                    <button
                      key={c}
                      type="button"
                      onClick={() => setProfileColor(c)}
                      title={c}
                      style={{
                        width: '26px', height: '26px', borderRadius: '50%', backgroundColor: c,
                        border: profileColor === c ? '2px solid var(--text)' : '2px solid transparent',
                        outline: profileColor === c ? '2px solid var(--bg)' : 'none', outlineOffset: '1px',
                        cursor: 'pointer', padding: 0,
                      }}
                    />
                  ))}
                  <button
                    type="button"
                    onClick={() => setProfileColor('')}
                    title="Colore automatico (in base allo username)"
                    style={{
                      padding: '4px 10px', borderRadius: '14px', fontSize: '11px',
                      border: `1px solid ${profileColor === '' ? 'var(--text)' : 'var(--border-soft)'}`,
                      backgroundColor: 'transparent', color: 'var(--text-muted)', cursor: 'pointer',
                    }}
                  >
                    Automatico
                  </button>
                </div>
              </div>
              <div style={{ display: 'flex', gap: '10px' }}>
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
                <button
                  type="button"
                  onClick={handleCancelProfile}
                  disabled={loadingProfile}
                  style={{
                    padding: '12px 28px', backgroundColor: 'transparent',
                    border: '1px solid var(--border-soft)', borderRadius: '8px', color: 'var(--text-muted)',
                    fontSize: '15px', fontWeight: '600', cursor: loadingProfile ? 'default' : 'pointer',
                  }}
                >
                  Annulla
                </button>
              </div>
            </form>
          )}
        </div>

        {/* ── Cambia password ── */}
        <div style={CARD_STYLE}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: editingPassword ? '20px' : 0 }}>
            <h2 style={{ color: 'var(--text)', fontSize: '17px', fontWeight: '700', margin: 0 }}>
              🔒 Cambia password
            </h2>
            {!editingPassword && (
              <button
                type="button"
                onClick={() => setEditingPassword(true)}
                style={{
                  padding: '6px 16px', backgroundColor: 'transparent',
                  border: '1px solid var(--border-soft)', borderRadius: '8px',
                  color: 'var(--text-muted)', fontSize: '13px', fontWeight: '600', cursor: 'pointer',
                }}
              >
                ✏️ Modifica
              </button>
            )}
          </div>

          {editingPassword && (
          <form onSubmit={handlePasswordSubmit}>
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>Password attuale</label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showCurrent ? 'text' : 'password'}
                  value={currentPassword}
                  onChange={e => setCurrentPassword(e.target.value)}
                  required
                  style={{ ...INPUT_STYLE, paddingRight: '40px' }}
                />
                <button type="button" onClick={() => setShowCurrent(v => !v)}
                  style={{ position: 'absolute', right: '12px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px', color: 'var(--text-dark)', padding: 0 }}>
                  {showCurrent ? '🙈' : '👁️'}
                </button>
              </div>
            </div>
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>Nuova password</label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showNew ? 'text' : 'password'}
                  value={newPassword}
                  onChange={e => setNewPassword(e.target.value)}
                  required
                  style={{ ...INPUT_STYLE, paddingRight: '40px' }}
                />
                <button type="button" onClick={() => setShowNew(v => !v)}
                  style={{ position: 'absolute', right: '12px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px', color: 'var(--text-dark)', padding: 0 }}>
                  {showNew ? '🙈' : '👁️'}
                </button>
              </div>
              {/* Fix (disallineamento con RegisterPage): stesso indicatore live
                  già usato in registrazione — qui prima non c'era nulla, solo
                  un controllo silenzioso sulla lunghezza al submit. */}
              {newPassword.length > 0 && (
                <div style={{ display: 'flex', gap: '10px', marginTop: '8px', flexWrap: 'wrap' }}>
                  {[
                    { ok: newPassword.length >= 8, label: '8+ caratteri' },
                    { ok: /[a-z]/.test(newPassword), label: 'minuscola' },
                    { ok: /[A-Z]/.test(newPassword), label: 'maiuscola' },
                    { ok: /\d/.test(newPassword), label: 'numero' },
                  ].map(req => (
                    <span key={req.label} style={{ fontSize: '11px', color: req.ok ? '#22c55e' : 'var(--text-dark)' }}>
                      {req.ok ? '✓' : '·'} {req.label}
                    </span>
                  ))}
                </div>
              )}
            </div>
            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>Conferma nuova password</label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showConfirmPwd ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={e => setConfirmPassword(e.target.value)}
                  required
                  style={{
                    ...INPUT_STYLE, paddingRight: '40px',
                    border: `1px solid ${confirmPassword && confirmPassword !== newPassword ? 'var(--accent)' : 'var(--border-soft)'}`,
                  }}
                />
                <button type="button" onClick={() => setShowConfirmPwd(v => !v)}
                  style={{ position: 'absolute', right: '12px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px', color: 'var(--text-dark)', padding: 0 }}>
                  {showConfirmPwd ? '🙈' : '👁️'}
                </button>
              </div>
              {confirmPassword && confirmPassword !== newPassword && (
                <p style={{ color: 'var(--accent)', fontSize: '12px', marginTop: '4px' }}>Le password non coincidono</p>
              )}
            </div>
            <div style={{ display: 'flex', gap: '10px' }}>
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
              <button
                type="button"
                onClick={handleCancelPassword}
                disabled={loadingPassword}
                style={{
                  padding: '12px 28px', backgroundColor: 'transparent',
                  border: '1px solid var(--border-soft)', borderRadius: '8px', color: 'var(--text-muted)',
                  fontSize: '15px', fontWeight: '600', cursor: loadingPassword ? 'default' : 'pointer',
                }}
              >
                Annulla
              </button>
            </div>
          </form>
          )}
        </div>

        {/* ── Zona pericolosa ── */}
        {/* Fix: rimosso il bottone di logout duplicato — c'è già in Navbar,
            sempre accessibile da ogni pagina. Il logout inoltre non è
            un'azione "pericolosa/irreversibile" come l'eliminazione account,
            non aveva senso raggrupparli insieme */}
        <div style={{ ...CARD_STYLE, borderColor: '#3a1a1a', marginBottom: 0 }}>
          <h2 style={{ color: 'var(--accent)', fontSize: '17px', fontWeight: '700', marginBottom: '20px' }}>
            🚨 Zona pericolosa
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div>
              <p style={{ color: 'var(--text-dark)', fontSize: '13px', marginBottom: '8px' }}>
                L'eliminazione anonimizza i tuoi dati in modo permanente. Le recensioni rimangono ma non saranno associate al tuo nome.
              </p>
              <button
                onClick={() => { setDeletePassword(''); setDeleteError(''); setConfirmingDelete(true) }}
                style={{
                  padding: '10px 24px', backgroundColor: 'transparent',
                  border: '1px solid var(--accent)', borderRadius: '8px',
                  color: 'var(--accent)', fontSize: '14px', fontWeight: '600', cursor: 'pointer',
                }}
              >
                Elimina account
              </button>
            </div>
          </div>
        </div>

        {confirmingDelete && (
          <>
            <div
              onClick={() => setConfirmingDelete(false)}
              style={{ position: 'fixed', inset: 0, backgroundColor: 'var(--bg-modal)', zIndex: 500 }}
            />
            <div style={{
              position: 'fixed', top: '50%', left: '50%',
              transform: 'translate(-50%, -50%)',
              backgroundColor: 'var(--bg-card)', border: '1px solid var(--border-soft)',
              borderRadius: '12px', padding: '28px 32px',
              zIndex: 501, minWidth: '320px', maxWidth: '440px',
            }}>
              <p style={{ color: 'var(--text)', fontSize: '16px', marginBottom: '16px', lineHeight: 1.5, textAlign: 'center' }}>
                Sei sicuro di voler eliminare il tuo account? I tuoi dati verranno anonimizzati in modo permanente.
              </p>
              <label style={{ display: 'block', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '6px' }}>
                Conferma con la tua password
              </label>
              <input
                type="password"
                value={deletePassword}
                onChange={e => { setDeletePassword(e.target.value); setDeleteError('') }}
                autoFocus
                style={{ ...INPUT_STYLE, marginBottom: deleteError ? '6px' : '20px' }}
              />
              {deleteError && (
                <p style={{ color: 'var(--accent)', fontSize: '12px', marginBottom: '14px' }}>{deleteError}</p>
              )}
              <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
                <button
                  onClick={() => setConfirmingDelete(false)}
                  style={{
                    padding: '10px 24px', borderRadius: '8px',
                    border: '1px solid var(--border-soft)', backgroundColor: 'transparent',
                    color: 'var(--text)', fontSize: '14px', cursor: 'pointer',
                  }}
                >
                  Annulla
                </button>
                <button
                  onClick={handleDeleteAccount}
                  disabled={!deletePassword}
                  style={{
                    padding: '10px 24px', borderRadius: '8px',
                    border: 'none',
                    backgroundColor: 'var(--accent)', opacity: deletePassword ? 1 : 0.5,
                    color: 'var(--text)', fontSize: '14px', fontWeight: '600',
                    cursor: deletePassword ? 'pointer' : 'not-allowed',
                  }}
                >
                  Elimina account
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

export default SettingsPage
