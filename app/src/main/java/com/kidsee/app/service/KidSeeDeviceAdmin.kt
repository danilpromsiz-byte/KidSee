package com.kidsee.app.service

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Handles device administration policies including instant screen lock.
 */
class KidSeeDeviceAdmin : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Timber.d("KidSee Device Admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Timber.d("KidSee Device Admin disabled")
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, KidSeeDeviceAdmin::class.java)
        }

        fun isAdminActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return dpm.isAdminActive(getComponentName(context))
        }

        fun lockNow(context: Context) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (isAdminActive(context)) {
                dpm.lockNow()
                Timber.d("Screen locked via DevicePolicyManager.lockNow()")
            } else {
                Timber.w("Cannot lock screen: Device Admin not active")
            }
        }
    }
}
