plugins {
    application
    java
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

application {
    mainClass.set("dev.updater.matcher.Matcher")
}

javafx {
    version = "11.0.2"
    modules = listOf("javafx.base", "javafx.graphics", "javafx.controls", "javafx.web", "javafx.media")
}

dependencies {
    implementation("org.ow2.asm:asm:_")
    implementation("org.ow2.asm:asm-util:_")
    implementation("org.ow2.asm:asm-commons:_")
    implementation("org.ow2.asm:asm-tree:_")
    implementation("no.tornado:tornadofx:_")
    implementation("org.tinylog:tinylog-api-kotlin:_")
    implementation("org.tinylog:tinylog-impl:_")
}