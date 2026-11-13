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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ShopProduct
import com.example.ui.ShopViewModel
import com.example.ui.components.BoxNumberBadge
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.OfficialBadge
import com.example.ui.components.ScreenSizeBadge
import com.example.ui.components.StockStatusBadge
import com.example.ui.components.StockStepper
import com.example.ui.components.UniversalGroupBadge
import com.example.ui.theme.StockOutText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
  productId: Long,
  viewModel: ShopViewModel,
  onBack: () -> Unit,
  onEditProduct: (Long) -> Unit,
  onEditUniversal: (ShopProduct) -> Unit,
) {
  val productFlow = remember(productId) { viewModel.observeProduct(productId) }
  val product by productFlow.collectAsStateWithLifecycle(initialValue = null)
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

  var showDeleteDialog by remember { mutableStateOf(false) }
  var showAdjustStockDialog by remember { mutableStateOf(false) }
  var stockInput by remember { mutableStateOf("") }
  var stockError by remember { mutableStateOf<String?>(null) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = product?.category?.displayName ?: "Part Details",
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("product_detail_back"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
            )
          }
        },
        actions = {
          if (product != null) {
            IconButton(
              onClick = { onEditProduct(productId) },
              modifier = Modifier.testTag("product_detail_top_edit"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = stringResource(R.string.action_edit),
                tint = MaterialTheme.colorScheme.primary,
              )
            }
            IconButton(
              onClick = { showDeleteDialog = true },
              modifier = Modifier.testTag("product_detail_top_delete"),
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
    val current = product
    if (current == null) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "Product not found",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onBack) {
          Text("Go Back")
        }
      }
      return@Scaffold
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Main Header Card
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          shadowElevation = 2.dp,
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = current.category.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = current.category.displayName.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
              StockStatusBadge(status = current.stockStatus, stockQuantity = current.stockQuantity)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = current.mainModel,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              if (current.productName.isNotBlank() && !current.productName.equals(current.mainModel, ignoreCase = true)) {
                Text(
                  text = current.productName,
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              if (current.isOfficialLinked) {
                OfficialBadge(label = "Official Cloud Sync")
              }
              if (!current.universalGroupId.isNullOrBlank()) {
                UniversalGroupBadge(groupCode = "Universal Group: ${current.universalGroupId}")
              }
              if (current.screenSize.isNotBlank()) {
                ScreenSizeBadge(screenSize = "Screen Size: ${current.screenSize}")
              }
              if (current.boxNumber.isNotBlank()) {
                BoxNumberBadge(boxNumber = current.boxNumber)
              }
            }
            if (current.officialReleaseNote.isNotBlank()) {
              Text(
                text = "Official Update: ${current.officialReleaseNote}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }

      // Current Stock Card
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          shadowElevation = 2.dp,
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "Current Stock",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "${current.stockQuantity} pieces (Min alert: ${current.minimumStock})",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              StockStepper(
                quantity = current.stockQuantity,
                onDecrease = { viewModel.adjustStock(current.id, -1) },
                onIncrease = { viewModel.adjustStock(current.id, 1) },
                testTagPrefix = "detail_stock_${current.id}",
              )
            }
          }
        }
      }

      // Compatible Models Card
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          shadowElevation = 2.dp,
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Compatible Models (${current.allModels.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              if (!current.isOfficialLinked || isAdmin) {
                OutlinedButton(
                  onClick = { onEditUniversal(current) },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("detail_edit_universal_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (current.isOfficialLinked) "Official Panel" else stringResource(R.string.action_edit_universal),
                    style = MaterialTheme.typography.labelMedium,
                  )
                }
              }
            }

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              current.allModels.forEachIndexed { index, model ->
                val isMain = index == 0
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (isMain) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                  border = if (isMain) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
                ) {
                  Text(
                    text = if (isMain) "$model (Main)" else model,
                    style = MaterialTheme.typography.labelLarge.copy(
                      fontWeight = if (isMain) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = if (isMain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                  )
                }
              }
            }
          }
        }
      }

      if (current.notes.isNotBlank()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "Notes",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = current.notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Action Buttons Row: Edit, Adjust Stock, Delete
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = { onEditProduct(current.id) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("detail_edit_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.action_edit))
          }

          OutlinedButton(
            onClick = {
              stockInput = current.stockQuantity.toString()
              stockError = null
              showAdjustStockDialog = true
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1.2f)
              .minimumInteractiveComponentSize()
              .testTag("detail_adjust_stock_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Inventory,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.action_adjust_stock), maxLines = 1)
          }

          OutlinedButton(
            onClick = { showDeleteDialog = true },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, StockOutText.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StockOutText),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("detail_delete_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = stringResource(R.string.action_delete),
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }

  if (showAdjustStockDialog && product != null) {
    AlertDialog(
      onDismissRequest = { showAdjustStockDialog = false },
      title = { Text("Adjust Stock", style = MaterialTheme.typography.titleLarge) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Enter new stock quantity for ${product?.mainModel}:",
            style = MaterialTheme.typography.bodyMedium,
          )
          OutlinedTextField(
            value = stockInput,
            onValueChange = { value ->
              stockInput = value
              stockError = null
            },
            label = { Text("Stock Quantity") },
            singleLine = true,
            isError = stockError != null,
            supportingText = stockError?.let { err -> { Text(err) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("adjust_stock_dialog_input"),
          )
        }
      },
      dismissButton = {
        TextButton(onClick = { showAdjustStockDialog = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val parsed = stockInput.trim().toIntOrNull()
            if (parsed == null || parsed < 0) {
              stockError = "Enter a valid non-negative number"
              return@Button
            }
            viewModel.setStock(productId, parsed)
            showAdjustStockDialog = false
          },
          modifier = Modifier.testTag("adjust_stock_dialog_save"),
        ) {
          Text("Save")
        }
      },
    )
  }

  if (showDeleteDialog) {
    DeleteConfirmationDialog(
      onDismiss = { showDeleteDialog = false },
      onConfirmDelete = {
        showDeleteDialog = false
        viewModel.deleteProduct(productId, onDeleted = onBack)
      },
    )
  }
}
