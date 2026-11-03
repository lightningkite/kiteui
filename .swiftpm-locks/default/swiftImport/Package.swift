// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "KotlinMultiplatformLinkedPackage",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "KotlinMultiplatformLinkedPackage",
      type: .none,
      targets: ["KotlinMultiplatformLinkedPackage"]
    )
  ],
  dependencies: [
    .package(path: "subpackages/_example-app"),
    .package(path: "subpackages/_library-map"),
    .package(path: "subpackages/_library_map")
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackage",
      dependencies: [
        .product(name: "_example-app", package: "_example-app"),
        .product(name: "_library-map", package: "_library-map"),
        .product(name: "_library_map", package: "_library_map")
      ]
    )
  ]
)
