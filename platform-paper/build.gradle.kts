plugins {
    id("java")
    `maven-publish`
    signing
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25

    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    gradlePluginPortal()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
}

publishing {
    publications.create<MavenPublication>("maven") {
        groupId = "net.worldseed.multipart"
        artifactId = "WorldSeedEntityEngine-Paper"
        version = "11.3.5"

        from(components["java"])
    }

    repositories {
        maven {
            name = "AtlasEngine"
            url = uri("https://reposilite.atlasengine.ca/public")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

dependencies {
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)

    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")

    implementation(libs.commons.io)
    implementation(libs.zt.zip)

    implementation(libs.javax.json.api)
    implementation(libs.javax.json)

    implementation(libs.mql)
}

tasks.test {
    useJUnitPlatform()
}