package com.shieldtv.launcher

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shieldtv.launcher.data.LauncherApp
import com.shieldtv.launcher.ui.LauncherScreen
import com.shieldtv.launcher.ui.LauncherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = viewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            LauncherTheme {
                LauncherScreen(
                    state = state,
                    onLaunch = ::launch,
                    onAppInfo = ::openAppInfo,
                    onOpenSettings = { startSafely(Intent(Settings.ACTION_SETTINGS)) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onMoveFavorite = viewModel::moveFavorite,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        // Catch installs/updates that happened while we were in the background.
        viewModel.refresh()
    }

    override fun onStop() {
        unregisterReceiver(packageReceiver)
        super.onStop()
    }

    private fun launch(app: LauncherApp) = startSafely(app.launchIntent())

    private fun openAppInfo(app: LauncherApp) = startSafely(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
    )

    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Unable to open", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Unable to open", Toast.LENGTH_SHORT).show()
        }
    }
}
