package com.appswithlove.updraft

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

abstract class GitBranchValueSource implements ValueSource<String, ValueSourceParameters.None> {
    String obtain() {
        def error = null
        def bashUrl = "git rev-parse --abbrev-ref HEAD"
        def command = bashUrl.execute()
        def outputUrlStream = new StringBuffer()
        command.waitForProcessOutput(outputUrlStream, error)
        if (error == null && outputUrlStream.size() > 0) {
            return outputUrlStream.toString()
        } else {
            return ""
        }
    }
}
