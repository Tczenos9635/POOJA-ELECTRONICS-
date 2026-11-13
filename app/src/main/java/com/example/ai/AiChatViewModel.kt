package com.example.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.InventoryRepository
import com.example.data.ShopProduct
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiChatViewModel"
private const val DEFAULT_GENERAL_MODEL = "gemini-3.8-flash"
private const val FAST_LITE_MODEL = "gemini-3.5-flash-lite"

enum class AssistantRole(
  val title: String,
  val modelName: String,
  val description: String,
  val systemPrompt: String,
  val starterPrompts: List<String>,
) {
  PARTS_EXPERT(
    title = "Parts & Stock",
    modelName = DEFAULT_GENERAL_MODEL,
    description = "Compatibility, universal groups & shop stock lookup",
    systemPrompt = """
      You are the expert Spare Parts & Compatibility Assistant for Pooja Electronics, a mobile phone repair shop.
      Your job is to help the shop owner and repair technicians quickly answer:
      1) Which spare parts (Tempered Glass, Back Cover, Combo/Display, CC Board, Battery, Middle Frame, Camera Ring) fit a mobile model.
      2) What stock or box location is currently saved in the shop's inventory (provided in the live inventory snapshot below).
      3) Practical advice on checking screen dimensions, camera cutouts, flex connector compatibility, and avoiding duplicate stock via Universal Groups.
      Never claim a part is in stock if it is not listed in the live inventory snapshot; clearly distinguish between what is saved in Pooja Electronics' inventory versus general repair knowledge.
      Be concise, technician-friendly, and clear. Support English, Hindi, and Hinglish naturally if the user writes in them.
    """.trimIndent(),
    starterPrompts = listOf(
      "Which items in my shop are low or out of stock?",
      "Explain how to check combo display & tempered glass fit safely",
      "Summarize my current shop inventory by category",
    ),
  ),
  REPAIR_TECH(
    title = "Repair Tech",
    modelName = DEFAULT_GENERAL_MODEL,
    description = "Hardware troubleshooting for displays, CC boards & batteries",
    systemPrompt = """
      You are a Senior Mobile Hardware Repair Technician assisting Pooja Electronics.
      Help diagnose common smartphone hardware issues clearly and step-by-step:
      - Charging port / CC Board issues, voltage/amp checks, mic & network antenna checks
      - Combo / Display touch, backlight, flex cable, and frame fitting issues
      - Battery swelling, thermal NTC, and connector safety
      - Camera Ring & lens dust/OIS precautions
      Keep answers structured with bullet steps, safety precautions, and practical shop bench checks.
    """.trimIndent(),
    starterPrompts = listOf(
      "Phone not charging after CC board swap — what to check?",
      "Precautions before installing a new Combo Display",
      "Safe steps to replace a swollen lithium battery",
    ),
  ),
  FAST_QUOTE(
    title = "Fast Quote",
    modelName = FAST_LITE_MODEL,
    description = "Quick customer replies & repair estimate drafts",
    systemPrompt = """
      You are a fast Customer Desk Assistant for Pooja Electronics mobile repair shop.
      Draft short, polite, clear customer messages (WhatsApp/SMS or counter replies) for repair estimates, part availability updates, and pickup reminders.
      Keep replies crisp and ready to copy-paste. Match the user's requested language (English, Hindi, Hinglish, or Gujarati).
    """.trimIndent(),
    starterPrompts = listOf(
      "Write a polite WhatsApp update that the phone repair is ready",
      "Draft a message explaining original vs compatible display options",
      "Ask customer to confirm before replacing a Back Cover & Camera Ring",
    ),
  ),
}

data class ChatMessage(
  val id: String = UUID.randomUUID().toString(),
  val isUser: Boolean,
  val text: String,
  val isStreaming: Boolean = false,
)

sealed interface ChatUiState {
  data object Idle : ChatUiState
  data object Loading : ChatUiState
  data class Streaming(val partialText: String) : ChatUiState
  data class Success(val lastResponse: String) : ChatUiState
  data class Error(val message: String) : ChatUiState
}

class AiChatViewModel(private val repository: InventoryRepository) : ViewModel() {

  private val shopProducts: StateFlow<List<ShopProduct>> = repository.shopProducts
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val officialGroups = repository.officialGroups
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val _selectedRole = MutableStateFlow(AssistantRole.PARTS_EXPERT)
  val selectedRole: StateFlow<AssistantRole> = _selectedRole.asStateFlow()

  private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

  private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
  val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

  private var activeChat: Chat? = null
  private var lastFailedPrompt: String? = null

  private val generalModel by lazy {
    buildModelForRole(AssistantRole.PARTS_EXPERT)
  }

  private fun buildModelForRole(role: AssistantRole) =
    Firebase.ai.generativeModel(
      modelName = role.modelName,
      systemInstruction = content {
        text(buildSystemInstructionWithInventory(role, shopProducts.value, officialGroups.value))
      },
    )

