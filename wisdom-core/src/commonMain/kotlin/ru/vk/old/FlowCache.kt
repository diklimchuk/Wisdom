package ru.vk.old

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

// TODO: Rewrite with Tussaud so that state is kept inside the plot and state is updated iside the performer
//@OptIn(ExperimentalAtomicApi::class)
//class FlowCacheElement<Args, Item>(
//    private val flowProvider: (Args) -> Flow<Item>,
//    private val args: Args,
//) : CacheElement<Item> {
//
//    private val isProviding = AtomicReference<Boolean>(false)
//    private val stateFlowReference = AtomicReference<StateFlow<Item>?>(null)
//
//
//    /**
//     * Returns current value in cache or `null`
//     */
//    override fun peek(): Item? = stateFlowReference.load()?.value?.item
//
//    /**
//     * Allows to observe data
//     * Fetches data if necessary
//     * NOTE: [forceUpdateAndObserve] errors will not be propagated to this observable
//     */
//    override fun observe(): Flow<Item> = Observable // Required to be able to handle cache clearing
//        .merge(ensureFirstValue().toObservable(), cache.observe())
//        .distinctUntilChanged()
//
//    /**
//     * Ensures data is loaded
//     */
//    override suspend fun updateIfEmpty() = observe().ignoreElements()
//
//    /**
//     * Reloads data in cache and then starts observing the [updater]
//     * NOTE: Present and all subsequent values will not be returned
//     * NOTE: Don't use for cache of [Single]s. Use [forceUpdateAndGet] instead.
//     */
//    fun forceUpdateAndObserve(): Observable<T> = getStateFlow()
//
//    /**
//     * Reloads data in cache and returns the first value provided by this [updater]
//     * NOTE: Present and all subsequent values will not be returned
//     */
//    fun forceUpdateAndGet(): Single<T> = forceUpdateAndObserve().firstOrError()
//
//    /**
//     * Reloads data in cache
//     */
//    fun forceUpdate(): Completable = forceUpdateAndObserve().ignoreElements()
//
//    /**
//     * Reloads data in cache ONLY if value was added before
//     */
//    fun updateIfNeeded(): Completable {
//        return Single.fromCallable { hasValue() }
//            .filter { it }
//            .flatMapCompletable { forceUpdate() }
//    }
//
//    /**
//     * Updates current value in cache
//     * NOTE: This value may be overwritten later by currently running updaters
//     */
//    fun set(value: T) = cache.set(value)
//
//    /**
//     * Modifies data **only** if it's already present in cache
//     */
//    fun updateIfPresent(updater: (oldValue: T) -> T) = cache.updateIfPresent(updater)
//
//    /**
//     * Modifies data in cache. In case cache is empty, `oldValue` will be null
//     */
//    fun update(updater: (oldValue: T?) -> T) = cache.update(updater)
//
//    /**
//     * Clear current cache value
//     */
//    fun clear() = cache.clear()
//
//    /**
//     * Checks whether cache has a value
//     */
//    fun hasValue(): Boolean = cache.hasValue()
//
//    private fun ensureFirstValue(): Single<T> = cache.get().switchIfEmpty(getStateFlow().firstOrError())
//
//    private fun getStateFlow(): StateFlow<Item> = stateFlowReference.getOrPut { materializeFlow() }
//
//    private suspend fun materializeFlow(): StateFlow<Item> = flowProvider(args)
//        .onCompletion { isProviding.store(false) }
//        .stateIn(CoroutineScope(Dispatchers.Default))
//}