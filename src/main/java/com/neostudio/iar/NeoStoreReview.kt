package com.neostudio.iar

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import java.util.WeakHashMap

object NeoStoreReview {
    const val ACTION_REQUEST_REVIEW = "com.neostudio.neostore.action.REQUEST_REVIEW"
    const val EXTRA_APP_ID = "in_app_review_app_id"
    const val EXTRA_RESULT_ERROR = "in_app_review_error"
    const val EXTRA_RESULT_RATING = "in_app_review_rating"
    const val EXTRA_RESULT_COMMENT = "in_app_review_comment"

    const val RESULT_ALREADY_REVIEWED = 1
    const val RESULT_NOT_AUTHENTICATED = 2
    const val RESULT_FAILED = 3

    private const val LAUNCHER_KEY = "com.neostudio.iar.REQUEST_REVIEW"
    private val lock = Any()
    private val launchers = WeakHashMap<ComponentActivity, ReviewLauncher>()

    private var configuredAppId = 0
    private var application: Application? = null

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (activity is ComponentActivity) {
                registerLauncher(activity)
            }
        }

        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) {
            if (activity is ComponentActivity) {
                synchronized(lock) {
                    launchers.remove(activity)
                }
            }
        }
    }

    @JvmStatic
    fun initialize(context: Context, appId: String) {
        val parsedAppId = appId.trim().toIntOrNull()
            ?: throw IllegalArgumentException("appId must be a positive numeric string")
        require(parsedAppId > 0) { "appId must be a positive number" }

        val app = context.applicationContext as? Application
            ?: throw IllegalArgumentException("context must provide an Application context")

        synchronized(lock) {
            if (application !== app) {
                application?.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
                application = app
                app.registerActivityLifecycleCallbacks(lifecycleCallbacks)
            }
            configuredAppId = parsedAppId
        }
    }

    @JvmStatic
    fun requestReview(activity: Activity, callback: ReviewCallback) {
        if (activity !is ComponentActivity) {
            callback.onResult(
                ReviewResult.Error(
                    "NeoStoreReview требует Activity на базе ComponentActivity, например AppCompatActivity."
                )
            )
            return
        }

        val (appId, reviewLauncher) = synchronized(lock) {
            configuredAppId to launchers[activity]
        }

        if (appId <= 0) {
            callback.onResult(
                ReviewResult.Error(
                    "Вызовите NeoStoreReview.initialize(context, appId) до запроса отзыва."
                )
            )
            return
        }
        if (reviewLauncher == null) {
            callback.onResult(
                ReviewResult.Error(
                    "SDK не подготовил Activity для запуска. " +
                        "Инициализируйте его в Application.onCreate()."
                )
            )
            return
        }

        val intent = Intent(ACTION_REQUEST_REVIEW)
            .setPackage("com.neostudio.neostore")
            .putExtra(EXTRA_APP_ID, appId)
        reviewLauncher.launch(intent, callback)
    }

    @JvmStatic
    fun showReview(activity: Activity, callback: ReviewCallback) {
        requestReview(activity, callback)
    }

    @JvmStatic
    fun isNeoStoreInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.neostudio.neostore", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun registerLauncher(activity: ComponentActivity) {
        synchronized(lock) {
            if (launchers.containsKey(activity)) {
                return
            }
            launchers[activity] = ReviewLauncher(activity)
        }
    }

    private fun parseResult(result: ActivityResult): ReviewResult {
        val data = result.data
        return when (result.resultCode) {
            Activity.RESULT_OK -> {
                if (data == null) {
                    ReviewResult.Error("NeoStore вернул успешный результат без данных.")
                } else {
                    ReviewResult.Submitted(
                        data.getIntExtra(EXTRA_RESULT_RATING, 0),
                        data.getStringExtra(EXTRA_RESULT_COMMENT)
                    )
                }
            }

            Activity.RESULT_CANCELED -> ReviewResult.Cancelled()
            RESULT_ALREADY_REVIEWED -> ReviewResult.AlreadyReviewed()
            RESULT_NOT_AUTHENTICATED -> ReviewResult.NotAuthenticated()
            RESULT_FAILED -> {
                val message = data?.getStringExtra(EXTRA_RESULT_ERROR)
                    ?.takeIf { it.isNotBlank() }
                    ?: "NeoStore не удалось отправить отзыв. Попробуйте позже."
                ReviewResult.Error(message)
            }

            else -> ReviewResult.Error("Неизвестный код результата от NeoStore.")
        }
    }

    private class ReviewLauncher(activity: ComponentActivity) {
        private var pendingCallback: ReviewCallback? = null
        private val launcher: ActivityResultLauncher<Intent> =
            activity.activityResultRegistry.register(
                LAUNCHER_KEY,
                activity,
                ActivityResultContracts.StartActivityForResult(),
                ::onActivityResult
            )

        fun launch(intent: Intent, callback: ReviewCallback) {
            if (pendingCallback != null) {
                callback.onResult(ReviewResult.Error("Для этой Activity уже выполняется запрос отзыва."))
                return
            }

            pendingCallback = callback
            try {
                launcher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                pendingCallback = null
                callback.onResult(
                    ReviewResult.Error("Не удалось открыть NeoStore. Убедитесь, что приложение установлено.")
                )
            }
        }

        private fun onActivityResult(result: ActivityResult) {
            val callback = pendingCallback
            pendingCallback = null
            callback?.onResult(parseResult(result))
        }
    }
}
