package org.dpdns.meanwhile131.autov2ray.libXray

import kotlinx.serialization.Serializable

@Serializable
data class Inbound(
    val protocol: String,
    val settings: TunSettings,
    val tag: String
)

@Serializable
data class TunSettings(
    val name: String,
)