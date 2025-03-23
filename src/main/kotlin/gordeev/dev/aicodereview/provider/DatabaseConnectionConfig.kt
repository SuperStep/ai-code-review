package gordeev.dev.aicodereview.provider

import gordeev.dev.aicodereview.settings.AppSettingsState

class DatabaseConnectionConfig(
    val host: String = AppSettingsState.instance.dbHost ?: "localhost",
    val port: Int = AppSettingsState.instance.dbPort ?: 5432,
    val database: String = AppSettingsState.instance.dbName ?: "postgres",
    val username: String = AppSettingsState.instance.dbUsername ?: "postgres",
    val password: String = AppSettingsState.instance.dbPassword ?: "",
    val ragRequestStatement: String =
        AppSettingsState.instance.ragRequestStatement ?: "SELECT path, chunk FROM find_rag_content(?)",
) {
    val jdbcUrl: String
        get() = "jdbc:postgresql://$host:$port/$database"
}