import {
  ArrowDownRight,
  ArrowUpRight,
  BarChart3,
  CircleDollarSign,
  CreditCard,
  Download,
  FileUp,
  Pencil,
  PiggyBank,
  Plus,
  Search,
  Settings,
  Tags,
  Trash2,
  WalletCards,
  Users,
  CheckCircle,
} from 'lucide-react'
import { useLiveQuery } from 'dexie-react-hooks'
import { format } from 'date-fns'
import { type ChangeEvent, type FormEvent, useEffect, useRef, useState } from 'react'
import { db, type Budget, type Category, type MoneySource, type Transaction, type Debt } from './data/db'
import './App.css'

type SourceForm = {
  name: string
  type: MoneySource['type']
  includeInTotal: boolean
  initialBalance: string
}

type TxForm = {
  sourceName: string
  type: 'INCOME' | 'EXPENSE'
  amount: string
  category: string
  note: string
  currency: string
}

type TxTypeFilter = 'ALL' | 'INCOME' | 'EXPENSE'
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
}

const defaultSourceForm: SourceForm = {
  name: '',
  type: 'CASH',
  includeInTotal: true,
  initialBalance: '',
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

const sourceTypeLabel: Record<MoneySource['type'], string> = {
  BANK: 'Ngan hang',
  WALLET: 'Vi dien tu',
  CASH: 'Tien mat',
  SAVINGS: 'Tiet kiem',
}

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

function getRecentMonthKeys(monthCount: number): string[] {
  const result: string[] = []
  const now = new Date()
  for (let index = monthCount - 1; index >= 0; index -= 1) {
    const cursor = new Date(now.getFullYear(), now.getMonth() - index, 1)
    result.push(format(cursor, 'yyyy-MM'))
  }
  return result
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

  const [isSourceModalOpen, setSourceModalOpen] = useState(false)
  const [isTxModalOpen, setTxModalOpen] = useState(false)
  const [isCategoryModalOpen, setCategoryModalOpen] = useState(false)
  const [isBudgetModalOpen, setBudgetModalOpen] = useState(false)
  const [isDebtModalOpen, setDebtModalOpen] = useState(false)
  
  const [editingSourceId, setEditingSourceId] = useState<number | null>(null)
  const [editingTxId, setEditingTxId] = useState<number | null>(null)
  const [editingCategoryId, setEditingCategoryId] = useState<number | null>(null)
  const [editingBudgetId, setEditingBudgetId] = useState<number | null>(null)
  const [editingDebtId, setEditingDebtId] = useState<number | null>(null)
  
  const [sourceForm, setSourceForm] = useState<SourceForm>(defaultSourceForm)
  const [txForm, setTxForm] = useState<TxForm>(defaultTxForm)
  const [categoryForm, setCategoryForm] = useState<CategoryForm>(defaultCategoryForm)
  const [budgetForm, setBudgetForm] = useState<BudgetForm>(defaultBudgetForm)
  const [debtForm, setDebtForm] = useState<DebtForm>(defaultDebtForm)
  const [sourceFilter, setSourceFilter] = useState<string>('ALL')
  const [txTypeFilter, setTxTypeFilter] = useState<TxTypeFilter>('ALL')
  const [categoryTypeFilter, setCategoryTypeFilter] = useState<CategoryTypeFilter>('ALL')
  const [budgetMonthFilter, setBudgetMonthFilter] = useState(format(new Date(), 'yyyy-MM'))
  const [reportMonthFilter, setReportMonthFilter] = useState(format(new Date(), 'yyyy-MM'))
  const [txSearch, setTxSearch] = useState('')
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

  for (const tx of transactions) {
    const delta = transactionDelta(tx)
    balancesBySource[tx.sourceName] = (balancesBySource[tx.sourceName] ?? 0) + delta
    if (format(tx.timestamp, 'yyyy-MM') !== monthKey) continue
    if (delta > 0) monthIncome += delta
    if (delta < 0) monthExpense += Math.abs(delta)
  }

  let netWorth = 0
  for (const source of sources) {
    if (!source.includeInTotal) continue
    netWorth += balancesBySource[source.name] ?? 0
  }

  const budgetUsagePercent =
    monthIncome <= 0 ? 0 : Math.min(100, Math.round((monthExpense / monthIncome) * 100))

  const normalizedSearch = txSearch.trim().toLowerCase()
  const filteredTransactions = transactions.filter((tx) => {
    if (sourceFilter !== 'ALL' && tx.sourceName !== sourceFilter) return false
    if (txTypeFilter !== 'ALL' && tx.type !== txTypeFilter) return false
    if (!normalizedSearch) return true

    const searchable = `${tx.category} ${tx.note} ${tx.sourceName} ${tx.amount}`.toLowerCase()
    return searchable.includes(normalizedSearch)
  })
  const visibleTransactions = filteredTransactions.slice(0, 24)
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
  const reportExpenseByCategory: Record<string, number> = {}
  for (const tx of transactions) {
    if (format(tx.timestamp, 'yyyy-MM') !== reportMonthFilter) continue
    if (tx.type === 'INCOME') {
      reportIncomeTotal += tx.amount
      continue
    }
    if (tx.type === 'EXPENSE') {
      reportExpenseTotal += tx.amount
      reportExpenseByCategory[tx.category] = (reportExpenseByCategory[tx.category] ?? 0) + tx.amount
    }
  }

  const reportCategoryRows = Object.entries(reportExpenseByCategory)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 8)
    .map(([categoryName, amount]) => ({
      categoryName,
      amount,
      percent: reportExpenseTotal <= 0 ? 0 : Math.round((amount / reportExpenseTotal) * 100),
    }))

  const monthlyCashflowRows = getRecentMonthKeys(6).map((monthYear) => {
    let income = 0
    let expense = 0
    for (const tx of transactions) {
      if (format(tx.timestamp, 'yyyy-MM') !== monthYear) continue
      if (tx.type === 'INCOME') income += tx.amount
      if (tx.type === 'EXPENSE') expense += tx.amount
    }
    return {
      monthYear,
      income,
      expense,
      net: income - expense,
    }
  })

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
        type: tx.type === 'INCOME' ? 'INCOME' : 'EXPENSE',
        amount: tx.amount.toString(),
        category: tx.category,
        note: tx.note,
        currency: tx.currency,
      })
    } else {
      setEditingTxId(null)
      setTxForm({
        ...defaultTxForm,
        sourceName: sources[0]?.name ?? '',
      })
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
      setEditingDebtId(debt.id)
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
        interestRate: 0,
        interestPeriod: 'NONE',
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
    const hasTransactions = transactions.some((tx) => tx.sourceName === source.name)
    if (hasTransactions) {
      setStatusMessage('Khong the xoa tai khoan dang co giao dich.')
      return
    }
    await db.sources.delete(source.id)
    setStatusMessage('Da xoa tai khoan.')
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

  const handleSaveTransaction = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const amount = parseAmount(txForm.amount)
    if (amount <= 0 || !txForm.sourceName) return

    const category = txForm.category.trim() || (txForm.type === 'INCOME' ? 'Thu nhap' : 'Chi tieu')
    const normalizedCurrency = txForm.currency.trim().toUpperCase() || 'VND'

    if (editingTxId == null) {
      await db.transactions.add({
        amount,
        type: txForm.type,
        category,
        note: txForm.note.trim(),
        timestamp: Date.now(),
        sourceName: txForm.sourceName,
        currency: normalizedCurrency,
      })
      setStatusMessage('Da them giao dich.')
    } else {
      const existing = transactions.find((tx) => tx.id === editingTxId)
      if (!existing) return
      await db.transactions.put({
        ...existing,
        amount,
        type: txForm.type,
        category,
        note: txForm.note.trim(),
        sourceName: txForm.sourceName,
        currency: normalizedCurrency,
      })
      setStatusMessage('Da cap nhat giao dich.')
    }

    setTxModalOpen(false)
    setEditingTxId(null)
  }

  const handleDeleteTransaction = async (id: number) => {
    await db.transactions.delete(id)
    setStatusMessage('Da xoa giao dich.')
  }

  const handleSaveDebt = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const amount = parseAmount(debtForm.amount)
    const personName = debtForm.personName.trim()
    if (amount <= 0 || !personName) return

    const dueDateTimestamp = debtForm.dueDate ? new Date(debtForm.dueDate).getTime() : null

    if (editingDebtId == null) {
      await db.debts.add({
        personName,
        amount,
        currency: debtForm.currency.trim().toUpperCase() || 'VND',
        type: debtForm.type,
        note: debtForm.note.trim(),
        timestamp: Date.now(),
        isPaid: false,
        dueDate: dueDateTimestamp,
        paidAmount: 0,
      })
      setStatusMessage('Da them ghi chu vay/no.')
    } else {
      const existing = debts.find((d) => d.id === editingDebtId)
      if (!existing) return
      await db.debts.put({
        ...existing,
        personName,
        amount,
        currency: debtForm.currency.trim().toUpperCase() || 'VND',
        type: debtForm.type,
        note: debtForm.note.trim(),
        dueDate: dueDateTimestamp,
      })
      setStatusMessage('Da cap nhat ghi chu vay/no.')
    }

    setDebtModalOpen(false)
    setEditingDebtId(null)
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
    }

    const rawJson = JSON.stringify(payload)
    const blob = await gzipString(rawJson)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `KatBudget_Backup_${format(new Date(), 'yyyyMMdd_HHmmss')}.kat`
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

      await db.transaction('rw', db.sources, db.transactions, db.categories, db.budgets, db.debts, async () => {
        await db.sources.clear()
        await db.transactions.clear()
        await db.categories.clear()
        await db.budgets.clear()
        await db.debts.clear()

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
      })
      setStatusMessage('Da phuc hoi backup .kat/.json.')
    } catch {
      setStatusMessage('File backup khong hop le hoac sai dinh dang .kat/.json.')
    } finally {
      event.target.value = ''
    }
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">KAT Budget PWA</p>
          <h1>Tong quan tai chinh</h1>
        </div>
        <button className="icon-button" type="button" aria-label="Cai dat">
          <Settings size={20} />
        </button>
      </header>

      {statusMessage && <p className="status-banner">{statusMessage}</p>}

      <section className="balance-panel" aria-label="Tai san rong">
        <div>
          <p className="panel-label">Tai san rong</p>
          <strong>{formatCurrency(netWorth)}</strong>
        </div>
        <div className="cashflow">
          <span><ArrowUpRight size={16} /> {formatCurrency(monthIncome)}</span>
          <span><ArrowDownRight size={16} /> {formatCurrency(monthExpense)}</span>
        </div>
      </section>

      <section className="quick-actions" aria-label="Tac vu nhanh">
        <button type="button" className="action-tile" onClick={() => openTxModal()}>
          <Plus size={20} /> Giao dich
        </button>
        <button type="button" className="action-tile" onClick={() => openSourceModal()}>
          <CreditCard size={20} /> Tai khoan
        </button>
        <button type="button" className="action-tile" onClick={() => openCategoryModal()}>
          <Tags size={20} /> Danh muc
        </button>
        <button type="button" className="action-tile" onClick={() => openBudgetModal()}>
          <PiggyBank size={20} /> Ngan sach
        </button>
        <button type="button" className="action-tile" onClick={() => openDebtModal()}>
          <Users size={20} /> Vay / No
        </button>
        <button type="button" className="action-tile" onClick={handleExportBackup}>
          <Download size={20} /> Sao luu .kat
        </button>
        <button type="button" className="action-tile" onClick={handlePickImportFile}>
          <FileUp size={20} /> Phuc hoi
        </button>
        <input
          ref={fileInputRef}
          type="file"
          className="visually-hidden"
          accept=".json,.kat,application/json,text/plain"
          onChange={handleImportBackup}
        />
      </section>

      <section className="content-grid">
        <article className="section-block">
          <div className="section-title">
            <h2>Tai khoan</h2>
            <WalletCards size={20} />
          </div>
          <div className="stack">
            {sources.map((source) => (
              <div className="account-row" key={source.id}>
                <div>
                  <strong>{source.name}</strong>
                  <span>{sourceTypeLabel[source.type]}</span>
                </div>
                <div className="row-right">
                  <b>{formatCurrency(balancesBySource[source.name] ?? 0)}</b>
                  <div className="row-actions">
                    <button type="button" className="mini-icon" onClick={() => openSourceModal(source)} aria-label="Sua tai khoan">
                      <Pencil size={14} />
                    </button>
                    <button type="button" className="mini-icon danger" onClick={() => handleDeleteSource(source)} aria-label="Xoa tai khoan">
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
              </div>
            ))}
            {sources.length === 0 && <p className="empty-note">Chua co tai khoan nao.</p>}
          </div>
        </article>

        <article className="section-block">
          <div className="section-title">
            <h2>Danh muc</h2>
            <Tags size={20} />
          </div>

          <div className="category-filter-bar">
            <label>
              <span>Nhom</span>
              <select
                value={categoryTypeFilter}
                onChange={(event) => setCategoryTypeFilter(event.target.value as CategoryTypeFilter)}
              >
                <option value="ALL">Tat ca</option>
                <option value="INCOME">Thu nhap</option>
                <option value="EXPENSE">Chi tieu</option>
              </select>
            </label>
          </div>

          <div className="stack">
            {filteredCategories.map((category) => (
              <div className="category-row" key={category.id}>
                <div>
                  <strong>{category.emoji ? `${category.emoji} ${category.name}` : category.name}</strong>
                  <span>{category.type === 'INCOME' ? 'Thu nhap' : 'Chi tieu'}</span>
                </div>
                <div className="row-actions">
                  <button type="button" className="mini-icon" onClick={() => openCategoryModal(category)} aria-label="Sua danh muc">
                    <Pencil size={14} />
                  </button>
                  <button type="button" className="mini-icon danger" onClick={() => handleDeleteCategory(category)} aria-label="Xoa danh muc">
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))}
            {filteredCategories.length === 0 && <p className="empty-note">Chua co danh muc phu hop.</p>}
          </div>
        </article>

        <article className="section-block">
          <div className="section-title">
            <h2>Ngan sach thang</h2>
            <PiggyBank size={20} />
          </div>

          <div className="budget-toolbar">
            <label>
              <span>Thang</span>
              <input
                type="month"
                value={budgetMonthFilter}
                onChange={(event) => setBudgetMonthFilter(event.target.value)}
              />
            </label>
            <button type="button" className="mini-action" onClick={() => openBudgetModal()}>
              <Plus size={14} /> Them ngan sach
            </button>
          </div>

          <div className="budget-summary">
            <span>Da chi: {formatCurrency(totalBudgetSpent)}</span>
            <span>Han muc: {formatCurrency(totalBudgetLimit)}</span>
            <span>{totalBudgetPercent}%</span>
          </div>

          {overBudgetRows.length > 0 && (
            <p className="budget-alert">
              Co {overBudgetRows.length} danh muc vuot ngan sach, tong vuot {formatCurrency(overBudgetTotal)}.
            </p>
          )}

          <div className="stack">
            {budgetRows.map((row) => (
              <div className="budget-row" key={row.budget.id}>
                <div>
                  <strong>
                    {row.category?.emoji ? `${row.category.emoji} ` : ''}
                    {row.category?.name ?? 'Danh muc da xoa'}
                  </strong>
                  <span>
                    {formatCurrency(row.spent, row.budget.currencyCode)} / {formatCurrency(row.budget.limitAmountMinor, row.budget.currencyCode)}
                  </span>
                  <div className="budget-progress">
                    <div className="budget-progress-bar">
                      <div className={`budget-progress-fill${row.isOver ? ' over' : ''}`} style={{ width: `${row.percent}%` }} />
                    </div>
                    <span className={row.isOver ? 'expense' : ''}>
                      {row.isOver ? `Vuot ${formatCurrency(Math.abs(row.remaining), row.budget.currencyCode)}` : `Con lai ${formatCurrency(row.remaining, row.budget.currencyCode)}`}
                    </span>
                  </div>
                </div>
                <div className="row-actions">
                  <button type="button" className="mini-icon" onClick={() => openBudgetModal(row.budget)} aria-label="Sua ngan sach">
                    <Pencil size={14} />
                  </button>
                  <button type="button" className="mini-icon danger" onClick={() => handleDeleteBudget(row.budget.id)} aria-label="Xoa ngan sach">
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))}
            {budgetRows.length === 0 && <p className="empty-note">Chua co ngan sach cho thang nay.</p>}
          </div>
        </article>

        <article className="section-block">
          <div className="section-title">
            <h2>Bao cao</h2>
            <BarChart3 size={20} />
          </div>

          <div className="report-toolbar">
            <label>
              <span>Thang</span>
              <input
                type="month"
                value={reportMonthFilter}
                onChange={(event) => setReportMonthFilter(event.target.value)}
              />
            </label>
          </div>

          <div className="report-summary">
            <span>Thu: {formatCurrency(reportIncomeTotal)}</span>
            <span>Chi: {formatCurrency(reportExpenseTotal)}</span>
            <span className={reportIncomeTotal - reportExpenseTotal < 0 ? 'expense' : 'income'}>
              Net: {formatCurrency(reportIncomeTotal - reportExpenseTotal)}
            </span>
          </div>

          <div className="stack">
            {reportCategoryRows.map((row) => (
              <div className="report-category-row" key={row.categoryName}>
                <div>
                  <strong>{row.categoryName}</strong>
                  <span>{formatCurrency(row.amount)} • {row.percent}%</span>
                  <div className="report-bar-track">
                    <div className="report-bar-fill" style={{ width: `${row.percent}%` }} />
                  </div>
                </div>
              </div>
            ))}
            {reportCategoryRows.length === 0 && <p className="empty-note">Thang nay chua co chi tieu de thong ke.</p>}
          </div>

          <div className="report-monthly-grid">
            {monthlyCashflowRows.map((row) => (
              <div className="report-month-cell" key={row.monthYear}>
                <strong>{row.monthYear}</strong>
                <span className="income">+{formatCurrency(row.income)}</span>
                <span className="expense">-{formatCurrency(row.expense)}</span>
                <span className={row.net < 0 ? 'expense' : 'income'}>
                  {row.net < 0 ? '-' : '+'}{formatCurrency(Math.abs(row.net))}
                </span>
              </div>
            ))}
          </div>
        </article>

        <article className="section-block">
          <div className="section-title">
            <h2>Giao dich</h2>
            <CircleDollarSign size={20} />
          </div>

          <div className="tx-filters">
            <label>
              <span><Search size={14} /> Tim kiem</span>
              <input
                value={txSearch}
                onChange={(event) => setTxSearch(event.target.value)}
                placeholder="Danh muc, ghi chu, so tien..."
              />
            </label>

            <label>
              <span>Loai</span>
              <select value={txTypeFilter} onChange={(event) => setTxTypeFilter(event.target.value as TxTypeFilter)}>
                <option value="ALL">Tat ca</option>
                <option value="INCOME">Thu nhap</option>
                <option value="EXPENSE">Chi tieu</option>
              </select>
            </label>

            <label>
              <span>Tai khoan</span>
              <select value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)}>
                <option value="ALL">Tat ca</option>
                {sources.map((source) => (
                  <option key={source.id} value={source.name}>
                    {source.name}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="stack">
            {visibleTransactions.map((transaction) => (
              <div className="transaction-row" key={transaction.id}>
                <div>
                  <strong>{transaction.category}</strong>
                  <span>
                    {transaction.sourceName} • {format(transaction.timestamp, 'dd/MM HH:mm')}
                  </span>
                  {transaction.note && <span className="note-line">{transaction.note}</span>}
                </div>
                <div className="row-right">
                  <b className={transaction.type === 'INCOME' ? 'income' : 'expense'}>
                    {transaction.type === 'INCOME' ? '+' : '-'}
                    {formatCurrency(transaction.amount, transaction.currency)}
                  </b>
                  <div className="row-actions">
                    <button type="button" className="mini-icon" onClick={() => openTxModal(transaction)} aria-label="Sua giao dich">
                      <Pencil size={14} />
                    </button>
                    <button type="button" className="mini-icon danger" onClick={() => handleDeleteTransaction(transaction.id)} aria-label="Xoa giao dich">
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
              </div>
            ))}
            {visibleTransactions.length === 0 && <p className="empty-note">Khong co giao dich phu hop bo loc.</p>}
          </div>
        </article>

        <article className="section-block">
          <div className="section-title">
            <h2>Vay / No</h2>
            <Users size={20} />
          </div>
          <div className="stack">
            {debts.map((debt) => (
              <div className={`transaction-row ${debt.isPaid ? 'paid-debt' : ''}`} key={debt.id} style={{ opacity: debt.isPaid ? 0.6 : 1 }}>
                <div>
                  <strong>{debt.personName}</strong>
                  <span>
                    {debt.type === 'DEBT' ? 'Vay' : 'Cho vay'} • {format(debt.timestamp, 'dd/MM')}
                    {debt.dueDate ? ` • Han: ${format(debt.dueDate, 'dd/MM/yyyy')}` : ''}
                  </span>
                  {debt.note && <span className="note-line">{debt.note}</span>}
                </div>
                <div className="row-right">
                  <b className={debt.type === 'DEBT' ? 'income' : 'expense'}>
                    {debt.type === 'DEBT' ? '+' : '-'}{formatCurrency(debt.amount, debt.currency)}
                  </b>
                  <div className="row-actions">
                    <button type="button" className={`mini-icon ${debt.isPaid ? 'success' : ''}`} onClick={() => handleTogglePaidDebt(debt)} aria-label="Danh dau da tra">
                      <CheckCircle size={14} color={debt.isPaid ? '#16A34A' : 'currentColor'} />
                    </button>
                    <button type="button" className="mini-icon" onClick={() => openDebtModal(debt)} aria-label="Sua ghi chu vay">
                      <Pencil size={14} />
                    </button>
                    <button type="button" className="mini-icon danger" onClick={() => handleDeleteDebt(debt.id)} aria-label="Xoa ghi chu vay">
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
              </div>
            ))}
            {debts.length === 0 && <p className="empty-note">Khong co ghi chu vay/no nao.</p>}
          </div>
        </article>
      </section>

      <section className="report-strip" aria-label="Bao cao nhanh">
        <div>
          <BarChart3 size={22} />
          <strong>Chi / Thu trong thang</strong>
        </div>
        <progress
          max="100"
          value={budgetUsagePercent}
          aria-label={`Da dung ${budgetUsagePercent} phan tram theo thu nhap`}
        />
        <span>{budgetUsagePercent}%</span>
      </section>

      <nav className="bottom-nav" aria-label="Dieu huong chinh">
        <button className="active" type="button"><WalletCards size={20} /> Tong quan</button>
        <button type="button"><BarChart3 size={20} /> Bao cao</button>
        <button type="button"><PiggyBank size={20} /> Muc tieu</button>
        <button type="button"><Settings size={20} /> Cai dat</button>
      </nav>

      {isSourceModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <form className="modal-card" onSubmit={handleSaveSource}>
            <div className="modal-head">
              <h3>{editingSourceId == null ? 'Tao tai khoan' : 'Sua tai khoan'}</h3>
              <button type="button" onClick={() => setSourceModalOpen(false)}>
                Dong
              </button>
            </div>

            <label>
              Ten tai khoan
              <input
                required
                value={sourceForm.name}
                onChange={(event) => setSourceForm((prev) => ({ ...prev, name: event.target.value }))}
              />
            </label>

            <label>
              Loai
              <select
                value={sourceForm.type}
                onChange={(event) =>
                  setSourceForm((prev) => ({ ...prev, type: event.target.value as MoneySource['type'] }))
                }
              >
                <option value="CASH">Tien mat</option>
                <option value="BANK">Ngan hang</option>
                <option value="WALLET">Vi dien tu</option>
                <option value="SAVINGS">Tiet kiem</option>
              </select>
            </label>

            {editingSourceId == null && (
              <label>
                So du ban dau
                <input
                  inputMode="decimal"
                  placeholder="0"
                  value={sourceForm.initialBalance}
                  onChange={(event) =>
                    setSourceForm((prev) => ({ ...prev, initialBalance: event.target.value }))
                  }
                />
              </label>
            )}

            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={sourceForm.includeInTotal}
                onChange={(event) =>
                  setSourceForm((prev) => ({ ...prev, includeInTotal: event.target.checked }))
                }
              />
              Tinh vao tai san rong
            </label>

            <button className="primary-button" type="submit">
              {editingSourceId == null ? 'Luu tai khoan' : 'Cap nhat tai khoan'}
            </button>
          </form>
        </div>
      )}

      {isTxModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <form className="modal-card" onSubmit={handleSaveTransaction}>
            <div className="modal-head">
              <h3>{editingTxId == null ? 'Them giao dich' : 'Sua giao dich'}</h3>
              <button type="button" onClick={() => setTxModalOpen(false)}>
                Dong
              </button>
            </div>

            <label>
              Tai khoan
              <select
                value={txForm.sourceName}
                onChange={(event) => setTxForm((prev) => ({ ...prev, sourceName: event.target.value }))}
              >
                {sources.map((source) => (
                  <option key={source.id} value={source.name}>
                    {source.name}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Loai giao dich
              <select
                value={txForm.type}
                onChange={(event) => setTxForm((prev) => ({ ...prev, type: event.target.value as TxForm['type'] }))}
              >
                <option value="EXPENSE">Chi tieu</option>
                <option value="INCOME">Thu nhap</option>
              </select>
            </label>

            <label>
              So tien
              <input
                required
                inputMode="decimal"
                placeholder="0"
                value={txForm.amount}
                onChange={(event) => setTxForm((prev) => ({ ...prev, amount: event.target.value }))}
              />
            </label>

            <label>
              Danh muc
              <input
                list={`category-presets-${txForm.type.toLowerCase()}`}
                value={txForm.category}
                onChange={(event) => setTxForm((prev) => ({ ...prev, category: event.target.value }))}
                placeholder={txForm.type === 'INCOME' ? 'Thu nhap' : 'Chi tieu'}
              />
              <datalist id={`category-presets-${txForm.type.toLowerCase()}`}>
                {txCategoryPresets.map((category) => (
                  <option
                    key={category.id}
                    value={category.name}
                    label={category.emoji ? `${category.emoji} ${category.name}` : category.name}
                  />
                ))}
              </datalist>
            </label>

            <label>
              Ghi chu
              <input
                value={txForm.note}
                onChange={(event) => setTxForm((prev) => ({ ...prev, note: event.target.value }))}
              />
            </label>

            <label>
              Don vi tien
              <input
                value={txForm.currency}
                onChange={(event) => setTxForm((prev) => ({ ...prev, currency: event.target.value }))}
              />
            </label>

            <button className="primary-button" type="submit">
              {editingTxId == null ? 'Luu giao dich' : 'Cap nhat giao dich'}
            </button>
          </form>
        </div>
      )}

      {isBudgetModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <form className="modal-card" onSubmit={handleSaveBudget}>
            <div className="modal-head">
              <h3>{editingBudgetId == null ? 'Tao ngan sach' : 'Sua ngan sach'}</h3>
              <button type="button" onClick={() => setBudgetModalOpen(false)}>
                Dong
              </button>
            </div>

            <label>
              Thang ap dung
              <input
                required
                type="month"
                value={budgetForm.monthYear}
                onChange={(event) => setBudgetForm((prev) => ({ ...prev, monthYear: event.target.value }))}
              />
            </label>

            <label>
              Danh muc chi tieu
              <select
                required
                value={budgetForm.categoryId}
                onChange={(event) => setBudgetForm((prev) => ({ ...prev, categoryId: event.target.value }))}
              >
                {expenseCategories.map((category) => (
                  <option key={category.id} value={category.id.toString()}>
                    {category.emoji ? `${category.emoji} ` : ''}{category.name}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Han muc
              <input
                required
                inputMode="decimal"
                placeholder="0"
                value={budgetForm.limitAmount}
                onChange={(event) => setBudgetForm((prev) => ({ ...prev, limitAmount: event.target.value }))}
              />
            </label>

            <label>
              Don vi tien
              <input
                value={budgetForm.currencyCode}
                onChange={(event) => setBudgetForm((prev) => ({ ...prev, currencyCode: event.target.value }))}
              />
            </label>

            <button className="primary-button" type="submit">
              {editingBudgetId == null ? 'Luu ngan sach' : 'Cap nhat ngan sach'}
            </button>
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
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <form className="modal-card" onSubmit={handleSaveDebt}>
            <div className="modal-head">
              <h3>{editingDebtId == null ? 'Them ghi chu vay/no' : 'Sua ghi chu vay/no'}</h3>
              <button type="button" onClick={() => setDebtModalOpen(false)}>
                Dong
              </button>
            </div>

            <label>
              Ten nguoi vay / cho vay
              <input
                required
                value={debtForm.personName}
                onChange={(event) => setDebtForm((prev) => ({ ...prev, personName: event.target.value }))}
                placeholder="Vi du: Tuan, Ngoc..."
              />
            </label>

            <label>
              Loai
              <select
                value={debtForm.type}
                onChange={(event) => setDebtForm((prev) => ({ ...prev, type: event.target.value as DebtForm['type'] }))}
              >
                <option value="DEBT">Di vay (Minh nhan tien)</option>
                <option value="LOAN">Cho vay (Minh dua tien)</option>
              </select>
            </label>

            <label>
              So tien
              <input
                required
                inputMode="decimal"
                placeholder="0"
                value={debtForm.amount}
                onChange={(event) => setDebtForm((prev) => ({ ...prev, amount: event.target.value }))}
              />
            </label>
            
            <label>
              Ngay dao han (khong bat buoc)
              <input
                type="date"
                value={debtForm.dueDate}
                onChange={(event) => setDebtForm((prev) => ({ ...prev, dueDate: event.target.value }))}
              />
            </label>

            <label>
              Ghi chu
              <input
                value={debtForm.note}
                onChange={(event) => setDebtForm((prev) => ({ ...prev, note: event.target.value }))}
              />
            </label>

            <button className="primary-button" type="submit">
              {editingDebtId == null ? 'Luu ghi chu' : 'Cap nhat ghi chu'}
            </button>
          </form>
        </div>
      )}
    </main>
  )
}

export default App
