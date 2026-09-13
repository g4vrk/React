plugins {
    id("java")
    id("react.base-conventions")
    id("react.shadow-conventions")
}

val dependenciesDir = file("${rootProject.projectDir}/dependencies")

dependencies {

    implementation(project(":common"))

    compileOnly(libs.paper.api)

    implementation(
        fileTree(dependenciesDir) {
            include("schedula-bukkit-1.0.0.jar")
        }
    )

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
            "com.g4vrk.react.paper.PaperReactPlugin"
        )

        filesMatching("plugin.yml") {

            expand(
                "mainClass" to "com.g4vrk.react.paper.PaperReactPlugin"
            )

        }

    }

    withType<Jar>().configureEach {
        enabled = false
    }

}