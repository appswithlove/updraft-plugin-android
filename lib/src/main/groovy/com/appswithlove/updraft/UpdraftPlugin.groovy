package com.appswithlove.updraft

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

            String variantName = variant.name
            String variantNameCapitalized = variantName.capitalize()
            String projectBasePath = getProject().getProjectDir().getAbsolutePath()

            def uploadUrlsProvider = project.providers.provider { project.updraft.urls }
            def uploadUrls = uploadUrlsProvider.map { urlsMap ->
                def urlsForVariant = urlsMap[variantNameCapitalized]
                if (urlsForVariant == null) {
                    return []
                }
                if (urlsForVariant instanceof String) {
                    return [urlsForVariant]
                }
                return urlsForVariant
            }

            def gitBranchProvider = project.providers.of(GitBranchValueSource.class) {}
            def gitTagsProvider = project.providers.of(GitTagsValueSource.class) {}
            def gitCommitProvider = project.providers.of(GitCommitValueSource.class) {}
            def gitUrlProvider = project.providers.of(GitUrlValueSource.class) {}
            def releaseNotesProvider = project.providers.provider { getReleaseNotes(project, variant) }

            def apkFileProvider = project.provider {
                def file = variant.outputs.find { it.outputFile != null && it.outputFile.name.endsWith('.apk') }?.outputFile
                if (file.exists()) {
                    return file
                }
                return null
            }

            def aabFileProvider = project.provider {
                def buildDir = project.layout.buildDirectory.get().asFile
                def bundleDir = new File(buildDir, "outputs/bundle/${variantName}")
                if (bundleDir.exists() && bundleDir.isDirectory()) {
                    def aabFiles = bundleDir.listFiles({ file -> file.name.endsWith(".aab") } as FileFilter)
                    return aabFiles ? aabFiles[0] : null
                }
                return null
            }

            // Registers a task for every available build variant / flavor
            project.tasks.register("updraft${variantNameCapitalized}", UpdraftTask) {

                def apkFile = apkFileProvider.flatMap { file ->
                    file ? project.layout.file(project.provider { file }) : project.objects.fileProperty()
                }

                outputFile.set(apkFile)
                isBundle.set(false)
                basePath.set(projectBasePath)
                currentVariantName.set(variantName)
                urls.set(uploadUrls)
                gitBranch.set(gitBranchProvider)
                gitTags.set(gitTagsProvider)
                gitCommit.set(gitCommitProvider)
                gitUrl.set(gitUrlProvider)
                releaseNotes.set(releaseNotesProvider)

                doFirst {
                    def outputFile = apkFile.getOrNull()?.asFile
                    if (outputFile == null || !outputFile.exists()) {
                        throw new GradleException("Could not find a build artifact. (Make sure to run assemble${variantNameCapitalized} task first)")
                    }
                }
            }

            // Registers a task for every available build variant / flavor
            project.tasks.register("updraftBundle${variantNameCapitalized}", UpdraftTask) { task ->

                def aabFile = aabFileProvider.flatMap { file ->
                    file ? project.layout.file(project.provider { file }) : project.objects.fileProperty()
                }

                outputFile.set(aabFile)
                isBundle.set(true)
                basePath.set(projectBasePath)
                currentVariantName.set(variantName)
                urls.set(uploadUrls)
                gitBranch.set(gitBranchProvider)
                gitTags.set(gitTagsProvider)
                gitCommit.set(gitCommitProvider)
                gitUrl.set(gitUrlProvider)
                releaseNotes.set(releaseNotesProvider)

                doFirst {
                    def outputFile = aabFile.getOrNull()?.asFile
                    if (outputFile == null || !outputFile.exists()) {
                        throw new GradleException("Could not find a build artifact. (Make sure to run bundle${variantNameCapitalized} task first)")
                    }
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
                try {
                    def result = project.providers.exec {
                        commandLine("git", "log", "-1", "--pretty=%B")
                    }.standardOutput.asText
                    return result.get()
                } catch (Exception ignored) {
                    return ""
                }
            }
        }
    }
}
