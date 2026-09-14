package com.lightningkite.kiteui.testing

import org.junit.runner.RunWith
import kotlin.reflect.KClass

/**
 * Android actual - marker annotation that triggers Robolectric test runner.
 * On Android, test classes should also be annotated with the real @RunWith.
 * This is handled by applying @RunWith directly in tests via annotation processing.
 */
actual typealias JUnitRunWith = RunWith

actual typealias JUnitRunner = org.junit.runner.Runner

actual typealias Application = android.app.Application

/**
 * Android actual - typealias to Robolectric's test runner class.
 * This allows referencing RobolectricTestRunner in common code.
 */
actual typealias RobolectricTestRunner = org.robolectric.RobolectricTestRunner

/**
 * Android actual - typealias to Robolectric's @Config annotation.
 */
actual typealias RobolectricConfig = org.robolectric.annotation.Config

actual typealias GraphicsMode = org.robolectric.annotation.GraphicsMode

actual typealias GraphicsModeEnum = org.robolectric.annotation.GraphicsMode.Mode