plugins {
    id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT"
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

repositories {
    mavenCentral()
}

dependencies {
    // Minecraft（26.1.2 は難読化解除済み JAR が配布される）
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")

    // Fabric Loader
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    // Fabric API
    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

loom {
    serverOnlyMinecraftJar()
}
