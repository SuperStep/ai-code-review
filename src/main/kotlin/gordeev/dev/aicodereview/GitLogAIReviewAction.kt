package gordeev.dev.aicodereview

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.vcs.log.VcsLogDataKeys
import git4idea.GitUtil

/**
 * Action that triggers AI code review from Git Log view.
 * This action appears in the git log context menu when a commit is selected.
 */
class GitLogAIReviewAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val repository = GitUtil.getRepositoryManager(project).repositories.firstOrNull() ?: return

        // Get the selected commit from Git log using the non-deprecated API
        val commitSelection = e.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION) ?: return
        val selectedCommits = commitSelection.commits
        if (selectedCommits.isEmpty()) return

        // Use the selected commit's hash as the target branch
        val targetBranch = selectedCommits.first().hash.asString()

        // Get current branch as source branch
        val currentBranch = repository.currentBranch?.name ?: return

        // Use the new DiffReviewService to perform the review
        val reviewService = DiffReviewService()
        reviewService.performReview(project, currentBranch, targetBranch)
    }

    override fun update(e: AnActionEvent) {
        // Only enable the action if we have a project and git log selection
        val project = e.getData(CommonDataKeys.PROJECT)
        val commitSelection = e.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION)
        e.presentation.isEnabledAndVisible = project != null &&
                commitSelection != null &&
                !commitSelection.commits.isEmpty()
    }
}
