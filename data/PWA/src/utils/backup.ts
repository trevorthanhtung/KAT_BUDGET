import { db, type Budget, type Category, type Debt, type MoneySource, type RecurringTransaction, type SavingGoal, type Transaction } from '../data/db'
import { defaultCategoryPresets, KAT_BACKUP_PREFIX } from './constants'
import { decodeBase64Utf8, gzipString, ungzipBlob } from './helpers'

type BackupRecord = Record<string, unknown>

type NativeBackupPayload = {
  sources?: BackupRecord[]
  transactions?: BackupRecord[]
  categories?: BackupRecord[]
  budgets?: BackupRecord[]
  debts?: BackupRecord[]
  recurrings?: BackupRecord[]
  goals?: BackupRecord[]
  saving_goals?: BackupRecord[]
}

const FALLBACK_CATEGORY = 'Khac'
const FALLBACK_SOURCE = 'Tien mat'
const FALLBACK_GOAL = 'Muc tieu da khoi phuc'

function isRecord(value: unknown): value is BackupRecord {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function recordArray(value: unknown): BackupRecord[] {
  return Array.isArray(value) ? value.filter(isRecord) : []
}

function cleanString(value: unknown, fallback = ''): string {
  return typeof value === 'string' && value.trim() ? value.trim() : fallback
}

function optionalString(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined
}

function cleanNumber(value: unknown, fallback = 0): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const parsed = Number(value.replaceAll(',', '').trim())
    if (Number.isFinite(parsed)) return parsed
  }
  return fallback
}

function cleanBoolean(value: unknown, fallback = false): boolean {
  return typeof value === 'boolean' ? value : fallback
}

function normalizeCurrency(value: unknown): string {
  const currency = cleanString(value, 'VND').toUpperCase()
  return currency === 'VNĐ' || currency === '' ? 'VND' : currency
}

function validSourceType(value: string): MoneySource['type'] {
  return value === 'BANK' || value === 'WALLET' || value === 'CASH' || value === 'SAVINGS'
    ? value
    : 'CASH'
}

function validCategoryType(value: string): Category['type'] {
  return value === 'INCOME' || value === 'EXPENSE' ? value : 'EXPENSE'
}

function validDebtType(value: string): Debt['type'] {
  return value === 'LOAN' || value === 'DEBT' ? value : 'DEBT'
}

function validRecurringType(value: string): RecurringTransaction['type'] {
  return value === 'INCOME' || value === 'EXPENSE' ? value : 'EXPENSE'
}

function positiveInt(value: unknown): number | null {
  const parsed = Math.round(cleanNumber(value, 0))
  return parsed > 0 ? parsed : null
}

function allocateId(preferredId: number | null, usedIds: Set<number>): number {
  if (preferredId && !usedIds.has(preferredId)) {
    usedIds.add(preferredId)
    return preferredId
  }

  let nextId = 1
  while (usedIds.has(nextId)) nextId += 1
  usedIds.add(nextId)
  return nextId
}

async function readBackupText(file: Blob): Promise<string> {
  try {
    return await ungzipBlob(file)
  } catch {
    const rawText = (await file.text()).trim()
    if (rawText.startsWith(KAT_BACKUP_PREFIX)) {
      return decodeBase64Utf8(rawText.slice(KAT_BACKUP_PREFIX.length).trim())
    }

    try {
      JSON.parse(rawText)
      return rawText
    } catch {
      return decodeBase64Utf8(rawText)
    }
  }
}

function parsePayload(text: string): NativeBackupPayload {
  const parsed: unknown = JSON.parse(text)
  if (!isRecord(parsed)) throw new Error('Invalid backup payload')

  return {
    sources: recordArray(parsed.sources),
    transactions: recordArray(parsed.transactions),
    categories: recordArray(parsed.categories),
    budgets: recordArray(parsed.budgets),
    debts: recordArray(parsed.debts),
    recurrings: recordArray(parsed.recurrings),
    goals: recordArray(parsed.goals),
    saving_goals: recordArray(parsed.saving_goals),
  }
}

