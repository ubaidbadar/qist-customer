package pk.qist.customer.data

import org.junit.Assert.assertEquals
import org.junit.Test
import pk.qist.customer.platform.NotificationPermissionAction
import pk.qist.customer.platform.notificationPermissionAction

class NotificationPermissionTest {
    @Test fun `explicit opt in may request the Android permission`() {
        assertEquals(NotificationPermissionAction.Ask, notificationPermissionAction(true, true, false))
    }
    @Test fun `resume never prompts again for a permission revoked in settings`() {
        assertEquals(NotificationPermissionAction.TurnOff, notificationPermissionAction(true, false, false))
    }
    @Test fun `granted and disabled preferences do not trigger permission prompts`() {
        assertEquals(NotificationPermissionAction.Keep, notificationPermissionAction(true, false, true))
        assertEquals(NotificationPermissionAction.Keep, notificationPermissionAction(false, false, false))
    }
}
