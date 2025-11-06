import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.0"
    id("jacoco")

}

group = "org.example"
//version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

}



dependencies {
    implementation("guru.nidi:graphviz-java:0.18.1")
    //implementation("org.graalvm.js:js:23.1.0")
    implementation("org.graalvm.sdk:graal-sdk:23.1.0")
    implementation("guru.nidi:graphviz-kotlin:0.18.1")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")


   // implementation("org.jetbrains.kotlin.formver:formver.annotations:0.1.0-SNAPSHOT")
   // implementation("org.jetbrains.kotlin.formver:formver.common:0.1.0-SNAPSHOT")
   // implementation("org.jetbrains.kotlin.formver:formver.compiler-plugin:0.1.0-SNAPSHOT")

}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}
kotlin {
    //jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xnested-type-aliases"
        )
        //  jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        //}
    }
}



java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}


jacoco { toolVersion = "0.8.12" }

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}


