package com.khp.v2ray.handler

import com.khp.v2ray.AppConfig
import com.khp.v2ray.dto.entities.SubscriptionItem
import com.khp.v2ray.util.JsonUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettingsManagerKhpSubscriptionTest {
    private val mainValues get() = MmkvTestStorage.mainValues
    private val subValues get() = MmkvTestStorage.subValues

    @BeforeEach
    fun prepareStorage() {
        MmkvTestStorage.prepareStorage()
    }

    @Test
    fun seedsKhpSubscriptionWhenAbsent() {
        SettingsManager.ensureKhpSubscription()

        val rows = MmkvManager.decodeSubscriptions()
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals(AppConfig.KHP_SUBSCRIPTION_ID, row.guid)
        assertEquals(AppConfig.KHP_SUBSCRIPTION_URL, row.subscription.url)
        assertEquals(AppConfig.KHP_SUBSCRIPTION_REMARKS, row.subscription.remarks)
        assertTrue(row.subscription.enabled)
        assertTrue(row.subscription.autoUpdate)
        assertEquals(1440L, row.subscription.updateInterval)
        assertEquals(listOf(AppConfig.KHP_SUBSCRIPTION_ID), MmkvManager.decodeSubsList())
    }

    @Test
    fun secondSeedDoesNotDuplicate() {
        SettingsManager.ensureKhpSubscription()
        SettingsManager.ensureKhpSubscription()

        val rows = MmkvManager.decodeSubscriptions()
        assertEquals(1, rows.size)
        assertEquals(AppConfig.KHP_SUBSCRIPTION_URL, rows.single().subscription.url)
    }

    @Test
    fun existingSameUrlSubscriptionIsNotDuplicated() {
        subValues["user-added"] = JsonUtil.toJson(
            SubscriptionItem(remarks = "Mine", url = AppConfig.KHP_SUBSCRIPTION_URL)
        )
        mainValues["SUB_IDS"] = """["user-added"]"""

        SettingsManager.ensureKhpSubscription()

        val rows = MmkvManager.decodeSubscriptions()
        assertEquals(1, rows.size)
        assertEquals("user-added", rows.single().guid)
        assertEquals("Mine", rows.single().subscription.remarks)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun initializeHandles() {
            MmkvTestStorage.initializeHandles()
        }
    }
}
