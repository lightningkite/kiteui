# Gradle Plugin Review - KiteUI

**Date:** 2025-11-08  
**Plugin ID:** `com.lightningkite.kiteui`  
**Gradle Version:** 8.14.3  
**Configuration Cache:** Disabled (project setting)

---

## Executive Summary

The KiteUI Gradle plugin provides essential build automation for the KiteUI multiplatform framework, handling resource generation, route generation, and platform-specific configuration. While the plugin is functional, it has significant issues with configuration cache compatibility, build performance, error handling, and adherence to Gradle best practices. The codebase would benefit from modernization to use the Task Configuration Avoidance API, proper incremental builds, and better separation of concerns.

**Overall Grade:** C+ (Functional but needs modernization)

---

## Plugin Overview

### Core Functionality

The plugin provides five main features:

1. **Resource Management** - Converts common resources (fonts, images, videos, audio, SVGs) into platform-specific formats
   - Common: Generates expect declarations
   - JS/Web: Copies resources to public folders with web-compatible paths
   - Android: Generates Android resource files (drawable, raw, font XML)
   - iOS: Creates Xcode asset catalogs and updates Info.plist
   - JVM: Similar to JS approach

2. **Auto-routing** - Scans Kotlin source files for `@Routable` annotations and generates routing code
   - Parses route patterns with path parameters and query parameters
   - Generates type-safe route parsers and renderers

3. **Localization Generation** - Scans for string literals and generates localization interfaces (appears incomplete)

4. **Version Synchronization** - Syncs version codes/names between Android and iOS/JS

5. **Source Set Configuration** - Adds generated source directories to Kotlin compilation

### Plugin Extension

```kotlin
interface KiteUiPluginExtension {
    var packageName: String
    var iosProjectRoot: File
}
```

Simple configuration with only two required properties.

---

## Critical Issues

### 1. Configuration Cache Incompatibility ⚠️ **HIGH PRIORITY**

**Status:** The project explicitly disables configuration cache (`org.gradle.configuration-cache=false`)

**Issues:**
- **Direct file access during configuration:** Lines 50-84, 103-111, 134-149, 178-186
  ```kotlin
  val resourceFolder = project.file("src/commonMain/resources")
  task.inputs.files(resourceFolder)  // File access during task registration
  ```

- **Task execution logic in configuration phase:** Using `afterEvaluate` doesn't make it configuration cache safe
  ```kotlin
  afterEvaluate {
      if (ext.packageName == null)  // Property access during configuration
          throw IllegalArgumentException(...)
  }
  ```

- **Reflection usage without serialization:** Line 34
  ```kotlin
  val kotlinSourceSets = kotlinExtension.javaClass.getMethod("getSourceSets").invoke(kotlinExtension)
  ```

- **GroovyObject casting:** Lines 224, 264 - Not serializable
  ```kotlin
  project.extensions.findByName("android")?.groovyObject?.getPropertyAsObject("defaultConfig")
  ```

**Impact:**
- Cannot use configuration cache (significant performance penalty)
- Slower builds, especially for incremental builds
- Not ready for Gradle 9.0+ where configuration cache will be mandatory

**Recommendation:**
- Use `Provider<T>` and `Property<T>` APIs throughout
- Move all file access to task execution phase
- Use proper Gradle APIs instead of reflection
- Replace GroovyObject with typed Android Gradle Plugin APIs

---

### 2. Task Configuration API Violations ⚠️ **HIGH PRIORITY**

**Problem:** Using eager task registration API (`tasks.register().apply { val task = this.get() }`)

**Examples:**
- Lines 47-62: `tasks.register("kiteuiResourcesCommon", Task::class.java).apply { val task = this.get() }`
- Lines 64-93: Multiple tasks using `.get()` immediately
- Lines 95-111, 112-128, 130-171, 173-191

**Issues:**
- `.get()` realizes the task immediately, defeating lazy configuration
- Configures tasks even when they won't be executed
- Significantly slower configuration time
- Against Gradle best practices since Gradle 4.9 (2018)

