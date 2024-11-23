import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version libs.versions.org.springframework.boot.get() apply false
    id("io.spring.dependency-management") version libs.versions.io.spring.dependency.management.get() apply false
    kotlin("jvm") version libs.versions.org.jetbrains.kotlin.get() apply false
}

allprojects {
    group = "org.projectforge"
    version = "8.0.0"

    repositories {
        mavenCentral()
        gradlePluginPortal() // Spring Boot Plugins are here.
    }
}

configurations.all {
    exclude(group = "org.slf4j", module = "jul-to-slf4j")
    exclude(group = "org.slf4j", module = "slf4j-jul")
    exclude(group = "org.slf4j", module = "slf4j-log4j12")
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "org.slf4j", module = "log4j-over-slf4j")
    resolutionStrategy {
        preferProjectModules() // Prioritize local modules.
        force(
            "org.jboss.logging:jboss-logging:${libs.versions.jboss.logging.get()}",
            "org.hibernate:hibernate-core:${libs.versions.org.hibernate.orm.get()}"
        )
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