function toNativeTransaction(tx: Transaction): BackupRecord {
  return {
    amount: tx.amount,
    type: tx.type,
    category: tx.category,
    note: tx.note,
    timestamp: tx.timestamp,
    sourceName: tx.sourceName,
    currency: normalizeCurrency(tx.currency),
    projectTag: tx.projectTag ?? null,
    imageUri: tx.imageUri ?? null,
    destinationName: tx.destinationName ?? null,
  }
}

function toNativeBudget(budget: Budget, categoryNamesById: Map<number, string>): BackupRecord {
  return {
    categoryId: budget.categoryId,
    category: categoryNamesById.get(budget.categoryId) ?? '',
    limitAmount: budget.limitAmountMinor,
    currency: normalizeCurrency(budget.currencyCode),
    monthYear: budget.monthYear,
  }
}

export async function createAndroidBackupBlob(): Promise<Blob> {
  const [sources, transactions, categories, budgets, debts, recurrings, goals] = await Promise.all([
    db.sources.toArray(),
    db.transactions.toArray(),
    db.categories.toArray(),
    db.budgets.toArray(),
    db.debts.toArray(),
    db.recurrings.toArray(),
    db.saving_goals.toArray(),
  ])

  const categoryNamesById = new Map(categories.map((category) => [category.id, category.name]))
  const payload = {
    sources: sources.map((source) => ({
      name: source.name,
      type: source.type,
      includeInTotal: source.includeInTotal,
      interestRate: source.interestRate,
      interestPeriod: source.interestPeriod,
      createdTimestamp: source.createdTimestamp,
    })),
    transactions: transactions.map(toNativeTransaction),
    debts: debts.map((debt) => ({
      personName: debt.personName,
      amount: debt.amount,
      currency: normalizeCurrency(debt.currency),
      type: debt.type,
      note: debt.note,
      timestamp: debt.timestamp,
      isPaid: debt.isPaid,
      dueDate: debt.dueDate ?? null,
      paidAmount: debt.paidAmount,
    })),
    budgets: budgets.map((budget) => toNativeBudget(budget, categoryNamesById)),
    recurrings: recurrings.map((recurring) => ({
      amount: recurring.amount,
      type: recurring.type,
      category: recurring.category,
      note: recurring.note,
      sourceName: recurring.sourceName,
      currency: normalizeCurrency(recurring.currency),
      dayOfMonth: recurring.dayOfMonth,
      lastExecutedMonth: recurring.lastExecutedMonth,
    })),
    goals: goals.map((goal) => ({
      name: goal.name,
      targetAmount: goal.targetAmount,
      currentAmount: goal.currentAmount,
      currency: normalizeCurrency(goal.currency),
    })),
    categories: categories.map((category) => ({
      id: category.id,
      name: category.name,
      emoji: category.emoji,
      type: category.type,
    })),
  }

  return gzipString(JSON.stringify(payload))
}

async function restoreCategories(records: BackupRecord[]) {
  if (records.length === 0) {
    await db.categories.bulkAdd(defaultCategoryPresets)
    return db.categories.toArray()
  }

  const usedIds = new Set<number>()
  const categories = records
    .map((record) => ({
      id: allocateId(positiveInt(record.id), usedIds),
      name: cleanString(record.name, FALLBACK_CATEGORY),
      emoji: cleanString(record.emoji, ''),
      type: validCategoryType(cleanString(record.type, 'EXPENSE')),
    }))
    .filter((category) => category.name)

  if (categories.length === 0) {
    await db.categories.bulkAdd(defaultCategoryPresets)
  } else {
    await db.categories.bulkPut(categories)
  }

  return db.categories.toArray()
}

