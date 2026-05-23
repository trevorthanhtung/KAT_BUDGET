import { useMemo } from 'react'
import { format } from 'date-fns'
import type { MoneySource, Transaction, Category, Budget } from '../data/db'
import { transactionDelta } from '../utils/helpers'

interface UseCalculationsProps {
  sources: MoneySource[]
  transactions: Transaction[]
  categories: Category[]
  budgets: Budget[]
  exchangeRate: number
  budgetMonthFilter: string
  reportMonthFilter: string
  reportTypeFilter: 'ALL' | 'EXPENSE' | 'INCOME'
  reportCurrencyFilter: string
  categoryTypeFilter: 'ALL' | 'INCOME' | 'EXPENSE'
}

export function useCalculations({
  sources,
  transactions,
  categories,
  budgets,
  exchangeRate,
  budgetMonthFilter,
  reportMonthFilter,
  reportTypeFilter,
  reportCurrencyFilter,
  categoryTypeFilter
}: UseCalculationsProps) {

  return useMemo(() => {
    // 1. Dashboard calculations
    const balancesBySource: Record<string, number> = {}
    let monthIncome = 0
    let monthExpense = 0
    const monthKey = format(new Date(), 'yyyy-MM')

    transactions.forEach((tx) => {
      const delta = transactionDelta(tx)
      let convertedDelta = delta
      if (tx.currency && tx.currency !== 'VND') {
        convertedDelta = delta * exchangeRate
      }
      balancesBySource[tx.sourceName] = (balancesBySource[tx.sourceName] ?? 0) + convertedDelta
      if (format(tx.timestamp, 'yyyy-MM') !== monthKey) return
      if (delta > 0) monthIncome += delta
      if (delta < 0) monthExpense += Math.abs(delta)
    })

    let netWorth = 0
    for (const source of sources) {
      if (!source.includeInTotal) continue
      netWorth += balancesBySource[source.name] ?? 0
    }

    const netWorthByCurrency: Record<string, number> = {}
    transactions.forEach((tx) => {
      const source = sources.find((s) => s.name === tx.sourceName)
      if (source && !source.includeInTotal) return
      const delta = transactionDelta(tx)
      const cur = tx.currency || 'VND'
      netWorthByCurrency[cur] = (netWorthByCurrency[cur] ?? 0) + delta
    })

    // 2. Categories
    const filteredCategories = categories.filter((category) => {
      if (categoryTypeFilter === 'ALL') return true
      return category.type === categoryTypeFilter
    })
    
    const expenseCategories = categories.filter((category) => category.type === 'EXPENSE')
    const expenseCategoryIdByName = new Map(
      expenseCategories.map((category) => [category.name.toLowerCase(), category.id]),
    )
    const categoriesById = new Map(categories.map((category) => [category.id, category]))

    // 3. Budgets
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

    // 4. Reports
    let reportIncomeTotal = 0
    let reportExpenseTotal = 0
    let prevPeriodExpenseTotal = 0
    const reportBalancesByCurrency: Record<string, number> = {}
    const reportExpenseByCategory: Record<string, number> = {}
    const reportDailyExpenses: Record<string, number> = {}

    const prevMonthDate = new Date(reportMonthFilter + '-01')
    prevMonthDate.setMonth(prevMonthDate.getMonth() - 1)
    const prevMonthFilter = format(prevMonthDate, 'yyyy-MM')

    for (const tx of transactions) {
      const txMonth = format(tx.timestamp, 'yyyy-MM')
      if (txMonth === prevMonthFilter) {
        if (reportCurrencyFilter === 'ALL' || tx.currency === reportCurrencyFilter) {
          if (tx.type === 'EXPENSE') prevPeriodExpenseTotal += tx.amount
        }
      }
      
      if (txMonth !== reportMonthFilter) continue
      if (reportTypeFilter !== 'ALL' && tx.type !== reportTypeFilter) continue
      if (reportCurrencyFilter !== 'ALL' && tx.currency !== reportCurrencyFilter) continue

      if (tx.type === 'INCOME') {
        reportIncomeTotal += tx.amount
        reportBalancesByCurrency[tx.currency] = (reportBalancesByCurrency[tx.currency] ?? 0) + tx.amount
      }
      if (tx.type === 'EXPENSE') {
        reportExpenseTotal += tx.amount
        reportBalancesByCurrency[tx.currency] = (reportBalancesByCurrency[tx.currency] ?? 0) - tx.amount
        reportExpenseByCategory[tx.category] = (reportExpenseByCategory[tx.category] ?? 0) + tx.amount

        const day = format(tx.timestamp, 'dd/MM')
        reportDailyExpenses[day] = (reportDailyExpenses[day] ?? 0) + tx.amount
      }
    }

    return {
      balancesBySource,
      monthIncome,
      monthExpense,
      netWorth,
      netWorthByCurrency,
      filteredCategories,
      expenseCategories,
      monthlyBudgets,
      budgetRows,
      totalBudgetLimit,
      totalBudgetSpent,
      totalBudgetPercent,
      overBudgetRows,
      overBudgetTotal,
      reportIncomeTotal,
      reportExpenseTotal,
      prevPeriodExpenseTotal,
      reportBalancesByCurrency,
      reportExpenseByCategory,
      reportDailyExpenses
    }
  }, [
    sources,
    transactions,
    categories,
    budgets,
    exchangeRate,
    budgetMonthFilter,
    reportMonthFilter,
    reportTypeFilter,
    reportCurrencyFilter,
    categoryTypeFilter
  ])
}
