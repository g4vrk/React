plugins {
    id("java-library")
    id("react.base-conventions")
    id("react.shadow-conventions")
}

val mainClass = "com.g4vrk.react.folia.FoliaReactPlugin"

dependencies {

    implementation(project(":common"))

    compileOnly(libs.folia.api)

    implementation(libs.schedula.folia)

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

    named<ProcessResources>("processResources") {

        notCompatibleWithConfigurationCache(
            "Uses Groovy template expansion for plugin.yml"
        )

        inputs.properties(
            "version" to rootProject.version.toString(),
            "mainClass" to mainClass
        )

        filesMatching("plugin.yml") {
            expand(
                "version" to rootProject.version.toString(),
                "mainClass" to mainClass
            )
        }
    }

    named<Jar>("jar") {
        enabled = false
    }

}