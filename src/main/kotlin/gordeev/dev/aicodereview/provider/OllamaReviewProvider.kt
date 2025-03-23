package gordeev.dev.aicodereview.provider

import com.google.gson.Gson
import com.intellij.openapi.project.Project
import gordeev.dev.aicodereview.NotificationUtil
import gordeev.dev.aicodereview.settings.AppSettingsState
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class OllamaReviewProvider : AiReviewProvider {
    override fun getReview(project: Project, diff: String): String? {
        val settings = AppSettingsState.instance
        if (settings.ollamaModel.isBlank()) {
            NotificationUtil.showErrorNotification(project, "Ollama model is not selected.")
            return null
        }

        var contextString = ""
        if (settings.dbHost?.isNotBlank() == true){
            var contextProvider = PostgresDatabaseProvider()
            var context = contextProvider.getContext(diff)

            contextString = buildString {
                append("Here is context with relevant existing code:\n\n")
                context.forEach {
                    append(it).append("\n")
                }
            }
        }

        val client = HttpClient.newHttpClient()
        val requestBody = Gson().toJson(
            mapOf(
                "prompt" to "#######${settings.userMessage}\n\n$diff\n\n#######\n\n${contextString}",
                "model" to settings.ollamaModel,
                "stream" to false
            )
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create(settings.ollamaUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                val body = response.body()
                val jsonResponse = Gson().fromJson(body, Map::class.java)
                return jsonResponse["response"] as? String ?: "No response from AI."
            } else {
                NotificationUtil.showErrorNotification(
                    project,
                    "Ollama API error: ${response.statusCode()} - ${response.body()}"
                )
                return null
            }
        } catch (e: Exception) {
            NotificationUtil.showErrorNotification(project, "Error communicating with Ollama: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
}
