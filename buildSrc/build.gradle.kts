plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

apply(plugin = "org.jetbrains.kotlin.jvm")

repositories {
    mavenCentral()
    jcenter()
}

val fatJar = tasks.create("fatJar", Jar::class) {
    exclude("META-INF/*.RSA', 'META-INF/*.SF','META-INF/*.DSA")

    from(configurations.compile.get().map({ if (it.isDirectory) it else zipTree(it) }))
    with(tasks.jar.get() as CopySpec)

    manifest {
        attributes["Main-Class"] = "io.pleo.antaeus.app.AntaeusApp"
        attributes["Implementation-Version"] = "1.0"
        attributes["Implementation-Title"] = "Gradle Jar File Example"
        attributes["Class-Path"] = configurations.runtimeClasspath.get().map({ it.name }).joinToString(" ")
    }
    baseName = project.name + "-all"

}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
