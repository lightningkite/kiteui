// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_example-app",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "_example-app",
      type: .none,
      targets: ["_example-app"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_example-app",
      dependencies: [
      ]
    )
  ]
)
