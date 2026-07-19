plugins {
    kotlin("jvm")
}

// TODO: Video features require platform-specific media APIs.
// Scaffold only – implement with platform-native bindings or GStreamer JNI.
dependencies {
    implementation(project(":meshcore-core"))
    implementation(project(":meshcore-network"))
}
