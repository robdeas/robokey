import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"

    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.graalvm.buildtools.native") version "0.10.3"

    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "tech.robd.robokey"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("org.springframework.boot:spring-boot-starter-logging")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.ibm.icu:icu4j:75.1")

    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")
    implementation("com.github.purejavacomm:purejavacomm:1.0.1.RELEASE")
    // Swing is quite old, so lets use a modern third party theme
    implementation("com.formdev:flatlaf:3.4.1")
    // Then do FlatLightLaf.setup(); before creating swing components or UIManager.setLookAndFeel(FlatLightLaf())
    // You can also force FlatLaf Light theme only with dependency (class com.formdev.flatlaf.FlatLightLaf)
    // or you can force FlatLaf Dark theme only with dependency (class com.formdev.flatlaf.FlatDarkLaf)
    implementation("org.commonmark:commonmark:0.22.0")
    implementation("org.aspectj:aspectjrt:1.9.19") // Runtime library for AspectJ
    implementation("org.springframework.boot:spring-boot-starter-aop") // Spring AOP starter (includes aspectjweaver)

    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("io.mockk:mockk-agent-jvm:1.13.11")

    testImplementation("io.projectreactor:reactor-test")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
//
// Configure ktlint
ktlint {
    version.set("1.3.1")
    android.set(false) // Set to true if this is an Android project
    outputToConsole.set(true)
    enableExperimentalRules.set(true)
    ignoreFailures.set(true)
    filter {
        exclude("**/generated/**")
        include("**/src/main/kotlin/**")
    }
    reporters {
        // reporter(ReporterType.PLAIN) // Plain text format
        reporter(ReporterType.JSON) // JSON format
        reporter(ReporterType.CHECKSTYLE) // Checkstyle XML format
        reporter(ReporterType.HTML) // HTML report for viewing in a browser
        reporter(ReporterType.SARIF) // SARIF format for security tools
        reporter(ReporterType.PLAIN_GROUP_BY_FILE) // Baseline format to ignore existing issues
    }
}

// detekt {
// 	toolVersion = "1.23.6"
// 	config.setFrom(file("config/detekt/detekt.yml"))
// 	buildUponDefaultConfig = true
// }
//
//
// // Kotlin DSL
// tasks.withType<Detekt>().configureEach {
// 	reports {
// 		xml.required.set(true)
// 		html.required.set(true)
// 		txt.required.set(true)
// 		sarif.required.set(true)
// 		md.required.set(true)
// 	}
// }

// Configure GraalVM Native plugin
graalvmNative {
    binaries {
        named("main") {
            mainClass.set("tech.robd.robokey.MainKt")
            buildArgs.add("--no-fallback")
            imageName.set("robokey-app") // Set the output name here
        }
    }
}

tasks.test {
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    useJUnitPlatform()
    maxParallelForks = 1
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Define the source set for integration tests
sourceSets {
    create("integrationTest") {
        kotlin.srcDir("src/integrationTest/kotlin")
        resources.srcDir("src/integrationTest/resources")
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
}

configurations {
    named("integrationTestImplementation") {
        extendsFrom(configurations["testImplementation"])
    }
    named("integrationTestRuntimeOnly") {
        extendsFrom(configurations["testRuntimeOnly"])
    }
}

// Define the integrationTest task
val integrationTest by tasks.creating(Test::class) {
    description = "Runs integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter("test") // Run integration tests after unit tests
}

tasks.check {
    dependsOn(integrationTest)
}
