package com.Lia.assistant.action

import android.content.Context
import org.json.JSONArray

object LiaToolCatalog {
    fun declarations(ctx: Context): JSONArray = FlavorToolCatalog.declarations(ctx)
}

object ActionExecutor {
    suspend fun execute(ctx: Context, name: String, args: org.json.JSONObject): org.json.JSONObject =
        FlavorActionExecutor.execute(ctx, args, name)

    fun getLiaCapabilityPrompts(ctx: Context): LiaCapabilityPrompts =
        FlavorCapabilityPrompts.instance(ctx)
}
