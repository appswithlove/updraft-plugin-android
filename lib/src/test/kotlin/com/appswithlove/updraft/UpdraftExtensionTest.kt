package com.appswithlove.updraft

import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UpdraftExtensionTest {

    private val extension = UpdraftExtension()

    @Test
    fun urls_default_isEmpty() {
        extension.urls.shouldBeEmpty()
    }

    @Test
    fun releaseNotes_default_isNull() {
        extension.releaseNotes shouldBe null
    }
}
