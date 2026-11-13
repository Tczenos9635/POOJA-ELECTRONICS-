package com.example.data

import androidx.room.withTransaction
import com.example.data.remote.OfficialCatalogRepository
import com.example.data.remote.OfficialUniversalGroupDoc
import com.example.data.remote.Product
import com.example.data.remote.ShopCloudSyncRepository
import com.example.data.remote.UniversalModel
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class InventoryRepository(
  private val database: AppDatabase,
  private val officialCatalogRepository: OfficialCatalogRepository? = null,
  private val cloudSyncRepository: ShopCloudSyncRepository? = null,
) {
  private val groupDao = database.universalGroupDao()
  private val productDao = database.productDao()

  private val _officialSyncError = MutableStateFlow<String?>(null)
  val officialSyncError: StateFlow<String?> = _officialSyncError.asStateFlow()

  private val officialDocsFlow: Flow<List<OfficialUniversalGroupDoc>> =
    officialCatalogRepository?.observeOfficialGroups()
      ?.onEach { _officialSyncError.value = null }
      ?.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        _officialSyncError.value = rootError.localizedMessage ?: "Failed to sync official updates"
        emit(emptyList())
      }
      ?: flowOf(emptyList())

  private val publishedUniversalModelsFlow: Flow<List<UniversalModel>> =
    cloudSyncRepository?.observePublishedUniversalModels()
      ?.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        _officialSyncError.value = rootError.localizedMessage ?: "Failed to sync universal models"
        emit(emptyList())
      }
      ?: flowOf(emptyList())

  val officialGroups: Flow<List<UniversalGroup>> =
    combine(
      officialDocsFlow,
      publishedUniversalModelsFlow,
      productDao.observeAllProducts(),
    ) { officialDocs, publishedModels, products ->
      val productsByGroupCode = products
        .filter { !it.universalGroupId.isNullOrBlank() }
        .groupBy { it.universalGroupId!!.uppercase() }

      val fromOfficialCatalog = officialDocs.map { doc ->
        val linked = productsByGroupCode[doc.groupCode.uppercase()]?.firstOrNull()
        doc.toDomain(
          linkedProductId = linked?.id,
          stockQuantity = linked?.stockQuantity ?: 0,
          minimumStock = linked?.minimumStock ?: 3,
          boxNumber = linked?.boxNumber ?: "",
        )
      }
      val seenCodes = fromOfficialCatalog.map { it.groupCode.uppercase() }.toSet()

      val fromPublishedUniversalModels = publishedModels
        .filter { it.groupCode.uppercase() !in seenCodes }
        .map { doc ->
          val linked = productsByGroupCode[doc.groupCode.uppercase()]?.firstOrNull()
          doc.toDomain(
            linkedProductId = linked?.id,
            stockQuantity = linked?.stockQuantity ?: 0,
            minimumStock = linked?.minimumStock ?: 3,
            boxNumber = linked?.boxNumber ?: "",
          )
        }

      fromOfficialCatalog + fromPublishedUniversalModels
    }

  val universalGroups: Flow<List<UniversalGroup>> =
    combine(
      officialGroups,
      groupDao.observeAllGroups(),
      productDao.observeAllProducts(),
    ) { officialDomain, localGroups, products ->
      val productsByGroupCode = products
        .filter { !it.universalGroupId.isNullOrBlank() }
        .groupBy { it.universalGroupId!!.uppercase() }
      val officialCodes = officialDomain.map { it.groupCode.uppercase() }.toSet()

      val localDomain = localGroups
        .filter { it.groupCode.uppercase() !in officialCodes }
        .map { entity ->
          val linked = productsByGroupCode[entity.groupCode.uppercase()]?.firstOrNull()
          val compat = ModelListCodec.decode(entity.compatibleModelsJson)
          UniversalGroup(
            id = entity.id,
            groupCode = entity.groupCode,
            category = PartCategory.fromName(entity.category),
            mainModel = entity.mainModel,
            brand = entity.brand,
            screenSize = entity.screenSize,
            compatibleModels = compat,
            linkedProductId = linked?.id,
            stockQuantity = linked?.stockQuantity ?: 0,
            minimumStock = linked?.minimumStock ?: 3,
            boxNumber = linked?.boxNumber ?: "",
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
          )
        }

      officialDomain + localDomain
    }

  val shopProducts: Flow<List<ShopProduct>> =
    combine(
      productDao.observeAllProducts(),
      groupDao.observeAllGroups(),
      officialGroups,
    ) { products, groups, officialDomain ->
      val localGroupMap = groups.associateBy { it.groupCode.uppercase() }
      val officialMap = officialDomain.associateBy { it.groupCode.uppercase() }

      products.map { entity ->
        val codeKey = entity.universalGroupId?.uppercase()
        val officialDomainItem = codeKey?.let { officialMap[it] }
        val localGroup = codeKey?.let { localGroupMap[it] }

        if (officialDomainItem != null) {
          ShopProduct(
            id = entity.id,
            productName = entity.productName,
            category = officialDomainItem.category,
            mainModel = officialDomainItem.mainModel,
            brand = officialDomainItem.brand,
            screenSize = officialDomainItem.screenSize.ifBlank { entity.screenSize },
            stockQuantity = entity.stockQuantity,
            minimumStock = entity.minimumStock,
            boxNumber = entity.boxNumber,
            universalGroupId = officialDomainItem.groupCode,
            universalGroupDbId = officialDomainItem.id,
            compatibleModels = officialDomainItem.compatibleModels,
            notes = entity.notes,
            createdAt = entity.createdAt,
            updatedAt = maxOf(entity.updatedAt, officialDomainItem.updatedAt),
            isOfficialLinked = true,
            officialReleaseNote = officialDomainItem.releaseNote,
          )
        } else {
          val resolvedCategory = localGroup?.let { PartCategory.fromName(it.category) }
            ?: PartCategory.fromName(entity.category)
          val resolvedMainModel = localGroup?.mainModel ?: entity.mainModel
          val resolvedBrand = localGroup?.brand ?: entity.brand
          val resolvedScreenSize = localGroup?.screenSize?.ifBlank { entity.screenSize }
            ?: entity.screenSize
          val resolvedCompat = localGroup?.let {
            ModelListCodec.decode(it.compatibleModelsJson)
          } ?: ModelListCodec.decode(entity.compatibleModelsJson)

          ShopProduct(
            id = entity.id,
            productName = entity.productName,
            category = resolvedCategory,
            mainModel = resolvedMainModel,
            brand = resolvedBrand,
            screenSize = resolvedScreenSize,
            stockQuantity = entity.stockQuantity,
            minimumStock = entity.minimumStock,
            boxNumber = entity.boxNumber,
            universalGroupId = localGroup?.groupCode ?: entity.universalGroupId,
            universalGroupDbId = localGroup?.id,
            compatibleModels = resolvedCompat,
            notes = entity.notes,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
          )
        }
      }
    }

  fun observeProduct(productId: Long): Flow<ShopProduct?> =
    shopProducts.map { list -> list.firstOrNull { it.id == productId } }

  /**
   * Starts real-time Firestore snapshot listeners for `universal_models` and `products`
   * scoped to the current authenticated user, keeping Room and all app installations synchronized.
   */
  suspend fun syncFromCloud() {
    val syncRepo = cloudSyncRepository ?: return
    coroutineScope {
      launch {
        var pushedInitialLocalGroups = false
        syncRepo.observeMyUniversalModels()
          .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            _officialSyncError.value = rootError.localizedMessage ?: "Universal models sync error"
          }
          .collect { remoteDocs ->
            reconcileRemoteUniversalModels(remoteDocs)
            if (!pushedInitialLocalGroups) {
              pushedInitialLocalGroups = true
              pushUnsyncedLocalGroups(syncRepo)
            }
          }
      }

      launch {
        var pushedInitialLocalProducts = false
        syncRepo.observeMyProducts()
          .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            _officialSyncError.value = rootError.localizedMessage ?: "Products sync error"
          }
          .collect { remoteDocs ->
            reconcileRemoteProducts(remoteDocs)
            if (!pushedInitialLocalProducts) {
              pushedInitialLocalProducts = true
              pushUnsyncedLocalProducts(syncRepo)
            }
          }
      }
    }
  }

  private suspend fun reconcileRemoteUniversalModels(remoteDocs: List<UniversalModel>) {
    database.withTransaction {
      val localGroups = groupDao.getAllGroupsOnce()
      val localByCloudId = localGroups.filter { it.cloudId.isNotBlank() }.associateBy { it.cloudId }
      val localByCode = localGroups.associateBy { it.groupCode.uppercase() }
      val remoteIds = remoteDocs.map { it.id }.toSet()

      remoteDocs.forEach { doc ->
        val existingByCloud = localByCloudId[doc.id]
        val existingByCode = localByCode[doc.groupCode.uppercase()]
        val existing = existingByCloud ?: existingByCode
        val remoteUpdatedMs = doc.updatedAtMillis()

        if (existing == null) {
          groupDao.insertGroup(doc.toEntity())
        } else if (
          existing.cloudId.isBlank() ||
          remoteUpdatedMs > existing.updatedAt ||
          existing.mainModel != doc.mainModel ||
          existing.compatibleModelsJson != ModelListCodec.encode(doc.compatibleModels)
        ) {
          groupDao.updateGroup(
            existing.copy(
              cloudId = doc.id,
              groupCode = doc.groupCode,
              category = PartCategory.fromName(doc.category).name,
              mainModel = doc.mainModel,
              brand = doc.brand.ifBlank { BrandParser.extractBrand(doc.mainModel) },
              screenSize = doc.screenSize,
              compatibleModelsJson = ModelListCodec.encode(doc.compatibleModels),
              updatedAt = maxOf(existing.updatedAt, remoteUpdatedMs),
            ),
          )
        }
      }

      // Remove items deleted on another installation
      localGroups
        .filter { it.cloudId.isNotBlank() && it.cloudId !in remoteIds }
        .forEach { deleted -> groupDao.deleteGroupById(deleted.id) }
    }
  }

  private suspend fun reconcileRemoteProducts(remoteDocs: List<Product>) {
    database.withTransaction {
      val localProducts = productDao.getAllProductsOnce()
      val localByCloudId = localProducts.filter { it.cloudId.isNotBlank() }.associateBy { it.cloudId }
      val remoteIds = remoteDocs.map { it.id }.toSet()

      remoteDocs.forEach { doc ->
        val existing = localByCloudId[doc.id]
        val remoteUpdatedMs = doc.updatedAtMillis()
        val mappedCompatJson = ModelListCodec.encode(doc.compatibleModels)

        if (existing == null) {
          productDao.insertProduct(doc.toEntity())
        } else if (
          remoteUpdatedMs > existing.updatedAt ||
          existing.stockQuantity != doc.stockQuantity.toInt().coerceAtLeast(0) ||
          existing.productName != doc.productName ||
          existing.compatibleModelsJson != mappedCompatJson
        ) {
          productDao.updateProduct(
            existing.copy(
              cloudId = doc.id,
              productName = doc.productName,
              category = PartCategory.fromName(doc.category).name,
              mainModel = doc.mainModel,
              brand = doc.brand.ifBlank { BrandParser.extractBrand(doc.mainModel) },
              screenSize = doc.screenSize,
              stockQuantity = doc.stockQuantity.toInt().coerceAtLeast(0),
              minimumStock = doc.minimumStock.toInt().coerceAtLeast(0),
              boxNumber = doc.boxNumber,
              universalGroupId = doc.universalGroupId.ifBlank { null },
              compatibleModelsJson = mappedCompatJson,
              notes = doc.notes,
              updatedAt = maxOf(existing.updatedAt, remoteUpdatedMs),
            ),
          )
        }
      }

      // Remove items deleted on another installation
      localProducts
        .filter { it.cloudId.isNotBlank() && it.cloudId !in remoteIds }
        .forEach { deleted -> productDao.deleteProductById(deleted.id) }
    }
  }

  private suspend fun pushUnsyncedLocalGroups(syncRepo: ShopCloudSyncRepository) {
    val unsynced = groupDao.getAllGroupsOnce().filter { it.cloudId.isBlank() }
    unsynced.forEach { entity ->
      val result = syncRepo.upsertUniversalModel(
        docId = null,
        category = PartCategory.fromName(entity.category),
        groupCode = entity.groupCode,
        mainModel = entity.mainModel,
        screenSize = entity.screenSize,
        compatibleModels = ModelListCodec.decode(entity.compatibleModelsJson),
      )
      result.getOrNull()?.let { docId ->
        groupDao.getGroupById(entity.id)?.let { current ->
          groupDao.updateGroup(current.copy(cloudId = docId))
        }
      }
    }
  }

  private suspend fun pushUnsyncedLocalProducts(syncRepo: ShopCloudSyncRepository) {
    val unsynced = productDao.getAllProductsOnce().filter { it.cloudId.isBlank() }
    unsynced.forEach { entity ->
      val result = syncRepo.upsertProduct(
        docId = null,
        category = PartCategory.fromName(entity.category),
        productName = entity.productName,
        mainModel = entity.mainModel,
        brand = entity.brand,
        screenSize = entity.screenSize,
        stockQuantity = entity.stockQuantity,
        minimumStock = entity.minimumStock,
        boxNumber = entity.boxNumber,
        universalGroupId = entity.universalGroupId,
        compatibleModels = ModelListCodec.decode(entity.compatibleModelsJson),
        notes = entity.notes,
      )
      result.getOrNull()?.let { docId ->
        productDao.getProductById(entity.id)?.let { current ->
          productDao.updateProduct(current.copy(cloudId = docId))
        }
      }
    }
  }

  suspend fun trackOfficialGroupInShop(
    group: UniversalGroup,
    initialStock: Int = 1,
    minimumStock: Int = 3,
    boxNumber: String = "",
  ): Long {
    val now = System.currentTimeMillis()
    val cleanCode = group.groupCode.trim().uppercase()
    val insertedId = database.withTransaction {
      val existingLinked = productDao.getProductsByUniversalGroupId(cleanCode).firstOrNull()
      if (existingLinked != null) {
        return@withTransaction existingLinked.id
      }
      productDao.insertProduct(
        ProductEntity(
          productName = "${group.mainModel} ${group.category.displayName}",
          category = group.category.name,
          mainModel = group.mainModel,
          brand = group.brand,
          screenSize = group.screenSize,
          stockQuantity = initialStock.coerceAtLeast(0),
          minimumStock = minimumStock.coerceAtLeast(0),
          boxNumber = boxNumber.trim().uppercase(),
          universalGroupId = cleanCode,
          compatibleModelsJson = ModelListCodec.encode(group.compatibleModels),
          notes = group.releaseNote,
          createdAt = now,
          updatedAt = now,
        ),
      )
    }
    syncProductToCloudById(insertedId)
    return insertedId
  }

  suspend fun generateNextGroupCode(category: PartCategory): String {
    val prefix = "${category.shortPrefix}-"
    val existing = groupDao.getGroupCodesByPrefix("$prefix%")
    val maxNum = existing.mapNotNull { code ->
      code.removePrefix(prefix).toIntOrNull()
    }.maxOrNull() ?: 0
    return "$prefix${(maxNum + 1).toString().padStart(3, '0')}"
  }

  suspend fun saveNewUniversalGroup(
    category: PartCategory,
    mainModel: String,
    screenSize: String,
    compatibleModels: List<String>,
    customGroupCode: String = "",
    initialStock: Int = 0,
    minimumStock: Int = 3,
    boxNumber: String = "",
  ): Long {
    val now = System.currentTimeMillis()
    val cleanMain = mainModel.trim()
    val cleanScreen = screenSize.trim()
    val cleanCompat = compatibleModels
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.equals(cleanMain, ignoreCase = true) }
      .distinctBy { it.lowercase() }
    val cleanBrand = BrandParser.extractBrand(cleanMain)

    val (groupId, productId) = database.withTransaction {
      val requestedCode = customGroupCode.trim().uppercase()
      val finalCode = if (requestedCode.isNotEmpty() && groupDao.getGroupByCode(requestedCode) == null) {
        requestedCode
      } else {
        generateNextGroupCode(category)
      }

      val gId = groupDao.insertGroup(
        UniversalGroupEntity(
          groupCode = finalCode,
          category = category.name,
          mainModel = cleanMain,
          brand = cleanBrand,
          screenSize = cleanScreen,
          compatibleModelsJson = ModelListCodec.encode(cleanCompat),
          createdAt = now,
          updatedAt = now,
        ),
      )

      // Create the single physical stock entry linked to this universal group (no duplicate stock per model)
      val pId = productDao.insertProduct(
        ProductEntity(
          productName = "$cleanMain ${category.displayName}",
          category = category.name,
          mainModel = cleanMain,
          brand = cleanBrand,
          screenSize = cleanScreen,
          stockQuantity = initialStock.coerceAtLeast(0),
          minimumStock = minimumStock.coerceAtLeast(0),
          boxNumber = boxNumber.trim().uppercase(),
          universalGroupId = finalCode,
          compatibleModelsJson = ModelListCodec.encode(cleanCompat),
          notes = "",
          createdAt = now,
          updatedAt = now,
        ),
      )
      gId to pId
    }

    syncUniversalGroupToCloudById(groupId)
    syncProductToCloudById(productId)
    return groupId
  }

  suspend fun updateUniversalGroup(
    groupId: Long,
    category: PartCategory,
    groupCode: String,
    mainModel: String,
    screenSize: String,
    compatibleModels: List<String>,
  ) {
    val now = System.currentTimeMillis()
    val cleanMain = mainModel.trim()
    val cleanBrand = BrandParser.extractBrand(cleanMain)
    val cleanScreen = screenSize.trim()
    val cleanCompat = compatibleModels
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.equals(cleanMain, ignoreCase = true) }
      .distinctBy { it.lowercase() }
    val compatJson = ModelListCodec.encode(cleanCompat)

    val linkedProductIds = database.withTransaction {
      val existing = groupDao.getGroupById(groupId) ?: return@withTransaction emptyList()
      val requestedCode = groupCode.trim().uppercase().ifEmpty { existing.groupCode }
      val codeConflict = groupDao.getGroupByCode(requestedCode)
      val finalCode = if (codeConflict == null || codeConflict.id == groupId) requestedCode else existing.groupCode

      groupDao.updateGroup(
        existing.copy(
          groupCode = finalCode,
          category = category.name,
          mainModel = cleanMain,
          brand = cleanBrand,
          screenSize = cleanScreen,
          compatibleModelsJson = compatJson,
          updatedAt = now,
        ),
      )

      val linkedProducts = productDao.getProductsByUniversalGroupId(existing.groupCode)
      linkedProducts.forEach { product ->
        productDao.updateProduct(
          product.copy(
            category = category.name,
            mainModel = cleanMain,
            brand = cleanBrand,
            screenSize = cleanScreen,
            universalGroupId = finalCode,
            compatibleModelsJson = compatJson,
            updatedAt = now,
          ),
        )
      }
      linkedProducts.map { it.id }
    }

    syncUniversalGroupToCloudById(groupId)
    linkedProductIds.forEach { syncProductToCloudById(it) }
  }

  suspend fun ensureUniversalGroupForProduct(productId: Long): Long? {
    val groupId = database.withTransaction {
      val product = productDao.getProductById(productId) ?: return@withTransaction null
      val existingCode = product.universalGroupId
      if (!existingCode.isNullOrBlank()) {
        val existingGroup = groupDao.getGroupByCode(existingCode)
        if (existingGroup != null) return@withTransaction existingGroup.id
      }
      val category = PartCategory.fromName(product.category)
      val newCode = generateNextGroupCode(category)
      val now = System.currentTimeMillis()
      val gId = groupDao.insertGroup(
        UniversalGroupEntity(
          groupCode = newCode,
          category = category.name,
          mainModel = product.mainModel,
          brand = product.brand,
          screenSize = product.screenSize,
          compatibleModelsJson = product.compatibleModelsJson,
          createdAt = now,
          updatedAt = now,
        ),
      )
      productDao.updateProduct(product.copy(universalGroupId = newCode, updatedAt = now))
      gId
    }
    if (groupId != null) {
      syncUniversalGroupToCloudById(groupId)
      syncProductToCloudById(productId)
    }
    return groupId
  }

  suspend fun deleteUniversalGroup(groupId: Long) {
    val resultPair = database.withTransaction {
      val group = groupDao.getGroupById(groupId) ?: return@withTransaction null
      val linkedProducts = productDao.getProductsByUniversalGroupId(group.groupCode)
      linkedProducts.forEach { product ->
        productDao.updateProduct(
          product.copy(
            universalGroupId = null,
            updatedAt = System.currentTimeMillis(),
          ),
        )
      }
      groupDao.deleteGroupById(groupId)
      group.cloudId to linkedProducts.map { it.id }
    } ?: return

    val (deletedCloudId, linkedIds) = resultPair
    if (deletedCloudId.isNotBlank()) {
      cloudSyncRepository?.deleteUniversalModel(deletedCloudId)
    }
    linkedIds.forEach { syncProductToCloudById(it) }
  }

  suspend fun saveProduct(
    productId: Long?,
    category: PartCategory,
    productName: String,
    mainModel: String,
    brand: String,
    screenSize: String,
    stockQuantity: Int,
    minimumStock: Int,
    boxNumber: String,
    selectedUniversalGroupCode: String?,
    compatibleModels: List<String>,
    notes: String,
  ): Long {
    val now = System.currentTimeMillis()
    val cleanMain = mainModel.trim()
    val cleanBrand = brand.trim().ifEmpty { BrandParser.extractBrand(cleanMain) }
    val cleanName = productName.trim().ifEmpty { "$cleanMain ${category.displayName}" }
    val cleanScreen = screenSize.trim()
    val cleanBox = boxNumber.trim().uppercase()
    val cleanCompat = compatibleModels
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.equals(cleanMain, ignoreCase = true) }
      .distinctBy { it.lowercase() }
    val compatJson = ModelListCodec.encode(cleanCompat)

    val (savedProductId, touchedGroupId) = database.withTransaction {
      var finalGroupCode = selectedUniversalGroupCode?.trim()?.uppercase()?.ifEmpty { null }
      var groupDbId: Long? = null

      if (finalGroupCode != null) {
        val group = groupDao.getGroupByCode(finalGroupCode)
        if (group != null) {
          groupDao.updateGroup(
            group.copy(
              category = category.name,
              mainModel = cleanMain,
              brand = cleanBrand,
              screenSize = cleanScreen,
              compatibleModelsJson = compatJson,
              updatedAt = now,
            ),
          )
          groupDbId = group.id
        }
      } else if (cleanCompat.isNotEmpty()) {
        // Automatically create a universal group when user enters compatible models for a product
        finalGroupCode = generateNextGroupCode(category)
        groupDbId = groupDao.insertGroup(
          UniversalGroupEntity(
            groupCode = finalCodeOrDefault(finalGroupCode),
            category = category.name,
            mainModel = cleanMain,
            brand = cleanBrand,
            screenSize = cleanScreen,
            compatibleModelsJson = compatJson,
            createdAt = now,
            updatedAt = now,
          ),
        )
      }

      if (productId != null && productId > 0L) {
        val existing = productDao.getProductById(productId)
        if (existing != null) {
          productDao.updateProduct(
            existing.copy(
              productName = cleanName,
              category = category.name,
              mainModel = cleanMain,
              brand = cleanBrand,
              screenSize = cleanScreen,
              stockQuantity = stockQuantity.coerceAtLeast(0),
              minimumStock = minimumStock.coerceAtLeast(0),
              boxNumber = cleanBox,
              universalGroupId = finalGroupCode,
              compatibleModelsJson = compatJson,
              notes = notes.trim(),
              updatedAt = now,
            ),
          )
          return@withTransaction existing.id to groupDbId
        }
      }

      val insertedId = productDao.insertProduct(
        ProductEntity(
          productName = cleanName,
          category = category.name,
          mainModel = cleanMain,
          brand = cleanBrand,
          screenSize = cleanScreen,
          stockQuantity = stockQuantity.coerceAtLeast(0),
          minimumStock = minimumStock.coerceAtLeast(0),
          boxNumber = cleanBox,
          universalGroupId = finalGroupCode,
          compatibleModelsJson = compatJson,
          notes = notes.trim(),
          createdAt = now,
          updatedAt = now,
        ),
      )
      insertedId to groupDbId
    }

    if (touchedGroupId != null) {
      syncUniversalGroupToCloudById(touchedGroupId)
    }
    syncProductToCloudById(savedProductId)
    return savedProductId
  }

  private fun finalCodeOrDefault(code: String): String = code

  suspend fun adjustProductStock(productId: Long, delta: Int) {
    val updatedInfo = database.withTransaction {
      val existing = productDao.getProductById(productId) ?: return@withTransaction null
      val newStock = (existing.stockQuantity + delta).coerceAtLeast(0)
      productDao.updateProduct(
        existing.copy(
          stockQuantity = newStock,
          updatedAt = System.currentTimeMillis(),
        ),
      )
      existing.cloudId to newStock
    } ?: return

    val (cloudId, newStock) = updatedInfo
    if (cloudId.isNotBlank()) {
      cloudSyncRepository?.updateProductStock(cloudId, newStock)
    } else {
      syncProductToCloudById(productId)
    }
  }

  suspend fun setProductStock(productId: Long, newStock: Int) {
    val clamped = newStock.coerceAtLeast(0)
    val cloudId = database.withTransaction {
      val existing = productDao.getProductById(productId) ?: return@withTransaction null
      productDao.updateProduct(
        existing.copy(
          stockQuantity = clamped,
          updatedAt = System.currentTimeMillis(),
        ),
      )
      existing.cloudId
    } ?: return

    if (cloudId.isNotBlank()) {
      cloudSyncRepository?.updateProductStock(cloudId, clamped)
    } else {
      syncProductToCloudById(productId)
    }
  }

  suspend fun deleteProduct(productId: Long) {
    val cloudId = database.withTransaction {
      val existing = productDao.getProductById(productId)
      productDao.deleteProductById(productId)
      existing?.cloudId.orEmpty()
    }
    if (cloudId.isNotBlank()) {
      cloudSyncRepository?.deleteProduct(cloudId)
    }
  }

  private suspend fun syncProductToCloudById(productId: Long) {
    val syncRepo = cloudSyncRepository ?: return
    val entity = productDao.getProductById(productId) ?: return
    val result = syncRepo.upsertProduct(
      docId = entity.cloudId.ifBlank { null },
      category = PartCategory.fromName(entity.category),
      productName = entity.productName,
      mainModel = entity.mainModel,
      brand = entity.brand,
      screenSize = entity.screenSize,
      stockQuantity = entity.stockQuantity,
      minimumStock = entity.minimumStock,
      boxNumber = entity.boxNumber,
      universalGroupId = entity.universalGroupId,
      compatibleModels = ModelListCodec.decode(entity.compatibleModelsJson),
      notes = entity.notes,
    )
    val newDocId = result.getOrNull()
    if (!newDocId.isNullOrBlank() && entity.cloudId != newDocId) {
      productDao.getProductById(productId)?.let { latest ->
        productDao.updateProduct(latest.copy(cloudId = newDocId))
      }
    }
  }

  private suspend fun syncUniversalGroupToCloudById(groupId: Long) {
    val syncRepo = cloudSyncRepository ?: return
    val entity = groupDao.getGroupById(groupId) ?: return
    val result = syncRepo.upsertUniversalModel(
      docId = entity.cloudId.ifBlank { null },
      category = PartCategory.fromName(entity.category),
      groupCode = entity.groupCode,
      mainModel = entity.mainModel,
      screenSize = entity.screenSize,
      compatibleModels = ModelListCodec.decode(entity.compatibleModelsJson),
    )
    val newDocId = result.getOrNull()
    if (!newDocId.isNullOrBlank() && entity.cloudId != newDocId) {
      groupDao.getGroupById(groupId)?.let { latest ->
        groupDao.updateGroup(latest.copy(cloudId = newDocId))
      }
    }
  }
}
