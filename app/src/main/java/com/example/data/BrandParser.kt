package com.example.data

import org.json.JSONArray

object BrandParser {
  private val knownBrands = listOf(
    "Oppo" to listOf("oppo", "reno", "find x"),
    "Vivo" to listOf("vivo", "iqoo", "x100", "v30", "v29", "y200"),
    "Samsung" to listOf("samsung", "galaxy"),
    "Realme" to listOf("realme", "narzo"),
    "Redmi" to listOf("redmi"),
    "Xiaomi" to listOf("xiaomi", "mi "),
    "Poco" to listOf("poco"),
    "Apple" to listOf("apple", "iphone"),
    "OnePlus" to listOf("oneplus", "one plus", "nord"),
    "Motorola" to listOf("motorola", "moto"),
    "Nothing" to listOf("nothing", "cmf"),
    "Google" to listOf("google", "pixel"),
    "Tecno" to listOf("tecno"),
    "Infinix" to listOf("infinix"),
    "Lava" to listOf("lava"),
    "Nokia" to listOf("nokia"),
    "Honor" to listOf("honor"),
    "Huawei" to listOf("huawei"),
    "iQOO" to listOf("iqoo"),
  )

  fun extractBrand(modelName: String): String {
    val trimmed = modelName.trim()
    if (trimmed.isEmpty()) return "Other"
    val lower = trimmed.lowercase()
    for ((brand, keywords) in knownBrands) {
      if (keywords.any { lower.startsWith(it) || lower.contains(" $it") }) {
        return brand
      }
    }
    val firstWord = trimmed.substringBefore(" ").trim()
    return firstWord.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
      .ifEmpty { "Other" }
  }
}

object ModelListCodec {
  fun encode(models: List<String>): String {
    val array = JSONArray()
    models
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .distinctBy { it.lowercase() }
      .forEach { array.put(it) }
    return array.toString()
  }

  fun decode(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
      val array = JSONArray(json)
      buildList {
        for (i in 0 until array.length()) {
          val value = array.optString(i).trim()
          if (value.isNotEmpty()) add(value)
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun mergeModels(mainModel: String, compatibleModels: List<String>): List<String> {
    val cleanMain = mainModel.trim()
    val result = mutableListOf<String>()
    val seen = mutableSetOf<String>()
    if (cleanMain.isNotEmpty()) {
      result.add(cleanMain)
      seen.add(cleanMain.lowercase())
    }
    compatibleModels.forEach { model ->
      val trimmed = model.trim()
      if (trimmed.isNotEmpty() && seen.add(trimmed.lowercase())) {
        result.add(trimmed)
      }
    }
    return result
  }
}
