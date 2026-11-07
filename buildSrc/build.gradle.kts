plugins {
    `kotlin-dsl`
}

repositories {

    gradlePluginPortal()

    mavenCentral()

}

dependencies {

    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.11.0")

    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.6.1")

}