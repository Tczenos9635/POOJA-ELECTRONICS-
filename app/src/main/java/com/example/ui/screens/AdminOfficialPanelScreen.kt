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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.PartCategory
import com.example.data.SCREEN_SIZE_PRESETS
import com.example.data.SearchEngine
import com.example.data.UniversalGroup
import com.example.ui.ShopViewModel
import com.example.ui.components.CategoryCardChip
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OfficialBadge
import com.example.ui.components.RemovableModelChip
import com.example.ui.components.ScreenSizeBadge
import com.example.ui.components.ShopSearchBar
import com.example.ui.components.UniversalGroupBadge
import com.example.ui.theme.StockNormalBg
import com.example.ui.theme.StockNormalBorder
import com.example.ui.theme.StockNormalText
import com.example.ui.theme.StockOutBg
import com.example.ui.theme.StockOutBorder
import com.example.ui.theme.StockOutText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminOfficialPanelScreen(
  viewModel: ShopViewModel,
  onBack: () -> Unit,
  onShowMessage: (String) -> Unit,
) {
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val officialGroups by viewModel.officialGroups.collectAsStateWithLifecycle()
  val isLoading by viewModel.adminActionLoading.collectAsStateWithLifecycle()
  val actionError by viewModel.adminActionError.collectAsStateWithLifecycle()
  val syncError by viewModel.officialSyncError.collectAsStateWithLifecycle()

  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()

  var editingDocId by rememberSaveable { mutableStateOf<String?>(null) }
  var selectedCategoryName by rememberSaveable { mutableStateOf(PartCategory.TEMPERED_GLASS.name) }
  val selectedCategory = PartCategory.fromName(selectedCategoryName)

  var groupCode by rememberSaveable { mutableStateOf("") }
  var groupCodeError by rememberSaveable { mutableStateOf<String?>(null) }
  var mainModel by rememberSaveable { mutableStateOf("") }
  var mainModelError by rememberSaveable { mutableStateOf<String?>(null) }
  var screenSize by rememberSaveable { mutableStateOf("") }
  var compatibleInput by rememberSaveable { mutableStateOf("") }
  val compatibleModels = remember { mutableStateListOf<String>() }
  var releaseNote by rememberSaveable { mutableStateOf("") }
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var pendingDeleteGroup by remember { mutableStateOf<UniversalGroup?>(null) }

  LaunchedEffect(selectedCategoryName, editingDocId, officialGroups.size) {
    if (editingDocId == null && groupCode.isBlank()) {
      groupCode = viewModel.generateNextOfficialCode(selectedCategory)
    }
  }

  fun resetForm(suggestNext: Boolean = true) {
    editingDocId = null
    mainModel = ""
    mainModelError = null
    groupCodeError = null
    screenSize = ""
    compatibleInput = ""
    compatibleModels.clear()
    releaseNote = ""
    groupCode = if (suggestNext) viewModel.generateNextOfficialCode(selectedCategory) else ""
    viewModel.clearAdminError()
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

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = stringResource(R.string.title_admin_panel),
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = viewModel.currentUserEmail ?: "Admin Console",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("admin_panel_back"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
  ) { innerPadding ->
    if (!isAdmin) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        EmptyStateCard(
          icon = Icons.Rounded.Lock,
          title = "Admin Access Only",
          subtitle = "Official Universal Parts updates can only be published by the verified owner account.",
          primaryButtonLabel = "Go Back",
          onPrimaryAction = onBack,
          primaryButtonTag = "admin_restricted_back_button",
        )
      }
      return@Scaffold
    }

    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Live Broadcast Info Banner
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = StockNormalBg,
          border = BorderStroke(1.dp, StockNormalBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.AdminPanelSettings,
              contentDescription = null,
              tint = StockNormalText,
              modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Live Global Sync Enabled",
                style = MaterialTheme.typography.titleSmall,
                color = StockNormalText,
              )
              Text(
                text = "Every universal model published here automatically updates for all Pooja Electronics app users in real time.",
                style = MaterialTheme.typography.bodySmall,
                color = StockNormalText.copy(alpha = 0.85f),
              )
            }
          }
        }
      }

      if (actionError != null || syncError != null) {
        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = StockOutBg,
            border = BorderStroke(1.dp, StockOutBorder),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = actionError ?: syncError ?: "",
              style = MaterialTheme.typography.bodySmall,
              color = StockOutText,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
      }

      // Publish / Edit Form
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
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (editingDocId != null) "Edit Official Group" else "Publish Official Group",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              if (editingDocId != null) {
                TextButton(onClick = { resetForm(suggestNext = true) }) {
                  Text("Cancel Edit")
                }
              }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(PartCategory.entries) { category ->
                CategoryCardChip(
                  label = category.displayName,
                  icon = category.icon(),
                  selected = selectedCategory == category,
                  onClick = {
                    selectedCategoryName = category.name
                    if (editingDocId == null) {
                      groupCode = viewModel.generateNextOfficialCode(category)
                    }
                  },
                  testTag = "admin_cat_${category.name.lowercase()}",
                )
              }
            }

            OutlinedTextField(
              value = groupCode,
              onValueChange = {
                groupCode = it.uppercase()
                groupCodeError = null
              },
              label = { Text("Official Group ID *") },
              placeholder = { Text("e.g. TG-001") },
              isError = groupCodeError != null,
              supportingText = groupCodeError?.let { err -> { Text(err) } },
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next,
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_group_code_input"),
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
                .testTag("admin_main_model_input"),
            )

            if (selectedCategory.supportsScreenSize || screenSize.isNotBlank()) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  text = "Screen Size (Optional)",
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
                    .testTag("admin_screen_size_input"),
                )
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Compatible Models (${compatibleModels.size})",
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
                    .testTag("admin_compat_input"),
                )
                Button(
                  onClick = { addCompatibleModel() },
                  shape = RoundedCornerShape(12.dp),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("admin_add_compat_button"),
                ) {
                  Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Add")
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

            OutlinedTextField(
              value = releaseNote,
              onValueChange = { releaseNote = it },
              label = { Text("Official Update Note (Optional)") },
              placeholder = { Text("e.g. Verified fit for Oppo/Vivo/Realme batch") },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_release_note_input"),
            )

            Button(
              onClick = {
                val cleanCode = groupCode.trim().uppercase()
                val cleanMain = mainModel.trim()
                if (cleanCode.length < 2) {
                  groupCodeError = "Group ID must be at least 2 characters"
                  return@Button
                }
                if (cleanMain.isEmpty()) {
                  mainModelError = "Please enter the main model name"
                  return@Button
                }
                if (compatibleInput.trim().isNotEmpty()) {
                  addCompatibleModel()
                }
                val activeDocId = editingDocId
                if (activeDocId == null) {
                  viewModel.publishOfficialGroup(
                    category = selectedCategory,
                    groupCode = cleanCode,
                    mainModel = cleanMain,
                    screenSize = screenSize,
                    compatibleModels = compatibleModels.toList(),
                    releaseNote = releaseNote,
                    onSuccess = {
                      resetForm(suggestNext = true)
                      onShowMessage("Published Official Update: $cleanCode ($cleanMain)")
                    },
                  )
                } else {
                  viewModel.updateOfficialGroup(
                    docId = activeDocId,
                    category = selectedCategory,
                    groupCode = cleanCode,
                    mainModel = cleanMain,
                    screenSize = screenSize,
                    compatibleModels = compatibleModels.toList(),
                    releaseNote = releaseNote,
                    onSuccess = {
                      resetForm(suggestNext = true)
                      onShowMessage("Updated Official Group: $cleanCode ($cleanMain)")
                    },
                  )
                }
              },
              enabled = !isLoading,
              shape = RoundedCornerShape(14.dp),
              contentPadding = PaddingValues(vertical = 15.dp),
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("admin_publish_button"),
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  color = Color.White,
                  strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Syncing Official Update…")
              } else {
                Icon(
                  imageVector = Icons.Rounded.CloudUpload,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (editingDocId != null) {
                    stringResource(R.string.btn_update_official)
                  } else {
                    stringResource(R.string.btn_publish_official)
                  },
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }
          }
        }
      }

      // Published Official Groups Section
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Published Official Catalog (${officialGroups.size})",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
      }

      item {
        ShopSearchBar(
          query = searchQuery,
          onQueryChange = { searchQuery = it },
          placeholder = "Search published official groups…",
          testTag = "admin_search_bar",
        )
      }

      val filteredOfficial = SearchEngine.searchUniversalGroups(officialGroups, searchQuery)
      if (filteredOfficial.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Rounded.CloudUpload,
            title = if (officialGroups.isEmpty()) {
              "No official updates published yet"
            } else {
              "No matching official groups"
            },
            subtitle = "Use the form above to publish official universal compatibility groups for all app users.",
            primaryButtonLabel = if (searchQuery.isNotBlank()) "Clear Search" else "Scroll to Form",
            onPrimaryAction = {
              if (searchQuery.isNotBlank()) {
                searchQuery = ""
              } else {
                scope.launch { listState.animateScrollToItem(0) }
              }
            },
            primaryButtonTag = "admin_empty_action_button",
          )
        }
      } else {
        items(filteredOfficial, key = { it.officialDocId ?: it.id.toString() }) { group ->
          AdminOfficialGroupCard(
            group = group,
            onEdit = {
              val docId = group.officialDocId ?: return@AdminOfficialGroupCard
              editingDocId = docId
              selectedCategoryName = group.category.name
              groupCode = group.groupCode
              mainModel = group.mainModel
              screenSize = group.screenSize
              compatibleInput = ""
              compatibleModels.clear()
              compatibleModels.addAll(group.compatibleModels)
              releaseNote = group.releaseNote
              viewModel.clearAdminError()
              scope.launch { listState.animateScrollToItem(1) }
            },
            onDelete = { pendingDeleteGroup = group },
          )
        }
      }
    }
  }

  pendingDeleteGroup?.let { groupToDelete ->
    DeleteConfirmationDialog(
      message = "Remove official group ${groupToDelete.groupCode} (${groupToDelete.mainModel}) for all users?",
      onDismiss = { pendingDeleteGroup = null },
      onConfirmDelete = {
        val docId = groupToDelete.officialDocId
        pendingDeleteGroup = null
        if (!docId.isNullOrBlank()) {
          viewModel.deleteOfficialGroup(docId) {
            if (editingDocId == docId) resetForm(suggestNext = true)
            onShowMessage("Deleted Official Group ${groupToDelete.groupCode}")
          }
        }
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminOfficialGroupCard(
  group: UniversalGroup,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("admin_official_card_${group.groupCode}"),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    shadowElevation = 2.dp,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        OfficialBadge()
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
      }

      Text(
        text = group.mainModel,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      if (group.releaseNote.isNotBlank()) {
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
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onDelete,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("admin_delete_official_${group.groupCode}"),
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
            .testTag("admin_edit_official_${group.groupCode}"),
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
