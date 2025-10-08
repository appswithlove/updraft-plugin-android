package com.appswithlove.updraft

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * A plugin for uploading APK/AAB files to updraft.*/
class UpdraftPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val updraftExtension = project.extensions.create("updraft", UpdraftExtension::class.java)

        val android = project.extensions.getByName("android") as com.android.build.gradle.AppExtension
        android.applicationVariants.all { variant ->
            val variantName = variant.name
            val variantNameCapitalized = variantName.replaceFirstChar { it.uppercase() }
            val flavorName = variant.flavorName
            val buildTypeName = variant.buildType.name

            // Providers
            val uploadUrlsProvider = project.providers.provider { updraftExtension.urls }
            val uploadUrls = uploadUrlsProvider.map { urlsMap ->
                val urlsForVariant = urlsMap[variantNameCapitalized]
                when (urlsForVariant) {
                    is Iterable<*> -> urlsForVariant
                    else -> emptyList()
                }
            }

            val gitBranchProvider = project.providers.of(GitBranchValueSource::class.java) {}
            val gitTagsProvider = project.providers.of(GitTagsValueSource::class.java) {}
            val gitCommitProvider = project.providers.of(GitCommitValueSource::class.java) {}
            val gitUrlProvider = project.providers.of(GitUrlValueSource::class.java) {}
            val flavors = variant.productFlavors.map { it.name }
            val releaseNotesProvider = project.providers.provider { getReleaseNotes(project, flavors, updraftExtension) }

            // APK provider
            val apkFileProvider = project.provider {
                val buildDir = project.layout.buildDirectory.asFile.get()
                val apkDir = File(buildDir, "outputs/apk/$flavorName/$buildTypeName")

                if (apkDir.exists() && apkDir.isDirectory) {
                    val apkFiles = apkDir.listFiles { file -> file.name.endsWith(".apk") } ?: emptyArray()
                    if (apkFiles.size > 1) {
                        throw GradleException("More than one .apk file exists in $apkDir: ${apkFiles.map { it.name }}")
                    }
                    apkFiles.firstOrNull()
                } else null
            }

            val apkFile = apkFileProvider.flatMap { file ->
                file.let { project.layout.file(project.provider { it }) }
            }

            // AAB provider
            val aabFileProvider = project.provider {
                val buildDir = project.layout.buildDirectory.asFile.get()
                val bundleDir = File(buildDir, "outputs/bundle/$variantName")

                if (bundleDir.exists() && bundleDir.isDirectory) {
                    val aabFiles = bundleDir.listFiles { file -> file.name.endsWith(".aab") } ?: emptyArray()
                    if (aabFiles.size > 1) {
                        throw GradleException("More than one .aab file exists in $bundleDir: ${aabFiles.map { it.name }}")
                    }
                    aabFiles.firstOrNull()
                } else null
            }

            val aabFile = aabFileProvider.flatMap { file ->
                file.let { project.layout.file(project.provider { it }) }
            }

            // Register APK task
            project.tasks.register("updraft$variantNameCapitalized", UpdraftTask::class.java) { task ->
                task.outputFile.set(apkFile)
                task.urls.set(uploadUrls)
                task.gitBranch.set(gitBranchProvider)
                task.gitTags.set(gitTagsProvider)
                task.gitCommit.set(gitCommitProvider)
                task.gitUrl.set(gitUrlProvider)
                task.releaseNotes.set(releaseNotesProvider)

                task.doFirst {
                    val outputFile = apkFile.getOrNull()?.asFile
                    if (outputFile == null || !outputFile.exists()) {
                        throw GradleException("Could not find a build artifact. (Make sure to run assemble$variantNameCapitalized task first)")
                    }
                }
            }

            // Register AAB task
            project.tasks.register("updraftBundle$variantNameCapitalized", UpdraftTask::class.java) { task ->
                task.outputFile.set(aabFile)
                task.urls.set(uploadUrls)
                task.gitBranch.set(gitBranchProvider)
                task.gitTags.set(gitTagsProvider)
                task.gitCommit.set(gitCommitProvider)
                task.gitUrl.set(gitUrlProvider)
                task.releaseNotes.set(releaseNotesProvider)

                task.doFirst {
                    val outputFile = aabFile.getOrNull()?.asFile
                    if (outputFile == null || !outputFile.exists()) {
                        throw GradleException("Could not find a build artifact. (Make sure to run bundle$variantNameCapitalized task first)")
                    }
                }
            }
        }
    }

    private fun getReleaseNotes(project: Project, flavors: List<String>, updraftExtension: UpdraftExtension): String {
        project.findProperty("releaseNotes")?.let {
            println("Using releaseNotes from gradle property")
            return it.toString()
        }

        updraftExtension.releaseNotes?.let {
            println("Using releaseNotes from updraft extension")
            return it
        }

        val variantFile = if (flavors.isNotEmpty()) {
            File("${project.projectDir}/src/${flavors[0]}/updraft/release-notes.txt")
        } else {
            null
        }

        val mainFile = File("${project.projectDir}/src/main/updraft/release-notes.txt")

        return when {
            variantFile != null && variantFile.exists() -> {
                println("Using releaseNotes from variant file")
                variantFile.readLines().joinToString("\n")
            }
            mainFile.exists() -> {
                println("Using releaseNotes from main file")
                mainFile.readLines().joinToString("\n")
            }
            else -> {
                println("Using releaseNotes last commit")
                try {
                    val output = project.providers.exec {
                        it.commandLine("git", "log", "-1", "--pretty=%B")
                    }.standardOutput.asText.get()
                    output
                } catch (_: Exception) {
                    ""
                }
            }
        }
    }
}
