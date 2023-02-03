plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "1.8.10-1.0.9"
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

dependencies {
    implementation(project(":easystore-core"))

    implementation("androidx.datastore:datastore-preferences-core:1.0.0")
    implementation("com.google.devtools.ksp:symbol-processing-api:1.8.10-1.0.9")
    
    implementation("com.squareup:kotlinpoet:1.12.0")
    implementation("com.squareup:kotlinpoet-ksp:1.12.0")

    implementation("com.google.auto.service:auto-service-annotations:1.0")
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.0.0")

    testImplementation("junit:junit:4.13.2")
}

rootProject.extra.apply {
    set("PUBLISH_GROUP_ID", LibraryConfigs.groupId)
    set("PUBLISH_VERSION", rootProject.extra.get("rootVersionName"))
    set("PUBLISH_ARTIFACT_ID", "processor")
}

apply(from ="${rootDir}/scripts/publish-module.gradle")
