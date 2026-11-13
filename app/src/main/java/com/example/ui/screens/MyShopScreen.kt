package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.PartCategory
import com.example.data.ShopProduct
import com.example.data.StockFilter
import com.example.data.StockStatus
import com.example.ui.ShopViewModel
import com.example.ui.components.BoxNumberBadge
import com.example.ui.components.CategoryCardChip
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OfficialBadge
import com.example.ui.components.ScreenSizeBadge
import com.example.ui.components.ShopBrandHeader
import com.example.ui.components.ShopSearchBar
import com.example.ui.components.StockStatusBadge
import com.example.ui.components.StockStepper
import com.example.ui.components.UniversalGroupBadge
import com.example.ui.theme.StockLowBg
import com.example.ui.theme.StockLowBorder
import com.example.ui.theme.StockLowText
import com.example.ui.theme.StockOutBg
import com.example.ui.theme.StockOutBorder
import com.example.ui.theme.StockOutText

@Composable
fun MyShopScreen(
  viewModel: ShopViewModel,
  onAddProduct: (PartCategory?) -> Unit,
  onEditProduct: (Long) -> Unit,
  onProductClick: (Long) -> Unit,
  onEditUniversalForProduct: (ShopProduct) -> Unit,
  modifier: Modifier = Modifier,
) {
  val summary by viewModel.inventorySummary.collectAsStateWithLifecycle()
  val searchQuery by viewModel.shopSearchQuery.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.shopSelectedCategory.collectAsStateWithLifecycle()
  val stockFilter by viewModel.shopStockFilter.collectAsStateWithLifecycle()
  val filteredResults by viewModel.filteredShopProducts.collectAsStateWithLifecycle()
  val categoryCounts by viewModel.categoryCounts.collectAsStateWithLifecycle()
  val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      ShopBrandHeader(
        title = stringResource(R.string.title_my_inventory),
        trailingContent = {
          Button(
            onClick = { onAddProduct(selectedCategory) },
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("shop_add_product_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Add,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Add")
          }
        },
      )
    }

    // 4 Summary Cards in clean 2x2 grid
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          SummaryMetricCard(
            label = stringResource(R.string.label_total_products),
            value = summary.totalProducts.toString(),
            caption = "${summary.totalPieces} pcs total",
            icon = Icons.Rounded.Inventory2,
            selected = stockFilter == StockFilter.ALL,
            accentColor = MaterialTheme.colorScheme.primary,
            bgColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outline,
            onClick = {
              if (stockFilter != StockFilter.ALL) {
                viewModel.toggleShopStockFilter(stockFilter)
              }
            },
            testTag = "summary_total_products",
            modifier = Modifier.weight(1f),
          )
          SummaryMetricCard(
            label = stringResource(R.string.label_categories),
            value = summary.activeCategories.toString(),
            caption = "Part types",
            icon = Icons.Rounded.Category,
            selected = false,
            accentColor = MaterialTheme.colorScheme.primary,
            bgColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outline,
            onClick = { viewModel.setShopSelectedCategory(null) },
            testTag = "summary_categories",
            modifier = Modifier.weight(1f),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          SummaryMetricCard(
            label = stringResource(R.string.label_low_stock),
            value = summary.lowStockCount.toString(),
            caption = if (stockFilter == StockFilter.LOW_STOCK) "Filter active" else "Tap to filter",
            icon = Icons.Rounded.WarningAmber,
            selected = stockFilter == StockFilter.LOW_STOCK,
            accentColor = if (summary.lowStockCount > 0) StockLowText else MaterialTheme.colorScheme.onSurfaceVariant,
            bgColor = if (stockFilter == StockFilter.LOW_STOCK || summary.lowStockCount > 0) StockLowBg else MaterialTheme.colorScheme.surface,
            borderColor = if (stockFilter == StockFilter.LOW_STOCK) StockLowText else if (summary.lowStockCount > 0) StockLowBorder else MaterialTheme.colorScheme.outline,
            onClick = { viewModel.toggleShopStockFilter(StockFilter.LOW_STOCK) },
            testTag = "summary_low_stock",
            modifier = Modifier.weight(1f),
          )
          SummaryMetricCard(
            label = stringResource(R.string.label_out_of_stock),
            value = summary.outOfStockCount.toString(),
            caption = if (stockFilter == StockFilter.OUT_OF_STOCK) "Filter active" else "Tap to filter",
            icon = Icons.Rounded.ErrorOutline,
            selected = stockFilter == StockFilter.OUT_OF_STOCK,
            accentColor = if (summary.outOfStockCount > 0) StockOutText else MaterialTheme.colorScheme.onSurfaceVariant,
            bgColor = if (stockFilter == StockFilter.OUT_OF_STOCK || summary.outOfStockCount > 0) StockOutBg else MaterialTheme.colorScheme.surface,
            borderColor = if (stockFilter == StockFilter.OUT_OF_STOCK) StockOutText else if (summary.outOfStockCount > 0) StockOutBorder else MaterialTheme.colorScheme.outline,
            onClick = { viewModel.toggleShopStockFilter(StockFilter.OUT_OF_STOCK) },
            testTag = "summary_out_of_stock",
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    item {
      ShopSearchBar(
        query = searchQuery,
        onQueryChange = viewModel::updateShopSearchQuery,
        placeholder = stringResource(R.string.hint_shop_search),
        testTag = "shop_search_bar",
      )
    }

    // Horizontal Category Cards
    item {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        item {
          CategoryCardChip(
            label = "All",
            icon = Icons.Rounded.Apps,
            selected = selectedCategory == null,
            count = allProducts.size,
            onClick = { viewModel.setShopSelectedCategory(null) },
            testTag = "shop_category_all",
          )
        }
        items(PartCategory.entries) { category ->
          CategoryCardChip(
            label = category.displayName,
            icon = category.icon(),
            selected = selectedCategory == category,
            count = categoryCounts[category] ?: 0,
            onClick = {
              viewModel.setShopSelectedCategory(if (selectedCategory == category) null else category)
            },
            testTag = "shop_category_${category.name.lowercase()}",
          )
        }
      }
    }

    if (allProducts.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Rounded.Inventory2,
          title = stringResource(R.string.empty_products_title),
          subtitle = stringResource(R.string.empty_products_subtitle),
          primaryButtonLabel = stringResource(R.string.btn_add_product),
          onPrimaryAction = { onAddProduct(selectedCategory) },
          primaryButtonTag = "shop_empty_add_product",
        )
      }
    } else if (filteredResults.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Rounded.Inventory2,
          title = "No matching stock items",
          subtitle = "Try clearing the search query or switching category/stock filters.",
          primaryButtonLabel = "Reset Filters",
          onPrimaryAction = {
            viewModel.updateShopSearchQuery("")
            viewModel.setShopSelectedCategory(null)
            if (stockFilter != StockFilter.ALL) viewModel.toggleShopStockFilter(stockFilter)
          },
          primaryButtonTag = "shop_reset_filters_button",
        )
      }
    } else {
      items(filteredResults, key = { it.product.id }) { result ->
        ShopInventoryCard(
          product = result.product,
          onCardClick = { onProductClick(result.product.id) },
          onDecrease = { viewModel.adjustStock(result.product.id, -1) },
          onIncrease = { viewModel.adjustStock(result.product.id, 1) },
          onEdit = { onEditProduct(result.product.id) },
          onEditUniversal = { onEditUniversalForProduct(result.product) },
        )
      }
    }
  }
}

