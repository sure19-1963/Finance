package com.suresh.finance

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suresh.finance.notify.Notifications
import com.suresh.finance.ui.AppNav
import com.suresh.finance.ui.LocalCurrency
import com.suresh.finance.ui.theme.FinanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as FinanceApp).repo
        setContent {
            val currency by repo.settings.currency.collectAsStateWithLifecycle()
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canNotify(this@MainActivity)) {
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            FinanceTheme {
                CompositionLocalProvider(LocalCurrency provides currency) {
                    AppNav()
                }
            }
        }
    }
}
