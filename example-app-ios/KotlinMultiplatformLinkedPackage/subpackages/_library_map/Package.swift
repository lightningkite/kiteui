// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_library_map",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "_library_map",
      type: .none,
      targets: ["_library_map"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/maplibre/maplibre-gl-native-distribution.git",
      from: "6.0.0"
    )
  ],
  targets: [
    .target(
      name: "_library_map",
      dependencies: [
        .product(
          name: "MapLibre",
          package: "maplibre-gl-native-distribution"
        )
      ]
    )
  ]
)
