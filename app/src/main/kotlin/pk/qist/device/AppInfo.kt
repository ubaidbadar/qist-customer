package pk.qist.device

/** What the app tells the server about itself. Never personal data. */
data class AppInfo(val appVersion: String, val androidVersion: String)
