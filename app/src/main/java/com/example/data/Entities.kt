package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "universal_groups",
  indices = [
    Index(value = ["groupCode"], unique = true),
    Index(value = ["cloudId"]),
  ],
)
data class UniversalGroupEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val groupCode: String,
  val category: String,
  val mainModel: String,
  val brand: String,
  val screenSize: String,
  val compatibleModelsJson: String,
  val createdAt: Long,
  val updatedAt: Long,
  val cloudId: String = "",
)

@Entity(
  tableName = "products",
  indices = [
    Index(value = ["universalGroupId"]),
    Index(value = ["cloudId"]),
  ],
)
data class ProductEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val productName: String,
  val category: String,
  val mainModel: String,
  val brand: String,
  val screenSize: String,
  val stockQuantity: Int,
  val minimumStock: Int,
  val boxNumber: String,
  val universalGroupId: String?,
  val compatibleModelsJson: String,
  val notes: String,
  val createdAt: Long,
  val updatedAt: Long,
  val cloudId: String = "",
)
