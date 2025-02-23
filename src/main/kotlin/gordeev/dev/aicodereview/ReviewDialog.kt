package gordeev.dev.aicodereview

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import java.awt.Dimension

class ReviewDialog(project: Project?, private val reviewText: String) : DialogWrapper(project) {
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
        return panel
    }
}