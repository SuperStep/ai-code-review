# AI Code Review - IntelliJ IDEA Plugin

[![JetBrains Plugin](https://img.shields.io/jetbrains/plugin/v/gordeev.dev.aicodereview.svg)](https://plugins.jetbrains.com/plugin/YOUR_PLUGIN_ID)  <!-- Replace YOUR_PLUGIN_ID -->
[![Downloads](https://img.shields.io/jetbrains/plugin/d/gordeev.dev.aicodereview.svg)](https://plugins.jetbrains.com/plugin/YOUR_PLUGIN_ID)  <!-- Replace YOUR_PLUGIN_ID -->

## Overview

AI Code Review is a powerful IntelliJ IDEA plugin that brings AI-powered code review capabilities directly to your IDE.  It leverages local Large Language Models (LLMs), specifically Ollama and Google's Gemini, to provide intelligent suggestions and feedback on your code changes.  This plugin prioritizes privacy by processing code locally, ensuring no sensitive data leaves your machine.

This plugin is particularly useful for teams using Git, allowing you to analyze the differences between branches and get AI-powered reviews.

## Features

*   **Local LLM Integration:**  Performs code reviews using local LLMs (Ollama) or Google's Gemini, ensuring data privacy.
*   **Git Diff Analysis:**  Analyzes the differences between two selected Git branches.
*   **Configurable Model Provider:** Choose between Ollama and Gemini as the AI model provider.
*   **Ollama Support:**
    *   Configure the Ollama server URL.
    *   Select from available Ollama models (fetched dynamically).
*   **Gemini Support:**
    *   Provide your Gemini API token.
*   **Customizable User Message:**  Add a custom message to provide context to the AI model along with the code diff.
*   **Repository Context (Optional):**  Option to include additional repository context for improved AI analysis (configurable).
*   **Notifications:**  Displays notifications for successful reviews, errors, and configuration issues.

## Installation

1.  **From the JetBrains Marketplace:**
    *   Open IntelliJ IDEA.
    *   Go to **File > Settings > Plugins** (or **IntelliJ IDEA > Preferences > Plugins** on macOS).
    *   Select the **Marketplace** tab.
    *   Search for "AI Code Review".
    *   Click **Install**.
    *   Restart IntelliJ IDEA.

2.  **From Disk (if you have a .zip or .jar file):**
    *   Open IntelliJ IDEA.
    *   Go to **File > Settings > Plugins** (or **IntelliJ IDEA > Preferences > Plugins** on macOS).
    *   Click the gear icon (⚙️) at the top of the Plugins settings window.
    *   Select **Install Plugin from Disk...**.
    *   Choose the plugin's `.zip` or `.jar` file.
    *   Click **OK**.
    *   Restart IntelliJ IDEA.

## Prerequisites

*   **Java:** Ensure you have a compatible Java Development Kit (JDK) installed.  The `gradlew.bat` script suggests setting the `JAVA_HOME` environment variable.
*   **Git:**  The plugin uses Git for diff generation, so Git must be installed and accessible in your system's PATH.
*   **Ollama (if using Ollama):**
    *   Download and install Ollama from [https://ollama.ai/](https://ollama.ai/).
    *   Pull a suitable model (e.g., `ollama pull codellama:7b-code`).  The plugin's settings allow you to fetch available models.
*   **Gemini API Token (if using Gemini):**
    *   Obtain a Gemini API token from Google AI Studio ([https://ai.google.dev/](https://ai.google.dev/)).

## Usage

1.  **Configure the Plugin:**
    *   Go to **File > Settings > Tools > AI Code Review Settings** (or **IntelliJ IDEA > Preferences > Tools > AI Code Review Settings** on macOS).
    *   **Model Provider:** Select either "Ollama" or "Gemini".
    *   **Ollama Settings (if Ollama is selected):**
        *   **Ollama URL:**  Enter the URL of your Ollama server (default: `http://localhost:11434/api/generate`).
        *   **Ollama Model:** Click "Fetch Models" to retrieve a list of available models from your Ollama server, then select the desired model.
    *   **Gemini Settings (if Gemini is selected):**
        *   **Gemini Token:** Enter your Gemini API token.
    *   **Include repository context:** Check this box if you want to include additional context from your repository (currently, this feature is a placeholder and doesn't have specific functionality).
    * **User Message:**  Enter a custom message to provide context to the AI model (e.g., "Review this code for potential bugs and performance improvements.").
    *   Click **Apply** and **OK**.

2.  **Perform a Code Review:**
    *   In your IntelliJ IDEA project, open the **VCS** menu (or right-click in the editor).
    *   Select **Git** -> **Perform AI Code Review**.
    *   A dialog will appear, allowing you to select the **Source Branch** and **Target Branch** for comparison.
    *   Click **OK**.
    *   The plugin will generate the diff between the selected branches and send it to the configured AI model (along with the user message).
    *   The AI's review will be displayed in a notification.  Error messages will also be shown as notifications.

## Contributing

Contributions are welcome!  If you'd like to contribute to this project, please follow these steps:

1.  **Fork the repository.**
2.  **Create a new branch** for your feature or bug fix.
3.  **Make your changes.**
4.  **Write tests** for your changes (if applicable).
5.  **Run the tests** and ensure they pass.
6.  **Submit a pull request.**

Please ensure your code follows the project's coding style and conventions.

## Troubleshooting

*   **"Failed to get diff" error:**  Make sure Git is installed and accessible in your PATH.  Also, ensure that the selected branches exist in your repository.
*   **"Ollama API error" or "Error communicating with Ollama":**
    *   Verify that the Ollama server is running and accessible at the configured URL.
    *   Check that the selected Ollama model is available.
    *   Inspect the IntelliJ IDEA logs for more detailed error messages.
*   **"Gemini API error":**
    *   Verify that your Gemini API token is correct and valid.
    *   Check the Gemini API documentation for any rate limits or usage restrictions.
* **Plugin not showing up:**
    * Ensure that you have restarted IntelliJ IDEA after installing the plugin.
    * Check that the plugin is enabled in the Plugins settings.

## License

This project is licensed under the [MIT License](LICENSE) - see the LICENSE file for details. (You'll need to create a LICENSE file).

## Contact

For questions or support, please contact [mail@gordeev.dev](mailto:mail@gordeev.dev) or visit [https://gordeev.dev](https://gordeev.dev).
