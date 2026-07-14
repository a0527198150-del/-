import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

fun main() {
    val client = OkHttpClient()
    val apiKey = "AIzaSyBF4WVQIUDNgwbUsViQ_RiRzNzdP6KUG50"
    val channelId = "UCzqiIOm_TUH0lG3pwlbMkyA" // Hidabroot
    val uploadsPlaylistId = "UU" + channelId.substring(2)
    val url = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=30&playlistId=$uploadsPlaylistId&key=$apiKey"
    val request = Request.Builder().url(url).build()
    client.newCall(request).execute().use { response ->
        println("Response code: ${response.code}")
        val body = response.body?.string()
        println("Body length: ${body?.length}")
    }
}
