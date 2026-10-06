package it.sanges.ilturno.util

import android.os.SystemClock
import android.util.Log
import android.content.Context
import android.content.pm.ApplicationInfo
import kotlinx.coroutines.CancellationException

/** Local Logcat diagnostics for physical debug trials; no names, file contents or telemetry. */
object DebugDiagnostics {
    const val TAG = "ilTurno"
    @Volatile private var enabled = false

    fun configure(context: Context) {
        enabled = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    fun event(message: String) {
        if (enabled) Log.d(TAG, message)
    }

    fun failure(operation: String, error: Exception) {
        if (enabled) {
            // Stack locations and exception type help diagnosis without logging exception messages,
            // which can contain database values or other personal input.
            Log.e(TAG, "$operation failed (${error.javaClass.simpleName})\n" +
                error.stackTrace.joinToString("\n") { "at $it" })
        }
    }

    suspend fun <T> trace(operation: String, action: suspend () -> T): T {
        if (!enabled) return action()
        val started = SystemClock.elapsedRealtime()
        event("$operation started")
        try {
            val result = action()
            event("$operation completed elapsed_ms=${SystemClock.elapsedRealtime() - started}")
            return result
        } catch (cancelled: CancellationException) {
            event("$operation cancelled")
            throw cancelled
        } catch (error: Exception) {
            failure(operation, error)
            throw error
        }
    }
}
