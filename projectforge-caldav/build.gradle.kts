plugins {
    id("buildlogic.pf-module-conventions")
}

dependencies {
    api(project(":projectforge-model"))
    api(project(":projectforge-business"))
    api(project(":projectforge-rest"))
    api(libs.org.postgresql.postgresql)
    api(libs.org.springframework.spring.webmvc)
    api(libs.io.milton.milton.server.ent)
    api(libs.com.googlecode.ez.vcard)
    api(libs.org.slf4j.jcl.over.slf4j)
    testImplementation(libs.org.mockito.core)
}

description = "projectforge-caldav"
