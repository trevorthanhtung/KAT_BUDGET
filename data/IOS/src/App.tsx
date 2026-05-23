import {
  BarChart3,
  Settings,
  Users,
  Wallet,
  Pencil,
  Trash2,
  Lock,
  X,
  Delete,
  Plus,
} from 'lucide-react'
import { useLiveQuery } from 'dexie-react-hooks'
import { format } from 'date-fns'
import { type ChangeEvent, type FormEvent, useEffect, useRef, useState } from 'react'
import { db, type Budget, type Category, type MoneySource, type Transaction, type Debt, type SavingGoal, type RecurringTransaction } from './data/db'
import * as echarts from 'echarts'
import './App.css'

function EChart({ options, style }: { options: echarts.EChartsCoreOption, style?: React.CSSProperties }) {
  const chartRef = useRef<HTMLDivElement>(null)
  
  useEffect(() => {
    if (!chartRef.current) return
    const chart = echarts.init(chartRef.current)
    chart.setOption(options)
    
    const handleResize = () => chart.resize()
    window.addEventListener('resize', handleResize)
    
    return () => {
      window.removeEventListener('resize', handleResize)
      chart.dispose()
    }
  }, [options])

  return <div ref={chartRef} style={{ width: '100%', height: '300px', ...style }} />
}


type SourceForm = {
  name: string
  type: MoneySource['type']
  includeInTotal: boolean
  initialBalance: string
  interestRate: string
  interestPeriod: string
}

type GoalForm = {
  name: string
  targetAmount: string
  currentAmount: string
  currency: string
}

type RecurringForm = {
  amount: string
  type: 'INCOME' | 'EXPENSE'
  category: string
  note: string
  sourceName: string
  currency: string
  dayOfMonth: string
}

type TxForm = {
  sourceName: string
  type: 'INCOME' | 'EXPENSE' | 'TRANSFER_OUT'
  amount: string
  category: string
  note: string
  currency: string
  timestamp: string
  imageUri: string
  destinationName?: string
}

type CategoryTypeFilter = 'ALL' | 'INCOME' | 'EXPENSE'

type CategoryForm = {
  name: string
  emoji: string
  type: 'INCOME' | 'EXPENSE'
}

type BudgetForm = {
  monthYear: string
  categoryId: string
  limitAmount: string
  currencyCode: string
}

type DebtForm = {
  personName: string
  amount: string
  currency: string
  type: 'DEBT' | 'LOAN'
  note: string
  dueDate: string
}

type BackupPayload = {
  app?: string
  version?: number
  exportedAt?: string
  sources?: MoneySource[]
  transactions?: Transaction[]
  categories?: Category[]
  budgets?: Budget[]
  debts?: Debt[]
  saving_goals?: SavingGoal[]
  recurrings?: RecurringTransaction[]
}

const defaultSourceForm: SourceForm = {
  name: '',
  type: 'CASH',
  includeInTotal: true,
  initialBalance: '',
  interestRate: '',
  interestPeriod: '1 tháng',
}

const defaultDebtForm: DebtForm = {
  personName: '',
  amount: '',
  currency: 'VND',
  type: 'DEBT',
  note: '',
  dueDate: '',
}

const defaultTxForm: TxForm = {
  sourceName: '',
  type: 'EXPENSE',
  amount: '',
  category: '',
  note: '',
  currency: 'VND',
  timestamp: new Date().toISOString().split('T')[0],
  imageUri: '',
  destinationName: '',
}

const defaultCategoryForm: CategoryForm = {
  name: '',
  emoji: '',
  type: 'EXPENSE',
}

const defaultBudgetForm: BudgetForm = {
  monthYear: format(new Date(), 'yyyy-MM'),
  categoryId: '',
  limitAmount: '',
  currencyCode: 'VND',
}

const defaultCategoryPresets: Array<Omit<Category, 'id'>> = [
  { name: 'Luong', emoji: 'PAY', type: 'INCOME' },
  { name: 'Thuong', emoji: 'BONUS', type: 'INCOME' },
  { name: 'Dau tu', emoji: 'INV', type: 'INCOME' },
  { name: 'An uong', emoji: 'FOOD', type: 'EXPENSE' },
  { name: 'Di chuyen', emoji: 'TRAVEL', type: 'EXPENSE' },
  { name: 'Hoa don', emoji: 'BILL', type: 'EXPENSE' },
  { name: 'Mua sam', emoji: 'SHOP', type: 'EXPENSE' },
  { name: 'Suc khoe', emoji: 'HEALTH', type: 'EXPENSE' },
]

const KAT_BACKUP_PREFIX = 'KAT1:'

const SUPPORTED_CURRENCIES = [
  'VND', 'USD', 'EUR', 'GBP', 'JPY',
  'AUD', 'CAD', 'CHF', 'CNY', 'DKK',
  'HKD', 'INR', 'KRW', 'KWD', 'MYR',
  'NOK', 'RUB', 'SAR', 'SEK', 'SGD',
  'THB'
]

function parseAmount(value: string): number {
  const normalized = value.replaceAll(',', '').trim()
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : 0
}

function formatCurrency(amount: number, currency = 'VND'): string {
  return `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(Math.round(amount))} ${currency}`
}

function transactionDelta(tx: Transaction): number {
  if (tx.type === 'INCOME' || tx.type === 'TRANSFER_IN') return tx.amount
  if (tx.type === 'EXPENSE' || tx.type === 'TRANSFER_OUT') return -tx.amount
  return 0
}



async function gzipString(value: string): Promise<Blob> {
  const stream = new Blob([value], { type: 'application/json' }).stream()
  const compressedStream = stream.pipeThrough(new CompressionStream('gzip'))
  return new Response(compressedStream).blob()
}

async function ungzipBlob(blob: Blob): Promise<string> {
  const stream = blob.stream()
  const decompressedStream = stream.pipeThrough(new DecompressionStream('gzip'))
  return new Response(decompressedStream).text()
}

function decodeBase64Utf8(base64Value: string): string {
  const binary = atob(base64Value)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return new TextDecoder().decode(bytes)
}

