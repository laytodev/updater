plugins {
    id("org.openjfx.javafxplugin") version "0.0.13"
    application
}

application {
    mainClass.set("dev.updater.matcher.Main")
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