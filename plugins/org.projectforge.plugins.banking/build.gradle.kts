/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api(project(":projectforge-rest"))
    testImplementation(project(":projectforge-business"))
}

description = "org.projectforge.plugins.banking"