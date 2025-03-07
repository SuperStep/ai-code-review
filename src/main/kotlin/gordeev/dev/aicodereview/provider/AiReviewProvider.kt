package gordeev.dev.aicodereview.provider

import com.intellij.openapi.project.Project

interface AiReviewProvider {
    fun getReview(project: Project, diff: String): String?
}
