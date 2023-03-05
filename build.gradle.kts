import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.legitimoose"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://repo.spongepowered.org/maven")
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.Minestom:Minestom:-SNAPSHOT")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.10")
    implementation("net.kyori:adventure-text-minimessage:4.10.1")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.mineskin:java-client:1.2.0-SNAPSHOT") //Must be installed locally
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "17"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.legitimoose.MainDemo"
    }
}