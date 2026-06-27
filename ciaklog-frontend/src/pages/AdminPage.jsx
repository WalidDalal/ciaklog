import { useEffect, useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../services/api'
import useAuthStore from '../store/authStore'

const statusColor = (s) => ({ ACTIVE: '#22c55e', SUSPENDED: '#f59e0b', PERMANENTLY_SUSPENDED: '#e50914' }[s] || '#6b7280')
const statusLabel = (s) => ({ ACTIVE: 'Attivo', SUSPENDED: 'Sospeso', PERMANENTLY_SUSPENDED: 'Sospeso perm.' }[s] || s)

// Drawer laterale con dettaglio utente
function UserDrawer({ user: u, onClose, onSuspend, onReinstate }) {
  if (!u) return null
  return (
    <>
      {/* Overlay */}
      <div onClick={onClose} style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.6)', zIndex: 300 }} />
      {/* Pannello */}
      <div style={{
        position: 'fixed', right: 0, top: 0, bottom: 0, width: '380px',
        backgroundColor: '#111', borderLeft: '1px solid #222',
        zIndex: 400, padding: '32px 28px', overflowY: 'auto',
      }}>
        <button onClick={onClose} style={{ position: 'absolute', top: '16px', right: '16px', backgroundColor: 'transparent', border: 'none', color: '#6b7280', fontSize: '20px', cursor: 'pointer' }}>✕</button>

        {/* Avatar + nome */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '28px' }}>
          <div style={{ width: '56px', height: '56px', borderRadius: '50%', backgroundColor: '#e50914', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '24px', fontWeight: '800', color: 'white' }}>
            {u.username?.[0]?.toUpperCase()}
          </div>
          <div>
            <div style={{ color: 'white', fontWeight: '700', fontSize: '18px' }}>{u.username}</div>
            <div style={{ color: '#9ca3af', fontSize: '13px' }}>{u.email}</div>
          </div>
        </div>

        {/* Status badge */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '24px' }}>
          <span style={{
            padding: '4px 14px', borderRadius: '20px', fontSize: '13px', fontWeight: '600',
            backgroundColor: statusColor(u.status) + '22', color: statusColor(u.status), border: `1px solid ${statusColor(u.status)}44`
          }}>
            {statusLabel(u.status)}
          </span>
          <span style={{ color: '#6b7280', fontSize: '13px' }}>Ruolo: {u.role}</span>
        </div>

        {/* Statistiche */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '28px' }}>
          {[
            { label: 'Violazioni', value: u.violationCount, color: u.violationCount > 0 ? '#f59e0b' : '#9ca3af' },
            { label: 'Recensioni', value: u.reviewCount ?? '—', color: '#9ca3af' },
            { label: 'Segnalazioni ricevute', value: u.reportCount ?? '—', color: '#9ca3af' },
            { label: 'Membro dal', value: u.createdAt ? new Date(u.createdAt).toLocaleDateString('it-IT') : '—', color: '#9ca3af' },
          ].map(s => (
            <div key={s.label} style={{ backgroundColor: '#1a1a1a', borderRadius: '8px', padding: '14px' }}>
              <div style={{ color: '#6b7280', fontSize: '11px', marginBottom: '4px', textTransform: 'uppercase', letterSpacing: '1px' }}>{s.label}</div>
              <div style={{ color: s.color, fontWeight: '700', fontSize: '18px' }}>{s.value}</div>
            </div>
          ))}
        </div>

        {/* Violazioni — elenco recensioni incriminate */}
        {u.violations?.length > 0 && (
          <div style={{ marginBottom: '24px' }}>
            <div style={{ color: '#9ca3af', fontSize: '12px', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '12px' }}>
              Recensioni con violazioni
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {u.violations.map((v, i) => (
                <div key={i} style={{ backgroundColor: '#1a1a1a', borderLeft: '3px solid #e50914', borderRadius: '6px', padding: '10px 12px' }}>
                  <div style={{ color: '#e50914', fontSize: '11px', fontWeight: '600', marginBottom: '4px' }}>{v.category}</div>
                  <div style={{ color: '#9ca3af', fontSize: '13px', fontStyle: 'italic' }}>"{v.reviewText}"</div>
                  <div style={{ color: '#555', fontSize: '11px', marginTop: '4px' }}>{v.date ? new Date(v.date).toLocaleDateString('it-IT') : ''}</div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Link profilo pubblico */}
        <Link to={`/profile/${u.username}`} target="_blank">
          <button style={{ width: '100%', padding: '10px', backgroundColor: 'transparent', border: '1px solid #333', borderRadius: '8px', color: '#9ca3af', fontSize: '14px', marginBottom: '12px', cursor: 'pointer' }}>
            👤 Vedi profilo pubblico
          </button>
        </Link>

        {/* Azioni */}
        {u.status === 'ACTIVE' && (
          <button onClick={() => { onSuspend(u.id); onClose() }} style={{ width: '100%', padding: '12px', backgroundColor: 'transparent', border: '1px solid #e50914', borderRadius: '8px', color: '#e50914', fontSize: '14px', fontWeight: '600', cursor: 'pointer' }}>
            Sospendi utente
          </button>
        )}
        {u.status === 'SUSPENDED' && (
          <button onClick={() => { onReinstate(u.id); onClose() }} style={{ width: '100%', padding: '12px', backgroundColor: 'transparent', border: '1px solid #f59e0b', borderRadius: '8px', color: '#f59e0b', fontSize: '14px', fontWeight: '600', cursor: 'pointer' }}>
            Riabilita utente
          </button>
        )}
      </div>
    </>
  )
}

function AdminPage() {
  const { user, token } = useAuthStore()
  const navigate = useNavigate()
  const [tab, setTab] = useState('users')

  // Stats
  const [stats, setStats] = useState(null)

  // Utenti
  const [users, setUsers] = useState([])
  const [usersLoading, setUsersLoading] = useState(true)
  const [usersPage, setUsersPage] = useState(0)
  const [usersTotalPages, setUsersTotalPages] = useState(1)
  const [selectedUser, setSelectedUser] = useState(null)
  const [userSearch, setUserSearch] = useState('')

  // Segnalazioni
  const [reports, setReports] = useState([])
  const [reportsLoading, setReportsLoading] = useState(false)
  const [reportFilter, setReportFilter] = useState('PENDING')
  const [expandedReport, setExpandedReport] = useState(null)

  useEffect(() => {
    if (!token || user?.role !== 'ADMIN') { navigate('/'); return }
    loadStats()
    loadUsers()
  }, [token, user, usersPage])

  useEffect(() => {
    if (tab === 'reports') loadReports()
  }, [tab, reportFilter])

  const loadStats = async () => {
    try {
      const [usersRes, reportsRes] = await Promise.all([
        api.get('/admin/users', { params: { page: 0, size: 1 } }),
        api.get('/reports', { params: { status: 'PENDING' } }),
      ])
      setStats({
        totalUsers: usersRes.data.totalElements ?? users.length,
        pendingReports: Array.isArray(reportsRes.data) ? reportsRes.data.length : reportsRes.data.totalElements ?? 0,
      })
    } catch { }
  }

  const loadUsers = async () => {
    setUsersLoading(true)
    try {
      const res = await api.get('/admin/users', { params: { page: usersPage, size: 10 } })
      setUsers(res.data.content || res.data)
      setUsersTotalPages(res.data.totalPages || 1)
    } catch { setUsers([]) }
    finally { setUsersLoading(false) }
  }

  const loadReports = async () => {
    setReportsLoading(true)
    try {
      const params = reportFilter ? { status: reportFilter } : {}
      const res = await api.get('/reports', { params })
      setReports(res.data.content || res.data)
    } catch { setReports([]) }
    finally { setReportsLoading(false) }
  }

  const handleSuspend = async (userId) => {
    try { await api.put(`/admin/users/${userId}/suspend`); loadUsers(); loadStats() }
    catch (err) { alert(err.response?.data?.error || 'Errore') }
  }

  const handleReinstate = async (userId) => {
    try { await api.put(`/admin/users/${userId}/reinstate`); loadUsers(); loadStats() }
    catch (err) { alert(err.response?.data?.error || 'Errore') }
  }

  const handleReport = async (reportId, action) => {
    try { await api.put(`/reports/${reportId}`, { action }); loadReports(); loadStats() }
    catch (err) { alert(err.response?.data?.error || 'Errore') }
  }

  const filteredUsers = userSearch
    ? users.filter(u => u.username?.toLowerCase().includes(userSearch.toLowerCase()))
    : users

  return (
    <div style={{ backgroundColor: '#0a0a0a', minHeight: '100vh' }}>
      <Navbar />

      {/* Drawer utente */}
      <UserDrawer
        user={selectedUser}
        onClose={() => setSelectedUser(null)}
        onSuspend={handleSuspend}
        onReinstate={handleReinstate}
      />

      <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '40px 24px' }}>

        {/* Header */}
        <div style={{ marginBottom: '32px' }}>
          <h1 style={{ fontSize: '28px', fontWeight: '800', color: 'white', marginBottom: '8px' }}>🛡️ Pannello Admin</h1>
          <p style={{ color: '#6b7280', fontSize: '14px' }}>Gestisci utenti e segnalazioni di CiakLog</p>
        </div>

        {/* Stats cards */}
        {stats && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '32px' }}>
            {[
              { label: 'Utenti totali', value: stats.totalUsers, icon: '👥', color: '#3b82f6' },
              { label: 'Segnalazioni in attesa', value: stats.pendingReports, icon: '🚩', color: stats.pendingReports > 0 ? '#f59e0b' : '#22c55e' },
              { label: 'Pagina', value: `${usersPage + 1} / ${usersTotalPages}`, icon: '📄', color: '#9ca3af' },
            ].map(s => (
              <div key={s.label} style={{ backgroundColor: '#0f0f0f', border: '1px solid #1a1a1a', borderRadius: '10px', padding: '20px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                  <span style={{ fontSize: '20px' }}>{s.icon}</span>
                  <span style={{ color: '#6b7280', fontSize: '13px' }}>{s.label}</span>
                </div>
                <div style={{ color: s.color, fontWeight: '800', fontSize: '28px' }}>{s.value}</div>
              </div>
            ))}
          </div>
        )}

        {/* Tab */}
        <div style={{ display: 'flex', gap: '8px', marginBottom: '28px' }}>
          {[{ label: '👥 Utenti', value: 'users' }, { label: '🚩 Segnalazioni', value: 'reports' }].map(t => (
            <button key={t.value} onClick={() => setTab(t.value)} style={{
              padding: '10px 24px', borderRadius: '8px', fontSize: '14px', fontWeight: '600',
              border: `1px solid ${tab === t.value ? '#e50914' : '#333'}`,
              backgroundColor: tab === t.value ? '#e50914' : 'transparent',
              color: 'white', cursor: 'pointer',
            }}>
              {t.label}
            </button>
          ))}
        </div>

        {/* ── TAB UTENTI ── */}
        {tab === 'users' && (
          <div>
            {/* Ricerca utente */}
            <input
              type="text"
              placeholder="Filtra per username..."
              value={userSearch}
              onChange={e => setUserSearch(e.target.value)}
              style={{
                width: '100%', padding: '10px 16px', backgroundColor: '#1a1a1a',
                border: '1px solid #333', borderRadius: '8px', color: 'white',
                fontSize: '14px', marginBottom: '16px', boxSizing: 'border-box',
              }}
            />

            {usersLoading ? (
              <div style={{ textAlign: 'center', color: '#9ca3af', marginTop: '60px' }}>Caricamento...</div>
            ) : (
              <>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid #222' }}>
                      {['Username', 'Stato', 'Violazioni', 'Azioni'].map(h => (
                        <th key={h} style={{ padding: '12px 16px', textAlign: 'left', color: '#6b7280', fontSize: '12px', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '1px' }}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {filteredUsers.map(u => (
                      <tr key={u.username} style={{ borderBottom: '1px solid #111' }}
                        onMouseEnter={e => e.currentTarget.style.backgroundColor = '#0f0f0f'}
                        onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}
                      >
                        {/* Username — cliccabile per aprire drawer */}
                        <td style={{ padding: '14px 16px' }}>
                          <button onClick={() => setSelectedUser(u)} style={{ backgroundColor: 'transparent', border: 'none', color: 'white', fontWeight: '600', fontSize: '14px', cursor: 'pointer', padding: 0, textAlign: 'left' }}>
                            {u.username}
                          </button>
                        </td>
                        <td style={{ padding: '14px 16px' }}>
                          <span style={{
                            padding: '3px 12px', borderRadius: '20px', fontSize: '12px', fontWeight: '600',
                            backgroundColor: statusColor(u.status) + '22', color: statusColor(u.status), border: `1px solid ${statusColor(u.status)}44`,
                          }}>
                            {statusLabel(u.status)}
                          </span>
                        </td>
                        {/* Violazioni — cliccabile per aprire drawer */}
                        <td style={{ padding: '14px 16px' }}>
                          <button onClick={() => setSelectedUser(u)} style={{ backgroundColor: 'transparent', border: 'none', cursor: 'pointer', padding: 0 }}>
                            <span style={{
                              color: u.violationCount > 0 ? '#f59e0b' : '#6b7280',
                              fontWeight: '700', fontSize: '15px',
                              textDecoration: u.violationCount > 0 ? 'underline' : 'none',
                            }}>
                              {u.violationCount}
                              {u.violationCount > 0 && <span style={{ fontSize: '11px', marginLeft: '4px' }}>→</span>}
                            </span>
                          </button>
                        </td>
                        <td style={{ padding: '14px 16px' }}>
                          {u.status === 'ACTIVE' && (
                            <button onClick={() => handleSuspend(u.id)} style={{ padding: '5px 14px', backgroundColor: 'transparent', border: '1px solid #e50914', borderRadius: '6px', color: '#e50914', fontSize: '12px', fontWeight: '600', cursor: 'pointer' }}>
                              Sospendi
                            </button>
                          )}
                          {u.status === 'SUSPENDED' && (
                            <button onClick={() => handleReinstate(u.id)} style={{ padding: '5px 14px', backgroundColor: 'transparent', border: '1px solid #f59e0b', borderRadius: '6px', color: '#f59e0b', fontSize: '12px', fontWeight: '600', cursor: 'pointer' }}>
                              Riabilita
                            </button>
                          )}
                          {u.status === 'PERMANENTLY_SUSPENDED' && (
                            <span style={{ color: '#333', fontSize: '13px' }}>—</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                {filteredUsers.length === 0 && (
                  <div style={{ textAlign: 'center', color: '#6b7280', padding: '40px' }}>Nessun utente trovato.</div>
                )}

                {usersTotalPages > 1 && (
                  <div style={{ display: 'flex', justifyContent: 'center', gap: '12px', marginTop: '24px', alignItems: 'center' }}>
                    <button onClick={() => setUsersPage(p => Math.max(0, p - 1))} disabled={usersPage === 0}
                      style={{ padding: '8px 16px', backgroundColor: 'transparent', border: '1px solid #333', borderRadius: '6px', color: usersPage === 0 ? '#333' : 'white', cursor: usersPage === 0 ? 'default' : 'pointer' }}>←</button>
                    <span style={{ color: '#9ca3af', fontSize: '14px' }}>Pagina {usersPage + 1} di {usersTotalPages}</span>
                    <button onClick={() => setUsersPage(p => Math.min(usersTotalPages - 1, p + 1))} disabled={usersPage >= usersTotalPages - 1}
                      style={{ padding: '8px 16px', backgroundColor: 'transparent', border: '1px solid #333', borderRadius: '6px', color: usersPage >= usersTotalPages - 1 ? '#333' : 'white', cursor: usersPage >= usersTotalPages - 1 ? 'default' : 'pointer' }}>→</button>
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {/* ── TAB SEGNALAZIONI ── */}
        {tab === 'reports' && (
          <div>
            <div style={{ display: 'flex', gap: '8px', marginBottom: '24px' }}>
              {[{ label: '⏳ In attesa', value: 'PENDING' }, { label: '✅ Approvate', value: 'APPROVED' }, { label: '❌ Rifiutate', value: 'REJECTED' }, { label: 'Tutte', value: '' }].map(f => (
                <button key={f.value} onClick={() => setReportFilter(f.value)} style={{
                  padding: '6px 16px', borderRadius: '20px', fontSize: '13px',
                  border: `1px solid ${reportFilter === f.value ? '#e50914' : '#333'}`,
                  backgroundColor: reportFilter === f.value ? '#e50914' : 'transparent',
                  color: 'white', cursor: 'pointer',
                }}>
                  {f.label}
                </button>
              ))}
            </div>

            {reportsLoading ? (
              <div style={{ textAlign: 'center', color: '#9ca3af' }}>Caricamento...</div>
            ) : reports.length === 0 ? (
              <div style={{ textAlign: 'center', color: '#6b7280', marginTop: '60px', fontSize: '16px' }}>
                {reportFilter === 'PENDING' ? '✅ Nessuna segnalazione in attesa' : 'Nessuna segnalazione'}
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {reports.map(r => {
                  const expanded = expandedReport === r.id
                  return (
                    <div key={r.id} style={{ backgroundColor: '#0f0f0f', border: '1px solid #1a1a1a', borderRadius: '12px', overflow: 'hidden' }}>

                      {/* Header segnalazione */}
                      <div style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', gap: '12px', cursor: 'pointer' }}
                        onClick={() => setExpandedReport(expanded ? null : r.id)}
                      >
                        <span style={{ padding: '3px 10px', borderRadius: '4px', fontSize: '12px', fontWeight: '600', backgroundColor: '#2d1a1a', color: '#e50914', border: '1px solid #e5091444', flexShrink: 0 }}>
                          {r.reasonCategory}
                        </span>
                        <span style={{ color: 'white', fontSize: '14px', flex: 1 }}>
                          <strong>{r.reporterUsername}</strong> ha segnalato la recensione di <strong>{r.reviewAuthorUsername || '—'}</strong>
                        </span>
                        <span style={{ color: '#6b7280', fontSize: '12px', flexShrink: 0 }}>{new Date(r.createdAt).toLocaleDateString('it-IT')}</span>
                        <span style={{ color: '#6b7280', fontSize: '14px', flexShrink: 0 }}>{expanded ? '▲' : '▼'}</span>
                      </div>

                      {/* Corpo espandibile */}
                      {expanded && (
                        <div style={{ padding: '0 20px 20px', borderTop: '1px solid #1a1a1a' }}>

                          {/* Testo recensione incriminata — inline */}
                          {(r.reviewText || r.review?.text) && (
                            <div style={{ backgroundColor: '#141414', borderLeft: '3px solid #e50914', borderRadius: '0 8px 8px 0', padding: '14px 16px', margin: '16px 0' }}>
                              <div style={{ color: '#6b7280', fontSize: '11px', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '6px' }}>
                                Recensione segnalata
                              </div>
                              <p style={{ color: '#d1d5db', fontSize: '14px', lineHeight: 1.6, margin: 0, fontStyle: 'italic' }}>
                                "{r.reviewText || r.review?.text}"
                              </p>
                              {(r.reviewRating || r.review?.rating) && (
                                <div style={{ marginTop: '8px', color: '#f5c518', fontSize: '13px' }}>
                                  {'★'.repeat(r.reviewRating || r.review?.rating)}
                                </div>
                              )}
                            </div>
                          )}

                          {/* Motivo segnalazione */}
                          {r.reasonText && (
                            <div style={{ color: '#9ca3af', fontSize: '13px', marginBottom: '16px' }}>
                              <span style={{ color: '#6b7280' }}>Motivo dichiarato:</span> {r.reasonText}
                            </div>
                          )}

                          {/* Link al film */}
                          {r.tmdbId && (
                            <Link to={`/movie/${r.tmdbId}?type=${r.contentType || 'MOVIE'}`} style={{ color: '#3b82f6', fontSize: '13px', display: 'inline-block', marginBottom: '16px' }}>
                              → Vai alla pagina del contenuto
                            </Link>
                          )}

                          {/* Azioni */}
                          {r.status === 'PENDING' && (
                            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                              <button onClick={() => handleReport(r.id, 'APPROVED')} style={{ padding: '9px 20px', backgroundColor: '#e50914', border: 'none', borderRadius: '6px', color: 'white', fontSize: '13px', fontWeight: '600', cursor: 'pointer' }}>
                                ✅ Approva — rimuovi recensione
                              </button>
                              <button onClick={() => handleReport(r.id, 'REJECTED')} style={{ padding: '9px 20px', backgroundColor: 'transparent', border: '1px solid #333', borderRadius: '6px', color: '#9ca3af', fontSize: '13px', cursor: 'pointer' }}>
                                ❌ Rifiuta segnalazione
                              </button>
                            </div>
                          )}
                          {r.status !== 'PENDING' && (
                            <div style={{ color: '#6b7280', fontSize: '13px' }}>
                              Gestita il {r.resolvedAt ? new Date(r.resolvedAt).toLocaleDateString('it-IT') : '—'}
                              {r.resolvedByUsername && ` da ${r.resolvedByUsername}`}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

export default AdminPage