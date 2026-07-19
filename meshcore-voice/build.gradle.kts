plugins {
    kotlin("jvm")
}

// TODO: Voice features require platform-specific audio APIs.
// Scaffold only – implement with javax.sound or platform-native bindings.
dependencies {
    implementation(project(":meshcore-core"))
    implementation(project(":meshcore-network"))
}