async function restoreSources(records: BackupRecord[]) {
  const usedIds = new Set<number>()
  const sources = records
    .map((record) => ({
      id: allocateId(positiveInt(record.id), usedIds),
      name: cleanString(record.name, FALLBACK_SOURCE),
      type: validSourceType(cleanString(record.type, 'CASH')),
      includeInTotal: cleanBoolean(record.includeInTotal, true),
      interestRate: Math.max(0, cleanNumber(record.interestRate, 0)),
      interestPeriod: cleanString(record.interestPeriod, 'NONE'),
      createdTimestamp: cleanNumber(record.createdTimestamp, Date.now()),
    }))
    .filter((source) => source.name)

  if (sources.length === 0) {
    await db.sources.add({
      name: FALLBACK_SOURCE,
      type: 'CASH',
      includeInTotal: true,
      interestRate: 0,
      interestPeriod: 'NONE',
      createdTimestamp: Date.now(),
    })
    return
  }

  await db.sources.bulkPut(sources)
}

function normalizeTransactions(records: BackupRecord[]): Transaction[] {
  const usedIds = new Set<number>()
  return records.map((record) => ({
    id: allocateId(positiveInt(record.id), usedIds),
    amount: Math.max(0, cleanNumber(record.amount, 0)),
    type: cleanString(record.type, 'EXPENSE'),
    category: cleanString(record.category, FALLBACK_CATEGORY),
    note: cleanString(record.note, ''),
    timestamp: cleanNumber(record.timestamp, Date.now()),
    sourceName: cleanString(record.sourceName, FALLBACK_SOURCE),
    currency: normalizeCurrency(record.currency),
    projectTag: optionalString(record.projectTag),
    imageUri: optionalString(record.imageUri),
    destinationName: optionalString(record.destinationName),
  }))
}

function normalizeDebts(records: BackupRecord[]): Debt[] {
  const usedIds = new Set<number>()
  return records.map((record) => {
    const amount = Math.max(0, cleanNumber(record.amount, 0))
    return {
      id: allocateId(positiveInt(record.id), usedIds),
      personName: cleanString(record.personName, ''),
      amount,
      currency: normalizeCurrency(record.currency),
      type: validDebtType(cleanString(record.type, 'DEBT')),
      note: cleanString(record.note, ''),
      timestamp: cleanNumber(record.timestamp, Date.now()),
      isPaid: cleanBoolean(record.isPaid, false),
      dueDate: positiveInt(record.dueDate),
      paidAmount: Math.min(amount, Math.max(0, cleanNumber(record.paidAmount, 0))),
    }
  })
}

async function ensureCategory(
  record: BackupRecord,
  categoriesById: Map<number, Category>,
  categoriesByName: Map<string, Category>,
): Promise<number> {
  const nativeCategoryId = positiveInt(record.categoryId)
  if (nativeCategoryId && categoriesById.has(nativeCategoryId)) return nativeCategoryId

  const categoryName = cleanString(record.category, FALLBACK_CATEGORY)
  const existingByName = categoriesByName.get(categoryName.toLowerCase())
  if (existingByName) return existingByName.id

  const createdId = await db.categories.add({
    name: categoryName,
    emoji: '',
    type: 'EXPENSE',
  })
  const createdCategory: Category = {
    id: createdId,
    name: categoryName,
    emoji: '',
    type: 'EXPENSE',
  }
  categoriesById.set(createdId, createdCategory)
  categoriesByName.set(categoryName.toLowerCase(), createdCategory)
  return createdId
}

async function restoreBudgets(records: BackupRecord[], categories: Category[]) {
  const usedIds = new Set<number>()
  const categoriesById = new Map(categories.map((category) => [category.id, category]))
  const categoriesByName = new Map(categories.map((category) => [category.name.toLowerCase(), category]))
  const budgets: Budget[] = []

  for (const record of records) {
    const currency = normalizeCurrency(record.currencyCode ?? record.currency)
    const limitAmount = Math.round(
      Math.max(0, cleanNumber(record.limitAmountMinor ?? record.limitAmount ?? record.limit, 0)),
    )
    budgets.push({
      id: allocateId(positiveInt(record.id), usedIds),
      categoryId: await ensureCategory(record, categoriesById, categoriesByName),
      limitAmountMinor: limitAmount,
      currencyCode: currency,
      monthYear: cleanString(record.monthYear, new Date().toISOString().slice(0, 7)),
    })
  }

  if (budgets.length > 0) await db.budgets.bulkPut(budgets)
}

