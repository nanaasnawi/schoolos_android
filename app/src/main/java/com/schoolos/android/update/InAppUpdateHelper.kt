package com.schoolos.android.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import timber.log.Timber

/**
 * Helper to manage Google Play In-App Updates.
 * Automatically checks Google Play Store for new version availability,
 * prompts the user with official Google Play update UI, and enforces update completion.
 */
class InAppUpdateHelper(private val activity: Activity) {

    companion object {
        const val UPDATE_REQUEST_CODE = 9901

        fun openPlayStore(context: Context) {
            val packageName = context.packageName
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    /**
     * Checks if an update is available on Google Play Store.
     * If available, triggers the IMMEDIATE (force update) flow so the user is required to update.
     */
    fun checkForAppUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
            val availability = appUpdateInfo.updateAvailability()
            Timber.i("Google Play UpdateAvailability: %d, availableVersionCode: %d", availability, appUpdateInfo.availableVersionCode())

            if (availability == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activity,
                        AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start Google Play Immediate Update flow")
                }
            } else if (availability == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activity,
                        AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start Google Play Flexible Update flow")
                }
            }
        }.addOnFailureListener { e ->
            Timber.d("In-app update check failed or not running via Google Play: %s", e.message)
        }
    }

    /**
     * Call on Activity onResume() to resume in-progress immediate updates
     * preventing users from circumventing the update by leaving and returning.
     */
    fun onResume() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activity,
                        AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to resume developer triggered in-app update")
                }
            }
        }
    }
}
