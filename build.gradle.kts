plugins {
    kotlin("jvm") version "1.8.20"
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")


    group = "dev.updater"
    version = "1.0.0"

    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

        if(!JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_11)) {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(11))
            }
        }
    }

    tasks.compileJava {
        options.encoding = "UTF-8"
        options.release.set(11)
    }
}