package util

import org.gradle.api.Project
import org.jetbrains.annotations.NotNull

final class VersionUtility {

    static @NotNull String branch(final @NotNull Project project) {

        final String environmentBranch = System.getenv("GIT_BRANCH")

        if (environmentBranch != null && !environmentBranch.isBlank()) {
            return sanitize(environmentBranch)
        }

        final String branch = execute(
                project,
                "git",
                "branch",
                "--show-current"
        )

        if (branch.isBlank()) {
            return "local"
        }

        return sanitize(branch)

    }

    static @NotNull String commitHash(final @NotNull Project project) {

        final String environmentCommit = System.getenv("GIT_COMMIT")

        if (environmentCommit != null && !environmentCommit.isBlank()) {
            return environmentCommit.take(7)
        }

        final String commit = execute(
                project,
                "git",
                "rev-parse",
                "--short",
                "HEAD"
        )

        if (commit.isBlank()) {
            return "unknown"
        }

        return commit

    }

    static @NotNull String version(
            final @NotNull Project project,
            final @NotNull String baseVersion
    ) {

        final String branch = branch(project)
        final String commit = commitHash(project)

        final StringBuilder fileName = new StringBuilder(baseVersion)

        if (branch != "master") {
            fileName.append("-")
                    .append(branch)
        }

        fileName.append("-")
                .append(commit)

        return fileName.toString()

    }

    private static @NotNull String execute(
            final @NotNull Project project,
            final @NotNull String... command
    ) {

        try {

            return project.providers.exec {
                commandLine command
                ignoreExitValue = true
            }.standardOutput.asText.get().trim()

        } catch (final Exception ignored) {
            return ""

        }

    }

    private static @NotNull String sanitize(final @NotNull String value) {

        return value.replaceAll("[^a-zA-Z0-9._-]", "-")

    }

}