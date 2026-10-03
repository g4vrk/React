plugins {
    `kotlin-dsl`
}

repositories {

    gradlePluginPortal()

    mavenCentral()

}

dependencies {

    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.10.3")

    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.6.1")

}