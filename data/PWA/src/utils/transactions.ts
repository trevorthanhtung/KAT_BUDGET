import type { Transaction } from '../data/db'
import type { AppLanguage } from '../types'

export const TRANSFER_TAG_PREFIX = 'TRF_'

const OPENING_BALANCE_LABELS = new Set(['Số dư ban đầu', 'So du ban dau', 'Opening Balance'])

export function isOpeningBalanceTransaction(transaction: Transaction): boolean {
  return OPENING_BALANCE_LABELS.has(transaction.category.trim())
}

export function isTransferTransaction(transaction: Transaction): boolean {
  return transaction.type === 'TRANSFER_IN' || transaction.type === 'TRANSFER_OUT'
}

export function isTransferTag(projectTag?: string): boolean {
  return Boolean(projectTag?.startsWith(TRANSFER_TAG_PREFIX))
}

export function makeTransferTag(timestamp: number): string {
  return `${TRANSFER_TAG_PREFIX}${timestamp}_${Date.now()}`
}

export function transferCategory(lang: AppLanguage): string {
  return lang === 'vi' ? 'Chuyển tiền' : 'Transfer'
}

export function transferOutNote(destinationName: string, note: string, lang: AppLanguage): string {
  const suffix = note.trim() ? ` (${note.trim()})` : ''
  return `${lang === 'vi' ? 'Chuyển đến' : 'Transfer to'}: ${destinationName.trim()}${suffix}`
}

export function transferInNote(sourceName: string, note: string, lang: AppLanguage): string {
  const suffix = note.trim() ? ` (${note.trim()})` : ''
  return `${lang === 'vi' ? 'Chuyển từ' : 'Transfer from'}: ${sourceName.trim()}${suffix}`
}

export function transactionPrefix(type: string): string {
  if (['EXPENSE', 'GOAL_DEPOSIT', 'TRANSFER_OUT', 'LENDING', 'DEBT_PAYMENT'].includes(type)) return '-'
  if (['INCOME', 'TRANSFER_IN', 'BORROWING', 'DEBT_COLLECTION'].includes(type)) return '+'
  return ''
}

export function transactionToneClass(type: string): string {
  if (type === 'TRANSFER_IN' || type === 'TRANSFER_OUT') return 'transfer'
  if (transactionPrefix(type) === '+') return 'income'
  if (transactionPrefix(type) === '-') return 'expense'
  return ''
}

export function transactionAccent(type: string): string {
  if (type === 'TRANSFER_IN' || type === 'TRANSFER_OUT') return 'var(--info)'
  if (transactionPrefix(type) === '+') return 'var(--positive)'
  if (transactionPrefix(type) === '-') return 'var(--negative)'
  return 'var(--muted)'
}

export function evaluateAmountExpression(input: string): number {
  const normalized = input
    .replace(/×/g, '*')
    .replace(/÷/g, '/')
    .replace(/,/g, '')
    .replace(/\s+/g, '')

  if (!normalized) return 0

  const tokens = normalized.match(/(\d+(?:\.\d+)?|[+\-*/])/g)
  if (!tokens || tokens.join('') !== normalized) return Number.NaN
  if (['+', '*', '/'].includes(tokens[0])) return Number.NaN
  if (['+', '-', '*', '/'].includes(tokens[tokens.length - 1])) return Number.NaN

  const values: number[] = []
  const ops: string[] = []

  for (let index = 0; index < tokens.length; index += 1) {
    const token = tokens[index]
    if (token === '-' && (index === 0 || ['+', '-', '*', '/'].includes(tokens[index - 1]))) {
      const next = Number(tokens[index + 1])
      if (!Number.isFinite(next)) return Number.NaN
      values.push(-next)
      index += 1
      continue
    }

    if (['+', '-', '*', '/'].includes(token)) {
      if (['+', '-', '*', '/'].includes(tokens[index - 1])) return Number.NaN
      ops.push(token)
      continue
    }

    const value = Number(token)
    if (!Number.isFinite(value)) return Number.NaN
    values.push(value)
  }

  for (let index = 0; index < ops.length;) {
    const op = ops[index]
    if (op !== '*' && op !== '/') {
      index += 1
      continue
    }

    const left = values[index]
    const right = values[index + 1]
    const result = op === '*' ? left * right : left / right
    if (!Number.isFinite(result)) return Number.NaN
    values.splice(index, 2, result)
    ops.splice(index, 1)
  }

  return ops.reduce((total, op, index) => (
    op === '+' ? total + values[index + 1] : total - values[index + 1]
  ), values[0])
}
