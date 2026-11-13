package com.example.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class FirestoreEmulatorTestBase {

  protected lateinit var firestore: FirebaseFirestore
  protected lateinit var auth: FirebaseAuth
  protected lateinit var databaseId: String

  @Before
  open fun setUpFirebase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    databaseId = try {
      context.getString(R.string.firestore_database_id)
    } catch (_: Exception) {
      FALLBACK_DATABASE_ID
    }

    val app = if (FirebaseApp.getApps(context).isEmpty()) {
      val newApp = FirebaseApp.initializeApp(
        context,
        FirebaseOptions.Builder()
          .setApplicationId("com.aistudio.poojaelectronics.mndkpx")
          .setProjectId(PROJECT_ID)
          .setApiKey(FAKE_API_KEY)
          .build(),
      )
      val newFirestore = FirebaseFirestore.getInstance(newApp, databaseId)
      newFirestore.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
      newFirestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
        .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
        .build()

      val newAuth = FirebaseAuth.getInstance(newApp)
      newAuth.useEmulator(EMULATOR_HOST, AUTH_PORT)
      newApp
    } else {
      FirebaseApp.getInstance()
    }

    firestore = FirebaseFirestore.getInstance(app, databaseId)
    auth = FirebaseAuth.getInstance(app)
  }

  @After
  open fun tearDownFirebase() {
    auth.signOut()
  }

  protected suspend fun signInTestUser(
    email: String,
    emailVerified: Boolean = true,
  ): String = withContext(Dispatchers.IO) {
    withTimeout(AUTH_TIMEOUT_MS) {
      val result = try {
        auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
      } catch (_: FirebaseAuthInvalidUserException) {
        auth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
      } catch (_: Exception) {
        // Retry create if user didn't exist under generic emulator exception
        try {
          auth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
        } catch (_: Exception) {
          auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
        }
      }
      val user = checkNotNull(result.user) { "User auth failed for $email" }
      updateEmulatorEmailVerified(user.uid, emailVerified)
      auth.signOut()
      val refreshedResult = auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
      val refreshedUser = checkNotNull(refreshedResult.user) { "Refreshed auth failed for $email" }
      refreshedUser.getIdToken(true).await()
      refreshedUser.uid
    }
  }

  private fun updateEmulatorEmailVerified(uid: String, verified: Boolean) {
    val endpoint = URL(
      "http://$EMULATOR_HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/projects/$PROJECT_ID/accounts:update",
    )
    val conn = (endpoint.openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      doOutput = true
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Authorization", "Bearer owner")
      connectTimeout = 3000
      readTimeout = 3000
    }
    try {
      val body = JSONObject().apply {
        put("localId", uid)
        put("emailVerified", verified)
      }.toString()
      conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
      val code = conn.responseCode
      check(code in 200..299) { "Auth emulator accounts:update returned HTTP $code" }
    } finally {
      conn.disconnect()
    }
  }

  protected fun Throwable.unwrapFirestoreException(): FirebaseFirestoreException? {
    var current: Throwable? = this
    while (current != null) {
      if (current is FirebaseFirestoreException) return current
      current = current.cause
    }
    return null
  }

  protected suspend inline fun <reified T : Throwable> assertThrowsSuspend(
    crossinline block: suspend () -> Unit,
  ): T {
    try {
      block()
    } catch (e: Throwable) {
      if (e is T) return e
      throw AssertionError("Expected ${T::class.java.simpleName}, but threw ${e::class.java.name}: ${e.message}", e)
    }
    throw AssertionError("Expected ${T::class.java.simpleName}, but block succeeded without throwing")
  }

  private companion object {
    const val EMULATOR_HOST = "127.0.0.1"
    const val FIRESTORE_PORT = 8085
    const val AUTH_PORT = 9099
    const val PROJECT_ID = "demo-no-project"
    const val FAKE_API_KEY = "fake-api-key-for-emulator"
    const val DEFAULT_PASSWORD = "password123"
    const val AUTH_TIMEOUT_MS = 8000L
    const val FALLBACK_DATABASE_ID = "ai-studio-android-poojaele-4b00f867-de53-471d-a55f-c2cb7524ef41"
  }
}
