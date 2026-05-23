import { format } from 'date-fns'
import type { Category } from '../data/db'
import type { SourceForm, DebtForm, TxForm, CategoryForm, BudgetForm } from '../types'

export const defaultSourceForm: SourceForm = {
  name: '',
  type: 'CASH',
  includeInTotal: true,
  initialBalance: '',
  interestRate: '',
  interestPeriod: '1 tháng',
}

export const defaultDebtForm: DebtForm = {
  personName: '',
  amount: '',
  currency: 'VND',
  type: 'DEBT',
  note: '',
  dueDate: '',
}

export const defaultTxForm: TxForm = {
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

export const defaultCategoryForm: CategoryForm = {
  name: '',
  emoji: '',
  type: 'EXPENSE',
}

export const defaultBudgetForm: BudgetForm = {
  monthYear: format(new Date(), 'yyyy-MM'),
  categoryId: '',
  limitAmount: '',
  currencyCode: 'VND',
}

export const defaultCategoryPresets: Array<Omit<Category, 'id'>> = [
  { name: 'Luong', emoji: 'PAY', type: 'INCOME' },
  { name: 'Thuong', emoji: 'BONUS', type: 'INCOME' },
  { name: 'Dau tu', emoji: 'INV', type: 'INCOME' },
  { name: 'An uong', emoji: 'FOOD', type: 'EXPENSE' },
  { name: 'Di chuyen', emoji: 'TRAVEL', type: 'EXPENSE' },
  { name: 'Hoa don', emoji: 'BILL', type: 'EXPENSE' },
  { name: 'Mua sam', emoji: 'SHOP', type: 'EXPENSE' },
  { name: 'Suc khoe', emoji: 'HEALTH', type: 'EXPENSE' },
]

export const KAT_BACKUP_PREFIX = 'KAT1:'

export const SUPPORTED_CURRENCIES = [
  'VND', 'USD', 'EUR', 'GBP', 'JPY',
  'AUD', 'CAD', 'CHF', 'CNY', 'DKK',
  'HKD', 'INR', 'KRW', 'KWD', 'MYR',
  'NOK', 'RUB', 'SAR', 'SEK', 'SGD',
  'THB'
]
