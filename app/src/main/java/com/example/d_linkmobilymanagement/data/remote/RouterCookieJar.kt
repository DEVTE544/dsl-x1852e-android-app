package com.example.d_linkmobilymanagement.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class RouterCookieJar : CookieJar {

    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val current = store[host]?.toMutableList() ?: mutableListOf()

        cookies.forEach { newCookie ->
            current.removeAll { oldCookie ->
                oldCookie.name == newCookie.name &&
                    oldCookie.domain == newCookie.domain &&
                    oldCookie.path == newCookie.path
            }
            current.add(newCookie)
        }

        store[host] = current
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val now = System.currentTimeMillis()

        val validCookies = (store[host] ?: mutableListOf())
            .filter { cookie ->
                cookie.expiresAt >= now && cookie.matches(url)
            }
            .toMutableList()

        store[host] = validCookies
        return validCookies
    }

    fun clear() {
        store.clear()
    }
}
