# MapLibre style spec

`v8.json` is copied from [`@maplibre/maplibre-gl-style-spec`](https://github.com/maplibre/maplibre-style-spec) v26.4.4
(`src/reference/v8.json`), the version `maplibre-gl` 6.12.0 depends on. `LICENSE.txt` is that package's license.

The `generateMapLibreStyle` task generates the typed layer classes in `com.lightningkite.kiteui.map.style` from it.
To update, copy the newer `v8.json` over this one, matching the `maplibre-gl` version in `../build.gradle.kts`.
