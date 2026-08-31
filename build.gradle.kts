plugins {
    java
}

group = "dev.fauza"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.paper.api)

    // paper-api is compileOnly, so its transitive Adventure/JOML artifacts never reach the test
    // classpath. The pure-logic tests need them standalone, hence the explicit re-declaration.
    testImplementation(libs.adventure.api)
    testImplementation(libs.adventure.minimessage)
    testImplementation(libs.adventure.legacy)
    testImplementation(libs.adventure.plain)
    testImplementation(libs.joml)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:all,-serial,-processing")
}

tasks.jar {
    archiveBaseName = "3DEH-Lite"
    archiveClassifier = ""
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
