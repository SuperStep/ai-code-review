package gordeev.dev.aicodereview.provider

interface ContexProvider {
    fun getContext(query: String): List<String>
}