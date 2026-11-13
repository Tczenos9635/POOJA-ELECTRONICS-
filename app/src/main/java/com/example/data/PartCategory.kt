package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.ui.graphics.vector.ImageVector

enum class PartCategory(
  val displayName: String,
  val shortPrefix: String,
  val supportsScreenSize: Boolean,
  val supportsBoxNumber: Boolean,
) {
  TEMPERED_GLASS(
    displayName = "Tempered Glass",
    shortPrefix = "TG",
    supportsScreenSize = true,
    supportsBoxNumber = false,
  ),
  BACK_COVER(
    displayName = "Back Cover",
    shortPrefix = "BC",
    supportsScreenSize = false,
    supportsBoxNumber = true,
  ),
  COMBO_DISPLAY(
    displayName = "Combo / Display",
    shortPrefix = "CD",
    supportsScreenSize = true,
    supportsBoxNumber = false,
  ),
  CC_BOARD(
    displayName = "CC Board",
    shortPrefix = "CC",
    supportsScreenSize = false,
    supportsBoxNumber = false,
  ),
  BATTERY(
    displayName = "Battery",
    shortPrefix = "BT",
    supportsScreenSize = false,
    supportsBoxNumber = false,
  ),
  MIDDLE_FRAME(
    displayName = "Middle Frame",
    shortPrefix = "MF",
    supportsScreenSize = false,
    supportsBoxNumber = false,
  ),
  CAMERA_RING(
    displayName = "Camera Ring",
    shortPrefix = "CR",
    supportsScreenSize = false,
    supportsBoxNumber = false,
  );

  fun icon(): ImageVector =
    when (this) {
      TEMPERED_GLASS -> Icons.Rounded.Layers
      BACK_COVER -> Icons.Rounded.PhoneAndroid
      COMBO_DISPLAY -> Icons.Rounded.Smartphone
      CC_BOARD -> Icons.Rounded.DeveloperBoard
      BATTERY -> Icons.Rounded.BatteryChargingFull
      MIDDLE_FRAME -> Icons.Rounded.ViewInAr
      CAMERA_RING -> Icons.Rounded.CameraAlt
    }

  companion object {
    fun fromName(name: String?): PartCategory =
      entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: TEMPERED_GLASS
  }
}

val SCREEN_SIZE_PRESETS = listOf(
  "6.1\"",
  "6.3\"",
  "6.4\"",
  "6.5\"",
  "6.6\"",
  "6.67\"",
  "6.7\"",
  "6.72\"",
)
