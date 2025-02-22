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
import java.awt.BorderLayout
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
                    println("Diff:\n$diff") // Replace with your processing logic
                } else {
                    // Handle the case where diff couldn't be retrieved
                    println("Failed to get diff") // Replace with proper error handling (notification)
                }
            }

            // Send the diff to your AI model for review


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
            e.printStackTrace() // Log the exception
            null // Return null to indicate failure
        }
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