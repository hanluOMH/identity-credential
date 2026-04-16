// multipaz-server-deployment/build.gradle.kts
// Tasks for building Docker images and deployment bundles

// Maps Gradle project path → short name used for the JAR file and service identity.
// Short names must match the names used in start-servers.sh and nginx.conf.
val serverProjects = mapOf(
    "multipaz-verifier-server"                            to "verifier",
    "multipaz-openid4vci-server"                          to "openid4vci",
    "multipaz-backend-server"                             to "backend",
    "multipaz-records-server"                             to "records",
    "multipaz-csa-server"                                 to "csa",
    "multipaz-utopia:organizations:brewery:backend"       to "brewery"
)

tasks.register("collectDependencies") {
    description = "Collect thin server JARs and shared dependency JARs into a staging directory"
    group = "multipaz-server-deployment"

    // Depend on jar tasks for server projects plus their full runtime classpath build dependencies.
    // Also declare the server JARs and their runtime classpaths as *inputs* so Gradle considers
    // this task out-of-date whenever any JAR changes (without inputs the task would appear
    // UP-TO-DATE and Docker would receive a stale staging directory).
    for ((projectPath, _) in serverProjects) {
        val serverProject = project(":${projectPath}")
        dependsOn("${serverProject.path}:jar")
        dependsOn(serverProject.configurations.getByName("runtimeClasspath").buildDependencies)
        inputs.files(serverProject.tasks.named("jar").map { (it as Jar).archiveFile })
        inputs.files(serverProject.configurations.named("runtimeClasspath"))
    }

    val stagingDir = layout.buildDirectory.dir("docker-staging")

    outputs.dir(stagingDir)

    doLast {
        val jarsDir = stagingDir.get().dir("jars").asFile
        val libsDir = stagingDir.get().dir("libs").asFile
        jarsDir.mkdirs()
        libsDir.mkdirs()

        // Collect all unique dependency JARs from all server projects
        val seenLibs = mutableSetOf<String>()
        for ((projectPath, shortName) in serverProjects) {
            val serverProject = project(":${projectPath}")
            val runtimeCp = serverProject.configurations.getByName("runtimeClasspath")

            // Copy the thin server JAR under its short name
            val jarFile = serverProject.tasks.getByName<Jar>("jar").archiveFile.get().asFile
            jarFile.copyTo(File(jarsDir, "${shortName}.jar"), overwrite = true)

            // Copy dependency JARs (deduplicated by filename)
            for (dep in runtimeCp.resolve()) {
                if (dep.name.endsWith(".jar") && seenLibs.add(dep.name)) {
                    dep.copyTo(File(libsDir, dep.name), overwrite = true)
                }
            }
        }

        val libCount = libsDir.listFiles()?.size ?: 0
        println("Collected ${serverProjects.size} server JARs and ${libCount} shared dependency JARs")
    }
}

tasks.register("buildWebFrontend") {
    description = "Build the web frontend (Kotlin/JS)"
    group = "multipaz-server-deployment"

    dependsOn(":multipaz-server-frontend:jsBrowserDistribution")
}

tasks.register("buildAll") {
    description = "Build all server JARs and web frontend"
    group = "multipaz-server-deployment"

    dependsOn("collectDependencies", "buildWebFrontend")
}

// Helper function to get container tool (podman or docker)
fun getContainerTool(): String {
    return if (file("/usr/bin/podman").exists() ||
        file("/usr/local/bin/podman").exists() ||
        file("/opt/homebrew/bin/podman").exists()) {
        "podman"
    } else {
        "docker"
    }
}

tasks.register<Exec>("buildDockerImage") {
    description = "Build Docker image for native architecture"
    group = "multipaz-server-deployment"

    dependsOn("buildAll")

    workingDir = rootProject.projectDir

    val containerTool = getContainerTool()
    val version = rootProject.extra["projectVersionName"] as String

    // Detect native architecture
    val arch = System.getProperty("os.arch")
    val platform = when {
        arch == "aarch64" || arch == "arm64" -> "linux/arm64"
        else -> "linux/amd64"
    }

    commandLine(
        containerTool, "build",
        "--platform", platform,
        "-f", "multipaz-server-deployment/docker/Dockerfile",
        "-t", "multipaz/server-bundle:${version}",
        "-t", "multipaz/server-bundle:latest",
        "."
    )
}

tasks.register<Exec>("buildDockerImageAmd64") {
    description = "Build Docker image for amd64 (x86_64) architecture"
    group = "multipaz-server-deployment"

    dependsOn("buildAll")

    workingDir = rootProject.projectDir

    val containerTool = getContainerTool()
    val version = rootProject.extra["projectVersionName"] as String

    commandLine(
        containerTool, "build",
        "--platform", "linux/amd64",
        "-f", "multipaz-server-deployment/docker/Dockerfile",
        "-t", "multipaz/server-bundle:${version}-amd64",
        "-t", "multipaz/server-bundle:latest-amd64",
        "."
    )
}

tasks.register<Exec>("buildDockerImageArm64") {
    description = "Build Docker image for arm64 (Apple Silicon, AWS Graviton) architecture"
    group = "multipaz-server-deployment"

    dependsOn("buildAll")

    workingDir = rootProject.projectDir

    val containerTool = getContainerTool()
    val version = rootProject.extra["projectVersionName"] as String

    commandLine(
        containerTool, "build",
        "--platform", "linux/arm64",
        "-f", "multipaz-server-deployment/docker/Dockerfile",
        "-t", "multipaz/server-bundle:${version}-arm64",
        "-t", "multipaz/server-bundle:latest-arm64",
        "."
    )
}

tasks.register<Exec>("runDockerImage") {
    description = "Run the Docker image locally"
    group = "multipaz-server-deployment"

    val containerTool = getContainerTool()

    commandLine(
        containerTool, "run",
        "--rm",
        "-p", "8000-8009:8000-8009",
        "multipaz/server-bundle:latest"
    )
}
