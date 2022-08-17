plugins {
    kotlin("jvm")
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

rootProject.extra.apply {
    set("PUBLISH_GROUP_ID", LibraryConfigs.groupId)
    set("PUBLISH_VERSION", LibraryConfigs.version)
    set("PUBLISH_ARTIFACT_ID", "core")
}

apply(from ="${rootDir}/scripts/publish-module.gradle")
