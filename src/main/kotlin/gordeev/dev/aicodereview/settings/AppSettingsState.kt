package gordeev.dev.aicodereview.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Persistent application settings state
 */
@State(
    name = "gordeev.dev.aicodereview.settings.AppSettingsState",
    storages = [Storage("AICodeReviewSettings.xml")]
)
class AppSettingsState : PersistentStateComponent<AppSettingsState> {

    enum class ModelProvider(val code: String) {
        OLLAMA("Ollama"),
        GEMINI("Gemini"),
        TOGETHER_AI("Together AI")
    }
    var modelProvider: ModelProvider = ModelProvider.OLLAMA
    var ollamaUrl: String = "http://localhost:11434/api/generate"
    var ollamaModel: String = ""
    var geminiToken: String = ""
    var togetherApiKey: String = ""
    var togetherAiModel: String = "meta-llama/Llama-Vision-Free"
    var includeRepositoryContext: Boolean = false
    var userMessage: String = """
    Please review and analyze the code below and identify 
    potential areas for improvement 
    related to code smells, readability, maintainability, performance, security, etc. 
    For each suggestion, provide a brief explanation of the potential benefits.
    Provide filename, method name you are suggesting to improve.
    Provide examples if there code that need to be changed.
         
    After listing any recommendations, summarize
    if you found notable opportunities to enhance the code quality overall 
    or if the code generally follows sound design principles. 
    If no issues found, reply "There are no errors."    
        
    Here is a git diff of the changes:
    """.trimIndent()

    // Bitbucket settings
    var bitbucketHostname: String = ""
    var bitbucketToken: String = ""
    var bitbucketWorkspace: String = ""
    var bitbucketRepo: String = ""
    var bitbucketCertificatePath: String = ""
    var keystorePassword: String = ""

    // RAG Database settings
    var dbHost: String? = null
    var dbPort: Int? = null
    var dbName: String? = null
    var dbUsername: String? = null
    var dbPassword: String? = null
    var ragRequestStatement: String? = "SELECT path, chunk FROM find_rag_content(?)"


    override fun getState(): AppSettingsState = this

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: AppSettingsState
            get() = ApplicationManager.getApplication().getService(AppSettingsState::class.java)
    }
}