function App() {
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const sources = useLiveQuery(() => db.sources.orderBy('createdTimestamp').toArray(), []) ?? []
  const transactions = useLiveQuery(() => db.transactions.orderBy('timestamp').reverse().toArray(), []) ?? []
  const categories = useLiveQuery(() => db.categories.orderBy('name').toArray(), []) ?? []
  const budgets = useLiveQuery(() => db.budgets.orderBy('monthYear').reverse().toArray(), []) ?? []
  const debts = useLiveQuery(() => db.debts.orderBy('timestamp').reverse().toArray(), []) ?? []
  const savingGoals = useLiveQuery(() => db.saving_goals.orderBy('name').toArray(), []) ?? []
  const recurrings = useLiveQuery(() => db.recurrings.toArray(), []) ?? []

  const [activeTab, setActiveTab] = useState<'DASHBOARD' | 'REPORTS' | 'DEBTS' | 'SETTINGS'>('DASHBOARD')
  const [showMoreTx, setShowMoreTx] = useState(false)
  const [debtTab, setDebtTab] = useState<'CHO_VAY' | 'DI_VAY'>('CHO_VAY')
  const [activeTool, setActiveTool] = useState<'NONE' | 'BUDGET' | 'RECURRING' | 'GOAL' | 'EXCHANGE_RATE' | 'CATEGORY'>('NONE')
  const [theme, setTheme] = useState<'system' | 'light' | 'dark'>('system')
  const [lang, setLang] = useState<'vi' | 'en'>('vi')
  const [currency, setCurrency] = useState('VND')
  const [exchangeRate] = useState(() => Number(localStorage.getItem('kat_exchange_rate')) || 25000)
  
  const [pin, setPin] = useState(() => localStorage.getItem('kat_pin') || '')
  const [isUnlocked, setIsUnlocked] = useState(() => !localStorage.getItem('kat_pin'))
  const [pinInput, setPinInput] = useState('')
  const [newPinInput, setNewPinInput] = useState('')
  const [showPinSetupModal, setShowPinSetupModal] = useState(false)
  const [showPwaBanner, setShowPwaBanner] = useState(false)
  const [exchangeRatesData, setExchangeRatesData] = useState<any[]>([])
  const [loadingRates, setLoadingRates] = useState(false)
  const [showDonateModal, setShowDonateModal] = useState(false)
  const [showAboutModal, setShowAboutModal] = useState(false)
  const [showSupportModal, setShowSupportModal] = useState(false)
  const [showCsvModal, setShowCsvModal] = useState(false)
  const [csvFilter, setCsvFilter] = useState({
    sourceId: 'ALL',
    type: 'ALL'
  })

  useEffect(() => {
    if (!window.matchMedia('(display-mode: standalone)').matches) {
      if (!sessionStorage.getItem('kat_pwa_dismissed')) {
        setShowPwaBanner(true)
      }
    }
  }, [])

  useEffect(() => {
    const root = document.documentElement
    if (theme === 'system') {
      const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      root.setAttribute('data-theme', isDark ? 'dark' : 'light')
    } else {
      root.setAttribute('data-theme', theme)
    }
  }, [theme])

  const [isSourceModalOpen, setSourceModalOpen] = useState(false)
  const [isTxModalOpen, setTxModalOpen] = useState(false)
  const [calcInput, setCalcInput] = useState('0')
  const [isCategoryModalOpen, setCategoryModalOpen] = useState(false)
  const [isBudgetModalOpen, setBudgetModalOpen] = useState(false)
  const [isDebtModalOpen, setDebtModalOpen] = useState(false)
  const [isGoalModalOpen, setGoalModalOpen] = useState(false)
  const [isRecurringModalOpen, setRecurringModalOpen] = useState(false)
  
  const [editingSourceId, setEditingSourceId] = useState<number | null>(null)
  const [editingTxId, setEditingTxId] = useState<number | null>(null)
  const [editingCategoryId, setEditingCategoryId] = useState<number | null>(null)
  const [editingBudgetId, setEditingBudgetId] = useState<number | null>(null)
  const [editingDebtId, setEditingDebtId] = useState<number | null>(null)
  const [editingGoalId, setEditingGoalId] = useState<number | null>(null)
  const [editingRecurringId, setEditingRecurringId] = useState<number | null>(null)

  const [goalForm, setGoalForm] = useState<GoalForm>({
    name: '',
    targetAmount: '',
    currentAmount: '0',
    currency: 'VND'
  })

  const [recurringForm, setRecurringForm] = useState<RecurringForm>({
    amount: '',
    type: 'EXPENSE',
    category: '',
    note: '',
    sourceName: '',
    currency: 'VND',
    dayOfMonth: '1'
  })
  
  const [sourceForm, setSourceForm] = useState<SourceForm>(defaultSourceForm)
  const [txForm, setTxForm] = useState<TxForm>(defaultTxForm)
  const [categoryForm, setCategoryForm] = useState<CategoryForm>(defaultCategoryForm)
  const [budgetForm, setBudgetForm] = useState<BudgetForm>(defaultBudgetForm)
  const [debtForm, setDebtForm] = useState<DebtForm>(defaultDebtForm)
  const [categoryTypeFilter, setCategoryTypeFilter] = useState<CategoryTypeFilter>('ALL')
  const [budgetMonthFilter, setBudgetMonthFilter] = useState(format(new Date(), 'yyyy-MM'))
  const [reportMonthFilter, setReportMonthFilter] = useState(format(new Date(), 'yyyy-MM'))
  const [reportTypeFilter, setReportTypeFilter] = useState<'ALL' | 'EXPENSE' | 'INCOME'>('ALL')
  const [reportCurrencyFilter, setReportCurrencyFilter] = useState<string>('ALL')
  const [statusMessage, setStatusMessage] = useState('')

  useEffect(() => {
    const initializeData = async () => {
      const [sourceCount, categoryCount] = await Promise.all([
        db.sources.count(),
        db.categories.count(),
      ])

      if (sourceCount === 0) {
        await db.sources.add({
          name: 'Tien mat',
          type: 'CASH',
          includeInTotal: true,
          interestRate: 0,
          interestPeriod: 'NONE',
          createdTimestamp: Date.now(),
        })
      }

      if (categoryCount === 0) {
        await db.categories.bulkAdd(defaultCategoryPresets)
      }
    }

    void initializeData()
  }, [])

  useEffect(() => {
    if (!statusMessage) return
    const timeout = window.setTimeout(() => setStatusMessage(''), 2600)
    return () => window.clearTimeout(timeout)
  }, [statusMessage])

  const balancesBySource: Record<string, number> = {}
  let monthIncome = 0
  let monthExpense = 0
  const monthKey = format(new Date(), 'yyyy-MM')

  transactions.forEach((tx) => {
    const delta = transactionDelta(tx)
    let convertedDelta = delta
    if (tx.currency && tx.currency !== 'VND') {
      convertedDelta = delta * exchangeRate
    }
    balancesBySource[tx.sourceName] = (balancesBySource[tx.sourceName] ?? 0) + convertedDelta
    if (format(tx.timestamp, 'yyyy-MM') !== monthKey) return
    if (delta > 0) monthIncome += delta
    if (delta < 0) monthExpense += Math.abs(delta)
  })

  let netWorth = 0
  for (const source of sources) {
    if (!source.includeInTotal) continue
    netWorth += balancesBySource[source.name] ?? 0
  }

  const netWorthByCurrency: Record<string, number> = {}
  transactions.forEach((tx) => {
    const source = sources.find((s) => s.name === tx.sourceName)
    if (source && !source.includeInTotal) return
    const delta = transactionDelta(tx)
    const cur = tx.currency || 'VND'
    netWorthByCurrency[cur] = (netWorthByCurrency[cur] ?? 0) + delta
  })



  const filteredCategories = categories.filter((category) => {
    if (categoryTypeFilter === 'ALL') return true
    return category.type === categoryTypeFilter
  })
  const txCategoryPresets = categories.filter((category) => category.type === txForm.type)
  const expenseCategories = categories.filter((category) => category.type === 'EXPENSE')
  const expenseCategoryIdByName = new Map(
    expenseCategories.map((category) => [category.name.toLowerCase(), category.id]),
  )
  const categoriesById = new Map(categories.map((category) => [category.id, category]))

  const monthlySpentByCategoryId: Record<number, number> = {}
  for (const tx of transactions) {
    if (tx.type !== 'EXPENSE') continue
    if (format(tx.timestamp, 'yyyy-MM') !== budgetMonthFilter) continue
    const categoryId = expenseCategoryIdByName.get(tx.category.trim().toLowerCase())
    if (!categoryId) continue
    monthlySpentByCategoryId[categoryId] = (monthlySpentByCategoryId[categoryId] ?? 0) + tx.amount
  }

  const monthlyBudgets = budgets
    .filter((budget) => budget.monthYear === budgetMonthFilter)
    .sort((a, b) => b.limitAmountMinor - a.limitAmountMinor)

  const budgetRows = monthlyBudgets.map((budget) => {
    const category = categoriesById.get(budget.categoryId)
    const spent = monthlySpentByCategoryId[budget.categoryId] ?? 0
    const remaining = budget.limitAmountMinor - spent
    const percent = budget.limitAmountMinor <= 0
      ? 0
      : Math.min(100, Math.round((spent / budget.limitAmountMinor) * 100))

    return {
      budget,
      category,
      spent,
      remaining,
      percent,
      isOver: remaining < 0,
    }
  })

  const totalBudgetLimit = budgetRows.reduce((sum, row) => sum + row.budget.limitAmountMinor, 0)
  const totalBudgetSpent = budgetRows.reduce((sum, row) => sum + row.spent, 0)
  const totalBudgetPercent =
    totalBudgetLimit <= 0 ? 0 : Math.min(100, Math.round((totalBudgetSpent / totalBudgetLimit) * 100))
  const overBudgetRows = budgetRows.filter((row) => row.isOver)
  const overBudgetTotal = overBudgetRows.reduce((sum, row) => sum + Math.abs(row.remaining), 0)

  let reportIncomeTotal = 0
  let reportExpenseTotal = 0
  let prevPeriodExpenseTotal = 0
  const reportBalancesByCurrency: Record<string, number> = {}
  const reportExpenseByCategory: Record<string, number> = {}
  const reportDailyExpenses: Record<string, number> = {}

  const prevMonthDate = new Date(reportMonthFilter + '-01')
  prevMonthDate.setMonth(prevMonthDate.getMonth() - 1)
  const prevMonthFilter = format(prevMonthDate, 'yyyy-MM')

  for (const tx of transactions) {
    const txMonth = format(tx.timestamp, 'yyyy-MM')
    if (txMonth === prevMonthFilter) {
      if (reportCurrencyFilter === 'ALL' || tx.currency === reportCurrencyFilter) {
        if (tx.type === 'EXPENSE') prevPeriodExpenseTotal += tx.amount
      }
    }
    
    if (txMonth !== reportMonthFilter) continue
    if (reportTypeFilter !== 'ALL' && tx.type !== reportTypeFilter) continue
    if (reportCurrencyFilter !== 'ALL' && tx.currency !== reportCurrencyFilter) continue

    if (tx.type === 'INCOME') {
      reportIncomeTotal += tx.amount
      reportBalancesByCurrency[tx.currency] = (reportBalancesByCurrency[tx.currency] ?? 0) + tx.amount
    }
    if (tx.type === 'EXPENSE') {
      reportExpenseTotal += tx.amount
      reportBalancesByCurrency[tx.currency] = (reportBalancesByCurrency[tx.currency] ?? 0) - tx.amount
      reportExpenseByCategory[tx.category] = (reportExpenseByCategory[tx.category] ?? 0) + tx.amount

      const day = format(tx.timestamp, 'dd/MM')
      reportDailyExpenses[day] = (reportDailyExpenses[day] ?? 0) + tx.amount
    }
  }

  const categoryColors = ['#F44336', '#FF9800', '#9C27B0', '#FF5722', '#E91E63', '#2196F3', '#4CAF50', '#009688']
  const reportCategoryRows = Object.entries(reportExpenseByCategory)
    .sort((a, b) => b[1] - a[1])
    .map(([categoryName, amount], index) => {
      const preset = txCategoryPresets.find(c => c.name === categoryName)
      return {
        name: categoryName,
        emoji: preset?.emoji || '🏷️',
        value: amount,
        percent: reportExpenseTotal > 0 ? (amount / reportExpenseTotal) * 100 : 0,
        color: categoryColors[index % categoryColors.length]
      }
    })

  const categoryPieOptions: echarts.EChartsCoreOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [
      {
        type: 'pie',
        radius: ['120%', '150%'],
        center: ['50%', '95%'],
        startAngle: 180,
        endAngle: 0,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: reportCategoryRows.map(r => ({ name: r.name, value: r.value, itemStyle: { color: r.color } })),
      }
    ]
  }

  // Build full month day range (01/MM to last day)
  const reportYear = parseInt(reportMonthFilter.split('-')[0])
  const reportMonth = parseInt(reportMonthFilter.split('-')[1])
  const daysInMonth = new Date(reportYear, reportMonth, 0).getDate()
  const fullMonthDays: string[] = []
  for (let d = 1; d <= daysInMonth; d++) {
    const dd = String(d).padStart(2, '0')
    const mm = String(reportMonth).padStart(2, '0')
    fullMonthDays.push(`${dd}/${mm}`)
  }

  const dailyTrendOptions: echarts.EChartsCoreOption = {
    grid: { left: 40, right: 20, top: 20, bottom: 30, containLabel: false },
    xAxis: {
      type: 'category',
      data: fullMonthDays,
      boundaryGap: false,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: 'var(--text-muted)',
        fontSize: 10,
        interval: (index: number) => index === 0 || index === Math.floor(daysInMonth / 2) || index === daysInMonth - 1
      }
    },
    yAxis: {
      type: 'value',
      show: true,
      min: 0,
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)', type: 'dashed' } },
      axisLabel: { show: false },
      axisTick: { show: false },
      axisLine: { show: false }
    },
    series: [{
      data: fullMonthDays.map(d => reportDailyExpenses[d] ?? 0),
      type: 'line',
      smooth: false,
      symbol: 'none',
      lineStyle: { width: 2, color: '#F44336' },
      areaStyle: { color: 'rgba(244, 67, 54, 0.08)' },
      itemStyle: { color: '#F44336' }
    }]
  }

  const compCurrency = reportCurrencyFilter !== 'ALL' ? reportCurrencyFilter : 'VND'
  const comparisonOptions: echarts.EChartsCoreOption = {
    grid: { left: 40, right: 40, top: 40, bottom: 30, containLabel: false },
    xAxis: { type: 'category', data: ['Kỳ trước', 'Kỳ này'], axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: 'var(--text-muted)', fontSize: 13 } },
    yAxis: { type: 'value', show: false },
    series: [{
      data: [
        { value: prevPeriodExpenseTotal, itemStyle: { color: '#B0BEC5', borderRadius: [12,12,0,0] } },
        { value: reportExpenseTotal, itemStyle: { color: '#F44336', borderRadius: [12,12,0,0] } }
      ],
      type: 'bar',
      barWidth: 60,
      label: {
        show: true,
        position: 'top',
        formatter: (params: any) => formatCurrency(params.value, compCurrency),
        rich: {
          prev: { color: 'var(--text-muted)', fontSize: 12, fontWeight: 'bold' },
          curr: { color: '#F44336', fontSize: 12, fontWeight: 'bold' }
        },
        color: (params: any) => params.dataIndex === 1 ? '#F44336' : 'var(--text-muted)',
        fontSize: 12,
        fontWeight: 'bold'
      }
    }]
  }

  useEffect(() => {
    if (budgetForm.categoryId) return
    const firstExpenseCategory = expenseCategories[0]
    if (!firstExpenseCategory) return
    setBudgetForm((prev) => ({ ...prev, categoryId: firstExpenseCategory.id.toString() }))
  }, [budgetForm.categoryId, expenseCategories])

  const openSourceModal = (source?: MoneySource) => {
    if (source) {
      setEditingSourceId(source.id)
      setSourceForm({
        name: source.name,
        type: source.type,
        includeInTotal: source.includeInTotal,
        initialBalance: '',
        interestRate: source.interestRate?.toString() || '',
        interestPeriod: source.interestPeriod || '1 tháng',
      })
    } else {
      setEditingSourceId(null)
      setSourceForm(defaultSourceForm)
    }
    setSourceModalOpen(true)
  }

  const openTxModal = (tx?: Transaction) => {
    if (!tx && sources.length === 0) {
      openSourceModal()
      return
    }

    if (tx) {
      setEditingTxId(tx.id)
      setTxForm({
        sourceName: tx.sourceName,
        type: tx.type === 'INCOME' ? 'INCOME' : tx.type === 'TRANSFER_OUT' ? 'TRANSFER_OUT' : 'EXPENSE',
        amount: tx.amount.toString(),
        category: tx.category,
        note: tx.note,
        currency: tx.currency,
        timestamp: new Date(tx.timestamp).toISOString().split('T')[0],
        imageUri: tx.imageUri || '',
        destinationName: tx.destinationName || '',
      })
      setCalcInput(tx.amount.toString())
    } else {
      setEditingTxId(null)
      setTxForm({
        ...defaultTxForm,
        sourceName: sources[0]?.name ?? '',
      })
      setCalcInput('0')
    }
    setTxModalOpen(true)
  }

  const openCategoryModal = (category?: Category) => {
    if (category) {
      setEditingCategoryId(category.id)
      setCategoryForm({
        name: category.name,
        emoji: category.emoji,
        type: category.type,
      })
    } else {
      setEditingCategoryId(null)
      setCategoryForm(defaultCategoryForm)
    }
    setCategoryModalOpen(true)
  }

  const openBudgetModal = (budget?: Budget) => {
    if (expenseCategories.length === 0) {
      setStatusMessage('Can co it nhat 1 danh muc chi tieu de tao ngan sach.')
      return
    }

    if (budget) {
      setEditingBudgetId(budget.id)
      setBudgetForm({
        monthYear: budget.monthYear,
        categoryId: budget.categoryId.toString(),
        limitAmount: budget.limitAmountMinor.toString(),
        currencyCode: budget.currencyCode,
      })
    } else {
      setEditingBudgetId(null)
      setBudgetForm({
        ...defaultBudgetForm,
        monthYear: budgetMonthFilter,
        categoryId: expenseCategories[0]?.id?.toString() ?? '',
      })
    }
    setBudgetModalOpen(true)
  }

  const openDebtModal = (debt?: Debt) => {
    if (debt) {
      setEditingDebtId(debt.id!)
      setDebtForm({
        personName: debt.personName,
        amount: debt.amount.toString(),
        currency: debt.currency,
        type: debt.type,
        note: debt.note,
        dueDate: debt.dueDate ? format(debt.dueDate, 'yyyy-MM-dd') : '',
      })
    } else {
      setEditingDebtId(null)
      setDebtForm(defaultDebtForm)
    }
    setDebtModalOpen(true)
  }

  const openGoalModal = (goal?: SavingGoal) => {
    if (goal) {
      setEditingGoalId(goal.id!)
      setGoalForm({
        name: goal.name,
        targetAmount: goal.targetAmount.toString(),
        currentAmount: goal.currentAmount.toString(),
        currency: goal.currency,
      })
    } else {
      setEditingGoalId(null)
      setGoalForm({
        name: '',
        targetAmount: '',
        currentAmount: '',
        currency: 'VND',
      })
    }
    setGoalModalOpen(true)
  }


  const handleSaveSource = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const name = sourceForm.name.trim()
    if (!name) return

    const exists = sources.some((source) => {
      if (editingSourceId != null && source.id === editingSourceId) return false
      return source.name.toLowerCase() === name.toLowerCase()
    })
    if (exists) {
      setStatusMessage('Ten tai khoan da ton tai.')
      return
    }

    if (editingSourceId == null) {
      await db.sources.add({
        name,
        type: sourceForm.type,
        includeInTotal: sourceForm.includeInTotal,
        interestRate: sourceForm.type === 'SAVINGS' ? (parseFloat(sourceForm.interestRate) || 0) : 0,
        interestPeriod: sourceForm.type === 'SAVINGS' ? sourceForm.interestPeriod : 'NONE',
        createdTimestamp: Date.now(),
      })

      const initialBalance = parseAmount(sourceForm.initialBalance)
      if (initialBalance > 0) {
        await db.transactions.add({
          amount: initialBalance,
          type: 'INCOME',
          category: 'So du ban dau',
          note: 'Khoi tao so du',
          timestamp: Date.now(),
          sourceName: name,
          currency: 'VND',
        })
      }

      setStatusMessage('Da tao tai khoan.')
    } else {
      const oldSource = sources.find((source) => source.id === editingSourceId)
      if (!oldSource) return

      await db.sources.put({
        ...oldSource,
        name,
        type: sourceForm.type,
        includeInTotal: sourceForm.includeInTotal,
        interestRate: sourceForm.type === 'SAVINGS' ? (parseFloat(sourceForm.interestRate) || 0) : 0,
        interestPeriod: sourceForm.type === 'SAVINGS' ? sourceForm.interestPeriod : 'NONE',
      })

      if (oldSource.name !== name) {
        await db.transactions.where('sourceName').equals(oldSource.name).modify({ sourceName: name })
      }
      setStatusMessage('Da cap nhat tai khoan.')
    }

    setSourceModalOpen(false)
    setEditingSourceId(null)
  }



  const handleSaveCategory = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const name = categoryForm.name.trim()
    if (!name) return

    const exists = categories.some((category) => {
      if (editingCategoryId != null && category.id === editingCategoryId) return false
      return (
        category.type === categoryForm.type &&
        category.name.toLowerCase() === name.toLowerCase()
      )
    })
    if (exists) {
      setStatusMessage('Danh muc da ton tai trong nhom nay.')
      return
    }

    const normalizedEmoji = categoryForm.emoji.trim()
    if (editingCategoryId == null) {
      await db.categories.add({
        name,
        emoji: normalizedEmoji,
        type: categoryForm.type,
      })
      setStatusMessage('Da tao danh muc.')
    } else {
      const oldCategory = categories.find((category) => category.id === editingCategoryId)
      if (!oldCategory) return

      await db.categories.put({
        ...oldCategory,
        name,
        emoji: normalizedEmoji,
        type: categoryForm.type,
      })
      if (oldCategory.name !== name) {
        await db.transactions.where('category').equals(oldCategory.name).modify({ category: name })
      }
      setStatusMessage('Da cap nhat danh muc.')
    }

    setCategoryModalOpen(false)
    setEditingCategoryId(null)
  }

  const handleDeleteCategory = async (category: Category) => {
    const inUse = transactions.some((tx) => tx.category.toLowerCase() === category.name.toLowerCase())
    if (inUse) {
      setStatusMessage('Khong the xoa danh muc dang co giao dich.')
      return
    }
    const budgetInUse = budgets.some((budget) => budget.categoryId === category.id)
    if (budgetInUse) {
      setStatusMessage('Khong the xoa danh muc dang duoc gan ngan sach.')
      return
    }
    await db.categories.delete(category.id)
    setStatusMessage('Da xoa danh muc.')
  }

  const handleSaveBudget = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const categoryId = Number(budgetForm.categoryId)
    const limitAmount = parseAmount(budgetForm.limitAmount)
    const monthYear = budgetForm.monthYear.trim()
    const currencyCode = budgetForm.currencyCode.trim().toUpperCase() || 'VND'

    if (!Number.isInteger(categoryId) || categoryId <= 0) return
    if (!monthYear) return
    if (limitAmount <= 0) return

    const duplicate = budgets.some((budget) => {
      if (editingBudgetId != null && budget.id === editingBudgetId) return false
      return budget.monthYear === monthYear && budget.categoryId === categoryId
    })
    if (duplicate) {
      setStatusMessage('Danh muc nay da co ngan sach trong thang duoc chon.')
      return
    }

    if (editingBudgetId == null) {
      await db.budgets.add({
        categoryId,
        limitAmountMinor: Math.round(limitAmount),
        currencyCode,
        monthYear,
      })
      setStatusMessage('Da tao ngan sach.')
    } else {
      const oldBudget = budgets.find((budget) => budget.id === editingBudgetId)
      if (!oldBudget) return
      await db.budgets.put({
        ...oldBudget,
        categoryId,
        limitAmountMinor: Math.round(limitAmount),
        currencyCode,
        monthYear,
      })
      setStatusMessage('Da cap nhat ngan sach.')
    }

    setBudgetModalOpen(false)
    setEditingBudgetId(null)
  }

  const handleDeleteBudget = async (id: number) => {
    await db.budgets.delete(id)
    setStatusMessage('Da xoa ngan sach.')
  }

  const handleSaveTransaction = async (event?: FormEvent<HTMLFormElement>) => {
    if (event) event.preventDefault()
    
    // Evaluate calcInput if it's an expression
    let finalAmount = 0
    try {
      // safe eval for basic math: replace x with * and evaluate
      const expression = calcInput.replace(/×/g, '*').replace(/÷/g, '/').replace(/,/g, '')
      finalAmount = new Function('return ' + expression)()
    } catch {
      finalAmount = parseAmount(txForm.amount || calcInput)
    }
    
    if (finalAmount <= 0 || !txForm.sourceName) return

    const category = txForm.category.trim() || (txForm.type === 'INCOME' ? 'Thu nhap' : txForm.type === 'TRANSFER_OUT' ? 'Chuyen tien' : 'Chi tieu')
    const normalizedCurrency = txForm.currency.trim().toUpperCase() || 'VND'
    const ts = new Date(txForm.timestamp).getTime()

    if (editingTxId == null) {
      const outId = await db.transactions.add({
        amount: finalAmount,
        type: txForm.type,
        category,
        note: txForm.note.trim(),
        timestamp: ts,
        sourceName: txForm.sourceName,
        currency: normalizedCurrency,
        imageUri: txForm.imageUri,
        destinationName: txForm.type === 'TRANSFER_OUT' ? txForm.destinationName : undefined,
      })
      
      if (txForm.type === 'TRANSFER_OUT' && txForm.destinationName) {
        await db.transactions.add({
          amount: finalAmount,
          type: 'TRANSFER_IN',
          category: 'Nhan tien',
          note: `Nhan tu ${txForm.sourceName}`,
          timestamp: ts,
          sourceName: txForm.destinationName,
          currency: normalizedCurrency,
          projectTag: outId.toString(), // store outId to link
        })
      }
      
      setStatusMessage('Da them giao dich.')
    } else {
      const existing = transactions.find((tx) => tx.id === editingTxId)
      if (!existing) return
      await db.transactions.put({
        ...existing,
        amount: finalAmount,
        type: txForm.type,
        category,
        note: txForm.note.trim(),
        timestamp: ts,
        sourceName: txForm.sourceName,
        currency: normalizedCurrency,
        imageUri: txForm.imageUri,
        destinationName: txForm.type === 'TRANSFER_OUT' ? txForm.destinationName : undefined,
      })
      
      // If it's a transfer, we might need to update the paired IN transaction
      if (txForm.type === 'TRANSFER_OUT') {
        const pairedIn = transactions.find(t => t.type === 'TRANSFER_IN' && t.projectTag === existing.id.toString())
        if (pairedIn && txForm.destinationName) {
          await db.transactions.put({
            ...pairedIn,
            amount: finalAmount,
            timestamp: ts,
            sourceName: txForm.destinationName,
            currency: normalizedCurrency,
          })
        }
      }
      
      setStatusMessage('Da cap nhat giao dich.')
    }

    setTxModalOpen(false)
    setEditingTxId(null)
  }



  const handleSaveDebt = async (event: FormEvent) => {
    event.preventDefault()
    if (!debtForm.personName || !debtForm.amount) return

    const parsedAmount = Number(debtForm.amount)
    if (isNaN(parsedAmount) || parsedAmount <= 0) return

    let parsedDueDate: number | null = null
    if (debtForm.dueDate) {
      const parts = debtForm.dueDate.split('-')
      if (parts.length === 3) {
        parsedDueDate = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2])).getTime()
      }
    }

    if (editingDebtId != null) {
      await db.debts.update(editingDebtId, {
        personName: debtForm.personName,
        amount: parsedAmount,
        type: debtForm.type,
        note: debtForm.note,
        dueDate: parsedDueDate
      })
    } else {
      await db.debts.add({
        personName: debtForm.personName,
        amount: parsedAmount,
        currency: debtForm.currency || 'VND',
        type: debtForm.type,
        note: debtForm.note,
        timestamp: Date.now(),
        isPaid: false,
        paidAmount: 0,
        dueDate: parsedDueDate,
      })
      setStatusMessage('Da cap nhat ghi chu vay/no.')
    }

    setDebtModalOpen(false)
    setEditingDebtId(null)
  }

  const handleSaveGoal = async (e: React.FormEvent | React.MouseEvent) => {
    if (e) e.preventDefault()
    const targetAmount = parseAmount(goalForm.targetAmount)
    const currentAmount = parseAmount(goalForm.currentAmount)

    if (editingGoalId != null) {
      await db.saving_goals.update(editingGoalId, {
        name: goalForm.name,
        targetAmount,
        currentAmount,
        currency: goalForm.currency,
      })
    } else {
      await db.saving_goals.add({
        name: goalForm.name,
        targetAmount,
        currentAmount,
        currency: goalForm.currency,
      })
    }
    setGoalModalOpen(false)
  }

  const handleDeleteGoal = async (id: number) => {
    await db.saving_goals.delete(id)
    setGoalModalOpen(false)
  }

  const openRecurringModal = (recurring?: RecurringTransaction) => {
    if (recurring) {
      setEditingRecurringId(recurring.id!)
      setRecurringForm({
        amount: recurring.amount.toString(),
        currency: recurring.currency,
        type: recurring.type,
        category: recurring.category,
        note: recurring.note,
        sourceName: recurring.sourceName,
        dayOfMonth: recurring.dayOfMonth.toString(),
      })
    } else {
      setEditingRecurringId(null)
      setRecurringForm({
        amount: '',
        currency: 'VND',
        type: 'EXPENSE',
        category: expenseCategories[0]?.name ?? '',
        note: '',
        sourceName: sources[0]?.name ?? '',
        dayOfMonth: '1',
      })
    }
    setRecurringModalOpen(true)
  }

  const handleSaveRecurring = async (e: React.FormEvent | React.MouseEvent) => {
    if (e) e.preventDefault()
    const amount = parseAmount(recurringForm.amount)
    const dayOfMonth = parseInt(recurringForm.dayOfMonth, 10) || 1

    if (editingRecurringId != null) {
      await db.recurrings.update(editingRecurringId, {
        amount,
        currency: recurringForm.currency,
        type: recurringForm.type,
        category: recurringForm.category,
        note: recurringForm.note,
        sourceName: recurringForm.sourceName,
        dayOfMonth,
      })
    } else {
      await db.recurrings.add({
        amount,
        currency: recurringForm.currency,
        type: recurringForm.type,
        category: recurringForm.category,
        note: recurringForm.note,
        sourceName: recurringForm.sourceName,
        dayOfMonth,
        lastExecutedMonth: '',
      })
    }
    setRecurringModalOpen(false)
  }

  const handleDeleteRecurring = async (id: number) => {
    await db.recurrings.delete(id)
    setRecurringModalOpen(false)
  }

  const handleDeleteDebt = async (id: number) => {
    await db.debts.delete(id)
    setStatusMessage('Da xoa ghi chu vay/no.')
  }

  const handleTogglePaidDebt = async (debt: Debt) => {
    await db.debts.put({
      ...debt,
      isPaid: !debt.isPaid,
      paidAmount: !debt.isPaid ? debt.amount : 0,
    })
  }

  const handleExportBackup = async () => {
    const payload: BackupPayload = {
      app: 'kat-budget-pwa',
      version: 1,
      exportedAt: new Date().toISOString(),
      sources: await db.sources.toArray(),
      transactions: await db.transactions.toArray(),
      categories: await db.categories.toArray(),
      budgets: await db.budgets.toArray(),
      debts: await db.debts.toArray(),
      saving_goals: await db.saving_goals.toArray(),
      recurrings: await db.recurrings.toArray(),
    }

    const rawJson = JSON.stringify(payload)
    const blob = await gzipString(rawJson)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'KATBudget_BackUp.kat'
    link.click()
    URL.revokeObjectURL(url)
    setStatusMessage('Da xuat backup .kat (Android Native GZIP).')
  }

  const handlePickImportFile = () => {
    fileInputRef.current?.click()
  }

  const handleImportBackup = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    try {
      let payloadText = ''
      try {
        payloadText = await ungzipBlob(file)
      } catch {
        payloadText = await file.text()
        if (payloadText.startsWith(KAT_BACKUP_PREFIX)) {
          payloadText = decodeBase64Utf8(payloadText.slice(KAT_BACKUP_PREFIX.length).trim())
        }
      }
      const payload = JSON.parse(payloadText) as BackupPayload

      await db.transaction('rw', [db.sources, db.transactions, db.categories, db.budgets, db.debts, db.saving_goals, db.recurrings], async () => {
        await db.sources.clear()
        await db.transactions.clear()
        await db.categories.clear()
        await db.budgets.clear()
        await db.debts.clear()
        await db.saving_goals.clear()
        await db.recurrings.clear()

        if (payload.sources?.length) {
          await db.sources.bulkPut(payload.sources)
        }
        if (payload.transactions?.length) {
          await db.transactions.bulkPut(payload.transactions)
        }
        if (payload.categories?.length) {
          await db.categories.bulkPut(payload.categories)
        } else {
          await db.categories.bulkAdd(defaultCategoryPresets)
        }
        if (payload.budgets?.length) {
          await db.budgets.bulkPut(payload.budgets)
        }
        if (payload.debts?.length) {
          await db.debts.bulkPut(payload.debts)
        }
        if (payload.saving_goals?.length) {
          await db.saving_goals.bulkPut(payload.saving_goals)
        }
        if (payload.recurrings?.length) {
          await db.recurrings.bulkPut(payload.recurrings)
        }
      })
      setStatusMessage('Da phuc hoi backup .kat/.json.')
    } catch {
      setStatusMessage('File backup khong hop le hoac sai dinh dang .kat/.json.')
    } finally {
      event.target.value = ''
    }
  }

  const handleExportCsv = () => {
    let filtered = transactions;
    
    if (csvFilter.type !== 'ALL') {
      filtered = filtered.filter(tx => tx.type === csvFilter.type)
    }
    
    if (csvFilter.sourceId !== 'ALL') {
      filtered = filtered.filter(tx => tx.sourceName === csvFilter.sourceId)
    }



    if (filtered.length === 0) {
      alert(lang === 'vi' ? 'Không có giao dịch nào phù hợp để xuất!' : 'No matching transactions to export!')
      return
    }

    const header = '\uFEFF"ID","Date","Amount","Currency","Type","Category","Source","Note"\n'
    const rows = filtered.map(tx => {
      const dateStr = format(tx.timestamp, 'yyyy-MM-dd HH:mm:ss')
      return `"${tx.id}","${dateStr}","${tx.amount}","${tx.currency}","${tx.type}","${tx.category}","${tx.sourceName}","${tx.note.replace(/"/g, '""')}"`
    }).join('\n')
    
    const blob = new Blob([header + rows], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `kat_transactions_${format(new Date(), 'yyyyMMdd_HHmm')}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    setShowCsvModal(false)
    setStatusMessage(lang === 'vi' ? 'Đã tải xuống file CSV.' : 'CSV file downloaded.')
  }

  useEffect(() => {
    if (activeTool === 'EXCHANGE_RATE' && exchangeRatesData.length === 0) {
      setLoadingRates(true)
      fetch('/api/vcb-rates')
        .then(res => res.text())
        .then(str => {
          const parser = new DOMParser()
          const xmlDoc = parser.parseFromString(str, "text/xml")
          const exrates = Array.from(xmlDoc.getElementsByTagName('Exrate'))
          const rates = exrates.map(node => ({
            CurrencyCode: node.getAttribute('CurrencyCode'),
            CurrencyName: node.getAttribute('CurrencyName'),
            Buy: node.getAttribute('Buy'),
            Transfer: node.getAttribute('Transfer'),
            Sell: node.getAttribute('Sell')
          }))
          setExchangeRatesData(rates)
        })
        .catch(err => console.error('Fetch Exchange Rates Error:', err))
        .finally(() => setLoadingRates(false))
    }
  }, [activeTool])

  if (!isUnlocked) {
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
            <div key={i} style={{ 
              width: '16px', height: '16px', borderRadius: '50%', 
              background: i < pinInput.length ? 'var(--accent)' : 'var(--surface)',
              border: i < pinInput.length ? 'none' : '1px solid var(--border)' 
            }} />
          ))}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', maxWidth: '280px', width: '100%' }}>
          {[1, 2, 3, 4, 5, 6, 7, 8, 9].map(num => (
            <button key={num} className="numpad-key" style={{ width: '72px', height: '72px', borderRadius: '50%', background: 'var(--surface)', fontSize: '28px', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', justifySelf: 'center', color: 'var(--text)' }} onClick={() => {
              if (pinInput.length < 4) {
                const newVal = pinInput + num;
                setPinInput(newVal);
                if (newVal.length === 4) {
                  setTimeout(() => {
                    if (newVal === pin) {
                      setIsUnlocked(true);
                    } else {
                      alert(lang === 'vi' ? 'Mã PIN sai!' : 'Incorrect PIN!');
                      setPinInput('');
                    }
                  }, 150);
                }
              }
            }}>{num}</button>
          ))}
          <div />
          <button className="numpad-key" style={{ width: '72px', height: '72px', borderRadius: '50%', background: 'var(--surface)', fontSize: '28px', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', justifySelf: 'center', color: 'var(--text)' }} onClick={() => {
            if (pinInput.length < 4) {
              const newVal = pinInput + '0';
              setPinInput(newVal);
              if (newVal.length === 4) {
                setTimeout(() => {
                  if (newVal === pin) {
                    setIsUnlocked(true);
                  } else {
                    alert(lang === 'vi' ? 'Mã PIN sai!' : 'Incorrect PIN!');
                    setPinInput('');
                  }
                }, 150);
              }
            }
          }}>0</button>
          <button className="numpad-key" style={{ width: '72px', height: '72px', borderRadius: '50%', background: 'transparent', fontSize: '28px', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', justifySelf: 'center', color: 'var(--text)' }} onClick={() => setPinInput(p => p.slice(0, -1))}>
            <Delete size={28} />
          </button>
        </div>
      </main>
    )
  }

  return (
    <main className="app-shell">
      {activeTool !== 'NONE' ? (
        <header className="topbar">
          <button 
            type="button" 
            onClick={() => setActiveTool('NONE')} 
            style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'none', border: 'none', color: 'var(--accent)', fontWeight: 600, fontSize: '16px', cursor: 'pointer', padding: 0 }}
          >
            &larr; {lang === 'vi' ? 'Quay lại' : 'Back'}
          </button>
          <h1>
            {activeTool === 'BUDGET' && (lang === 'vi' ? 'Ngân sách hằng tháng' : 'Monthly Budget')}
            {activeTool === 'RECURRING' && (lang === 'vi' ? 'Giao dịch định kỳ' : 'Recurring Transactions')}
            {activeTool === 'GOAL' && (lang === 'vi' ? 'Mục tiêu tiết kiệm' : 'Saving Goals')}
            {activeTool === 'EXCHANGE_RATE' && (lang === 'vi' ? 'Tỷ giá quy đổi' : 'Exchange Rates')}
            {activeTool === 'CATEGORY' && (lang === 'vi' ? 'Quản lý danh mục' : 'Categories')}
          </h1>
          <button 
            type="button" 
            onClick={() => setLang(lang === 'vi' ? 'en' : 'vi')}
            style={{ fontWeight: 'bold', padding: '9px 14px', borderRadius: '20px', border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)', cursor: 'pointer' }}
          >
            {lang === 'vi' ? 'VI' : 'EN'}
          </button>
        </header>
      ) : (
        <header className="topbar">
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <p className="eyebrow" style={{ margin: '0 0 4px 0', fontSize: '14px', color: 'var(--muted)', textTransform: 'none', fontWeight: 'normal' }}>{lang === 'vi' ? 'Chào bạn!' : 'Hi there!'}</p>
            <h1 style={{ fontSize: '28px', margin: 0 }}>
              {activeTab === 'DASHBOARD' && (lang === 'vi' ? 'Tổng quan tài chính' : 'Financial Overview')}
              {activeTab === 'DEBTS' && (lang === 'vi' ? 'Theo dõi vay nợ' : 'Debt tracking')}
              {activeTab === 'REPORTS' && (lang === 'vi' ? 'Báo cáo & Phân tích' : 'Financial Reports')}
              {activeTab === 'SETTINGS' && (lang === 'vi' ? 'Cài đặt' : 'Settings')}
            </h1>
          </div>
          <button 
            type="button" 
            onClick={() => setLang(lang === 'vi' ? 'en' : 'vi')}
            style={{ fontWeight: 'bold', padding: '9px 14px', borderRadius: '20px', border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)', cursor: 'pointer' }}
          >
            {lang === 'vi' ? 'VI' : 'EN'}
          </button>
        </header>
      )}

      {statusMessage && <p className="status-banner">{statusMessage}</p>}

      {activeTool === 'BUDGET' && (
        <section style={{ position: 'fixed', inset: 0, background: 'var(--bg)', zIndex: 100, overflowY: 'auto', padding: '16px', paddingBottom: '40px' }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: '24px' }}>
            <button onClick={() => setActiveTool('NONE')} style={{ background: 'var(--surface)', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', width: '40px', height: '40px', borderRadius: '12px', cursor: 'pointer' }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
            </button>
            <h2 style={{ marginLeft: '16px', fontSize: '20px', fontWeight: 'bold', margin: 0, flex: 1 }}>
              {lang === 'vi' ? 'Ngân sách hằng tháng' : 'Monthly Budget'}
            </h2>
            <input
              type="month"
              value={budgetMonthFilter}
              onChange={(event) => setBudgetMonthFilter(event.target.value)}
              style={{ background: 'var(--surface)', border: 'none', color: 'var(--text)', fontSize: '14px', padding: '8px 12px', borderRadius: '16px', outline: 'none' }}
            />
          </div>

          <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '20px', marginBottom: '16px' }}>
            <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px' }}>
              {lang === 'vi' ? 'Tổng ngân sách' : 'Total budget'}
            </p>
            <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '28px', fontWeight: 'bold', margin: 0 }}>
                {formatCurrency(totalBudgetLimit)}
              </h3>
              <span style={{ fontSize: '16px', color: 'var(--accent)', fontWeight: 'bold' }}>
                {totalBudgetPercent}%
              </span>
            </div>
            
            <div style={{ height: '8px', background: 'rgba(255, 255, 255, 0.1)', borderRadius: '4px', overflow: 'hidden', marginBottom: '16px' }}>
              <div style={{ height: '100%', background: 'var(--accent)', width: `${Math.min(totalBudgetPercent, 100)}%`, borderRadius: '4px' }} />
            </div>

            <p style={{ fontSize: '13px', color: 'var(--text-muted)', margin: 0 }}>
              {lang === 'vi' ? 'Đã chi' : 'Spent'}: {formatCurrency(totalBudgetSpent)}
            </p>
          </div>

          {overBudgetRows.length > 0 && (
            <div style={{ background: 'rgba(244, 67, 54, 0.15)', color: 'var(--negative)', padding: '16px', borderRadius: '16px', marginBottom: '24px', fontSize: '14px' }}>
              {lang === 'vi' ? `Có ${overBudgetRows.length} danh mục vượt ngân sách, tổng vượt ` : `There are ${overBudgetRows.length} categories over budget, total over `}
              <strong>{formatCurrency(overBudgetTotal)}</strong>.
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 'bold', margin: 0 }}>
              {lang === 'vi' ? 'Danh sách ngân sách' : 'Budget list'}
            </h3>
            <button 
              type="button" 
              onClick={() => openBudgetModal()}
              style={{ background: 'transparent', border: 'none', color: 'var(--accent)', fontWeight: 'bold', fontSize: '14px', cursor: 'pointer' }}
            >
              {lang === 'vi' ? 'Tạo mới' : 'Create new'}
            </button>
          </div>

          {budgetRows.length === 0 ? (
            <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '40px 20px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '15px' }}>
              {lang === 'vi' ? 'Chưa thiết lập ngân sách nào.' : 'No budget set.'}
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {budgetRows.map((row) => (
                <div key={row.budget.id} style={{ background: 'var(--surface)', borderRadius: '20px', padding: '16px', cursor: 'pointer' }} onClick={() => openBudgetModal(row.budget)}>
                   <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <span style={{ fontWeight: 'bold' }}>
                         {row.category?.emoji ? `${row.category.emoji} ` : ''}
                         {row.category?.name ?? (lang === 'vi' ? 'Danh mục đã xóa' : 'Deleted category')}
                      </span>
                      <span style={{ fontWeight: 'bold' }}>
                         {formatCurrency(row.spent, row.budget.currencyCode)} / {formatCurrency(row.budget.limitAmountMinor, row.budget.currencyCode)}
                      </span>
                   </div>
                   
                   <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                     <div style={{ flex: 1, height: '6px', background: 'rgba(255, 255, 255, 0.1)', borderRadius: '3px', overflow: 'hidden' }}>
                       <div style={{ height: '100%', background: row.isOver ? 'var(--negative)' : 'var(--accent)', width: `${Math.min(row.percent, 100)}%`, borderRadius: '3px' }} />
                     </div>
                     <span style={{ fontSize: '13px', color: row.isOver ? 'var(--negative)' : 'var(--text-muted)', fontWeight: 'bold' }}>
                        {row.percent.toFixed(0)}%
                     </span>
                   </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {activeTool === 'RECURRING' && (
        <section style={{ position: 'fixed', inset: 0, background: 'var(--bg)', zIndex: 100, overflowY: 'auto', padding: '16px', paddingBottom: '40px' }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: '24px' }}>
            <button onClick={() => setActiveTool('NONE')} style={{ background: 'var(--surface)', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', width: '40px', height: '40px', borderRadius: '12px', cursor: 'pointer' }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
            </button>
            <h2 style={{ marginLeft: '16px', fontSize: '20px' }}>{lang === 'vi' ? 'Giao dịch định kỳ' : 'Recurring'}</h2>
          </div>

          <div style={{ display: 'flex', gap: '16px', marginBottom: '24px' }}>
            <div style={{ flex: 1, background: 'var(--surface)', borderRadius: '20px', padding: '20px', boxShadow: 'var(--shadow)' }}>
              <div style={{ fontSize: '13px', color: 'var(--accent)', fontWeight: 'bold', marginBottom: '12px' }}>{lang === 'vi' ? 'Tổng thu' : 'Total Income'}</div>
              <div style={{ fontSize: '20px', fontWeight: 'bold' }}>
                {formatCurrency(recurrings.filter(r => r.type === 'INCOME').reduce((s, r) => s + r.amount, 0), 'VND')}
              </div>
            </div>
            <div style={{ flex: 1, background: 'var(--surface)', borderRadius: '20px', padding: '20px', boxShadow: 'var(--shadow)' }}>
              <div style={{ fontSize: '13px', color: 'var(--negative)', fontWeight: 'bold', marginBottom: '12px' }}>{lang === 'vi' ? 'Tổng chi' : 'Total Expense'}</div>
              <div style={{ fontSize: '20px', fontWeight: 'bold' }}>
                {formatCurrency(recurrings.filter(r => r.type === 'EXPENSE').reduce((s, r) => s + r.amount, 0), 'VND')}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', padding: '0 4px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 'bold' }}>{lang === 'vi' ? 'Lịch giao dịch' : 'Transaction schedule'}</h3>
            <button onClick={() => openRecurringModal()} style={{ background: 'transparent', border: 'none', color: 'var(--accent)', fontWeight: 'bold', fontSize: '14px', cursor: 'pointer' }}>
              {lang === 'vi' ? 'Tạo mới' : 'Create new'}
            </button>
          </div>

          <div className="stack">
            {recurrings.length === 0 ? (
              <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '60px 20px', textAlign: 'center', boxShadow: 'var(--shadow)' }}>
                <p style={{ color: 'var(--text-muted)' }}>{lang === 'vi' ? 'Chưa có giao dịch định kỳ nào.' : 'No recurring transactions.'}</p>
              </div>
            ) : (
              recurrings.map((rec) => (
                <div className="transaction-row" key={rec.id} onClick={() => openRecurringModal(rec)} style={{ display: 'flex', cursor: 'pointer', background: 'var(--surface)', borderRadius: '16px', padding: '16px', border: 'none', boxShadow: 'var(--shadow)' }}>
                  <div style={{ flex: 1 }}>
                    <strong style={{ fontSize: '15px', display: 'block', marginBottom: '4px' }}>{rec.category}</strong>
                    <span style={{ fontSize: '13px', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>{lang === 'vi' ? 'Ngày' : 'Day'} {rec.dayOfMonth} {lang === 'vi' ? 'hàng tháng' : 'every month'}</span>
                    <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{rec.sourceName} {rec.note ? `• ${rec.note}` : ''}</span>
                  </div>
                  <div className="row-right" style={{ textAlign: 'right' }}>
                    <b className={rec.type === 'INCOME' ? 'income' : 'expense'} style={{ fontSize: '16px' }}>
                      {rec.type === 'INCOME' ? '+' : '-'}{formatCurrency(rec.amount, rec.currency)}
                    </b>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>
      )}

      {activeTool === 'GOAL' && (
        <section style={{ position: 'fixed', inset: 0, background: 'var(--bg)', zIndex: 100, overflowY: 'auto', padding: '16px', paddingBottom: '40px' }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: '24px' }}>
            <button onClick={() => setActiveTool('NONE')} style={{ background: 'var(--surface)', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', width: '40px', height: '40px', borderRadius: '12px', cursor: 'pointer' }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
            </button>
            <h2 style={{ marginLeft: '16px', fontSize: '20px' }}>{lang === 'vi' ? 'Mục tiêu tiết kiệm' : 'Saving Goals'}</h2>
          </div>

          <div style={{ background: 'var(--surface)', borderRadius: '20px', padding: '24px', marginBottom: '24px', boxShadow: 'var(--shadow)' }}>
            <div style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px' }}>{lang === 'vi' ? 'Tổng tích lũy' : 'Total accumulated'}</div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '16px' }}>
              <div style={{ fontSize: '28px', fontWeight: 'bold', color: 'var(--accent)' }}>
                {formatCurrency(savingGoals.reduce((sum, g) => sum + Number(g.currentAmount), 0), 'VND')}
              </div>
              <div style={{ fontSize: '16px', color: 'var(--accent)', fontWeight: 'bold' }}>
                {savingGoals.reduce((sum, g) => sum + Number(g.targetAmount), 0) > 0 ? Math.min(100, Math.round((savingGoals.reduce((sum, g) => sum + Number(g.currentAmount), 0) / savingGoals.reduce((sum, g) => sum + Number(g.targetAmount), 0)) * 100)) : 0}%
              </div>
            </div>
            <progress className="goal-progress" max="100" value={savingGoals.reduce((sum, g) => sum + Number(g.targetAmount), 0) > 0 ? Math.min(100, Math.round((savingGoals.reduce((sum, g) => sum + Number(g.currentAmount), 0) / savingGoals.reduce((sum, g) => sum + Number(g.targetAmount), 0)) * 100)) : 0} style={{ width: '100%', height: '8px', borderRadius: '4px' }} />
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', padding: '0 4px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 'bold' }}>{lang === 'vi' ? 'Danh sách mục tiêu' : 'Goal list'}</h3>
            <button onClick={() => openGoalModal()} style={{ background: 'transparent', border: 'none', color: 'var(--accent)', fontWeight: 'bold', fontSize: '14px', cursor: 'pointer' }}>
              {lang === 'vi' ? 'Tạo mới' : 'Create new'}
            </button>
          </div>

          <div className="stack">
            {savingGoals.length === 0 ? (
              <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '60px 20px', textAlign: 'center', boxShadow: 'var(--shadow)' }}>
                <p style={{ color: 'var(--text-muted)' }}>{lang === 'vi' ? 'Chưa có mục tiêu nào.' : 'No saving goals.'}</p>
              </div>
            ) : (
              savingGoals.map((goal) => {
                const progress = Math.min(100, Math.round((goal.currentAmount / goal.targetAmount) * 100))
                return (
                  <div className="transaction-row" key={goal.id} onClick={() => openGoalModal(goal)} style={{ display: 'block', cursor: 'pointer', background: 'var(--surface)', borderRadius: '16px', padding: '16px', border: 'none', boxShadow: 'var(--shadow)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
                      <strong style={{ fontSize: '15px' }}>{goal.name}</strong>
                      <span style={{ fontSize: '14px', color: 'var(--accent)', fontWeight: 'bold' }}>{progress}%</span>
                    </div>
                    <progress className="goal-progress" max="100" value={progress} style={{ width: '100%', height: '6px', marginBottom: '12px', borderRadius: '3px' }} />
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: 'var(--text-muted)' }}>
                      <span>{formatCurrency(goal.currentAmount, goal.currency)}</span>
                      <span>{formatCurrency(goal.targetAmount, goal.currency)}</span>
                    </div>
                  </div>
                )
              })
            )}
          </div>
        </section>
      )}

      {activeTool === 'EXCHANGE_RATE' && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 100 }}>
          <div className="modal-card" style={{ padding: '24px', textAlign: 'center', width: '90%', maxWidth: '400px' }}>
            <h3 style={{ fontSize: '20px', marginBottom: '8px' }}>{lang === 'vi' ? 'Tỷ giá ngoại tệ' : 'Exchange Rates'}</h3>
            <p style={{ fontSize: '14px', color: 'var(--text-muted)', marginBottom: '24px' }}>
              {lang === 'vi' ? 'Tỷ giá tham chiếu quy đổi sang VND.' : 'Reference exchange rates to VND.'}
            </p>

            <div style={{ maxHeight: '50vh', overflowY: 'auto', marginBottom: '20px', paddingRight: '4px' }}>
              {loadingRates && <p>{lang === 'vi' ? 'Đang tải...' : 'Loading...'}</p>}
              {!loadingRates && exchangeRatesData.map((rate, i) => {
                const valStr = rate.Transfer || rate.Buy || '0';
                const num = parseFloat(valStr.replace(/,/g, ''));
                const formattedRate = isNaN(num) ? valStr : new Intl.NumberFormat('vi-VN').format(num);

                return (
                  <div key={i} style={{ display: 'flex', alignItems: 'center', padding: '12px 0', borderBottom: '1px solid var(--border)' }}>
                    <div style={{ width: '36px', height: '36px', borderRadius: '50%', background: '#E8F5E9', color: 'var(--accent)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', marginRight: '16px', flexShrink: 0 }}>
                      {rate.CurrencyCode[0]}
                    </div>
                    <strong style={{ fontSize: '16px', marginRight: 'auto' }}>1 {rate.CurrencyCode}</strong>
                    <span style={{ fontSize: '16px', fontWeight: '500', color: 'var(--accent)' }}>= {formattedRate} VND</span>
                  </div>
                )
              })}
              {!loadingRates && exchangeRatesData.length === 0 && (
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
              onClick={() => setActiveTool('NONE')}
            >
              {lang === 'vi' ? 'Đóng' : 'Close'}
            </button>
          </div>
        </div>
      )}

      {showPinSetupModal && (
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
                value={newPinInput}
                onChange={(e) => {
                  const val = e.target.value.replace(/\D/g, '')
                  if (val.length <= 4) setNewPinInput(val)
                }}
                style={{ width: '100%', padding: '16px', fontSize: '24px', letterSpacing: '12px', textAlign: 'center', border: '2px solid var(--accent)', borderRadius: '16px', background: 'transparent', color: 'var(--text)' }}
              />
            </div>

            <button 
              className="primary-button" 
              disabled={newPinInput.length !== 4}
              style={{ width: '100%', padding: '14px', fontSize: '16px', borderRadius: '24px', marginBottom: '12px', opacity: newPinInput.length !== 4 ? 0.5 : 1 }} 
              onClick={() => {
                if (newPinInput.length === 4) {
                  localStorage.setItem('kat_pin', newPinInput)
                  setPin(newPinInput)
                  setShowPinSetupModal(false)
                }
              }}
            >
              {lang === 'vi' ? 'Lưu' : 'Save'}
            </button>
            <button 
              style={{ width: '100%', padding: '14px', fontSize: '16px', borderRadius: '24px', background: 'var(--bg)', border: 'none', color: 'var(--text)', fontWeight: 'bold' }} 
              onClick={() => setShowPinSetupModal(false)}
            >
              {lang === 'vi' ? 'Đóng' : 'Close'}
            </button>
          </div>
        </div>
      )}

      {activeTool === 'CATEGORY' && (
        <section style={{ position: 'fixed', inset: 0, background: 'var(--bg)', zIndex: 100, overflowY: 'auto', padding: '16px', paddingBottom: '40px' }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: '24px' }}>
            <button onClick={() => setActiveTool('NONE')} style={{ background: 'var(--surface)', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', width: '40px', height: '40px', borderRadius: '12px', cursor: 'pointer' }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
            </button>
            <h1 style={{ fontSize: '20px', marginLeft: '16px', fontWeight: 'bold' }}>{lang === 'vi' ? 'Quản lý danh mục' : 'Categories'}</h1>
          </div>

          <div className="category-filter-bar" style={{ marginBottom: '16px' }}>
            <label>
              <span style={{ fontSize: '14px', color: 'var(--text-muted)' }}>Nhóm</span>
              <select
                value={categoryTypeFilter}
                onChange={(event) => setCategoryTypeFilter(event.target.value as CategoryTypeFilter)}
                style={{ padding: '8px', borderRadius: '12px', border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)', marginLeft: '12px', outline: 'none' }}
              >
                <option value="ALL">Tất cả</option>
                <option value="INCOME">Thu nhập</option>
                <option value="EXPENSE">Chi tiêu</option>
              </select>
            </label>
          </div>

          <div className="stack" style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {filteredCategories.map((category) => (
              <div className="category-row" key={category.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'var(--surface)', padding: '16px', borderRadius: '20px', border: '1px solid var(--border)' }}>
                <div>
                  <strong style={{ display: 'block', fontSize: '16px', marginBottom: '4px' }}>{category.emoji ? `${category.emoji} ${category.name}` : category.name}</strong>
                  <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>{category.type === 'INCOME' ? 'Thu nhập' : 'Chi tiêu'}</span>
                </div>
                <div className="row-actions" style={{ display: 'flex', gap: '8px' }}>
                  <button type="button" className="mini-icon" onClick={() => openCategoryModal(category)} aria-label="Sua danh muc" style={{ width: '36px', height: '36px', borderRadius: '18px', border: 'none', background: 'var(--background)', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', color: 'var(--text)' }}>
                    <Pencil size={14} />
                  </button>
                  <button type="button" className="mini-icon danger" onClick={() => handleDeleteCategory(category)} aria-label="Xoa danh muc" style={{ width: '36px', height: '36px', borderRadius: '18px', border: 'none', background: 'rgba(244, 67, 54, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', color: '#F44336' }}>
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))}
            {filteredCategories.length === 0 && <p className="empty-note" style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '24px' }}>Chưa có danh mục phù hợp.</p>}
          </div>
        </section>
      )}

      {activeTool === 'NONE' && (
        <>
          {activeTab === 'DASHBOARD' && (
            <>
              <section style={{ 
                background: 'var(--surface)', 
                borderRadius: '24px', 
                padding: '32px 16px', 
                textAlign: 'center',
                boxShadow: '0 4px 20px rgba(0,0,0,0.02)',
                marginBottom: '32px'
              }} aria-label="Tai san rong">
                <p style={{ color: 'rgba(255, 255, 255, 0.85)', fontSize: '14px', marginBottom: '12px', fontWeight: 'bold', textTransform: 'uppercase' }}>
                  {lang === 'vi' ? 'Tài sản ròng' : 'Net Worth'}
                </p>
                <strong style={{ fontSize: '36px', display: 'block', marginBottom: '24px', color: '#fff' }}>
                  {formatCurrency(netWorth)}
                </strong>
                
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', justifyContent: 'center' }}>
                  {Object.entries(netWorthByCurrency).filter(([c, val]) => c !== 'VND' && val !== 0).map(([cur, val]) => (
                    <span key={cur} style={{ 
                      padding: '8px 16px', 
                      borderRadius: '20px', 
                      fontSize: '14px', 
                      fontWeight: 'bold', 
                      background: val >= 0 ? 'rgba(46, 204, 113, 0.1)' : 'rgba(231, 76, 60, 0.1)',
                      color: val >= 0 ? 'var(--positive)' : 'var(--negative)',
                      border: `1px solid ${val >= 0 ? 'rgba(46, 204, 113, 0.2)' : 'rgba(231, 76, 60, 0.2)'}`
                    }}>
                      {val > 0 ? '+' : ''}{val} {cur}
                    </span>
                  ))}
                </div>
              </section>

              <section style={{ marginBottom: '32px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                  <h2 style={{ fontSize: '18px', margin: 0, fontWeight: 'bold' }}>{lang === 'vi' ? 'Tài khoản' : 'Accounts'}</h2>
                  <button type="button" onClick={() => openSourceModal()} style={{ color: 'var(--positive)', background: 'none', border: 'none', fontWeight: 'bold', fontSize: '15px' }}>
                    {lang === 'vi' ? 'Thêm' : 'Add'}
                  </button>
                </div>
                <div style={{ display: 'flex', overflowX: 'auto', gap: '16px', paddingBottom: '8px', scrollSnapType: 'x mandatory' }}>
                  {sources.map((source) => (
                    <div key={source.id} onClick={() => openSourceModal(source)} style={{ 
                      background: 'var(--surface)', 
                      borderRadius: '20px', 
                      padding: '24px 20px', 
                      minWidth: '160px',
                      scrollSnapAlign: 'start',
                      boxShadow: '0 4px 16px rgba(0,0,0,0.02)'
                    }}>
                      <strong style={{ display: 'block', fontSize: '16px', marginBottom: '16px' }}>{source.name}</strong>
                      <span style={{ fontSize: '14px', color: 'var(--text-muted)' }}>{formatCurrency(balancesBySource[source.name] ?? 0)}</span>
                    </div>
                  ))}
                  {sources.length === 0 && <p className="empty-note">Chưa có tài khoản.</p>}
                </div>
              </section>

              <section style={{ marginTop: '24px' }}>
                <h2 style={{ fontSize: '18px', marginBottom: '12px', fontWeight: 'bold' }}>{lang === 'vi' ? 'Giao dịch gần đây' : 'Recent Transactions'}</h2>
                
                <div style={{ marginBottom: '16px' }}>
                  <input 
                    type="text"
                    placeholder={lang === 'vi' ? 'Tìm kiếm danh mục, ghi chú hoặc số tiền' : 'Search category, note or amount'}
                    style={{ width: '100%', padding: '16px 20px', borderRadius: '24px', border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)', fontSize: '15px', outline: 'none' }}
                  />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {transactions.slice(0, showMoreTx ? undefined : 5).map((transaction) => (
                    <div key={transaction.id} onClick={() => openTxModal(transaction)} className="transaction-row" style={{ cursor: 'pointer' }}>
                      <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                        <div style={{ width: '4px', height: '36px', borderRadius: '4px', background: transaction.type === 'INCOME' ? 'var(--positive)' : 'var(--negative)', marginTop: '4px' }} />
                        <div>
                          <strong style={{ display: 'block', fontSize: '16px', marginBottom: '4px' }}>{transaction.category}</strong>
                          <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
                            {transaction.note && <span style={{ display: 'block', marginBottom: '2px' }}>{transaction.note}</span>}
                            {transaction.sourceName} • {format(transaction.timestamp, 'dd/MM/yyyy HH:mm')}
                          </span>
                        </div>
                      </div>
                      <b style={{ fontSize: '16px', whiteSpace: 'nowrap', marginLeft: '12px' }} className={transaction.type === 'INCOME' ? 'income' : 'expense'}>
                        {transaction.type === 'INCOME' ? '+' : '-'}{formatCurrency(transaction.amount, transaction.currency)}
                      </b>
                    </div>
                  ))}
                  {transactions.length === 0 && <p style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '24px' }}>{lang === 'vi' ? 'Không có giao dịch nào.' : 'No transactions.'}</p>}
                  
                  {transactions.length > 5 && (
                    <button 
                      type="button" 
                      onClick={() => setShowMoreTx(!showMoreTx)}
                      style={{ marginTop: '8px', padding: '16px', background: 'transparent', border: 'none', color: 'var(--accent)', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer', textAlign: 'center' }}
                    >
                      {showMoreTx ? (lang === 'vi' ? 'Thu gọn' : 'Show less') : (lang === 'vi' ? 'Xem thêm' : 'See more')}
                    </button>
                  )}
                </div>
              </section>
            </>
          )}

          <section className="content-grid">


            {activeTab === 'DEBTS' && (
              <section style={{ padding: '16px', paddingBottom: '100px', gridColumn: '1 / -1' }}>
                <div style={{ display: 'flex', background: 'var(--surface)', borderRadius: '24px', padding: '4px', marginBottom: '32px', border: '1px solid var(--border)' }}>
                  <button 
                    type="button" 
                    onClick={() => setDebtTab('CHO_VAY')}
                    style={{ flex: 1, padding: '12px 0', borderRadius: '20px', background: debtTab === 'CHO_VAY' ? 'rgba(76, 175, 80, 0.15)' : 'transparent', color: debtTab === 'CHO_VAY' ? 'var(--accent)' : 'var(--text-muted)', border: 'none', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
                  >
                    {lang === 'vi' ? 'Cho vay' : 'Lending'}
                  </button>
                  <button 
                    type="button" 
                    onClick={() => setDebtTab('DI_VAY')}
                    style={{ flex: 1, padding: '12px 0', borderRadius: '20px', background: debtTab === 'DI_VAY' ? 'rgba(76, 175, 80, 0.15)' : 'transparent', color: debtTab === 'DI_VAY' ? 'var(--accent)' : 'var(--text-muted)', border: 'none', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
                  >
                    {lang === 'vi' ? 'Đi vay' : 'Borrowing'}
                  </button>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                  <h3 style={{ fontSize: '16px', fontWeight: 'bold', margin: 0 }}>
                    {lang === 'vi' ? 'Khoản vay và cho vay' : 'Loans and Debts'}
                  </h3>
                  <button 
                    type="button" 
                    onClick={() => openDebtModal()}
                    style={{ background: 'transparent', border: 'none', color: 'var(--accent)', fontWeight: 'bold', fontSize: '14px', cursor: 'pointer' }}
                  >
                    {lang === 'vi' ? 'Thêm' : 'Add'}
                  </button>
                </div>

                {debts.filter(d => debtTab === 'CHO_VAY' ? d.type === 'LOAN' : d.type === 'DEBT').length === 0 ? (
                  <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '40px 20px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '15px' }}>
                    {lang === 'vi' ? 'Chưa có khoản nợ nào trong mục này' : 'No records in this section'}
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {debts.filter(d => debtTab === 'CHO_VAY' ? d.type === 'LOAN' : d.type === 'DEBT').map((debt) => (
                      <div key={debt.id} onClick={() => openDebtModal(debt)} style={{ background: 'var(--surface)', borderRadius: '20px', padding: '16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', opacity: debt.isPaid ? 0.6 : 1, cursor: 'pointer' }}>
                        <div>
                          <strong style={{ display: 'block', marginBottom: '4px', fontSize: '16px' }}>{debt.personName}</strong>
                          <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
                            {format(debt.timestamp, 'dd/MM/yyyy')}
                            {debt.dueDate ? ` • Hạn: ${format(debt.dueDate, 'dd/MM/yyyy')}` : ''}
                          </span>
                          {debt.note && <span style={{ display: 'block', fontSize: '13px', color: 'var(--text-muted)', marginTop: '4px' }}>{debt.note}</span>}
                        </div>
                        <div style={{ textAlign: 'right' }}>
                          <div style={{ fontWeight: 'bold', fontSize: '16px', color: debt.type === 'DEBT' ? 'var(--accent)' : 'var(--negative)', marginBottom: '8px' }}>
                            {debt.type === 'DEBT' ? '+' : '-'}{formatCurrency(debt.amount, debt.currency)}
                          </div>
                          <button 
                            type="button" 
                            onClick={(e) => { e.stopPropagation(); handleTogglePaidDebt(debt) }}
                            style={{ background: debt.isPaid ? 'rgba(76, 175, 80, 0.15)' : 'transparent', border: debt.isPaid ? 'none' : '1px solid var(--border)', color: debt.isPaid ? 'var(--accent)' : 'var(--text-muted)', padding: '4px 12px', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}
                          >
                            {debt.isPaid ? (lang === 'vi' ? 'Đã trả' : 'Paid') : (lang === 'vi' ? 'Chưa trả' : 'Unpaid')}
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </section>
            )}

            {activeTab === 'REPORTS' && (
              <section style={{ padding: '0 16px 120px' }}>
                <h1 style={{ fontSize: '28px', fontWeight: 'bold', margin: '24px 0 20px', color: 'var(--text)' }}>Báo cáo tài chính</h1>
                
                {/* Date Picker Card */}
                <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '16px', textAlign: 'center', marginBottom: '20px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}>
                  <div style={{ color: 'var(--muted)', fontSize: '14px', marginBottom: '8px' }}>Khoảng thời gian</div>
                  <input type="month" value={reportMonthFilter} onChange={e => setReportMonthFilter(e.target.value)} style={{ background: 'transparent', border: 'none', color: 'var(--text)', fontSize: '18px', fontWeight: 'bold', fontFamily: 'inherit', outline: 'none' }} />
                </div>

                {/* Filters */}
                <div style={{ display: 'flex', gap: '10px', marginBottom: '16px' }} className="hide-scrollbar">
                  <button type="button" onClick={() => setReportTypeFilter('ALL')} style={{ flex: 1, padding: '10px 16px', borderRadius: '16px', background: reportTypeFilter === 'ALL' ? 'rgba(76, 175, 80, 0.15)' : 'var(--surface)', color: reportTypeFilter === 'ALL' ? 'var(--accent)' : 'var(--text)', fontWeight: reportTypeFilter === 'ALL' ? 'bold' : 'normal', border: '1px solid', borderColor: reportTypeFilter === 'ALL' ? 'transparent' : 'var(--border)' }}>Tất cả</button>
                  <button type="button" onClick={() => setReportTypeFilter('EXPENSE')} style={{ flex: 1, padding: '10px 16px', borderRadius: '16px', background: reportTypeFilter === 'EXPENSE' ? 'rgba(76, 175, 80, 0.15)' : 'var(--surface)', color: reportTypeFilter === 'EXPENSE' ? 'var(--accent)' : 'var(--text)', fontWeight: reportTypeFilter === 'EXPENSE' ? 'bold' : 'normal', border: '1px solid', borderColor: reportTypeFilter === 'EXPENSE' ? 'transparent' : 'var(--border)' }}>Chi tiêu</button>
                  <button type="button" onClick={() => setReportTypeFilter('INCOME')} style={{ flex: 1, padding: '10px 16px', borderRadius: '16px', background: reportTypeFilter === 'INCOME' ? 'rgba(76, 175, 80, 0.15)' : 'var(--surface)', color: reportTypeFilter === 'INCOME' ? 'var(--accent)' : 'var(--text)', fontWeight: reportTypeFilter === 'INCOME' ? 'bold' : 'normal', border: '1px solid', borderColor: reportTypeFilter === 'INCOME' ? 'transparent' : 'var(--border)' }}>Thu nhập</button>
                </div>
                <div style={{ display: 'flex', gap: '10px', marginBottom: '24px', overflowX: 'auto' }} className="hide-scrollbar account-slider">
                  <button type="button" onClick={() => setReportCurrencyFilter('ALL')} style={{ flex: '0 0 auto', padding: '8px 16px', borderRadius: '16px', background: reportCurrencyFilter === 'ALL' ? 'rgba(76, 175, 80, 0.15)' : 'var(--surface)', color: reportCurrencyFilter === 'ALL' ? 'var(--accent)' : 'var(--text)', fontWeight: reportCurrencyFilter === 'ALL' ? 'bold' : 'normal', border: '1px solid', borderColor: reportCurrencyFilter === 'ALL' ? 'transparent' : 'var(--border)' }}>Tất cả</button>
                  {['VND', 'USD', 'EUR', 'GBP'].map(curr => (
                    <button key={curr} type="button" onClick={() => setReportCurrencyFilter(curr)} style={{ flex: '0 0 auto', padding: '8px 16px', borderRadius: '16px', background: reportCurrencyFilter === curr ? 'rgba(76, 175, 80, 0.15)' : 'var(--surface)', color: reportCurrencyFilter === curr ? 'var(--accent)' : 'var(--text)', fontWeight: reportCurrencyFilter === curr ? 'bold' : 'normal', border: '1px solid', borderColor: reportCurrencyFilter === curr ? 'transparent' : 'var(--border)' }}>{curr}</button>
                  ))}
                </div>

                {/* Net Cash Flow */}
                <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '24px', marginBottom: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}>
                  <h3 style={{ fontSize: '18px', margin: '0 0 20px' }}>Dòng tiền ròng</h3>
                  <div style={{ background: 'rgba(244, 67, 54, 0.05)', borderRadius: '20px', padding: '24px 16px', textAlign: 'center', marginBottom: '24px' }}>
                    <div style={{ color: 'var(--muted)', fontSize: '14px', marginBottom: '12px' }}>Số dư còn lại</div>
                    <div style={{ color: '#F44336', fontSize: '28px', fontWeight: 'bold', marginBottom: '16px' }}>{formatCurrency(reportIncomeTotal - reportExpenseTotal, reportCurrencyFilter !== 'ALL' ? reportCurrencyFilter : 'VND')}</div>
                    {reportCurrencyFilter === 'ALL' && Object.keys(reportBalancesByCurrency).length > 1 && (
                      <div style={{ display: 'flex', gap: '8px', justifyContent: 'center', flexWrap: 'wrap' }}>
                        {Object.entries(reportBalancesByCurrency).map(([c, amt]) => (
                          <span key={c} style={{ background: amt >= 0 ? 'rgba(76, 175, 80, 0.15)' : 'rgba(244, 67, 54, 0.15)', color: amt >= 0 ? '#4CAF50' : '#F44336', padding: '6px 12px', borderRadius: '16px', fontSize: '13px', fontWeight: 'bold' }}>{amt >= 0 ? '+' : ''}{amt} {c}</span>
                        ))}
                      </div>
                    )}
                  </div>
                  
                  <div style={{ marginBottom: '20px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '14px', fontWeight: 'bold' }}>
                      <span style={{ color: 'var(--muted)' }}>Thu nhập</span>
                      <span>{formatCurrency(reportIncomeTotal, reportCurrencyFilter !== 'ALL' ? reportCurrencyFilter : 'VND')}</span>
                    </div>
                    <div style={{ background: 'var(--background)', borderRadius: '4px', height: '8px', overflow: 'hidden' }}>
                      <div style={{ background: '#4CAF50', height: '100%', width: `${Math.min(100, (reportIncomeTotal / (reportIncomeTotal + reportExpenseTotal || 1)) * 100)}%`, borderRadius: '4px' }} />
                    </div>
                  </div>
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '14px', fontWeight: 'bold' }}>
                      <span style={{ color: 'var(--muted)' }}>Chi tiêu</span>
                      <span>{formatCurrency(reportExpenseTotal, reportCurrencyFilter !== 'ALL' ? reportCurrencyFilter : 'VND')}</span>
                    </div>
                    <div style={{ background: 'var(--background)', borderRadius: '4px', height: '8px', overflow: 'hidden' }}>
                      <div style={{ background: '#F44336', height: '100%', width: `${Math.min(100, (reportExpenseTotal / (reportIncomeTotal + reportExpenseTotal || 1)) * 100)}%`, borderRadius: '4px' }} />
                    </div>
                  </div>
                </div>

                {/* Expense Breakdown */}
                {(reportTypeFilter === 'ALL' || reportTypeFilter === 'EXPENSE') && (
                  <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '24px 24px 0', marginBottom: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}>
                    <div style={{ position: 'relative', height: '200px', overflow: 'hidden', display: 'flex', justifyContent: 'center' }}>
                      <div style={{ position: 'absolute', top: '-40px', width: '300px', height: '300px' }}>
                        <EChart options={categoryPieOptions} style={{ width: '100%', height: '100%' }} />
                      </div>
                      <div style={{ position: 'absolute', bottom: '30px', left: 0, right: 0, textAlign: 'center' }}>
                        <div style={{ color: 'var(--muted)', fontSize: '14px', marginBottom: '4px' }}>Tổng chi tiêu</div>
                        <div style={{ color: '#F44336', fontSize: '24px', fontWeight: 'bold' }}>{formatCurrency(reportExpenseTotal, reportCurrencyFilter !== 'ALL' ? reportCurrencyFilter : 'VND')}</div>
                      </div>
                    </div>
                    
                    <h3 style={{ fontSize: '18px', margin: '0 0 16px' }}>Chi tiêu</h3>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', paddingBottom: '24px' }}>
                      {reportCategoryRows.map(row => (
                        <div key={row.name} style={{ background: 'var(--background)', padding: '16px', borderRadius: '16px' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 'bold', fontSize: '15px' }}>
                              <span style={{ color: row.color }}>●</span> {row.name}
                            </div>
                            <span style={{ fontWeight: 'bold' }}>{row.percent.toFixed(1)}%</span>
                          </div>
                          <div style={{ background: 'var(--border)', height: '6px', borderRadius: '3px', marginBottom: '12px', overflow: 'hidden' }}>
                            <div style={{ background: row.color, height: '100%', width: `${row.percent}%`, borderRadius: '3px' }} />
                          </div>
                          <div style={{ color: 'var(--muted)', fontSize: '14px' }}>{formatCurrency(row.value, reportCurrencyFilter !== 'ALL' ? reportCurrencyFilter : 'VND')}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Budget Comparison */}
                {budgetRows.length > 0 && (
                  <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '24px', marginBottom: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}>
                    <h3 style={{ fontSize: '18px', margin: '0 0 20px' }}>Đối chiếu ngân sách</h3>
                    
                    <div style={{ background: totalBudgetSpent > totalBudgetLimit ? 'rgba(244, 67, 54, 0.05)' : 'rgba(76, 175, 80, 0.05)', borderRadius: '20px', padding: '20px', marginBottom: '20px', border: `1px solid ${totalBudgetSpent > totalBudgetLimit ? 'rgba(244, 67, 54, 0.2)' : 'rgba(76, 175, 80, 0.2)'}` }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px', fontSize: '15px' }}>
                        <span style={{ color: 'var(--muted)' }}>Đã chi</span>
                        <span style={{ fontWeight: 'bold' }}>{formatCurrency(totalBudgetSpent, currency)} / {formatCurrency(totalBudgetLimit, currency)}</span>
                      </div>
                      <div style={{ background: 'var(--border)', height: '8px', borderRadius: '4px', overflow: 'hidden' }}>
                        <div style={{ background: totalBudgetSpent > totalBudgetLimit ? '#F44336' : '#4CAF50', height: '100%', width: `${Math.min(100, totalBudgetPercent)}%`, borderRadius: '4px' }} />
                      </div>
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                      {budgetRows.map(row => (
                        <div key={row.budget.id} style={{ border: '1px solid var(--border)', borderRadius: '16px', padding: '16px' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
                            <strong style={{ fontSize: '15px' }}>{row.category?.name}</strong>
                            <span style={{ fontWeight: 'bold', color: row.isOver ? '#F44336' : '#4CAF50' }}>{row.percent}%</span>
                          </div>
                          <div style={{ background: 'var(--background)', height: '6px', borderRadius: '3px', marginBottom: '12px', overflow: 'hidden' }}>
                            <div style={{ background: row.isOver ? '#F44336' : '#4CAF50', height: '100%', width: `${Math.min(100, row.percent)}%`, borderRadius: '3px' }} />
                          </div>
                          <div style={{ color: 'var(--muted)', fontSize: '13px' }}>{formatCurrency(row.spent, row.budget.currencyCode)} / {formatCurrency(row.budget.limitAmountMinor, row.budget.currencyCode)}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Daily Trend */}
                <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '24px', marginBottom: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}>
                  <h3 style={{ fontSize: '18px', textAlign: 'center', margin: '0 0 4px' }}>Xu hướng chi tiêu hàng ngày</h3>
                  <p style={{ color: 'var(--muted)', fontSize: '13px', textAlign: 'center', margin: '0 0 24px' }}>Chạm hoặc kéo để xem chi tiết</p>
                  <EChart options={dailyTrendOptions} style={{ height: '240px' }} />
                </div>

                {/* Comparison */}
                <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '24px', marginBottom: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}>
                  <h3 style={{ fontSize: '18px', textAlign: 'center', margin: '0 0 16px' }}>So sánh chi tiêu</h3>
                  <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                    <span style={{ background: 'rgba(244, 67, 54, 0.1)', color: '#F44336', padding: '6px 16px', borderRadius: '16px', fontSize: '14px', fontWeight: 'bold' }}>
                      {prevPeriodExpenseTotal === 0 ? '▲ +100.0%' : `${reportExpenseTotal >= prevPeriodExpenseTotal ? '▲' : '▼'} ${(Math.abs(reportExpenseTotal - prevPeriodExpenseTotal) / prevPeriodExpenseTotal * 100).toFixed(1)}%`}
                    </span>
                  </div>

                  <EChart options={comparisonOptions} style={{ height: '200px' }} />
                </div>
              </section>
            )}
          </section>
        </>
      )}

      {activeTab === 'SETTINGS' && (
        <section style={{ padding: '0 0 80px' }}>
          <div className="settings-header" style={{ marginTop: '24px' }}>{lang === 'vi' ? 'Đơn vị tiền tệ mặc định' : 'Default Currency'}</div>
          <div className="currency-pills" style={{ display: 'flex', overflowX: 'auto', gap: '8px', padding: '0 16px', marginBottom: '24px', scrollbarWidth: 'none', msOverflowStyle: 'none' }}>
            {SUPPORTED_CURRENCIES.map((curr) => (
              <button 
                key={curr} 
                className={`currency-pill ${currency === curr ? 'active' : ''}`}
                onClick={() => setCurrency(curr)}
                style={{ flexShrink: 0 }}
              >
                {curr}
              </button>
            ))}
          </div>

          <div className="settings-header">{lang === 'vi' ? 'Công cụ tài chính' : 'Financial Tools'}</div>
          <div className="settings-card" style={{ margin: '0 16px' }}>
            <button className="settings-list-item" onClick={() => setActiveTool('CATEGORY')}>
              <div className="settings-list-content">
                <span className="settings-list-title">{lang === 'vi' ? 'Quản lý danh mục' : 'Categories'}</span>
                <span className="settings-list-desc">{lang === 'vi' ? 'Tạo, sửa, xóa danh mục thu chi' : 'Create, edit, or delete categories'}</span>
              </div>
            </button>
            <button className="settings-list-item" onClick={() => setActiveTool('BUDGET')}>
              <div className="settings-list-content">
                <span className="settings-list-title">{lang === 'vi' ? 'Ngân sách hằng tháng' : 'Monthly Budget'}</span>
                <span className="settings-list-desc">{lang === 'vi' ? 'Theo dõi hạn mức chi theo danh mục' : 'Track category spending limits'}</span>
              </div>
            </button>
            <button className="settings-list-item" onClick={() => setActiveTool('RECURRING')}>
              <div className="settings-list-content">
                <span className="settings-list-title">{lang === 'vi' ? 'Giao dịch định kỳ' : 'Recurring Transactions'}</span>
                <span className="settings-list-desc">{lang === 'vi' ? 'Tự động ghi nhận các khoản thu chi định kỳ' : 'Automatically record recurring income and expenses'}</span>
              </div>
            </button>
            <button className="settings-list-item" onClick={() => setActiveTool('GOAL')}>
              <div className="settings-list-content">
                <span className="settings-list-title">{lang === 'vi' ? 'Mục tiêu tiết kiệm' : 'Saving Goals'}</span>
                <span className="settings-list-desc">{lang === 'vi' ? 'Theo dõi tiến độ tích lũy cho từng mục tiêu' : 'Track progress toward each goal'}</span>
              </div>
            </button>
            <button className="settings-list-item" onClick={() => setActiveTool('EXCHANGE_RATE')}>
              <div className="settings-list-content">
                <span className="settings-list-title">{lang === 'vi' ? 'Tỷ giá quy đổi' : 'Exchange Rates'}</span>
                <span className="settings-list-desc">{lang === 'vi' ? 'Xem tỷ giá ngoại tệ đang áp dụng' : 'View active exchange rates'}</span>
              </div>
            </button>
            <button className="settings-list-item" onClick={() => setShowCsvModal(true)}>
              <div className="settings-list-content">
                <span className="settings-list-title">{lang === 'vi' ? 'Xuất báo cáo CSV' : 'Export CSV Report'}</span>
                <span className="settings-list-desc">{lang === 'vi' ? 'Tạo báo cáo giao dịch dạng CSV' : 'Create a CSV transaction report'}</span>
              </div>
            </button>
          </div>

          <div className="settings-header">{lang === 'vi' ? 'Hệ thống & Bảo mật' : 'System & Security'}</div>
          <div className="settings-card" style={{ margin: '0 16px' }}>
            <div className="settings-list-item" style={{ cursor: 'default' }}>
              <div className="settings-list-content">
                <span className="settings-list-title">{lang === 'vi' ? 'Khóa ứng dụng' : 'App Lock'}</span>
                <span className="settings-list-desc">{lang === 'vi' ? 'Bảo vệ ứng dụng bằng mã PIN' : 'Protect the app with a PIN'}</span>
              </div>
              <label className="kat-switch">
                <input type="checkbox" checked={!!pin} onChange={(e) => {
                  if (!e.target.checked) {
                    localStorage.removeItem('kat_pin')
                    setPin('')
                  } else {
                    setNewPinInput('')
                    setShowPinSetupModal(true)
                  }
                }} />
                <span className="kat-slider" />
              </label>
            </div>

            <div className="settings-list-item" style={{ cursor: 'default' }}>
              <div className="settings-list-content">
                <span className="settings-list-title">{lang === 'vi' ? 'Giao diện tối' : 'Dark Theme'}</span>
                <span className="settings-list-desc">{lang === 'vi' ? 'Chuyển đổi chế độ hiển thị của ứng dụng' : 'Switch the app display mode'}</span>
              </div>
              <label className="kat-switch">
                <input type="checkbox" checked={theme === 'dark' || (theme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)} onChange={(e) => {
                  setTheme(e.target.checked ? 'dark' : 'light')
                }} />
                <span className="kat-slider" />
              </label>
            </div>
          </div>

          <div className="settings-header">{lang === 'vi' ? 'Đồng bộ & Sao lưu' : 'Sync & Backup'}</div>
          <div className="settings-card" style={{ margin: '0 16px' }}>
            <button className="settings-list-item" onClick={handleExportBackup}>
              <div className="settings-list-content">
                <span className="settings-list-title">{lang === 'vi' ? 'Sao lưu thủ công' : 'Manual Backup'}</span>
                <span className="settings-list-desc">{lang === 'vi' ? 'Xuất dữ liệu hiện tại thành file sao lưu' : 'Export current data to a backup file'}</span>
              </div>
            </button>
            <button className="settings-list-item" onClick={handlePickImportFile}>
              <div className="settings-list-content">
                <span className="settings-list-title">{lang === 'vi' ? 'Khôi phục dữ liệu' : 'Restore Data'}</span>
                <span className="settings-list-desc">{lang === 'vi' ? 'Nhập dữ liệu từ file sao lưu' : 'Import data from a backup file'}</span>
              </div>
            </button>
            <input
              ref={fileInputRef}
              type="file"
              className="visually-hidden"
              accept=".json,.kat,application/json,text/plain"
              onChange={handleImportBackup}
            />
          </div>

          <div className="settings-header">{lang === 'vi' ? 'Thông tin & Hỗ trợ' : 'Info & Support'}</div>
          <div className="settings-card" style={{ margin: '0 16px' }}>
            <button className="settings-list-item" onClick={() => setShowSupportModal(true)}>
              <div className="settings-list-content">
                <span className="settings-list-title">{lang === 'vi' ? 'Hỗ trợ' : 'Support'}</span>
                <span className="settings-list-desc">{lang === 'vi' ? 'Liên hệ khi cần hỗ trợ hoặc báo lỗi' : 'Contact support or report an issue'}</span>
              </div>
            </button>
            <button className="settings-list-item" onClick={() => setShowAboutModal(true)}>
              <div className="settings-list-content">
                <span className="settings-list-title">{lang === 'vi' ? 'Giới thiệu' : 'About'}</span>
                <span className="settings-list-desc">{lang === 'vi' ? 'Thông tin phiên bản và nhà phát triển' : 'Version and developer information'}</span>
              </div>
            </button>

            <button className="settings-list-item" onClick={() => setShowDonateModal(true)}>
              <div className="settings-list-content">
                <span className="settings-list-title">{lang === 'vi' ? 'Ủng hộ nhà phát triển' : 'Support Developer'}</span>
                <span className="settings-list-desc">{lang === 'vi' ? 'Hỗ trợ quá trình duy trì và cải thiện ứng dụng' : 'Support the maintenance and improvement of the app'}</span>
              </div>
            </button>
          </div>
        </section>
      )}



      <nav className="bottom-nav" aria-label="Dieu huong chinh">
        <button className={activeTab === 'DASHBOARD' && activeTool === 'NONE' ? 'active' : ''} onClick={() => { setActiveTab('DASHBOARD'); setActiveTool('NONE') }} type="button">
          <Wallet size={20} fill={activeTab === 'DASHBOARD' && activeTool === 'NONE' ? 'currentColor' : 'none'} />
          {lang === 'vi' ? 'Tổng quan' : 'Overview'}
        </button>
        <button className={activeTab === 'DEBTS' && activeTool === 'NONE' ? 'active' : ''} onClick={() => { setActiveTab('DEBTS'); setActiveTool('NONE') }} type="button">
          <Users size={20} fill={activeTab === 'DEBTS' && activeTool === 'NONE' ? 'currentColor' : 'none'} />
          {lang === 'vi' ? 'Vay nợ' : 'Debts'}
        </button>
        <div className="fab-container">
          <button className="fab-button" onClick={() => openTxModal()} type="button">
            <Plus size={32} color="currentColor" strokeWidth={2.5} />
          </button>
        </div>
        <button className={activeTab === 'REPORTS' && activeTool === 'NONE' ? 'active' : ''} onClick={() => { setActiveTab('REPORTS'); setActiveTool('NONE') }} type="button">
          <BarChart3 size={20} fill={activeTab === 'REPORTS' && activeTool === 'NONE' ? 'currentColor' : 'none'} />
          {lang === 'vi' ? 'Báo cáo' : 'Reports'}
        </button>
        <button className={activeTab === 'SETTINGS' || activeTool !== 'NONE' ? 'active' : ''} onClick={() => { setActiveTab('SETTINGS'); setActiveTool('NONE') }} type="button">
          <Settings size={20} fill={activeTab === 'SETTINGS' || activeTool !== 'NONE' ? 'currentColor' : 'none'} />
          {lang === 'vi' ? 'Cài đặt' : 'Settings'}
        </button>
      </nav>

      

      {showPwaBanner && (
        <div style={{ position: 'fixed', bottom: 84, left: 16, right: 16, background: 'var(--accent)', color: 'white', padding: '12px 16px', borderRadius: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', zIndex: 50, boxShadow: 'var(--shadow)' }}>
          <div style={{ fontSize: 13, lineHeight: 1.4 }}>
            <strong>{lang === 'vi' ? 'Cài đặt KAT Budget' : 'Install KAT Budget'}</strong><br/>
            {lang === 'vi' ? 'Bấm Chia sẻ -> Thêm vào MH chính để dùng như App.' : 'Tap Share -> Add to Home Screen to use as App.'}
          </div>
          <button onClick={() => { setShowPwaBanner(false); sessionStorage.setItem('kat_pwa_dismissed', '1') }} style={{ background: 'none', border: 'none', color: 'white' }}>
            <X size={20} />
          </button>
        </div>
      )}

      {isSourceModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ alignItems: 'center', justifyContent: 'center', padding: '16px' }}>
          <form onSubmit={handleSaveSource} style={{
            background: 'var(--surface)',
            width: '100%',
            maxWidth: '400px',
            borderRadius: '24px',
            padding: '24px',
            boxShadow: '0 10px 40px rgba(0,0,0,0.1)'
          }}>
            <h3 style={{ fontSize: '20px', fontWeight: 'bold', marginBottom: '24px', textAlign: 'center' }}>
              {editingSourceId == null ? 'Tạo tài khoản' : 'Sửa tài khoản'}
            </h3>

            <label style={{ display: 'block', marginBottom: '16px', fontWeight: 'bold', fontSize: '14px', color: 'var(--text-muted)' }}>
              Loại tài khoản
            </label>
            <div style={{ display: 'flex', overflowX: 'auto', gap: '8px', paddingBottom: '8px', marginBottom: '24px', scrollbarWidth: 'none' }}>
              {[{ value: 'BANK', label: 'Ngân hàng' }, { value: 'WALLET', label: 'Ví điện tử' }, { value: 'CASH', label: 'Tiền mặt' }, { value: 'SAVINGS', label: 'Tiết kiệm' }].map(opt => (
                <button 
                  key={opt.value} 
                  type="button" 
                  onClick={() => setSourceForm(prev => ({ ...prev, type: opt.value as MoneySource['type'] }))}
                  style={{
                    padding: '10px 16px',
                    borderRadius: '24px',
                    border: sourceForm.type === opt.value ? '1px solid rgba(46, 204, 113, 0.5)' : '1px solid var(--border)',
                    background: sourceForm.type === opt.value ? 'rgba(46, 204, 113, 0.1)' : 'transparent',
                    color: sourceForm.type === opt.value ? 'var(--positive)' : 'var(--text)',
                    fontWeight: sourceForm.type === opt.value ? 'bold' : 'normal',
                    whiteSpace: 'nowrap',
                    fontSize: '15px'
                  }}
                >
                  {opt.label}
                </button>
              ))}
            </div>

            <div style={{ position: 'relative', marginBottom: '16px' }}>
              <input 
                required
                value={sourceForm.name}
                onChange={(e) => setSourceForm(prev => ({ ...prev, name: e.target.value }))}
                placeholder="Tên tài khoản"
                style={{
                  width: '100%',
                  padding: '16px',
                  borderRadius: '16px',
                  border: '1px solid var(--border)',
                  background: 'transparent',
                  fontSize: '16px',
                  color: 'var(--text)'
                }}
              />
            </div>

            {editingSourceId == null && (
              <div style={{ position: 'relative', marginBottom: '24px', display: 'flex', alignItems: 'center' }}>
                <input 
                  inputMode="decimal"
                  placeholder="Số dư ban đầu"
                  value={sourceForm.initialBalance}
                  onChange={(e) => setSourceForm(prev => ({ ...prev, initialBalance: e.target.value }))}
                  style={{
                    flex: 1,
                    padding: '16px',
                    borderRadius: '16px',
                    border: '1px solid var(--border)',
                    background: 'transparent',
                    fontSize: '16px',
                    color: 'var(--text)'
                  }}
                />
                <button 
                  type="button" 
                  onClick={() => setSourceForm(prev => ({ ...prev, initialBalance: prev.initialBalance + '000' }))}
                  style={{
                    position: 'absolute',
                    right: '8px',
                    padding: '8px 16px',
                    borderRadius: '12px',
                    background: 'rgba(46, 204, 113, 0.15)',
                    color: 'var(--positive)',
                    fontWeight: 'bold',
                    border: 'none',
                    fontSize: '15px'
                  }}
                >
                  +000
                </button>
              </div>
            )}

            {sourceForm.type === 'SAVINGS' && (
              <>
                <div style={{ position: 'relative', marginBottom: '16px', display: 'flex', alignItems: 'center' }}>
                  <input 
                    inputMode="decimal"
                    placeholder="Lãi suất"
                    value={sourceForm.interestRate}
                    onChange={(e) => setSourceForm(prev => ({ ...prev, interestRate: e.target.value }))}
                    style={{
                      flex: 1,
                      padding: '16px',
                      borderRadius: '16px',
                      border: '1px solid var(--border)',
                      background: 'transparent',
                      fontSize: '16px',
                      color: 'var(--text)'
                    }}
                  />
                  <span style={{ position: 'absolute', right: '16px', color: 'var(--text-muted)', fontSize: '15px' }}>%/năm</span>
                </div>

                <label style={{ display: 'block', marginBottom: '12px', fontWeight: 'bold', fontSize: '14px', color: 'var(--text-muted)' }}>
                  Kỳ hạn
                </label>
                <div style={{ display: 'flex', overflowX: 'auto', gap: '8px', paddingBottom: '8px', marginBottom: '24px', scrollbarWidth: 'none' }}>
                  {['1 tháng', '3 tháng', '6 tháng', '12 tháng'].map(opt => (
                    <button 
                      key={opt} 
                      type="button" 
                      onClick={() => setSourceForm(prev => ({ ...prev, interestPeriod: opt }))}
                      style={{
                        padding: '10px 16px',
                        borderRadius: '24px',
                        border: sourceForm.interestPeriod === opt ? '1px solid rgba(46, 204, 113, 0.5)' : '1px solid var(--border)',
                        background: sourceForm.interestPeriod === opt ? 'rgba(46, 204, 113, 0.1)' : 'transparent',
                        color: sourceForm.interestPeriod === opt ? 'var(--positive)' : 'var(--text)',
                        fontWeight: sourceForm.interestPeriod === opt ? 'bold' : 'normal',
                        whiteSpace: 'nowrap',
                        fontSize: '15px'
                      }}
                    >
                      {opt}
                    </button>
                  ))}
                </div>
              </>
            )}

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
              <div style={{ paddingRight: '16px' }}>
                <strong style={{ display: 'block', marginBottom: '4px', fontSize: '16px' }}>Tính vào tài sản ròng</strong>
                <span style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: '1.4', display: 'block' }}>Tắt tùy chọn này nếu tài khoản chỉ dùng để theo dõi riêng.</span>
              </div>
              <label className="kat-switch">
                <input 
                  type="checkbox" 
                  checked={sourceForm.includeInTotal}
                  onChange={(e) => setSourceForm(prev => ({ ...prev, includeInTotal: e.target.checked }))}
                />
                <span className="kat-slider"></span>
              </label>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <button type="submit" style={{ padding: '16px', borderRadius: '16px', background: 'rgba(46, 204, 113, 0.2)', color: 'var(--positive)', fontWeight: 'bold', border: 'none', fontSize: '16px' }}>
                Lưu
              </button>
              <button type="button" onClick={() => setSourceModalOpen(false)} style={{ padding: '16px', borderRadius: '16px', background: 'var(--surface-sunken)', color: 'var(--text)', fontWeight: 'bold', border: 'none', fontSize: '16px' }}>
                Đóng
              </button>
            </div>
          </form>
        </div>
      )}

      {isTxModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ padding: 0, alignItems: 'flex-end', justifyContent: 'center' }}>
          <form 
            className="modal-card" 
            style={{ padding: 0, borderBottomLeftRadius: 0, borderBottomRightRadius: 0, height: '95vh', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}
          >
            <div className="modal-head" style={{ padding: '20px 20px 10px', justifyContent: 'center' }}>
              <h3 style={{ fontSize: '18px' }}>{editingTxId == null ? 'Thêm giao dịch' : 'Sửa giao dịch'}</h3>
            </div>

            <div className="hide-scrollbar" style={{ flex: 1, overflowY: 'auto', padding: '0 20px 20px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
              
              <div style={{ display: 'flex', gap: '10px' }}>
                {['Chi tiêu', 'Thu nhập', 'Chuyển tiền'].map(t => {
                  const val = t === 'Chi tiêu' ? 'EXPENSE' : t === 'Thu nhập' ? 'INCOME' : 'TRANSFER_OUT';
                  const active = txForm.type === val;
                  return (
                    <button 
                      key={t} type="button" 
                      onClick={() => setTxForm(p => ({ ...p, type: val }))}
                      style={{ flex: 1, padding: '10px', borderRadius: '16px', background: active ? 'rgba(76, 175, 80, 0.15)' : 'transparent', border: '1px solid', borderColor: active ? 'rgba(76,175,80,0.5)' : 'var(--border)', color: active ? 'var(--accent)' : 'var(--text)', fontWeight: active ? 'bold' : 'normal', fontSize: '14px' }}
                    >{t}</button>
                  )
                })}
              </div>

              <div className="account-slider" style={{ gap: '10px' }}>
                {['VND', 'USD', 'EUR', 'GBP'].map(curr => (
                  <button 
                    key={curr} type="button" 
                    onClick={() => setTxForm(p => ({ ...p, currency: curr }))}
                    style={{ flex: '0 0 auto', padding: '8px 16px', borderRadius: '16px', background: txForm.currency === curr ? 'rgba(76, 175, 80, 0.15)' : 'transparent', border: '1px solid', borderColor: txForm.currency === curr ? 'rgba(76,175,80,0.5)' : 'var(--border)', color: txForm.currency === curr ? 'var(--accent)' : 'var(--text)', fontWeight: txForm.currency === curr ? 'bold' : 'normal', fontSize: '14px' }}
                  >{curr}</button>
                ))}
              </div>

              <div>
                <label style={{ marginBottom: '8px', display: 'block', fontSize: '13px', color: 'var(--muted)' }}>Tài khoản nguồn</label>
                <div className="account-slider" style={{ gap: '10px' }}>
                  {sources.map(s => (
                    <button 
                      key={s.id} type="button" 
                      onClick={() => setTxForm(p => ({ ...p, sourceName: s.name }))}
                      style={{ flex: '0 0 auto', padding: '8px 16px', borderRadius: '16px', background: txForm.sourceName === s.name ? 'rgba(76, 175, 80, 0.15)' : 'transparent', border: '1px solid', borderColor: txForm.sourceName === s.name ? 'rgba(76,175,80,0.5)' : 'var(--border)', color: txForm.sourceName === s.name ? 'var(--accent)' : 'var(--text)', fontWeight: txForm.sourceName === s.name ? 'bold' : 'normal', fontSize: '14px' }}
                    >{s.name}</button>
                  ))}
                </div>
              </div>

              {txForm.type === 'TRANSFER_OUT' && (
                <div>
                  <label style={{ marginBottom: '8px', display: 'block', fontSize: '13px', color: 'var(--muted)' }}>Tài khoản nhận</label>
                  <div className="account-slider" style={{ gap: '10px' }}>
                    {sources.map(s => (
                      <button 
                        key={s.id} type="button" 
                        onClick={() => setTxForm(p => ({ ...p, destinationName: s.name }))}
                        style={{ flex: '0 0 auto', padding: '8px 16px', borderRadius: '16px', background: txForm.destinationName === s.name ? 'rgba(76, 175, 80, 0.15)' : 'transparent', border: '1px solid', borderColor: txForm.destinationName === s.name ? 'rgba(76,175,80,0.5)' : 'var(--border)', color: txForm.destinationName === s.name ? 'var(--accent)' : 'var(--text)', fontWeight: txForm.destinationName === s.name ? 'bold' : 'normal', fontSize: '14px' }}
                      >{s.name}</button>
                    ))}
                  </div>
                </div>
              )}

              <div>
                <label style={{ marginBottom: '8px', display: 'block', fontSize: '13px', color: 'var(--muted)' }}>Danh mục</label>
                <div className="account-slider" style={{ gap: '10px' }}>
                  {txCategoryPresets.map(c => (
                    <button 
                      key={c.id} type="button" 
                      onClick={() => setTxForm(p => ({ ...p, category: c.name }))}
                      style={{ flex: '0 0 auto', padding: '8px 16px', borderRadius: '16px', background: txForm.category === c.name ? 'rgba(76, 175, 80, 0.15)' : 'transparent', border: '1px solid', borderColor: txForm.category === c.name ? 'rgba(76,175,80,0.5)' : 'var(--border)', color: txForm.category === c.name ? 'var(--accent)' : 'var(--text)', fontWeight: txForm.category === c.name ? 'bold' : 'normal', fontSize: '14px' }}
                    >{c.emoji} {c.name}</button>
                  ))}
                  <button type="button" onClick={() => { setTxModalOpen(false); openCategoryModal(); }} style={{ flex: '0 0 auto', padding: '8px 16px', borderRadius: '16px', background: 'transparent', border: '1px solid var(--border)', color: 'var(--text)', fontSize: '14px' }}>+ Tạo mới</button>
                </div>
              </div>

              <div>
                <label style={{ marginBottom: '8px', display: 'block', fontSize: '13px', color: 'var(--muted)' }}>Ghi chú</label>
                <input 
                  value={txForm.note} onChange={e => setTxForm(p => ({ ...p, note: e.target.value }))}
                  style={{ width: '100%', padding: '12px 16px', borderRadius: '16px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: '15px', outline: 'none' }}
                  placeholder="Nhập ghi chú..."
                />
              </div>

              <div>
                <label style={{ marginBottom: '8px', display: 'block', fontSize: '13px', color: 'var(--muted)' }}>Ngày giao dịch</label>
                <input 
                  type="date"
                  value={txForm.timestamp} onChange={e => setTxForm(p => ({ ...p, timestamp: e.target.value }))}
                  style={{ width: '100%', padding: '12px 16px', borderRadius: '16px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: '15px', outline: 'none' }}
                />
              </div>

              <div>
                {txForm.imageUri ? (
                  <div style={{ position: 'relative', display: 'inline-block' }}>
                    <img src={txForm.imageUri} alt="Invoice" style={{ height: '100px', borderRadius: '12px', objectFit: 'cover' }} />
                    <button type="button" onClick={() => setTxForm(p => ({ ...p, imageUri: '' }))} style={{ position: 'absolute', top: '-8px', right: '-8px', background: 'var(--negative)', color: 'white', borderRadius: '50%', width: '24px', height: '24px', display: 'flex', alignItems: 'center', justifyContent: 'center', border: 'none', cursor: 'pointer', padding: 0 }}>&times;</button>
                  </div>
                ) : (
                  <label style={{ padding: '10px 20px', borderRadius: '16px', background: 'rgba(76, 175, 80, 0.1)', color: 'var(--accent)', fontWeight: 'bold', fontSize: '14px', cursor: 'pointer', display: 'inline-block' }}>
                    + Đính kèm hóa đơn
                    <input type="file" accept="image/*" style={{ display: 'none' }} onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) {
                        const reader = new FileReader();
                        reader.onload = (ev) => {
                          setTxForm(p => ({ ...p, imageUri: ev.target?.result as string }));
                        };
                        reader.readAsDataURL(file);
                      }
                    }} />
                  </label>
                )}
              </div>
            </div>

            <div style={{ padding: '16px 20px', background: 'var(--surface-sunken)', borderTop: '1px solid var(--border)' }}>
              <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '12px' }}>
                <span style={{ fontSize: '32px', fontWeight: 'bold', color: 'var(--text)' }}>
                  {calcInput || '0'}
                </span>
              </div>
              
              <div style={{ display: 'flex', gap: '8px', marginBottom: '16px', overflowX: 'auto' }} className="hide-scrollbar">
                {['37.000', '10.000', '3.000', '50.000'].map(preset => (
                  <button type="button" key={preset} onClick={() => setCalcInput(preset.replace(/\./g, ''))} style={{ padding: '6px 12px', borderRadius: '12px', background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--text)', fontSize: '13px', cursor: 'pointer', whiteSpace: 'nowrap' }}>
                    {preset}
                  </button>
                ))}
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '8px' }}>
                <button type="button" onClick={() => setCalcInput('0')} style={{ padding: '12px', borderRadius: '16px', background: 'rgba(76, 175, 80, 0.1)', color: 'var(--accent)', fontSize: '20px', border: 'none', cursor: 'pointer' }}>C</button>
                <button type="button" onClick={() => setCalcInput(p => p === '0' ? '0' : p + ' ÷ ')} style={{ padding: '12px', borderRadius: '16px', background: 'rgba(76, 175, 80, 0.1)', color: 'var(--accent)', fontSize: '20px', border: 'none', cursor: 'pointer' }}>÷</button>
                <button type="button" onClick={() => setCalcInput(p => p === '0' ? '0' : p + ' × ')} style={{ padding: '12px', borderRadius: '16px', background: 'rgba(76, 175, 80, 0.1)', color: 'var(--accent)', fontSize: '20px', border: 'none', cursor: 'pointer' }}>×</button>
                <button type="button" onClick={() => setCalcInput(p => p.length > 1 ? p.slice(0, -1).trim() : '0')} style={{ padding: '12px', borderRadius: '16px', background: 'rgba(76, 175, 80, 0.1)', color: 'var(--accent)', fontSize: '20px', border: 'none', cursor: 'pointer' }}>⌫</button>

                {[7, 8, 9].map(n => (
                  <button type="button" key={n} onClick={() => setCalcInput(p => p === '0' ? n.toString() : p + n)} style={{ padding: '16px', borderRadius: '16px', background: 'var(--surface)', color: 'var(--text)', fontSize: '20px', border: 'none', cursor: 'pointer' }}>{n}</button>
                ))}
                <button type="button" onClick={() => setCalcInput(p => p === '0' ? '0' : p + ' - ')} style={{ padding: '12px', borderRadius: '16px', background: 'rgba(76, 175, 80, 0.1)', color: 'var(--accent)', fontSize: '24px', border: 'none', cursor: 'pointer' }}>-</button>

                {[4, 5, 6].map(n => (
                  <button type="button" key={n} onClick={() => setCalcInput(p => p === '0' ? n.toString() : p + n)} style={{ padding: '16px', borderRadius: '16px', background: 'var(--surface)', color: 'var(--text)', fontSize: '20px', border: 'none', cursor: 'pointer' }}>{n}</button>
                ))}
                <button type="button" onClick={() => setCalcInput(p => p === '0' ? '0' : p + ' + ')} style={{ padding: '12px', borderRadius: '16px', background: 'rgba(76, 175, 80, 0.1)', color: 'var(--accent)', fontSize: '24px', border: 'none', cursor: 'pointer' }}>+</button>

                {[1, 2, 3].map(n => (
                  <button type="button" key={n} onClick={() => setCalcInput(p => p === '0' ? n.toString() : p + n)} style={{ padding: '16px', borderRadius: '16px', background: 'var(--surface)', color: 'var(--text)', fontSize: '20px', border: 'none', cursor: 'pointer' }}>{n}</button>
                ))}
                <button type="button" onClick={() => handleSaveTransaction()} style={{ padding: '0', borderRadius: '16px', background: 'var(--accent)', color: 'white', fontSize: '24px', border: 'none', cursor: 'pointer', gridRow: 'span 2', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 4px 12px rgba(22, 163, 74, 0.3)' }}>&gt;</button>

                <button type="button" onClick={() => setCalcInput(p => p === '0' ? '0' : p + '0')} style={{ padding: '16px', borderRadius: '16px', background: 'var(--surface)', color: 'var(--text)', fontSize: '20px', border: 'none', cursor: 'pointer' }}>0</button>
                <button type="button" onClick={() => setCalcInput(p => p === '0' ? '0' : p + '000')} style={{ padding: '16px', borderRadius: '16px', background: 'var(--surface)', color: 'var(--text)', fontSize: '18px', border: 'none', cursor: 'pointer' }}>000</button>
                <button type="button" onClick={() => setCalcInput(p => p.includes('.') ? p : p + '.')} style={{ padding: '16px', borderRadius: '16px', background: 'var(--surface)', color: 'var(--text)', fontSize: '24px', border: 'none', cursor: 'pointer', lineHeight: 1 }}>.</button>
              </div>

              <button type="button" onClick={() => setTxModalOpen(false)} style={{ width: '100%', padding: '14px', borderRadius: '16px', background: 'var(--surface)', color: 'var(--text)', fontWeight: 'bold', fontSize: '16px', border: 'none', marginTop: '16px', cursor: 'pointer' }}>
                Đóng
              </button>
            </div>
          </form>
        </div>
      )}

      {isBudgetModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 200 }}>
          <form className="modal-card" onSubmit={handleSaveBudget} style={{ padding: 0, textAlign: 'left', width: '90%', maxWidth: '400px', maxHeight: '90vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            
            <div className="hide-scrollbar" style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
              <h3 style={{ fontSize: '22px', fontWeight: 'bold', marginBottom: '24px' }}>
                {editingBudgetId == null ? (lang === 'vi' ? 'Thiết lập ngân sách' : 'Setup Budget') : (lang === 'vi' ? 'Sửa ngân sách' : 'Edit Budget')}
              </h3>

              <label style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px', display: 'block' }}>{lang === 'vi' ? 'Danh mục' : 'Category'}</label>
              <div className="hide-scrollbar" style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px', marginBottom: '16px', WebkitOverflowScrolling: 'touch' }}>
                {expenseCategories.map(c => (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => setBudgetForm(p => ({ ...p, categoryId: c.id.toString() }))}
                    style={{ flex: '0 0 auto', padding: '12px 24px', borderRadius: '20px', border: '1px solid', background: budgetForm.categoryId === c.id.toString() ? 'rgba(76, 175, 80, 0.15)' : 'transparent', borderColor: budgetForm.categoryId === c.id.toString() ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)', color: budgetForm.categoryId === c.id.toString() ? 'var(--accent)' : 'var(--text)', fontWeight: budgetForm.categoryId === c.id.toString() ? 'bold' : 'normal', cursor: 'pointer' }}
                  >
                    {c.emoji} {c.name}
                  </button>
                ))}
              </div>

              <label style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px', display: 'block' }}>{lang === 'vi' ? 'Tiền tệ' : 'Currency'}</label>
              <div className="hide-scrollbar" style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px', marginBottom: '24px', WebkitOverflowScrolling: 'touch' }}>
                {['VND', 'USD', 'EUR', 'GBP', 'AUD', 'CAD', 'CHF', 'CNY', 'DKK', 'HKD', 'INR', 'JPY', 'KRW', 'KWD', 'MYR', 'NOK', 'RUB', 'SAR', 'SEK', 'SGD', 'THB'].map(curr => (
                  <button
                    key={curr}
                    type="button"
                    onClick={() => setBudgetForm(p => ({ ...p, currencyCode: curr }))}
                    style={{ flex: '0 0 auto', padding: '12px 24px', borderRadius: '20px', border: '1px solid', background: budgetForm.currencyCode === curr ? 'rgba(76, 175, 80, 0.15)' : 'transparent', borderColor: budgetForm.currencyCode === curr ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)', color: budgetForm.currencyCode === curr ? 'var(--accent)' : 'var(--text)', fontWeight: budgetForm.currencyCode === curr ? 'bold' : 'normal', cursor: 'pointer' }}
                  >
                    {curr}
                  </button>
                ))}
              </div>

              <div style={{ display: 'flex', gap: '12px', marginBottom: '16px' }}>
                <div style={{ position: 'relative', flex: 1 }}>
                  <label style={{ position: 'absolute', top: '-10px', left: '16px', background: 'var(--surface)', padding: '0 4px', fontSize: '12px', color: 'var(--text-muted)' }}>
                    {lang === 'vi' ? 'Hạn mức' : 'Limit amount'}
                  </label>
                  <input 
                    required 
                    inputMode="decimal" 
                    value={budgetForm.limitAmount} 
                    onChange={e => setBudgetForm(p => ({ ...p, limitAmount: e.target.value }))}
                    style={{ width: '100%', padding: '16px 20px', borderRadius: '24px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: '15px' }}
                  />
                </div>
                <button 
                  type="button"
                  onClick={() => setBudgetForm(p => ({ ...p, limitAmount: p.limitAmount + '000' }))}
                  style={{ padding: '0 20px', borderRadius: '24px', background: 'rgba(76, 175, 80, 0.15)', color: 'var(--accent)', border: 'none', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
                >
                  +000
                </button>
              </div>

            </div>

            <div style={{ padding: '24px', paddingTop: 0, display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <button 
                type="submit"
                className="primary-button"
                style={{ width: '100%', padding: '16px', borderRadius: '24px', fontWeight: 'bold', fontSize: '15px' }}
              >
                {lang === 'vi' ? 'Lưu' : 'Save'}
              </button>
              <button 
                type="button"
                onClick={() => setBudgetModalOpen(false)}
                style={{ width: '100%', padding: '16px', borderRadius: '24px', background: 'rgba(255, 255, 255, 0.05)', color: 'var(--text)', border: 'none', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
              >
                {lang === 'vi' ? 'Đóng' : 'Close'}
              </button>
              {editingBudgetId != null && (
                <button 
                  type="button"
                  onClick={() => handleDeleteBudget(editingBudgetId)}
                  style={{ width: '100%', padding: '16px', borderRadius: '24px', background: 'transparent', color: 'var(--negative)', border: '1px solid var(--negative)', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
                >
                  {lang === 'vi' ? 'Xóa ngân sách này' : 'Delete this budget'}
                </button>
              )}
            </div>
          </form>
        </div>
      )}

      {isCategoryModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <form className="modal-card" onSubmit={handleSaveCategory}>
            <div className="modal-head">
              <h3>{editingCategoryId == null ? 'Tao danh muc' : 'Sua danh muc'}</h3>
              <button type="button" onClick={() => setCategoryModalOpen(false)}>
                Dong
              </button>
            </div>

            <label>
              Ten danh muc
              <input
                required
                value={categoryForm.name}
                onChange={(event) => setCategoryForm((prev) => ({ ...prev, name: event.target.value }))}
              />
            </label>

            <label>
              Emoji (khong bat buoc)
              <input
                value={categoryForm.emoji}
                onChange={(event) => setCategoryForm((prev) => ({ ...prev, emoji: event.target.value }))}
                placeholder="Vi du: FOOD"
              />
            </label>

            <label>
              Nhom danh muc
              <select
                value={categoryForm.type}
                onChange={(event) =>
                  setCategoryForm((prev) => ({ ...prev, type: event.target.value as CategoryForm['type'] }))
                }
              >
                <option value="EXPENSE">Chi tieu</option>
                <option value="INCOME">Thu nhap</option>
              </select>
            </label>

            <button className="primary-button" type="submit">
              {editingCategoryId == null ? 'Luu danh muc' : 'Cap nhat danh muc'}
            </button>
          </form>
        </div>
      )}

      {isDebtModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 200 }}>
          <form className="modal-card" onSubmit={handleSaveDebt} style={{ padding: 0, textAlign: 'left', width: '90%', maxWidth: '400px', maxHeight: '90vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            
            <div className="hide-scrollbar" style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
              <h3 style={{ fontSize: '22px', fontWeight: 'bold', marginBottom: '24px', textAlign: 'center' }}>
                {editingDebtId == null ? (lang === 'vi' ? 'Thêm khoản vay' : 'Add debt') : (lang === 'vi' ? 'Sửa khoản vay' : 'Edit debt')}
              </h3>

              <div style={{ display: 'flex', background: 'var(--surface)', borderRadius: '24px', padding: '4px', marginBottom: '24px', border: '1px solid var(--border)' }}>
                <button 
                  type="button" 
                  onClick={() => setDebtForm(p => ({ ...p, type: 'LOAN' }))}
                  style={{ flex: 1, padding: '12px 0', borderRadius: '20px', background: debtForm.type === 'LOAN' ? 'rgba(76, 175, 80, 0.15)' : 'transparent', color: debtForm.type === 'LOAN' ? 'var(--accent)' : 'var(--text-muted)', border: 'none', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
                >
                  {lang === 'vi' ? 'Cho vay' : 'Lending'}
                </button>
                <button 
                  type="button" 
                  onClick={() => setDebtForm(p => ({ ...p, type: 'DEBT' }))}
                  style={{ flex: 1, padding: '12px 0', borderRadius: '20px', background: debtForm.type === 'DEBT' ? 'rgba(76, 175, 80, 0.15)' : 'transparent', color: debtForm.type === 'DEBT' ? 'var(--accent)' : 'var(--text-muted)', border: 'none', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
                >
                  {lang === 'vi' ? 'Đi vay' : 'Borrowing'}
                </button>
              </div>

              <div className="hide-scrollbar" style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px', marginBottom: '24px', WebkitOverflowScrolling: 'touch' }}>
                {['VND', 'USD', 'EUR', 'GBP', 'JPY'].map(curr => (
                  <button
                    key={curr}
                    type="button"
                    onClick={() => setDebtForm(p => ({ ...p, currency: curr }))}
                    style={{ flex: '0 0 auto', padding: '10px 20px', borderRadius: '20px', border: '1px solid', background: debtForm.currency === curr ? 'rgba(76, 175, 80, 0.15)' : 'transparent', borderColor: debtForm.currency === curr ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)', color: debtForm.currency === curr ? 'var(--accent)' : 'var(--text)', fontWeight: debtForm.currency === curr ? 'bold' : 'normal', cursor: 'pointer', fontSize: '14px' }}
                  >
                    {curr}
                  </button>
                ))}
              </div>

              <div style={{ position: 'relative', marginBottom: '16px' }}>
                <input 
                  required 
                  placeholder={lang === 'vi' ? 'Tên người liên quan (*)' : 'Person involved (*)'}
                  value={debtForm.personName} 
                  onChange={e => setDebtForm(p => ({ ...p, personName: e.target.value }))}
                  style={{ width: '100%', padding: '16px 20px', borderRadius: '24px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: '15px', outline: 'none' }}
                />
              </div>

              <div style={{ display: 'flex', gap: '12px', marginBottom: '16px' }}>
                <div style={{ position: 'relative', flex: 1 }}>
                  <input 
                    required 
                    inputMode="decimal" 
                    placeholder={lang === 'vi' ? 'Số tiền' : 'Amount'}
                    value={debtForm.amount} 
                    onChange={e => setDebtForm(p => ({ ...p, amount: e.target.value }))}
                    style={{ width: '100%', padding: '16px 20px', borderRadius: '24px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: '15px', outline: 'none' }}
                  />
                </div>
                <button 
                  type="button"
                  onClick={() => setDebtForm(p => ({ ...p, amount: p.amount + '000' }))}
                  style={{ padding: '0 20px', borderRadius: '24px', background: 'rgba(76, 175, 80, 0.15)', color: 'var(--accent)', border: 'none', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
                >
                  +000
                </button>
              </div>

              <div style={{ position: 'relative', marginBottom: '16px' }}>
                <input 
                  placeholder={lang === 'vi' ? 'Ghi chú' : 'Note'}
                  value={debtForm.note} 
                  onChange={e => setDebtForm(p => ({ ...p, note: e.target.value }))}
                  style={{ width: '100%', padding: '16px 20px', borderRadius: '24px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: '15px', outline: 'none' }}
                />
              </div>

              <div style={{ position: 'relative', marginBottom: '24px' }}>
                <div style={{ position: 'absolute', top: '-10px', left: '16px', background: 'var(--surface)', padding: '0 4px', fontSize: '12px', color: 'var(--text-muted)' }}>
                  {lang === 'vi' ? 'Chọn hạn thanh toán (không bắt buộc)' : 'Select due date (optional)'}
                </div>
                <input 
                  type="date"
                  value={debtForm.dueDate} 
                  onChange={e => setDebtForm(p => ({ ...p, dueDate: e.target.value }))}
                  style={{ width: '100%', padding: '16px 20px', borderRadius: '24px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: '15px', outline: 'none' }}
                />
              </div>

            </div>

            <div style={{ padding: '24px', paddingTop: 0, display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <button 
                type="submit"
                className="primary-button"
                style={{ width: '100%', padding: '16px', borderRadius: '24px', fontWeight: 'bold', fontSize: '15px' }}
              >
                {lang === 'vi' ? 'Lưu' : 'Save'}
              </button>
              <button 
                type="button"
                onClick={() => setDebtModalOpen(false)}
                style={{ width: '100%', padding: '16px', borderRadius: '24px', background: 'rgba(255, 255, 255, 0.05)', color: 'var(--text)', border: 'none', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
              >
                {lang === 'vi' ? 'Đóng' : 'Close'}
              </button>
              {editingDebtId != null && (
                <button 
                  type="button"
                  onClick={() => handleDeleteDebt(editingDebtId)}
                  style={{ width: '100%', padding: '16px', borderRadius: '24px', background: 'transparent', color: 'var(--negative)', border: '1px solid var(--negative)', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
                >
                  {lang === 'vi' ? 'Xóa khoản nợ' : 'Delete record'}
                </button>
              )}
            </div>
          </form>
        </div>
      )}
      {isGoalModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 200 }}>
          <div className="modal-card" style={{ padding: 0, textAlign: 'center', width: '90%', maxWidth: '400px', maxHeight: '90vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            
            <div className="hide-scrollbar" style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
              <h3 style={{ fontSize: '20px', fontWeight: 'bold', marginBottom: '24px' }}>
                {editingGoalId == null ? (lang === 'vi' ? 'Mục tiêu mới' : 'New Goal') : (lang === 'vi' ? 'Sửa mục tiêu' : 'Edit Goal')}
              </h3>

              <input 
                required 
                placeholder={lang === 'vi' ? 'Tên mục tiêu' : 'Goal name'}
                value={goalForm.name} 
                onChange={e => setGoalForm(p => ({ ...p, name: e.target.value }))}
                style={{ width: '100%', padding: '16px 20px', borderRadius: '24px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', marginBottom: '16px', fontSize: '15px' }}
              />

              <div style={{ display: 'flex', gap: '12px', marginBottom: '20px' }}>
                <input 
                  required 
                  inputMode="decimal" 
                  placeholder={lang === 'vi' ? 'Số tiền mục tiêu' : 'Target amount'}
                  value={goalForm.targetAmount} 
                  onChange={e => setGoalForm(p => ({ ...p, targetAmount: e.target.value }))}
                  style={{ flex: 1, padding: '16px 20px', borderRadius: '24px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: '15px' }}
                />
                <button 
                  type="button"
                  onClick={() => setGoalForm(p => ({ ...p, targetAmount: p.targetAmount + '000' }))}
                  style={{ padding: '0 20px', borderRadius: '24px', background: 'rgba(76, 175, 80, 0.15)', color: 'var(--accent)', border: 'none', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
                >
                  +000
                </button>
              </div>

              <div className="hide-scrollbar" style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px', marginBottom: '24px', WebkitOverflowScrolling: 'touch' }}>
                {['VND', 'USD', 'EUR', 'GBP', 'AUD', 'CAD', 'CHF', 'CNY', 'DKK', 'HKD', 'INR', 'JPY', 'KRW', 'KWD', 'MYR', 'NOK', 'RUB', 'SAR', 'SEK', 'SGD', 'THB'].map(curr => (
                  <button
                    key={curr}
                    type="button"
                    onClick={() => setGoalForm(p => ({ ...p, currency: curr }))}
                    style={{
                      flex: '0 0 auto',
                      padding: '12px 24px', borderRadius: '20px', border: '1px solid',
                      background: goalForm.currency === curr ? 'rgba(76, 175, 80, 0.15)' : 'transparent',
                      borderColor: goalForm.currency === curr ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)',
                      color: goalForm.currency === curr ? 'var(--accent)' : 'var(--text)',
                      fontWeight: goalForm.currency === curr ? 'bold' : 'normal',
                      cursor: 'pointer'
                    }}
                  >
                    {curr}
                  </button>
                ))}
              </div>

              {editingGoalId != null && (
                <div style={{ marginBottom: '24px' }}>
                  <input 
                    required 
                    inputMode="decimal" 
                    placeholder={lang === 'vi' ? 'Đã tích lũy' : 'Accumulated amount'}
                    value={goalForm.currentAmount} 
                    onChange={e => setGoalForm(p => ({ ...p, currentAmount: e.target.value }))}
                    style={{ width: '100%', padding: '16px 20px', borderRadius: '24px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: '15px' }}
                  />
                </div>
              )}
            </div>

            <div style={{ padding: '24px', paddingTop: 0, display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <button 
                type="button"
                className="primary-button"
                onClick={handleSaveGoal}
                style={{ width: '100%', padding: '16px', borderRadius: '24px', fontWeight: 'bold', fontSize: '15px' }}
              >
                {lang === 'vi' ? 'Lưu' : 'Save'}
              </button>
              <button 
                type="button"
                onClick={() => setGoalModalOpen(false)}
                style={{ width: '100%', padding: '16px', borderRadius: '24px', background: 'rgba(255, 255, 255, 0.05)', color: 'var(--text)', border: 'none', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
              >
                {lang === 'vi' ? 'Đóng' : 'Close'}
              </button>
              {editingGoalId != null && (
                <button 
                  type="button"
                  onClick={() => handleDeleteGoal(editingGoalId)}
                  style={{ width: '100%', padding: '16px', borderRadius: '24px', background: 'transparent', color: 'var(--negative)', border: '1px solid var(--negative)', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
                >
                  {lang === 'vi' ? 'Xóa mục tiêu này' : 'Delete this goal'}
                </button>
              )}
            </div>

          </div>
        </div>
      )}

      {isRecurringModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 200 }}>
          <div className="modal-card" style={{ padding: 0, textAlign: 'left', width: '90%', maxWidth: '400px', maxHeight: '90vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            
            <div className="hide-scrollbar" style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
              <h3 style={{ fontSize: '22px', fontWeight: 'bold', marginBottom: '24px' }}>
                {editingRecurringId == null ? (lang === 'vi' ? 'Thiết lập giao dịch định kỳ' : 'Setup Recurring') : (lang === 'vi' ? 'Sửa giao dịch định kỳ' : 'Edit Recurring')}
              </h3>

              <div style={{ display: 'flex', gap: '12px', marginBottom: '24px' }}>
                <button
                  type="button"
                  onClick={() => setRecurringForm(p => ({ ...p, type: 'EXPENSE' }))}
                  style={{ flex: 1, padding: '12px', borderRadius: '24px', border: '1px solid', background: recurringForm.type === 'EXPENSE' ? 'rgba(76, 175, 80, 0.15)' : 'transparent', borderColor: recurringForm.type === 'EXPENSE' ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)', color: recurringForm.type === 'EXPENSE' ? 'var(--accent)' : 'var(--text)', fontWeight: recurringForm.type === 'EXPENSE' ? 'bold' : 'normal', cursor: 'pointer', textAlign: 'center' }}
                >
                  {lang === 'vi' ? 'Chi định kỳ' : 'Recurring Expense'}
                </button>
                <button
                  type="button"
                  onClick={() => setRecurringForm(p => ({ ...p, type: 'INCOME' }))}
                  style={{ flex: 1, padding: '12px', borderRadius: '24px', border: '1px solid', background: recurringForm.type === 'INCOME' ? 'rgba(76, 175, 80, 0.15)' : 'transparent', borderColor: recurringForm.type === 'INCOME' ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)', color: recurringForm.type === 'INCOME' ? 'var(--accent)' : 'var(--text)', fontWeight: recurringForm.type === 'INCOME' ? 'bold' : 'normal', cursor: 'pointer', textAlign: 'center' }}
                >
                  {lang === 'vi' ? 'Thu định kỳ' : 'Recurring Income'}
                </button>
              </div>

              <label style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px', display: 'block' }}>{lang === 'vi' ? 'Tài khoản' : 'Account'}</label>
              <div className="hide-scrollbar" style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px', marginBottom: '16px', WebkitOverflowScrolling: 'touch' }}>
                {sources.map(s => (
                  <button
                    key={s.id}
                    type="button"
                    onClick={() => setRecurringForm(p => ({ ...p, sourceName: s.name }))}
                    style={{ flex: '0 0 auto', padding: '12px 24px', borderRadius: '20px', border: '1px solid', background: recurringForm.sourceName === s.name ? 'rgba(76, 175, 80, 0.15)' : 'transparent', borderColor: recurringForm.sourceName === s.name ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)', color: recurringForm.sourceName === s.name ? 'var(--accent)' : 'var(--text)', fontWeight: recurringForm.sourceName === s.name ? 'bold' : 'normal', cursor: 'pointer' }}
                  >
                    {s.name}
                  </button>
                ))}
              </div>

              <label style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px', display: 'block' }}>{lang === 'vi' ? 'Tiền tệ' : 'Currency'}</label>
              <div className="hide-scrollbar" style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px', marginBottom: '16px', WebkitOverflowScrolling: 'touch' }}>
                {['VND', 'USD', 'EUR', 'GBP', 'AUD', 'CAD', 'CHF', 'CNY', 'DKK', 'HKD', 'INR', 'JPY', 'KRW', 'KWD', 'MYR', 'NOK', 'RUB', 'SAR', 'SEK', 'SGD', 'THB'].map(curr => (
                  <button
                    key={curr}
                    type="button"
                    onClick={() => setRecurringForm(p => ({ ...p, currency: curr }))}
                    style={{ flex: '0 0 auto', padding: '12px 24px', borderRadius: '20px', border: '1px solid', background: recurringForm.currency === curr ? 'rgba(76, 175, 80, 0.15)' : 'transparent', borderColor: recurringForm.currency === curr ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)', color: recurringForm.currency === curr ? 'var(--accent)' : 'var(--text)', fontWeight: recurringForm.currency === curr ? 'bold' : 'normal', cursor: 'pointer' }}
                  >
                    {curr}
                  </button>
                ))}
              </div>

              <label style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px', display: 'block' }}>{lang === 'vi' ? 'Danh mục' : 'Category'}</label>
              <div className="hide-scrollbar" style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px', marginBottom: '24px', WebkitOverflowScrolling: 'touch' }}>
                {categories.filter(c => c.type === recurringForm.type).map(c => (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => setRecurringForm(p => ({ ...p, category: c.name }))}
                    style={{ flex: '0 0 auto', padding: '12px 24px', borderRadius: '20px', border: '1px solid', background: recurringForm.category === c.name ? 'rgba(76, 175, 80, 0.15)' : 'transparent', borderColor: recurringForm.category === c.name ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)', color: recurringForm.category === c.name ? 'var(--accent)' : 'var(--text)', fontWeight: recurringForm.category === c.name ? 'bold' : 'normal', cursor: 'pointer' }}
                  >
                    {c.emoji} {c.name}
                  </button>
                ))}
              </div>

              <div style={{ position: 'relative', marginBottom: '16px' }}>
                <label style={{ position: 'absolute', top: '-10px', left: '16px', background: 'var(--surface)', padding: '0 4px', fontSize: '12px', color: 'var(--text-muted)' }}>
                  {lang === 'vi' ? 'Ngày chạy hằng tháng: 1-31' : 'Monthly run day: 1-31'}
                </label>
                <input 
                  required 
                  type="number" 
                  min="1" 
                  max="31" 
                  value={recurringForm.dayOfMonth} 
                  onChange={e => setRecurringForm(p => ({ ...p, dayOfMonth: e.target.value }))}
                  style={{ width: '100%', padding: '16px 20px', borderRadius: '24px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: '15px' }}
                />
              </div>

              <div style={{ display: 'flex', gap: '12px', marginBottom: '16px' }}>
                <div style={{ position: 'relative', flex: 1 }}>
                  <label style={{ position: 'absolute', top: '-10px', left: '16px', background: 'var(--surface)', padding: '0 4px', fontSize: '12px', color: 'var(--text-muted)' }}>
                    {lang === 'vi' ? 'Số tiền' : 'Amount'}
                  </label>
                  <input 
                    required 
                    inputMode="decimal" 
                    value={recurringForm.amount} 
                    onChange={e => setRecurringForm(p => ({ ...p, amount: e.target.value }))}
                    style={{ width: '100%', padding: '16px 20px', borderRadius: '24px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: '15px' }}
                  />
                </div>
                <button 
                  type="button"
                  onClick={() => setRecurringForm(p => ({ ...p, amount: p.amount + '000' }))}
                  style={{ padding: '0 20px', borderRadius: '24px', background: 'rgba(76, 175, 80, 0.15)', color: 'var(--accent)', border: 'none', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
                >
                  +000
                </button>
              </div>

              <div style={{ position: 'relative', marginBottom: '24px' }}>
                <label style={{ position: 'absolute', top: '-10px', left: '16px', background: 'var(--surface)', padding: '0 4px', fontSize: '12px', color: 'var(--text-muted)' }}>
                  {lang === 'vi' ? 'Ghi chú' : 'Note'}
                </label>
                <input 
                  value={recurringForm.note} 
                  onChange={e => setRecurringForm(p => ({ ...p, note: e.target.value }))}
                  style={{ width: '100%', padding: '16px 20px', borderRadius: '24px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: '15px' }}
                />
              </div>
            </div>

            <div style={{ padding: '24px', paddingTop: 0, display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <button 
                type="button"
                className="primary-button"
                onClick={handleSaveRecurring}
                style={{ width: '100%', padding: '16px', borderRadius: '24px', fontWeight: 'bold', fontSize: '15px' }}
              >
                {lang === 'vi' ? 'Lưu' : 'Save'}
              </button>
              <button 
                type="button"
                onClick={() => setRecurringModalOpen(false)}
                style={{ width: '100%', padding: '16px', borderRadius: '24px', background: 'rgba(255, 255, 255, 0.05)', color: 'var(--text)', border: 'none', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
              >
                {lang === 'vi' ? 'Đóng' : 'Close'}
              </button>
              {editingRecurringId != null && (
                <button 
                  type="button"
                  onClick={() => handleDeleteRecurring(editingRecurringId)}
                  style={{ width: '100%', padding: '16px', borderRadius: '24px', background: 'transparent', color: 'var(--negative)', border: '1px solid var(--negative)', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
                >
                  {lang === 'vi' ? 'Xóa định kỳ này' : 'Delete this recurring'}
                </button>
              )}
            </div>

          </div>
        </div>
      )}
      {showCsvModal && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 100 }}>
          <div className="modal-card" style={{ padding: '24px', textAlign: 'left' }}>
            <h3 style={{ fontSize: '20px', fontWeight: 'bold', marginBottom: '24px' }}>
              {lang === 'vi' ? 'Tùy chọn xuất báo cáo CSV' : 'CSV Export Options'}
            </h3>



            <div style={{ marginBottom: '20px' }}>
              <label style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px', display: 'block' }}>
                {lang === 'vi' ? 'Tài khoản' : 'Account'}
              </label>
              <div style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px' }}>
                <button 
                  type="button"
                  onClick={() => setCsvFilter(p => ({ ...p, sourceId: 'ALL' }))}
                  style={{ 
                    padding: '8px 16px', borderRadius: '20px', whiteSpace: 'nowrap', border: '1px solid',
                    background: csvFilter.sourceId === 'ALL' ? 'rgba(76, 175, 80, 0.15)' : 'transparent',
                    borderColor: csvFilter.sourceId === 'ALL' ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)',
                    color: csvFilter.sourceId === 'ALL' ? 'var(--accent)' : 'var(--text)',
                    cursor: 'pointer'
                  }}
                >
                  {lang === 'vi' ? 'Tất cả' : 'All'}
                </button>
                {sources.map(s => (
                  <button 
                    key={s.id}
                    type="button"
                    onClick={() => setCsvFilter(p => ({ ...p, sourceId: s.name }))}
                    style={{ 
                      padding: '8px 16px', borderRadius: '20px', whiteSpace: 'nowrap', border: '1px solid',
                      background: csvFilter.sourceId === s.name ? 'rgba(76, 175, 80, 0.15)' : 'transparent',
                      borderColor: csvFilter.sourceId === s.name ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)',
                      color: csvFilter.sourceId === s.name ? 'var(--accent)' : 'var(--text)',
                      cursor: 'pointer'
                    }}
                  >
                    {s.name}
                  </button>
                ))}
              </div>
            </div>

            <div style={{ marginBottom: '32px' }}>
              <label style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px', display: 'block' }}>
                {lang === 'vi' ? 'Loại giao dịch' : 'Transaction Type'}
              </label>
              <div style={{ display: 'flex', gap: '8px' }}>
                {[
                  { value: 'ALL', labelVi: 'Tất cả', labelEn: 'All' },
                  { value: 'EXPENSE', labelVi: 'Chi tiêu', labelEn: 'Expense' },
                  { value: 'INCOME', labelVi: 'Thu nhập', labelEn: 'Income' }
                ].map(type => (
                  <button 
                    key={type.value}
                    type="button"
                    onClick={() => setCsvFilter(p => ({ ...p, type: type.value as any }))}
                    style={{ 
                      padding: '8px 16px', borderRadius: '20px', border: '1px solid',
                      background: csvFilter.type === type.value ? 'rgba(76, 175, 80, 0.15)' : 'transparent',
                      borderColor: csvFilter.type === type.value ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)',
                      color: csvFilter.type === type.value ? 'var(--accent)' : 'var(--text)',
                      cursor: 'pointer'
                    }}
                  >
                    {lang === 'vi' ? type.labelVi : type.labelEn}
                  </button>
                ))}
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '16px', alignItems: 'center' }}>
              <button 
                onClick={() => setShowCsvModal(false)}
                style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
              >
                {lang === 'vi' ? 'Hủy' : 'Cancel'}
              </button>
              <button 
                onClick={handleExportCsv}
                style={{ background: 'var(--accent)', color: '#fff', border: 'none', borderRadius: '24px', padding: '12px 24px', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}
              >
                {lang === 'vi' ? 'Xuất báo cáo' : 'Export report'}
              </button>
            </div>
          </div>
        </div>
      )}



      {showDonateModal && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 100 }}>
          <div className="modal-card" style={{ padding: '24px', textAlign: 'center' }}>
            <h3 style={{ fontSize: '20px', marginBottom: '8px' }}>{lang === 'vi' ? 'Ủng hộ phát triển' : 'Support Developer'}</h3>
            <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '20px', lineHeight: '1.4' }}>
              {lang === 'vi' ? 'Đóng góp của bạn giúp KAT Budget được duy trì và cải thiện liên tục.' : 'Your contribution helps maintain and improve KAT Budget continuously.'}
            </p>
            
            <img 
              src="/donates.webp" 
              alt="Donate QR Code" 
              style={{ width: '100%', maxWidth: '240px', borderRadius: '16px', margin: '0 auto 20px', display: 'block' }} 
            />

            <div style={{ border: '1px solid var(--border)', borderRadius: '16px', padding: '16px', marginBottom: '20px' }}>
              <div style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '4px' }}>MB Bank</div>
              <div style={{ fontSize: '24px', fontWeight: 'bold', color: 'var(--accent)', marginBottom: '4px', letterSpacing: '1px' }}>0816158215</div>
              <div style={{ fontSize: '14px', fontWeight: '500' }}>TRAN THANH TUNG</div>
            </div>

            <button 
              className="primary-button" 
              style={{ width: '100%', padding: '14px', fontSize: '16px', borderRadius: '24px' }} 
              onClick={() => setShowDonateModal(false)}
            >
              {lang === 'vi' ? 'Đóng' : 'Close'}
            </button>
          </div>
        </div>
      )}

      {showAboutModal && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 100 }}>
          <div className="modal-card" style={{ padding: '24px', textAlign: 'center' }}>
            <h3 style={{ fontSize: '20px', marginBottom: '24px' }}>{lang === 'vi' ? 'Giới thiệu ứng dụng' : 'About App'}</h3>
            
            <img 
              src="/logo.webp" 
              alt="KAT Budget Logo" 
              style={{ width: '120px', height: '120px', borderRadius: '24px', margin: '0 auto 16px', display: 'block', background: '#fff', boxShadow: '0 8px 24px rgba(0,0,0,0.15)', objectFit: 'contain' }} 
            />

            <h2 style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '8px' }}>KAT Budget</h2>
            <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '24px' }}>
              {lang === 'vi' ? 'Phiên bản 1.0.0' : 'Version 1.0.0'}
            </p>

            <p style={{ fontStyle: 'italic', fontSize: '15px', marginBottom: '16px' }}>
              {lang === 'vi' ? 'Hành trình tự chủ tài chính của bạn.' : 'Your journey to financial independence.'}
            </p>

            <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '32px' }}>
              {lang === 'vi' ? 'thực hiện bởi thanhtungg.' : 'created by thanhtungg.'}
            </p>

            <button 
              className="primary-button" 
              style={{ width: '100%', padding: '14px', fontSize: '16px', borderRadius: '24px' }} 
              onClick={() => setShowAboutModal(false)}
            >
              {lang === 'vi' ? 'Đóng' : 'Close'}
            </button>
          </div>
        </div>
      )}

      {showSupportModal && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 100 }}>
          <div className="modal-card" style={{ padding: '24px' }}>
            <h3 style={{ fontSize: '20px', marginBottom: '16px' }}>{lang === 'vi' ? 'Hỗ trợ' : 'Support'}</h3>
            <p style={{ fontSize: '14px', color: 'var(--text-muted)', marginBottom: '8px', lineHeight: '1.4' }}>
              {lang === 'vi' ? 'Liên hệ chúng tôi để được hỗ trợ hoặc báo lỗi:' : 'Contact us for support or bug reports:'}
            </p>
            <p style={{ fontSize: '16px', fontWeight: 'bold', color: 'var(--accent)', marginBottom: '24px' }}>
              trevorthanhtung@gmail.com
            </p>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '16px', alignItems: 'center' }}>
              <button 
                type="button"
                style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', fontSize: '15px', fontWeight: '500', padding: '8px 16px', cursor: 'pointer' }} 
                onClick={() => setShowSupportModal(false)}
              >
                {lang === 'vi' ? 'Đóng' : 'Close'}
              </button>
              <button 
                type="button"
                className="primary-button" 
                style={{ padding: '10px 24px', fontSize: '15px', borderRadius: '16px', width: 'auto' }} 
                onClick={() => {
                  window.location.href = 'mailto:trevorthanhtung@gmail.com'
                  setShowSupportModal(false)
                }}
              >
                {lang === 'vi' ? 'Mở Mail' : 'Open Mail'}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  )
}

export default App





