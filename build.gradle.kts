import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    id("com.google.cloud.tools.jib") version "3.3.0"
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("com.coditory.integration-test") version "2.2.2"
    id("pl.allegro.tech.build.axion-release") version "1.18.16"
    id("org.barfuin.gradle.jacocolog") version "3.1.0"
    jacoco
}

group = "com.example.graphql.unionpagination"
project.version = scmVersion.version

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

val mavenUser: String? by ext
val mavenPassword: String? by ext

repositories {
    mavenLocal()
    maven {
        name = "nexus-thirdparty"
        url = uri("https://nexus3.genesis-platform.io/repository/thirdparty/")
        credentials {
            username = System.getenv("NEXUS_USERNAME") ?: mavenUser
            password = System.getenv("NEXUS_PASSWORD") ?: mavenPassword
        }
    }
    maven {
        name = "nexus-releases"
        url = uri("https://nexus3.genesis-platform.io/repository/releases/")
        credentials {
            username = System.getenv("NEXUS_USERNAME") ?: mavenUser
            password = System.getenv("NEXUS_PASSWORD") ?: mavenPassword
        }
    }
    maven {
        name = "nexus-confluent"
        url = uri("https://nexus3.genesis-platform.io/repository/confluent/")
        credentials {
            username = System.getenv("NEXUS_USERNAME") ?: mavenUser
            password = System.getenv("NEXUS_PASSWORD") ?: mavenPassword
        }
    }
    maven {
        name = "nexus-central"
        url = uri("https://nexus3.genesis-platform.io/repository/central/")
        credentials {
            username = System.getenv("NEXUS_USERNAME") ?: mavenUser
            password = System.getenv("NEXUS_PASSWORD") ?: mavenPassword
        }
    }
}

extra["springCloudVersion"] = "2024.0.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.data:spring-data-commons")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("com.graphql-java:graphql-java:21.5")
    implementation("com.graphql-java:graphql-java-extended-scalars:21.0")
    implementation("com.apollographql.federation:federation-graphql-java-support:5.0.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.0")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude("org.mockito", "mockito-core")
        exclude("org.hamcrest", "hamcrest")
        exclude("com.vaadin.external.google", "android-json")
    }
    testImplementation("io.mockk:mockk:1.13.13") {
        // https://github.com/Ninja-Squad/springmockk?tab=readme-ov-file#usage
        exclude("mockito-core")
    }
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.springframework.graphql:spring-graphql-test")
    testImplementation("io.projectreactor:reactor-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_21)
        allWarningsAsErrors.set(true)
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.bootJar {
    enabled = true
    archiveFileName.set("${rootProject.name}-api.jar")
}

tasks.jar {
    enabled = true
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.test {
    systemProperty("spring.profiles.active", project.properties["springProfile"] ?: "test")
    dependsOn("unitTests", "integrationTests", "graphQlTests")
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

tasks.register<Test>("unitTests") {
    description = "Runs unit tests."
    group = "verification"

    systemProperty("spring.profiles.active", project.properties["springProfile"] ?: "test")
    useJUnitPlatform {
        includeTags("UnitTest")
        excludeTags("IntegrationTest")
        excludeTags("GraphQlTest")
    }

    testLogging {
        showStandardStreams = true
        events = setOf(STANDARD_ERROR, FAILED, PASSED)
    }

    jacoco {
        exclude("*Test")
    }

    finalizedBy("jacocoUnitTestsReport")
}

tasks.register<JacocoReport>("jacocoUnitTestsReport") {
    dependsOn("unitTests")
    executionData.setFrom(fileTree(layout.buildDirectory.get()).include("jacoco/unitTests.exec"))
    sourceDirectories.from(files(sourceSets["main"].allSource.srcDirs))
    classDirectories.from("${layout.buildDirectory.get()}/classes/kotlin/main")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<Test>("graphQlTests") {
    description = "Runs graphQl tests."
    group = "verification"

    systemProperty("spring.profiles.active", project.properties["springProfile"] ?: "test")
    useJUnitPlatform {
        includeTags("GraphQlTest")
        excludeTags("UnitTest")
        excludeTags("IntegrationTest")
    }

    testLogging {
        showStandardStreams = true
        events = setOf(STANDARD_ERROR, FAILED, PASSED)
    }

    jacoco {
        exclude("*Test")
    }

    finalizedBy("jacocoGraphQlTestsReport")
}

tasks.register<JacocoReport>("jacocoGraphQlTestsReport") {
    dependsOn("graphQlTests")
    executionData.setFrom(fileTree(layout.buildDirectory.get()).include("jacoco/graphQlTests.exec"))
    sourceDirectories.from(files(sourceSets["main"].allSource.srcDirs))
    classDirectories.from("${layout.buildDirectory.get()}/classes/kotlin/main")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<Test>("integrationTests") {
    useJUnitPlatform()
    description = "Runs integration tests."
    group = "verification"

    systemProperty("spring.profiles.active", project.properties["springProfile"] ?: "test")
    useJUnitPlatform {
        includeTags("IntegrationTest")
        excludeTags("UnitTest")
        excludeTags("GraphQlTest")
    }

    testLogging {
        showStandardStreams = true
        events = setOf(STANDARD_ERROR, FAILED)
    }
    finalizedBy("jacocoIntegrationTestsReport")
}

tasks.register<JacocoReport>("jacocoIntegrationTestsReport") {
    dependsOn("integrationTests")
    executionData.setFrom(fileTree(layout.buildDirectory.get()).include("jacoco/integrationTests.exec"))
    sourceDirectories.from(files(sourceSets["main"].allSource.srcDirs))
    classDirectories.from("${layout.buildDirectory.get()}/classes/kotlin/main")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register("coverage") {
    dependsOn("jacocoUnitTestsReport")
}

jib {
    from {
        image = "git.genesis-platform.io:4567/docker-images/vp-base:java-21-alpine3.18"
        platforms {
            platform {
                architecture = "${findProperty("jibArchitecture") ?: "arm64"}"
                os = "linux"
            }
        }
    }
    to {
        image = "spring-graphql-union-with-pagination"
    }
    container {
        mainClass = "com.example.graphql.unionpagination.ApplicationKt"
        entrypoint = listOf("sh", "entrypoint.sh")
        ports = listOf("8086")
        environment = mapOf("APP_SLEEP" to "0", "environment" to "local")
        creationTime = "USE_CURRENT_TIMESTAMP"
        user = "1000"
    }
    extraDirectories {
        paths {
            path {
                setFrom("src/main/docker/jib")
                into = "/app"
            }
        }
        permissions = mapOf("/app/entrypoint.sh" to "755")
    }
}

configure<KtlintExtension> {
    version.set("0.50.0")
}
