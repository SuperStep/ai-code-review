package gordeev.dev.aicodereview.bitbucket

import com.google.gson.Gson
import gordeev.dev.aicodereview.settings.AppSettingsState
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class BitbucketService {
    private val settings = AppSettingsState.instance
    private val gson = Gson()

    /**
     * Creates a pull request in Bitbucket
     *
     * @param sourceBranch The source branch name
     * @param targetBranch The target branch name
     * @param title The title for the pull request (optional)
     * @param description The description for the pull request (optional)
     * @return The created pull request data or null if creation failed
     */
    fun createPullRequest(
        sourceBranch: String,
        targetBranch: String,
        title: String = "Pull request from $sourceBranch to $targetBranch",
        description: String = ""
    ): PullRequestResponse? {

        val baseUrl = "${settings.bitbucketHostname}/rest/api/1.0/projects/${settings.bitbucketWorkspace}/repos/${settings.bitbucketRepo}/pull-requests"

        val requestBody = gson.toJson(mapOf(
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
        ))

        // Create HTTP client with SSL context based on settings
        val client = createHttpClient()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${settings.bitbucketToken}")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..299) {
                gson.fromJson(response.body(), PullRequestResponse::class.java)
            } else {
                println("Error creating pull request: ${response.statusCode()} - ${response.body()}")
                null
            }
        } catch (e: Exception) {
            println("Exception creating pull request: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates an HTTP client with custom SSL context based on settings
     */
    private fun createHttpClient(): HttpClient {
        // If verification should be disabled, create a trust-all SSL context
        if (settings.bitbucketDisableCertVerification) {
            try {
                // Create a trust manager that does not validate certificate chains
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                })
                // Install the all-trusting trust manager
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())

                // Create an HttpClient that trusts all certificates
                return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build()
            } catch (e: Exception) {
                println("Error creating trust-all SSL context: ${e.message}")
                e.printStackTrace()
                return HttpClient.newHttpClient()
            }
        }
        // If a custom certificate path is provided, use it
        else if (settings.bitbucketCertificatePath.isNotBlank()) {
            try {
                val certificateFile = File(settings.bitbucketCertificatePath)
                if (certificateFile.exists()) {
                    // Create a KeyStore containing our trusted CAs
                    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                    keyStore.load(null, null)

                    // Load the certificate
                    val certificateFactory = CertificateFactory.getInstance("X.509")
                    val certificate = FileInputStream(certificateFile).use { fis ->
                        certificateFactory.generateCertificate(fis) as X509Certificate
                    }

                    // Add certificate to keystore
                    keyStore.setCertificateEntry("bitbucket-cert", certificate)

                    // Create a TrustManager that trusts the CAs in our KeyStore
                    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    tmf.init(keyStore)

                    // Create an SSLContext that uses our TrustManager
                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(null, tmf.trustManagers, null)

                    // Create HttpClient with our SSLContext
                    return HttpClient.newBuilder()
                        .sslContext(sslContext)
                        .build()
                } else {
                    println("Certificate file not found: ${settings.bitbucketCertificatePath}")
                    return HttpClient.newHttpClient()
                }
            } catch (e: Exception) {
                println("Error setting up SSL context with certificate: ${e.message}")
                e.printStackTrace()
                return HttpClient.newHttpClient()
            }
        }
        // Otherwise, use the default HTTP client
        else {
            return HttpClient.newHttpClient()
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