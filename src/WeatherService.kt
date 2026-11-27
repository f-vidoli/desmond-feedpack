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
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
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
            val settings = readSettings(context)
            val state = withContext(Dispatchers.IO) { runCatching { fetchWeather(settings) }.getOrElse { error(it, settings) } }
            lastState = state
            context.renderWidget("widget", state)
            withTimeoutOrNull(REFRESH_MS) { context.actions().firstOrNull { it.actionId == "refresh" } }
        }
    }

    private fun readSettings(context: ServiceContext): LocationSettings {
        val raw = context.readEntry("settings") ?: return LocationSettings()
        val obj = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return LocationSettings()
        val data = obj["data"]?.jsonObject ?: obj
        return LocationSettings(
            latitude = data["latitude"]?.jsonPrimitive?.doubleOrNull ?: 44.4949,
            longitude = data["longitude"]?.jsonPrimitive?.doubleOrNull ?: 11.3426,
            label = data["label"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: "Bologna",
            timezone = data["timezone"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: "Europe/Rome",
        )
    }

    private fun fetchWeather(settings: LocationSettings): WidgetState {
        val url = "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
            .addQueryParameter("latitude", settings.latitude.toString())
            .addQueryParameter("longitude", settings.longitude.toString())
            .addQueryParameter("hourly", "temperature_2m,weather_code")
            .addQueryParameter("forecast_days", "1")
            .addQueryParameter("timezone", settings.timezone)
            .build()
        val payload = client.newCall(Request.Builder().url(url).build()).execute().use { r ->
            require(r.isSuccessful) { "Weather HTTP ${r.code}" }
            r.body?.string().orEmpty()
        }
        val weather = json.decodeFromString<WeatherResponse>(payload)
        val items = weather.hourly.time.take(HOURS).mapIndexed { i, t ->
            "${t.substringAfter('T')} · ${weather.hourly.temperature.getOrNull(i)}° · ${code(weather.hourly.weatherCode.getOrNull(i))}"
        }
        return WidgetState(
            title = "Weather ${settings.label}",
            status = "Open-Meteo · ${settings.latitude}, ${settings.longitude}",
            items = items,
            actions = listOf(WidgetActionSpec("refresh", "Refresh")),
        )
    }

    private fun error(t: Throwable, settings: LocationSettings) = WidgetState(
        title = "Weather ${settings.label}",
        status = "Error: ${t.message ?: t::class.java.simpleName}",
        actions = listOf(WidgetActionSpec("refresh", "Refresh")),
    )

    private fun code(c: Int?) = when (c) {
        0 -> "Clear"; 1,2,3 -> "Cloudy"; 45,48 -> "Fog"; 51,53,55,56,57 -> "Drizzle"
        61,63,65,66,67 -> "Rain"; 71,73,75,77 -> "Snow"; 95,96,99 -> "Storm"; else -> "Weather"
    }

    private data class LocationSettings(
        val latitude: Double = 44.4949,
        val longitude: Double = 11.3426,
        val label: String = "Bologna",
        val timezone: String = "Europe/Rome",
    )
    @Serializable private data class WeatherResponse(val hourly: Hourly)
    @Serializable private data class Hourly(
        val time: List<String> = emptyList(),
        @SerialName("temperature_2m") val temperature: List<Double> = emptyList(),
        @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    )
    private companion object { const val HOURS = 6; const val REFRESH_MS = 30 * 60 * 1000L }
}
