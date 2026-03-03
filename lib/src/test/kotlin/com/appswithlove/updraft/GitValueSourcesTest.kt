package com.appswithlove.updraft

import io.kotest.matchers.shouldNotBe
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class GitValueSourcesTest {

    private val project = ProjectBuilder.builder().build()

    @Test
    fun gitBranch_obtain_returnsNonNullString() {
        val result = project.providers.of(GitBranchValueSource::class.java) {}.get()
        result shouldNotBe null
    }

    @Test
    fun gitCommit_obtain_returnsNonNullString() {
        val result = project.providers.of(GitCommitValueSource::class.java) {}.get()
        result shouldNotBe null
    }

    @Test
    fun gitTags_obtain_returnsNonNullString() {
        val result = project.providers.of(GitTagsValueSource::class.java) {}.get()
        result shouldNotBe null
    }

    @Test
    fun gitUrl_obtain_returnsNonNullString() {
        val result = project.providers.of(GitUrlValueSource::class.java) {}.get()
        result shouldNotBe null
    }
}
