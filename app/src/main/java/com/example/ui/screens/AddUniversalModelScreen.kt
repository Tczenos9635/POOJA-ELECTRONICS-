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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.PartCategory
import com.example.data.SCREEN_SIZE_PRESETS
import com.example.data.SearchEngine
import com.example.data.UniversalGroup
import com.example.ui.ShopViewModel
import com.example.ui.components.BoxNumberBadge
import com.example.ui.components.CategoryCardChip
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OfficialBadge
import com.example.ui.components.RemovableModelChip
import com.example.ui.components.ScreenSizeBadge
import com.example.ui.components.ShopBrandHeader
import com.example.ui.components.ShopSearchBar
import com.example.ui.components.StockStatusBadge
import com.example.ui.components.StockStepper
import com.example.ui.components.TrackOfficialGroupDialog
import com.example.ui.components.UniversalGroupBadge
import com.example.ui.theme.StockNormalBg
import com.example.ui.theme.StockNormalBorder
import com.example.ui.theme.StockNormalText
import com.example.ui.theme.StockOutText

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddUniversalModelScreen(
  viewModel: ShopViewModel,
  initialCategory: PartCategory = PartCategory.TEMPERED_GLASS,
  onEditGroup: (Long) -> Unit,
  onSavedSuccess: (String) -> Unit,
  onOpenAdminPanel: () -> Unit = {},
  onProductClick: (Long) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val universalGroups by viewModel.allUniversalGroups.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

  var activeTabIndex by rememberSaveable { mutableIntStateOf(0) } // 0 = Create New, 1 = Saved Groups
  var selectedCategoryName by rememberSaveable { mutableStateOf(initialCategory.name) }
  val selectedCategory = PartCategory.fromName(selectedCategoryName)

  var mainModel by rememberSaveable { mutableStateOf("") }
  var mainModelError by rememberSaveable { mutableStateOf<String?>(null) }
  var screenSize by rememberSaveable { mutableStateOf("") }
  var customGroupCode by rememberSaveable { mutableStateOf("") }
  var compatibleInput by rememberSaveable { mutableStateOf("") }
  val compatibleModels = remember { mutableStateListOf<String>() }

  var initialStock by rememberSaveable { mutableIntStateOf(0) }
  var minimumStockText by rememberSaveable { mutableStateOf("3") }
  var boxNumber by rememberSaveable { mutableStateOf("") }

  var savedSearchQuery by rememberSaveable { mutableStateOf("") }
  var pendingDeleteGroup by remember { mutableStateOf<UniversalGroup?>(null) }
  var pendingTrackGroup by remember { mutableStateOf<UniversalGroup?>(null) }

  LaunchedEffect(selectedCategoryName, universalGroups.size) {
    customGroupCode = viewModel.generateNextCode(selectedCategory)
  }

  fun addCompatibleModel() {
    val trimmed = compatibleInput.trim()
    if (trimmed.isNotEmpty() &&
      !trimmed.equals(mainModel.trim(), ignoreCase = true) &&
      compatibleModels.none { it.equals(trimmed, ignoreCase = true) }
    ) {
      compatibleModels.add(trimmed)
      compatibleInput = ""
    }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      ShopBrandHeader(
        title = stringResource(R.string.title_add_universal),
      )
    }

    // 2-Segment Switch: Create New / Saved Groups
    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          SegmentTabButton(
            label = "Create New",
            selected = activeTabIndex == 0,
            onClick = { activeTabIndex = 0 },
            testTag = "universal_tab_create",
            modifier = Modifier.weight(1f),
          )
          SegmentTabButton(
            label = "Saved Groups (${universalGroups.size})",
            selected = activeTabIndex == 1,
            onClick = { activeTabIndex = 1 },
            testTag = "universal_tab_saved",
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    if (isAdmin) {
      item {
        Surface(
          onClick = onOpenAdminPanel,
          shape = RoundedCornerShape(14.dp),
          color = StockNormalBg,
          border = BorderStroke(1.dp, StockNormalBorder),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("universal_open_admin_panel"),
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
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = "Want to push to ALL app users? Open Official Panel",
                style = MaterialTheme.typography.labelLarge,
                color = StockNormalText,
              )
            }
          }
        }
      }
    }

    if (activeTabIndex == 0) {
      item {
        Text(
          text = "Select Category",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(PartCategory.entries) { category ->
            CategoryCardChip(
              label = category.displayName,
              icon = category.icon(),
              selected = selectedCategory == category,
              onClick = { selectedCategoryName = category.name },
              testTag = "add_universal_category_${category.name.lowercase()}",
            )
          }
        }
      }

      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          shadowElevation = 2.dp,
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Text(
              text = "Add New Universal Model",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
              value = mainModel,
              onValueChange = {
                mainModel = it
                mainModelError = null
              },
              label = { Text("Main Model Name *") },
              placeholder = { Text("e.g. Oppo F31") },
              isError = mainModelError != null,
              supportingText = mainModelError?.let { err -> { Text(err) } },
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("universal_main_model_input"),
            )

            OutlinedTextField(
              value = customGroupCode,
              onValueChange = { customGroupCode = it.uppercase() },
              label = { Text("Universal Group ID") },
              placeholder = { Text("e.g. TG-001") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next,
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("universal_group_code_input"),
            )

            if (selectedCategory.supportsScreenSize) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  text = "Screen Size (Manual selection)",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  SCREEN_SIZE_PRESETS.forEach { preset ->
                    FilterChip(
                      selected = screenSize.trim() == preset,
                      onClick = {
                        screenSize = if (screenSize.trim() == preset) "" else preset
                      },
                      label = { Text(preset) },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                      ),
                    )
                  }
                }
                OutlinedTextField(
                  value = screenSize,
                  onValueChange = { screenSize = it },
                  label = { Text("Custom Screen Size") },
                  placeholder = { Text("e.g. 6.57\"") },
                  singleLine = true,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("universal_screen_size_input"),
                )
              }
            }

            // Compatible Models Builder
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Compatible Models",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedTextField(
                  value = compatibleInput,
                  onValueChange = { compatibleInput = it },
                  label = { Text("Add compatible model") },
                  placeholder = { Text("e.g. Vivo V30") },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                  ),
                  keyboardActions = KeyboardActions(onDone = { addCompatibleModel() }),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("universal_compatible_model_input"),
                )
                Button(
                  onClick = { addCompatibleModel() },
                  shape = RoundedCornerShape(12.dp),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("universal_add_model_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Add Model")
                }
              }

              if (compatibleModels.isNotEmpty()) {
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  compatibleModels.forEach { model ->
                    RemovableModelChip(
                      modelName = model,
                      onRemove = { compatibleModels.remove(model) },
                    )
                  }
                }
              }
            }

            // Shop Stock & Box Number Section
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = "Shop Inventory Stock",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "Initial Stock (pcs)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  StockStepper(
                    quantity = initialStock,
                    onDecrease = { if (initialStock > 0) initialStock-- },
                    onIncrease = { initialStock++ },
                    testTagPrefix = "universal_initial_stock",
                  )
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  OutlinedTextField(
                    value = minimumStockText,
                    onValueChange = { value ->
                      if (value.all { it.isDigit() }) minimumStockText = value
                    },
                    label = { Text("Min Stock Alert") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                  )
                  if (selectedCategory.supportsBoxNumber || boxNumber.isNotEmpty()) {
                    OutlinedTextField(
                      value = boxNumber,
                      onValueChange = { boxNumber = it.uppercase() },
                      label = { Text("Box Number") },
                      placeholder = { Text("B-023") },
                      singleLine = true,
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier
                        .weight(1f)
                        .testTag("universal_box_number_input"),
                    )
                  }
                }

                if (selectedCategory == PartCategory.BACK_COVER && boxNumber.isBlank()) {
                  Text(
                    text = "Tip: Enter Box Number (e.g. B-023) to locate this Back Cover quickly in your shop.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }

            Button(
              onClick = {
                if (mainModel.trim().isEmpty()) {
                  mainModelError = "Please enter the main model name"
                  return@Button
                }
                if (compatibleInput.trim().isNotEmpty()) {
                  addCompatibleModel()
                }
                val minStock = minimumStockText.toIntOrNull()?.coerceAtLeast(0) ?: 3
                val savedCode = customGroupCode.trim().uppercase()
                viewModel.saveNewUniversalGroup(
                  category = selectedCategory,
                  mainModel = mainModel,
                  screenSize = screenSize,
                  compatibleModels = compatibleModels.toList(),
                  customGroupCode = savedCode,
                  initialStock = initialStock,
                  minimumStock = minStock,
                  boxNumber = boxNumber,
                  onSaved = {
                    val label = "$mainModel (${selectedCategory.displayName})"
                    mainModel = ""
                    screenSize = ""
                    compatibleInput = ""
                    compatibleModels.clear()
                    initialStock = 0
                    boxNumber = ""
                    onSavedSuccess("Saved universal group for $label")
                    activeTabIndex = 1
                  },
                )
              },
              shape = RoundedCornerShape(14.dp),
              contentPadding = PaddingValues(vertical = 15.dp),
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("save_universal_model_button"),
            ) {
              Text(
                text = stringResource(R.string.btn_save_universal),
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }
      }
    } else {
      item {
        ShopSearchBar(
          query = savedSearchQuery,
          onQueryChange = { savedSearchQuery = it },
          placeholder = "Search universal group, model or code…",
          testTag = "universal_saved_search",
        )
      }

      val displayedGroups = SearchEngine.searchUniversalGroups(universalGroups, savedSearchQuery)
      if (universalGroups.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Rounded.AddLink,
            title = stringResource(R.string.empty_universal_title),
            subtitle = stringResource(R.string.empty_universal_subtitle),
            primaryButtonLabel = stringResource(R.string.btn_add_universal),
            onPrimaryAction = { activeTabIndex = 0 },
            primaryButtonTag = "universal_empty_create_button",
          )
        }
      } else if (displayedGroups.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Rounded.AddLink,
            title = "No matching universal groups",
            subtitle = "Try another model name or group code.",
            primaryButtonLabel = "Clear Search",
            onPrimaryAction = { savedSearchQuery = "" },
            primaryButtonTag = "universal_clear_search_button",
          )
        }
      } else {
        items(displayedGroups, key = { "${it.isOfficial}_${it.groupCode}_${it.id}" }) { group ->
          SavedUniversalGroupCard(
            group = group,
            isAdmin = isAdmin,
            onEdit = { onEditGroup(group.id) },
            onDelete = { pendingDeleteGroup = group },
            onTrackOfficial = { pendingTrackGroup = group },
            onViewLinkedProduct = { group.linkedProductId?.let(onProductClick) },
            onOpenAdminPanel = onOpenAdminPanel,
          )
        }
      }
    }
  }

  pendingDeleteGroup?.let { groupToDelete ->
    DeleteConfirmationDialog(
      message = "Are you sure you want to delete universal group ${groupToDelete.groupCode} (${groupToDelete.mainModel})?",
      onDismiss = { pendingDeleteGroup = null },
      onConfirmDelete = {
        viewModel.deleteUniversalGroup(groupToDelete.id)
        pendingDeleteGroup = null
      },
    )
  }

  pendingTrackGroup?.let { groupToTrack ->
    TrackOfficialGroupDialog(
      group = groupToTrack,
      onDismiss = { pendingTrackGroup = null },
      onTrackInShop = { stock, box ->
        pendingTrackGroup = null
        viewModel.trackOfficialGroupInShop(
          group = groupToTrack,
          initialStock = stock,
          boxNumber = box,
          onTracked = { newId ->
            onSavedSuccess("Added ${groupToTrack.groupCode} to My Shop")
            onProductClick(newId)
          },
        )
      },
    )
  }
}

