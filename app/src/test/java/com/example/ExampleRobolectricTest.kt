package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.util.ContactsHelper
import com.example.util.LocaleHelper
import com.example.util.PermissionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("WA Lead Saver", appName)
  }

  @Test
  fun `activity launches without crashing`() {
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    scenario.onActivity { activity ->
      assertNotNull(activity)
      assertFalse(activity.isFinishing)
    }
  }

  @Test
  fun `locale helper context works for arabic and english`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val arContext = LocaleHelper.getLocalizedContext(context, LocaleHelper.LANGUAGE_ARABIC)
    assertNotNull(arContext)
    val arTitle = arContext.getString(R.string.tab_dashboard)
    assertEquals("لوحة التحكم", arTitle)

    val enContext = LocaleHelper.getLocalizedContext(context, LocaleHelper.LANGUAGE_ENGLISH)
    assertNotNull(enContext)
    val enTitle = enContext.getString(R.string.tab_dashboard)
    assertEquals("Dashboard", enTitle)
  }

  @Test
  fun `permission and contacts helpers execute safely`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    // Should not throw any exception even without permissions granted
    val isListenerEnabled = PermissionHelper.isNotificationListenerEnabled(context)
    assertFalse(isListenerEnabled)

    val isBatteryIgnored = PermissionHelper.isBatteryOptimizationIgnored(context)
    // Safe boolean check
    assertNotNull(isBatteryIgnored)

    val hasContacts = ContactsHelper.hasContactsPermissions(context)
    assertFalse(hasContacts)
  }
}
