package org.dpdns.meanwhile131.autov2ray.libXray

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class runXray (
    val configJSON: String
)
