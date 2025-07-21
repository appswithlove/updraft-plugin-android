package com.appswithlove.updraft

import groovy.json.JsonSlurperClassic
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * A plugin for uploading apk files to updraft.*/
class UpdraftPlugin implements Plugin<Project> {

    private final ExecOperations execOps

    @Inject
    UpdraftPlugin(ExecOperations execOperations) {
        this.execOps = execOperations
    }

    @Override
    void apply(Project project) {
        project.extensions.create('updraft', UpdraftExtension.class)
        project.android.applicationVariants.all { variant ->

            def plugin = this
            String variantName = variant.name
            String variantNameCapitalized = variantName.capitalize()
            String basePath = getProject().getProjectDir().getAbsolutePath()
            String filename = variant.outputs[0].outputFile

            def urls = project.updraft.urls[variantNameCapitalized]
            def gitBranchProvider = project.providers.of(GitBranchValueSource.class) {}
            def gitTagsProvider = project.providers.of(GitTagsValueSource.class) {}
            def gitCommitProvider = project.providers.of(GitCommitValueSource.class) {}
            def gitUrlProvider = project.providers.of(GitUrlValueSource.class) {}
            String releaseNotes = getReleaseNotes(project, variant)
            String whatsNew = createCurlParam(releaseNotes, "whats_new")

            // Registers a task for every available build variant / flavor
            project.tasks.register("updraft${variantNameCapitalized}") {
                // Declares that the project runs after the build, not before.
                // If not stated, it will run at every gradle sync.

                String gitBranch = createCurlParam(gitBranchProvider.get(), "custom_branch")
                String gitTags = createCurlParam(gitTagsProvider.get(), "custom_tags")
                String gitCommit = createCurlParam(gitCommitProvider.get(), "custom_commit")
                String gitUrl = createCurlParam(gitUrlProvider.get(), "custom_URL")

                doLast {
                    // APK
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
                        throw new GradleException("Could not find a build artifact. (Make sure to run assemble${variantNameCapitalized} before)")
                    }

                    plugin.upload(apkFile, urls, gitBranch, gitTags, gitCommit, gitUrl, whatsNew)
                }
            }

            // Registers a task for every available build variant / flavor
            project.tasks.register("updraftBundle${variantNameCapitalized}") {
                // Declares that the project runs after the build, not before.
                // If not stated, it will run at every gradle sync.

                String gitBranch = createCurlParam(gitBranchProvider.get(), "custom_branch")
                String gitTags = createCurlParam(gitTagsProvider.get(), "custom_tags")
                String gitCommit = createCurlParam(gitCommitProvider.get(), "custom_commit")
                String gitUrl = createCurlParam(gitUrlProvider.get(), "custom_URL")

                doLast {
                    def apkFile = new File(filename)
                    String fileWithoutExt = apkFile.name.take(apkFile.name.lastIndexOf('.'))

                    // AAB
                    def bundlePath =
                            "${basePath}/build/outputs/bundle/${variantName}/${fileWithoutExt}.aab"

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
                        throw new GradleException("Could not find a build artifact. (Make sure to run bundle${variantNameCapitalized} before). \n We tried following location ${bundlePath}")
                    }

                    plugin.upload(aabFile, urls, gitBranch, gitTags, gitCommit, gitUrl, whatsNew)
                }
            }
        }
    }

    private void upload(file, urls, gitBranch, gitTags, gitCommit, gitUrl, whatsNew) {
        if (urls == null || urls.isEmpty()) {
            throw new GradleException(
                    'There was no url provided for this buildVariant. Please check for typos.'
            )
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
            List<String> curlArgs = [
                    '-X', 'PUT',
                    '-F', "app=@${file}",
                    '-F', "build_type=Gradle",
                    gitBranch,
                    gitUrl,
                    gitTags,
                    gitCommit,
                    whatsNew,
                    url
            ].findAll { it != '' } // Filter out empty strings

            new ByteArrayOutputStream().withStream { os ->
                execOps.exec {
                    executable 'curl'
                    args curlArgs
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
                def result = project.providers.exec {
                    commandLine("git", "log", "-1", "--pretty=%B")
                }.standardOutput.asText
                return result.get()
            }
        }
    }

    private static void ok(String publicUrl) {
        println()
        println("--------------------------------------")
        println("Your App was successfully updrafted!")
        if (publicUrl != null) {
            println("Get it here -> $publicUrl")
        }
        println("--------------------------------------")
    }

    private static String createCurlParam(String text, String name) {
        if (text.isBlank() || text.isEmpty()) {
            ""
        } else {
            "-F ${name}=${text} "
        }
    }
}