**Impact:**
- Slow configuration phase
- Unnecessary work when running unrelated tasks
- Poor developer experience

**Correct Pattern:**
```kotlin
tasks.register("kiteuiResourcesCommon") {
    group = "kiteui"
    val resourceFolder = project.layout.projectDirectory.dir("src/commonMain/resources")
    inputs.dir(resourceFolder)
    val out = project.layout.buildDirectory.file("generated/kiteui-common/Resources.kt")
    outputs.file(out)
    doLast {
        // execution logic
    }
}
```

---

### 3. Missing Incremental Build Support ⚠️ **MEDIUM PRIORITY**

**Problem:** Tasks don't properly declare inputs/outputs or use Gradle's incremental task APIs

**Issues:**

1. **Missing @Input annotations on task properties**
   - Extension properties not declared as task inputs
   - Plugin behavior not tracked

2. **Insufficient input tracking:**
   ```kotlin
   task.inputs.files(resourceFolder)  // Good
   // But missing: task inputs for ext.packageName, ext.iosProjectRoot
   ```

3. **No incremental task actions:**
   - `doLast` blocks run fully every time
   - No use of `@Incremental` or `InputChanges`
   - Full file tree scanning on every run

4. **Auto-routes task doesn't track parsing logic:**
   - Line 203-218: Scans all Kotlin files every time
   - Should use Gradle's input change detection

**Impact:**
- Tasks run when they don't need to
- Wasted build time
- Poor incremental build performance

**Recommendations:**
- Use `@InputDirectory`, `@OutputDirectory`, `@Input` annotations
- Create proper task classes extending `DefaultTask`
- Implement incremental processing with `InputChanges`
- Use Gradle's file tree comparison

---

### 4. Error Handling Deficiencies ⚠️ **MEDIUM PRIORITY**

**Issues:**

1. **Silent failures:**
   ```kotlin
   // Line 55-57
   if (resourceFolder.listFiles()?.isNotEmpty() == true) {
       resourcesCommon(resourceFolder, out, ext)
   }
   // Silently does nothing if folder doesn't exist
   ```

2. **Poor validation messages:**
   ```kotlin
   // Lines 24-27
   if (ext.packageName == null)
       throw IllegalArgumentException("KiteUiPluginExtension property packageName is null...")
   // Better: Provide examples of how to configure
   ```

3. **Swallowed exceptions:**
   ```kotlin
   // generateLocalizations.kt lines 12-17
   try {
       it.readText().localizer(localizations)
   } catch (e: Exception) {
       println("WARNING: Could not parse $it")
       e.printStackTrace()
   }
   // Continues despite parse errors; should fail or collect errors
   ```

4. **Reflection without fallbacks:**
   ```kotlin
   // Line 34
   val kotlinSourceSets = kotlinExtension.javaClass.getMethod("getSourceSets").invoke(kotlinExtension)
   // Will throw if method signature changes
   ```

5. **File operations without validation:**
   ```kotlin
   // resourcesPlatforms.kt line 118-121
   f.source.copyTo(i, overwrite = true)
   // No check if source exists or is readable
   ```

6. **No build failure on resource processing errors**
   - Resources.kt parsing errors don't fail the build
   - SVG parsing can throw (line 358) without graceful handling

**Recommendations:**
- Use Gradle's problem reporting API
- Validate inputs early with clear error messages
- Fail fast with actionable guidance
- Add try-catch around file operations with proper error propagation
- Use `GradleException` with user-friendly messages

---

### 5. String-Based Source Parsing ⚠️ **HIGH PRIORITY**

**Problem:** Custom regex-based Kotlin source parsing instead of proper AST parsing

**Files:**
- `generateRoutes.kt` (228 lines of regex parsing)
- `resources.kt` (localization parsing with 285 lines of string manipulation)
- `parsingHelpers.kt` (manual parenthesis matching)

**Issues:**

