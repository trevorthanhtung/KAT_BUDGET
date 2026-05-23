import { type Budget, type Category, type MoneySource, type Transaction, type Debt, type SavingGoal, type RecurringTransaction } from '../data/db'

export type SourceForm = {
  name: string
  type: MoneySource['type']
  includeInTotal: boolean
  initialBalance: string
  interestRate: string
  interestPeriod: string
}

export type GoalForm = {
  name: string
  targetAmount: string
  currentAmount: string
  currency: string
}

export type RecurringForm = {
  amount: string
  type: 'INCOME' | 'EXPENSE'
  category: string
  note: string
  sourceName: string
  currency: string
  dayOfMonth: string
}

export type TxForm = {
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

export type CategoryTypeFilter = 'ALL' | 'INCOME' | 'EXPENSE'

export type CategoryForm = {
  name: string
  emoji: string
  type: 'INCOME' | 'EXPENSE'
}

export type BudgetForm = {
  monthYear: string
  categoryId: string
  limitAmount: string
  currencyCode: string
}

export type DebtForm = {
  personName: string
  amount: string
  currency: string
  type: 'DEBT' | 'LOAN'
  note: string
  dueDate: string
}

export type BackupPayload = {
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
