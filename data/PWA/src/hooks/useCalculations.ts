import { useMemo } from 'react'
import { format } from 'date-fns'
import type { MoneySource, Transaction, Category, Budget } from '../data/db'
import { budgetMinorToMajor, convertCurrency, normalizeCurrency, transactionDelta } from '../utils/helpers'

interface UseCalculationsProps {
  sources: MoneySource[]
  transactions: Transaction[]
  categories: Category[]
  budgets: Budget[]
  exchangeRate: number
  displayCurrency: string
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
  displayCurrency,
  budgetMonthFilter,
  reportMonthFilter,
  reportTypeFilter,
  reportCurrencyFilter,
  categoryTypeFilter
}: UseCalculationsProps) {

  return useMemo(() => {
    // 1. Dashboard calculations
    const balancesBySource: Record<string, number> = {}
    const sourceBalancesByCurrency: Record<string, Record<string, number>> = {}
    let monthIncome = 0
    let monthExpense = 0
    const monthKey = format(new Date(), 'yyyy-MM')

    transactions.forEach((tx) => {
      const delta = transactionDelta(tx)
      const txCurrency = tx.currency || 'VND'
      let convertedDelta = delta
      if (tx.currency && tx.currency !== 'VND') {
        convertedDelta = delta * exchangeRate
      }
      balancesBySource[tx.sourceName] = (balancesBySource[tx.sourceName] ?? 0) + convertedDelta
      sourceBalancesByCurrency[tx.sourceName] = sourceBalancesByCurrency[tx.sourceName] ?? {}
      sourceBalancesByCurrency[tx.sourceName][txCurrency] = (sourceBalancesByCurrency[tx.sourceName][txCurrency] ?? 0) + delta
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
    const makeBudgetRows = (monthYear: string) => budgets
      .filter((budget) => budget.monthYear === monthYear)
      .map((budget) => {
        const category = categoriesById.get(budget.categoryId)
        const budgetCurrency = normalizeCurrency(budget.currencyCode)
        const limitAmount = budgetMinorToMajor(budget.limitAmountMinor, budgetCurrency)
        const spent = transactions.reduce((sum, tx) => {
          if (tx.type !== 'EXPENSE') return sum
          if (format(tx.timestamp, 'yyyy-MM') !== monthYear) return sum
          const categoryId = expenseCategoryIdByName.get(tx.category.trim().toLowerCase())
          if (categoryId !== budget.categoryId) return sum
          return sum + convertCurrency(tx.amount, tx.currency, budgetCurrency, exchangeRate)
        }, 0)
        const remaining = limitAmount - spent
        const percent = limitAmount <= 0
          ? 0
          : Math.min(100, Math.round((spent / limitAmount) * 100))

        return {
          budget,
          category,
          limitAmount,
          spent,
          remaining,
          percent,
          isOver: remaining < 0,
        }
      })
      .sort((a, b) => (
        convertCurrency(b.limitAmount, b.budget.currencyCode, displayCurrency, exchangeRate) -
        convertCurrency(a.limitAmount, a.budget.currencyCode, displayCurrency, exchangeRate)
      ))

    const monthlyBudgets = budgets
      .filter((budget) => budget.monthYear === budgetMonthFilter)
      .sort((a, b) => b.limitAmountMinor - a.limitAmountMinor)

    const budgetRows = makeBudgetRows(budgetMonthFilter)

    const totalBudgetLimit = budgetRows.reduce((sum, row) => (
      sum + convertCurrency(row.limitAmount, row.budget.currencyCode, displayCurrency, exchangeRate)
    ), 0)
    const totalBudgetSpent = budgetRows.reduce((sum, row) => (
      sum + convertCurrency(row.spent, row.budget.currencyCode, displayCurrency, exchangeRate)
    ), 0)
    const totalBudgetPercent =
      totalBudgetLimit <= 0 ? 0 : Math.min(100, Math.round((totalBudgetSpent / totalBudgetLimit) * 100))
    const overBudgetRows = budgetRows.filter((row) => row.isOver)
    const overBudgetTotal = overBudgetRows.reduce((sum, row) => (
      sum + convertCurrency(Math.abs(row.remaining), row.budget.currencyCode, displayCurrency, exchangeRate)
    ), 0)

    const reportDisplayCurrency = reportCurrencyFilter === 'ALL' ? displayCurrency : reportCurrencyFilter
    const reportBudgetRows = makeBudgetRows(reportMonthFilter)
    const reportBudgetLimit = reportBudgetRows.reduce((sum, row) => (
      sum + convertCurrency(row.limitAmount, row.budget.currencyCode, reportDisplayCurrency, exchangeRate)
    ), 0)
    const reportBudgetSpent = reportBudgetRows.reduce((sum, row) => (
      sum + convertCurrency(row.spent, row.budget.currencyCode, reportDisplayCurrency, exchangeRate)
    ), 0)
    const reportBudgetPercent =
      reportBudgetLimit <= 0 ? 0 : Math.min(100, Math.round((reportBudgetSpent / reportBudgetLimit) * 100))

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
          if (tx.type === 'EXPENSE') {
            prevPeriodExpenseTotal += convertCurrency(tx.amount, tx.currency, reportDisplayCurrency, exchangeRate)
          }
        }
      }
      
      if (txMonth !== reportMonthFilter) continue
      if (reportTypeFilter !== 'ALL' && tx.type !== reportTypeFilter) continue
      if (reportCurrencyFilter !== 'ALL' && tx.currency !== reportCurrencyFilter) continue
      const reportAmount = convertCurrency(tx.amount, tx.currency, reportDisplayCurrency, exchangeRate)

      if (tx.type === 'INCOME') {
        reportIncomeTotal += reportAmount
        reportBalancesByCurrency[tx.currency] = (reportBalancesByCurrency[tx.currency] ?? 0) + tx.amount
      }
      if (tx.type === 'EXPENSE') {
        reportExpenseTotal += reportAmount
        reportBalancesByCurrency[tx.currency] = (reportBalancesByCurrency[tx.currency] ?? 0) - tx.amount
        reportExpenseByCategory[tx.category] = (reportExpenseByCategory[tx.category] ?? 0) + reportAmount

        const day = format(tx.timestamp, 'dd/MM')
        reportDailyExpenses[day] = (reportDailyExpenses[day] ?? 0) + reportAmount
      }
    }

    return {
      balancesBySource,
      sourceBalancesByCurrency,
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
      reportBudgetRows,
      reportBudgetLimit,
      reportBudgetSpent,
      reportBudgetPercent,
      reportDisplayCurrency,
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
    displayCurrency,
    budgetMonthFilter,
    reportMonthFilter,
    reportTypeFilter,
    reportCurrencyFilter,
    categoryTypeFilter
  ])
}
