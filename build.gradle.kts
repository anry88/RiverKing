buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.9.23")
    }
}

plugins {
    application
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

repositories { mavenCentral() }

val ktorVersion = "2.3.11"
val exposedVersion = "0.53.0"

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-double-receive:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")

    // (optional) Ktor client for Bot API
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Exposed + SQLite
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("de.mkammerer:argon2-jvm:2.12")
    implementation("com.google.api-client:google-api-client:2.7.0")
    implementation("com.google.http-client:google-http-client-gson:1.45.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.30.1")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

application {
    mainClass.set("app.ApplicationKt")
    applicationDefaultJvmArgs = listOf(
        "-Xms256m",
        "-Xmx1024m",
        "-XX:+UseG1GC",
    )
}

tasks.withType<Jar> {
    manifest { attributes["Main-Class"] = "app.ApplicationKt" }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    archiveFileName.set("riverking-all.jar")
}
