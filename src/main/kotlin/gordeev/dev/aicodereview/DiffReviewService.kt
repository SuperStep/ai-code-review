package gordeev.dev.aicodereview

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import git4idea.GitUtil
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import gordeev.dev.aicodereview.settings.AppSettingsState

class DiffReviewService {

    fun performReview(project: Project, sourceBranch: String, targetBranch: String) {
        val repository = GitUtil.getRepositoryManager(project).repositories.firstOrNull() ?: return

        val diff = getDiff(project, repository, sourceBranch, targetBranch)
        if (diff != null) {
            val settings = AppSettingsState.instance
            val aiService: AiReviewProvider = when (settings.modelProvider) {
                AppSettingsState.ModelProvider.OLLAMA -> OllamaReviewProvider()
                AppSettingsState.ModelProvider.GEMINI -> GeminiReviewProvider()
            }
            val review = aiService.getReview(project, diff)
            if (review != null) {
                ReviewDialog(project, review, sourceBranch, targetBranch).show()
            } // Error handling is done within the service implementations
        } else {
            NotificationUtil.showErrorNotification(project, "Failed to get diff")
        }
    }

    fun getDiff(project: Project, repository: GitRepository, sourceBranch: String, targetBranch: String): String? {
        val handler = GitLineHandler(project, repository.root, GitCommand.DIFF)
        handler.addParameters(targetBranch, sourceBranch)
        handler.setSilent(true)
        handler.endOptions()

        return try {
            Git.getInstance().runCommand(handler).getOutputOrThrow()
        } catch (e: VcsException) {
            NotificationUtil.showErrorNotification(project, "Failed to get diff: ${e.message}")
            null
        }
    }
}
