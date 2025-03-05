package gordeev.dev.aicodereview.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import git4idea.GitUtil
import gordeev.dev.aicodereview.DiffReviewService
import gordeev.dev.aicodereview.bitbucket.BitbucketService
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.awt.*
import javax.swing.*

class ReviewToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val reviewPanel = ReviewPanel(project)
        val content = ContentFactory.getInstance().createContent(reviewPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class ReviewPanel(private val project: Project) : JPanel() {

    private val targetBranchComboBox = ComboBox<String>()
    private val reviewButton = JButton("Review against target branch")
    private val currentBranchLabel = JBLabel()
    private val diffEditorPane = JEditorPane().apply {
        isEditable = false
        contentType = "text/html"
    }
    private val loadingPanel = JBLoadingPanel(BorderLayout(), project)
    private val updateBranchesButton = JButton("Update Branches")
    private val createPullRequestButton = JButton("Create Pull Request")

    init {
        layout = GridBagLayout()
        val gbc = GridBagConstraints()

        // --- Row 0: Update Branches Button ---
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 1
        gbc.weightx = 0.01
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        add(updateBranchesButton, gbc)

        updateBranchesButton.icon = com.intellij.icons.AllIcons.Actions.Refresh
        updateBranchesButton.text = ""

        // --- Row 0: Create Pull Request Button ---
        gbc.gridx = 1 //  Next column
        gbc.gridy = 0
        gbc.gridwidth = 1
        gbc.weightx = 0.5
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        add(createPullRequestButton, gbc)
        // TODO: Enable after PR is implemented
        createPullRequestButton.isEnabled = false



        // --- Row 1: Current Branch ---
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.WEST
        add(JBLabel("Current Branch: "), gbc)

        gbc.gridx = 1
        gbc.gridy = 1
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        add(currentBranchLabel, gbc)

        // --- Row 2: Target Branch ---
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        add(JBLabel("Target Branch: "), gbc)

        gbc.gridx = 1
        gbc.gridy = 2
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        add(targetBranchComboBox, gbc)

        // --- Row 3: Review Button ---
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.CENTER
        add(reviewButton, gbc)

        // --- Row 4: Diff Text Area (inside JBLoadingPanel) ---
        gbc.gridx = 0
        gbc.gridy = 4
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        loadingPanel.add(JBScrollPane(diffEditorPane), BorderLayout.CENTER)
        add(loadingPanel, gbc)

        reviewButton.addActionListener {
            performReview()
        }

        updateBranchesButton.addActionListener {
            updateBranches()
        }

        createPullRequestButton.addActionListener {
            createPullRequest()
        }

        updateBranches()
    }

    private fun updateBranches() {
        val repository = GitUtil.getRepositoryManager(project).repositories.firstOrNull() ?: return
        val branches = repository.branches.localBranches
        val branchNames = branches.map { it.name }
        val currentTargetBranch = targetBranchComboBox.selectedItem as? String

        targetBranchComboBox.model = DefaultComboBoxModel(branchNames.toTypedArray())
        if (currentTargetBranch != null && branchNames.contains(currentTargetBranch)) {
            targetBranchComboBox.selectedItem = currentTargetBranch
        }
        targetBranchComboBox.isEditable = false

        currentBranchLabel.text = repository.currentBranchName ?: "No branch"
    }

    private fun performReview() {
        val repository = GitUtil.getRepositoryManager(project).repositories.firstOrNull() ?: return
        val currentBranch = repository.currentBranchName ?: return
        val targetBranch = targetBranchComboBox.selectedItem as? String ?: return

        val reviewService = DiffReviewService()

        loadingPanel.startLoading()
        diffEditorPane.text = ""

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Performing AI Code Review") {
            override fun run(indicator: ProgressIndicator) {
                val reviewResult = reviewService.performReview(project, currentBranch, targetBranch)

                ApplicationManager.getApplication().invokeLater {
                    loadingPanel.stopLoading()
                    if (reviewResult != null) {
                        val parser = Parser.builder().build()
                        val document = parser.parse(reviewResult)
                        val renderer = HtmlRenderer.builder().build()
                        val html = renderer.render(document)
                        diffEditorPane.text = html
                    } else {
                        diffEditorPane.text = "<html><body><b>Review failed.</b></body></html>"
                    }
                }
            }
        })
    }

    private fun createPullRequest() {
        val repository = GitUtil.getRepositoryManager(project).repositories.firstOrNull() ?: return
        val currentBranch = repository.currentBranchName ?: return
        val targetBranch = targetBranchComboBox.selectedItem as? String ?: return

        val bitbucketService = BitbucketService.getInstance()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Creating Pull Request") {
            override fun run(indicator: ProgressIndicator) {
                val pullRequest = bitbucketService.createPullRequest(
                    sourceBranch = currentBranch,
                    targetBranch = targetBranch
                )
                ApplicationManager.getApplication().invokeLater {
                    if (pullRequest != null) {
                        JOptionPane.showMessageDialog(
                            null,
                            "Pull request created successfully: ${pullRequest.getWebUrl()}",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    } else {
                        JOptionPane.showMessageDialog(
                            null,
                            "Failed to create pull request.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
        })
    }
}
