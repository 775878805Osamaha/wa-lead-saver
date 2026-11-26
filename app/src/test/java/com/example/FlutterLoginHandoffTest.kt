package com.example

import com.example.data.auth.AuthState
import com.example.data.auth.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class FlutterLoginHandoffTest {
    private val profile = UserProfile(id = "fixture", email = "test@example.invalid")

    @Test
    fun nativeDashboardIsNeverSelectedDuringFlutterHandoff() {
        val cases = listOf(
            AuthState.Initializing to FlutterLoginRoute.CHECKING,
            AuthState.Unauthenticated to FlutterLoginRoute.LOGIN,
            AuthState.AuthenticatedUser(profile, null) to FlutterLoginRoute.RETURN_TO_FLUTTER,
            AuthState.AuthenticatedAdmin(profile) to FlutterLoginRoute.RETURN_TO_FLUTTER,
            AuthState.AccountInactive("fixture", "inactive") to FlutterLoginRoute.LOGIN,
            AuthState.AccountExpired("fixture", "expired") to FlutterLoginRoute.LOGIN,
            AuthState.DeviceLimitExceeded("fixture", "limit") to FlutterLoginRoute.LOGIN
        )
        for ((state, expected) in cases) {
            assertEquals(state.toString(), expected, resolveFlutterLoginRoute(true, state, false))
            assertEquals(FlutterLoginRoute.RECOVERY, resolveFlutterLoginRoute(true, state, true))
            assertEquals(FlutterLoginRoute.NATIVE, resolveFlutterLoginRoute(false, state, false))
            assertEquals(FlutterLoginRoute.RECOVERY, resolveFlutterLoginRoute(false, state, true))
        }
    }
}
