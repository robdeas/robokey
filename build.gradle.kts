import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.3.5"
	id("io.spring.dependency-management") version "1.1.6"

	kotlin("jvm") version "1.9.23"
	kotlin("plugin.spring") version "1.9.23"
	id("org.graalvm.buildtools.native") version "0.10.3" // GraalVM plugin

//	id("io.kotest") version "0.4.11"
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
	implementation("com.formdev:flatlaf:3.4.1") // then do FlatLightLaf.setup(); before creating swing components or UIManager.setLookAndFeel(FlatLightLaf())
	// You can also force FlatLaf Light theme only with dependency (class com.formdev.flatlaf.FlatLightLaf)
	// or you can force FlatLaf Dark  theme only with dependency (class com.formdev.flatlaf.FlatDarkLaf)
	implementation("org.commonmark:commonmark:0.22.0")

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
	jvmArgs ("-XX:+EnableDynamicAgentLoading")
	useJUnitPlatform()
	maxParallelForks = 1
}

tasks.withType<Test> {
	useJUnitPlatform()
}

