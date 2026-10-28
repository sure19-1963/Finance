package com.suresh.finance

import android.app.Application
import com.suresh.finance.data.AppDatabase
import com.suresh.finance.data.FinanceRepository
import com.suresh.finance.data.SettingsStore
import com.suresh.finance.notify.Notifications
import com.suresh.finance.notify.ReminderWorker

class FinanceApp : Application() {
    lateinit var repo: FinanceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repo = FinanceRepository(AppDatabase.get(this), SettingsStore(this))
        Notifications.createChannels(this)
        ReminderWorker.scheduleDaily(this)
    }
}
