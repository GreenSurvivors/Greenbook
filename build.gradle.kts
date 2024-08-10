plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("xyz.jpenilla.run-paper") version "2.3.0" // Adds runServer task for testing
}

group = "de.greensurvivors"
version = "0.0.3-SNAPSHOT"
description = "Like Craftbook, but not a buggy dinosaur"
// this is the minecraft. This is also used as the api version of the plugin.yml
val mcVersion = "1.21"
// don't use spigots reobfused jar
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 8 installed for example.
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

repositories {
    mavenCentral()
    mavenLocal()

    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven { // world edit
        name = "enginehub.org"
        url = uri("https://maven.enginehub.org/repo/")
    }
}

dependencies {
    paperweight.paperDevBundle("$mcVersion-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:24.1.0")
    api("com.github.ben-manes.caffeine:caffeine:3.1.8") // caches
    api("org.apache.commons:commons-collections4:4.5.0-M2")
    api("com.sk89q.worldedit:worldedit-bukkit:7.3.5-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(targetJavaVersion)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything

        expand(
            "version" to project.version,
            "description" to project.description,
            "apiVersion" to mcVersion
        )
    }
}
