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
import com.intellij.ui.components.JBRadioButton
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

    // AI Model fields
    private lateinit var ollamaUrlField: JBTextField
    private lateinit var ollamaModelComboBox: ComboBox<String>
    private lateinit var fetchModelsButton: JButton
    private lateinit var geminiTokenField: JBTextField
    private lateinit var ollamaRadioButton: JBRadioButton
    private lateinit var geminiRadioButton: JBRadioButton
    private lateinit var ollamaUrlLabel: JBLabel
    private lateinit var ollamaModelLabel: JBLabel
    private lateinit var geminiTokenLabel: JBLabel
    private lateinit var includeRepositoryContextCheckbox: JBCheckBox
    private lateinit var userMessageTextArea: JBTextArea

    // Bitbucket fields
    private lateinit var bitbucketHostnameField: JBTextField
    private lateinit var bitbucketTokenField: JBTextField
    private lateinit var bitbucketWorkspaceField: JBTextField
    private lateinit var bitbucketRepoField: JBTextField
    private lateinit var bitbucketCertificatePathField: TextFieldWithBrowseButton

    private var myMainPanel: JPanel? = null
    private var tabbedPane: JBTabbedPane? = null

    override fun getDisplayName(): String = "AI Code Review"

    override fun createComponent(): JPanel {

        tabbedPane = JBTabbedPane()

        // Create AI Model tab
        val aiModelPanel = createAiModelPanel()

        // Create Bitbucket tab
        val bitbucketPanel = createBitbucketPanel()

        // Add tabs
        tabbedPane!!.addTab("AI Model", aiModelPanel)
        tabbedPane!!.addTab("Bitbucket", bitbucketPanel)

        myMainPanel = JPanel(BorderLayout()) // Use BorderLayout
        val tabWrapper = JPanel(BorderLayout()) // Wrap tabbedPane
        tabWrapper.add(tabbedPane!!, BorderLayout.NORTH) // Add tabbedPane to wrapper
        myMainPanel!!.add(tabWrapper, BorderLayout.NORTH) // Add the wrapper to the main panel

        return myMainPanel!!
    }

    private fun createAiModelPanel(): JPanel {
        ollamaUrlField = JBTextField(settings.ollamaUrl)
        ollamaModelComboBox = ComboBox()
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

        val panel = FormBuilder.createFormBuilder()
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
        if (settings.modelProvider == AppSettingsState.ModelProvider.OLLAMA && settings.ollamaUrl.isNotBlank() && ollamaModelComboBox.itemCount == 0) {
            fetchOllamaModels()
        }

        return panel
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

        // Reset Bitbucket fields
        bitbucketHostnameField.text = settings.bitbucketHostname
        bitbucketTokenField.text = settings.bitbucketToken
        bitbucketWorkspaceField.text = settings.bitbucketWorkspace
        bitbucketRepoField.text = settings.bitbucketRepo
        bitbucketCertificatePathField.text = settings.bitbucketCertificatePath

        updateComponentsVisibility()
    }

    override fun disposeUIResources() {
        myMainPanel = null
        tabbedPane = null
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
