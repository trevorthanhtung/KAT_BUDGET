package com.katgr0up.katbudget.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.w3c.dom.Element
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.xml.parsers.DocumentBuilderFactory

object ExchangeRateManager {
    private const val TAG = "ExchangeRateManager"
    private const val PREF_NAME = "katbudget_prefs"
    private const val LAST_UPDATE_KEY = "last_rate_update_time"
    private const val CACHED_RATES_KEY = "cached_exchange_rates"
    private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
    private const val NETWORK_TIMEOUT_MILLIS = 10_000

    private const val API_URL = "https://portal.vietcombank.com.vn/Usercontrols/TVPortal.TyGia/pXML.aspx"

    // Fallback rates keep currency conversion available when the network is unavailable.
    private val currentRates = ConcurrentHashMap<String, Double>().apply {
        put("VND", 1.0)
        put("USD", 26161.0)
        put("EUR", 30162.0)
        put("GBP", 34844.0)
        put("JPY", 162.36)
        put("KRW", 16.86)
        put("CNY", 3818.69)
        put("AUD", 18461.61)
        put("CAD", 18869.45)
        put("CHF", 32995.16)
        put("DKK", 4025.71)
        put("HKD", 3305.29)
        put("INR", 270.80)
        put("KWD", 85459.27)
        put("MYR", 6573.22)
        put("NOK", 2786.11)
        put("RUB", 350.98)
        put("SAR", 6987.24)
        put("SEK", 2763.87)
        put("SGD", 20280.76)
        put("THB", 791.70)
    }

    fun getRates(): Map<String, Double> = currentRates.toMap()

    suspend fun fetchLiveRatesIfNeeded(context: Context): Map<String, Double> = withContext(Dispatchers.IO) {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadCachedRates(prefs)

        val lastUpdate = prefs.getLong(LAST_UPDATE_KEY, 0L)
        val currentTime = System.currentTimeMillis()

        // Tránh gọi API liên tục nếu chưa qua 24h
        if (currentTime - lastUpdate < ONE_DAY_MILLIS) {
            return@withContext getRates()
        }

        runCatching {
            val xmlString = readRatesXml()

            val factory = DocumentBuilderFactory.newInstance().apply {
                isExpandEntityReferences = false
                trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                trySetFeature("http://xml.org/sax/features/external-general-entities", false)
                trySetFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(xmlString.byteInputStream())

            val nodeList = document.getElementsByTagName("Exrate")

            val newRates = mutableMapOf("VND" to 1.0)

            for (i in 0 until nodeList.length) {
                val element = nodeList.item(i) as Element
                val currencyCode = normalizeCurrency(element.getAttribute("CurrencyCode"))

                if (currencyCode.isNotBlank()) {
                    val rateStr = element.getAttribute("Transfer").ifEmpty { element.getAttribute("Sell") }
                    val rate = rateStr.replace(",", "").toDoubleOrNull()

                    if (rate != null && rate > 0.0) {
                        newRates[currencyCode] = rate
                    }
                }
            }

            if (newRates.size > 1) {
                currentRates.putAll(newRates)
                prefs.edit {
                    putLong(LAST_UPDATE_KEY, currentTime)
                    putString(CACHED_RATES_KEY, JSONObject(newRates).toString())
                }
            }

        }.onFailure { error ->
            Log.w(TAG, "Unable to refresh exchange rates.", error)
        }

        return@withContext getRates()
    }

    private fun readRatesXml(): String {
        val connection = URL(API_URL).openConnection().apply {
            connectTimeout = NETWORK_TIMEOUT_MILLIS
            readTimeout = NETWORK_TIMEOUT_MILLIS
        }

        return connection.getInputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun loadCachedRates(prefs: SharedPreferences) {
        val rawJson = prefs.getString(CACHED_RATES_KEY, null) ?: return
        runCatching {
            val json = JSONObject(rawJson)
            for (key in json.keys()) {
                val currency = normalizeCurrency(key)
                val rate = json.optDouble(key, 0.0)
                if (currency.isNotBlank() && rate > 0.0) {
                    currentRates[currency] = rate
                }
            }
        }
    }

    private fun DocumentBuilderFactory.trySetFeature(name: String, value: Boolean) {
        runCatching { setFeature(name, value) }
    }

    private fun normalizeCurrency(currency: String): String {
        return when (currency.trim().uppercase(java.util.Locale.ROOT)) {
            "VNĐ", "VND", "" -> "VND"
            else -> currency.trim().uppercase(java.util.Locale.ROOT)
        }
    }
}
