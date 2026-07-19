plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("io.meshcore.cli.MainKt")
}

val cliktVersion: String = rootProject.property("cliktVersion").toString()
val logbackVersion: String = rootProject.property("logbackVersion").toString()

dependencies {
    implementation(project(":meshcore-core"))
    implementation(project(":meshcore-crypto"))
    implementation(project(":meshcore-storage"))
    implementation(project(":meshcore-network"))
    implementation(project(":meshcore-discovery"))
    implementation(project(":meshcore-api"))
    implementation("com.github.ajalt.clikt:clikt:$cliktVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
}
