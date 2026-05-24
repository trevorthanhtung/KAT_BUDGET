import type { AppLanguage } from '../types'

export type ExchangeRateRow = {
  CurrencyCode: string
  CurrencyName: string
  Buy: string
  Transfer: string
  Sell: string
}

type ExchangeRateDialogProps = {
  lang: AppLanguage
  rates: ExchangeRateRow[]
  loading: boolean
  onClose: () => void
}

export function ExchangeRateDialog({ lang, rates, loading, onClose }: ExchangeRateDialogProps) {
  return (
    <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 100 }}>
      <div className="modal-card exchange-rate-card" style={{ padding: '24px', textAlign: 'center', width: '90%', maxWidth: '400px' }}>
        <h3 style={{ fontSize: '20px', marginBottom: '8px' }}>{lang === 'vi' ? 'Tỷ giá ngoại tệ' : 'Exchange Rates'}</h3>
        <p style={{ fontSize: '14px', color: 'var(--text-muted)', marginBottom: '24px' }}>
          {lang === 'vi' ? 'Tỷ giá tham chiếu quy đổi sang VND.' : 'Reference exchange rates to VND.'}
        </p>

        <div className="exchange-rate-list" style={{ maxHeight: '50vh', overflowY: 'auto', marginBottom: '20px', paddingRight: '4px' }}>
          {loading && <p>{lang === 'vi' ? 'Đang tải...' : 'Loading...'}</p>}
          {!loading && rates.map((rate, index) => {
            const valueText = rate.Transfer || rate.Buy || '0'
            const parsed = Number.parseFloat(valueText.replace(/,/g, ''))
            const formattedRate = Number.isNaN(parsed) ? valueText : new Intl.NumberFormat('vi-VN').format(parsed)

            return (
              <div key={`${rate.CurrencyCode}-${index}`} className="exchange-rate-row" style={{ display: 'flex', alignItems: 'center', padding: '12px 0', borderBottom: '1px solid var(--border)' }}>
                <div style={{ width: '36px', height: '36px', borderRadius: '50%', background: '#E8F5E9', color: 'var(--accent)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', marginRight: '16px', flexShrink: 0 }}>
                  {rate.CurrencyCode[0]}
                </div>
                <strong style={{ fontSize: '16px', marginRight: 'auto' }}>1 {rate.CurrencyCode}</strong>
                <span className="exchange-rate-value" style={{ fontSize: '16px', fontWeight: '500', color: 'var(--accent)' }}>= {formattedRate} VND</span>
              </div>
            )
          })}
          {!loading && rates.length === 0 && (
            <p className="empty-note">{lang === 'vi' ? 'Không thể lấy dữ liệu tỷ giá.' : 'Cannot fetch exchange rates.'}</p>
          )}
        </div>

        <p style={{ fontSize: '12px', fontStyle: 'italic', color: 'var(--text-muted)', textAlign: 'left', marginBottom: '16px' }}>
          * {lang === 'vi' ? 'Nguồn dữ liệu: Ngân hàng Vietcombank' : 'Data source: Vietcombank'}
        </p>

        <button
          type="button"
          className="primary-button"
          style={{ width: '100%', padding: '14px', fontSize: '16px', borderRadius: '24px' }}
          onClick={onClose}
        >
          {lang === 'vi' ? 'Đóng' : 'Close'}
        </button>
      </div>
    </div>
  )
}
