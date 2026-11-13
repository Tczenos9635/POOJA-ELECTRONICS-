package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.BrandParser
import com.example.data.PartCategory
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ShopCloudSyncRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id),
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before syncing Firestore data.")

  fun observeMyProducts(): Flow<List<Product>> {
    val uid = auth.currentUser?.uid ?: ""
    return db.collection(PRODUCTS_COLLECTION)
      .whereEqualTo("userId", uid)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            Product::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null && auth.currentUser?.uid == uid) {
          handleFirestoreError(toLog, OperationType.LIST, PRODUCTS_COLLECTION, auth)
        }
        throw error
      }
  }

  fun observePublishedProducts(): Flow<List<Product>> =
    db.collection(PRODUCTS_COLLECTION)
      .whereEqualTo("isPublished", true)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            Product::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null) {
          handleFirestoreError(toLog, OperationType.LIST, PRODUCTS_COLLECTION, auth)
        }
        throw error
      }

  fun observeMyUniversalModels(): Flow<List<UniversalModel>> {
    val uid = auth.currentUser?.uid ?: ""
    return db.collection(UNIVERSAL_MODELS_COLLECTION)
      .whereEqualTo("userId", uid)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            UniversalModel::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null && auth.currentUser?.uid == uid) {
          handleFirestoreError(toLog, OperationType.LIST, UNIVERSAL_MODELS_COLLECTION, auth)
        }
        throw error
      }
  }

  fun observePublishedUniversalModels(): Flow<List<UniversalModel>> =
    db.collection(UNIVERSAL_MODELS_COLLECTION)
      .whereEqualTo("isPublished", true)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            UniversalModel::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null) {
          handleFirestoreError(toLog, OperationType.LIST, UNIVERSAL_MODELS_COLLECTION, auth)
        }
        throw error
      }

  suspend fun upsertProduct(
    docId: String? = null,
    category: PartCategory,
    productName: String,
    mainModel: String,
    brand: String,
    screenSize: String,
    stockQuantity: Int,
    minimumStock: Int,
    boxNumber: String,
    universalGroupId: String?,
    compatibleModels: List<String>,
    notes: String,
    isPublished: Boolean = false,
  ): Result<String> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }

    val cleanMain = mainModel.trim()
    val cleanBrand = brand.trim().ifEmpty { BrandParser.extractBrand(cleanMain) }
    val cleanName = productName.trim().ifEmpty { "$cleanMain ${category.displayName}" }
    val cleanCompat = compatibleModels
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.equals(cleanMain, ignoreCase = true) }
      .distinctBy { it.lowercase() }
      .take(100)
    val cleanGroupCode = universalGroupId?.trim()?.uppercase().orEmpty()

    val collection = db.collection(PRODUCTS_COLLECTION)
    val docRef = if (docId.isNullOrBlank()) collection.document() else collection.document(docId)

    val updateFields = mapOf(
      "productName" to cleanName,
      "category" to category.name,
      "mainModel" to cleanMain,
      "brand" to cleanBrand,
      "screenSize" to screenSize.trim(),
      "stockQuantity" to stockQuantity.coerceAtLeast(0).toLong(),
      "minimumStock" to minimumStock.coerceAtLeast(0).toLong(),
      "boxNumber" to boxNumber.trim().uppercase(),
      "universalGroupId" to cleanGroupCode,
      "compatibleModels" to cleanCompat,
      "notes" to notes.trim(),
      "isPublished" to isPublished,
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    val createPayload = updateFields + mapOf(
      "userId" to uid,
      "createdAt" to FieldValue.serverTimestamp(),
    )

    return try {
      if (docId.isNullOrBlank()) {
        docRef.set(createPayload).await()
      } else {
        try {
          docRef.update(updateFields).await()
        } catch (e: FirebaseFirestoreException) {
          if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
            docRef.set(createPayload).await()
          } else {
            throw e
          }
        }
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val op = if (docId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE
      handleFirestoreError(e, op, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun saveProduct(product: Product): Result<String> =
    upsertProduct(
      docId = product.id.ifBlank { null },
      category = product.resolvedCategory(),
      productName = product.productName,
      mainModel = product.mainModel,
      brand = product.brand,
      screenSize = product.screenSize,
      stockQuantity = product.resolvedStockQuantity(),
      minimumStock = product.resolvedMinimumStock(),
      boxNumber = product.boxNumber,
      universalGroupId = product.universalGroupId.ifBlank { null },
      compatibleModels = product.compatibleModels,
      notes = product.notes,
      isPublished = product.isPublished,
    )

  suspend fun updateProductStock(docId: String, newStock: Int): Result<Unit> {
    try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    require(docId.isNotBlank()) { "Product document ID must not be blank" }
    val docRef = db.collection(PRODUCTS_COLLECTION).document(docId)
    return try {
      docRef.update(
        mapOf(
          "stockQuantity" to newStock.coerceAtLeast(0).toLong(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun deleteProduct(docId: String): Result<Unit> {
    try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    require(docId.isNotBlank()) { "Product document ID must not be blank" }
    val docRef = db.collection(PRODUCTS_COLLECTION).document(docId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun upsertUniversalModel(
    docId: String? = null,
    category: PartCategory,
    groupCode: String,
    mainModel: String,
    screenSize: String,
    compatibleModels: List<String>,
    releaseNote: String = "",
    isPublished: Boolean = false,
  ): Result<String> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }

    val cleanMain = mainModel.trim()
    val cleanBrand = BrandParser.extractBrand(cleanMain)
    val cleanCode = groupCode.trim().uppercase()
    val cleanCompat = compatibleModels
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.equals(cleanMain, ignoreCase = true) }
      .distinctBy { it.lowercase() }
      .take(100)

    val collection = db.collection(UNIVERSAL_MODELS_COLLECTION)
    val docRef = if (docId.isNullOrBlank()) collection.document() else collection.document(docId)

    val updateFields = mapOf(
      "updatedByUid" to uid,
      "groupCode" to cleanCode,
      "category" to category.name,
      "mainModel" to cleanMain,
      "brand" to cleanBrand,
      "screenSize" to screenSize.trim(),
      "compatibleModels" to cleanCompat,
      "releaseNote" to releaseNote.trim(),
      "isPublished" to isPublished,
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    val createPayload = updateFields + mapOf(
      "userId" to uid,
      "createdAt" to FieldValue.serverTimestamp(),
    )

    return try {
      if (docId.isNullOrBlank()) {
        docRef.set(createPayload).await()
      } else {
        try {
          docRef.update(updateFields).await()
        } catch (e: FirebaseFirestoreException) {
          if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
            docRef.set(createPayload).await()
          } else {
            throw e
          }
        }
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val op = if (docId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE
      handleFirestoreError(e, op, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun saveUniversalModel(model: UniversalModel): Result<String> =
    upsertUniversalModel(
      docId = model.id.ifBlank { null },
      category = model.resolvedCategory(),
      groupCode = model.groupCode,
      mainModel = model.mainModel,
      screenSize = model.screenSize,
      compatibleModels = model.compatibleModels,
      releaseNote = model.releaseNote,
      isPublished = model.isPublished,
    )

  suspend fun deleteUniversalModel(docId: String): Result<Unit> {
    try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    require(docId.isNotBlank()) { "Universal model document ID must not be blank" }
    val docRef = db.collection(UNIVERSAL_MODELS_COLLECTION).document(docId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      Result.failure(e)
    }
  }

  companion object {
    const val PRODUCTS_COLLECTION = "products"
    const val UNIVERSAL_MODELS_COLLECTION = "universal_models"
  }
}
