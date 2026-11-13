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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DevicesOther
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.BrandModelEntry
import com.example.data.PartCategory
import com.example.data.UniversalGroup
import com.example.ui.ShopViewModel
import com.example.ui.components.BoxNumberBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OfficialBadge
import com.example.ui.components.ScreenSizeBadge
import com.example.ui.components.ShopSearchBar
import com.example.ui.components.StockStatusBadge
import com.example.ui.components.TrackOfficialGroupDialog
import com.example.ui.components.UniversalGroupBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCompatibilityScreen(
  category: PartCategory,
  viewModel: ShopViewModel,
  onBack: () -> Unit,
  onProductClick: (Long) -> Unit,
  onAddProductInCategory: () -> Unit,
  onAddUniversalInCategory: () -> Unit,
) {
  var searchQuery by rememberSaveable { mutableStateOf("") }
  val sectionsFlow = remember(category, searchQuery) {
    viewModel.categoryBrandSections(category, searchQuery)
  }
  val sections by sectionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
  val officialGroups by viewModel.officialGroups.collectAsStateWithLifecycle()
  var pendingTrackGroup by remember { mutableStateOf<UniversalGroup?>(null) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "${category.displayName} Compatibility",
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("category_compat_back"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
            )
          }
        },
        actions = {
          IconButton(
            onClick = onAddProductInCategory,
            modifier = Modifier.testTag("category_compat_add"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Add,
              contentDescription = stringResource(R.string.btn_add_product),
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        ShopSearchBar(
          query = searchQuery,
          onQueryChange = { searchQuery = it },
          placeholder = stringResource(R.string.hint_category_search),
          testTag = "category_compat_search",
        )
      }

      if (sections.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Rounded.DevicesOther,
            title = if (searchQuery.isBlank()) {
              "No ${category.displayName} models yet"
            } else {
              "No models match \"$searchQuery\""
            },
            subtitle = "Add a universal group or shop product under ${category.displayName} to view brand-wise compatibility.",
            primaryButtonLabel = stringResource(R.string.btn_add_universal),
            onPrimaryAction = onAddUniversalInCategory,
            primaryButtonTag = "category_empty_add_universal",
            secondaryButtonLabel = stringResource(R.string.btn_add_product),
            onSecondaryAction = onAddProductInCategory,
          )
        }
      } else {
        sections.forEach { section ->
          item(key = "brand_${section.brand}") {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = section.brand,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
              )
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text = "${section.entries.size} models",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
          }

          items(
            items = section.entries,
            key = { "${section.brand}_${it.modelName}_${it.universalGroupCode ?: ""}_${it.productId}" },
          ) { entry ->
            BrandModelCard(
              entry = entry,
              onClick = {
                if (entry.productId > 0L) {
                  onProductClick(entry.productId)
                } else {
                  val match = officialGroups.firstOrNull {
                    it.groupCode.equals(entry.universalGroupCode, ignoreCase = true)
                  }
                  if (match != null) {
                    pendingTrackGroup = match
                  }
                }
              },
            )
          }
        }
      }
    }
  }

  pendingTrackGroup?.let { group ->
    TrackOfficialGroupDialog(
      group = group,
      onDismiss = { pendingTrackGroup = null },
      onTrackInShop = { stock, box ->
        pendingTrackGroup = null
        viewModel.trackOfficialGroupInShop(
          group = group,
          initialStock = stock,
          boxNumber = box,
          onTracked = { newId -> onProductClick(newId) },
        )
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrandModelCard(
  entry: BrandModelEntry,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("brand_model_card_${entry.productId}_${entry.modelName.lowercase().replace(" ", "_")}"),
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
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = entry.modelName,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (!entry.isMainModel) {
          Text(
            text = "Shared with ${entry.mainModel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (entry.isOfficial) {
            OfficialBadge()
          }
          if (!entry.universalGroupCode.isNullOrBlank()) {
            UniversalGroupBadge(groupCode = entry.universalGroupCode)
          }
          if (entry.screenSize.isNotBlank()) {
            ScreenSizeBadge(screenSize = entry.screenSize)
          }
          if (entry.boxNumber.isNotBlank()) {
            BoxNumberBadge(boxNumber = entry.boxNumber)
          }
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      if (entry.productId < 0L) {
        Text(
          text = "Track",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      } else {
        StockStatusBadge(status = entry.stockStatus, stockQuantity = entry.stockQuantity)
      }
    }
  }
}
