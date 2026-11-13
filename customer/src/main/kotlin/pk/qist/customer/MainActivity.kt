package pk.qist.customer

import android.os.Bundle
import android.content.Intent
import pk.qist.customer.platform.CustomerPushSignals
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import pk.qist.customer.ui.CustomerShell
import pk.qist.customer.ui.theme.QistTheme

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("open_customer_notifications", false)) { CustomerPushSignals.openInbox.value = true; intent.removeExtra("open_customer_notifications"); CustomerPushSignals.changed() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("open_customer_notifications", false)) { CustomerPushSignals.openInbox.value = true; intent.removeExtra("open_customer_notifications") }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent {
            QistTheme {
                Surface { CustomerShell() }
            }
        }
    }
}
