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
}
