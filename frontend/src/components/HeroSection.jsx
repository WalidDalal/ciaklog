import { Link } from 'react-router-dom'

// Solo testo + CTA — lo sfondo poster/gradiente è già gestito dalla hero
// esterna in HomePage.jsx
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
        {/* CTA singola: login (che ha già il link alla registrazione) */}
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
