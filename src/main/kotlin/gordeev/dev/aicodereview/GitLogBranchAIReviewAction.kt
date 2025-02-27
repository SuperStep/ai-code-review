package gordeev.dev.aicodereview

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.vcs.log.VcsLogDataKeys
import git4idea.GitUtil

/**
 * Action that triggers AI code review when a branch is selected in Git Log view.
 * This will compare the current branch with the selected branch.
 */
class GitLogBranchAIReviewAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val repository = GitUtil.getRepositoryManager(project).repositories.firstOrNull() ?: return

        // Get the selected branch from Git log
        val selectedRefs = e.getData(VcsLogDataKeys.VCS_LOG_BRANCHES) ?: return
        if (selectedRefs.isEmpty()) return

        val targetBranch = selectedRefs.first().name

        // Get current branch as source branch
        val currentBranch = repository.currentBranch?.name ?: return

        // Use the new DiffReviewService to perform the review
        val reviewService = DiffReviewService()
        reviewService.performReview(project, currentBranch, targetBranch)
    }

    override fun update(e: AnActionEvent) {
        // Only enable the action if we have a project and branch selection
        val project = e.getData(CommonDataKeys.PROJECT)
        val vcsLog = e.getData(VcsLogDataKeys.VCS_LOG)
        val selectedRefs = e.getData(VcsLogDataKeys.VCS_LOG_BRANCHES)

        // Check if any branches are selected
        val branchesSelected = selectedRefs != null && selectedRefs.isNotEmpty()

        e.presentation.isEnabledAndVisible = project != null && vcsLog != null && branchesSelected
    }
}