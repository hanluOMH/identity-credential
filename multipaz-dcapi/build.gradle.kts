import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.konan.target.HostManager
import com.android.build.gradle.LibraryExtension

// Check if Android SDK is available (Cloud Run build doesn't have it).
// Note: local.properties may exist but point to a developer machine path; only treat it as valid
// if the directory exists on the current machine.
val localPropertiesFile = rootProject.file("local.properties")
val sdkDirFromLocalProperties = localPropertiesFile
    .takeIf { it.exists() }
    ?.readLines()
    ?.firstOrNull { it.startsWith("sdk.dir=") }
    ?.substringAfter("sdk.dir=")

val androidHome = System.getenv("ANDROID_HOME")
val androidSdkRoot = System.getenv("ANDROID_SDK_ROOT")
val androidSdkDirProp = System.getProperty("android.sdk.dir")

val androidSdkAvailable =
    (androidHome != null && rootProject.file(androidHome).exists()) ||
    (androidSdkRoot != null && rootProject.file(androidSdkRoot).exists()) ||
    (androidSdkDirProp != null && rootProject.file(androidSdkDirProp).exists()) ||
    (sdkDirFromLocalProperties != null && rootProject.file(sdkDirFromLocalProperties).exists())

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary) apply false
    id("maven-publish")
}

if (androidSdkAvailable) {
    apply(plugin = "com.android.library")
}

val projectVersionCode: Int by rootProject.extra
val projectVersionName: String by rootProject.extra

kotlin {
    jvmToolchain(17)

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // Cloud Run builds need at least one Kotlin target (Linux builder has no Android/iOS toolchains).
    jvm()

    if (androidSdkAvailable) {
        androidTarget {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }

            publishLibraryVariants("release")
        }
    }

    // iOS targets only build on macOS (Cloud Run builder is Linux).
    if (HostManager.hostIsMac) {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach {
            val platform = when (it.name) {
                "iosX64" -> "iphonesimulator"
                "iosArm64" -> "iphoneos"
                "iosSimulatorArm64" -> "iphonesimulator"
                else -> error("Unsupported target ${it.name}")
            }
            it.binaries.all {
                linkerOpts(
                    "-L/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${platform}/",
                )
            }
        }
    }

    applyDefaultHierarchyTemplate()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":multipaz"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.io.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        if (androidSdkAvailable) {
            val androidMain by getting {
                dependencies {
                    implementation(libs.accompanist.permissions)
                    implementation(libs.androidx.material)
                    implementation(libs.play.services.identity.credentials)
                    implementation(libs.androidx.credentials)
                    implementation(libs.androidx.credentials.play.services.auth)
                }
            }
        }

        val commonTest by getting {
            kotlin.srcDir("build/generated/ksp/metadata/commonTest/kotlin")
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
                implementation(project(":multipaz-doctypes"))
            }
        }

        if (androidSdkAvailable) {
            val androidInstrumentedTest by getting {
                dependsOn(commonTest)
                dependencies {
                    // Only include matcherTest if Android SDK is available
                    if (rootProject.findProject(":multipaz-dcapi:matcherTest") != null) {
                        implementation(project(":multipaz-dcapi:matcherTest"))
                    }
                    implementation(libs.androidx.espresso.core)
                    implementation(libs.kotlin.test)
                    implementation(libs.kotlinx.coroutines.test)
                    implementation(libs.kotlinx.coroutines.android)
                }
            }
        }
    }
}

if (androidSdkAvailable) {
    // Configure Android only when SDK is available. Use typed extension configuration
    // to avoid Kotlin DSL accessors requiring the plugin at script compile-time.
    extensions.configure<LibraryExtension>("android") {
        namespace = "org.multipaz.dcapi"
        compileSdk = libs.versions.android.compileSdk.get().toInt()

        defaultConfig {
            minSdk = 26
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        dependencies {
            add("implementation", libs.kotlinx.datetime)
        }

        packaging {
            resources {
                excludes += listOf("/META-INF/{AL2.0,LGPL2.1}")
                excludes += listOf("/META-INF/versions/9/OSGI-INF/MANIFEST.MF")
            }
        }

        publishing {
            singleVariant("release") {
                withSourcesJar()
            }
        }

        testOptions {
            unitTests.isReturnDefaultValues = true
        }
    }
}

group = "org.multipaz"
version = projectVersionName

publishing {
    repositories {
        maven {
            url = uri("${rootProject.rootDir}/repo")
        }
    }
    publications.withType(MavenPublication::class) {
        pom {
            licenses {
                license {
                    name = "Apache 2.0"
                    url = "https://opensource.org/licenses/Apache-2.0"
                }
            }
        }
    }
}
