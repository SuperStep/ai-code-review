package gordeev.dev.aicodereview.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
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
    var userMessage: String = "Please review the following code changes:" // Add user message

    override fun getState(): AppSettingsState {
        return this
    }

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: AppSettingsState
            get() = ServiceManager.getService(AppSettingsState::class.java)
    }
}