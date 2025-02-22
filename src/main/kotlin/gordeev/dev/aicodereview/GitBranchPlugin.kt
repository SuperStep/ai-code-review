package gordeev.dev.aicodereview

import com.google.gson.Gson
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import git4idea.GitUtil
import git4idea.branch.GitBranchUtil
import git4idea.repo.GitRepository
import gordeev.dev.aicodereview.settings.AppSettingsState
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class GitBranchPlugin : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repository = getRepository(project) ?: return
        val settings = AppSettingsState.instance

        if (settings.modelProvider != AppSettingsState.ModelProvider.OLLAMA) {
            Messages.showMessageDialog(
                project,
                "Currently only Ollama model provider is supported for code review.",
                "Model Provider Not Supported",
                Messages.getInformationIcon()
            )
            return
        }
        val currentBranch = repository.currentBranch ?: return
        val selectedBranch = GitBranchUtil.chooseBranch(project, repository, "Select Branch to Compare") ?: return

        val diff = getDiff(repository, currentBranch.name, selectedBranch.name)

        if (diff.isNullOrBlank()) {
            Messages.showMessageDialog(project, "No changes found.", "Empty Diff", Messages.getInformationIcon())
            return
        }

        val prompt = "${settings.userMessage}\n\n\n$diff\n"
        val requestBody = """{"prompt": "$prompt", "model": "${settings.ollamaModel}", "stream": false}"""
        val ollamaResponse = sendToOllama(settings.ollamaUrl, requestBody)

        Messages.showMessageDialog(project, ollamaResponse, "AI Code Review", Messages.getInformationIcon())
    }

    private fun getRepository(project: Project): GitRepository? {
        val repositories = GitUtil.getRepositories(project)
        if (repositories.isEmpty()) {
            Messages.showMessageDialog(
                project,
                "No Git repositories found in this project.",
                "No Repositories Found",
                Messages.getErrorIcon()
            )
            return null
        }
        // For simplicity, we'll just use the first repository.  In a real plugin,
        // you might want to let the user choose if there are multiple.
        return repositories.first()
    }

    private fun getDiff(repository: GitRepository, baseBranch: String, targetBranch: String): String? {
        val changes = GitUtil.getDiff(repository, baseBranch, targetBranch, true) ?: return null
        return changes.joinToString("\n") { change -> formatChange(change) }
    }

    private fun formatChange(change: Change): String {
        return when (change.type) {
            Change.Type.MODIFIED -> {
                "--- a/${change.beforeRevision?.file?.path}\n" +
                        "+++ b/${change.afterRevision?.file?.path}\n" +
                        change.afterRevision?.content.let {
                            git4idea.util.GitChangeDiffOperations.getDiff(
                                change.beforeRevision?.content,
                                it
                            )
                        }
            }

            Change.Type.NEW -> {
                "--- /dev/null\n" +
                        "+++ b/${change.afterRevision?.file?.path}\n" +
                        change.afterRevision?.content
            }

            Change.Type.DELETED -> {
                "--- a/${change.beforeRevision?.file?.path}\n" +
                        "+++ /dev/null\n" +
                        change.beforeRevision?.content
            }

            Change.Type.MOVED -> {
                "--- a/${change.beforeRevision?.file?.path}\n" +
                        "+++ b/${change.afterRevision?.file?.path}\n" +
                        change.afterRevision?.content.let {
                            git4idea.util.GitChangeDiffOperations.getDiff(
                                change.beforeRevision?.content,
                                it
                            )
                        }
            }
        }
    }

    private fun sendToOllama(ollamaUrl: String, requestBody: String): String {
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(ollamaUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val gson = Gson()
                val ollamaResponse = gson.fromJson(response.body(), OllamaResponse::class.java)
                ollamaResponse.response
            } else {
                "Error: ${response.statusCode()} - ${response.body()}"
            }
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }

    data class OllamaResponse(val response: String)
}