import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    `java-library`
}

apply(plugin = "com.diffplug.spotless")

java {

    disableAutoTargetJvm()

}

extensions.configure<SpotlessExtension> {

    java {

        endWithNewline()
        leadingTabsToSpaces(4)

        removeUnusedImports()

        trimTrailingWhitespace()
        targetExclude("build/generated/**/*")

    }

    groovyGradle {

        endWithNewline()

        leadingTabsToSpaces(4)

        trimTrailingWhitespace()

    }

}