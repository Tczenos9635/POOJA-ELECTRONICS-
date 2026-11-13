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

class OfficialCatalogRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun verifiedAdmin_canPublishUpdateObserveAndDeleteOfficialGroup() = runBlocking {
    signInTestUser(ADMIN_EMAIL, emailVerified = true)
    val repo = OfficialCatalogRepository(firestore, auth)
    val uniqueCode = "TG-${UUID.randomUUID().toString().take(6).uppercase()}"

    val publishResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.publishOfficialGroup(
        category = PartCategory.TEMPERED_GLASS,
        groupCode = uniqueCode,
        mainModel = "Oppo F31",
        screenSize = "6.57\"",
        compatibleModels = listOf("Vivo V30", "Realme X"),
        releaseNote = "Initial official batch",
      )
    }
    assertTrue("Admin publish failed: ${publishResult.exceptionOrNull()}", publishResult.isSuccess)
    val docId = publishResult.getOrThrow()

    val observedAfterCreate = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeOfficialGroups().first { list -> list.any { it.id == docId } }
    }
    val createdDoc = observedAfterCreate.first { it.id == docId }
    assertEquals(uniqueCode, createdDoc.groupCode)
    assertEquals("Oppo", createdDoc.brand)
    assertEquals(listOf("Vivo V30", "Realme X"), createdDoc.compatibleModels)

    val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateOfficialGroup(
        docId = docId,
        category = PartCategory.TEMPERED_GLASS,
        groupCode = uniqueCode,
        mainModel = "Oppo F31",
        screenSize = "6.57\"",
        compatibleModels = listOf("Vivo V30", "Realme X", "Samsung A25"),
        releaseNote = "Added Samsung A25",
      )
    }
    assertTrue("Admin update failed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)

    val observedAfterUpdate = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeOfficialGroups().first { list ->
        list.any { it.id == docId && it.compatibleModels.contains("Samsung A25") }
      }
    }
    assertTrue(observedAfterUpdate.any { it.id == docId && it.releaseNote == "Added Samsung A25" })

    val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.deleteOfficialGroup(docId)
    }
    assertTrue("Admin delete failed: ${deleteResult.exceptionOrNull()}", deleteResult.isSuccess)
  }

  @Test
  fun regularUser_canReadOfficialGroupsInRealtime_butWriteIsRejectedWithPermissionDenied() = runBlocking {
    signInTestUser(ADMIN_EMAIL, emailVerified = true)
    val adminRepo = OfficialCatalogRepository(firestore, auth)
    val uniqueCode = "BC-${UUID.randomUUID().toString().take(6).uppercase()}"
    val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
      adminRepo.publishOfficialGroup(
        category = PartCategory.BACK_COVER,
        groupCode = uniqueCode,
        mainModel = "Vivo V30",
        screenSize = "",
        compatibleModels = listOf("Oppo Reno 12"),
        releaseNote = "Back cover batch",
      ).getOrThrow()
    }

    try {
      signInTestUser(REGULAR_USER_EMAIL, emailVerified = true)
      val userRepo = OfficialCatalogRepository(firestore, auth)

      val groups = withTimeout(FLOW_TIMEOUT_MS) {
        userRepo.observeOfficialGroups().first { list -> list.any { it.id == docId } }
      }
      assertTrue(groups.any { it.id == docId && it.groupCode == uniqueCode })

      val createAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
        userRepo.publishOfficialGroup(
          category = PartCategory.BATTERY,
          groupCode = "BT-999",
          mainModel = "Unauthorized Model",
          screenSize = "",
          compatibleModels = emptyList(),
          releaseNote = "Should fail",
        )
      }
      assertTrue(createAttempt.isFailure)
      val createErr = createAttempt.exceptionOrNull()?.unwrapFirestoreException()
      assertNotNull(createErr)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, createErr!!.code)

      val updateAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
        userRepo.updateOfficialGroup(
          docId = docId,
          category = PartCategory.BACK_COVER,
          groupCode = uniqueCode,
          mainModel = "Tampered Model",
          screenSize = "",
          compatibleModels = emptyList(),
          releaseNote = "Should fail",
        )
      }
      assertTrue(updateAttempt.isFailure)
      assertEquals(
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        updateAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code,
      )

      val deleteAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
        userRepo.deleteOfficialGroup(docId)
      }
      assertTrue(deleteAttempt.isFailure)
      assertEquals(
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        deleteAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code,
      )
    } finally {
      signInTestUser(ADMIN_EMAIL, emailVerified = true)
      withTimeout(DEFAULT_TIMEOUT_MS) {
        adminRepo.deleteOfficialGroup(docId)
      }
    }
  }

  @Test
  fun unverifiedAdminEmail_failsPublishWithPermissionDenied() = runBlocking {
    signInTestUser(ADMIN_EMAIL, emailVerified = false)
    val spoofRepo = OfficialCatalogRepository(firestore, auth)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      spoofRepo.publishOfficialGroup(
        category = PartCategory.CC_BOARD,
        groupCode = "CC-001",
        mainModel = "Spoof Oppo",
        screenSize = "",
        compatibleModels = emptyList(),
        releaseNote = "Unverified spoof",
      )
    }
    assertTrue(result.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      result.exceptionOrNull()?.unwrapFirestoreException()?.code,
    )
  }

  @Test
  fun unauthenticatedUser_observeOfficialGroups_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = OfficialCatalogRepository(firestore, auth)

    val exception = assertThrowsSuspend<Exception> {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeOfficialGroups().first()
      }
    }
    val firestoreException = checkNotNull(exception.unwrapFirestoreException()) {
      "Expected nested FirebaseFirestoreException, got $exception"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  private companion object {
    const val ADMIN_EMAIL = "sbstudio17@gmail.com"
    const val REGULAR_USER_EMAIL = "shopuser@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
