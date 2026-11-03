package com.lightningkite.kiteui

import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * Regression test for the SSR isDevelopment fix (commit 94f1ebe6b).
 *
 * Before the fix, `Platform.isDevelopment` on jvmSsr was hardcoded to `true`, so every
 * deployed SSR server installed debug exception handlers and could leak stack traces to
 * real visitors. It now reads `KITEUI_DEVELOPMENT` and defaults to `false` when unset.
 *
 * This test asserts the security-relevant default: with no `KITEUI_DEVELOPMENT` env var
 * set, `isDevelopment` must be false. It would have failed against the pre-fix
 * `get() = true` implementation, so it's a true regression test for the default path.
 *
 * NOT covered here: the `KITEUI_DEVELOPMENT=true` opt-in path. The JVM does not allow a
 * running process to mutate its own environment variables (System.getenv is a read-only
 * snapshot taken at process start), so exercising that branch in-process would require
 * either reflection into the environment map or a production test-seam - both explicitly
 * out of scope. It could be exercised by spawning a child JVM with the env var set, but
 * that trades a simple, robust test for a process-launching one tied to the test task's
 * classpath, for one branch of a one-line `?:` fallback; not done here without sign-off.
 */
class PlatformIsDevelopmentTest {
    @Test
    fun defaultsToNotDevelopmentWhenEnvVarUnset() {
        // Guard the assumption this test relies on: if some CI/dev environment happens to
        // export KITEUI_DEVELOPMENT, this test would be exercising the wrong branch.
        assertFalse(
            System.getenv("KITEUI_DEVELOPMENT")?.toBoolean() == true,
            "Test assumes KITEUI_DEVELOPMENT is unset or false in the test environment"
        )
        assertFalse(Platform.isDevelopment, "SSR must default to production (not development) when KITEUI_DEVELOPMENT is unset")
    }
}
