rootProject.name = "MultipazProject"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// As per https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html#-o0tm8i_54
// "Currently, you cannot run common Compose Multiplatform tests using android (local) test
// configurations, so gutter icons in Android Studio, for example, won't be helpful."
//
// This is not a problem because the tests will get run as part of the multipaz-compose:connectedCheck
// tasks.
//
// When this starts working again, we can remove the lines below.
//
// Check if Android SDK is available (for Cloud Run builds).
//
// Note: local.properties may be present in the repo but can point to a developer machine path.
// Only treat it as valid if the SDK directory exists on the current machine.
val localPropertiesFile = file("local.properties")
val sdkDirFromLocalProperties = localPropertiesFile
    .takeIf { it.exists() }
    ?.readLines()
    ?.firstOrNull { it.startsWith("sdk.dir=") }
    ?.substringAfter("sdk.dir=")

val androidHome = System.getenv("ANDROID_HOME")
val androidSdkRoot = System.getenv("ANDROID_SDK_ROOT")
val androidSdkDirProp = System.getProperty("android.sdk.dir")

val androidSdkAvailable =
    (androidHome != null && file(androidHome).exists()) ||
    (androidSdkRoot != null && file(androidSdkRoot).exists()) ||
    (androidSdkDirProp != null && file(androidSdkDirProp).exists()) ||
    (sdkDirFromLocalProperties != null && file(sdkDirFromLocalProperties).exists())

// Only exclude these tasks if multipaz-compose project is included
if (androidSdkAvailable) {
startParameter.excludedTaskNames +=
    listOf(
        ":multipaz-compose:testDebugUnitTest",
        ":multipaz-compose:testReleaseUnitTest"
    )
}

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://jitpack.io") {
            mavenContent {
                includeGroup("com.github.yuriy-budiyev")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.9.0")
}

include(":multipaz-cbor-rpc")
include(":multipaz")
include(":multipaz:SwiftBridge")
include(":multipaz-dcapi")
include(":multipaz-doctypes")
if (androidSdkAvailable) {
include(":multipaz-android-legacy")
    include(":multipaz-dcapi:matcherTest")
    include(":multipaz-compose")
    include(":samples:testapp")
}
include(":multipaz-csa")
include(":multipaz-longfellow")
include(":multipazctl")
include(":multipaz-backend-server")
include(":multipaz-openid4vci-server")
include(":multipaz-verifier-server")
include(":multipaz-csa-server")
include(":multipaz-records-server")
if (androidSdkAvailable) {
include(":xcframework")
}
