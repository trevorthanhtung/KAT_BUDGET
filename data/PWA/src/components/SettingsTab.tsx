import {
  PiggyBank,
} from 'lucide-react'
import type { ChangeEvent, ReactNode, RefObject } from 'react'
import type { ActiveTool, AppLanguage } from '../types'
import { SUPPORTED_CURRENCIES } from '../utils/constants'

type ThemeMode = 'system' | 'light' | 'dark'

type SettingsTabProps = {
  lang: AppLanguage
  currency: string
  theme: ThemeMode
  hasPin: boolean
  fileInputRef: RefObject<HTMLInputElement | null>
  onCurrencyChange: (currency: string) => void
  onThemeChange: (theme: ThemeMode) => void
  onOpenTool: (tool: ActiveTool) => void
  onOpenCsv: () => void
  onExportBackup: () => void
  onPickImportFile: () => void
  onImportBackup: (event: ChangeEvent<HTMLInputElement>) => void
  onPinEnabled: () => void
  onPinDisabled: () => void
  onSupport: () => void
  onAbout: () => void
  onDonate: () => void
}

type SettingsRowProps = {
  title: string
  subtitle: string
  onClick: () => void
  enabled?: boolean
}

type SettingsSwitchRowProps = {
  title: string
  subtitle: string
  checked: boolean
  onCheckedChange: (checked: boolean) => void
  enabled?: boolean
}

function copy(lang: AppLanguage) {
  const vi = lang === 'vi'

  return {
    currency: vi ? 'Đơn vị tiền tệ mặc định' : 'Default Currency',
    tools: vi ? 'Công cụ tài chính' : 'Financial Tools',
    budgetTitle: vi ? 'Ngân sách hằng tháng' : 'Monthly Budget',
    budgetSub: vi ? 'Theo dõi hạn mức chi theo danh mục' : 'Track category spending limits',
    recurringTitle: vi ? 'Giao dịch định kỳ' : 'Recurring Transactions',
    recurringSub: vi ? 'Tự động ghi nhận các khoản thu chi định kỳ' : 'Automatically record recurring income and expenses',
    goalsTitle: vi ? 'Mục tiêu tiết kiệm' : 'Saving Goals',
    goalsSub: vi ? 'Theo dõi tiến độ tích lũy cho từng mục tiêu' : 'Track progress toward each goal',
    ratesTitle: vi ? 'Tỷ giá quy đổi' : 'Exchange Rates',
    ratesSub: vi ? 'Xem tỷ giá ngoại tệ đang áp dụng' : 'View active exchange rates',
    csvTitle: vi ? 'Xuất báo cáo CSV' : 'Export CSV Report',
    csvSub: vi ? 'Tạo báo cáo giao dịch dạng CSV' : 'Create a CSV transaction report',
    security: vi ? 'Hệ thống & Bảo mật' : 'System & Security',
    lockTitle: vi ? 'Khóa ứng dụng' : 'App Lock',
    lockSub: vi ? 'Bảo vệ ứng dụng bằng mã PIN' : 'Protect the app with a PIN',
    themeTitle: vi ? 'Giao diện tối' : 'Dark Theme',
    themeSub: vi ? 'Chuyển đổi chế độ hiển thị của ứng dụng' : 'Switch the app display mode',
    sync: vi ? 'Đồng bộ & Sao lưu' : 'Sync & Backup',
    manualBackupTitle: vi ? 'Sao lưu thủ công' : 'Manual Backup',
    manualBackupSub: vi ? 'Xuất dữ liệu hiện tại thành file sao lưu' : 'Export current data to a backup file',
    restoreTitle: vi ? 'Khôi phục dữ liệu' : 'Restore Data',
    restoreSub: vi ? 'Nhập dữ liệu từ file sao lưu' : 'Import data from a backup file',
    info: vi ? 'Thông tin & Hỗ trợ' : 'Info & Support',
    supportTitle: vi ? 'Hỗ trợ' : 'Support',
    supportSub: vi ? 'Liên hệ khi cần hỗ trợ hoặc báo lỗi' : 'Contact support or report an issue',
    aboutTitle: vi ? 'Giới thiệu' : 'About',
    aboutSub: vi ? 'Thông tin phiên bản và nhà phát triển' : 'Version and developer information',
    donateTitle: vi ? 'Ủng hộ nhà phát triển' : 'Support Development',
    donateSub: vi ? 'Hỗ trợ quá trình duy trì và cải thiện ứng dụng' : 'Support ongoing development and improvements',
  }
}

function SettingsSectionTitle({ children }: { children: ReactNode }) {
  return <div className="settings-header">{children}</div>
}

