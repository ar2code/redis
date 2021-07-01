package ru.ar2code.redis.core.android

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import ru.ar2code.redis.core.android.test.TestSavedStateStoreProvider
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class SavedStateStoreProviderTest {

    private val testData = mapOf("1" to "1", "2" to "2")

    /*
     Внутрь android bundle положим ключ, который не имеет отношения к нашему провайдеру.
     Наши операции не должны испортить этот ключ, а также не должны копировать его себе.
     */
    private val keyInsideBundle = "keep_it"

    private val random = Random(System.currentTimeMillis())

    private fun getStoreProviderWithData(): SavedStateStoreProvider {
        val storeProvider = TestSavedStateStoreProvider()

        testData.forEach {
            storeProvider.getSavedStateStore().set(it.key, it.value)
        }

        return storeProvider
    }

    private fun getBundleWithStoredData(): Bundle {
        val store = getStoreProviderWithData()
        val bundle = Bundle()

        bundle.putString(keyInsideBundle, keyInsideBundle)

        store.copyStateProviderToBundle(bundle)
        return bundle
    }

    @Test
    fun testCopyStateProviderToBundle() {

        val storeProvider = getStoreProviderWithData()

        val outBundle = Bundle()

        storeProvider.copyStateProviderToBundle(outBundle)

        Truth.assertThat(outBundle.size()).isEqualTo(testData.size)

        testData.forEach {
            val outKey = "${TestSavedStateStoreProvider.KEY_PREFIX}${it.key}"

            val outValue = outBundle.get(outKey)

            Truth.assertThat(outValue).isEqualTo(it.value)
        }
    }

    @Test
    fun testSeveralTimesCopyStateProviderToBundle() {

        val storeProvider = getStoreProviderWithData()

        val outBundle = Bundle()

        storeProvider.copyStateProviderToBundle(outBundle)
        storeProvider.copyStateProviderToBundle(outBundle)
        storeProvider.copyStateProviderToBundle(outBundle)

        /*
        Ключи не должны размножаться, значения должно просто перезаписываться
         */
        Truth.assertThat(outBundle.size()).isEqualTo(testData.size)

        testData.forEach {
            val outKey = "${TestSavedStateStoreProvider.KEY_PREFIX}${it.key}"

            val outValue = outBundle.get(outKey)

            Truth.assertThat(outValue).isEqualTo(it.value)
        }
    }

    @Test
    fun testCopyBundleToStateProvider() {
        val bundleWithData = getBundleWithStoredData()

        val storeProvider = TestSavedStateStoreProvider()

        storeProvider.copyBundleToStateProvider(bundleWithData)

        Truth.assertThat(storeProvider.getSavedStateStore().keys().size).isEqualTo(testData.size)

        testData.forEach {
            val storedValue = storeProvider.getSavedStateStore().get<String>(it.key)
            Truth.assertThat(storedValue).isEqualTo(it.value)
        }
    }

    @Test
    fun testSeveralTimesCopyBundleToStateProvider() {
        val bundleWithData = getBundleWithStoredData()

        val storeProvider = TestSavedStateStoreProvider()

        storeProvider.copyBundleToStateProvider(bundleWithData)
        storeProvider.copyBundleToStateProvider(bundleWithData)
        storeProvider.copyBundleToStateProvider(bundleWithData)

        Truth.assertThat(storeProvider.getSavedStateStore().keys().size).isEqualTo(testData.size)

        /*
        Ключи не должны размножаться, значения должно просто перезаписываться
         */
        testData.forEach {
            val storedValue = storeProvider.getSavedStateStore().get<String>(it.key)
            Truth.assertThat(storedValue).isEqualTo(it.value)
        }
    }

    @Test
    fun testSeveralTimesCopyBetweenBundles() {
        val bundleWithData = getBundleWithStoredData()

        val storeProvider = TestSavedStateStoreProvider()

        repeat(50) {
            val randomOperation = random.nextBoolean()

            if (randomOperation) {
                storeProvider.copyBundleToStateProvider(bundleWithData)
            } else {
                storeProvider.copyStateProviderToBundle(bundleWithData)
            }
        }

        Truth.assertThat(storeProvider.getSavedStateStore().keys().size).isEqualTo(testData.size)
        Truth.assertThat(bundleWithData.size()).isEqualTo(testData.size + 1) //+ ключ keep it

        /*
        Проверяем, что в storeProvider верные ключи без префикса
         */
        testData.forEach {
            val storedValue = storeProvider.getSavedStateStore().get<String>(it.key)
            Truth.assertThat(storedValue).isEqualTo(it.value)
        }

        /*
        Проверяем, что в bundle верные ключи с префиксом
        */
        testData.forEach {
            val outKey = "${TestSavedStateStoreProvider.KEY_PREFIX}${it.key}"

            val outValue = bundleWithData.get(outKey)

            Truth.assertThat(outValue).isEqualTo(it.value)
        }

        Truth.assertThat(bundleWithData.get(keyInsideBundle)).isEqualTo(keyInsideBundle)
    }
}