1. **Fragile parsing:**
   ```kotlin
   // Line 21-22, generateRoutes.kt
   val text = it.readLines().map { it.trim() }.filter { !it.startsWith("//") }.joinToString("\n")
       .replace(blockComment, "")
   ```
   - Removes comments but doesn't handle string literals containing "//"
   - Block comment regex (line 13) doesn't handle nested comments correctly
   - Can break on valid Kotlin code

2. **No syntax validation:**
   - Parser assumes well-formed code
   - No error recovery
   - Cryptic failures on malformed input

3. **Limited feature support:**
   ```kotlin
   // Lines 48-49
   val name = text.substring(nameStart, text.indexOf(nameStart, ' ', '(', ':', '<')).trim().trim(':')
   ```
   - Won't handle annotations on classes
   - Won't handle complex generic types
   - Breaks on comments between class/object and name

4. **Maintenance burden:**
   - Custom parser needs updating for new Kotlin features
   - Hard to debug when parsing fails
   - Brittle against code formatting changes

**Better Approaches:**

1. **Use KSP (Kotlin Symbol Processing):**
   ```kotlin
   // Let KSP handle the parsing, generate routes from symbols
   @AutoInject
   class RoutableProcessor : SymbolProcessor {
       // Proper AST traversal
   }
   ```

2. **Use Kotlin compiler APIs:**
   - PSI (Program Structure Interface)
   - More robust, maintained by JetBrains
   - Handles all Kotlin syntax correctly

3. **Code generation via KSP plugin:**
   - Move route generation to a KSP processor
   - Let Gradle plugin just configure KSP
   - Cleaner separation of concerns

**Impact:**
- Current approach will break on valid Kotlin code
- Hard to maintain and extend
- Generates incorrect routes silently

---

### 6. Performance Issues ⚠️ **MEDIUM PRIORITY**

**Problems:**

1. **Walks entire source tree multiple times:**
   ```kotlin
   // generateRoutes.kt line 17
   sources.walkTopDown().filter { it.extension == "kt" }
   
   // generateLocalizations.kt lines 8-11
   toRead.filterNotNull().asSequence()
       .flatMap { it.walkTopDown() }
       .filter { it.extension == "kt" && it.isFile }
   ```
   - Both tasks scan all .kt files independently
   - No caching of parse results

2. **Reads files multiple times:**
   ```kotlin
   // generateLocalizations.kt
   .forEach { it.readText().localizer(localizations) }  // Line 13
   // ...later...
   .forEach { it.writeText(it.readText().applyLocalizations(...)) }  // Line 44
   ```
   - Reads every file twice
   - Modifies source files in place (!!)

3. **Font parsing on every build:**
   ```kotlin
   // resources.kt lines 70-83
   val font = when (relativeFile.extension) {
       "otf" -> OTFParser().parseEmbedded(file.inputStream())
       "ttf" -> TTFParser().parseEmbedded(file.inputStream())
   }
   ```
   - Parses font metadata every time, even if unchanged
   - Should cache based on file hash

4. **Unnecessary string concatenation:**
   ```kotlin
   // resources.kt line 293
   val name: String = "${content.zip(args) { a, b -> "$a ${b}" }.joinToString("")}${content.last()}"...
   ```
   - Complex string building in data class default parameter
   - Computed on every instance creation

5. **No parallel processing:**
   - Sequential file processing
   - Could parallelize resource conversion
   - Could parallelize source scanning

**Recommendations:**
- Cache parsing results between builds
- Use build cache for generated files
- Process files in parallel
- Avoid redundant file reads
- Don't modify source files (major issue!)

---

### 7. Source File Modification ⚠️ **CRITICAL**

**Problem:** Plugin modifies source files in place

```kotlin
// generateLocalizations.kt lines 40-46
toRead.filterNotNull().asSequence()
    .flatMap { it.walkTopDown() }
    .filter { it.extension == "kt" && it.isFile }
    .forEach {
        it.writeText(it.readText().applyLocalizations(ext.packageName, localizations))
    }
```

