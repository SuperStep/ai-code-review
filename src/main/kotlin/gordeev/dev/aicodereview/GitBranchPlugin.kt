package gordeev.dev.aicodereview

import com.google.gson.Gson
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vcs.VcsException
import git4idea.GitUtil
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import gordeev.dev.aicodereview.settings.AppSettingsState
import java.awt.BorderLayout
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.swing.*


class GitBranchPlugin : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repository = GitUtil.getRepositoryManager(project).repositories.firstOrNull() ?: return

        val branchDialog = BranchSelectionDialog(project, repository)
        if (branchDialog.showAndGet()) {
            val sourceBranch = branchDialog.sourceBranch
            val targetBranch = branchDialog.targetBranch

            if (sourceBranch != null && targetBranch != null) {
                val diff = getDiff(project, repository, sourceBranch, targetBranch)
                if (diff != null) {
                    // Process the diff string (e.g., send it to your AI model)
                    //println("Diff:\n$diff") // Replace with your processing logic
                    sendDiffToOllama(project, diff)
                } else {
                    // Handle the case where diff couldn't be retrieved
                    showErrorNotification(project, "Failed to get diff")
                }
            }
        }
    }

    private fun getDiff(project: Project, repository: GitRepository, sourceBranch: String, targetBranch: String): String? {
        val handler = GitLineHandler(project, repository.root, GitCommand.DIFF)
        handler.addParameters(targetBranch, sourceBranch) // target..source  (like in git diff command)
        handler.setSilent(true) // Don't show the command in the console
        handler.endOptions()

        return try {
            Git.getInstance().runCommand(handler).getOutputOrThrow()
        } catch (e: VcsException) {
            showErrorNotification(project, "Failed to get diff: ${e.message}")
            null // Return null to indicate failure
        }
    }

    private fun sendDiffToOllama(project: Project, diff: String) {
        val settings = AppSettingsState.instance
        if (settings.modelProvider != AppSettingsState.ModelProvider.OLLAMA) {
            showErrorNotification(project, "Ollama is not selected as the model provider.")
            return
        }
        if (settings.ollamaModel.isBlank()) {
            showErrorNotification(project, "Ollama model is not selected.")
            return
        }

        val client = HttpClient.newHttpClient()
        val requestBody = Gson().toJson(
            mapOf(
                "prompt" to "${settings.userMessage}\n ${diff}",
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
                // Assuming a simple JSON response with a "response" field.  Adapt as needed.
                val jsonResponse = Gson().fromJson(body, Map::class.java)
                val aiResponse = jsonResponse["response"] as? String ?: "No response from AI."
                showNotification(project, "AI Review:\n$aiResponse", NotificationType.INFORMATION)
            } else {
                showErrorNotification(project, "Ollama API error: ${response.statusCode()} - ${response.body()}")
            }
        } catch (e: Exception) {
            showErrorNotification(project, "Error communicating with Ollama: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun showErrorNotification(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AI Code Review Errors")
            .createNotification(message, NotificationType.ERROR)
            .notify(project)
    }

    private fun showNotification(project: Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AI Code Review") // Use a consistent group ID
            .createNotification(message, type)
            .notify(project)
    }

    class BranchSelectionDialog(private val project: Project, private val repository: GitRepository) : DialogWrapper(project) {
        private val sourceBranchComboBox = JComboBox<String>()
        private val targetBranchComboBox = JComboBox<String>()
        var sourceBranch: String? = null
            private set
        var targetBranch: String? = null
            private set

        init {
            title = "Select Branches for AI Code Review"
            init() // Important: Initialize the dialog
            populateBranchComboBoxes()
        }

        private fun populateBranchComboBoxes() {
            val branches = repository.branches.localBranches.map { it.name }
            sourceBranchComboBox.model = DefaultComboBoxModel(branches.toTypedArray())
            targetBranchComboBox.model = DefaultComboBoxModel(branches.toTypedArray())

            // Set default selections (optional, but good for UX)
            val currentBranch = repository.currentBranch?.name
            if (currentBranch != null) {
                targetBranchComboBox.selectedItem = currentBranch
            }
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(BorderLayout())

            val sourcePanel = JPanel()
            sourcePanel.add(JLabel("Source Branch:"))
            sourcePanel.add(sourceBranchComboBox)

            val targetPanel = JPanel()
            targetPanel.add(JLabel("Target Branch:"))
            targetPanel.add(targetBranchComboBox)

            panel.add(sourcePanel, BorderLayout.NORTH)
            panel.add(targetPanel, BorderLayout.SOUTH)

            return panel
        }

        override fun doOKAction() {
            sourceBranch = sourceBranchComboBox.selectedItem as? String
            targetBranch = targetBranchComboBox.selectedItem as? String
            super.doOKAction()
        }
    }
}
