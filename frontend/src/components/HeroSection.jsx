import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '../services/api'

function HeroSection() {
  const [posters, setPosters] = useState([])

  useEffect(() => {
    api.get('/charts/films').then(r => {
      const paths = (r.data || []).slice(0, 4).map(f => f.posterPath).filter(Boolean)
      setPosters(paths)
    }).catch(() => {})
  }, [])

  return (
    <section style={{
      position: 'relative',
      minHeight: '520px',
      display: 'flex',
      alignItems: 'center',
      overflow: 'hidden',
      backgroundColor: 'var(--bg)',
    }}>
      {/* Poster sfondo dinamici */}
      <div style={{
        position: 'absolute',
        right: 0,
        top: 0,
        bottom: 0,
        width: '60%',
        display: 'flex',
        gap: '8px',
        opacity: 0.3,
        maskImage: 'linear-gradient(to left, rgba(0,0,0,0.8), transparent)',
        WebkitMaskImage: 'linear-gradient(to left, rgba(0,0,0,0.8), transparent)',
        overflow: 'hidden',
      }}>
        {posters.map((path, i) => (
          <img
            key={i}
            src={`https://image.tmdb.org/t/p/w300${path}`}
            alt=""
            style={{ height: '100%', width: '160px', objectFit: 'cover', flexShrink: 0 }}
          />
        ))}
      </div>

      {/* Gradiente sinistro */}
      <div style={{
        position: 'absolute',
        inset: 0,
        background: 'linear-gradient(to right, #0a0a0a 50%, transparent)',
        zIndex: 1,
      }} />

      {/* Contenuto */}
      <div style={{ position: 'relative', zIndex: 2, padding: '0 64px', maxWidth: '600px' }}>
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
          <Link to="/register">
            <button style={{
              padding: '14px 40px', backgroundColor: 'var(--accent)',
              border: 'none', borderRadius: '8px',
              color: 'var(--text)', fontSize: '16px', fontWeight: '700', cursor: 'pointer',
            }}>
              Inizia ora — è gratis
            </button>
          </Link>
          <Link to="/login">
            <button style={{
              padding: '14px 32px', backgroundColor: 'transparent',
              border: '1px solid #555', borderRadius: '8px',
              color: 'var(--text)', fontSize: '16px', cursor: 'pointer',
            }}>
              Ho già un account
            </button>
          </Link>
        </div>
      </div>
    </section>
  )
}

export default HeroSection
