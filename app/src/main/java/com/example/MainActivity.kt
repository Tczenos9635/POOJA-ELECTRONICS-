package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.AiChatViewModel
import com.example.ai.AiChatViewModelFactory
import com.example.ai.PartLensViewModel
import com.example.ai.PartLensViewModelFactory
import com.example.ai.configureAppCheck
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.AppDatabase
import com.example.data.InventoryRepository
import com.example.data.remote.OfficialCatalogRepository
import com.example.data.remote.ShopCloudSyncRepository
import com.example.ui.ShopViewModel
import com.example.ui.ShopViewModelFactory
import com.example.ui.navigation.ShopNavHost
import com.example.ui.theme.PoojaElectronicsTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
    configureAppCheck(this, intent)

    enableEdgeToEdge()

    setContent {
      PoojaElectronicsTheme {
        val auth = remember { Firebase.auth }
        val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
        val user = currentUser

        if (user == null) {
          AuthScreen(onAuthSuccess = {})
        } else {
          val context = LocalContext.current
          val scope = rememberCoroutineScope()
          val credentialManager = remember { CredentialManager.create(context) }

          key(user.uid) {
            val repositoryBundle = remember(user.uid) {
              val appContext = context.applicationContext
              val databaseId = appContext.getString(R.string.firestore_database_id)
              val firestore = FirebaseFirestore.getInstance(databaseId)
              val officialRepo = OfficialCatalogRepository(db = firestore, auth = auth)
              val cloudSyncRepo = ShopCloudSyncRepository(db = firestore, auth = auth)
              val roomDb = AppDatabase.getInstance(appContext)
              val inventoryRepo = InventoryRepository(roomDb, officialRepo, cloudSyncRepo)
              Triple(inventoryRepo, officialRepo, officialRepo.isCurrentUserAdmin())
            }

            val (inventoryRepo, officialRepo, initialIsAdmin) = repositoryBundle
            val shopViewModel: ShopViewModel = viewModel(
              key = "shop_${user.uid}",
              factory = ShopViewModelFactory(
                repository = inventoryRepo,
                officialCatalogRepository = officialRepo,
                currentUserEmail = user.email,
                initialIsAdmin = initialIsAdmin,
                enableCloudSync = true,
              ),
            )
            val aiChatViewModel: AiChatViewModel = viewModel(
              key = "ai_${user.uid}",
              factory = AiChatViewModelFactory(inventoryRepo),
            )
            val partLensViewModel: PartLensViewModel = viewModel(
              key = "lens_${user.uid}",
              factory = PartLensViewModelFactory(inventoryRepo),
            )

            ShopNavHost(
              viewModel = shopViewModel,
              aiChatViewModel = aiChatViewModel,
              partLensViewModel = partLensViewModel,
              onSignOut = {
                signOutUser(
                  credentialManager = credentialManager,
                  scope = scope,
                )
              },
            )
          }
        }
      }
    }
  }
}
