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
                    /*if (project.updraftExtension.updraftUrl == null) {
                      throw new GradleException('Please define Updraft url in build.gradle.')
                    }*/
                    String filename = variant.outputs[0].outputFile

                    def file = new File(filename)
                    String fileWithoutExt = file.name.take(file.name.lastIndexOf('.'))

                    if (file != null && !file.exists()) {
                        def apkFolder = new File(file.getParent())
                        for (File currentFile in apkFolder.listFiles()) {
                            if (currentFile.getName().startsWith(fileWithoutExt)) {
                                file = currentFile
                            }
                        }
                    }

                    if (file != null && !file.exists()) {
                        throw new GradleException("Could not find a build artifact. (Make sure to run assemble${variant.name.capitalize()} before)")
                    }

                    //Getting git information
                    def gitBranch = executeGitCommand("git rev-parse --abbrev-ref HEAD", "custom_branch")
                    def gitTags = executeGitCommand("git describe --tags", "custom_tags")
                    def gitCommit = executeGitCommand("git rev-parse HEAD", "custom_commit")
                    def gitUrl = executeGitCommand("git config --get remote.origin.url", "custom_URL")
                    def whatsNew = executeGitCommand("git log -1 --pretty=%B", "whats_new")

                    def urls = project.updraft.urls[variant.name.capitalize()]

                    if (urls == null || urls.isEmpty()) {
                        throw new GradleException('There was no url provided for this buildVariant. Please check for typos.')
                    }

                    for (String url in urls) {
                        //Build and execute of the curl command for Updraft upload

                        new ByteArrayOutputStream().withStream { os ->
                            def result = project.exec {
                                executable 'curl'
                                args '-X', 'PUT',
                                        '-F', "app=@${file}",
                                        '-F', "build_type=Gradle",
                                        gitBranch.join(" "),
                                        gitUrl.join(" "),
                                        gitTags.join(" "),
                                        gitCommit.join(" "),
                                        whatsNew.join(" "),
                                        url
                                standardOutput os
                            }

                            def execResponse = new JsonSlurperClassic().parseText(os.toString())

                            if (execResponse instanceof HashMap && execResponse.size() > 0) {
                                if (execResponse.containsKey("success") && execResponse["success"] == "ok") {
                                    ok(variant.name.capitalize(), url, filename)
                                } else if (execResponse.containsKey("detail") && execResponse["detail"] == "Not found.") {
                                    throw new GradleException('Could not updraft to the given url. Please recheck that.')
                                } else {
                                    throw new GradleException(os.toString())
                                }
                            } else {
                                ok(variant.name.capitalize(), url, filename)
                            }
                        }
                    }
                }
            }
        }
    }

    private void ok(String variant, String updraftUrl, String apkPath) {
        println()
        println("--------------------------------------")
        println("Your App was sucessfully updrafted!")
        println("Local APK: $apkPath")
        println("Flavour: $variant")
        println("Url: $updraftUrl")
        println("--------------------------------------")
    }

    private static List<String> executeGitCommand(bashUrl, name) {
        def item = [""]
        def error = null
        def command = bashUrl.execute()
        def outputUrlStream = new StringBuffer()
        command.waitForProcessOutput(outputUrlStream, error)
        if (error == null && outputUrlStream.size() > 0) {
            item = ['-F', "${name}=${outputUrlStream}"]
        }
        item
    }
}
