package gordeev.dev.aicodereview.provider

import com.google.gson.Gson
import com.intellij.openapi.project.Project
import gordeev.dev.aicodereview.NotificationUtil
import gordeev.dev.aicodereview.settings.AppSettingsState
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class GeminiReviewProvider : AiReviewProvider {
    override fun getReview(project: Project, diff: String): String? {
        val settings = AppSettingsState.instance
        if (settings.geminiToken.isBlank()) {
            NotificationUtil.showErrorNotification(project, "Gemini token is not set.")
            return null
        }

        val client = HttpClient.newHttpClient()
        val requestBody = Gson().toJson(
            mapOf(
                "contents" to listOf(
                    mapOf(
                        "parts" to listOf(
                            mapOf("text" to "Review the following code diff:\n\n$diff\n\n\n${settings.userMessage}")
                        )
                    )
                )
            )
        )
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=${settings.geminiToken}"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()


        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val body = response.body()
                val jsonResponse = Gson().fromJson(body, Map::class.java)
                return extractGeminiResponse(jsonResponse)
            } else {
                NotificationUtil.showErrorNotification(
                    project,
                    "Gemini API error: ${response.statusCode()} - ${response.body()}"
                )
                return null
            }

        } catch (e: Exception) {
            NotificationUtil.showErrorNotification(project, "Error communicating with Gemini: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    private fun extractGeminiResponse(jsonResponse: Map<*, *>): String {
        val candidates = jsonResponse["candidates"] as? List<*> ?: return "No response from AI."
        val firstCandidate = candidates.firstOrNull() as? Map<*, *> ?: return "No response from AI."
        val content = firstCandidate["content"] as? Map<*, *> ?: return "No response from AI."
        val parts = content["parts"] as? List<*> ?: return "No response from AI."
        val firstPart = parts.firstOrNull() as? Map<*, *> ?: return "No response from AI."
        return firstPart["text"] as? String ?: "No response from AI."
    }
}
