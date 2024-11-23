/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("java-library")
}

dependencies {
    api(project(":projectforge-wicket"))
    api(project(":projectforge-rest"))
    testImplementation(project(":projectforge-business"))
    testImplementation(libs.jakarta.servlet.api)
    testImplementation(libs.org.postgresql.postgresql)
}

description = "org.projectforge.plugins.marketing"
