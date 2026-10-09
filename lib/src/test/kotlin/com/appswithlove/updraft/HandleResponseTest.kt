package com.appswithlove.updraft

import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class HandleResponseTest {

    @Test
    fun handleResponse_withSuccessOk_succeeds() {
        assertDoesNotThrow { UpdraftTask.handleResponse("""{"success":"ok","public_link":"https://example.com"}""") }
    }

    @Test
    fun handleResponse_withTaskId_succeeds() {
        assertDoesNotThrow {
            UpdraftTask.handleResponse("""{"task_id":"b0e7","task_upload_description":"You can check the results of the build processing at the link: https://example.com/b0e7/"}""")
        }
    }

    @Test
    fun handleResponse_withNotFound_throws() {
        assertThrows<GradleException> { UpdraftTask.handleResponse("""{"detail":"Not found."}""") }
    }

    @Test
    fun handleResponse_withUnknownResponse_throws() {
        assertThrows<GradleException> { UpdraftTask.handleResponse("""{"error":"invalid app"}""") }
    }
}
