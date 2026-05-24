import type * as echarts from 'echarts'
import { format } from 'date-fns'
import { Pencil, Trash2 } from 'lucide-react'
import { type ChangeEvent, type FormEvent, type MouseEvent, type PointerEvent, useEffect, useMemo, useRef, useState } from 'react'
import { AppHeader, BottomNav, PwaInstallBanner } from './components/AppChrome'
import { EChart } from './components/EChart'
import { ExchangeRateDialog, type ExchangeRateRow } from './components/ExchangeRateDialog'
import { PinLockScreen } from './components/PinLockScreen'
import { PinSetupModal } from './components/PinSetupModal'
import { SettingsTab } from './components/SettingsTab'
import { db, type Budget, type Category, type Debt, type MoneySource, type RecurringTransaction, type SavingGoal, type Transaction } from './data/db'
import { useCalculations } from './hooks/useCalculations'
import { useDatabase } from './hooks/useDatabase'
import type {
  ActiveTool,
  AppLanguage,
  AppTab,
  BudgetForm,
  CategoryForm,
  CategoryTypeFilter,
  DebtForm,
  GoalForm,
  RecurringForm,
  SourceForm,
  TxForm,
} from './types'
import {
  evaluateAmountExpression,
  isOpeningBalanceTransaction,
  isTransferTag,
  makeTransferTag,
  transactionAccent,
  transactionPrefix,
  transactionToneClass,
  transferCategory,
  transferInNote,
  transferOutNote,
} from './utils/transactions'
import {
  defaultBudgetForm,
  defaultCategoryForm,
  defaultCategoryPresets,
  defaultDebtForm,
  defaultSourceForm,
  defaultTxForm,
  SUPPORTED_CURRENCIES,
} from './utils/constants'
import { createAndroidBackupBlob, importBackupFile } from './utils/backup'
import { budgetMajorToMinor, budgetMinorToMajor, convertCurrency, formatCurrency, normalizeCurrency, parseAmount } from './utils/helpers'
import './App.css'

type ChartLabelParams = {
  value?: unknown
  dataIndex?: number
}

type CsvFilterType = 'ALL' | 'EXPENSE' | 'INCOME'

type CsvFilter = {
  sourceId: string
  type: CsvFilterType
}

let hasQueuedDefaultSeed = false
const FALLBACK_SOURCE_NAME = 'Tien mat'
const DEBT_ROLLBACK_PREFIX = 'DEBT_ROLLBACK_'
const GOAL_DEPOSIT_TYPE = 'GOAL_DEPOSIT'
const RECURRING_TAG_PREFIX = 'REC_'

