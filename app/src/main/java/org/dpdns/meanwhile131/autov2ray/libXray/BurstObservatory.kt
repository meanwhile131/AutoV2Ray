package org.dpdns.meanwhile131.autov2ray.libXray

import kotlinx.serialization.Serializable

@Serializable
data class BurstObservatory (
    val subjectSelector: Array<String>,
    val pingConfig: PingConfig
)

@Serializable
data class PingConfig(
    val interval: String
)
