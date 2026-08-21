import Navbar from '../components/Navbar'

// Sezione "Crediti" dedicata, richiesta dai Termini d'Uso di TMDB (logo +
// dicitura in una sezione apposita, non solo testo sparso in pagina).
// Il logo va scaricato a mano da
// https://www.themoviedb.org/about/logos-attribution (formato SVG
// ufficiale, es. "Primary Full (Blue)") e salvato come
// frontend/public/tmdb-logo.svg — finché manca, l'<img> sotto mostra solo
// lo spazio vuoto dell'alt text, non blocca nient'altro.
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
