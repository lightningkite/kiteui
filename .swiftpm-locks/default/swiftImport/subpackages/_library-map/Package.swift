// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_library-map",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "_library-map",
      type: .none,
      targets: ["_library-map"]
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
      name: "_library-map",
      dependencies: [
        .product(
          name: "MapLibre",
          package: "maplibre-gl-native-distribution"
        )
      ]
    )
  ]
)
