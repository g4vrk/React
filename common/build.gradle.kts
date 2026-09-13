plugins {
    id("java-library")
    id("react.base-conventions")
}

repositories {
    maven {
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    maven {
        url = uri("https://repo.opencollab.dev/main/")
    }

    maven {
        url = uri("https://repo.opencollab.dev/snapshots/")
    }
}

tasks.named<ProcessResources>("processResources") {
    enabled = false
}

val dependenciesDir = file("${rootProject.projectDir}/dependencies")

dependencies {

    compileOnly(libs.paper.api)

    compileOnly(libs.placeholderapi)
    compileOnly(libs.packetevents)

    compileOnly(libs.moshi)
    compileOnly(libs.moshi.adapters)

    implementation(libs.okhttp)

    implementation(libs.hikari) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    implementation(libs.h2)

    implementation(libs.mysql) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    implementation(libs.mongodb) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    implementation(libs.sqlite) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    compileOnly(libs.ejml.core)
    compileOnly(libs.ejml.ddense)
    compileOnly(libs.ejml.simple)

    compileOnly(libs.configurate.yaml)

    compileOnly(libs.cloud.paper)

    compileOnly(libs.geyser.api)
    compileOnly(libs.floodgate.api)

    implementation(libs.textserializer.legacy)

    implementation(fileTree(dependenciesDir) {
        include("schedula-common-1.0.0.jar")
        include("FunctionalActions-2.1.1-d94a91d.jar")
        include("FunctionalConfiguration-1.0.0.jar")
    })

}