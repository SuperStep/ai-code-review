package gordeev.dev.aicodereview

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import gordeev.dev.aicodereview.bitbucket.BitbucketService
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

class ReviewDialog(
    project: Project?,
    private val reviewText: String,
    private val sourceBranch: String,
    private val targetBranch: String
) : DialogWrapper(project) {

    private val bitbucketService = BitbucketService.getInstance()

    init {
        title = "AI Code Review"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        val textArea = JTextArea(reviewText)
        textArea.isEditable = false
        textArea.lineWrap = true
        textArea.wrapStyleWord = true

        val scrollPane = JScrollPane(textArea)
        scrollPane.preferredSize = Dimension(600, 400) // Adjust size as needed
        panel.add(scrollPane, BorderLayout.CENTER)

        // Add button panel at the bottom
        if (sourceBranch.isNotEmpty() && targetBranch.isNotEmpty()) {
            val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
            val createPrButton = JButton("Create Pull Request")
            createPrButton.addActionListener {
                createPullRequest()
            }
            buttonPanel.add(createPrButton)
            panel.add(buttonPanel, BorderLayout.SOUTH)
        }

        return panel
    }

    private fun createPullRequest() {
        try {
            val pullRequest = bitbucketService.createPullRequest(
                sourceBranch = sourceBranch,
                targetBranch = targetBranch,
                title = "Pull request from $sourceBranch to $targetBranch",
                description = reviewText
            )

            if (pullRequest != null) {
                val webUrl = pullRequest.getWebUrl()
                Messages.showInfoMessage(
                    "Pull request created successfully!\nID: ${pullRequest.id}\n${if (webUrl != null) "URL: $webUrl" else ""}",
                    "Pull Request Created"
                )
            } else {
                Messages.showErrorDialog(
                    "Failed to create pull request. Check console for details.",
                    "Pull Request Creation Failed"
                )
            }
        } catch (e: Exception) {
            Messages.showErrorDialog(
                "Error creating pull request: ${e.message}",
                "Error"
            )
        }
    }
}