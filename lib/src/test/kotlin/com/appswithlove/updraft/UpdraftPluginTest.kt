package com.appswithlove.updraft

import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class UpdraftPluginTest {

    @TempDir
    lateinit var tempDir: Path

    private val plugin = UpdraftPlugin()

    @Test
    fun validateBuildArtifact_withNullFile_throwsGradleException() {
        val exc = assertThrows<GradleException> {
            plugin.validateBuildArtifact(null, "assembleDebug")
        }
        exc.message shouldContain "Could not find a build artifact"
    }

    @Test
    fun validateBuildArtifact_withMissingFile_throwsGradleException() {
        val missing = tempDir.resolve("app-debug.apk").toFile()
        val exc = assertThrows<GradleException> {
            plugin.validateBuildArtifact(missing, "assembleDebug")
        }
        exc.message shouldContain "Could not find a build artifact"
    }

    @Test
    fun validateBuildArtifact_withExistingFile_doesNotThrow() {
        val existing = tempDir.resolve("app-debug.apk").toFile().also { it.createNewFile() }
        plugin.validateBuildArtifact(existing, "assembleDebug")
    }
}
