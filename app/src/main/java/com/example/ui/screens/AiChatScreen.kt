package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ai.AiChatViewModel
import com.example.ai.AssistantRole
import com.example.ai.ChatMessage
import com.example.ai.ChatUiState
import com.example.ui.theme.StockOutBg
import com.example.ui.theme.StockOutBorder
import com.example.ui.theme.StockOutText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
  viewModel: AiChatViewModel,
  onBack: () -> Unit,
) {
  val selectedRole by viewModel.selectedRole.collectAsStateWithLifecycle()
  val messages by viewModel.messages.collectAsStateWithLifecycle()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  var draft by rememberSaveable { mutableStateOf("") }
  val listState = rememberLazyListState()
  val isBusy = uiState is ChatUiState.Loading || uiState is ChatUiState.Streaming

  LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  fun submitMessage() {
    val text = draft.trim()
    if (text.isNotEmpty() && !isBusy) {
      draft = ""
      viewModel.sendMessage(text)
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = stringResource(R.string.title_ai_assistant),
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = "${stringResource(R.string.subtitle_ai_assistant)} • ${selectedRole.title}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("ai_chat_back"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
            )
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.startNewChat() },
            enabled = messages.isNotEmpty() && !isBusy,
            modifier = Modifier.testTag("ai_new_chat_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.RestartAlt,
              contentDescription = stringResource(R.string.cd_new_chat),
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
      )
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .imePadding(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = { Text(stringResource(R.string.hint_chat_message)) },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
              capitalization = KeyboardCapitalization.Sentences,
              imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { submitMessage() }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.background,
              unfocusedContainerColor = MaterialTheme.colorScheme.background,
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("ai_chat_input"),
          )

          Surface(
            onClick = { submitMessage() },
            enabled = draft.isNotBlank() && !isBusy,
            shape = CircleShape,
            color = if (draft.isNotBlank() && !isBusy) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier
              .size(50.dp)
              .testTag("ai_chat_send_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              if (isBusy) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.primary,
                )
              } else {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.Send,
                  contentDescription = stringResource(R.string.cd_send_message),
                  tint = if (draft.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      // Role selector chips
      Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(AssistantRole.entries) { role ->
            val selected = role == selectedRole
            FilterChip(
              selected = selected,
              onClick = { viewModel.selectRole(role) },
              label = { Text(role.title) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.primary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
              ),
              modifier = Modifier.testTag("ai_role_${role.name.lowercase()}"),
            )
          }
        }
      }

      if (uiState is ChatUiState.Error) {
        val errMsg = (uiState as ChatUiState.Error).message
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("ai_chat_error_banner"),
          shape = RoundedCornerShape(14.dp),
          color = StockOutBg,
          border = BorderStroke(1.dp, StockOutBorder),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.ErrorOutline,
              contentDescription = null,
              tint = StockOutText,
            )
            Text(
              text = errMsg,
              style = MaterialTheme.typography.bodySmall,
              color = StockOutText,
              modifier = Modifier.weight(1f),
            )
            TextButton(
              onClick = { viewModel.retryLastMessage() },
              modifier = Modifier.testTag("ai_chat_retry_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Retry")
            }
          }
        }
      }

      if (messages.isEmpty()) {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              shadowElevation = 2.dp,
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.AutoAwesome,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                  Column {
                    Text(
                      text = selectedRole.title,
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = selectedRole.description,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                Text(
                  text = "Ask anything about spare part compatibility, your Pooja Electronics stock, hardware repair checks, or customer quotes.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          item {
            Text(
              text = "Quick Prompts",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }

          items(selectedRole.starterPrompts) { prompt ->
            Surface(
              onClick = { viewModel.sendMessage(prompt) },
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize(),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = prompt,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.Send,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        }
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .testTag("ai_chat_thread"),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(messages, key = { it.id }) { message ->
            ChatBubble(message = message)
          }
        }
      }
    }
  }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
  ) {
    Surface(
      shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (message.isUser) 18.dp else 4.dp,
        bottomEnd = if (message.isUser) 4.dp else 18.dp,
      ),
      color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
      border = if (message.isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      shadowElevation = if (message.isUser) 1.dp else 2.dp,
      modifier = Modifier
        .widthIn(max = 310.dp)
        .testTag(if (message.isUser) "chat_user_bubble" else "chat_ai_bubble"),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (!message.isUser) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(13.dp),
            )
            Text(
              text = "Parts Assistant",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        if (message.text.isBlank() && message.isStreaming) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(14.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Thinking…",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}
