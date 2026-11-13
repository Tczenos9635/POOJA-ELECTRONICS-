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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class OfficialCatalogRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id),
    ),
    auth = Firebase.auth,
  )

  fun isCurrentUserAdmin(): Boolean {
    val user = auth.currentUser ?: return false
    return user.email.equals(ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
  }

  suspend fun checkIsAdminAccount(): Boolean {
    val user = auth.currentUser ?: return false
    if (user.email.equals(ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified) return true
    return try {
      val marker = db.collection(ADMINS_COLLECTION).document(user.uid).get().await()
      marker.exists()
    } catch (_: Exception) {
      false
    }
  }

  fun observeOfficialGroups(): Flow<List<OfficialUniversalGroupDoc>> =
    db.collection(COLLECTION_NAME)
      .whereEqualTo("isPublished", true)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            OfficialUniversalGroupDoc::class.java,
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
          handleFirestoreError(toLog, OperationType.LIST, COLLECTION_NAME, auth)
        }
        throw error
      }

  suspend fun publishOfficialGroup(
    category: PartCategory,
    groupCode: String,
    mainModel: String,
    screenSize: String,
    compatibleModels: List<String>,
    releaseNote: String,
  ): Result<String> {
    val uid = auth.currentUser?.uid ?: ""
    val cleanMain = mainModel.trim()
    val cleanBrand = BrandParser.extractBrand(cleanMain)
    val cleanCode = groupCode.trim().uppercase()
    val cleanCompat = compatibleModels
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.equals(cleanMain, ignoreCase = true) }
      .distinctBy { it.lowercase() }

    val docRef = db.collection(COLLECTION_NAME).document()
    val payload = mapOf(
      "groupCode" to cleanCode,
      "category" to category.name,
      "mainModel" to cleanMain,
      "brand" to cleanBrand,
      "screenSize" to screenSize.trim(),
      "compatibleModels" to cleanCompat,
      "releaseNote" to releaseNote.trim(),
      "isPublished" to true,
      "updatedByUid" to uid,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun updateOfficialGroup(
    docId: String,
    category: PartCategory,
    groupCode: String,
    mainModel: String,
    screenSize: String,
    compatibleModels: List<String>,
    releaseNote: String,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: ""
    require(docId.isNotBlank()) { "Official document ID must not be blank for update" }
    val cleanMain = mainModel.trim()
    val cleanBrand = BrandParser.extractBrand(cleanMain)
    val cleanCode = groupCode.trim().uppercase()
    val cleanCompat = compatibleModels
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.equals(cleanMain, ignoreCase = true) }
      .distinctBy { it.lowercase() }

    val docRef = db.collection(COLLECTION_NAME).document(docId)
    val updates = mapOf(
      "groupCode" to cleanCode,
      "category" to category.name,
      "mainModel" to cleanMain,
      "brand" to cleanBrand,
      "screenSize" to screenSize.trim(),
      "compatibleModels" to cleanCompat,
      "releaseNote" to releaseNote.trim(),
      "isPublished" to true,
      "updatedByUid" to uid,
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun deleteOfficialGroup(docId: String): Result<Unit> {
    require(docId.isNotBlank()) { "Official document ID must not be blank for delete" }
    val docRef = db.collection(COLLECTION_NAME).document(docId)
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
    const val COLLECTION_NAME = "official_universal_groups"
    const val ADMINS_COLLECTION = "admins"
    const val ADMIN_EMAIL = "sbstudio17@gmail.com"
  }
}
