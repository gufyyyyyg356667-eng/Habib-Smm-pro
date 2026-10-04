package com.Lia.assistant.voice

data class ConversationTurn(val role: Role, val text: String) {
    enum class Role { USER, ASSISTANT }
}

object ConversationMemory {
    private const val MAX_TURNS = 12
    private const val MAX_RECAP_CHARS = 1600
    private val turns = ArrayDeque<ConversationTurn>()

    @Synchronized
    fun add(role: ConversationTurn.Role, text: String) {
        if (text.isBlank()) return
        if (turns.size >= MAX_TURNS) turns.removeFirst()
        turns.addLast(ConversationTurn(role, text))
    }

    @Synchronized
    fun recap(): String? {
        if (turns.isEmpty()) return null
        val sb = StringBuilder()
        for (turn in turns) {
            val line = "${if (turn.role == ConversationTurn.Role.USER) "User" else "You"}: ${turn.text}\n"
            sb.append(line)
        }
        val full = sb.toString()
        return if (full.length > MAX_RECAP_CHARS) full.takeLast(MAX_RECAP_CHARS) else full
    }

    @Synchronized
    fun clear() { turns.clear() }

    @Synchronized
    fun size() = turns.size
}