@Composable
private fun SummaryMetricCard(
  label: String,
  value: String,
  caption: String,
  icon: ImageVector,
  selected: Boolean,
  accentColor: Color,
  bgColor: Color,
  borderColor: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    modifier = modifier.testTag(testTag),
    shape = RoundedCornerShape(16.dp),
    color = bgColor,
    border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
    shadowElevation = 1.dp,
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(18.dp),
        )
      }
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = caption,
        style = MaterialTheme.typography.bodySmall,
        color = accentColor,
        maxLines = 1,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShopInventoryCard(
  product: ShopProduct,
  onCardClick: () -> Unit,
  onDecrease: () -> Unit,
  onIncrease: () -> Unit,
  onEdit: () -> Unit,
  onEditUniversal: () -> Unit,
) {
  Surface(
    onClick = onCardClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("shop_product_card_${product.id}"),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    shadowElevation = 2.dp,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Top badges row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FlowRow(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (product.isOfficialLinked) {
            OfficialBadge()
          }
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = product.category.displayName,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          if (!product.universalGroupId.isNullOrBlank()) {
            UniversalGroupBadge(groupCode = product.universalGroupId)
          }
          if (product.boxNumber.isNotBlank()) {
            BoxNumberBadge(boxNumber = product.boxNumber)
          }
          if (product.screenSize.isNotBlank()) {
            ScreenSizeBadge(screenSize = product.screenSize)
          }
        }
        if (product.stockStatus != StockStatus.NORMAL) {
          Spacer(modifier = Modifier.width(8.dp))
          StockStatusBadge(status = product.stockStatus)
        }
      }

      // Title
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = product.mainModel,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (product.productName.isNotBlank() && !product.productName.equals(product.mainModel, ignoreCase = true)) {
          Text(
            text = product.productName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Compatible Models preview
      if (product.compatibleModels.isNotEmpty()) {
        Text(
          text = "Compatible: ${product.compatibleModels.joinToString(", ")}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      // Stock Stepper + Edit + Edit Universal
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        StockStepper(
          quantity = product.stockQuantity,
          onDecrease = onDecrease,
          onIncrease = onIncrease,
          testTagPrefix = "shop_stock_${product.id}",
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = onEdit,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("shop_edit_product_${product.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = stringResource(R.string.action_edit), style = MaterialTheme.typography.labelMedium)
          }

          OutlinedButton(
            onClick = onEditUniversal,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("shop_edit_universal_${product.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Link,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Universal", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }
  }
}
