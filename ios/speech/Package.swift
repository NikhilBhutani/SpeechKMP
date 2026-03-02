// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "DeviceAISpeech",
    platforms: [
        .iOS(.v16)
    ],
    products: [
        .library(
            name: "DeviceAISpeech",
            targets: ["DeviceAISpeech"]
        )
    ],
    targets: [
        // Future: wrap the KMP framework produced by kmp/speech with
        // idiomatic Swift APIs (async/await, Combine publishers, etc.)
        .target(
            name: "DeviceAISpeech",
            path: "Sources/DeviceAISpeech"
        )
    ]
)
