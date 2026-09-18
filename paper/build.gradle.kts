plugins {
    id("java")
    id("react.base-conventions")
    id("react.shadow-conventions")
}

val mainClass = "com.g4vrk.react.paper.PaperReactPlugin"

dependencies {

    implementation(project(":common"))

    compileOnly(libs.paper.api)

    implementation(libs.schedula.bukkit)

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