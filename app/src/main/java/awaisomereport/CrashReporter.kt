package awaisomereport

import android.app.Application
import android.util.Log

class CrashReporter private constructor(application: Application) : Thread.UncaughtExceptionHandler {

    private val crashHandler: CrashHandler? = CrashHandler(application) // Initialize CrashHandler
    private var isStarted = false // Track if the crash reporter has started
    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null // Store the default handler

    // Start the crash reporter
    fun start() {
        if (isStarted) return // Prevent multiple starts
        isStarted = true
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler() // Get the default handler
        Thread.setDefaultUncaughtExceptionHandler(this) // Set this class as the default handler
    }

    // Handle uncaught exceptions
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        // Log the uncaught exception
        Log.e("CrashReporter", "Uncaught exception in thread ${thread.name}: ${exception.message}", exception)

        // Handle the exception using CrashHandler
        crashHandler?.uncaughtException(thread, exception, defaultExceptionHandler)

        // Optionally, restart the application or close it
        // android.os.Process.killProcess(android.os.Process.myPid())
        // System.exit(1)
    }

    companion object {
        @Volatile
        private var INSTANCE: CrashReporter? = null // Ensure thread-safe singleton

        // Singleton access method
        fun getInstance(application: Application): CrashReporter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CrashReporter(application).also { INSTANCE = it } // Create instance if not already created
            }
        }
    }
}
