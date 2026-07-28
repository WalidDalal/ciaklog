import Navbar from '../components/Navbar'

// Fix (🔴 attribuzione TMDB — versione completa): i Termini d'Uso dell'API
// TMDB richiedono 3 cose che il fix precedente (solo un testo in fondo alla
// pagina Dettaglio + un accenno nel footer di Homepage) non copriva del
// tutto:
//   1. Il LOGO TMDB, non solo testo — "You must use the TMDB logo to
//      identify Your use of TMDB, the TMDB APIs, or TMDB Content."
//   2. L'attribuzione deve stare in una sezione tipo "About" o "Crediti"
//      dedicata dell'applicazione — non un punto qualsiasi.
//   3. La dicitura esatta, in modo prominente (vedi sotto).
// Questa pagina è quella sezione dedicata. Il logo va scaricato a mano da
// https://www.themoviedb.org/about/logos-attribution (uno dei formati SVG
// ufficiali, es. "Primary Full (Blue)") e salvato come
// frontend/public/tmdb-logo.svg — non l'ho potuto scaricare io: il dominio
// themoviedb.org non è raggiungibile dal mio ambiente, ed è comunque un
// asset di loro proprietà da prendere dalla fonte ufficiale, non da
// ricostruire. Finché il file non c'è, l'<img> sotto mostra solo lo spazio
// vuoto dell'alt text — non blocca nient'altro.
function CreditsPage() {
    return (
        <div style={{ backgroundColor: 'var(--bg)', minHeight: '100vh' }}>
            <Navbar />
            <div style={{ maxWidth: '640px', margin: '0 auto', padding: '48px 24px' }}>
                <h1 style={{ color: 'var(--text)', fontSize: '26px', fontWeight: '800', marginBottom: '8px' }}>
                    Crediti
                </h1>
                <p style={{ color: 'var(--text-muted)', fontSize: '14px', marginBottom: '40px' }}>
                    Dati e librerie di terze parti usati da CiakLog.
                </p>

                <div style={{
                    backgroundColor: 'var(--bg-nav)', border: '1px solid var(--border-soft)',
                    borderRadius: '14px', padding: '28px', display: 'flex', flexDirection: 'column',
                    alignItems: 'flex-start', gap: '16px',
                }}>
                    {/* Logo TMDB — deve essere MENO prominente del logo/brand di CiakLog
                        stesso, per questo dimensioni contenute qui e non nell'header
                        principale dell'app. */}
                    <img
                        src="/tmdb-logo.svg"
                        alt="The Movie Database (TMDB)"
                        style={{ height: '32px', maxWidth: '220px', objectFit: 'contain' }}
                        onError={e => { e.currentTarget.style.display = 'none' }}
                    />

                    <p style={{ color: 'var(--text)', fontSize: '14px', lineHeight: 1.6, margin: 0 }}>
                        Tutti i dati su film e serie TV mostrati in CiakLog — titoli, trame, poster,
                        cast, valutazioni — provengono da{' '}
                        <a href="https://www.themoviedb.org/" target="_blank" rel="noopener noreferrer" style={{ color: 'var(--accent)' }}>
                            The Movie Database (TMDB)
                        </a>.
                    </p>

                    <p style={{ color: 'var(--text-muted)', fontSize: '13px', lineHeight: 1.6, margin: 0, fontStyle: 'italic' }}>
                        This product uses TMDB and the TMDB APIs but is not endorsed, certified, or otherwise approved by TMDB.
                    </p>

                    <a
                        href="https://www.themoviedb.org/api-terms-of-use"
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{ color: 'var(--text-dark)', fontSize: '12px' }}
                    >
                        Termini d'uso dell'API TMDB →
                    </a>
                </div>
            </div>
        </div>
    )
}

export default CreditsPage