@Composable
private fun SegmentTabButton(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    modifier = modifier
      .minimumInteractiveComponentSize()
      .testTag(testTag),
    shape = RoundedCornerShape(10.dp),
    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
    shadowElevation = if (selected) 2.dp else 0.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp, horizontal = 12.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedUniversalGroupCard(
  group: UniversalGroup,
  isAdmin: Boolean,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onTrackOfficial: () -> Unit,
  onViewLinkedProduct: () -> Unit,
  onOpenAdminPanel: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("saved_universal_card_${group.groupCode}"),
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
          if (group.isOfficial) {
            OfficialBadge()
          }
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = group.category.displayName,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          UniversalGroupBadge(groupCode = group.groupCode)
          if (group.screenSize.isNotBlank()) {
            ScreenSizeBadge(screenSize = group.screenSize)
          }
          if (group.boxNumber.isNotBlank()) {
            BoxNumberBadge(boxNumber = group.boxNumber)
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (group.isOfficial && group.linkedProductId == null) {
          Text(
            text = "Not in shop",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          StockStatusBadge(status = group.stockStatus, stockQuantity = group.stockQuantity)
        }
      }

      Text(
        text = group.mainModel,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      if (group.isOfficial && group.releaseNote.isNotBlank()) {
        Text(
          text = group.releaseNote,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      if (group.compatibleModels.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          group.compatibleModels.forEach { model ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Text(
                text = model,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
          }
        }
      } else {
        Text(
          text = "No compatible models linked yet",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (group.isOfficial) {
          if (isAdmin) {
            OutlinedButton(
              onClick = onOpenAdminPanel,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("edit_official_group_${group.groupCode}"),
            ) {
              Icon(
                imageVector = Icons.Rounded.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Official Panel")
            }
            Spacer(modifier = Modifier.width(8.dp))
          }
          if (group.linkedProductId == null) {
            Button(
              onClick = onTrackOfficial,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("track_official_group_${group.groupCode}"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Track Stock")
            }
          } else {
            OutlinedButton(
              onClick = onViewLinkedProduct,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Text("In My Shop")
            }
          }
        } else {
          IconButton(
            onClick = onDelete,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("delete_universal_group_${group.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = stringResource(R.string.action_delete),
              tint = StockOutText,
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          OutlinedButton(
            onClick = onEdit,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("edit_universal_group_${group.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.action_edit))
          }
        }
      }
    }
  }
}
