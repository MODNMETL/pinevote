import org.gradle.kotlin.dsl.*

plugins {
    java
    // OLD: id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.gradleup.shadow") version "9.2.0"
}


group = "com.modnmetl"
version = System.getenv("VERSION") ?: "1.0.0-SNAPSHOT"


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("org.xerial:sqlite-jdbc:3.46.0.1")
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand(
                "version" to project.version
            )
        }
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("org.sqlite", "com.modnmetl.pinevote.libs.sqlite")
    }
}
