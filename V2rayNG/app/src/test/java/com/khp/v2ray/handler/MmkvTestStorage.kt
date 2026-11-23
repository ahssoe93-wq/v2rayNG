package com.khp.v2ray.handler

import com.tencent.mmkv.MMKV
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Shared MMKV test fixture for handler tests that read through [MmkvManager].
 *
 * [MmkvManager] is a Kotlin `object` whose MMKV handles are `lazy`: whichever
 * test class runs first fixes the handles to ITS mocks for the whole JVM fork,
 * and a later class's own static mock is then ignored, so its tests silently
 * read the first class's stubbed storage. (That is why all six
 * SubscriptionIndexTest tests failed while the SettingsManager tests passed:
 * the Settings class ran first and its last test left SUB_IDS set.)
 *
 * This fixture owns the single set of mocks and backing maps, so every class
 * sees the same storage regardless of execution order. Test classes must call
 * [initializeHandles] once from `@BeforeAll` and [prepareStorage] from
 * `@BeforeEach`, and must not keep their own MMKV mocks for MAIN, SUB, or
 * SETTING.
 */
object MmkvTestStorage {
    val main: MMKV = mock()
    val subs: MMKV = mock()
    val settings: MMKV = mock()

    val mainValues = mutableMapOf<String, String>()
    val subValues = mutableMapOf<String, String>()

    /**
     * Binds MmkvManager's lazy MMKV handles to this fixture's mocks. Call once
     * per test class from `@BeforeAll`; on later calls the handles are already
     * resolved to these same mocks, so the call is a harmless re-read.
     */
    fun initializeHandles() {
        mockStatic(MMKV::class.java).use {
            it.`when`<MMKV> { MMKV.mmkvWithID("MAIN", MMKV.MULTI_PROCESS_MODE) }.thenReturn(main)
            it.`when`<MMKV> { MMKV.mmkvWithID("SUB", MMKV.MULTI_PROCESS_MODE) }.thenReturn(subs)
            it.`when`<MMKV> { MMKV.mmkvWithID("SETTING", MMKV.MULTI_PROCESS_MODE) }.thenReturn(settings)
            MmkvManager.decodeSubscriptions()
            MmkvManager.decodeSettingsString("test-initialize")
        }
    }

    /** Clears both backing maps and re-stubs the mocks from them. Call from `@BeforeEach`. */
    fun prepareStorage() {
        mainValues.clear()
        subValues.clear()
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
}
