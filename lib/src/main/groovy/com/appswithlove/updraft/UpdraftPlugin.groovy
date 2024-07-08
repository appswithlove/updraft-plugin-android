package com.appswithlove.updraft

import groovy.json.JsonSlurperClassic
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A plugin for uploading apk files to updraft.*/
class UpdraftPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create('updraft', UpdraftExtension.class)
        project.android.applicationVariants.all { variant ->

            //Creates a task for every available build variant / flavor
            project.tasks.create("updraft${variant.name.capitalize()}") {
                // Declares that the project runs after the build, not before.
                // If not stated, it will run at every gradle sync.
                doLast {
                    // APK
                    String filename = variant.outputs[0].outputFile

                    def apkFile = new File(filename)
                    String fileWithoutExt = apkFile.name.take(apkFile.name.lastIndexOf('.'))

                    if (apkFile != null && !apkFile.exists()) {
                        def apkFolder = new File(apkFile.getParent())
                        for (File currentFile in apkFolder.listFiles()) {
                            if (currentFile.getName().startsWith(fileWithoutExt)) {
                                apkFile = currentFile
                            }
                        }
                    }

                    if (apkFile != null && !apkFile.exists()) {
                        throw new GradleException("Could not find a build artifact. (Make sure to run assemble${variant.name.capitalize()} before)")
                    }

                    upload(project, variant, apkFile, filename)
                }
            }

            //Creates a task for every available build variant / flavor
            project.tasks.create("updraftBundle${variant.name.capitalize()}") {
                // Declares that the project runs after the build, not before.
                // If not stated, it will run at every gradle sync.
                doLast {
                    String filename = variant.outputs[0].outputFile

                    def apkFile = new File(filename)
                    String fileWithoutExt = apkFile.name.take(apkFile.name.lastIndexOf('.'))

                    // AAB
                    def basePath = getProject().getProjectDir().getAbsolutePath()
                    def bundlePath = "${basePath}/build/outputs/bundle/${variant.name}/${fileWithoutExt}.aab"

                    def aabFile = new File(bundlePath)
                    if (aabFile != null && !aabFile.exists()) {
                        def aabFolder = new File(aabFile.getParent())
                        for (File currentFile in aabFolder.listFiles()) {
                            println("file $currentFile.path")
                            if (currentFile.getName().startsWith(fileWithoutExt)) {
                                aabFile = currentFile
                            }
                        }
                    }

                    if (aabFile != null && !aabFile.exists()) {
                        throw new GradleException("Could not find a build artifact. (Make sure to run bundle${variant.name.capitalize()} before). \n We tried following location ${bundlePath}")
                    }


                    upload(project, variant, aabFile, bundlePath)
                }
            }
        }
    }

    private void upload(Project project, variant, file, String filename) {
        // Getting git information
        def gitBranch = createCurlParam(executeGitCommand("git rev-parse --abbrev-ref HEAD"), "custom_branch")
        def gitTags = createCurlParam(executeGitCommand("git describe --tags"), "custom_tags")
        def gitCommit = createCurlParam(executeGitCommand("git rev-parse HEAD"), "custom_commit")
        def gitUrl = createCurlParam(executeGitCommand("git config --get remote.origin.url"), "custom_URL")

        def releaseNotes = getReleaseNotes(project, variant)
        println("releaseNotes: $releaseNotes")

        def whatsNew = createCurlParam(releaseNotes, "whats_new")

        def urls = project.updraft.urls[variant.name.capitalize()]

        if (urls == null || urls.isEmpty()) {
            throw new GradleException('There was no url provided for this buildVariant. Please check for typos.')
        }

        if (urls instanceof String) {
            println("--------------------------------------")
            println("Url was not wrapped in array. Doing it for you. :)")
            println("url --> [url]")
            println("--------------------------------------")
            urls = [urls]
        }

        for (String url in urls) {
            //Build and execute of the curl command for Updraft upload

            println("Curl command --> curl -X PUT -F app=@${file} -F build_type=Gradle ${gitBranch} ${gitUrl} ${gitTags} ${gitCommit} ${whatsNew} ${url}")

            new ByteArrayOutputStream().withStream { os ->
                def result = project.exec {
                    executable 'curl'
                    args '-X', 'PUT',
                            '-F', "\"app=@${file}\"",
                            '-F', "\"build_type=Gradle\"",
                            gitBranch,
                            gitUrl,
                            gitTags,
                            gitCommit,
                            whatsNew,
                            url
                    standardOutput os
                }


                def execResponse = new JsonSlurperClassic().parseText(os.toString())

                if (execResponse instanceof HashMap && execResponse.size() > 0) {
                    if (execResponse.containsKey("success") && execResponse["success"] == "ok") {
                        def publicUrl = execResponse["public_link"]
                        ok(publicUrl)
                    } else if (execResponse.containsKey("detail") && execResponse["detail"] == "Not found.") {
                        throw new GradleException('Could not updraft to the given url. Please recheck that.')
                    } else {
                        throw new GradleException(os.toString())
                    }
                } else {
                    println(execResponse)
                    ok(null)
                }
            }
        }
    }

    private static String getReleaseNotes(Project project, variant) {
        if (project.hasProperty('releaseNotes')) {
            println("Using releaseNotes from gradle property")
            return project.property('releaseNotes')
        }

        if (project.updraft.releaseNotes != null) {
            println("Using releaseNotes from updraft extension")
            return project.updraft.releaseNotes
        } else {
            def variantFile = null
            if (variant.productFlavors.size() > 0) {
                variantFile = new File(project.projectDir.toString() + "/src/" + variant.productFlavors[0].name + "/updraft/release-notes.txt")
            }
            def mainFile = new File(project.projectDir.toString() + "/src/main/updraft/release-notes.txt")

            if (variantFile != null && variantFile.exists()) {
                println("Using releaseNotes from variant file")
                return variantFile.readLines().join("\n")
            } else if (mainFile.exists()) {
                println("Using releaseNotes from main file")
                return mainFile.readLines().join("\n")
            } else {
                println("Using releaseNotes last commit")
                return executeGitCommand("git log -1 --pretty=%B")
            }
        }
    }

    private void ok(String publicUrl) {
        println()
        println("--------------------------------------")
        println("Your App was sucessfully updrafted!")
        if (publicUrl != null) {
            println("Get it here -> $publicUrl")
        }
        println("--------------------------------------")
    }

    private static String executeGitCommand(bashUrl) {
        def error = null
        def command = bashUrl.execute()
        def outputUrlStream = new StringBuffer()
        command.waitForProcessOutput(outputUrlStream, error)
        if (error == null && outputUrlStream.size() > 0) {
            outputUrlStream.toString()
        } else {
            ""
        }
    }

    private static String createCurlParam(String text, String name) {
        if (text.isBlank() || text.isEmpty()) {
            ""
        } else {
            "-F \"${name}=${text}\" "
        }
    }
}
