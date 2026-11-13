package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PartCategory
import com.example.data.StockStatus
import com.example.data.UniversalGroup
import com.example.ui.theme.BoxBadgeBg
import com.example.ui.theme.BoxBadgeBorder
import com.example.ui.theme.BoxBadgeText
import com.example.ui.theme.StockLowBg
import com.example.ui.theme.StockLowBorder
import com.example.ui.theme.StockLowText
import com.example.ui.theme.StockNormalBg
import com.example.ui.theme.StockNormalBorder
import com.example.ui.theme.StockNormalText
import com.example.ui.theme.StockOutBg
import com.example.ui.theme.StockOutBorder
import com.example.ui.theme.StockOutText

@Composable
fun ShopBrandHeader(
  title: String,
  subtitle: String = stringResource(R.string.shop_subtitle),
  modifier: Modifier = Modifier,
  trailingContent: @Composable (() -> Unit)? = null,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f),
    ) {
      Image(
        painter = painterResource(id = R.drawable.pooja_shop_logo),
        contentDescription = stringResource(R.string.cd_shop_logo),
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    if (trailingContent != null) {
      Spacer(modifier = Modifier.width(8.dp))
      trailingContent()
    }
  }
}

@Composable
fun AiAssistantPillButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    modifier = modifier
      .minimumInteractiveComponentSize()
      .testTag("open_ai_chat_button"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.AutoAwesome,
        contentDescription = stringResource(R.string.cd_open_ai_chat),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = "Ask AI",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
fun ShopSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  placeholder: String,
  testTag: String,
  modifier: Modifier = Modifier,
  onSearchSubmit: () -> Unit = {},
  onLensClick: (() -> Unit)? = null,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp), clip = false),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Rounded.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Box(
        modifier = Modifier
          .weight(1f)
          .padding(vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
      ) {
        if (query.isEmpty()) {
          Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
          ),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
          modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        )
      }
      if (query.isNotEmpty()) {
        IconButton(
          onClick = { onQueryChange("") },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("${testTag}_clear"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.cd_clear_search),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      if (onLensClick != null) {
        IconButton(
          onClick = onLensClick,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("${testTag}_lens"),
        ) {
          Icon(
            imageVector = Icons.Rounded.CenterFocusStrong,
            contentDescription = stringResource(R.string.cd_visual_lens),
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

@Composable
fun CategoryCardChip(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  count: Int? = null,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
  val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
  val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
  val borderWidth = if (selected) 1.5.dp else 1.dp

  Surface(
    onClick = onClick,
    modifier = modifier
      .minimumInteractiveComponentSize()
      .testTag(testTag),
    shape = RoundedCornerShape(14.dp),
    color = bgColor,
    border = BorderStroke(borderWidth, borderColor),
    shadowElevation = if (selected) 2.dp else 1.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (count != null) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }
    }
  }
}

@Composable
fun CategoryGridCard(
  category: PartCategory,
  count: Int,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
  val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
  val iconBg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
  val iconTint = if (selected) Color.White else MaterialTheme.colorScheme.primary

  Surface(
    onClick = onClick,
    modifier = modifier
      .widthIn(min = 124.dp)
      .testTag("home_category_${category.name.lowercase()}"),
    shape = RoundedCornerShape(16.dp),
    color = bgColor,
    border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
    shadowElevation = 2.dp,
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(iconBg),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = category.icon(),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
          )
        }
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }
      Text(
        text = category.displayName,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun StockStatusBadge(
  status: StockStatus,
  stockQuantity: Int? = null,
  modifier: Modifier = Modifier,
) {
  val (bg, textColor, border) = when (status) {
    StockStatus.NORMAL -> Triple(StockNormalBg, StockNormalText, StockNormalBorder)
    StockStatus.LOW_STOCK -> Triple(StockLowBg, StockLowText, StockLowBorder)
    StockStatus.OUT_OF_STOCK -> Triple(StockOutBg, StockOutText, StockOutBorder)
  }
  val text = when {
    status == StockStatus.OUT_OF_STOCK -> status.label
    status == StockStatus.LOW_STOCK && stockQuantity != null -> "LOW STOCK ($stockQuantity)"
    status == StockStatus.LOW_STOCK -> status.label
    stockQuantity != null -> "$stockQuantity pcs"
    else -> status.label
  }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = bg,
    border = BorderStroke(1.dp, border),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = textColor,
      maxLines = 1,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun UniversalGroupBadge(
  groupCode: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Link,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(13.dp),
      )
      Text(
        text = groupCode,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun BoxNumberBadge(
  boxNumber: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = BoxBadgeBg,
    border = BorderStroke(1.dp, BoxBadgeBorder),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Inventory2,
        contentDescription = null,
        tint = BoxBadgeText,
        modifier = Modifier.size(13.dp),
      )
      Text(
        text = "Box $boxNumber",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = BoxBadgeText,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun ScreenSizeBadge(
  screenSize: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Straighten,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(13.dp),
      )
      Text(
        text = screenSize,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun StockStepper(
  quantity: Int,
  onDecrease: () -> Unit,
  onIncrease: () -> Unit,
  testTagPrefix: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
          .clickable(enabled = quantity > 0, onClick = onDecrease)
          .testTag("${testTagPrefix}_decrease"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.Remove,
          contentDescription = stringResource(R.string.cd_decrease_stock),
          tint = if (quantity > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
          modifier = Modifier.size(18.dp),
        )
      }
      Text(
        text = quantity.toString(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 10.dp),
      )
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
          .clickable(onClick = onIncrease)
          .testTag("${testTagPrefix}_increase"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = stringResource(R.string.cd_increase_stock),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
fun RemovableModelChip(
  modelName: String,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
  ) {
    Row(
      modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = modelName,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      IconButton(
        onClick = onRemove,
        modifier = Modifier
          .size(36.dp)
          .testTag("remove_chip_${modelName.lowercase().replace(" ", "_")}"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = "Remove $modelName",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}

@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  primaryButtonLabel: String,
  onPrimaryAction: () -> Unit,
  primaryButtonTag: String,
  modifier: Modifier = Modifier,
  secondaryButtonLabel: String? = null,
  onSecondaryAction: (() -> Unit)? = null,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    shadowElevation = 1.dp,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp),
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Button(
        onClick = onPrimaryAction,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag(primaryButtonTag),
      ) {
        Text(text = primaryButtonLabel)
      }
      if (secondaryButtonLabel != null && onSecondaryAction != null) {
        OutlinedButton(
          onClick = onSecondaryAction,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text(text = secondaryButtonLabel)
        }
      }
    }
  }
}

@Composable
fun DeleteConfirmationDialog(
  title: String = stringResource(R.string.confirm_delete_title),
  message: String = stringResource(R.string.confirm_delete_message),
  onDismiss: () -> Unit,
  onConfirmDelete: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("dialog_cancel_button"),
      ) {
        Text(stringResource(R.string.action_cancel))
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirmDelete,
        colors = ButtonDefaults.buttonColors(
          containerColor = StockOutText,
          contentColor = Color.White,
        ),
        modifier = Modifier.testTag("dialog_delete_button"),
      ) {
        Text(stringResource(R.string.action_delete))
      }
    },
  )
}

@Composable
fun OfficialBadge(
  modifier: Modifier = Modifier,
  label: String = stringResource(R.string.badge_official),
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = StockNormalBg,
    border = BorderStroke(1.dp, StockNormalBorder),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Verified,
        contentDescription = null,
        tint = StockNormalText,
        modifier = Modifier.size(13.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = StockNormalText,
        maxLines = 1,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackOfficialGroupDialog(
  group: UniversalGroup,
  onDismiss: () -> Unit,
  onTrackInShop: (initialStock: Int, boxNumber: String) -> Unit,
) {
  var initialStock by remember(group.groupCode) { mutableIntStateOf(1) }
  var boxNumber by remember(group.groupCode) { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OfficialBadge()
          UniversalGroupBadge(groupCode = group.groupCode)
        }
        Text(
          text = group.mainModel,
          style = MaterialTheme.typography.titleLarge,
        )
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "${group.category.displayName}${if (group.screenSize.isNotBlank()) " • ${group.screenSize}" else ""}",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        if (group.releaseNote.isNotBlank()) {
          Text(
            text = "Update Note: ${group.releaseNote}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (group.compatibleModels.isNotEmpty()) {
          Text(
            text = "Compatible Models (${group.allModels.size})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            group.allModels.take(12).forEach { model ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
              ) {
                Text(
                  text = model,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
          }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Shop Stock (pcs)",
            style = MaterialTheme.typography.bodyMedium,
          )
          StockStepper(
            quantity = initialStock,
            onDecrease = { if (initialStock > 0) initialStock-- },
            onIncrease = { initialStock++ },
            testTagPrefix = "track_official_stock",
          )
        }
        OutlinedTextField(
          value = boxNumber,
          onValueChange = { boxNumber = it.uppercase() },
          label = { Text("Box Number (Optional)") },
          placeholder = { Text("e.g. B-023") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("track_official_box_input"),
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel))
      }
    },
    confirmButton = {
      Button(
        onClick = { onTrackInShop(initialStock, boxNumber) },
        modifier = Modifier.testTag("confirm_track_official_button"),
      ) {
        Text("Add to My Shop")
      }
    },
  )
}

@Composable
fun AccountMenuDialog(
  email: String?,
  isAdmin: Boolean,
  onDismiss: () -> Unit,
  onOpenAdminPanel: (() -> Unit)?,
  onSignOut: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.AccountCircle,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp),
        )
        Text(
          text = "Account & Cloud Sync",
          style = MaterialTheme.typography.titleLarge,
        )
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = email ?: "Signed in with Google",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            if (isAdmin) {
              OfficialBadge(label = "Verified Official Admin")
            } else {
              Text(
                text = "Auto-receiving Official Universal Parts updates",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        if (isAdmin && onOpenAdminPanel != null) {
          Button(
            onClick = {
              onDismiss()
              onOpenAdminPanel()
            },
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("dialog_open_admin_panel_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.AdminPanelSettings,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.title_admin_panel))
          }
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    },
    confirmButton = {
      OutlinedButton(
        onClick = {
          onDismiss()
          onSignOut()
        },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = StockOutText),
        border = BorderStroke(1.dp, StockOutText.copy(alpha = 0.5f)),
        modifier = Modifier.testTag("account_sign_out_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Logout,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.action_sign_out))
      }
    },
  )
}
