import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withType
import util.VersionUtility
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    id("java-library")
    id("react.base-conventions")
    id("react.shadow-conventions")
    alias(libs.plugins.run.paper)
}

version = VersionUtility.version(project, version.toString())

allprojects {

    repositories {

        mavenCentral()

        maven {
            name = "papermc-repo"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }

        maven {
            name = "sonatype"
            url = uri("https://oss.sonatype.org/content/groups/public/")
        }

        maven {
            name = "codemc-public"
            url = uri("https://repo.codemc.io/repository/maven-public/")
        }

        maven {
            name = "jitpack"
            url = uri("https://jitpack.io/")
        }

    }

}

subprojects {

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "com.gradleup.shadow")

    group = rootProject.group
    version = rootProject.version
    description = rootProject.description

    dependencies {

        compileOnly(rootProject.libs.lombok)
        annotationProcessor(rootProject.libs.lombok)

    }

    plugins.withType<JavaPlugin> {

        dependencies {
            "testImplementation"(
                platform(rootProject.libs.junit.bom)
            )

            "testImplementation"(
                rootProject.libs.junit.jupiter
            )

            "testRuntimeOnly"(
                rootProject.libs.junit.platform.launcher
            )
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }

    extensions.configure<JavaPluginExtension> {

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        withSourcesJar()

    }


    tasks {

        withType<JavaCompile>().configureEach {

            options.encoding = "UTF-8"

        }

        withType<RunServer>().configureEach {
            minecraftVersion(libs.versions.minecraft.get())
            jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
        }

        named("build") {

            dependsOn(named("shadowJar"))

        }

    }


    extensions.configure<PublishingExtension> {

        publications {

            create<MavenPublication>("maven") {

                groupId = System.getenv("GROUP") ?: project.group.toString()
                artifactId = rootProject.name + project.name
                version = System.getenv("VERSION") ?: project.version.toString()

                artifact(tasks.named<ShadowJar>("shadowJar"))
                artifact(tasks.named<Jar>("sourcesJar"))

                pom {
                    name.set("${rootProject.name}-${project.name}")
                    description.set(provider { project.description })
                }

            }

        }

    }

}


tasks {

    clean {

        dependsOn(subprojects.map { "${it.path}:clean" })

    }

    build {

        dependsOn(subprojects.map { "${it.path}:build" })

    }

}

defaultTasks("clean", "build")
