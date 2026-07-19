plugins {
    kotlin("jvm")
}

val ktorVersion: String = rootProject.property("ktorVersion").toString()
val jacksonVersion: String = rootProject.property("jacksonVersion").toString()

dependencies {
    implementation(project(":meshcore-core"))
    implementation(project(":meshcore-crypto"))
    implementation(project(":meshcore-storage"))
    implementation(project(":meshcore-network"))
    implementation(project(":meshcore-discovery"))
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
}
