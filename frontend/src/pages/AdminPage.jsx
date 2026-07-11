import { useEffect, useState, useCallback, useMemo } from 'react'
import { Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../services/api'
import useAuthStore from '../store/authStore'
import useToastStore from '../store/toastStore'

const statusColor = (s) => ({ ACTIVE: '#22c55e', SUSPENDED: '#f59e0b', PERMANENTLY_SUSPENDED: 'var(--accent)' }[s] || 'var(--text-dark)')
const statusLabel = (s) => ({ ACTIVE: 'Attivo', SUSPENDED: 'Sospeso', PERMANENTLY_SUSPENDED: 'Sospeso perm.' }[s] || s)

// Modal motivo sospensione — appare quando si sospende manualmente dalla tabella
function SuspendModal({ username, onConfirm, onCancel }) {
  const [reason, setReason] = useState('')
  return (
      <>
        <div onClick={onCancel} style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.7)', zIndex: 500 }} />
        <div style={{ position: 'fixed', top: '50%', left: '50%', transform: 'translate(-50%,-50%)', backgroundColor: 'var(--bg-card)', border: '1px solid #333', borderRadius: '16px', padding: '32px', width: '100%', maxWidth: '440px', zIndex: 600 }}>
          <h2 style={{ color: 'var(--text)', fontSize: '18px', fontWeight: '700', marginBottom: '8px' }}>Sospendi {username}</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '13px', marginBottom: '20px' }}>
            Inserisci il motivo della sospensione. Questo verrà registrato nel sistema.
          </p>
          <textarea
              value={reason}
              onChange={e => setReason(e.target.value)}
              placeholder="Es: Comportamento ripetutamente offensivo nelle recensioni..."
              rows={3}
              style={{ width: '100%', padding: '10px 14px', backgroundColor: 'var(--bg-hover)', border: '1px solid #333', borderRadius: '8px', color: 'var(--text)', fontSize: '14px', resize: 'none', boxSizing: 'border-box', marginBottom: '20px' }}
          />
          <div style={{ display: 'flex', gap: '10px' }}>
            <button
                onClick={() => { if (reason.trim()) onConfirm(reason.trim()) }}
                disabled={!reason.trim()}
                style={{ flex: 1, padding: '11px', backgroundColor: !reason.trim() ? '#555' : 'var(--accent)', border: 'none', borderRadius: '8px', color: 'var(--text)', fontSize: '14px', fontWeight: '600', cursor: !reason.trim() ? 'default' : 'pointer' }}
            >
              Sospendi
            </button>
            <button onClick={onCancel} style={{ padding: '11px 20px', backgroundColor: 'transparent', border: '1px solid #333', borderRadius: '8px', color: 'var(--text-muted)', fontSize: '14px', cursor: 'pointer' }}>
              Annulla
            </button>
          </div>
        </div>
      </>
  )
}

