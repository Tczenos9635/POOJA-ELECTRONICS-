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
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.BrandParser
import com.example.data.PartCategory
import com.example.data.SCREEN_SIZE_PRESETS
import com.example.ui.ShopViewModel
import com.example.ui.components.CategoryCardChip
import com.example.ui.components.RemovableModelChip

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditProductScreen(
  productId: Long?,
  initialCategory: PartCategory = PartCategory.TEMPERED_GLASS,
  viewModel: ShopViewModel,
  onBack: () -> Unit,
  onSaved: (Long) -> Unit,
) {
  val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
  val universalGroups by viewModel.allUniversalGroups.collectAsStateWithLifecycle()
  val existingProduct = remember(productId, allProducts) {
    productId?.let { id -> allProducts.firstOrNull { it.id == id } }
  }
  val isEditing = productId != null && productId > 0L

  var initialized by rememberSaveable(productId) { mutableStateOf(false) }
  var selectedCategoryName by rememberSaveable(productId) { mutableStateOf(initialCategory.name) }
  var productName by rememberSaveable(productId) { mutableStateOf("") }
  var mainModel by rememberSaveable(productId) { mutableStateOf("") }
  var mainModelError by rememberSaveable(productId) { mutableStateOf<String?>(null) }
  var brand by rememberSaveable(productId) { mutableStateOf("") }
  var screenSize by rememberSaveable(productId) { mutableStateOf("") }
  var boxNumber by rememberSaveable(productId) { mutableStateOf("") }
  var stockText by rememberSaveable(productId) { mutableStateOf("1") }
  var stockError by rememberSaveable(productId) { mutableStateOf<String?>(null) }
  var minStockText by rememberSaveable(productId) { mutableStateOf("3") }
  var selectedGroupCode by rememberSaveable(productId) { mutableStateOf<String?>(null) }
  var compatibleInput by rememberSaveable(productId) { mutableStateOf("") }
  val compatibleModels = remember(productId) { mutableStateListOf<String>() }
  var notes by rememberSaveable(productId) { mutableStateOf("") }
  var groupDropdownExpanded by remember { mutableStateOf(false) }

  LaunchedEffect(existingProduct?.id) {
    if (existingProduct != null && !initialized) {
      selectedCategoryName = existingProduct.category.name
      productName = existingProduct.productName
      mainModel = existingProduct.mainModel
      brand = existingProduct.brand
      screenSize = existingProduct.screenSize
      boxNumber = existingProduct.boxNumber
      stockText = existingProduct.stockQuantity.toString()
      minStockText = existingProduct.minimumStock.toString()
      selectedGroupCode = existingProduct.universalGroupId
      compatibleModels.clear()
      compatibleModels.addAll(existingProduct.compatibleModels)
      notes = existingProduct.notes
      initialized = true
    }
  }

  val selectedCategory = PartCategory.fromName(selectedCategoryName)
  val categoryGroups = universalGroups.filter { it.category == selectedCategory }

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
          Text(
            text = if (isEditing) "Edit Product" else "Add Product",
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("add_edit_product_back"),
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
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Text(
          text = "Select Category",
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
              testTag = "product_category_${category.name.lowercase()}",
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
            if (categoryGroups.isNotEmpty()) {
              ExposedDropdownMenuBox(
                expanded = groupDropdownExpanded,
                onExpandedChange = { groupDropdownExpanded = !groupDropdownExpanded },
              ) {
                OutlinedTextField(
                  value = selectedGroupCode?.let { code ->
                    val g = categoryGroups.firstOrNull { it.groupCode == code }
                    if (g != null) "${g.groupCode} — ${g.mainModel}" else code
                  } ?: "None (Create new or standalone)",
                  onValueChange = {},
                  readOnly = true,
                  label = { Text("Link Existing Universal Group (Optional)") },
                  leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                  expanded = groupDropdownExpanded,
                  onDismissRequest = { groupDropdownExpanded = false },
                ) {
                  DropdownMenuItem(
                    text = { Text("None (Standalone / Auto-create)") },
                    onClick = {
                      selectedGroupCode = null
                      groupDropdownExpanded = false
                    },
                  )
                  categoryGroups.forEach { group ->
                    DropdownMenuItem(
                      text = {
                        Text("${group.groupCode} — ${group.mainModel} (${group.allModels.size} models)")
                      },
                      onClick = {
                        selectedGroupCode = group.groupCode
                        mainModel = group.mainModel
                        brand = group.brand
                        screenSize = group.screenSize
                        compatibleModels.clear()
                        compatibleModels.addAll(group.compatibleModels)
                        groupDropdownExpanded = false
                      },
                    )
                  }
                }
              }
            }

            OutlinedTextField(
              value = mainModel,
              onValueChange = {
                mainModel = it
                mainModelError = null
                if (brand.isBlank() || brand == BrandParser.extractBrand(mainModel)) {
                  brand = BrandParser.extractBrand(it)
                }
              },
              label = { Text("Main Model *") },
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
                .testTag("product_main_model_input"),
            )

            OutlinedTextField(
              value = productName,
              onValueChange = { productName = it },
              label = { Text("Product Name (Optional)") },
              placeholder = { Text("Auto-fills e.g. Oppo F31 ${selectedCategory.displayName}") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("product_name_input"),
            )

            OutlinedTextField(
              value = brand,
              onValueChange = { brand = it },
              label = { Text("Brand") },
              placeholder = { Text("Auto-detected e.g. Oppo") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("product_brand_input"),
            )

            if (selectedCategory == PartCategory.BACK_COVER || boxNumber.isNotBlank()) {
              OutlinedTextField(
                value = boxNumber,
                onValueChange = { boxNumber = it.uppercase() },
                label = {
                  Text(
                    if (selectedCategory == PartCategory.BACK_COVER) {
                      "Box Number (Shop Rack/Box Location)"
                    } else {
                      "Box Number (Optional)"
                    },
                  )
                },
                placeholder = { Text("e.g. B-023") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("product_box_number_input"),
              )
            }

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
                    .testTag("product_screen_size_input"),
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedTextField(
                value = stockText,
                onValueChange = { value ->
                  stockText = value
                  stockError = null
                },
                label = { Text("Stock Quantity *") },
                isError = stockError != null,
                supportingText = stockError?.let { err -> { Text(err) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("product_stock_input"),
              )

              OutlinedTextField(
                value = minStockText,
                onValueChange = { value ->
                  if (value.all { it.isDigit() }) minStockText = value
                },
                label = { Text("Min Stock Alert") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("product_min_stock_input"),
              )
            }

            // Optional Compatible Models
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Compatible Models (Optional)",
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
                    .testTag("product_compatible_input"),
                )
                Button(
                  onClick = { addCompatibleModel() },
                  shape = RoundedCornerShape(12.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("product_add_compat_button"),
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

            if (selectedCategory != PartCategory.BACK_COVER && boxNumber.isBlank()) {
              OutlinedTextField(
                value = boxNumber,
                onValueChange = { boxNumber = it.uppercase() },
                label = { Text("Box Number (Optional)") },
                placeholder = { Text("e.g. B-023") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
              )
            }

            OutlinedTextField(
              value = notes,
              onValueChange = { notes = it },
              label = { Text("Notes (Optional)") },
              placeholder = { Text("Quality, supplier or shelf note…") },
              minLines = 2,
              maxLines = 4,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("product_notes_input"),
            )

            Button(
              onClick = {
                if (mainModel.trim().isEmpty()) {
                  mainModelError = "Please enter the main model"
                  return@Button
                }
                val parsedStock = stockText.trim().toIntOrNull()
                if (parsedStock == null || parsedStock < 0) {
                  stockError = "Enter valid stock (0 or more)"
                  return@Button
                }
                val parsedMin = minStockText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 3
                if (compatibleInput.trim().isNotEmpty()) {
                  addCompatibleModel()
                }
                viewModel.saveProduct(
                  productId = if (isEditing) productId else null,
                  category = selectedCategory,
                  productName = productName,
                  mainModel = mainModel,
                  brand = brand,
                  screenSize = screenSize,
                  stockQuantity = parsedStock,
                  minimumStock = parsedMin,
                  boxNumber = boxNumber,
                  selectedUniversalGroupCode = selectedGroupCode,
                  compatibleModels = compatibleModels.toList(),
                  notes = notes,
                  onSaved = onSaved,
                )
              },
              shape = RoundedCornerShape(14.dp),
              contentPadding = PaddingValues(vertical = 15.dp),
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("save_product_button"),
            ) {
              Text(
                text = if (isEditing) {
                  stringResource(R.string.btn_update_product)
                } else {
                  stringResource(R.string.btn_save_product)
                },
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }
      }
    }
  }
}
