plugins {
    application
}
apply(plugin = "org.jetbrains.kotlin.jvm")

repositories { mavenCentral() }

dependencies {
    implementation("io.ktor:ktor-client-cio:2.3.11")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:2.3.11")
}

application { mainClass.set("stub.MainKt") }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}
tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes["Main-Class"] = "stub.MainKt" }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

// Server integration tests need client classes, not an executable JAR that also embeds dependencies.
val testApiJar by tasks.registering(Jar::class) {
    archiveClassifier.set("test-api")
    from(sourceSets.main.get().output)
}
configurations.create("testApiElements") {
    isCanBeConsumed = true
    isCanBeResolved = false
    extendsFrom(configurations.getByName("implementation"))
    outgoing.artifact(testApiJar)
}