// Drawer laterale con dettaglio utente
function UserDrawer({ user: u, onClose, onSuspend, onReinstate, loading = false }) {
  const [expandViolations, setExpandViolations] = useState(false)
  if (!u) return null
  return (
      <>
        <div onClick={onClose} style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.6)', zIndex: 300 }} />
        <div style={{
          position: 'fixed', right: 0, top: 0, bottom: 0, width: '380px',
          backgroundColor: 'var(--bg-card)', borderLeft: '1px solid #222',
          zIndex: 400, padding: '32px 28px', overflowY: 'auto',
        }}>
          <button onClick={onClose} style={{ position: 'absolute', top: '16px', right: '16px', backgroundColor: 'transparent', border: 'none', color: 'var(--text-dark)', fontSize: '20px', cursor: 'pointer' }}>✕</button>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '28px' }}>
            <div style={{ width: '56px', height: '56px', borderRadius: '50%', backgroundColor: 'var(--accent)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '24px', fontWeight: '800', color: 'var(--text)' }}>
              {u.username?.[0]?.toUpperCase()}
            </div>
            <div>
              <div style={{ color: 'var(--text)', fontWeight: '700', fontSize: '18px' }}>{u.username}</div>
              <div style={{ color: 'var(--text-muted)', fontSize: '13px' }}>{u.email}</div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '24px' }}>
          <span style={{
            padding: '4px 14px', borderRadius: '20px', fontSize: '13px', fontWeight: '600',
            backgroundColor: statusColor(u.status) + '22', color: statusColor(u.status), border: `1px solid ${statusColor(u.status)}44`
          }}>
            {statusLabel(u.status)}
          </span>
          </div>

          {loading && <div style={{ color: 'var(--text-dark)', fontSize: '13px', marginBottom: '16px' }}>Caricamento dettagli...</div>}

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '28px' }}>
            {/* Card violazioni — cliccabile se > 0 */}
            <div
                onClick={() => u.violationCount > 0 && setExpandViolations(v => !v)}
                style={{ backgroundColor: 'var(--bg-hover)', borderRadius: '8px', padding: '14px', cursor: u.violationCount > 0 ? 'pointer' : 'default', border: u.violationCount > 0 ? '1px solid #f59e0b44' : '1px solid transparent' }}
            >
              <div style={{ color: 'var(--text-dark)', fontSize: '11px', marginBottom: '4px', textTransform: 'uppercase', letterSpacing: '1px' }}>
                Violazioni {u.violationCount > 0 && <span style={{ color: '#f59e0b' }}>{expandViolations ? '▲' : '▼'}</span>}
              </div>
              <div style={{ color: u.violationCount > 0 ? '#f59e0b' : 'var(--text-muted)', fontWeight: '700', fontSize: '18px' }}>{u.violationCount}</div>
            </div>
            {[
              { label: 'Recensioni', value: u.reviewCount ?? '—', color: 'var(--text-muted)' },
              { label: 'Segnalazioni ricevute', value: u.reportCount ?? '—', color: 'var(--text-muted)' },
              { label: 'Membro dal', value: u.createdAt ? new Date(u.createdAt).toLocaleDateString('it-IT') : '—', color: 'var(--text-muted)' },
            ].map(s => (
                <div key={s.label} style={{ backgroundColor: 'var(--bg-hover)', borderRadius: '8px', padding: '14px' }}>
                  <div style={{ color: 'var(--text-dark)', fontSize: '11px', marginBottom: '4px', textTransform: 'uppercase', letterSpacing: '1px' }}>{s.label}</div>
                  <div style={{ color: s.color, fontWeight: '700', fontSize: '18px' }}>{s.value}</div>
                </div>
            ))}
          </div>

          {/* Dettaglio violazioni — espandibile cliccando la card */}
          {expandViolations && (
              <div style={{ marginBottom: '24px', gridColumn: '1 / -1' }}>
                <div style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '12px' }}>
                  Dettaglio violazioni
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {!u.violations?.length && (
                      <div style={{ color: 'var(--text-dark)', fontSize: '13px', padding: '8px 0' }}>Nessun dettaglio disponibile.</div>
                  )}
                  {u.violations?.map((v, i) => (
                      <div key={i} style={{ backgroundColor: 'var(--bg-hover)', borderLeft: '3px solid #f59e0b', borderRadius: '6px', padding: '12px 14px' }}>
                        <div style={{ color: '#f59e0b', fontSize: '11px', fontWeight: '700', marginBottom: '4px', textTransform: 'uppercase' }}>{v.category}</div>
                        {v.reviewText && v.reviewText !== '—' && (
                            <div style={{ color: 'var(--text-muted)', fontSize: '13px', fontStyle: 'italic', marginBottom: '6px' }}>"{v.reviewText}"</div>
                        )}
                        {v.reasonText && v.reasonText !== '—' && (
                            <div style={{ color: 'var(--text-dark)', fontSize: '12px', marginBottom: '4px' }}>
                              <span style={{ color: '#555' }}>Motivo segnalazione: </span>{v.reasonText}
                            </div>
                        )}
                        <div style={{ color: '#555', fontSize: '11px' }}>{v.date ? new Date(v.date).toLocaleDateString('it-IT') : ''}</div>
                      </div>
                  ))}
                </div>
              </div>
          )}

          <Link to={`/profile/${u.username}`} target="_blank">
            <button style={{ width: '100%', padding: '10px', backgroundColor: 'transparent', border: '1px solid #333', borderRadius: '8px', color: 'var(--text-muted)', fontSize: '14px', marginBottom: '12px', cursor: 'pointer' }}>
              👤 Vedi profilo pubblico
            </button>
          </Link>

          {/* Sospensione disponibile solo dalla tabella (richiede motivo) — qui solo riabilita */}
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
  const toast = useToastStore()
  const [tab, setTab] = useState('users')

  const [stats, setStats] = useState(null)
  const [users, setUsers] = useState([])
  const [usersLoading, setUsersLoading] = useState(true)
  const [usersPage, setUsersPage] = useState(0)
  const [usersTotalPages, setUsersTotalPages] = useState(1)
  const [selectedUser, setSelectedUser] = useState(null)
  const [drawerLoading, setDrawerLoading] = useState(false)
  const [userSearch, setUserSearch] = useState('')

  // Modal sospensione manuale dalla tabella
  const [suspendTarget, setSuspendTarget] = useState(null) // { id, username }

  const [reports, setReports] = useState([])
  const [reportsLoading, setReportsLoading] = useState(false)
  const [reportFilter, setReportFilter] = useState('PENDING')
  const [expandedReport, setExpandedReport] = useState(null)

  const openUserDrawer = async (u) => {
    setSelectedUser(u)
    setDrawerLoading(true)
    try {
      const res = await api.get(`/admin/users/${u.id}`)
      setSelectedUser(res.data)
    } catch {
    } finally {
      setDrawerLoading(false)
    }
  }

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

  const loadUsers = useCallback(async () => {
    setUsersLoading(true)
    try {
      const res = await api.get('/admin/users', {
        params: { page: usersPage, size: 10, search: userSearch || undefined }
      })
      setUsers(res.data.content || res.data)
      setUsersTotalPages(res.data.totalPages || 1)
    } catch { setUsers([]) }
    finally { setUsersLoading(false) }
  }, [usersPage, userSearch])

  const loadReports = async () => {
    setReportsLoading(true)
    try {
      // Nessun filtro "Tutti" — se non c'è filtro si usa PENDING di default
      const params = reportFilter ? { status: reportFilter } : {}
      const res = await api.get('/reports', { params })
      setReports(res.data.content || res.data)
    } catch { setReports([]) }
    finally { setReportsLoading(false) }
  }

  useEffect(() => { loadStats() }, [token, user])

  useEffect(() => {
    const timer = setTimeout(() => loadUsers(), 400)
    return () => clearTimeout(timer)
  }, [loadUsers])

  useEffect(() => {
    if (tab === 'reports') loadReports()
  }, [tab, reportFilter])

  const handleSuspend = async (userId, reason) => {
    try {
      await api.put(`/admin/users/${userId}/suspend`, { reason })
      loadUsers(); loadStats()
    } catch (err) { toast.show(err.response?.data?.error || 'Errore') }
  }

  const handleReinstate = async (userId) => {
    try { await api.put(`/admin/users/${userId}/reinstate`); loadUsers(); loadStats() }
    catch (err) { toast.show(err.response?.data?.error || 'Errore') }
  }

  // Fix: raggruppa le segnalazioni per recensione, invece di mostrarle come
  // righe separate — così l'admin vede "Recensione X — N segnalazioni"
  const reportGroups = useMemo(() => {
    const map = new Map()
    reports.forEach(r => {
      if (!map.has(r.reviewId)) map.set(r.reviewId, [])
      map.get(r.reviewId).push(r)
    })
    return Array.from(map.values())
  }, [reports])

  // Approva/rifiuta in blocco tutte le segnalazioni pendenti di una recensione
  const handleReportGroup = async (group, action) => {
    const pendingIds = group.filter(r => r.status === 'PENDING').map(r => r.id)
    try {
      await Promise.all(pendingIds.map(id => api.put(`/reports/${id}`, { action })))
      loadReports(); loadStats()
    } catch (err) { toast.show(err.response?.data?.error || 'Errore') }
  }

  return (
      <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}>
        <Navbar />

        <UserDrawer
            user={selectedUser}
            onClose={() => setSelectedUser(null)}
            onSuspend={handleSuspend}
            onReinstate={handleReinstate}
            loading={drawerLoading}
        />

        {/* Modal motivo sospensione */}
        {suspendTarget && (
            <SuspendModal
                username={suspendTarget.username}
                onConfirm={(reason) => { handleSuspend(suspendTarget.id, reason); setSuspendTarget(null) }}
                onCancel={() => setSuspendTarget(null)}
            />
        )}

        <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '40px 24px' }}>

          <div style={{ marginBottom: '32px' }}>
            <h1 style={{ fontSize: '28px', fontWeight: '800', color: 'var(--text)', marginBottom: '8px' }}>🛡️ Pannello Admin</h1>
            <p style={{ color: 'var(--text-dark)', fontSize: '14px' }}>Gestisci utenti e segnalazioni di CiakLog</p>
          </div>

          {stats && (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '32px' }}>
                {[
                  { label: 'Utenti totali', value: stats.totalUsers, icon: '👥', color: '#3b82f6' },
                  { label: 'Segnalazioni in attesa', value: stats.pendingReports, icon: '🚩', color: stats.pendingReports > 0 ? '#f59e0b' : '#22c55e' },
                  { label: 'Pagina', value: `${usersPage + 1} / ${usersTotalPages}`, icon: '📄', color: 'var(--text-muted)' },
                ].map(s => (
                    <div key={s.label} style={{ backgroundColor: 'var(--bg-nav)', border: '1px solid #1a1a1a', borderRadius: '10px', padding: '20px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                        <span style={{ fontSize: '20px' }}>{s.icon}</span>
                        <span style={{ color: 'var(--text-dark)', fontSize: '13px' }}>{s.label}</span>
                      </div>
                      <div style={{ color: s.color, fontWeight: '800', fontSize: '28px' }}>{s.value}</div>
                    </div>
                ))}
              </div>
          )}

          <div style={{ display: 'flex', gap: '8px', marginBottom: '28px' }}>
            {[{ label: '👥 Utenti', value: 'users' }, { label: '🚩 Segnalazioni', value: 'reports' }].map(t => (
                <button key={t.value} onClick={() => setTab(t.value)} style={{
                  padding: '10px 24px', borderRadius: '8px', fontSize: '14px', fontWeight: '600',
                  border: `1px solid ${tab === t.value ? 'var(--accent)' : 'var(--border-soft)'}`,
                  backgroundColor: tab === t.value ? 'var(--accent)' : 'transparent',
                  color: 'var(--text)', cursor: 'pointer',
                }}>
                  {t.label}
                </button>
            ))}
          </div>

          {/* ── TAB UTENTI ── */}
          {tab === 'users' && (
              <div>
                <input
                    type="text"
                    placeholder="Filtra per username..."
                    value={userSearch}
                    onChange={e => setUserSearch(e.target.value)}
                    style={{
                      width: '100%', padding: '10px 16px', backgroundColor: 'var(--bg-hover)',
                      border: '1px solid #333', borderRadius: '8px', color: 'var(--text)',
                      fontSize: '14px', marginBottom: '16px', boxSizing: 'border-box',
                    }}
                />

                {usersLoading ? (
                    <div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '60px' }}>Caricamento...</div>
                ) : (
                    <>
                      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                        <tr style={{ borderBottom: '1px solid #222' }}>
                          {['Username', 'Stato', 'Violazioni', 'Azioni'].map(h => (
                              <th key={h} style={{ padding: '12px 16px', textAlign: 'left', color: 'var(--text-dark)', fontSize: '12px', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '1px' }}>{h}</th>
                          ))}
                        </tr>
                        </thead>
                        <tbody>
                        {users.map(u => (
                            <tr key={u.username} style={{ borderBottom: '1px solid #111' }}
                                onMouseEnter={e => e.currentTarget.style.backgroundColor = 'var(--bg-nav)'}
                                onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}
                            >
                              <td style={{ padding: '14px 16px' }}>
                                <button onClick={() => openUserDrawer(u)} style={{ backgroundColor: 'transparent', border: 'none', color: 'var(--text)', fontWeight: '600', fontSize: '14px', cursor: 'pointer', padding: 0, textAlign: 'left' }}>
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
                              <td style={{ padding: '14px 16px' }}>
                          <span style={{
                            color: u.violationCount > 0 ? '#f59e0b' : 'var(--text-dark)',
                            fontWeight: '700', fontSize: '15px',
                          }}>
                            {u.violationCount}
                          </span>
                              </td>
                              <td style={{ padding: '14px 16px' }}>
                                {u.status === 'ACTIVE' && (
                                    // Sospensione dalla tabella → richiede motivo
                                    <button onClick={() => setSuspendTarget({ id: u.id, username: u.username })}
                                            style={{ padding: '5px 14px', backgroundColor: 'transparent', border: '1px solid #e50914', borderRadius: '6px', color: 'var(--accent)', fontSize: '12px', fontWeight: '600', cursor: 'pointer' }}>
                                      Sospendi
                                    </button>
                                )}
                                {u.status === 'SUSPENDED' && (
                                    <button onClick={() => handleReinstate(u.id)}
                                            style={{ padding: '5px 14px', backgroundColor: 'transparent', border: '1px solid #f59e0b', borderRadius: '6px', color: '#f59e0b', fontSize: '12px', fontWeight: '600', cursor: 'pointer' }}>
                                      Riabilita
                                    </button>
                                )}
                                {u.status === 'PERMANENTLY_SUSPENDED' && (
                                    <span style={{ color: 'var(--border-soft)', fontSize: '13px' }}>—</span>
                                )}
                              </td>
                            </tr>
                        ))}
                        </tbody>
                      </table>

                      {users.length === 0 && (
                          <div style={{ textAlign: 'center', color: 'var(--text-dark)', padding: '40px' }}>Nessun utente trovato.</div>
                      )}

                      {usersTotalPages > 1 && (
                          <div style={{ display: 'flex', justifyContent: 'center', gap: '12px', marginTop: '24px', alignItems: 'center' }}>
                            <button onClick={() => setUsersPage(p => Math.max(0, p - 1))} disabled={usersPage === 0}
                                    style={{ padding: '8px 16px', backgroundColor: 'transparent', border: '1px solid #333', borderRadius: '6px', color: usersPage === 0 ? 'var(--border-soft)' : 'var(--text)', cursor: usersPage === 0 ? 'default' : 'pointer' }}>←</button>
                            <span style={{ color: 'var(--text-muted)', fontSize: '14px' }}>Pagina {usersPage + 1} di {usersTotalPages}</span>
                            <button onClick={() => setUsersPage(p => Math.min(usersTotalPages - 1, p + 1))} disabled={usersPage >= usersTotalPages - 1}
                                    style={{ padding: '8px 16px', backgroundColor: 'transparent', border: '1px solid #333', borderRadius: '6px', color: usersPage >= usersTotalPages - 1 ? 'var(--border-soft)' : 'var(--text)', cursor: usersPage >= usersTotalPages - 1 ? 'default' : 'pointer' }}>→</button>
                          </div>
                      )}
                    </>
                )}
              </div>
          )}

          {/* ── TAB SEGNALAZIONI ── */}
          {tab === 'reports' && (
              <div>
                {/* Filtri — rimosso "Tutte" */}
                <div style={{ display: 'flex', gap: '8px', marginBottom: '24px' }}>
                  {[
                    { label: '⏳ In attesa', value: 'PENDING' },
                    { label: '✅ Approvate', value: 'APPROVED' },
                    { label: '❌ Rifiutate', value: 'REJECTED' },
                  ].map(f => (
                      <button key={f.value} onClick={() => setReportFilter(f.value)} style={{
                        padding: '6px 16px', borderRadius: '20px', fontSize: '13px',
                        border: `1px solid ${reportFilter === f.value ? 'var(--accent)' : 'var(--border-soft)'}`,
                        backgroundColor: reportFilter === f.value ? 'var(--accent)' : 'transparent',
                        color: 'var(--text)', cursor: 'pointer',
                      }}>
                        {f.label}
                      </button>
                  ))}
                </div>

                {reportsLoading ? (
                    <div style={{ textAlign: 'center', color: 'var(--text-muted)' }}>Caricamento...</div>
                ) : reportGroups.length === 0 ? (
                    <div style={{ textAlign: 'center', color: 'var(--text-dark)', marginTop: '60px', fontSize: '16px' }}>
                      {reportFilter === 'PENDING' ? '✅ Nessuna segnalazione in attesa' : 'Nessuna segnalazione'}
                    </div>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                      {reportGroups.map(group => {
                        const first = group[0]
                        const groupKey = first.reviewId
                        const expanded = expandedReport === groupKey
                        const pendingCount = group.filter(r => r.status === 'PENDING').length
                        return (
                            <div key={groupKey} style={{ backgroundColor: 'var(--bg-nav)', border: '1px solid #1a1a1a', borderRadius: '12px', overflow: 'hidden' }}>

                              <div style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', gap: '12px', cursor: 'pointer' }}
                                   onClick={() => setExpandedReport(expanded ? null : groupKey)}
                              >
                        <span style={{ padding: '3px 10px', borderRadius: '4px', fontSize: '12px', fontWeight: '600', backgroundColor: '#2d1a1a', color: 'var(--accent)', border: '1px solid #e5091444', flexShrink: 0 }}>
                          {group.length} {group.length === 1 ? 'segnalazione' : 'segnalazioni'}
                        </span>
                                <span style={{ color: 'var(--text)', fontSize: '14px', flex: 1 }}>
                          Recensione di <strong>{first.reviewAuthorUsername || '—'}</strong>
                          {group.length > 1 && (
                              <span style={{ color: 'var(--text-dark)' }}> — segnalata da {group.map(r => r.reporterUsername).join(', ')}</span>
                          )}
                        </span>
                                <span style={{ color: 'var(--text-dark)', fontSize: '12px', flexShrink: 0 }}>{new Date(first.createdAt).toLocaleDateString('it-IT')}</span>
                                <span style={{ color: 'var(--text-dark)', fontSize: '14px', flexShrink: 0 }}>{expanded ? '▲' : '▼'}</span>
                              </div>

                              {expanded && (
                                  <div style={{ padding: '0 20px 20px', borderTop: '1px solid #1a1a1a' }}>

                                    {(first.reviewText) && (
                                        <div style={{ backgroundColor: 'var(--bg-card)', borderLeft: '3px solid #e50914', borderRadius: '0 8px 8px 0', padding: '14px 16px', margin: '16px 0' }}>
                                          <div style={{ color: 'var(--text-dark)', fontSize: '11px', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '6px' }}>
                                            Recensione segnalata
                                          </div>
                                          <p style={{ color: '#d1d5db', fontSize: '14px', lineHeight: 1.6, margin: 0, fontStyle: 'italic' }}>
                                            "{first.reviewText}"
                                          </p>
                                          {first.reviewRating && (
                                              <div style={{ marginTop: '8px', color: 'var(--gold)', fontSize: '13px' }}>
                                                {'★'.repeat(first.reviewRating)}
                                              </div>
                                          )}
                                        </div>
                                    )}

                                    {/* Elenco delle singole segnalazioni ricevute su questa recensione */}
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '16px' }}>
                                      {group.map(r => (
                                          <div key={r.id} style={{ backgroundColor: 'var(--bg-hover)', borderRadius: '8px', padding: '10px 14px' }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: r.reasonText ? '4px' : 0 }}>
                                              <span style={{ color: 'var(--text)', fontSize: '13px', fontWeight: '600' }}>{r.reporterUsername}</span>
                                              <span style={{ padding: '1px 8px', borderRadius: '4px', fontSize: '11px', fontWeight: '600', backgroundColor: '#2d1a1a', color: 'var(--accent)', border: '1px solid #e5091444' }}>
                                                {r.reasonCategory}
                                              </span>
                                              <span style={{ color: 'var(--text-dark)', fontSize: '11px', marginLeft: 'auto' }}>{new Date(r.createdAt).toLocaleDateString('it-IT')}</span>
                                              {r.status !== 'PENDING' && (
                                                  <span style={{ color: 'var(--text-dark)', fontSize: '11px' }}>
                                                    {r.status === 'APPROVED' ? '✅' : '❌'} {r.resolvedByUsername ? `da ${r.resolvedByUsername}` : ''}
                                                  </span>
                                              )}
                                            </div>
                                            {r.reasonText && (
                                                <div style={{ color: 'var(--text-muted)', fontSize: '12px' }}>{r.reasonText}</div>
                                            )}
                                          </div>
                                      ))}
                                    </div>

                                    {pendingCount > 0 && (
                                        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                                          <button onClick={() => handleReportGroup(group, 'APPROVED')} style={{ padding: '9px 20px', backgroundColor: 'var(--accent)', border: 'none', borderRadius: '6px', color: 'var(--text)', fontSize: '13px', fontWeight: '600', cursor: 'pointer' }}>
                                            ✅ Approva {pendingCount > 1 ? `tutte (${pendingCount})` : ''} — rimuovi recensione
                                          </button>
                                          <button onClick={() => handleReportGroup(group, 'REJECTED')} style={{ padding: '9px 20px', backgroundColor: 'transparent', border: '1px solid #333', borderRadius: '6px', color: 'var(--text-muted)', fontSize: '13px', cursor: 'pointer' }}>
                                            ❌ Rifiuta {pendingCount > 1 ? `tutte (${pendingCount})` : ''}
                                          </button>
                                        </div>
                                    )}
                                    {pendingCount === 0 && (
                                        <div style={{ color: 'var(--text-dark)', fontSize: '13px' }}>
                                          Tutte le segnalazioni di questa recensione sono state gestite.
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