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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.PartCategory
import com.example.data.SCREEN_SIZE_PRESETS
import com.example.ui.ShopViewModel
import com.example.ui.components.CategoryCardChip
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.RemovableModelChip
import com.example.ui.theme.StockOutText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditUniversalGroupScreen(
  groupId: Long,
  viewModel: ShopViewModel,
  onBack: () -> Unit,
  onSaved: () -> Unit,
  onDeleted: () -> Unit,
) {
  val groups by viewModel.allUniversalGroups.collectAsStateWithLifecycle()
  val group = groups.firstOrNull { it.id == groupId }

  var initialized by rememberSaveable(groupId) { mutableStateOf(false) }
  var selectedCategoryName by rememberSaveable(groupId) { mutableStateOf(PartCategory.TEMPERED_GLASS.name) }
  var groupCode by rememberSaveable(groupId) { mutableStateOf("") }
  var mainModel by rememberSaveable(groupId) { mutableStateOf("") }
  var mainModelError by rememberSaveable(groupId) { mutableStateOf<String?>(null) }
  var screenSize by rememberSaveable(groupId) { mutableStateOf("") }
  var compatibleInput by rememberSaveable(groupId) { mutableStateOf("") }
  val compatibleModels = remember(groupId) { mutableStateListOf<String>() }
  var showDeleteDialog by remember { mutableStateOf(false) }

  LaunchedEffect(group?.id) {
    if (group != null && !initialized) {
      selectedCategoryName = group.category.name
      groupCode = group.groupCode
      mainModel = group.mainModel
      screenSize = group.screenSize
      compatibleModels.clear()
      compatibleModels.addAll(group.compatibleModels)
      initialized = true
    }
  }

  val selectedCategory = PartCategory.fromName(selectedCategoryName)

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
        title = { Text("Edit Universal Model", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("edit_universal_back"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
            )
          }
        },
        actions = {
          if (group != null) {
            IconButton(
              onClick = { showDeleteDialog = true },
              modifier = Modifier.testTag("edit_universal_delete_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.action_delete),
                tint = StockOutText,
              )
            }
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
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Text(
          text = "Category",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
          modifier = Modifier.padding(top = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(PartCategory.entries) { category ->
            CategoryCardChip(
              label = category.displayName,
              icon = category.icon(),
              selected = selectedCategory == category,
              onClick = { selectedCategoryName = category.name },
              testTag = "edit_universal_cat_${category.name.lowercase()}",
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
            OutlinedTextField(
              value = groupCode,
              onValueChange = { groupCode = it.uppercase() },
              label = { Text("Universal Group ID") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_universal_code_input"),
            )

            OutlinedTextField(
              value = mainModel,
              onValueChange = {
                mainModel = it
                mainModelError = null
              },
              label = { Text("Main Model Name *") },
              isError = mainModelError != null,
              supportingText = mainModelError?.let { err -> { Text(err) } },
              singleLine = true,
              keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_universal_main_model_input"),
            )

            if (selectedCategory.supportsScreenSize || screenSize.isNotBlank()) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  text = "Screen Size",
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
                    .testTag("edit_universal_screen_size_input"),
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
                  label = { Text("Add model") },
                  placeholder = { Text("e.g. Oppo A79") },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                  ),
                  keyboardActions = KeyboardActions(onDone = { addCompatibleModel() }),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("edit_universal_add_model_input"),
                )
                Button(
                  onClick = { addCompatibleModel() },
                  shape = RoundedCornerShape(12.dp),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("edit_universal_add_model_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
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

            Button(
              onClick = {
                if (mainModel.trim().isEmpty()) {
                  mainModelError = "Main model name is required"
                  return@Button
                }
                if (compatibleInput.trim().isNotEmpty()) {
                  addCompatibleModel()
                }
                viewModel.updateUniversalGroup(
                  groupId = groupId,
                  category = selectedCategory,
                  groupCode = groupCode,
                  mainModel = mainModel,
                  screenSize = screenSize,
                  compatibleModels = compatibleModels.toList(),
                  onSaved = onSaved,
                )
              },
              shape = RoundedCornerShape(14.dp),
              contentPadding = PaddingValues(vertical = 15.dp),
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("update_universal_model_button"),
            ) {
              Text(
                text = stringResource(R.string.btn_update_universal),
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }
      }
    }
  }

  if (showDeleteDialog) {
    DeleteConfirmationDialog(
      message = "Are you sure you want to delete this universal group?",
      onDismiss = { showDeleteDialog = false },
      onConfirmDelete = {
        showDeleteDialog = false
        viewModel.deleteUniversalGroup(groupId, onDeleted = onDeleted)
      },
    )
  }
}
