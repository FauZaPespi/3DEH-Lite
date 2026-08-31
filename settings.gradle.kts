plugins {
    // Lets Gradle provision a JDK 21 toolchain when none is installed locally (CI, fresh clones).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "3DEH-Lite"
