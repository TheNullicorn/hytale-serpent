import java.io.FileNotFoundException

plugins {
    id("java")
}

group = "me.nullicorn"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "hytale-release"
        url = uri("https://maven.hytale.com/release/")
    }
    maven {
        name = "hytale-pre-release"
        url = uri("https://maven.hytale.com/pre-release/")
    }
}

dependencies {
    implementation("com.hypixel.hytale:Server:2026.04.23-937872667")
}

fun runServer(task: JavaExec, vararg extraArgs: String) {
    val hytaleServerArtifact =
        project.configurations.compileClasspath.get().resolvedConfiguration.resolvedArtifacts.find { it.moduleVersion.id.group == "com.hypixel.hytale" && it.moduleVersion.id.name == "Server" }
    if (hytaleServerArtifact == null) {
        throw FileNotFoundException("failed to locate Hytale server dependency and its jar file")
    }

    val hytaleAssetsZip = layout.projectDirectory.file("run/Assets.zip").asFile
    if (!hytaleAssetsZip.isFile) {
        throw FileNotFoundException("please copy the Assets.zip file for version ${hytaleServerArtifact.moduleVersion.id.version} into the `run` folder")
    }

    task.workingDir = layout.projectDirectory.dir("run/").asFile
    task.classpath = files(hytaleServerArtifact.file)
    task.mainClass = "com.hypixel.hytale.Main"
    task.args = listOf(
        "--assets", hytaleAssetsZip.path,
        // Load our asset pack folder so that the assets in it can be hot reloaded.
        "--mods", layout.projectDirectory.dir("src/main/resources/").asFile.path,
        // Load our mod jar. This also has our assets, but they are ignored because we loaded the direct folder first.
        "--mods", layout.buildDirectory.get().dir("libs/").asFile.path,
    ) + extraArgs
}

tasks.register<JavaExec>("runServer") {
    dependsOn("jar")
    group = "hytale"

    // Use JVM arguments in `run/jvm.options`, enabling assertions and remote debugging.
    jvmArgs = listOf("@jvm.options")
    // Allow Hytale console commands to be input through this Gradle task's stdin.
    standardInput = System.`in`

    runServer(task = this)
}

tasks.register<JavaExec>("generateAssetSchema") {
    group = "hytale"

    runServer(
        this,
        "--generate-asset-schema", layout.projectDirectory.dir("src/main/resources/").asFile.path
    )
}
