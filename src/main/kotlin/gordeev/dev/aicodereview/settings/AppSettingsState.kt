package gordeev.dev.aicodereview.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "gordeev.dev.aicodereview.settings.AppSettingsState",
    storages = [Storage("AiCodeReviewSettings.xml")]
)
class AppSettingsState : PersistentStateComponent<AppSettingsState> {

    enum class ModelProvider {
        OLLAMA,
        GEMINI
    }
    var modelProvider: ModelProvider = ModelProvider.OLLAMA // Default to Ollama
    var ollamaUrl: String = "http://localhost:11434/api/generate"
    var ollamaModel: String = ""
    var geminiToken: String = ""
    var includeRepositoryContext: Boolean = false
    var userMessage: String = ""

    // Bitbucket settings
    var bitbucketHostname: String = ""
    var bitbucketToken: String = ""
    var bitbucketWorkspace: String = ""
    var bitbucketRepo: String = ""
    var bitbucketCertificatePath: String = """
    Please review and analyze the code below and identify potential areas for improvement 
    related to code smells, readability, maintainability, performance, security, etc. 
    For each suggestion, provide a brief explanation of the potential benefits.
    Provide filename, method name you are suggesting to improve.
    Provide examples if there code that need to be changed.
         
    After listing any recommendations, summarize if you found notable opportunities to enhance the code quality overall or if the code generally follows sound design principles. 
    If no issues found, reply "There are no errors."    
        
    Here are the code for you review:
    """.trimIndent()
    var bitbucketDisableCertVerification: Boolean = true // Added option to disable cert verification

    override fun getState(): AppSettingsState {
        return this
    }

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: AppSettingsState
            get() = ApplicationManager.getApplication().getService(AppSettingsState::class.java)
    }
}
