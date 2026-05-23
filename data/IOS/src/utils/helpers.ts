import { type Transaction } from '../data/db'

export function parseAmount(value: string): number {
  const normalized = value.replaceAll(',', '').trim()
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : 0
}

export function formatCurrency(amount: number, currency = 'VND'): string {
  return `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(Math.round(amount))} ${currency}`
}

export function transactionDelta(tx: Transaction): number {
  if (tx.type === 'INCOME' || tx.type === 'TRANSFER_IN') return tx.amount
  if (tx.type === 'EXPENSE' || tx.type === 'TRANSFER_OUT') return -tx.amount
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
