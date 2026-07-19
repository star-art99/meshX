import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
}

val kotlinVersion: String by project
val coroutinesVersion: String by project
val ktorVersion: String by project
val logbackVersion: String by project
val slf4jVersion: String by project
val cliktVersion: String by project
val bouncyCastleVersion: String by project
val junitVersion: String by project
val kotestVersion: String by project
val sqliteVersion: String by project
val jacksonVersion: String by project

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "io.meshcore"
    version = "0.1.0-SNAPSHOT"

    val kotlinVersionProp: String = rootProject.findProperty("kotlinVersion")?.toString() ?: "2.0.21"
    val coroutinesVersionProp: String = rootProject.findProperty("coroutinesVersion")?.toString() ?: "1.8.1"
    val slf4jVersionProp: String = rootProject.findProperty("slf4jVersion")?.toString() ?: "2.0.16"
    val junitVersionProp: String = rootProject.findProperty("junitVersion")?.toString() ?: "5.11.3"
    val kotestVersionProp: String = rootProject.findProperty("kotestVersion")?.toString() ?: "5.9.1"
    val logbackVersionProp: String = rootProject.findProperty("logbackVersion")?.toString() ?: "1.5.8"

    dependencies {
        add("implementation", platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersionProp"))
        add("implementation", "org.jetbrains.kotlin:kotlin-stdlib")
        add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersionProp")
        add("implementation", "org.slf4j:slf4j-api:$slf4jVersionProp")

        add("testImplementation", "org.jetbrains.kotlin:kotlin-test")
        add("testImplementation", "org.junit.jupiter:junit-jupiter-api:$junitVersionProp")
        add("testImplementation", "org.junit.jupiter:junit-jupiter-params:$junitVersionProp")
        add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine:$junitVersionProp")
        add("testImplementation", "io.kotest:kotest-runner-junit5:$kotestVersionProp")
        add("testImplementation", "io.kotest:kotest-assertions-core:$kotestVersionProp")
        add("testImplementation", "ch.qos.logback:logback-classic:$logbackVersionProp")
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    tasks.withType<Jar> {
        manifest {
            attributes["Implementation-Title"] = project.name
            attributes["Implementation-Version"] = project.version
        }
    }
}
