package org.dpdns.meanwhile131.autov2ray.libXray

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Response(
    val success: Boolean,
    val data: JsonElement,
    val error: String?
)