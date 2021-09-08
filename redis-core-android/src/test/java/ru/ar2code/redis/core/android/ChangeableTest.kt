package ru.ar2code.redis.core.android

import com.google.common.truth.Truth
import org.junit.Test

class ChangeableTest {

    @Test
    fun testShouldBeRendered1() {
        val ch = Changeable("data", 1)
        val chUpdated = Changeable("data", 2)

        val shouldRender = chUpdated.shouldBeRendered(ch)

        Truth.assertThat(shouldRender).isTrue()
    }

    @Test
    fun testShouldBeRendered2() {
        val ch = Changeable("data", 1)
        val chUpdated = Changeable("data", 1)

        val shouldRender = chUpdated.shouldBeRendered(ch)

        Truth.assertThat(shouldRender).isFalse()
    }

    @Test
    fun testShouldBeRendered3() {
        val ch = Changeable("data", 2)
        val chUpdated = Changeable("data", 1)

        val shouldRender = chUpdated.shouldBeRendered(ch)

        Truth.assertThat(shouldRender).isTrue()
    }

    @Test
    fun testShouldBeRendered4() {
        val ch = Changeable("data", 2)
        val chUpdated = Changeable<String>(null, 1)

        val shouldRender = chUpdated.shouldBeRendered(ch)

        Truth.assertThat(shouldRender).isFalse()
    }

    @Test
    fun testGenerateUpperVersion() {
        val ch = Changeable("data", 2)
        val newVersion = ch.generateUpperVersion()

        Truth.assertThat(newVersion).isGreaterThan(ch.version)
    }

    @Test
    fun givenChangeableNotNull_WhenUpdateDataWithUpperVersion_ThenReturnChangeableWithDataAndUpperVersion() {
        val ch = Changeable("data")
        val newData = "new"
        val updated = ch.updateDataWithUpperVersion(newData)

        Truth.assertThat(updated.data).isEqualTo(newData)
        Truth.assertThat(updated.version).isGreaterThan(ch.version)
    }

    @Test
    fun givenChangeableIsNull_WhenUpdateDataWithUpperVersion_ThenReturnChangeableWithDataAndInitVersion() {
        val ch: Changeable<String>? = null
        val newData = "new"
        val updated = ch.updateDataWithUpperVersion(newData)

        Truth.assertThat(updated.data).isEqualTo(newData)
        Truth.assertThat(updated.version).isEqualTo(Changeable.INIT_VERSION)
    }

    @Test
    fun givenChangeableNotNull_WhenUpdateDataWithSameVersion_ThenReturnChangeableWithDataAndSameVersion() {
        val ch = Changeable("data")
        val newData = "new"
        val updated = ch.updateDataWithSameVersion(newData)

        Truth.assertThat(updated.data).isEqualTo(newData)
        Truth.assertThat(updated.version).isEqualTo(ch.version)
    }

    @Test
    fun givenChangeableIsNull_WhenUpdateDataWithSameVersion_ThenReturnChangeableWithDataAndInitVersion() {
        val ch: Changeable<String>? = null
        val newData = "new"
        val updated = ch.updateDataWithSameVersion(newData)

        Truth.assertThat(updated.data).isEqualTo(newData)
        Truth.assertThat(updated.version).isEqualTo(Changeable.INIT_VERSION)
    }

    @Test
    fun givenChangeableNotNull_WhenGetUpperVersion_ThenReturnGreaterVersion() {
        val ch = Changeable("data")
        val updatedVersion = ch.getUpperVersion()

        Truth.assertThat(updatedVersion).isGreaterThan(ch.version)
    }

    @Test
    fun givenChangeableIsNull_WhenGetUpperVersion_ThenReturnInitialVersion() {
        val ch: Changeable<String>? = null
        val updatedVersion = ch.getUpperVersion()

        Truth.assertThat(updatedVersion).isEqualTo(Changeable.INIT_VERSION)
    }

    @Test
    fun givenChangeableNotNull_WhenGetCurrentOrInitVersion_ThenReturnCurrentVersion() {
        val ch = Changeable("data")
        val updatedVersion = ch.getCurrentOrInitVersion()

        Truth.assertThat(updatedVersion).isEqualTo(ch.version)
    }

    @Test
    fun givenChangeableIsNull_WhenGetCurrentOrInitVersion_ThenReturnInitialVersion() {
        val ch: Changeable<String>? = null
        val updatedVersion = ch.getCurrentOrInitVersion()

        Truth.assertThat(updatedVersion).isEqualTo(Changeable.INIT_VERSION)
    }

    @Test
    fun givenChangeableNotNull_WhenOrEmpty_ThenReturnChangeableWithCurrentData() {
        val ch = Changeable("data")
        val emptyData = "empty"

        val updated = ch.orEmpty(emptyData)

        Truth.assertThat(updated.data).isEqualTo(ch.data)
    }

    @Test
    fun givenChangeableIsNull_WhenOrEmpty_ThenReturnChangeableWithEmptyData() {
        val ch: Changeable<String>? = null
        val emptyData = "empty"
        val updated = ch.orEmpty(emptyData)

        Truth.assertThat(updated.data).isEqualTo(emptyData)
    }
}