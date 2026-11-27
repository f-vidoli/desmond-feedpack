package com.desmond.feedpack

import com.desmond.core.model.WidgetActionSpec
import com.desmond.core.model.WidgetState
import com.desmond.plugin.api.ServiceContext
import com.desmond.plugin.api.ServiceProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class WeatherService(
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ServiceProgram {
    override val id: String = "com.desmond.feedpack.WeatherService"

    @Volatile private var lastState: WidgetState? = null
    fun currentState(): WidgetState? = lastState

    override suspend fun run(context: ServiceContext) {
        while (context.isActive()) {
            val state = withContext(Dispatchers.IO) { runCatching { fetchWeather() }.getOrElse { error(it) } }
            lastState = state
            context.renderWidget("widget", state)
            withTimeoutOrNull(REFRESH_MS) { context.actions().firstOrNull { it.actionId == "refresh" } }
        }
    }

    private fun fetchWeather(): WidgetState {
        val request = Request.Builder().url(API_URL).build()
        val payload = client.newCall(request).execute().use { r ->
            require(r.isSuccessful) { "Weather HTTP ${r.code}" }
            r.body?.string().orEmpty()
        }
        val weather = json.decodeFromString<WeatherResponse>(payload)
        val items = weather.hourly.time.take(HOURS).mapIndexed { i, t ->
            "${t.substringAfter('T')} · ${weather.hourly.temperature.getOrNull(i)}° · ${code(weather.hourly.weatherCode.getOrNull(i))}"
        }
        return WidgetState(title = "Weather Bologna", status = "Open-Meteo", items = items, actions = listOf(WidgetActionSpec("refresh","Refresh")))
    }
    private fun error(t: Throwable) = WidgetState(title="Weather Bologna", status="Error: ${t.message}", actions=listOf(WidgetActionSpec("refresh","Refresh")))
    private fun code(c: Int?) = when(c){0->"Clear";1,2,3->"Cloudy";45,48->"Fog";51,53,55,56,57->"Drizzle";61,63,65,66,67->"Rain";71,73,75,77->"Snow";95,96,99->"Storm";else->"Weather"}
    @Serializable private data class WeatherResponse(val hourly: Hourly)
    @Serializable private data class Hourly(val time: List<String> = emptyList(), @SerialName("temperature_2m") val temperature: List<Double> = emptyList(), @SerialName("weather_code") val weatherCode: List<Int> = emptyList())
    private companion object { const val API_URL = "https://api.open-meteo.com/v1/forecast?latitude=44.4949&longitude=11.3426&hourly=temperature_2m,weather_code&forecast_days=1&timezone=Europe%2FRome"; const val HOURS=6; const val REFRESH_MS=30*60*1000L }
}
