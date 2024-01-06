plugins {
    application

    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val ktorVersion: String by project

group = "org.example"
version = "1.1.0"

repositories {
    google()
    mavenCentral()
    maven {
        name = "Sonatype Snapshots (Legacy)"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }

    maven {
        name = "Sonatype Snapshots"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

application {
    // This is deprecated, but the Shadow plugin requires it
    mainClass = "org.ecorous.synapse.MainKt"
}

dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.7")
    implementation(libs.kord.extensions)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}


tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "org.ecorous.synapse.MainKt"
        )
    }
}