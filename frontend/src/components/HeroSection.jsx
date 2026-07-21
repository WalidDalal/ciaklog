import { Link } from 'react-router-dom'

// Prima questo componente renderizzava una intera <section> con il suo
// proprio sfondo poster + gradiente, annidata dentro la hero già esistente in
// HomePage.jsx (che ha già sfondo poster + gradiente propri). Il risultato
// erano "2 poster in più a sinistra", visibili solo in home guest, perché lo
// sfondo poster di questo componente finiva confinato dentro la colonna di
// testo (maxWidth 620px) della hero esterna. Ora espone solo testo + CTA e
// riusa lo sfondo già presente nella hero di HomePage.
function HeroSection() {
  return (
    <>
      <h1 style={{ fontSize: '48px', fontWeight: '800', lineHeight: '1.1', color: 'var(--text)', marginBottom: '16px' }}>
        Il tuo diario<br />
        cinematografico,<br />
        <span style={{ color: 'var(--accent)' }}>con un tocco di AI</span>
      </h1>
      <p style={{ fontSize: '16px', color: 'var(--text-muted)', marginBottom: '32px', lineHeight: '1.6' }}>
        Traccia ciò che guardi, scopri cosa pensa la community,
        ricevi consigli su misura.
      </p>
      <div style={{ display: 'flex', gap: '16px' }}>
        {/* Fix: CTA semplificata a un solo bottone -> login (che ha già il link alla registrazione) */}
        <Link to="/login">
          <button style={{
            padding: '14px 40px', backgroundColor: 'var(--accent)',
            border: 'none', borderRadius: '8px',
            color: 'var(--text)', fontSize: '16px', fontWeight: '700', cursor: 'pointer',
          }}>
            Inizia ora — è gratis
          </button>
        </Link>
      </div>
    </>
  )
}

export default HeroSection
