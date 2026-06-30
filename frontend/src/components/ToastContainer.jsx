import { useEffect } from 'react'
import useToastStore from '../store/toastStore'

const TYPE_STYLES = {
    error:   { bg: '#2d1a1a', border: 'var(--accent)', color: '#ff6b6b', icon: '✕' },
    success: { bg: '#0f2d1a', border: '#22c55e', color: '#4ade80', icon: '✓' },
    info:    { bg: '#0f1a2d', border: '#3b82f6', color: '#60a5fa', icon: 'ℹ' },
}

function Toast({ toast, onRemove }) {
    const s = TYPE_STYLES[toast.type] || TYPE_STYLES.error

    useEffect(() => {
        const t = setTimeout(() => onRemove(toast.id), toast.duration)
        return () => clearTimeout(t)
    }, [toast.id, toast.duration, onRemove])

    return (
        <div style={{
            display: 'flex', alignItems: 'center', gap: '10px',
            backgroundColor: s.bg, border: `1px solid ${s.border}`,
            borderRadius: '8px', padding: '12px 16px',
            color: s.color, fontSize: '14px',
            boxShadow: 'var(--shadow)',
            animation: 'slideIn 0.2s ease',
            minWidth: '260px', maxWidth: '380px',
        }}>
            <span style={{ fontWeight: '700', flexShrink: 0 }}>{s.icon}</span>
            <span style={{ flex: 1 }}>{toast.msg}</span>
            <button
                onClick={() => onRemove(toast.id)}
                aria-label="Chiudi notifica"
                style={{ background: 'none', border: 'none', color: s.color, cursor: 'pointer', fontSize: '16px', padding: 0, flexShrink: 0 }}
            >✕</button>
        </div>
    )
}

function ToastContainer() {
    const { toasts, remove } = useToastStore()
    return (
        <div style={{
            position: 'fixed', bottom: '80px', right: '24px',
            zIndex: 9999, display: 'flex', flexDirection: 'column', gap: '10px',
        }}>
            <style>{`@keyframes slideIn { from { opacity:0; transform:translateX(20px) } to { opacity:1; transform:translateX(0) } }`}</style>
            {toasts.map(t => <Toast key={t.id} toast={t} onRemove={remove} />)}
        </div>
    )
}

export default ToastContainer