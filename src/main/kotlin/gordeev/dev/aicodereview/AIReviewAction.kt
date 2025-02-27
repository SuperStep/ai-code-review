package gordeev.dev.aicodereview

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ComboBox
import git4idea.GitUtil
import git4idea.repo.GitRepository
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
                val reviewService = DiffReviewService()
                reviewService.performReview(project, sourceBranch, targetBranch)
            }
        }
    }

    class BranchSelectionDialog(private val project: Project, private val repository: GitRepository) :
        DialogWrapper(project) {
        private val sourceBranchComboBox = ComboBox<String>()
        private val targetBranchComboBox = ComboBox<String>()
        var sourceBranch: String? = null
            private set
        var targetBranch: String? = null
            private set

        init {
            title = "Select Branches to Extract Diff for AI Code Review"
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
