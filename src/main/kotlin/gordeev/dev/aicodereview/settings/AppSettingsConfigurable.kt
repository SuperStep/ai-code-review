package gordeev.dev.aicodereview.settings

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.event.ActionEvent
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.swing.*

class AppSettingsConfigurable : Configurable {

    private val settings = AppSettingsState.instance

    private lateinit var ollamaUrlField: JBTextField
    private lateinit var ollamaModelComboBox: JComboBox<String>
    private lateinit var fetchModelsButton: JButton
    private lateinit var geminiTokenField: JBTextField
    private lateinit var ollamaRadioButton: JBRadioButton
    private lateinit var geminiRadioButton: JBRadioButton
    private lateinit var ollamaUrlLabel: JBLabel
    private lateinit var ollamaModelLabel: JBLabel
    private lateinit var geminiTokenLabel: JBLabel
    private lateinit var includeRepositoryContextCheckbox: JBCheckBox
    private lateinit var userMessageTextArea: JBTextArea
    private var myMainPanel: JPanel? = null


    override fun getDisplayName(): String = "AI Code Review"

    override fun createComponent(): JPanel {
        ollamaUrlField = JBTextField(settings.ollamaUrl)
        ollamaModelComboBox = JComboBox()
        fetchModelsButton = JButton("Fetch Models")
        geminiTokenField = JBTextField(settings.geminiToken)
        ollamaRadioButton = JBRadioButton("Ollama", settings.modelProvider == AppSettingsState.ModelProvider.OLLAMA)
        geminiRadioButton = JBRadioButton("Gemini", settings.modelProvider == AppSettingsState.ModelProvider.GEMINI)
        ollamaUrlLabel = JBLabel("Ollama URL: ")
        ollamaModelLabel = JBLabel("Ollama Model: ")
        geminiTokenLabel = JBLabel("Gemini Token: ")
        includeRepositoryContextCheckbox = JBCheckBox("Include repository context", settings.includeRepositoryContext)
        userMessageTextArea = JBTextArea(settings.userMessage, 5, 30)

        val buttonGroup = ButtonGroup()
        buttonGroup.add(ollamaRadioButton)
        buttonGroup.add(geminiRadioButton)

        fetchModelsButton.addActionListener { fetchOllamaModels() }
        ollamaRadioButton.addActionListener { updateComponentsVisibility() }
        geminiRadioButton.addActionListener { updateComponentsVisibility() }

        myMainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Model Provider: "), JPanel().apply {
                add(ollamaRadioButton)
                add(geminiRadioButton)
            })
            .addLabeledComponent(ollamaUrlLabel, ollamaUrlField, 1, false)
            .addLabeledComponent(ollamaModelLabel, ollamaModelComboBox, 1, false)
            .addComponent(fetchModelsButton)
            .addLabeledComponent(geminiTokenLabel, geminiTokenField, 1, false)
            .addComponent(includeRepositoryContextCheckbox)
            .addLabeledComponent(JBLabel("User Message: "), userMessageTextArea, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        updateComponentsVisibility()
        if (settings.modelProvider == AppSettingsState.ModelProvider.OLLAMA && settings.ollamaUrl.isNotBlank() && ollamaModelComboBox.model.size == 0) {
            fetchOllamaModels()
        }

        return myMainPanel!!
    }

    override fun isModified(): Boolean {
        return ollamaUrlField.text != settings.ollamaUrl ||
                (ollamaModelComboBox.selectedItem as? String ?: "") != settings.ollamaModel ||
                geminiTokenField.text != settings.geminiToken ||
                getSelectedProvider() != settings.modelProvider ||
                includeRepositoryContextCheckbox.isSelected != settings.includeRepositoryContext ||
                userMessageTextArea.text != settings.userMessage
    }

    override fun apply() {
        settings.ollamaUrl = ollamaUrlField.text
        settings.ollamaModel = ollamaModelComboBox.selectedItem as? String ?: ""
        settings.geminiToken = geminiTokenField.text
        settings.modelProvider = getSelectedProvider()
        settings.includeRepositoryContext = includeRepositoryContextCheckbox.isSelected
        settings.userMessage = userMessageTextArea.text
    }

    override fun reset() {
        ollamaUrlField.text = settings.ollamaUrl
        if (settings.ollamaModel.isNotEmpty() && (0 until ollamaModelComboBox.itemCount).any { ollamaModelComboBox.getItemAt(it) == settings.ollamaModel }) {
            ollamaModelComboBox.selectedItem = settings.ollamaModel
        }
        geminiTokenField.text = settings.geminiToken
        when (settings.modelProvider) {
            AppSettingsState.ModelProvider.OLLAMA -> ollamaRadioButton.isSelected = true
            AppSettingsState.ModelProvider.GEMINI -> geminiRadioButton.isSelected = true
        }
        includeRepositoryContextCheckbox.isSelected = settings.includeRepositoryContext
        userMessageTextArea.text = settings.userMessage
        updateComponentsVisibility()
    }

    override fun disposeUIResources() {
        myMainPanel = null
    }

    private fun getSelectedProvider(): AppSettingsState.ModelProvider {
        return if (ollamaRadioButton.isSelected) {
            AppSettingsState.ModelProvider.OLLAMA
        } else {
            AppSettingsState.ModelProvider.GEMINI
        }
    }

    private fun updateComponentsVisibility() {
        val isOllamaSelected = ollamaRadioButton.isSelected

        ollamaUrlField.isVisible = isOllamaSelected
        ollamaModelComboBox.isVisible = isOllamaSelected
        fetchModelsButton.isVisible = isOllamaSelected
        geminiTokenField.isVisible = !isOllamaSelected

        ollamaUrlLabel.isVisible = isOllamaSelected
        ollamaModelLabel.isVisible = isOllamaSelected
        geminiTokenLabel.isVisible = !isOllamaSelected
    }

    private fun fetchOllamaModels() {
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(ollamaUrlField.text.replace("/api/generate", "/api/tags")))
            .build()

        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val body = response.body()
                val gson = Gson()
                val modelListType = object : TypeToken<ModelList>() {}.type
                val modelList: ModelList = gson.fromJson(body, modelListType)

                val modelNames = modelList.models.map { it.name }
                ollamaModelComboBox.model = DefaultComboBoxModel(modelNames.toTypedArray())
                if (settings.ollamaModel.isNotEmpty() && modelNames.contains(settings.ollamaModel)) {
                    ollamaModelComboBox.selectedItem = settings.ollamaModel
                }

            } else {
                // Handle error (e.g., show a notification)
                println("Error fetching models: ${response.statusCode()}") // Replace with proper error handling
            }
        } catch (e: Exception) {
            // Handle exception (e.g., show a notification)
            println("Exception during model fetch: ${e.message}") // Replace with proper error handling
        }
    }

    data class ModelList(val models: List<Model>)
    data class Model(val name: String, val modified_at: String, val size: Long)
}