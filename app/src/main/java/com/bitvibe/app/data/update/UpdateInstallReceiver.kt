package com.bitvibe.app.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.core.content.IntentCompat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Receives the [PackageInstaller] session result for a self-update. */
class UpdateInstallReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UpdateEntryPoint {
        fun updateManager(): UpdateManager
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return
        val manager = EntryPointAccessors
            .fromApplication(context.applicationContext, UpdateEntryPoint::class.java)
            .updateManager()
        val update = AvailableUpdate(
            versionName = intent.getStringExtra(EXTRA_VERSION_NAME).orEmpty(),
            build = intent.getIntExtra(EXTRA_BUILD, 0),
            apkUrl = intent.getStringExtra(EXTRA_APK_URL).orEmpty(),
            sizeBytes = intent.getLongExtra(EXTRA_SIZE, 0L),
            notes = ""
        )

        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // First self-update, or Android 11 and older: Android asks the user to confirm.
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                if (confirm == null || !manager.inForeground) {
                    // Can't show the dialog from the background; offer it on the next app launch.
                    manager.onInstallAborted(update)
                } else {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirm)
                    } catch (e: Exception) {
                        Log.w(TAG, "Couldn't show install confirmation", e)
                        manager.onInstallAborted(update)
                    }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit // the new version replaces this process
            PackageInstaller.STATUS_FAILURE_ABORTED -> manager.onInstallAborted(update)
            else -> manager.onInstallFailed(
                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "error $status"
            )
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.bitvibe.app.UPDATE_INSTALL_STATUS"
        const val EXTRA_VERSION_NAME = "versionName"
        const val EXTRA_BUILD = "build"
        const val EXTRA_APK_URL = "apkUrl"
        const val EXTRA_SIZE = "size"
        private const val TAG = "UpdateInstallReceiver"
    }
}
