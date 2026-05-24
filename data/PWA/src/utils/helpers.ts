import { type Transaction } from '../data/db'

const ZERO_DECIMAL_CURRENCIES = new Set(['VND', 'JPY', 'KRW'])

export function parseAmount(value: string): number {
  const normalized = value.replaceAll(',', '').trim()
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : 0
}

export function normalizeCurrency(currency?: string): string {
  return (currency || 'VND').trim().toUpperCase() || 'VND'
}

export function formatCurrency(amount: number, currency = 'VND'): string {
  return `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(Math.round(amount))} ${currency}`
}

export function budgetMajorToMinor(amount: number, currency?: string): number {
  const normalizedCurrency = normalizeCurrency(currency)
  const multiplier = ZERO_DECIMAL_CURRENCIES.has(normalizedCurrency) ? 1 : 100
  return Math.round(amount * multiplier)
}

export function budgetMinorToMajor(amountMinor: number, currency?: string): number {
  const normalizedCurrency = normalizeCurrency(currency)
  const divisor = ZERO_DECIMAL_CURRENCIES.has(normalizedCurrency) ? 1 : 100
  return amountMinor / divisor
}

export function convertCurrency(
  amount: number,
  fromCurrency: string | undefined,
  toCurrency: string | undefined,
  exchangeRate: number,
): number {
  const from = normalizeCurrency(fromCurrency)
  const to = normalizeCurrency(toCurrency)
  const rate = Number.isFinite(exchangeRate) && exchangeRate > 0 ? exchangeRate : 25000

  if (from === to) return amount
  if (from === 'VND' && to !== 'VND') return amount / rate
  if (from !== 'VND' && to === 'VND') return amount * rate
  return amount
}

export function transactionDelta(tx: Transaction): number {
  if (tx.type === 'INCOME' || tx.type === 'TRANSFER_IN') return tx.amount
  if (tx.type === 'EXPENSE' || tx.type === 'TRANSFER_OUT' || tx.type === 'GOAL_DEPOSIT') return -tx.amount
  return 0
}

export async function gzipString(value: string): Promise<Blob> {
  const stream = new Blob([value], { type: 'application/json' }).stream()
  const compressedStream = stream.pipeThrough(new CompressionStream('gzip'))
  return new Response(compressedStream).blob()
}

export async function ungzipBlob(blob: Blob): Promise<string> {
  const stream = blob.stream()
  const decompressedStream = stream.pipeThrough(new DecompressionStream('gzip'))
  return new Response(decompressedStream).text()
}

export function decodeBase64Utf8(base64Value: string): string {
  const binary = atob(base64Value)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return new TextDecoder().decode(bytes)
}
