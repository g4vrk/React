plugins {
    id("java-library")
    id("react.base-conventions")
    id("react.shadow-conventions")
}

val dependenciesDir = file("${rootProject.projectDir}/dependencies")

dependencies {

    implementation(project(":common"))

    compileOnly(libs.folia.api)

    implementation(
        fileTree(dependenciesDir) {
            include("schedula-folia-1.0.0.jar")
        }
    )

}

java {

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

}

sourceSets {
    main {
        resources {
            srcDir(project(":common").file("src/main/resources"))
        }
    }
}

tasks {
    withType<ProcessResources>().configureEach {

        inputs.property(
            "mainClass",
            "com.g4vrk.react.folia.FoliaReactPlugin"
        )

        filesMatching("plugin.yml") {

            expand(
                "mainClass" to "com.g4vrk.react.folia.FoliaReactPlugin"
            )

        }

    }

    withType<Jar>().configureEach {
        enabled = false
    }
}