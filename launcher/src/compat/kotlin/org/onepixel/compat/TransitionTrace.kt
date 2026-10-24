package org.onepixel.compat

import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.SurfaceControl
import android.window.IRemoteTransition
import android.window.IRemoteTransitionFinishedCallback
import android.window.RemoteTransition
import android.window.TransitionInfo
import android.window.WindowAnimationState
import android.window.WindowContainerTransaction

internal object TransitionTrace {
    private const val TAG = "OnePixelTransition"

    fun wrap(transition: RemoteTransition): RemoteTransition {
        val delegate = transition.remoteTransition

        val traced = object : IRemoteTransition.Stub() {
            override fun startAnimation(
                token: IBinder,
                info: TransitionInfo,
                transaction: SurfaceControl.Transaction,
                finishCallback: IRemoteTransitionFinishedCallback
            ) {
                val started = SystemClock.uptimeMillis()
                Log.i(TAG, "Pixel transition started: type=" + info.type + ", changes=" + info.changes.size)

                val completion = object : IRemoteTransitionFinishedCallback.Stub() {
                    override fun onTransitionFinished(
                        containerTransaction: WindowContainerTransaction?,
                        surfaceTransaction: SurfaceControl.Transaction?
                    ) {
                        Log.i(TAG, "Pixel transition finished in " + (SystemClock.uptimeMillis() - started) + " ms")
                        finishCallback.onTransitionFinished(containerTransaction, surfaceTransaction)
                    }
                }

                delegate.startAnimation(token, info, transaction, completion)
            }

            override fun mergeAnimation(
                token: IBinder,
                info: TransitionInfo,
                transaction: SurfaceControl.Transaction,
                mergeTarget: IBinder,
                finishCallback: IRemoteTransitionFinishedCallback
            ) {
                delegate.mergeAnimation(token, info, transaction, mergeTarget, finishCallback)
            }

            override fun takeOverAnimation(
                token: IBinder,
                info: TransitionInfo,
                transaction: SurfaceControl.Transaction,
                finishCallback: IRemoteTransitionFinishedCallback,
                states: Array<WindowAnimationState>?
            ) {
                delegate.takeOverAnimation(token, info, transaction, finishCallback, states)
            }

            override fun onTransitionConsumed(
                token: IBinder,
                aborted: Boolean
            ) {
                delegate.onTransitionConsumed(token, aborted)
            }
        }

        return RemoteTransition(traced, transition.appThread, "OnePixel:" + transition.debugName)
    }
}
