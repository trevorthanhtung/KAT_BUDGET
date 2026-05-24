import { Delete, Lock } from 'lucide-react'
import type { Dispatch, SetStateAction } from 'react'
import type { AppLanguage } from '../types'

type PinLockScreenProps = {
  lang: AppLanguage
  pin: string
  pinInput: string
  setPinInput: Dispatch<SetStateAction<string>>
  onUnlock: () => void
}

export function PinLockScreen({ lang, pin, pinInput, setPinInput, onUnlock }: PinLockScreenProps) {
  const appendDigit = (digit: string) => {
    if (pinInput.length >= 4) return

    const newValue = pinInput + digit
    setPinInput(newValue)
    if (newValue.length !== 4) return

    window.setTimeout(() => {
      if (newValue === pin) {
        onUnlock()
      } else {
        alert(lang === 'vi' ? 'Mã PIN sai!' : 'Incorrect PIN!')
        setPinInput('')
      }
    }, 150)
  }

  return (
    <main className="app-shell" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: 'var(--bg)' }}>
      <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: 'rgba(76, 175, 80, 0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '24px' }}>
        <Lock size={40} color="var(--accent)" />
      </div>
      <h2 style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '8px' }}>{lang === 'vi' ? 'Nhập mã PIN' : 'Enter PIN'}</h2>
      <p style={{ fontSize: '14px', color: 'var(--text-muted)', marginBottom: '40px' }}>
        {lang === 'vi' ? 'Vui lòng nhập mã PIN để mở khóa' : 'Please enter your PIN to unlock'}
      </p>

      <div style={{ display: 'flex', gap: '16px', marginBottom: '48px' }}>
        {[0, 1, 2, 3].map(i => (
          <div
            key={i}
            style={{
              width: '16px',
              height: '16px',
              borderRadius: '50%',
              background: i < pinInput.length ? 'var(--accent)' : 'var(--surface)',
              border: i < pinInput.length ? 'none' : '1px solid var(--border)',
            }}
          />
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', maxWidth: '280px', width: '100%' }}>
        {[1, 2, 3, 4, 5, 6, 7, 8, 9].map(num => (
          <button key={num} className="numpad-key" style={{ width: '72px', height: '72px', borderRadius: '50%', background: 'var(--surface)', fontSize: '28px', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', justifySelf: 'center', color: 'var(--text)' }} onClick={() => appendDigit(num.toString())}>
            {num}
          </button>
        ))}
        <div />
        <button className="numpad-key" style={{ width: '72px', height: '72px', borderRadius: '50%', background: 'var(--surface)', fontSize: '28px', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', justifySelf: 'center', color: 'var(--text)' }} onClick={() => appendDigit('0')}>
          0
        </button>
        <button className="numpad-key" style={{ width: '72px', height: '72px', borderRadius: '50%', background: 'transparent', fontSize: '28px', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', justifySelf: 'center', color: 'var(--text)' }} onClick={() => setPinInput(value => value.slice(0, -1))}>
          <Delete size={28} />
        </button>
      </div>
    </main>
  )
}
