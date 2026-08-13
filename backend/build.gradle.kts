plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "com.attendance.tracker"
version = "1.0.0"

application {
    mainClass.set("com.attendance.tracker.backend.ApplicationKt")
}



dependencies {
    // Ktor Server Core and Engine
    implementation("io.ktor:ktor-server-core-jvm:3.0.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.3")
    
    // Ktor Server Plugins
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.3")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.0.3")
    implementation("io.ktor:ktor-server-call-logging-jvm:3.0.3")
    implementation("io.ktor:ktor-server-double-receive-jvm:3.0.3")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.12")
    
    // Environment Configuration
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    
    // Database and Storage
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.2.0")
    implementation("software.amazon.awssdk:s3:2.29.3")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.0.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.20")
    testImplementation("io.mockk:mockk:1.13.13")
}

kotlin {
    jvmToolchain(17)
}
