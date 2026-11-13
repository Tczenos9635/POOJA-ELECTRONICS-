package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.InventoryRepository
import com.example.data.PartCategory
import com.example.data.ProductSearchResult
import com.example.data.SearchEngine
import com.example.data.ShopProduct
import com.example.data.UniversalGroup
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val LENS_TAG = "PartLensViewModel"
private const val LENS_MODEL_NAME = "gemini-3.8-flash"
private const val MAX_LENS_IMAGE_DIM = 1280

data class LensScanResult(
  val detectedModel: String,
  val detectedBrand: String,
  val detectedCategory: PartCategory,
  val detectedPartType: String,
  val extractedTextOrCode: String,
  val searchQuery: String,
  val confidence: String,
  val visualNotes: String,
  val matchedProducts: List<ProductSearchResult>,
)

sealed interface LensUiState {
  data object Idle : LensUiState
  data class Analyzing(val bitmap: Bitmap) : LensUiState
  data class Success(val bitmap: Bitmap, val result: LensScanResult) : LensUiState
  data class Error(val bitmap: Bitmap?, val message: String) : LensUiState
}

class PartLensViewModel(
  private val repository: InventoryRepository,
) : ViewModel() {

  private val shopProducts: StateFlow<List<ShopProduct>> = repository.shopProducts
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val officialGroups: StateFlow<List<UniversalGroup>> = repository.officialGroups
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val _uiState = MutableStateFlow<LensUiState>(LensUiState.Idle)
  val uiState: StateFlow<LensUiState> = _uiState.asStateFlow()

  private var lastSelectedBitmap: Bitmap? = null
  private var lastUserNote: String = ""

  private val lensResponseSchema = Schema.obj(
    properties = mapOf(
      "detectedModel" to Schema.string(
        description = "Identified smartphone model name such as Oppo F31, Vivo V30, or Samsung A15. Use Unknown if unclear.",
      ),
      "detectedBrand" to Schema.string(
        description = "Detected smartphone or part brand such as Oppo, Vivo, Samsung, Realme, Xiaomi, Apple, or Generic.",
      ),
      "detectedCategory" to Schema.enumeration(
        values = PartCategory.entries.map { it.name },
        description = "Best matching spare part category.",
      ),
      "detectedPartType" to Schema.string(
        description = "Human-readable description of the visible object or spare part (e.g., Tempered Glass, Back Cover, CC Board, Camera Ring, Smartphone).",
      ),
      "extractedTextOrCode" to Schema.string(
        description = "Any visible printed model code, box label (e.g., B-023), flex marking, or sticker text read from the image. Empty string if none.",
      ),
      "searchQuery" to Schema.string(
        description = "Best concise search term to find compatible parts in the shop inventory (e.g., Oppo F31 or B-023).",
      ),
      "confidence" to Schema.enumeration(
        values = listOf("HIGH", "MEDIUM", "LOW"),
        description = "Identification confidence level.",
      ),
      "visualNotes" to Schema.string(
        description = "Brief technician observation about camera layout, notch/cutout, connector, or fitment check.",
      ),
    ),
  )

  private val lensModel by lazy {
    Firebase.ai.generativeModel(
      modelName = LENS_MODEL_NAME,
      generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = lensResponseSchema
      },
      systemInstruction = content {
        text(
          """
          You are Smart Part Lens for Pooja Electronics, a mobile phone repair shop.
          Analyze the photo of a smartphone, mobile back cover, tempered glass, combo display, CC charging board, battery, middle frame, camera ring, or spare part box label.
          Identify:
          1) The phone model name and brand (or read printed model stickers / box numbers / flex markings).
          2) Which spare part category it belongs to.
          3) A short, practical search query so the technician can instantly locate matching stock or universal compatible parts in their shop.
          Be honest if a model cannot be identified with certainty; mark confidence LOW and describe visible clues in visualNotes.
          """.trimIndent(),
        )
      },
    )
  }

  fun analyzeImageUri(context: Context, uri: Uri, userHint: String = "") {
    viewModelScope.launch {
      try {
        val scaledBitmap = withContext(Dispatchers.IO) {
          decodeAndScaleBitmap(context.applicationContext, uri)
        }
        analyzeBitmap(scaledBitmap, userHint)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(LENS_TAG, "Failed to load selected image", e)
        _uiState.value = LensUiState.Error(
          bitmap = null,
          message = e.localizedMessage ?: "Unable to read the selected photo.",
        )
      }
    }
  }

  fun analyzeBitmap(bitmap: Bitmap, userHint: String = "") {
    if (_uiState.value is LensUiState.Analyzing) return
    lastSelectedBitmap = bitmap
    lastUserNote = userHint.trim()
    _uiState.value = LensUiState.Analyzing(bitmap)

    viewModelScope.launch {
      try {
        val promptText = buildString {
          append("Identify the mobile phone model, spare part category, and any printed text or box/model codes in this photo.")
          if (lastUserNote.isNotBlank()) {
            append("\nShop owner note: ")
            append(lastUserNote)
          }
        }

        val response = withTimeout(60_000L) {
          lensModel.generateContent(
            content {
              image(bitmap)
              text(promptText)
            },
          )
        }

        val jsonText = response.text?.trim()
          ?: throw IllegalStateException("Lens AI returned an empty response.")
        val json = JSONObject(jsonText)

        val detectedModel = json.optString("detectedModel", "Unknown").trim().ifEmpty { "Unknown" }
        val detectedBrand = json.optString("detectedBrand", "").trim()
        val categoryName = json.optString("detectedCategory", PartCategory.TEMPERED_GLASS.name)
        val detectedCategory = PartCategory.fromName(categoryName)
        val detectedPartType = json.optString("detectedPartType", detectedCategory.displayName)
          .trim()
          .ifEmpty { detectedCategory.displayName }
        val extractedText = json.optString("extractedTextOrCode", "").trim()
        val rawSearchQuery = json.optString("searchQuery", "").trim()
        val searchQuery = rawSearchQuery.ifEmpty {
          when {
            !detectedModel.equals("Unknown", ignoreCase = true) -> detectedModel
            extractedText.isNotBlank() -> extractedText
            detectedBrand.isNotBlank() -> detectedBrand
            else -> detectedCategory.displayName
          }
        }
        val confidence = json.optString("confidence", "MEDIUM").uppercase()
        val visualNotes = json.optString("visualNotes", "").trim()

        val matched = findMatchingInventory(
          searchQuery = searchQuery,
          detectedModel = detectedModel,
          extractedCode = extractedText,
          detectedBrand = detectedBrand,
        )

        _uiState.value = LensUiState.Success(
          bitmap = bitmap,
          result = LensScanResult(
            detectedModel = detectedModel,
            detectedBrand = detectedBrand,
            detectedCategory = detectedCategory,
            detectedPartType = detectedPartType,
            extractedTextOrCode = extractedText,
            searchQuery = searchQuery,
            confidence = confidence,
            visualNotes = visualNotes,
            matchedProducts = matched,
          ),
        )
      } catch (e: TimeoutCancellationException) {
        Log.e(LENS_TAG, "Lens analysis timed out", e)
        _uiState.value = LensUiState.Error(
          bitmap = bitmap,
          message = "Visual scan timed out after 60 seconds. Please try again.",
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(LENS_TAG, "Lens analysis failed", e)
        _uiState.value = LensUiState.Error(
          bitmap = bitmap,
          message = e.localizedMessage ?: "Failed to analyze photo. Please check network and try again.",
        )
      }
    }
  }

  fun retryLastScan() {
    val bitmap = lastSelectedBitmap ?: return
    analyzeBitmap(bitmap, lastUserNote)
  }

  fun resetScan() {
    _uiState.value = LensUiState.Idle
  }

  private fun findMatchingInventory(
    searchQuery: String,
    detectedModel: String,
    extractedCode: String,
    detectedBrand: String,
  ): List<ProductSearchResult> {
    val tracked = shopProducts.value
    val trackedGroupCodes = tracked.mapNotNull { it.universalGroupId?.uppercase() }.toSet()
    val untrackedOfficial = officialGroups.value
      .filter { it.linkedProductId == null && it.groupCode.uppercase() !in trackedGroupCodes }
      .map { group ->
        ShopProduct(
          id = group.id,
          productName = "${group.mainModel} ${group.category.displayName}",
          category = group.category,
          mainModel = group.mainModel,
          brand = group.brand,
          screenSize = group.screenSize,
          stockQuantity = 0,
          minimumStock = 3,
          boxNumber = "",
          universalGroupId = group.groupCode,
          universalGroupDbId = group.id,
          compatibleModels = group.compatibleModels,
          notes = "",
          createdAt = group.createdAt,
          updatedAt = group.updatedAt,
          isOfficialLinked = true,
          officialReleaseNote = group.releaseNote,
        )
      }
    val combined = tracked + untrackedOfficial

    val candidateQueries = listOf(searchQuery, detectedModel, extractedCode, detectedBrand)
      .map { it.trim() }
      .filter { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) }
      .distinctBy { it.lowercase() }

    val seenKeys = mutableSetOf<String>()
    val results = mutableListOf<ProductSearchResult>()
    for (candidate in candidateQueries) {
      val hits = SearchEngine.searchProducts(combined, candidate)
      for (hit in hits) {
        val key = "${hit.product.id}_${hit.product.universalGroupId.orEmpty()}"
        if (seenKeys.add(key)) {
          results.add(hit)
        }
      }
      if (results.size >= 8) break
    }
    return results.take(8)
  }

  private fun decodeAndScaleBitmap(context: Context, uri: Uri): Bitmap {
    val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val source = ImageDecoder.createSource(context.contentResolver, uri)
      ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val width = info.size.width
        val height = info.size.height
        val maxDim = max(width, height)
        if (maxDim > MAX_LENS_IMAGE_DIM && maxDim > 0) {
          val scale = MAX_LENS_IMAGE_DIM.toFloat() / maxDim.toFloat()
          decoder.setTargetSize(
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1),
          )
        }
      }
    } else {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
      }
      var sampleSize = 1
      val maxDim = max(bounds.outWidth, bounds.outHeight)
      while (maxDim / sampleSize > MAX_LENS_IMAGE_DIM * 2) {
        sampleSize *= 2
      }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOpts)
      } ?: throw IllegalStateException("Could not decode image from URI.")

      val scaledMax = max(decoded.width, decoded.height)
      if (scaledMax > MAX_LENS_IMAGE_DIM && scaledMax > 0) {
        val ratio = MAX_LENS_IMAGE_DIM.toFloat() / scaledMax.toFloat()
        Bitmap.createScaledBitmap(
          decoded,
          (decoded.width * ratio).roundToInt().coerceAtLeast(1),
          (decoded.height * ratio).roundToInt().coerceAtLeast(1),
          true,
        )
      } else {
        decoded
      }
    }

    // Ensure compressed payload size stays safely compact (< 2MB JPEG equivalent in memory/transfer)
    ByteArrayOutputStream().use { baos ->
      rawBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
      if (baos.size() > 15 * 1024 * 1024) {
        val shrunkWidth = (rawBitmap.width * 0.6f).roundToInt().coerceAtLeast(1)
        val shrunkHeight = (rawBitmap.height * 0.6f).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(rawBitmap, shrunkWidth, shrunkHeight, true)
      }
    }
    return rawBitmap
  }
}

class PartLensViewModelFactory(
  private val repository: InventoryRepository,
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(PartLensViewModel::class.java)) {
      return PartLensViewModel(repository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