function chartValue(params: ChartLabelParams): number {
  return typeof params.value === 'number' ? params.value : Number(params.value) || 0
}
function App() {
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const queuedRecurringTagsRef = useRef<Set<string>>(new Set())
  const sourceSwipeStartXRef = useRef(0)
  const sourceSwipeLastXRef = useRef(0)
  const { sources, transactions, categories, budgets, debts, savingGoals, recurrings } = useDatabase()

  const [activeTab, setActiveTab] = useState<AppTab>('DASHBOARD')
  const [showMoreTx, setShowMoreTx] = useState(false)
  const [debtTab, setDebtTab] = useState<'CHO_VAY' | 'DI_VAY'>('CHO_VAY')
  const [activeTool, setActiveTool] = useState<ActiveTool>('NONE')
  const [theme, setTheme] = useState<'system' | 'light' | 'dark'>('system')
  const [lang, setLang] = useState<AppLanguage>('vi')
  const [currency, setCurrency] = useState('VND')
  const [exchangeRate] = useState(() => Number(localStorage.getItem('kat_exchange_rate')) || 25000)
  
  const [pin, setPin] = useState(() => localStorage.getItem('kat_pin') || '')
  const [isUnlocked, setIsUnlocked] = useState(() => !localStorage.getItem('kat_pin'))
  const [pinInput, setPinInput] = useState('')
  const [newPinInput, setNewPinInput] = useState('')
  const [showPinSetupModal, setShowPinSetupModal] = useState(false)
  const [showPwaBanner, setShowPwaBanner] = useState(() => !window.matchMedia('(display-mode: standalone)').matches && !sessionStorage.getItem('kat_pwa_dismissed'))
  const [exchangeRatesData, setExchangeRatesData] = useState<ExchangeRateRow[]>([])
  const [loadingRates, setLoadingRates] = useState(false)
  const [showDonateModal, setShowDonateModal] = useState(false)
  const [showAboutModal, setShowAboutModal] = useState(false)
  const [showSupportModal, setShowSupportModal] = useState(false)
  const [showCsvModal, setShowCsvModal] = useState(false)
  const [csvFilter, setCsvFilter] = useState<CsvFilter>({
    sourceId: 'ALL',
    type: 'ALL'
  })


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
  const [payingDebtId, setPayingDebtId] = useState<number | null>(null)
  const [debtPayForm, setDebtPayForm] = useState({ amount: '', sourceName: '' })
  const [depositingGoalId, setDepositingGoalId] = useState<number | null>(null)
  const [goalDepositForm, setGoalDepositForm] = useState({ amount: '', sourceName: '', currency: 'VND' })
  const [categoryTypeFilter, setCategoryTypeFilter] = useState<CategoryTypeFilter>('ALL')
  const [budgetMonthFilter] = useState(format(new Date(), 'yyyy-MM'))
  const [selectedSourceFilter, setSelectedSourceFilter] = useState('ALL')
  const [openSourceActionId, setOpenSourceActionId] = useState<number | null>(null)
  const [transactionSearch, setTransactionSearch] = useState('')
  const [reportMonthFilter, setReportMonthFilter] = useState(format(new Date(), 'yyyy-MM'))
  const [reportTypeFilter, setReportTypeFilter] = useState<'ALL' | 'EXPENSE' | 'INCOME'>('ALL')
  const [reportCurrencyFilter, setReportCurrencyFilter] = useState<string>('ALL')
  const [statusMessage, setStatusMessage] = useState('')
  const [currentTimestamp] = useState(() => new Date().getTime())

  useEffect(() => {
    if (hasQueuedDefaultSeed) return
    hasQueuedDefaultSeed = true

    const initializeData = async () => {
      const [sourceCount, categoryCount] = await Promise.all([
        db.sources.count(),
        db.categories.count(),
      ])

      if (sourceCount === 0) {
        await db.sources.add({
          name: FALLBACK_SOURCE_NAME,
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

  useEffect(() => {
    if (recurrings.length === 0) return

    const runDueRecurrings = async () => {
      const now = new Date()
      const currentMonthYear = format(now, 'yyyy-MM')
      const currentDay = now.getDate()
      const lastDayOfCurrentMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
      const monthStart = new Date(now.getFullYear(), now.getMonth(), 1).getTime()
      const monthEnd = new Date(now.getFullYear(), now.getMonth() + 1, 1).getTime() - 1

      for (const recurring of recurrings) {
        const effectiveRunDay = Math.min(Math.max(recurring.dayOfMonth, 1), lastDayOfCurrentMonth)
        const recurringTag = `${RECURRING_TAG_PREFIX}${recurring.id}_${currentMonthYear}`
        if (queuedRecurringTagsRef.current.has(recurringTag)) continue

        const alreadyCreatedThisMonth = await db.transactions
          .where('projectTag')
          .equals(recurringTag)
          .filter((transaction) => transaction.timestamp >= monthStart && transaction.timestamp <= monthEnd)
          .count()

        if (alreadyCreatedThisMonth > 0) {
          if (recurring.lastExecutedMonth !== currentMonthYear) {
            queuedRecurringTagsRef.current.add(recurringTag)
            await db.recurrings.update(recurring.id, { lastExecutedMonth: currentMonthYear })
          }
          continue
        }

        if (currentDay < effectiveRunDay || recurring.lastExecutedMonth === currentMonthYear) continue

        queuedRecurringTagsRef.current.add(recurringTag)
        await db.transactions.add({
          amount: recurring.amount,
          type: recurring.type,
          category: recurring.category,
          note: `${recurring.note.trim()} ${lang === 'vi' ? '(Tự động ghi nhận)' : '(Auto recorded)'}`.trim(),
          timestamp: new Date().getTime(),
          projectTag: recurringTag,
          sourceName: recurring.sourceName,
          currency: normalizeCurrency(recurring.currency),
        })
        await db.recurrings.update(recurring.id, { lastExecutedMonth: currentMonthYear })
      }
    }

    void runDueRecurrings()
  }, [lang, recurrings])

  const {
    balancesBySource,
    sourceBalancesByCurrency,
    netWorth,
    netWorthByCurrency,
    filteredCategories,
    expenseCategories,
    budgetRows,
    totalBudgetLimit,
    totalBudgetSpent,
    totalBudgetPercent,
    overBudgetRows,
    overBudgetTotal,
    reportBudgetRows,
    reportBudgetLimit,
    reportBudgetSpent,
    reportBudgetPercent,
    reportDisplayCurrency,
    reportIncomeTotal,
    reportExpenseTotal,
    prevPeriodExpenseTotal,
    reportBalancesByCurrency,
    reportExpenseByCategory,
    reportDailyExpenses,
  } = useCalculations({
    sources,
    transactions,
    categories,
    budgets,
    exchangeRate,
    displayCurrency: currency,
    budgetMonthFilter,
    reportMonthFilter,
    reportTypeFilter,
    reportCurrencyFilter,
    categoryTypeFilter,
  })

  const txCategoryPresets = categories.filter((category) => category.type === txForm.type)
  const selectedSourceName = selectedSourceFilter === 'ALL' ? '' : selectedSourceFilter
  const selectedSource = selectedSourceName ? sources.find((source) => source.name === selectedSourceName) : undefined
  const debtTypeFilter = debtTab === 'CHO_VAY' ? 'LOAN' : 'DEBT'
  const filteredDebtRows = useMemo(
    () => debts.filter((debt) => debt.type === debtTypeFilter),
    [debts, debtTypeFilter],
  )
  const activeDebtRows = useMemo(
    () => filteredDebtRows.filter((debt) => !debt.isPaid),
    [filteredDebtRows],
  )
  const paidDebtRows = useMemo(
    () => filteredDebtRows.filter((debt) => debt.isPaid),
    [filteredDebtRows],
  )
  const payingDebt = payingDebtId == null
    ? undefined
    : debts.find((debt) => debt.id === payingDebtId)
  const payingDebtRemaining = payingDebt
    ? Math.max(0, payingDebt.amount - (payingDebt.paidAmount ?? 0))
    : 0
  const goalRows = useMemo(() => (
    savingGoals.map((goal) => {
      const currentAmount = Number(goal.currentAmount) || 0
      const targetAmount = Number(goal.targetAmount) || 0
      const percent = targetAmount > 0 ? Math.min(100, Math.round((currentAmount / targetAmount) * 100)) : 0
      return { goal, currentAmount, targetAmount, percent }
    })
  ), [savingGoals])
  const goalTotalSaved = useMemo(() => (
    goalRows.reduce((sum, row) => sum + convertCurrency(row.currentAmount, row.goal.currency, currency, exchangeRate), 0)
  ), [currency, exchangeRate, goalRows])
  const goalTotalTarget = useMemo(() => (
    goalRows.reduce((sum, row) => sum + convertCurrency(row.targetAmount, row.goal.currency, currency, exchangeRate), 0)
  ), [currency, exchangeRate, goalRows])
  const goalTotalPercent = goalTotalTarget > 0
    ? Math.min(100, Math.round((goalTotalSaved / goalTotalTarget) * 100))
    : 0
  const depositingGoal = depositingGoalId == null
    ? undefined
    : savingGoals.find((goal) => goal.id === depositingGoalId)
  const depositSourceBalances = depositingGoal
    ? sourceBalancesByCurrency[goalDepositForm.sourceName] ?? {}
    : {}
  const depositSourceBalanceText = Object.keys(depositSourceBalances).length === 0
    ? '0 VND'
    : Object.entries(depositSourceBalances)
      .sort(([leftCurrency], [rightCurrency]) => leftCurrency.localeCompare(rightCurrency))
      .map(([balanceCurrency, balanceAmount]) => formatCurrency(balanceAmount, balanceCurrency))
      .join(', ')
  const recurringRows = useMemo(() => (
    [...recurrings].sort((left, right) => (
      left.dayOfMonth - right.dayOfMonth || left.category.localeCompare(right.category)
    ))
  ), [recurrings])
  const recurringTotalIncome = useMemo(() => (
    recurringRows
      .filter((recurring) => recurring.type === 'INCOME')
      .reduce((sum, recurring) => sum + convertCurrency(recurring.amount, recurring.currency, currency, exchangeRate), 0)
  ), [currency, exchangeRate, recurringRows])
  const recurringTotalExpense = useMemo(() => (
    recurringRows
      .filter((recurring) => recurring.type === 'EXPENSE')
      .reduce((sum, recurring) => sum + convertCurrency(recurring.amount, recurring.currency, currency, exchangeRate), 0)
  ), [currency, exchangeRate, recurringRows])
  const selectedSourceBalance = selectedSourceName ? balancesBySource[selectedSourceName] ?? 0 : netWorth
  const selectedSourceCurrencies = selectedSourceName
    ? sourceBalancesByCurrency[selectedSourceName] ?? {}
    : netWorthByCurrency
  const displayTransactions = transactions.filter((transaction) => !isOpeningBalanceTransaction(transaction))
  const filteredTransactions = displayTransactions
    .filter((transaction) => selectedSourceFilter === 'ALL' || transaction.sourceName === selectedSourceFilter)
    .filter((transaction) => {
      const query = transactionSearch.trim().toLowerCase()
      if (!query) return true
      return (
        transaction.category.toLowerCase().includes(query) ||
        transaction.note.toLowerCase().includes(query) ||
        transaction.sourceName.toLowerCase().includes(query) ||
        (transaction.destinationName ?? '').toLowerCase().includes(query) ||
        transaction.amount.toString().includes(query)
      )
    })
  const categoryColors = ['#F44336', '#FF9800', '#9C27B0', '#FF5722', '#E91E63', '#2196F3', '#4CAF50', '#009688']
  const reportCategoryRows = Object.entries(reportExpenseByCategory)
    .sort((a, b) => b[1] - a[1])
    .map(([categoryName, amount], index) => {
      const preset = categories.find(c => c.name === categoryName)
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

  const compCurrency = reportDisplayCurrency
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
        formatter: (params: ChartLabelParams) => formatCurrency(chartValue(params), compCurrency),
        rich: {
          prev: { color: 'var(--text-muted)', fontSize: 12, fontWeight: 'bold' },
          curr: { color: '#F44336', fontSize: 12, fontWeight: 'bold' }
        },
        color: (params: ChartLabelParams) => params.dataIndex === 1 ? '#F44336' : 'var(--text-muted)',
        fontSize: 12,
        fontWeight: 'bold'
      }
    }]
  }


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

  const getLinkedTransferTransactions = (transaction: Transaction): Transaction[] => {
    const ids = new Set<number>([transaction.id])
    const tag = transaction.projectTag?.trim()

    if (isTransferTag(tag)) {
      transactions
        .filter((candidate) => candidate.projectTag === tag)
        .forEach((candidate) => ids.add(candidate.id))
    } else if (transaction.type === 'TRANSFER_OUT') {
      transactions
        .filter((candidate) => (
          candidate.type === 'TRANSFER_IN' &&
          (candidate.projectTag === transaction.id.toString() ||
            (candidate.timestamp >= transaction.timestamp && candidate.timestamp <= transaction.timestamp + 1 &&
              candidate.sourceName === transaction.destinationName))
        ))
        .forEach((candidate) => ids.add(candidate.id))
    } else if (transaction.type === 'TRANSFER_IN' && tag) {
      const legacyOutId = Number(tag)
      if (Number.isInteger(legacyOutId)) {
        const outgoing = transactions.find((candidate) => candidate.id === legacyOutId)
        if (outgoing) ids.add(outgoing.id)
      }
    }

    return transactions.filter((candidate) => ids.has(candidate.id))
  }

  const getEditableTransaction = (transaction: Transaction): Transaction => {
    if (transaction.type !== 'TRANSFER_IN') return transaction
    return getLinkedTransferTransactions(transaction).find((candidate) => candidate.type === 'TRANSFER_OUT') ?? transaction
  }

  const getTransferMemo = (transaction: Transaction): string => {
    if (transaction.type !== 'TRANSFER_OUT' && transaction.type !== 'TRANSFER_IN') return transaction.note
    const memo = transaction.note.match(/\((.*)\)$/)?.[1]
    return memo ?? ''
  }

  const openTxModal = (tx?: Transaction) => {
    if (!tx && sources.length === 0) {
      openSourceModal()
      return
    }

    if (tx) {
      const editableTransaction = getEditableTransaction(tx)
      const linkedTransferIn = getLinkedTransferTransactions(editableTransaction)
        .find((candidate) => candidate.type === 'TRANSFER_IN')

      setEditingTxId(editableTransaction.id)
      setTxForm({
        sourceName: editableTransaction.sourceName,
        type: editableTransaction.type === 'INCOME' ? 'INCOME' : editableTransaction.type === 'TRANSFER_OUT' ? 'TRANSFER_OUT' : 'EXPENSE',
        amount: editableTransaction.amount.toString(),
        category: editableTransaction.type === 'TRANSFER_OUT' ? transferCategory(lang) : editableTransaction.category,
        note: getTransferMemo(editableTransaction),
        currency: editableTransaction.currency,
        timestamp: new Date(editableTransaction.timestamp).toISOString().split('T')[0],
        imageUri: editableTransaction.imageUri || '',
        destinationName: editableTransaction.destinationName || linkedTransferIn?.sourceName || '',
      })
      setCalcInput(editableTransaction.amount.toString())
    } else {
      const defaultSourceName = selectedSourceName && sources.some((source) => source.name === selectedSourceName)
        ? selectedSourceName
        : sources[0]?.name ?? ''

      setEditingTxId(null)
      setTxForm({
        ...defaultTxForm,
        sourceName: defaultSourceName,
        destinationName: sources.find((source) => source.name !== defaultSourceName)?.name ?? '',
      })
      setCalcInput('0')
    }
    setTxModalOpen(true)
  }

  const handleDeleteTransaction = async (transaction: Transaction) => {
    const linkedTransactions = getLinkedTransferTransactions(transaction)
    await db.transactions.bulkDelete(linkedTransactions.map((item) => item.id))
    const linkedDebtId = transaction.projectTag?.startsWith(DEBT_ROLLBACK_PREFIX)
      ? Number(transaction.projectTag.replace(DEBT_ROLLBACK_PREFIX, ''))
      : 0
    const linkedDebt = Number.isInteger(linkedDebtId) && linkedDebtId > 0
      ? debts.find((debt) => debt.id === linkedDebtId)
      : undefined
    if (linkedDebt) {
      const paidAmount = Math.max(0, (linkedDebt.paidAmount ?? 0) - transaction.amount)
      await db.debts.put({
        ...linkedDebt,
        paidAmount,
        isPaid: paidAmount >= linkedDebt.amount,
      })
    }
    if (transaction.type === GOAL_DEPOSIT_TYPE) {
      const linkedGoalName = transaction.projectTag?.trim() || transaction.note.replace(/^.*?:\s*/, '').trim()
      const linkedGoal = savingGoals.find((goal) => goal.name === linkedGoalName)
      if (linkedGoal) {
        const rollbackAmount = convertCurrency(transaction.amount, transaction.currency, linkedGoal.currency, exchangeRate)
        await db.saving_goals.put({
          ...linkedGoal,
          currentAmount: Math.max(0, linkedGoal.currentAmount - rollbackAmount),
        })
      }
    }
    if (editingTxId && linkedTransactions.some((item) => item.id === editingTxId)) {
      setTxModalOpen(false)
      setEditingTxId(null)
    }
    setStatusMessage(transaction.type === 'TRANSFER_IN' || transaction.type === 'TRANSFER_OUT'
      ? 'Da xoa cap chuyen tien.'
      : 'Da xoa giao dich.')
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
        limitAmount: budgetMinorToMajor(budget.limitAmountMinor, budget.currencyCode).toString(),
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
      setDebtForm({
        ...defaultDebtForm,
        type: debtTypeFilter,
      })
    }
    setDebtModalOpen(true)
  }

  const openDebtPaymentModal = (debt: Debt) => {
    const remainingAmount = Math.max(0, debt.amount - (debt.paidAmount ?? 0))
    setPayingDebtId(debt.id)
    setDebtPayForm({
      amount: remainingAmount > 0 ? remainingAmount.toString() : '',
      sourceName: sources[0]?.name ?? FALLBACK_SOURCE_NAME,
    })
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

  const openGoalDepositModal = (goal: SavingGoal) => {
    setDepositingGoalId(goal.id)
    setGoalDepositForm({
      amount: '',
      sourceName: sources[0]?.name ?? FALLBACK_SOURCE_NAME,
      currency: normalizeCurrency(goal.currency),
    })
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

  const handleDeleteSource = async (source: MoneySource) => {
    if (source.id == null) return
    const ok = window.confirm(lang === 'vi'
      ? `Xóa tài khoản "${source.name}"? Giao dịch cũ vẫn được giữ lại.`
      : `Delete account "${source.name}"? Existing transactions will be kept.`)
    if (!ok) return

    await db.sources.delete(source.id)
    if (selectedSourceFilter === source.name) {
      setSelectedSourceFilter('ALL')
    }
    if (editingSourceId === source.id) {
      setSourceModalOpen(false)
      setEditingSourceId(null)
    }
    setOpenSourceActionId(null)
    setStatusMessage(lang === 'vi' ? 'Đã xóa tài khoản.' : 'Account deleted.')
  }

  const handleSourceSwipeStart = (event: PointerEvent<HTMLDivElement>) => {
    sourceSwipeStartXRef.current = event.clientX
    sourceSwipeLastXRef.current = event.clientX
  }

  const handleSourceSwipeMove = (event: PointerEvent<HTMLDivElement>) => {
    sourceSwipeLastXRef.current = event.clientX
  }

  const handleSourceSwipeEnd = (sourceId: number | undefined) => {
    if (sourceId == null) return
    const deltaX = sourceSwipeLastXRef.current - sourceSwipeStartXRef.current
    if (deltaX < -32) {
      setOpenSourceActionId(sourceId)
    } else if (deltaX > 24) {
      setOpenSourceActionId(null)
    }
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
        limitAmountMinor: budgetMajorToMinor(limitAmount, currencyCode),
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
        limitAmountMinor: budgetMajorToMinor(limitAmount, currencyCode),
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
    
    const expressionAmount = evaluateAmountExpression(calcInput)
    const finalAmount = Number.isFinite(expressionAmount)
      ? expressionAmount
      : parseAmount(txForm.amount || calcInput)
    
    if (finalAmount <= 0 || !txForm.sourceName) return

    if (txForm.type === 'TRANSFER_OUT' && (!txForm.destinationName || txForm.destinationName === txForm.sourceName)) {
      setStatusMessage('Chon tai khoan nhan khac tai khoan nguon.')
      return
    }

    const category = txForm.type === 'TRANSFER_OUT'
      ? transferCategory(lang)
      : txForm.category.trim() || (txForm.type === 'INCOME' ? 'Thu nhap' : 'Chi tieu')
    const normalizedCurrency = txForm.currency.trim().toUpperCase() || 'VND'
    const ts = new Date(txForm.timestamp).getTime()

    const saveTransferPair = async () => {
      const transferTag = makeTransferTag(ts)
      const destinationName = txForm.destinationName?.trim() ?? ''
      await db.transactions.bulkAdd([
        {
          amount: finalAmount,
          type: 'TRANSFER_OUT',
          category,
          note: transferOutNote(destinationName, txForm.note, lang),
          timestamp: ts,
          sourceName: txForm.sourceName,
          currency: normalizedCurrency,
          imageUri: txForm.imageUri,
          destinationName,
          projectTag: transferTag,
        },
        {
          amount: finalAmount,
          type: 'TRANSFER_IN',
          category,
          note: transferInNote(txForm.sourceName, txForm.note, lang),
          timestamp: ts + 1,
          sourceName: destinationName,
          currency: normalizedCurrency,
          projectTag: transferTag,
          destinationName: txForm.sourceName,
        },
      ])
    }

    if (editingTxId == null) {
      if (txForm.type === 'TRANSFER_OUT') {
        await saveTransferPair()
      } else {
        await db.transactions.add({
          amount: finalAmount,
          type: txForm.type,
          category,
          note: txForm.note.trim(),
          timestamp: ts,
          sourceName: txForm.sourceName,
          currency: normalizedCurrency,
          imageUri: txForm.imageUri,
        })
      }
      
      setStatusMessage('Da them giao dich.')
    } else {
      const existing = transactions.find((tx) => tx.id === editingTxId)
      if (!existing) return

      if (txForm.type === 'TRANSFER_OUT') {
        await db.transactions.bulkDelete(getLinkedTransferTransactions(existing).map((item) => item.id))
        await saveTransferPair()
      } else if (existing.type === 'TRANSFER_OUT' || existing.type === 'TRANSFER_IN') {
        await db.transactions.bulkDelete(getLinkedTransferTransactions(existing).map((item) => item.id))
        await db.transactions.add({
          amount: finalAmount,
          type: txForm.type,
          category,
          note: txForm.note.trim(),
          timestamp: ts,
          sourceName: txForm.sourceName,
          currency: normalizedCurrency,
          imageUri: txForm.imageUri,
        })
      } else {
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
          destinationName: undefined,
        })
      }
      
      setStatusMessage('Da cap nhat giao dich.')
    }

    setTxModalOpen(false)
    setEditingTxId(null)
  }

  const handleSaveDebt = async (event: FormEvent) => {
    event.preventDefault()
    const personName = debtForm.personName.trim()
    if (!personName || !debtForm.amount) return

    const parsedAmount = parseAmount(debtForm.amount)
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) return
    const normalizedCurrency = normalizeCurrency(debtForm.currency)

    let parsedDueDate: number | null = null
    if (debtForm.dueDate) {
      const parts = debtForm.dueDate.split('-')
      if (parts.length === 3) {
        parsedDueDate = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2])).getTime()
      }
    }

    if (editingDebtId != null) {
      const oldDebt = debts.find((debt) => debt.id === editingDebtId)
      const paidAmount = Math.min(oldDebt?.paidAmount ?? 0, parsedAmount)
      await db.debts.update(editingDebtId, {
        personName,
        amount: parsedAmount,
        currency: normalizedCurrency,
        type: debtForm.type,
        note: debtForm.note.trim(),
        isPaid: paidAmount >= parsedAmount,
        paidAmount,
        dueDate: parsedDueDate
      })
      setStatusMessage('Da cap nhat khoan vay/no.')
    } else {
      await db.debts.add({
        personName,
        amount: parsedAmount,
        currency: normalizedCurrency,
        type: debtForm.type,
        note: debtForm.note.trim(),
        timestamp: Date.now(),
        isPaid: false,
        paidAmount: 0,
        dueDate: parsedDueDate,
      })
      setStatusMessage('Da tao khoan vay/no.')
    }

    setDebtModalOpen(false)
    setEditingDebtId(null)
  }

  const handleSaveGoal = async (e: FormEvent | MouseEvent) => {
    if (e) e.preventDefault()
    const name = goalForm.name.trim()
    const targetAmount = parseAmount(goalForm.targetAmount)
    const normalizedCurrency = normalizeCurrency(goalForm.currency)

    if (!name || targetAmount <= 0) {
      setStatusMessage('Vui long nhap du thong tin muc tieu.')
      return
    }

    if (editingGoalId != null) {
      const oldGoal = savingGoals.find((goal) => goal.id === editingGoalId)
      const currentAmount = oldGoal
        ? convertCurrency(oldGoal.currentAmount, oldGoal.currency, normalizedCurrency, exchangeRate)
        : parseAmount(goalForm.currentAmount)
      await db.saving_goals.update(editingGoalId, {
        name,
        targetAmount,
        currentAmount,
        currency: normalizedCurrency,
      })
      if (oldGoal && oldGoal.name !== name) {
        await db.transactions.where('projectTag').equals(oldGoal.name).modify({
          projectTag: name,
        })
      }
      setStatusMessage('Da cap nhat muc tieu.')
    } else {
      await db.saving_goals.add({
        name,
        targetAmount,
        currentAmount: 0,
        currency: normalizedCurrency,
      })
      setStatusMessage('Da tao muc tieu.')
    }
    setGoalModalOpen(false)
    setEditingGoalId(null)
  }

  const handleDeleteGoal = async (id: number) => {
    await db.saving_goals.delete(id)
    setGoalModalOpen(false)
    setEditingGoalId(null)
    if (depositingGoalId === id) setDepositingGoalId(null)
    setStatusMessage('Da xoa muc tieu.')
  }

  const handleDepositGoal = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!depositingGoal) return

    const amount = parseAmount(goalDepositForm.amount)
    if (!Number.isFinite(amount) || amount <= 0) return

    const depositCurrency = normalizeCurrency(goalDepositForm.currency)
    const contributionAmount = convertCurrency(amount, depositCurrency, depositingGoal.currency, exchangeRate)
    const sourceName = goalDepositForm.sourceName.trim() || sources[0]?.name || FALLBACK_SOURCE_NAME

    await db.saving_goals.put({
      ...depositingGoal,
      currentAmount: depositingGoal.currentAmount + contributionAmount,
    })
    await db.transactions.add({
      amount,
      type: GOAL_DEPOSIT_TYPE,
      category: 'Tiết kiệm',
      note: `Tích lũy cho mục tiêu: ${depositingGoal.name}`,
      timestamp: new Date().getTime(),
      sourceName,
      currency: depositCurrency,
      projectTag: depositingGoal.name,
    })

    setDepositingGoalId(null)
    setGoalDepositForm({ amount: '', sourceName: '', currency: 'VND' })
    setStatusMessage('Da ghi nhan tich luy muc tieu.')
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
      const defaultType: RecurringTransaction['type'] = 'EXPENSE'
      const defaultCategory = categories.find((category) => category.type === defaultType)?.name ?? (lang === 'vi' ? 'Khác' : 'Other')
      setEditingRecurringId(null)
      setRecurringForm({
        amount: '',
        currency,
        type: defaultType,
        category: defaultCategory,
        note: '',
        sourceName: sources[0]?.name ?? FALLBACK_SOURCE_NAME,
        dayOfMonth: '1',
      })
    }
    setRecurringModalOpen(true)
  }

  const handleSaveRecurring = async (e: FormEvent | MouseEvent) => {
    if (e) e.preventDefault()
    const amount = parseAmount(recurringForm.amount)
    const dayOfMonth = Number.parseInt(recurringForm.dayOfMonth, 10)
    const category = recurringForm.category.trim() || (recurringForm.type === 'INCOME' ? (lang === 'vi' ? 'Thu khác' : 'Other income') : (lang === 'vi' ? 'Khác' : 'Other'))
    const sourceName = recurringForm.sourceName.trim() || sources[0]?.name || FALLBACK_SOURCE_NAME
    const normalizedCurrency = normalizeCurrency(recurringForm.currency)

    if (amount <= 0 || dayOfMonth < 1 || dayOfMonth > 31 || !sourceName) {
      setStatusMessage('Vui long kiem tra lai thong tin dinh ky.')
      return
    }

    if (editingRecurringId != null) {
      await db.recurrings.update(editingRecurringId, {
        amount,
        currency: normalizedCurrency,
        type: recurringForm.type,
        category,
        note: recurringForm.note.trim(),
        sourceName,
        dayOfMonth,
      })
      setStatusMessage('Da cap nhat giao dich dinh ky.')
    } else {
      await db.recurrings.add({
        amount,
        currency: normalizedCurrency,
        type: recurringForm.type,
        category,
        note: recurringForm.note.trim(),
        sourceName,
        dayOfMonth,
        lastExecutedMonth: '',
      })
      setStatusMessage('Da tao giao dich dinh ky.')
    }
    setRecurringModalOpen(false)
    setEditingRecurringId(null)
  }

  const handleDeleteRecurring = async (id: number) => {
    await db.recurrings.delete(id)
    setRecurringModalOpen(false)
    setEditingRecurringId(null)
    setStatusMessage('Da xoa giao dich dinh ky.')
  }

  const handleDeleteDebt = async (id: number) => {
    await db.debts.delete(id)
    await db.transactions.where('projectTag').equals(`${DEBT_ROLLBACK_PREFIX}${id}`).delete()
    if (payingDebtId === id) setPayingDebtId(null)
    if (editingDebtId === id) {
      setDebtModalOpen(false)
      setEditingDebtId(null)
    }
    setStatusMessage('Da xoa ghi chu vay/no.')
  }

  const handlePayDebt = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!payingDebt) return

    const inputAmount = parseAmount(debtPayForm.amount)
    const remainingAmount = Math.max(0, payingDebt.amount - (payingDebt.paidAmount ?? 0))
    if (!Number.isFinite(inputAmount) || inputAmount <= 0 || remainingAmount <= 0) return

    const paidDelta = Math.min(inputAmount, remainingAmount)
    const totalPaid = Math.min(payingDebt.amount, (payingDebt.paidAmount ?? 0) + paidDelta)
    const sourceName = debtPayForm.sourceName.trim() || sources[0]?.name || FALLBACK_SOURCE_NAME
    const note = payingDebt.type === 'DEBT'
      ? `Thanh toán một phần khoản vay cho ${payingDebt.personName}`
      : `Thu một phần khoản cho vay từ ${payingDebt.personName}`

    await db.debts.put({
      ...payingDebt,
      paidAmount: totalPaid,
      isPaid: totalPaid >= payingDebt.amount,
    })
    await db.transactions.add({
      amount: paidDelta,
      type: payingDebt.type === 'DEBT' ? 'EXPENSE' : 'INCOME',
      category: 'Vay nợ',
      note,
      timestamp: new Date().getTime(),
      sourceName,
      currency: normalizeCurrency(payingDebt.currency),
      projectTag: `${DEBT_ROLLBACK_PREFIX}${payingDebt.id}`,
    })

    setPayingDebtId(null)
    setDebtPayForm({ amount: '', sourceName: '' })
    setStatusMessage(payingDebt.type === 'DEBT' ? 'Da ghi nhan thanh toan khoan vay.' : 'Da ghi nhan thu khoan cho vay.')
  }

  const handleExportBackup = async () => {
    const blob = await createAndroidBackupBlob()
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'KATBudget_BackUp.kat'
    link.click()
    URL.revokeObjectURL(url)
    setStatusMessage('Đã xuất backup .kat tương thích Android.')
  }

  const handlePickImportFile = () => {
    fileInputRef.current?.click()
  }

  const handleImportBackup = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    try {
      await importBackupFile(file)
      setStatusMessage('Đã phục hồi backup .kat/.json.')
    } catch {
      setStatusMessage('File backup không hợp lệ hoặc sai định dạng .kat/.json.')
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
    if (activeTool !== 'EXCHANGE_RATE' || exchangeRatesData.length > 0) return

    const loadExchangeRates = async () => {
      setLoadingRates(true)
      try {
        const response = await fetch('/api/ty-gia')
        if (!response.ok) throw new Error(`Exchange rate request failed: ${response.status}`)
        const text = await response.text()
        const parser = new DOMParser()
        const xmlDoc = parser.parseFromString(text, "text/xml")
        const exrates = Array.from(xmlDoc.getElementsByTagName('Exrate'))
        const rates: ExchangeRateRow[] = exrates.map(node => ({
          CurrencyCode: node.getAttribute('CurrencyCode') ?? '',
          CurrencyName: node.getAttribute('CurrencyName') ?? '',
          Buy: node.getAttribute('Buy') ?? '',
          Transfer: node.getAttribute('Transfer') ?? '',
          Sell: node.getAttribute('Sell') ?? ''
        }))
        setExchangeRatesData(rates)
      } catch (error) {
        console.error('Fetch Exchange Rates Error:', error)
      } finally {
        setLoadingRates(false)
      }
    }

    void loadExchangeRates()
  }, [activeTool, exchangeRatesData.length])

  if (!isUnlocked) {
    return (
      <PinLockScreen
        lang={lang}
        pin={pin}
        pinInput={pinInput}
        setPinInput={setPinInput}
        onUnlock={() => setIsUnlocked(true)}
      />
    )
  }

  return (
    <main className="app-shell">
      <AppHeader
        activeTab={activeTab}
        activeTool={activeTool}
        lang={lang}
        onBackTool={() => setActiveTool('NONE')}
        onToggleLang={() => setLang(current => current === 'vi' ? 'en' : 'vi')}
      />

      {statusMessage && <p className="status-banner">{statusMessage}</p>}

      {activeTool === 'BUDGET' && (
        <section className="tool-screen budget-tool-screen" style={{ position: 'fixed', inset: 0, background: 'var(--bg)', zIndex: 100, overflowY: 'auto', padding: '16px', paddingBottom: '40px' }}>
          <div className="tool-screen-header" style={{ display: 'flex', alignItems: 'center', marginBottom: '24px' }}>
            <button onClick={() => setActiveTool('NONE')} style={{ background: 'var(--surface)', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', width: '40px', height: '40px', borderRadius: '12px', cursor: 'pointer' }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
            </button>
            <h2 style={{ marginLeft: '16px', fontSize: '20px', fontWeight: 'bold', margin: 0, flex: 1 }}>
              {lang === 'vi' ? 'Ngân sách hằng tháng' : 'Monthly Budget'}
            </h2>
          </div>

          <div className="tool-summary-card" style={{ background: 'var(--surface)', borderRadius: '24px', padding: '20px', marginBottom: '16px' }}>
            <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px' }}>
              {lang === 'vi' ? 'Tổng ngân sách' : 'Total budget'}
            </p>
            <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: '16px' }}>
              <h3 className="tool-summary-amount" style={{ fontSize: '28px', fontWeight: 'bold', margin: 0 }}>
                {formatCurrency(totalBudgetLimit, currency)}
              </h3>
              <span style={{ fontSize: '16px', color: 'var(--accent)', fontWeight: 'bold' }}>
                {totalBudgetPercent}%
              </span>
            </div>
            
            <div style={{ height: '8px', background: 'rgba(255, 255, 255, 0.1)', borderRadius: '4px', overflow: 'hidden', marginBottom: '16px' }}>
              <div style={{ height: '100%', background: 'var(--accent)', width: `${Math.min(totalBudgetPercent, 100)}%`, borderRadius: '4px' }} />
            </div>

            <p style={{ fontSize: '13px', color: 'var(--text-muted)', margin: 0 }}>
              {lang === 'vi' ? 'Đã chi' : 'Spent'}: {formatCurrency(totalBudgetSpent, currency)}
            </p>
          </div>

          {overBudgetRows.length > 0 && (
            <div style={{ background: 'rgba(244, 67, 54, 0.15)', color: 'var(--negative)', padding: '16px', borderRadius: '16px', marginBottom: '24px', fontSize: '14px' }}>
              {lang === 'vi' ? `Có ${overBudgetRows.length} danh mục vượt ngân sách, tổng vượt ` : `There are ${overBudgetRows.length} categories over budget, total over `}
              <strong>{formatCurrency(overBudgetTotal, currency)}</strong>.
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
                <div key={row.budget.id} className="tool-list-card" style={{ background: 'var(--surface)', borderRadius: '20px', padding: '16px', cursor: 'pointer' }} onClick={() => openBudgetModal(row.budget)}>
                   <div className="tool-split-row" style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <span style={{ fontWeight: 'bold' }}>
                         {row.category?.emoji ? `${row.category.emoji} ` : ''}
                         {row.category?.name ?? (lang === 'vi' ? 'Danh mục đã xóa' : 'Deleted category')}
                      </span>
                      <span style={{ fontWeight: 'bold' }}>
                         {formatCurrency(row.spent, row.budget.currencyCode)} / {formatCurrency(row.limitAmount, row.budget.currencyCode)}
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
        <section className="tool-screen recurring-tool-screen" style={{ position: 'fixed', inset: 0, background: 'var(--bg)', zIndex: 100, overflowY: 'auto', padding: '16px', paddingBottom: '40px' }}>
          <div className="tool-screen-header" style={{ display: 'flex', alignItems: 'center', marginBottom: '24px' }}>
            <button onClick={() => setActiveTool('NONE')} style={{ background: 'var(--surface)', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', width: '40px', height: '40px', borderRadius: '12px', cursor: 'pointer' }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
            </button>
            <h2 style={{ marginLeft: '16px', fontSize: '20px' }}>{lang === 'vi' ? 'Giao dịch định kỳ' : 'Recurring'}</h2>
          </div>

          <div className="tool-stat-grid" style={{ display: 'flex', gap: '16px', marginBottom: '24px' }}>
            <div className="tool-stat-card" style={{ flex: 1, background: 'var(--surface)', borderRadius: '20px', padding: '20px', boxShadow: 'var(--shadow)' }}>
              <div style={{ fontSize: '13px', color: 'var(--accent)', fontWeight: 'bold', marginBottom: '12px' }}>{lang === 'vi' ? 'Tổng thu' : 'Total Income'}</div>
              <div className="tool-stat-amount" style={{ fontSize: '20px', fontWeight: 'bold' }}>
                {formatCurrency(recurringTotalIncome, currency)}
              </div>
            </div>
            <div className="tool-stat-card" style={{ flex: 1, background: 'var(--surface)', borderRadius: '20px', padding: '20px', boxShadow: 'var(--shadow)' }}>
              <div style={{ fontSize: '13px', color: 'var(--negative)', fontWeight: 'bold', marginBottom: '12px' }}>{lang === 'vi' ? 'Tổng chi' : 'Total Expense'}</div>
              <div className="tool-stat-amount" style={{ fontSize: '20px', fontWeight: 'bold' }}>
                {formatCurrency(recurringTotalExpense, currency)}
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
            {recurringRows.length === 0 ? (
              <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '60px 20px', textAlign: 'center', boxShadow: 'var(--shadow)' }}>
                <p style={{ color: 'var(--text-muted)' }}>{lang === 'vi' ? 'Chưa có giao dịch định kỳ nào.' : 'No recurring transactions.'}</p>
              </div>
            ) : (
              recurringRows.map((rec) => (
                <div key={rec.id} className="tool-list-card recurring-row" onClick={() => openRecurringModal(rec)} style={{ display: 'flex', alignItems: 'center', gap: '16px', cursor: 'pointer', background: 'var(--surface)', borderRadius: '16px', padding: '16px', border: '1px solid var(--border)', boxShadow: 'var(--shadow)' }}>
                  <div style={{ width: '46px', height: '46px', borderRadius: '50%', border: `1px solid ${rec.type === 'EXPENSE' ? 'var(--negative)' : 'var(--positive)'}`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '16px', fontWeight: 700 }}>
                    {rec.dayOfMonth}
                  </div>
                  <div style={{ flex: 1 }}>
                    <strong style={{ fontSize: '15px', display: 'block', marginBottom: '4px' }}>{rec.category}</strong>
                    <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{rec.sourceName} {rec.note ? `• ${rec.note}` : ''}</span>
                    {rec.lastExecutedMonth && (
                      <span style={{ fontSize: '11px', color: 'var(--text-muted)', display: 'block', marginTop: '4px' }}>
                        {lang === 'vi' ? 'Đã chạy:' : 'Last run:'} {rec.lastExecutedMonth}
                      </span>
                    )}
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
        <section className="tool-screen goal-tool-screen" style={{ position: 'fixed', inset: 0, background: 'var(--bg)', zIndex: 100, overflowY: 'auto', padding: '16px', paddingBottom: '40px' }}>
          <div className="tool-screen-header" style={{ display: 'flex', alignItems: 'center', marginBottom: '24px' }}>
            <button onClick={() => setActiveTool('NONE')} style={{ background: 'var(--surface)', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', width: '40px', height: '40px', borderRadius: '12px', cursor: 'pointer' }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
            </button>
            <h2 style={{ marginLeft: '16px', fontSize: '20px' }}>{lang === 'vi' ? 'Mục tiêu tiết kiệm' : 'Saving Goals'}</h2>
          </div>

          <div className="tool-summary-card" style={{ background: 'var(--surface)', borderRadius: '20px', padding: '24px', marginBottom: '24px', boxShadow: 'var(--shadow)' }}>
            <div style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px' }}>{lang === 'vi' ? 'Tổng tích lũy' : 'Total accumulated'}</div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '16px' }}>
              <div className="tool-summary-amount" style={{ fontSize: '28px', fontWeight: 'bold', color: 'var(--savings)' }}>
                {formatCurrency(goalTotalSaved, currency)}
              </div>
              <div style={{ fontSize: '16px', color: 'var(--savings)', fontWeight: 'bold' }}>
                {goalTotalPercent}%
              </div>
            </div>
            <div style={{ width: '100%', height: '8px', background: 'rgba(255, 255, 255, 0.10)', borderRadius: '4px', overflow: 'hidden' }}>
              <div style={{ height: '100%', width: `${goalTotalPercent}%`, background: 'var(--savings)', borderRadius: '4px' }} />
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', padding: '0 4px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 'bold' }}>{lang === 'vi' ? 'Danh sách mục tiêu' : 'Goal list'}</h3>
            <button onClick={() => openGoalModal()} style={{ background: 'transparent', border: 'none', color: 'var(--accent)', fontWeight: 'bold', fontSize: '14px', cursor: 'pointer' }}>
              {lang === 'vi' ? 'Tạo mới' : 'Create new'}
            </button>
          </div>

          <div className="stack">
            {goalRows.length === 0 ? (
              <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '60px 20px', textAlign: 'center', boxShadow: 'var(--shadow)' }}>
                <p style={{ color: 'var(--text-muted)' }}>{lang === 'vi' ? 'Chưa có mục tiêu nào.' : 'No saving goals.'}</p>
              </div>
            ) : (
              goalRows.map(({ goal, currentAmount, targetAmount, percent }) => {
                return (
                  <div key={goal.id} className="tool-list-card goal-row" style={{ display: 'block', background: 'var(--surface)', borderRadius: '16px', padding: '16px', border: '1px solid var(--border)', boxShadow: 'var(--shadow)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
                      <strong style={{ fontSize: '15px' }}>{goal.name}</strong>
                      <span style={{ fontSize: '14px', color: percent >= 100 ? 'var(--positive)' : 'var(--savings)', fontWeight: 'bold' }}>{percent}%</span>
                    </div>
                    <div style={{ width: '100%', height: '6px', marginBottom: '12px', background: 'rgba(255, 255, 255, 0.08)', borderRadius: '3px', overflow: 'hidden' }}>
                      <div style={{ height: '100%', width: `${percent}%`, background: 'var(--savings)', borderRadius: '3px' }} />
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: 'var(--text-muted)' }}>
                      <span>{formatCurrency(currentAmount, goal.currency)}</span>
                      <span>{formatCurrency(targetAmount, goal.currency)}</span>
                    </div>
                    <div style={{ display: 'flex', gap: '8px', marginTop: '14px' }}>
                      <button
                        type="button"
                        onClick={() => openGoalDepositModal(goal)}
                        style={{ flex: 1, padding: '10px 12px', borderRadius: '16px', background: 'rgba(34, 197, 94, 0.12)', color: 'var(--positive)', border: '1px solid rgba(34, 197, 94, 0.18)', fontWeight: 700, cursor: 'pointer' }}
                      >
                        {lang === 'vi' ? 'Tích lũy' : 'Deposit'}
                      </button>
                      <button
                        type="button"
                        onClick={() => openGoalModal(goal)}
                        style={{ padding: '10px 16px', borderRadius: '16px', background: 'transparent', color: 'var(--text-muted)', border: '1px solid var(--border)', fontWeight: 700, cursor: 'pointer' }}
                      >
                        {lang === 'vi' ? 'Sửa' : 'Edit'}
                      </button>
                    </div>
                  </div>
                )
              })
            )}
          </div>
        </section>
      )}

      {activeTool === 'EXCHANGE_RATE' && (
        <ExchangeRateDialog
          lang={lang}
          rates={exchangeRatesData}
          loading={loadingRates}
          onClose={() => setActiveTool('NONE')}
        />
      )}

      {showPinSetupModal && (
        <PinSetupModal
          lang={lang}
          pinValue={newPinInput}
          onPinValueChange={setNewPinInput}
          onDismiss={() => setShowPinSetupModal(false)}
          onSave={(value) => {
            localStorage.setItem('kat_pin', value)
            setPin(value)
            setShowPinSetupModal(false)
          }}
        />
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
              <section className="asset-summary-card" aria-label="Tai san rong">
                <p className="asset-summary-label">
                  {selectedSourceFilter === 'ALL'
                    ? (lang === 'vi' ? 'Tài sản ròng' : 'Net Worth')
                    : `${lang === 'vi' ? 'Tài khoản' : 'Account'}: ${selectedSourceFilter}`}
                </p>
                <strong className="asset-summary-amount">
                  {formatCurrency(selectedSourceBalance, currency)}
                </strong>
                
                <div className="asset-currency-chips">
                  {Object.entries(selectedSourceCurrencies).filter(([, val]) => val !== 0).map(([cur, val]) => (
                    <span key={cur} className={`asset-currency-chip ${val >= 0 ? 'positive' : 'negative'}`}>
                      {val > 0 ? '+' : ''}{formatCurrency(val, cur)}
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
                <div className="source-filter-row">
                  {sources.map((source) => (
                    <div
                      key={source.id}
                      className={`source-chip-wrap ${openSourceActionId === source.id ? 'actions-open' : ''}`}
                      onPointerDown={handleSourceSwipeStart}
                      onPointerMove={handleSourceSwipeMove}
                      onPointerUp={() => handleSourceSwipeEnd(source.id)}
                      onPointerCancel={() => handleSourceSwipeEnd(source.id)}
                    >
                      <div className="source-chip-actions" aria-label={lang === 'vi' ? 'Thao tác tài khoản' : 'Account actions'}>
                        <button
                          type="button"
                          className="source-chip-action edit"
                          onClick={() => {
                            setOpenSourceActionId(null)
                            openSourceModal(source)
                          }}
                          aria-label={lang === 'vi' ? 'Sửa tài khoản' : 'Edit account'}
                        >
                          <Pencil size={14} />
                        </button>
                        <button
                          type="button"
                          className="source-chip-action danger"
                          onClick={() => void handleDeleteSource(source)}
                          aria-label={lang === 'vi' ? 'Xóa tài khoản' : 'Delete account'}
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                      <button
                        type="button"
                        className={`source-chip ${selectedSourceFilter === source.name ? 'active' : ''}`}
                        onClick={() => {
                          if (openSourceActionId === source.id) {
                            setOpenSourceActionId(null)
                            return
                          }
                          setSelectedSourceFilter(current => current === source.name ? 'ALL' : source.name)
                        }}
                      >
                        <strong>{source.name}</strong>
                        <span>{formatCurrency(balancesBySource[source.name] ?? 0, currency)}</span>
                      </button>
                      <div className="source-chip-inline-actions">
                        <button
                          type="button"
                          className="source-chip-mini-action"
                          onClick={() => openSourceModal(source)}
                          aria-label={lang === 'vi' ? 'Sửa tài khoản' : 'Edit account'}
                        >
                          <Pencil size={13} />
                        </button>
                        <button
                          type="button"
                          className="source-chip-mini-action danger"
                          onClick={() => void handleDeleteSource(source)}
                          aria-label={lang === 'vi' ? 'Xóa tài khoản' : 'Delete account'}
                        >
                          <Trash2 size={13} />
                        </button>
                      </div>
                    </div>
                  ))}
                  {sources.length === 0 && <p className="empty-note">Chưa có tài khoản.</p>}
                </div>
              </section>

              <section style={{ marginTop: '24px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px', gap: '12px' }}>
                  <h2 style={{ fontSize: '18px', margin: 0, fontWeight: 'bold' }}>
                    {selectedSource
                      ? `${lang === 'vi' ? 'Giao dịch' : 'Transactions'}: ${selectedSource.name}`
                      : (lang === 'vi' ? 'Giao dịch gần đây' : 'Recent Transactions')}
                  </h2>
                  <div className="source-section-actions">
                    {selectedSource && (
                      <button
                        type="button"
                        className="mini-icon danger"
                        onClick={() => void handleDeleteSource(selectedSource)}
                        aria-label={lang === 'vi' ? 'Xóa tài khoản đang chọn' : 'Delete selected account'}
                      >
                        <Trash2 size={14} />
                      </button>
                    )}
                    <span style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 600 }}>
                      {filteredTransactions.length}
                    </span>
                  </div>
                </div>
                
                <div style={{ marginBottom: '16px' }}>
                  <input 
                    type="text"
                    value={transactionSearch}
                    onChange={(event) => setTransactionSearch(event.target.value)}
                    placeholder={lang === 'vi' ? 'Tìm kiếm danh mục, ghi chú hoặc số tiền' : 'Search category, note or amount'}
                    className="dashboard-search-input"
                  />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {filteredTransactions.slice(0, showMoreTx ? undefined : 5).map((transaction) => (
                    <div key={transaction.id} onClick={() => openTxModal(transaction)} className="transaction-row" style={{ cursor: 'pointer' }}>
                      <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                        <div style={{ width: '4px', height: '50px', borderRadius: '4px', background: transactionAccent(transaction.type), marginTop: '2px' }} />
                        <div>
                          <strong style={{ display: 'block', fontSize: '16px', marginBottom: '4px' }}>{transaction.category}</strong>
                          <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
                            {transaction.note && <span style={{ display: 'block', marginBottom: '2px' }}>{transaction.note}</span>}
                            {transaction.sourceName} • {format(transaction.timestamp, 'dd/MM/yyyy HH:mm')}
                          </span>
                        </div>
                      </div>
                      <div className="row-right">
                        <b style={{ fontSize: '16px', whiteSpace: 'nowrap', marginLeft: '12px' }} className={transactionToneClass(transaction.type)}>
                          {transactionPrefix(transaction.type)}{formatCurrency(transaction.amount, transaction.currency)}
                        </b>
                        <button
                          type="button"
                          className="mini-icon danger"
                          onClick={(event) => {
                            event.stopPropagation()
                            void handleDeleteTransaction(transaction)
                          }}
                          aria-label={lang === 'vi' ? 'Xóa giao dịch' : 'Delete transaction'}
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                  ))}
                  {filteredTransactions.length === 0 && <p style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '24px' }}>{lang === 'vi' ? 'Không có giao dịch phù hợp.' : 'No matching transactions.'}</p>}
                  
                  {filteredTransactions.length > 5 && (
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
              <section className="debt-screen" style={{ padding: '16px', paddingBottom: '100px', gridColumn: '1 / -1' }}>
                <div className="debt-tab-bar" style={{ display: 'flex', background: 'var(--surface)', borderRadius: '24px', padding: '4px', marginBottom: '32px', border: '1px solid var(--border)' }}>
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

                {filteredDebtRows.length === 0 ? (
                  <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '40px 20px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '15px' }}>
                    {lang === 'vi' ? 'Chưa có khoản nợ nào trong mục này' : 'No records in this section'}
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {[...activeDebtRows, ...paidDebtRows].map((debt, index) => {
                      const remainingAmount = Math.max(0, debt.amount - (debt.paidAmount ?? 0))
                      const isOverdue = Boolean(debt.dueDate && !debt.isPaid && debt.dueDate < currentTimestamp)
                      const statusText = debt.isPaid
                        ? (lang === 'vi' ? 'Hoàn tất' : 'Completed')
                        : `${debt.type === 'DEBT' ? (lang === 'vi' ? 'Đã trả' : 'Paid') : (lang === 'vi' ? 'Đã thu' : 'Received')} ${formatCurrency(debt.paidAmount ?? 0, debt.currency)}`
                      const shouldSeparatePaid = index === activeDebtRows.length && activeDebtRows.length > 0 && paidDebtRows.length > 0

                      return (
                        <div key={debt.id} style={{ marginTop: shouldSeparatePaid ? '8px' : 0 }}>
                          <div
                            className="debt-card"
                            onClick={() => openDebtModal(debt)}
                            style={{
                              background: 'var(--surface)',
                              borderRadius: '22px',
                              padding: '13px 14px',
                              display: 'grid',
                              gridTemplateColumns: '4px minmax(0, 1fr) auto',
                              gap: '12px',
                              alignItems: 'center',
                              border: '1px solid var(--border)',
                              opacity: debt.isPaid ? 0.68 : 1,
                              cursor: 'pointer',
                            }}
                          >
                            <span style={{ width: '4px', height: '52px', borderRadius: '999px', background: 'var(--debt)' }} />
                            <div style={{ minWidth: 0 }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: 0 }}>
                                <span style={{ flex: '0 0 auto', color: 'var(--debt)', background: 'rgba(234, 179, 8, 0.12)', border: '1px solid rgba(234, 179, 8, 0.18)', borderRadius: '14px', padding: '4px 8px', fontSize: '10px', fontWeight: 700 }}>
                                  {debt.type === 'DEBT' ? (lang === 'vi' ? 'Đi vay' : 'Borrow') : (lang === 'vi' ? 'Cho vay' : 'Lend')}
                                </span>
                                <strong style={{ display: 'block', fontSize: '15px', minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{debt.personName}</strong>
                              </div>
                              {debt.note && <span style={{ display: 'block', fontSize: '12px', color: 'var(--text-muted)', marginTop: '5px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{debt.note}</span>}
                              <span style={{ display: 'block', fontSize: '10px', color: 'var(--text-muted)', marginTop: '4px' }}>
                                {format(debt.timestamp, 'dd/MM/yyyy')}
                              </span>
                              {debt.dueDate && !debt.isPaid && (
                                <span style={{ display: 'block', fontSize: '11px', color: isOverdue ? 'var(--negative)' : 'var(--text-muted)', fontWeight: 600, marginTop: '3px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                  {lang === 'vi' ? 'Hạn thanh toán:' : 'Due:'} {format(debt.dueDate, 'dd/MM/yyyy')}
                                </span>
                              )}
                            </div>
                            <div className="debt-card-side" style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '6px', maxWidth: '150px' }}>
                              <div className="debt-card-amount" style={{ fontWeight: 700, fontSize: '15px', color: 'var(--text)', maxWidth: '128px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                {formatCurrency(debt.amount, debt.currency)}
                              </div>
                              <span className="debt-status-chip" style={{ color: debt.isPaid ? 'var(--positive)' : 'var(--debt)', background: debt.isPaid ? 'rgba(34, 197, 94, 0.10)' : 'rgba(234, 179, 8, 0.10)', border: `1px solid ${debt.isPaid ? 'rgba(34, 197, 94, 0.16)' : 'rgba(234, 179, 8, 0.16)'}`, borderRadius: '14px', padding: '5px 8px', fontSize: '11px', fontWeight: 700, maxWidth: '140px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                {statusText}
                              </span>
                              {!debt.isPaid && (
                                <button
                                  type="button"
                                  onClick={(event) => {
                                    event.stopPropagation()
                                    openDebtPaymentModal(debt)
                                  }}
                                  style={{ background: 'rgba(34, 197, 94, 0.12)', border: '1px solid rgba(34, 197, 94, 0.18)', color: 'var(--positive)', padding: '5px 10px', borderRadius: '14px', fontSize: '11px', fontWeight: 700, cursor: 'pointer' }}
                                >
                                  {debt.type === 'DEBT' ? (lang === 'vi' ? 'Trả' : 'Pay') : (lang === 'vi' ? 'Thu' : 'Collect')}
                                </button>
                              )}
                              {!debt.isPaid && remainingAmount > 0 && debt.paidAmount > 0 && (
                                <span style={{ color: 'var(--text-muted)', fontSize: '10px', maxWidth: '140px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                  {lang === 'vi' ? 'Còn' : 'Left'} {formatCurrency(remainingAmount, debt.currency)}
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}
              </section>
            )}
            {activeTab === 'REPORTS' && (
              <section className="report-screen">
                
                {/* Date Picker Card */}
                <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '16px', textAlign: 'center', marginBottom: '20px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)', minWidth: 0, overflow: 'hidden' }}>
                  <div style={{ color: 'var(--muted)', fontSize: '14px', marginBottom: '8px' }}>Khoảng thời gian</div>
                  <input type="month" value={reportMonthFilter} onChange={e => setReportMonthFilter(e.target.value)} style={{ width: '150px', maxWidth: '100%', background: 'transparent', border: 'none', color: 'var(--text)', fontSize: '18px', fontWeight: 'bold', fontFamily: 'inherit', outline: 'none', textAlign: 'center' }} />
                </div>

                {/* Filters */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: '10px', marginBottom: '16px', minWidth: 0 }}>
                  <button type="button" onClick={() => setReportTypeFilter('ALL')} style={{ minWidth: 0, padding: '10px 8px', borderRadius: '16px', background: reportTypeFilter === 'ALL' ? 'rgba(76, 175, 80, 0.15)' : 'var(--surface)', color: reportTypeFilter === 'ALL' ? 'var(--accent)' : 'var(--text)', fontWeight: reportTypeFilter === 'ALL' ? 'bold' : 'normal', border: '1px solid', borderColor: reportTypeFilter === 'ALL' ? 'transparent' : 'var(--border)', fontSize: '14px' }}>Tất cả</button>
                  <button type="button" onClick={() => setReportTypeFilter('EXPENSE')} style={{ minWidth: 0, padding: '10px 8px', borderRadius: '16px', background: reportTypeFilter === 'EXPENSE' ? 'rgba(76, 175, 80, 0.15)' : 'var(--surface)', color: reportTypeFilter === 'EXPENSE' ? 'var(--accent)' : 'var(--text)', fontWeight: reportTypeFilter === 'EXPENSE' ? 'bold' : 'normal', border: '1px solid', borderColor: reportTypeFilter === 'EXPENSE' ? 'transparent' : 'var(--border)', fontSize: '14px' }}>Chi tiêu</button>
                  <button type="button" onClick={() => setReportTypeFilter('INCOME')} style={{ minWidth: 0, padding: '10px 8px', borderRadius: '16px', background: reportTypeFilter === 'INCOME' ? 'rgba(76, 175, 80, 0.15)' : 'var(--surface)', color: reportTypeFilter === 'INCOME' ? 'var(--accent)' : 'var(--text)', fontWeight: reportTypeFilter === 'INCOME' ? 'bold' : 'normal', border: '1px solid', borderColor: reportTypeFilter === 'INCOME' ? 'transparent' : 'var(--border)', fontSize: '14px' }}>Thu nhập</button>
                </div>
                <div style={{ display: 'flex', gap: '10px', marginBottom: '24px', overflowX: 'auto', minWidth: 0, WebkitOverflowScrolling: 'touch', scrollbarWidth: 'none' }} className="hide-scrollbar report-currency-row">
                  <button type="button" onClick={() => setReportCurrencyFilter('ALL')} style={{ flex: '0 0 auto', padding: '8px 16px', borderRadius: '16px', background: reportCurrencyFilter === 'ALL' ? 'rgba(76, 175, 80, 0.15)' : 'var(--surface)', color: reportCurrencyFilter === 'ALL' ? 'var(--accent)' : 'var(--text)', fontWeight: reportCurrencyFilter === 'ALL' ? 'bold' : 'normal', border: '1px solid', borderColor: reportCurrencyFilter === 'ALL' ? 'transparent' : 'var(--border)' }}>Tất cả</button>
                  {SUPPORTED_CURRENCIES.map(curr => (
                    <button key={curr} type="button" onClick={() => setReportCurrencyFilter(curr)} style={{ flex: '0 0 auto', padding: '8px 16px', borderRadius: '16px', background: reportCurrencyFilter === curr ? 'rgba(76, 175, 80, 0.15)' : 'var(--surface)', color: reportCurrencyFilter === curr ? 'var(--accent)' : 'var(--text)', fontWeight: reportCurrencyFilter === curr ? 'bold' : 'normal', border: '1px solid', borderColor: reportCurrencyFilter === curr ? 'transparent' : 'var(--border)' }}>{curr}</button>
                  ))}
                </div>

                {/* Net Cash Flow */}
                <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '24px', marginBottom: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)', minWidth: 0, overflow: 'hidden' }}>
                  <h3 style={{ fontSize: '18px', margin: '0 0 20px' }}>Dòng tiền ròng</h3>
                  <div style={{ background: 'rgba(244, 67, 54, 0.05)', borderRadius: '20px', padding: '24px 16px', textAlign: 'center', marginBottom: '24px', minWidth: 0 }}>
                    <div style={{ color: 'var(--muted)', fontSize: '14px', marginBottom: '12px' }}>Số dư còn lại</div>
                    <div style={{ color: reportIncomeTotal >= reportExpenseTotal ? '#4CAF50' : '#F44336', fontSize: 'clamp(20px, 7vw, 28px)', lineHeight: 1.2, fontWeight: 'bold', marginBottom: '16px', overflowWrap: 'anywhere' }}>{formatCurrency(reportIncomeTotal - reportExpenseTotal, reportDisplayCurrency)}</div>
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
                      <span>{formatCurrency(reportIncomeTotal, reportDisplayCurrency)}</span>
                    </div>
                    <div style={{ background: 'var(--background)', borderRadius: '4px', height: '8px', overflow: 'hidden' }}>
                      <div style={{ background: '#4CAF50', height: '100%', width: `${Math.min(100, (reportIncomeTotal / (reportIncomeTotal + reportExpenseTotal || 1)) * 100)}%`, borderRadius: '4px' }} />
                    </div>
                  </div>
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '14px', fontWeight: 'bold' }}>
                      <span style={{ color: 'var(--muted)' }}>Chi tiêu</span>
                      <span>{formatCurrency(reportExpenseTotal, reportDisplayCurrency)}</span>
                    </div>
                    <div style={{ background: 'var(--background)', borderRadius: '4px', height: '8px', overflow: 'hidden' }}>
                      <div style={{ background: '#F44336', height: '100%', width: `${Math.min(100, (reportExpenseTotal / (reportIncomeTotal + reportExpenseTotal || 1)) * 100)}%`, borderRadius: '4px' }} />
                    </div>
                  </div>
                </div>

                {/* Expense Breakdown */}
                {(reportTypeFilter === 'ALL' || reportTypeFilter === 'EXPENSE') && (
                  <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '24px 24px 0', marginBottom: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)', minWidth: 0, overflow: 'hidden' }}>
                    <div style={{ position: 'relative', height: '200px', overflow: 'hidden', display: 'flex', justifyContent: 'center' }}>
                      <div style={{ position: 'absolute', top: '-40px', width: 'min(300px, 100%)', height: '300px' }}>
                        <EChart options={categoryPieOptions} style={{ width: '100%', height: '100%' }} />
                      </div>
                      <div style={{ position: 'absolute', bottom: '30px', left: 0, right: 0, textAlign: 'center' }}>
                        <div style={{ color: 'var(--muted)', fontSize: '14px', marginBottom: '4px' }}>Tổng chi tiêu</div>
                        <div style={{ color: '#F44336', fontSize: '24px', fontWeight: 'bold' }}>{formatCurrency(reportExpenseTotal, reportDisplayCurrency)}</div>
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
                          <div style={{ color: 'var(--muted)', fontSize: '14px' }}>{formatCurrency(row.value, reportDisplayCurrency)}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Budget Comparison */}
                {(reportTypeFilter === 'ALL' || reportTypeFilter === 'EXPENSE') && reportBudgetRows.length > 0 && (
                  <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '24px', marginBottom: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)', minWidth: 0, overflow: 'hidden' }}>
                    <h3 style={{ fontSize: '18px', margin: '0 0 20px' }}>Đối chiếu ngân sách</h3>
                    
                    <div style={{ background: reportBudgetSpent > reportBudgetLimit ? 'rgba(244, 67, 54, 0.05)' : 'rgba(76, 175, 80, 0.05)', borderRadius: '20px', padding: '20px', marginBottom: '20px', border: `1px solid ${reportBudgetSpent > reportBudgetLimit ? 'rgba(244, 67, 54, 0.2)' : 'rgba(76, 175, 80, 0.2)'}` }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px', fontSize: '15px' }}>
                        <span style={{ color: 'var(--muted)' }}>Đã chi</span>
                        <span style={{ fontWeight: 'bold', minWidth: 0, overflowWrap: 'anywhere', textAlign: 'right' }}>{formatCurrency(reportBudgetSpent, reportDisplayCurrency)} / {formatCurrency(reportBudgetLimit, reportDisplayCurrency)}</span>
                      </div>
                      <div style={{ background: 'var(--border)', height: '8px', borderRadius: '4px', overflow: 'hidden' }}>
                        <div style={{ background: reportBudgetSpent > reportBudgetLimit ? '#F44336' : '#4CAF50', height: '100%', width: `${Math.min(100, reportBudgetPercent)}%`, borderRadius: '4px' }} />
                      </div>
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>

                      {reportBudgetRows.map(row => (
                        <div key={row.budget.id} style={{ border: '1px solid var(--border)', borderRadius: '16px', padding: '16px' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
                            <strong style={{ fontSize: '15px' }}>{row.category?.name}</strong>
                            <span style={{ fontWeight: 'bold', color: row.isOver ? '#F44336' : '#4CAF50' }}>{row.percent}%</span>
                          </div>
                          <div style={{ background: 'var(--background)', height: '6px', borderRadius: '3px', marginBottom: '12px', overflow: 'hidden' }}>
                            <div style={{ background: row.isOver ? '#F44336' : '#4CAF50', height: '100%', width: `${Math.min(100, row.percent)}%`, borderRadius: '3px' }} />
                          </div>
                          <div style={{ color: 'var(--muted)', fontSize: '13px' }}>{formatCurrency(row.spent, row.budget.currencyCode)} / {formatCurrency(row.limitAmount, row.budget.currencyCode)}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Daily Trend */}
                <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '24px', marginBottom: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)', minWidth: 0, overflow: 'hidden' }}>
                  <h3 style={{ fontSize: '18px', textAlign: 'center', margin: '0 0 4px' }}>Xu hướng chi tiêu hàng ngày</h3>
                  <p style={{ color: 'var(--muted)', fontSize: '13px', textAlign: 'center', margin: '0 0 24px' }}>Chạm hoặc kéo để xem chi tiết</p>
                  <EChart options={dailyTrendOptions} style={{ height: '240px' }} />
                </div>

                {/* Comparison */}
                <div style={{ background: 'var(--surface)', borderRadius: '24px', padding: '24px', marginBottom: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)', minWidth: 0, overflow: 'hidden' }}>
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
        <SettingsTab
          lang={lang}
          currency={currency}
          theme={theme}
          hasPin={!!pin}
          fileInputRef={fileInputRef}
          onCurrencyChange={setCurrency}
          onThemeChange={setTheme}
          onOpenTool={setActiveTool}
          onOpenCsv={() => setShowCsvModal(true)}
          onExportBackup={handleExportBackup}
          onPickImportFile={handlePickImportFile}
          onImportBackup={handleImportBackup}
          onPinEnabled={() => {
            setNewPinInput('')
            setShowPinSetupModal(true)
          }}
          onPinDisabled={() => {
            localStorage.removeItem('kat_pin')
            setPin('')
          }}
          onSupport={() => setShowSupportModal(true)}
          onAbout={() => setShowAboutModal(true)}
          onDonate={() => setShowDonateModal(true)}
        />
      )}



      <BottomNav
        activeTab={activeTab}
        activeTool={activeTool}
        lang={lang}
        onAddTransaction={() => openTxModal()}
        onTabChange={(tab) => {
          setActiveTab(tab)
          setActiveTool('NONE')
        }}
      />

      

      {showPwaBanner && (
        <PwaInstallBanner
          lang={lang}
          onDismiss={() => {
            setShowPwaBanner(false)
            sessionStorage.setItem('kat_pwa_dismissed', '1')
          }}
        />
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
            className="modal-card tx-modal-card"
            onSubmit={handleSaveTransaction}
            style={{ padding: 0, borderBottomLeftRadius: 0, borderBottomRightRadius: 0, height: '95vh', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}
          >
            <div className="modal-head" style={{ padding: '20px 20px 10px', justifyContent: 'center' }}>
              <h3 style={{ fontSize: '18px' }}>{editingTxId == null ? 'Thêm giao dịch' : 'Sửa giao dịch'}</h3>
            </div>

            <div className="hide-scrollbar tx-modal-body" style={{ flex: 1, overflowY: 'auto', padding: '0 20px 20px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
              
              <div style={{ display: 'flex', gap: '10px' }}>
                {['Chi tiêu', 'Thu nhập', 'Chuyển tiền'].map(t => {
                  const val = t === 'Chi tiêu' ? 'EXPENSE' : t === 'Thu nhập' ? 'INCOME' : 'TRANSFER_OUT';
                  const active = txForm.type === val;
                  return (
                    <button 
                      key={t} type="button" 
                      onClick={() => setTxForm(p => ({
                        ...p,
                        type: val,
                        category: val === 'TRANSFER_OUT' ? transferCategory(lang) : p.category,
                      }))}
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
                      onClick={() => setTxForm(p => ({
                        ...p,
                        sourceName: s.name,
                        destinationName: p.destinationName === s.name
                          ? sources.find((candidate) => candidate.name !== s.name)?.name ?? ''
                          : p.destinationName,
                      }))}
                      style={{ flex: '0 0 auto', padding: '8px 16px', borderRadius: '16px', background: txForm.sourceName === s.name ? 'rgba(76, 175, 80, 0.15)' : 'transparent', border: '1px solid', borderColor: txForm.sourceName === s.name ? 'rgba(76,175,80,0.5)' : 'var(--border)', color: txForm.sourceName === s.name ? 'var(--accent)' : 'var(--text)', fontWeight: txForm.sourceName === s.name ? 'bold' : 'normal', fontSize: '14px' }}
                    >{s.name}</button>
                  ))}
                </div>
              </div>

              {txForm.type === 'TRANSFER_OUT' && (
                <div>
                  <label style={{ marginBottom: '8px', display: 'block', fontSize: '13px', color: 'var(--muted)' }}>Tài khoản nhận</label>
                  <div className="account-slider" style={{ gap: '10px' }}>
                    {sources.filter(s => s.name !== txForm.sourceName).map(s => (
                      <button 
                        key={s.id} type="button" 
                        onClick={() => setTxForm(p => ({ ...p, destinationName: s.name }))}
                        style={{ flex: '0 0 auto', padding: '8px 16px', borderRadius: '16px', background: txForm.destinationName === s.name ? 'rgba(76, 175, 80, 0.15)' : 'transparent', border: '1px solid', borderColor: txForm.destinationName === s.name ? 'rgba(76,175,80,0.5)' : 'var(--border)', color: txForm.destinationName === s.name ? 'var(--accent)' : 'var(--text)', fontWeight: txForm.destinationName === s.name ? 'bold' : 'normal', fontSize: '14px' }}
                      >{s.name}</button>
                    ))}
                  </div>
                </div>
              )}

              {txForm.type !== 'TRANSFER_OUT' && (
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
              )}

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

            <div className="tx-numpad-panel" style={{ padding: '16px 20px', background: 'var(--surface-sunken)', borderTop: '1px solid var(--border)' }}>
              <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '12px' }}>
                <span className="tx-calc-display" style={{ fontSize: '32px', fontWeight: 'bold', color: 'var(--text)' }}>
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

              <div className="tx-keypad" style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '8px' }}>
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
                <button type="submit" style={{ padding: '0', borderRadius: '16px', background: 'var(--accent)', color: 'white', fontSize: '24px', border: 'none', cursor: 'pointer', gridRow: 'span 2', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 4px 12px rgba(22, 163, 74, 0.3)' }}>&gt;</button>

                <button type="button" onClick={() => setCalcInput(p => p === '0' ? '0' : p + '0')} style={{ padding: '16px', borderRadius: '16px', background: 'var(--surface)', color: 'var(--text)', fontSize: '20px', border: 'none', cursor: 'pointer' }}>0</button>
                <button type="button" onClick={() => setCalcInput(p => p === '0' ? '0' : p + '000')} style={{ padding: '16px', borderRadius: '16px', background: 'var(--surface)', color: 'var(--text)', fontSize: '18px', border: 'none', cursor: 'pointer' }}>000</button>
                <button type="button" onClick={() => setCalcInput(p => p.includes('.') ? p : p + '.')} style={{ padding: '16px', borderRadius: '16px', background: 'var(--surface)', color: 'var(--text)', fontSize: '24px', border: 'none', cursor: 'pointer', lineHeight: 1 }}>.</button>
              </div>

              {editingTxId != null && (
                <button
                  type="button"
                  onClick={() => {
                    const transaction = transactions.find((item) => item.id === editingTxId)
                    if (transaction) void handleDeleteTransaction(transaction)
                  }}
                  style={{ width: '100%', padding: '14px', borderRadius: '16px', background: 'rgba(239, 68, 68, 0.12)', color: 'var(--negative)', fontWeight: 'bold', fontSize: '16px', border: 'none', marginTop: '16px', cursor: 'pointer' }}
                >
                  {lang === 'vi' ? 'Xóa giao dịch' : 'Delete transaction'}
                </button>
              )}

              <button type="button" onClick={() => setTxModalOpen(false)} style={{ width: '100%', padding: '14px', borderRadius: '16px', background: 'var(--surface)', color: 'var(--text)', fontWeight: 'bold', fontSize: '16px', border: 'none', marginTop: '12px', cursor: 'pointer' }}>
                Đóng
              </button>
            </div>
          </form>
        </div>
      )}

      {isBudgetModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 200 }}>
          <form className="modal-card form-modal-card budget-form-modal" onSubmit={handleSaveBudget} style={{ padding: 0, textAlign: 'left', width: '90%', maxWidth: '400px', maxHeight: '90vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            
            <div className="hide-scrollbar form-modal-body" style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
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
                {SUPPORTED_CURRENCIES.map(curr => (
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

            <div className="form-modal-actions" style={{ padding: '24px', paddingTop: 0, display: 'flex', flexDirection: 'column', gap: '12px' }}>
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

      {payingDebt && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 210 }}>
          <form className="modal-card form-modal-card debt-pay-form-modal" onSubmit={handlePayDebt} style={{ padding: 0, textAlign: 'left', width: '90%', maxWidth: '400px', maxHeight: '90vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <div className="hide-scrollbar form-modal-body" style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
              <h3 style={{ fontSize: '22px', fontWeight: 'bold', marginBottom: '20px' }}>
                {payingDebt.type === 'DEBT'
                  ? (lang === 'vi' ? 'Thanh toán khoản vay' : 'Pay debt')
                  : (lang === 'vi' ? 'Thu khoản cho vay' : 'Collect loan')}
              </h3>

              <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '20px', padding: '14px 16px', marginBottom: '16px' }}>
                <div style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  {lang === 'vi' ? 'Còn lại cần thanh toán:' : 'Remaining:'}
                </div>
                <div style={{ color: 'var(--text)', fontSize: '16px', fontWeight: 700 }}>
                  {formatCurrency(payingDebtRemaining, payingDebt.currency)}
                </div>
              </div>

              <label style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 700, display: 'block', marginBottom: '8px' }}>
                {lang === 'vi' ? 'Tài khoản trích tiền' : 'Account'}
              </label>
              <div className="hide-scrollbar" style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px', marginBottom: '16px', WebkitOverflowScrolling: 'touch' }}>
                {(sources.length ? sources.map((source) => source.name) : [FALLBACK_SOURCE_NAME]).map(sourceName => (
                  <button
                    key={sourceName}
                    type="button"
                    onClick={() => setDebtPayForm((prev) => ({ ...prev, sourceName }))}
                    style={{ flex: '0 0 auto', padding: '10px 18px', borderRadius: '20px', border: '1px solid', background: debtPayForm.sourceName === sourceName ? 'rgba(76, 175, 80, 0.15)' : 'transparent', borderColor: debtPayForm.sourceName === sourceName ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)', color: debtPayForm.sourceName === sourceName ? 'var(--accent)' : 'var(--text)', fontWeight: debtPayForm.sourceName === sourceName ? 700 : 500, cursor: 'pointer', fontSize: '14px' }}
                  >
                    {sourceName}
                  </button>
                ))}
              </div>

              <div style={{ display: 'flex', gap: '12px', marginBottom: '8px' }}>
                <input
                  required
                  inputMode="decimal"
                  value={debtPayForm.amount}
                  onChange={(event) => setDebtPayForm((prev) => ({ ...prev, amount: event.target.value }))}
                  placeholder={lang === 'vi' ? 'Số tiền thanh toán' : 'Payment amount'}
                  style={{ flex: 1, minWidth: 0, padding: '16px 20px', borderRadius: '24px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: '15px', outline: 'none' }}
                />
                <button
                  type="button"
                  onClick={() => setDebtPayForm((prev) => ({ ...prev, amount: payingDebtRemaining.toString() }))}
                  style={{ padding: '0 18px', borderRadius: '24px', background: 'rgba(76, 175, 80, 0.15)', color: 'var(--accent)', border: 'none', fontWeight: 700, fontSize: '14px', cursor: 'pointer' }}
                >
                  {lang === 'vi' ? 'Tất cả' : 'All'}
                </button>
              </div>
            </div>

            <div className="form-modal-actions" style={{ padding: '24px', paddingTop: 0, display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <button
                type="submit"
                className="primary-button"
                style={{ width: '100%', padding: '16px', borderRadius: '24px', fontWeight: 700, fontSize: '15px' }}
              >
                {lang === 'vi' ? 'Xác nhận' : 'Confirm'}
              </button>
              <button
                type="button"
                onClick={() => {
                  setPayingDebtId(null)
                  setDebtPayForm({ amount: '', sourceName: '' })
                }}
                style={{ width: '100%', padding: '16px', borderRadius: '24px', background: 'rgba(255, 255, 255, 0.05)', color: 'var(--text)', border: 'none', fontWeight: 700, fontSize: '15px', cursor: 'pointer' }}
              >
                {lang === 'vi' ? 'Đóng' : 'Close'}
              </button>
            </div>
          </form>
        </div>
      )}

      {isDebtModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 200 }}>
          <form className="modal-card form-modal-card debt-form-modal" onSubmit={handleSaveDebt} style={{ padding: 0, textAlign: 'left', width: '90%', maxWidth: '400px', maxHeight: '90vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            
            <div className="hide-scrollbar form-modal-body" style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
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
                {SUPPORTED_CURRENCIES.map(curr => (
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

            <div className="form-modal-actions" style={{ padding: '24px', paddingTop: 0, display: 'flex', flexDirection: 'column', gap: '12px' }}>
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
      {depositingGoal && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 210 }}>
          <form className="modal-card form-modal-card goal-deposit-form-modal" onSubmit={handleDepositGoal} style={{ padding: 0, textAlign: 'left', width: '90%', maxWidth: '400px', maxHeight: '90vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <div className="hide-scrollbar form-modal-body" style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
              <h3 style={{ fontSize: '22px', fontWeight: 'bold', marginBottom: '20px' }}>
                {lang === 'vi' ? `Tích lũy cho ${depositingGoal.name}` : `Add savings to ${depositingGoal.name}`}
              </h3>

              <label style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 700, display: 'block', marginBottom: '8px' }}>
                {lang === 'vi' ? 'Tài khoản' : 'Account'}
              </label>
              <div className="hide-scrollbar" style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px', marginBottom: '14px', WebkitOverflowScrolling: 'touch' }}>
                {(sources.length ? sources.map((source) => source.name) : [FALLBACK_SOURCE_NAME]).map(sourceName => (
                  <button
                    key={sourceName}
                    type="button"
                    onClick={() => setGoalDepositForm((prev) => ({ ...prev, sourceName }))}
                    style={{ flex: '0 0 auto', padding: '10px 18px', borderRadius: '20px', border: '1px solid', background: goalDepositForm.sourceName === sourceName ? 'rgba(76, 175, 80, 0.15)' : 'transparent', borderColor: goalDepositForm.sourceName === sourceName ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)', color: goalDepositForm.sourceName === sourceName ? 'var(--accent)' : 'var(--text)', fontWeight: goalDepositForm.sourceName === sourceName ? 700 : 500, cursor: 'pointer', fontSize: '14px' }}
                  >
                    {sourceName}
                  </button>
                ))}
              </div>

              <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '20px', padding: '14px 16px', marginBottom: '16px' }}>
                <div style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  {lang === 'vi' ? 'Số dư hiện tại:' : 'Current balance:'}
                </div>
                <div style={{ color: 'var(--text)', fontSize: '14px', fontWeight: 700, overflowX: 'auto', whiteSpace: 'nowrap' }}>
                  {depositSourceBalanceText}
                </div>
              </div>

              <label style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 700, display: 'block', marginBottom: '8px' }}>
                {lang === 'vi' ? 'Ngoại tệ' : 'Currency'}
              </label>
              <div className="hide-scrollbar" style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px', marginBottom: '16px', WebkitOverflowScrolling: 'touch' }}>
                {SUPPORTED_CURRENCIES.map(curr => (
                  <button
                    key={curr}
                    type="button"
                    onClick={() => setGoalDepositForm((prev) => ({ ...prev, currency: curr }))}
                    style={{ flex: '0 0 auto', padding: '10px 18px', borderRadius: '20px', border: '1px solid', background: goalDepositForm.currency === curr ? 'rgba(76, 175, 80, 0.15)' : 'transparent', borderColor: goalDepositForm.currency === curr ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)', color: goalDepositForm.currency === curr ? 'var(--accent)' : 'var(--text)', fontWeight: goalDepositForm.currency === curr ? 700 : 500, cursor: 'pointer', fontSize: '14px' }}
                  >
                    {curr}
                  </button>
                ))}
              </div>

              <div style={{ display: 'flex', gap: '12px' }}>
                <input
                  required
                  inputMode="decimal"
                  value={goalDepositForm.amount}
                  onChange={(event) => setGoalDepositForm((prev) => ({ ...prev, amount: event.target.value }))}
                  placeholder={lang === 'vi' ? 'Số tiền' : 'Amount'}
                  style={{ flex: 1, minWidth: 0, padding: '16px 20px', borderRadius: '24px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: '15px', outline: 'none' }}
                />
                <button
                  type="button"
                  onClick={() => setGoalDepositForm((prev) => ({ ...prev, amount: `${prev.amount}000` }))}
                  style={{ padding: '0 18px', borderRadius: '24px', background: 'rgba(76, 175, 80, 0.15)', color: 'var(--accent)', border: 'none', fontWeight: 700, fontSize: '14px', cursor: 'pointer' }}
                >
                  +000
                </button>
              </div>
            </div>

            <div className="form-modal-actions" style={{ padding: '24px', paddingTop: 0, display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <button
                type="submit"
                className="primary-button"
                style={{ width: '100%', padding: '16px', borderRadius: '24px', fontWeight: 700, fontSize: '15px' }}
              >
                {lang === 'vi' ? 'Lưu' : 'Save'}
              </button>
              <button
                type="button"
                onClick={() => {
                  setDepositingGoalId(null)
                  setGoalDepositForm({ amount: '', sourceName: '', currency: 'VND' })
                }}
                style={{ width: '100%', padding: '16px', borderRadius: '24px', background: 'rgba(255, 255, 255, 0.05)', color: 'var(--text)', border: 'none', fontWeight: 700, fontSize: '15px', cursor: 'pointer' }}
              >
                {lang === 'vi' ? 'Đóng' : 'Close'}
              </button>
            </div>
          </form>
        </div>
      )}
      {isGoalModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true" style={{ zIndex: 200 }}>
          <div className="modal-card form-modal-card goal-form-modal" style={{ padding: 0, textAlign: 'center', width: '90%', maxWidth: '400px', maxHeight: '90vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            
            <div className="hide-scrollbar form-modal-body" style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
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

            </div>

            <div className="form-modal-actions" style={{ padding: '24px', paddingTop: 0, display: 'flex', flexDirection: 'column', gap: '12px' }}>
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
          <div className="modal-card form-modal-card recurring-form-modal" style={{ padding: 0, textAlign: 'left', width: '90%', maxWidth: '400px', maxHeight: '90vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            
            <div className="hide-scrollbar form-modal-body" style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
              <h3 style={{ fontSize: '22px', fontWeight: 'bold', marginBottom: '24px' }}>
                {editingRecurringId == null ? (lang === 'vi' ? 'Thiết lập giao dịch định kỳ' : 'Setup Recurring') : (lang === 'vi' ? 'Sửa giao dịch định kỳ' : 'Edit Recurring')}
              </h3>

              <div style={{ display: 'flex', gap: '12px', marginBottom: '24px' }}>
                <button
                  type="button"
                  onClick={() => setRecurringForm(p => ({ ...p, type: 'EXPENSE', category: categories.find(c => c.type === 'EXPENSE')?.name ?? (lang === 'vi' ? 'Khác' : 'Other') }))}
                  style={{ flex: 1, padding: '12px', borderRadius: '24px', border: '1px solid', background: recurringForm.type === 'EXPENSE' ? 'rgba(76, 175, 80, 0.15)' : 'transparent', borderColor: recurringForm.type === 'EXPENSE' ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)', color: recurringForm.type === 'EXPENSE' ? 'var(--accent)' : 'var(--text)', fontWeight: recurringForm.type === 'EXPENSE' ? 'bold' : 'normal', cursor: 'pointer', textAlign: 'center' }}
                >
                  {lang === 'vi' ? 'Chi định kỳ' : 'Recurring Expense'}
                </button>
                <button
                  type="button"
                  onClick={() => setRecurringForm(p => ({ ...p, type: 'INCOME', category: categories.find(c => c.type === 'INCOME')?.name ?? (lang === 'vi' ? 'Thu khác' : 'Other income') }))}
                  style={{ flex: 1, padding: '12px', borderRadius: '24px', border: '1px solid', background: recurringForm.type === 'INCOME' ? 'rgba(76, 175, 80, 0.15)' : 'transparent', borderColor: recurringForm.type === 'INCOME' ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)', color: recurringForm.type === 'INCOME' ? 'var(--accent)' : 'var(--text)', fontWeight: recurringForm.type === 'INCOME' ? 'bold' : 'normal', cursor: 'pointer', textAlign: 'center' }}
                >
                  {lang === 'vi' ? 'Thu định kỳ' : 'Recurring Income'}
                </button>
              </div>

              <label style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px', display: 'block' }}>{lang === 'vi' ? 'Tài khoản' : 'Account'}</label>
              <div className="hide-scrollbar" style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px', marginBottom: '16px', WebkitOverflowScrolling: 'touch' }}>
                {(sources.length ? sources.map((source) => source.name) : [FALLBACK_SOURCE_NAME]).map(sourceName => (
                  <button
                    key={sourceName}
                    type="button"
                    onClick={() => setRecurringForm(p => ({ ...p, sourceName }))}
                    style={{ flex: '0 0 auto', padding: '12px 24px', borderRadius: '20px', border: '1px solid', background: recurringForm.sourceName === sourceName ? 'rgba(76, 175, 80, 0.15)' : 'transparent', borderColor: recurringForm.sourceName === sourceName ? 'rgba(76, 175, 80, 0.3)' : 'var(--border)', color: recurringForm.sourceName === sourceName ? 'var(--accent)' : 'var(--text)', fontWeight: recurringForm.sourceName === sourceName ? 'bold' : 'normal', cursor: 'pointer' }}
                  >
                    {sourceName}
                  </button>
                ))}
              </div>

              <label style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px', display: 'block' }}>{lang === 'vi' ? 'Tiền tệ' : 'Currency'}</label>
              <div className="hide-scrollbar" style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px', marginBottom: '16px', WebkitOverflowScrolling: 'touch' }}>
                {SUPPORTED_CURRENCIES.map(curr => (
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
                {(categories.filter(c => c.type === recurringForm.type).length > 0 ? categories.filter(c => c.type === recurringForm.type) : [{ id: -1, emoji: '', name: recurringForm.type === 'INCOME' ? (lang === 'vi' ? 'Thu khác' : 'Other income') : (lang === 'vi' ? 'Khác' : 'Other'), type: recurringForm.type }]).map(c => (
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

            <div className="form-modal-actions" style={{ padding: '24px', paddingTop: 0, display: 'flex', flexDirection: 'column', gap: '12px' }}>
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
                    onClick={() => setCsvFilter(p => ({ ...p, type: type.value as CsvFilterType }))}
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
