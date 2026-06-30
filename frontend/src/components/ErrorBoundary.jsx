import { Component } from 'react'

class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, info) {
    console.error('[ErrorBoundary]', error, info.componentStack)
  }

  render() {
    if (!this.state.hasError) return this.props.children

    return (
      <div style={{
        minHeight: '100vh', backgroundColor: 'var(--bg)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        flexDirection: 'column', gap: '16px', padding: '32px', textAlign: 'center',
      }}>
        <div style={{ fontSize: '48px' }}>🎬</div>
        <h1 style={{ color: 'var(--text)', fontSize: '24px', fontWeight: '700' }}>
          Qualcosa è andato storto
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '15px', maxWidth: '400px' }}>
          Si è verificato un errore inatteso. Prova a ricaricare la pagina.
        </p>
        {import.meta.env.DEV && this.state.error && (
          <pre style={{
            backgroundColor: 'var(--bg-card)', border: '1px solid var(--border)',
            borderRadius: '8px', padding: '16px', fontSize: '12px',
            color: 'var(--accent)', textAlign: 'left', maxWidth: '600px',
            overflow: 'auto', maxHeight: '200px',
          }}>
            {this.state.error.toString()}
          </pre>
        )}
        <button
          onClick={() => window.location.href = '/'}
          style={{
            padding: '12px 28px', backgroundColor: 'var(--accent)',
            border: 'none', borderRadius: '8px', color: 'white',
            fontSize: '15px', fontWeight: '600', cursor: 'pointer',
          }}
        >
          Torna alla home
        </button>
      </div>
    )
  }
}

export default ErrorBoundary
