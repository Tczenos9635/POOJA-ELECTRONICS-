package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UniversalGroupDao {
  @Query("SELECT * FROM universal_groups ORDER BY updatedAt DESC")
  fun observeAllGroups(): Flow<List<UniversalGroupEntity>>

  @Query("SELECT * FROM universal_groups ORDER BY updatedAt DESC")
  suspend fun getAllGroupsOnce(): List<UniversalGroupEntity>

  @Query("SELECT * FROM universal_groups WHERE id = :id LIMIT 1")
  suspend fun getGroupById(id: Long): UniversalGroupEntity?

  @Query("SELECT * FROM universal_groups WHERE groupCode = :groupCode LIMIT 1")
  suspend fun getGroupByCode(groupCode: String): UniversalGroupEntity?

  @Query("SELECT * FROM universal_groups WHERE cloudId = :cloudId LIMIT 1")
  suspend fun getGroupByCloudId(cloudId: String): UniversalGroupEntity?

  @Query("SELECT groupCode FROM universal_groups WHERE groupCode LIKE :prefixPattern")
  suspend fun getGroupCodesByPrefix(prefixPattern: String): List<String>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGroup(group: UniversalGroupEntity): Long

  @Update
  suspend fun updateGroup(group: UniversalGroupEntity)

  @Query("DELETE FROM universal_groups WHERE id = :id")
  suspend fun deleteGroupById(id: Long)
}

@Dao
interface ProductDao {
  @Query("SELECT * FROM products ORDER BY updatedAt DESC")
  fun observeAllProducts(): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products ORDER BY updatedAt DESC")
  suspend fun getAllProductsOnce(): List<ProductEntity>

  @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
  fun observeProductById(id: Long): Flow<ProductEntity?>

  @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
  suspend fun getProductById(id: Long): ProductEntity?

  @Query("SELECT * FROM products WHERE cloudId = :cloudId LIMIT 1")
  suspend fun getProductByCloudId(cloudId: String): ProductEntity?

  @Query("SELECT * FROM products WHERE universalGroupId = :groupCode")
  suspend fun getProductsByUniversalGroupId(groupCode: String): List<ProductEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProduct(product: ProductEntity): Long

  @Update
  suspend fun updateProduct(product: ProductEntity)

  @Query("DELETE FROM products WHERE id = :id")
  suspend fun deleteProductById(id: Long)
}
