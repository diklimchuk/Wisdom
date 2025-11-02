package ru.vk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import money.vivid.elmslie.core.config.TussaudConfig
import ru.vk.memory.Memory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryTest {

    @BeforeTest
    fun beforeEach() {
        val testDispatcher = StandardTestDispatcher()
        TussaudConfig.elmDispatcher = testDispatcher
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Peek after put should return value`() {
        val key = "key"
        val memory = Memory()
        memory.put(key, 1)
        assertEquals(1, memory.peek(key))
    }

    @Test
    fun `Peek after putting value with the same key twice should return the latest value`() {
        val key = "key"
        val memory = Memory()
        memory.put(key, 1)
        memory.put(key, 2)
        assertEquals(2, memory.peek(key))
    }

    @Test
    fun `Peek after provider put should return value`() {
        val key = "key"
        val memory = Memory()
        memory.put<Int>(key, { 1 })
        assertEquals(1, memory.peek(key))
    }

    @Test
    fun `Peek after putting value provider with the same key twice should return the latest value`() {
        val key = "key"
        val memory = Memory()
        memory.put<Int>(key, { 1 })
        memory.put<Int>(key, { (it ?: 0) + 1 })
        assertEquals(2, memory.peek(key))
    }

    @Test
    fun `After removing the value peek should return empty key`() {
        val key = "key"
        val memory = Memory()
        memory.put(key, 1)
        memory.remove(key)
        assertEquals(null, memory.peek<Int>(key))
    }

    @Test
    fun `Removing the different key should have no effect`() {
        val key = "key"
        val memory = Memory()
        memory.put(key, 1)
        memory.remove("other key")
        assertEquals(1, memory.peek(key))
    }

    @Test
    fun `Peek after putIfPresent should return null`() {
        val key = "key"
        val memory = Memory()
        memory.putIfPresent<Int>(key, { 1 })
        assertEquals(null, memory.peek<Int>(key))
    }

    @Test
    fun `Peek after putting value if present with the same key twice should return the latest value`() {
        val key = "key"
        val memory = Memory()
        memory.put(key, 1)
        memory.putIfPresent<Int>(key, { it + 2 })
        assertEquals(3, memory.peek(key))
    }

    @Test
    fun `Observe should emit values the are put in the order they were put`() = runTest {
        val key = "key"
        val memory = Memory()
        val values = mutableListOf<Int?>()
        val observeJob = launch {
            memory.observe<Int>(key).toList(values)
        }
        launch {
            // This coroutine is launched to start observing before putting values
            memory.put(key, 1)
            memory.putIfPresent<Int>(key, { it + 2 })
        }
        advanceUntilIdle()
        assertEquals(listOf<Int?>(1, 3), values)
        observeJob.cancel()
    }

    @Test
    fun `Remove triggers observing null value`() = runTest {
        val key = "key"
        val memory = Memory()
        val values = mutableListOf<Int?>()
        val observeJob = launch {
            memory.observe<Int>(key).toList(values)
        }
        launch {
            // This coroutine is launched to start observing before putting values
            memory.put(key, 1)
            memory.remove(key)
            memory.put(key, 2)
        }
        advanceUntilIdle()
        assertEquals(listOf(1, null, 2), values)
        observeJob.cancel()
    }

    @Test
    fun `Contains before putting returns false`() {
        val key = "key"
        val memory = Memory()
        assertFalse(memory.contains(key))
    }

    @Test
    fun `Contains after putting returns true`() {
        val key = "key"
        val memory = Memory()
        memory.put(key, 1)
        assertTrue(memory.contains(key))
    }

    @Test
    fun `Contains after removing returns false`() {
        val key = "key"
        val memory = Memory()
        memory.put(key, 1)
        memory.remove(key)
        assertFalse(memory.contains(key))
    }

    @Test
    fun `of returns a handle for memory that allows to peek put value`() {
        val key = "key"
        val memory = Memory()
        val handle = memory.of<Int>(key, true)
        handle.put(1)
        assertEquals(1, handle.peek())
    }

    @Test
    fun `Clear removes all put values from cache`() {
        val key1 = "key1"
        val key2 = "key2"
        val memory = Memory()
        memory.put(key1, 1)
        memory.put(key2, 2)
        memory.clear()
        assertEquals(null, memory.peek<Int>(key1))
        assertEquals(null, memory.peek<Int>(key2))
    }

    @Test
    fun `Clear doesn't remove persistable value`() {
        val key = "key"
        val memory = Memory()
        val handle = memory.of<Int>(key, true)
        handle.put(1)
        memory.clear()
        assertEquals(1, memory.peek(key))
    }

    @Test
    fun `ClearAll removes even persistable values put to cache`() {
        val key = "key"
        val memory = Memory()
        val handle = memory.of<Int>(key, true)
        handle.put(1)
        memory.clearWithPersistable()
        assertNull(memory.peek(key))
    }

    @Test
    fun `updateIfEmpty keeps the value`() = runTest {
        val key = "key"
        val memory = Memory()
        memory.put(key, 1)
        async { memory.updateIfEmpty(key) }.join()
        assertEquals(1, memory.peek(key))
    }

    @Test
    fun `forceUpdate keeps the value`() = runTest {
        val key = "key"
        val memory = Memory()
        memory.put(key, 1)
        async { memory.forceUpdate(key) }.join()
        assertEquals(1, memory.peek(key))
    }

    @Test
    fun `updateIfPresent keeps the value`() = runTest {
        val key = "key"
        val memory = Memory()
        memory.put(key, 1)
        async { memory.updateIfPresent(key) }.join()
        assertEquals(1, memory.peek(key))
    }

    @Test
    fun `putting persistent value after putting not persistent value updates clearability`() {
        val key = "key"
        val memory = Memory()
        memory.put(key, 1, persistClearing = false)
        memory.put(key, 2, persistClearing = true)
        memory.clear()
        assertEquals(2, memory.peek(key))
    }

    @Test
    fun `putting not persistent value after putting persistent value updates clearability`() {
        val key = "key"
        val memory = Memory()
        memory.put(key, 1, persistClearing = true)
        memory.put(key, 2, persistClearing = false)
        memory.clear()
        assertNull(memory.peek(key))
    }
}