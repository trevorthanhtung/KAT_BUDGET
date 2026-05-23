import Dexie, { type EntityTable } from 'dexie'

export type MoneySource = {
  id: number
  name: string
  type: 'BANK' | 'WALLET' | 'CASH' | 'SAVINGS'
  includeInTotal: boolean
  interestRate: number
  interestPeriod: 'NONE' | 'DAILY' | 'MONTHLY' | string
  createdTimestamp: number
}

export type Transaction = {
  id: number
  amount: number
  type: 'INCOME' | 'EXPENSE' | 'TRANSFER_IN' | 'TRANSFER_OUT' | string
  category: string
  note: string
  timestamp: number
  projectTag?: string
  sourceName: string
  currency: string
  imageUri?: string
}

export type Budget = {
  id: number
  categoryId: number
  limitAmountMinor: number
  currencyCode: string
  monthYear: string
}

export type Category = {
  id: number
  name: string
  emoji: string
  type: 'INCOME' | 'EXPENSE'
}

export type Debt = {
  id: number
  personName: string
  amount: number
  currency: string
  type: 'DEBT' | 'LOAN'
  note: string
  timestamp: number
  isPaid: boolean
  dueDate: number | null
  paidAmount: number
}

export const db = new Dexie('kat_budget_pwa') as Dexie & {
  sources: EntityTable<MoneySource, 'id'>
  transactions: EntityTable<Transaction, 'id'>
  budgets: EntityTable<Budget, 'id'>
  categories: EntityTable<Category, 'id'>
  debts: EntityTable<Debt, 'id'>
}

db.version(1).stores({
  sources: '++id, name, type, includeInTotal, createdTimestamp',
  transactions: '++id, timestamp, type, category, sourceName, projectTag',
  budgets: '++id, monthYear, categoryId, currencyCode',
  categories: '++id, name, type',
})

db.version(2).stores({
  debts: '++id, personName, type, timestamp, isPaid, dueDate',
})
