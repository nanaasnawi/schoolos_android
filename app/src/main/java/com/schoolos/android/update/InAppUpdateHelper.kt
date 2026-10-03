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
 * Fail-safe helper to manage Google Play In-App Updates.
 * Designed to NEVER crash the application under any circumstances
 * (e.g. sideloaded builds, missing Play Services, ProGuard minification).
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
            } catch (_: Throwable) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Throwable) {}
            }
        }
    }

    private var appUpdateManager: AppUpdateManager? = null

    init {
        try {
            appUpdateManager = AppUpdateManagerFactory.create(activity)
            Timber.d("AppUpdateManager initialized successfully")
        } catch (t: Throwable) {
            Timber.w(t, "AppUpdateManagerFactory could not be initialized: %s", t.message)
            appUpdateManager = null
        }
    }

    /**
     * Checks if an update is available on Google Play Store.
     * Triggers the update flow safely.
     */
    fun checkForAppUpdate() {
        val manager = appUpdateManager ?: return
        try {
            manager.appUpdateInfo
                .addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
                    try {
                        val availability = appUpdateInfo.updateAvailability()
                        Timber.i("Google Play UpdateAvailability: %d, availableVersionCode: %d", availability, appUpdateInfo.availableVersionCode())

                        if (availability == UpdateAvailability.UPDATE_AVAILABLE
                            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                        ) {
                            manager.startUpdateFlowForResult(
                                appUpdateInfo,
                                activity,
                                AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
                                UPDATE_REQUEST_CODE
                            )
                        } else if (availability == UpdateAvailability.UPDATE_AVAILABLE
                            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                        ) {
                            manager.startUpdateFlowForResult(
                                appUpdateInfo,
                                activity,
                                AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                                UPDATE_REQUEST_CODE
                            )
                        }
                    } catch (t: Throwable) {
                        Timber.w(t, "Failed during in-app update trigger: %s", t.message)
                    }
                }
                .addOnFailureListener { e ->
                    Timber.d("In-app update check failed or not running via Google Play: %s", e.message)
                }
        } catch (t: Throwable) {
            Timber.w(t, "checkForAppUpdate failed safely: %s", t.message)
        }
    }

    /**
     * Resumes an in-progress immediate update if present.
     */
    fun onResume() {
        val manager = appUpdateManager ?: return
        try {
            manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                try {
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        manager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activity,
                            AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
                            UPDATE_REQUEST_CODE
                        )
                    }
                } catch (t: Throwable) {
                    Timber.w(t, "Failed to resume developer triggered in-app update: %s", t.message)
                }
            }
        } catch (t: Throwable) {
            Timber.w(t, "onResume update check failed safely: %s", t.message)
        }
    }
}
