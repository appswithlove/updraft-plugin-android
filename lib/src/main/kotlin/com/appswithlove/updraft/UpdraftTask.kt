package com.appswithlove.updraft

import groovy.json.JsonSlurperClassic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

abstract class UpdraftTask : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val urls: ListProperty<String>

    @get:Input
    abstract val gitBranch: Property<String>

    @get:Input
    abstract val gitTags: Property<String>

    @get:Input
    abstract val gitCommit: Property<String>

    @get:Input
    abstract val gitUrl: Property<String>

    @get:Input
    abstract val releaseNotes: Property<String>

    @TaskAction
    fun upload() {
        val fileToUpload = outputFile.asFile.get()

        var uploadUrls = urls.get()
        if (uploadUrls.isEmpty()) {
            throw GradleException("There was no url provided for this buildVariant. Please check for typos.")
        }

        if (uploadUrls.size == 1 && uploadUrls[0].isNotBlank() && !uploadUrls[0].startsWith("http")) {
            println("--------------------------------------")
            println("Url was not wrapped in array. Doing it for you. :)")
            println("url --> [url]")
            println("--------------------------------------")
            uploadUrls = listOf(uploadUrls[0])
        }

        uploadUrls.forEach { url ->
            val curlArgs = listOf(
                "-X", "PUT",
                "-F", "app=@$fileToUpload",
                "-F", "build_type=Gradle",
                createCurlParam(gitBranch.get(), "custom_branch"),
                createCurlParam(gitUrl.get(), "custom_URL"),
                createCurlParam(gitTags.get(), "custom_tags"),
                createCurlParam(gitCommit.get(), "custom_commit"),
                createCurlParam(releaseNotes.get(), "whats_new"),
                url
            ).filter { it.isNotBlank() }

            ByteArrayOutputStream().use { os ->
                execOperations.exec {
                    it.executable = "curl"
                    it.args = curlArgs
                    it.standardOutput = os
                }

                val execResponse = JsonSlurperClassic().parseText(os.toString())

                if (execResponse is Map<*, *> && execResponse.isNotEmpty()) {
                    when {
                        execResponse["success"] == "ok" -> {
                            val publicUrl = execResponse["public_link"]
                            println("\n--------------------------------------")
                            println("Your App was successfully updrafted!")
                            if (publicUrl != null) println("Get it here -> $publicUrl")
                            println("--------------------------------------")
                        }

                        execResponse["detail"] == "Not found." -> {
                            throw GradleException("Could not updraft to the given url. Please recheck that.")
                        }

                        else -> {
                            throw GradleException(os.toString())
                        }
                    }
                } else {
                    println(execResponse)
                    println("\n--------------------------------------")
                    println("Your App was successfully updrafted!")
                    println("--------------------------------------")
                }
            }
        }
    }

    companion object {
        private fun createCurlParam(text: String?, name: String): String {
            return if (text.isNullOrBlank()) "" else "-F $name=$text"
        }
    }
}
