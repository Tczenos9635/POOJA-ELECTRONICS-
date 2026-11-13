package com.example.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.ai.AiChatViewModel
import com.example.ai.PartLensViewModel
import com.example.data.PartCategory
import com.example.ui.ShopViewModel
import com.example.ui.screens.AddEditProductScreen
import com.example.ui.screens.AddUniversalModelScreen
import com.example.ui.screens.AdminOfficialPanelScreen
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.CategoryCompatibilityScreen
import com.example.ui.screens.EditUniversalGroupScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyShopScreen
import com.example.ui.screens.PartLensScreen
import com.example.ui.screens.ProductDetailScreen
import kotlinx.coroutines.launch

object ShopRoutes {
  const val HOME = "home"
  const val MY_SHOP = "my_shop"
  const val ADD_UNIVERSAL = "add_universal"
  const val AI_CHAT = "ai_chat"
  const val PART_LENS = "part_lens"
  const val ADMIN_PANEL = "admin_panel"
  const val CATEGORY_COMPAT = "category_compat/{category}"
  const val PRODUCT_DETAIL = "product_detail/{productId}"
  const val ADD_PRODUCT = "add_product?category={category}&productId={productId}"
  const val EDIT_UNIVERSAL = "edit_universal/{groupId}"

  fun categoryCompat(category: PartCategory) = "category_compat/${category.name}"
  fun productDetail(productId: Long) = "product_detail/$productId"
  fun addProduct(category: PartCategory? = null, productId: Long? = null): String {
    val cat = category?.name ?: PartCategory.TEMPERED_GLASS.name
    val id = productId ?: -1L
    return "add_product?category=$cat&productId=$id"
  }
  fun editUniversal(groupId: Long) = "edit_universal/$groupId"
}

