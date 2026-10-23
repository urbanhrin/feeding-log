package com.hoon.feedinglog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat

class MainActivity : ComponentActivity() {

    private var notifOk by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dark = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = dark, navigationBarStyle = dark)

        setContent {
            FeedTheme {
                val permLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { refreshNotif() }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                var settingsOpen by rememberSaveable { mutableStateOf(false) }
                Box(Modifier.fillMaxSize().background(C.bg)) {
                    if (settingsOpen) {
                        BackHandler { settingsOpen = false }
                        SettingsScreen(notifOk, onBack = { settingsOpen = false }, onFixNotif = { openNotifSettings() })
                    } else {
                        LogScreen(onSettings = { settingsOpen = true })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshNotif()
    }

    // 앱을 열 때마다 알림이 떠 있는지 확인하고 다시 띄운다
    private fun refreshNotif() {
        notifOk = NotificationManagerCompat.from(this).areNotificationsEnabled()
        Notif.post(this)
    }

    private fun openNotifSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        )
    }
}