  private fun buildSystemInstructionWithInventory(
    role: AssistantRole,
    products: List<ShopProduct>,
    officialCatalog: List<com.example.data.UniversalGroup>,
  ): String {
    val inventorySummary = if (products.isEmpty()) {
      "Live Shop Inventory Snapshot: Currently 0 products saved in My Shop."
    } else {
      val lines = products.take(45).joinToString("\n") { p ->
        val groupPart = p.universalGroupId?.let { " | Group: $it" } ?: ""
        val sizePart = if (p.screenSize.isNotBlank()) " | Screen: ${p.screenSize}" else ""
        val boxPart = if (p.boxNumber.isNotBlank()) " | Box: ${p.boxNumber}" else ""
        val compatPart = if (p.compatibleModels.isNotEmpty()) {
          " | Fits: ${p.compatibleModels.joinToString(", ")}"
        } else {
          ""
        }
        "- [${p.category.displayName}] ${p.mainModel} | Stock: ${p.stockQuantity} (${p.stockStatus.label})$groupPart$sizePart$boxPart$compatPart"
      }
      "Live Shop Inventory Snapshot (${products.size} total items, showing up to 45):\n$lines"
    }
    val officialSummary = if (officialCatalog.isEmpty()) {
      ""
    } else {
      val lines = officialCatalog.take(35).joinToString("\n") { g ->
        val sizePart = if (g.screenSize.isNotBlank()) " | Screen: ${g.screenSize}" else ""
        val compatPart = if (g.compatibleModels.isNotEmpty()) {
          " | Fits: ${g.compatibleModels.joinToString(", ")}"
        } else {
          ""
        }
        "- [${g.category.displayName}] ${g.groupCode}: ${g.mainModel}$sizePart$compatPart"
      }
      "\n\nOfficial Cloud Universal Catalog (${officialCatalog.size} groups):\n$lines"
    }
    return "${role.systemPrompt}\n\n$inventorySummary$officialSummary"
  }

  private fun ensureChatSession(): Chat {
    val existing = activeChat
    if (existing != null) return existing
    val role = _selectedRole.value
    val model = if (role == AssistantRole.PARTS_EXPERT && shopProducts.value.isEmpty()) {
      generalModel
    } else {
      buildModelForRole(role)
    }
    val history = _messages.value
      .filter { !it.isStreaming && it.text.isNotBlank() }
      .map { msg ->
        content(if (msg.isUser) "user" else "model") { text(msg.text) }
      }
    return model.startChat(history = history).also { activeChat = it }
  }

  fun selectRole(role: AssistantRole) {
    if (_selectedRole.value == role) return
    _selectedRole.value = role
    activeChat = null
    _uiState.value = ChatUiState.Idle
  }

  fun startNewChat() {
    activeChat = null
    lastFailedPrompt = null
    _messages.value = emptyList()
    _uiState.value = ChatUiState.Idle
  }

  fun retryLastMessage() {
    val prompt = lastFailedPrompt ?: return
    // Remove the trailing failed user message before resending so it doesn't duplicate
    val current = _messages.value
    if (current.isNotEmpty() && current.last().isUser) {
      _messages.value = current.dropLast(1)
    }
    sendMessage(prompt)
  }

  fun sendMessage(rawPrompt: String) {
    val prompt = rawPrompt.trim()
    if (prompt.isEmpty()) return
    if (_uiState.value is ChatUiState.Loading || _uiState.value is ChatUiState.Streaming) return

    lastFailedPrompt = prompt
    val userMessage = ChatMessage(isUser = true, text = prompt)
    val placeholderId = UUID.randomUUID().toString()
    val assistantPlaceholder = ChatMessage(
      id = placeholderId,
      isUser = false,
      text = "",
      isStreaming = true,
    )

    // Ensure chat session is initialized from prior history BEFORE appending current turn
    val chat = ensureChatSession()
    _messages.value = _messages.value + userMessage + assistantPlaceholder
    _uiState.value = ChatUiState.Loading

    viewModelScope.launch {
      val accumulated = StringBuilder()
      try {
        withTimeout(60_000L) {
          chat.sendMessageStream(prompt).collect { chunk ->
            val chunkText = chunk.text.orEmpty()
            if (chunkText.isNotEmpty()) {
              accumulated.append(chunkText)
              val currentText = accumulated.toString()
              _uiState.value = ChatUiState.Streaming(currentText)
              updateAssistantMessage(placeholderId, currentText, isStreaming = true)
            }
          }
        }
        val finalText = accumulated.toString().trim()
        if (finalText.isNotEmpty()) {
          updateAssistantMessage(placeholderId, finalText, isStreaming = false)
          _uiState.value = ChatUiState.Success(finalText)
          lastFailedPrompt = null
        } else {
          removeMessage(placeholderId)
          _uiState.value = ChatUiState.Error("Assistant returned an empty response. Please try again.")
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Gemini chat request timed out", e)
        removeMessage(placeholderId)
        _uiState.value = ChatUiState.Error("Request timed out after 60 seconds. Please try again.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Firebase AI Logic chat failed", e)
        removeMessage(placeholderId)
        _uiState.value = ChatUiState.Error(
          e.localizedMessage ?: "Failed to connect to Gemini AI. Please check network and try again.",
        )
      }
    }
  }

  private fun updateAssistantMessage(id: String, text: String, isStreaming: Boolean) {
    _messages.value = _messages.value.map { msg ->
      if (msg.id == id) msg.copy(text = text, isStreaming = isStreaming) else msg
    }
  }

  private fun removeMessage(id: String) {
    _messages.value = _messages.value.filterNot { it.id == id }
  }
}

class AiChatViewModelFactory(private val repository: InventoryRepository) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(AiChatViewModel::class.java)) {
      return AiChatViewModel(repository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
