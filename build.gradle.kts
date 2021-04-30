import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm") version "1.3.70" apply false
    id("com.diffplug.spotless") version "5.12.4"
}

allprojects {
    group = "io.pleo"
    version = "1.0"

    apply(plugin = "com.diffplug.gradle.spotless")

    spotless {
        kotlin {
            // by default the target is every '.kt' and '.kts` file in the java sourcesets
            ktlint()   // has its own section below
            ktfmt()    // has its own section below
            prettier() // has its own section below
            licenseHeader '/* (C)$YEAR */' // or licenseHeaderFile
        }
        kotlinGradle {
            target '*.gradle.kts' // default target for kotlinGradle
            ktlint() // or ktfmt() or prettier()
        }
    }

    repositories {
        mavenCentral()
        jcenter()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "11"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}