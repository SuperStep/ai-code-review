package gordeev.dev.aicodereview

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object NotificationUtil {
    fun showErrorNotification(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AI Code Review Errors")
            .createNotification(message, NotificationType.ERROR)
            .notify(project)
    }

    fun showSuccessNotification(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AI Code Review Errors") // Reusing the same group
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
    }

    fun showNotificationWithUrl(
        project: Project,
        title: String,
        message: String,
        linkText: String,
        url: String,
        notificationType: NotificationType = NotificationType.INFORMATION
    ) {
        val htmlContent = "$message <a href=\"$url\">$linkText</a>"

        NotificationGroupManager.getInstance()
            .getNotificationGroup("AI Code Review Errors")
            .createNotification(title, htmlContent, notificationType)
            .setImportant(true)
            .notify(project)
    }
}
