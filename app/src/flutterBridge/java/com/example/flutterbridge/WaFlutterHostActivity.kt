package com.example.flutterbridge

import android.os.SystemClock
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthState
import com.example.data.auth.LicenseManager
import com.example.data.auth.LicenseStatus
import com.example.data.auth.SessionManager
import com.example.data.bridge.AndroidLeadUiDataSource
import com.example.data.bridge.NativeUiCommandRouter
import com.example.data.database.AppDatabase
import com.example.data.datastore.SettingsDataStore
import com.example.data.repository.LeadRepository
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Embeds the REAL Flutter UI in the existing Android application only when
 * -PwaFlutterUi=true. Normal Android builds exclude this file entirely.
 *
 * Keeps the original authentication, license, Room database, DataStore,
 * notification listener and Contacts provider. All calls pass a session
 * check and the explicit native method whitelist.
 */
class WaFlutterHostActivity : FlutterActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val authMutex = Mutex()
    private var authorizedUntilElapsed: Long = 0L
    private var authorizedUserId: String? = null
    private val sessionManager by lazy { SessionManager(applicationContext) }

    private val authRepository by lazy { AuthRepository(applicationContext) }
    private val licenseManager by lazy { LicenseManager(applicationContext) }
    private val commandRouter by lazy {
        val app = applicationContext
        val db = AppDatabase.getDatabase(app)
        val settings = SettingsDataStore(app)
        val repository = LeadRepository(app, db, settings)
        NativeUiCommandRouter(AndroidLeadUiDataSource(app, db, repository, settings))
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger, "wa_lead_saver/native_v1"
        ).setMethodCallHandler { call, reply ->
            // Returning to the native login screen must work even if the
            // session has expired or the account has been stopped.
            if (call.method == "closeFlutter") {
                reply.success(mapOf("ok" to true))
                finish()
                return@setMethodCallHandler
            }
            val raw = call.arguments
            if (raw != null && raw !is Map<*, *>) {
                reply.error("BAD_REQUEST", "Invalid method arguments", null)
                return@setMethodCallHandler
            }
            val args = (raw as? Map<*, *>)?.entries?.associate { (key, value) ->
                key.toString() to value
            } ?: emptyMap()

            scope.launch {
                try {
                    requireAuthenticatedSession()
                    reply.success(commandRouter.execute(call.method, args))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SecurityException) {
                    reply.error("AUTH_REQUIRED", e.message ?: "Authentication required", null)
                } catch (e: IllegalArgumentException) {
                    reply.error("BAD_REQUEST", e.message ?: "Invalid input", null)
                } catch (e: Exception) {
                    reply.error("NATIVE_ERROR", "تعذر تنفيذ الإجراء، يرجى المحاولة", null)
                }
            }
        }
    }

    /**
     * Real Supabase session validation (including existing offline-grace
     * policy), with a short cache to avoid a network lookup on each page.
     */
    private suspend fun requireAuthenticatedSession() {
        authMutex.withLock {
            // Check the encrypted local session on EVERY call: logout in the
            // original Kotlin UI must revoke cached bridge access promptly.
            val session = withContext(Dispatchers.IO) {
                sessionManager.getCachedSession()
            } ?: run {
                authorizedUntilElapsed = 0L
                authorizedUserId = null
                throw SecurityException("انتهت الجلسة، يرجى تسجيل الدخول")
            }

            if (authorizedUserId == session.userId &&
                SystemClock.elapsedRealtime() < authorizedUntilElapsed) {
                val cachedLicense = licenseManager.checkLicense()
                if (cachedLicense is LicenseStatus.Active ||
                    cachedLicense is LicenseStatus.PendingActivation) {
                    return@withLock
                }
            }

            val state = withContext(Dispatchers.IO) {
                authRepository.checkInitialSession()
            }
            val validLogin = state is AuthState.AuthenticatedAdmin ||
                state is AuthState.AuthenticatedUser
            val license = licenseManager.checkLicense()
            // Pending accounts can read local pages, but the original
            // LeadRepository disallows saving Contacts until activation.
            val allowedLicense = license is LicenseStatus.Active ||
                license is LicenseStatus.PendingActivation

            if (!validLogin || !allowedLicense) {
                authorizedUntilElapsed = 0L
                authorizedUserId = null
                throw SecurityException("يرجى تسجيل الدخول بحساب مصرح")
            }
            authorizedUserId = session.userId
            authorizedUntilElapsed = SystemClock.elapsedRealtime() + 30_000L
        }
    }

    override fun onPause() {
        // Lock authorization when the original app comes to the foreground,
        // including login/logout flows, to avoid a stale 30-second grant.
        authorizedUntilElapsed = 0L
        authorizedUserId = null
        super.onPause()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
