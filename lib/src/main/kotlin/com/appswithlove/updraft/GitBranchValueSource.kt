package com.appswithlove.updraft

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.ByteArrayOutputStream

abstract class GitBranchValueSource : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String {
        return try {
            val process = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
                .redirectErrorStream(true)
                .start()

            val outputStream = ByteArrayOutputStream()
            process.inputStream.copyTo(outputStream)
            process.waitFor()

            val result = outputStream.toString().trim()
            result.ifEmpty { "" }
        } catch (_: Exception) {
            ""
        }
    }
}
