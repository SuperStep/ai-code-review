package gordeev.dev.aicodereview.settings

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.swing.*
import com.intellij.ui.components.JBTabbedPane
import java.awt.BorderLayout

class AppSettingsConfigurable : Configurable {

    private val settings = AppSettingsState.instance

    // Ollama fields
    private lateinit var ollamaUrlField: JBTextField
    private lateinit var ollamaModelComboBox: ComboBox<String>
    private lateinit var fetchModelsButton: JButton

    // Gemini fields
    private lateinit var geminiTokenField: JBTextField

    // TogetherAI fields
    private lateinit var togetherApiKeyField: JBTextField
    private lateinit var togetherAiModelField: JBTextField

    // Common fields
    private lateinit var includeRepositoryContextCheckbox: JBCheckBox
    private lateinit var userMessageTextArea: JBTextArea

    // Bitbucket fields
    private lateinit var bitbucketHostnameField: JBTextField
    private lateinit var bitbucketTokenField: JBTextField
    private lateinit var bitbucketWorkspaceField: JBTextField
    private lateinit var bitbucketRepoField: JBTextField
    private lateinit var bitbucketCertificatePathField: TextFieldWithBrowseButton

    private var mainTabbedPane: JBTabbedPane? = null
    private var aiModelTabbedPane: JBTabbedPane? = null
    private var myMainPanel: JPanel? = null

    override fun getDisplayName(): String = "AI Code Review"

    override fun createComponent(): JPanel {
        mainTabbedPane = JBTabbedPane()

        // Create main tabs
        val aiModelPanel = createAiModelPanel()
        val bitbucketPanel = createBitbucketPanel()

        mainTabbedPane!!.addTab("AI Model", aiModelPanel)
        mainTabbedPane!!.addTab("Bitbucket", bitbucketPanel)

        myMainPanel = JPanel(BorderLayout())
        myMainPanel!!.add(mainTabbedPane!!, BorderLayout.CENTER)

        return myMainPanel!!
    }

    private fun createAiModelPanel(): JPanel {
        // Create tabbed pane for AI model providers
        aiModelTabbedPane = JBTabbedPane()

        // Create tabs
        val generalPanel = createGeneralPanel()
        val ollamaPanel = createOllamaPanel()
        val geminiPanel = createGeminiPanel()
        val togetherAiPanel = createTogetherAiPanel()

        // Add tabs in order
        aiModelTabbedPane!!.addTab("General", generalPanel)
        aiModelTabbedPane!!.addTab("Ollama", ollamaPanel)
        aiModelTabbedPane!!.addTab("Gemini", geminiPanel)
        aiModelTabbedPane!!.addTab("TogetherAI", togetherAiPanel)

        // Select the correct tab based on current settings
        when (settings.modelProvider) {
            AppSettingsState.ModelProvider.OLLAMA -> aiModelTabbedPane!!.selectedIndex = 1
            AppSettingsState.ModelProvider.GEMINI -> aiModelTabbedPane!!.selectedIndex = 2
            AppSettingsState.ModelProvider.TOGETHER_AI -> aiModelTabbedPane!!.selectedIndex = 3
        }

        // Assemble the main AI model panel
        val panel = JPanel(BorderLayout())
        panel.add(aiModelTabbedPane!!, BorderLayout.CENTER)

        return panel
    }

