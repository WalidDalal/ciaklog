import { useEffect, useState, useRef } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../services/api'
import useAuthStore from '../store/authStore'
import useToastStore from '../store/toastStore'

// Fix (Profilo — "immagini rosse"): l'avatar grande era sempre var(--accent)
// (rosso) per chiunque, fisso. Richiesto un colore diverso per utente, ma
// "casuale, non assegnato da te" — quindi non una lista di colori scelti a
// mano per singolo utente, ma un hash dello username che sceglie da una
// piccola palette: stesso utente = sempre lo stesso colore (utile per
// riconoscerlo a colpo d'occhio), ma quale colore tocchi a chi non è deciso
// a mano, deriva dai caratteri dello username stesso.
const AVATAR_COLORS = ['var(--accent)', '#3b82f6', '#22c55e', '#f59e0b', '#8b5cf6', '#ec4899', '#14b8a6', '#eab308']
function colorForUsername(name) {
  if (!name) return AVATAR_COLORS[0]
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = (hash * 31 + name.charCodeAt(i)) >>> 0
  }
  return AVATAR_COLORS[hash % AVATAR_COLORS.length]
}


function ProfilePage() {
  const { username } = useParams()
  const navigate = useNavigate()
  const { user: me, updateToken } = useAuthStore()
  const toast = useToastStore()
  const isOwn = me?.username === username

  const [profile, setProfile] = useState(null)
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [reviewsPage, setReviewsPage] = useState(0)
  const [hasMoreReviews, setHasMoreReviews] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)

  // Editing state
  const [editing, setEditing] = useState(false)
  const [editUsername, setEditUsername] = useState('')
  const [editBio, setEditBio] = useState('')
  const [editColor, setEditColor] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    setReviews([])
    setReviewsPage(0)
    Promise.all([
      api.get(`/users/${username}`),
      api.get(`/reviews/user/${username}`, { params: { page: 0, size: 10 } }),
    ])
        .then(([profileRes, reviewsRes]) => {
          setProfile(profileRes.data)
          setEditUsername(profileRes.data.username || username)
          setEditBio(profileRes.data.bio || '')
          setEditColor(profileRes.data.profileColor || '')
          const data = reviewsRes.data
          setReviews(data.content || data || [])
          // Stesso problema di AdminPage — con PageSerializationMode.VIA_DTO
          // i metadati di paginazione sono annidati sotto `.page.`, non in cima.
          // Prima "data.page" leggeva l'oggetto metadata invece del numero pagina
          // corrente (0, appena richiesta), e "data.totalPages" era sempre undefined
          setHasMoreReviews(data.page?.totalPages ? 0 < data.page.totalPages - 1 : false)
        })
        .catch(() => {})
        .finally(() => setLoading(false))
  }, [username])

  const loadMoreReviews = async () => {
    setLoadingMore(true)
    const nextPage = reviewsPage + 1
    try {
      const res = await api.get(`/reviews/user/${username}`, { params: { page: nextPage, size: 10 } })
      const data = res.data
      setReviews(prev => [...prev, ...(data.content || data || [])])
      setReviewsPage(nextPage)
      setHasMoreReviews(data.page?.totalPages ? nextPage < data.page.totalPages - 1 : false)
    } catch {} finally { setLoadingMore(false) }
  }

  const handleSaveProfile = async () => {
    setSaving(true)
    try {
      const res = await api.put('/users/me', {
        username: editUsername !== username ? editUsername : undefined,
        bio: editBio,
        profileColor: editColor,
      })
      if (res.data?.token) updateToken(res.data.token)
      setProfile(p => ({ ...p, bio: editBio, username: editUsername, profileColor: editColor || null }))
      setEditing(false)
      toast.show('Profilo aggiornato!', 'success')
      // Se l'username è cambiato naviga al nuovo URL
      if (editUsername !== username) navigate(`/profile/${editUsername}`, { replace: true })
    } catch (err) {
      toast.show(err.response?.data?.error || 'Errore durante il salvataggio')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return (
      <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}>
        <Navbar />
        <div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '100px' }}>Caricamento...</div>
      </div>
  )

  if (!profile) return (
      <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}>
        <Navbar />
        <div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '100px' }}>Utente non trovato</div>
      </div>
  )

  const watching = profile.watching
      ? (Array.isArray(profile.watching) ? profile.watching : [profile.watching])
      : profile.watchingTitle
          ? [{ title: profile.watchingTitle, season: profile.watchingSeason }]
          : []

  const INPUT_STYLE = {
    width: '100%', padding: '8px 12px', boxSizing: 'border-box',
    backgroundColor: 'var(--bg-input)', border: '1px solid var(--border-soft)',
    borderRadius: '8px', color: 'var(--text)', fontSize: '14px', fontFamily: 'inherit',
  }

  return (
      <div style={{ backgroundColor: 'var(--bg)', height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <Navbar />

        {/* Fix: la parte superiore (avatar, bio, stats) resta fissa; solo la lista
            sotto (in visione + recensioni) scrolla */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', maxWidth: '900px', width: '100%', margin: '0 auto', padding: '0 24px', boxSizing: 'border-box', overflow: 'hidden' }}>

          {/* ── Header profilo (fisso) ── */}
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '28px', paddingTop: '48px', paddingBottom: '32px', flexShrink: 0 }}>
            {/* Avatar */}
            <div style={{
              width: '88px', height: '88px', borderRadius: '50%', flexShrink: 0,
              backgroundColor: profile.profileColor || colorForUsername(profile.username || username),
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '36px', fontWeight: '800', color: 'white',
            }}>
              {(editUsername || username)?.[0]?.toUpperCase()}
            </div>

            <div style={{ flex: 1 }}>
              {editing ? (
                  /* ── Form modifica inline ── */
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', maxWidth: '400px' }}>
                    <div>
                      <label style={{ color: 'var(--text-muted)', fontSize: '12px', display: 'block', marginBottom: '4px' }}>Username</label>
                      <input
                          value={editUsername}
                          onChange={e => setEditUsername(e.target.value)}
                          style={INPUT_STYLE}
                          maxLength={30}
                      />
                    </div>
                    <div>
                      <label style={{ color: 'var(--text-muted)', fontSize: '12px', display: 'block', marginBottom: '4px' }}>Bio</label>
                      <textarea
                          value={editBio}
                          onChange={e => setEditBio(e.target.value)}
                          rows={3}
                          maxLength={200}
                          placeholder="Scrivi qualcosa su di te..."
                          style={{ ...INPUT_STYLE, resize: 'vertical' }}
                      />
                      <div style={{ color: 'var(--text-dark)', fontSize: '11px', textAlign: 'right', marginTop: '2px' }}>
                        {editBio.length}/200
                      </div>
                    </div>
                    <div>
                      <label style={{ color: 'var(--text-muted)', fontSize: '12px', display: 'block', marginBottom: '6px' }}>Colore profilo</label>
                      <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
                        {AVATAR_COLORS.map(c => (
                            <button
                                key={c}
                                type="button"
                                onClick={() => setEditColor(c)}
                                title={c}
                                style={{
                                  width: '26px', height: '26px', borderRadius: '50%', backgroundColor: c,
                                  border: editColor === c ? '2px solid var(--text)' : '2px solid transparent',
                                  outline: editColor === c ? '2px solid var(--bg)' : 'none', outlineOffset: '1px',
                                  cursor: 'pointer', padding: 0,
                                }}
                            />
                        ))}
                        <button
                            type="button"
                            onClick={() => setEditColor('')}
                            title="Colore automatico (in base allo username)"
                            style={{
                              padding: '4px 10px', borderRadius: '14px', fontSize: '11px',
                              border: `1px solid ${editColor === '' ? 'var(--text)' : 'var(--border-soft)'}`,
                              backgroundColor: 'transparent', color: 'var(--text-muted)', cursor: 'pointer',
                            }}
                        >
                          Automatico
                        </button>
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button
                          onClick={handleSaveProfile}
                          disabled={saving}
                          style={{
                            padding: '8px 20px', backgroundColor: 'var(--accent)', border: 'none',
                            borderRadius: '8px', color: 'white', fontSize: '14px', fontWeight: '600',
                            cursor: saving ? 'default' : 'pointer', opacity: saving ? 0.7 : 1,
                          }}
                      >
                        {saving ? 'Salvataggio...' : 'Salva'}
                      </button>
                      <button
                          onClick={() => { setEditing(false); setEditUsername(profile.username || username); setEditBio(profile.bio || ''); setEditColor(profile.profileColor || '') }}
                          style={{
                            padding: '8px 16px', backgroundColor: 'transparent',
                            border: '1px solid var(--border)', borderRadius: '8px',
                            color: 'var(--text-muted)', fontSize: '14px', cursor: 'pointer',
                          }}
                      >
                        Annulla
                      </button>
                    </div>
                  </div>
              ) : (
                  /* ── Visualizzazione normale ── */
                  <>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '6px', flexWrap: 'wrap' }}>
                      <h1 style={{ fontSize: '28px', fontWeight: '800', color: 'var(--text)' }}>
                        {profile.username || username}
                      </h1>
                      {/* Fix (styling): badge generi accanto al nome — più rapido da vedere
                          a colpo d'occhio rispetto a metterli sotto, nella riga stats */}
                      {profile.topGenres?.length > 0 && (
                          <div style={{ display: 'flex', gap: '6px', alignItems: 'center', flexWrap: 'wrap' }}>
                            {profile.topGenres.slice(0, 3).map(g => (
                                <span key={g} style={{
                                  padding: '3px 10px', backgroundColor: 'var(--bg-hover)',
                                  border: '1px solid var(--border)', borderRadius: '20px',
                                  color: 'var(--text-muted)', fontSize: '12px',
                                }}>{g}</span>
                            ))}
                          </div>
                      )}
                    </div>

                    {/* Fix (Profilo — posizione bottone Modifica, secondo giro): prima stava
                        alla sinistra dei generi, sulla stessa riga dello username — spostato
                        qui sotto, su una riga propria, così non si confonde con lo username/
                        generi come elementi "informativi" mentre Modifica è un'azione */}
                    {isOwn && (
                        <button
                            onClick={() => setEditing(true)}
                            title="Modifica profilo"
                            style={{
                              display: 'inline-flex', marginBottom: '10px',
                              backgroundColor: 'transparent', border: '1px solid var(--border)',
                              borderRadius: '6px', padding: '4px 10px',
                              color: 'var(--text-dark)', fontSize: '12px', cursor: 'pointer', whiteSpace: 'nowrap',
                            }}
                        >
                          ✏️ Modifica
                        </button>
                    )}

                    {profile.bio ? (
                        <p style={{ color: 'var(--text-muted)', fontSize: '14px', lineHeight: 1.6, marginBottom: '14px', maxWidth: '500px' }}>
                          {profile.bio}
                        </p>
                    ) : isOwn && (
                        <p
                            onClick={() => setEditing(true)}
                            style={{ color: 'var(--text-dark)', fontSize: '14px', marginBottom: '14px', cursor: 'pointer', fontStyle: 'italic' }}
                        >
                          + Aggiungi una bio
                        </p>
                    )}

                    {/* Fix (Profilo — contatore recensioni duplicato): qui c'era un
                        blocco "Stats" con "N recensioni" basato sul totale reale
                        (profile.totalReviews), ma sotto, nell'header della sezione
                        Recensioni, c'è un secondo contatore (reviews.length) che invece
                        cresce 10 alla volta man mano che si clicca "Carica altre
                        recensioni" — due numeri diversi per lo stesso concetto,
                        confusionario (es. "22 recensioni" qui sopra ma "10" appena sotto
                        finché non carichi tutto). Rimosso questo, il secondo resta: è
                        quello corretto per riflettere cosa è effettivamente visibile in
                        pagina in quel momento. */}
                  </>
              )}
            </div>

            {/* Fix (Profilo): il bottone Wrapped era un cerchietto anonimo — solo
                un'emoji, capibile solo passandoci sopra col mouse (title). Aggiunta
                un'etichetta visibile sotto, niente più tooltip-only. "Modifica" è
                stato spostato accanto allo username qui sotto: è lì che agisce
                davvero (username + bio), non ha senso vicino al Wrapped. */}
            {isOwn && !editing && (
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px', flexShrink: 0 }}>
                  <Link to="/wrapped" style={{
                    width: '52px', height: '52px', borderRadius: '50%',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    backgroundColor: 'var(--accent-subtle)', border: '1px solid var(--border-cta)',
                    color: 'var(--gold)', fontSize: '22px', textDecoration: 'none',
                  }}>
                    🎬
                  </Link>
                  <span style={{ color: 'var(--text-dark)', fontSize: '11px', fontWeight: '600' }}>Wrapped</span>
                </div>
            )}
          </div>

          {/* ── Area scrollabile: in visione + recensioni ── */}
          <div style={{ flex: 1, overflowY: 'auto', paddingBottom: '48px' }}>

          {/* ── In visione ── */}
          {watching.length > 0 && (
              <div style={{ marginBottom: '40px' }}>
                <h2 style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '12px' }}>
                  {/* Fix (Profilo — "togliere le emoji", secondo giro): tolta anche
                      l'emoji del titolo sezione, non solo quella per ogni riga */}
                  In visione
                </h2>
                <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                  {watching.map((w, i) => (
                      <div key={i} style={{
                        backgroundColor: 'var(--bg-card)', border: '1px solid var(--border)',
                        borderRadius: '8px', padding: '10px 16px',
                        display: 'flex', alignItems: 'center', gap: '10px',
                      }}>
                        {/* Fix (Profilo — "togliamo ste emoji con i film/serie visti"):
                            rimossa l'icona ▶️ ripetuta su ogni titolo, era solo rumore visivo */}
                        <div>
                          <div style={{ color: 'var(--text)', fontWeight: '600', fontSize: '14px' }}>{w.title || w}</div>
                          {w.season && <div style={{ color: 'var(--text-dark)', fontSize: '12px' }}>Stagione {w.season}</div>}
                        </div>
                      </div>
                  ))}
                </div>
              </div>
          )}

          {/* ── Recensioni ── */}
          <div>
            {/* Fix (Profilo — "tornato il problema", la causa vera): l'header
                mostrava reviews.length, cioè quante recensioni sono caricate
                finora in pagina (10, poi 20 dopo un click su "Carica altre") —
                un numero che CAMBIA mentre carichi, invece del totale reale.
                Il fix precedente aveva tolto il contatore statico in alto
                pensando risolvesse la confusione tra due numeri diversi, ma il
                problema vero era proprio questo: il numero qui doveva essere
                il totale reale fin da subito, non "quanti ne ho scaricati
                finora". Stessa causa, stesso fix già fatto in Dettaglio
                Film/Serie per "Recensioni della community (N)". */}
            <h2 style={{ color: 'var(--text)', fontSize: '18px', fontWeight: '700', marginBottom: '20px' }}>
              🎬 Recensioni
              <span style={{ color: 'var(--text-dark)', fontSize: '14px', fontWeight: '400', marginLeft: '8px' }}>
              {profile.totalReviews ?? reviews.length}
            </span>
            </h2>

            {reviews.length === 0 ? (
                <p style={{ color: 'var(--text-dark)', fontSize: '14px' }}>Nessuna recensione ancora.</p>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--border)' }}>
                  {reviews.map((r, idx) => (
                      <Link
                          to={`/movie/${r.tmdbId}?type=${r.contentType || r.mediaType}`}
                          key={r.id}
                          style={{ textDecoration: 'none' }}
                      >
                        <div
                            style={{
                              display: 'flex', alignItems: 'flex-start', gap: '16px',
                              padding: '16px 20px',
                              backgroundColor: 'var(--bg-card)',
                              borderBottom: idx < reviews.length - 1 ? '1px solid var(--border)' : 'none',
                              transition: 'background-color 0.15s',
                            }}
                            onMouseEnter={e => e.currentTarget.style.backgroundColor = 'var(--bg-hover)'}
                            onMouseLeave={e => e.currentTarget.style.backgroundColor = 'var(--bg-card)'}
                        >
                          {/* Poster */}
                          <div style={{
                            width: '40px', height: '56px', borderRadius: '4px', flexShrink: 0,
                            backgroundColor: 'var(--bg-hover)', border: '1px solid var(--border)',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            fontSize: '20px', overflow: 'hidden',
                          }}>
                            {r.posterPath
                                ? <img src={`https://image.tmdb.org/t/p/w92${r.posterPath}`} alt=""
                                       style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                : '🎬'
                            }
                          </div>

                          {/* Contenuto */}
                          <div style={{ flex: 1, minWidth: 0 }}>
                            <div style={{ display: 'flex', alignItems: 'baseline', gap: '10px', marginBottom: '4px', flexWrap: 'wrap' }}>
                        <span style={{ color: 'var(--text)', fontWeight: '600', fontSize: '15px' }}>
                          {r.title || `#${r.tmdbId}`}
                        </span>
                              <span style={{ color: 'var(--text-dark)', fontSize: '12px' }}>
                          {new Date(r.createdAt).toLocaleDateString('it-IT')}
                        </span>
                            </div>

                            {/* Fix (Profilo — emoji nel rating, trovata dopo diversi giri):
                                non era la sezione "In Visione" ma questa — 5 clapperboard 🎬
                                come "stelle" più una faccina in base al voto, mentre ovunque
                                nel resto dell'app (Home, Admin, Wrapped) il voto si mostra con
                                ★/☆ semplici. Allineato allo stesso stile, tolta la faccina. */}
                            <div style={{ display: 'flex', gap: '2px', marginBottom: r.text ? '6px' : '0', alignItems: 'center' }}>
                              <span style={{ color: 'var(--gold)', fontSize: '13px' }}>
                                {'★'.repeat(Math.max(0, Math.min(5, Math.round(r.rating || 0))))}{'☆'.repeat(5 - Math.max(0, Math.min(5, Math.round(r.rating || 0))))}
                              </span>
                            </div>

                            {/* Testo — max 2 righe, gestisce testi lunghi */}
                            {r.text && (
                                <p style={{
                                  color: 'var(--text-muted)', fontSize: '13px', lineHeight: 1.55,
                                  margin: 0,
                                  display: '-webkit-box',
                                  WebkitLineClamp: 2,
                                  WebkitBoxOrient: 'vertical',
                                  overflow: 'hidden',
                                  wordBreak: 'break-word',
                                }}>
                                  {r.text}
                                </p>
                            )}
                          </div>

                          <span style={{ color: 'var(--border-soft)', fontSize: '18px', flexShrink: 0, alignSelf: 'center' }}>›</span>
                        </div>
                      </Link>
                  ))}
                </div>
            )}

            {hasMoreReviews && (
                <button
                    onClick={loadMoreReviews}
                    disabled={loadingMore}
                    style={{
                      marginTop: '12px', width: '100%', padding: '12px',
                      backgroundColor: 'transparent', border: '1px solid var(--border)',
                      borderRadius: '8px', color: 'var(--text-dark)', fontSize: '14px',
                      cursor: loadingMore ? 'default' : 'pointer',
                    }}
                >
                  {loadingMore ? 'Caricamento...' : 'Carica altre recensioni'}
                </button>
            )}
          </div>
          </div>
        </div>
      </div>
  )
}

export default ProfilePage