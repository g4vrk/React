rootProject.name = "react"

include(
    "common",
    "paper",
    "folia"
)

dependencyResolutionManagement {

    versionCatalogs {

        create("libs") {

            from(files("libs.versions.toml"))

        }

    }

}