package org.dpdns.meanwhile131.autov2ray.libXray

import kotlinx.serialization.Serializable

@Serializable
data class Inbound(
    val protocol: String,
    val settings: TunSettings
)

@Serializable
data class TunSettings(
    val name: String,
    val tag: String
)