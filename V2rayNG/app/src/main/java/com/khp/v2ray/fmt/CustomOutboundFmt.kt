package com.khp.v2ray.fmt

import com.khp.v2ray.AppConfig
import com.khp.v2ray.dto.V2rayConfig
import com.khp.v2ray.dto.entities.ProfileItem
import com.khp.v2ray.enums.EConfigType
import com.khp.v2ray.util.JsonUtil

object CustomOutboundFmt : FmtBase() {
    fun parse(str: String): Pair<ProfileItem, String> {
        val config = ProfileItem.create(EConfigType.CUSTOM_OUTBOUND)

        val outbound = JsonUtil.fromJson(str, V2rayConfig.OutboundBean::class.java)

        config.remarks = outbound?.tag ?: System.currentTimeMillis().toString()
        config.server = outbound?.getServerAddress()
        config.serverPort = outbound?.getServerPort()?.toString()

        outbound?.tag = AppConfig.TAG_PROXY
        val rawOutbound = outbound?.let { JsonUtil.toJsonPretty(it) } ?: ""
        return Pair(config, rawOutbound)
    }
}
