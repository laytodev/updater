plugins {
    kotlin("jvm") version "1.7.21"
}

group = "dev.updater"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("org.ow2.asm:asm:_")
    implementation("org.ow2.asm:asm-commons:_")
    implementation("org.ow2.asm:asm-tree:_")
    implementation("org.ow2.asm:asm-util:_")
}