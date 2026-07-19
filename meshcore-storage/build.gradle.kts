plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":meshcore-core"))
    implementation(project(":meshcore-crypto"))
    implementation("org.xerial:sqlite-jdbc:${rootProject.property("sqliteVersion")}")
}
