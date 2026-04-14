package com.example.d_linkmobilymanagement.data.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class RouterHttpClient(
    private val cookieJar: RouterCookieJar = RouterCookieJar(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        private const val REQUEST_TIMEOUT_SECONDS = 30L
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:149.0) Gecko/20100101 Firefox/149.0"
        private const val ACCEPT_LANGUAGE = "ar,en-US;q=0.9,en;q=0.8"
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    suspend fun login(routerIp: String, username: String, password: String): Boolean {
        return withContext(ioDispatcher) {
            val baseUrl = baseUrl(routerIp)
            val url = "$baseUrl/cgi-bin/Login.asp".toHttpUrl().newBuilder()
                .addQueryParameter("User", username.trim())
                .addQueryParameter("Pwd", md5(password))
                .addQueryParameter("_", timestamp())
                .build()

            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", ACCEPT_LANGUAGE)
                .header("Accept", "*/*")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "$baseUrl/cgi-bin/Login.asp")
                .build()

            executeText(request).trim() == "1"
        }
    }

    suspend fun getSessionKey(routerIp: String): String {
        return withContext(ioDispatcher) {
            val baseUrl = baseUrl(routerIp)
            val url = "$baseUrl/cgi-bin/get/New_GUI/get_sessionKey.asp".toHttpUrl().newBuilder()
                .addQueryParameter("_", timestamp())
                .build()

            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", ACCEPT_LANGUAGE)
                .header("Accept", "*/*")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "$baseUrl/cgi-bin/New_GUI/wifiFilter.asp")
                .build()

            val text = executeText(request).trim()
            Regex("(\\d+)").find(text)?.groupValues?.get(1)
                ?: throw IllegalStateException("Unable to read session key.")
        }
    }

    suspend fun fetchConnectedDevicesRaw(routerIp: String): String {
        return getWithRetry(
            routerIp = routerIp,
            path = "/cgi-bin/get/New_GUI/home_getclientList.asp",
            refererPath = "/cgi-bin/New_GUI/Home.asp",
            accept = "text/javascript, application/javascript, */*; q=0.01"
        )
    }

    suspend fun fetchWifiFilterPageRaw(routerIp: String): String {
        return getWithRetry(
            routerIp = routerIp,
            path = "/cgi-bin/New_GUI/wifiFilter.asp",
            refererPath = "cgi-bin/New_GUI/WiFi.asp",
            accept = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        )
    }

    suspend fun fetchBlockListsRaw(routerIp: String): String {
        return getWithRetry(
            routerIp = routerIp,
            path = "/cgi-bin/get/New_GUI/wifi_getmaclist.asp",
            refererPath = "/cgi-bin/New_GUI/wifiFilter.asp",
            accept = "*/*"
        )
    }

    suspend fun fetchLoginPageRaw(routerIp: String): String {
        return getWithRetry(
            routerIp = routerIp,
            path = "/cgi-bin/Login.asp",
            refererPath = "", // يمكن تركها فارغة أو "http://$routerIp/"
            accept = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        )
    }

    suspend fun submitWifiFilter(
        routerIp: String,
        payload: List<Pair<String, String>>
    ): String {
        return withContext(ioDispatcher) {
            val baseUrl = baseUrl(routerIp)
            val url = "$baseUrl/cgi-bin/New_GUI/wifiFilter.asp"

            val formBody = FormBody.Builder()
            payload.forEach { (key, value) ->
                formBody.add(key, value)
            }

            val request = Request.Builder()
                .url(url)
                .post(formBody.build())
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", ACCEPT_LANGUAGE)
                .header("Accept", "*/*")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Origin", baseUrl)
                .header("Referer", "$baseUrl/cgi-bin/New_GUI/wifiFilter.asp")
                .build()

            executeText(request)
        }
    }

    fun clearSession() {
        cookieJar.clear()
    }

    private suspend fun getWithRetry(
        routerIp: String,
        path: String,
        refererPath: String,
        accept: String,
        maxAttempts: Int = 3
    ): String {
        return withContext(ioDispatcher) {
            val baseUrl = baseUrl(routerIp)
            var lastError: Throwable? = null

            repeat(maxAttempts) { attempt ->
                try {
                    val url = "$baseUrl$path".toHttpUrl().newBuilder()
                        .addQueryParameter("_", timestamp())
                        .build()

                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .header("User-Agent", USER_AGENT)
                        .header("Accept-Language", ACCEPT_LANGUAGE)
                        .header("Accept", accept)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Referer", "$baseUrl$refererPath")
                        .build()

                    return@withContext executeText(request)
                } catch (e: Throwable) {
                    lastError = e
                    if (attempt < maxAttempts - 1) {
                        delay(1500L + (attempt * 1000L))
                    }
                }
            }

            throw lastError ?: IOException("Unknown network error.")
        }
    }

    private fun executeText(request: Request): String {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${response.message}")
            }
            return response.body?.string().orEmpty()
        }
    }

    private fun baseUrl(routerIp: String): String {
        val trimmed = routerIp.trim().removeSuffix("/")
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "http://$trimmed"
        }
    }

    suspend fun getInternetStatus(routerIp: String): String {
        return withContext(ioDispatcher) {
            val baseUrl = baseUrl(routerIp)
            val url = "$baseUrl/cgi-bin/get/New_GUI/home_internet_status.asp".toHttpUrl().newBuilder()
                .addQueryParameter("_", timestamp())
                .build()

            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/javascript, application/javascript, application/ecmascript, application/x-ecmascript, */*; q=0.01")
                .header("Accept-Language", ACCEPT_LANGUAGE)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Sec-GPC", "1")
                .header("Referer", "$baseUrl/cgi-bin/New_GUI/Home.asp")
                .build()

            executeText(request)
        }
    }

    private fun timestamp(): String = System.currentTimeMillis().toString()

    private fun md5(value: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
