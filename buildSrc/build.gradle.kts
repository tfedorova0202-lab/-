plugins {
  `kotlin-dsl`
}

dependencies {
  implementation("com.android.tools.build:aapt2-proto:9.1.1-14792394")
  implementation("com.google.protobuf:protobuf-java:3.25.5")
  implementation("com.google.code.gson:gson:2.11.0")

  testImplementation("junit:junit:4.13.2")
}

tasks.withType<Test> {
  testLogging {
    events("passed", "failed", "skipped")
  }
}
