package awaisomereport

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import awais.instagrabber.BuildConfig
import awais.instagrabber.R
import awais.instagrabber.utils.Constants
import awais.instagrabber.utils.extensions.TAG
import java.io.*
import java.time.LocalDateTime

object CrashReporterHelper {
    private val shortBorder = "=".repeat(14)
    private val longBorder = "=".repeat(21)
    private const val prefix = "stack-"
    private const val suffix = ".stacktrace"

    fun startErrorReporterActivity(
        application: Application,
        exception: Throwable
    ) {
        try {
            application.openFileOutput(
                "$prefix${System.currentTimeMillis()}$suffix",
                Context.MODE_PRIVATE
            ).use { it.write(getReportContent(exception).toByteArray()) }
        } catch (ex: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error saving crash report", ex)
        }
        application.startActivity(Intent(application, ErrorReporterActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun getReportContent(exception: Throwable): String {
        val reportContent = """
            IMPORTANT: If sending by email, your email address and the entire content will be made public at https://github.com/austinhuang0131/barinsta/issues.
    
            When possible, please describe the steps leading to this crash. Thank you for your cooperation.
    
            Error report collected on: ${LocalDateTime.now()}
    
            Information:
            $shortBorder
            VERSION         : ${BuildConfig.VERSION_NAME}
            VERSION_CODE    : ${BuildConfig.VERSION_CODE}
            PHONE-MODEL     : ${Build.MODEL}
            ANDROID_VERS    : ${Build.VERSION.RELEASE}
            ANDROID_REL     : ${Build.VERSION.SDK_INT}
            BRAND           : ${Build.BRAND}
            MANUFACTURER    : ${Build.MANUFACTURER}
            BOARD           : ${Build.BOARD}
            DEVICE          : ${Build.DEVICE}
            PRODUCT         : ${Build.PRODUCT}
            HOST            : ${Build.HOST}
            TAGS            : ${Build.TAGS}
    
            Stack:
            $shortBorder
        """.trimIndent()

        return "$reportContent${getStackTrace(exception)}\n\n*** End of current Report ***"
            .replace("\n", "\r\n")
    }

    private fun getStackTrace(exception: Throwable): String {
        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        exception.printStackTrace(printWriter)

        val reportBuilder = StringBuilder(writer.toString())
        var cause = exception.cause
        if (cause != null) reportBuilder.append("\nCause:\n$shortBorder\n")

        while (cause != null) {
            cause.printStackTrace(printWriter)
            reportBuilder.append(writer.toString())
            cause = cause.cause
        }

        printWriter.close()
        return reportBuilder.toString()
    }

    @JvmStatic
    fun startCrashEmailIntent(context: Context) {
        try {
            val filePath = context.filesDir.absolutePath
            val errorFileList = File(filePath).list { _, name -> name.endsWith(suffix) } ?: return
            
            val errorStringBuilder = StringBuilder("\n\n")
            val maxSendMail = 5
            
            errorFileList.take(maxSendMail).forEach { fileName ->
                val file = File("$filePath/$fileName")
                errorStringBuilder.append("New Trace collected:\n$longBorder\n")
                BufferedReader(FileReader(file)).use { input ->
                    var line: String?
                    while (input.readLine().also { line = it } != null) {
                        errorStringBuilder.append(line).append("\n")
                    }
                }
                file.delete()
            }

            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.CRASH_REPORT_EMAIL))
                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.crash_report_subject))
                        putExtra(Intent.EXTRA_TEXT, errorStringBuilder.toString().replace("\n", "\r\n"))
                    },
                    context.getString(R.string.crash_report_title)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting crash email intent", e)
        }
    }

    @JvmStatic
    fun deleteAllStacktraceFiles(context: Context) {
        val filePath = context.filesDir.absolutePath
        File(filePath).listFiles { _, name -> name.endsWith(suffix) }?.forEach { it.delete() }
    }
}
