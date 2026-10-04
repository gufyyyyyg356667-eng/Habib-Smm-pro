package com.Lia.assistant.action

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.content.Context
import android.provider.Settings

class NovaAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: NovaAccessibilityService? = null

        fun isEnabled(ctx: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                ctx.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(ctx.packageName)
        }
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun clickOnElementByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return findAndClickByText(root, text.lowercase())
    }

    private fun findAndClickByText(node: AccessibilityNodeInfo, text: String): Boolean {
        val nodeText = (node.text?.toString() ?: node.contentDescription?.toString() ?: "").lowercase()
        if (nodeText.contains(text)) {
            val clickable = findNearestClickable(node)
            if (clickable != null) {
                clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickByText(child, text)) return true
        }
        return false
    }

    private fun findNearestClickable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isClickable) return node
        var parent = node.parent
        var depth = 0
        while (parent != null && depth < 5) {
            if (parent.isClickable) return parent
            parent = parent.parent
            depth++
        }
        return null
    }

    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = findFocusedInput(root) ?: return false
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        return true
    }

    private fun findFocusedInput(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused && node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFocusedInput(child)
            if (result != null) return result
        }
        return null
    }

    fun scroll(down: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return findScrollable(root)?.performAction(action) ?: false
    }

    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findScrollable(child)
            if (result != null) return result
        }
        return null
    }

    fun readScreen(maxElements: Int = 40, maxLabelLength: Int = 40): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val lines = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        collectElements(root, lines, seen, maxElements, maxLabelLength)
        return lines
    }

    private fun collectElements(
        node: AccessibilityNodeInfo,
        lines: MutableList<String>,
        seen: MutableSet<String>,
        maxElements: Int,
        maxLabelLength: Int
    ) {
        if (lines.size >= maxElements) return
        val text = (node.text?.toString() ?: node.contentDescription?.toString() ?: "").take(maxLabelLength)
        if (text.isNotBlank()) {
            val type = when {
                node.isEditable -> "[input]"
                node.isClickable -> "[tap]"
                else -> "[text]"
            }
            val line = "$type $text"
            if (seen.add(line)) lines.add(line)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectElements(child, lines, seen, maxElements, maxLabelLength)
        }
    }
}
