plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":meshcore-core"))
    implementation("org.bouncycastle:bcprov-jdk18on:${rootProject.property("bouncyCastleVersion")}")
}
