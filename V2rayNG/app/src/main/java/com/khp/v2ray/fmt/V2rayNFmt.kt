package com.khp.v2ray.fmt

import com.khp.v2ray.AppConfig
import com.khp.v2ray.dto.V2rayNShareItem
import com.khp.v2ray.dto.entities.ProfileItem
import com.khp.v2ray.extension.isNotNullEmpty
import com.khp.v2ray.util.JsonUtil
import com.khp.v2ray.util.LogUtil
import com.khp.v2ray.util.Utils

object V2rayNFmt : FmtBase() {
    fun parse(list: List<String>, subId: String): List<ProfileItem> {
        val idMap = mutableMapOf<String, V2rayNShareItem>()
        list.mapNotNull(::parseShareItem).forEach { item ->
            idMap.putIfAbsent(item.IndexId.orEmpty(), item)
        }

        return idMap.values.map { item ->
            item.toProfileItem().apply {
                val proto = item.ProtoExtraObj
                policyGroupSubscriptionId = if (proto?.SubChildItems == "self") {
                    subId
                } else {
                    null
                }
                proto?.ChildItems?.takeIf { it.isNotNullEmpty() }?.let { ids ->
                    val remarks = ids.split(",")
                        .mapNotNull { idMap[it]?.Remarks }
                        .filter { it.isNotNullEmpty() }

                    if (remarks.isNotEmpty()) {
                        when (item.ConfigType) {
                            101 -> policyGroupFilter = remarks.joinToString("|", "^(", ")$") { Regex.escape(it) }
                            102 -> proxyChainProfileList = remarks
                        }
                    }
                }
            }
        }
    }

    private fun parseShareItem(str: String): V2rayNShareItem? = try {
        JsonUtil.fromJson(Utils.decode(str.substringAfterLast('/')), V2rayNShareItem::class.java)
    } catch (e: Exception) {
        LogUtil.e(AppConfig.TAG, "Failed to parse V2rayN share item", e)
        null
    }
}