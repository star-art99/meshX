plugins {
    kotlin("jvm")
}

dependencies {
    implementation("ch.qos.logback:logback-classic:${rootProject.property("logbackVersion")}")
}
