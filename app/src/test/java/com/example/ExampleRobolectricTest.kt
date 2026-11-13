package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.InventoryRepository
import com.example.data.PartCategory
import com.example.data.SearchEngine
import com.example.data.StockStatus
import com.example.data.calculateStockStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: InventoryRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = InventoryRepository(db)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `app name matches Pooja Electronics`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Pooja Electronics", appName)
  }

  @Test
  fun `universal group links single stock across compatible models and updates cleanly`() = runTest {
    val groupId = repository.saveNewUniversalGroup(
      category = PartCategory.TEMPERED_GLASS,
      mainModel = "Oppo F31",
      screenSize = "6.57\"",
      compatibleModels = listOf("Vivo V30", "Realme X", "Samsung A25"),
      customGroupCode = "TG-001",
      initialStock = 5,
      minimumStock = 3,
      boxNumber = "",
    )

    val products = repository.shopProducts.first()
    assertEquals(1, products.size)
    val tgProduct = products.first()
    assertEquals("TG-001", tgProduct.universalGroupId)
    assertEquals(5, tgProduct.stockQuantity)

    // Searching any compatible model returns the same TG-001 part
    assertEquals(1, SearchEngine.searchProducts(products, "Oppo F31").size)
    assertEquals(1, SearchEngine.searchProducts(products, "Vivo V30").size)
    assertEquals(1, SearchEngine.searchProducts(products, "Realme X").size)
    assertEquals(1, SearchEngine.searchProducts(products, "TG-001").size)

    // Edit universal group to add Oppo A79 without duplicating stock
    repository.updateUniversalGroup(
      groupId = groupId,
      category = PartCategory.TEMPERED_GLASS,
      groupCode = "TG-001",
      mainModel = "Oppo F31",
      screenSize = "6.57\"",
      compatibleModels = listOf("Vivo V30", "Realme X", "Samsung A25", "Oppo A79"),
    )

    val updatedProducts = repository.shopProducts.first()
    assertEquals(1, updatedProducts.size)
    assertEquals(1, SearchEngine.searchProducts(updatedProducts, "Oppo A79").size)

    // Brand-wise grouping organizes OPPO, VIVO, REALME, SAMSUNG
    val brandSections = SearchEngine.groupCategoryModelsByBrand(
      category = PartCategory.TEMPERED_GLASS,
      products = updatedProducts,
      query = "",
    )
    assertEquals(listOf("OPPO", "REALME", "SAMSUNG", "VIVO"), brandSections.map { it.brand })
  }

  @Test
  fun `back cover box number search and stock clamping work correctly`() = runTest {
    val id = repository.saveProduct(
      productId = null,
      category = PartCategory.BACK_COVER,
      productName = "",
      mainModel = "Oppo F31",
      brand = "Oppo",
      screenSize = "",
      stockQuantity = 3,
      minimumStock = 3,
      boxNumber = "B-023",
      selectedUniversalGroupCode = null,
      compatibleModels = emptyList(),
      notes = "",
    )

    val products = repository.shopProducts.first()
    val boxMatches = SearchEngine.searchProducts(products, "B-023")
    assertEquals(1, boxMatches.size)
    assertEquals(StockStatus.LOW_STOCK, boxMatches.first().product.stockStatus)

    // Decreasing stock below zero clamps at 0 -> OUT_OF_STOCK
    repository.adjustProductStock(id, -10)
    val afterDecrease = repository.shopProducts.first().first()
    assertEquals(0, afterDecrease.stockQuantity)
    assertEquals(StockStatus.OUT_OF_STOCK, afterDecrease.stockStatus)
    assertTrue(calculateStockStatus(10, 3) == StockStatus.NORMAL)
  }

  @Test
  fun `MainActivity launches without crashing`() {
    val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
    assertTrue(!activity.isFinishing)
  }
}
