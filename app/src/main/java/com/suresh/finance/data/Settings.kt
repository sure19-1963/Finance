package com.suresh.finance.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small app preferences kept in SharedPreferences. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _currency = MutableStateFlow(prefs.getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY)
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _daysAhead = MutableStateFlow(prefs.getInt(KEY_DAYS_AHEAD, 3))
    val reminderDaysAhead: StateFlow<Int> = _daysAhead.asStateFlow()

    private val _notifications = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY, true))
    val notificationsEnabled: StateFlow<Boolean> = _notifications.asStateFlow()

    fun setCurrency(code: String) {
        prefs.edit().putString(KEY_CURRENCY, code).apply()
        _currency.value = code
    }

    fun setReminderDaysAhead(days: Int) {
        prefs.edit().putInt(KEY_DAYS_AHEAD, days).apply()
        _daysAhead.value = days
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY, enabled).apply()
        _notifications.value = enabled
    }

    /** Returns true the first time [key] is seen, so each alert is shown only once. */
    fun markNotifiedOnce(key: String): Boolean {
        val seen = prefs.getStringSet(KEY_NOTIFIED, emptySet()) ?: emptySet()
        if (key in seen) return false
        // Keep the set from growing forever: alerts are keyed by month, so old ones can go.
        val trimmed = if (seen.size > 500) seen.toList().takeLast(300).toSet() else seen
        prefs.edit().putStringSet(KEY_NOTIFIED, trimmed + key).apply()
        return true
    }

    companion object {
        const val DEFAULT_CURRENCY = "AED"
        val CURRENCIES = listOf("AED", "INR", "USD", "EUR", "GBP", "SAR", "QAR", "OMR", "KWD", "BHD")
        private const val KEY_CURRENCY = "currency"
        private const val KEY_DAYS_AHEAD = "days_ahead"
        private const val KEY_NOTIFY = "notify"
        private const val KEY_NOTIFIED = "notified"
    }
}
