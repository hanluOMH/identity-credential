import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.konan.target.HostManager
import com.android.build.gradle.LibraryExtension

// Check if Android SDK is available (for Cloud Run builds)
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
    // Don't apply Android plugin unless SDK is available (Cloud Run build doesn't have it).
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildconfig)
    id("maven-publish")
}

if (androidSdkAvailable) {
    apply(plugin = "com.android.library")
}

val projectVersionCode: Int by rootProject.extra
val projectVersionName: String by rootProject.extra

buildConfig {
    packageName("org.multipaz.util")
    buildConfigField("VERSION", projectVersionName)
    useKotlinOutput { internalVisibility = true }
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

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
            it.compilations.getByName("main") {
                val SwiftBridge by cinterops.creating {
                    definitionFile.set(project.file("nativeInterop/cinterop/SwiftBridge-$platform.def"))
                    includeDirs.headerFilterOnly("$rootDir/multipaz/SwiftBridge/build/Release-$platform/include")

                    val interopTask = tasks[interopProcessingTaskName]
                    val capitalizedPlatform = platform.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase()
                        else it.toString()
                    }
                    interopTask.dependsOn(":multipaz:SwiftBridge:build${capitalizedPlatform}")
                }

                it.binaries.all {
                    // Linker options required to link to the library.
                    linkerOpts(
                        "-L/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${platform}/",
                        "-L$rootDir/multipaz/SwiftBridge/build/Release-${platform}/",
                        "-lSwiftBridge"
                    )
                }
            }
        }
    }

    // we want some extra dependsOn calls to create
    // javaSharedMain to share between JVM and Android,
    // but otherwise want to follow default hierarchy.
    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            // KSP generated sources for commonMain (metadata compilation).
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation(libs.kotlinx.io.bytestring)
                implementation(libs.kotlinx.io.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                api(libs.kotlinx.io.bytestring)
                api(libs.kotlinx.io.core)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.json)
                api(libs.ktor.client.core)
            }
        }

        val commonTest by getting {
            // KSP generated sources for commonTest (metadata compilation).
            kotlin.srcDir("build/generated/ksp/metadata/commonTest/kotlin")
            dependencies {
                implementation(libs.bouncy.castle.bcprov)
                implementation(libs.bouncy.castle.bcpkix)
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
                implementation(project(":multipaz-doctypes"))
            }
        }

        val javaSharedMain by creating {
            dependsOn(commonMain)
            dependencies {
            }
        }

        val jvmMain by getting {
            dependsOn(javaSharedMain)
            // Include KSP output for the JVM target.
            //
            // NOTE: KSP output folder names vary across KSP/Kotlin versions and Gradle plugins.
            // Cloud Run failures indicate generated sources are not being seen, so we include the common variants.
            kotlin.srcDir("build/generated/ksp/jvm/jvmMain/kotlin")
            kotlin.srcDir("build/generated/ksp/jvmMain/kotlin")
            kotlin.srcDir("build/generated/ksp/kotlinJvm/jvmMain/kotlin")
            dependencies {
                implementation(libs.ktor.client.java)
            }
        }

        if (androidSdkAvailable) {
        val androidMain by getting {
            dependsOn(javaSharedMain)
            dependencies {
                implementation(libs.androidx.biometrics)
                implementation(libs.androidx.lifecycle.viewmodel)
                }
            }
        }

        // iOS source sets only exist when iOS targets are created (macOS builds).
        if (HostManager.hostIsMac) {
        val iosMain by getting {
            dependencies {
                // This dependency is needed for SqliteStorage implementation.
                // KMP-compatible version is still alpha and it is not compatible with
                // other androidx packages, particularly androidx.work that we use in wallet.
                // TODO: once compatibility issues are resolved, SqliteStorage and this
                // dependency can be moved into commonMain.
                implementation(libs.androidx.sqlite)
                implementation(libs.androidx.sqlite.framework)
                }
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.hsqldb)
                implementation(libs.mysql)
                implementation(libs.postgresql)
                implementation(libs.nimbus.oauth2.oidc.sdk)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.tink)
            }
        }

//        val androidInstrumentedTest by getting {
//            dependsOn(commonTest)
//            dependencies {
//                implementation(libs.bouncy.castle.bcprov)
//                implementation(libs.bouncy.castle.bcpkix)
//                implementation(project(":multipaz-doctypes"))
//                implementation(project(":multipaz-dcapi"))
//                // Only include matcherTest if Android SDK is available
//                if (rootProject.findProject(":multipaz-dcapi:matcherTest") != null) {
//                    implementation(project(":multipaz-dcapi:matcherTest"))
//                }
//                implementation(libs.androidx.sqlite)
//                implementation(libs.androidx.sqlite.framework)
//                implementation(libs.androidx.sqlite.bundled)
//                implementation(libs.androidx.test.junit)
//                implementation(libs.androidx.espresso.core)
//                implementation(libs.kotlin.test)
//                implementation(libs.kotlinx.coroutines.test)
//                implementation(libs.kotlinx.coroutines.android)
//                implementation(libs.ktor.client.mock)
//                implementation(project(":multipaz-csa"))
//            }
//        }
//
//        val iosTest by getting {
//            dependencies {
//                implementation(libs.androidx.sqlite)
//                implementation(libs.androidx.sqlite.framework)
//                implementation(libs.androidx.sqlite.bundled)
//            }
//        }
    }
}

