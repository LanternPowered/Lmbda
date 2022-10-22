plugins {
  java
  idea
  eclipse
  signing
  `maven-publish`
  id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
  kotlin("jvm") version "1.6.0"
  id("me.champeau.jmh") version "0.6.6"
  id("org.cadixdev.licenser") version "0.6.1"
}

group = "org.lanternpowered"
version = "3.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  val guavaVersion = "31.0.1-jre"
  implementation(group = "org.ow2.asm", name = "asm", version = "9.4")
  implementation(group = "org.checkerframework", name = "checker-qual" , version = "3.25.0")
  compileOnly(kotlin("stdlib-jdk8"))
  compileOnly(kotlin("reflect"))
  compileOnly(group = "com.google.guava", name = "guava", version = guavaVersion)
  testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.9.0")
  testImplementation(kotlin("stdlib-jdk8"))
  testImplementation(kotlin("reflect"))
  testImplementation(group = "com.google.guava", name = "guava", version = guavaVersion)
}

defaultTasks("licenseFormat", "build")

java {
  base.archivesName.set(project.name.toLowerCase())
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

jmh {
  duplicateClassesStrategy.set(DuplicatesStrategy.WARN)
}

tasks {
  val javadocJar = create<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(javadoc)
  }

  val sourceJar = create<Jar>("sourceJar") {
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

  val jars = listOf(jar.get(), sourceJar, javadocJar)
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
      artifactId = project.name.toLowerCase()
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
