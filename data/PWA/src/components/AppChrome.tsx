import { BarChart3, Plus, Settings, Users, Wallet, X } from 'lucide-react'
import type { ActiveTool, AppLanguage, AppTab } from '../types'

type AppHeaderProps = {
  activeTab: AppTab
  activeTool: ActiveTool
  lang: AppLanguage
  onBackTool: () => void
  onToggleLang: () => void
}

function toolTitle(activeTool: ActiveTool, lang: AppLanguage): string {
  if (activeTool === 'BUDGET') return lang === 'vi' ? 'Ngân sách hằng tháng' : 'Monthly Budget'
  if (activeTool === 'RECURRING') return lang === 'vi' ? 'Giao dịch định kỳ' : 'Recurring Transactions'
  if (activeTool === 'GOAL') return lang === 'vi' ? 'Mục tiêu tiết kiệm' : 'Saving Goals'
  if (activeTool === 'EXCHANGE_RATE') return lang === 'vi' ? 'Tỷ giá quy đổi' : 'Exchange Rates'
  if (activeTool === 'CATEGORY') return lang === 'vi' ? 'Quản lý danh mục' : 'Categories'
  return ''
}

function tabTitle(activeTab: AppTab, lang: AppLanguage): string {
  if (activeTab === 'DASHBOARD') return lang === 'vi' ? 'Tổng quan tài chính' : 'Financial Overview'
  if (activeTab === 'DEBTS') return lang === 'vi' ? 'Theo dõi vay nợ' : 'Debt tracking'
  if (activeTab === 'REPORTS') return lang === 'vi' ? 'Báo cáo tài chính' : 'Financial Reports'
  return lang === 'vi' ? 'Cài đặt' : 'Settings'
}

export function AppHeader({ activeTab, activeTool, lang, onBackTool, onToggleLang }: AppHeaderProps) {
  if (activeTool !== 'NONE') {
    return (
      <header className="topbar" style={{ display: 'grid', gridTemplateColumns: 'auto minmax(0, 1fr) 44px', alignItems: 'center' }}>
        <button
          type="button"
          onClick={onBackTool}
          style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'none', border: 'none', color: 'var(--accent)', fontWeight: 600, fontSize: '16px', cursor: 'pointer', padding: 0 }}
        >
          &larr; {lang === 'vi' ? 'Quay lại' : 'Back'}
        </button>
        <h1 style={{ fontSize: 24, lineHeight: 1.15 }}>{toolTitle(activeTool, lang)}</h1>
        <button
          type="button"
          onClick={onToggleLang}
          style={{ width: '44px', height: '40px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', padding: 0, borderRadius: '20px', border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)', cursor: 'pointer' }}
        >
          {lang === 'vi' ? 'VI' : 'EN'}
        </button>
      </header>
    )
  }

  if (activeTab === 'DASHBOARD') {
    return (
      <header className="topbar" style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 44px', alignItems: 'start' }}>
        <div style={{ display: 'flex', flexDirection: 'column' }}>
          <p className="eyebrow" style={{ margin: '0 0 4px 0', fontSize: '14px', color: 'var(--muted)', textTransform: 'none', fontWeight: 'normal' }}>
            {lang === 'vi' ? 'Chào bạn!' : 'Hi there!'}
          </p>
          <h1 style={{ fontSize: 'clamp(24px, 7vw, 28px)', margin: 0 }}>{tabTitle(activeTab, lang)}</h1>
        </div>
        <button
          type="button"
          onClick={onToggleLang}
          style={{ width: '44px', height: '40px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', padding: 0, borderRadius: '20px', border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)', cursor: 'pointer' }}
        >
          {lang === 'vi' ? 'VI' : 'EN'}
        </button>
      </header>
    )
  }

  return (
    <header className="topbar" style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 44px', alignItems: 'start', paddingTop: '16px' }}>
      <div style={{ display: 'flex', flexDirection: 'column' }}>
        <h1 style={{ fontSize: '28px', margin: 0 }}>{tabTitle(activeTab, lang)}</h1>
      </div>
      <button
        type="button"
        onClick={onToggleLang}
        style={{ fontWeight: 'bold', width: '44px', height: '40px', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '20px', border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)', cursor: 'pointer', padding: 0 }}
      >
        {lang === 'vi' ? 'VI' : 'EN'}
      </button>
    </header>
  )
}

type BottomNavProps = {
  activeTab: AppTab
  activeTool: ActiveTool
  lang: AppLanguage
  onAddTransaction: () => void
  onTabChange: (tab: AppTab) => void
}

export function BottomNav({ activeTab, activeTool, lang, onAddTransaction, onTabChange }: BottomNavProps) {
  const isMainTab = activeTool === 'NONE'

  return (
    <nav className="bottom-nav" aria-label="Dieu huong chinh">
      <button className={activeTab === 'DASHBOARD' && isMainTab ? 'active' : ''} onClick={() => onTabChange('DASHBOARD')} type="button">
        <Wallet size={20} fill={activeTab === 'DASHBOARD' && isMainTab ? 'currentColor' : 'none'} />
        {lang === 'vi' ? 'Tổng quan' : 'Overview'}
      </button>
      <button className={activeTab === 'DEBTS' && isMainTab ? 'active' : ''} onClick={() => onTabChange('DEBTS')} type="button">
        <Users size={20} fill={activeTab === 'DEBTS' && isMainTab ? 'currentColor' : 'none'} />
        {lang === 'vi' ? 'Vay nợ' : 'Debts'}
      </button>
      <div className="fab-container">
        <button className="fab-button" onClick={onAddTransaction} type="button">
          <Plus size={32} color="currentColor" strokeWidth={2.5} />
        </button>
      </div>
      <button className={activeTab === 'REPORTS' && isMainTab ? 'active' : ''} onClick={() => onTabChange('REPORTS')} type="button">
        <BarChart3 size={20} fill={activeTab === 'REPORTS' && isMainTab ? 'currentColor' : 'none'} />
        {lang === 'vi' ? 'Báo cáo' : 'Reports'}
      </button>
      <button className={activeTab === 'SETTINGS' || !isMainTab ? 'active' : ''} onClick={() => onTabChange('SETTINGS')} type="button">
        <Settings size={20} fill={activeTab === 'SETTINGS' || !isMainTab ? 'currentColor' : 'none'} />
        {lang === 'vi' ? 'Cài đặt' : 'Settings'}
      </button>
    </nav>
  )
}

type PwaInstallBannerProps = {
  lang: AppLanguage
  onDismiss: () => void
}

export function PwaInstallBanner({ lang, onDismiss }: PwaInstallBannerProps) {
  return (
    <div style={{ position: 'fixed', bottom: 84, left: 16, right: 16, background: 'var(--accent)', color: 'white', padding: '12px 16px', borderRadius: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', zIndex: 50, boxShadow: 'var(--shadow)' }}>
      <div style={{ fontSize: 13, lineHeight: 1.4 }}>
        <strong>{lang === 'vi' ? 'Cài đặt KAT Budget' : 'Install KAT Budget'}</strong><br />
        {lang === 'vi' ? 'Bấm Chia sẻ -> Thêm vào MH chính để dùng như App.' : 'Tap Share -> Add to Home Screen to use as App.'}
      </div>
      <button onClick={onDismiss} style={{ background: 'none', border: 'none', color: 'white' }}>
        <X size={20} />
      </button>
    </div>
  )
}
