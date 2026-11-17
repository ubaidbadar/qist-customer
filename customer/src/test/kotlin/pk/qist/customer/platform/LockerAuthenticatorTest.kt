package pk.qist.customer.platform

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class LockerAuthenticatorTest {
    @Test fun `missing enrolled Locker cannot approve a local sign in`() = runBlocking {
        val authenticator = AndroidLockerAuthenticator(ApplicationProvider.getApplicationContext())
        assertFalse(authenticator.available())
        assertFalse(authenticator.authenticate("a".repeat(24), "n".repeat(43)))
    }
}
