rootProject.name = "react"

include(
    "common",
    "paper",
    "folia",
    "collector"
)

dependencyResolutionManagement {

    versionCatalogs {

        create("libs") {

            from(files("libs.versions.toml"))

        }

    }

}