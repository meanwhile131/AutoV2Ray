package org.dpdns.meanwhile131.autov2ray.libXray

import kotlinx.serialization.Serializable

@Serializable
data class convertShareLinksToXrayJson(
    val text: String
) : RequestPayload() {
}