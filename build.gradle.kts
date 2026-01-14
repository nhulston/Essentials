plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.3.1"
}

group = findProperty("pluginGroup") as String? ?: "com.nhulston"
version = findProperty("pluginVersion") as String? ?: "1.1.0"
description = findProperty("pluginDescription") as String? ?: "Your Essentials for a Hytale server"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Hytale Server API (provided by server at runtime)
    compileOnly(files("libs/HytaleServer.jar"))

    // Common dependencies (will be bundled in JAR)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.tomlj:tomlj:1.1.1")
    implementation("org.jetbrains:annotations:24.1.0")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    // Configure Java compilation
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 25
    }

    // Configure resource processing
    processResources {
        filteringCharset = Charsets.UTF_8.name()

        // Replace placeholders in manifest.json
        val props = mapOf(
            "group" to project.group,
            "version" to project.version,
            "description" to project.description
        )
        inputs.properties(props)

        filesMatching("manifest.json") {
            expand(props)
        }
    }

    // Configure ShadowJar (bundle dependencies)
    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")
        destinationDirectory.set(file("/mnt/c/Users/Nicholas/Development/hytale-server/mods"))

        // Relocate dependencies to avoid conflicts
        relocate("com.google.gson", "com.nhulston.libs.gson")
        relocate("org.tomlj", "com.nhulston.libs.tomlj")
        relocate("org.antlr", "com.nhulston.libs.antlr")

        // Minimize JAR size (removes unused classes)
        minimize()
    }

    // Configure tests
    test {
        useJUnitPlatform()
    }

    // Make build depend on shadowJar
    build {
        dependsOn(shadowJar)
    }
}

// Configure Java toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