**Issues:**
- Modifies files in `src/` directories
- No backup or recovery mechanism
- Can cause version control conflicts
- Violates principle of immutable sources
- Can corrupt files if build is interrupted
- Makes builds non-reproducible

**Impact:**
- **CRITICAL:** This is a serious anti-pattern
- Can lose code changes
- Breaks incremental compilation
- Causes confusion with VCS

**Recommendation:**
- **Remove this feature entirely** or
- Generate wrapper files in `build/generated` instead
- Never modify source files from build scripts

---

## Code Quality Issues

### 8. Deprecated API Usage

**Problems:**

1. **String case methods:**
   ```kotlin
   // resources.kt lines 350-356
   internal fun String.capitalize() = ...  // Deprecated
   internal fun String.decapitalize() = ... // Deprecated
   ```
   - Should use `replaceFirstChar { it.uppercase() }`

2. **Groovy interop:**
   ```kotlin
   // Lines 290-291
   internal val Any?.groovyObject: GroovyObject? get() = this as? GroovyObject
   internal fun GroovyObject.getPropertyAsObject(key: String): GroovyObject?
   ```
   - Should use typed Android Gradle Plugin API

### 9. Lack of Documentation

**Issues:**
- No KDoc on public functions
- No README in gradle-plugin directory
- Extension properties have no description
- No examples of plugin usage
- Tasks have minimal descriptions

**Example:**
```kotlin
interface KiteUiPluginExtension {
    var packageName: String  // What format? What's it used for?
    var iosProjectRoot: File  // Relative to what? Must exist?
}
```

**Recommendations:**
- Add KDoc to all public APIs
- Document expected file structures
- Provide usage examples
- Document task dependencies

### 10. Inconsistent Naming and Organization

**Issues:**

1. **Task naming inconsistency:**
   - `kiteuiResourcesCommon` (camelCase with prefix)
   - `generateAutoRoutes` (camelCase without prefix)
   - `syncVersionsIos` (camelCase with suffix)

2. **File organization:**
   - `resources.kt` has localization code mixed with resource code
   - `resourcesPlatforms.kt` has platform-specific code but mixed responsibilities
   - `parsingHelpers.kt` used only by route generation

3. **Magic strings:**
   ```kotlin
   // Line 39
   if (it.name.endsWith("Main")) {
       it.kotlin.srcDir("build/generated/kiteui-${it.name.removeSuffix("Main")}")
   }
   ```
   - Should use constants

**Recommendations:**
- Consistent task naming: `kiteui<Feature><Platform>`
- Separate files by concern
- Use constants for magic values
- Better package structure

### 11. Insufficient Testing

**Current State:**
- Only one test file: `ParsingHelpersKtTest.kt`
- Tests only `splitParens()` function
- No tests for:
  - Resource generation
  - Route generation
  - Localization
  - Platform-specific outputs
  - Error cases

**Recommendations:**
- Add Gradle TestKit tests for plugin behavior
- Test each task independently
- Test error conditions
- Test incremental builds
- Add integration tests with sample projects

### 12. Hard-coded Assumptions

**Examples:**

1. **Fixed directory structure:**
   ```kotlin
   val sources = project.file("src/commonMain/kotlin")  // Line 205
   ```
   - Assumes standard structure
   - Won't work with custom source sets

2. **Platform detection by naming:**
   ```kotlin
   if (it.name.endsWith("Main"))  // Line 38
   ```
   - Fragile, breaks with custom source set names

3. **Fixed resource paths:**
   ```kotlin
   task.from("src/commonMain/resources")  // Line 68
   ```
   - Should use Kotlin source set API

**Recommendations:**
- Use Gradle/Kotlin APIs to discover source sets
- Make paths configurable
- Support non-standard project structures

---

## Specific File Reviews

### KiteUiPlugin.kt

**Strengths:**
- Clear plugin structure
- Well-organized task registration
- Good task grouping

**Issues:**
- Eager task registration (`.get()`)
- Reflection-based Kotlin extension access
- Configuration cache incompatible
- Missing task classes (all inline lambdas)
- No validation of inputs
- GroovyObject usage for Android integration

