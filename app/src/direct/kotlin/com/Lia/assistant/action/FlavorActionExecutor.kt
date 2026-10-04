package com.Lia.assistant.action

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object FlavorToolCatalog {
    fun declarations(ctx: Context): JSONArray = JSONArray().apply {
        put(functionDecl("open_app", "Open an app by name", mapOf("app_name" to "The app name"), listOf("app_name")))
        put(functionDecl("call_contact", "Call a contact or phone number", mapOf("contact" to "Contact name or phone number"), listOf("contact")))
        put(functionDecl("message_contact",
            "Prepare and send a message to a contact.",
            mapOf("contact" to "Contact name or phone number", "message" to "Message text", "app" to "sms or whatsapp"),
            listOf("contact", "message")))
        put(functionDecl("read_screen", "Read what is currently on screen", emptyMap(), emptyList()))
        put(functionDecl("tap_text", "Tap an element by its visible text", mapOf("text" to "Text of element to tap"), listOf("text")))
        put(functionDecl("type_text", "Type text in the focused field", mapOf("text" to "Text to type"), listOf("text")))
        put(functionDecl("scroll_screen", "Scroll the screen up or down", mapOf("direction" to "up or down"), listOf("direction")))
        put(functionDecl("go_back", "Press back", emptyMap(), emptyList()))
        put(functionDecl("go_home", "Go to home screen", emptyMap(), emptyList()))
        put(functionDecl("build_website", "Build an animated website", mapOf("prompt" to "Description of the website"), listOf("prompt")))
    }
}

object FlavorCapabilityPrompts : LiaCapabilityPrompts {
    fun instance(ctx: Context): LiaCapabilityPrompts = this

    override fun toolInstruction(assistantName: String): String = """
        You can do these things on the phone:
        - open_app(app_name): open any installed app
        - call_contact(contact): call a contact or number
        - message_contact(contact, message, app): send a message via SMS or WhatsApp
        - read_screen(): read what is currently on screen
        - tap_text(text): tap an element by its visible text
        - type_text(text): type text in the focused field
        - scroll_screen(direction): scroll up or down
        - go_back(): press back button
        - go_home(): go to home screen
        - build_website(prompt): build an animated website
        
        Always read_screen first before trying to tap or type.
        Ask before anything irreversible. Report honestly if you cannot verify success.
    """.trimIndent()
}

object FlavorActionExecutor {
    suspend fun execute(ctx: Context, args: JSONObject, name: String): JSONObject =
        withContext(Dispatchers.IO) {
            when (name) {
                "open_app" -> openApp(ctx, args.optString("app_name", ""))
                "call_contact" -> callContact(ctx, args.optString("contact", ""))
                "message_contact" -> messageContact(
                    ctx,
                    args.optString("contact", ""),
                    args.optString("message", ""),
                    args.optString("app", "sms")
                )
                "read_screen" -> readScreen(ctx)
                "tap_text" -> tapText(ctx, args.optString("text", ""))
                "type_text" -> typeText(ctx, args.optString("text", ""))
                "scroll_screen" -> scrollScreen(ctx, args.optString("direction", "down"))
                "go_back" -> goBack(ctx)
                "go_home" -> goHome(ctx)
                else -> JSONObject().put("error", "Unknown tool: $name")
            }
        }

    private suspend fun openApp(ctx: Context, appName: String): JSONObject {
        val pkg = AppLauncher.findApp(ctx, appName)
        return if (pkg != null && AppLauncher.launch(ctx, pkg)) {
            JSONObject().put("result", "opened").put("app", appName)
        } else {
            JSONObject().put("result", "not_found").put("app", appName)
        }
    }

