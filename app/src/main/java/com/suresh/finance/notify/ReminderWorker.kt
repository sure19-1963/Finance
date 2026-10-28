package com.suresh.finance.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.suresh.finance.FinanceApp
import com.suresh.finance.data.Dues
import com.suresh.finance.data.FinanceMath
import com.suresh.finance.data.TxType
import com.suresh.finance.ui.formatMoney
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.concurrent.TimeUnit

/** Runs once a day (and after expenses are saved) to send due-date and budget alerts. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = (applicationContext as FinanceApp).repo
        val settings = repo.settings
        if (!settings.notificationsEnabled.value) return Result.success()

        val currency = settings.currency.value
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        val monthTxns = repo.txns.inRangeOnce(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())

        // Due dates for bills, debt payments and rent.
        val daysAhead = settings.reminderDaysAhead.value
        val dues = Dues.forMonth(
            month = month,
            bills = repo.bills.enabledOnce(),
            debts = repo.debts.openOnce(),
            leases = repo.leases.activeOnce(),
            properties = emptyList(),
            monthTxns = monthTxns,
        )
        dues.filter { !it.done }.forEach { due ->
            val days = due.daysUntil(today)
            val whenText = when {
                days < 0 -> "was due ${-days} day(s) ago"
                days == 0L -> "is due today"
                days <= daysAhead -> "is due in $days day(s)"
                else -> return@forEach
            }
            // One alert per item per day, so overdue items repeat daily until marked done.
            if (settings.markNotifiedOnce("${due.key}_$today")) {
                Notifications.show(
                    applicationContext,
                    Notifications.CHANNEL_DUES,
                    due.key,
                    due.title,
                    "${formatMoney(due.amount, currency)} $whenText (${due.subtitle}).",
                )
            }
        }

        // Budget alerts at 80% and 100% of the monthly limit.
        val categories = repo.categories.allOnce().associateBy { it.id }
        val spentByCategory = monthTxns.filter { it.type == TxType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val ym = FinanceMath.ym(month)
        repo.budgets.allOnce().forEach { budget ->
            if (budget.monthlyLimit <= 0) return@forEach
            val spent = spentByCategory[budget.categoryId] ?: 0.0
            val name = categories[budget.categoryId]?.name ?: "Budget"
            val ratio = spent / budget.monthlyLimit
            val level = when {
                ratio >= 1.0 -> 100
                ratio >= 0.8 -> 80
                else -> return@forEach
            }
            val key = "budget_${budget.categoryId}_${ym}_$level"
            if (settings.markNotifiedOnce(key)) {
                val text = if (level == 100) {
                    "You've spent ${formatMoney(spent, currency)} of ${formatMoney(budget.monthlyLimit, currency)} this month, which is over budget."
                } else {
                    "You've used ${(ratio * 100).toInt()}% of your ${formatMoney(budget.monthlyLimit, currency)} budget."
                }
                Notifications.show(applicationContext, Notifications.CHANNEL_BUDGET, key, "$name budget", text)
            }
        }
        return Result.success()
    }

    companion object {
        private const val DAILY = "daily_reminders"
        private const val NOW = "check_now"

        /** Daily check, first run around 9 AM. */
        fun scheduleDaily(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(9, 0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delay = Duration.between(now, next).toMinutes()
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(DAILY, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Runs a check straight away, for example after saving an expense. */
        fun checkNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<ReminderWorker>().build(),
            )
        }
    }
}