**Recommendations:**
- Create dedicated task classes in separate files
- Use Gradle's Kotlin DSL properly
- Replace reflection with Kotlin Gradle Plugin API
- Add input validation with clear messages
- Use AGP typed API instead of Groovy

### generateRoutes.kt

**Strengths:**
- Generates type-safe routing code
- Handles path and query parameters
- Clean output code generation

**Issues:**
- Fragile regex-based parsing (HIGH RISK)
- No validation of route patterns
- Doesn't handle complex Kotlin syntax
- Single-pass parsing (no error recovery)
- No caching of parsed routes
- TabAppendable is reinventing the wheel (use kotlinpoet)

**Recommendations:**
- Switch to KSP-based generation
- Use KotlinPoet for code generation
- Add route pattern validation
- Cache parsing results
- Better error messages when parsing fails

### resources.kt

**Strengths:**
- Smart font family detection
- Comprehensive resource type support
- SVG parsing for ImageVector

**Issues:**
- Mixes localization and resource concerns
- Extremely complex string parsing for localization (285 lines)
- Modifies source files (CRITICAL)
- Deprecated string methods
- No error handling for font/SVG parsing
- Localization feature seems half-implemented

**Recommendations:**
- Split into separate files
- Remove or complete localization feature
- Use proper Kotlin parser for string extraction
- Never modify source files
- Add error handling for resource parsing
- Consider using existing i18n solutions

### resourcesPlatforms.kt

**Strengths:**
- Handles platform quirks well
- Good iOS/Android resource generation
- Comprehensive SVG to ImageVector conversion

**Issues:**
- Very long (421 lines) - should be split
- No error handling on file operations
- Modifies Info.plist with fragile string replacement
- Hard-coded XML templates
- SVG parser makes assumptions about structure

**Recommendations:**
- Split into one file per platform
- Use XML DOM properly for plist modification
- Validate SVG structure before parsing
- Add error handling and validation
- Use XML builders instead of string templates

### parsingHelpers.kt

**Strengths:**
- Clean implementation
- Has tests

**Issues:**
- Only used in one place (generateRoutes.kt)
- Reinventing lexer functionality
- No error messages on malformed input

**Recommendations:**
- Inline into generateRoutes.kt or
- Expand to full expression parser or
- Replace with proper parser library

---

## Architecture Recommendations

### 1. Separate Plugin and Processors

**Current:** All generation in Gradle tasks  
**Better:** Move generation to KSP processors

```
kiteui-gradle-plugin/
  - Configures KSP
  - Handles resources
  - Minimal code generation

kiteui-ksp/
  - Route generation processor
  - Localization processor
  - Proper symbol-based generation
```

**Benefits:**
- Leverages Kotlin compiler infrastructure
- Better error messages
- Incremental annotation processing
- Easier to test and maintain

### 2. Create Dedicated Task Classes

**Current:** All tasks as inline lambdas  
**Better:** Proper task classes with typed inputs/outputs

```kotlin
abstract class GenerateResourcesTask : DefaultTask() {
    @InputDirectory
    abstract val resourceDirectory: DirectoryProperty
    
    @Input
    abstract val packageName: Property<String>
    
    @OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun generate() {
        // Implementation
    }
}
```

**Benefits:**
- Configuration cache compatible
- Proper input/output tracking
- Easier to test
- Better IDE support

### 3. Improve Extension Configuration

**Current:**
```kotlin
interface KiteUiPluginExtension {
    var packageName: String
    var iosProjectRoot: File
}
```

**Better:**
```kotlin
abstract class KiteUiPluginExtension @Inject constructor(
    objects: ObjectFactory
) {
    @get:Input
    abstract val packageName: Property<String>
    
    @get:InputDirectory
    abstract val iosProjectRoot: DirectoryProperty
    
    @get:Input
    val generateLocalization: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)
}
```

