package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.utils.UserPreferences

/**
 * Ensures FLAG_SECURE is enforced consistently on both the host Activity window
 * and all separate Compose Dialog / ModalBottomSheet windows whenever Incognito Mode
 * or PIN lock is enabled.
 */
object SecureUiHelper {

    fun shouldEnforceSecureFlag(context: Context): Boolean {
        val prefs = UserPreferences(context)
        return prefs.isIncognitoMode() || prefs.hasPin()
    }

    fun securePolicy(context: Context): SecureFlagPolicy {
        return if (shouldEnforceSecureFlag(context)) {
            SecureFlagPolicy.SecureOn
        } else {
            SecureFlagPolicy.Inherit
        }
    }

    fun dialogProperties(
        context: Context,
        dismissOnBackPress: Boolean = true,
        dismissOnClickOutside: Boolean = true,
        usePlatformDefaultWidth: Boolean = true
    ): DialogProperties {
        return DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            securePolicy = securePolicy(context),
            usePlatformDefaultWidth = usePlatformDefaultWidth
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun bottomSheetProperties(context: Context): ModalBottomSheetProperties {
        return ModalBottomSheetProperties(
            securePolicy = securePolicy(context),
            shouldDismissOnBackPress = true
        )
    }

    fun syncActivitySecureFlag(context: Context) {
        val activity = context.findActivity() ?: return
        if (shouldEnforceSecureFlag(context)) {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
