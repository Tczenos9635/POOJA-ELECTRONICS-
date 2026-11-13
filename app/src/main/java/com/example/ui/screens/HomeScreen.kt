package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.PartCategory
import com.example.data.ProductSearchResult
import com.example.data.ShopProduct
import com.example.data.UniversalGroup
import com.example.ui.ShopViewModel
import com.example.ui.components.AccountMenuDialog
import com.example.ui.components.AiAssistantPillButton
import com.example.ui.components.BoxNumberBadge
import com.example.ui.components.CategoryCardChip
import com.example.ui.components.CategoryGridCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OfficialBadge
import com.example.ui.components.ScreenSizeBadge
import com.example.ui.components.ShopBrandHeader
import com.example.ui.components.ShopSearchBar
import com.example.ui.components.StockStatusBadge
import com.example.ui.components.TrackOfficialGroupDialog
import com.example.ui.components.UniversalGroupBadge
import com.example.ui.theme.StockNormalBg
import com.example.ui.theme.StockNormalBorder
import com.example.ui.theme.StockNormalText
import com.example.ui.theme.StockOutBg
import com.example.ui.theme.StockOutBorder
import com.example.ui.theme.StockOutText

@Composable
fun HomeScreen(
  viewModel: ShopViewModel,
  onCategoryClick: (PartCategory) -> Unit,
  onProductClick: (Long) -> Unit,
  onEditUniversalGroup: (Long) -> Unit,
  onNavigateToAddUniversal: () -> Unit,
  onNavigateToAddProduct: () -> Unit,
  onOpenAiChat: () -> Unit,
  onOpenPartLens: () -> Unit,
  onOpenAdminPanel: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val searchQuery by viewModel.homeSearchQuery.collectAsStateWithLifecycle()
  val searchResults by viewModel.homeSearchResults.collectAsStateWithLifecycle()
  val resultCategoryFilter by viewModel.homeResultCategoryFilter.collectAsStateWithLifecycle()
  val categoryCounts by viewModel.categoryCounts.collectAsStateWithLifecycle()
  val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
  val universalGroups by viewModel.allUniversalGroups.collectAsStateWithLifecycle()
  val officialGroups by viewModel.officialGroups.collectAsStateWithLifecycle()
  val officialSyncError by viewModel.officialSyncError.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

  var showAccountDialog by remember { mutableStateOf(false) }
  var pendingTrackOfficialGroup by remember { mutableStateOf<UniversalGroup?>(null) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    item {
      ShopBrandHeader(
        title = stringResource(R.string.title_mobile_parts),
        trailingContent = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            AiAssistantPillButton(onClick = onOpenAiChat)
            IconButton(
              onClick = { showAccountDialog = true },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("account_menu_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = "Account and Official Sync",
                tint = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
              )
            }
          }
        },
      )
    }

    item {
      ShopSearchBar(
        query = searchQuery,
        onQueryChange = viewModel::updateHomeSearchQuery,
        placeholder = stringResource(R.string.hint_home_search),
        testTag = "home_search_bar",
        onLensClick = onOpenPartLens,
      )
    }

    if (officialSyncError != null) {
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = StockOutBg,
          border = BorderStroke(1.dp, StockOutBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Cloud Sync Notice: $officialSyncError",
            style = MaterialTheme.typography.bodySmall,
            color = StockOutText,
            modifier = Modifier.padding(12.dp),
          )
        }
      }
    }

    if (isAdmin && searchQuery.isBlank()) {
      item {
        Surface(
          onClick = onOpenAdminPanel,
          shape = RoundedCornerShape(16.dp),
          color = StockNormalBg,
          border = BorderStroke(1.dp, StockNormalBorder),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("open_admin_panel_button"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Rounded.AdminPanelSettings,
                contentDescription = null,
                tint = StockNormalText,
                modifier = Modifier.size(22.dp),
              )
              Column {
                Text(
                  text = stringResource(R.string.title_admin_panel),
                  style = MaterialTheme.typography.titleSmall,
                  color = StockNormalText,
                )
                Text(
                  text = "Publish Official Universal updates for all users (${officialGroups.size} live)",
                  style = MaterialTheme.typography.bodySmall,
                  color = StockNormalText.copy(alpha = 0.85f),
                )
              }
            }
            Text(
              text = "Manage",
              style = MaterialTheme.typography.labelLarge,
              color = StockNormalText,
            )
          }
        }
      }
    }

    if (searchQuery.isNotBlank()) {
      item {
        val availableCategories = PartCategory.entries
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          item {
            CategoryCardChip(
              label = "All",
              icon = Icons.Rounded.AddLink,
              selected = resultCategoryFilter == null,
              onClick = { viewModel.setHomeResultCategoryFilter(null) },
              testTag = "home_filter_all",
            )
          }
          items(availableCategories) { category ->
            CategoryCardChip(
              label = category.displayName,
              icon = category.icon(),
              selected = resultCategoryFilter == category,
              onClick = { viewModel.setHomeResultCategoryFilter(category) },
              testTag = "home_filter_${category.name.lowercase()}",
            )
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Compatible Parts (${searchResults.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          if (resultCategoryFilter != null) {
            TextButton(onClick = { viewModel.setHomeResultCategoryFilter(null) }) {
              Text("Show All")
            }
          }
        }
      }

      if (searchResults.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Rounded.SearchOff,
            title = "No matching parts found",
            subtitle = "No saved part or universal model matches \"$searchQuery\". Add a new product or universal group to track it.",
            primaryButtonLabel = stringResource(R.string.btn_add_universal),
            onPrimaryAction = onNavigateToAddUniversal,
            primaryButtonTag = "home_empty_add_universal",
            secondaryButtonLabel = stringResource(R.string.btn_add_product),
            onSecondaryAction = onNavigateToAddProduct,
          )
        }
      } else {
        items(searchResults, key = { "${it.product.id}_${it.product.universalGroupId ?: ""}" }) { result ->
          HomeSearchResultCard(
            result = result,
            onClick = {
              val id = result.product.id
              if (id > 0L) {
                onProductClick(id)
              } else {
                val matchingGroup = officialGroups.firstOrNull {
                  it.groupCode.equals(result.product.universalGroupId, ignoreCase = true)
                }
                if (matchingGroup != null) {
                  pendingTrackOfficialGroup = matchingGroup
                }
              }
            },
          )
        }
      }
    } else {
      item {
        Text(
          text = stringResource(R.string.label_part_categories),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      // 2-column responsive grid of all 7 part categories
      val categoryPairs = PartCategory.entries.chunked(2)
      items(categoryPairs.size) { rowIndex ->
        val pair = categoryPairs[rowIndex]
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          pair.forEach { category ->
            CategoryGridCard(
              category = category,
              count = categoryCounts[category] ?: 0,
              selected = false,
              onClick = { onCategoryClick(category) },
              modifier = Modifier.weight(1f),
            )
          }
          if (pair.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }

      if (universalGroups.isNotEmpty()) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.label_popular_groups),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = "${universalGroups.size} saved",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        item {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(universalGroups.take(10), key = { "${it.isOfficial}_${it.groupCode}_${it.id}" }) { group ->
              HomeUniversalGroupCard(
                group = group,
                isAdmin = isAdmin,
                onClick = {
                  val linkedId = group.linkedProductId
                  if (linkedId != null) {
                    onProductClick(linkedId)
                  } else if (group.isOfficial) {
                    pendingTrackOfficialGroup = group
                  } else {
                    onEditUniversalGroup(group.id)
                  }
                },
                onEditClick = {
                  if (group.isOfficial) {
                    if (isAdmin) onOpenAdminPanel() else pendingTrackOfficialGroup = group
                  } else {
                    onEditUniversalGroup(group.id)
                  }
                },
              )
            }
          }
        }
      }

      if (allProducts.isEmpty() && officialGroups.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Rounded.AddLink,
            title = stringResource(R.string.empty_products_title),
            subtitle = "Create a universal compatibility group or add shop inventory to start searching parts in seconds.",
            primaryButtonLabel = stringResource(R.string.btn_add_universal),
            onPrimaryAction = onNavigateToAddUniversal,
            primaryButtonTag = "home_empty_create_universal",
            secondaryButtonLabel = stringResource(R.string.btn_add_product),
            onSecondaryAction = onNavigateToAddProduct,
          )
        }
      } else if (allProducts.isNotEmpty()) {
        item {
          Text(
            text = stringResource(R.string.label_recent_models),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }

        items(allProducts.take(8), key = { it.id }) { product ->
          RecentModelCard(
            product = product,
            onClick = { onProductClick(product.id) },
          )
        }
      }
    }
  }

  if (showAccountDialog) {
    AccountMenuDialog(
      email = viewModel.currentUserEmail,
      isAdmin = isAdmin,
      onDismiss = { showAccountDialog = false },
      onOpenAdminPanel = if (isAdmin) onOpenAdminPanel else null,
      onSignOut = onSignOut,
    )
  }

  pendingTrackOfficialGroup?.let { group ->
    TrackOfficialGroupDialog(
      group = group,
      onDismiss = { pendingTrackOfficialGroup = null },
      onTrackInShop = { stock, box ->
        pendingTrackOfficialGroup = null
        viewModel.trackOfficialGroupInShop(
          group = group,
          initialStock = stock,
          boxNumber = box,
          onTracked = { newProductId -> onProductClick(newProductId) },
        )
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeSearchResultCard(
  result: ProductSearchResult,
  onClick: () -> Unit,
) {
  val product = result.product
  Surface(
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("home_search_result_${product.id}"),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    shadowElevation = 2.dp,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = product.category.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = product.category.displayName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
              )
            }
          }
          if (!product.universalGroupId.isNullOrBlank()) {
            UniversalGroupBadge(groupCode = product.universalGroupId)
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (product.id < 0L) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            Text(
              text = "Tap to Track",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        } else {
          StockStatusBadge(status = product.stockStatus, stockQuantity = product.stockQuantity)
        }
      }

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

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (product.screenSize.isNotBlank()) {
          ScreenSizeBadge(screenSize = product.screenSize)
        }
        if (product.boxNumber.isNotBlank()) {
          BoxNumberBadge(boxNumber = product.boxNumber)
        }
        if (result.matchReason.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Text(
              text = "Match: ${result.matchReason}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }

      if (product.compatibleModels.isNotEmpty()) {
        Text(
          text = "Fits: ${product.allModels.joinToString(", ")}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun HomeUniversalGroupCard(
  group: UniversalGroup,
  isAdmin: Boolean,
  onClick: () -> Unit,
  onEditClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    modifier = Modifier
      .width(248.dp)
      .testTag("home_universal_group_${group.groupCode}"),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    shadowElevation = 2.dp,
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (group.isOfficial) {
            OfficialBadge()
          }
          UniversalGroupBadge(groupCode = group.groupCode)
        }
        if (!group.isOfficial || isAdmin) {
          IconButton(
            onClick = onEditClick,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("home_edit_group_${group.groupCode}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = stringResource(R.string.action_edit),
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      Text(
        text = group.mainModel,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Text(
        text = "${group.category.displayName} • ${group.allModels.size} models",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (group.screenSize.isNotBlank()) {
          ScreenSizeBadge(screenSize = group.screenSize)
        } else {
          Box(modifier = Modifier.height(1.dp))
        }
        if (group.linkedProductId == null && group.isOfficial) {
          Text(
            text = "Track Stock",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
          )
        } else {
          StockStatusBadge(status = group.stockStatus, stockQuantity = group.stockQuantity)
        }
      }
    }
  }
}

@Composable
private fun RecentModelCard(
  product: ShopProduct,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("home_recent_product_${product.id}"),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    shadowElevation = 1.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(42.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = product.category.icon(),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = product.mainModel,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          val subParts = buildList {
            add(product.category.displayName)
            if (!product.universalGroupId.isNullOrBlank()) add(product.universalGroupId)
            if (product.screenSize.isNotBlank()) add(product.screenSize)
            if (product.boxNumber.isNotBlank()) add("Box ${product.boxNumber}")
          }.joinToString(" • ")
          Text(
            text = subParts,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      StockStatusBadge(status = product.stockStatus, stockQuantity = product.stockQuantity)
    }
  }
}
