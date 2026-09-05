package org.dpdns.meanwhile131.autov2ray.libXray

import kotlinx.serialization.Serializable

@Serializable
data class Outbound(
    val protocol: String,
    val tag: String
)