dependencies {
    add("kspCommonMainMetadata", project(":multipaz-cbor-rpc"))
    // Generate CBOR/RPC code for the JVM compilation (Cloud Run builds are JVM-only).
    // In KMP+KSP, the *task* is typically `kspKotlinJvm`, while the *configuration* is `kspJvm`.
    add("kspJvm", project(":multipaz-cbor-rpc"))
    add("kspJvmTest", project(":multipaz-cbor-rpc"))
}

// Ensure KSP runs before compilation.
//
// Root cause on Cloud Run: Kotlin compilation happens without the KSP-generated sources present.
// CRITICAL: commonMain code is compiled during metadata compilation, so we MUST ensure KSP runs
// before ANY compilation task that processes commonMain sources.
//
// Strategy: Use whenTaskAdded to hook into tasks as they're registered, and make compilation
// tasks depend on any KSP task that matches common patterns. This is more robust than trying
// to find tasks by exact name, since KSP task names can vary by version/platform.
tasks.whenTaskAdded {
    // When a KSP task is added, make all compilation tasks depend on it
    if (name.startsWith("ksp") && (name.contains("CommonMain") || name.contains("Metadata") || name.contains("Jvm"))) {
        tasks.withType<KotlinCompile>().configureEach {
            if (!name.startsWith("ksp")) {
                dependsOn(this@whenTaskAdded)
            }
        }
        tasks.withType<KotlinNativeCompile>().configureEach {
            if (!name.startsWith("ksp")) {
                dependsOn(this@whenTaskAdded)
            }
        }
    }
}

afterEvaluate {
    // Find all KSP tasks by pattern (more flexible than exact name matching)
    val kspTasks = tasks.matching { it.name.startsWith("ksp") && 
        (it.name.contains("CommonMain") || it.name.contains("Metadata") || it.name.contains("Jvm")) }
    
    // Convert to list to check if empty (TaskCollection doesn't have isEmpty property)
    val kspTasksList = kspTasks.toList()
    
    if (kspTasksList.isEmpty()) {
        // Only warn if we have KSP dependencies configured but no tasks
        val hasKspDependency = configurations.findByName("kspCommonMainMetadata") != null || 
                               configurations.findByName("kspJvm") != null
        if (hasKspDependency) {
            logger.warn(
                "KSP dependencies configured but no KSP tasks found. " +
                "This might be normal if KSP tasks are created lazily. " +
                "If build fails with 'Unresolved reference', check KSP plugin configuration."
            )
        }
    } else {
        // Make all compilation tasks depend on found KSP tasks
        // Use the TaskCollection (kspTasks) for dependsOn as it accepts TaskCollection
        tasks.withType<KotlinCompile>().configureEach {
            if (!name.startsWith("ksp")) {
                dependsOn(kspTasks)
            }
        }
        
        tasks.withType<KotlinNativeCompile>().configureEach {
            if (!name.startsWith("ksp")) {
                dependsOn(kspTasks)
            }
        }
        
        // Ensure assemble and classes depend on KSP
        tasks.matching { it.name == "assemble" || it.name == "classes" }.configureEach {
            dependsOn(kspTasks)
        }
    }
    
    // Fail fast with a clear message if KSP outputs are missing in CI/Cloud Run.
    // Check BEFORE any compilation task runs.
    tasks.withType<KotlinCompile>().configureEach {
        if (!name.startsWith("ksp")) {
            doFirst {
                val buildDir = layout.buildDirectory.get().asFile
                val metadataDir = file("$buildDir/generated/ksp/metadata/commonMain/kotlin")
                val jvmDir = file("$buildDir/generated/ksp/jvm/jvmMain/kotlin")
                val hasMetadataGenerated = (metadataDir.exists() && 
                    project.fileTree(metadataDir).matching { include("**/*.kt") }.files.isNotEmpty()) ||
                    (jvmDir.exists() && 
                    project.fileTree(jvmDir).matching { include("**/*.kt") }.files.isNotEmpty())
                
                if (!hasMetadataGenerated) {
                    throw GradleException(
                        "KSP generated sources not found.\n" +
                        "Task: ${this.name}\n" +
                        "Checked directories:\n" +
                        "  - ${metadataDir.path}\n" +
                        "  - ${jvmDir.path}\n" +
                        "This means KSP tasks did not run or failed.\n" +
                        "Check build logs for KSP task execution and errors.\n" +
                        "Expected files like: DeviceAssertion_Cbor.kt, ClientCheckStub.kt, etc."
                    )
                }
            }
        }
    }
}

tasks.withType<Test> {
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }
}

// Configure Android only when SDK is available. Use typed extension configuration
// to avoid Kotlin DSL accessors requiring the plugin at script compile-time.
if (androidSdkAvailable) {
    extensions.configure<LibraryExtension>("android") {
    namespace = "org.multipaz"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

subprojects {
	apply(plugin = "org.jetbrains.dokka")
}