function SettingsGroup({ children }: { children: ReactNode }) {
  return <div className="settings-card">{children}</div>
}

function SettingsRow({ title, subtitle, onClick, enabled = true }: SettingsRowProps) {
  return (
    <button className="settings-list-item" type="button" onClick={onClick} disabled={!enabled}>
      <span className="settings-list-content">
        <span className="settings-list-title">{title}</span>
        <span className="settings-list-desc">{subtitle}</span>
      </span>
    </button>
  )
}

function SettingsSwitchRow({
  title,
  subtitle,
  checked,
  onCheckedChange,
  enabled = true,
}: SettingsSwitchRowProps) {
  return (
    <div className={`settings-list-item ${enabled ? '' : 'disabled'}`}>
      <span className="settings-list-content">
        <span className="settings-list-title">{title}</span>
        <span className="settings-list-desc">{subtitle}</span>
      </span>
      <label className="kat-switch">
        <input
          type="checkbox"
          checked={checked}
          disabled={!enabled}
          onChange={(event) => onCheckedChange(event.target.checked)}
        />
        <span className="kat-slider" />
      </label>
    </div>
  )
}

export function SettingsTab({
  lang,
  currency,
  theme,
  hasPin,
  fileInputRef,
  onCurrencyChange,
  onThemeChange,
  onOpenTool,
  onOpenCsv,
  onExportBackup,
  onPickImportFile,
  onImportBackup,
  onPinEnabled,
  onPinDisabled,
  onSupport,
  onAbout,
  onDonate,
}: SettingsTabProps) {
  const text = copy(lang)
  const isDarkTheme = theme === 'dark' || (
    theme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches
  )

  return (
    <section className="settings-page">
      <SettingsSectionTitle>{text.currency}</SettingsSectionTitle>
      <div className="currency-pills" aria-label={text.currency}>
        {SUPPORTED_CURRENCIES.map((curr) => (
          <button
            key={curr}
            type="button"
            className={`currency-pill ${currency === curr ? 'active' : ''}`}
            onClick={() => onCurrencyChange(curr)}
          >
            {curr}
          </button>
        ))}
      </div>

      <SettingsSectionTitle>{text.tools}</SettingsSectionTitle>
      <SettingsGroup>
        <SettingsRow title={text.budgetTitle} subtitle={text.budgetSub} onClick={() => onOpenTool('BUDGET')} />
        <SettingsRow title={text.recurringTitle} subtitle={text.recurringSub} onClick={() => onOpenTool('RECURRING')} />
        <SettingsRow title={text.goalsTitle} subtitle={text.goalsSub} onClick={() => onOpenTool('GOAL')} />
        <SettingsRow title={text.ratesTitle} subtitle={text.ratesSub} onClick={() => onOpenTool('EXCHANGE_RATE')} />
        <SettingsRow title={text.csvTitle} subtitle={text.csvSub} onClick={onOpenCsv} />
      </SettingsGroup>

      <SettingsSectionTitle>{text.security}</SettingsSectionTitle>
      <SettingsGroup>
        <SettingsSwitchRow
          title={text.lockTitle}
          subtitle={text.lockSub}
          checked={hasPin}
          onCheckedChange={(checked) => checked ? onPinEnabled() : onPinDisabled()}
        />
        <SettingsSwitchRow
          title={text.themeTitle}
          subtitle={text.themeSub}
          checked={isDarkTheme}
          onCheckedChange={(checked) => onThemeChange(checked ? 'dark' : 'light')}
        />
      </SettingsGroup>

      <SettingsSectionTitle>{text.sync}</SettingsSectionTitle>
      <SettingsGroup>
        <SettingsRow title={text.manualBackupTitle} subtitle={text.manualBackupSub} onClick={onExportBackup} />
        <SettingsRow title={text.restoreTitle} subtitle={text.restoreSub} onClick={onPickImportFile} />
        <input
          ref={fileInputRef}
          type="file"
          className="visually-hidden"
          accept=".json,.kat,application/json,text/plain"
          onChange={onImportBackup}
        />
      </SettingsGroup>

      <SettingsSectionTitle>{text.info}</SettingsSectionTitle>
      <SettingsGroup>
        <SettingsRow title={text.supportTitle} subtitle={text.supportSub} onClick={onSupport} />
        <SettingsRow title={text.aboutTitle} subtitle={text.aboutSub} onClick={onAbout} />
        <SettingsRow title={text.donateTitle} subtitle={text.donateSub} onClick={onDonate} />
      </SettingsGroup>

      <div className="settings-version-note">
        <span>KAT Budget PWA</span>
        <PiggyBank size={15} />
      </div>
    </section>
  )
}
