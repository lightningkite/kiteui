// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "KotlinMultiplatformLinkedPackageDylib",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "KotlinMultiplatformLinkedPackageDylib",
      type: .dynamic,
      targets: ["KotlinMultiplatformLinkedPackageDylib"]
    )
  ],
  dependencies: [
    .package(path: "../_library_map")
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackageDylib",
      dependencies: [
        .product(name: "_library_map", package: "_library_map")
      ]
    )
  ]
)
