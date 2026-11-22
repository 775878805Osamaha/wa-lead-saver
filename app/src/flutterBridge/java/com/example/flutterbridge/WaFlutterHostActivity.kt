package com.example.flutterbridge

import android.os.SystemClock
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthState
import com.example.data.auth.LicenseManager
import com.example.data.auth.LicenseStatus
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
            if (SystemClock.elapsedRealtime() < authorizedUntilElapsed) {
                return@withLock
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
                throw SecurityException("يرجى تسجيل الدخول بحساب مصرح")
            }
            authorizedUntilElapsed = SystemClock.elapsedRealtime() + 30_000L
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
