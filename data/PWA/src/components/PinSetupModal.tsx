import type { AppLanguage } from '../types'

type PinSetupModalProps = {
  lang: AppLanguage
  pinValue: string
  onPinValueChange: (value: string) => void
  onDismiss: () => void
  onSave: (value: string) => void
}

export function PinSetupModal({ lang, pinValue, onPinValueChange, onDismiss, onSave }: PinSetupModalProps) {
  return (
    <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 100 }}>
      <div className="modal-card" style={{ padding: '24px', textAlign: 'center', maxWidth: '320px' }}>
        <h3 style={{ fontSize: '22px', fontWeight: 'bold', marginBottom: '24px' }}>{lang === 'vi' ? 'Thiết lập mã PIN' : 'Setup PIN'}</h3>

        <div style={{ position: 'relative', marginBottom: '24px', textAlign: 'left' }}>
          <label style={{ position: 'absolute', top: '-10px', left: '16px', background: 'var(--surface)', padding: '0 4px', fontSize: '13px', color: 'var(--text-muted)' }}>
            {lang === 'vi' ? 'Nhập 4 chữ số' : 'Enter 4 digits'}
          </label>
          <input
            type="text"
            inputMode="numeric"
            maxLength={4}
            value={pinValue}
            onChange={(event) => {
              const value = event.target.value.replace(/\D/g, '')
              if (value.length <= 4) onPinValueChange(value)
            }}
            style={{ width: '100%', padding: '16px', fontSize: '24px', letterSpacing: '12px', textAlign: 'center', border: '2px solid var(--accent)', borderRadius: '16px', background: 'transparent', color: 'var(--text)' }}
          />
        </div>

        <button
          className="primary-button"
          disabled={pinValue.length !== 4}
          style={{ width: '100%', padding: '14px', fontSize: '16px', borderRadius: '24px', marginBottom: '12px', opacity: pinValue.length !== 4 ? 0.5 : 1 }}
          onClick={() => {
            if (pinValue.length === 4) onSave(pinValue)
          }}
        >
          {lang === 'vi' ? 'Lưu' : 'Save'}
        </button>
        <button
          style={{ width: '100%', padding: '14px', fontSize: '16px', borderRadius: '24px', background: 'var(--bg)', border: 'none', color: 'var(--text)', fontWeight: 'bold' }}
          onClick={onDismiss}
        >
          {lang === 'vi' ? 'Đóng' : 'Close'}
        </button>
      </div>
    </div>
  )
}
