import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

group = "dev.kergos"

// One artifact line per Kotlin line: the plugin is compiled against the compiler's IR API,
// so it is rebuilt for every Kotlin minor and never shared across minors.
version = "2.4.0"

val kotlinMinor = libs.versions.kotlin.get().substringBeforeLast('.')

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)

    testImplementation(libs.kotlin.compiler.embeddable)
    testImplementation(libs.kctfork.core)
    testImplementation(kotlin("test-junit5"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

if (!version.toString().startsWith("$kotlinMinor.")) {
    throw GradleException("version $version does not match the Kotlin line $kotlinMinor; bump both together")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        freeCompilerArgs.add("-Xjdk-release=8")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

tasks.compileTestKotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
