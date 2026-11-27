package com.desmond.feedpack

import com.desmond.core.model.WidgetState
import com.desmond.plugin.api.ServiceContext
import com.desmond.plugin.api.ServiceProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

class NatureBiotechService(
    private val client: OkHttpClient = OkHttpClient(),
) : ServiceProgram {
    override val id: String = "com.desmond.feedpack.NatureBiotechService"

    override suspend fun run(context: ServiceContext) {
        while (context.isActive()) {
            context.renderWidget("nature.biotech", WidgetState(title = "Nature Biotech", status = "Fetching RSS…"))
            val payload = withContext(Dispatchers.IO) {
                runCatching { fetchPayload() }.getOrElse { error ->
                    """{"status":"Error","error":"${error.message?.replace("\"","'") ?: error::class.java.simpleName}","papers":[]}"""
                }
            }
            context.renderWidget("nature.biotech", payload)
            kotlinx.coroutines.delay(REFRESH_MS)
        }
    }

    private fun fetchPayload(): String {
        val xml = client.newCall(Request.Builder().url(FEED_URL).build()).execute().use { r ->
            require(r.isSuccessful) { "RSS HTTP ${r.code}" }
            r.body?.string().orEmpty()
        }
        val items = parseRss(xml)
        val papers = items.take(MAX_ITEMS).map { item ->
            val page = runCatching {
                client.newCall(Request.Builder().url(item.link).build()).execute().use { it.body?.string().orEmpty() }
            }.getOrDefault("")
            val openAccess = page.contains("Open Access", ignoreCase = true)
            val abstract = Regex("(?is)<meta[^>]+name=\"dc.description\"[^>]+content=\"([^\"]+)\"").find(page)?.groupValues?.getOrNull(1)
                ?: Regex("(?is)<meta[^>]+property=\"og:description\"[^>]+content=\"([^\"]+)\"").find(page)?.groupValues?.getOrNull(1)
                ?: ""
            val pdf = Regex("(?is)href=\"([^\"]+\\.pdf[^\"]*)\"").find(page)?.groupValues?.getOrNull(1)?.let {
                if (it.startsWith("http")) it else "https://www.nature.com$it"
            } ?: ""
            mapOf(
                "title" to item.title, "link" to item.link, "pubDate" to item.date,
                "openAccess" to openAccess.toString(), "abstractText" to abstract.take(800), "pdfUrl" to pdf,
            )
        }
        val oa = papers.count { it["openAccess"] == "true" }
        val papersJson = papers.joinToString(",", "[", "]") { p ->
            """{"title":${json(p["title"]) },"link":${json(p["link"])},"pubDate":${json(p["pubDate"])},"openAccess":${p["openAccess"]},"abstractText":${json(p["abstractText"])},"pdfUrl":${json(p["pdfUrl"])}}"""
        }
        return """{"status":"Nature Biotechnology · ${papers.size} items · $oa OA","error":"","papers":$papersJson}"""
    }

    private data class Item(val title: String, val link: String, val date: String)
    private fun parseRss(xml: String): List<Item> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))
        var event = parser.eventType
        var inItem = false; var tag = ""; val text = StringBuilder()
        var title = ""; var link = ""; var date = ""; val out = mutableListOf<Item>()
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> { tag = parser.name.lowercase(); if (tag == "item" || tag == "entry") { inItem = true; title=""; link=""; date="" }; text.clear() }
                XmlPullParser.TEXT -> text.append(parser.text ?: "")
                XmlPullParser.END_TAG -> {
                    val v = text.toString().trim()
                    if (inItem) when (tag) { "title" -> title = v; "link" -> if (link.isBlank()) link = parser.getAttributeValue(null, "href") ?: v; "pubdate", "published", "updated" -> date = v }
                    if (parser.name.lowercase() in setOf("item","entry")) { if (title.isNotBlank()) out += Item(title, link, date); inItem = false }
                    tag = ""; text.clear()
                }
            }
            event = parser.next()
        }
        return out
    }

    private fun json(s: String?): String = "\"" + (s ?: "").replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""

    private companion object {
        const val FEED_URL = "https://www.nature.com/subjects/biotechnology.rss"
        const val MAX_ITEMS = 8
        const val REFRESH_MS = 30 * 60 * 1000L
    }
}
