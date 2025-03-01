import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  java
  idea
  eclipse
  signing
  `maven-publish`
  id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
  kotlin("jvm") version libs.versions.kotlin.core
  id("me.champeau.jmh") version libs.versions.jmh
  id("org.cadixdev.licenser") version libs.versions.licenser
  id("com.adarshr.test-logger") version libs.versions.testlogger
}

group = "org.lanternpowered"
version = "3.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation(libs.asm)
  implementation(libs.checkerqual)
  compileOnly(kotlin("stdlib-jdk8"))
  compileOnly(kotlin("reflect"))
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testImplementation(kotlin("stdlib-jdk8"))
  testImplementation(kotlin("reflect"))
  testImplementation(libs.kotlin.coroutines)
}

defaultTasks("licenseFormat", "build")

java {
  base.archivesName.set(project.name.lowercase())
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
  target {
    compilerOptions.jvmTarget = JvmTarget.JVM_1_8
  }
}

jmh {
  duplicateClassesStrategy.set(DuplicatesStrategy.WARN)
}

tasks {
  val javadocJar = register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(javadoc)
  }

  val sourceJar = register<Jar>("sourceJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
    exclude("**/*.class") // For module-info.class
  }

  jar {
    exclude("module-info.java")
  }

  assemble {
    dependsOn(sourceJar)
    dependsOn(javadocJar)
  }

  val jars = listOf(jar.get(), sourceJar.get(), javadocJar.get())
  jars.forEach { jar ->
    jar.from(project.file("LICENSE.txt"))
  }

  artifacts {
    jars.forEach { jar -> archives(jar) }
  }

  test {
    useJUnitPlatform()
  }
}

if (project.hasProperty("sonatypeUsername")) {
  nexusPublishing {
    repositories {
      sonatype()
    }
  }
}

publishing {
  repositories {
    maven {
      val releasesRepoUrl = layout.buildDirectory.dir("repos/releases")
      val snapshotsRepoUrl = layout.buildDirectory.dir("repos/snapshots")
      val snapshot = project.version.toString().endsWith("-SNAPSHOT")
      url = uri(if (snapshot) snapshotsRepoUrl else releasesRepoUrl)
    }
  }
  publications {
    create<MavenPublication>("maven") {
      groupId = project.group.toString()
      artifactId = project.name.lowercase()
      version = project.version.toString()

      from(components["java"])
      artifact(tasks["javadocJar"])
      artifact(tasks["sourceJar"])

      pom {
        name.set(project.name)
        description.set("A lambda generation library")
        url.set("https://github.com/LanternPowered/Lmbda")
        inceptionYear.set("2018")
        licenses {
          license {
            name.set("MIT License")
            url.set("https://opensource.org/licenses/MIT")
          }
        }
        developers {
          developer {
            id.set("Cybermaxke")
            name.set("Seppe Volkaerts")
            email.set("contact@seppevolkaerts.be")
          }
        }
        issueManagement {
          system.set("GitHub Issues")
          url.set("https://github.com/LanternPowered/Lmbda/issues")
        }
        scm {
          connection.set("scm:git@github.com:LanternPowered/Lmbda.git")
          developerConnection.set("scm:git@github.com:LanternPowered/Lmbda.git")
          url.set("https://github.com/LanternPowered/Lmbda")
        }
      }
    }
  }
}

signing {
  val signingKey = project.findProperty("signingKey")?.toString()
  val signingPassword = project.findProperty("signingPassword")?.toString()
  if (signingKey != null && signingPassword != null) {
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["maven"])
  }
}

license {
  header(rootProject.file("HEADER.txt"))
  newLine(false)
  ignoreFailures(false)

  include("**/*.java")
  include("**/*.kt")

  ext {
    set("name", rootProject.name)
    set("url", "https://www.lanternpowered.org")
    set("organization", "LanternPowered")
  }
}

testlogger {
  theme = com.adarshr.gradle.testlogger.theme.ThemeType.PLAIN
}