    private fun createGeneralPanel(): JPanel {
        // Initialize common fields
        includeRepositoryContextCheckbox = JBCheckBox("Include repository context", settings.includeRepositoryContext)
        userMessageTextArea = JBTextArea(settings.userMessage, 5, 30)

        return FormBuilder.createFormBuilder()
            .addComponent(includeRepositoryContextCheckbox)
            .addLabeledComponent(JBLabel("User Message: "), JScrollPane(userMessageTextArea), 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun createOllamaPanel(): JPanel {
        ollamaUrlField = JBTextField(settings.ollamaUrl)
        ollamaModelComboBox = ComboBox()
        fetchModelsButton = JButton("Fetch Models")

        fetchModelsButton.addActionListener { fetchOllamaModels() }

        // Populate the model combo box if settings are available
        if (settings.ollamaUrl.isNotBlank() && settings.modelProvider == AppSettingsState.ModelProvider.OLLAMA) {
            fetchOllamaModels()
        }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Ollama URL: "), ollamaUrlField, 1, false)
            .addLabeledComponent(JBLabel("Ollama Model: "), ollamaModelComboBox, 1, false)
            .addComponent(fetchModelsButton)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun createGeminiPanel(): JPanel {
        geminiTokenField = JBTextField(settings.geminiToken)

        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Gemini Token: "), geminiTokenField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun createTogetherAiPanel(): JPanel {
        togetherApiKeyField = JBTextField(settings.togetherApiKey)
        togetherAiModelField = JBTextField(settings.togetherAiModel) // Initialize the new field

        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("TogetherAI API Key: "), togetherApiKeyField, 1, false)
            .addLabeledComponent(JBLabel("TogetherAI Model: "), togetherAiModelField, 1, false) // Add to the form
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun createBitbucketPanel(): JPanel {
        bitbucketHostnameField = JBTextField(settings.bitbucketHostname)
        bitbucketTokenField = JBTextField(settings.bitbucketToken)
        bitbucketWorkspaceField = JBTextField(settings.bitbucketWorkspace)
        bitbucketRepoField = JBTextField(settings.bitbucketRepo)

        // Add certificate path field with file browser
        bitbucketCertificatePathField = TextFieldWithBrowseButton()
        bitbucketCertificatePathField.text = settings.bitbucketCertificatePath
        bitbucketCertificatePathField.addBrowseFolderListener(
            TextBrowseFolderListener(
                FileChooserDescriptorFactory.createSingleFileDescriptor("crt;pem;cer")
            )
        )

        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Bitbucket Hostname: "), bitbucketHostnameField, 1, false)
            .addLabeledComponent(JBLabel("Bitbucket Token: "), bitbucketTokenField, 1, false)
            .addLabeledComponent(JBLabel("Bitbucket Workspace: "), bitbucketWorkspaceField, 1, false)
            .addLabeledComponent(JBLabel("Bitbucket Repository: "), bitbucketRepoField, 1, false)
            .addLabeledComponent(JBLabel("Certificate Path: "), bitbucketCertificatePathField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        return ollamaUrlField.text != settings.ollamaUrl ||
                (ollamaModelComboBox.selectedItem as? String ?: "") != settings.ollamaModel ||
                geminiTokenField.text != settings.geminiToken ||
                togetherApiKeyField.text != settings.togetherApiKey ||
                togetherAiModelField.text != settings.togetherAiModel || // Check the new field
                getSelectedProvider() != settings.modelProvider ||
                includeRepositoryContextCheckbox.isSelected != settings.includeRepositoryContext ||
                userMessageTextArea.text != settings.userMessage ||
                bitbucketHostnameField.text != settings.bitbucketHostname ||
                bitbucketTokenField.text != settings.bitbucketToken ||
                bitbucketWorkspaceField.text != settings.bitbucketWorkspace ||
                bitbucketRepoField.text != settings.bitbucketRepo ||
                bitbucketCertificatePathField.text != settings.bitbucketCertificatePath
    }

    override fun apply() {
        settings.ollamaUrl = ollamaUrlField.text
        settings.ollamaModel = ollamaModelComboBox.selectedItem as? String ?: ""
        settings.geminiToken = geminiTokenField.text
        settings.togetherApiKey = togetherApiKeyField.text
        settings.togetherAiModel = togetherAiModelField.text // Save the new field
        settings.modelProvider = getSelectedProvider()
        settings.includeRepositoryContext = includeRepositoryContextCheckbox.isSelected
        settings.userMessage = userMessageTextArea.text

        // Save Bitbucket settings
        settings.bitbucketHostname = bitbucketHostnameField.text
        settings.bitbucketToken = bitbucketTokenField.text
        settings.bitbucketWorkspace = bitbucketWorkspaceField.text
        settings.bitbucketRepo = bitbucketRepoField.text
        settings.bitbucketCertificatePath = bitbucketCertificatePathField.text
    }

    override fun reset() {
        // Reset AI provider selection by selecting the appropriate tab
        when (settings.modelProvider) {
            AppSettingsState.ModelProvider.OLLAMA -> aiModelTabbedPane?.selectedIndex = 1
            AppSettingsState.ModelProvider.GEMINI -> aiModelTabbedPane?.selectedIndex = 2
            AppSettingsState.ModelProvider.TOGETHER_AI -> aiModelTabbedPane?.selectedIndex = 3
        }

        // Reset Ollama fields
        ollamaUrlField.text = settings.ollamaUrl
        if (settings.ollamaModel.isNotEmpty() && (0 until ollamaModelComboBox.itemCount).any {
                ollamaModelComboBox.getItemAt(it) == settings.ollamaModel
            }) {
            ollamaModelComboBox.selectedItem = settings.ollamaModel
        }

        // Reset other provider fields
        geminiTokenField.text = settings.geminiToken
        togetherApiKeyField.text = settings.togetherApiKey
        togetherAiModelField.text = settings.togetherAiModel // Reset the new field

        // Reset common fields
        includeRepositoryContextCheckbox.isSelected = settings.includeRepositoryContext
        userMessageTextArea.text = settings.userMessage

        // Reset Bitbucket fields
        bitbucketHostnameField.text = settings.bitbucketHostname
        bitbucketTokenField.text = settings.bitbucketToken
        bitbucketWorkspaceField.text = settings.bitbucketWorkspace
        bitbucketRepoField.text = settings.bitbucketRepo
        bitbucketCertificatePathField.text = settings.bitbucketCertificatePath
    }

    override fun disposeUIResources() {
        myMainPanel = null
        mainTabbedPane = null
        aiModelTabbedPane = null
    }

    private fun getSelectedProvider(): AppSettingsState.ModelProvider {
        return when (aiModelTabbedPane?.selectedIndex) {
            1 -> AppSettingsState.ModelProvider.OLLAMA
            2 -> AppSettingsState.ModelProvider.GEMINI
            3 -> AppSettingsState.ModelProvider.TOGETHER_AI
            else -> AppSettingsState.ModelProvider.OLLAMA // Default
        }
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
                ollamaModelComboBox.removeAllItems() // Clear existing items
                modelNames.forEach { ollamaModelComboBox.addItem(it) }

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
