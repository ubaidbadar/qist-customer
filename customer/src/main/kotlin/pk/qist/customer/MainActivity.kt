package pk.qist.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.ui.graphics.toArgb
import pk.qist.customer.ui.CustomerShell
import pk.qist.customer.ui.theme.Clear
import pk.qist.customer.ui.theme.QistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Clear.toArgb(), Clear.toArgb()),
        )
        setContent {
            QistTheme {
                Surface { CustomerShell() }
            }
        }
    }
}
