import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.zip.ZipFile

plugins {
    kotlin("jvm") version "2.2.21"

    application
}

repositories {
    mavenCentral()

    maven("https://jitpack.io") {
        content {
            includeGroup("com.github.iBotPeaches.smali")
        }
    }
}

val localState = file(
    System.getenv("ONEPIXEL_HOME") ?: "${System.getProperty("user.home")}/.local/share/onepixel"
)
    .resolve(relative = "launcher")

layout.buildDirectory.set(localState.resolve(relative = "build"))

kotlin {
    jvmToolchain(21)
}

val androidLibraries by configurations.creating {
    isTransitive = false
}

val compat by sourceSets.creating
val extractedLibraries = layout.buildDirectory.dir("android-libraries")

dependencies {
    implementation("org.apktool:apktool-cli:3.0.3")
    implementation("io.github.java-diff-utils:java-diff-utils:4.15")

    add(compat.compileOnlyConfigurationName, "org.robolectric:android-all:16-robolectric-13921718")

    androidLibraries("org.lsposed.hiddenapibypass:hiddenapibypass:6.2@aar")

    for (artifact in listOf("api", "aidl", "shared", "provider")) {
        androidLibraries("dev.rikka.shizuku:$artifact:13.1.5@aar")
    }

    add(
        compat.compileOnlyConfigurationName,
        files(extractedLibraries).asFileTree.matching {
            include("*.jar")
        }
    )
}

val extractAndroidLibraries by tasks.registering {
    inputs.files(androidLibraries)
    outputs.dir(extractedLibraries)

    doLast {
        val output = extractedLibraries.get().asFile
        output.mkdirs()

        for (aar in androidLibraries.files) {
            ZipFile(aar).use { archive ->
                archive.getInputStream(archive.getEntry("classes.jar")).use { input ->
                    output
                        .resolve(relative = aar.nameWithoutExtension + ".jar")
                        .outputStream()
                        .use { destination ->
                            input.copyTo(out = destination)
                        }
                }
            }
        }
    }
}

tasks.named<KotlinCompile>("compileCompatKotlin") {
    dependsOn(extractAndroidLibraries)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.addAll("-Xno-param-assertions", "-Xno-call-assertions", "-Xno-receiver-assertions")
    }
}

tasks.named<JavaCompile>("compileCompatJava") {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

val compatJar by tasks.registering(Jar::class) {
    from(compat.output)
    archiveBaseName.set("onepixel-compat")
}

application {
    mainClass.set("org.onepixel.tools.MainKt")
}

tasks.named<JavaExec>("run") {
    dependsOn(compatJar, extractAndroidLibraries)

    systemProperty("onepixel.module", projectDir.absolutePath)
    systemProperty("onepixel.compat", compatJar.get().archiveFile.get().asFile.absolutePath)
    systemProperty("onepixel.libraries", extractedLibraries.get().asFile.absolutePath)
}
