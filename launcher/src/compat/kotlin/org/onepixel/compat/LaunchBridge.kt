package org.onepixel.compat

import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.IActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.view.RemoteAnimationAdapter
import android.window.RemoteTransition
import java.util.concurrent.atomic.AtomicInteger
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

object LaunchBridge {
    private const val TAG = "OnePixelTransition"
    private const val REMOTE_TRANSITION_KEY = "android:activity.remoteTransition"
    private const val DEFERRED_TRANSITION_KEY = "org.onepixel.remoteTransition"

    private val requestCodes = AtomicInteger()

    @JvmStatic
    fun makeLaunchOptions(
        adapter: RemoteAnimationAdapter,
        transition: RemoteTransition?
    ): ActivityOptions {
        val options = TaskBridge.makeLaunchOptions(adapter = adapter, transition = transition)

        if (transition != null) {
            options.setRemoteTransition(TransitionTrace.wrap(transition = transition))
        }

        return options
    }

    @JvmStatic
    fun toBundle(options: ActivityOptions): Bundle {
        val bundle = options.toBundle()
        val transition = options.remoteTransition

        if (transition != null) {
            bundle.remove(REMOTE_TRANSITION_KEY)
            bundle.putParcelable(DEFERRED_TRANSITION_KEY, transition)
        }

        return bundle
    }

    @JvmStatic
    fun startActivity(
        context: Context,
        intent: Intent,
        options: Bundle?
    ) {
        val transition = options?.getParcelable(DEFERRED_TRANSITION_KEY, RemoteTransition::class.java)
        val cleanOptions = if (options != null) {
            Bundle(options)
        } else {
            null
        }

        cleanOptions?.remove(DEFERRED_TRANSITION_KEY)

        if (transition != null && cleanOptions != null) {
            if (sendWithTransition(
                    context = context,
                    intent = intent,
                    options = cleanOptions,
                    transition = transition
                )
            ) {
                return
            }
        }

        context.startActivity(intent, cleanOptions)
    }

    private fun sendWithTransition(
        context: Context,
        intent: Intent,
        options: Bundle,
        transition: RemoteTransition
    ): Boolean {
        var pending: PendingIntent? = null

        try {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != 0) {
                return false
            }

            val creatorOptions = ActivityOptions.makeBasic()
                .setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
                )
                .toBundle()

            pending = PendingIntent.getActivity(
                context,
                requestCodes.incrementAndGet(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT,
                creatorOptions
            )

            val launchOptions = ActivityOptions.fromBundle(options)
                .setRemoteTransition(transition)
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS)
                .toBundle()

            val service = IActivityManager.Stub.asInterface(
                ShizukuBinderWrapper(ActivityManager.getService().asBinder())
            )

            val result = service.sendIntentSender(
                null,
                pending.target,
                null,
                0,
                null,
                null,
                null,
                null,
                launchOptions
            )

            if (ActivityManager.isStartResultSuccessful(result)) {
                Log.i(TAG, "App launch submitted with Pixel remote transition through Shizuku")
                return true
            }

            Log.w(TAG, "Remote app launch rejected: " + result)
        } catch (exception: RemoteException) {
            logUnavailable(exception = exception)
        } catch (exception: SecurityException) {
            logUnavailable(exception = exception)
        } catch (exception: IllegalStateException) {
            logUnavailable(exception = exception)
        } finally {
            pending?.cancel()
        }

        return false
    }

    private fun logUnavailable(exception: Exception) {
        Log.w(TAG, "Using ordinary app launch; remote transition unavailable", exception)
    }
}
