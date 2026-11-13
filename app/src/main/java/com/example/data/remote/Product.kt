package com.example.data.remote

import com.example.data.BrandParser
import com.example.data.ModelListCodec
import com.example.data.PartCategory
import com.example.data.ProductEntity
import com.example.data.ShopProduct
import com.example.data.StockStatus
import com.example.data.calculateStockStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.abs

/**
 * Firestore-backed spare part inventory product model synchronized in real time
 * under `/products/{productId}`.
 */
@IgnoreExtraProperties
data class Product(
  @DocumentId val id: String = "",
  val userId: String = "",
  val productName: String = "",
  val category: String = PartCategory.TEMPERED_GLASS.name,
  val mainModel: String = "",
  val brand: String = "",
  val screenSize: String = "",
  val stockQuantity: Long = 0L,
  val minimumStock: Long = 3L,
  val boxNumber: String = "",
  val universalGroupId: String = "",
  val compatibleModels: List<String> = emptyList(),
  val notes: String = "",
  val isPublished: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @Exclude
  fun resolvedCategory(): PartCategory = PartCategory.fromName(category)

  @Exclude
  fun resolvedStockQuantity(): Int = stockQuantity.toInt().coerceAtLeast(0)

  @Exclude
  fun resolvedMinimumStock(): Int = minimumStock.toInt().coerceAtLeast(0)

  @Exclude
  fun stockStatus(): StockStatus =
    calculateStockStatus(resolvedStockQuantity(), resolvedMinimumStock())

  @Exclude
  fun allModels(): List<String> = ModelListCodec.mergeModels(mainModel, compatibleModels)

  @Exclude
  fun createdAtMillis(): Long = createdAt?.toDate()?.time ?: 0L

  @Exclude
  fun updatedAtMillis(): Long = (updatedAt ?: createdAt)?.toDate()?.time ?: 0L

  @Exclude
  fun toEntity(localId: Long = 0L): ProductEntity {
    val resolvedBrand = brand.ifBlank { BrandParser.extractBrand(mainModel) }
    val resolvedCat = resolvedCategory()
    val resolvedName = productName.ifBlank { "$mainModel ${resolvedCat.displayName}" }
    return ProductEntity(
      id = localId,
      productName = resolvedName,
      category = resolvedCat.name,
      mainModel = mainModel,
      brand = resolvedBrand,
      screenSize = screenSize,
      stockQuantity = resolvedStockQuantity(),
      minimumStock = resolvedMinimumStock(),
      boxNumber = boxNumber,
      universalGroupId = universalGroupId.ifBlank { null },
      compatibleModelsJson = ModelListCodec.encode(compatibleModels),
      notes = notes,
      createdAt = createdAtMillis().takeIf { it > 0L } ?: System.currentTimeMillis(),
      updatedAt = updatedAtMillis().takeIf { it > 0L } ?: System.currentTimeMillis(),
      cloudId = id,
    )
  }

  @Exclude
  fun toDomain(): ShopProduct {
    val resolvedCat = resolvedCategory()
    val resolvedBrand = brand.ifBlank { BrandParser.extractBrand(mainModel) }
    return ShopProduct(
      id = -(abs(id.hashCode().toLong()).let { if (it == 0L) 1L else it }),
      productName = productName.ifBlank { "$mainModel ${resolvedCat.displayName}" },
      category = resolvedCat,
      mainModel = mainModel,
      brand = resolvedBrand,
      screenSize = screenSize,
      stockQuantity = resolvedStockQuantity(),
      minimumStock = resolvedMinimumStock(),
      boxNumber = boxNumber,
      universalGroupId = universalGroupId.ifBlank { null },
      universalGroupDbId = null,
      compatibleModels = compatibleModels,
      notes = notes,
      createdAt = createdAtMillis(),
      updatedAt = updatedAtMillis(),
      isOfficialLinked = isPublished,
      officialReleaseNote = notes,
    )
  }

  /**
   * Builds a create payload conforming to `/products/{productId}` security rules,
   * omitting `"id"` (managed by `@DocumentId`) and using server timestamps.
   */
  @Exclude
  fun toCreatePayload(ownerUid: String = userId): Map<String, Any> =
    toUpdatePayload() + mapOf(
      "userId" to ownerUid,
      "createdAt" to FieldValue.serverTimestamp(),
    )

  /**
   * Builds a partial update payload conforming to `/products/{productId}` security rules.
   */
  @Exclude
  fun toUpdatePayload(): Map<String, Any> {
    val cleanMain = mainModel.trim()
    val cleanBrand = brand.trim().ifEmpty { BrandParser.extractBrand(cleanMain) }
    val resolvedCat = resolvedCategory()
    val cleanName = productName.trim().ifEmpty { "$cleanMain ${resolvedCat.displayName}" }
    val cleanCompat = compatibleModels
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.equals(cleanMain, ignoreCase = true) }
      .distinctBy { it.lowercase() }
      .take(100)

    return mapOf<String, Any?>(
      "productName" to cleanName,
      "category" to resolvedCat.name,
      "mainModel" to cleanMain,
      "brand" to cleanBrand,
      "screenSize" to screenSize.trim(),
      "stockQuantity" to stockQuantity.coerceAtLeast(0L),
      "minimumStock" to minimumStock.coerceAtLeast(0L),
      "boxNumber" to boxNumber.trim().uppercase(),
      "universalGroupId" to universalGroupId.trim().uppercase(),
      "compatibleModels" to cleanCompat,
      "notes" to notes.trim(),
      "isPublished" to isPublished,
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
  }

  companion object {
    fun fromSnapshot(snapshot: DocumentSnapshot): Product? =
      snapshot.toObject(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
  }
}