**Benefits:**
- Configuration cache safe
- Lazy evaluation
- Default values
- Validation support

### 4. Add Build Cache Support

```kotlin
@CacheableTask
abstract class GenerateResourcesTask : DefaultTask() {
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputDirectory
    abstract val resourceDirectory: DirectoryProperty
    
    // Task implementation
}
```

**Benefits:**
- Reuse outputs across machines
- Faster CI builds
- Better incremental builds

---

## Performance Improvement Plan

### Phase 1: Quick Wins
1. Add `@CacheableTask` to tasks
2. Fix task configuration to avoid `.get()`
3. Remove duplicate file scanning
4. Cache font parsing results

**Estimated Impact:** 20-30% faster builds

### Phase 2: Incremental Support
1. Implement `InputChanges` for file scanning
2. Create proper task classes
3. Add parallel processing

**Estimated Impact:** 40-60% faster incremental builds

### Phase 3: Architecture Change
1. Move to KSP for code generation
2. Implement build cache
3. Configuration cache compatibility

**Estimated Impact:** 70-90% faster clean builds with cache

---

## Migration to Configuration Cache

### Required Changes

1. **Use Provider API:**
```kotlin
// Bad
val resourceFolder = project.file("src/commonMain/resources")

// Good
val resourceFolder: Provider<Directory> = project.layout.projectDirectory.dir("src/commonMain/resources")
```

2. **Lazy task configuration:**
```kotlin
// Bad
tasks.register("myTask").apply {
    val task = this.get()
    task.doLast { ... }
}

// Good
tasks.register<MyTask>("myTask") {
    resourceDir.set(layout.projectDirectory.dir("src/commonMain/resources"))
}
```

3. **Remove reflection:**
```kotlin
// Bad
val kotlinSourceSets = kotlinExtension.javaClass.getMethod("getSourceSets")...

// Good
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
kotlin.sourceSets.matching { it.name.endsWith("Main") }
```

4. **Typed AGP access:**
```kotlin
// Bad
project.extensions.findByName("android")?.groovyObject

// Good
import com.android.build.api.dsl.ApplicationExtension
extensions.findByType<ApplicationExtension>()?.defaultConfig?.versionName
```

---

## Security Concerns

### 1. File System Access
- Plugin has unrestricted file system access
- Could read/write anywhere (by design for Gradle plugins)
- No validation of user-provided paths

**Mitigation:**
- Validate all file paths are within project directory
- Use Gradle's file resolution APIs

### 2. Code Injection Risk
- Generates Kotlin code from parsed sources
- No sanitization of extracted strings
- Could inject malicious code if parsing is exploited

**Example:**
```kotlin
// If route parsing finds malicious input:
@Routable("test")
object MyPage  // Followed by: "); System.exit(0); println("

// Generated code could contain injection
```

**Mitigation:**
- Use KotlinPoet which escapes properly
- Validate all extracted identifiers
- Sanitize string interpolation

### 3. XML Parsing (XXE)
```kotlin
// Line 350, resourcesPlatforms.kt
val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
```

- No XXE protection configured
- Could be exploited with malicious SVG files

**Mitigation:**
```kotlin
val factory = DocumentBuilderFactory.newInstance().apply {
    setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    setFeature("http://xml.org/sax/features/external-general-entities", false)
    setFeature("http://xml.org/sax/features/external-parameter-entities", false)
}
```

---

## Compatibility Analysis

### Gradle Version Compatibility
- **Current:** 8.14.3
- **Minimum supported:** Likely 7.0+ (not documented)
- **Configuration cache:** ❌ Not compatible
- **Build cache:** ⚠️ Partially (tasks not marked `@CacheableTask`)

### Kotlin Gradle Plugin Compatibility
- Uses reflection to access KGP internals
- Likely breaks with KGP version changes
- Should use public KGP APIs

### Android Gradle Plugin Compatibility
- Uses GroovyObject casting
- Fragile against AGP updates
- Should use typed DSL (AGP 7.0+)

---

## Testing Recommendations

