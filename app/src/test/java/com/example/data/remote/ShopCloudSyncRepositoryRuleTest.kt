package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.PartCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopCloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun owner_canCreateObserveUpdateAndDelete_productsAndUniversalModels() = runBlocking {
    val uid = signInTestUser(SHOP_OWNER_EMAIL, emailVerified = true)
    val repo = ShopCloudSyncRepository(firestore, auth)
    val code = "TG-${UUID.randomUUID().toString().take(6).uppercase()}"

    val modelCreateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.upsertUniversalModel(
        docId = null,
        category = PartCategory.TEMPERED_GLASS,
        groupCode = code,
        mainModel = "Oppo F31",
        screenSize = "6.57\"",
        compatibleModels = listOf("Vivo V30", "Realme X"),
        releaseNote = "Shop sync model",
        isPublished = false,
      )
    }
    assertTrue("Model create failed: ${modelCreateResult.exceptionOrNull()}", modelCreateResult.isSuccess)
    val modelId = modelCreateResult.getOrThrow()

    val prodCreateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.upsertProduct(
        docId = null,
        category = PartCategory.TEMPERED_GLASS,
        productName = "Oppo F31 Tempered Glass",
        mainModel = "Oppo F31",
        brand = "Oppo",
        screenSize = "6.57\"",
        stockQuantity = 12,
        minimumStock = 3,
        boxNumber = "B-009",
        universalGroupId = code,
        compatibleModels = listOf("Vivo V30", "Realme X"),
        notes = "Matte finish",
        isPublished = false,
      )
    }
    assertTrue("Product create failed: ${prodCreateResult.exceptionOrNull()}", prodCreateResult.isSuccess)
    val productId = prodCreateResult.getOrThrow()

    val observedModels = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMyUniversalModels().first { list -> list.any { it.id == modelId } }
    }
    val syncedModel = observedModels.first { it.id == modelId }
    assertEquals(uid, syncedModel.userId)
    assertEquals(code, syncedModel.groupCode)
    assertEquals(listOf("Vivo V30", "Realme X"), syncedModel.compatibleModels)

    val observedProducts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMyProducts().first { list -> list.any { it.id == productId } }
    }
    val syncedProduct = observedProducts.first { it.id == productId }
    assertEquals(12L, syncedProduct.stockQuantity)
    assertEquals("B-009", syncedProduct.boxNumber)

    val stockUpdateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateProductStock(productId, 20)
    }
    assertTrue("Stock update failed: ${stockUpdateResult.exceptionOrNull()}", stockUpdateResult.isSuccess)

    val updatedProducts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMyProducts().first { list -> list.any { it.id == productId && it.stockQuantity == 20L } }
    }
    assertEquals(20L, updatedProducts.first { it.id == productId }.stockQuantity)

    val modelUpdateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.upsertUniversalModel(
        docId = modelId,
        category = PartCategory.TEMPERED_GLASS,
        groupCode = code,
        mainModel = "Oppo F31",
        screenSize = "6.57\"",
        compatibleModels = listOf("Vivo V30", "Realme X", "Samsung A25"),
        releaseNote = "Added Samsung A25",
        isPublished = false,
      )
    }
    assertTrue("Model update failed: ${modelUpdateResult.exceptionOrNull()}", modelUpdateResult.isSuccess)

    val updatedModels = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMyUniversalModels().first { list ->
        list.any { it.id == modelId && it.compatibleModels.contains("Samsung A25") }
      }
    }
    assertEquals("Added Samsung A25", updatedModels.first { it.id == modelId }.releaseNote)

    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteProduct(productId) }.isSuccess)
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteUniversalModel(modelId) }.isSuccess)
  }

  @Test
  fun crossUserIsolation_andGlobalPublishRestrictions_enforcedOnProductsAndUniversalModels() = runBlocking {
    signInTestUser(SHOP_OWNER_EMAIL, emailVerified = true)
    val ownerRepo = ShopCloudSyncRepository(firestore, auth)
    val code = "BC-${UUID.randomUUID().toString().take(6).uppercase()}"

    val modelId = withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo.upsertUniversalModel(
        docId = null,
        category = PartCategory.BACK_COVER,
        groupCode = code,
        mainModel = "Vivo V30",
        screenSize = "",
        compatibleModels = listOf("Vivo V30 Pro"),
        releaseNote = "Owner private group",
        isPublished = false,
      ).getOrThrow()
    }
    val productId = withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo.upsertProduct(
        docId = null,
        category = PartCategory.BACK_COVER,
        productName = "Vivo V30 Silicon Cover",
        mainModel = "Vivo V30",
        brand = "Vivo",
        screenSize = "",
        stockQuantity = 8,
        minimumStock = 2,
        boxNumber = "C-14",
        universalGroupId = code,
        compatibleModels = listOf("Vivo V30 Pro"),
        notes = "Black",
        isPublished = false,
      ).getOrThrow()
    }

    try {
      signInTestUser(OTHER_USER_EMAIL, emailVerified = true)
      val otherRepo = ShopCloudSyncRepository(firestore, auth)

      // Other user cannot see owner's private documents in their own real-time stream
      val otherProducts = withTimeout(FLOW_TIMEOUT_MS) { otherRepo.observeMyProducts().first() }
      assertTrue(otherProducts.none { it.id == productId })

      val otherModels = withTimeout(FLOW_TIMEOUT_MS) { otherRepo.observeMyUniversalModels().first() }
      assertTrue(otherModels.none { it.id == modelId })

      // Other user cannot modify or delete owner's product/model
      val tamperStock = withTimeout(DEFAULT_TIMEOUT_MS) { otherRepo.updateProductStock(productId, 999) }
      assertTrue(tamperStock.isFailure)
      assertEquals(
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        tamperStock.exceptionOrNull()?.unwrapFirestoreException()?.code,
      )

      val tamperModelDelete = withTimeout(DEFAULT_TIMEOUT_MS) { otherRepo.deleteUniversalModel(modelId) }
      assertTrue(tamperModelDelete.isFailure)
      assertEquals(
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        tamperModelDelete.exceptionOrNull()?.unwrapFirestoreException()?.code,
      )

      // Non-admin user cannot publish globally (isPublished = true)
      val unauthorizedPublish = withTimeout(DEFAULT_TIMEOUT_MS) {
        otherRepo.upsertUniversalModel(
          docId = null,
          category = PartCategory.BATTERY,
          groupCode = "BT-777",
          mainModel = "Fake Global Model",
          screenSize = "",
          compatibleModels = emptyList(),
          releaseNote = "Unauthorized",
          isPublished = true,
        )
      }
      assertTrue(unauthorizedPublish.isFailure)
      assertEquals(
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        unauthorizedPublish.exceptionOrNull()?.unwrapFirestoreException()?.code,
      )
    } finally {
      signInTestUser(SHOP_OWNER_EMAIL, emailVerified = true)
      withTimeout(DEFAULT_TIMEOUT_MS) { ownerRepo.deleteProduct(productId) }
      withTimeout(DEFAULT_TIMEOUT_MS) { ownerRepo.deleteUniversalModel(modelId) }
    }
  }

  @Test
  fun verifiedAdmin_canPublishGlobalUniversalModelsAndProducts_syncedToAllInstallations() = runBlocking {
    signInTestUser(ADMIN_EMAIL, emailVerified = true)
    val adminRepo = ShopCloudSyncRepository(firestore, auth)
    val code = "CD-${UUID.randomUUID().toString().take(6).uppercase()}"

    val globalModelId = withTimeout(DEFAULT_TIMEOUT_MS) {
      adminRepo.upsertUniversalModel(
        docId = null,
        category = PartCategory.COMBO_DISPLAY,
        groupCode = code,
        mainModel = "Samsung A15",
        screenSize = "6.5\"",
        compatibleModels = listOf("Samsung A25", "Samsung M34"),
        releaseNote = "Official combo sync",
        isPublished = true,
      ).getOrThrow()
    }
    val globalProductId = withTimeout(DEFAULT_TIMEOUT_MS) {
      adminRepo.upsertProduct(
        docId = null,
        category = PartCategory.COMBO_DISPLAY,
        productName = "Samsung A15 Combo Display",
        mainModel = "Samsung A15",
        brand = "Samsung",
        screenSize = "6.5\"",
        stockQuantity = 5,
        minimumStock = 2,
        boxNumber = "D-001",
        universalGroupId = code,
        compatibleModels = listOf("Samsung A25", "Samsung M34"),
        notes = "OLED original",
        isPublished = true,
      ).getOrThrow()
    }

    try {
      signInTestUser(OTHER_USER_EMAIL, emailVerified = true)
      val userRepo = ShopCloudSyncRepository(firestore, auth)

      val publishedModels = withTimeout(FLOW_TIMEOUT_MS) {
        userRepo.observePublishedUniversalModels().first { list -> list.any { it.id == globalModelId } }
      }
      assertTrue(publishedModels.any { it.id == globalModelId && it.groupCode == code })

      val publishedProducts = withTimeout(FLOW_TIMEOUT_MS) {
        userRepo.observePublishedProducts().first { list -> list.any { it.id == globalProductId } }
      }
      assertTrue(publishedProducts.any { it.id == globalProductId && it.mainModel == "Samsung A15" })

      // Regular user cannot delete admin's global model
      val deleteAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
        userRepo.deleteUniversalModel(globalModelId)
      }
      assertTrue(deleteAttempt.isFailure)
      assertEquals(
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        deleteAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code,
      )
    } finally {
      signInTestUser(ADMIN_EMAIL, emailVerified = true)
      withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.deleteProduct(globalProductId) }
      withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.deleteUniversalModel(globalModelId) }
    }
  }

  @Test
  fun unauthenticatedUser_observePublishedStreams_failWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = ShopCloudSyncRepository(firestore, auth)

    val modelEx = assertThrowsSuspend<Exception> {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observePublishedUniversalModels().first() }
    }
    val firestoreModelEx = modelEx.unwrapFirestoreException()
    assertNotNull(firestoreModelEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreModelEx!!.code)

    val prodEx = assertThrowsSuspend<Exception> {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observePublishedProducts().first() }
    }
    val firestoreProdEx = prodEx.unwrapFirestoreException()
    assertNotNull(firestoreProdEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreProdEx!!.code)
  }

  @Test
  fun typedProductAndUniversalModelDataClasses_roundTripWithFirestore() = runBlocking {
    val uid = signInTestUser(SHOP_OWNER_EMAIL, emailVerified = true)
    val repo = ShopCloudSyncRepository(firestore, auth)
    val code = "BC-${UUID.randomUUID().toString().take(6).uppercase()}"

    val modelInput = UniversalModel(
      userId = uid,
      updatedByUid = uid,
      groupCode = code,
      category = PartCategory.BACK_COVER.name,
      mainModel = "Oppo F31",
      brand = "Oppo",
      compatibleModels = listOf("Vivo V30", "Realme 12"),
      releaseNote = "Typed model test",
    )
    assertTrue("id" !in modelInput.toCreatePayload(uid))
    val modelId = withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveUniversalModel(modelInput).getOrThrow() }

    val productInput = Product(
      userId = uid,
      productName = "Oppo F31 Back Cover",
      category = PartCategory.BACK_COVER.name,
      mainModel = "Oppo F31",
      brand = "Oppo",
      stockQuantity = 14L,
      minimumStock = 3L,
      boxNumber = "B-045",
      universalGroupId = code,
      compatibleModels = listOf("Vivo V30", "Realme 12"),
    )
    assertTrue("id" !in productInput.toCreatePayload(uid))
    val productId = withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveProduct(productInput).getOrThrow() }

    try {
      val observedModel = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeMyUniversalModels().first { list -> list.any { it.id == modelId } }
      }.first { it.id == modelId }
      assertEquals(modelId, observedModel.id)
      assertEquals(PartCategory.BACK_COVER.name, observedModel.category)
      assertEquals("Oppo F31", observedModel.mainModel)
      assertEquals(listOf("Vivo V30", "Realme 12"), observedModel.compatibleModels)

      val observedProduct = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeMyProducts().first { list -> list.any { it.id == productId } }
      }.first { it.id == productId }
      assertEquals(productId, observedProduct.id)
      assertEquals(14, observedProduct.resolvedStockQuantity())
      assertEquals("B-045", observedProduct.boxNumber)
      assertEquals(listOf("Vivo V30", "Realme 12"), observedProduct.compatibleModels)
    } finally {
      withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteProduct(productId) }
      withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteUniversalModel(modelId) }
    }
  }

  private companion object {
    const val ADMIN_EMAIL = "sbstudio17@gmail.com"
    const val SHOP_OWNER_EMAIL = "owner_sync@test.com"
    const val OTHER_USER_EMAIL = "other_sync@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
