# usage-scan

A K2 Kotlin compiler plugin that records, as stable string keys, every reference **into** the
kiteui packages made by a downstream repo — resolved by the real compiler, so it covers **all**
platforms (JVM, JS, Android, Native/iOS) at member granularity. Neither source-text scanning
(misses members) nor JVM-bytecode scanning (misses JS/Native) can do this.

Purpose: derive, deterministically, which library declarations downstream code actually uses, to
drive the explicit-API migration's public-vs-internal decisions.

## Layout
- `plugin/` — the compiler plugin (standalone build, **not** part of kiteui's `settings.gradle.kts`).
- `inject.init.gradle.kts` — a Gradle init script that injects the plugin into every Kotlin
  compile task of a target build, with no edits to that build. Matches tasks by name and configures
  `freeCompilerArgs` reflectively to sidestep init-script classloader isolation.
- `scan-repo.sh` — convenience runner.
- `out/` — report files (gitignored). One file per compilation; merge with `sort -u`.

## Two modes
- `refs` (default) — run over a **downstream** repo: record references into watched packages.
- `decls` — run over the **library**: dump every declaration in watched packages (the denominator).

## Version pinning
The plugin jar must be compiled against the **same Kotlin minor** as the target build
(`kotlin-compiler-embeddable` in `plugin/build.gradle.kts`). 2.3.x repos (usbe, starter) share one
jar; a 2.2.x repo (e.g. lightning-time-tracker) needs a separately-built jar.

## Build & run
```bash
../../gradlew -p . :plugin:jar
./scan-repo.sh ~/Projects/ls-kiteui-starter starter \
    :apps:compileKotlinJvmSsr :apps:compileKotlinJs \
    :apps:compileDebugKotlinAndroid :apps:compileKotlinIosArm64
```

## Interpreting output
The union of `refs` across downstream repos is a **public floor**: every symbol in it MUST stay
`public`. Absence means "not used by *these* repos" — **not** "make it internal." Aggressive
internalization requires either the whole org's repos or human judgment.
