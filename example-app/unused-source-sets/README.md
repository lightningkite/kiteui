Source sets the Gradle build never compiled either: `jvmMain` and `wasmJsMain` have no matching
Kotlin target in `build.gradle.kts`, and `commonTest/AndroidManifest.xml` was shadowed by
`androidUnitTest/AndroidManifest.xml`. Parked here rather than deleted; deleting them is a
separate decision from the build-tool move.
