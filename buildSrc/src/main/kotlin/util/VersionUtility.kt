package util

import org.gradle.api.Project

object VersionUtility {

    private const val DEFAULT_BRANCH = "local"
    private const val UNKNOWN_COMMIT = "unknown"
    private const val MAIN_BRANCH = "master"
    private const val SHORT_COMMIT_LENGTH = 7

    fun branch(project: Project): String {
        val environmentBranch = System.getenv("GIT_BRANCH")

        if (!environmentBranch.isNullOrBlank()) {
            return sanitize(environmentBranch)
        }

        val branch = execute(
            project,
            "git",
            "branch",
            "--show-current"
        )

        return branch
            .takeIf { it.isNotBlank() }
            ?.let(::sanitize)
            ?: DEFAULT_BRANCH
    }

    fun commitHash(project: Project): String {
        val environmentCommit = System.getenv("GIT_COMMIT")

        if (!environmentCommit.isNullOrBlank()) {
            return environmentCommit.take(SHORT_COMMIT_LENGTH)
        }

        return execute(
            project,
            "git",
            "rev-parse",
            "--short",
            "HEAD"
        ).ifBlank {
            UNKNOWN_COMMIT
        }
    }

    fun version(
        project: Project,
        baseVersion: String
    ): String {
        val branch = branch(project)
        val commit = commitHash(project)

        return buildString {
            append(baseVersion)

            if (branch != MAIN_BRANCH) {
                append('-')
                append(branch)
            }

            append('-')
            append(commit)
        }
    }

    private fun execute(
        project: Project,
        vararg command: String
    ): String {
        return runCatching {
            project.providers.exec {
                commandLine(command.toList())
                isIgnoreExitValue = true
            }
                .standardOutput
                .asText
                .get()
                .trim()
        }.getOrDefault("")
    }

    private fun sanitize(value: String): String {
        return value.replace(
            Regex("[^a-zA-Z0-9._-]"),
            "-"
        )
    }
}