package no.andersius.infoscreen

import android.content.Context
import android.webkit.URLUtil

object FeedPrefs {
    const val DEFAULT_URL = "https://infoscreen-1bc.pages.dev"

    private const val FILE = "infoscreen"
    private const val KEY_URL = "feed_url"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isSet(context: Context): Boolean = prefs(context).contains(KEY_URL)

    fun url(context: Context): String =
        prefs(context).getString(KEY_URL, null) ?: DEFAULT_URL

    fun setUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_URL, url).apply()
    }

    /** Trims, adds https:// if no scheme was typed, and returns null if it still isn't an http(s) URL. */
    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
        return withScheme.takeIf { URLUtil.isHttpUrl(it) || URLUtil.isHttpsUrl(it) }
    }
}