private data class BottomNavTab(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

@Composable
fun ShopNavHost(
  viewModel: ShopViewModel,
  aiChatViewModel: AiChatViewModel,
  partLensViewModel: PartLensViewModel,
  onSignOut: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  val tabs = remember {
    listOf(
      BottomNavTab(
        route = ShopRoutes.HOME,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "bottom_nav_home",
      ),
      BottomNavTab(
        route = ShopRoutes.MY_SHOP,
        labelRes = R.string.nav_my_shop,
        selectedIcon = Icons.Rounded.Storefront,
        unselectedIcon = Icons.Outlined.Storefront,
        testTag = "bottom_nav_my_shop",
      ),
      BottomNavTab(
        route = ShopRoutes.ADD_UNIVERSAL,
        labelRes = R.string.nav_add_universal,
        selectedIcon = Icons.Rounded.AddLink,
        unselectedIcon = Icons.Outlined.AddLink,
        testTag = "bottom_nav_add_universal",
      ),
    )
  }

  val showBottomBar = currentRoute in setOf(
    ShopRoutes.HOME,
    ShopRoutes.MY_SHOP,
    ShopRoutes.ADD_UNIVERSAL,
  )

  fun navigateToTab(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.statusBars,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    bottomBar = {
      if (showBottomBar) {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 4.dp,
          windowInsets = WindowInsets.navigationBars,
        ) {
          tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            val label = stringResource(tab.labelRes)
            NavigationBarItem(
              selected = selected,
              onClick = { navigateToTab(tab.route) },
              icon = {
                Icon(
                  imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = label,
                )
              },
              label = {
                Text(
                  text = label,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
              ),
              modifier = Modifier.testTag(tab.testTag),
            )
          }
        }
      }
    },
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = ShopRoutes.HOME,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      composable(ShopRoutes.HOME) {
        HomeScreen(
          viewModel = viewModel,
          onCategoryClick = { category ->
            navController.navigate(ShopRoutes.categoryCompat(category))
          },
          onProductClick = { productId ->
            navController.navigate(ShopRoutes.productDetail(productId))
          },
          onEditUniversalGroup = { groupId ->
            navController.navigate(ShopRoutes.editUniversal(groupId))
          },
          onNavigateToAddUniversal = {
            navigateToTab(ShopRoutes.ADD_UNIVERSAL)
          },
          onNavigateToAddProduct = {
            navController.navigate(ShopRoutes.addProduct())
          },
          onOpenAiChat = {
            navController.navigate(ShopRoutes.AI_CHAT)
          },
          onOpenPartLens = {
            navController.navigate(ShopRoutes.PART_LENS)
          },
          onOpenAdminPanel = {
            navController.navigate(ShopRoutes.ADMIN_PANEL)
          },
          onSignOut = onSignOut,
        )
      }

      composable(ShopRoutes.MY_SHOP) {
        MyShopScreen(
          viewModel = viewModel,
          onAddProduct = { category ->
            navController.navigate(ShopRoutes.addProduct(category = category))
          },
          onEditProduct = { productId ->
            navController.navigate(ShopRoutes.addProduct(productId = productId))
          },
          onProductClick = { productId ->
            navController.navigate(ShopRoutes.productDetail(productId))
          },
          onEditUniversalForProduct = { product ->
            if (product.isOfficialLinked) {
              if (viewModel.isAdmin.value) {
                navController.navigate(ShopRoutes.ADMIN_PANEL)
              } else {
                scope.launch { snackbarHostState.showSnackbar("Synced automatically via Official Updates") }
              }
            } else {
              val existingGroupId = product.universalGroupDbId
              if (existingGroupId != null) {
                navController.navigate(ShopRoutes.editUniversal(existingGroupId))
              } else {
                viewModel.ensureUniversalGroupForProduct(product.id) { createdId ->
                  navController.navigate(ShopRoutes.editUniversal(createdId))
                }
              }
            }
          },
        )
      }

      composable(ShopRoutes.ADD_UNIVERSAL) {
        AddUniversalModelScreen(
          viewModel = viewModel,
          onEditGroup = { groupId ->
            navController.navigate(ShopRoutes.editUniversal(groupId))
          },
          onSavedSuccess = { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
          },
          onOpenAdminPanel = {
            navController.navigate(ShopRoutes.ADMIN_PANEL)
          },
          onProductClick = { productId ->
            navController.navigate(ShopRoutes.productDetail(productId))
          },
        )
      }

      composable(ShopRoutes.ADMIN_PANEL) {
        AdminOfficialPanelScreen(
          viewModel = viewModel,
          onBack = { navController.popBackStack() },
          onShowMessage = { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
          },
        )
      }

      composable(ShopRoutes.AI_CHAT) {
        AiChatScreen(
          viewModel = aiChatViewModel,
          onBack = { navController.popBackStack() },
        )
      }

      composable(ShopRoutes.PART_LENS) {
        PartLensScreen(
          lensViewModel = partLensViewModel,
          onBack = { navController.popBackStack() },
          onSearchInHome = { query ->
            viewModel.updateHomeSearchQuery(query)
            navController.popBackStack(ShopRoutes.HOME, inclusive = false)
          },
          onProductClick = { productId ->
            navController.navigate(ShopRoutes.productDetail(productId))
          },
          onAskAiWithPrompt = {
            navController.navigate(ShopRoutes.AI_CHAT)
          },
        )
      }

      composable(
        route = ShopRoutes.CATEGORY_COMPAT,
        arguments = listOf(navArgument("category") { type = NavType.StringType }),
      ) { backStackEntry ->
        val categoryName = backStackEntry.arguments?.getString("category")
        val category = PartCategory.fromName(categoryName)
        CategoryCompatibilityScreen(
          category = category,
          viewModel = viewModel,
          onBack = { navController.popBackStack() },
          onProductClick = { productId ->
            navController.navigate(ShopRoutes.productDetail(productId))
          },
          onAddProductInCategory = {
            navController.navigate(ShopRoutes.addProduct(category = category))
          },
          onAddUniversalInCategory = {
            navigateToTab(ShopRoutes.ADD_UNIVERSAL)
          },
        )
      }

      composable(
        route = ShopRoutes.PRODUCT_DETAIL,
        arguments = listOf(navArgument("productId") { type = NavType.LongType }),
      ) { backStackEntry ->
        val productId = backStackEntry.arguments?.getLong("productId") ?: -1L
        ProductDetailScreen(
          productId = productId,
          viewModel = viewModel,
          onBack = { navController.popBackStack() },
          onEditProduct = { id ->
            navController.navigate(ShopRoutes.addProduct(productId = id))
          },
          onEditUniversal = { product ->
            if (product.isOfficialLinked) {
              if (viewModel.isAdmin.value) {
                navController.navigate(ShopRoutes.ADMIN_PANEL)
              } else {
                scope.launch { snackbarHostState.showSnackbar("Synced automatically via Official Updates") }
              }
            } else {
              val groupId = product.universalGroupDbId
              if (groupId != null) {
                navController.navigate(ShopRoutes.editUniversal(groupId))
              } else {
                viewModel.ensureUniversalGroupForProduct(product.id) { createdId ->
                  navController.navigate(ShopRoutes.editUniversal(createdId))
                }
              }
            }
          },
        )
      }

      composable(
        route = ShopRoutes.ADD_PRODUCT,
        arguments = listOf(
          navArgument("category") {
            type = NavType.StringType
            defaultValue = PartCategory.TEMPERED_GLASS.name
          },
          navArgument("productId") {
            type = NavType.LongType
            defaultValue = -1L
          },
        ),
      ) { backStackEntry ->
        val categoryArg = backStackEntry.arguments?.getString("category")
        val productIdArg = backStackEntry.arguments?.getLong("productId") ?: -1L
        AddEditProductScreen(
          productId = if (productIdArg > 0L) productIdArg else null,
          initialCategory = PartCategory.fromName(categoryArg),
          viewModel = viewModel,
          onBack = { navController.popBackStack() },
          onSaved = {
            scope.launch { snackbarHostState.showSnackbar("Product saved") }
            navController.popBackStack()
          },
        )
      }

      composable(
        route = ShopRoutes.EDIT_UNIVERSAL,
        arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
      ) { backStackEntry ->
        val groupId = backStackEntry.arguments?.getLong("groupId") ?: -1L
        EditUniversalGroupScreen(
          groupId = groupId,
          viewModel = viewModel,
          onBack = { navController.popBackStack() },
          onSaved = {
            scope.launch { snackbarHostState.showSnackbar("Universal model updated") }
            navController.popBackStack()
          },
          onDeleted = {
            scope.launch { snackbarHostState.showSnackbar("Universal group deleted") }
            navController.popBackStack()
          },
        )
      }
    }
  }
}
