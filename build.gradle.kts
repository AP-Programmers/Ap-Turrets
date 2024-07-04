plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("io.github.0ffz.github-packages") version "1.2.1"
}

repositories {
    gradlePluginPortal()
    mavenLocal()
    githubPackage("apdevteam/movecraft")
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
    maven {
        url = uri("https://maven.enginehub.org/repo/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    api("org.jetbrains:annotations-java5:24.1.0")
    api("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.18.2-R0.1-SNAPSHOT")
    compileOnly("net.countercraft:movecraft:+")
    compileOnly(files("../Movecraft-WorldGuard/target/Movecraft-WorldGuard.jar"))
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.7")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
}

group = "snowleapord.github.com"
version = "3.0.0_beta-2"
description = "APTurrets"

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.jar {
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.processResources {
    from(rootProject.file("LICENSE.md"))
    filesMatching("*.yml") {
        expand(mapOf("projectVersion" to project.version))
    }
}
