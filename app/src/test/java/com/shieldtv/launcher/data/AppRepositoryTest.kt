package com.shieldtv.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AppRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val packageInfo = PackageInfo().apply {
        packageName = PACKAGE
        lastUpdateTime = 1_000L
        applicationInfo = ApplicationInfo().apply { packageName = PACKAGE }
    }

    @Before
    fun installTvApp() {
        val pm = shadowOf(context.packageManager)
        pm.installPackage(packageInfo)
        val component = ComponentName(PACKAGE, "$PACKAGE.Main")
        pm.addActivityIfNotPresent(component)
        pm.addIntentFilterForActivity(
            component,
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) },
        )
    }

    @Test
    fun reloadReusesUnchangedApps() = runBlocking {
        val repository = AppRepository(context)
        val first = repository.loadApps().single { it.packageName == PACKAGE }
        val second = repository.loadApps().single { it.packageName == PACKAGE }
        // Same instance: no artwork re-decode and Compose can skip the tile.
        assertSame(first, second)
    }

    @Test
    fun updatedAppIsReloaded() = runBlocking {
        val repository = AppRepository(context)
        val before = repository.loadApps().single { it.packageName == PACKAGE }

        packageInfo.lastUpdateTime = 2_000L
        shadowOf(context.packageManager).installPackage(packageInfo)
        val after = repository.loadApps().single { it.packageName == PACKAGE }

        assertNotSame(before, after)
        assertEquals(before.key, after.key)
    }

    private companion object {
        const val PACKAGE = "com.example.tvapp"
    }
}
