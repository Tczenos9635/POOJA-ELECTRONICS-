package com.example.data

data class UniversalGroup(
  val id: Long,
  val groupCode: String,
  val category: PartCategory,
  val mainModel: String,
  val brand: String,
  val screenSize: String,
  val compatibleModels: List<String>,
  val linkedProductId: Long?,
  val stockQuantity: Int,
  val minimumStock: Int,
  val boxNumber: String,
  val createdAt: Long,
  val updatedAt: Long,
  val officialDocId: String? = null,
  val isOfficial: Boolean = false,
  val releaseNote: String = "",
) {
  val allModels: List<String>
    get() = ModelListCodec.mergeModels(mainModel, compatibleModels)

  val stockStatus: StockStatus
    get() = calculateStockStatus(stockQuantity, minimumStock)
}

data class ShopProduct(
  val id: Long,
  val productName: String,
  val category: PartCategory,
  val mainModel: String,
  val brand: String,
  val screenSize: String,
  val stockQuantity: Int,
  val minimumStock: Int,
  val boxNumber: String,
  val universalGroupId: String?,
  val universalGroupDbId: Long?,
  val compatibleModels: List<String>,
  val notes: String,
  val createdAt: Long,
  val updatedAt: Long,
  val isOfficialLinked: Boolean = false,
  val officialReleaseNote: String = "",
) {
  val allModels: List<String>
    get() = ModelListCodec.mergeModels(mainModel, compatibleModels)

  val stockStatus: StockStatus
    get() = calculateStockStatus(stockQuantity, minimumStock)

  val displayTitle: String
    get() = productName.ifBlank { mainModel }
}

data class InventorySummary(
  val totalProducts: Int = 0,
  val totalPieces: Int = 0,
  val activeCategories: Int = PartCategory.entries.size,
  val lowStockCount: Int = 0,
  val outOfStockCount: Int = 0,
)

enum class StockFilter {
  ALL,
  LOW_STOCK,
  OUT_OF_STOCK,
}