    private fun callContact(ctx: Context, contact: String): JSONObject {
        return when (val result = ContactResolver.lookup(ctx, contact)) {
            is ContactResult.Found -> {
                val hasPerm = ctx.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                val intent = if (hasPerm) {
                    Intent(Intent.ACTION_CALL, Uri.parse("tel:${result.number}"))
                } else {
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${result.number}"))
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
                JSONObject().put("result", if (hasPerm) "calling" else "dialer_opened_no_permission")
            }
            is ContactResult.NoPermission -> JSONObject().put("result", "no_permission")
            is ContactResult.NotFound -> JSONObject().put("result", "contact_not_found")
        }
    }

    private suspend fun messageContact(ctx: Context, contact: String, message: String, app: String): JSONObject {
        return when (val result = ContactResolver.lookup(ctx, contact)) {
            is ContactResult.Found -> {
                val whatsappPkg = listOf("com.whatsapp", "com.whatsapp.w4b")
                    .firstOrNull { ctx.packageManager.getLaunchIntentForPackage(it) != null }

                val intent = if (app == "whatsapp" && whatsappPkg != null) {
                    Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/${result.number}?text=${Uri.encode(message)}"))
                        .setPackage(whatsappPkg)
                } else {
                    Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${result.number}"))
                        .putExtra("sms_body", message)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)

                // Try to auto-tap Send
                for (i in 0 until 12) {
                    delay(400)
                    val acc = NovaAccessibilityService.instance
                    if (acc != null) {
                        if (app == "whatsapp") {
                            val tapped = acc.clickOnElementByText("Continue to chat")
                            if (!tapped) {
                                if (acc.clickOnElementByText("Send")) {
                                    return@withContext JSONObject().put("result", "sent").put("contact", result.displayName)
                                }
                            }
                        } else {
                            if (acc.clickOnElementByText("Send")) {
                                return@withContext JSONObject().put("result", "sent").put("contact", result.displayName)
                            }
                        }
                    }
                }
                JSONObject().put("result", "composer_opened_send_not_found").put("contact", result.displayName)
            }
            is ContactResult.NoPermission -> JSONObject().put("result", "no_permission")
            is ContactResult.NotFound -> JSONObject().put("result", "contact_not_found")
        }
    }

    private fun readScreen(ctx: Context): JSONObject {
        val acc = NovaAccessibilityService.instance
            ?: return JSONObject().put("result", "accessibility_not_enabled")
        val lines = acc.readScreen(maxElements = 40, maxLabelLength = 40)
        return JSONObject().put("result", "ok").put("screen", lines.joinToString("\n"))
    }

    private fun tapText(ctx: Context, text: String): JSONObject {
        val acc = NovaAccessibilityService.instance
            ?: return JSONObject().put("result", "accessibility_not_enabled")
        return if (acc.clickOnElementByText(text)) {
            JSONObject().put("result", "tapped").put("text", text)
        } else {
            JSONObject().put("result", "element_not_found").put("text", text)
        }
    }

    private fun typeText(ctx: Context, text: String): JSONObject {
        val acc = NovaAccessibilityService.instance
            ?: return JSONObject().put("result", "accessibility_not_enabled")
        return if (acc.typeText(text)) {
            JSONObject().put("result", "typed")
        } else {
            JSONObject().put("result", "no_focused_field")
        }
    }

    private fun scrollScreen(ctx: Context, direction: String): JSONObject {
        val acc = NovaAccessibilityService.instance
            ?: return JSONObject().put("result", "accessibility_not_enabled")
        val down = direction.lowercase() != "up"
        return if (acc.scroll(down)) {
            JSONObject().put("result", "scrolled")
        } else {
            JSONObject().put("result", "nothing_scrollable")
        }
    }

    private fun goBack(ctx: Context): JSONObject {
        val acc = NovaAccessibilityService.instance
            ?: return JSONObject().put("result", "accessibility_not_enabled")
        acc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        return JSONObject().put("result", "done")
    }

    private fun goHome(ctx: Context): JSONObject {
        val acc = NovaAccessibilityService.instance
            ?: return JSONObject().put("result", "accessibility_not_enabled")
        acc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
        return JSONObject().put("result", "done")
    }
}
