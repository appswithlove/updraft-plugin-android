package com.appswithlove.updraft

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CreateCurlParamTest {

    @Test
    fun createCurlParam_withNullText_returnsEmpty() {
        UpdraftTask.createCurlParam(null, "custom_branch") shouldBe ""
    }

    @Test
    fun createCurlParam_withBlankText_returnsEmpty() {
        UpdraftTask.createCurlParam("   ", "custom_branch") shouldBe ""
    }

    @Test
    fun createCurlParam_withValue_returnsFormattedParam() {
        UpdraftTask.createCurlParam("main", "custom_branch") shouldBe "-F custom_branch=main"
    }
}
