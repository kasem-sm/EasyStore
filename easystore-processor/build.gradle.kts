plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

dependencies {
    implementation("androidx.datastore:datastore-preferences-core:1.0.0")

    implementation(project(":easystore-core"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.7.10-1.0.6")

    implementation("com.squareup:kotlinpoet:1.12.0")
    implementation("com.squareup:kotlinpoet-ksp:1.12.0")

    implementation("com.google.auto.service:auto-service-annotations:1.0")
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.0.0")
}

rootProject.extra.apply {
    set("PUBLISH_GROUP_ID", LibraryConfigs.groupId)
    set("PUBLISH_VERSION", LibraryConfigs.version)
    set("PUBLISH_ARTIFACT_ID", "processor")
}

apply(from ="${rootDir}/scripts/publish-module.gradle")
