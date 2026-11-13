package com.example.data.remote

import com.example.data.BrandParser
import com.example.data.ModelListCodec
import com.example.data.PartCategory
import com.example.data.UniversalGroup
import com.example.data.UniversalGroupEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.abs

/**
 * Firestore-backed universal compatibility model group synchronized in real time
 * under `/universal_models/{modelId}`.
 */
@IgnoreExtraProperties
data class UniversalModel(
  @DocumentId val id: String = "",
  val userId: String = "",
  val updatedByUid: String = "",
  val groupCode: String = "",
  val category: String = PartCategory.TEMPERED_GLASS.name,
  val mainModel: String = "",
  val brand: String = "",
  val screenSize: String = "",
  val compatibleModels: List<String> = emptyList(),
  val releaseNote: String = "",
  val isPublished: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @Exclude
  fun resolvedCategory(): PartCategory = PartCategory.fromName(category)

  @Exclude
  fun allModels(): List<String> = ModelListCodec.mergeModels(mainModel, compatibleModels)

  @Exclude
  fun createdAtMillis(): Long = createdAt?.toDate()?.time ?: 0L

  @Exclude
  fun updatedAtMillis(): Long = (updatedAt ?: createdAt)?.toDate()?.time ?: 0L

  @Exclude
  fun toEntity(localId: Long = 0L): UniversalGroupEntity {
    val resolvedBrand = brand.ifBlank { BrandParser.extractBrand(mainModel) }
    return UniversalGroupEntity(
      id = localId,
      groupCode = groupCode,
      category = resolvedCategory().name,
      mainModel = mainModel,
      brand = resolvedBrand,
      screenSize = screenSize,
      compatibleModelsJson = ModelListCodec.encode(compatibleModels),
      createdAt = createdAtMillis().takeIf { it > 0L } ?: System.currentTimeMillis(),
      updatedAt = updatedAtMillis().takeIf { it > 0L } ?: System.currentTimeMillis(),
      cloudId = id,
    )
  }

  @Exclude
  fun toDomain(
    linkedProductId: Long? = null,
    stockQuantity: Int = 0,
    minimumStock: Int = 3,
    boxNumber: String = "",
  ): UniversalGroup {
    val resolvedBrand = brand.ifBlank { BrandParser.extractBrand(mainModel) }
    return UniversalGroup(
      id = -(abs(id.hashCode().toLong()).let { if (it == 0L) 1L else it }),
      officialDocId = if (isPublished) id else null,
      isOfficial = isPublished,
      releaseNote = releaseNote,
      groupCode = groupCode,
      category = resolvedCategory(),
      mainModel = mainModel,
      brand = resolvedBrand,
      screenSize = screenSize,
      compatibleModels = compatibleModels,
      linkedProductId = linkedProductId,
      stockQuantity = stockQuantity,
      minimumStock = minimumStock,
      boxNumber = boxNumber,
      createdAt = createdAtMillis(),
      updatedAt = updatedAtMillis(),
    )
  }

  /**
   * Builds a create payload conforming to `/universal_models/{modelId}` security rules,
   * omitting `"id"` (managed by `@DocumentId`) and using server timestamps.
   */
  @Exclude
  fun toCreatePayload(ownerUid: String = userId): Map<String, Any> =
    toUpdatePayload(editorUid = ownerUid) + mapOf(
      "userId" to ownerUid,
      "createdAt" to FieldValue.serverTimestamp(),
    )

  /**
   * Builds a partial update payload conforming to `/universal_models/{modelId}` security rules.
   */
  @Exclude
  fun toUpdatePayload(editorUid: String = updatedByUid): Map<String, Any> {
    val cleanMain = mainModel.trim()
    val cleanBrand = brand.trim().ifEmpty { BrandParser.extractBrand(cleanMain) }
    val resolvedCat = resolvedCategory()
    val cleanCompat = compatibleModels
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.equals(cleanMain, ignoreCase = true) }
      .distinctBy { it.lowercase() }
      .take(100)

    return mapOf<String, Any?>(
      "updatedByUid" to editorUid,
      "groupCode" to groupCode.trim().uppercase(),
      "category" to resolvedCat.name,
      "mainModel" to cleanMain,
      "brand" to cleanBrand,
      "screenSize" to screenSize.trim(),
      "compatibleModels" to cleanCompat,
      "releaseNote" to releaseNote.trim(),
      "isPublished" to isPublished,
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
  }

  companion object {
    fun fromSnapshot(snapshot: DocumentSnapshot): UniversalModel? =
      snapshot.toObject(UniversalModel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
  }
}
