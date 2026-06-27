import { Link } from 'react-router-dom'

function HeroSection() {
  return (
    <section style={{
      position: 'relative',
      minHeight: '520px',
      display: 'flex',
      alignItems: 'center',
      overflow: 'hidden',
      backgroundColor: '#0a0a0a',
    }}>
      {/* Poster sfondo */}
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
        {[
          'https://image.tmdb.org/t/p/w300/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg',
          'https://image.tmdb.org/t/p/w300/qNBAXBIQlnOThrVvA6mA2B5ggV6.jpg',
          'https://image.tmdb.org/t/p/w300/tmU7GeKVybMWFButWEGl2M4GeiP.jpg',
          'https://image.tmdb.org/t/p/w300/3bhkrj58Vtu7enYsRolD1fZdja1.jpg',
        ].map((url, i) => (
          <img
            key={i}
            src={url}
            alt=""
            style={{
              height: '100%',
              width: '160px',
              objectFit: 'cover',
              flexShrink: 0,
            }}
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
      <div style={{
        position: 'relative',
        zIndex: 2,
        padding: '0 64px',
        maxWidth: '600px',
      }}>
        <h1 style={{
          fontSize: '48px',
          fontWeight: '800',
          lineHeight: '1.1',
          color: 'white',
          marginBottom: '16px',
        }}>
          Il tuo diario<br />
          cinematografico,<br />
          <span style={{ color: '#e50914' }}>con un tocco di AI</span>
        </h1>
        <p style={{
          fontSize: '16px',
          color: '#9ca3af',
          marginBottom: '32px',
          lineHeight: '1.6',
        }}>
          Traccia ciò che guardi, scopri cosa pensa la community,
          ricevi consigli su misura.
        </p>
        <div style={{ display: 'flex', gap: '16px' }}>
          <Link to="/register">
            <button style={{
              padding: '14px 32px',
              backgroundColor: '#e50914',
              border: 'none',
              borderRadius: '8px',
              color: 'white',
              fontSize: '16px',
              fontWeight: '600',
            }}>
              Inizia ora
            </button>
          </Link>
          <Link to="/">
            <button style={{
              padding: '14px 32px',
              backgroundColor: 'transparent',
              border: '1px solid #555',
              borderRadius: '8px',
              color: 'white',
              fontSize: '16px',
              fontWeight: '600',
            }}>
              Esplora senza registrarti
            </button>
          </Link>
        </div>
      </div>
    </section>
  )
}

export default HeroSection