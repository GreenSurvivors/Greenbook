plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "2.3.1" // Adds runServer task for testing
    id("net.raphimc.class-token-replacer") version "1.1.7" // replace tokens in java code
}

group = "de.greensurvivors"
description = "Like Craftbook, but not a buggy dinosaur"
version = buildString {
    append(project.properties["plugin_version"])

    if ((project.properties["is_release"] as String).toBoolean().not()) {
        append("-Snapshot")
    }

    append("+${project.properties["minecraft_version"]}")
}

// todo remove with 26.1
// don't use spigots reobfused jar
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

val targetJavaVersion = JavaLanguageVersion.of("${rootProject.properties["java_version"]}").asInt()
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
    paperweight.paperDevBundle("${project.properties["minecraft_version"]}-R0.1-SNAPSHOT")
    api("com.github.ben-manes.caffeine:caffeine:${project.properties["caffeine_version"]}") // caches
    api("com.sk89q.worldedit:worldedit-bukkit:${project.properties["worldedit_version"]}")
}

sourceSets {
    main {
        classTokenReplacer {
            property("\${caffeine_version}", project.properties["caffeine_version"].toString())
        }
    }
}

tasks {
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything

        filesNotMatching("**/WirelessRedstone.yml") { // the complex pattern doesn't play nicely with the expand task
            expand(
                "version" to project.version,
                "description" to project.description as String,
                "apiVersion" to project.properties["minecraft_version"].toString()
            )
        }
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(targetJavaVersion)
    }

    runServer {
        downloadPlugins {
            // make sure to double-check the version id on the Modrinth version page
            modrinth("worldedit", project.properties["worldEdit_runVersion"].toString())
        }

        // disable bstats, as it isn't needed for dev environment
        doFirst { // this happens after downloading the plugins above, but before the server starts
            val bStatsCfg = runDirectory.get().asFile.resolve("plugins/bStats/config.yml")
            if (!bStatsCfg.exists()) {
                bStatsCfg.parentFile.mkdirs()
                bStatsCfg.createNewFile()
            }
            bStatsCfg.writeText("enabled: false\n")
        }
        // automatically agree to eula
        jvmArgs("-Dcom.mojang.eula.agree=true")
    }
}