function normalizeRecurrings(records: BackupRecord[]): RecurringTransaction[] {
  const usedIds = new Set<number>()
  return records.map((record) => ({
    id: allocateId(positiveInt(record.id), usedIds),
    amount: Math.max(0, cleanNumber(record.amount, 0)),
    type: validRecurringType(cleanString(record.type, 'EXPENSE')),
    category: cleanString(record.category, FALLBACK_CATEGORY),
    note: cleanString(record.note, ''),
    sourceName: cleanString(record.sourceName, FALLBACK_SOURCE),
    currency: normalizeCurrency(record.currency),
    dayOfMonth: Math.min(31, Math.max(1, Math.round(cleanNumber(record.dayOfMonth, 1)))),
    lastExecutedMonth: cleanString(record.lastExecutedMonth, ''),
  }))
}

function normalizeGoals(records: BackupRecord[]): SavingGoal[] {
  const usedIds = new Set<number>()
  let restoredIndex = 1
  return records.map((record) => {
    const currentAmount = Math.max(0, cleanNumber(record.currentAmount, 0))
    const targetAmount = Math.max(currentAmount, cleanNumber(record.targetAmount, currentAmount || 1))
    return {
      id: allocateId(positiveInt(record.id), usedIds),
      name: cleanString(record.name, `${FALLBACK_GOAL} ${restoredIndex++}`),
      targetAmount,
      currentAmount,
      currency: normalizeCurrency(record.currency),
    }
  })
}

async function ensureReferencedSources(records: Array<Pick<Transaction, 'sourceName'> | Pick<RecurringTransaction, 'sourceName'>>) {
  const existingSources = new Set((await db.sources.toArray()).map((source) => source.name.trim()))
  for (const record of records) {
    const sourceName = record.sourceName.trim() || FALLBACK_SOURCE
    if (existingSources.has(sourceName)) continue
    await db.sources.add({
      name: sourceName,
      type: 'CASH',
      includeInTotal: true,
      interestRate: 0,
      interestPeriod: 'NONE',
      createdTimestamp: Date.now(),
    })
    existingSources.add(sourceName)
  }
}

export async function importBackupFile(file: Blob): Promise<void> {
  const payload = parsePayload(await readBackupText(file))
  const goalRecords = payload.goals?.length ? payload.goals : payload.saving_goals ?? []

  await db.transaction('rw', [db.sources, db.transactions, db.categories, db.budgets, db.debts, db.saving_goals, db.recurrings], async () => {
    await db.transactions.clear()
    await db.budgets.clear()
    await db.recurrings.clear()
    await db.saving_goals.clear()
    await db.debts.clear()
    await db.sources.clear()
    await db.categories.clear()

    const restoredCategories = await restoreCategories(payload.categories ?? [])
    await restoreSources(payload.sources ?? [])

    const transactions = normalizeTransactions(payload.transactions ?? [])
    const recurrings = normalizeRecurrings(payload.recurrings ?? [])
    await ensureReferencedSources([...transactions, ...recurrings])

    if (transactions.length > 0) await db.transactions.bulkPut(transactions)
    if (payload.debts?.length) await db.debts.bulkPut(normalizeDebts(payload.debts))
    await restoreBudgets(payload.budgets ?? [], restoredCategories)
    if (recurrings.length > 0) await db.recurrings.bulkPut(recurrings)
    if (goalRecords.length > 0) await db.saving_goals.bulkPut(normalizeGoals(goalRecords))
  })
}
