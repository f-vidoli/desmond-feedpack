package com.desmond.feedpack

import com.desmond.core.model.WidgetActionSpec
import com.desmond.core.model.WidgetState
import com.desmond.plugin.api.ServiceContext
import com.desmond.plugin.api.ServiceProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class ReutersRssService(
    private val feedUrl: String = "https://feeds.reuters.com/reuters/worldNews",
    private val client: OkHttpClient = OkHttpClient(),
) : ServiceProgram {
    override val id: String = "com.desmond.feedpack.ReutersRssService"
    override suspend fun run(context: ServiceContext) {
        while (context.isActive()) {
            val state = withContext(Dispatchers.IO) { runCatching { fetch() }.getOrElse { WidgetState(title="Reuters", status="Error: ${it.message}", actions=listOf(WidgetActionSpec("refresh","Refresh"))) } }
            context.renderWidget("widget", state)
            withTimeoutOrNull(15*60*1000L) { context.actions().firstOrNull { it.actionId == "refresh" } }
        }
    }
    private fun fetch(): WidgetState {
        val xml = client.newCall(Request.Builder().url(feedUrl).header("User-Agent","DesmondFeedPack/1.0").build()).execute().use { r ->
            require(r.isSuccessful) { "HTTP ${r.code}" }; r.body?.string().orEmpty()
        }
        val parser = XmlPullParserFactory.newInstance().newPullParser(); parser.setInput(xml.reader())
        var inItem=false; var tag=""; val text=StringBuilder(); var title=""; var date=""; var feed="Reuters"; val items=mutableListOf<String>()
        var ev=parser.eventType
        while (ev!=XmlPullParser.END_DOCUMENT) {
            when(ev){
                XmlPullParser.START_TAG->{ tag=parser.name.lowercase(); if(tag=="item"||tag=="entry"){inItem=true; title=""; date=""}; text.clear() }
                XmlPullParser.TEXT-> if(parser.text?.isNotBlank()==true) text.append(parser.text.trim())
                XmlPullParser.END_TAG->{ val v=text.toString().trim(); if(inItem){ when(tag){"title"->title=v; "pubdate","published","updated"->date=v} } else if(tag=="title"&&v.isNotBlank()) feed=v; if(parser.name.lowercase() in setOf("item","entry")){ if(title.isNotBlank() && items.size<6) items += "${items.size+1}. $title"+if(date.isNotBlank()) " · $date" else ""; inItem=false }; tag=""; text.clear() }
            }; ev=parser.next()
        }
        return WidgetState(title=feed, status=feedUrl, items=items, actions=listOf(WidgetActionSpec("refresh","Refresh")))
    }
}
