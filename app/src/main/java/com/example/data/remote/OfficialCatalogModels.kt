package com.example.data.remote

import com.example.data.BrandParser
import com.example.data.PartCategory
import com.example.data.UniversalGroup
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class OfficialUniversalGroupDoc(
  @DocumentId val id: String = "",
  val groupCode: String = "",
  val category: String = PartCategory.TEMPERED_GLASS.name,
  val mainModel: String = "",
  val brand: String = "",
  val screenSize: String = "",
  val compatibleModels: List<String> = emptyList(),
  val releaseNote: String = "",
  val isPublished: Boolean = true,
  val updatedByUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toDomain(
    linkedProductId: Long? = null,
    stockQuantity: Int = 0,
    minimumStock: Int = 3,
    boxNumber: String = "",
  ): UniversalGroup {
    val resolvedBrand = brand.ifBlank { BrandParser.extractBrand(mainModel) }
    val createdMs = createdAt?.toDate()?.time ?: 0L
    val updatedMs = (updatedAt ?: createdAt)?.toDate()?.time ?: 0L
    return UniversalGroup(
      id = -(id.hashCode().toLong().let { if (it == 0L) 1L else kotlin.math.abs(it) }),
      officialDocId = id,
      isOfficial = true,
      releaseNote = releaseNote,
      groupCode = groupCode,
      category = PartCategory.fromName(category),
      mainModel = mainModel,
      brand = resolvedBrand,
      screenSize = screenSize,
      compatibleModels = compatibleModels,
      linkedProductId = linkedProductId,
      stockQuantity = stockQuantity,
      minimumStock = minimumStock,
      boxNumber = boxNumber,
      createdAt = createdMs,
      updatedAt = updatedMs,
    )
  }
}
