import { useLiveQuery } from 'dexie-react-hooks'
import { db } from '../data/db'

export function useDatabase() {
  const sources = useLiveQuery(() => db.sources.orderBy('createdTimestamp').toArray(), []) ?? []
  const transactions = useLiveQuery(() => db.transactions.orderBy('timestamp').reverse().toArray(), []) ?? []
  const categories = useLiveQuery(() => db.categories.orderBy('name').toArray(), []) ?? []
  const budgets = useLiveQuery(() => db.budgets.orderBy('monthYear').reverse().toArray(), []) ?? []
  const debts = useLiveQuery(() => db.debts.orderBy('timestamp').reverse().toArray(), []) ?? []
  const savingGoals = useLiveQuery(() => db.saving_goals.orderBy('name').toArray(), []) ?? []
  const recurrings = useLiveQuery(() => db.recurrings.toArray(), []) ?? []

  return {
    sources,
    transactions,
    categories,
    budgets,
    debts,
    savingGoals,
    recurrings,
  }
}
