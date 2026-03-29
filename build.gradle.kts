import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jreleaser.model.Active

plugins {
  java
  `maven-publish`
  id("org.jreleaser") version "1.23.0"
  kotlin("jvm") version libs.versions.kotlin.core
  id("me.champeau.jmh") version libs.versions.jmh
  id("org.cadixdev.licenser") version libs.versions.licenser
  id("com.adarshr.test-logger") version libs.versions.testlogger
}

group = "org.lanternpowered"
version = "3.0.1-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation(libs.asm)
  implementation(libs.jspecify)
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
    archiveClassifier = "javadoc"
    from(javadoc)
  }

  val sourceJar = register<Jar>("sourceJar") {
    archiveClassifier = "sources"
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

publishing {
  repositories {
    maven {
      url = uri(layout.buildDirectory.dir("staging-deploy").get())
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
        name = project.name
        description = "A lambda generation library"
        url = "https://github.com/LanternPowered/Lmbda"
        inceptionYear = "2018"
        licenses {
          license {
            name = "MIT License"
            url = "https://opensource.org/licenses/MIT"
          }
        }
        developers {
          developer {
            id = "Cybermaxke"
            name = "Seppe Volkaerts"
            email = "contact@seppevolkaerts.be"
          }
        }
        issueManagement {
          system = "GitHub Issues"
          url = "https://github.com/LanternPowered/Lmbda/issues"
        }
        scm {
          connection = "scm:git@github.com:LanternPowered/Lmbda.git"
          developerConnection = "scm:git@github.com:LanternPowered/Lmbda.git"
          url = "https://github.com/LanternPowered/Lmbda"
        }
      }
    }
  }
}

jreleaser {
  signing {
    active = Active.ALWAYS
    armored = true
  }
  deploy {
    maven {
      mavenCentral {
        create("sonatype") {
          active = Active.RELEASE
          url = "https://central.sonatype.com/api/v1/publisher"
          stagingRepository("build/staging-deploy")
          applyMavenCentralRules = true
        }
      }
      nexus2 {
        create("sonatype-snapshots") {
          active = Active.SNAPSHOT
          url = "https://central.sonatype.com/repository/maven-snapshots/"
          snapshotUrl = url
          snapshotSupported = true
          stagingRepository("build/staging-deploy")
          applyMavenCentralRules = true
        }
      }
    }
  }
  val snapshot = project.version.get().endsWith("-SNAPSHOT")
  release {
    github {
      skipRelease = snapshot
      skipTag = snapshot
    }
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
