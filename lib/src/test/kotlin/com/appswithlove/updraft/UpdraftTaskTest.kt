package com.appswithlove.updraft

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class UpdraftTaskTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun upload_withEmptyUrls_throwsGradleException() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        val task = project.tasks.register("testUpload", UpdraftTask::class.java).get()
        val apkFile = tempDir.resolve("app.apk").toFile().also { it.createNewFile() }
        task.outputFile.set(apkFile)
        task.urls.set(emptyList())
        task.gitBranch.set("")
        task.gitTags.set("")
        task.gitCommit.set("")
        task.gitUrl.set("")
        task.releaseNotes.set("")

        assertThrows<GradleException> { task.upload() }
    }
}
