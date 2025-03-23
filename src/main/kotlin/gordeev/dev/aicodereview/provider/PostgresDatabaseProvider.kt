package gordeev.dev.aicodereview.provider

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.Properties

class PostgresDatabaseProvider(
    private val connectionConfig: DatabaseConnectionConfig = DatabaseConnectionConfig()
) : ContexProvider {

    override fun getContext(query: String): List<String> {
        var connection: Connection? = null
        var statement: PreparedStatement? = null

        try {
            connection = getConnection()
            val sql = connectionConfig.ragRequestStatement
            statement = connection.prepareStatement(sql)
            statement.setString(1, query)

            val resultSet = statement.executeQuery()
            val results = mutableListOf<String>()

            while (resultSet.next()) {
                val path = resultSet.getString("path")
                val chunk = resultSet.getString("chunk")
                if (!chunk.isNullOrBlank()) {
                    results.add("$path\n$chunk")
                }
            }

            return results
        } catch (e: Exception) {
            throw DatabaseException("Failed to execute RAG query", e)
        } finally {
            statement?.close()
            connection?.close()
        }
    }


    private fun getConnection(): Connection {
        try {
            Class.forName("org.postgresql.Driver")

            val props = Properties()
            props.setProperty("user", connectionConfig.username)
            props.setProperty("password", connectionConfig.password)

            return DriverManager.getConnection(connectionConfig.jdbcUrl, props)
        } catch (e: Exception) {
            throw DatabaseException("Failed to establish database connection", e)
        }
    }
}
