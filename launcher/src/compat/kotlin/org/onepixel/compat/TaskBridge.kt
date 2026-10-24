package org.onepixel.compat

import android.app.Activity
import android.app.ActivityOptions
import android.app.ActivityTaskManager
import android.app.IActivityClientController
import android.app.IActivityManager
import android.app.IActivityTaskManager
import android.os.RemoteException
import android.util.Log
import android.view.RemoteAnimationAdapter
import android.view.RemoteAnimationDefinition
import android.window.RemoteTransition
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

object TaskBridge {
    private const val TAG = "OnePixelShizuku"

    @JvmStatic
    fun closeSystemDialogs(
        service: IActivityManager,
        reason: String
    ) {
        try {
            if (isAuthorized()) {
                IActivityManager.Stub
                    .asInterface(ShizukuBinderWrapper(service.asBinder()))
                    .closeSystemDialogs(reason)
            }
        } catch (exception: RemoteException) {
            logUnavailable(exception = exception)
        } catch (exception: SecurityException) {
            logUnavailable(exception = exception)
        } catch (exception: IllegalStateException) {
            logUnavailable(exception = exception)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun makeLaunchOptions(
        adapter: RemoteAnimationAdapter,
        transition: RemoteTransition?
    ): ActivityOptions {
        try {
            if (isAuthorized()) {
                taskManager(service = ActivityTaskManager.getService())
                    .registerRemoteAnimationForNextActivityStart(
                        "com.google.android.apps.nexuslauncher",
                        adapter,
                        null
                    )

                Log.i(TAG, "Next app launch animation registered through Shizuku")
            }
        } catch (exception: RemoteException) {
            logUnavailable(exception = exception)
        } catch (exception: SecurityException) {
            logUnavailable(exception = exception)
        } catch (exception: IllegalStateException) {
            logUnavailable(exception = exception)
        }

        return ActivityOptions.makeBasic()
    }

    @JvmStatic
    fun registerRemoteAnimations(
        activity: Activity,
        definition: RemoteAnimationDefinition
    ) {
        try {
            if (isAuthorized()) {
                activityController().registerRemoteAnimations(activity.activityToken, definition)
                Log.i(TAG, "registerRemoteAnimations succeeded through Shizuku")
            }
        } catch (exception: RemoteException) {
            logUnavailable(exception = exception)
        } catch (exception: SecurityException) {
            logUnavailable(exception = exception)
        } catch (exception: IllegalStateException) {
            logUnavailable(exception = exception)
        }
    }

    @JvmStatic
    fun unregisterRemoteAnimations(activity: Activity) {
        try {
            if (isAuthorized()) {
                activityController().unregisterRemoteAnimations(activity.activityToken)
            }
        } catch (exception: RemoteException) {
            logUnavailable(exception = exception)
        } catch (exception: SecurityException) {
            logUnavailable(exception = exception)
        } catch (exception: IllegalStateException) {
            logUnavailable(exception = exception)
        }
    }

    @JvmStatic
    fun getRootTaskInfo(
        service: IActivityTaskManager,
        windowingMode: Int,
        activityType: Int
    ): ActivityTaskManager.RootTaskInfo? {
        try {
            if (isAuthorized()) {
                return taskManager(service = service).getRootTaskInfo(windowingMode, activityType)
            }
        } catch (exception: RemoteException) {
            logUnavailable(exception = exception)
        } catch (exception: SecurityException) {
            logUnavailable(exception = exception)
        } catch (exception: IllegalStateException) {
            logUnavailable(exception = exception)
        }

        return null
    }

    private fun isAuthorized(): Boolean {
        return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == 0
    }

    private fun taskManager(service: IActivityTaskManager): IActivityTaskManager {
        return IActivityTaskManager.Stub.asInterface(ShizukuBinderWrapper(service.asBinder()))
    }

    private fun activityController(): IActivityClientController {
        val binder = ActivityTaskManager.getService().activityClientController.asBinder()

        return IActivityClientController.Stub.asInterface(ShizukuBinderWrapper(binder))
    }

    private fun logUnavailable(exception: Exception) {
        Log.w(TAG, "Optional privileged operation unavailable", exception)
    }
}
