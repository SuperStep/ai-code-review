package gordeev.dev.aicodereview.provider

import com.google.gson.Gson
import com.intellij.openapi.project.Project
import gordeev.dev.aicodereview.settings.AppSettingsState
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import gordeev.dev.aicodereview.NotificationUtil


class TogetherAiReviewProvider : AiReviewProvider {

    override fun getReview(project: Project, diff: String): String? {
        val settings = AppSettingsState.instance
        if (settings.togetherApiKey.isBlank()) {
            NotificationUtil.showErrorNotification(project, "TogetherAI API Key is not set.")
            return null
        }

        val client = HttpClient.newHttpClient()
        val requestBody = Gson().toJson(
            mapOf(
                "model" to "meta-llama/Llama-3-70B-Instruct-Turbo",
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to "${settings.userMessage} \n\n$diff\n\n\n${settings.userMessage}"
                    )
                )
            )
        )
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.together.xyz/v1/chat/completions"))
            .header("Authorization", "Bearer ${settings.togetherApiKey}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val body = response.body()
                val jsonResponse = Gson().fromJson(body, Map::class.java)
                return extractTogetherAiResponse(jsonResponse)
            } else {
                NotificationUtil.showErrorNotification(
                    project,
                    "TogetherAI API error: ${response.statusCode()} - ${response.body()}"
                )
                return null
            }

        } catch (e: Exception) {
            NotificationUtil.showErrorNotification(project, "Error communicating with TogetherAI: ${e.message}")
            e.printStackTrace()
            return null
        }
    }


    private fun extractTogetherAiResponse(jsonResponse: Map<*, *>): String {
        val choices = jsonResponse["choices"] as? List<*> ?: return "No response from AI."
        val firstChoice = choices.firstOrNull() as? Map<*, *> ?: return "No response from AI."
        val message = firstChoice["message"] as? Map<*, *> ?: return "No response from AI."
        return message["content"] as? String ?: "No response from AI."
    }
}
