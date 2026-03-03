package com.appswithlove.updraft

import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class UpdraftPluginReleaseNotesTest {

    @TempDir
    lateinit var tempDir: Path

    private val plugin = UpdraftPlugin()

    @Test
    fun releaseNotes_fromGradleProperty_returnsPropertyValue() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.extensions.extraProperties["releaseNotes"] = "property notes"
        val extension = UpdraftExtension()

        val result = plugin.getReleaseNotes(project, emptyList(), extension)

        result shouldBe "property notes"
    }

    @Test
    fun releaseNotes_fromExtension_returnsExtensionValue() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        val extension = UpdraftExtension().apply { releaseNotes = "extension notes" }

        val result = plugin.getReleaseNotes(project, emptyList(), extension)

        result shouldBe "extension notes"
    }

    @Test
    fun releaseNotes_fromVariantFile_returnsFileContent() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        val noteFile = File(tempDir.toFile(), "src/prod/updraft/release-notes.txt")
        noteFile.parentFile.mkdirs()
        noteFile.writeText("variant release notes")
        val extension = UpdraftExtension()

        val result = plugin.getReleaseNotes(project, listOf("prod"), extension)

        result shouldBe "variant release notes"
    }

    @Test
    fun releaseNotes_fromMainFile_returnsFileContent() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        val noteFile = File(tempDir.toFile(), "src/main/updraft/release-notes.txt")
        noteFile.parentFile.mkdirs()
        noteFile.writeText("main release notes")
        val extension = UpdraftExtension()

        val result = plugin.getReleaseNotes(project, emptyList(), extension)

        result shouldBe "main release notes"
    }

    @Test
    fun releaseNotes_variantFileTakesPrecedenceOverMainFile() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        File(tempDir.toFile(), "src/prod/updraft/release-notes.txt")
            .also { it.parentFile.mkdirs() }
            .writeText("variant notes")
        File(tempDir.toFile(), "src/main/updraft/release-notes.txt")
            .also { it.parentFile.mkdirs() }
            .writeText("main notes")
        val extension = UpdraftExtension()

        val result = plugin.getReleaseNotes(project, listOf("prod"), extension)

        result shouldBe "variant notes"
    }

    @Test
    fun releaseNotes_whenVariantFileMissing_usesMainFile() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        File(tempDir.toFile(), "src/main/updraft/release-notes.txt")
            .also { it.parentFile.mkdirs() }
            .writeText("main notes")
        val extension = UpdraftExtension()

        val result = plugin.getReleaseNotes(project, listOf("prod"), extension)

        result shouldBe "main notes"
    }

    @Test
    fun releaseNotes_gradlePropertyTakesPrecedenceOverExtension() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.extensions.extraProperties["releaseNotes"] = "property notes"
        val extension = UpdraftExtension().apply { releaseNotes = "extension notes" }

        val result = plugin.getReleaseNotes(project, emptyList(), extension)

        result shouldBe "property notes"
    }
}
