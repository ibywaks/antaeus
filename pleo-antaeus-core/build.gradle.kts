plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    api(project(":pleo-antaeus-models"))
    implementation("com.beust:klaxon:5.5")
    implementation ("com.stripe:stripe-java:20.47.1")
}
