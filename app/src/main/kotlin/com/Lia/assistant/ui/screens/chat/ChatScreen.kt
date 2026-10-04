package com.Lia.assistant.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import com.Lia.assistant.data.*
import com.Lia.assistant.forge.ForgeController
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.theme.*
import com.Lia.assistant.voice.*
import kotlinx.coroutines.*

@Composable
fun ChatScreen(appState: NovaAppState, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val colors = LocalNovaColors.current
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun send() {
        val text = input.trim(); if (text.isBlank() || loading) return
        input = ""
        if (ForgeIntent.extract(text) != null) {
            messages.add(ChatMessage(System.currentTimeMillis().toString(), text, true))
            messages.add(ChatMessage((System.currentTimeMillis()+1).toString(), "Opening the Forge…", false))
            ForgeController.start(ctx, text); return
        }
        messages.add(ChatMessage(System.currentTimeMillis().toString(), text, true))
        loading = true
        scope.launch(Dispatchers.IO) {
            val key = ApiKeyStore.getKey(ctx)
            val sys = ChatPromptBuilder.build(appState.personality, appState.language, appState.assistantName)
            val hist = messages.map { ChatTurn(it.isUser, it.text) }
            val result = GeminiTextClient.reply(key, sys, hist)
            withContext(Dispatchers.Main) {
                loading = false
                val id = (System.currentTimeMillis()+1).toString()
                when (result) {
                    is ChatReplyResult.Success -> messages.add(ChatMessage(id, result.text, false))
                    is ChatReplyResult.Error -> messages.add(ChatMessage(id, result.message, false, isError = true))
                }
                scope.launch { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NovaTopBar(title = appState.assistantName, onBack = onBack)
        LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            items(messages, key = { it.id }) { NovaMessageBubble(it) }
            if (loading) item { Row(Modifier.padding(16.dp)) { NovaTypingDots() } }
        }
        Row(modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            NovaTextField(value = input, onValueChange = { input = it }, placeholder = "Message Lia…", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = ::send) { Icon(Icons.Default.Send, "Send", tint = colors.accent) }
        }
    }
}
