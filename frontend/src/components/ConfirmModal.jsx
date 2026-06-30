function ConfirmModal({ message, onConfirm, onCancel, confirmLabel = 'Conferma', danger = true }) {
  return (
    <>
      <div
        onClick={onCancel}
        style={{ position: 'fixed', inset: 0, backgroundColor: 'var(--bg-modal)', zIndex: 500 }}
      />
      <div style={{
        position: 'fixed', top: '50%', left: '50%',
        transform: 'translate(-50%, -50%)',
        backgroundColor: 'var(--bg-card)', border: '1px solid #333',
        borderRadius: '12px', padding: '28px 32px',
        zIndex: 501, minWidth: '320px', maxWidth: '440px',
        textAlign: 'center',
      }}>
        <p style={{ color: 'var(--text)', fontSize: '16px', marginBottom: '24px', lineHeight: 1.5 }}>{message}</p>
        <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
          <button
            onClick={onCancel}
            style={{
              padding: '10px 24px', borderRadius: '8px',
              border: '1px solid #444', backgroundColor: 'transparent',
              color: 'var(--text)', fontSize: '14px', cursor: 'pointer',
            }}
          >
            Annulla
          </button>
          <button
            onClick={onConfirm}
            style={{
              padding: '10px 24px', borderRadius: '8px',
              border: 'none',
              backgroundColor: danger ? 'var(--accent)' : '#22c55e',
              color: 'var(--text)', fontSize: '14px', fontWeight: '600', cursor: 'pointer',
            }}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </>
  )
}

export default ConfirmModal
