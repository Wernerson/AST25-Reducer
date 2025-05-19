import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("jvm") version "2.1.20"
    id("org.graalvm.buildtools.native") version "0.10.6"
    id("com.strumenta.antlr-kotlin") version "1.0.0"
}

group = "net.sebyte"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli-jvm:0.3.6")
    implementation("com.strumenta:antlr-kotlin-runtime:1.0.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(23)
    sourceSets.main {
        kotlin.srcDir(layout.buildDirectory.dir("generatedAntlr"))
    }
}

//sourceSets {
//    main {
//        java {
//            srcDir(tasks.generateGrammarSource)
//        }
//    }
//}
//
//tasks.generateGrammarSource {
//    outputDirectory = file("${project.buildDir}/generated/sources/main/java/antlr")
//}

val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    dependsOn("cleanGenerateKotlinGrammarSource")

    // ANTLR .g4 files are under {example-project}/antlr
    // Only include *.g4 files. This allows tools (e.g., IDE plugins)
    // to generate temporary files inside the base path
    source = fileTree(layout.projectDirectory.dir("antlr")) {
        include("**/*.g4")
    }

    // We want the generated source files to have this package name
    val pkgName = "com.strumenta.antlrkotlin.parsers.generated"
    packageName = pkgName

    // We want visitors alongside listeners.
    // The Kotlin target language is implicit, as is the file encoding (UTF-8)
    arguments = listOf("-visitor")

    // Generated files are outputted inside build/generatedAntlr/{package-name}
    val outDir = "generatedAntlr/${pkgName.replace(".", "/")}"
    outputDirectory = layout.buildDirectory.dir(outDir).get().asFile
}

tasks.withType<KotlinCompilationTask<*>> {
    dependsOn(generateKotlinGrammarSource)
}

graalvmNative {
    toolchainDetection.set(true)
    binaries {
        named("main") {
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(23))
            })
            mainClass.set("net.sebyte.MainKt")
        }
    }
}