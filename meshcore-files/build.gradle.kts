plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":meshcore-core"))
    implementation(project(":meshcore-crypto"))
    implementation(project(":meshcore-storage"))
    implementation(project(":meshcore-network"))
}
