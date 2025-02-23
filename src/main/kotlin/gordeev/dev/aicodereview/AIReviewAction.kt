package gordeev.dev.aicodereview

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

import javax.swing.*

class AIReviewAction : AnAction() {

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
                    val settings = AppSettingsState.instance
                    val aiService: AiReviewProvider = when (settings.modelProvider) {
                        AppSettingsState.ModelProvider.OLLAMA -> OllamaReviewProvider()
                        AppSettingsState.ModelProvider.GEMINI -> GeminiReviewProvider()
                    }
                    val review = aiService.getReview(project, diff)
                    if (review != null) {
                        ReviewDialog(project, review).show()
                    } // Error handling is done within the service implementations
                } else {
                    NotificationUtil.showErrorNotification(project, "Failed to get diff")
                }
            }
        }
    }

    private fun getDiff(project: Project, repository: GitRepository, sourceBranch: String, targetBranch: String): String? {
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


    class BranchSelectionDialog(private val project: Project, private val repository: GitRepository) :
        DialogWrapper(project) {
        private val sourceBranchComboBox = JComboBox<String>()
        private val targetBranchComboBox = JComboBox<String>()
        var sourceBranch: String? = null
            private set
        var targetBranch: String? = null
            private set

        init {
            title = "Select branches to extract diff for AI code review"
            init()
            populateBranchComboBoxes()
        }

        private fun populateBranchComboBoxes() {
            val branches = repository.branches.localBranches.map { it.name }
            sourceBranchComboBox.model = DefaultComboBoxModel(branches.toTypedArray())
            targetBranchComboBox.model = DefaultComboBoxModel(branches.toTypedArray())

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
