package com.khp.v2ray.dto

data class OutboundTrafficStat(
    val tag: String,
    val direction: String,
    val value: Long,
)