package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ai.LensScanResult
import com.example.ai.LensUiState
import com.example.ai.PartLensViewModel
import com.example.data.ProductSearchResult
import com.example.ui.components.BoxNumberBadge
import com.example.ui.components.OfficialBadge
import com.example.ui.components.ScreenSizeBadge
import com.example.ui.components.StockStatusBadge
import com.example.ui.components.UniversalGroupBadge
import com.example.ui.theme.StockLowBg
import com.example.ui.theme.StockLowBorder
import com.example.ui.theme.StockLowText
import com.example.ui.theme.StockNormalBg
import com.example.ui.theme.StockNormalBorder
import com.example.ui.theme.StockNormalText
import com.example.ui.theme.StockOutBg
import com.example.ui.theme.StockOutBorder
import com.example.ui.theme.StockOutText
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartLensScreen(
  lensViewModel: PartLensViewModel,
  onBack: () -> Unit,
  onSearchInHome: (String) -> Unit,
  onProductClick: (Long) -> Unit,
  onAskAiWithPrompt: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val uiState by lensViewModel.uiState.collectAsStateWithLifecycle()

  var userHint by remember { mutableStateOf("") }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  var permissionWarning by remember { mutableStateOf<String?>(null) }

  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      permissionWarning = null
      lensViewModel.analyzeImageUri(context, uri, userHint)
    }
  }

  val takePictureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture(),
  ) { success ->
    val capturedUri = pendingCameraUri
    if (success && capturedUri != null) {
      permissionWarning = null
      lensViewModel.analyzeImageUri(context, capturedUri, userHint)
    }
  }

  fun launchCameraCapture() {
    val uri = createLensCaptureUri(context)
    pendingCameraUri = uri
    takePictureLauncher.launch(uri)
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      permissionWarning = null
      launchCameraCapture()
    } else {
      permissionWarning = "Camera permission is required for live camera scan. You can still choose a photo from gallery."
    }
  }

  fun onCameraScanClicked() {
    val granted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
    if (granted) {
      launchCameraCapture()
    } else {
      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  fun onGalleryClicked() {
    galleryLauncher.launch(
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = stringResource(R.string.title_part_lens),
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = stringResource(R.string.subtitle_part_lens),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("lens_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
            )
          }
        },
        actions = {
          if (uiState !is LensUiState.Idle) {
            IconButton(
              onClick = { lensViewModel.resetScan() },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("lens_reset_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "New scan",
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (permissionWarning != null) {
        item {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = StockLowBg,
            border = BorderStroke(1.dp, StockLowBorder),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = permissionWarning!!,
              style = MaterialTheme.typography.bodyMedium,
              color = StockLowText,
              modifier = Modifier.padding(14.dp),
            )
          }
        }
      }

      when (val state = uiState) {
        is LensUiState.Idle -> {
          item {
            LensViewfinderPromptCard(
              userHint = userHint,
              onHintChange = { userHint = it },
              onCameraScan = ::onCameraScanClicked,
              onGalleryPick = ::onGalleryClicked,
            )
          }
        }

        is LensUiState.Analyzing -> {
          item {
            LensBitmapPreviewCard(
              bitmap = state.bitmap,
              isAnalyzing = true,
            )
          }
        }

        is LensUiState.Error -> {
          if (state.bitmap != null) {
            item {
              LensBitmapPreviewCard(
                bitmap = state.bitmap,
                isAnalyzing = false,
              )
            }
          }
          item {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = StockOutBg,
              border = BorderStroke(1.dp, StockOutBorder),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = StockOutText,
                  )
                  Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StockOutText,
                  )
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  if (state.bitmap != null) {
                    Button(
                      onClick = { lensViewModel.retryLastScan() },
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier
                        .weight(1f)
                        .testTag("lens_retry_button"),
                    ) {
                      Text("Retry Scan")
                    }
                  }
                  OutlinedButton(
                    onClick = ::onGalleryClicked,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("lens_pick_another_button"),
                  ) {
                    Text(stringResource(R.string.btn_lens_gallery))
                  }
                }
              }
            }
          }
        }

        is LensUiState.Success -> {
          item {
            LensBitmapPreviewCard(
              bitmap = state.bitmap,
              isAnalyzing = false,
            )
          }

          item {
            LensResultSummaryCard(
              result = state.result,
              onSearchInHome = { onSearchInHome(state.result.searchQuery) },
              onCameraScanAgain = ::onCameraScanClicked,
              onPickAnotherPhoto = ::onGalleryClicked,
            )
          }

          item {
            Text(
              text = "Matching Parts in Shop (${state.result.matchedProducts.size})",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }

          if (state.result.matchedProducts.isEmpty()) {
            item {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(18.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = "No exact inventory match found for \"${state.result.searchQuery}\"",
                    style = MaterialTheme.typography.titleSmall,
                  )
                  Text(
                    text = "Try searching with a shorter model name on Home or ask the Parts Assistant for cross-brand alternatives.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                      onClick = { onSearchInHome(state.result.searchQuery) },
                      shape = RoundedCornerShape(12.dp),
                    ) {
                      Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(stringResource(R.string.btn_lens_search_home))
                    }
                    OutlinedButton(
                      onClick = onAskAiWithPrompt,
                      shape = RoundedCornerShape(12.dp),
                    ) {
                      Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Ask AI")
                    }
                  }
                }
              }
            }
          } else {
            items(
              items = state.result.matchedProducts,
              key = { "${it.product.id}_${it.product.universalGroupId.orEmpty()}" },
            ) { match ->
              LensMatchedProductCard(
                match = match,
                onClick = {
                  if (match.product.id > 0L) {
                    onProductClick(match.product.id)
                  } else {
                    onSearchInHome(state.result.searchQuery)
                  }
                },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LensViewfinderPromptCard(
  userHint: String,
  onHintChange: (String) -> Unit,
  onCameraScan: () -> Unit,
  onGalleryPick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    tonalElevation = 2.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      val primaryColor = MaterialTheme.colorScheme.primary
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(190.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
          val stroke = 4.dp.toPx()
          val corner = 26.dp.toPx()
          // Top-left bracket
          drawLine(primaryColor, Offset(0f, 0f), Offset(corner, 0f), stroke, StrokeCap.Round)
          drawLine(primaryColor, Offset(0f, 0f), Offset(0f, corner), stroke, StrokeCap.Round)
          // Top-right bracket
          drawLine(primaryColor, Offset(size.width - corner, 0f), Offset(size.width, 0f), stroke, StrokeCap.Round)
          drawLine(primaryColor, Offset(size.width, 0f), Offset(size.width, corner), stroke, StrokeCap.Round)
          // Bottom-left bracket
          drawLine(primaryColor, Offset(0f, size.height), Offset(corner, size.height), stroke, StrokeCap.Round)
          drawLine(primaryColor, Offset(0f, size.height - corner), Offset(0f, size.height), stroke, StrokeCap.Round)
          // Bottom-right bracket
          drawLine(
            primaryColor,
            Offset(size.width - corner, size.height),
            Offset(size.width, size.height),
            stroke,
            StrokeCap.Round,
          )
          drawLine(
            primaryColor,
            Offset(size.width, size.height - corner),
            Offset(size.width, size.height),
            stroke,
            StrokeCap.Round,
          )
        }
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(horizontal = 24.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.CenterFocusStrong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp),
          )
          Text(
            text = "Visual Model & Part Finder",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "Scan back cameras, tempered glass packs, CC boards, batteries, or box stickers",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      OutlinedTextField(
        value = userHint,
        onValueChange = onHintChange,
        label = { Text("Optional Scan Note") },
        placeholder = { Text("e.g., Find compatible tempered glass or read sticker") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("lens_hint_input"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(
          onClick = onCameraScan,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .testTag("lens_camera_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.btn_lens_camera), maxLines = 1)
        }

        OutlinedButton(
          onClick = onGalleryPick,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .testTag("lens_gallery_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.btn_lens_gallery), maxLines = 1)
        }
      }
    }
  }
}

@Composable
private fun LensBitmapPreviewCard(
  bitmap: Bitmap,
  isAnalyzing: Boolean,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(220.dp),
    ) {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Scanned spare part photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      if (isAnalyzing) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CircularProgressIndicator(color = Color.White)
            Text(
              text = "Identifying model & compatible parts…",
              style = MaterialTheme.typography.titleSmall,
              color = Color.White,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LensResultSummaryCard(
  result: LensScanResult,
  onSearchInHome: () -> Unit,
  onCameraScanAgain: () -> Unit,
  onPickAnotherPhoto: () -> Unit,
) {
  val (confBg, confBorder, confText) = when (result.confidence) {
    "HIGH" -> Triple(StockNormalBg, StockNormalBorder, StockNormalText)
    "LOW" -> Triple(StockOutBg, StockOutBorder, StockOutText)
    else -> Triple(StockLowBg, StockLowBorder, StockLowText)
  }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
    tonalElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("lens_result_card"),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = result.detectedModel,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${result.detectedBrand.ifBlank { "Mobile" }} • ${result.detectedPartType}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = confBg,
          border = BorderStroke(1.dp, confBorder),
        ) {
          Text(
            text = "${result.confidence} MATCH",
            style = MaterialTheme.typography.labelSmall,
            color = confText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = result.detectedCategory.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
        if (result.extractedTextOrCode.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = result.extractedTextOrCode,
                style = MaterialTheme.typography.labelMedium,
              )
            }
          }
        }
      }

      if (result.visualNotes.isNotBlank()) {
        Text(
          text = result.visualNotes,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Button(
        onClick = onSearchInHome,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("lens_apply_search_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Search,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Search \"${result.searchQuery}\" in Home",
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onCameraScanAgain,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Rescan", maxLines = 1)
        }
        OutlinedButton(
          onClick = onPickAnotherPhoto,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("New Photo", maxLines = 1)
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LensMatchedProductCard(
  match: ProductSearchResult,
  onClick: () -> Unit,
) {
  val product = match.product
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("lens_match_card_${product.id}"),
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
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = product.displayTitle,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${product.category.displayName} • ${product.brand}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (product.id > 0L) {
          StockStatusBadge(status = product.stockStatus, stockQuantity = product.stockQuantity)
        } else {
          OfficialBadge()
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (product.isOfficialLinked && product.id > 0L) {
          OfficialBadge()
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

      if (product.compatibleModels.isNotEmpty()) {
        Text(
          text = "Fits: ${product.allModels.joinToString(" • ")}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

private fun createLensCaptureUri(context: Context): Uri {
  val dir = File(context.cacheDir, "lens_captures").apply { mkdirs() }
  val imageFile = File(dir, "lens_${System.currentTimeMillis()}.jpg")
  return FileProvider.getUriForFile(
    context,
    "${context.packageName}.fileprovider",
    imageFile,
  )
}
