package com.Lia.assistant.action

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object FlavorToolCatalog {
    fun declarations(ctx: Context): JSONArray = JSONArray().apply {
        put(functionDecl("open_app", "Open an app by name", mapOf("app_name" to "The app name"), listOf("app_name")))
        put(functionDecl("call_contact", "Call a contact or phone number", mapOf("contact" to "Contact name or phone number"), listOf("contact")))
        put(functionDecl("message_contact",
            "Prepare a message to send to a contact. Opens the composer so the user taps Send.",
            mapOf(
                "contact" to "Contact name or phone number",
                "message" to "The message text",
                "app" to "Which app: sms or whatsapp"
            ),
            listOf("contact", "message")))
        put(functionDecl("build_website", "Build an animated website for the user", mapOf("prompt" to "What website to build"), listOf("prompt")))
    }
}

object FlavorCapabilityPrompts : LiaCapabilityPrompts {
    fun instance(ctx: Context): LiaCapabilityPrompts = this

    override fun toolInstruction(assistantName: String): String = """
        You can do these things:
        - open_app(app_name): open any installed app by name
        - call_contact(contact): call a contact or phone number
        - message_contact(contact, message, app): prepare a message (user taps Send)
        - build_website(prompt): build an animated website
        
        You CANNOT read or control the screen. You CANNOT post to social apps.
    """.trimIndent()
}

object FlavorActionExecutor {
    suspend fun execute(ctx: Context, args: JSONObject, name: String): JSONObject =
        withContext(Dispatchers.IO) {
            when (name) {
                "open_app" -> {
                    val appName = args.optString("app_name", "")
                    val pkg = AppLauncher.findApp(ctx, appName)
                    if (pkg != null && AppLauncher.launch(ctx, pkg)) {
                        JSONObject().put("result", "opened").put("app", appName)
                    } else {
                        JSONObject().put("result", "not_found").put("app", appName)
                    }
                }
                "call_contact" -> {
                    val contact = args.optString("contact", "")
                    when (val result = ContactResolver.lookup(ctx, contact)) {
                        is ContactResult.Found -> {
                            val hasPermission = ctx.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                            val intent = if (hasPermission) {
                                Intent(Intent.ACTION_CALL, Uri.parse("tel:${result.number}"))
                            } else {
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${result.number}"))
                            }
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ctx.startActivity(intent)
                            JSONObject().put("result", if (hasPermission) "calling" else "dialer_opened_no_permission")
                                .put("contact", result.displayName)
                        }
                        is ContactResult.NoPermission -> JSONObject().put("result", "no_permission")
                        is ContactResult.NotFound -> JSONObject().put("result", "contact_not_found")
                    }
                }
                "message_contact" -> {
                    val contact = args.optString("contact", "")
                    val message = args.optString("message", "")
                    val app = args.optString("app", "sms")
                    when (val result = ContactResolver.lookup(ctx, contact)) {
                        is ContactResult.Found -> {
                            val intent = if (app == "whatsapp") {
                                val whatsappPkg = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull {
                                    ctx.packageManager.getLaunchIntentForPackage(it) != null
                                }
                                if (whatsappPkg != null) {
                                    Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://wa.me/${result.number}?text=${Uri.encode(message)}"))
                                        .setPackage(whatsappPkg)
                                } else {
                                    Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://wa.me/${result.number}?text=${Uri.encode(message)}"))
                                }
                            } else {
                                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${result.number}"))
                                    .putExtra("sms_body", message)
                            }
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ctx.startActivity(intent)
                            JSONObject().put("result", "composer_opened").put("contact", result.displayName)
                        }
                        is ContactResult.NoPermission -> JSONObject().put("result", "no_permission")
                        is ContactResult.NotFound -> JSONObject().put("result", "contact_not_found")
                    }
                }
                else -> JSONObject().put("error", "Unknown tool: $name")
            }
        }
}
