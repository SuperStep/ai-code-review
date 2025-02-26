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
    var includeRepositoryContext: Boolean = false // Add the new setting, default to false
    var userMessage: String = ""
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