plugins {
  java
  idea
  eclipse
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
  implementation(group = "org.ow2.asm", name = "asm", version = "9.2")
  implementation(group = "org.checkerframework", name = "checker-qual" , version = "2.8.1")
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))
  implementation(group = "com.google.guava", name = "guava", version = "31.0.1-jre")
  testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.8.2")
}

java {
  base.archivesName.set(project.name.toLowerCase())
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

jmh {
  duplicateClassesStrategy.set(DuplicatesStrategy.WARN)
}

tasks {
  test {
    useJUnitPlatform()
  }

  val license = file("LICENSE.txt")

  jar {
    from(license)
  }

  val sourcesJar by creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
    from(license)
  }

  val javadocJar by creating(Jar::class) {
    dependsOn(javadoc)
    archiveClassifier.set("javadoc")
    from(javadoc)
    from(license)
  }

  artifacts {
    archives(sourcesJar)
    archives(javadocJar)
    archives(jar)
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