### Unit Tests Needed
- [ ] Route pattern parsing
- [ ] Resource type detection
- [ ] Font family grouping
- [ ] SVG parsing
- [ ] Case conversion utilities
- [ ] Path manipulation helpers

### Integration Tests Needed
- [ ] Full plugin application
- [ ] Resource generation for each platform
- [ ] Route generation with various patterns
- [ ] Incremental build behavior
- [ ] Clean build behavior
- [ ] Error handling scenarios

### Test Structure
```
src/test/
  kotlin/
    unit/
      ParsingHelpersTest.kt ✓ (exists)
      ResourceParsingTest.kt
      RouteParsingTest.kt
      CaseConversionTest.kt
    integration/
      PluginApplicationTest.kt
      ResourceGenerationTest.kt
      IncrementalBuildTest.kt
      ConfigurationCacheTest.kt
```

---

## Documentation Gaps

### Missing Documentation
1. **README.md** - How to use the plugin
2. **Task documentation** - What each task does
3. **Extension configuration** - All available options
4. **File structure requirements** - Expected project layout
5. **Platform-specific behavior** - Differences per platform
6. **Troubleshooting guide** - Common issues
7. **Migration guide** - Version upgrade notes
8. **API documentation** - KDoc for public APIs

### Recommended Documentation

```markdown
# KiteUI Gradle Plugin

## Installation
...

## Configuration
```groovy
kiteui {
    packageName = "com.example.app"  // Required: Root package for generated code
    iosProjectRoot = file("../ios-app/App")  // Required: Path to Xcode project folder
}
```

## Tasks
- `kiteuiResourcesAll` - Generate all platform resources
- `generateAutoRoutes` - Generate routing code from @Routable annotations
...

## Resource Structure
Place resources in `src/commonMain/resources/`:
...
```

---

## Priority Action Items

### Critical (Fix Immediately)
1. ❌ **Remove source file modification** in localization feature
2. ⚠️ **Add error handling** for file operations
3. 🔒 **Fix XXE vulnerability** in SVG parsing

### High Priority (Next Sprint)
4. 🚀 **Fix eager task registration** (use lazy API)
5. ⚡ **Remove duplicate file scanning**
6. 📝 **Add basic documentation** (README, task descriptions)
7. 🔧 **Replace reflection** with typed APIs

### Medium Priority (Next Month)
8. 🏗️ **Create dedicated task classes**
9. ✅ **Add integration tests**
10. 📦 **Add build cache support** (`@CacheableTask`)
11. 🔄 **Implement incremental builds** properly

### Low Priority (Future)
12. 🎯 **Configuration cache compatibility**
13. 🏛️ **Migrate to KSP** for code generation
14. 📊 **Performance optimization** (parallel processing)
15. 🧹 **Code cleanup** (deprecated APIs, magic strings)

---

## Conclusion

The KiteUI Gradle plugin provides valuable functionality but requires significant modernization to meet current Gradle best practices and performance standards. The most critical issues are:

1. **Source file modification** - Must be removed
2. **Configuration cache incompatibility** - Blocks future Gradle versions
3. **Fragile source parsing** - Should use proper AST tools
4. **Poor incremental build support** - Wastes developer time
5. **Insufficient error handling** - Poor user experience

### Recommended Roadmap

**Short-term (1-2 weeks):**
- Remove source modification feature
- Fix eager task registration
- Add error handling and validation
- Document basic usage

**Medium-term (1-2 months):**
- Create proper task classes
- Migrate to KSP for code generation
- Add comprehensive testing
- Implement incremental builds

**Long-term (3-6 months):**
- Full configuration cache compatibility
- Build cache optimization
- Performance improvements
- Complete documentation

### Estimated Effort
- Critical fixes: 2-3 days
- High priority items: 1-2 weeks
- Medium priority items: 3-4 weeks
- Full modernization: 2-3 months

The plugin is functional for current needs but technical debt is accumulating. Addressing these issues proactively will prevent future maintenance problems and provide a better developer experience.
