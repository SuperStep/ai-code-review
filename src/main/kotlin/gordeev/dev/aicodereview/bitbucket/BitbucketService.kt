package gordeev.dev.aicodereview.bitbucket

import com.google.gson.Gson
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import gordeev.dev.aicodereview.NotificationUtil
import gordeev.dev.aicodereview.settings.AppSettingsState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import javax.net.ssl.TrustManager

class BitbucketService {
    private val settings = AppSettingsState.instance
    private val gson = Gson()
    private val mediaTypeJson = "application/json".toMediaType()

    /**
     * Creates a pull request in Bitbucket
     *
     * @param project The IntelliJ project
     * @param sourceBranch The source branch name
     * @param targetBranch The target branch name
     * @param title The title for the pull request (optional)
     * @param description The description for the pull request (optional)
     * @return The created pull request data or null if creation failed
     */
    fun createPullRequest(
        project: Project,
        sourceBranch: String,
        targetBranch: String,
        title: String = "Pull request from $sourceBranch to $targetBranch",
        description: String = ""
    ): PullRequestResponse? {

        val baseUrl = "${settings.bitbucketHostname}/rest/api/1.0/projects/${settings.bitbucketWorkspace}/repos/${settings.bitbucketRepo}/pull-requests"

        val requestBody = gson.toJson(
            mapOf(
                "title" to title,
                "description" to description,
                "state" to "OPEN",
                "open" to true,
                "closed" to false,
                "fromRef" to mapOf(
                    "id" to "refs/heads/$sourceBranch",
                    "repository" to mapOf(
                        "slug" to settings.bitbucketRepo,
                        "project" to mapOf(
                            "key" to settings.bitbucketWorkspace
                        )
                    )
                ),
                "toRef" to mapOf(
                    "id" to "refs/heads/$targetBranch",
                    "repository" to mapOf(
                        "slug" to settings.bitbucketRepo,
                        "project" to mapOf(
                            "key" to settings.bitbucketWorkspace
                        )
                    )
                )
            )
        )

        // Create HTTP client with SSL context based on settings
        try {
            val client = createOkHttpClient()

            val request = Request.Builder()
                .url(baseUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer ${settings.bitbucketToken}")
                .post(requestBody.toRequestBody(mediaTypeJson))
                .build()

            return client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let {
                        val prResponse = gson.fromJson(it, PullRequestResponse::class.java)
                        NotificationUtil.showNotificationWithUrl(
                            project = project,
                            message = "Pull request #${prResponse.id} created successfully",
                            title = "Pull Request Created",
                            linkText =  prResponse.getWebUrl()?: "",
                            url = prResponse.getWebUrl()?: ""
                        )
                        prResponse
                    }
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    NotificationUtil.showErrorNotification(
                        project,
                        "Error creating pull request: ${response.code} - $errorBody"
                    )
                    null
                }
            }
        } catch (e: IOException) {
            NotificationUtil.showErrorNotification(
                project,
                "Failed to create pull request: ${e.message}"
            )
            e.printStackTrace()
            return null
        } catch (e: Exception) {
            NotificationUtil.showErrorNotification(
                project,
                "Unexpected error creating pull request: ${e.message}"
            )
            e.printStackTrace()
            return null
        }
    }

    /**
     * Creates an OkHttp client with custom SSL context based on settings
     */
    private fun createOkHttpClient(): OkHttpClient {
        try {
            val keyStoreFile = File(settings.bitbucketCertificatePath)
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(FileInputStream(keyStoreFile), settings.keystorePassword.toCharArray())

            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, settings.keystorePassword.toCharArray())

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagerFactory.keyManagers, null, null)

            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, getTrustManager())
                .build()
        } catch (e: Exception) {
            // We can't show notifications here since we don't have a Project reference
            // The exception will be caught in the calling method
            throw IOException("Failed to create HTTP client: ${e.message}", e)
        }
    }

    private fun getTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    /**
     * Class representing a Bitbucket pull request response
     */
    data class PullRequestResponse(
        val id: Int,
        val version: Int,
        val title: String,
        val description: String,
        val state: String,
        val open: Boolean,
        val closed: Boolean,
        val createdDate: Long,
        val updatedDate: Long,
        val links: Map<String, List<Map<String, String>>>
    ) {
        /**
         * Get the URL to view this pull request in the web interface
         */
        fun getWebUrl(): String? {
            return links["self"]?.firstOrNull()?.get("href")
        }
    }

    companion object {
        /**
         * Get an instance of the BitbucketService
         */
        fun getInstance(): BitbucketService {
            return BitbucketService()
        }
    }
}
