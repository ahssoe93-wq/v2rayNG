package com.khp.v2ray.handler

import com.tencent.mmkv.MMKV
import com.khp.v2ray.AppConfig
import com.khp.v2ray.dto.entities.SubscriptionItem
import com.khp.v2ray.util.JsonUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

class SettingsManagerKhpSubscriptionTest {
    private val mainValues = mutableMapOf<String, String>()
    private val subValues = mutableMapOf<String, String>()

    @BeforeEach
    fun prepareStorage() {
        for ((storage, values) in listOf(main to mainValues, subs to subValues)) {
            reset(storage)
            whenever(storage.decodeString(any())).thenAnswer { values[it.getArgument<String>(0)] }
            whenever(storage.encode(any<String>(), any<String>())).thenAnswer {
                values[it.getArgument(0)] = it.getArgument(1)
                true
            }
            whenever(storage.allKeys()).thenAnswer { values.keys.toTypedArray() }
        }
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
        private val main: MMKV = mock()
        private val subs: MMKV = mock()
        private val settings: MMKV = mock()

        @BeforeAll
        @JvmStatic
        fun initializeHandles() {
            mockStatic(MMKV::class.java).use {
                it.`when`<MMKV> { MMKV.mmkvWithID("MAIN", MMKV.MULTI_PROCESS_MODE) }.thenReturn(main)
                it.`when`<MMKV> { MMKV.mmkvWithID("SUB", MMKV.MULTI_PROCESS_MODE) }.thenReturn(subs)
                it.`when`<MMKV> { MMKV.mmkvWithID("SETTING", MMKV.MULTI_PROCESS_MODE) }.thenReturn(settings)
                MmkvManager.decodeSubscriptions()
                MmkvManager.decodeSettingsString("test-initialize")
            }
        }
    }
}
