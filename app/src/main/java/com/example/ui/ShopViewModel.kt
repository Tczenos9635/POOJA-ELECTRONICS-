package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BrandModelsSection
import com.example.data.InventoryRepository
import com.example.data.InventorySummary
import com.example.data.PartCategory
import com.example.data.ProductSearchResult
import com.example.data.SearchEngine
import com.example.data.ShopProduct
import com.example.data.StockFilter
import com.example.data.StockStatus
import com.example.data.UniversalGroup
import com.example.data.remote.OfficialCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShopViewModel(
  private val repository: InventoryRepository,
  private val officialCatalogRepository: OfficialCatalogRepository? = null,
  val currentUserEmail: String? = null,
  initialIsAdmin: Boolean = false,
  private val enableCloudSync: Boolean = false,
) : ViewModel() {

  val allProducts: StateFlow<List<ShopProduct>> = repository.shopProducts
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allUniversalGroups: StateFlow<List<UniversalGroup>> = repository.universalGroups
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val officialGroups: StateFlow<List<UniversalGroup>> = repository.officialGroups
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val officialSyncError: StateFlow<String?> = repository.officialSyncError

  private val _isAdmin = MutableStateFlow(initialIsAdmin)
  val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

  private val _adminActionLoading = MutableStateFlow(false)
  val adminActionLoading: StateFlow<Boolean> = _adminActionLoading.asStateFlow()

  private val _adminActionError = MutableStateFlow<String?>(null)
  val adminActionError: StateFlow<String?> = _adminActionError.asStateFlow()

  init {
    viewModelScope.launch {
      val verified = officialCatalogRepository?.checkIsAdminAccount() ?: initialIsAdmin
      _isAdmin.value = verified
    }
    if (enableCloudSync) {
      viewModelScope.launch {
        repository.syncFromCloud()
      }
    }
  }

  // Home Search State
  private val _homeSearchQuery = MutableStateFlow("")
  val homeSearchQuery: StateFlow<String> = _homeSearchQuery.asStateFlow()

  private val _homeResultCategoryFilter = MutableStateFlow<PartCategory?>(null)
  val homeResultCategoryFilter: StateFlow<PartCategory?> = _homeResultCategoryFilter.asStateFlow()

  // Combines tracked shop products with untracked Official Universal Groups for instant global search
  val homeSearchResults: StateFlow<List<ProductSearchResult>> = combine(
    allProducts,
    officialGroups,
    _homeSearchQuery,
    _homeResultCategoryFilter,
  ) { products, official, query, catFilter ->
    val trackedGroupCodes = products.mapNotNull { it.universalGroupId?.uppercase() }.toSet()
    val untrackedOfficialAsProducts = official
      .filter { it.linkedProductId == null && it.groupCode.uppercase() !in trackedGroupCodes }
      .map { group ->
        ShopProduct(
          id = group.id, // negative ID (< 0) marks untracked official catalog entry
          productName = "${group.mainModel} ${group.category.displayName}",
          category = group.category,
          mainModel = group.mainModel,
          brand = group.brand,
          screenSize = group.screenSize,
          stockQuantity = 0,
          minimumStock = 3,
          boxNumber = "",
          universalGroupId = group.groupCode,
          universalGroupDbId = group.id,
          compatibleModels = group.compatibleModels,
          notes = "",
          createdAt = group.createdAt,
          updatedAt = group.updatedAt,
          isOfficialLinked = true,
          officialReleaseNote = group.releaseNote,
        )
      }

    val matched = SearchEngine.searchProducts(products + untrackedOfficialAsProducts, query)
    if (catFilter != null) {
      matched.filter { it.product.category == catFilter }
    } else {
      matched
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val categoryCounts: StateFlow<Map<PartCategory, Int>> = combine(
    allProducts,
    officialGroups,
  ) { products, official ->
    val trackedGroupCodes = products.mapNotNull { it.universalGroupId?.uppercase() }.toSet()
    val untrackedOfficial = official.filter {
      it.linkedProductId == null && it.groupCode.uppercase() !in trackedGroupCodes
    }
    PartCategory.entries.associateWith { cat ->
      products.count { it.category == cat } + untrackedOfficial.count { it.category == cat }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

  // My Shop State
  private val _shopSearchQuery = MutableStateFlow("")
  val shopSearchQuery: StateFlow<String> = _shopSearchQuery.asStateFlow()

  private val _shopSelectedCategory = MutableStateFlow<PartCategory?>(null)
  val shopSelectedCategory: StateFlow<PartCategory?> = _shopSelectedCategory.asStateFlow()

  private val _shopStockFilter = MutableStateFlow(StockFilter.ALL)
  val shopStockFilter: StateFlow<StockFilter> = _shopStockFilter.asStateFlow()

  val inventorySummary: StateFlow<InventorySummary> = allProducts.map { products ->
    InventorySummary(
      totalProducts = products.size,
      totalPieces = products.sumOf { it.stockQuantity },
      activeCategories = PartCategory.entries.size,
      lowStockCount = products.count { it.stockStatus == StockStatus.LOW_STOCK },
      outOfStockCount = products.count { it.stockStatus == StockStatus.OUT_OF_STOCK },
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventorySummary())

  val filteredShopProducts: StateFlow<List<ProductSearchResult>> = combine(
    allProducts,
    _shopSearchQuery,
    _shopSelectedCategory,
    _shopStockFilter,
  ) { products, query, category, stockFilter ->
    val byCategory = if (category == null) products else products.filter { it.category == category }
    val byStock = when (stockFilter) {
      StockFilter.ALL -> byCategory
      StockFilter.LOW_STOCK -> byCategory.filter { it.stockStatus == StockStatus.LOW_STOCK }
      StockFilter.OUT_OF_STOCK -> byCategory.filter { it.stockStatus == StockStatus.OUT_OF_STOCK }
    }
    SearchEngine.searchProducts(byStock, query)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun updateHomeSearchQuery(query: String) {
    _homeSearchQuery.value = query
    if (query.isBlank()) {
      _homeResultCategoryFilter.value = null
    }
  }

  fun setHomeResultCategoryFilter(category: PartCategory?) {
    _homeResultCategoryFilter.value = if (_homeResultCategoryFilter.value == category) null else category
  }

  fun updateShopSearchQuery(query: String) {
    _shopSearchQuery.value = query
  }

  fun setShopSelectedCategory(category: PartCategory?) {
    _shopSelectedCategory.value = category
  }

  fun toggleShopStockFilter(filter: StockFilter) {
    _shopStockFilter.value = if (_shopStockFilter.value == filter) StockFilter.ALL else filter
  }

  fun clearAdminError() {
    _adminActionError.value = null
  }

  fun observeProduct(productId: Long): Flow<ShopProduct?> = repository.observeProduct(productId)

  fun categoryBrandSections(category: PartCategory, query: String): Flow<List<BrandModelsSection>> =
    combine(allProducts, allUniversalGroups) { products, groups ->
      SearchEngine.groupCategoryModelsByBrand(category, products, query, groups)
    }

  suspend fun generateNextCode(category: PartCategory): String =
    repository.generateNextGroupCode(category)

  fun generateNextOfficialCode(category: PartCategory): String {
    val prefix = "${category.shortPrefix}-"
    val maxNum = allUniversalGroups.value
      .map { it.groupCode.uppercase() }
      .filter { it.startsWith(prefix) }
      .mapNotNull { it.removePrefix(prefix).toIntOrNull() }
      .maxOrNull() ?: 0
    return "$prefix${(maxNum + 1).toString().padStart(3, '0')}"
  }

  fun adjustStock(productId: Long, delta: Int) {
    viewModelScope.launch {
      repository.adjustProductStock(productId, delta)
    }
  }

  fun setStock(productId: Long, newStock: Int) {
    viewModelScope.launch {
      repository.setProductStock(productId, newStock)
    }
  }

  fun deleteProduct(productId: Long, onDeleted: () -> Unit = {}) {
    viewModelScope.launch {
      repository.deleteProduct(productId)
      onDeleted()
    }
  }

  fun trackOfficialGroupInShop(
    group: UniversalGroup,
    initialStock: Int = 1,
    minimumStock: Int = 3,
    boxNumber: String = "",
    onTracked: (Long) -> Unit = {},
  ) {
    viewModelScope.launch {
      val productId = repository.trackOfficialGroupInShop(
        group = group,
        initialStock = initialStock,
        minimumStock = minimumStock,
        boxNumber = boxNumber,
      )
      onTracked(productId)
    }
  }

  fun saveNewUniversalGroup(
    category: PartCategory,
    mainModel: String,
    screenSize: String,
    compatibleModels: List<String>,
    customGroupCode: String,
    initialStock: Int,
    minimumStock: Int,
    boxNumber: String,
    onSaved: (Long) -> Unit,
  ) {
    viewModelScope.launch {
      val id = repository.saveNewUniversalGroup(
        category = category,
        mainModel = mainModel,
        screenSize = screenSize,
        compatibleModels = compatibleModels,
        customGroupCode = customGroupCode,
        initialStock = initialStock,
        minimumStock = minimumStock,
        boxNumber = boxNumber,
      )
      onSaved(id)
    }
  }

  fun updateUniversalGroup(
    groupId: Long,
    category: PartCategory,
    groupCode: String,
    mainModel: String,
    screenSize: String,
    compatibleModels: List<String>,
    onSaved: () -> Unit,
  ) {
    viewModelScope.launch {
      repository.updateUniversalGroup(
        groupId = groupId,
        category = category,
        groupCode = groupCode,
        mainModel = mainModel,
        screenSize = screenSize,
        compatibleModels = compatibleModels,
      )
      onSaved()
    }
  }

  fun ensureUniversalGroupForProduct(productId: Long, onReady: (Long) -> Unit) {
    viewModelScope.launch {
      val groupId = repository.ensureUniversalGroupForProduct(productId)
      if (groupId != null) {
        onReady(groupId)
      }
    }
  }

  fun deleteUniversalGroup(groupId: Long, onDeleted: () -> Unit = {}) {
    viewModelScope.launch {
      repository.deleteUniversalGroup(groupId)
      onDeleted()
    }
  }

  fun publishOfficialGroup(
    category: PartCategory,
    groupCode: String,
    mainModel: String,
    screenSize: String,
    compatibleModels: List<String>,
    releaseNote: String,
    onSuccess: (String) -> Unit,
  ) {
    val remote = officialCatalogRepository ?: run {
      _adminActionError.value = "Cloud catalog service unavailable."
      return
    }
    viewModelScope.launch {
      _adminActionLoading.value = true
      _adminActionError.value = null
      val result = remote.publishOfficialGroup(
        category = category,
        groupCode = groupCode,
        mainModel = mainModel,
        screenSize = screenSize,
        compatibleModels = compatibleModels,
        releaseNote = releaseNote,
      )
      _adminActionLoading.value = false
      result.fold(
        onSuccess = { docId -> onSuccess(docId) },
        onFailure = { error ->
          _adminActionError.value = error.localizedMessage ?: "Failed to publish official update."
        },
      )
    }
  }

  fun updateOfficialGroup(
    docId: String,
    category: PartCategory,
    groupCode: String,
    mainModel: String,
    screenSize: String,
    compatibleModels: List<String>,
    releaseNote: String,
    onSuccess: () -> Unit,
  ) {
    val remote = officialCatalogRepository ?: run {
      _adminActionError.value = "Cloud catalog service unavailable."
      return
    }
    viewModelScope.launch {
      _adminActionLoading.value = true
      _adminActionError.value = null
      val result = remote.updateOfficialGroup(
        docId = docId,
        category = category,
        groupCode = groupCode,
        mainModel = mainModel,
        screenSize = screenSize,
        compatibleModels = compatibleModels,
        releaseNote = releaseNote,
      )
      _adminActionLoading.value = false
      result.fold(
        onSuccess = { onSuccess() },
        onFailure = { error ->
          _adminActionError.value = error.localizedMessage ?: "Failed to update official group."
        },
      )
    }
  }

  fun deleteOfficialGroup(docId: String, onSuccess: () -> Unit = {}) {
    val remote = officialCatalogRepository ?: run {
      _adminActionError.value = "Cloud catalog service unavailable."
      return
    }
    viewModelScope.launch {
      _adminActionLoading.value = true
      _adminActionError.value = null
      val result = remote.deleteOfficialGroup(docId)
      _adminActionLoading.value = false
      result.fold(
        onSuccess = { onSuccess() },
        onFailure = { error ->
          _adminActionError.value = error.localizedMessage ?: "Failed to delete official group."
        },
      )
    }
  }

  fun saveProduct(
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
    onSaved: (Long) -> Unit,
  ) {
    viewModelScope.launch {
      val id = repository.saveProduct(
        productId = productId,
        category = category,
        productName = productName,
        mainModel = mainModel,
        brand = brand,
        screenSize = screenSize,
        stockQuantity = stockQuantity,
        minimumStock = minimumStock,
        boxNumber = boxNumber,
        selectedUniversalGroupCode = selectedUniversalGroupCode,
        compatibleModels = compatibleModels,
        notes = notes,
      )
      onSaved(id)
    }
  }
}

class ShopViewModelFactory(
  private val repository: InventoryRepository,
  private val officialCatalogRepository: OfficialCatalogRepository? = null,
  private val currentUserEmail: String? = null,
  private val initialIsAdmin: Boolean = false,
  private val enableCloudSync: Boolean = false,
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
      return ShopViewModel(
        repository = repository,
        officialCatalogRepository = officialCatalogRepository,
        currentUserEmail = currentUserEmail,
        initialIsAdmin = initialIsAdmin,
        enableCloudSync = enableCloudSync,
      ) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
