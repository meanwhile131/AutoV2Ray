package org.dpdns.meanwhile131.autov2ray.libXray

import kotlinx.serialization.Serializable

@Serializable
data class Rule(
    val inboundTag: Array<String>,
    val balancerTag: String
)


@Serializable
data class Balancer(
    val tag: String,
    val selector: Array<String>,
    val strategy: BalancerStrategy,
    val fallbackTag: String
)

@Serializable
data class BalancerStrategy(
    val type: String
)


@Serializable
data class RoutingSettings(
    val rules: Array<Rule>,
    val balancers: Array<Balancer>
)