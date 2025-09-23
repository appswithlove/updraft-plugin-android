package com.appswithlove.updraft

import groovy.json.JsonSlurperClassic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Optional
import org.gradle.process.ExecOperations

import javax.inject.Inject

abstract class UpdraftTask extends DefaultTask {

    @Inject
    abstract ExecOperations getExecOperations()

    @InputFile
    @Optional
    abstract RegularFileProperty getOutputFile()

    @Input
    abstract Property<Boolean> getIsBundle()

    @Input
    abstract Property<String> getBasePath()

    @Input
    abstract Property<String> getCurrentVariantName()

    @Input
    abstract ListProperty<String> getUrls()

    @Input
    abstract Property<String> getGitBranch()

    @Input
    abstract Property<String> getGitTags()

    @Input
    abstract Property<String> getGitCommit()

    @Input
    abstract Property<String> getGitUrl()

    @Input
    abstract Property<String> getReleaseNotes()

    @TaskAction
    void upload() {
        def fileToUpload = outputFile.asFile.get()

        def uploadUrls = urls.get()
        if (uploadUrls == null || uploadUrls.isEmpty()) {
            throw new GradleException('There was no url provided for this buildVariant. Please check for typos.')
        }

        if (uploadUrls instanceof String) {
            println("--------------------------------------")
            println("Url was not wrapped in array. Doing it for you. :)")
            println("url --> [url]")
            println("--------------------------------------")
            uploadUrls = [uploadUrls]
        }

        for (String url in uploadUrls) {
            List<String> curlArgs = [
                    '-X', 'PUT',
                    '-F', "app=@${fileToUpload}",
                    '-F', "build_type=Gradle",
                    createCurlParam(gitBranch.get(), "custom_branch"),
                    createCurlParam(gitUrl.get(), "custom_URL"),
                    createCurlParam(gitTags.get(), "custom_tags"),
                    createCurlParam(gitCommit.get(), "custom_commit"),
                    createCurlParam(releaseNotes.get(), "whats_new"),
                    url
            ].findAll { it != '' }

            new ByteArrayOutputStream().withStream { os ->
                getExecOperations().exec {
                    executable 'curl'
                    args curlArgs
                    standardOutput os
                }

                def execResponse = new JsonSlurperClassic().parseText(os.toString())

                if (execResponse instanceof HashMap && execResponse.size() > 0) {
                    if (execResponse.containsKey("success") && execResponse["success"] == "ok") {
                        def publicUrl = execResponse["public_link"]
                        println()
                        println("--------------------------------------")
                        println("Your App was successfully updrafted!")
                        if (publicUrl != null) {
                            println("Get it here -> $publicUrl")
                        }
                        println("--------------------------------------")
                    } else if (execResponse.containsKey("detail") && execResponse["detail"] == "Not found.") {
                        throw new GradleException('Could not updraft to the given url. Please recheck that.')
                    } else {
                        throw new GradleException(os.toString())
                    }
                } else {
                    println(execResponse)
                    println()
                    println("--------------------------------------")
                    println("Your App was successfully updrafted!")
                    println("--------------------------------------")
                }
            }
        }
    }

    private static String createCurlParam(String text, String name) {
        if (text == null || text.isBlank()) {
            ""
        } else {
            "-F ${name}=${text}"
        }
    }
}
