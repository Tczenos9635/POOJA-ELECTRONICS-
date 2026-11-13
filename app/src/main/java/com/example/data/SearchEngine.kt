package com.example.data

data class ProductSearchResult(
  val product: ShopProduct,
  val matchReason: String,
)

data class BrandModelEntry(
  val modelName: String,
  val brand: String,
  val isMainModel: Boolean,
  val mainModel: String,
  val screenSize: String,
  val universalGroupCode: String?,
  val boxNumber: String,
  val stockQuantity: Int,
  val stockStatus: StockStatus,
  val productId: Long,
  val compatibleCount: Int,
  val isOfficial: Boolean = false,
  val universalGroupId: Long? = null,
)

data class BrandModelsSection(
  val brand: String,
  val entries: List<BrandModelEntry>,
)

object SearchEngine {
  fun searchProducts(products: List<ShopProduct>, query: String): List<ProductSearchResult> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return products.map { ProductSearchResult(it, "") }

    val tokens = q.split(Regex("\\s+")).filter { it.isNotEmpty() }

    return products.mapNotNull { product ->
      val reason = findMatchReason(product, q, tokens)
      if (reason != null) ProductSearchResult(product, reason) else null
    }
  }

  private fun findMatchReason(
    product: ShopProduct,
    q: String,
    tokens: List<String>,
  ): String? {
    if (product.boxNumber.isNotBlank() && product.boxNumber.lowercase().contains(q)) {
      return "Box ${product.boxNumber}"
    }
    if (!product.universalGroupId.isNullOrBlank() && product.universalGroupId.lowercase().contains(q)) {
      return "Universal ${product.universalGroupId}"
    }
    if (product.mainModel.lowercase().contains(q)) {
      return "Main model: ${product.mainModel}"
    }
    val matchedCompat = product.compatibleModels.firstOrNull { it.lowercase().contains(q) }
    if (matchedCompat != null) {
      return "Compatible: $matchedCompat"
    }
    if (product.productName.lowercase().contains(q)) {
      return product.productName
    }
    if (product.brand.lowercase().contains(q)) {
      return "Brand: ${product.brand}"
    }
    if (product.category.displayName.lowercase().contains(q)) {
      return "Category: ${product.category.displayName}"
    }
    if (product.screenSize.isNotBlank() && product.screenSize.lowercase().contains(q)) {
      return "Screen ${product.screenSize}"
    }
    if (product.notes.isNotBlank() && product.notes.lowercase().contains(q)) {
      return "Notes match"
    }
    if (product.officialReleaseNote.isNotBlank() && product.officialReleaseNote.lowercase().contains(q)) {
      return "Official: ${product.officialReleaseNote}"
    }

    // Fallback multi-token matching across combined searchable fields
    val haystack = buildString {
      append(product.productName).append(' ')
      append(product.mainModel).append(' ')
      append(product.brand).append(' ')
      append(product.category.displayName).append(' ')
      append(product.universalGroupId ?: "").append(' ')
      append(product.boxNumber).append(' ')
      append(product.screenSize).append(' ')
      append(product.compatibleModels.joinToString(" "))
    }.lowercase()

    return if (tokens.all { haystack.contains(it) }) {
      "Matches \"${tokens.joinToString(" ")}\""
    } else {
      null
    }
  }

  fun searchUniversalGroups(groups: List<UniversalGroup>, query: String): List<UniversalGroup> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return groups
    return groups.filter { group ->
      group.groupCode.lowercase().contains(q) ||
        group.mainModel.lowercase().contains(q) ||
        group.brand.lowercase().contains(q) ||
        group.category.displayName.lowercase().contains(q) ||
        group.screenSize.lowercase().contains(q) ||
        group.releaseNote.lowercase().contains(q) ||
        group.compatibleModels.any { it.lowercase().contains(q) }
    }
  }

  fun groupCategoryModelsByBrand(
    category: PartCategory,
    products: List<ShopProduct>,
    query: String,
    universalGroups: List<UniversalGroup> = emptyList(),
  ): List<BrandModelsSection> {
    val q = query.trim().lowercase()
    val categoryProducts = products.filter { it.category == category }
    val trackedGroupCodes = categoryProducts.mapNotNull { it.universalGroupId?.uppercase() }.toSet()

    val entries = mutableListOf<BrandModelEntry>()
    val seenKeys = mutableSetOf<String>()

    categoryProducts.forEach { product ->
      val allModels = product.allModels
      allModels.forEachIndexed { index, modelName ->
        val brand = BrandParser.extractBrand(modelName)
        val matchesQuery = q.isEmpty() ||
          modelName.lowercase().contains(q) ||
          brand.lowercase().contains(q) ||
          product.mainModel.lowercase().contains(q) ||
          (product.universalGroupId?.lowercase()?.contains(q) == true) ||
          product.boxNumber.lowercase().contains(q)

        if (matchesQuery) {
          val dedupeKey = "${modelName.lowercase()}::product_${product.id}"
          if (seenKeys.add(dedupeKey)) {
            entries.add(
              BrandModelEntry(
                modelName = modelName,
                brand = brand,
                isMainModel = index == 0,
                mainModel = product.mainModel,
                screenSize = product.screenSize,
                universalGroupCode = product.universalGroupId,
                boxNumber = product.boxNumber,
                stockQuantity = product.stockQuantity,
                stockStatus = product.stockStatus,
                productId = product.id,
                compatibleCount = product.allModels.size,
                isOfficial = product.isOfficialLinked,
                universalGroupId = product.universalGroupDbId,
              ),
            )
          }
        }
      }
    }

    // Also include untracked Universal Groups (e.g. Official Cloud Updates not yet added to My Shop stock)
    universalGroups
      .filter { it.category == category && it.linkedProductId == null && it.groupCode.uppercase() !in trackedGroupCodes }
      .forEach { group ->
        val allModels = group.allModels
        allModels.forEachIndexed { index, modelName ->
          val brand = BrandParser.extractBrand(modelName)
          val matchesQuery = q.isEmpty() ||
            modelName.lowercase().contains(q) ||
            brand.lowercase().contains(q) ||
            group.mainModel.lowercase().contains(q) ||
            group.groupCode.lowercase().contains(q) ||
            group.releaseNote.lowercase().contains(q)

          if (matchesQuery) {
            val dedupeKey = "${modelName.lowercase()}::group_${group.groupCode.uppercase()}"
            if (seenKeys.add(dedupeKey)) {
              entries.add(
                BrandModelEntry(
                  modelName = modelName,
                  brand = brand,
                  isMainModel = index == 0,
                  mainModel = group.mainModel,
                  screenSize = group.screenSize,
                  universalGroupCode = group.groupCode,
                  boxNumber = group.boxNumber,
                  stockQuantity = group.stockQuantity,
                  stockStatus = group.stockStatus,
                  productId = group.id,
                  compatibleCount = group.allModels.size,
                  isOfficial = group.isOfficial,
                  universalGroupId = group.id,
                ),
              )
            }
          }
        }
      }

    return entries
      .groupBy { it.brand.uppercase() }
      .entries
      .sortedBy { it.key }
      .map { (brandHeader, brandEntries) ->
        BrandModelsSection(
          brand = brandHeader,
          entries = brandEntries.sortedBy { it.modelName.lowercase() },
        )
      }
  }
}
