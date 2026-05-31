plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    `maven-publish`
}

group = "kh.com.ktor"
version = "0.0.12"

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        name = "GitHubSovannarithcheav"
        url = uri("https://maven.pkg.github.com/sovannarithcheav/ktor-plugins")
        credentials {
            username = System.getenv("GIT_PUBLISH_USER")
            password = System.getenv("GIT_PUBLISH_PASSWORD")
        }
    }
}

dependencies {
    compileOnly("kh.com.ktor:plugins:0.0.8")

    compileOnly("io.ktor:ktor-server-core-jvm:3.1.1")
    compileOnly("io.ktor:ktor-server-websockets-jvm:3.1.1")
    compileOnly("io.ktor:ktor-websockets:3.1.1")
    compileOnly("io.ktor:ktor-server-content-negotiation:3.1.1")
    compileOnly("io.ktor:ktor-serialization-kotlinx-json:3.1.1")
    compileOnly("io.ktor:ktor-client-core:3.1.1")
    compileOnly("io.ktor:ktor-client-cio:3.1.1")
    compileOnly("io.ktor:ktor-client-content-negotiation:3.1.1")

    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    compileOnly("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    compileOnly("org.jetbrains.exposed:exposed-core:0.54.0")
    compileOnly("org.jetbrains.exposed:exposed-jdbc:0.54.0")
    compileOnly("org.jetbrains.exposed:exposed-kotlin-datetime:0.54.0")
    compileOnly("org.jetbrains.exposed:exposed-json:0.54.0")

    compileOnly("org.eclipse.angus:angus-mail:2.0.3")
    compileOnly("org.slf4j:slf4j-api:2.0.9")
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/sovannarithcheav/ktor-notification")
            credentials {
                username = System.getenv("GIT_PUBLISH_USER")
                password = System.getenv("GIT_PUBLISH_PASSWORD")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            artifactId = "notification"
            from(components["java"])
        }
    }
}
