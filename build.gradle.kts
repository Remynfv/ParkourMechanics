import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

group = "com.legitimoose"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://repo.spongepowered.org/maven")
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.Minestom:Minestom:-SNAPSHOT")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "16"
}