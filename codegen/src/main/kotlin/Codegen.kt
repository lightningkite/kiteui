package com.lightningkite.kiteui.codegen

import java.io.File

/**
 * The KiteUI code generators, independent of any build tool.
 *
 * This is the API that build integrations bind to: the Gradle plugin, and the small generated shim
 * a Kotlin Toolchain project vendors. Every entry point is a pure function of its input files, so a
 * caller only has to decide when to run it and where to put the output.
 *
 * Treat these signatures as public API. A consumer's build file names them directly, so changing one
 * breaks that consumer's build rather than their application code, which is a worse failure — it
 * happens at build time, for everyone, at once.
 *
 * Three preconditions are not visible in the signatures, because in the Gradle plugin something
 * outside these functions satisfies them. Any other caller has to satisfy them itself:
 *
 * 1. **Output directories are not created.** Every function writes its output file directly, so the
 *    file's parent directory must already exist. The Gradle plugin creates it when it registers the
 *    task, far from the call.
 * 2. **[resourcesJs] and [resourcesJvm] do not copy resources.** They generate declarations that
 *    load each resource at runtime from `common/<path>`, relative to the built artifact, but place
 *    nothing there; the caller must copy the resource folder to wherever that path resolves. In the
 *    Gradle plugin the JS copy is a separate `Copy` task — and the JVM one is missing entirely, so
 *    the JVM declarations currently reference files nothing places.
 * 3. **[resourcesIos]'s `outPlist` is an input as well as an output.** It rewrites the `UIAppFonts`
 *    array of an existing `Info.plist` rather than authoring the file, so the path must point at a
 *    real plist — normally the one in the Xcode project.
 */
public object Codegen {

    /**
     * Writes the `expect` declarations for everything in [resources] into [out].
     *
     * Every platform needs a matching call to one of the `resources*` functions below; common code
     * only sees what this generates.
     */
    public fun resourcesCommon(resources: File, packageName: String, out: File): Unit =
        com.lightningkite.kiteui.resourcesCommon(resources, out, packageName)

    /**
     * Writes the JavaScript `actual` declarations into [out].
     *
     * @param gitIgnores files to fill with a `.gitignore` covering the copied resources; the caller
     * decides which directories the copies land in, so it also owns the ignore files.
     */
    public fun resourcesJs(resources: File, packageName: String, gitIgnores: List<File>, out: File): Unit =
        com.lightningkite.kiteui.resourcesJs(gitIgnores, resources, out, packageName)

    /**
     * Writes the JVM `actual` declarations into [out].
     *
     * JVM loads resources the same way JS does — off a path relative to the built artifact — so this
     * shares the JS generator rather than duplicating it.
     */
    public fun resourcesJvm(resources: File, packageName: String, gitIgnores: List<File>, out: File): Unit =
        com.lightningkite.kiteui.resourcesJs(gitIgnores, resources, out, packageName)

    /**
     * Writes the iOS `actual` declarations into [outKt], and the Xcode-side assets that back them.
     *
     * Unlike the other platforms, iOS resources are not read from a bundle path at runtime: images
     * and vectors become asset catalog entries and fonts must be declared in the Info.plist, so this
     * writes into the Xcode project as well as into Kotlin.
     */
    public fun resourcesIos(
        resources: File,
        packageName: String,
        outPlist: File,
        outNonAssets: File,
        outAssets: File,
        outKt: File,
    ): Unit = com.lightningkite.kiteui.resourcesIos(resources, outPlist, outNonAssets, outAssets, outKt, packageName)

    /**
     * Writes the Android `actual` declarations into [outKt], and the resources they reference into
     * [androidResFolder] under Android's naming rules.
     */
    /**
     * @param lookUpResourcesByName emit runtime `getIdentifier` lookups rather than `R` constants.
     * Required where no generated `R` is available -- an Android library built by the Kotlin
     * Toolchain never invokes AGP. Leave false for Gradle builds: they keep compile-time resource
     * checking, and name-only lookups are invisible to resource shrinking.
     */
    public fun resourcesAndroid(
        resources: File,
        packageName: String,
        androidResFolder: File,
        outKt: File,
        lookUpResourcesByName: Boolean = false,
    ): Unit =
        com.lightningkite.kiteui.resourcesAndroid(resources, androidResFolder, outKt, packageName, lookUpResourcesByName)

    /** Scans [sources] for `@Routable` and `@FallbackRoute` and writes the route table into [out]. */
    public fun autoRoutes(sources: File, out: File): Unit =
        com.lightningkite.kiteui.generateAutoroutes(sources, out)

    /**
     * Collects the string templates used across [sources] and writes localization accessors into [out].
     *
     * Not currently wired into any build integration.
     */
    public fun localizations(sources: List<File>, packageName: String, out: File): Unit =
        com.lightningkite.kiteui.generateLocalizations(sources, out, packageName)
}
